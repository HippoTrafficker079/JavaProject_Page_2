package application.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LecturerDAO {
    public List<String> getEligibleLecturerNamesByDept(int deptId) throws SQLException {
        List<String> lecturerNames = new ArrayList<>();
        String sql = """
                SELECT DISTINCT t.full_name
                FROM teacher t
                JOIN user_account ua ON t.user_id = ua.user_id
                WHERE ua.status = TRUE
                  AND ua.user_role IN ('TEACHER', 'ADMIN')
                  AND t.dept_id = ?
                ORDER BY t.full_name ASC
                """;

        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, deptId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lecturerNames.add(rs.getString("full_name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }

        return lecturerNames;
    }

    public int getLecturerTeacherIdByNameAndDept(Connection conn, String lecturerName, int deptId) throws SQLException {
        String sql = """
                SELECT t.teacher_id
                FROM teacher t
                JOIN user_account ua ON t.user_id = ua.user_id
                WHERE ua.status = TRUE
                AND ua.user_role IN('TEACHER', 'ADMIN')
                AND t.full_name = ?
                AND t.dept_id = ?
                LIMIT 1;
                """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, lecturerName);
            pstmt.setInt(2, deptId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("teacher_id");
                } else {
                    throw new SQLException("Teacher not found for input: " + lecturerName);
                }
            }
        }
    }
}
