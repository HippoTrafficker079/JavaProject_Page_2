package application;

import application.dao.CourseDAO;
import application.dao.CourseOfferingDAO;
import application.dao.CourseOfferingDAO.SessionInput;
import application.dao.DatabaseConnector;
import application.dao.DepartmentDAO;
import javafx.application.Platform;
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
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminCreateClassController {

    @FXML
    private VBox rootPane;

    // Academic & Course Details (courseCodeField removed — auto-resolved from
    // CourseCatalogStore)
    @FXML
    private ComboBox<String> courseTitleCombo;
    @FXML
    private ComboBox<String> majorCombo;
    @FXML
    private ComboBox<String> semesterCombo;
    @FXML
    private ComboBox<String> academicYearCombo;

    // Custom Card Symbol & Badge Color + Live Preview
    @FXML
    private ComboBox<String> iconSymbolCombo;
    @FXML
    private ComboBox<String> badgeColorCombo;
    @FXML
    private StackPane iconPreviewPane;
    @FXML
    private Label iconPreviewLabel;

    // Faculty, Intake, Start Date & Total Sessions
    @FXML
    private ComboBox<String> teacherCombo;
    @FXML
    private TextField maxStudentsField;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private TextField totalSessionsField;

    // Weekly Timetable Slot Builder
    @FXML
    private ComboBox<String> slotDayCombo;
    @FXML
    private ComboBox<String> slotPeriodCombo;
    @FXML
    private ComboBox<String> slotDurationCombo;
    @FXML
    private FlowPane weeklySlotsPane;

    // Live Generated Session Preview Table
    @FXML
    private TableView<SessionRow> sessionTable;
    @FXML
    private TableColumn<SessionRow, String> colSessionNo;
    @FXML
    private TableColumn<SessionRow, String> colSessionDate;
    @FXML
    private TableColumn<SessionRow, String> colSessionDay;
    @FXML
    private TableColumn<SessionRow, String> colSessionPeriod;
    @FXML
    private TableColumn<SessionRow, String> colSessionAction;
    @FXML
    private Label sessionSummaryLabel;

    // Exam & Assessment Distribution
    @FXML
    private TextField tutorialWeightField;
    @FXML
    private TextField assignmentWeightField;
    @FXML
    private TextField labTestWeightField;
    @FXML
    private TextField practicalWeightField;
    @FXML
    private TextField finalExamWeightField;
    @FXML
    private Label weightTotalBadge;
    @FXML
    private ProgressBar weightProgressBar;
    
    private boolean isUpdatingYearOrSem = false;
    private boolean yearWasAutoSelected = false;
    private boolean semWasAutoSelected = false;

    private final Map<String, String> colorPalette = new LinkedHashMap<>();
    private final List<WeeklySlot> weeklySlots = new ArrayList<>();
    private final ObservableList<SessionRow> generatedSessions = FXCollections.observableArrayList();
    private final ObservableList<String> courseBackingItems = FXCollections.observableArrayList();
    private boolean isUpdatingCourse = false;
    private boolean isSyncingSessionCount = false;

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
        CourseCatalogStore.reload();
        initDropdowns();
        initSearchableCourseCombo();
        initIconAndColorControls();
        initSessionTable();
        initWeightageControls();

        if (startDatePicker != null) {
            startDatePicker.setValue(LocalDate.of(2026, 6, 1));
            startDatePicker.valueProperty().addListener((obs, o, n) -> regenerateSessions());
        }

        if (totalSessionsField != null) {
            totalSessionsField.setText("24");
            totalSessionsField.textProperty().addListener((obs, o, n) -> {
                if (!isSyncingSessionCount) {
                    regenerateSessions();
                }
            });
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
                    "</>", "💾", "🌐", "🖥", "⚙", "<*>", "📘", "☕", "🔌", "🧠", "🔒", "☁", "🏗", "🔥", "📡", "⚡", "🤖",
                    "🧪"));
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
        if (sym.isEmpty())
            sym = "📘";
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

    private void onAcademicYearChanged() {
        if (courseTitleCombo == null) return;

        String selectedYear = academicYearCombo != null ? academicYearCombo.getValue() : null;

        isUpdatingCourse = true;
        TextField editor = courseTitleCombo.getEditor();
        String currentText = (editor != null && editor.getText() != null) ? editor.getText().trim() : "";
        CourseCatalogStore.CourseItem currentCourse = CourseCatalogStore.findByLabelOrCode(currentText);

        // Filter strictly by Year only (semester = null)
        List<String> filtered = CourseCatalogStore.getActiveCourseLabelsByYearAndSemester(selectedYear, null);
        courseBackingItems.setAll(filtered);

        if (currentCourse != null && selectedYear != null) {
            String courseYear = CourseCatalogStore.extractAcademicYearFromCode(currentCourse.courseCode);
            if (courseYear == null || !courseYear.equalsIgnoreCase(selectedYear)) {
                courseTitleCombo.getSelectionModel().clearSelection();
                courseTitleCombo.setValue(null);
                if (editor != null) editor.clear();
            }
        }
        isUpdatingCourse = false;
    }

    private void initSearchableCourseCombo() {
        if (courseTitleCombo == null) return;

        courseBackingItems.setAll(CourseCatalogStore.getActiveCourseLabels());
        courseTitleCombo.setItems(courseBackingItems);

        TextField editor = courseTitleCombo.getEditor();
        if (editor == null) return;

        courseTitleCombo.setOnShowing(e -> {
            if (isUpdatingCourse) return;
            String text = editor.getText() != null ? editor.getText().trim() : "";
            boolean isSelectedCourse =
                    (courseTitleCombo.getValue() != null && courseTitleCombo.getValue().equals(text))
                    || CourseCatalogStore.findByLabelOrCode(text) != null;

            if (text.isEmpty() || isSelectedCourse) {
                isUpdatingCourse = true;
                String activeYear = academicYearCombo != null ? academicYearCombo.getValue() : null;
                List<String> baseList = CourseCatalogStore.getActiveCourseLabelsByYearAndSemester(activeYear, null);
                courseBackingItems.setAll(baseList);
                if (isSelectedCourse && baseList.contains(text)) {
                    courseTitleCombo.getSelectionModel().select(text);
                    editor.setText(text);
                } else {
                    courseTitleCombo.getSelectionModel().clearSelection();
                }
                isUpdatingCourse = false;
            }
        });

        editor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.BACK_SPACE) {
                String text = editor.getText() != null ? editor.getText() : "";
                boolean isSelectedCourse =
                        (courseTitleCombo.getValue() != null && courseTitleCombo.getValue().equals(text))
                        || CourseCatalogStore.findByLabelOrCode(text) != null;

                if (!text.isEmpty() && isSelectedCourse) {
                    event.consume();
                    isUpdatingCourse = true;
                    courseTitleCombo.hide();
                    courseTitleCombo.getSelectionModel().clearSelection();
                    courseTitleCombo.setValue(null);
                    editor.clear();

                    isUpdatingYearOrSem = true;
                    if (yearWasAutoSelected && academicYearCombo != null) {
                        academicYearCombo.setValue(null);
                        academicYearCombo.getSelectionModel().clearSelection();
                        yearWasAutoSelected = false;
                    }
                    if (semWasAutoSelected && semesterCombo != null) {
                        semesterCombo.setValue(null);
                        semesterCombo.getSelectionModel().clearSelection();
                        semWasAutoSelected = false;
                    }
                    isUpdatingYearOrSem = false;

                    String activeYear = academicYearCombo != null ? academicYearCombo.getValue() : null;
                    courseBackingItems.setAll(
                            CourseCatalogStore.getActiveCourseLabelsByYearAndSemester(activeYear, null));
                    courseTitleCombo.getSelectionModel().clearSelection();
                    isUpdatingCourse = false;

                    Platform.runLater(() -> {
                        if (courseTitleCombo.isFocused() || editor.isFocused()) {
                            courseTitleCombo.show();
                        }
                    });
                }
            }
        });

        // Picking a course auto-fills Year (and fills Semester only if still unselected)
        courseTitleCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingCourse || newVal == null) return;
            CourseCatalogStore.CourseItem course = CourseCatalogStore.findByLabelOrCode(newVal);
            if (course == null) return;

            String derivedYear = CourseCatalogStore.extractAcademicYearFromCode(course.courseCode);
            String derivedSem = CourseCatalogStore.extractSemesterFromCode(course.courseCode);

            isUpdatingYearOrSem = true;
            if (derivedYear != null && academicYearCombo != null && academicYearCombo.getItems().contains(derivedYear)) {
                boolean wasEmpty = (academicYearCombo.getValue() == null);
                academicYearCombo.setValue(derivedYear);
                if (wasEmpty) yearWasAutoSelected = true;
            }
            if (derivedSem != null && semesterCombo != null
                    && semesterCombo.getValue() == null
                    && semesterCombo.getItems().contains(derivedSem)) {
                semesterCombo.setValue(derivedSem);
                semWasAutoSelected = true;
            }
            isUpdatingYearOrSem = false;
        });

        editor.textProperty().addListener((obs, oldText, newText) -> {
            if (isUpdatingCourse) return;
            String selected = courseTitleCombo.getSelectionModel().getSelectedItem();
            if (selected != null && selected.equals(newText)) return;

            isUpdatingCourse = true;
            String typed = newText == null ? "" : newText;
            String q = typed.trim().toLowerCase();

            String activeYear = academicYearCombo != null ? academicYearCombo.getValue() : null;
            List<String> matches = CourseCatalogStore.getActiveCourseLabelsByYearAndSemester(activeYear, null)
                    .stream()
                    .filter(label -> q.isEmpty() || label.toLowerCase().contains(q))
                    .collect(Collectors.toList());

            courseTitleCombo.getSelectionModel().clearSelection();
            courseBackingItems.setAll(matches);
            courseTitleCombo.getSelectionModel().clearSelection();

            if (!typed.equals(editor.getText())) {
                editor.setText(typed);
                editor.positionCaret(typed.length());
            }
            isUpdatingCourse = false;

            if (!matches.isEmpty() && !courseTitleCombo.isShowing()
                    && (courseTitleCombo.isFocused() || editor.isFocused())) {
                courseTitleCombo.show();
            }
        });
    }

    private void lockComboPopupWidth(ComboBox<String> combo) {
        if (combo == null)
            return;

        Runnable bindWidth = () -> {
            if (combo.getSkin() instanceof ComboBoxListViewSkin<?> skin
                    && skin.getPopupContent() instanceof ListView<?> listView) {
                if (!listView.prefWidthProperty().isBound()) {
                    listView.minWidthProperty().bind(combo.widthProperty());
                    listView.prefWidthProperty().bind(combo.widthProperty());
                    listView.maxWidthProperty().bind(combo.widthProperty());
                }
            }
        };

        combo.skinProperty().addListener((obs, oldSkin, newSkin) -> bindWidth.run());
        combo.addEventHandler(ComboBox.ON_SHOWING, e -> bindWidth.run());
    }

    private void initDropdowns() {
        if (majorCombo != null) {
            majorCombo.setItems(FXCollections.observableArrayList(
                    "CEIT", "C", "Mech", "EC", "EP", "McE", "ChE", "Tex", "Mn", "PE", "Arch", "Tele", "Met", "FE"));
            String defaultDept = UserSession.departmentCode != null ? UserSession.departmentCode : "CEIT";
            majorCombo.setValue(defaultDept);
        }

        if (academicYearCombo != null) {
            academicYearCombo.setItems(FXCollections.observableArrayList(
                    "First Year", "Second Year", "Third Year", "Fourth Year", "Fifth Year"));
            academicYearCombo.setValue(null);
            academicYearCombo.getSelectionModel().clearSelection();

            academicYearCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (isUpdatingYearOrSem) return;
                yearWasAutoSelected = false;
                onAcademicYearChanged();
            });
        }

        if (semesterCombo != null) {
            semesterCombo.setItems(FXCollections.observableArrayList(
                    "First Semester", "Second Semester"));
            semesterCombo.setValue(null);
            semesterCombo.getSelectionModel().clearSelection();
        }

        // Load all active faculty from MySQL (supports cross-major teaching), with
        // fallback to directory store
        if (teacherCombo != null) {
            List<String> teachers = loadAllActiveTeachersFromDb();
            if (teachers.isEmpty()) {
                teachers = TeacherDirectoryStore.getTeacherNamesByDept(null);
            }
            teacherCombo.setItems(FXCollections.observableArrayList(teachers));
        }

        if (slotDayCombo != null) {
            slotDayCombo.setItems(FXCollections.observableArrayList(
                    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"));
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
                    "Period 9 (4:00 – 4:50 PM)"));
            slotPeriodCombo.setValue("Period 6 (1:00 – 1:50 PM)");
        }

        if (slotDurationCombo != null) {
            slotDurationCombo.setItems(FXCollections.observableArrayList(
                    "1 Period (Single)",
                    "2 Periods (Double)",
                    "3 Periods (Triple)",
                    "4 Periods (Quad)"));
            slotDurationCombo.setValue("2 Periods (Double)");
        }
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

    // ==================== EXAM & ASSESSMENT WEIGHTAGE ====================

    private void initWeightageControls() {
        TextField[] fields = {
                tutorialWeightField, assignmentWeightField, labTestWeightField,
                practicalWeightField, finalExamWeightField
        };
        for (TextField tf : fields) {
            if (tf == null)
                continue;
            tf.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.matches("\\d*(\\.\\d{0,2})?")) {
                    tf.setText(oldVal);
                    return;
                }
                updateWeightageIndicator();
            });
        }
        updateWeightageIndicator();
    }

    @FXML
    private void handlePresetTheory(ActionEvent event) {
        setWeightFields("15", "15", "10", "0", "60");
    }

    @FXML
    private void handlePresetLabHeavy(ActionEvent event) {
        setWeightFields("10", "10", "15", "15", "50");
    }

    @FXML
    private void handlePresetExamFocused(ActionEvent event) {
        setWeightFields("15", "15", "0", "0", "70");
    }

    @FXML
    private void handleAutoFillFinal(ActionEvent event) {
        BigDecimal others = parseWeight(tutorialWeightField)
                .add(parseWeight(assignmentWeightField))
                .add(parseWeight(labTestWeightField))
                .add(parseWeight(practicalWeightField));
        BigDecimal remaining = new BigDecimal("100.00").subtract(others);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            showAlert(Alert.AlertType.WARNING, "Weights Exceed 100%",
                    "The other 4 components already total " + others.stripTrailingZeros().toPlainString()
                            + "%. Lower them before auto-filling Final Exam.");
            return;
        }
        if (finalExamWeightField != null) {
            finalExamWeightField.setText(remaining.stripTrailingZeros().toPlainString());
        }
    }

    private void setWeightFields(String tut, String assign, String lab, String prac, String fin) {
        if (tutorialWeightField != null)
            tutorialWeightField.setText(tut);
        if (assignmentWeightField != null)
            assignmentWeightField.setText(assign);
        if (labTestWeightField != null)
            labTestWeightField.setText(lab);
        if (practicalWeightField != null)
            practicalWeightField.setText(prac);
        if (finalExamWeightField != null)
            finalExamWeightField.setText(fin);
        updateWeightageIndicator();
    }

    private BigDecimal parseWeight(TextField field) {
        if (field == null || field.getText() == null || field.getText().trim().isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(field.getText().trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal getTotalWeight() {
        return parseWeight(tutorialWeightField)
                .add(parseWeight(assignmentWeightField))
                .add(parseWeight(labTestWeightField))
                .add(parseWeight(practicalWeightField))
                .add(parseWeight(finalExamWeightField));
    }

    private void updateWeightageIndicator() {
        BigDecimal total = getTotalWeight();
        BigDecimal hundred = new BigDecimal("100.00");
        int cmp = total.compareTo(hundred);

        if (weightProgressBar != null) {
            double progress = Math.min(1.0, Math.max(0.0, total.doubleValue() / 100.0));
            weightProgressBar.setProgress(progress);
            if (cmp == 0) {
                weightProgressBar.setStyle("-fx-accent: #16a34a;");
            } else if (cmp < 0) {
                weightProgressBar.setStyle("-fx-accent: #d97706;");
            } else {
                weightProgressBar.setStyle("-fx-accent: #dc2626;");
            }
        }

        if (weightTotalBadge != null) {
            if (cmp == 0) {
                weightTotalBadge.setText("✔ Total: 100% — Ready");
                weightTotalBadge.setStyle(
                        "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 4px 12px; -fx-background-radius: 14px;");
            } else if (cmp < 0) {
                BigDecimal rem = hundred.subtract(total);
                weightTotalBadge.setText("⚠ Total: " + total.stripTrailingZeros().toPlainString()
                        + "% (" + rem.stripTrailingZeros().toPlainString() + "% left)");
                weightTotalBadge.setStyle(
                        "-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 4px 12px; -fx-background-radius: 14px;");
            } else {
                BigDecimal over = total.subtract(hundred);
                weightTotalBadge.setText("✖ Total: " + total.stripTrailingZeros().toPlainString()
                        + "% (" + over.stripTrailingZeros().toPlainString() + "% over)");
                weightTotalBadge.setStyle(
                        "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 4px 12px; -fx-background-radius: 14px;");
            }
        }
    }

    // ==================== WEEKLY TIMETABLE SLOTS & SESSIONS ====================

    @FXML
    private void handleAddWeeklySlot(ActionEvent event) {
        String dayName = slotDayCombo != null ? slotDayCombo.getValue() : null;
        String periodStr = slotPeriodCombo != null ? slotPeriodCombo.getValue() : null;
        String durationStr = slotDurationCombo != null ? slotDurationCombo.getValue() : "1 Period (Single)";

        if (dayName == null || periodStr == null)
            return;

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
        if (durationText == null || durationText.isEmpty())
            return 1;
        char firstChar = durationText.trim().charAt(0);
        if (Character.isDigit(firstChar)) {
            int val = Character.getNumericValue(firstChar);
            return Math.max(1, Math.min(val, 4));
        }
        return 1;
    }

    private void renderWeeklySlotChips() {
        if (weeklySlotsPane == null)
            return;
        weeklySlotsPane.getChildren().clear();

        if (weeklySlots.isEmpty()) {
            Label emptyLbl = new Label(
                    "No weekly slots added yet. Pick a Day, Start Period, and Length above, then click + Add Slot.");
            emptyLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-style: italic;");
            weeklySlotsPane.getChildren().add(emptyLbl);
            return;
        }

        for (WeeklySlot slot : weeklySlots) {
            HBox chip = new HBox(6.0);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.setStyle(
                    "-fx-background-color: #dbeafe; -fx-border-color: #93c5fd; " +
                            "-fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 4px 10px;");

            Label textLbl = new Label(slot.toChipLabel());
            textLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");

            Button closeBtn = new Button("✕");
            closeBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #1d4ed8; -fx-font-size: 10px; " +
                            "-fx-font-weight: bold; -fx-padding: 0 2px; -fx-cursor: hand;");
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
        if (sessionTable == null)
            return;

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
                                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3px 8px;");
                removeBtn.setOnMouseEntered(e -> removeBtn.setStyle(
                        "-fx-background-color: #fee2e2; -fx-text-fill: #e11d48; -fx-background-radius: 5px; " +
                                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3px 8px;"));
                removeBtn.setOnMouseExited(e -> removeBtn.setStyle(
                        "-fx-background-color: transparent; -fx-text-fill: #475569; " +
                                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3px 8px;"));
                removeBtn.setOnAction(e -> {
                    SessionRow row = getTableView().getItems().get(getIndex());
                    generatedSessions.remove(row);
                    renumberSessions();

                    // Drop Total Sessions field to match remaining rows without regenerating
                    if (totalSessionsField != null) {
                        isSyncingSessionCount = true;
                        totalSessionsField.setText(String.valueOf(generatedSessions.size()));
                        isSyncingSessionCount = false;
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

                    for (int offset = 0; offset < slot.duration
                            && generatedSessions.size() < cappedSessions; offset++) {
                        int p = slot.startPeriod + offset;
                        String periodDisplay = "Period " + p + " (" + PERIOD_START_TIMES[p] + " – "
                                + PERIOD_END_TIMES[p] + ")";
                        generatedSessions.add(new SessionRow(
                                String.valueOf(sessionCounter++),
                                dateStr,
                                dayShort,
                                p,
                                periodDisplay));
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
        if (sessionSummaryLabel == null)
            return;

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
                    "Generated " + generatedSessions.size() + " period sessions (" + patternSummary + " · " + firstDate
                            + " → " + lastDate + ")");
        }
    }

    // ==================== NAVIGATION & SUBMIT ====================

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
        // List<String> multiMajor = new ArrayList<>(); --If targetDept supports
        // multiMajor
        String semester = semesterCombo != null ? semesterCombo.getValue() : null;
        String year = academicYearCombo != null ? academicYearCombo.getValue() : null;

        if (year == null || year.isBlank() || semester == null || semester.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Missing Required Fields",
                    "Please select both Academic Year and Semester before creating the class.");
            return;
        }
        String teacher = teacherCombo != null ? teacherCombo.getValue() : null;
        String maxStudentsStr = getSafeText(maxStudentsField);
        String totalSessionsStr = getSafeText(totalSessionsField);
        LocalDate startDate = startDatePicker != null ? startDatePicker.getValue() : null;

        String customSymbol = getComboText(iconSymbolCombo);
        if (customSymbol.isEmpty())
            customSymbol = "📘";
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

        if (generatedSessions.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Class Sessions",
                    "There are 0 sessions in the schedule table. Please generate at least 1 session before publishing.");
            return;
        }

        if (generatedSessions.size() != totalSessions) {
            showAlert(Alert.AlertType.WARNING, "Session Count Conflict",
                    "Total Sessions is set to " + totalSessions + ", but the schedule table currently has "
                            + generatedSessions.size()
                            + " sessions. Please adjust Total Sessions or regenerate the schedule.");
            return;
        }

        // Validate Exam & Assessment Distribution (must total 100.00%)
        BigDecimal tutorialWeight = parseWeight(tutorialWeightField);
        BigDecimal assignmentWeight = parseWeight(assignmentWeightField);
        BigDecimal labTestWeight = parseWeight(labTestWeightField);
        BigDecimal practicalWeight = parseWeight(practicalWeightField);
        BigDecimal finalExamWeight = parseWeight(finalExamWeightField);

        BigDecimal weightSum = tutorialWeight.add(assignmentWeight)
                .add(labTestWeight).add(practicalWeight).add(finalExamWeight);
        if (weightSum.compareTo(new BigDecimal("100.00")) != 0) {
            showAlert(Alert.AlertType.WARNING, "Invalid Exam Distribution",
                    "Exam & Assessment weights must total exactly 100% (currently "
                            + weightSum.stripTrailingZeros().toPlainString()
                            + "%). Tip: click 'Auto-Fill Final Exam' to balance.");
            return;
        }

        // Auto-assign matching (major, year, semester) students to this offering's
        // Roster & Daily Log
        LocalDate firstDate = LocalDate.parse(generatedSessions.get(0).getDate());

        String patternSummary = weeklySlots.stream()
                .map(WeeklySlot::toShortSummary)
                .collect(Collectors.joining(", "));
        String scheduleSummary = weeklySlots.stream()
                .map(WeeklySlot::toBadgeSchedule)
                .collect(Collectors.joining(" | "));
        String shortSection = buildSectionLabel(year);

        if (ClassesController.SHARED_CLASS_DATA.isEmpty()) {
            ClassesController.initSampleClassData();
        }
        int offeringId = -1;
        System.out.println("Target Major: " + major);
        try (Connection conn = DatabaseConnector.getConnection()) {
            CourseOfferingDAO courseOfferingDAO = new CourseOfferingDAO();
            DepartmentDAO departmentDAO = new DepartmentDAO();
            CourseDAO courseDAO = new CourseDAO();
            int userDeptId = departmentDAO.getDeptIdByUserId(UserSession.userId);
            List<Integer> targetDeptIds = new ArrayList<>();
            // =========================================================================
            // if (multiMajor != null && !multiMajor.isEmpty()) {
            // try (Connection conn = DatabaseConnector.getConnection()) {
            // for (String eachMajor : multiMajor) {
            // if (eachMajor == null || eachMajor.isBlank())
            // continue;
            // int majorId = departmentDAO.getDeptIdByNameAndDept(conn, null, eachMajor);
            // targetDeptIds.add(majorId);
            // }
            // } catch (SQLException e) {
            // e.printStackTrace();
            // }
            // }
            // =========================================================================
            int teacherDeptId = departmentDAO.getDeptIdByTeacherName(conn, teacher);
            int targetDeptId = departmentDAO.getDeptIdByNameAndDept(conn, major, major);
            targetDeptIds.add(targetDeptId);
            int courseId = courseDAO.getCourseIdByNameAndDept(conn, courseTitle, userDeptId);
            List<SessionInput> sessions = new ArrayList<>();

            for (SessionRow row : generatedSessions) {
                LocalDate sessionDate = LocalDate.parse(row.getDate().trim());
                int period = row.getPeriodNumber();
                sessions.add(new SessionInput(sessionDate, period));
            }

            offeringId = courseOfferingDAO.createCourseOffering(teacher, teacherDeptId, targetDeptIds, courseId,
                    userDeptId, year, semester, startDate, capacity,
                    totalSessions, customSymbol, badgeColor, sessions,
                    tutorialWeight, assignmentWeight, labTestWeight, practicalWeight, finalExamWeight);
            if (offeringId > 0) {
                int enrolledCount = courseOfferingDAO.getTotalEnrolledStudents(conn, offeringId);

                ClassesController.SHARED_CLASS_DATA.add(0, new ClassesController.ClassItem(
                        offeringId,
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
                        customSymbol));

                showAlert(Alert.AlertType.INFORMATION, "Class Created & Published",
                        String.format(Locale.ENGLISH,
                                "Class (%s - %s) has been published to both Admin and Lecturer views!\n"
                                        + "Timetable: %s (Starting %s, %d sessions)\n"
                                        + "Exam Distribution: Tut %s%% | Assign %s%% | Lab %s%% | Prac %s%% | Final %s%%\n"
                                        + "Auto-Assigned Students: %d students (%s · %s · %s)",
                                courseCode, courseTitle, patternSummary, startDate, generatedSessions.size(),
                                tutorialWeight.stripTrailingZeros().toPlainString(),
                                assignmentWeight.stripTrailingZeros().toPlainString(),
                                labTestWeight.stripTrailingZeros().toPlainString(),
                                practicalWeight.stripTrailingZeros().toPlainString(),
                                finalExamWeight.stripTrailingZeros().toPlainString(),
                                enrolledCount, major, year, semester));

                navigateBackToClasses(event);
            } else {
                showAlert(Alert.AlertType.ERROR, "Class Creation Failed",
                        "Could not create the class offering. No offering ID was returned.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Failed to create class offering: " + e.getMessage());
        }
    }

    private void navigateBackToClasses(ActionEvent event) {
        try {
            Scene scene = null;
            if (event != null && event.getSource() instanceof Node source) {
                scene = source.getScene();
            }
            if (scene == null && rootPane != null) {
                scene = rootPane.getScene();
            }
            if (scene == null) {
                return;
            }

            StackPane contentArea = (StackPane) scene.lookup("#contentArea");
            if (contentArea == null) {
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("AdminClassesView.fxml"));
            Parent classesView = loader.load();
            contentArea.getChildren().setAll(classesView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getSafeText(TextField field) {
        return (field == null || field.getText() == null) ? "" : field.getText().trim();
    }

    private String getComboText(ComboBox<String> combo) {
        if (combo == null)
            return "";
        if (combo.isEditable() && combo.getEditor() != null && combo.getEditor().getText() != null
                && !combo.getEditor().getText().trim().isEmpty()) {
            return combo.getEditor().getText().trim();
        }
        if (combo.getValue() != null && !combo.getValue().trim().isEmpty()) {
            return combo.getValue().trim();
        }
        return "";
    }

    private int extractPeriodNumber(String periodText) {
        if (periodText == null)
            return 1;
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

        public String getSessionNo() {
            return sessionNo.get();
        }

        public void setSessionNo(String val) {
            sessionNo.set(val);
        }

        public StringProperty sessionNoProperty() {
            return sessionNo;
        }

        public String getDate() {
            return date.get();
        }

        public StringProperty dateProperty() {
            return date;
        }

        public String getDay() {
            return day.get();
        }

        public StringProperty dayProperty() {
            return day;
        }

        public int getPeriodNumber() {
            return periodNumber;
        }

        public String getPeriodLabel() {
            return periodLabel.get();
        }

        public StringProperty periodLabelProperty() {
            return periodLabel;
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
    
}