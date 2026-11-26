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
    /**
     * Validates YTU course code format: [Prefix]-[5-Digit Number]
     * - Prefix: 1 to 6 letters (e.g., EM, ME, CEIT, ECh, Met)
     * - 1st digit: Year (1–5)
     * - 2nd digit: Semester (1–2)
     * - Last 3 digits: Course sequence (001–999)
     */
    private static final java.util.regex.Pattern YTU_COURSE_CODE_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z]{1,6}-[1-5][1-2]\\d{3}$");
    public static boolean isValidYtuCourseCode(String courseCode) {
    	
        if (courseCode == null) return false;
        String trimmed = courseCode.trim();
        return YTU_COURSE_CODE_PATTERN.matcher(trimmed).matches()
                && !trimmed.endsWith("000");
    }

    /**
     * Extracts Academic Year from the 1st digit after the hyphen (or 1st digit in code).
     * e.g. EM-11001 -> "First Year", ME-31016 -> "Third Year"
     */
    public static String extractAcademicYearFromCode(String courseCode) {
        String digits = extractNumericPart(courseCode);
        if (digits.isEmpty()) return null;
        return switch (digits.charAt(0)) {
            case '1' -> "First Year";
            case '2' -> "Second Year";
            case '3' -> "Third Year";
            case '4' -> "Fourth Year";
            case '5' -> "Fifth Year";
            default -> null;
        };
    }

    /**
     * Extracts Semester from the 2nd digit after the hyphen (or 2nd digit in code).
     * e.g. EM-11001 -> "First Semester", CEIT-22005 -> "Second Semester"
     */
    public static String extractSemesterFromCode(String courseCode) {
        String digits = extractNumericPart(courseCode);
        if (digits.length() < 2) return null;
        return switch (digits.charAt(1)) {
            case '1' -> "First Semester";
            case '2' -> "Second Semester";
            default -> null;
        };
    }

    private static String extractNumericPart(String courseCode) {
        if (courseCode == null || courseCode.isBlank()) return "";
        String part = courseCode.contains("-")
                ? courseCode.substring(courseCode.indexOf('-') + 1).trim()
                : courseCode.trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < part.length(); i++) {
            if (Character.isDigit(part.charAt(i))) {
                sb.append(part.charAt(i));
            }
        }
        return sb.toString();
    }

    /**
     * Filters active courses by Academic Year and/or Semester derived from the course code.
     */
    public static List<String> getActiveCourseLabelsByYearAndSemester(String academicYear, String semester) {
        return COURSES.stream()
                .filter(c -> c.isActive)
                .filter(c -> academicYear == null || academicYear.isBlank()
                        || academicYear.equalsIgnoreCase(extractAcademicYearFromCode(c.courseCode)))
                .filter(c -> semester == null || semester.isBlank()
                        || semester.equalsIgnoreCase(extractSemesterFromCode(c.courseCode)))
                .map(CourseItem::getDisplayLabel)
                .collect(Collectors.toList());
    }
 


    /**
     * Returns active course labels filtered by academic year (based on first digit of courseCode).
     * If academicYear is null or blank, returns all active courses.
     */
    public static List<String> getActiveCourseLabelsByYear(String academicYear) {
        if (academicYear == null || academicYear.isBlank()) {
            return getActiveCourseLabels();
        }
        return COURSES.stream()
                .filter(c -> c.isActive)
                .filter(c -> academicYear.equalsIgnoreCase(extractAcademicYearFromCode(c.courseCode)))
                .map(CourseItem::getDisplayLabel)
                .collect(Collectors.toList());
    }
    
    
}