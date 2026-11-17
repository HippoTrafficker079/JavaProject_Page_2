package application;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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

    // Faculty, Intake, Start Date & Total Sessions
    @FXML private ComboBox<String> teacherCombo;
    @FXML private TextField maxStudentsField;
    @FXML private DatePicker startDatePicker;
    @FXML private TextField totalSessionsField;

    // Weekly Timetable Slot Builder (Supports Double Periods & Different Periods per Day)
    @FXML private ComboBox<String> slotDayCombo;
    @FXML private ComboBox<String> slotPeriodCombo;
    @FXML private ComboBox<String> slotDurationCombo;
    @FXML private FlowPane weeklySlotsPane;

    // Live Generated Session Preview Table
    @FXML private TableView<SessionRow> sessionTable;
    @FXML private TableColumn<SessionRow, String> colSessionNo;
    @FXML private TableColumn<SessionRow, String> colSessionDate;
    @FXML private TableColumn<SessionRow, String> colSessionDay;
    @FXML private TableColumn<SessionRow, String> colSessionPeriod;
    @FXML private TableColumn<SessionRow, String> colSessionAction;
    @FXML private Label sessionSummaryLabel;

    // Exam & Assessment Weightage Distribution
    @FXML private TextField tutorialWeightField;
    @FXML private TextField assignmentWeightField;
    @FXML private TextField labTestWeightField;
    @FXML private TextField practicalWeightField;
    @FXML private TextField finalExamWeightField;
    @FXML private Label totalWeightageBadge;

    private final Map<String, CoursePreset> courseCatalog = new LinkedHashMap<>();
    private final List<WeeklySlot> weeklySlots = new ArrayList<>();
    private final ObservableList<SessionRow> generatedSessions = FXCollections.observableArrayList();

    private static final String[] PERIOD_START_TIMES = {
        "", "8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM",
        "12:00 PM", "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM"
    };

    private static final String[] PERIOD_END_TIMES = {
        "", "8:50 AM", "9:50 AM", "10:50 AM", "11:50 AM",
        "12:50 PM", "1:50 PM", "2:50 PM", "3:50 PM", "4:50 PM"
    };

    @FXML
    public void initialize() {
        initCourseCatalog();
        initDropdowns();
        initSessionTable();
        initWeightageFields();

        if (startDatePicker != null) {
            startDatePicker.setValue(LocalDate.of(2026, 6, 1));
            startDatePicker.valueProperty().addListener((obs, o, n) -> regenerateSessions());
        }

        if (totalSessionsField != null) {
            totalSessionsField.setText("24");
            totalSessionsField.textProperty().addListener((obs, o, n) -> regenerateSessions());
        }

        // Default example matching EM-4012 on YTU timetable:
        // Tuesday Periods 6-7 (1:00 - 2:50 PM) & Friday Periods 1-2 (8:00 - 9:50 AM)
        weeklySlots.add(new WeeklySlot(DayOfWeek.TUESDAY, 6, 2));
        weeklySlots.add(new WeeklySlot(DayOfWeek.FRIDAY, 1, 2));
        renderWeeklySlotChips();
        regenerateSessions();
    }

    private void initWeightageFields() {
        applyDefaultWeightage();

        for (TextField tf : List.of(tutorialWeightField, assignmentWeightField, labTestWeightField, practicalWeightField, finalExamWeightField)) {
            if (tf != null) {
                tf.textProperty().addListener((obs, o, n) -> updateWeightageTotal());
            }
        }
        updateWeightageTotal();
    }

    private void applyDefaultWeightage() {
        if (tutorialWeightField != null) tutorialWeightField.setText("10");
        if (assignmentWeightField != null) assignmentWeightField.setText("10");
        if (labTestWeightField != null) labTestWeightField.setText("10");
        if (practicalWeightField != null) practicalWeightField.setText("10");
        if (finalExamWeightField != null) finalExamWeightField.setText("60");
    }

    @FXML
    private void handleResetWeightage(ActionEvent event) {
        applyDefaultWeightage();
        updateWeightageTotal();
    }

    private void updateWeightageTotal() {
        if (totalWeightageBadge == null) return;

        double tutorial = parseWeight(getSafeText(tutorialWeightField));
        double assignment = parseWeight(getSafeText(assignmentWeightField));
        double labTest = parseWeight(getSafeText(labTestWeightField));
        double practical = parseWeight(getSafeText(practicalWeightField));
        double finalExam = parseWeight(getSafeText(finalExamWeightField));

        if (tutorial < 0 || assignment < 0 || labTest < 0 || practical < 0 || finalExam < 0) {
            totalWeightageBadge.setText("Total Weightage: Invalid Input");
            totalWeightageBadge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-padding: 4px 12px;");
            return;
        }

        double total = tutorial + assignment + labTest + practical + finalExam;
        totalWeightageBadge.setText(String.format(Locale.ENGLISH, "Total Weightage: %.1f%%", total));

        if (Math.abs(total - 100.0) < 0.01) {
            totalWeightageBadge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-padding: 4px 12px;");
        } else if (total < 100.0) {
            totalWeightageBadge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-padding: 4px 12px;");
        } else {
            totalWeightageBadge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-padding: 4px 12px;");
        }
    }

    private double parseWeight(String text) {
        if (text == null || text.isEmpty()) return 0.0;
        try {
            double val = Double.parseDouble(text);
            return val < 0 ? -1.0 : val;
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    private void initCourseCatalog() {
        addCoursePreset("EM-4012", "Engineering Mathematics IV", "CEIT");
        addCoursePreset("CEIT-4021", "Advanced Java Programming", "CEIT");
        addCoursePreset("CEIT-4051", "Data Structure and Algorithms", "CEIT");
        addCoursePreset("CEIT-4002", "Digital Communications", "CEIT");
        addCoursePreset("CEIT-4041", "Database Management System", "CEIT");
        addCoursePreset("E-4031", "English IV", "CEIT");
        addCoursePreset("CEIT-3101", "Advanced Database Systems", "CEIT");
        addCoursePreset("ME-3101", "Thermodynamics II", "ME");
        addCoursePreset("EC-3101", "Digital Signal Processing", "EC");
        addCoursePreset("CE301", "Structural Mechanics", "CE");
        addCoursePreset("EP302", "Power Transmission Systems", "EP");

        if (courseTitleCombo != null) {
            ObservableList<String> allTitles = FXCollections.observableArrayList(courseCatalog.keySet());
            courseTitleCombo.setItems(allTitles);

            courseTitleCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && courseCatalog.containsKey(newVal)) {
                    CoursePreset preset = courseCatalog.get(newVal);
                    if (courseCodeField != null) courseCodeField.setText(preset.courseCode);
                    if (majorCombo != null) majorCombo.setValue(preset.deptCode);
                }
            });

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
            semesterCombo.setValue("Second Semester");
        }

        if (academicYearCombo != null) {
            academicYearCombo.setItems(FXCollections.observableArrayList(
                "First Year", "Second Year", "Third Year", "Fourth Year", "Fifth Year"
            ));
            academicYearCombo.setValue("Second Year");
        }

        if (teacherCombo != null) {
            teacherCombo.setItems(FXCollections.observableArrayList(
                "Daw Hnin Hnin", "Daw Khin San Su Kyi", "Dr. Nyein Nyein Oo", "Dr. Khine Thin Zar",
                "Daw Wint Theingi Zaw", "Dr. Thanda Win", "Dr. Hla Myint", "Dr. Aung Naing"
            ));
        }

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
                "2 Periods (Double)"
            ));
            slotDurationCombo.setValue("2 Periods (Double)");
        }
    }

    @FXML
    private void handleAddWeeklySlot(ActionEvent event) {
        String dayName = slotDayCombo != null ? slotDayCombo.getValue() : null;
        String periodStr = slotPeriodCombo != null ? slotPeriodCombo.getValue() : null;
        String durationStr = slotDurationCombo != null ? slotDurationCombo.getValue() : "1 Period (Single)";

        if (dayName == null || periodStr == null) return;

        DayOfWeek day = DayOfWeek.valueOf(dayName.toUpperCase(Locale.ENGLISH));
        int startPeriod = extractPeriodNumber(periodStr);
        int duration = (durationStr != null && durationStr.startsWith("2")) ? 2 : 1;

        if (startPeriod + duration - 1 > 9) {
            showAlert(Alert.AlertType.WARNING, "Exceeds Daily Periods",
                    "Period 9 is the final period of the day, so a 2-period block cannot start at Period 9.");
            return;
        }

        // Check for overlapping periods on the same day
        int newEnd = startPeriod + duration - 1;
        for (WeeklySlot existing : weeklySlots) {
            if (existing.day == day) {
                int existingEnd = existing.startPeriod + existing.duration - 1;
                boolean overlaps = startPeriod <= existingEnd && newEnd >= existing.startPeriod;
                if (overlaps) {
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
            Label emptyLbl = new Label("No weekly slots added yet. Pick a Day, Start Period, and Length above, then click + Add Slot.");
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

    private void initSessionTable() {
        if (sessionTable == null) return;

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
    }

    /**
     * Expands each WeeklySlot (single or double period) across calendar dates starting from startDate
     * until totalSessionsNeeded individual periods are generated.
     */
    private void regenerateSessions() {
        generatedSessions.clear();

        LocalDate startDate = startDatePicker != null ? startDatePicker.getValue() : null;
        int totalSessionsNeeded = parsePositiveInt(getSafeText(totalSessionsField), 0);

        if (startDate == null || totalSessionsNeeded <= 0 || weeklySlots.isEmpty()) {
            updateSummaryLabel();
            return;
        }

        int cappedSessions = Math.min(totalSessionsNeeded, 200);
        LocalDate currentDate = startDate;
        int sessionCounter = 1;

        while (generatedSessions.size() < cappedSessions) {
            DayOfWeek dow = currentDate.getDayOfWeek();
            for (WeeklySlot slot : weeklySlots) {
                if (slot.day == dow) {
                    String dayShort = dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    String dateStr = currentDate.toString();

                    for (int offset = 0; offset < slot.duration && generatedSessions.size() < cappedSessions; offset++) {
                        int p = slot.startPeriod + offset;
                        String periodDisplay = "Period " + p + " (" + PERIOD_START_TIMES[p] + " – " + PERIOD_END_TIMES[p] + ")";
                        generatedSessions.add(new SessionRow(
                            String.valueOf(sessionCounter++),
                            dateStr,
                            dayShort,
                            p,
                            periodDisplay
                        ));
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

        if (weeklySlots.isEmpty()) {
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
                "Generated " + generatedSessions.size() + " period sessions (" + patternSummary + " · " + firstDate + " → " + lastDate + ")"
            );
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
        LocalDate startDate = startDatePicker != null ? startDatePicker.getValue() : null;

        if (courseTitle.isEmpty() || courseCode.isEmpty() || teacher == null || maxStudentsStr.isEmpty() || totalSessionsStr.isEmpty() || startDate == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Required Fields",
                    "Please complete all required fields marked with * (Course Title, Course Code, Assigned Teacher, Max Students, Start Date, and Total Sessions).");
            return;
        }

        if (weeklySlots.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Weekly Slots Defined",
                    "Please add at least one weekly timetable slot (Day, Start Period, and Length) for the class.");
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
                    "Total Sessions must be a valid positive number (e.g. 24).");
            return;
        }

        // Validate Exam & Assessment Weightage Distribution (must sum to 100%)
        double tutorialW = parseWeight(getSafeText(tutorialWeightField));
        double assignmentW = parseWeight(getSafeText(assignmentWeightField));
        double labTestW = parseWeight(getSafeText(labTestWeightField));
        double practicalW = parseWeight(getSafeText(practicalWeightField));
        double finalExamW = parseWeight(getSafeText(finalExamWeightField));

        if (tutorialW < 0 || assignmentW < 0 || labTestW < 0 || practicalW < 0 || finalExamW < 0) {
            showAlert(Alert.AlertType.WARNING, "Invalid Assessment Weightage",
                    "Assessment weightage percentages must be valid non-negative numbers.");
            return;
        }

        double totalWeight = tutorialW + assignmentW + labTestW + practicalW + finalExamW;
        if (Math.abs(totalWeight - 100.0) >= 0.01) {
            showAlert(Alert.AlertType.WARNING, "Weightage Must Equal 100%",
                    String.format(Locale.ENGLISH, "Your current assessment weightage sums to %.1f%%. Please adjust the values so they equal 100.0%%.", totalWeight));
            return;
        }

        String patternSummary = weeklySlots.stream()
                .map(WeeklySlot::toShortSummary)
                .collect(Collectors.joining(", "));
        String scheduleSummary = patternSummary + " · " + generatedSessions.size() + " Sessions";
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

        showAlert(Alert.AlertType.INFORMATION, "Class Created",
                String.format(Locale.ENGLISH,
                    "Class (%s - %s) has been successfully created and published!\n"
                    + "Timetable: %s (Starting %s, %d sessions)\n"
                    + "Grading Split: Tutorial %.0f%% | Assignment %.0f%% | Lab %.0f%% | Practical %.0f%% | Final %.0f%%",
                    courseCode, courseTitle, patternSummary, startDate, generatedSessions.size(),
                    tutorialW, assignmentW, labTestW, practicalW, finalExamW));

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

    private static class WeeklySlot {
        final DayOfWeek day;
        final int startPeriod;
        final int duration; // 1 = single period, 2 = double period

        WeeklySlot(DayOfWeek day, int startPeriod, int duration) {
            this.day = day;
            this.startPeriod = startPeriod;
            this.duration = duration;
        }

        String formatPeriods() {
            int endPeriod = startPeriod + duration - 1;
            return duration == 1 ? "Period " + startPeriod : "Periods " + startPeriod + "–" + endPeriod;
        }

        String toShortSummary() {
            String dayShort = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            int endPeriod = startPeriod + duration - 1;
            return duration == 1 ? dayShort + " P" + startPeriod : dayShort + " P" + startPeriod + "–" + endPeriod;
        }

        String toChipLabel() {
            String dayShort = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            int endPeriod = startPeriod + duration - 1;
            String timeSpan = PERIOD_START_TIMES[startPeriod] + " – " + PERIOD_END_TIMES[endPeriod];
            return dayShort + " · " + formatPeriods() + " (" + timeSpan + ")";
        }
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
}