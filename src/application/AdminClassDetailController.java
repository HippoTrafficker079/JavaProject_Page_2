package application;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdminClassDetailController {

    @FXML private VBox rootPane;
    @FXML private Label classNameLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label activeStudentsLabel;
    @FXML private Label atRiskLabel;
    @FXML private Label inactiveLabel;

    // Course Allocation & Schedule
    @FXML private ComboBox<String> assignTeacherCombo;
    @FXML private TextField scheduleField;

    // Navigation Tabs
    @FXML private Button tabRosterBtn;
    @FXML private Button tabAttendanceBtn;
    @FXML private VBox rosterSection;
    @FXML private VBox attendanceSection;

    // Tab 1: Student Roster
    @FXML private ComboBox<String> rollNoCombo;
    @FXML private ComboBox<String> standingCombo;
    @FXML private TextField searchField;
    @FXML private Label studentCountFooterLabel;

    @FXML private TableView<StudentRecord> studentTable;
    @FXML private TableColumn<StudentRecord, String> colId;
    @FXML private TableColumn<StudentRecord, String> colRollNo;
    @FXML private TableColumn<StudentRecord, String> colName;
    @FXML private TableColumn<StudentRecord, String> colEmail;
    @FXML private TableColumn<StudentRecord, Double> colGpa;
    @FXML private TableColumn<StudentRecord, String> colAcademicStanding;

    // Tab 2: Daily Attendance Log
    @FXML private DatePicker dailyDatePicker;
    @FXML private TextField dailySearchField;
    @FXML private CheckBox chkPresent;
    @FXML private CheckBox chkAbsent;
    @FXML private CheckBox chkExcuse;
    @FXML private Label dailyLogFooterLabel;

    @FXML private TableView<DailyLogEntry> dailyLogTable;
    @FXML private TableColumn<DailyLogEntry, Integer> colNoLog;
    @FXML private TableColumn<DailyLogEntry, String> colRollLog;
    @FXML private TableColumn<DailyLogEntry, String> colNameLog;
    @FXML private TableColumn<DailyLogEntry, String> colDateLog;
    @FXML private TableColumn<DailyLogEntry, String> colSubjLog;
    @FXML private TableColumn<DailyLogEntry, String> colStatusLog;
    @FXML private TableColumn<DailyLogEntry, String> colRemarksLog;

    private String currentClassCode = "IT201";
    private String currentCourseTitle = "Data Structures and Algorithms";
    private String currentMajor = "CEIT";
    private String currentOfferingKey = "";
    private ClassesController.ClassItem currentClassItem;

    private final ObservableList<StudentRecord> masterStudentData = FXCollections.observableArrayList();
    private FilteredList<StudentRecord> filteredStudentData;

    private final ObservableList<DailyLogEntry> masterDailyLogData = FXCollections.observableArrayList();
    private FilteredList<DailyLogEntry> filteredDailyLogData;
    private final Map<String, String> originalDailyStatuses = new HashMap<>();

    @FXML
    public void initialize() {
        setupStudentTable();
        setupDailyLogControlsAndTable();
        switchToRosterTab();
    }

    public void initClassData(String classKeyOrCode) {
        String lookup = (classKeyOrCode != null && !classKeyOrCode.isBlank()) ? classKeyOrCode.trim() : "IT201";

        currentClassItem = ClassesController.SHARED_CLASS_DATA.stream()
                .filter(c -> c.getOfferingKey().equalsIgnoreCase(lookup) || c.code.equalsIgnoreCase(lookup))
                .findFirst()
                .orElse(null);

        if (currentClassItem != null) {
            this.currentClassCode = currentClassItem.code;
            this.currentCourseTitle = currentClassItem.title;
            this.currentMajor = currentClassItem.major;
            this.currentOfferingKey = currentClassItem.getOfferingKey();
        } else {
            this.currentClassCode = lookup;
            this.currentOfferingKey = lookup;
        }

        if (classNameLabel != null) {
            classNameLabel.setText("Admin Class Management (" + currentClassCode + " · " + currentMajor + ")");
        }

        // Load all faculty across departments so cross-major reassignment is supported
        if (assignTeacherCombo != null) {
            List<String> teachers = TeacherDirectoryStore.getTeacherNamesByDept(null);
            assignTeacherCombo.setItems(FXCollections.observableArrayList(teachers));
            if (currentClassItem != null && currentClassItem.lecturer != null) {
                String cleanLecturer = currentClassItem.lecturer.replace(" (Assigned)", "").trim();
                if (!teachers.contains(cleanLecturer)) {
                    assignTeacherCombo.getItems().add(cleanLecturer);
                }
                assignTeacherCombo.setValue(cleanLecturer);
            } else if (!teachers.isEmpty()) {
                assignTeacherCombo.setValue(teachers.get(0));
            }
        }

        if (scheduleField != null && currentClassItem != null) {
            scheduleField.setText(currentClassItem.schedule);
        }

        loadSampleRosterAndAttendance();
    }

    private void loadSampleRosterAndAttendance() {
        masterStudentData.clear();
        masterDailyLogData.clear();
        originalDailyStatuses.clear();

        // Load auto-assigned students & daily logs if this offering was created via Create New Class
        if (ClassRosterStore.hasRoster(currentOfferingKey)) {
            for (ClassRosterStore.RosterRecord r : ClassRosterStore.getRoster(currentOfferingKey)) {
                String standing = r.gpa >= 3.0 ? "Active" : (r.gpa >= 2.2 ? "At Risk" : "Inactive");
                masterStudentData.add(new StudentRecord(r.id, r.rollNo, r.name, r.email, r.gpa, standing));
            }

            for (ClassRosterStore.DailyLogRecord l : ClassRosterStore.getDailyLogs(currentOfferingKey)) {
                DailyLogEntry entry = new DailyLogEntry(
                        l.no, l.rollNo, l.studentName, l.date, l.subject, l.status, l.remarks
                );
                masterDailyLogData.add(entry);
                originalDailyStatuses.put(entry.getRollNo() + "_" + entry.getDate(), entry.getStatus());
            }

            if (!masterDailyLogData.isEmpty() && dailyDatePicker != null) {
                try {
                    dailyDatePicker.setValue(LocalDate.parse(masterDailyLogData.get(0).getDate()));
                } catch (Exception ignored) {}
            }
        } else {
            // Default sample roster for pre-seeded classes
            String prefix = "III-" + (currentMajor != null ? currentMajor.toUpperCase() : "CEIT") + "-";
            String emailPrefix = "iii" + (currentMajor != null ? currentMajor.toLowerCase() : "ceit");

            masterStudentData.setAll(
                new StudentRecord("STD-001", prefix + "1", "Aung Thura",  emailPrefix + "1@ytu.edu.mm", 3.85, "Active"),
                new StudentRecord("STD-002", prefix + "2", "Su Myat Mya", emailPrefix + "2@ytu.edu.mm", 3.72, "Active"),
                new StudentRecord("STD-003", prefix + "3", "Min Khant",   emailPrefix + "3@ytu.edu.mm", 3.40, "Active"),
                new StudentRecord("STD-004", prefix + "4", "Hnin Wai",    emailPrefix + "4@ytu.edu.mm", 3.65, "Active"),
                new StudentRecord("STD-005", prefix + "5", "Ko Thant",    emailPrefix + "5@ytu.edu.mm", 2.45, "At Risk"),
                new StudentRecord("STD-006", prefix + "6", "Zaw Hein",    emailPrefix + "6@ytu.edu.mm", 3.90, "Active"),
                new StudentRecord("STD-007", prefix + "7", "May Thu",     emailPrefix + "7@ytu.edu.mm", 2.10, "Inactive"),
                new StudentRecord("STD-008", prefix + "8", "Kyaw Zin",    emailPrefix + "8@ytu.edu.mm", 2.60, "At Risk")
            );

            String logDate = (dailyDatePicker != null && dailyDatePicker.getValue() != null)
                    ? dailyDatePicker.getValue().toString()
                    : "2026-11-16";

            int rowNum = 1;
            for (StudentRecord s : masterStudentData) {
                String status = "Present";
                String remarks = "On Time";
                if (rowNum == 3) { status = "Excused"; remarks = "Medical Leave"; }
                else if (rowNum == 5) { status = "Absent"; remarks = "Unexcused"; }

                DailyLogEntry entry = new DailyLogEntry(rowNum, s.getRollNo(), s.getName(), logDate, currentCourseTitle, status, remarks);
                masterDailyLogData.add(entry);
                originalDailyStatuses.put(entry.getRollNo() + "_" + entry.getDate(), status);
                rowNum++;
            }
        }

        if (rollNoCombo != null) {
            ObservableList<String> rollOptions = FXCollections.observableArrayList("All Roll Nos");
            for (StudentRecord s : masterStudentData) {
                rollOptions.add(s.getRollNo());
            }
            rollNoCombo.setItems(rollOptions);
            rollNoCombo.setValue("All Roll Nos");
        }

        if (standingCombo != null) {
            standingCombo.setItems(FXCollections.observableArrayList("All Standings", "Active", "At Risk", "Inactive"));
            standingCombo.setValue("All Standings");
        }

        updateStudentMetrics();
        handleFilterStudents();
        applyDailyFilters();
    }

    private void setupStudentTable() {
        if (studentTable == null) return;

        colId.setCellValueFactory(c -> c.getValue().idProperty());
        colRollNo.setCellValueFactory(c -> c.getValue().rollNoProperty());
        colName.setCellValueFactory(c -> c.getValue().nameProperty());
        colEmail.setCellValueFactory(c -> c.getValue().emailProperty());
        colGpa.setCellValueFactory(c -> c.getValue().gpaProperty().asObject());
        colAcademicStanding.setCellValueFactory(c -> c.getValue().standingProperty());

        colAcademicStanding.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equalsIgnoreCase("Active")) {
                        setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
                    } else if (item.equalsIgnoreCase("At Risk")) {
                        setStyle("-fx-text-fill: #b45309; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                    }
                }
            }
        });

        filteredStudentData = new FilteredList<>(masterStudentData, b -> true);
        studentTable.setItems(filteredStudentData);
        lockTableColumns(studentTable, 0.10, 0.15, 0.23, 0.27, 0.09, 0.156);
    }

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
                    setStyle("");
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
    }

    // ==================== TAB SWITCHING ====================

    @FXML
    public void switchToRosterTab() {
        if (rosterSection != null) { rosterSection.setVisible(true); rosterSection.setManaged(true); }
        if (attendanceSection != null) { attendanceSection.setVisible(false); attendanceSection.setManaged(false); }
        if (tabRosterBtn != null) {
            tabRosterBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent transparent #1b315e transparent; -fx-border-width: 0 0 3.5 0; -fx-text-fill: #0f172a; -fx-font-size: 14px; -fx-font-weight: 800; -fx-padding: 6px 8px; -fx-cursor: hand;");
        }
        if (tabAttendanceBtn != null) {
            tabAttendanceBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-border-width: 0 0 3.5 0; -fx-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 6px 8px; -fx-cursor: hand;");
        }
    }

    @FXML
    public void switchToAttendanceTab() {
        if (rosterSection != null) { rosterSection.setVisible(false); rosterSection.setManaged(false); }
        if (attendanceSection != null) { attendanceSection.setVisible(true); attendanceSection.setManaged(true); }
        if (tabAttendanceBtn != null) {
            tabAttendanceBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent transparent #1b315e transparent; -fx-border-width: 0 0 3.5 0; -fx-text-fill: #0f172a; -fx-font-size: 14px; -fx-font-weight: 800; -fx-padding: 6px 8px; -fx-cursor: hand;");
        }
        if (tabRosterBtn != null) {
            tabRosterBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-border-width: 0 0 3.5 0; -fx-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 6px 8px; -fx-cursor: hand;");
        }
        if (dailyLogTable != null) dailyLogTable.refresh();
    }

    // ==================== NAVIGATION & ALLOCATION ====================

    @FXML
    public void handleBackToClasses(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AdminClassesView.fxml"));
            Parent classesView = loader.load();
            StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(classesView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSaveAdminChanges() {
        if (currentClassItem != null) {
            if (assignTeacherCombo != null && assignTeacherCombo.getValue() != null) {
                currentClassItem.lecturer = assignTeacherCombo.getValue();
            }
            if (scheduleField != null && scheduleField.getText() != null && !scheduleField.getText().isBlank()) {
                currentClassItem.schedule = scheduleField.getText().trim();
            }
        }
        // TODO (DB Integration): UPDATE course_offering / class_session in MySQL
        showInfo("Changes Saved", "Lecturer assignment and schedule updated for " + currentClassCode + " (" + currentMajor + ").");
    }

    // ==================== STUDENT ROSTER ACTIONS ====================

    @FXML
    public void handleFilterStudents() {
        if (filteredStudentData == null) return;
        String selRoll = rollNoCombo != null ? rollNoCombo.getValue() : "All Roll Nos";
        String selStanding = standingCombo != null ? standingCombo.getValue() : "All Standings";
        String q = (searchField != null && searchField.getText() != null)
                ? searchField.getText().trim().toLowerCase() : "";

        filteredStudentData.setPredicate(s -> {
            boolean matchRoll = selRoll == null || selRoll.equals("All Roll Nos") || s.getRollNo().equalsIgnoreCase(selRoll);
            boolean matchStand = selStanding == null || selStanding.equals("All Standings") || s.getStanding().equalsIgnoreCase(selStanding);
            boolean matchSearch = q.isEmpty()
                    || s.getId().toLowerCase().contains(q)
                    || s.getRollNo().toLowerCase().contains(q)
                    || s.getName().toLowerCase().contains(q)
                    || s.getEmail().toLowerCase().contains(q);
            return matchRoll && matchStand && matchSearch;
        });

        updateStudentFooter();
    }

    @FXML
    public void handleResetFilters() {
        if (rollNoCombo != null) rollNoCombo.setValue("All Roll Nos");
        if (standingCombo != null) standingCombo.setValue("All Standings");
        if (searchField != null) searchField.clear();
        handleFilterStudents();
    }

    @FXML
    public void handleAdminAddStudent() {
        Dialog<StudentRecord> dialog = new Dialog<>();
        dialog.setTitle("Enroll Student");
        dialog.setHeaderText("Add a student to " + currentClassCode + " (" + currentMajor + ")");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        TextField rollField = new TextField();
        rollField.setPromptText("e.g. III-" + currentMajor + "-9");
        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");
        TextField emailField = new TextField();
        emailField.setPromptText("e.g. student@ytu.edu.mm");
        TextField gpaField = new TextField("3.50");

        grid.addRow(0, new Label("Roll No:"), rollField);
        grid.addRow(1, new Label("Student Name:"), nameField);
        grid.addRow(2, new Label("YTU Email:"), emailField);
        grid.addRow(3, new Label("GPA:"), gpaField);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !nameField.getText().isBlank()) {
                double gpa = 3.50;
                try { gpa = Double.parseDouble(gpaField.getText().trim()); } catch (Exception ignored) {}
                String newId = String.format("STD-%03d", masterStudentData.size() + 1);
                return new StudentRecord(newId, rollField.getText().trim(), nameField.getText().trim(),
                        emailField.getText().trim(), gpa, "Active");
            }
            return null;
        });

        Optional<StudentRecord> res = dialog.showAndWait();
        res.ifPresent(s -> {
            masterStudentData.add(s);
            updateStudentMetrics();
            handleFilterStudents();
        });
    }

    @FXML
    public void handleModifyStudent() {
        StudentRecord selected = studentTable != null ? studentTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            showWarning("No Student Selected", "Please select a student row first.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modify Student");
        dialog.setHeaderText("Edit details for " + selected.getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        TextField nameField = new TextField(selected.getName());
        TextField emailField = new TextField(selected.getEmail());
        TextField gpaField = new TextField(String.valueOf(selected.getGpa()));
        ComboBox<String> standBox = new ComboBox<>(FXCollections.observableArrayList("Active", "At Risk", "Inactive"));
        standBox.setValue(selected.getStanding());

        grid.addRow(0, new Label("Name:"), nameField);
        grid.addRow(1, new Label("Email:"), emailField);
        grid.addRow(2, new Label("GPA:"), gpaField);
        grid.addRow(3, new Label("Standing:"), standBox);
        dialog.getDialogPane().setContent(grid);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            selected.setName(nameField.getText().trim());
            selected.setEmail(emailField.getText().trim());
            try { selected.setGpa(Double.parseDouble(gpaField.getText().trim())); } catch (Exception ignored) {}
            selected.setStanding(standBox.getValue());
            studentTable.refresh();
            updateStudentMetrics();
        }
    }

    @FXML
    public void handleRemoveStudent() {
        StudentRecord selected = studentTable != null ? studentTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            showWarning("No Student Selected", "Please select a student row to remove.");
            return;
        }
        masterStudentData.remove(selected);
        updateStudentMetrics();
        handleFilterStudents();
    }

    private void updateStudentMetrics() {
        int total = masterStudentData.size();
        long active = masterStudentData.stream().filter(s -> s.getStanding().equalsIgnoreCase("Active")).count();
        long atRisk = masterStudentData.stream().filter(s -> s.getStanding().equalsIgnoreCase("At Risk")).count();
        long inactive = masterStudentData.stream().filter(s -> s.getStanding().equalsIgnoreCase("Inactive")).count();

        if (totalStudentsLabel != null) totalStudentsLabel.setText(String.valueOf(total));
        if (activeStudentsLabel != null) activeStudentsLabel.setText(String.valueOf(active));
        if (atRiskLabel != null) atRiskLabel.setText(String.valueOf(atRisk));
        if (inactiveLabel != null) inactiveLabel.setText(String.valueOf(inactive));
        updateStudentFooter();
    }

    private void updateStudentFooter() {
        if (studentCountFooterLabel != null && filteredStudentData != null) {
            int shown = filteredStudentData.size();
            int total = masterStudentData.size();
            studentCountFooterLabel.setText(shown == 0
                    ? "Showing 0 of " + total + " students"
                    : "Showing 1 - " + shown + " of " + total + " students");
        }
    }

    // ==================== DAILY ATTENDANCE LOG ACTIONS ====================

    @FXML
    public void applyDailyFilters() {
        if (filteredDailyLogData == null) return;
        LocalDate selDate = dailyDatePicker != null ? dailyDatePicker.getValue() : null;
        String dateStr = selDate != null ? selDate.toString() : "";
        String q = (dailySearchField != null && dailySearchField.getText() != null)
                ? dailySearchField.getText().trim().toLowerCase() : "";
        boolean p = chkPresent != null && chkPresent.isSelected();
        boolean a = chkAbsent != null && chkAbsent.isSelected();
        boolean e = chkExcuse != null && chkExcuse.isSelected();
        boolean any = p || a || e;

        filteredDailyLogData.setPredicate(entry -> {
            boolean matchDate = dateStr.isEmpty() || entry.getDate().equals(dateStr);
            boolean matchSearch = q.isEmpty()
                    || entry.getStudentName().toLowerCase().contains(q)
                    || entry.getRollNo().toLowerCase().contains(q)
                    || entry.getSubject().toLowerCase().contains(q);
            boolean matchStatus = !any
                    || (p && entry.getStatus().equalsIgnoreCase("Present"))
                    || (a && entry.getStatus().equalsIgnoreCase("Absent"))
                    || (e && (entry.getStatus().equalsIgnoreCase("Excused") || entry.getStatus().equalsIgnoreCase("Excuse")));
            return matchDate && matchSearch && matchStatus;
        });

        if (dailyLogFooterLabel != null) {
            int n = filteredDailyLogData.size();
            dailyLogFooterLabel.setText("Showing " + n + " attendance record" + (n == 1 ? "" : "s"));
        }
    }

    @FXML
    public void handleResetDailyFilters() {
        if (dailyDatePicker != null) dailyDatePicker.setValue(null);
        if (dailySearchField != null) dailySearchField.clear();
        if (chkPresent != null) chkPresent.setSelected(false);
        if (chkAbsent != null) chkAbsent.setSelected(false);
        if (chkExcuse != null) chkExcuse.setSelected(false);
        applyDailyFilters();
    }

    @FXML
    public void handleToggleDailyStatus() {
        DailyLogEntry selected = dailyLogTable != null
                ? dailyLogTable.getSelectionModel().getSelectedItem()
                : null;

        if (selected == null) {
            showWarning("No Row Selected", "Please select a student row in the Daily Attendance Log first.");
            return;
        }

        String current = selected.getStatus() != null ? selected.getStatus() : "Present";
        if (current.equalsIgnoreCase("Present")) {
            selected.setStatus("Absent");
            selected.setRemarks("Unexcused");
        } else if (current.equalsIgnoreCase("Absent")) {
            selected.setStatus("Excused");
            selected.setRemarks("Medical Leave");
        } else {
            selected.setStatus("Present");
            selected.setRemarks("On Time");
        }

        dailyLogTable.refresh();
    }

    @FXML
    public void handleResetDailyStatus() {
        for (DailyLogEntry entry : masterDailyLogData) {
            String key = entry.getRollNo() + "_" + entry.getDate();
            if (originalDailyStatuses.containsKey(key)) {
                entry.setStatus(originalDailyStatuses.get(key));
            }
        }
        if (dailyLogTable != null) dailyLogTable.refresh();
    }

    @FXML
    public void handleMarkAllPresent() {
        for (DailyLogEntry entry : filteredDailyLogData) {
            entry.setStatus("Present");
            entry.setRemarks("On Time");
        }
        if (dailyLogTable != null) dailyLogTable.refresh();
    }

    @FXML
    public void handleSaveDailyChanges() {
        for (DailyLogEntry entry : masterDailyLogData) {
            originalDailyStatuses.put(entry.getRollNo() + "_" + entry.getDate(), entry.getStatus());
        }
        // TODO (DB Integration): Persist attendance records to `attendance` table
        showInfo("Attendance Saved", "Daily attendance changes for " + currentClassCode + " (" + currentMajor + ") have been saved!");
    }

    // ==================== HELPERS & MODELS ====================

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

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showWarning(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    public static class StudentRecord {
        private final SimpleStringProperty id;
        private final SimpleStringProperty rollNo;
        private final SimpleStringProperty name;
        private final SimpleStringProperty email;
        private final SimpleDoubleProperty gpa;
        private final SimpleStringProperty standing;

        public StudentRecord(String id, String rollNo, String name, String email, double gpa, String standing) {
            this.id = new SimpleStringProperty(id);
            this.rollNo = new SimpleStringProperty(rollNo);
            this.name = new SimpleStringProperty(name);
            this.email = new SimpleStringProperty(email);
            this.gpa = new SimpleDoubleProperty(gpa);
            this.standing = new SimpleStringProperty(standing);
        }

        public String getId() { return id.get(); }
        public SimpleStringProperty idProperty() { return id; }
        public String getRollNo() { return rollNo.get(); }
        public SimpleStringProperty rollNoProperty() { return rollNo; }
        public String getName() { return name.get(); }
        public void setName(String v) { name.set(v); }
        public SimpleStringProperty nameProperty() { return name; }
        public String getEmail() { return email.get(); }
        public void setEmail(String v) { email.set(v); }
        public SimpleStringProperty emailProperty() { return email; }
        public double getGpa() { return gpa.get(); }
        public void setGpa(double v) { gpa.set(v); }
        public SimpleDoubleProperty gpaProperty() { return gpa; }
        public String getStanding() { return standing.get(); }
        public void setStanding(String v) { standing.set(v); }
        public SimpleStringProperty standingProperty() { return standing; }
    }

    public static class DailyLogEntry {
        private final SimpleIntegerProperty no;
        private final SimpleStringProperty rollNo;
        private final SimpleStringProperty studentName;
        private final SimpleStringProperty date;
        private final SimpleStringProperty subject;
        private final SimpleStringProperty status;
        private final SimpleStringProperty remarks;

        public DailyLogEntry(int no, String rollNo, String studentName, String date, String subject, String status, String remarks) {
            this.no = new SimpleIntegerProperty(no);
            this.rollNo = new SimpleStringProperty(rollNo);
            this.studentName = new SimpleStringProperty(studentName);
            this.date = new SimpleStringProperty(date);
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
        public String getSubject() { return subject.get(); }
        public SimpleStringProperty subjectProperty() { return subject; }
        public String getStatus() { return status.get(); }
        public void setStatus(String v) { status.set(v); }
        public SimpleStringProperty statusProperty() { return status; }
        public String getRemarks() { return remarks.get(); }
        public void setRemarks(String v) { remarks.set(v); }
        public SimpleStringProperty remarksProperty() { return remarks; }
    }
}