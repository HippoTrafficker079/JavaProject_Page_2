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

    public static String buildKey(String courseCode, String major, String academicYear, String semester) {
        String c = courseCode == null ? "" : courseCode.trim().toUpperCase();
        String m = major == null ? "" : major.trim().toUpperCase();
        String y = academicYear == null ? "" : academicYear.trim().toUpperCase();
        String s = semester == null ? "" : semester.trim().toUpperCase();
        return c + "|" + m + "|" + y + "|" + s;
    }

    /**
     * Called right after Admin clicks "Create & Publish Class".
     * Automatically assigns all students matching (major, academicYear, semester)
     * into this class offering's Student List and Daily Attendance Log.
     *
     * @return The number of students auto-assigned to this class offering.
     */

    public static boolean hasRoster(String key) {
        return key != null && ROSTER_BY_CLASS.containsKey(key.trim().toUpperCase());
    }

    public static List<RosterRecord> getRoster(String key) {
        // TODO (DB Integration): Query enrolled students for offering_id from DB if not
        // cached.
        if (key == null)
            return new ArrayList<>();
        return ROSTER_BY_CLASS.getOrDefault(key.trim().toUpperCase(), new ArrayList<>());
    }

    public static List<DailyLogRecord> getDailyLogs(String key) {
        // TODO (DB Integration): Query daily attendance logs for offering_id from DB if
        // not cached.
        if (key == null)
            return new ArrayList<>();
        return DAILY_LOGS_BY_CLASS.getOrDefault(key.trim().toUpperCase(), new ArrayList<>());
    }

    private static String toRomanYear(String academicYear) {
        if (academicYear == null)
            return "II";
        return switch (academicYear) {
            case "First Year" -> "I";
            case "Second Year" -> "II";
            case "Third Year" -> "III";
            case "Fourth Year" -> "IV";
            case "Fifth Year" -> "V";
            default -> "II";
        };
    }
}