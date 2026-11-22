package application;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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

    // Academic & Course Details (courseCodeField removed — auto-resolved from CourseCatalogStore)
    @FXML private ComboBox<String> courseTitleCombo;
    @FXML private ComboBox<String> majorCombo;
    @FXML private ComboBox<String> semesterCombo;
    @FXML private ComboBox<String> academicYearCombo;

    // Custom Card Symbol & Badge Color + Live Preview
    @FXML private ComboBox<String> iconSymbolCombo;
    @FXML private ComboBox<String> badgeColorCombo;
    @FXML private StackPane iconPreviewPane;
    @FXML private Label iconPreviewLabel;

    // Faculty, Intake, Start Date & Total Sessions
    @FXML private ComboBox<String> teacherCombo;
    @FXML private TextField maxStudentsField;
    @FXML private DatePicker startDatePicker;
    @FXML private TextField totalSessionsField;

    // Weekly Timetable Slot Builder
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

    private final Map<String, String> colorPalette = new LinkedHashMap<>();
    private final List<WeeklySlot> weeklySlots = new ArrayList<>();
    private final ObservableList<SessionRow> generatedSessions = FXCollections.observableArrayList();
    private boolean updatingCourseText = false;

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
        initDropdowns();
        initSearchableCourseCombo();
        initIconAndColorControls();
        initSessionTable();

        if (startDatePicker != null) {
            startDatePicker.setValue(LocalDate.of(2026, 6, 1));
            startDatePicker.valueProperty().addListener((obs, o, n) -> regenerateSessions());
        }

        if (totalSessionsField != null) {
            totalSessionsField.setText("24");
            totalSessionsField.textProperty().addListener((obs, o, n) -> regenerateSessions());
        }

        weeklySlots.add(new WeeklySlot(DayOfWeek.TUESDAY, 6, 2));
        weeklySlots.add(new WeeklySlot(DayOfWeek.FRIDAY, 1, 2));
        renderWeeklySlotChips();
        regenerateSessions();

        lockTableColumns(sessionTable, 0.09, 0.22, 0.13, 0.34, 0.216);
    }

    private void initIconAndColorControls() {
        colorPalette.put("Royal Blue (#2563eb)", "#2563eb");
        colorPalette.put("Violet (#7c3aed)", "#7c3aed");
        colorPalette.put("Sky Blue (#0284c7)", "#0284c7");
        colorPalette.put("Purple (#9333ea)", "#9333ea");
        colorPalette.put("Crimson (#dc2626)", "#dc2626");
        colorPalette.put("Emerald (#16a34a)", "#16a34a");
        colorPalette.put("Orange (#ea580c)", "#ea580c");
        colorPalette.put("Teal (#0891b2)", "#0891b2");
        colorPalette.put("Indigo (#4f46e5)", "#4f46e5");
        colorPalette.put("Amber (#d97706)", "#d97706");
        colorPalette.put("Rose (#db2777)", "#db2777");

        if (iconSymbolCombo != null) {
            iconSymbolCombo.setItems(FXCollections.observableArrayList(
                "</>", "💾", "🌐", "🖥", "⚙", "<*>", "📘", "☕", "🔌", "🧠", "🔒", "☁", "🏗", "🔥", "📡", "⚡", "🤖", "🧪"
            ));
            iconSymbolCombo.setValue("</>");
            iconSymbolCombo.valueProperty().addListener((obs, o, n) -> updateIconPreview());
            if (iconSymbolCombo.getEditor() != null) {
                iconSymbolCombo.getEditor().textProperty().addListener((obs, o, n) -> updateIconPreview());
            }
        }

        if (badgeColorCombo != null) {
            badgeColorCombo.setItems(FXCollections.observableArrayList(colorPalette.keySet()));
            badgeColorCombo.setValue("Royal Blue (#2563eb)");
            badgeColorCombo.valueProperty().addListener((obs, o, n) -> updateIconPreview());
        }

        updateIconPreview();
    }

    private void updateIconPreview() {
        String sym = getComboText(iconSymbolCombo);
        if (sym.isEmpty()) sym = "📘";
        String colorHex = resolveSelectedColorHex();

        if (iconPreviewLabel != null) {
            iconPreviewLabel.setText(sym);
        }
        if (iconPreviewPane != null) {
            iconPreviewPane.setStyle("-fx-background-color: " + colorHex + "; -fx-background-radius: 8px;");
        }
    }

    private String resolveSelectedColorHex() {
        String selected = badgeColorCombo != null ? badgeColorCombo.getValue() : null;
        return colorPalette.getOrDefault(selected, "#2563eb");
    }

    private void initSearchableCourseCombo() {
        if (courseTitleCombo == null) return;

        // Pulls live active courses registered via Create New Course (CourseCatalogStore)
        FilteredList<String> filteredCourses = new FilteredList<>(
            FXCollections.observableArrayList(CourseCatalogStore.getActiveCourseLabels()),
            s -> true
        );
        courseTitleCombo.setItems(filteredCourses);

        courseTitleCombo.setOnAction(e -> {
            String sel = courseTitleCombo.getSelectionModel().getSelectedItem();
            if (sel != null) {
                updatingCourseText = true;
                courseTitleCombo.getEditor().setText(sel);
                updatingCourseText = false;

                // Default major to the course's home department, while still letting Admin switch
                // to any other target major (e.g. Mech, C, EC) for cross-major classes
                CourseCatalogStore.CourseItem course = CourseCatalogStore.findByLabelOrCode(sel);
                if (course != null && majorCombo != null && majorCombo.getItems().contains(course.deptCode)) {
                    majorCombo.setValue(course.deptCode);
                }
            }
        });

        if (courseTitleCombo.getEditor() != null) {
            courseTitleCombo.getEditor().textProperty().addListener((obs, oldText, newText) -> {
                if (updatingCourseText) return;
                String q = newText == null ? "" : newText.trim().toLowerCase();
                Platform.runLater(() -> {
                    filteredCourses.setPredicate(item -> q.isEmpty() || item.toLowerCase().contains(q));
                    if (!q.isEmpty() && !filteredCourses.isEmpty() && courseTitleCombo.isFocused()) {
                        courseTitleCombo.show();
                    }
                });
            });
        }
    }

    private void initDropdowns() {
        if (majorCombo != null) {
            majorCombo.setItems(FXCollections.observableArrayList(
                "CEIT", "C", "Mech", "EC", "EP", "McE", "ChE", "Tex", "Mn", "PE", "Arch", "Tele", "Met", "FE"
            ));
            String defaultDept = UserSession.departmentCode != null ? UserSession.departmentCode : "CEIT";
            majorCombo.setValue(defaultDept);

            majorCombo.valueProperty().addListener((obs, oldDept, newDept) -> {
                if (teacherCombo != null && newDept != null) {
                    teacherCombo.setItems(FXCollections.observableArrayList(
                        TeacherDirectoryStore.getTeacherNamesByDept(newDept)
                    ));
                }
            });
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
            String initialDept = (majorCombo != null && majorCombo.getValue() != null) ? majorCombo.getValue() : "CEIT";
            teacherCombo.setItems(FXCollections.observableArrayList(
                TeacherDirectoryStore.getTeacherNamesByDept(initialDept)
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
                "2 Periods (Double)",
                "3 Periods (Triple)",
                "4 Periods (Quad)"
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

    private int extractDurationCount(String durationText) {
        if (durationText == null || durationText.isEmpty()) return 1;
        char firstChar = durationText.trim().charAt(0);
        if (Character.isDigit(firstChar)) {
            int val = Character.getNumericValue(firstChar);
            return Math.max(1, Math.min(val, 4));
        }
        return 1;
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
        String selectedCourseText = getComboText(courseTitleCombo);
        CourseCatalogStore.CourseItem course = CourseCatalogStore.findByLabelOrCode(selectedCourseText);

        if (course == null) {
            showAlert(Alert.AlertType.WARNING, "Invalid Course",
                    "Please select a registered course from the searchable Course / Subject dropdown (or register it first in Create New Course).");
            return;
        }

        String courseCode = course.courseCode;
        String courseTitle = course.courseName;
        String major = majorCombo != null && majorCombo.getValue() != null ? majorCombo.getValue() : "CEIT";
        String semester = semesterCombo != null && semesterCombo.getValue() != null ? semesterCombo.getValue() : "First Semester";
        String year = academicYearCombo != null && academicYearCombo.getValue() != null ? academicYearCombo.getValue() : "Second Year";
        String teacher = teacherCombo != null ? teacherCombo.getValue() : null;
        String maxStudentsStr = getSafeText(maxStudentsField);
        String totalSessionsStr = getSafeText(totalSessionsField);
        LocalDate startDate = startDatePicker != null ? startDatePicker.getValue() : null;

        String customSymbol = getComboText(iconSymbolCombo);
        if (customSymbol.isEmpty()) customSymbol = "📘";
        String badgeColor = resolveSelectedColorHex();

        if (teacher == null || maxStudentsStr.isEmpty() || totalSessionsStr.isEmpty() || startDate == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Required Fields",
                    "Please complete all required fields marked with * (Course, Assigned Teacher, Max Students, Start Date, and Total Sessions).");
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

        // Auto-assign matching (major, year, semester) students to Roster & Daily Attendance Log
        // (Includes MySQL JDBC integration hooks inside ClassRosterStore.autoAssignStudentsForNewClass)
        LocalDate firstDate = !generatedSessions.isEmpty()
                ? LocalDate.parse(generatedSessions.get(0).getDate())
                : startDate;
        int enrolledCount = ClassRosterStore.autoAssignStudentsForNewClass(
                courseCode, courseTitle, major, year, semester, firstDate
        );

        String patternSummary = weeklySlots.stream()
                .map(WeeklySlot::toShortSummary)
                .collect(Collectors.joining(", "));
        String scheduleSummary = weeklySlots.stream()
                .map(WeeklySlot::toBadgeSchedule)
                .collect(Collectors.joining(" | "));
        String shortSection = buildSectionLabel(year);

        ClassesController.initSampleClassData();

        // =========================================================================
        // TODO (DB Integration — Matches schema.sql):
        // 1. Lookup course_id from `course` WHERE course_code = courseCode.
        // 2. Lookup teacher_id from `teacher` WHERE full_name = teacher.
        // 3. Insert into `course_offering` (teacher_id, course_id, year, total_sessions, term).
        // 4. Batch-insert `generatedSessions` into `class_session` (offering_id, session_date, period).
        // =========================================================================
        ClassesController.SHARED_CLASS_DATA.add(0, new ClassesController.ClassItem(
            courseCode,
            courseTitle,
            major,
            year,
            semester,
            shortSection,
            enrolledCount + " / " + capacity + " students",
            scheduleSummary,
            teacher,
            badgeColor,
            customSymbol
        ));

        showAlert(Alert.AlertType.INFORMATION, "Class Created & Published",
                String.format(Locale.ENGLISH,
                    "Class (%s - %s) has been published to both Admin and Lecturer views!\n"
                    + "Timetable: %s (Starting %s, %d sessions)\n"
                    + "Auto-Assigned Students: %d students (%s · %s · %s)",
                    courseCode, courseTitle, patternSummary, startDate, generatedSessions.size(),
                    enrolledCount, major, year, semester));

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
        if (combo.isEditable() && combo.getEditor() != null && combo.getEditor().getText() != null && !combo.getEditor().getText().trim().isEmpty()) {
            return combo.getEditor().getText().trim();
        }
        if (combo.getValue() != null && !combo.getValue().trim().isEmpty()) {
            return combo.getValue().trim();
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
            case "First Year"  -> "Year 1 - Class 1A";
            case "Second Year" -> "Year 2 - Class 2A";
            case "Third Year"  -> "Year 3 - Class 3A";
            case "Fourth Year" -> "Year 4 - Class 4A";
            case "Fifth Year"  -> "Year 5 - Class 5A";
            default            -> year + " - Class A";
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
        final int duration;

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

        String toBadgeSchedule() {
            String dayShort = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            int endPeriod = startPeriod + duration - 1;
            return dayShort + " " + PERIOD_START_24H[startPeriod] + " - " + PERIOD_END_24H[endPeriod];
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
}