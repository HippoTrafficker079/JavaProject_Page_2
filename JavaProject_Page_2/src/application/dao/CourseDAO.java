package application.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CourseDAO {
    public List<String> getAllActiveCourseNameForAdmin(int deptHeadId) {
        List<String> courseList = new ArrayList<>();
        String sql = """
                    SELECT DISTINCT
                    c.course_name
                FROM course c
                JOIN department d ON c.dept_id = d.dept_id
                WHERE
                    c.is_active = TRUE
                    AND d.dept_head_id = ?
                ORDER BY c.course_name ASC;
                        """;
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);) {
            pstmt.setInt(1, deptHeadId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    courseList.add(rs.getString("course_name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courseList;
    }

    public int getCourseIdByNameAndDept(Connection conn, String courseName, int deptId) throws SQLException {
        String sql = """
                SELECT course_id
                    FROM course
                    WHERE course_name = ?
                      AND dept_id = ?
                      AND is_active = TRUE
                    LIMIT 1;
                """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseName);
            pstmt.setInt(2, deptId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("course_id");
                } else {
                    throw new SQLException("Active course not found: " + courseName + " in dept_id=" + deptId);
                }
            }
        }
    }

    public List<Integer> getCourseIdsByNamesAndDept(
            Connection conn,
            List<String> courseNames,
            int deptId) throws SQLException {

        List<Integer> courseIds = new ArrayList<>();
        if (courseNames == null || courseNames.isEmpty()) {
            return courseIds;
        }

        for (String courseName : courseNames) {
            if (courseName == null || courseName.isBlank()) {
                continue;
            }
            int courseId = getCourseIdByNameAndDept(conn, courseName, deptId);
            if (!courseIds.contains(courseId)) {
                courseIds.add(courseId);
            }
        }

        return courseIds;
    }

    public int createCourseWithPrerequisites(
            int deptId,
            String courseCode,
            String courseName,
            String courseType,
            double credit,
            List<Integer> prerequisiteCourseIds) throws SQLException {
        String insertCourseSql = """
                INSERT INTO course (
                    course_code,
                    course_name,
                    dept_id,
                    course_type,
                    credit
                )
                VALUES (?, ?, ?, ?, ?);
                """;

        String insertPrereqSql = """
                INSERT INTO prerequisite_course (
                    course_id,
                    prerequisite_course_id
                )
                VALUES (?, ?);
                """;
        try (Connection conn = DatabaseConnector.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int newCourseId;
                try (PreparedStatement pstmtCourse = conn.prepareStatement(insertCourseSql,
                        Statement.RETURN_GENERATED_KEYS)) {
                    pstmtCourse.setString(1, courseCode);
                    pstmtCourse.setString(2, courseName);
                    pstmtCourse.setInt(3, deptId);
                    pstmtCourse.setString(4, courseType);
                    pstmtCourse.setBigDecimal(5, BigDecimal.valueOf(credit));
                    pstmtCourse.executeUpdate();

                    try (ResultSet keys = pstmtCourse.getGeneratedKeys()) {
                        if (keys.next()) {
                            newCourseId = keys.getInt(1);
                        } else {
                            throw new SQLException("Failed to retrieve generated course_id.");
                        }
                    }
                }
                if (prerequisiteCourseIds != null && !prerequisiteCourseIds.isEmpty()) {
                    try (PreparedStatement prereqStmt = conn.prepareStatement(insertPrereqSql)) {
                        for (int prereqId : prerequisiteCourseIds) {
                            if (prereqId == newCourseId) {
                                throw new SQLException("A course cannot be a prerequisite of itself.");
                            }
                            prereqStmt.setInt(1, newCourseId);
                            prereqStmt.setInt(2, prereqId);
                            prereqStmt.addBatch();
                        }
                        prereqStmt.executeBatch();
                    }
                }
                conn.commit();
                return newCourseId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
