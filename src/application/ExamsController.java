package application;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ExamsController {
	@FXML private Label profileNameLabel;
	
    @FXML private VBox rootPane;
    @FXML private TableView<Exam> examsTable;
    @FXML private TableColumn<Exam, String> titleColumn;
    @FXML private TableColumn<Exam, String> subjectColumn;
//    @FXML private TableColumn<Exam, String> classColumn;
    @FXML private TableColumn<Exam, String> dateColumn;
    @FXML private TableColumn<Exam, String> durationColumn;
    @FXML private TableColumn<Exam, String> statusColumn;

    @FXML private ComboBox<String> classCombo;
    @FXML private ComboBox<String> semesterCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField searchField;
    @FXML private Label examsDeptLabel;

    @FXML private Button viewResultBtn;

    // Calendar UI components
    @FXML private Label monthYearLabel;
    @FXML private GridPane calendarGrid;

    private YearMonth currentYearMonth = YearMonth.of(2024, 1);
    private FilteredList<Exam> filteredData;

    public static ObservableList<Exam> examMasterData = FXCollections.observableArrayList(
        new Exam("Tutorial 1", "Data Structures", "2A", "10 Jan 2024", "1 hour", "Finished", 50),
        new Exam("Final Exam", "Database Systems", "2A", "18 Jan 2024", "1 hour", "Finished", 50),
        new Exam("Final Exam", "Computer Networks", "2A", "25 Jan 2024", "3 hours", "Finished", 100),
        new Exam("Quiz", "Web Technologies", "2A", "28 Jan 2024", "-", "Unfinished", 50),
        new Exam("Tutorial 1", "Operating Systems", "2A", "5 Feb 2024", "2 hours", "Finished", 100),
        new Exam("Final Exam", "Software Engineering", "2A", "8 Feb 2024", "1 hour", "Unfinished", 50)
    );

    @FXML
    public void initialize() {
        if (examsDeptLabel != null) {
            examsDeptLabel.setText(UserSession.getFormattedDepartment());
        }

        if (rootPane != null) {
            rootPane.setOnMouseClicked(event -> {
                examsTable.getSelectionModel().clearSelection();
                rootPane.requestFocus();
            });
        }
        if (profileNameLabel != null) {
            profileNameLabel.setText("Lecturer (" + UserSession.lecturerName + ")");
        }

        examsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

     // Cell value bindings (Make sure classColumn is completely removed here!)
        titleColumn.setCellValueFactory(cell -> cell.getValue().titleProperty());
        subjectColumn.setCellValueFactory(cell -> cell.getValue().subjectProperty());
        dateColumn.setCellValueFactory(cell -> cell.getValue().examDateProperty());
        durationColumn.setCellValueFactory(cell -> cell.getValue().durationProperty());
        statusColumn.setCellValueFactory(cell -> cell.getValue().statusProperty());

        // Setup dropdown options (Major removed; automatically scoped by logged-in department)
        if (classCombo != null) {
            classCombo.setItems(FXCollections.observableArrayList("All Classes", "2A", "2B"));
            classCombo.setValue("All Classes");
        }
        if (semesterCombo != null) {
            semesterCombo.setItems(FXCollections.observableArrayList("Semester 1", "Semester 2"));
            semesterCombo.setValue("Semester 1");
        }
        if (statusCombo != null) {
            statusCombo.setItems(FXCollections.observableArrayList("All Status", "Finished", "Unfinished"));
            statusCombo.setValue("All Status");
        }

        // Filter and Sort setup
        filteredData = new FilteredList<>(examMasterData, b -> true);
        SortedList<Exam> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(examsTable.comparatorProperty());
        examsTable.setItems(sortedData);
        examsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        if (viewResultBtn != null) {
            viewResultBtn.setOnAction(e -> handleViewResult());
        }

        buildCalendar();
        lockTableColumns(examsTable, 0.20, 0.32, 0.18, 0.14, 0.156);
    }

    @FXML
    public void handleFilter() {
        applyFilters();
    }

    @FXML
    public void handleReset() {
        if (classCombo != null) classCombo.setValue("All Classes");
        if (semesterCombo != null) semesterCombo.setValue("Semester 1");
        if (statusCombo != null) statusCombo.setValue("All Status");
        if (searchField != null) searchField.clear();
        if (filteredData != null) filteredData.setPredicate(b -> true);
    }

    private void applyFilters() {
        String selectedClass = classCombo != null ? classCombo.getValue() : "All Classes";
        String selectedStatus = statusCombo != null ? statusCombo.getValue() : "All Status";
        String searchKeyword = searchField != null ? searchField.getText().toLowerCase().trim() : "";

        filteredData.setPredicate(exam -> {
            boolean matchesClass = (selectedClass.equals("All Classes") || exam.getClassName().equalsIgnoreCase(selectedClass));
            boolean matchesStatus = (selectedStatus.equals("All Status") || exam.getStatus().equalsIgnoreCase(selectedStatus));
            boolean matchesSearch = searchKeyword.isEmpty() ||
                exam.getTitle().toLowerCase().contains(searchKeyword) ||
                exam.getSubject().toLowerCase().contains(searchKeyword) ||
                exam.getClassName().toLowerCase().contains(searchKeyword) ||
                exam.getExamDate().toLowerCase().contains(searchKeyword);

            return matchesClass && matchesStatus && matchesSearch;
        });
    }

    @FXML
    public void handleViewResult() {
        Exam selected = examsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openExamResult(selected);
        } else {
            if (!examsTable.getItems().isEmpty()) {
                openExamResult(examsTable.getItems().get(0));
            } else {
                showAlert("No Selection", "Please select an exam row from the table first.", javafx.scene.control.Alert.AlertType.WARNING);
            }
        }
    }

    private void openExamResult(Exam exam) {
        try {
            java.net.URL fxmlUrl = getClass().getResource("ResultView.fxml");
            if (fxmlUrl == null) {
                showAlert("File Missing", "ResultView.fxml could not be found in the application package folder!", javafx.scene.control.Alert.AlertType.ERROR);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent resultView = loader.load();

            ResultController controller = loader.getController();
            if (controller != null) {
                controller.initData(exam);
            }

            if (MainController.instance != null) {
                MainController.instance.loadCustomView(resultView);
            } else {
                showAlert("Layout Error", "Could not locate MainController instance to switch views.", javafx.scene.control.Alert.AlertType.ERROR);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Loading Error", "Failed to load ResultView.fxml:\n" + e.getMessage(), javafx.scene.control.Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, javafx.scene.control.Alert.AlertType type) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

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
            monthYearLabel.setText(currentYearMonth.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH) + " " + currentYearMonth.getYear());
        }

        calendarGrid.getChildren().removeIf(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) > 0);

        Set<LocalDate> examDates = new HashSet<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
        for (Exam exam : examMasterData) {
            try {
                LocalDate date = LocalDate.parse(exam.getExamDate(), formatter);
                examDates.add(date);
            } catch (DateTimeParseException ignored) {}
        }

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeekValue = firstOfMonth.getDayOfWeek().getValue();
        int startDayOffset = liturgicalSundayOffset(dayOfWeekValue);
        int daysInMonth = currentYearMonth.lengthOfMonth();

        int row = 1;
        int col = startDayOffset;

        YearMonth prevMonth = currentYearMonth.minusMonths(1);
        int daysInPrevMonth = prevMonth.lengthOfMonth();
        for (int i = startDayOffset - 1; i >= 0; i--) {
            int dayVal = daysInPrevMonth - i;
            StackPane cellPane = new StackPane();
            cellPane.setMinSize(26, 26);
            cellPane.setMaxSize(26, 26);
            Label dayLabel = new Label(String.valueOf(dayVal));
            dayLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");
            cellPane.getChildren().add(dayLabel);
            StackPane.setAlignment(dayLabel, Pos.CENTER);
            calendarGrid.add(cellPane, i, row);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDate = currentYearMonth.atDay(day);
            boolean isExamDate = examDates.contains(currentDate);

            StackPane cellPane = new StackPane();
            cellPane.setMinSize(26, 26);
            cellPane.setMaxSize(26, 26);

            Label dayLabel = new Label(String.valueOf(day));

            if (isExamDate) {
                cellPane.setStyle("-fx-background-color: #2563eb; -fx-background-radius: 13px;");
                dayLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
            } else {
                dayLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #1e293b;");
            }

            cellPane.getChildren().add(dayLabel);
            StackPane.setAlignment(dayLabel, Pos.CENTER);
            calendarGrid.add(cellPane, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }

        int nextMonthDay = 1;
        while (row <= 5 && (row > 1 || col > 0)) {
            if (col > 6) {
                col = 0;
                row++;
                if (row > 5) break;
            }
            StackPane cellPane = new StackPane();
            cellPane.setMinSize(26, 26);
            cellPane.setMaxSize(26, 26);
            Label dayLabel = new Label(String.valueOf(nextMonthDay++));
            dayLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");
            cellPane.getChildren().add(dayLabel);
            StackPane.setAlignment(dayLabel, Pos.CENTER);
            calendarGrid.add(cellPane, col, row);
            col++;
        }
    }

    private int liturgicalSundayOffset(int dayOfWeekValue) {
        return dayOfWeekValue % 7;
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