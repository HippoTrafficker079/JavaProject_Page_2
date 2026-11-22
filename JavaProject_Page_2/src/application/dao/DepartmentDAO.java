package application.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DepartmentDAO {
    public int getDeptIdForAdmin(Connection conn, int deptHeadId) throws SQLException {
        String sql = """
                SELECT dept_id
                FROM department
                WHERE dept_head_id = ?
                LIMIT 1;
                    """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, deptHeadId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("dept_id");
                } else {
                    throw new SQLException("Department not found for input: " + deptHeadId);
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
}
