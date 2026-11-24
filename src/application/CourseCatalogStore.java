package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import application.dao.CourseDAO;
import application.dao.DatabaseConnector;
import application.dao.DepartmentDAO;

public class CourseCatalogStore {

    public static class CourseItem {
        public final int courseId;
        public final String courseCode;
        public final String courseName;
        public final String deptCode; // Maps to dept_id in MySQL
        public final String courseType; // ENUM('Bachelor', 'Master', 'PHD')
        public final double credit; // DECIMAL(3, 1)
        public final boolean isActive; // BOOLEAN
        public final List<String> prerequisites; // List of prerequisite course_codes

        public CourseItem(int courseId, String courseCode, String courseName, String deptCode,
                String courseType, double credit, boolean isActive, List<String> prerequisites) {
            this.courseId = courseId;
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.deptCode = deptCode;
            this.courseType = courseType;
            this.credit = credit;
            this.isActive = isActive;
            this.prerequisites = prerequisites != null ? new ArrayList<>(prerequisites) : new ArrayList<>();
        }

        public String getDisplayLabel() {
            return courseCode + " - " + courseName;
        }

        public String getPrerequisitesDisplay() {
            return prerequisites.isEmpty() ? "None" : String.join(", ", prerequisites);
        }
    }

    public static final ObservableList<CourseItem> COURSES = FXCollections.observableArrayList();
    static {
        reload();
    }

    public static void reload() {
        try (Connection conn = DatabaseConnector.getConnection()) {
            CourseDAO courseDAO = new CourseDAO();
            DepartmentDAO departmentDAO = new DepartmentDAO();
            int deptId = departmentDAO.getDeptIdForAdmin(conn, UserSession.userId);
            COURSES.setAll(courseDAO.findCatalogCoursesByDeptId(deptId));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean existsByCode(String code) {
        if (code == null)
            return false;
        return COURSES.stream().anyMatch(c -> c.courseCode.equalsIgnoreCase(code.trim()));
    }

    public static List<String> getActiveCourseLabels() {
        return COURSES.stream()
                .filter(c -> c.isActive)
                .map(CourseItem::getDisplayLabel)
                .collect(Collectors.toList());
    }

    public static List<String> getAllCourseLabels() {
        return COURSES.stream()
                .map(CourseItem::getDisplayLabel)
                .collect(Collectors.toList());
    }

    public static CourseItem findByLabelOrCode(String input) {
        if (input == null || input.isBlank())
            return null;
        String q = input.trim();
        return COURSES.stream()
                .filter(c -> c.getDisplayLabel().equalsIgnoreCase(q)
                        || c.courseCode.equalsIgnoreCase(q)
                        || c.courseName.equalsIgnoreCase(q))
                .findFirst()
                .orElse(null);
    }
}