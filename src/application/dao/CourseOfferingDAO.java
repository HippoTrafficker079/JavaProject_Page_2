package application.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

public class CourseOfferingDAO {
    public record SessionInput(LocalDate sessionDate, int period) {
    }

    public int createCourseOffering(
            String lecturerName,
            int lecturerDeptId,
            List<Integer> targetDeptIds,
            int courseId,
            int year,
            String term,
            LocalDate startDate,
            int maxStudent,
            int totalSessions,
            List<SessionInput> sessions,
            BigDecimal tutorialWeight,
            BigDecimal assignmentWeight,
            BigDecimal labTestWeight,
            BigDecimal practicalWeight,
            BigDecimal finalExamWeight) throws SQLException {
        if (sessions == null || sessions.isEmpty()) {
            throw new IllegalArgumentException("At least one class session is required.");
        }
        if (sessions.size() != totalSessions) {
            throw new IllegalArgumentException(
                    "Session count (" + sessions.size() + ") does not match totalSessions (" + totalSessions + ").");
        }
        if (targetDeptIds == null || targetDeptIds.isEmpty()) {
            throw new IllegalArgumentException("At least one target department ID is required.");
        }
        for (Integer deptId : targetDeptIds) {
            if (deptId == null || deptId <= 0) {
                throw new IllegalArgumentException("Invalid target department ID: " + deptId);
            }
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required.");
        }
        if (maxStudent < 0 || totalSessions <= 0) {
            throw new IllegalArgumentException("maxStudent must be non-negative and totalSessions must be positive.");
        }
        validateWeights(tutorialWeight, assignmentWeight, labTestWeight, practicalWeight, finalExamWeight);

        String insertOfferingSql = """
                INSERT INTO course_offering (
                    teacher_id,
                    course_id,
                    `year`,
                    total_sessions,
                    term,
                    max_student
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        String insertSessionSql = """
                INSERT INTO class_session (
                    offering_id,
                    session_date,
                    period
                )
                VALUES (?, ?, ?)
                """;

        String insertWeightSql = """
                INSERT INTO assessment_weight (
                    offering_id,
                    tutorial_weight,
                    assignment_weight,
                    lab_test_weight,
                    practical_weight,
                    final_exam_weight
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        String placeholders = String.join(", ", java.util.Collections.nCopies(targetDeptIds.size(), "?"));

        String enrollStudentsSql = """
                INSERT IGNORE INTO course_enrollment (offering_id, student_id, enrollment_date)
                SELECT ?, s.student_id, ?
                FROM student s
                JOIN user_account ua ON s.user_id = ua.user_id
                WHERE ua.status = TRUE
                  AND s.current_year = ?
                  AND s.dept_id IN (%s)
                """.formatted(placeholders);

        try (Connection conn = DatabaseConnector.getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                LecturerDAO lecturerDAO = new LecturerDAO();
                int teacherId = lecturerDAO.getLecturerTeacherIdByNameAndDept(conn, lecturerName, lecturerDeptId);

                int offeringId;
                try (PreparedStatement offeringStmt = conn.prepareStatement(
                        insertOfferingSql, Statement.RETURN_GENERATED_KEYS)) {

                    offeringStmt.setInt(1, teacherId);
                    offeringStmt.setInt(2, courseId);
                    offeringStmt.setInt(3, year);
                    offeringStmt.setInt(4, totalSessions);
                    offeringStmt.setString(5, term);
                    offeringStmt.setInt(6, maxStudent);

                    if (offeringStmt.executeUpdate() == 0) {
                        throw new SQLException("Creating course_offering failed, no rows affected.");
                    }

                    try (ResultSet keys = offeringStmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            offeringId = keys.getInt(1);
                        } else {
                            throw new SQLException("Creating course_offering failed, no generated ID returned.");
                        }
                    }
                }

                try (PreparedStatement sessionStmt = conn.prepareStatement(insertSessionSql)) {
                    for (SessionInput session : sessions) {
                        if (session.sessionDate() == null || session.period() <= 0) {
                            throw new IllegalArgumentException("Invalid session entry: " + session);
                        }
                        sessionStmt.setInt(1, offeringId);
                        sessionStmt.setDate(2, Date.valueOf(session.sessionDate()));
                        sessionStmt.setInt(3, session.period());
                        sessionStmt.addBatch();
                    }
                    int[] batchCounts = sessionStmt.executeBatch();
                    if (batchCounts.length != sessions.size()) {
                        throw new SQLException("Incomplete class_session batch insert.");
                    }
                }

                try (PreparedStatement weightStmt = conn.prepareStatement(insertWeightSql)) {
                    weightStmt.setInt(1, offeringId);
                    weightStmt.setBigDecimal(2, tutorialWeight);
                    weightStmt.setBigDecimal(3, assignmentWeight);
                    weightStmt.setBigDecimal(4, labTestWeight);
                    weightStmt.setBigDecimal(5, practicalWeight);
                    weightStmt.setBigDecimal(6, finalExamWeight);

                    if (weightStmt.executeUpdate() == 0) {
                        throw new SQLException("Creating assessment_weight failed, no rows affected.");
                    }
                }

                try (PreparedStatement enrollStmt = conn.prepareStatement(enrollStudentsSql)) {
                    enrollStmt.setInt(1, offeringId);
                    enrollStmt.setDate(2, Date.valueOf(startDate));
                    enrollStmt.setInt(3, year);

                    int paramIndex = 4;
                    for (Integer deptId : targetDeptIds) {
                        enrollStmt.setInt(paramIndex++, deptId);
                    }
                    enrollStmt.executeUpdate();
                }

                conn.commit();
                return offeringId;

            } catch (Exception ex) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
                if (ex instanceof SQLException sqlEx) {
                    throw sqlEx;
                }
                throw new SQLException("Failed to create course offering transactionally", ex);
            } finally {
                try {
                    conn.setAutoCommit(previousAutoCommit);
                } catch (SQLException ignored) {
                    // Connection is about to close
                }
            }
        }
    }

    private void validateWeights(
            BigDecimal tutorial,
            BigDecimal assignment,
            BigDecimal labTest,
            BigDecimal practical,
            BigDecimal finalExam) {

        BigDecimal[] weights = { tutorial, assignment, labTest, practical, finalExam };
        BigDecimal sum = BigDecimal.ZERO;

        for (BigDecimal w : weights) {
            if (w == null || w.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Assessment weights must be non-negative.");
            }
            sum = sum.add(w);
        }

        if (sum.compareTo(new BigDecimal("100.00")) != 0) {
            throw new IllegalArgumentException(
                    "Assessment weights must sum to 100.00, got: " + sum);
        }
    }
}
