package application.dao;

import application.dto.Student;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class StudentDAO {

    public record StudentTabMetrics(
            int totalStudents,
            int enrolledStudents,
            int atRiskStudents,
            double averageGpa) {
    }

    private String lastBatchError = "";

    public String getLastBatchError() {
        return lastBatchError;
    }

    // =========================================================================
    // PAGE 2 ROLE-SCOPED QUERIES (Department Admin & Lecturer)
    // =========================================================================

    public List<Student> getStudentsForDepartment(int deptId, String search, String year, String semester) {
        List<Student> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                WITH RankedStudents AS (
                    SELECT
                        s.student_id,
                        s.roll_no,
                        s.full_name,
                        d.dept_code,
                        s.current_year,
                        COALESCE(co.term, 'Pending') AS term,
                        COALESCE(ci.primary_phone_number, '-') AS primary_phone_number,
                        COALESCE(ci.email_address, ua.email) AS email_address,
                        ua.status,
                        ROW_NUMBER() OVER (
                            PARTITION BY s.student_id
                            ORDER BY ce.enrollment_id DESC
                        ) AS rn
                    FROM student s
                    JOIN user_account ua ON s.user_id = ua.user_id
                    JOIN department d ON s.dept_id = d.dept_id
                    LEFT JOIN student_contact_information ci ON s.student_id = ci.student_id
                    LEFT JOIN course_enrollment ce ON s.student_id = ce.student_id AND ce.status = 'ENROLLED'
                    LEFT JOIN course_offering co ON ce.offering_id = co.offering_id
                    WHERE ua.status = TRUE
                      AND s.dept_id = ?
                )
                SELECT student_id, roll_no, full_name, dept_code, current_year, term, primary_phone_number, email_address
                FROM RankedStudents
                WHERE rn = 1
                """);

        List<Object> params = new ArrayList<>();
        params.add(deptId);

        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(full_name) LIKE ? OR LOWER(roll_no) LIKE ? OR LOWER(primary_phone_number) LIKE ? OR LOWER(email_address) LIKE ?) ");
            String kw = "%" + search.trim().toLowerCase() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        if (year != null && !year.isBlank() && !"All Years".equalsIgnoreCase(year)) {
            sql.append(" AND LOWER(TRIM(current_year)) = LOWER(TRIM(?)) ");
            params.add(year.trim());
        }

        if (semester != null && !semester.isBlank() && !"All Semesters".equalsIgnoreCase(semester)) {
            sql.append(" AND LOWER(TRIM(term)) = LOWER(TRIM(?)) ");
            params.add(semester.trim());
        }

        sql.append(" ORDER BY current_year ASC, roll_no ASC ");

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Student st = new Student(
                            rs.getInt("student_id"),
                            rs.getString("roll_no"),
                            rs.getString("full_name"),
                            rs.getString("dept_code"),
                            rs.getString("current_year"),
                            rs.getString("term"),
                            rs.getString("primary_phone_number"));
                    st.setEmail(rs.getString("email_address"));
                    list.add(st);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Student> getStudentsForLecturer(int teacherUserId, String search, String year, String subjectName) {
        List<Student> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                WITH LecturerStudents AS (
                    SELECT
                        s.student_id,
                        s.roll_no,
                        s.full_name,
                        d.dept_code,
                        s.current_year,
                        COALESCE(co.term, 'Pending') AS term,
                        COALESCE(ci.primary_phone_number, '-') AS primary_phone_number,
                        COALESCE(ci.email_address, ua.email) AS email_address,
                        ROW_NUMBER() OVER (
                            PARTITION BY s.student_id
                            ORDER BY ce.enrollment_id DESC
                        ) AS rn
                    FROM student s
                    JOIN user_account ua ON s.user_id = ua.user_id
                    JOIN department d ON s.dept_id = d.dept_id
                    LEFT JOIN student_contact_information ci ON s.student_id = ci.student_id
                    JOIN course_enrollment ce ON s.student_id = ce.student_id AND ce.status = 'ENROLLED'
                    JOIN course_offering co ON ce.offering_id = co.offering_id
                    JOIN course c ON co.course_id = c.course_id
                    JOIN teacher t ON co.teacher_id = t.teacher_id
                    WHERE ua.status = TRUE
                      AND t.user_id = ?
                """);

        List<Object> params = new ArrayList<>();
        params.add(teacherUserId);

        if (subjectName != null && !subjectName.isBlank() && !"All Subjects".equalsIgnoreCase(subjectName)) {
            sql.append(" AND LOWER(TRIM(c.course_name)) = LOWER(TRIM(?)) ");
            params.add(subjectName.trim());
        }

        sql.append("""
                )
                SELECT student_id, roll_no, full_name, dept_code, current_year, term, primary_phone_number, email_address
                FROM LecturerStudents
                WHERE rn = 1
                """);

        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(full_name) LIKE ? OR LOWER(roll_no) LIKE ? OR LOWER(primary_phone_number) LIKE ? OR LOWER(email_address) LIKE ?) ");
            String kw = "%" + search.trim().toLowerCase() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        if (year != null && !year.isBlank() && !"All Years".equalsIgnoreCase(year)) {
            sql.append(" AND LOWER(TRIM(current_year)) = LOWER(TRIM(?)) ");
            params.add(year.trim());
        }

        sql.append(" ORDER BY current_year ASC, roll_no ASC ");

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Student st = new Student(
                            rs.getInt("student_id"),
                            rs.getString("roll_no"),
                            rs.getString("full_name"),
                            rs.getString("dept_code"),
                            rs.getString("current_year"),
                            rs.getString("term"),
                            rs.getString("primary_phone_number"));
                    st.setEmail(rs.getString("email_address"));
                    list.add(st);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<String> getSubjectsForScope(boolean isAdmin, int deptId, int teacherUserId) {
        List<String> subjects = new ArrayList<>();
        String sql = isAdmin
                ? """
                  SELECT DISTINCT c.course_name
                  FROM course c
                  WHERE c.is_active = TRUE
                    AND c.dept_id = ?
                  ORDER BY c.course_name ASC
                  """
                : """
                  SELECT DISTINCT c.course_name
                  FROM course c
                  JOIN course_offering co ON c.course_id = co.course_id
                  JOIN teacher t ON co.teacher_id = t.teacher_id
                  WHERE c.is_active = TRUE
                    AND t.user_id = ?
                  ORDER BY c.course_name ASC
                  """;

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, isAdmin ? deptId : teacherUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    subjects.add(rs.getString("course_name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return subjects;
    }

    public StudentTabMetrics getStudentTabMetrics(boolean isAdmin, int deptId, int teacherUserId) {
        String sql = isAdmin
                ? """
                  WITH StudentStats AS (
                      SELECT
                          s.student_id,
                          COALESCE(s.gpa, 0.00) AS gpa,
                          EXISTS (
                              SELECT 1 FROM course_enrollment ce
                              WHERE ce.student_id = s.student_id AND ce.status = 'ENROLLED'
                          ) AS is_enrolled,
                          (
                              SELECT COUNT(cs.session_id)
                              FROM class_session cs
                              JOIN course_enrollment ce ON cs.offering_id = ce.offering_id
                              WHERE ce.student_id = s.student_id
                                AND ce.status = 'ENROLLED'
                                AND cs.is_cancelled = FALSE
                                AND cs.session_date <= CURDATE()
                          ) AS total_sessions,
                          (
                              SELECT COUNT(a.attendance_id)
                              FROM attendance a
                              JOIN course_enrollment ce ON a.enrollment_id = ce.enrollment_id
                              JOIN class_session cs ON a.session_id = cs.session_id
                              WHERE ce.student_id = s.student_id
                                AND ce.status = 'ENROLLED'
                                AND cs.is_cancelled = FALSE
                                AND cs.session_date <= CURDATE()
                          ) AS total_absent
                      FROM student s
                      JOIN user_account ua ON s.user_id = ua.user_id
                      WHERE ua.status = TRUE
                        AND s.dept_id = ?
                  )
                  SELECT
                      COUNT(*) AS total_students,
                      SUM(CASE WHEN is_enrolled THEN 1 ELSE 0 END) AS enrolled_students,
                      SUM(CASE WHEN total_sessions > 0
                                AND ((total_sessions - total_absent) * 100.0 / total_sessions) < 75.0
                               THEN 1 ELSE 0 END) AS at_risk_students,
                      COALESCE(AVG(gpa), 0.00) AS avg_gpa
                  FROM StudentStats
                  """
                : """
                  WITH StudentStats AS (
                      SELECT
                          s.student_id,
                          COALESCE(s.gpa, 0.00) AS gpa,
                          COUNT(DISTINCT cs.session_id) AS total_sessions,
                          COUNT(DISTINCT a.attendance_id) AS total_absent
                      FROM student s
                      JOIN user_account ua ON s.user_id = ua.user_id
                      JOIN course_enrollment ce ON s.student_id = ce.student_id AND ce.status = 'ENROLLED'
                      JOIN course_offering co ON ce.offering_id = co.offering_id
                      JOIN teacher t ON co.teacher_id = t.teacher_id
                      LEFT JOIN class_session cs ON cs.offering_id = co.offering_id
                           AND cs.is_cancelled = FALSE
                           AND cs.session_date <= CURDATE()
                      LEFT JOIN attendance a ON a.session_id = cs.session_id
                           AND a.enrollment_id = ce.enrollment_id
                      WHERE ua.status = TRUE
                        AND t.user_id = ?
                      GROUP BY s.student_id, s.gpa
                  )
                  SELECT
                      COUNT(*) AS total_students,
                      COUNT(*) AS enrolled_students,
                      SUM(CASE WHEN total_sessions > 0
                                AND ((total_sessions - total_absent) * 100.0 / total_sessions) < 75.0
                               THEN 1 ELSE 0 END) AS at_risk_students,
                      COALESCE(AVG(gpa), 0.00) AS avg_gpa
                  FROM StudentStats
                  """;

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, isAdmin ? deptId : teacherUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new StudentTabMetrics(
                            rs.getInt("total_students"),
                            rs.getInt("enrolled_students"),
                            rs.getInt("at_risk_students"),
                            rs.getDouble("avg_gpa"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new StudentTabMetrics(0, 0, 0, 0.0);
    }

    // =========================================================================
    // STANDARD STUDENT CRUD & DETAIL QUERIES
    // =========================================================================

    public List<Student> getAllStudent() {
        List<Student> students = new ArrayList<>();
        String sql = """
                WITH RankedStudents AS (
                    SELECT s.student_id, s.roll_no, s.full_name, d.dept_code, s.current_year, COALESCE(co.term, 'Pending') AS term,
                           ci.primary_phone_number, ua.status, ROW_NUMBER() OVER (
                               PARTITION BY s.student_id
                               ORDER BY ce.enrollment_id DESC
                           ) AS rn
                    FROM student s
                    JOIN user_account ua ON s.user_id = ua.user_id
                    JOIN department d ON s.dept_id = d.dept_id
                    LEFT JOIN student_contact_information ci ON s.student_id = ci.student_id
                    LEFT JOIN course_enrollment ce ON s.student_id = ce.student_id
                    LEFT JOIN course_offering co ON ce.offering_id = co.offering_id
                )
                SELECT student_id, roll_no, full_name, dept_code, current_year, term, primary_phone_number
                FROM RankedStudents
                WHERE rn = 1 AND `status` = TRUE;
                """;

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                students.add(new Student(
                        rs.getInt("student_id"),
                        rs.getString("roll_no"),
                        rs.getString("full_name"),
                        rs.getString("dept_code"),
                        rs.getString("current_year"),
                        rs.getString("term"),
                        rs.getString("primary_phone_number")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }

    public List<Student> getDeletedStudents() {
        List<Student> students = new ArrayList<>();
        String sql = """
                WITH RankedStudents AS (
                    SELECT s.student_id, s.roll_no, s.full_name, d.dept_code, s.current_year, COALESCE(co.term, 'Pending') AS term,
                           ci.primary_phone_number, ua.status, ROW_NUMBER() OVER (
                               PARTITION BY s.student_id
                               ORDER BY ce.enrollment_id DESC
                           ) AS rn
                    FROM student s
                    JOIN user_account ua ON s.user_id = ua.user_id
                    JOIN department d ON s.dept_id = d.dept_id
                    LEFT JOIN student_contact_information ci ON s.student_id = ci.student_id
                    LEFT JOIN course_enrollment ce ON s.student_id = ce.student_id
                    LEFT JOIN course_offering co ON ce.offering_id = co.offering_id
                )
                SELECT student_id, roll_no, full_name, dept_code, current_year, term, primary_phone_number
                FROM RankedStudents
                WHERE rn = 1 AND `status` = FALSE;
                """;

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                students.add(new Student(
                        rs.getInt("student_id"),
                        rs.getString("roll_no"),
                        rs.getString("full_name"),
                        rs.getString("dept_code"),
                        rs.getString("current_year"),
                        rs.getString("term"),
                        rs.getString("primary_phone_number")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }

    public List<Student> getFilteredStudents(String searchText, String department, String currentYear, String semester) {
        StringBuilder sql = new StringBuilder("""
                WITH RankedStudents AS (
                    SELECT s.student_id, s.roll_no, s.full_name, d.dept_code, s.current_year, COALESCE(co.term, 'Pending') AS term,
                           ci.primary_phone_number, ua.status, ROW_NUMBER() OVER (
                               PARTITION BY s.student_id
                               ORDER BY ce.enrollment_id DESC
                           ) AS rn
                    FROM student s
                    JOIN user_account ua ON s.user_id = ua.user_id
                    JOIN department d ON s.dept_id = d.dept_id
                    LEFT JOIN student_contact_information ci ON s.student_id = ci.student_id
                    LEFT JOIN course_enrollment ce ON s.student_id = ce.student_id
                    LEFT JOIN course_offering co ON ce.offering_id = co.offering_id
                )
                SELECT student_id, roll_no, full_name, dept_code, current_year, term, primary_phone_number
                FROM RankedStudents
                WHERE rn = 1 AND `status` = TRUE
                """);

        List<Object> parameters = new ArrayList<>();
        if (searchText != null && !searchText.trim().isEmpty()) {
            sql.append(" AND (full_name LIKE ? OR roll_no LIKE ? OR primary_phone_number LIKE ?) ");
            String kw = "%" + searchText.toLowerCase().trim() + "%";
            parameters.add(kw);
            parameters.add(kw);
            parameters.add(kw);
        }
        if (department != null && !department.equalsIgnoreCase("All Majors")) {
            sql.append(" AND (dept_code = ?) ");
            parameters.add(department);
        }
        if (currentYear != null && !currentYear.equalsIgnoreCase("All Years")) {
            sql.append(" AND (current_year = ?) ");
            parameters.add(currentYear);
        }
        if (semester != null && !semester.equalsIgnoreCase("All Semesters")) {
            sql.append(" AND (term = ?) ");
            parameters.add(semester);
        }

        List<Student> students = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            bindParams(pstmt, parameters);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    students.add(new Student(
                            rs.getInt("student_id"),
                            rs.getString("roll_no"),
                            rs.getString("full_name"),
                            rs.getString("dept_code"),
                            rs.getString("current_year"),
                            rs.getString("term"),
                            rs.getString("primary_phone_number")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }

    public Student getStudentbyId(int studentId) {
        String sql = """
                WITH RankedStudent AS (
                    SELECT
                        s.student_id,
                        s.roll_no,
                        s.full_name,
                        d.dept_code AS faculty,
                        s.current_year,
                        COALESCE(co.term, 'Pending') AS term,
                        s.date_of_birth,
                        s.gender,
                        s.nationality,
                        s.nrc,
                        s.profile_image_url,
                        ci.primary_phone_number,
                        COALESCE(ci.email_address, ua.email) AS email_address,
                        ci.current_address,
                        ci.permanent_address,
                        sg.father_full_name AS father_name,
                        sg.father_occupation AS father_occupation,
                        sg.father_phone_number AS father_phone,
                        sg.father_email_address AS father_email,
                        sg.mother_full_name AS mother_name,
                        sg.mother_occupation AS mother_occupation,
                        sg.mother_phone_number AS mother_phone,
                        sg.mother_email_address AS mother_email,
                        seb.previous_school,
                        seb.year_completed,
                        seb.additional_info,
                        ROW_NUMBER() OVER (ORDER BY ce.enrollment_id DESC) AS rn
                    FROM student s
                    JOIN user_account ua ON s.user_id = ua.user_id
                    LEFT JOIN department d ON s.dept_id = d.dept_id
                    LEFT JOIN student_contact_information ci ON s.student_id = ci.student_id
                    LEFT JOIN student_guardian sg ON s.student_id = sg.student_id
                    LEFT JOIN student_education_background seb ON s.student_id = seb.student_id
                    LEFT JOIN course_enrollment ce ON s.student_id = ce.student_id
                    LEFT JOIN course_offering co ON ce.offering_id = co.offering_id
                    WHERE s.student_id = ?
                )
                SELECT * FROM RankedStudent WHERE rn = 1;
                """;
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    java.sql.Date sqlDate = rs.getDate("date_of_birth");
                    String dobString = (sqlDate != null) ? sqlDate.toString() : "";
                    Object yearCompObj = rs.getObject("year_completed");
                    String yearCompStr = (yearCompObj != null) ? String.valueOf(yearCompObj) : "";

                    return new Student(
                            rs.getInt("student_id"),
                            rs.getString("roll_no"),
                            rs.getString("full_name"),
                            rs.getString("faculty"),
                            rs.getString("current_year"),
                            rs.getString("term"),
                            rs.getString("primary_phone_number"),
                            dobString,
                            rs.getString("gender"),
                            rs.getString("nationality"),
                            rs.getString("nrc"),
                            rs.getString("email_address"),
                            rs.getString("current_address"),
                            rs.getString("permanent_address"),
                            rs.getString("father_name"),
                            rs.getString("father_occupation"),
                            rs.getString("father_phone"),
                            rs.getString("father_email"),
                            rs.getString("mother_name"),
                            rs.getString("mother_occupation"),
                            rs.getString("mother_phone"),
                            rs.getString("mother_email"),
                            rs.getString("previous_school"),
                            yearCompStr,
                            rs.getString("additional_info"),
                            rs.getString("profile_image_url"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int insertStudent(Student student) {
        Connection conn = null;
        try {
            conn = DatabaseConnector.getConnection();
            conn.setAutoCommit(false);

            int studentId = insertSingleStudent(conn, student, "Default123");
            conn.commit();
            return studentId;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return -1;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    public int insertBatchStudents(List<Student> students) {
        int successCount = 0;
        List<String> duplicateDetails = new ArrayList<>();
        lastBatchError = "";

        for (Student student : students) {
            try (Connection conn = DatabaseConnector.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    int studentId = insertSingleStudent(conn, student, "Default123");
                    if (studentId > -1) {
                        conn.commit();
                        successCount++;
                    } else {
                        conn.rollback();
                    }
                } catch (java.sql.SQLIntegrityConstraintViolationException dupEx) {
                    conn.rollback();
                    String msg = dupEx.getMessage() != null ? dupEx.getMessage() : "Duplicate record";
                    if (msg.contains("student.nrc")) {
                        duplicateDetails.add("• " + student.getName() + " (" + student.getRollNo() + "): Duplicate NRC '" + student.getNrc() + "'");
                    } else if (msg.contains("student.roll_no") || msg.contains("user_account.username")) {
                        duplicateDetails.add("• " + student.getName() + ": Duplicate Roll No '" + student.getRollNo() + "'");
                    } else if (msg.contains("email")) {
                        duplicateDetails.add("• " + student.getName() + " (" + student.getRollNo() + "): Duplicate Email '" + student.getEmail() + "'");
                    } else {
                        duplicateDetails.add("• " + student.getName() + " (" + student.getRollNo() + "): " + msg);
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    lastBatchError = e.getMessage();
                }
            } catch (SQLException connEx) {
                lastBatchError = connEx.getMessage();
            }
        }

        if (!duplicateDetails.isEmpty()) {
            lastBatchError = String.join("\n", duplicateDetails);
        }
        return successCount;
    }

    private int insertSingleStudent(Connection conn, Student student, String password) throws SQLException {
        String userCreateSql = """
                INSERT INTO user_account (username, email, password_hash, user_role)
                VALUES (?, ?, ?, 'STUDENT');
                """;
        String studentCreateSql = """
                INSERT INTO student (
                    user_id, roll_no, full_name, dept_id, current_year,
                    date_of_birth, gender, nationality, nrc, profile_image_url
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;
        String studentContactSql = """
                INSERT INTO student_contact_information (
                    student_id, primary_phone_number, secondary_phone_number,
                    email_address, current_address, permanent_address
                ) VALUES (?, ?, ?, ?, ?, ?);
                """;
        String studentGuardianSql = """
                INSERT INTO student_guardian (
                    student_id, father_full_name, father_occupation, father_phone_number, father_email_address,
                    mother_full_name, mother_occupation, mother_phone_number, mother_email_address
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;
        String studentEducationBackgroundSql = """
                INSERT INTO student_education_background (
                    student_id, previous_school, year_completed, additional_info
                ) VALUES (?, ?, ?, ?);
                """;

        String username = student.getRollNo();
        String email = student.getRollNo().toLowerCase().replace("-", "") + "@ytu.edu.mm";
        String hashedPassword = hashPassword(password);
        int userId = -1;

        try (PreparedStatement pstmtUser = conn.prepareStatement(userCreateSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmtUser.setString(1, username);
            pstmtUser.setString(2, email);
            pstmtUser.setString(3, hashedPassword);
            pstmtUser.executeUpdate();

            try (ResultSet generatedKeys = pstmtUser.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating user failed! No userID found.");
                }
            }
        }

        int studentId = -1;
        DepartmentDAO departmentDAO = new DepartmentDAO();
        int deptId = departmentDAO.getDeptIdByNameAndDept(conn, student.getMajor(), student.getMajor());

        try (PreparedStatement pstmtStudent = conn.prepareStatement(studentCreateSql, Statement.RETURN_GENERATED_KEYS)) {
            String rawDob = student.getDob();
            String formattedDob = rawDob;
            if (rawDob != null && rawDob.matches("\\d{2}-\\d{2}-\\d{4}")) {
                String[] parts = rawDob.split("-");
                formattedDob = parts[2] + "-" + parts[1] + "-" + parts[0];
            }

            pstmtStudent.setInt(1, userId);
            pstmtStudent.setString(2, student.getRollNo());
            pstmtStudent.setString(3, student.getName());
            pstmtStudent.setInt(4, deptId);
            pstmtStudent.setString(5, student.getYear());
            pstmtStudent.setString(6, formattedDob);
            pstmtStudent.setString(7, student.getGender());
            pstmtStudent.setString(8, student.getNationality());
            pstmtStudent.setString(9, student.getNrc());
            pstmtStudent.setString(10, student.getPhotoPath());
            pstmtStudent.executeUpdate();

            try (ResultSet generatedKeys = pstmtStudent.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    studentId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating student failed, no student ID obtained.");
                }
            }
        }

        try (PreparedStatement pstmtContact = conn.prepareStatement(studentContactSql)) {
            pstmtContact.setInt(1, studentId);
            pstmtContact.setString(2, student.getPhoneNo());
            pstmtContact.setString(3, null);
            pstmtContact.setString(4, student.getEmail());
            pstmtContact.setString(5, student.getCurrentAddress());
            pstmtContact.setString(6, student.getPermanentAddress());
            pstmtContact.executeUpdate();
        }

        try (PreparedStatement pstmtGuardian = conn.prepareStatement(studentGuardianSql)) {
            pstmtGuardian.setInt(1, studentId);
            pstmtGuardian.setString(2, student.getFatherName());
            pstmtGuardian.setString(3, student.getFatherOccupation());
            pstmtGuardian.setString(4, student.getFatherPhone());
            pstmtGuardian.setString(5, student.getFatherEmail());
            pstmtGuardian.setString(6, student.getMotherName());
            pstmtGuardian.setString(7, student.getMotherOccupation());
            pstmtGuardian.setString(8, student.getMotherPhone());
            pstmtGuardian.setString(9, student.getMotherEmail());
            pstmtGuardian.executeUpdate();
        }

        try (PreparedStatement pstmtBackground = conn.prepareStatement(studentEducationBackgroundSql)) {
            pstmtBackground.setInt(1, studentId);
            pstmtBackground.setString(2, student.getPrevSchool());
            pstmtBackground.setString(3, student.getYearCompleted());
            pstmtBackground.setString(4, student.getAdditionalInfo());
            pstmtBackground.executeUpdate();
        }

        String targetTerm = (student.getSemester() != null && !student.getSemester().trim().isEmpty()
                && !student.getSemester().equalsIgnoreCase("Pending"))
                ? student.getSemester().trim()
                : "First Semester";

        String autoEnrollSql = """
                INSERT IGNORE INTO course_enrollment (offering_id, student_id, enrollment_date, status)
                SELECT co.offering_id, ?, CURDATE(), 'ENROLLED'
                FROM course_offering co
                JOIN course c ON co.course_id = c.course_id
                WHERE c.dept_id = ?
                  AND co.term = ?
                """;

        try (PreparedStatement pstmtEnroll = conn.prepareStatement(autoEnrollSql)) {
            pstmtEnroll.setInt(1, studentId);
            pstmtEnroll.setInt(2, deptId);
            pstmtEnroll.setString(3, targetTerm);
            pstmtEnroll.executeUpdate();
        }

        return studentId;
    }

    public boolean modifyStudent(Student student) {
        Connection conn = null;
        String studentSql = """
                UPDATE student
                SET roll_no = ?, full_name = ?, dept_id = ?, current_year = ?,
                    date_of_birth = ?, gender = ?, nationality = ?, nrc = ?, profile_image_url = ?
                WHERE student_id = ?
                """;
        String studentContactSql = """
                UPDATE student_contact_information
                SET primary_phone_number = ?, email_address = ?, current_address = ?, permanent_address = ?
                WHERE student_id = ?
                """;
        String studentGuardianSql = """
                UPDATE student_guardian
                SET father_full_name = ?, father_occupation = ?, father_phone_number = ?, father_email_address = ?,
                    mother_full_name = ?, mother_occupation = ?, mother_phone_number = ?, mother_email_address = ?
                WHERE student_id = ?
                """;
        String studentEducationSql = """
                UPDATE student_education_background
                SET previous_school = ?, year_completed = ?, additional_info = ?
                WHERE student_id = ?
                """;
        try {
            conn = DatabaseConnector.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtStudent = conn.prepareStatement(studentSql)) {
                DepartmentDAO departmentDAO = new DepartmentDAO();
                int deptId = departmentDAO.getDeptIdByNameAndDept(conn, student.getMajor(), student.getMajor());

                String rawDob = student.getDob();
                String formattedDob = rawDob;
                if (rawDob != null && rawDob.matches("\\d{2}-\\d{2}-\\d{4}")) {
                    String[] parts = rawDob.split("-");
                    formattedDob = parts[2] + "-" + parts[1] + "-" + parts[0];
                }
                pstmtStudent.setString(1, student.getRollNo());
                pstmtStudent.setString(2, student.getName());
                pstmtStudent.setInt(3, deptId);
                pstmtStudent.setString(4, student.getYear());
                pstmtStudent.setString(5, formattedDob);
                pstmtStudent.setString(6, student.getGender());
                pstmtStudent.setString(7, student.getNationality());
                pstmtStudent.setString(8, student.getNrc());
                pstmtStudent.setString(9, student.getPhotoPath());
                pstmtStudent.setInt(10, student.getNo());
                pstmtStudent.executeUpdate();
            }
            try (PreparedStatement pstmtContact = conn.prepareStatement(studentContactSql)) {
                pstmtContact.setString(1, student.getPhoneNo());
                pstmtContact.setString(2, student.getEmail());
                pstmtContact.setString(3, student.getCurrentAddress());
                pstmtContact.setString(4, student.getPermanentAddress());
                pstmtContact.setInt(5, student.getNo());
                pstmtContact.executeUpdate();
            }
            try (PreparedStatement pstmtGuardian = conn.prepareStatement(studentGuardianSql)) {
                pstmtGuardian.setString(1, student.getFatherName());
                pstmtGuardian.setString(2, student.getFatherOccupation());
                pstmtGuardian.setString(3, student.getFatherPhone());
                pstmtGuardian.setString(4, student.getFatherEmail());
                pstmtGuardian.setString(5, student.getMotherName());
                pstmtGuardian.setString(6, student.getMotherOccupation());
                pstmtGuardian.setString(7, student.getMotherPhone());
                pstmtGuardian.setString(8, student.getMotherEmail());
                pstmtGuardian.setInt(9, student.getNo());
                pstmtGuardian.executeUpdate();
            }
            try (PreparedStatement pstmtEducation = conn.prepareStatement(studentEducationSql)) {
                pstmtEducation.setString(1, student.getPrevSchool());
                pstmtEducation.setString(2, student.getYearCompleted());
                pstmtEducation.setString(3, student.getAdditionalInfo());
                pstmtEducation.setInt(4, student.getNo());
                pstmtEducation.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    public boolean removeStudent(int studentId) {
        String sql = """
                UPDATE user_account
                SET `status` = FALSE
                WHERE user_id = (SELECT user_id FROM student WHERE student_id = ?);
                """;
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeStudents(List<Integer> studentIds) {
        return updateAccountStatusBatch(studentIds, false);
    }

    public boolean restoreStudents(List<Integer> studentIds) {
        return updateAccountStatusBatch(studentIds, true);
    }

    private boolean updateAccountStatusBatch(List<Integer> studentIds, boolean active) {
        if (studentIds == null || studentIds.isEmpty()) return false;

        String sql = "UPDATE user_account SET `status` = ? WHERE user_id IN (SELECT user_id FROM student WHERE student_id = ?);";
        try (Connection conn = DatabaseConnector.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Integer id : studentIds) {
                    pstmt.setBoolean(1, active);
                    pstmt.setInt(2, id);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return rawPassword;
        }
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }
}