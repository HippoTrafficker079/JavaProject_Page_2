package application;

import application.dao.CourseOfferingDAO;
import application.dao.CourseOfferingDAO.AssessmentWeightRecord;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ClassExamDetailController {

    @FXML private VBox rootPane;
    @FXML private Label classTitleLabel;
    @FXML private Label classSubtitleLabel;
    @FXML private Label lecturerBadgeLabel;
    @FXML private Label examsListHeaderLabel;

    @FXML private Button scheduleExamBtn;
    @FXML private Button editWeightageBtn;
    @FXML private Button viewResultBtn;
    @FXML private HBox adminActionBox;

    // 5-Category Grading Criteria & Donut Legend (Synced with MySQL assessment_weight)
    @FXML private Label tutorialWeightLabel;
    @FXML private Label assignmentWeightLabel;
    @FXML private Label labTestWeightLabel;
    @FXML private Label practicalWeightLabel;
    @FXML private Label finalWeightLabel;
    @FXML private Label totalWeightLabel;
    @FXML private Label donutTotalLabel;

    @FXML private Label legendTutLabel;
    @FXML private Label legendAssignLabel;
    @FXML private Label legendLabLabel;
    @FXML private Label legendPracticalLabel;
    @FXML private Label legendFinalLabel;
    @FXML private PieChart weightagePieChart;

    // Filter Controls
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TextField searchField;

    // Exams Table
    @FXML private TableView<AdminExamsController.AdminExamRecord> examsTable;
    @FXML private TableColumn<AdminExamsController.AdminExamRecord, String> titleColumn;
    @FXML private TableColumn<AdminExamsController.AdminExamRecord, String> subjectColumn;
    @FXML private TableColumn<AdminExamsController.AdminExamRecord, String> dateColumn;
    @FXML private TableColumn<AdminExamsController.AdminExamRecord, String> durationColumn;
    @FXML private TableColumn<AdminExamsController.AdminExamRecord, String> statusColumn;

    // Calendar
    @FXML private Label monthYearLabel;
    @FXML private GridPane calendarGrid;
    
    @FXML private HBox scheduleExamCard;

    private ClassesController.ClassItem currentClassItem;
    private boolean isAdminMode = true;

    private final ObservableList<AdminExamsController.AdminExamRecord> classExamsData = FXCollections.observableArrayList();
    private FilteredList<AdminExamsController.AdminExamRecord> filteredData;
    private YearMonth currentYearMonth = YearMonth.now();

    private int tutorialWeight = 15;
    private int assignmentWeight = 15;
    private int labTestWeight = 10;
    private int practicalWeight = 0;
    private int finalWeight = 60;

    @FXML
    public void initialize() {
        if (rootPane != null) {
            rootPane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                Node target = (event.getTarget() instanceof Node) ? (Node) event.getTarget() : null;
                if (isInsideNodeType(target, TableView.class) || isInsideNodeType(target, ButtonBase.class)) {
                    return;
                }
                if (examsTable != null) {
                    examsTable.getSelectionModel().clearSelection();
                }
                rootPane.requestFocus();
            });
        }

        setupExamsTable();
        setupFilterControls();
    }

    public void initData(ClassesController.ClassItem classItem, boolean isAdmin) {
        this.currentClassItem = classItem;
        this.isAdminMode = isAdmin;

        if (scheduleExamCard != null) {
            scheduleExamCard.setVisible(isAdmin);
            scheduleExamCard.setManaged(isAdmin);
        }
        if (editWeightageBtn != null) {
            editWeightageBtn.setVisible(isAdmin);
            editWeightageBtn.setManaged(isAdmin);
        }
        if (adminActionBox != null) {
            adminActionBox.setVisible(isAdmin);
            adminActionBox.setManaged(isAdmin);
        }
        if (viewResultBtn != null) {
            viewResultBtn.setText(isAdmin ? "📊 View & Audit Results" : "📊 View Result");
        }

        if (classItem != null) {
            String cleanLecturer = classItem.lecturer != null
                    ? classItem.lecturer.replace(" (Assigned)", "").trim()
                    : "Unassigned";

            if (classTitleLabel != null) {
                classTitleLabel.setText(classItem.code + " — " + classItem.title);
            }
            if (classSubtitleLabel != null) {
                classSubtitleLabel.setText(classItem.department + "  |  " + classItem.year + "  |  " + classItem.semester);
            }
            if (lecturerBadgeLabel != null) {
                lecturerBadgeLabel.setText("👨‍🏫 Lecturer: " + cleanLecturer);
            }
            if (examsListHeaderLabel != null) {
                examsListHeaderLabel.setText("📅 " + classItem.code + " Exams List");
            }
        }

        loadWeightageFromDatabase();
        reloadClassExams();
        jumpCalendarToNearestExamMonth();
        buildCalendar();
    }

    private void setupExamsTable() {
        if (examsTable == null) return;

        titleColumn.setCellValueFactory(c -> c.getValue().titleProperty());
        subjectColumn.setCellValueFactory(c -> c.getValue().subjectProperty());
        dateColumn.setCellValueFactory(c -> c.getValue().examDateProperty());
        durationColumn.setCellValueFactory(c -> c.getValue().durationProperty());

        statusColumn.setCellValueFactory(c -> c.getValue().statusProperty());
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(status);
                    String base = "-fx-padding: 3px 10px; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 11px;";
                    switch (status.toLowerCase()) {
                        case "finished"  -> badge.setStyle(base + " -fx-background-color: #dcfce7; -fx-text-fill: #16a34a;");
                        case "published" -> badge.setStyle(base + " -fx-background-color: #e0f2fe; -fx-text-fill: #0284c7;");
                        default          -> badge.setStyle(base + " -fx-background-color: #f3e8ff; -fx-text-fill: #9333ea;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        filteredData = new FilteredList<>(classExamsData, b -> true);
        SortedList<AdminExamsController.AdminExamRecord> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(examsTable.comparatorProperty());
        examsTable.setItems(sortedData);

        lockTableColumns(examsTable, 0.20, 0.32, 0.18, 0.14, 0.156);
    }

    private void setupFilterControls() {
        if (typeFilterCombo != null) {
            typeFilterCombo.setItems(FXCollections.observableArrayList(
                    "All Types", "Tutorial", "Quiz", "Assignment", "Lab Test", "Final Exam"));
            typeFilterCombo.setValue("All Types");
            typeFilterCombo.valueProperty().addListener((obs, o, n) -> handleFilter());
        }
        if (statusFilterCombo != null) {
            statusFilterCombo.setItems(FXCollections.observableArrayList(
                    "All Status", "Finished", "Published", "Draft"));
            statusFilterCombo.setValue("All Status");
            statusFilterCombo.valueProperty().addListener((obs, o, n) -> handleFilter());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> handleFilter());
        }
    }

    private void reloadClassExams() {
        AdminExamsController.autoUpdateExpiredStatuses();
        classExamsData.clear();
        if (currentClassItem == null) return;

        for (AdminExamsController.AdminExamRecord ex : AdminExamsController.deptExamMasterData) {
            if (AdminExamsController.examBelongsToClass(ex, currentClassItem)) {
                classExamsData.add(ex);
            }
        }
        handleFilter();
    }

    private void jumpCalendarToNearestExamMonth() {
        LocalDate firstFound = null;
        for (AdminExamsController.AdminExamRecord ex : classExamsData) {
            LocalDate d = AdminExamsController.parseExamDate(ex.getExamDate());
            if (d != null) {
                firstFound = d;
                break;
            }
        }
        currentYearMonth = (firstFound != null) ? YearMonth.from(firstFound) : YearMonth.now();
    }

    // =========================================================================
    // GRADING CRITERIA & WEIGHTAGE (SYNCED WITH MYSQL assessment_weight)
    // =========================================================================

    private void loadWeightageFromDatabase() {
        if (currentClassItem != null && currentClassItem.getOfferingId() > 0) {
            CourseOfferingDAO dao = new CourseOfferingDAO();
            AssessmentWeightRecord w = dao.getAssessmentWeightByOfferingId(currentClassItem.getOfferingId());
            if (w != null) {
                tutorialWeight = w.tutorial();
                assignmentWeight = w.assignment();
                labTestWeight = w.labTest();
                practicalWeight = w.practical();
                finalWeight = w.finalExam();
                refreshWeightageUI();
                return;
            }
        }

        // Fallback defaults if offering has no DB weight row yet
        tutorialWeight = 15;
        assignmentWeight = 15;
        labTestWeight = 10;
        practicalWeight = 0;
        finalWeight = 60;
        refreshWeightageUI();
    }

    private void refreshWeightageUI() {
        int total = tutorialWeight + assignmentWeight + labTestWeight + practicalWeight + finalWeight;

        if (tutorialWeightLabel != null) tutorialWeightLabel.setText(tutorialWeight + "%");
        if (assignmentWeightLabel != null) assignmentWeightLabel.setText(assignmentWeight + "%");
        if (labTestWeightLabel != null) labTestWeightLabel.setText(labTestWeight + "%");
        if (practicalWeightLabel != null) practicalWeightLabel.setText(practicalWeight + "%");
        if (finalWeightLabel != null) finalWeightLabel.setText(finalWeight + "%");
        if (totalWeightLabel != null) totalWeightLabel.setText(total + "%");
        if (donutTotalLabel != null) donutTotalLabel.setText(total + "%");

        if (legendTutLabel != null) legendTutLabel.setText(tutorialWeight + "%");
        if (legendAssignLabel != null) legendAssignLabel.setText(assignmentWeight + "%");
        if (legendLabLabel != null) legendLabLabel.setText(labTestWeight + "%");
        if (legendPracticalLabel != null) legendPracticalLabel.setText(practicalWeight + "%");
        if (legendFinalLabel != null) legendFinalLabel.setText(finalWeight + "%");

        if (weightagePieChart != null) {
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
            if (tutorialWeight > 0) pieChartData.add(new PieChart.Data("Tutorial", tutorialWeight));
            if (assignmentWeight > 0) pieChartData.add(new PieChart.Data("Assignment", assignmentWeight));
            if (labTestWeight > 0) pieChartData.add(new PieChart.Data("Lab Test", labTestWeight));
            if (practicalWeight > 0) pieChartData.add(new PieChart.Data("Practical", practicalWeight));
            if (finalWeight > 0) pieChartData.add(new PieChart.Data("Final Exam", finalWeight));

            weightagePieChart.setData(pieChartData);

            Platform.runLater(() -> {
                for (PieChart.Data data : weightagePieChart.getData()) {
                    if (data.getNode() != null) {
                        String name = data.getName();
                        if (name.equals("Tutorial")) data.getNode().setStyle("-fx-pie-color: #3b82f6;");
                        else if (name.equals("Assignment")) data.getNode().setStyle("-fx-pie-color: #9333ea;");
                        else if (name.equals("Lab Test")) data.getNode().setStyle("-fx-pie-color: #fbbf24;");
                        else if (name.equals("Practical")) data.getNode().setStyle("-fx-pie-color: #06b6d4;");
                        else if (name.equals("Final Exam")) data.getNode().setStyle("-fx-pie-color: #22c55e;");
                    }
                }
            });
        }
    }

    @FXML
    public void handleEditWeightage() {
        if (!isAdminMode) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Grading Criteria Weightage");
        dialog.setHeaderText("Configure Assessment Weightages for "
                + (currentClassItem != null ? currentClassItem.code : "Class") + " (Must Sum to 100%)");

        TextField tutField = new TextField(String.valueOf(tutorialWeight));
        TextField assignField = new TextField(String.valueOf(assignmentWeight));
        TextField labField = new TextField(String.valueOf(labTestWeight));
        TextField practicalField = new TextField(String.valueOf(practicalWeight));
        TextField finalField = new TextField(String.valueOf(finalWeight));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(14, 18, 10, 18));
        grid.addRow(0, new Label("Tutorial (%):"), tutField);
        grid.addRow(1, new Label("Assignment (%):"), assignField);
        grid.addRow(2, new Label("Lab Test (%):"), labField);
        grid.addRow(3, new Label("Practical (%):"), practicalField);
        grid.addRow(4, new Label("Final Exam (%):"), finalField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            try {
                int t = Integer.parseInt(tutField.getText().trim());
                int a = Integer.parseInt(assignField.getText().trim());
                int l = Integer.parseInt(labField.getText().trim());
                int p = Integer.parseInt(practicalField.getText().trim());
                int f = Integer.parseInt(finalField.getText().trim());
                if (t < 0 || a < 0 || l < 0 || p < 0 || f < 0 || (t + a + l + p + f) != 100) {
                    showAlert("Invalid Weightage", "Weightages must be non-negative and sum strictly to 100%.", Alert.AlertType.ERROR);
                    return;
                }

                if (currentClassItem != null && currentClassItem.getOfferingId() > 0) {
                    CourseOfferingDAO dao = new CourseOfferingDAO();
                    dao.updateAssessmentWeight(currentClassItem.getOfferingId(), t, a, l, p, f);
                }

                tutorialWeight = t;
                assignmentWeight = a;
                labTestWeight = l;
                practicalWeight = p;
                finalWeight = f;
                refreshWeightageUI();
            } catch (NumberFormatException ex) {
                showAlert("Invalid Input", "Please enter valid integer percentages.", Alert.AlertType.ERROR);
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Database Error", "Failed to save weightages:\n" + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // =========================================================================
    // FILTERING & EXAM ACTIONS
    // =========================================================================

    @FXML
    public void handleFilter() {
        if (filteredData == null) return;
        String selType = typeFilterCombo != null ? typeFilterCombo.getValue() : "All Types";
        String selStatus = statusFilterCombo != null ? statusFilterCombo.getValue() : "All Status";
        String q = (searchField != null && searchField.getText() != null)
                ? searchField.getText().trim().toLowerCase() : "";

        filteredData.setPredicate(exam -> {
            boolean matchType = selType == null || selType.equals("All Types")
                    || exam.getTitle().toLowerCase().contains(selType.toLowerCase().replace(" exam", ""));
            boolean matchStatus = selStatus == null || selStatus.equals("All Status")
                    || exam.getStatus().equalsIgnoreCase(selStatus);
            boolean matchSearch = q.isEmpty()
                    || exam.getTitle().toLowerCase().contains(q)
                    || exam.getSubject().toLowerCase().contains(q)
                    || exam.getExamDate().toLowerCase().contains(q)
                    || exam.getStatus().toLowerCase().contains(q);
            return matchType && matchStatus && matchSearch;
        });
    }

    @FXML
    public void handleResetFilters() {
        if (typeFilterCombo != null) typeFilterCombo.setValue("All Types");
        if (statusFilterCombo != null) statusFilterCombo.setValue("All Status");
        if (searchField != null) searchField.clear();
        if (filteredData != null) filteredData.setPredicate(b -> true);
    }

    @FXML
    public void handleScheduleExamForClass() {
        if (!isAdminMode || currentClassItem == null) return;
        AdminExamsController.showScheduleExamDialog(currentClassItem, () -> {
            reloadClassExams();
            buildCalendar();
        });
    }

    @FXML
    public void handleEditExam() {
        if (!isAdminMode) return;
        AdminExamsController.AdminExamRecord selected = examsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an exam row from the table to edit.", Alert.AlertType.WARNING);
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Exam Details");
        dialog.setHeaderText("Modify " + selected.getTitle() + " - " + selected.getSubject());

        TextField titleField = new TextField(selected.getTitle());
        LocalDate existingDate = AdminExamsController.parseExamDate(selected.getExamDate());
        DatePicker datePicker = new DatePicker(existingDate != null ? existingDate : LocalDate.now());
        TextField durationField = new TextField(selected.getDuration());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(14, 18, 10, 18));
        grid.addRow(0, new Label("Exam Title:"), titleField);
        grid.addRow(1, new Label("Exam Date:"), datePicker);
        grid.addRow(2, new Label("Duration:"), durationField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                if (!titleField.getText().trim().isEmpty()) {
                    selected.titleProperty().set(titleField.getText().trim());
                }
                if (datePicker.getValue() != null) {
                    LocalDate newDate = datePicker.getValue();
                    selected.examDateProperty().set(newDate.format(AdminExamsController.DISPLAY_DATE_FMT));
                    if (newDate.isBefore(LocalDate.now())) {
                        selected.statusProperty().set("Finished");
                    }
                }
                selected.durationProperty().set(durationField.getText().trim());
                examsTable.refresh();
                buildCalendar();
            }
        });
    }

    @FXML
    public void handleTogglePublishStatus() {
        if (!isAdminMode) return;
        AdminExamsController.AdminExamRecord selected = examsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an exam row to toggle status.", Alert.AlertType.WARNING);
            return;
        }

        LocalDate examDate = AdminExamsController.parseExamDate(selected.getExamDate());
        if (examDate != null && examDate.isBefore(LocalDate.now())) {
            selected.statusProperty().set("Finished");
            examsTable.refresh();
            showAlert("Exam Date Passed",
                    "This exam date (" + selected.getExamDate() + ") has already passed, so its status remains Finished.",
                    Alert.AlertType.INFORMATION);
            return;
        }

        String nextStatus = switch (selected.getStatus().toLowerCase()) {
            case "draft" -> "Published";
            case "published" -> "Finished";
            default -> "Draft";
        };
        selected.statusProperty().set(nextStatus);
        examsTable.refresh();
    }

    @FXML
    public void handleDeleteExam() {
        if (!isAdminMode) return;
        AdminExamsController.AdminExamRecord selected = examsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an exam row to delete.", Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + selected.getTitle() + " (" + selected.getSubject() + ")?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                AdminExamsController.deptExamMasterData.remove(selected);
                classExamsData.remove(selected);
                buildCalendar();
            }
        });
    }

    @FXML
    public void handleViewResult() {
        AdminExamsController.AdminExamRecord selected = examsTable.getSelectionModel().getSelectedItem();
        if (selected == null && !examsTable.getItems().isEmpty()) {
            selected = examsTable.getItems().get(0);
        }
        if (selected == null) {
            showAlert("No Exam", "No exam record available for this class yet.", Alert.AlertType.WARNING);
            return;
        }

        try {
            StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
            if (isAdminMode) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("AdminResultView.fxml"));
                Parent resultView = loader.load();
                AdminResultController controller = loader.getController();
                if (controller != null) {
                    controller.initData(selected, currentClassItem);
                }
                ViewNavigationHistory.navigateForward(contentArea, resultView);
            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("ResultView.fxml"));
                Parent resultView = loader.load();
                ResultController controller = loader.getController();
                if (controller != null) {
                    Exam lecturerExam = new Exam(
                            selected.getTitle(),
                            selected.getSubject(),
                            selected.getClassName(),
                            selected.getExamDate(),
                            selected.getDuration(),
                            selected.getStatus(),
                            selected.getTotalMarks()
                    );
                    controller.initData(lecturerExam, currentClassItem);
                }
                ViewNavigationHistory.navigateForward(contentArea, resultView);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Loading Error", "Failed to load Result view:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleBack() {
        StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
        if (ViewNavigationHistory.goBack(contentArea)) {
            return;
        }
        try {
            String targetFxml = isAdminMode ? "AdminExamsView.fxml" : "ExamsView.fxml";
            Parent examsView = FXMLLoader.load(getClass().getResource(targetFxml));
            if (contentArea != null) {
                contentArea.getChildren().setAll(examsView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // STYLED EXAM CALENDAR (MATCHES CREATE CLASS DATEPICKER THEME)
    // =========================================================================

    @FXML
    public void handlePrevMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        buildCalendar();
    }

    @FXML
    public void handleNextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        buildCalendar();
    }

    private void buildCalendar() {
        if (monthYearLabel != null) {
            monthYearLabel.setText(
                    currentYearMonth.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
                            + " " + currentYearMonth.getYear());
        }
        if (calendarGrid == null) return;

        calendarGrid.getChildren().removeIf(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) > 0);

        Set<LocalDate> examDates = new HashSet<>();
        for (AdminExamsController.AdminExamRecord exam : classExamsData) {
            LocalDate parsed = AdminExamsController.parseExamDate(exam.getExamDate());
            if (parsed != null) {
                examDates.add(parsed);
            }
        }

        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int startDayOffset = firstOfMonth.getDayOfWeek().getValue() % 7;
        int daysInMonth = currentYearMonth.lengthOfMonth();

        int row = 1;
        int col = startDayOffset;

        YearMonth prevMonth = currentYearMonth.minusMonths(1);
        int daysInPrevMonth = prevMonth.lengthOfMonth();
        for (int i = startDayOffset - 1; i >= 0; i--) {
            calendarGrid.add(createCalCell(String.valueOf(daysInPrevMonth - i), true, false, false), i, row);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate cellDate = currentYearMonth.atDay(day);
            boolean isExamDate = examDates.contains(cellDate);
            boolean isToday = cellDate.equals(today);
            calendarGrid.add(createCalCell(String.valueOf(day), false, isExamDate, isToday), col, row);
            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }

        int nextMonthDay = 1;
        while (row <= 6 && (row > 1 || col > 0)) {
            if (col > 6) {
                col = 0;
                row++;
                if (row > 6) break;
            }
            calendarGrid.add(createCalCell(String.valueOf(nextMonthDay++), true, false, false), col, row);
            col++;
        }
    }

    private StackPane createCalCell(String text, boolean outsideMonth, boolean isExamDate, boolean isToday) {
        StackPane cellPane = new StackPane();
        cellPane.setMinSize(0, 28);
        cellPane.setPrefHeight(28);
        cellPane.setMaxSize(Double.MAX_VALUE, 28);

        Label dayLabel = new Label(text);

        if (isExamDate) {
            cellPane.setStyle("-fx-background-color: #2563eb; -fx-background-radius: 6px; -fx-border-color: #1d4ed8; -fx-border-radius: 6px;");
            dayLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11.5px;");
        } else if (isToday) {
            cellPane.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 6px; -fx-border-color: #2563eb; -fx-border-width: 1.5px; -fx-border-radius: 6px;");
            dayLabel.setStyle("-fx-text-fill: #1d4ed8; -fx-font-weight: bold; -fx-font-size: 11.5px;");
        } else if (outsideMonth) {
            cellPane.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 5px; -fx-border-color: #f1f5f9; -fx-border-radius: 5px;");
            dayLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        } else {
            cellPane.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 5px; -fx-border-color: #f1f5f9; -fx-border-radius: 5px;");
            dayLabel.setStyle("-fx-text-fill: #1e293b; -fx-font-size: 11.5px;");
        }

        cellPane.getChildren().add(dayLabel);
        StackPane.setAlignment(dayLabel, Pos.CENTER);
        return cellPane;
    }

    private boolean isInsideNodeType(Node node, Class<?> type) {
        Node current = node;
        while (current != null) {
            if (type.isInstance(current)) return true;
            current = current.getParent();
        }
        return false;
    }

    private void lockTableColumns(TableView<?> table, double... ratios) {
        if (table == null || table.getColumns().size() != ratios.length) return;
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        for (int i = 0; i < ratios.length; i++) {
            TableColumn<?, ?> col = table.getColumns().get(i);
            col.setResizable(false);
            col.setReorderable(false);
            col.setSortable(false);
            col.minWidthProperty().bind(table.widthProperty().multiply(ratios[i]));
            col.maxWidthProperty().bind(table.widthProperty().multiply(ratios[i]));
            col.prefWidthProperty().bind(table.widthProperty().multiply(ratios[i]));
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}