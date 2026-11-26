package Attendance;

import application.UserSession;
import application.dao.AttendanceDAO;
import application.dao.AttendanceDAO.AttendanceKpiMetrics;
import application.dao.DatabaseConnector;
import application.dao.DepartmentDAO;
import application.dao.StudentDAO;
import application.dto.AttendanceRecord;
import application.dto.Student;
import application.dto.StudentProfile;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AttendanceController {

    private static final int CHRONO_ROWS_PER_PAGE = 6;

    // =========================================================================
    // TOP BAR & KPI CARDS
    // =========================================================================
    @FXML private VBox rootPane;
    @FXML private StackPane profileIconBg;
    @FXML private Label profileIconLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label deptBadgeLabel;

    @FXML private Label overallRateLabel;
    @FXML private Label todayRateLabel;
    @FXML private Label studentsTrackedLabel;
    @FXML private Label lowAttendanceCountLabel;

    // =========================================================================
    // 2-TIER MODE SWITCHER BAR
    // =========================================================================
    @FXML private Button tabClassViewBtn;
    @FXML private Button tabIndividualBtn;
    @FXML private HBox subModeBar;
    @FXML private Button tabMatrixBtn;
    @FXML private Button tabDailyBtn;

    @FXML private VBox matrixSection;
    @FXML private VBox dailySection;
    @FXML private VBox individualSection;

    // =========================================================================
    // TAB 1: MATRIX SUMMARY
    // =========================================================================
    @FXML private ComboBox<String> matrixYearCombo;
    @FXML private ComboBox<String> matrixSubjectCombo;
    @FXML private DatePicker matrixStartDate;
    @FXML private DatePicker matrixEndDate;
    @FXML private TextField matrixSearchField;
    @FXML private CheckBox chkLowAttendance;
    @FXML private Label matrixFooterLabel;

    @FXML private TableView<AttendanceRecord> matrixTable;
    @FXML private TableColumn<AttendanceRecord, Integer> colNoMatrix;
    @FXML private TableColumn<AttendanceRecord, String> colRollMatrix;
    @FXML private TableColumn<AttendanceRecord, String> colNameMatrix;
    @FXML private TableColumn<AttendanceRecord, String> colDeptMatrix;
    @FXML private TableColumn<AttendanceRecord, String> colYearMatrix;
    @FXML private TableColumn<AttendanceRecord, String> colAttendedMatrix;
    @FXML private TableColumn<AttendanceRecord, String> colOverallMatrix;

    // =========================================================================
    // TAB 2: DAILY ATTENDANCE LOG
    // =========================================================================
    @FXML private DatePicker dailyDatePicker;
    @FXML private ComboBox<String> dailyPeriodCombo;
    @FXML private TextField dailySearchField;
    @FXML private CheckBox chkPresent;
    @FXML private CheckBox chkAbsent;
    @FXML private CheckBox chkExcuse;
    @FXML private Label dailyFooterLabel;

    @FXML private TableView<AttendanceRecord> dailyLogTable;
    @FXML private TableColumn<AttendanceRecord, Integer> colNoLog;
    @FXML private TableColumn<AttendanceRecord, String> colRollLog;
    @FXML private TableColumn<AttendanceRecord, String> colNameLog;
    @FXML private TableColumn<AttendanceRecord, Integer> colPeriodLog;
    @FXML private TableColumn<AttendanceRecord, String> colSubjLog;
    @FXML private TableColumn<AttendanceRecord, String> colStatusLog;
    @FXML private TableColumn<AttendanceRecord, String> colMarkedByLog;
    @FXML private TableColumn<AttendanceRecord, String> colRemarksLog;

    // =========================================================================
    // TAB 3: INDIVIDUAL STUDENT ATTENDANCE DASHBOARD
    // =========================================================================
    @FXML private ComboBox<String> indivStudentCombo;
    @FXML private Label lblIndivStatusBadge;

    @FXML private ImageView indivProfileImage;
    @FXML private Label indivPhotoPlaceholder;
    @FXML private Label lblIndivName;
    @FXML private Label lblIndivMeta;
    @FXML private Label lblIndivEmail;
    @FXML private Label lblIndivPhone;

    @FXML private Label lblIndivAttended;
    @FXML private Label lblIndivRate;
    @FXML private Label lblIndivStanding;

    @FXML private TableView<AttendanceRecord> subjectBreakdownTable;
    @FXML private TableColumn<AttendanceRecord, Integer> colNoIndiv;
    @FXML private TableColumn<AttendanceRecord, String> colCourseCodeIndiv;
    @FXML private TableColumn<AttendanceRecord, String> colSubjIndiv;
    @FXML private TableColumn<AttendanceRecord, String> colAttendedIndiv;
    @FXML private TableColumn<AttendanceRecord, String> colPercentageIndiv;
    @FXML private TableColumn<AttendanceRecord, String> colStandingIndiv;

    @FXML private ComboBox<String> indivCourseFilterCombo;
    @FXML private ComboBox<String> indivStatusFilterCombo;
    @FXML private DatePicker indivStartDate;
    @FXML private DatePicker indivEndDate;

    @FXML private TableView<AttendanceRecord> chronologicalLogTable;
    @FXML private TableColumn<AttendanceRecord, Integer> colNoChrono;
    @FXML private TableColumn<AttendanceRecord, String> colDateChrono;
    @FXML private TableColumn<AttendanceRecord, Integer> colPeriodChrono;
    @FXML private TableColumn<AttendanceRecord, String> colCourseChrono;
    @FXML private TableColumn<AttendanceRecord, String> colStatusChrono;
    @FXML private TableColumn<AttendanceRecord, String> colMarkedByChrono;
    @FXML private TableColumn<AttendanceRecord, String> colRemarksChrono;

    @FXML private Label chronoPaginationLabel;
    @FXML private HBox chronoPaginationBox;

    // =========================================================================
    // STATE & DATA
    // =========================================================================
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    private final ObservableList<AttendanceRecord> matrixRows = FXCollections.observableArrayList();
    private final ObservableList<AttendanceRecord> dailyRows = FXCollections.observableArrayList();
    private final ObservableList<AttendanceRecord> subjectBreakdownRows = FXCollections.observableArrayList();
    private final ObservableList<AttendanceRecord> chronoPageRows = FXCollections.observableArrayList();
    private final List<AttendanceRecord> allChronoLogs = new ArrayList<>();

    private final Map<String, String> originalDailyStatuses = new HashMap<>();
    private final Map<String, String> originalDailyRemarks = new HashMap<>();
    private final Map<String, AttendanceRecord> modifiedDailyRecords = new LinkedHashMap<>();

    // Searchable Individual Student ComboBox state
    private final Map<String, Integer> studentLabelToId = new LinkedHashMap<>();
    private final ObservableList<String> indivStudentBackingItems = FXCollections.observableArrayList();
    private final List<String> allIndivStudentLabels = new ArrayList<>();
    private boolean isUpdatingIndivCombo = false;

    private boolean isAdminMode = true;
    private int resolvedDeptId = 1;
    private boolean isMatrixSubMode = true;
    private int selectedIndividualStudentId = -1;
    private int chronoCurrentPage = 1;

    @FXML
    public void initialize() {
        setupOutsideClickDeselect();
        setupMatrixTable();
        setupDailyLogTable();
        setupSubjectBreakdownTable();
        setupChronologicalLogTable();
        setupSearchableIndivStudentCombo();
        switchToMatrixTab();
    }

    /**
     * Called by AdminMainController (isAdmin = true) or MainController (isAdmin = false)
     * right after loading AttendanceView.fxml.
     */
    public void initRole(boolean isAdmin) {
        this.isAdminMode = isAdmin;
        this.resolvedDeptId = resolveCurrentDeptId();

        configureRoleHeader();
        initFilterControls();
        refreshAll();
    }

    /**
     * Deep-link from StudentsView or StudentDetailView directly into a student's Individual Dashboard.
     */
    public void openStudentAttendance(Student student) {
        if (student == null) return;
        openStudentAttendance(student.getNo());
    }

    public void openStudentAttendance(int studentId) {
        if (studentId <= 0) return;
        populateIndividualStudentChoices();
        selectStudentInSearchableCombo(studentId);
        loadIndividualDashboard(studentId);
        switchToIndividualTab();
    }

    @FXML
    public void handleRefreshAll() {
        refreshAll();
    }

    public void refreshAll() {
        loadKpiMetrics();
        loadMatrixSummary();
        loadDailyLogs();
        populateIndividualStudentChoices();

        if (selectedIndividualStudentId > 0) {
            loadIndividualDashboard(selectedIndividualStudentId);
        }
    }

    // =========================================================================
    // ROLE HEADER & FILTER INITIALIZATION
    // =========================================================================
    private void configureRoleHeader() {
        if (deptBadgeLabel != null) {
            deptBadgeLabel.setText(UserSession.getFormattedDepartment());
        }

        if (isAdminMode) {
            if (profileNameLabel != null) profileNameLabel.setText("System Administrator");
            if (profileIconLabel != null) {
                profileIconLabel.setText("⚙");
                profileIconLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #9333ea;");
            }
            if (profileIconBg != null) {
                profileIconBg.setStyle("-fx-background-color: #f3e8ff; -fx-background-radius: 12px; -fx-min-width: 24px; -fx-min-height: 24px;");
            }
            if (pageTitleLabel != null) pageTitleLabel.setText("Attendance Oversight (Admin)");
            if (pageSubtitleLabel != null) {
                pageSubtitleLabel.setText("Track department attendance rates, edit daily session logs, and inspect individual student history.");
            }
        } else {
            if (profileNameLabel != null) profileNameLabel.setText("Lecturer (" + UserSession.lecturerName + ")");
            if (profileIconLabel != null) {
                profileIconLabel.setText("👨‍🏫");
                profileIconLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2563eb;");
            }
            if (profileIconBg != null) {
                profileIconBg.setStyle("-fx-background-color: #dbeafe; -fx-background-radius: 12px; -fx-min-width: 24px; -fx-min-height: 24px;");
            }
            if (pageTitleLabel != null) pageTitleLabel.setText("Class Attendance");
            if (pageSubtitleLabel != null) {
                pageSubtitleLabel.setText("Monitor attendance across your assigned classes, update daily logs, and review student histories.");
            }
        }
    }

    private void initFilterControls() {
        if (matrixYearCombo != null) {
            matrixYearCombo.setItems(FXCollections.observableArrayList(
                    "All Years", "First year", "Second year", "Third year", "Fourth year", "Fifth year"));
            matrixYearCombo.setValue("All Years");
        }

        if (matrixSubjectCombo != null) {
            ObservableList<String> subjects = FXCollections.observableArrayList("All Subjects");
            try {
                subjects.addAll(studentDAO.getSubjectsForScope(isAdminMode, resolvedDeptId, UserSession.userId));
            } catch (Exception ignored) {
            }
            matrixSubjectCombo.setItems(subjects);
            matrixSubjectCombo.setValue("All Subjects");
        }

        if (matrixEndDate != null) {
            matrixEndDate.setValue(LocalDate.now());
        }
        if (matrixSearchField != null) {
            matrixSearchField.setOnAction(e -> handleApplyMatrixFilter());
        }

        if (dailyDatePicker != null) {
            dailyDatePicker.setValue(attendanceDAO.getLatestSessionDate(isAdminMode, resolvedDeptId, UserSession.userId));
        }
        if (dailyPeriodCombo != null) {
            dailyPeriodCombo.setItems(FXCollections.observableArrayList(
                    "All Periods", "Period 1", "Period 2", "Period 3", "Period 4", "Period 5", "Period 6"));
            dailyPeriodCombo.setValue("All Periods");
        }
        if (dailySearchField != null) {
            dailySearchField.setOnAction(e -> handleApplyDailyFilter());
        }

        if (indivStatusFilterCombo != null) {
            indivStatusFilterCombo.setItems(FXCollections.observableArrayList(
                    "All Status", "Present", "Absent", "Excused"));
            indivStatusFilterCombo.setValue("All Status");
        }
        if (indivEndDate != null) {
            indivEndDate.setValue(LocalDate.now());
        }
    }

    // =========================================================================
    // TAB SWITCHING (2-Tier Pill Bar + Sub-Mode Strip)
    // =========================================================================
    @FXML
    public void switchToClassView() {
        if (isMatrixSubMode) {
            switchToMatrixTab();
        } else {
            switchToDailyTab();
        }
    }

    @FXML
    public void switchToMatrixTab() {
        isMatrixSubMode = true;
        setVisibleSection(matrixSection, true);
        setVisibleSection(dailySection, false);
        setVisibleSection(individualSection, false);
        setSubModeBarVisible(true);

        applyPillState(tabClassViewBtn, true, "tab-pill-active", "tab-pill-inactive");
        applyPillState(tabIndividualBtn, false, "tab-pill-active", "tab-pill-inactive");
        applyPillState(tabMatrixBtn, true, "submode-pill-active", "submode-pill-inactive");
        applyPillState(tabDailyBtn, false, "submode-pill-active", "submode-pill-inactive");
    }

    @FXML
    public void switchToDailyTab() {
        isMatrixSubMode = false;
        setVisibleSection(matrixSection, false);
        setVisibleSection(dailySection, true);
        setVisibleSection(individualSection, false);
        setSubModeBarVisible(true);

        applyPillState(tabClassViewBtn, true, "tab-pill-active", "tab-pill-inactive");
        applyPillState(tabIndividualBtn, false, "tab-pill-active", "tab-pill-inactive");
        applyPillState(tabMatrixBtn, false, "submode-pill-active", "submode-pill-inactive");
        applyPillState(tabDailyBtn, true, "submode-pill-active", "submode-pill-inactive");
    }

    @FXML
    public void switchToIndividualTab() {
        setVisibleSection(matrixSection, false);
        setVisibleSection(dailySection, false);
        setVisibleSection(individualSection, true);
        setSubModeBarVisible(false);

        applyPillState(tabClassViewBtn, false, "tab-pill-active", "tab-pill-inactive");
        applyPillState(tabIndividualBtn, true, "tab-pill-active", "tab-pill-inactive");

        if (selectedIndividualStudentId <= 0 && !allIndivStudentLabels.isEmpty()) {
            String firstKey = allIndivStudentLabels.get(0);
            Integer firstId = studentLabelToId.get(firstKey);
            if (firstId != null && firstId > 0) {
                selectStudentInSearchableCombo(firstId);
                loadIndividualDashboard(firstId);
            }
        }
    }

    private void setVisibleSection(VBox section, boolean show) {
        if (section != null) {
            section.setVisible(show);
            section.setManaged(show);
        }
    }

    private void setSubModeBarVisible(boolean show) {
        if (subModeBar != null) {
            subModeBar.setVisible(show);
            subModeBar.setManaged(show);
        }
    }

    private void applyPillState(Button btn, boolean active, String activeClass, String inactiveClass) {
        if (btn == null) return;
        btn.setStyle("");
        btn.getStyleClass().removeAll(activeClass, inactiveClass);
        btn.getStyleClass().add(active ? activeClass : inactiveClass);
    }

    // =========================================================================
    // KPI METRICS
    // =========================================================================
    private void loadKpiMetrics() {
        AttendanceKpiMetrics m = attendanceDAO.getAttendanceKpiMetrics(isAdminMode, resolvedDeptId, UserSession.userId);
        if (overallRateLabel != null) {
            overallRateLabel.setText(String.format("%.1f%%", m.overallRate()));
        }
        if (todayRateLabel != null) {
            double todayPct = m.todayTotal() == 0 ? 0.0 : (m.todayPresent() * 100.0 / m.todayTotal());
            todayRateLabel.setText(String.format("%.1f%%", todayPct));
        }
        if (lowAttendanceCountLabel != null) {
            lowAttendanceCountLabel.setText(String.valueOf(m.lowAttendanceStudents()));
        }
    }

    // =========================================================================
    // TAB 1: MATRIX SUMMARY
    // =========================================================================
    private void setupMatrixTable() {
        if (matrixTable == null) return;

        colNoMatrix.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(matrixTable.getItems().indexOf(c.getValue()) + 1));
        colRollMatrix.setCellValueFactory(c -> c.getValue().rollNoProperty());
        colNameMatrix.setCellValueFactory(c -> c.getValue().studentNameProperty());
        colDeptMatrix.setCellValueFactory(c -> c.getValue().departmentProperty());
        colYearMatrix.setCellValueFactory(c -> c.getValue().yearProperty());
        colAttendedMatrix.setCellValueFactory(c -> c.getValue().attendedTotalProperty());

        colOverallMatrix.setCellValueFactory(c ->
                new SimpleStringProperty(String.format("%.1f%%", c.getValue().getPercentage())));
        colOverallMatrix.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String pctText, boolean empty) {
                super.updateItem(pctText, empty);
                if (empty || pctText == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    AttendanceRecord rec = getTableView().getItems().get(getIndex());
                    double pct = rec != null ? rec.getPercentage() : 0.0;
                    Label badge = new Label(pctText);
                    String base = "-fx-padding: 3px 10px; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 11.5px;";
                    if (pct >= 90.0) {
                        badge.setStyle(base + " -fx-background-color: #dcfce7; -fx-text-fill: #15803d;");
                    } else if (pct >= 75.0) {
                        badge.setStyle(base + " -fx-background-color: #e0f2fe; -fx-text-fill: #0369a1;");
                    } else {
                        badge.setStyle(base + " -fx-background-color: #fee2e2; -fx-text-fill: #b91c1c;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        matrixTable.setItems(matrixRows);
        matrixTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                AttendanceRecord selected = matrixTable.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getNo() > 0) {
                    openStudentAttendance(selected.getNo());
                }
            }
        });
    }

    @FXML
    public void handleApplyMatrixFilter() {
        loadMatrixSummary();
    }

    @FXML
    public void handleResetMatrixFilter() {
        if (matrixYearCombo != null) matrixYearCombo.setValue("All Years");
        if (matrixSubjectCombo != null) matrixSubjectCombo.setValue("All Subjects");
        if (matrixStartDate != null) matrixStartDate.setValue(null);
        if (matrixEndDate != null) matrixEndDate.setValue(LocalDate.now());
        if (matrixSearchField != null) matrixSearchField.clear();
        if (chkLowAttendance != null) chkLowAttendance.setSelected(false);
        loadMatrixSummary();
    }

    private void loadMatrixSummary() {
        String year = matrixYearCombo != null ? matrixYearCombo.getValue() : "All Years";
        String subject = matrixSubjectCombo != null ? matrixSubjectCombo.getValue() : "All Subjects";
        LocalDate start = matrixStartDate != null ? matrixStartDate.getValue() : null;
        LocalDate end = matrixEndDate != null ? matrixEndDate.getValue() : LocalDate.now();
        boolean lowOnly = chkLowAttendance != null && chkLowAttendance.isSelected();
        String search = (matrixSearchField != null && matrixSearchField.getText() != null)
                ? matrixSearchField.getText().trim() : "";

        List<AttendanceRecord> fetched = attendanceDAO.getMatrixSummary(
                isAdminMode, resolvedDeptId, UserSession.userId,
                year, subject, start, end, lowOnly, search);

        matrixRows.setAll(fetched);
        int count = matrixRows.size();
        if (studentsTrackedLabel != null) {
            studentsTrackedLabel.setText(String.valueOf(count));
        }
        if (matrixFooterLabel != null) {
            matrixFooterLabel.setText("Showing " + count + (count == 1 ? " student" : " students"));
        }
    }

    // =========================================================================
    // TAB 2: DAILY ATTENDANCE LOG
    // =========================================================================
    private void setupDailyLogTable() {
        if (dailyLogTable == null) return;

        dailyLogTable.setEditable(true);

        colNoLog.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(dailyLogTable.getItems().indexOf(c.getValue()) + 1));
        colRollLog.setCellValueFactory(c -> c.getValue().rollNoProperty());
        colNameLog.setCellValueFactory(c -> c.getValue().studentNameProperty());
        colPeriodLog.setCellValueFactory(c -> c.getValue().periodProperty().asObject());
        colSubjLog.setCellValueFactory(c -> c.getValue().subjectProperty());
        colMarkedByLog.setCellValueFactory(c -> c.getValue().markedByProperty());

        colStatusLog.setCellValueFactory(c -> c.getValue().statusProperty());
        colStatusLog.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(status);
                    String base = "-fx-padding: 3px 10px; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 11px;";
                    if ("Present".equalsIgnoreCase(status)) {
                        badge.setStyle(base + " -fx-background-color: #dcfce7; -fx-text-fill: #15803d;");
                    } else if ("Excused".equalsIgnoreCase(status) || "Excuse".equalsIgnoreCase(status)) {
                        badge.setStyle(base + " -fx-background-color: #fef3c7; -fx-text-fill: #b45309;");
                    } else {
                        badge.setStyle(base + " -fx-background-color: #fee2e2; -fx-text-fill: #b91c1c;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colRemarksLog.setCellValueFactory(c -> c.getValue().remarksProperty());
        colRemarksLog.setCellFactory(TextFieldTableCell.forTableColumn());
        colRemarksLog.setOnEditCommit(event -> {
            AttendanceRecord rec = event.getRowValue();
            if (rec != null) {
                String newRemark = event.getNewValue() != null ? event.getNewValue().trim() : "";
                rec.remarksProperty().set(newRemark);
                markDailyRecordModified(rec);
            }
        });

        dailyLogTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(AttendanceRecord item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("row-present", "row-absent", "row-excused");
                if (!empty && item != null && item.getStatus() != null) {
                    if ("Present".equalsIgnoreCase(item.getStatus())) {
                        getStyleClass().add("row-present");
                    } else if ("Absent".equalsIgnoreCase(item.getStatus())) {
                        getStyleClass().add("row-absent");
                    } else {
                        getStyleClass().add("row-excused");
                    }
                }
            }
        });

        dailyLogTable.setItems(dailyRows);
    }

    @FXML
    public void handleApplyDailyFilter() {
        loadDailyLogs();
    }

    @FXML
    public void handleResetDailyFilter() {
        if (dailyDatePicker != null) {
            dailyDatePicker.setValue(attendanceDAO.getLatestSessionDate(isAdminMode, resolvedDeptId, UserSession.userId));
        }
        if (dailyPeriodCombo != null) dailyPeriodCombo.setValue("All Periods");
        if (dailySearchField != null) dailySearchField.clear();
        if (chkPresent != null) chkPresent.setSelected(false);
        if (chkAbsent != null) chkAbsent.setSelected(false);
        if (chkExcuse != null) chkExcuse.setSelected(false);
        loadDailyLogs();
    }

    private void loadDailyLogs() {
        LocalDate date = dailyDatePicker != null && dailyDatePicker.getValue() != null
                ? dailyDatePicker.getValue() : LocalDate.now();
        Integer period = parsePeriodFilter(dailyPeriodCombo != null ? dailyPeriodCombo.getValue() : null);
        String search = (dailySearchField != null && dailySearchField.getText() != null)
                ? dailySearchField.getText().trim() : "";
        boolean p = chkPresent != null && chkPresent.isSelected();
        boolean a = chkAbsent != null && chkAbsent.isSelected();
        boolean e = chkExcuse != null && chkExcuse.isSelected();

        List<AttendanceRecord> fetched = attendanceDAO.getDailyLogs(
                isAdminMode, resolvedDeptId, UserSession.userId,
                date, period, "All Subjects", search, p, a, e);

        originalDailyStatuses.clear();
        originalDailyRemarks.clear();
        modifiedDailyRecords.clear();

        for (AttendanceRecord r : fetched) {
            String key = dailyKey(r);
            originalDailyStatuses.put(key, r.getStatus());
            originalDailyRemarks.put(key, r.getRemarks());
        }

        dailyRows.setAll(fetched);
        if (dailyFooterLabel != null) {
            int count = dailyRows.size();
            dailyFooterLabel.setText("Showing " + count + (count == 1 ? " record" : " records"));
        }
    }

    @FXML
    public void handleToggleDailyStatus() {
        AttendanceRecord selected = dailyLogTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Select a row in the Daily Attendance Log to toggle its status.", Alert.AlertType.WARNING);
            return;
        }

        String current = selected.getStatus() != null ? selected.getStatus() : "Present";
        if ("Present".equalsIgnoreCase(current)) {
            selected.statusProperty().set("Absent");
            if ("On Time".equalsIgnoreCase(selected.getRemarks())) {
                selected.remarksProperty().set("Unexcused");
            }
        } else if ("Absent".equalsIgnoreCase(current)) {
            selected.statusProperty().set("Excused");
            if ("Unexcused".equalsIgnoreCase(selected.getRemarks()) || "On Time".equalsIgnoreCase(selected.getRemarks())) {
                selected.remarksProperty().set("Excused");
            }
        } else {
            selected.statusProperty().set("Present");
            selected.remarksProperty().set("On Time");
        }

        markDailyRecordModified(selected);
        dailyLogTable.refresh();
    }

    @FXML
    public void handleResetDailyStatus() {
        for (AttendanceRecord r : dailyRows) {
            String key = dailyKey(r);
            if (originalDailyStatuses.containsKey(key)) {
                r.statusProperty().set(originalDailyStatuses.get(key));
            }
            if (originalDailyRemarks.containsKey(key)) {
                r.remarksProperty().set(originalDailyRemarks.get(key));
            }
        }
        modifiedDailyRecords.clear();
        dailyLogTable.refresh();
    }

    @FXML
    public void handleMarkAllPresent() {
        for (AttendanceRecord r : dailyRows) {
            r.statusProperty().set("Present");
            r.remarksProperty().set("On Time");
            markDailyRecordModified(r);
        }
        dailyLogTable.refresh();
    }

    @FXML
    public void handleSaveDailyChanges() {
        if (modifiedDailyRecords.isEmpty()) {
            showAlert("No Changes", "There are no unsaved attendance changes.", Alert.AlertType.INFORMATION);
            return;
        }

        boolean ok = attendanceDAO.saveDailyAttendanceChanges(new ArrayList<>(modifiedDailyRecords.values()));
        if (ok) {
            showAlert("Saved", "Daily attendance changes have been saved to the database.", Alert.AlertType.INFORMATION);
            refreshAll();
        } else {
            showAlert("Save Failed", "Could not save daily attendance changes. Check database connection.", Alert.AlertType.ERROR);
        }
    }

    private void markDailyRecordModified(AttendanceRecord rec) {
        if (rec != null) {
            modifiedDailyRecords.put(dailyKey(rec), rec);
        }
    }

    private String dailyKey(AttendanceRecord rec) {
        return rec.getSessionId() + "_" + rec.getEnrollmentId();
    }

    private Integer parsePeriodFilter(String val) {
        if (val == null || val.isBlank() || "All Periods".equalsIgnoreCase(val)) return null;
        try {
            return Integer.parseInt(val.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // =========================================================================
    // TAB 3: INDIVIDUAL STUDENT DASHBOARD (SEARCHABLE COMBOBOX + 6-ROW LOG)
    // =========================================================================
    private void setupSearchableIndivStudentCombo() {
        if (indivStudentCombo == null) return;

        indivStudentCombo.setEditable(true);
        indivStudentCombo.setItems(indivStudentBackingItems);

        TextField editor = indivStudentCombo.getEditor();
        if (editor == null) return;

        // 1. Clicking the dropdown arrow after selecting a student shows the FULL list
        indivStudentCombo.setOnShowing(e -> {
            if (isUpdatingIndivCombo) return;
            String text = editor.getText() != null ? editor.getText().trim() : "";
            boolean isSelectedStudent =
                    (indivStudentCombo.getValue() != null && indivStudentCombo.getValue().equals(text))
                    || studentLabelToId.containsKey(text);

            if (text.isEmpty() || isSelectedStudent) {
                isUpdatingIndivCombo = true;
                if (indivStudentBackingItems.size() != allIndivStudentLabels.size()) {
                    indivStudentBackingItems.setAll(allIndivStudentLabels);
                }
                if (isSelectedStudent) {
                    indivStudentCombo.getSelectionModel().select(text);
                    editor.setText(text);
                } else {
                    indivStudentCombo.getSelectionModel().clearSelection();
                }
                isUpdatingIndivCombo = false;
            }
        });

        // 2. Pressing Backspace once on a selected student clears it straight to blank
        editor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.BACK_SPACE) {
                String text = editor.getText() != null ? editor.getText() : "";
                boolean isSelectedStudent =
                        (indivStudentCombo.getValue() != null && indivStudentCombo.getValue().equals(text))
                        || studentLabelToId.containsKey(text);

                if (!text.isEmpty() && isSelectedStudent) {
                    event.consume();
                    isUpdatingIndivCombo = true;
                    indivStudentCombo.hide();
                    indivStudentCombo.getSelectionModel().clearSelection();
                    indivStudentCombo.setValue(null);
                    indivStudentBackingItems.setAll(allIndivStudentLabels);
                    indivStudentCombo.getSelectionModel().clearSelection();
                    editor.clear();
                    isUpdatingIndivCombo = false;

                    Platform.runLater(() -> {
                        if (indivStudentCombo.isFocused() || editor.isFocused()) {
                            indivStudentCombo.show();
                        }
                    });
                }
            }
        });

        // 3. Live search while typing (mutates backing list without replacing it)
        editor.textProperty().addListener((obs, oldText, newText) -> {
            if (isUpdatingIndivCombo) return;

            String selected = indivStudentCombo.getSelectionModel().getSelectedItem();
            if (selected != null && selected.equals(newText)) {
                return;
            }

            isUpdatingIndivCombo = true;
            String typed = newText == null ? "" : newText;
            String q = typed.trim().toLowerCase();

            List<String> matches = allIndivStudentLabels.stream()
                    .filter(label -> q.isEmpty() || label.toLowerCase().contains(q))
                    .collect(Collectors.toList());

            indivStudentCombo.getSelectionModel().clearSelection();
            indivStudentBackingItems.setAll(matches);
            indivStudentCombo.getSelectionModel().clearSelection();

            if (!typed.equals(editor.getText())) {
                editor.setText(typed);
                editor.positionCaret(typed.length());
            }
            isUpdatingIndivCombo = false;

            if (!matches.isEmpty() && !indivStudentCombo.isShowing()
                    && (indivStudentCombo.isFocused() || editor.isFocused())) {
                indivStudentCombo.show();
            }
        });

        // 4. Load dashboard when a valid student is selected from the dropdown
        indivStudentCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingIndivCombo || newVal == null) return;
            Integer studentId = studentLabelToId.get(newVal);
            if (studentId != null && studentId > 0) {
                loadIndividualDashboard(studentId);
            }
        });

        // 5. Lock popup width to match ComboBox width
        lockComboPopupWidth(indivStudentCombo);
    }

    private void lockComboPopupWidth(ComboBox<String> combo) {
        if (combo == null) return;

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

    private void populateIndividualStudentChoices() {
        List<AttendanceRecord> allScoped = attendanceDAO.getMatrixSummary(
                isAdminMode, resolvedDeptId, UserSession.userId,
                "All Years", "All Subjects", null, LocalDate.now(), false, "");

        studentLabelToId.clear();
        for (AttendanceRecord r : allScoped) {
            if (r.getNo() > 0) {
                String label = safe(r.getRollNo(), "-") + " — " + safe(r.getStudentName(), "Student");
                studentLabelToId.put(label, r.getNo());
            }
        }

        isUpdatingIndivCombo = true;
        allIndivStudentLabels.clear();
        allIndivStudentLabels.addAll(studentLabelToId.keySet());
        indivStudentBackingItems.setAll(allIndivStudentLabels);
        isUpdatingIndivCombo = false;

        if (selectedIndividualStudentId > 0) {
            selectStudentInSearchableCombo(selectedIndividualStudentId);
        }
    }

    private void selectStudentInSearchableCombo(int studentId) {
        if (indivStudentCombo == null) return;
        String matchedLabel = studentLabelToId.entrySet().stream()
                .filter(e -> e.getValue() == studentId)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (matchedLabel != null) {
            isUpdatingIndivCombo = true;
            indivStudentBackingItems.setAll(allIndivStudentLabels);
            indivStudentCombo.setValue(matchedLabel);
            if (indivStudentCombo.getEditor() != null) {
                indivStudentCombo.getEditor().setText(matchedLabel);
            }
            isUpdatingIndivCombo = false;
        }
    }

    private void setupSubjectBreakdownTable() {
        if (subjectBreakdownTable == null) return;

        colNoIndiv.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(subjectBreakdownTable.getItems().indexOf(c.getValue()) + 1));
        colCourseCodeIndiv.setCellValueFactory(c -> c.getValue().courseCodeProperty());
        colSubjIndiv.setCellValueFactory(c -> c.getValue().subjectProperty());
        colAttendedIndiv.setCellValueFactory(c -> c.getValue().attendedTotalProperty());
        colPercentageIndiv.setCellValueFactory(c ->
                new SimpleStringProperty(String.format("%.1f%%", c.getValue().getPercentage())));
        colStandingIndiv.setCellValueFactory(c -> c.getValue().remarksProperty());

        subjectBreakdownTable.setItems(subjectBreakdownRows);
    }

    private void setupChronologicalLogTable() {
        if (chronologicalLogTable == null) return;

        colNoChrono.setCellValueFactory(c -> {
            int rowIdx = chronologicalLogTable.getItems().indexOf(c.getValue());
            int globalNo = ((chronoCurrentPage - 1) * CHRONO_ROWS_PER_PAGE) + rowIdx + 1;
            return new ReadOnlyObjectWrapper<>(globalNo);
        });
        colDateChrono.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFormattedDateWithDay()));
        colPeriodChrono.setCellValueFactory(c -> c.getValue().periodProperty().asObject());
        colCourseChrono.setCellValueFactory(c -> c.getValue().subjectProperty());
        colMarkedByChrono.setCellValueFactory(c -> c.getValue().markedByProperty());
        colRemarksChrono.setCellValueFactory(c -> c.getValue().remarksProperty());

        colStatusChrono.setCellValueFactory(c -> c.getValue().statusProperty());
        colStatusChrono.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(status);
                    String base = "-fx-padding: 3px 10px; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 11px;";
                    if ("Present".equalsIgnoreCase(status)) {
                        badge.setStyle(base + " -fx-background-color: #dcfce7; -fx-text-fill: #15803d;");
                    } else if ("Excused".equalsIgnoreCase(status) || "EXCUSED".equalsIgnoreCase(status)) {
                        badge.setStyle(base + " -fx-background-color: #fef3c7; -fx-text-fill: #b45309;");
                    } else {
                        badge.setStyle(base + " -fx-background-color: #fee2e2; -fx-text-fill: #b91c1c;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        chronologicalLogTable.setItems(chronoPageRows);
        chronologicalLogTable.setFixedCellSize(40.0);
        Runnable fitSixRows = () -> {
            Node header = chronologicalLogTable.lookup(".column-header-background");
            double headerH = (header != null && header.getBoundsInLocal().getHeight() > 0)
                    ? header.getBoundsInLocal().getHeight()
                    : 36.0;
            double exactHeight = headerH + (CHRONO_ROWS_PER_PAGE * 40.0) + 2.0;
            chronologicalLogTable.setMinHeight(exactHeight);
            chronologicalLogTable.setPrefHeight(exactHeight);
            chronologicalLogTable.setMaxHeight(exactHeight);
        };
        chronologicalLogTable.skinProperty().addListener((obs, o, n) -> Platform.runLater(fitSixRows));
        chronologicalLogTable.sceneProperty().addListener((obs, o, n) -> Platform.runLater(fitSixRows));
    }

    private void loadIndividualDashboard(int studentId) {
        this.selectedIndividualStudentId = studentId;
        this.chronoCurrentPage = 1;

        StudentProfile profile = attendanceDAO.getIndividualProfile(studentId, isAdminMode, UserSession.userId);
        if (profile != null) {
            if (lblIndivName != null) lblIndivName.setText(safe(profile.getFullName(), "Student"));
            if (lblIndivMeta != null) {
                lblIndivMeta.setText(safe(profile.getRollNo(), "-") + "  ·  "
                        + safe(profile.getDeptCode(), "-") + "  ·  "
                        + safe(profile.getCurrentYear(), "-"));
            }
            if (lblIndivEmail != null) lblIndivEmail.setText("✉ " + safe(profile.getEmail(), "-"));
            if (lblIndivPhone != null) lblIndivPhone.setText("📞 " + safe(profile.getPhoneNumber(), "-"));

            if (lblIndivStatusBadge != null) {
                boolean active = profile.isUserStatus();
                lblIndivStatusBadge.setText(active ? "Active" : "Inactive");
                lblIndivStatusBadge.setStyle(active
                        ? "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4px 12px; -fx-background-radius: 12px;"
                        : "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4px 12px; -fx-background-radius: 12px;");
            }

            if (lblIndivAttended != null) {
                lblIndivAttended.setText(profile.getTotalAttended() + " / " + profile.getTotalSessions());
            }
            if (lblIndivRate != null) {
                double pct = profile.getAttendancePercentage();
                lblIndivRate.setText(String.format("%.1f%%", pct));
                lblIndivRate.setStyle(pct >= 75.0
                        ? "-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #16a34a;"
                        : "-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #dc2626;");
            }
            if (lblIndivStanding != null) {
                String standing = safe(profile.getAcademicStanding(), "Good");
                lblIndivStanding.setText(standing);
                lblIndivStanding.setStyle("Warning".equalsIgnoreCase(standing)
                        ? "-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #d97706;"
                        : "-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #15803d;");
            }

            loadIndivPhoto(profile.getProfileImageUrl());
        }

        List<AttendanceRecord> breakdown = attendanceDAO.getSubjectBreakdown(studentId, isAdminMode, UserSession.userId);
        subjectBreakdownRows.setAll(breakdown);

        if (indivCourseFilterCombo != null) {
            Set<String> courses = new LinkedHashSet<>();
            courses.add("All Courses");
            for (AttendanceRecord r : breakdown) {
                if (r.getSubject() != null && !r.getSubject().isBlank()) {
                    courses.add(r.getSubject());
                }
            }
            indivCourseFilterCombo.setItems(FXCollections.observableArrayList(courses));
            indivCourseFilterCombo.setValue("All Courses");
        }

        loadChronologicalLog();
    }

    @FXML
    public void handleApplyIndivLogFilter() {
        chronoCurrentPage = 1;
        loadChronologicalLog();
    }

    @FXML
    public void handleResetIndivLogFilter() {
        if (indivCourseFilterCombo != null) indivCourseFilterCombo.setValue("All Courses");
        if (indivStatusFilterCombo != null) indivStatusFilterCombo.setValue("All Status");
        if (indivStartDate != null) indivStartDate.setValue(null);
        if (indivEndDate != null) indivEndDate.setValue(LocalDate.now());
        chronoCurrentPage = 1;
        loadChronologicalLog();
    }

    private void loadChronologicalLog() {
        if (selectedIndividualStudentId <= 0) {
            allChronoLogs.clear();
            chronoPageRows.clear();
            renderChronoPagination(0, 1);
            return;
        }

        String course = indivCourseFilterCombo != null ? indivCourseFilterCombo.getValue() : "All Courses";
        String status = indivStatusFilterCombo != null ? indivStatusFilterCombo.getValue() : "All Status";
        LocalDate start = indivStartDate != null ? indivStartDate.getValue() : null;
        LocalDate end = indivEndDate != null ? indivEndDate.getValue() : LocalDate.now();

        List<AttendanceRecord> fetched = attendanceDAO.getChronologicalLogs(
                selectedIndividualStudentId, isAdminMode, UserSession.userId,
                course, status, start, end);

        allChronoLogs.clear();
        allChronoLogs.addAll(fetched);
        renderCurrentChronoPage();
    }

    private void renderCurrentChronoPage() {
        int totalRecords = allChronoLogs.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / CHRONO_ROWS_PER_PAGE));
        if (chronoCurrentPage > totalPages) chronoCurrentPage = totalPages;
        if (chronoCurrentPage < 1) chronoCurrentPage = 1;

        int startIdx = (chronoCurrentPage - 1) * CHRONO_ROWS_PER_PAGE;
        int endIdx = Math.min(startIdx + CHRONO_ROWS_PER_PAGE, totalRecords);

        if (totalRecords == 0) {
            chronoPageRows.clear();
        } else {
            chronoPageRows.setAll(allChronoLogs.subList(startIdx, endIdx));
        }

        renderChronoPagination(totalRecords, totalPages);
    }

    private void renderChronoPagination(int totalRecords, int totalPages) {
        if (totalPages <= 0) totalPages = 1;
        if (chronoCurrentPage > totalPages) chronoCurrentPage = totalPages;
        if (chronoCurrentPage < 1) chronoCurrentPage = 1;

        int startIdx = (chronoCurrentPage - 1) * CHRONO_ROWS_PER_PAGE;
        int endIdx = Math.min(startIdx + CHRONO_ROWS_PER_PAGE, totalRecords);

        if (chronoPaginationLabel != null) {
            chronoPaginationLabel.setText(totalRecords == 0
                    ? "Showing 0 - 0 of 0 records"
                    : "Showing " + (startIdx + 1) + " - " + endIdx + " of " + totalRecords + " records");
        }

        if (chronoPaginationBox == null) return;
        chronoPaginationBox.getChildren().clear();

        String navStyle = "-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-text-fill: #334155; "
                + "-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; "
                + "-fx-min-width: 34px; -fx-min-height: 34px; -fx-padding: 5px 11px; -fx-cursor: hand;";
        String activeStyle = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 13.5px; "
                + "-fx-font-weight: bold; -fx-background-radius: 6px; -fx-min-width: 34px; -fx-min-height: 34px; "
                + "-fx-padding: 5px 12px; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-text-fill: #1e293b; "
                + "-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; "
                + "-fx-min-width: 34px; -fx-min-height: 34px; -fx-padding: 5px 12px; -fx-cursor: hand;";

        Button prevBtn = new Button("<");
        prevBtn.setStyle(navStyle);
        prevBtn.setDisable(chronoCurrentPage <= 1);
        prevBtn.setOnAction(e -> {
            if (chronoCurrentPage > 1) {
                chronoCurrentPage--;
                renderCurrentChronoPage();
            }
        });
        chronoPaginationBox.getChildren().add(prevBtn);

        final int maxPages = totalPages;
        for (int p = 1; p <= maxPages; p++) {
            final int pageNum = p;
            Button pageBtn = new Button(String.valueOf(pageNum));
            pageBtn.setStyle(pageNum == chronoCurrentPage ? activeStyle : inactiveStyle);
            pageBtn.setOnAction(e -> {
                chronoCurrentPage = pageNum;
                renderCurrentChronoPage();
            });
            chronoPaginationBox.getChildren().add(pageBtn);
        }

        Button nextBtn = new Button(">");
        nextBtn.setStyle(navStyle);
        nextBtn.setDisable(chronoCurrentPage >= totalPages);
        nextBtn.setOnAction(e -> {
            if (chronoCurrentPage < maxPages) {
                chronoCurrentPage++;
                renderCurrentChronoPage();
            }
        });
        chronoPaginationBox.getChildren().add(nextBtn);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private void loadIndivPhoto(String path) {
        if (indivProfileImage == null) return;
        Image loaded = null;
        if (path != null && !path.isBlank() && !"null".equalsIgnoreCase(path.trim())) {
            try {
                String clean = path.trim();
                if (clean.startsWith("file:") || clean.startsWith("http:") || clean.startsWith("https:")) {
                    loaded = new Image(clean, 68, 86, false, true);
                } else {
                    File f = new File(clean);
                    if (f.exists()) {
                        loaded = new Image(f.toURI().toString(), 68, 86, false, true);
                    } else if (getClass().getResourceAsStream(clean) != null) {
                        loaded = new Image(getClass().getResourceAsStream(clean));
                    }
                }
            } catch (Exception ignored) {
            }
        }

        boolean hasImg = loaded != null && !loaded.isError();
        indivProfileImage.setImage(hasImg ? loaded : null);
        indivProfileImage.setVisible(hasImg);
        indivProfileImage.setManaged(hasImg);
        if (indivPhotoPlaceholder != null) {
            indivPhotoPlaceholder.setVisible(!hasImg);
            indivPhotoPlaceholder.setManaged(!hasImg);
        }
    }

    private int resolveCurrentDeptId() {
        DepartmentDAO deptDAO = new DepartmentDAO();
        try {
            return deptDAO.getDeptIdByUserId(UserSession.userId);
        } catch (Exception ignored) {
        }
        try (Connection conn = DatabaseConnector.getConnection()) {
            return deptDAO.getDeptIdByNameAndDept(conn, UserSession.departmentFullName, UserSession.departmentCode);
        } catch (Exception ignored) {
        }
        return 1;
    }

    private void setupOutsideClickDeselect() {
        if (rootPane == null) return;
        rootPane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            Node target = (event.getTarget() instanceof Node) ? (Node) event.getTarget() : null;
            if (isInsideNodeType(target, TableView.class)
                    || isInsideNodeType(target, ButtonBase.class)
                    || isInsideNodeType(target, ComboBox.class)
                    || isInsideNodeType(target, TextField.class)) {
                return;
            }
            if (matrixTable != null) matrixTable.getSelectionModel().clearSelection();
            if (dailyLogTable != null) dailyLogTable.getSelectionModel().clearSelection();
            rootPane.requestFocus();
        });
    }

    private boolean isInsideNodeType(Node node, Class<?> type) {
        Node current = node;
        while (current != null) {
            if (type.isInstance(current)) return true;
            current = current.getParent();
        }
        return false;
    }

    private String safe(String val, String fallback) {
        if (val == null || val.trim().isEmpty() || "null".equalsIgnoreCase(val.trim())) {
            return fallback;
        }
        return val.trim();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    public void openIndividualStudent(Student student) {
        openStudentAttendance(student);
    }
}