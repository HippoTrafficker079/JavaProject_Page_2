package application;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class AdminClassDetailController {

    @FXML private VBox rootPane;
    @FXML private Label classNameLabel;
    @FXML private ComboBox<String> assignTeacherCombo;
    @FXML private ComboBox<String> standingCombo;
    @FXML private TextField scheduleField;

    // Metric Labels
    @FXML private Label totalStudentsLabel;
    @FXML private Label activeStudentsLabel;
    @FXML private Label atRiskLabel;
    @FXML private Label inactiveLabel;

    // Navigation Tabs & Sections
    @FXML private Button tabRosterBtn;
    @FXML private Button tabAttendanceBtn;
    @FXML private VBox rosterSection;
    @FXML private VBox attendanceSection;

    // Tab 1: Student Roster Controls & Table
    @FXML private ComboBox<String> rollNoCombo;
    @FXML private TextField searchField;
    @FXML private Label studentCountFooterLabel;

    @FXML private TableView<StudentModel> studentTable;
    @FXML private TableColumn<StudentModel, String> colId;
    @FXML private TableColumn<StudentModel, String> colRollNo;
    @FXML private TableColumn<StudentModel, String> colName;
    @FXML private TableColumn<StudentModel, String> colEmail;
    @FXML private TableColumn<StudentModel, Double> colGpa;

    // Tab 2: Daily Attendance Log Controls & Table (Editable by Admin)
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
    @FXML private TableColumn<StudentModel, String> colAcademicStanding;
    
    private final ObservableList<StudentModel> masterStudents = FXCollections.observableArrayList();
    private FilteredList<StudentModel> filteredStudents;

    private final ObservableList<DailyLogEntry> masterDailyLogs = FXCollections.observableArrayList();
    private FilteredList<DailyLogEntry> filteredDailyLogs;
    private final Map<DailyLogEntry, String> originalStatuses = new HashMap<>();

    private String currentClassCode = "IT201";
    private String currentSubjectName = "Data Structures and Algorithms";

    @FXML
    public void initialize() {
        if (rootPane != null) {
            rootPane.setOnMouseClicked(event -> {
                if (studentTable != null) studentTable.getSelectionModel().clearSelection();
                if (dailyLogTable != null) dailyLogTable.getSelectionModel().clearSelection();
                rootPane.requestFocus();
            });
        }

        if (assignTeacherCombo != null) {
            assignTeacherCombo.setItems(FXCollections.observableArrayList(
                "Dr. Hla Myint", "Dr. Aung Naing", "Dr. Su Su", "Prof. Kyaw Zin", "Dr. Tin Maung", "Dr. Myo Tint"
            ));
            assignTeacherCombo.setValue("Dr. Hla Myint");
        }

        setupRosterTable();
        setupDailyLogControlsAndTable();

        if (masterStudents.isEmpty()) {
            loadSampleStudents();
            loadSampleDailyLogs(currentSubjectName);
        }

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
    // TAB 1: STUDENT ROSTER SETUP & FILTERING
    // =========================================================================

    private void setupRosterTable() {
        if (colId != null) colId.setCellValueFactory(cell -> cell.getValue().idProperty());
        if (colRollNo != null) colRollNo.setCellValueFactory(cell -> cell.getValue().rollNoProperty());
        if (colName != null) colName.setCellValueFactory(cell -> cell.getValue().nameProperty());
        if (colEmail != null) colEmail.setCellValueFactory(cell -> cell.getValue().emailProperty());
        if (colGpa != null) colGpa.setCellValueFactory(cell -> cell.getValue().gpaProperty().asObject());

        if (colAcademicStanding != null) {
            colAcademicStanding.setCellValueFactory(cell -> cell.getValue().academicStandingProperty());
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

        // No auto-filter listeners here — filters run only when Filter or Reset is clicked
        filteredStudents = new FilteredList<>(masterStudents, s -> true);
        if (studentTable != null) {
            SortedList<StudentModel> sorted = new SortedList<>(filteredStudents);
            sorted.comparatorProperty().bind(studentTable.comparatorProperty());
            studentTable.setItems(sorted);
            lockTableColumns(studentTable, 0.10, 0.14, 0.21, 0.26, 0.09, 0.196);
        }
    }

    @FXML
    public void handleFilterStudents(ActionEvent event) {
        applyStudentFilters();
    }

    @FXML
    public void handleResetFilters(ActionEvent event) {
        if (rollNoCombo != null) rollNoCombo.setValue("All Roll Nos");
        if (standingCombo != null) standingCombo.setValue("All Standings");
        if (searchField != null) searchField.clear();
        applyStudentFilters();
    }

    private void applyStudentFilters() {
        if (filteredStudents == null) return;
        String selRoll = rollNoCombo != null ? rollNoCombo.getValue() : "All Roll Nos";
        String selStanding = standingCombo != null ? standingCombo.getValue() : "All Standings";
        String q = (searchField != null && searchField.getText() != null)
                ? searchField.getText().trim().toLowerCase()
                : "";

        filteredStudents.setPredicate(s -> {
            boolean matchRoll = selRoll == null || selRoll.equals("All Roll Nos") || s.getRollNo().equalsIgnoreCase(selRoll);
            boolean matchStanding = selStanding == null || selStanding.equals("All Standings")
                    || s.getAcademicStanding().equalsIgnoreCase(selStanding);
            boolean matchSearch = q.isEmpty()
                    || s.getId().toLowerCase().contains(q)
                    || s.getRollNo().toLowerCase().contains(q)
                    || s.getName().toLowerCase().contains(q)
                    || s.getEmail().toLowerCase().contains(q);
            return matchRoll && matchStanding && matchSearch;
        });

        updateMetricsAndFooter();
    }

    // =========================================================================
    // TAB 2: DAILY ATTENDANCE LOG SETUP, EDITING & FILTERING
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

        colNoLog.setCellValueFactory(c -> c.getValue().noProperty().asObject());
        colRollLog.setCellValueFactory(c -> c.getValue().rollNoProperty());
        colNameLog.setCellValueFactory(c -> c.getValue().studentNameProperty());
        colDateLog.setCellValueFactory(c -> c.getValue().dateProperty());
        colSubjLog.setCellValueFactory(c -> c.getValue().subjectProperty());
        colRemarksLog.setCellValueFactory(c -> c.getValue().remarksProperty());

        dailyLogTable.setEditable(true);
        colStatusLog.setCellValueFactory(c -> c.getValue().statusProperty());
        colStatusLog.setCellFactory(tc -> {
            ComboBoxTableCell<DailyLogEntry, String> cell =
                new ComboBoxTableCell<>("Present", "Absent", "Excused") {
                    @Override
                    public void startEdit() {
                        getTableView().getSelectionModel().clearSelection();
                        super.startEdit();
                    }

                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else if (!isEditing()) {
                            if (item.equalsIgnoreCase("Present")) {
                                setText("✔ Present");
                                setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
                            } else if (item.equalsIgnoreCase("Absent")) {
                                setText("✖ Absent");
                                setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                            } else {
                                setText("● " + item);
                                setStyle("-fx-text-fill: #b45309; -fx-font-weight: bold;");
                            }
                        } else {
                            setStyle("-fx-text-fill: #0f172a;");
                        }
                    }
                };
            cell.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                    e -> dailyLogTable.getSelectionModel().clearSelection());
            return cell;
        });

        colStatusLog.setOnEditCommit(event -> {
            DailyLogEntry entry = event.getRowValue();
            String newStatus = event.getNewValue();
            entry.setStatus(newStatus);
            if ("Present".equalsIgnoreCase(newStatus)) {
                entry.setRemarks("On Time");
            } else if ("Excused".equalsIgnoreCase(newStatus)) {
                entry.setRemarks("Medical / Approved");
            } else {
                entry.setRemarks("Unexcused");
            }
            syncStudentAttendanceFromLogs();
            dailyLogTable.refresh();
            Platform.runLater(() -> dailyLogTable.getSelectionModel().clearSelection());
        });

        dailyLogTable.setRowFactory(tv -> new TableRow<>() {
            private final ChangeListener<String> statusListener = (obs, o, n) -> applyDailyRowStyle(this, n);

            @Override
            protected void updateItem(DailyLogEntry item, boolean empty) {
                if (getItem() != null) {
                    getItem().statusProperty().removeListener(statusListener);
                }
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                    getStyleClass().removeAll("row-present", "row-absent", "row-excused");
                } else {
                    item.statusProperty().addListener(statusListener);
                    applyDailyRowStyle(this, item.getStatus());
                }
            }
        });

        filteredDailyLogs = new FilteredList<>(masterDailyLogs, b -> true);
        dailyLogTable.setItems(filteredDailyLogs);
        lockTableColumns(dailyLogTable, 0.06, 0.13, 0.19, 0.13, 0.21, 0.14, 0.136);
    }

    private void applyDailyRowStyle(TableRow<DailyLogEntry> row, String status) {
        row.setStyle("");
        row.getStyleClass().removeAll("row-present", "row-absent", "row-excused");
        if (status == null) return;

        if (status.equalsIgnoreCase("Present")) {
            row.getStyleClass().add("row-present");
        } else if (status.equalsIgnoreCase("Absent")) {
            row.getStyleClass().add("row-absent");
        } else if (status.equalsIgnoreCase("Excused") || status.equalsIgnoreCase("Excuse")) {
            row.getStyleClass().add("row-excused");
        }
    }

    @FXML
    public void applyDailyFilters() {
        if (filteredDailyLogs == null) return;

        LocalDate selectedDate = dailyDatePicker != null ? dailyDatePicker.getValue() : null;
        String dateFilterStr = selectedDate != null ? selectedDate.toString() : "";
        String keyword = (dailySearchField != null && dailySearchField.getText() != null)
                ? dailySearchField.getText().trim().toLowerCase()
                : "";

        boolean pChecked = chkPresent != null && chkPresent.isSelected();
        boolean aChecked = chkAbsent != null && chkAbsent.isSelected();
        boolean eChecked = chkExcuse != null && chkExcuse.isSelected();
        boolean anyStatusChecked = pChecked || aChecked || eChecked;

        filteredDailyLogs.setPredicate(entry -> {
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

        updateMetricsAndFooter();
    }

    @FXML
    public void handleResetDailyFilters() {
        if (dailyDatePicker != null) dailyDatePicker.setValue(null);
        if (dailySearchField != null) dailySearchField.clear();
        if (chkPresent != null) chkPresent.setSelected(false);
        if (chkAbsent != null) chkAbsent.setSelected(false);
        if (chkExcuse != null) chkExcuse.setSelected(false);
        if (filteredDailyLogs != null) filteredDailyLogs.setPredicate(b -> true);
        updateMetricsAndFooter();
    }

    @FXML
    public void handleResetDailyStatus() {
        for (DailyLogEntry entry : masterDailyLogs) {
            if (originalStatuses.containsKey(entry)) {
                String orig = originalStatuses.get(entry);
                entry.setStatus(orig);
                entry.setRemarks("Present".equalsIgnoreCase(orig) ? "On Time"
                        : ("Excused".equalsIgnoreCase(orig) ? "Medical / Approved" : "Unexcused"));
            }
        }
        syncStudentAttendanceFromLogs();
        if (dailyLogTable != null) dailyLogTable.refresh();
    }

    @FXML
    public void handleMarkAllPresent() {
        for (DailyLogEntry entry : filteredDailyLogs) {
            entry.setStatus("Present");
            entry.setRemarks("On Time");
        }
        syncStudentAttendanceFromLogs();
        if (dailyLogTable != null) dailyLogTable.refresh();
    }

    @FXML
    public void handleSaveDailyChanges() {
        for (DailyLogEntry entry : masterDailyLogs) {
            originalStatuses.put(entry, entry.getStatus());
        }
        syncStudentAttendanceFromLogs();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Attendance Saved");
        alert.setHeaderText(null);
        alert.setContentText("Daily attendance logs for " + currentClassCode + " (" + currentSubjectName + ") have been saved.");
        alert.showAndWait();
    }

    // =========================================================================
    // DATA INITIALIZATION & ADMIN ACTIONS
    // =========================================================================

    public void initClassData(String classCode) {
        this.currentClassCode = classCode != null ? classCode : "IT201";
        this.currentSubjectName = switch (this.currentClassCode) {
            case "IT201" -> "Data Structures and Algorithms";
            case "IT202" -> "Database Systems";
            case "IT203" -> "Computer Networks";
            case "IT204" -> "Web Technologies";
            case "IT205" -> "Operating Systems";
            case "IT206" -> "Software Engineering";
            default -> "General Class";
        };

        if (classNameLabel != null) {
            classNameLabel.setText("Admin Class Management (" + this.currentClassCode + ") - " + this.currentSubjectName);
        }
        if (scheduleField != null) {
            scheduleField.setText("Tue P6–7, Fri P1–2");
        }

        loadSampleStudents();
        loadSampleDailyLogs(this.currentSubjectName);
        applyDailyFilters();
    }

    private void loadSampleStudents() {
        masterStudents.setAll(
            new StudentModel("256730", "III-CEIT-1", "Aung Thura", "aungthura@gmail.com", 92, 3.6),
            new StudentModel("256731", "III-CEIT-2", "Su Myat Mya", "sumyat@gmail.com", 82, 3.6),
            new StudentModel("256732", "III-CEIT-3", "Min Khant", "minkhant@gmail.com", 74, 2.4),
            new StudentModel("256733", "III-CEIT-4", "Hnin Wai", "hninwai@gmail.com", 95, 3.9),
            new StudentModel("256734", "III-CEIT-5", "Ko Thant", "kothant@gmail.com", 48, 2.1),
            new StudentModel("256735", "III-CEIT-6", "Zaw Hein", "zawhein@gmail.com", 98, 4.0),
            new StudentModel("256736", "III-CEIT-7", "May Thu", "maythu@gmail.com", 68, 3.2),
            new StudentModel("256737", "III-CEIT-8", "Aimon M", "aimonm@gmail.com", 50, 1.8),
            new StudentModel("256738", "III-CEIT-9", "Kyaw Zin", "kyawzin@gmail.com", 85, 3.4),
            new StudentModel("256739", "III-CEIT-10", "Nandar Lin", "nandarlin@gmail.com", 91, 3.7)
        );
        refreshRollNoOptions();
        updateMetricsAndFooter();
    }

    private void loadSampleDailyLogs(String subjectName) {
        masterDailyLogs.clear();
        originalStatuses.clear();
        String dateStr = "2026-11-16";

        for (int i = 0; i < masterStudents.size(); i++) {
            StudentModel s = masterStudents.get(i);
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

            DailyLogEntry entry = new DailyLogEntry(
                i + 1,
                s.getRollNo(),
                s.getName(),
                dateStr,
                subjectName,
                status,
                remarks
            );
            masterDailyLogs.add(entry);
            originalStatuses.put(entry, status);
        }
    }

    private void syncStudentAttendanceFromLogs() {
        for (StudentModel student : masterStudents) {
            for (DailyLogEntry entry : masterDailyLogs) {
                if (entry.getRollNo().equalsIgnoreCase(student.getRollNo())) {
                    String st = entry.getStatus();
                    if ("Present".equalsIgnoreCase(st) && student.getAttendancePercent() < 75) {
                        student.setAttendancePercent(80);
                    } else if ("Absent".equalsIgnoreCase(st) && student.getAttendancePercent() >= 75) {
                        student.setAttendancePercent(50);
                    } else if ("Excused".equalsIgnoreCase(st) && student.getAttendancePercent() >= 75) {
                        student.setAttendancePercent(70);
                    }
                }
            }
        }
        updateMetricsAndFooter();
    }

    private void refreshRollNoOptions() {
        if (rollNoCombo == null) return;
        String previous = rollNoCombo.getValue();
        ObservableList<String> rolls = FXCollections.observableArrayList("All Roll Nos");
        for (StudentModel s : masterStudents) {
            if (!rolls.contains(s.getRollNo())) {
                rolls.add(s.getRollNo());
            }
        }
        rollNoCombo.setItems(rolls);
        rollNoCombo.setValue(previous != null && rolls.contains(previous) ? previous : "All Roll Nos");
    }

    private void updateMetricsAndFooter() {
        int total = masterStudents.size();
        int active = 0;
        int atRisk = 0;
        int inactive = 0;

        for (StudentModel s : masterStudents) {
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

        if (studentCountFooterLabel != null && filteredStudents != null) {
            int shown = filteredStudents.size();
            studentCountFooterLabel.setText(shown == 0
                    ? "Showing 0 of " + total + " students"
                    : "Showing 1 - " + shown + " of " + total + " students");
        }

        if (dailyLogFooterLabel != null && filteredDailyLogs != null) {
            int logCount = filteredDailyLogs.size();
            dailyLogFooterLabel.setText("Showing " + logCount + " attendance record" + (logCount == 1 ? "" : "s"));
        }
    }

    @FXML
    public void handleModifyStudent(ActionEvent event) {
        StudentModel selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Student Selected");
            alert.setHeaderText(null);
            alert.setContentText("Please select a student from the Student List table to modify.");
            alert.showAndWait();
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modify Student Details");
        dialog.setHeaderText("Edit student record for " + selected.getRollNo() + " (" + selected.getName() + ")");

        TextField nameField = new TextField(selected.getName());
        TextField emailField = new TextField(selected.getEmail());
        TextField gpaField = new TextField(String.valueOf(selected.getGpa()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Student Name:"), nameField);
        grid.addRow(1, new Label("Email:"), emailField);
        grid.addRow(2, new Label("GPA:"), gpaField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    double parsedGpa = Double.parseDouble(gpaField.getText().trim());
                    int idx = masterStudents.indexOf(selected);
                    if (idx >= 0) {
                        StudentModel updated = new StudentModel(
                            selected.getId(),
                            selected.getRollNo(),
                            nameField.getText().trim(),
                            emailField.getText().trim(),
                            selected.getAttendancePercent(),
                            parsedGpa
                        );
                        masterStudents.set(idx, updated);
                        studentTable.refresh();
                        applyStudentFilters();
                    }
                } catch (NumberFormatException ex) {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Invalid GPA");
                    err.setHeaderText(null);
                    err.setContentText("Please enter a valid numeric GPA (e.g., 3.5).");
                    err.showAndWait();
                }
            }
        });
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
            if (res == ButtonType.YES) {
                masterStudents.remove(selected);
                masterDailyLogs.removeIf(log -> log.getRollNo().equalsIgnoreCase(selected.getRollNo()));
                refreshRollNoOptions();
                updateMetricsAndFooter();
            }
        });
    }

    @FXML
    public void handleSaveAdminChanges(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Admin Assignment Saved");
        alert.setHeaderText(null);
        alert.setContentText("Successfully assigned teacher (" + assignTeacherCombo.getValue() + ") and updated class schedule!");
        alert.showAndWait();
    }

    @FXML
    public void handleOpenCreateClassDialog(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Create New Class");
        alert.setHeaderText("University Class Creation Wizard");
        alert.setContentText("Admin Modal Wizard: Form to input Course Title, Code, Major, Year, Semester, and default Teacher allocation.");
        alert.showAndWait();
    }

    @FXML
    public void handleAdminAddStudent(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Enroll Student");
        dialog.setHeaderText("Add New Student to Class");

        int nextNum = masterStudents.size() + 1;
        TextField idField = new TextField(String.valueOf(256730 + nextNum - 1));
        TextField rollField = new TextField("III-CEIT-" + nextNum);
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Kyaw Swar");
        TextField emailField = new TextField();
        emailField.setPromptText("e.g. kyawswar@gmail.com");
        TextField gpaField = new TextField("3.5");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Student ID:"), idField);
        grid.addRow(1, new Label("Roll No:"), rollField);
        grid.addRow(2, new Label("Student Name:"), nameField);
        grid.addRow(3, new Label("Email:"), emailField);
        grid.addRow(4, new Label("GPA:"), gpaField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK && !nameField.getText().trim().isEmpty()) {
                try {
                    double parsedGpa = Double.parseDouble(gpaField.getText().trim());
                    StudentModel newStudent = new StudentModel(
                        idField.getText().trim(),
                        rollField.getText().trim(),
                        nameField.getText().trim(),
                        emailField.getText().trim(),
                        90,
                        parsedGpa
                    );
                    masterStudents.add(newStudent);

                    DailyLogEntry logEntry = new DailyLogEntry(
                        masterDailyLogs.size() + 1,
                        newStudent.getRollNo(),
                        newStudent.getName(),
                        "2026-11-16",
                        currentSubjectName,
                        "Present",
                        "On Time"
                    );
                    masterDailyLogs.add(logEntry);
                    originalStatuses.put(logEntry, "Present");

                    refreshRollNoOptions();
                    updateMetricsAndFooter();
                } catch (NumberFormatException ex) {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Invalid GPA");
                    err.setHeaderText(null);
                    err.setContentText("Please enter a valid numeric GPA (e.g., 3.5).");
                    err.showAndWait();
                }
            }
        });
    }

    @FXML
    private void handleBackToClasses(ActionEvent event) {
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
            if (gpa >= 3.8) return "Excellent";
            if (gpa >= 3.0) return "Good";
            if (gpa >= 2.0) return "Average";
            return "Poor";
        }

        public String getId() { return id.get(); }
        public SimpleStringProperty idProperty() { return id; }

        public String getRollNo() { return rollNo.get(); }
        public SimpleStringProperty rollNoProperty() { return rollNo; }

        public String getName() { return name.get(); }
        public SimpleStringProperty nameProperty() { return name; }

        public String getEmail() { return email.get(); }
        public SimpleStringProperty emailProperty() { return email; }

        public double getGpa() { return gpa.get(); }
        public SimpleDoubleProperty gpaProperty() { return gpa; }

        public String getAcademicStanding() { return academicStanding.get(); }
        public SimpleStringProperty academicStandingProperty() { return academicStanding; }

        public int getAttendancePercent() { return attendancePercent; }
        public void setAttendancePercent(int attendancePercent) { this.attendancePercent = attendancePercent; }
    }

    public static class DailyLogEntry {
        private final SimpleIntegerProperty no;
        private final SimpleStringProperty rollNo;
        private final SimpleStringProperty studentName;
        private final SimpleStringProperty date;
        private final SimpleStringProperty subject;
        private final SimpleStringProperty status;
        private final SimpleStringProperty remarks;

        public DailyLogEntry(int no, String rollNo, String studentName, String date,
                             String subject, String status, String remarks) {
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
        public void setStatus(String val) { status.set(val); }
        public SimpleStringProperty statusProperty() { return status; }

        public String getRemarks() { return remarks.get(); }
        public void setRemarks(String val) { remarks.set(val); }
        public SimpleStringProperty remarksProperty() { return remarks; }
    }
}