package application.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import application.ClassDetailController.StudentModel;

public class CourseEnrollmentDAO {
	
    public record EnrollableStudent(
            int studentId,
            String rollNo,
            String fullName,
            String email,
            String deptCode,
            String currentYear,
            double gpa) {
    }

    public List<StudentModel> getAllEnrolledStudent(int offeringId, int userDeptId) {
        String sql = """
                SELECT
                    s.student_id,
                    s.roll_no,
                    s.full_name,
                    sci.email_address,
                    COALESCE(
                        ROUND(
                            (
                                COUNT(DISTINCT cs.session_id) - COUNT(DISTINCT a.attendance_id)
                            ) * 100.0 / NULLIF(
                                COUNT(DISTINCT cs.session_id),
                                0
                            ),
                            2
                        ),
                        0.00
                    ) AS attendance_percentage,
                    s.gpa
                FROM student s
                LEFT JOIN student_contact_information sci ON s.student_id = sci.student_id
                JOIN course_enrollment ce ON s.student_id = ce.student_id AND ce.status = 'ENROLLED'
                JOIN course_offering co ON ce.offering_id = co.offering_id
                JOIN course c ON co.course_id = c.course_id
                LEFT JOIN class_session cs ON cs.offering_id = co.offering_id AND cs.is_cancelled = FALSE
                LEFT JOIN attendance a ON cs.session_id = a.session_id AND ce.enrollment_id = a.enrollment_id
                WHERE co.offering_id = ?
                  AND (c.dept_id = ? OR s.dept_id = ?)
                GROUP BY s.student_id, s.roll_no, s.full_name, sci.email_address, s.gpa
                ORDER BY s.roll_no ASC;
                """;
        List<StudentModel> activeStudents = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, offeringId);
            pstmt.setInt(2, userDeptId);
            pstmt.setInt(3, userDeptId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int studentId = rs.getInt("student_id");
                    String rollNum = rs.getString("roll_no");
                    String name = rs.getString("full_name");
                    String emailAddress = rs.getString("email_address");
                    BigDecimal attendanceBd = rs.getBigDecimal("attendance_percentage");
                    int attendancePercentage = (attendanceBd != null) ? attendanceBd.intValue() : 0;
                    BigDecimal gpaDecimal = rs.getBigDecimal("gpa");
                    double gpa = (gpaDecimal != null) ? gpaDecimal.doubleValue() : 0.0;
                    activeStudents.add(
                            new StudentModel(String.valueOf(studentId), rollNum, name, emailAddress,
                                    attendancePercentage, gpa));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return activeStudents;
    }

    public int removeStudentFromClass(int studentId, int offeringId) {
        String sql = """
                UPDATE course_enrollment
                SET status = 'DROPPED'
                WHERE student_id = ?
                  AND offering_id = ?;
                """;
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, offeringId);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Fetches all active students in the database who are NOT currently ENROLLED
     * in the given class offering (includes newly added students and previously dropped ones).
     */
    public List<EnrollableStudent> getUnenrolledStudentsForOffering(int offeringId) {
        List<EnrollableStudent> list = new ArrayList<>();
        String sql = """
                SELECT
                    s.student_id,
                    s.roll_no,
                    s.full_name,
                    COALESCE(sci.email_address, ua.email) AS email,
                    d.dept_code,
                    s.current_year,
                    COALESCE(s.gpa, 0.00) AS gpa
                FROM student s
                JOIN user_account ua ON s.user_id = ua.user_id
                JOIN department d ON s.dept_id = d.dept_id
                JOIN course_offering co ON co.offering_id = ?
                LEFT JOIN student_contact_information sci ON s.student_id = sci.student_id
                WHERE ua.status = TRUE
                  AND LOWER(TRIM(s.current_year)) = LOWER(TRIM(co.`year`))
                  AND NOT EXISTS (
                      SELECT 1
                      FROM course_enrollment ce
                      WHERE ce.offering_id = co.offering_id
                        AND ce.student_id = s.student_id
                        AND ce.status = 'ENROLLED'
                  )
                ORDER BY d.dept_code ASC, s.roll_no ASC
                """;

        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, offeringId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new EnrollableStudent(
                            rs.getInt("student_id"),
                            rs.getString("roll_no"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("dept_code"),
                            rs.getString("current_year"),
                            rs.getDouble("gpa")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Enrolls (or re-enrolls if previously DROPPED) the selected students into the offering.
     */
    public int enrollStudentsInOffering(int offeringId, List<Integer> studentIds) throws SQLException {
        if (offeringId <= 0 || studentIds == null || studentIds.isEmpty()) {
            return 0;
        }

        String sql = """
                INSERT INTO course_enrollment (offering_id, student_id, enrollment_date, status)
                VALUES (?, ?, CURDATE(), 'ENROLLED')
                ON DUPLICATE KEY UPDATE
                    status = 'ENROLLED',
                    enrollment_date = CURDATE()
                """;

        try (Connection conn = DatabaseConnector.getConnection()) {
            boolean prevAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Integer studentId : studentIds) {
                    ps.setInt(1, offeringId);
                    ps.setInt(2, studentId);
                    ps.addBatch();
                }
                int[] counts = ps.executeBatch();
                conn.commit();
                return counts.length;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(prevAuto);
            }
        }
    }

    /**
     * Promotes all ENROLLED students in this offering to COMPLETED and credits the course's credits
     * to each promoted student's total_credit.
     */
    public int completeClassForAllEnrolled(int offeringId) throws SQLException {
        if (offeringId <= 0) {
            return 0;
        }

        String addCreditSql = """
                UPDATE student s
                JOIN course_enrollment ce ON s.student_id = ce.student_id
                JOIN course_offering co ON ce.offering_id = co.offering_id
                JOIN course c ON co.course_id = c.course_id
                SET s.total_credit = COALESCE(s.total_credit, 0.0) + c.credit
                WHERE ce.offering_id = ?
                  AND ce.status = 'ENROLLED'
                """;

        String markCompletedSql = """
                UPDATE course_enrollment
                SET status = 'COMPLETED'
                WHERE offering_id = ?
                  AND status = 'ENROLLED'
                """;

        try (Connection conn = DatabaseConnector.getConnection()) {
            boolean prevAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement creditStmt = conn.prepareStatement(addCreditSql)) {
                    creditStmt.setInt(1, offeringId);
                    creditStmt.executeUpdate();
                }

                int promotedCount;
                try (PreparedStatement completeStmt = conn.prepareStatement(markCompletedSql)) {
                    completeStmt.setInt(1, offeringId);
                    promotedCount = completeStmt.executeUpdate();
                }

                conn.commit();
                return promotedCount;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(prevAuto);
            }
        }
    }
    
}