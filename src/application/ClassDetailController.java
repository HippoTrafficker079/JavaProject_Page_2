package application;

import application.dao.CourseEnrollmentDAO;
import application.dao.CourseOfferingDAO;
import application.dao.DatabaseConnector;
import application.dao.DepartmentDAO;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClassDetailController {

    @FXML
    private VBox rootPane;
    @FXML
    private Label classNameLabel;
    @FXML
    private Label totalStudentsLabel;
    @FXML
    private Label activeStudentsLabel;
    @FXML
    private Label atRiskLabel;
    @FXML
    private Label inactiveLabel;

    // Navigation Tabs & Containers
    @FXML
    private Button tabRosterBtn;
    @FXML
    private Button tabAttendanceBtn;
    @FXML
    private VBox rosterSection;
    @FXML
    private VBox attendanceSection;

    // Tab 1: Student Roster Controls & Table
    @FXML
    private ComboBox<String> rollNoFilterCombo;
    @FXML
    private TextField searchField;
    @FXML
    private Label footerCountLabel;
    @FXML
    private ComboBox<String> standingFilterCombo;

    @FXML
    private TableView<StudentModel> studentTable;
    @FXML
    private TableColumn<StudentModel, String> colId;
    @FXML
    private TableColumn<StudentModel, String> colRollNo;
    @FXML
    private TableColumn<StudentModel, String> colName;
    @FXML
    private TableColumn<StudentModel, String> colEmail;
    @FXML
    private TableColumn<StudentModel, Double> colGpa;
    @FXML
    private TableColumn<StudentModel, String> colAcademicStanding;

    // Tab 2: Daily Attendance Log Controls & Table (Read-Only for Lecturer)
    @FXML
    private DatePicker dailyDatePicker;
    @FXML
    private TextField dailySearchField;
    @FXML
    private CheckBox chkPresent;
    @FXML
    private CheckBox chkAbsent;
    @FXML
    private CheckBox chkExcuse;
    @FXML
    private Label dailyLogFooterLabel;

    @FXML
    private TableView<DailyLogEntry> dailyLogTable;
    @FXML
    private TableColumn<DailyLogEntry, Integer> colNoLog;
    @FXML
    private TableColumn<DailyLogEntry, String> colRollLog;
    @FXML
    private TableColumn<DailyLogEntry, String> colNameLog;
    @FXML
    private TableColumn<DailyLogEntry, String> colDateLog;
    @FXML
    private TableColumn<DailyLogEntry, String> colSubjLog;
    @FXML
    private TableColumn<DailyLogEntry, String> colStatusLog;
    @FXML
    private TableColumn<DailyLogEntry, String> colRemarksLog;
    
    @FXML
    private ComboBox<String> dailyPeriodCombo;

    private final ObservableList<StudentModel> masterStudentData = FXCollections.observableArrayList();
    private FilteredList<StudentModel> filteredRosterData;

    private final ObservableList<DailyLogEntry> masterDailyLogData = FXCollections.observableArrayList();
    private FilteredList<DailyLogEntry> filteredDailyLogData;

    private ClassesController.ClassItem currentClassItem;
    private int currentOfferingId = -1;
    private String currentClassCode = "IT201";
    private String currentSubjectName = "Data Structures and Algorithms";

    @FXML
    public void initialize() {
        if (rootPane != null) {
            rootPane.setOnMouseClicked(event -> {
                if (studentTable != null)
                    studentTable.getSelectionModel().clearSelection();
                if (dailyLogTable != null)
                    dailyLogTable.getSelectionModel().clearSelection();
                rootPane.requestFocus();
            });
        }

        setupRosterTable();
        setupDailyLogControlsAndTable();
        switchToRosterTab();
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
                    "-fx-background-color: transparent; -fx-border-color: transparent transparent #1b315e transparent; "
                            +
                            "-fx-border-width: 0 0 3.5 0; -fx-text-fill: #0f172a; -fx-font-size: 14px; -fx-font-weight: 800; "
                            +
                            "-fx-padding: 6px 8px; -fx-cursor: hand;");
        }
        if (inactiveBtn != null) {
            inactiveBtn.setStyle(
                    "-fx-background-color: transparent; -fx-border-color: transparent; " +
                            "-fx-border-width: 0 0 3.5 0; -fx-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: bold; "
                            +
                            "-fx-padding: 6px 8px; -fx-cursor: hand;");
        }
    }

    // =========================================================================
    // TAB 1: STUDENT ROSTER SETUP & FILTERING
    // =========================================================================

    private void setupRosterTable() {
        if (colId != null)
            colId.setCellValueFactory(c -> c.getValue().idProperty());
        if (colRollNo != null)
            colRollNo.setCellValueFactory(c -> c.getValue().rollNoProperty());
        if (colName != null)
            colName.setCellValueFactory(c -> c.getValue().nameProperty());
        if (colEmail != null)
            colEmail.setCellValueFactory(c -> c.getValue().emailProperty());
        if (colGpa != null)
            colGpa.setCellValueFactory(c -> c.getValue().gpaProperty().asObject());

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
                            case "excellent" ->
                                badge.setStyle(base + " -fx-background-color: #dcfce7; -fx-text-fill: #16a34a;");
                            case "good" ->
                                badge.setStyle(base + " -fx-background-color: #e0f2fe; -fx-text-fill: #0284c7;");
                            case "average" ->
                                badge.setStyle(base + " -fx-background-color: #fef3c7; -fx-text-fill: #d97706;");
                            default -> badge.setStyle(base + " -fx-background-color: #fee2e2; -fx-text-fill: #dc2626;");
                        }
                        setGraphic(badge);
                        setText(null);
                    }
                }
            });
        }

        if (standingFilterCombo != null) {
            standingFilterCombo.setItems(FXCollections.observableArrayList(
                    "All Standings", "Excellent", "Good", "Average", "Poor"));
            standingFilterCombo.setValue("All Standings");
        }

        filteredRosterData = new FilteredList<>(masterStudentData, b -> true);
        if (studentTable != null) {
            SortedList<StudentModel> sorted = new SortedList<>(filteredRosterData);
            sorted.comparatorProperty().bind(studentTable.comparatorProperty());
            studentTable.setItems(sorted);
            lockTableColumns(studentTable, 0.10, 0.14, 0.21, 0.26, 0.09, 0.196);
        }
    }

    @FXML
    public void handleFilter() {
        if (filteredRosterData == null)
            return;
        String selectedRoll = rollNoFilterCombo != null ? rollNoFilterCombo.getValue() : "All Roll Nos";
        String selectedStanding = standingFilterCombo != null ? standingFilterCombo.getValue() : "All Standings";
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
    public void handleReset() {
        if (rollNoFilterCombo != null)
            rollNoFilterCombo.setValue("All Roll Nos");
        if (standingFilterCombo != null)
            standingFilterCombo.setValue("All Standings");
        if (searchField != null)
            searchField.clear();
        if (filteredRosterData != null)
            filteredRosterData.setPredicate(p -> true);
        updateMetricsAndFooters();
    }

    // =========================================================================
    // TAB 2: DAILY ATTENDANCE LOG SETUP & FILTERING (READ-ONLY)
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
            dailyDatePicker.setValue(null);
        }

        if (dailyLogTable == null)
            return;

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
            protected void updateItem(DailyLogEntry item, boolean empty) {
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

        if (dailyPeriodCombo != null) {
            dailyPeriodCombo.valueProperty().addListener((obs, o, n) -> applyDailyFilters());
        }
    }

    @FXML
    public void applyDailyFilters() {
        if (filteredDailyLogData == null)
            return;

        LocalDate selectedDate = dailyDatePicker != null ? dailyDatePicker.getValue() : null;
        String dateFilterStr = selectedDate != null ? selectedDate.toString() : "";
        String selectedPeriod = dailyPeriodCombo != null ? dailyPeriodCombo.getValue() : null;
        Integer targetPeriod = parseSinglePeriodFilter(selectedPeriod);

        String keyword = (dailySearchField != null && dailySearchField.getText() != null)
                ? dailySearchField.getText().trim().toLowerCase()
                : "";

        boolean pChecked = chkPresent != null && chkPresent.isSelected();
        boolean aChecked = chkAbsent != null && chkAbsent.isSelected();
        boolean eChecked = chkExcuse != null && chkExcuse.isSelected();
        boolean anyStatusChecked = pChecked || aChecked || eChecked;

        filteredDailyLogData.setPredicate(entry -> {
            boolean matchesDate = dateFilterStr.isEmpty() || entry.getDate().equals(dateFilterStr);
            boolean matchesPeriod = (targetPeriod == null) || (entry.getPeriod() == targetPeriod);
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

            return matchesDate && matchesPeriod && matchesSearch && matchesStatus;
        });

        updateMetricsAndFooters();
    }

    private Integer parseSinglePeriodFilter(String selected) {
        if (selected == null || !selected.startsWith("Period ")) {
            return null; // "Periods 3 & 6" or "All Periods" -> show all periods of this class
        }
        try {
            return Integer.parseInt(selected.replace("Period ", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @FXML
    public void handleResetDailyFilters() {
        if (dailyDatePicker != null)
            dailyDatePicker.setValue(null);
        if (dailyPeriodCombo != null && !dailyPeriodCombo.getItems().isEmpty())
            dailyPeriodCombo.getSelectionModel().selectFirst();
        if (dailySearchField != null)
            dailySearchField.clear();
        if (chkPresent != null)
            chkPresent.setSelected(false);
        if (chkAbsent != null)
            chkAbsent.setSelected(false);
        if (chkExcuse != null)
            chkExcuse.setSelected(false);
        if (filteredDailyLogData != null)
            filteredDailyLogData.setPredicate(b -> true);
        updateMetricsAndFooters();
    }

    // =========================================================================
    // DATA INITIALIZATION & METRICS
    // =========================================================================

    public void initClassData(String classKeyOrCode) {
        initClassData(-1, classKeyOrCode);
    }

    public void initClassData(int offeringId, String fallbackKeyOrCode) {
        ClassesController.ClassItem matched = null;

        if (offeringId > 0) {
            matched = ClassesController.SHARED_CLASS_DATA.stream()
                    .filter(c -> c.getOfferingId() == offeringId)
                    .findFirst()
                    .orElse(null);
        }

        String lookup = (fallbackKeyOrCode != null && !fallbackKeyOrCode.isBlank())
                ? fallbackKeyOrCode.trim()
                : "IT201";

        if (matched == null) {
            matched = ClassesController.SHARED_CLASS_DATA.stream()
                    .filter(c -> String.valueOf(c.getOfferingId()).equals(lookup)
                            || c.getOfferingKey().equalsIgnoreCase(lookup)
                            || c.code.equalsIgnoreCase(lookup))
                    .findFirst()
                    .orElse(null);
        }

        int resolvedOfferingId = (offeringId > 0)
                ? offeringId
                : (matched != null ? matched.getOfferingId() : -1);

        applyMatchedClassItem(matched, resolvedOfferingId, lookup);
    }

    private void applyMatchedClassItem(ClassesController.ClassItem matched, int offeringId, String fallbackLookup) {
        this.currentClassItem = matched;
        this.currentOfferingId = offeringId;
        this.currentClassCode = (matched != null) ? matched.code : fallbackLookup;
        this.currentSubjectName = (matched != null) ? matched.title : "General Class";
        String offeringKey = (matched != null) ? matched.getOfferingKey() : fallbackLookup;
        String majorLabel = (matched != null) ? " · " + matched.major : "";

        if (classNameLabel != null) {
            classNameLabel
                    .setText("Class Details (" + this.currentClassCode + majorLabel + ") - " + this.currentSubjectName);
        }

        // Always prefer live MySQL data when offeringId is available
        if (this.currentOfferingId > 0) {
            loadStudentsFromDatabase(this.currentOfferingId);
            loadDailyLogsFromDatabase(this.currentOfferingId);
        } else if (ClassRosterStore.hasRoster(offeringKey)) {
            masterStudentData.clear();
            for (ClassRosterStore.RosterRecord r : ClassRosterStore.getRoster(offeringKey)) {
                masterStudentData.add(new StudentModel(r.id, r.rollNo, r.name, r.email, r.attendancePercent, r.gpa));
            }
            masterDailyLogData.clear();
            for (ClassRosterStore.DailyLogRecord l : ClassRosterStore.getDailyLogs(offeringKey)) {
                masterDailyLogData
                        .add(new DailyLogEntry(l.no, l.rollNo, l.studentName, l.date, l.subject, l.status, l.remarks));
            }
            if (!masterDailyLogData.isEmpty() && dailyDatePicker != null) {
                try {
                    dailyDatePicker.setValue(LocalDate.parse(masterDailyLogData.get(0).getDate()));
                } catch (Exception ignored) {
                }
            }
            refreshRollNoCombo();
            updateMetricsAndFooters();
        } else {
            masterStudentData.clear();
            masterDailyLogData.clear();
            refreshRollNoCombo();
            updateMetricsAndFooters();
        }

        applyDailyFilters();
    }

    private void loadStudentsFromDatabase(int offeringId) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            DepartmentDAO departmentDAO = new DepartmentDAO();
            int deptId = departmentDAO.getDeptIdByUserId(UserSession.userId);
            CourseEnrollmentDAO courseEnrollmentDAO = new CourseEnrollmentDAO();
            List<StudentModel> loadedList = courseEnrollmentDAO.getAllEnrolledStudent(offeringId, deptId);

            // Calculate real per-student attendance % from class_session + attendance
            CourseOfferingDAO offeringDAO = new CourseOfferingDAO();
            Map<String, Integer> attByStudent = offeringDAO.getAttendancePercentByStudent(offeringId);
            for (StudentModel s : loadedList) {
                if (attByStudent.containsKey(s.getId())) {
                    s.setAttendancePercent(attByStudent.get(s.getId()));
                }
            }

            masterStudentData.setAll(loadedList);
        } catch (SQLException e) {
            e.printStackTrace();
            masterStudentData.clear();
        }
        refreshRollNoCombo();
        updateMetricsAndFooters();
    }
    
    private void loadDailyLogsFromDatabase(int offeringId) {
        masterDailyLogData.clear();

        CourseOfferingDAO offeringDAO = new CourseOfferingDAO();
        for (CourseOfferingDAO.DailyAttendanceLogRecord l : offeringDAO.getDailyLogsByOfferingId(offeringId)) {
            masterDailyLogData.add(new DailyLogEntry(
                    l.no(),
                    l.rollNo(),
                    l.studentName(),
                    l.date(),
                    l.period(),
                    l.subject(),
                    l.status(),
                    l.remarks()));
        }

        refreshDailyPeriodCombo(offeringDAO.getDistinctPeriodsForOffering(offeringId));

        if (dailyDatePicker != null) {
            if (!masterDailyLogData.isEmpty()) {
                try {
                    dailyDatePicker.setValue(LocalDate.parse(masterDailyLogData.get(0).getDate()));
                } catch (Exception ignored) {
                }
            } else {
                dailyDatePicker.setValue(null);
            }
        }
    }

    private void refreshDailyPeriodCombo(List<Integer> periods) {
        if (dailyPeriodCombo == null)
            return;

        ObservableList<String> options = FXCollections.observableArrayList();
        if (periods == null || periods.isEmpty()) {
            options.add("No Sessions");
        } else if (periods.size() == 1) {
            options.add("Period " + periods.get(0));
        } else {
            String joined = (periods.size() == 2)
                    ? periods.get(0) + " & " + periods.get(1)
                    : periods.subList(0, periods.size() - 1).stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", "))
                            + " & " + periods.get(periods.size() - 1);
            options.add("Periods " + joined);
            for (Integer p : periods) {
                options.add("Period " + p);
            }
        }

        dailyPeriodCombo.setItems(options);
        dailyPeriodCombo.getSelectionModel().selectFirst();
    }

    private void refreshRollNoCombo() {
        if (rollNoFilterCombo == null)
            return;
        String current = rollNoFilterCombo.getValue();
        ObservableList<String> rollNos = FXCollections.observableArrayList("All Roll Nos");
        for (StudentModel s : masterStudentData) {
            if (!rollNos.contains(s.getRollNo())) {
                rollNos.add(s.getRollNo());
            }
        }
        rollNoFilterCombo.setItems(rollNos);
        rollNoFilterCombo.setValue(current != null && rollNos.contains(current) ? current : "All Roll Nos");
    }

    private void updateMetricsAndFooters() {
        int total = masterStudentData.size();
        int active = 0;
        int atRisk = 0;
        int inactive = 0;

        for (StudentModel s : masterStudentData) {
            int att = s.getAttendancePercent();
            if (att <= 50) {
                inactive++;
            } else if (att < 75) {
                atRisk++;
            } else {
                active++;
            }
        }

        if (totalStudentsLabel != null)
            totalStudentsLabel.setText(String.valueOf(total));
        if (activeStudentsLabel != null)
            activeStudentsLabel.setText(String.valueOf(active));
        if (atRiskLabel != null)
            atRiskLabel.setText(String.valueOf(atRisk));
        if (inactiveLabel != null)
            inactiveLabel.setText(String.valueOf(inactive));

        if (footerCountLabel != null && filteredRosterData != null) {
            int shown = filteredRosterData.size();
            footerCountLabel.setText(shown == 0
                    ? "Showing 0 of " + total + " students"
                    : "Showing 1 - " + shown + " of " + total + " students");
        }

        if (dailyLogFooterLabel != null && filteredDailyLogData != null) {
            int logCount = filteredDailyLogData.size();
            dailyLogFooterLabel.setText("Showing " + logCount + " attendance record" + (logCount == 1 ? "" : "s"));
        }
    }

    @FXML
    public void handleRemoveStudent(ActionEvent event) {
        StudentModel selected = studentTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Student Selected");
            alert.setHeaderText(null);
            alert.setContentText("Please select a student from the Student List table to remove.");
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove " + selected.getName() + " (" + selected.getRollNo() + ") from this class?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText(null);

        confirm.showAndWait().ifPresent(res -> {
            if (res != ButtonType.YES) {
                return;
            }

            try {
                int studentId = Integer.parseInt(selected.getId());
                CourseEnrollmentDAO courseEnrollmentDAO = new CourseEnrollmentDAO();
                int rowAffected = courseEnrollmentDAO.removeStudentFromClass(studentId, currentOfferingId);
                if (rowAffected > 0) {
                    masterStudentData.remove(selected);
                    masterDailyLogData.removeIf(log -> log.getRollNo().equalsIgnoreCase(selected.getRollNo()));
                    refreshRollNoCombo();
                    updateMetricsAndFooters();
                    if (studentTable != null) {
                        studentTable.refresh();
                    }
                } else {
                    showAlert(Alert.AlertType.WARNING, "Not Removed",
                            "No matching active enrollment was updated in the database.");
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Student ID",
                        "Could not parse student ID: " + selected.getId());
            }
        });
    }

    @FXML
    private void handleBackToClasses() {
        try {
        	StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
        	if (ViewNavigationHistory.goBack(contentArea)) {
        	    return;
        	}
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ClassesView.fxml"));
            Parent classesView = loader.load();

            if (contentArea != null) {
                contentArea.getChildren().setAll(classesView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void lockTableColumns(TableView<?> table, double... ratios) {
        if (table == null || table.getColumns().size() != ratios.length)
            return;
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // =========================================================================
    // MODELS
    // =========================================================================

    public static class StudentModel {
        private final SimpleStringProperty id;
        private final SimpleStringProperty rollNo;
        private final SimpleStringProperty name;
        private final SimpleStringProperty email;
        private final SimpleDoubleProperty gpa;
        private final SimpleStringProperty academicStanding;
        private int attendancePercent;

        public StudentModel(String id, String rollNo, String name, String email, int attendancePercent, double gpa) {
            this.id = new SimpleStringProperty(id);
            this.rollNo = new SimpleStringProperty(rollNo);
            this.name = new SimpleStringProperty(name);
            this.email = new SimpleStringProperty(email);
            this.attendancePercent = attendancePercent;
            this.gpa = new SimpleDoubleProperty(gpa);
            this.academicStanding = new SimpleStringProperty(computeStandingFromGpa(gpa));
        }

        private static String computeStandingFromGpa(double gpa) {
            if (gpa >= 3.8)
                return "Excellent";
            if (gpa >= 3.0)
                return "Good";
            if (gpa >= 2.0)
                return "Average";
            return "Poor";
        }

        public String getId() {
            return id.get();
        }

        public SimpleStringProperty idProperty() {
            return id;
        }

        public String getRollNo() {
            return rollNo.get();
        }

        public SimpleStringProperty rollNoProperty() {
            return rollNo;
        }

        public String getName() {
            return name.get();
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public String getEmail() {
            return email.get();
        }

        public SimpleStringProperty emailProperty() {
            return email;
        }

        public double getGpa() {
            return gpa.get();
        }

        public SimpleDoubleProperty gpaProperty() {
            return gpa;
        }

        public String getAcademicStanding() {
            return academicStanding.get();
        }

        public SimpleStringProperty academicStandingProperty() {
            return academicStanding;
        }

        public int getAttendancePercent() {
            return attendancePercent;
        }

        public void setAttendancePercent(int attendancePercent) {
            this.attendancePercent = attendancePercent;
        }
    }

    public static class DailyLogEntry {
        private final SimpleIntegerProperty no;
        private final SimpleStringProperty rollNo;
        private final SimpleStringProperty studentName;
        private final SimpleStringProperty date;
        private final SimpleIntegerProperty period;
        private final SimpleStringProperty subject;
        private final SimpleStringProperty status;
        private final SimpleStringProperty remarks;

        public DailyLogEntry(int no, String rollNo, String studentName, String date,
                String subject, String status, String remarks) {
            this(no, rollNo, studentName, date, 0, subject, status, remarks);
        }

        public DailyLogEntry(int no, String rollNo, String studentName, String date, int period,
                String subject, String status, String remarks) {
            this.no = new SimpleIntegerProperty(no);
            this.rollNo = new SimpleStringProperty(rollNo);
            this.studentName = new SimpleStringProperty(studentName);
            this.date = new SimpleStringProperty(date);
            this.period = new SimpleIntegerProperty(period);
            this.subject = new SimpleStringProperty(subject);
            this.status = new SimpleStringProperty(status);
            this.remarks = new SimpleStringProperty(remarks);
        }

        public int getNo() { return no.get(); }
        
        public SimpleIntegerProperty noProperty() { return no; }
        
        public String getRollNo() { return rollNo.get(); }
        
        public SimpleStringProperty rollNoProperty() { return rollNo; }
        
        public String getStudentName() { return studentName.get(); }
        
        public SimpleStringProperty studentNameProperty() { return studentName; }
        
        public String getDate() { return date.get(); }
        
        public SimpleStringProperty dateProperty() { return date; }
        
        public int getPeriod() { return period.get(); }
        
        public SimpleIntegerProperty periodProperty() { return period; }
        
        public String getSubject() { return subject.get(); }
        
        public SimpleStringProperty subjectProperty() { return subject; }
        
        public String getStatus() { return status.get(); }
        
        public void setStatus(String val) { status.set(val); }
        
        public SimpleStringProperty statusProperty() { return status; }
        
        public String getRemarks() { return remarks.get(); }
        
        public void setRemarks(String val) { remarks.set(val); }
        
        public SimpleStringProperty remarksProperty() { return remarks; }
    }
    private void setPillTabActive(Button activeBtn, Button inactiveBtn) {
        if (activeBtn != null) {
            activeBtn.setStyle("");
            activeBtn.getStyleClass().removeAll("tab-pill-active", "tab-pill-inactive");
            activeBtn.getStyleClass().add("tab-pill-active");
        }
        if (inactiveBtn != null) {
            inactiveBtn.setStyle("");
            inactiveBtn.getStyleClass().removeAll("tab-pill-active", "tab-pill-inactive");
            inactiveBtn.getStyleClass().add("tab-pill-inactive");
        }
    }
}