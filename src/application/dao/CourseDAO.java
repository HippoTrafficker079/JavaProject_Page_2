package application.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import application.CourseCatalogStore;
import application.CourseCatalogStore.CourseItem;

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

    public int getCourseIdByCodeAndDept(Connection conn, String courseCode, int deptId) throws SQLException {
        String sql = """
                SELECT course_id
                FROM course
                WHERE course_code = ?
                  AND dept_id = ?
                  AND is_active = TRUE
                LIMIT 1;
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseCode);
            pstmt.setInt(2, deptId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("course_id");
                } else {
                    throw new SQLException("Active course not found: " + courseCode + " in dept_id=" + deptId);
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
            boolean isActive,
            List<Integer> prerequisiteCourseIds) throws SQLException {
        String insertCourseSql = """
                INSERT INTO course (
                    course_code,
                    course_name,
                    dept_id,
                    course_type,
                    credit,
                    is_active
                )
                VALUES (?, ?, ?, ?, ?, ?);
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
                    pstmtCourse.setBoolean(6, isActive);
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

    public List<CourseItem> findCatalogCoursesByDeptId(int deptId) throws SQLException {
        String sql = """
                SELECT
                    c.course_id,
                    c.course_code,
                    c.course_name,
                    d.dept_code,
                    c.course_type,
                    c.credit,
                    c.is_active,
                    GROUP_CONCAT(DISTINCT prereq.course_code ORDER BY prereq.course_code SEPARATOR ',') AS prereq_csv
                FROM course c
                LEFT JOIN department d
                    ON c.dept_id = d.dept_id
                LEFT JOIN prerequisite_course pc
                    ON c.course_id = pc.course_id
                LEFT JOIN course prereq
                    ON pc.prerequisite_course_id = prereq.course_id
                WHERE c.dept_id = ?
                GROUP BY
                    c.course_id,
                    c.course_code,
                    c.course_name,
                    d.dept_code,
                    c.course_type,
                    c.credit,
                    c.is_active
                ORDER BY c.course_code ASC
                """;

        List<CourseItem> result = new ArrayList<>();

        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, deptId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    List<String> prereqList = new ArrayList<>();
                    String csv = rs.getString("prereq_csv");
                    if (csv != null && !csv.isBlank()) {
                        for (String code : csv.split(",")) {
                            String trimmed = code.trim();
                            if (!trimmed.isEmpty()) {
                                prereqList.add(trimmed);
                            }
                        }
                    }

                    CourseItem item = new CourseItem(
                            rs.getInt("course_id"),
                            rs.getString("course_code"),
                            rs.getString("course_name"),
                            rs.getString("dept_code"),
                            rs.getString("course_type"),
                            rs.getDouble("credit"),
                            rs.getBoolean("is_active"),
                            prereqList);
                    result.add(item);
                }
            }
        }

        return result;
    }
}
