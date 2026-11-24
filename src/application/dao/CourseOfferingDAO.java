package application.dao;

//import java.lang.classfile.instruction.SwitchCase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import application.ClassesController.ClassItem;

public class CourseOfferingDAO {
    public record SessionInput(LocalDate sessionDate, int period) {
    }

    public int createCourseOffering(
            String lecturerName,
            int lecturerDeptId,
            List<Integer> targetDeptIds,
            int courseId,
            int createDeptId,
            String year,
            String term,
            LocalDate startDate,
            int maxStudent,
            int totalSessions,
            String cardSymbol,
            String badgeColor,
            List<SessionInput> sessions,
            BigDecimal tutorialWeight,
            BigDecimal assignmentWeight,
            BigDecimal labTestWeight,
            BigDecimal practicalWeight,
            BigDecimal finalExamWeight) throws SQLException {
        if (sessions == null || sessions.isEmpty()) {
            throw new IllegalArgumentException("At least one class session is required.");
        }
        if (sessions.size() != totalSessions) {
            throw new IllegalArgumentException(
                    "Session count (" + sessions.size() + ") does not match totalSessions (" + totalSessions + ").");
        }
        if (targetDeptIds == null || targetDeptIds.isEmpty()) {
            throw new IllegalArgumentException("At least one target department ID is required.");
        }
        for (Integer deptId : targetDeptIds) {
            if (deptId == null || deptId <= 0) {
                throw new IllegalArgumentException("Invalid target department ID: " + deptId);
            }
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required.");
        }
        if (maxStudent < 0 || totalSessions <= 0) {
            throw new IllegalArgumentException("maxStudent must be non-negative and totalSessions must be positive.");
        }
        // validateWeights(tutorialWeight, assignmentWeight, labTestWeight,
        // practicalWeight, finalExamWeight);

        String insertOfferingSql = """
                INSERT INTO course_offering (
                    teacher_id,
                    course_id,
                    create_dept_id,
                    academic_year,
                    `year`,
                    total_sessions,
                    term,
                    max_student,
                    card_symbol,
                    badge_color
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String insertSessionSql = """
                INSERT INTO class_session (
                    offering_id,
                    session_date,
                    period
                )
                VALUES (?, ?, ?)
                """;

        /*
         * String insertWeightSql = """
         * INSERT INTO assessment_weight (
         * offering_id,
         * tutorial_weight,
         * assignment_weight,
         * lab_test_weight,
         * practical_weight,
         * final_exam_weight
         * )
         * VALUES (?, ?, ?, ?, ?, ?)
         * """;
         */

        String placeholders = String.join(", ", java.util.Collections.nCopies(targetDeptIds.size(), "?"));

        String enrollStudentsSql = """
                INSERT IGNORE INTO course_enrollment (offering_id, student_id, enrollment_date)
                SELECT ?, s.student_id, ?
                FROM student s
                JOIN user_account ua ON s.user_id = ua.user_id
                WHERE ua.status = TRUE
                  AND s.current_year = ?
                  AND s.dept_id IN (%s)
                """.formatted(placeholders);

        try (Connection conn = DatabaseConnector.getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                LecturerDAO lecturerDAO = new LecturerDAO();
                int teacherId = lecturerDAO.getLecturerTeacherIdByNameAndDept(conn, lecturerName, lecturerDeptId);

                int offeringId;
                try (PreparedStatement offeringStmt = conn.prepareStatement(
                        insertOfferingSql, Statement.RETURN_GENERATED_KEYS)) {

                    int academicYear = (startDate != null) ? startDate.getYear() : LocalDate.now().getYear();

                    offeringStmt.setInt(1, teacherId);
                    offeringStmt.setInt(2, courseId);
                    offeringStmt.setInt(3, createDeptId); // Department that created the offering

                    offeringStmt.setInt(4, academicYear);
                    offeringStmt.setString(5, year);
                    offeringStmt.setInt(6, totalSessions);
                    offeringStmt.setString(7, term);
                    offeringStmt.setInt(8, maxStudent);
                    offeringStmt.setString(9, cardSymbol);
                    offeringStmt.setString(10, badgeColor);

                    if (offeringStmt.executeUpdate() == 0) {
                        throw new SQLException("Creating course_offering failed, no rows affected.");
                    }

                    try (ResultSet keys = offeringStmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            offeringId = keys.getInt(1);
                        } else {
                            throw new SQLException("Creating course_offering failed, no generated ID returned.");
                        }
                    }
                }

                try (PreparedStatement sessionStmt = conn.prepareStatement(insertSessionSql)) {
                    for (SessionInput session : sessions) {
                        if (session.sessionDate() == null || session.period() <= 0) {
                            throw new IllegalArgumentException("Invalid session entry: " + session);
                        }
                        sessionStmt.setInt(1, offeringId);
                        sessionStmt.setDate(2, Date.valueOf(session.sessionDate()));
                        sessionStmt.setInt(3, session.period());
                        sessionStmt.addBatch();
                    }
                    int[] batchCounts = sessionStmt.executeBatch();
                    if (batchCounts.length != sessions.size()) {
                        throw new SQLException("Incomplete class_session batch insert.");
                    }
                }

                /*
                 * try (PreparedStatement weightStmt = conn.prepareStatement(insertWeightSql)) {
                 * weightStmt.setInt(1, offeringId);
                 * weightStmt.setBigDecimal(2, tutorialWeight);
                 * weightStmt.setBigDecimal(3, assignmentWeight);
                 * weightStmt.setBigDecimal(4, labTestWeight);
                 * weightStmt.setBigDecimal(5, practicalWeight);
                 * weightStmt.setBigDecimal(6, finalExamWeight);
                 * 
                 * if (weightStmt.executeUpdate() == 0) {
                 * throw new
                 * SQLException("Creating assessment_weight failed, no rows affected.");
                 * }
                 * }
                 */

                try (PreparedStatement enrollStmt = conn.prepareStatement(enrollStudentsSql)) {
                    enrollStmt.setInt(1, offeringId);
                    enrollStmt.setDate(2, Date.valueOf(startDate));
                    enrollStmt.setString(3, year);

                    int paramIndex = 4;
                    for (Integer deptId : targetDeptIds) {
                        enrollStmt.setInt(paramIndex++, deptId);
                    }
                    enrollStmt.executeUpdate();
                }

                conn.commit();
                return offeringId;

            } catch (Exception ex) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
                if (ex instanceof SQLException sqlEx) {
                    throw sqlEx;
                }
                throw new SQLException("Failed to create course offering transactionally", ex);
            } finally {
                try {
                    conn.setAutoCommit(previousAutoCommit);
                } catch (SQLException ignored) {
                    // Connection is about to close
                }
            }
        }
    }

    /*
     * private void validateWeights(
     * BigDecimal tutorial,
     * BigDecimal assignment,
     * BigDecimal labTest,
     * BigDecimal practical,
     * BigDecimal finalExam) {
     * 
     * BigDecimal[] weights = { tutorial, assignment, labTest, practical, finalExam
     * };
     * BigDecimal sum = BigDecimal.ZERO;
     * 
     * for (BigDecimal w : weights) {
     * if (w == null || w.compareTo(BigDecimal.ZERO) < 0) {
     * throw new
     * IllegalArgumentException("Assessment weights must be non-negative.");
     * }
     * sum = sum.add(w);
     * }
     * 
     * if (sum.compareTo(new BigDecimal("100.00")) != 0) {
     * throw new IllegalArgumentException(
     * "Assessment weights must sum to 100.00, got: " + sum);
     * }
     * }
     */
    public int getTotalEnrolledStudents(Connection conn, int offeringId) throws SQLException {
        String countSql = "SELECT COUNT(*) AS cnt FROM course_enrollment WHERE offering_id = ?";
        try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
            countStmt.setInt(1, offeringId);
            try (ResultSet rs = countStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                } else {
                    throw new SQLException("Offering not found for: " + offeringId);
                }
            }
        }
    }

    public Map<Integer, String> getSchedulesByOffering(int userDeptId) {
        String sql = """
                SELECT DISTINCT
                    cs.offering_id,
                    cs.session_date,
                    cs.period
                FROM class_session cs
                JOIN course_offering co ON cs.offering_id = co.offering_id
                WHERE cs.is_cancelled = FALSE
                  AND (
                      co.create_dept_id = ?
                      OR EXISTS (
                          SELECT 1
                          FROM course_enrollment ce
                          JOIN student s ON ce.student_id = s.student_id
                          WHERE ce.offering_id = co.offering_id
                            AND s.dept_id = ?
                      )
                  )
                ORDER BY cs.offering_id ASC, cs.session_date ASC, cs.period ASC
                """;

        Map<Integer, List<LocalDate>> datesByOffering = new HashMap<>();
        Map<Integer, List<Integer>> periodsByOffering = new HashMap<>();

        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userDeptId);
            pstmt.setInt(2, userDeptId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int offeringId = rs.getInt("offering_id");
                    java.sql.Date sqlDate = rs.getDate("session_date");
                    if (sqlDate == null) {
                        continue;
                    }
                    LocalDate date = sqlDate.toLocalDate();
                    int period = rs.getInt("period");

                    datesByOffering
                            .computeIfAbsent(offeringId, k -> new ArrayList<>())
                            .add(date);
                    periodsByOffering
                            .computeIfAbsent(offeringId, k -> new ArrayList<>())
                            .add(period);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Map<Integer, String> scheduleByOffering = new HashMap<>();
        for (Integer offeringId : datesByOffering.keySet()) {
            String schedule = buildScheduleString(
                    datesByOffering.get(offeringId),
                    periodsByOffering.get(offeringId));
            scheduleByOffering.put(offeringId, schedule);
        }

        return scheduleByOffering;
    }

    private static String periodToTimeRange(int period) {
        return switch (period) {
            case 1 -> "08:00 - 08:50";
            case 2 -> "09:00 - 09:50";
            case 3 -> "10:00 - 10:50";
            case 4 -> "11:00 - 11:50";
            case 5 -> "12:00 - 12:50";
            case 6 -> "1:00 - 1:50";
            case 7 -> "2:00 - 2:50";
            case 8 -> "3:00 - 3:50";
            case 9 -> "4:00 - 4:50";
            default -> "Period " + period;
        };
    }

    private record SlotKey(int dayOrder, String dayLabel, int period, String timeSlot) {
    }

    public static String buildScheduleString(List<LocalDate> dates, List<Integer> periods) {
        Set<SlotKey> uniqueSlots = new TreeSet<>(
                Comparator.comparingInt(SlotKey::dayOrder)
                        .thenComparingInt(SlotKey::period));

        for (int i = 0; i < dates.size(); i++) {
            LocalDate d = dates.get(i);
            int p = periods.get(i);
            if (d == null)
                continue;

            DayOfWeek dow = d.getDayOfWeek();
            String dayLabel = dow.name().substring(0, 3); // "MON", "TUE", "WED"...
            uniqueSlots.add(new SlotKey(dow.getValue(), dayLabel, p, periodToTimeRange(p)));
        }

        return uniqueSlots.stream()
                .map(s -> s.dayLabel() + " " + s.timeSlot())
                .collect(Collectors.joining(" | "));
    }

    public List<ClassItem> getActiveClassesForDepartment(int userDeptId) {
        Map<Integer, String> scheduleByOffering = getSchedulesByOffering(userDeptId);
        List<ClassItem> classItems = new ArrayList<>();
        String classSql = """
                    SELECT
                    co.offering_id,
                    c.course_code,
                    c.course_name,
                    COALESCE(
                        (
                            SELECT d.dept_id
                            FROM course_enrollment ce
                            JOIN student s ON ce.student_id = s.student_id
                            JOIN department d ON s.dept_id = d.dept_id
                            WHERE ce.offering_id = co.offering_id
                            LIMIT 1
                        ),
                        d_create.dept_id
                    ) AS target_major,
                    co.year,
                    co.term,
                    co.max_student,
                    c.dept_id AS course_dept,
                    t.full_name AS lecturer,
                    co.badge_color,
                    co.card_symbol
                FROM course_offering co
                JOIN course c ON co.course_id = c.course_id
                JOIN department d_create ON co.create_dept_id = d_create.dept_id
                LEFT JOIN teacher t ON co.teacher_id = t.teacher_id
                WHERE co.create_dept_id = ?
                   OR EXISTS (
                       SELECT 1
                       FROM course_enrollment ce
                       JOIN student s ON ce.student_id = s.student_id
                       WHERE ce.offering_id = co.offering_id
                         AND s.dept_id = ?
                   )
                ORDER BY co.offering_id DESC;
                        """;
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmtClass = conn.prepareStatement(classSql)) {
            pstmtClass.setInt(1, userDeptId);
            pstmtClass.setInt(2, userDeptId);
            try (ResultSet rs = pstmtClass.executeQuery()) {

                while (rs.next()) {
                    int offeringId = rs.getInt("offering_id");
                    String courseCode = rs.getString("course_code");
                    String courseTitle = rs.getString("course_name");

                    int targetDeptId = rs.getInt("target_major");
                    String major = resolveMajorLabel(targetDeptId);

                    String year = rs.getString("year");
                    String term = rs.getString("term");

                    int maxStudent = rs.getInt("max_student");
                    int enrolledCount = getTotalEnrolledStudents(conn, offeringId);
                    String studentCountStr = enrolledCount + " / " + maxStudent + " students";

                    String scheduleStr = scheduleByOffering.getOrDefault(offeringId, "TBD");

                    String lecturer = rs.getString("lecturer");
                    if (lecturer == null || lecturer.isBlank()) {
                        lecturer = "Unassigned"; // teacher_id is nullable
                    }

                    String badgeColor = rs.getString("badge_color");
                    if (badgeColor == null || badgeColor.isBlank()) {
                        badgeColor = "#2563eb";
                    }

                    String cardSymbol = rs.getString("card_symbol");
                    if (cardSymbol == null || cardSymbol.isBlank()) {
                        cardSymbol = "</>";
                    }

                    classItems.add(new ClassItem(
                            courseCode,
                            courseTitle,
                            major,
                            year,
                            term,
                            "", // className (not needed on card)
                            studentCountStr,
                            scheduleStr,
                            lecturer,
                            badgeColor,
                            cardSymbol));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return classItems;
    }

    private static String resolveMajorLabel(int deptId) {
        return switch (deptId) {
            case 1 -> "CEIT";
            case 2 -> "Mech";
            case 3 -> "Arch";
            case 4 -> "EC";
            case 5 -> "EP";
            case 6 -> "ChE";
            case 7 -> "PE";
            case 8 -> "C";
            case 9 -> "FE";
            case 10 -> "McE";
            case 11 -> "Mn";
            case 12 -> "Tele";
            case 13 -> "Tex";
            case 14 -> "Met";
            default -> "Dept " + deptId;
        };
    }

    private String formatYear(int yearNum) {
        return switch (yearNum) {
            case 1 -> "First Year";
            case 2 -> "Second Year";
            case 3 -> "Third Year";
            case 4 -> "Fourth Year";
            case 5 -> "Fifth Year";
            default -> "Year " + yearNum;
        };
    }

    private String formatTerm(int termNum) {
        return switch (termNum) {
            case 1 -> "First Semester";
            case 2 -> "Second Semester";
            default -> "Semester " + termNum;
        };
    }
    
    public int findOfferingId(Connection conn, String courseCode, String year, String term) throws SQLException {
        String sql = """
                SELECT co.offering_id
                FROM course_offering co
                JOIN course c ON co.course_id = c.course_id
                WHERE c.course_code = ?
                  AND co.year = ?
                  AND co.term = ?
                ORDER BY co.offering_id DESC
                LIMIT 1
                """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseCode);
            pstmt.setString(2, year);
            pstmt.setString(3, term);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("offering_id");
                }
            }
        }
        return -1;
    }

    public List<SessionInput> getSessionsForOffering(String courseCode, String year, String term) {
        List<SessionInput> sessions = new ArrayList<>();
        String sql = """
                SELECT cs.session_date, cs.period
                FROM class_session cs
                JOIN course_offering co ON cs.offering_id = co.offering_id
                JOIN course c ON co.course_id = c.course_id
                WHERE c.course_code = ?
                  AND co.year = ?
                  AND co.term = ?
                  AND cs.is_cancelled = FALSE
                ORDER BY cs.session_date ASC, cs.period ASC
                """;
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseCode);
            pstmt.setString(2, year);
            pstmt.setString(3, term);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("session_date");
                    if (d != null) {
                        sessions.add(new SessionInput(d.toLocalDate(), rs.getInt("period")));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sessions;
    }

    public boolean updateOfferingTeacherAndSessions(
            String courseCode,
            String year,
            String term,
            String lecturerName,
            List<SessionInput> newSessions) throws SQLException {

        if (newSessions == null || newSessions.isEmpty()) {
            throw new IllegalArgumentException("At least one class session is required.");
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            boolean prevAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                int offeringId = findOfferingId(conn, courseCode, year, term);
                if (offeringId <= 0) {
                    conn.rollback();
                    return false;
                }

                // Resolve teacher_id by name
                int teacherId = -1;
                String findTeacherSql = """
                        SELECT t.teacher_id
                        FROM teacher t
                        JOIN user_account ua ON t.user_id = ua.user_id
                        WHERE ua.status = TRUE
                          AND ua.user_role IN ('TEACHER', 'ADMIN')
                          AND t.full_name = ?
                        LIMIT 1
                        """;
                try (PreparedStatement tStmt = conn.prepareStatement(findTeacherSql)) {
                    tStmt.setString(1, lecturerName);
                    try (ResultSet rs = tStmt.executeQuery()) {
                        if (rs.next()) {
                            teacherId = rs.getInt("teacher_id");
                        }
                    }
                }

                // Update course_offering (teacher + total_sessions)
                if (teacherId > 0) {
                    String updateOfferingSql = "UPDATE course_offering SET teacher_id = ?, total_sessions = ? WHERE offering_id = ?";
                    try (PreparedStatement uStmt = conn.prepareStatement(updateOfferingSql)) {
                        uStmt.setInt(1, teacherId);
                        uStmt.setInt(2, newSessions.size());
                        uStmt.setInt(3, offeringId);
                        uStmt.executeUpdate();
                    }
                } else {
                    String updateCountSql = "UPDATE course_offering SET total_sessions = ? WHERE offering_id = ?";
                    try (PreparedStatement uStmt = conn.prepareStatement(updateCountSql)) {
                        uStmt.setInt(1, newSessions.size());
                        uStmt.setInt(2, offeringId);
                        uStmt.executeUpdate();
                    }
                }

                // Build a set of desired (date|period) keys so untouched sessions keep their attendance records
                Set<String> desiredKeys = newSessions.stream()
                        .map(s -> s.sessionDate().toString() + "|" + s.period())
                        .collect(Collectors.toSet());

                // Delete only sessions removed by the admin
                List<Integer> sessionIdsToDelete = new ArrayList<>();
                String existingSql = "SELECT session_id, session_date, period FROM class_session WHERE offering_id = ?";
                try (PreparedStatement eStmt = conn.prepareStatement(existingSql)) {
                    eStmt.setInt(1, offeringId);
                    try (ResultSet rs = eStmt.executeQuery()) {
                        while (rs.next()) {
                            String key = rs.getDate("session_date").toLocalDate() + "|" + rs.getInt("period");
                            if (!desiredKeys.contains(key)) {
                                sessionIdsToDelete.add(rs.getInt("session_id"));
                            }
                        }
                    }
                }

                if (!sessionIdsToDelete.isEmpty()) {
                    String delSql = "DELETE FROM class_session WHERE session_id = ?";
                    try (PreparedStatement dStmt = conn.prepareStatement(delSql)) {
                        for (int sid : sessionIdsToDelete) {
                            dStmt.setInt(1, sid);
                            dStmt.addBatch();
                        }
                        dStmt.executeBatch();
                    }
                }

                // Insert newly added sessions (existing matching ones are ignored cleanly)
                String insertSql = "INSERT IGNORE INTO class_session (offering_id, session_date, period) VALUES (?, ?, ?)";
                try (PreparedStatement iStmt = conn.prepareStatement(insertSql)) {
                    for (SessionInput s : newSessions) {
                        iStmt.setInt(1, offeringId);
                        iStmt.setDate(2, Date.valueOf(s.sessionDate()));
                        iStmt.setInt(3, s.period());
                        iStmt.addBatch();
                    }
                    iStmt.executeBatch();
                }

                conn.commit();
                return true;
            } catch (Exception ex) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
                if (ex instanceof SQLException sqlEx) throw sqlEx;
                throw new SQLException("Failed to update offering schedule", ex);
            } finally {
                try {
                    conn.setAutoCommit(prevAutoCommit);
                } catch (SQLException ignored) {}
            }
        }
    }
    
    public boolean updateOfferingMetadata(
            String courseCode,
            String oldYear,
            String oldTerm,
            String newCourseName,
            String newYear,
            String newTerm,
            int newMaxStudents,
            String newCardSymbol,
            String newBadgeColor) throws SQLException {

        try (Connection conn = DatabaseConnector.getConnection()) {
            boolean prevAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                int offeringId = findOfferingId(conn, courseCode, oldYear, oldTerm);
                if (offeringId <= 0) {
                    conn.rollback();
                    return false;
                }

                // 1. Update offering fields (year, term, max_student, card_symbol, badge_color)
                String updateOfferingSql = """
                        UPDATE course_offering
                        SET `year` = ?, term = ?, max_student = ?, card_symbol = ?, badge_color = ?
                        WHERE offering_id = ?
                        """;
                try (PreparedStatement oStmt = conn.prepareStatement(updateOfferingSql)) {
                    oStmt.setString(1, newYear);
                    oStmt.setString(2, newTerm);
                    oStmt.setInt(3, newMaxStudents);
                    oStmt.setString(4, newCardSymbol);
                    oStmt.setString(5, newBadgeColor);
                    oStmt.setInt(6, offeringId);
                    oStmt.executeUpdate();
                }

                // 2. Update course title if changed
                if (newCourseName != null && !newCourseName.isBlank()) {
                    String updateCourseSql = """
                            UPDATE course c
                            JOIN course_offering co ON c.course_id = co.course_id
                            SET c.course_name = ?
                            WHERE co.offering_id = ?
                            """;
                    try (PreparedStatement cStmt = conn.prepareStatement(updateCourseSql)) {
                        cStmt.setString(1, newCourseName.trim());
                        cStmt.setInt(2, offeringId);
                        cStmt.executeUpdate();
                    }
                }

                conn.commit();
                return true;
            } catch (Exception ex) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
                if (ex instanceof SQLException sqlEx) throw sqlEx;
                throw new SQLException("Failed to update class details", ex);
            } finally {
                try {
                    conn.setAutoCommit(prevAutoCommit);
                } catch (SQLException ignored) {}
            }
        }
    }
}
