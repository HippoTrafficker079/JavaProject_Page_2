package application;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TeacherDirectoryStore {

    public static class TeacherAccount {
        public final int teacherId;
        public final int userId;
        public final String fullName;
        public final String email;
        public final String password;
        public final String deptCode;
        public final String position;

        public TeacherAccount(int teacherId, int userId, String fullName, String email,
                              String password, String deptCode, String position) {
            this.teacherId = teacherId;
            this.userId = userId;
            this.fullName = fullName;
            this.email = email;
            this.password = password;
            this.deptCode = deptCode;
            this.position = position;
        }
    }

    private static final List<TeacherAccount> TEACHERS = new ArrayList<>();

    static {
        // =========================================================================
        // TODO (DB Integration — Matches schema.sql & seed.sql):
        // Load teachers, @ytu.edu.mm emails, and password_hash from `teacher` + `user_account`
        // + `department` using JDBC, and verify passwords via BCrypt in LoginController.
        // =========================================================================

        // Seeded Teachers from seed.sql (user_id 1..4) with @ytu.edu.mm emails & passwords
        TEACHERS.add(new TeacherAccount(1, 1, "Sadayo Kawakami", "t001@ytu.edu.mm", "ytu@ceit01", "CEIT", "Professor & Head"));
        TEACHERS.add(new TeacherAccount(2, 2, "Sojiro Sakura",   "t002@ytu.edu.mm", "ytu@ceit02", "CEIT", "Lecturer"));
        TEACHERS.add(new TeacherAccount(3, 3, "Tae Takemi",      "t003@ytu.edu.mm", "ytu@mech03", "Mech", "Professor"));
        TEACHERS.add(new TeacherAccount(4, 4, "Ichiko Ohya",     "t004@ytu.edu.mm", "ytu@ceit04", "CEIT", "Tutor"));

        // Additional Department Faculty Accounts with @ytu.edu.mm emails & passwords
        TEACHERS.add(new TeacherAccount(5, 101, "Dr. Hla Myint",     "hlamyint@ytu.edu.mm",     "ytu@ceit123", "CEIT", "Professor"));
        TEACHERS.add(new TeacherAccount(6, 102, "Daw Hnin Hnin",     "hninhnin@ytu.edu.mm",     "ytu@ceit234", "CEIT", "Lecturer"));
        TEACHERS.add(new TeacherAccount(7, 103, "Dr. Nyein Nyein Oo","nyeinnyeinoo@ytu.edu.mm", "ytu@ceit345", "CEIT", "Associate Professor"));
        TEACHERS.add(new TeacherAccount(8, 104, "Dr. Aung Naing",    "aungnaing@ytu.edu.mm",    "ytu@c123",    "C",    "Professor"));
        TEACHERS.add(new TeacherAccount(9, 105, "Dr. Su Su",         "susu@ytu.edu.mm",         "ytu@mech123", "Mech", "Lecturer"));
        TEACHERS.add(new TeacherAccount(10,106, "Prof. Kyaw Zin",    "kyawzin@ytu.edu.mm",      "ytu@ep123",   "EP",   "Professor"));
        TEACHERS.add(new TeacherAccount(11,107, "Dr. Tin Maung",     "tinmaung@ytu.edu.mm",     "ytu@ec123",   "EC",   "Professor"));
        TEACHERS.add(new TeacherAccount(12,108, "Dr. Myo Tint",      "myotint@ytu.edu.mm",      "ytu@mce123",  "McE",  "Lecturer"));
        TEACHERS.add(new TeacherAccount(13,109, "Dr. Khin Marlar",   "khinmarlar@ytu.edu.mm",   "ytu@che123",  "ChE",  "Professor"));
        TEACHERS.add(new TeacherAccount(14,110, "Daw Thiri Nandar",  "thirinandar@ytu.edu.mm",  "ytu@tex123",  "Tex",  "Lecturer"));
        TEACHERS.add(new TeacherAccount(15,111, "U Than Htike",      "thanhtike@ytu.edu.mm",    "ytu@mn123",   "Mn",   "Lecturer"));
        TEACHERS.add(new TeacherAccount(16,112, "Dr. Min Thu",       "minthu@ytu.edu.mm",       "ytu@pe123",   "PE",   "Professor"));
        TEACHERS.add(new TeacherAccount(17,113, "U Kyaw Swar",       "kyawswar@ytu.edu.mm",     "ytu@arch123", "Arch", "Professor"));
        TEACHERS.add(new TeacherAccount(18,114, "U Zaw Min",         "zawmin@ytu.edu.mm",       "ytu@tele123", "Tele", "Lecturer"));
        TEACHERS.add(new TeacherAccount(19,115, "Dr. Aye Aye Mon",   "ayeayemon@ytu.edu.mm",    "ytu@met123",  "Met",  "Professor"));
        TEACHERS.add(new TeacherAccount(20,116, "Daw Ei Ei Phyo",    "eieiphyo@ytu.edu.mm",     "ytu@fe123",   "FE",   "Lecturer"));
    }

    public static List<String> getTeacherNamesByDept(String deptCode) {
        if (deptCode == null || deptCode.isBlank()) {
            return TEACHERS.stream().map(t -> t.fullName).collect(Collectors.toList());
        }
        List<String> matches = TEACHERS.stream()
                .filter(t -> t.deptCode.equalsIgnoreCase(deptCode.trim()))
                .map(t -> t.fullName)
                .collect(Collectors.toList());
        return matches.isEmpty()
                ? TEACHERS.stream().map(t -> t.fullName).collect(Collectors.toList())
                : matches;
    }

    public static TeacherAccount findByCredentials(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) return null;
        String q = usernameOrEmail.trim().toLowerCase();
        return TEACHERS.stream()
                .filter(t -> t.fullName.toLowerCase().equals(q)
                        || t.fullName.toLowerCase().contains(q)
                        || t.email.toLowerCase().equals(q))
                .findFirst()
                .orElse(null);
    }
}