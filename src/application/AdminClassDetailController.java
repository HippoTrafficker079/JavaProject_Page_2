package application;

import application.dao.CourseOfferingDAO;
import application.dao.CourseOfferingDAO.SessionInput;
import application.dao.DatabaseConnector;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class AdminClassDetailController {

    @FXML private VBox rootPane;
    @FXML private Label classNameLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label activeStudentsLabel;
    @FXML private Label atRiskLabel;
    @FXML private Label inactiveLabel;

    // Course Allocation & Auto-Generate Schedule Builder
    @FXML private ComboBox<String> assignTeacherCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private TextField totalSessionsField;
    @FXML private ComboBox<String> slotDayCombo;
    @FXML private ComboBox<String> slotPeriodCombo;
    @FXML private ComboBox<String> slotDurationCombo;
    @FXML private FlowPane weeklySlotsPane;
    @FXML private TableView<SessionRow> sessionTable;
    @FXML private TableColumn<SessionRow, String> colSessionNo;
    @FXML private TableColumn<SessionRow, String> colSessionDate;
    @FXML private TableColumn<SessionRow, String> colSessionDay;
    @FXML private TableColumn<SessionRow, String> colSessionPeriod;
    @FXML private TableColumn<SessionRow, String> colSessionAction;
    @FXML private Label sessionSummaryLabel;

    // Navigation Tabs & Containers
    @FXML private Button tabRosterBtn;
    @FXML private Button tabAttendanceBtn;
    @FXML private VBox rosterSection;
    @FXML private VBox attendanceSection;

    // Tab 1: Student Roster Controls & Table
    @FXML private ComboBox<String> rollNoCombo;
    @FXML private ComboBox<String> standingCombo;
    @FXML private TextField searchField;
    @FXML private Label studentCountFooterLabel;

    @FXML private TableView<ClassDetailController.StudentModel> studentTable;
    @FXML private TableColumn<ClassDetailController.StudentModel, String> colId;
    @FXML private TableColumn<ClassDetailController.StudentModel, String> colRollNo;
    @FXML private TableColumn<ClassDetailController.StudentModel, String> colName;
    @FXML private TableColumn<ClassDetailController.StudentModel, String> colEmail;
    @FXML private TableColumn<ClassDetailController.StudentModel, Double> colGpa;
    @FXML private TableColumn<ClassDetailController.StudentModel, String> colAcademicStanding;

    // Tab 2: Daily Attendance Log Controls & Table
    @FXML private DatePicker dailyDatePicker;
    @FXML private TextField dailySearchField;
    @FXML private CheckBox chkPresent;
    @FXML private CheckBox chkAbsent;
    @FXML private CheckBox chkExcuse;
    @FXML private Label dailyLogFooterLabel;

    @FXML private TableView<ClassDetailController.DailyLogEntry> dailyLogTable;
    @FXML private TableColumn<ClassDetailController.DailyLogEntry, Integer> colNoLog;
    @FXML private TableColumn<ClassDetailController.DailyLogEntry, String> colRollLog;
    @FXML private TableColumn<ClassDetailController.DailyLogEntry, String> colNameLog;
    @FXML private TableColumn<ClassDetailController.DailyLogEntry, String> colDateLog;
    @FXML private TableColumn<ClassDetailController.DailyLogEntry, String> colSubjLog;
    @FXML private TableColumn<ClassDetailController.DailyLogEntry, String> colStatusLog;
    @FXML private TableColumn<ClassDetailController.DailyLogEntry, String> colRemarksLog;
    
    @FXML private HBox scheduleHeaderBar;
    @FXML private Button toggleScheduleBtn;
    @FXML private VBox scheduleBodyPane;
    private boolean isScheduleExpanded = false;
    
    
    private final ObservableList<ClassDetailController.StudentModel> masterStudentData = FXCollections.observableArrayList();
    private FilteredList<ClassDetailController.StudentModel> filteredRosterData;

    private final ObservableList<ClassDetailController.DailyLogEntry> masterDailyLogData = FXCollections.observableArrayList();
    private FilteredList<ClassDetailController.DailyLogEntry> filteredDailyLogData;
    private final Map<String, String> originalDailyStatuses = new HashMap<>();

    private final List<WeeklySlot> weeklySlots = new ArrayList<>();
    private final ObservableList<SessionRow> generatedSessions = FXCollections.observableArrayList();
    private boolean isSyncingSchedule = false;

    private ClassesController.ClassItem currentClassItem;
    private String currentClassCode = "IT201";
    private String currentSubjectName = "Data Structures and Algorithms";

    private static final String[] PERIOD_START_TIMES = {
        "", "8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM",
        "12:00 PM", "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM"
    };

    private static final String[] PERIOD_END_TIMES = {
        "", "8:50 AM", "9:50 AM", "10:50 AM", "11:50 AM",
        "12:50 PM", "1:50 PM", "2:50 PM", "3:50 PM", "4:50 PM"
    };

    private static final String[] PERIOD_START_24H = {
        "", "08:00", "09:00", "10:00", "11:00",
        "12:00", "13:00", "14:00", "15:00", "16:00"
    };

    private static final String[] PERIOD_END_24H = {
        "", "08:50", "09:50", "10:50", "11:50",
        "12:50", "13:50", "14:50", "15:50", "16:50"
    };

    @FXML
    public void initialize() {
        if (rootPane != null) {
            rootPane.setOnMouseClicked(event -> {
                if (studentTable != null) studentTable.getSelectionModel().clearSelection();
                if (dailyLogTable != null) dailyLogTable.getSelectionModel().clearSelection();
                rootPane.requestFocus();
            });
        }

        initScheduleBuilder();
        setupRosterTable();
        setupDailyLogControlsAndTable();
        switchToRosterTab();
    }

    // =========================================================================
    // COURSE ALLOCATION & AUTO-GENERATE SEMESTER SCHEDULE
    // =========================================================================

    private void initScheduleBuilder() {
        if (slotDayCombo != null) {
            slotDayCombo.setItems(FXCollections.observableArrayList(
                "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"
            ));
            slotDayCombo.setValue("Tuesday");
        }

        if (slotPeriodCombo != null) {
            slotPeriodCombo.setItems(FXCollections.observableArrayList(
                "Period 1 (8:00 – 8:50 AM)",
                "Period 2 (9:00 – 9:50 AM)",
                "Period 3 (10:00 – 10:50 AM)",
                "Period 4 (11:00 – 11:50 AM)",
                "Period 5 (12:00 – 12:50 PM)",
                "Period 6 (1:00 – 1:50 PM)",
                "Period 7 (2:00 – 2:50 PM)",
                "Period 8 (3:00 – 3:50 PM)",
                "Period 9 (4:00 – 4:50 PM)"
            ));
            slotPeriodCombo.setValue("Period 6 (1:00 – 1:50 PM)");
        }

        if (slotDurationCombo != null) {
            slotDurationCombo.setItems(FXCollections.observableArrayList(
                "1 Period (Single)",
                "2 Periods (Double)",
                "3 Periods (Triple)",
                "4 Periods (Quad)"
            ));
            slotDurationCombo.setValue("2 Periods (Double)");
        }

        if (startDatePicker != null) {
            startDatePicker.valueProperty().addListener((obs, o, n) -> {
                if (!isSyncingSchedule) regenerateSessions();
            });
        }

        if (totalSessionsField != null) {
            totalSessionsField.textProperty().addListener((obs, o, n) -> {
                if (!isSyncingSchedule) regenerateSessions();
            });
        }

        if (sessionTable != null) {
            colSessionNo.setCellValueFactory(c -> c.getValue().sessionNoProperty());
            colSessionDate.setCellValueFactory(c -> c.getValue().dateProperty());
            colSessionDay.setCellValueFactory(c -> c.getValue().dayProperty());
            colSessionPeriod.setCellValueFactory(c -> c.getValue().periodLabelProperty());

            colSessionAction.setCellFactory(col -> new TableCell<>() {
                private final Button removeBtn = new Button("🗑 Remove");

                {
                    removeBtn.setMinWidth(Button.USE_PREF_SIZE);
                    removeBtn.setStyle(
                        "-fx-background-color: transparent; -fx-text-fill: #475569; " +
                        "-fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3px 8px;"
                    );
                    removeBtn.setOnMouseEntered(e -> removeBtn.setStyle(
                        "-fx-background-color: #fee2e2; -fx-text-fill: #e11d48; -fx-background-radius: 5px; " +
                        "-fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3px 8px;"
                    ));
                    removeBtn.setOnMouseExited(e -> removeBtn.setStyle(
                        "-fx-background-color: transparent; -fx-text-fill: #475569; " +
                        "-fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3px 8px;"
                    ));
                    removeBtn.setOnAction(e -> {
                        SessionRow row = getTableView().getItems().get(getIndex());
                        generatedSessions.remove(row);
                        renumberSessions();

                        if (totalSessionsField != null) {
                            isSyncingSchedule = true;
                            totalSessionsField.setText(String.valueOf(generatedSessions.size()));
                            isSyncingSchedule = false;
                        }
                        updateSummaryLabel();
                    });
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setAlignment(Pos.CENTER_LEFT);
                        setGraphic(removeBtn);
                    }
                }
            });

            sessionTable.setItems(generatedSessions);
            lockTableColumns(sessionTable, 0.09, 0.22, 0.13, 0.34, 0.216);
        }
    }

    private void loadScheduleForClass(ClassesController.ClassItem classItem) {
        if (classItem == null) return;

        // Populate lecturer dropdown from MySQL (with fallback to TeacherDirectoryStore)
        if (assignTeacherCombo != null) {
            List<String> teachers = loadAllActiveTeachersFromDb();
            if (teachers.isEmpty()) {
                teachers = TeacherDirectoryStore.getTeacherNamesByDept(null);
            }
            assignTeacherCombo.setItems(FXCollections.observableArrayList(teachers));
            String cleanLecturer = classItem.lecturer != null
                    ? classItem.lecturer.replace(" (Assigned)", "").trim()
                    : "";
            if (!cleanLecturer.isEmpty()) {
                if (!assignTeacherCombo.getItems().contains(cleanLecturer)) {
                    assignTeacherCombo.getItems().add(cleanLecturer);
                }
                assignTeacherCombo.setValue(cleanLecturer);
            }
        }

        // Load real sessions from MySQL class_session table
        CourseOfferingDAO offeringDAO = new CourseOfferingDAO();
        List<SessionInput> dbSessions = offeringDAO.getSessionsForOffering(
                classItem.code, classItem.year, classItem.semester
        );

        isSyncingSchedule = true;
        weeklySlots.clear();
        generatedSessions.clear();

        if (!dbSessions.isEmpty()) {
            int counter = 1;
            Map<DayOfWeek, Set<Integer>> periodsByDay = new TreeMap<>();
            for (SessionInput s : dbSessions) {
                int p = Math.max(1, Math.min(9, s.period()));
                DayOfWeek dow = s.sessionDate().getDayOfWeek();
                String dayShort = dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                String periodDisplay = "Period " + p + " (" + PERIOD_START_TIMES[p] + " – " + PERIOD_END_TIMES[p] + ")";

                generatedSessions.add(new SessionRow(
                        String.valueOf(counter++),
                        s.sessionDate().toString(),
                        dayShort,
                        p,
                        periodDisplay
                ));
                periodsByDay.computeIfAbsent(dow, k -> new TreeSet<>()).add(p);
            }

            // Reconstruct weekly slot chips by grouping consecutive periods per day
            for (Map.Entry<DayOfWeek, Set<Integer>> entry : periodsByDay.entrySet()) {
                DayOfWeek dow = entry.getKey();
                List<Integer> periods = new ArrayList<>(entry.getValue());
                int i = 0;
                while (i < periods.size()) {
                    int start = periods.get(i);
                    int dur = 1;
                    while (i + dur < periods.size() && periods.get(i + dur) == start + dur && dur < 4) {
                        dur++;
                    }
                    weeklySlots.add(new WeeklySlot(dow, start, dur));
                    i += dur;
                }
            }

            if (startDatePicker != null) startDatePicker.setValue(dbSessions.get(0).sessionDate());
            if (totalSessionsField != null) totalSessionsField.setText(String.valueOf(dbSessions.size()));
        } else {
            if (startDatePicker != null) startDatePicker.setValue(LocalDate.of(2026, 6, 1));
            if (totalSessionsField != null) totalSessionsField.setText("24");
        }

        isSyncingSchedule = false;
        renderWeeklySlotChips();
        updateSummaryLabel();
    }

    private List<String> loadAllActiveTeachersFromDb() {
        List<String> names = new ArrayList<>();
        String sql = """
                SELECT DISTINCT t.full_name
                FROM teacher t
                JOIN user_account ua ON t.user_id = ua.user_id
                WHERE ua.status = TRUE
                  AND ua.user_role IN ('TEACHER', 'ADMIN')
                ORDER BY t.full_name ASC
                """;
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString("full_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return names;
    }

    @FXML
    private void handleAddWeeklySlot(ActionEvent event) {
        String dayName = slotDayCombo != null ? slotDayCombo.getValue() : null;
        String periodStr = slotPeriodCombo != null ? slotPeriodCombo.getValue() : null;
        String durationStr = slotDurationCombo != null ? slotDurationCombo.getValue() : "1 Period (Single)";
        if (dayName == null || periodStr == null) return;

        DayOfWeek day = DayOfWeek.valueOf(dayName.toUpperCase(Locale.ENGLISH));
        int startPeriod = extractPeriodNumber(periodStr);
        int duration = extractDurationCount(durationStr);

        if (startPeriod + duration - 1 > 9) {
            showAlert(Alert.AlertType.WARNING, "Exceeds Daily Periods",
                    "Period 9 is the final period of the day, so a " + duration
                    + "-period block cannot start at Period " + startPeriod + ".");
            return;
        }

        int newEnd = startPeriod + duration - 1;
        for (WeeklySlot existing : weeklySlots) {
            if (existing.day == day) {
                int existingEnd = existing.startPeriod + existing.duration - 1;
                if (startPeriod <= existingEnd && newEnd >= existing.startPeriod) {
                    showAlert(Alert.AlertType.WARNING, "Overlapping Weekly Slot",
                            dayName + " already has a scheduled block covering " + existing.formatPeriods() + ".");
                    return;
                }
            }
        }

        weeklySlots.add(new WeeklySlot(day, startPeriod, duration));
        weeklySlots.sort(Comparator.comparing((WeeklySlot s) -> s.day.getValue()).thenComparingInt(s -> s.startPeriod));

        renderWeeklySlotChips();
        regenerateSessions();
    }

    private void renderWeeklySlotChips() {
        if (weeklySlotsPane == null) return;
        weeklySlotsPane.getChildren().clear();

        if (weeklySlots.isEmpty()) {
            Label emptyLbl = new Label("No weekly slots added. Pick a Day, Start Period, and Length above, then click + Add Slot.");
            emptyLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-style: italic;");
            weeklySlotsPane.getChildren().add(emptyLbl);
            return;
        }

        for (WeeklySlot slot : weeklySlots) {
            HBox chip = new HBox(6.0);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.setStyle(
                "-fx-background-color: #dbeafe; -fx-border-color: #93c5fd; " +
                "-fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 4px 10px;"
            );

            Label textLbl = new Label(slot.toChipLabel());
            textLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");

            Button closeBtn = new Button("✕");
            closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #1d4ed8; -fx-font-size: 10px; " +
                "-fx-font-weight: bold; -fx-padding: 0 2px; -fx-cursor: hand;"
            );
            closeBtn.setOnAction(e -> {
                weeklySlots.remove(slot);
                renderWeeklySlotChips();
                regenerateSessions();
            });

            chip.getChildren().addAll(textLbl, closeBtn);
            weeklySlotsPane.getChildren().add(chip);
        }
    }

    private void regenerateSessions() {
        generatedSessions.clear();

        LocalDate startDate = startDatePicker != null ? startDatePicker.getValue() : null;
        int totalNeeded = parsePositiveInt(totalSessionsField != null ? totalSessionsField.getText() : "", 0);

        if (startDate == null || totalNeeded <= 0 || weeklySlots.isEmpty()) {
            updateSummaryLabel();
            return;
        }

        int capped = Math.min(totalNeeded, 200);
        LocalDate currentDate = startDate;
        int counter = 1;

        while (generatedSessions.size() < capped) {
            DayOfWeek dow = currentDate.getDayOfWeek();
            for (WeeklySlot slot : weeklySlots) {
                if (slot.day == dow) {
                    String dayShort = dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    String dateStr = currentDate.toString();
                    for (int offset = 0; offset < slot.duration && generatedSessions.size() < capped; offset++) {
                        int p = slot.startPeriod + offset;
                        String periodDisplay = "Period " + p + " (" + PERIOD_START_TIMES[p] + " – " + PERIOD_END_TIMES[p] + ")";
                        generatedSessions.add(new SessionRow(String.valueOf(counter++), dateStr, dayShort, p, periodDisplay));
                    }
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        updateSummaryLabel();
    }

    private void renumberSessions() {
        for (int i = 0; i < generatedSessions.size(); i++) {
            generatedSessions.get(i).setSessionNo(String.valueOf(i + 1));
        }
    }

    private void updateSummaryLabel() {
        if (sessionSummaryLabel == null) return;
        if (weeklySlots.isEmpty() && generatedSessions.isEmpty()) {
            sessionSummaryLabel.setText("Add at least one Weekly Timetable Slot above to generate class sessions.");
            return;
        }
        String patternSummary = weeklySlots.stream()
                .map(WeeklySlot::toShortSummary)
                .collect(Collectors.joining(" | "));
        if (generatedSessions.isEmpty()) {
            sessionSummaryLabel.setText("Enter a valid Start Date and Total Sessions (" + patternSummary + ")");
        } else {
            String firstDate = generatedSessions.get(0).getDate();
            String lastDate = generatedSessions.get(generatedSessions.size() - 1).getDate();
            sessionSummaryLabel.setText(
                "Scheduled " + generatedSessions.size() + " period sessions (" + patternSummary + " · " + firstDate + " → " + lastDate + ")"
            );
        }
    }

    @FXML
    public void handleSaveAdminChanges(ActionEvent event) {
        if (currentClassItem == null) return;

        String newLecturer = assignTeacherCombo != null ? assignTeacherCombo.getValue() : null;
        if (newLecturer == null || newLecturer.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Missing Lecturer", "Please select an Assigned Lecturer.");
            return;
        }

        int totalSessions = parsePositiveInt(totalSessionsField != null ? totalSessionsField.getText() : "", -1);
        if (generatedSessions.isEmpty() || totalSessions <= 0) {
            showAlert(Alert.AlertType.WARNING, "No Class Sessions",
                    "Please add at least one weekly slot and generate at least 1 session before saving.");
            return;
        }
        if (generatedSessions.size() != totalSessions) {
            showAlert(Alert.AlertType.WARNING, "Session Count Conflict",
                    "Total Sessions is " + totalSessions + ", but the schedule table has "
                    + generatedSessions.size() + " sessions.");
            return;
        }

        List<SessionInput> sessionInputs = generatedSessions.stream()
                .map(row -> new SessionInput(LocalDate.parse(row.getDate()), row.getPeriodNumber()))
                .collect(Collectors.toList());

        try {
            CourseOfferingDAO offeringDAO = new CourseOfferingDAO();
            offeringDAO.updateOfferingTeacherAndSessions(
                    currentClassItem.code,
                    currentClassItem.year,
                    currentClassItem.semester,
                    newLecturer,
                    sessionInputs
            );
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Failed to save schedule changes:\n" + e.getMessage());
            return;
        }

        // Refresh the card in SHARED_CLASS_DATA immediately
        String newScheduleSummary = weeklySlots.isEmpty()
                ? CourseOfferingDAO.buildScheduleString(
                        sessionInputs.stream().map(SessionInput::sessionDate).collect(Collectors.toList()),
                        sessionInputs.stream().map(SessionInput::period).collect(Collectors.toList()))
                : weeklySlots.stream().map(WeeklySlot::toBadgeSchedule).collect(Collectors.joining(" | "));

        currentClassItem.lecturer = newLecturer;
        currentClassItem.schedule = newScheduleSummary;

        int idx = ClassesController.SHARED_CLASS_DATA.indexOf(currentClassItem);
        if (idx >= 0) {
            ClassesController.SHARED_CLASS_DATA.set(idx, currentClassItem);
        }

        showAlert(Alert.AlertType.INFORMATION, "Changes Saved",
                "Updated lecturer (" + newLecturer + ") and " + generatedSessions.size()
                + " scheduled sessions for " + currentClassItem.code + ".");
    }

    // =========================================================================
    // TAB SWITCHING
    // =========================================================================

    @FXML
    public void switchToRosterTab() {
        if (rosterSection != null) {
            rosterSection.setVisible(true);
            rosterSection.setManaged(true);
        }
        if (attendanceSection != null) {
            attendanceSection.setVisible(false);
            attendanceSection.setManaged(false);
        }
        setActiveTabStyle(tabRosterBtn, tabAttendanceBtn);
    }

    @FXML
    public void switchToAttendanceTab() {
        if (rosterSection != null) {
            rosterSection.setVisible(false);
            rosterSection.setManaged(false);
        }
        if (attendanceSection != null) {
            attendanceSection.setVisible(true);
            attendanceSection.setManaged(true);
        }
        setActiveTabStyle(tabAttendanceBtn, tabRosterBtn);
        if (dailyLogTable != null) {
            dailyLogTable.refresh();
        }
    }

    private void setActiveTabStyle(Button activeBtn, Button inactiveBtn) {
        if (activeBtn != null) {
            activeBtn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent transparent #1b315e transparent; " +
                "-fx-border-width: 0 0 3.5 0; -fx-text-fill: #0f172a; -fx-font-size: 14px; -fx-font-weight: 800; " +
                "-fx-padding: 6px 8px; -fx-cursor: hand;"
            );
        }
        if (inactiveBtn != null) {
            inactiveBtn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent; " +
                "-fx-border-width: 0 0 3.5 0; -fx-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: bold; " +
                "-fx-padding: 6px 8px; -fx-cursor: hand;"
            );
        }
    }

    // =========================================================================
    // TAB 1: STUDENT ROSTER SETUP, FILTERING & CRUD
    // =========================================================================

    private void setupRosterTable() {
        if (colId != null) colId.setCellValueFactory(c -> c.getValue().idProperty());
        if (colRollNo != null) colRollNo.setCellValueFactory(c -> c.getValue().rollNoProperty());
        if (colName != null) colName.setCellValueFactory(c -> c.getValue().nameProperty());
        if (colEmail != null) colEmail.setCellValueFactory(c -> c.getValue().emailProperty());
        if (colGpa != null) colGpa.setCellValueFactory(c -> c.getValue().gpaProperty().asObject());

        if (colAcademicStanding != null) {
            colAcademicStanding.setCellValueFactory(c -> c.getValue().academicStandingProperty());
            colAcademicStanding.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String standing, boolean empty) {
                    super.updateItem(standing, empty);
                    if (empty || standing == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        Label badge = new Label(standing);
                        String base = "-fx-padding: 3px 10px; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 11px;";
                        switch (standing.toLowerCase()) {
                            case "excellent" -> badge.setStyle(base + " -fx-background-color: #dcfce7; -fx-text-fill: #16a34a;");
                            case "good"      -> badge.setStyle(base + " -fx-background-color: #e0f2fe; -fx-text-fill: #0284c7;");
                            case "average"   -> badge.setStyle(base + " -fx-background-color: #fef3c7; -fx-text-fill: #d97706;");
                            default          -> badge.setStyle(base + " -fx-background-color: #fee2e2; -fx-text-fill: #dc2626;");
                        }
                        setGraphic(badge);
                        setText(null);
                    }
                }
            });
        }

        if (standingCombo != null) {
            standingCombo.setItems(FXCollections.observableArrayList(
                "All Standings", "Excellent", "Good", "Average", "Poor"
            ));
            standingCombo.setValue("All Standings");
        }

        filteredRosterData = new FilteredList<>(masterStudentData, b -> true);
        if (studentTable != null) {
            SortedList<ClassDetailController.StudentModel> sorted = new SortedList<>(filteredRosterData);
            sorted.comparatorProperty().bind(studentTable.comparatorProperty());
            studentTable.setItems(sorted);
            lockTableColumns(studentTable, 0.10, 0.14, 0.21, 0.26, 0.09, 0.196);
        }
    }

    @FXML
    public void handleFilterStudents() {
        if (filteredRosterData == null) return;
        String selectedRoll = rollNoCombo != null ? rollNoCombo.getValue() : "All Roll Nos";
        String selectedStanding = standingCombo != null ? standingCombo.getValue() : "All Standings";
        String keyword = (searchField != null && searchField.getText() != null)
                ? searchField.getText().toLowerCase().trim()
                : "";

        filteredRosterData.setPredicate(student -> {
            boolean matchesRoll = (selectedRoll == null || selectedRoll.equals("All Roll Nos")
                    || student.getRollNo().equalsIgnoreCase(selectedRoll));
            boolean matchesStanding = (selectedStanding == null || selectedStanding.equals("All Standings")
                    || student.getAcademicStanding().equalsIgnoreCase(selectedStanding));
            boolean matchesSearch = keyword.isEmpty()
                    || student.getName().toLowerCase().contains(keyword)
                    || student.getEmail().toLowerCase().contains(keyword)
                    || student.getId().toLowerCase().contains(keyword)
                    || student.getRollNo().toLowerCase().contains(keyword);
            return matchesRoll && matchesStanding && matchesSearch;
        });

        updateMetricsAndFooters();
    }

    @FXML
    public void handleResetFilters() {
        if (rollNoCombo != null) rollNoCombo.setValue("All Roll Nos");
        if (standingCombo != null) standingCombo.setValue("All Standings");
        if (searchField != null) searchField.clear();
        if (filteredRosterData != null) filteredRosterData.setPredicate(p -> true);
        updateMetricsAndFooters();
    }

    @FXML
    public void handleAdminAddStudent(ActionEvent event) {
        Optional<ClassDetailController.StudentModel> result = showStudentFormDialog("Enroll Student", null);
        result.ifPresent(newStudent -> {
            masterStudentData.add(newStudent);

            String logDate = (dailyDatePicker != null && dailyDatePicker.getValue() != null)
                    ? dailyDatePicker.getValue().toString()
                    : LocalDate.now().toString();
            ClassDetailController.DailyLogEntry logEntry = new ClassDetailController.DailyLogEntry(
                    masterDailyLogData.size() + 1,
                    newStudent.getRollNo(),
                    newStudent.getName(),
                    logDate,
                    currentSubjectName,
                    "Present",
                    "Manually enrolled"
            );
            masterDailyLogData.add(logEntry);
            originalDailyStatuses.put(logEntry.getRollNo() + "_" + logEntry.getDate(), logEntry.getStatus());

            refreshRollNoCombo();
            updateMetricsAndFooters();
        });
    }

    @FXML
    public void handleModifyStudent(ActionEvent event) {
        ClassDetailController.StudentModel selected = studentTable != null
                ? studentTable.getSelectionModel().getSelectedItem()
                : null;
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Student Selected",
                    "Please select a student from the Student List table to modify.");
            return;
        }

        Optional<ClassDetailController.StudentModel> updated = showStudentFormDialog("Modify Student", selected);
        updated.ifPresent(modified -> {
            int idx = masterStudentData.indexOf(selected);
            if (idx >= 0) {
                masterStudentData.set(idx, modified);
            }
            refreshRollNoCombo();
            updateMetricsAndFooters();
            if (studentTable != null) studentTable.refresh();
        });
    }

    @FXML
    public void handleRemoveStudent(ActionEvent event) {
        ClassDetailController.StudentModel selected = studentTable != null
                ? studentTable.getSelectionModel().getSelectedItem()
                : null;
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Student Selected",
                    "Please select a student from the Student List table to remove.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove " + selected.getName() + " (" + selected.getRollNo() + ") from this class?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                masterStudentData.remove(selected);
                masterDailyLogData.removeIf(log -> log.getRollNo().equalsIgnoreCase(selected.getRollNo()));
                refreshRollNoCombo();
                updateMetricsAndFooters();
            }
        });
    }

    private Optional<ClassDetailController.StudentModel> showStudentFormDialog(
            String title, ClassDetailController.StudentModel existing) {

        Dialog<ClassDetailController.StudentModel> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        ButtonType saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16, 20, 10, 20));

        TextField idField = new TextField(existing != null ? existing.getId() : String.valueOf(256740 + masterStudentData.size()));
        TextField rollField = new TextField(existing != null ? existing.getRollNo() : "III-CEIT-" + (masterStudentData.size() + 1));
        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        TextField emailField = new TextField(existing != null ? existing.getEmail() : "");
        TextField gpaField = new TextField(existing != null ? String.valueOf(existing.getGpa()) : "3.5");
        TextField attField = new TextField(existing != null ? String.valueOf(existing.getAttendancePercent()) : "100");

        grid.add(new Label("Student ID:"), 0, 0);        grid.add(idField, 1, 0);
        grid.add(new Label("Roll No:"), 0, 1);           grid.add(rollField, 1, 1);
        grid.add(new Label("Full Name:"), 0, 2);         grid.add(nameField, 1, 2);
        grid.add(new Label("Email:"), 0, 3);             grid.add(emailField, 1, 3);
        grid.add(new Label("GPA (0.0 - 4.0):"), 0, 4);   grid.add(gpaField, 1, 4);
        grid.add(new Label("Attendance (%):"), 0, 5);    grid.add(attField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtnType) {
                String id = idField.getText().trim();
                String roll = rollField.getText().trim();
                String name = nameField.getText().trim();
                String email = emailField.getText().trim();
                if (name.isEmpty() || roll.isEmpty()) return null;

                double gpa = 3.0;
                int att = 100;
                try { gpa = Math.max(0.0, Math.min(4.0, Double.parseDouble(gpaField.getText().trim()))); } catch (Exception ignored) {}
                try { att = Math.max(0, Math.min(100, Integer.parseInt(attField.getText().trim()))); } catch (Exception ignored) {}

                return new ClassDetailController.StudentModel(id, roll, name, email, att, gpa);
            }
            return null;
        });

        return dialog.showAndWait();
    }

    // =========================================================================
    // TAB 2: DAILY ATTENDANCE LOG SETUP, FILTERING & ACTIONS
    // =========================================================================

    private void setupDailyLogControlsAndTable() {
        if (dailyDatePicker != null) {
            StringConverter<LocalDate> dateFormatter = new StringConverter<>() {
                private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

                @Override
                public String toString(LocalDate date) {
                    return date != null ? formatter.format(date) : "";
                }

                @Override
                public LocalDate fromString(String string) {
                    if (string != null && !string.trim().isEmpty()) {
                        try {
                            return LocalDate.parse(string, formatter);
                        } catch (Exception e) {
                            return null;
                        }
                    }
                    return null;
                }
            };
            dailyDatePicker.setConverter(dateFormatter);
            dailyDatePicker.setValue(LocalDate.of(2026, 11, 16));
        }

        if (dailyLogTable == null) return;

        dailyLogTable.setEditable(false);

        colNoLog.setCellValueFactory(c -> c.getValue().noProperty().asObject());
        colRollLog.setCellValueFactory(c -> c.getValue().rollNoProperty());
        colNameLog.setCellValueFactory(c -> c.getValue().studentNameProperty());
        colDateLog.setCellValueFactory(c -> c.getValue().dateProperty());
        colSubjLog.setCellValueFactory(c -> c.getValue().subjectProperty());
        colRemarksLog.setCellValueFactory(c -> c.getValue().remarksProperty());

        colStatusLog.setCellValueFactory(c -> c.getValue().statusProperty());
        colStatusLog.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item.equalsIgnoreCase("Present")) {
                    setText("✔ Present");
                    setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
                } else if (item.equalsIgnoreCase("Absent")) {
                    setText("✖ Absent");
                    setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                } else {
                    setText("● " + item);
                    setStyle("-fx-text-fill: #b45309; -fx-font-weight: bold;");
                }
            }
        });

        dailyLogTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ClassDetailController.DailyLogEntry item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("");
                getStyleClass().removeAll("row-present", "row-absent", "row-excused");
                if (item != null && !empty && item.getStatus() != null) {
                    String status = item.getStatus();
                    if (status.equalsIgnoreCase("Present")) {
                        getStyleClass().add("row-present");
                    } else if (status.equalsIgnoreCase("Absent")) {
                        getStyleClass().add("row-absent");
                    } else if (status.equalsIgnoreCase("Excused") || status.equalsIgnoreCase("Excuse")) {
                        getStyleClass().add("row-excused");
                    }
                }
            }
        });

        filteredDailyLogData = new FilteredList<>(masterDailyLogData, b -> true);
        dailyLogTable.setItems(filteredDailyLogData);
        lockTableColumns(dailyLogTable, 0.06, 0.13, 0.19, 0.13, 0.21, 0.14, 0.136);
    }

    @FXML
    public void applyDailyFilters() {
        if (filteredDailyLogData == null) return;

        LocalDate selectedDate = dailyDatePicker != null ? dailyDatePicker.getValue() : null;
        String dateFilterStr = selectedDate != null ? selectedDate.toString() : "";
        String keyword = (dailySearchField != null && dailySearchField.getText() != null)
                ? dailySearchField.getText().trim().toLowerCase()
                : "";

        boolean pChecked = chkPresent != null && chkPresent.isSelected();
        boolean aChecked = chkAbsent != null && chkAbsent.isSelected();
        boolean eChecked = chkExcuse != null && chkExcuse.isSelected();
        boolean anyStatusChecked = pChecked || aChecked || eChecked;

        filteredDailyLogData.setPredicate(entry -> {
            boolean matchesDate = dateFilterStr.isEmpty() || entry.getDate().equals(dateFilterStr);
            boolean matchesSearch = keyword.isEmpty()
                    || entry.getStudentName().toLowerCase().contains(keyword)
                    || entry.getRollNo().toLowerCase().contains(keyword)
                    || entry.getSubject().toLowerCase().contains(keyword);

            boolean matchesStatus = true;
            if (anyStatusChecked) {
                String st = entry.getStatus() != null ? entry.getStatus().toLowerCase() : "";
                matchesStatus = (pChecked && st.equals("present"))
                        || (aChecked && st.equals("absent"))
                        || (eChecked && (st.equals("excused") || st.equals("excuse")));
            }

            return matchesDate && matchesSearch && matchesStatus;
        });

        updateMetricsAndFooters();
    }

    @FXML
    public void handleResetDailyFilters() {
        if (dailyDatePicker != null) dailyDatePicker.setValue(null);
        if (dailySearchField != null) dailySearchField.clear();
        if (chkPresent != null) chkPresent.setSelected(false);
        if (chkAbsent != null) chkAbsent.setSelected(false);
        if (chkExcuse != null) chkExcuse.setSelected(false);
        if (filteredDailyLogData != null) filteredDailyLogData.setPredicate(b -> true);
        updateMetricsAndFooters();
    }

    @FXML
    public void handleToggleDailyStatus() {
        if (dailyLogTable == null) return;
        ClassDetailController.DailyLogEntry selected = dailyLogTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Row Selected",
                    "Please select a student row in the Daily Attendance Log to toggle status.");
            return;
        }

        String curr = selected.getStatus() != null ? selected.getStatus() : "Present";
        if (curr.equalsIgnoreCase("Present")) {
            selected.setStatus("Absent");
            selected.setRemarks("Unexcused");
        } else if (curr.equalsIgnoreCase("Absent")) {
            selected.setStatus("Excused");
            selected.setRemarks("Excused");
        } else {
            selected.setStatus("Present");
            selected.setRemarks("On Time");
        }
        dailyLogTable.refresh();
    }

    @FXML
    public void handleResetDailyStatus() {
        for (ClassDetailController.DailyLogEntry entry : masterDailyLogData) {
            String key = entry.getRollNo() + "_" + entry.getDate();
            if (originalDailyStatuses.containsKey(key)) {
                entry.setStatus(originalDailyStatuses.get(key));
            }
        }
        if (dailyLogTable != null) dailyLogTable.refresh();
    }

    @FXML
    public void handleMarkAllPresent() {
        if (filteredDailyLogData == null) return;
        for (ClassDetailController.DailyLogEntry entry : filteredDailyLogData) {
            entry.setStatus("Present");
            entry.setRemarks("On Time");
        }
        if (dailyLogTable != null) dailyLogTable.refresh();
    }

    @FXML
    public void handleSaveDailyChanges() {
        for (ClassDetailController.DailyLogEntry entry : masterDailyLogData) {
            originalDailyStatuses.put(entry.getRollNo() + "_" + entry.getDate(), entry.getStatus());
        }
        showAlert(Alert.AlertType.INFORMATION, "Attendance Saved",
                "Daily attendance changes have been saved!");
    }

    // =========================================================================
    // DATA INITIALIZATION & METRICS
    // =========================================================================

    public void initClassData(String classKeyOrCode) {
        String lookup = (classKeyOrCode != null && !classKeyOrCode.isBlank()) ? classKeyOrCode.trim() : "IT201";

        ClassesController.ClassItem matched = ClassesController.SHARED_CLASS_DATA.stream()
                .filter(c -> c.getOfferingKey().equalsIgnoreCase(lookup) || c.code.equalsIgnoreCase(lookup))
                .findFirst()
                .orElse(null);

        this.currentClassItem = matched;
        this.currentClassCode = (matched != null) ? matched.code : lookup;
        this.currentSubjectName = (matched != null) ? matched.title : "General Class";
        String offeringKey = (matched != null) ? matched.getOfferingKey() : lookup;
        String majorLabel = (matched != null) ? " · " + matched.major : "";

        if (classNameLabel != null) {
            classNameLabel.setText("Admin Class Management (" + this.currentClassCode + majorLabel + ") - " + this.currentSubjectName);
        }

        // Load lecturer & real MySQL sessions into the Auto-Generate Schedule Builder
        loadScheduleForClass(matched);

        // Load auto-assigned students if this offering was created via Create New Class
        originalDailyStatuses.clear();
        if (ClassRosterStore.hasRoster(offeringKey)) {
            masterStudentData.clear();
            for (ClassRosterStore.RosterRecord r : ClassRosterStore.getRoster(offeringKey)) {
                masterStudentData.add(new ClassDetailController.StudentModel(
                        r.id, r.rollNo, r.name, r.email, r.attendancePercent, r.gpa));
            }
            masterDailyLogData.clear();
            for (ClassRosterStore.DailyLogRecord l : ClassRosterStore.getDailyLogs(offeringKey)) {
                ClassDetailController.DailyLogEntry entry = new ClassDetailController.DailyLogEntry(
                        l.no, l.rollNo, l.studentName, l.date, l.subject, l.status, l.remarks);
                masterDailyLogData.add(entry);
                originalDailyStatuses.put(entry.getRollNo() + "_" + entry.getDate(), entry.getStatus());
            }
            if (!masterDailyLogData.isEmpty() && dailyDatePicker != null) {
                try {
                    dailyDatePicker.setValue(LocalDate.parse(masterDailyLogData.get(0).getDate()));
                } catch (Exception ignored) {}
            }
            refreshRollNoCombo();
            updateMetricsAndFooters();
        } else {
            loadSampleStudentsForClass(this.currentClassCode);
            loadSampleDailyLogsForClass(this.currentSubjectName);
        }

        applyDailyFilters();
    }

    private void loadSampleStudentsForClass(String classCode) {
        masterStudentData.setAll(
            new ClassDetailController.StudentModel("256730", "III-CEIT-1", "Aung Thura", "aungthura@gmail.com", 92, 3.6),
            new ClassDetailController.StudentModel("256731", "III-CEIT-2", "Su Myat Mya", "sumyat@gmail.com", 82, 3.6),
            new ClassDetailController.StudentModel("256732", "III-CEIT-3", "Min Khant", "minkhant@gmail.com", 74, 2.4),
            new ClassDetailController.StudentModel("256733", "III-CEIT-4", "Hnin Wai", "hninwai@gmail.com", 95, 3.9),
            new ClassDetailController.StudentModel("256734", "III-CEIT-5", "Ko Thant", "kothant@gmail.com", 48, 2.1),
            new ClassDetailController.StudentModel("256735", "III-CEIT-6", "Zaw Hein", "zawhein@gmail.com", 98, 4.0),
            new ClassDetailController.StudentModel("256736", "III-CEIT-7", "May Thu", "maythu@gmail.com", 68, 3.2),
            new ClassDetailController.StudentModel("256737", "III-CEIT-8", "Aimon M", "aimonm@gmail.com", 50, 1.8),
            new ClassDetailController.StudentModel("256738", "III-CEIT-9", "Kyaw Zin", "kyawzin@gmail.com", 85, 3.4),
            new ClassDetailController.StudentModel("256739", "III-CEIT-10", "Thiri San", "thirisan@gmail.com", 40, 1.5)
        );

        refreshRollNoCombo();
        updateMetricsAndFooters();
    }

    private void loadSampleDailyLogsForClass(String subjectName) {
        masterDailyLogData.clear();
        String dateStr = "2026-11-16";

        for (int i = 0; i < masterStudentData.size(); i++) {
            ClassDetailController.StudentModel s = masterStudentData.get(i);
            String status;
            String remarks;

            if (s.getAttendancePercent() <= 50) {
                status = "Absent";
                remarks = "Unexcused";
            } else if (s.getAttendancePercent() < 75) {
                status = "Excused";
                remarks = "Medical Leave";
            } else {
                status = "Present";
                remarks = "On Time";
            }

            ClassDetailController.DailyLogEntry entry = new ClassDetailController.DailyLogEntry(
                i + 1, s.getRollNo(), s.getName(), dateStr, subjectName, status, remarks
            );
            masterDailyLogData.add(entry);
            originalDailyStatuses.put(entry.getRollNo() + "_" + entry.getDate(), entry.getStatus());
        }
    }

    private void refreshRollNoCombo() {
        if (rollNoCombo == null) return;
        String current = rollNoCombo.getValue();
        ObservableList<String> rollNos = FXCollections.observableArrayList("All Roll Nos");
        for (ClassDetailController.StudentModel s : masterStudentData) {
            if (!rollNos.contains(s.getRollNo())) {
                rollNos.add(s.getRollNo());
            }
        }
        rollNoCombo.setItems(rollNos);
        rollNoCombo.setValue(current != null && rollNos.contains(current) ? current : "All Roll Nos");
    }

    private void updateMetricsAndFooters() {
        int total = masterStudentData.size();
        int active = 0;
        int atRisk = 0;
        int inactive = 0;

        for (ClassDetailController.StudentModel s : masterStudentData) {
            int att = s.getAttendancePercent();
            if (att <= 50) {
                inactive++;
            } else if (att < 75) {
                atRisk++;
            } else {
                active++;
            }
        }

        if (totalStudentsLabel != null) totalStudentsLabel.setText(String.valueOf(total));
        if (activeStudentsLabel != null) activeStudentsLabel.setText(String.valueOf(active));
        if (atRiskLabel != null) atRiskLabel.setText(String.valueOf(atRisk));
        if (inactiveLabel != null) inactiveLabel.setText(String.valueOf(inactive));

        if (studentCountFooterLabel != null && filteredRosterData != null) {
            int shown = filteredRosterData.size();
            studentCountFooterLabel.setText(shown == 0
                    ? "Showing 0 of " + total + " students"
                    : "Showing 1 - " + shown + " of " + total + " students");
        }

        if (dailyLogFooterLabel != null && filteredDailyLogData != null) {
            int logCount = filteredDailyLogData.size();
            dailyLogFooterLabel.setText("Showing " + logCount + " attendance record" + (logCount == 1 ? "" : "s"));
        }
    }

    @FXML
    private void handleBackToClasses() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AdminClassesView.fxml"));
            Parent classesView = loader.load();

            StackPane contentArea = (StackPane) classNameLabel.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(classesView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // HELPERS & SCHEDULE MODELS
    // =========================================================================

    private int extractPeriodNumber(String periodText) {
        if (periodText == null) return 1;
        for (char ch : periodText.toCharArray()) {
            if (Character.isDigit(ch)) return Character.getNumericValue(ch);
        }
        return 1;
    }

    private int extractDurationCount(String durationText) {
        if (durationText == null || durationText.isEmpty()) return 1;
        char first = durationText.trim().charAt(0);
        return Character.isDigit(first) ? Math.max(1, Math.min(Character.getNumericValue(first), 4)) : 1;
    }

    private int parsePositiveInt(String val, int fallback) {
        try {
            int parsed = Integer.parseInt(val != null ? val.trim() : "");
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void lockTableColumns(TableView<?> table, double... ratios) {
        if (table == null || table.getColumns().size() != ratios.length) return;
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        for (int i = 0; i < ratios.length; i++) {
            TableColumn<?, ?> col = table.getColumns().get(i);
            col.setReorderable(false);
            col.setSortable(false);
            col.minWidthProperty().bind(table.widthProperty().multiply(ratios[i]));
            col.maxWidthProperty().bind(table.widthProperty().multiply(ratios[i]));
            col.prefWidthProperty().bind(table.widthProperty().multiply(ratios[i]));
        }
    }

    private static class WeeklySlot {
        final DayOfWeek day;
        final int startPeriod;
        final int duration;

        WeeklySlot(DayOfWeek day, int startPeriod, int duration) {
            this.day = day;
            this.startPeriod = startPeriod;
            this.duration = duration;
        }

        String formatPeriods() {
            int end = startPeriod + duration - 1;
            return duration == 1 ? "Period " + startPeriod : "Periods " + startPeriod + "–" + end;
        }

        String toShortSummary() {
            String dayShort = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            int end = startPeriod + duration - 1;
            return duration == 1 ? dayShort + " P" + startPeriod : dayShort + " P" + startPeriod + "–" + end;
        }

        String toChipLabel() {
            String dayShort = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            int end = startPeriod + duration - 1;
            return dayShort + " · " + formatPeriods() + " (" + PERIOD_START_TIMES[startPeriod] + " – " + PERIOD_END_TIMES[end] + ")";
        }

        String toBadgeSchedule() {
            String dayShort = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            int end = startPeriod + duration - 1;
            return dayShort + " " + PERIOD_START_24H[startPeriod] + " - " + PERIOD_END_24H[end];
        }
    }

    public static class SessionRow {
        private final StringProperty sessionNo;
        private final StringProperty date;
        private final StringProperty day;
        private final int periodNumber;
        private final StringProperty periodLabel;

        public SessionRow(String sessionNo, String date, String day, int periodNumber, String periodLabel) {
            this.sessionNo = new SimpleStringProperty(sessionNo);
            this.date = new SimpleStringProperty(date);
            this.day = new SimpleStringProperty(day);
            this.periodNumber = periodNumber;
            this.periodLabel = new SimpleStringProperty(periodLabel);
        }

        public String getSessionNo() { return sessionNo.get(); }
        public void setSessionNo(String val) { sessionNo.set(val); }
        public StringProperty sessionNoProperty() { return sessionNo; }

        public String getDate() { return date.get(); }
        public StringProperty dateProperty() { return date; }

        public String getDay() { return day.get(); }
        public StringProperty dayProperty() { return day; }

        public int getPeriodNumber() { return periodNumber; }

        public String getPeriodLabel() { return periodLabel.get(); }
        public StringProperty periodLabelProperty() { return periodLabel; }
    }
    
    @FXML
    private void handleToggleScheduleSection(ActionEvent event) {
        isScheduleExpanded = !isScheduleExpanded;

        if (scheduleBodyPane != null) {
            scheduleBodyPane.setVisible(isScheduleExpanded);
            scheduleBodyPane.setManaged(isScheduleExpanded);
        }
        if (toggleScheduleBtn != null) {
            if (isScheduleExpanded) {
                toggleScheduleBtn.setText("▲  Collapse");
                toggleScheduleBtn.setStyle(
                    "-fx-background-color: #1b315e; -fx-text-fill: white; -fx-font-size: 12px; " +
                    "-fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 6px 14px; " +
                    "-fx-alignment: CENTER_LEFT; -fx-cursor: hand;"
                );
            } else {
                toggleScheduleBtn.setText("▼  Edit Schedule");
                toggleScheduleBtn.setStyle(
                    "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 12px; " +
                    "-fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 6px 14px; " +
                    "-fx-alignment: CENTER_LEFT; -fx-cursor: hand; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(37,99,235,0.28), 6, 0, 0, 2);"
                );
            }
        }
        if (scheduleHeaderBar != null) {
            scheduleHeaderBar.setStyle(isScheduleExpanded
                    ? "-fx-border-color: transparent transparent #e2e8f0 transparent; -fx-border-width: 0 0 1.5px 0; -fx-padding: 0 0 8px 0;"
                    : "");
        }
    }

    @FXML
    private void handleResetSchedule(ActionEvent event) {
        if (currentClassItem != null) {
            loadScheduleForClass(currentClassItem);
        }
    }
}