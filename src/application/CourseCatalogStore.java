package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CourseCatalogStore {

    public static class CourseItem {
        public final int courseId;
        public final String courseCode;
        public final String courseName;
        public final String deptCode;      // Maps to dept_id in MySQL
        public final String courseType;    // ENUM('Bachelor', 'Master', 'PHD')
        public final double credit;        // DECIMAL(3, 1)
        public final boolean isActive;     // BOOLEAN
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
    private static int nextCourseId = 100;

    static {
        // =========================================================================
        // TODO (DB Integration — MySQL):
        // Load courses from `course` JOIN `department` and prerequisites from
        // `prerequisite_course`:
        //   SELECT c.course_id, c.course_code, c.course_name, d.dept_code,
        //          c.course_type, c.credit, c.is_active
        //   FROM course c JOIN department d ON c.dept_id = d.dept_id;
        // =========================================================================
        COURSES.add(new CourseItem(1,  "EM-1001",   "Engineering Mathematics I",          "CEIT", "Bachelor", 3.0, true, List.of()));
        COURSES.add(new CourseItem(2,  "CEIT-2101", "Data Structures and Algorithms",     "CEIT", "Bachelor", 3.0, true, List.of("EM-1001")));
        COURSES.add(new CourseItem(3,  "CEIT-3101", "Advanced Database Systems",          "CEIT", "Bachelor", 3.0, true, List.of("CEIT-2101")));
        COURSES.add(new CourseItem(4,  "CEIT-3102", "Object-Oriented Programming with Java", "CEIT", "Bachelor", 3.0, true, List.of("CEIT-2101")));
        COURSES.add(new CourseItem(5,  "CEIT-4103", "Artificial Intelligence & Machine Learning", "CEIT", "Bachelor", 3.5, true, List.of("CEIT-2101", "EM-1001")));
        COURSES.add(new CourseItem(6,  "ME-1001",   "Engineering Mechanics",              "Mech", "Bachelor", 3.0, true, List.of()));
        COURSES.add(new CourseItem(7,  "ME-2101",   "Thermodynamics",                     "Mech", "Bachelor", 3.0, true, List.of("ME-1001")));
        COURSES.add(new CourseItem(8,  "C-301",     "Structural Mechanics",               "C",    "Bachelor", 3.0, true, List.of("EM-1001")));
        COURSES.add(new CourseItem(9,  "EC-401",    "Digital Signal Processing",          "EC",   "Bachelor", 3.0, true, List.of("EM-1001")));
        COURSES.add(new CourseItem(10, "EP-302",    "Power Transmission Systems",         "EP",   "Bachelor", 3.0, true, List.of("EM-1001")));
    }

    public static CourseItem addCourse(String courseCode, String courseName, String deptCode,
                                       String courseType, double credit, boolean isActive,
                                       List<String> prerequisiteCodes) {
        CourseItem item = new CourseItem(
            nextCourseId++, courseCode, courseName, deptCode,
            courseType, credit, isActive, prerequisiteCodes
        );
        COURSES.add(0, item);

        // =========================================================================
        // TODO (DB Integration — MySQL):
        // 1) INSERT INTO course (course_code, course_name, dept_id, course_type, credit, is_active)
        //    VALUES (?, ?, (SELECT dept_id FROM department WHERE dept_code = ?), ?, ?, ?);
        // 2) For each prereqCode in prerequisiteCodes:
        //    INSERT INTO prerequisite_course (course_id, prerequisite_course_id)
        //    VALUES (generatedCourseId, (SELECT course_id FROM course WHERE course_code = ?));
        // =========================================================================
        return item;
    }

    public static boolean existsByCode(String code) {
        if (code == null) return false;
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
        if (input == null || input.isBlank()) return null;
        String q = input.trim();
        return COURSES.stream()
                .filter(c -> c.getDisplayLabel().equalsIgnoreCase(q)
                        || c.courseCode.equalsIgnoreCase(q)
                        || c.courseName.equalsIgnoreCase(q))
                .findFirst()
                .orElse(null);
    }
}