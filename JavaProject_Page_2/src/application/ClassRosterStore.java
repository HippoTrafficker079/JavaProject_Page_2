package application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassRosterStore {

    public static class RosterRecord {
        public final String id;
        public final String rollNo;
        public final String name;
        public final String email;
        public final int attendancePercent;
        public final double gpa;

        public RosterRecord(String id, String rollNo, String name, String email, int attendancePercent, double gpa) {
            this.id = id;
            this.rollNo = rollNo;
            this.name = name;
            this.email = email;
            this.attendancePercent = attendancePercent;
            this.gpa = gpa;
        }
    }

    public static class DailyLogRecord {
        public final int no;
        public final String rollNo;
        public final String studentName;
        public final String date;
        public final String subject;
        public final String status;
        public final String remarks;

        public DailyLogRecord(int no, String rollNo, String studentName, String date,
                              String subject, String status, String remarks) {
            this.no = no;
            this.rollNo = rollNo;
            this.studentName = studentName;
            this.date = date;
            this.subject = subject;
            this.status = status;
            this.remarks = remarks;
        }
    }

    private static final Map<String, List<RosterRecord>> ROSTER_BY_CLASS = new HashMap<>();
    private static final Map<String, List<DailyLogRecord>> DAILY_LOGS_BY_CLASS = new HashMap<>();

    /**
     * Called right after Admin clicks "Create & Publish Class".
     * Automatically assigns all students matching (major, academicYear, semester)
     * into this class's Student List and Daily Attendance Log.
     *
     * @return The number of students auto-assigned to this class.
     */
    public static int autoAssignStudentsForNewClass(
            String courseCode,
            String courseTitle,
            String major,
            String academicYear,
            String semester,
            LocalDate firstSessionDate) {

        List<RosterRecord> assignedStudents = new ArrayList<>();

        // =========================================================================
        // TODO (DB Integration — For Backend/Database Teammate):
        // 1. Insert the new course_offering into the database and retrieve offering_id.
        // 2. Query all students matching (major, academicYear, semester):
        //      SELECT student_id, roll_no, full_name, email, gpa
        //      FROM student
        //      WHERE major_code = ? AND academic_year = ? AND semester = ?;
        // 3. Bulk-insert matching students into course_enrollment (offering_id, student_id).
        // 4. Insert generated class_session rows and initialize default attendance rows.
        // Replace the mock auto-assignment block below with the real DB result list.
        // =========================================================================

        String yearPrefix = toRomanYear(academicYear);
        String deptPrefix = (major == null || major.isBlank()) ? "CEIT" : major.toUpperCase();
        String[][] mockNames = {
            {"Aung Thura", "aungthura@ytu.edu.mm"},
            {"Su Myat Mya", "sumyatmya@ytu.edu.mm"},
            {"Min Khant", "minkhant@ytu.edu.mm"},
            {"Hnin Wai", "hninwai@ytu.edu.mm"},
            {"Ko Thant", "kothant@ytu.edu.mm"},
            {"Zaw Hein", "zawhein@ytu.edu.mm"},
            {"May Thu", "maythu@ytu.edu.mm"},
            {"Kyaw Zin", "kyawzin@ytu.edu.mm"}
        };
        double[] mockGpas = {3.6, 3.8, 2.5, 3.9, 2.1, 4.0, 3.2, 3.4};

        int baseId = 260001 + Math.abs(courseCode.hashCode() % 900);
        for (int i = 0; i < mockNames.length; i++) {
            String rollNo = yearPrefix + "-" + deptPrefix + "-" + (i + 1);
            assignedStudents.add(new RosterRecord(
                String.valueOf(baseId + i),
                rollNo,
                mockNames[i][0],
                mockNames[i][1],
                100, // Newly created class starts with 100% attendance
                mockGpas[i]
            ));
        }

        ROSTER_BY_CLASS.put(courseCode, assignedStudents);

        // Initialize Daily Attendance Log rows for all auto-assigned students
        List<DailyLogRecord> initialLogs = new ArrayList<>();
        String dateStr = (firstSessionDate != null) ? firstSessionDate.toString() : LocalDate.now().toString();
        for (int i = 0; i < assignedStudents.size(); i++) {
            RosterRecord s = assignedStudents.get(i);
            initialLogs.add(new DailyLogRecord(
                i + 1,
                s.rollNo,
                s.name,
                dateStr,
                courseTitle,
                "Present",
                "Auto-enrolled (" + semester + ")"
            ));
        }
        DAILY_LOGS_BY_CLASS.put(courseCode, initialLogs);

        return assignedStudents.size();
    }

    public static boolean hasRoster(String courseCode) {
        return ROSTER_BY_CLASS.containsKey(courseCode);
    }

    public static List<RosterRecord> getRoster(String courseCode) {
        // TODO (DB Integration): Query enrolled students for courseCode from DB if not cached.
        return ROSTER_BY_CLASS.getOrDefault(courseCode, new ArrayList<>());
    }

    public static List<DailyLogRecord> getDailyLogs(String courseCode) {
        // TODO (DB Integration): Query daily attendance logs for courseCode from DB if not cached.
        return DAILY_LOGS_BY_CLASS.getOrDefault(courseCode, new ArrayList<>());
    }

    private static String toRomanYear(String academicYear) {
        if (academicYear == null) return "II";
        return switch (academicYear) {
            case "First Year"  -> "I";
            case "Second Year" -> "II";
            case "Third Year"  -> "III";
            case "Fourth Year" -> "IV";
            case "Fifth Year"  -> "V";
            default            -> "II";
        };
    }
}