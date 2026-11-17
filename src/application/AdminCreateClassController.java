package application;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminCreateClassController {

    @FXML private VBox rootPane;

    // Academic & Course Details
    @FXML private ComboBox<String> courseTitleCombo;
    @FXML private TextField courseCodeField;
    @FXML private ComboBox<String> majorCombo;
    @FXML private ComboBox<String> semesterCombo;
    @FXML private ComboBox<String> academicYearCombo;

    // Faculty, Intake & Total Sessions
    @FXML private ComboBox<String> teacherCombo;
    @FXML private TextField maxStudentsField;
    @FXML private TextField totalSessionsField;

    // Session & Period Scheduling
    @FXML private DatePicker sessionDatePicker;
    @FXML private ComboBox<String> periodCombo;
    @FXML private TableView<SessionRow> sessionTable;
    @FXML private TableColumn<SessionRow, String> colSessionDate;
    @FXML private TableColumn<SessionRow, String> colSessionDay;
    @FXML private TableColumn<SessionRow, String> colSessionPeriod;
    @FXML private TableColumn<SessionRow, String> colSessionAction;
    @FXML private Label sessionSummaryLabel;

    // UI Course Catalog (Course Title -> CoursePreset)
    private final Map<String, CoursePreset> courseCatalog = new LinkedHashMap<>();
    private final ObservableList<SessionRow> scheduledSessions = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        initCourseCatalog();
        initDropdowns();
        initSessionTable();

        if (totalSessionsField != null) {
            totalSessionsField.setText("16");
            totalSessionsField.textProperty().addListener((obs, o, n) -> updateSessionSummary());
        }

        if (sessionDatePicker != null) {
            sessionDatePicker.setValue(LocalDate.now());
        }

        scheduledSessions.addListener((ListChangeListener<SessionRow>) change -> updateSessionSummary());
        updateSessionSummary();
    }

    private void initCourseCatalog() {
        addCoursePreset("CEIT-3101", "Advanced Database Systems", "CEIT");
        addCoursePreset("CEIT-3102", "Data Structures & Algorithms", "CEIT");
        addCoursePreset("IT201", "Data Structures", "CEIT");
        addCoursePreset("IT202", "Database Systems", "CEIT");
        addCoursePreset("IT203", "Computer Networks", "CEIT");
        addCoursePreset("IT204", "Web Technologies", "CEIT");
        addCoursePreset("IT205", "Operating Systems", "CEIT");
        addCoursePreset("IT206", "Software Engineering", "CEIT");
        addCoursePreset("ME-3101", "Thermodynamics II", "ME");
        addCoursePreset("EC-3101", "Digital Signal Processing", "EC");
        addCoursePreset("CE301", "Structural Mechanics", "CE");
        addCoursePreset("EP302", "Power Transmission Systems", "EP");

        if (courseTitleCombo != null) {
            ObservableList<String> allTitles = FXCollections.observableArrayList(courseCatalog.keySet());
            courseTitleCombo.setItems(allTitles);

            // Auto-populate Course Code and Major when a course is selected
            courseTitleCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && courseCatalog.containsKey(newVal)) {
                    CoursePreset preset = courseCatalog.get(newVal);
                    if (courseCodeField != null) courseCodeField.setText(preset.courseCode);
                    if (majorCombo != null) majorCombo.setValue(preset.deptCode);
                }
            });

            // Live search filtering inside editable Course Title dropdown
            if (courseTitleCombo.getEditor() != null) {
                courseTitleCombo.getEditor().textProperty().addListener((obs, oldText, newText) -> {
                    if (courseTitleCombo.getValue() != null && courseTitleCombo.getValue().equals(newText)) {
                        return;
                    }
                    String q = newText == null ? "" : newText.trim().toLowerCase();
                    ObservableList<String> matches = FXCollections.observableArrayList(
                        courseCatalog.keySet().stream()
                            .filter(name -> q.isEmpty() || name.toLowerCase().contains(q))
                            .collect(Collectors.toList())
                    );
                    courseTitleCombo.setItems(matches);
                    if (!matches.isEmpty() && !courseTitleCombo.isShowing() && courseTitleCombo.isFocused()) {
                        courseTitleCombo.show();
                    }
                });
            }
        }
    }

    private void addCoursePreset(String code, String name, String dept) {
        courseCatalog.put(name, new CoursePreset(code, name, dept));
    }

    private void initDropdowns() {
        if (majorCombo != null) {
            majorCombo.setItems(FXCollections.observableArrayList(
                "CEIT", "ME", "EC", "Arch", "ChE", "PE", "CE", "EP", "EcE", "FE", "McE", "MmsE", "MnE", "TE", "TxE"
            ));
            majorCombo.setValue("CEIT");
        }

        if (semesterCombo != null) {
            semesterCombo.setItems(FXCollections.observableArrayList("First Semester", "Second Semester"));
            semesterCombo.setValue("First Semester");
        }

        if (academicYearCombo != null) {
            academicYearCombo.setItems(FXCollections.observableArrayList(
                "First Year", "Second Year", "Third Year", "Fourth Year", "Fifth Year"
            ));
            academicYearCombo.setValue("Second Year");
        }

        if (teacherCombo != null) {
            teacherCombo.setItems(FXCollections.observableArrayList(
                "Dr. Hla Myint", "Dr. Aung Kyaw Thu", "Dr. Thin Thin Hlaing", "U Min Khant",
                "Daw Su Myat", "U Htet Naing Win", "Daw Ei Mon Phyo", "Dr. Aung Naing"
            ));
        }

        if (periodCombo != null) {
            periodCombo.setItems(FXCollections.observableArrayList(
                "Period 1 (08:00 - 10:00)",
                "Period 2 (10:15 - 12:15)",
                "Period 3 (13:00 - 15:00)",
                "Period 4 (15:15 - 17:15)"
            ));
            periodCombo.setValue("Period 2 (10:15 - 12:15)");
        }
    }

    private void initSessionTable() {
        if (sessionTable == null) return;

        colSessionDate.setCellValueFactory(c -> c.getValue().dateProperty());
        colSessionDay.setCellValueFactory(c -> c.getValue().dayProperty());
        colSessionPeriod.setCellValueFactory(c -> c.getValue().periodLabelProperty());

        colSessionAction.setCellFactory(col -> new TableCell<>() {
            private final Button removeBtn = new Button("🗑 Remove");

            {
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
                    scheduledSessions.remove(row);
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

        sessionTable.setItems(scheduledSessions);
    }

    @FXML
    private void handleAddSession(ActionEvent event) {
        LocalDate pickedDate = sessionDatePicker != null ? sessionDatePicker.getValue() : null;
        String pickedPeriod = periodCombo != null ? periodCombo.getValue() : null;

        if (pickedDate == null || pickedPeriod == null || pickedPeriod.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Incomplete Session Details",
                    "Please choose both a valid Date and Period before adding a session.");
            return;
        }

        int periodNumber = extractPeriodNumber(pickedPeriod);
        String shortPeriod = "Period " + periodNumber;
        String dateStr = pickedDate.toString();

        // Prevent duplicate Date + Period entries
        boolean duplicate = scheduledSessions.stream()
                .anyMatch(s -> s.getDate().equals(dateStr) && s.getPeriodNumber() == periodNumber);
        if (duplicate) {
            showAlert(Alert.AlertType.WARNING, "Duplicate Session",
                    "A session on " + dateStr + " during " + shortPeriod + " has already been added.");
            return;
        }

        String dayShort = pickedDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        scheduledSessions.add(new SessionRow(dateStr, dayShort, periodNumber, shortPeriod));
    }

    private void updateSessionSummary() {
        if (sessionSummaryLabel == null) return;
        int scheduledCount = scheduledSessions.size();
        int targetTotal = parsePositiveInt(getSafeText(totalSessionsField), 0);

        if (targetTotal > 0) {
            sessionSummaryLabel.setText(
                "Sessions Scheduled: " + scheduledCount + " of " + targetTotal + " (dynamically linked to Total Sessions)"
            );
        } else {
            sessionSummaryLabel.setText("Sessions Scheduled: " + scheduledCount + " (enter Total Sessions target)");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        navigateBackToClasses(event);
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        navigateBackToClasses(event);
    }

    @FXML
    private void handleCreate(ActionEvent event) {
        String courseTitle = getComboText(courseTitleCombo);
        String courseCode = getSafeText(courseCodeField).toUpperCase();
        String major = majorCombo != null && majorCombo.getValue() != null ? majorCombo.getValue() : "CEIT";
        String semester = semesterCombo != null && semesterCombo.getValue() != null ? semesterCombo.getValue() : "First Semester";
        String year = academicYearCombo != null && academicYearCombo.getValue() != null ? academicYearCombo.getValue() : "Second Year";
        String teacher = teacherCombo != null ? teacherCombo.getValue() : null;
        String maxStudentsStr = getSafeText(maxStudentsField);
        String totalSessionsStr = getSafeText(totalSessionsField);

        if (courseTitle.isEmpty() || courseCode.isEmpty() || teacher == null || maxStudentsStr.isEmpty() || totalSessionsStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Required Fields",
                    "Please complete all required fields marked with * (Course Title, Course Code, Assigned Teacher, Max Students, and Total Sessions).");
            return;
        }

        int capacity = parsePositiveInt(maxStudentsStr, -1);
        if (capacity <= 0) {
            showAlert(Alert.AlertType.WARNING, "Invalid Max Students",
                    "Max Students must be a valid positive number (e.g. 60).");
            return;
        }

        int totalSessions = parsePositiveInt(totalSessionsStr, -1);
        if (totalSessions <= 0) {
            showAlert(Alert.AlertType.WARNING, "Invalid Total Sessions",
                    "Total Sessions must be a valid positive number (e.g. 16).");
            return;
        }

        String scheduleSummary = scheduledSessions.isEmpty()
                ? totalSessions + " Sessions / Semester"
                : scheduledSessions.stream()
                    .limit(2)
                    .map(s -> s.getDay() + " (" + s.getPeriodLabel() + ")")
                    .collect(Collectors.joining(", ")) + " · " + totalSessions + " Sessions";

        String shortSection = buildSectionLabel(year);

        if (ClassesController.SHARED_CLASS_DATA != null) {
            ClassesController.SHARED_CLASS_DATA.add(0, new ClassesController.ClassItem(
                courseCode,
                courseTitle,
                major,
                year,
                semester,
                shortSection,
                capacity + " students",
                scheduleSummary,
                teacher + " (Assigned)",
                "#2563eb",
                "📘"
            ));
        }

        showAlert(Alert.AlertType.INFORMATION, "Course Offering Published",
                "Course Offering (" + courseCode + " - " + courseTitle + ") created with "
                + totalSessions + " total semester sessions (" + scheduledSessions.size() + " dates scheduled)!");

        navigateBackToClasses(event);
    }

    private void navigateBackToClasses(ActionEvent event) {
        try {
            Node source = (Node) event.getSource();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AdminClassesView.fxml"));
            Parent classesView = loader.load();

            StackPane contentArea = (StackPane) source.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(classesView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getSafeText(TextField field) {
        return (field == null || field.getText() == null) ? "" : field.getText().trim();
    }

    private String getComboText(ComboBox<String> combo) {
        if (combo == null) return "";
        if (combo.getValue() != null && !combo.getValue().trim().isEmpty()) {
            return combo.getValue().trim();
        }
        if (combo.isEditable() && combo.getEditor() != null && combo.getEditor().getText() != null) {
            return combo.getEditor().getText().trim();
        }
        return "";
    }

    private int extractPeriodNumber(String periodText) {
        if (periodText == null) return 1;
        for (char ch : periodText.toCharArray()) {
            if (Character.isDigit(ch)) {
                return Character.getNumericValue(ch);
            }
        }
        return 1;
    }

    private int parsePositiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String buildSectionLabel(String year) {
        return switch (year) {
            case "First Year" -> "Year 1 - Class 1A";
            case "Second Year" -> "Year 2 - Class 2A";
            case "Third Year" -> "Year 3 - Class 3A";
            case "Fourth Year" -> "Year 4 - Class 4A";
            case "Fifth Year" -> "Year 5 - Class 5A";
            default -> year + " - Class A";
        };
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static class CoursePreset {
        final String courseCode;
        final String courseName;
        final String deptCode;

        CoursePreset(String courseCode, String courseName, String deptCode) {
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.deptCode = deptCode;
        }
    }

    public static class SessionRow {
        private final StringProperty date;
        private final StringProperty day;
        private final int periodNumber;
        private final StringProperty periodLabel;

        public SessionRow(String date, String day, int periodNumber, String periodLabel) {
            this.date = new SimpleStringProperty(date);
            this.day = new SimpleStringProperty(day);
            this.periodNumber = periodNumber;
            this.periodLabel = new SimpleStringProperty(periodLabel);
        }

        public String getDate() { return date.get(); }
        public StringProperty dateProperty() { return date; }
        public String getDay() { return day.get(); }
        public StringProperty dayProperty() { return day; }
        public int getPeriodNumber() { return periodNumber; }
        public String getPeriodLabel() { return periodLabel.get(); }
        public StringProperty periodLabelProperty() { return periodLabel; }
    }
}