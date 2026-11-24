package application.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DepartmentDAO {
    public int getDeptIdForAdmin(Connection conn, int userId) throws SQLException {
        String sql = """
                SELECT d.dept_id
                FROM teacher t
                JOIN user_account ua ON t.user_id = ua.user_id
                JOIN department d ON t.teacher_id = d.dept_head_id
                WHERE
                ua.user_role IN ('ADMIN', 'TEACHER')
                AND ua.status = TRUE
                AND t.user_id = ?;
                    """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("dept_id");
                } else {
                    throw new SQLException("Department not found for input: " + userId);
                }
            }
        }
    }

    public int getDeptIdForTeacher(Connection conn, int teacherUserId) throws SQLException {
        String sql = """
                SELECT t.dept_id
                FROM teacher t
                JOIN user_account ua ON t.user_id = ua.user_id
                WHERE
                ua.user_role = 'TEACHER'
                AND ua.status = TRUE
                AND t.user_id = ?;
                    """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, teacherUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("dept_id");
                } else {
                    throw new SQLException("Department not found for input: " + teacherUserId);
                }
            }
        }
    }

    public int getDeptIdByUserId(int userId) throws SQLException {
        String roleSql = "SELECT user_role FROM user_account WHERE user_id = ?";

        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement stmt = conn.prepareStatement(roleSql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("user_role");
                    if ("ADMIN".equalsIgnoreCase(role)) {
                        return getDeptIdForAdmin(conn, userId);
                    }
                    if ("TEACHER".equalsIgnoreCase(role)) {
                        return getDeptIdForTeacher(conn, userId);
                    }
                }
            }
        }

        throw new SQLException("Could not resolve dept_id for user_id: " + userId);
    }

    public int getDeptIdByNameAndDept(Connection conn, String deptName, String deptCode) throws SQLException {
        String sql = """
                SELECT dept_id
                    FROM department
                    WHERE (dept_name = ? OR dept_code = ?)
                    LIMIT 1;
                """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, deptName);
            pstmt.setString(2, deptCode);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("dept_id");
                } else {
                    throw new SQLException("Department not found: " + deptName + " in dept_id =" + deptCode);
                }
            }
        }
    }

    public int getDeptIdByTeacherName(Connection conn, String teacherName) throws SQLException {
        String sql = """
                SELECT dept_id
                    FROM teacher
                    WHERE (full_name = ?)
                    LIMIT 1;
                """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, teacherName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("dept_id");
                } else {
                    throw new SQLException("Teacher not found: " + teacherName);
                }
            }
        }
    }
}
