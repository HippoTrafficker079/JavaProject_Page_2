package application;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class AdminExamsController {

    @FXML private VBox rootPane;
    @FXML private Label examsDeptLabel;
    @FXML private Label draftsCountLabel;
    @FXML private Label finishedCountLabel;
    @FXML private Label totalExamsCountLabel;

    @FXML private ComboBox<String> lecturerCombo;
    @FXML private ComboBox<String> classCombo;
    @FXML private ComboBox<String> semesterCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField searchField;

    @FXML private TableView<AdminExamRecord> examsTable;
    @FXML private TableColumn<AdminExamRecord, String> titleColumn;
    @FXML private TableColumn<AdminExamRecord, String> subjectColumn;
    @FXML private TableColumn<AdminExamRecord, String> lecturedByColumn;
    @FXML private TableColumn<AdminExamRecord, String> dateColumn;
    @FXML private TableColumn<AdminExamRecord, String> durationColumn;
    @FXML private TableColumn<AdminExamRecord, String> statusColumn;
    @FXML private Button viewResultBtn;

    @FXML private Label monthYearLabel;
    @FXML private GridPane calendarGrid;

    private YearMonth currentYearMonth = YearMonth.of(2024, 1);
    private FilteredList<AdminExamRecord> filteredData;

    public static final ObservableList<AdminExamRecord> deptExamMasterData = FXCollections.observableArrayList(
        new AdminExamRecord("Tutorial 1", "Data Structures", "Dr. Hla Myint", "2A", "Semester 1", "10 Jan 2024", "1 hour", "Finished", 50),
        new AdminExamRecord("Final Exam", "Database Systems", "Dr. Aung Naing", "2A", "Semester 1", "18 Jan 2024", "3 hours", "Finished", 100),
        new AdminExamRecord("Final Exam", "Computer Networks", "Dr. Su Su", "2A", "Semester 1", "25 Jan 2024", "3 hours", "Finished", 100),
        new AdminExamRecord("Quiz", "Web Technologies", "Prof. Kyaw Zin", "2B", "Semester 1", "28 Jan 2024", "30 mins", "Draft", 20),
        new AdminExamRecord("Tutorial 1", "Operating Systems", "Dr. Tin Maung", "3A", "Semester 2", "5 Feb 2024", "2 hours", "Finished", 50),
        new AdminExamRecord("Final Exam", "Software Engineering", "Dr. Myo Tint", "3A", "Semester 2", "8 Feb 2024", "3 hours", "Draft", 100),
        new AdminExamRecord("Tutorial 2", "Artificial Intelligence", "Dr. Hla Myint", "4A", "Semester 2", "14 Feb 2024", "1 hour", "Published", 50),
        new AdminExamRecord("Quiz", "Machine Learning", "Dr. Aung Naing", "4A", "Semester 2", "20 Feb 2024", "30 mins", "Draft", 20)
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

        titleColumn.setCellValueFactory(c -> c.getValue().titleProperty());
        subjectColumn.setCellValueFactory(c -> c.getValue().subjectProperty());
        lecturedByColumn.setCellValueFactory(c -> c.getValue().lecturedByProperty());
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

        if (lecturerCombo != null) {
            lecturerCombo.setItems(FXCollections.observableArrayList(
                "All Lecturers", "Dr. Hla Myint", "Dr. Aung Naing", "Dr. Su Su", "Prof. Kyaw Zin", "Dr. Tin Maung", "Dr. Myo Tint"
            ));
            lecturerCombo.setValue("All Lecturers");
        }
        if (classCombo != null) {
            classCombo.setItems(FXCollections.observableArrayList("All Classes", "2A", "2B", "3A", "4A"));
            classCombo.setValue("All Classes");
        }
        if (semesterCombo != null) {
            semesterCombo.setItems(FXCollections.observableArrayList("All Semesters", "Semester 1", "Semester 2"));
            semesterCombo.setValue("All Semesters");
        }
        if (statusCombo != null) {
            statusCombo.setItems(FXCollections.observableArrayList("All Status", "Finished", "Published", "Draft"));
            statusCombo.setValue("All Status");
        }

        filteredData = new FilteredList<>(deptExamMasterData, b -> true);
        SortedList<AdminExamRecord> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(examsTable.comparatorProperty());
        examsTable.setItems(sortedData);

        lockTableColumns(examsTable, 0.14, 0.23, 0.20, 0.16, 0.12, 0.146);
        updateMetricCards();
        buildCalendar();
    }

    private void updateMetricCards() {
        int drafts = 0;
        int finished = 0;
        for (AdminExamRecord ex : deptExamMasterData) {
            if ("Draft".equalsIgnoreCase(ex.getStatus())) drafts++;
            else if ("Finished".equalsIgnoreCase(ex.getStatus())) finished++;
        }
        if (draftsCountLabel != null) draftsCountLabel.setText(String.valueOf(drafts));
        if (finishedCountLabel != null) finishedCountLabel.setText(String.valueOf(finished));
        if (totalExamsCountLabel != null) totalExamsCountLabel.setText(String.valueOf(deptExamMasterData.size()));
    }

    @FXML
    public void handleFilter() {
        String selLecturer = lecturerCombo != null ? lecturerCombo.getValue() : "All Lecturers";
        String selClass = classCombo != null ? classCombo.getValue() : "All Classes";
        String selSemester = semesterCombo != null ? semesterCombo.getValue() : "All Semesters";
        String selStatus = statusCombo != null ? statusCombo.getValue() : "All Status";
        String q = searchField != null && searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";

        filteredData.setPredicate(exam -> {
            boolean matchLecturer = selLecturer == null || selLecturer.equals("All Lecturers")
                    || exam.getLecturedBy().equalsIgnoreCase(selLecturer);
            boolean matchClass = selClass == null || selClass.equals("All Classes")
                    || exam.getClassName().equalsIgnoreCase(selClass);
            boolean matchSemester = selSemester == null || selSemester.equals("All Semesters")
                    || exam.getSemester().equalsIgnoreCase(selSemester);
            boolean matchStatus = selStatus == null || selStatus.equals("All Status")
                    || exam.getStatus().equalsIgnoreCase(selStatus);
            boolean matchSearch = q.isEmpty()
                    || exam.getTitle().toLowerCase().contains(q)
                    || exam.getSubject().toLowerCase().contains(q)
                    || exam.getLecturedBy().toLowerCase().contains(q)
                    || exam.getExamDate().toLowerCase().contains(q);
            return matchLecturer && matchClass && matchSemester && matchStatus && matchSearch;
        });
    }

    @FXML
    public void handleReset() {
        if (lecturerCombo != null) lecturerCombo.setValue("All Lecturers");
        if (classCombo != null) classCombo.setValue("All Classes");
        if (semesterCombo != null) semesterCombo.setValue("All Semesters");
        if (statusCombo != null) statusCombo.setValue("All Status");
        if (searchField != null) searchField.clear();
        if (filteredData != null) filteredData.setPredicate(b -> true);
    }

    // =========================================================================
    // ADMIN-EXCLUSIVE EXAM ACTIONS
    // =========================================================================

    @FXML
    public void handleScheduleNewExam() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Schedule Department Exam");
        dialog.setHeaderText("Create & Assign New Exam");

        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("Tutorial 1", "Tutorial 2", "Quiz", "Final Exam"));
        typeBox.setValue("Tutorial 1");
        TextField subjectField = new TextField();
        subjectField.setPromptText("e.g. Cloud Computing");
        ComboBox<String> lecturerBox = new ComboBox<>(FXCollections.observableArrayList(
            "Dr. Hla Myint", "Dr. Aung Naing", "Dr. Su Su", "Prof. Kyaw Zin", "Dr. Tin Maung", "Dr. Myo Tint"
        ));
        lecturerBox.setValue("Dr. Hla Myint");
        ComboBox<String> classBox = new ComboBox<>(FXCollections.observableArrayList("2A", "2B", "3A", "4A"));
        classBox.setValue("3A");
        TextField dateField = new TextField("25 Feb 2024");
        TextField durationField = new TextField("1 hour");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Exam Type:"), typeBox);
        grid.addRow(1, new Label("Subject:"), subjectField);
        grid.addRow(2, new Label("Lectured By:"), lecturerBox);
        grid.addRow(3, new Label("Class:"), classBox);
        grid.addRow(4, new Label("Exam Date:"), dateField);
        grid.addRow(5, new Label("Duration:"), durationField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK && !subjectField.getText().trim().isEmpty()) {
                String type = typeBox.getValue();
                int maxMarks = type.toLowerCase().contains("quiz") ? 20 : (type.toLowerCase().contains("tutorial") ? 50 : 100);
                deptExamMasterData.add(0, new AdminExamRecord(
                    type,
                    subjectField.getText().trim(),
                    lecturerBox.getValue(),
                    classBox.getValue(),
                    "Semester 2",
                    dateField.getText().trim(),
                    durationField.getText().trim(),
                    "Published",
                    maxMarks
                ));
                updateMetricCards();
                buildCalendar();
            }
        });
    }

    @FXML
    public void handleEditExam() {
        AdminExamRecord selected = examsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an exam row from the table to edit.", Alert.AlertType.WARNING);
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Exam Details");
        dialog.setHeaderText("Modify " + selected.getTitle() + " - " + selected.getSubject());

        ComboBox<String> lecturerBox = new ComboBox<>(FXCollections.observableArrayList(
            "Dr. Hla Myint", "Dr. Aung Naing", "Dr. Su Su", "Prof. Kyaw Zin", "Dr. Tin Maung", "Dr. Myo Tint"
        ));
        lecturerBox.setValue(selected.getLecturedBy());
        TextField dateField = new TextField(selected.getExamDate());
        TextField durationField = new TextField(selected.getDuration());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Lectured By:"), lecturerBox);
        grid.addRow(1, new Label("Exam Date:"), dateField);
        grid.addRow(2, new Label("Duration:"), durationField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                selected.lecturedByProperty().set(lecturerBox.getValue());
                selected.examDateProperty().set(dateField.getText().trim());
                selected.durationProperty().set(durationField.getText().trim());
                examsTable.refresh();
                buildCalendar();
            }
        });
    }

    @FXML
    public void handleTogglePublishStatus() {
        AdminExamRecord selected = examsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an exam row to toggle Draft / Published / Finished status.", Alert.AlertType.WARNING);
            return;
        }
        String nextStatus = switch (selected.getStatus().toLowerCase()) {
            case "draft" -> "Published";
            case "published" -> "Finished";
            default -> "Draft";
        };
        selected.statusProperty().set(nextStatus);
        examsTable.refresh();
        updateMetricCards();
    }

    @FXML
    public void handleDeleteExam() {
        AdminExamRecord selected = examsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an exam row to delete.", Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + selected.getTitle() + " (" + selected.getSubject() + " - " + selected.getLecturedBy() + ")?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                deptExamMasterData.remove(selected);
                updateMetricCards();
                buildCalendar();
            }
        });
    }

    @FXML
    public void handleViewResult() {
        AdminExamRecord selected = examsTable.getSelectionModel().getSelectedItem();
        if (selected == null && !examsTable.getItems().isEmpty()) {
            selected = examsTable.getItems().get(0);
        }
        if (selected == null) {
            showAlert("No Exam", "No exam record available to audit.", Alert.AlertType.WARNING);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AdminResultView.fxml"));
            Parent resultView = loader.load();

            AdminResultController controller = loader.getController();
            if (controller != null) {
                controller.initData(selected);
            }

            StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(resultView);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Loading Error", "Failed to load AdminResultView.fxml:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // =========================================================================
    // CALENDAR WIDGET
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
            monthYearLabel.setText(currentYearMonth.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH) + " " + currentYearMonth.getYear());
        }
        if (calendarGrid == null) return;

        calendarGrid.getChildren().removeIf(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) > 0);

        Set<LocalDate> examDates = new HashSet<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
        for (AdminExamRecord exam : deptExamMasterData) {
            try {
                examDates.add(LocalDate.parse(exam.getExamDate(), formatter));
            } catch (DateTimeParseException ignored) {}
        }

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int startDayOffset = firstOfMonth.getDayOfWeek().getValue() % 7;
        int daysInMonth = currentYearMonth.lengthOfMonth();

        int row = 1;
        int col = startDayOffset;

        YearMonth prevMonth = currentYearMonth.minusMonths(1);
        int daysInPrevMonth = prevMonth.lengthOfMonth();
        for (int i = startDayOffset - 1; i >= 0; i--) {
            StackPane cellPane = createCalCell(String.valueOf(daysInPrevMonth - i), "#cbd5e1", false);
            calendarGrid.add(cellPane, i, row);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            boolean isExamDate = examDates.contains(currentYearMonth.atDay(day));
            StackPane cellPane = createCalCell(String.valueOf(day), isExamDate ? "white" : "#1e293b", isExamDate);
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
            calendarGrid.add(createCalCell(String.valueOf(nextMonthDay++), "#cbd5e1", false), col, row);
            col++;
        }
    }

    private StackPane createCalCell(String text, String textFill, boolean highlight) {
        StackPane cellPane = new StackPane();
        cellPane.setMinSize(25, 25);
        cellPane.setMaxSize(25, 25);
        Label dayLabel = new Label(text);
        if (highlight) {
            cellPane.setStyle("-fx-background-color: #2563eb; -fx-background-radius: 13px;");
            dayLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        } else {
            dayLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + textFill + ";");
        }
        cellPane.getChildren().add(dayLabel);
        StackPane.setAlignment(dayLabel, Pos.CENTER);
        return cellPane;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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

    public static class AdminExamRecord {
        private final SimpleStringProperty title;
        private final SimpleStringProperty subject;
        private final SimpleStringProperty lecturedBy;
        private final SimpleStringProperty className;
        private final SimpleStringProperty semester;
        private final SimpleStringProperty examDate;
        private final SimpleStringProperty duration;
        private final SimpleStringProperty status;
        private final SimpleIntegerProperty totalMarks;

        public AdminExamRecord(String title, String subject, String lecturedBy, String className,
                               String semester, String examDate, String duration, String status, int totalMarks) {
            this.title = new SimpleStringProperty(title);
            this.subject = new SimpleStringProperty(subject);
            this.lecturedBy = new SimpleStringProperty(lecturedBy);
            this.className = new SimpleStringProperty(className);
            this.semester = new SimpleStringProperty(semester);
            this.examDate = new SimpleStringProperty(examDate);
            this.duration = new SimpleStringProperty(duration);
            this.status = new SimpleStringProperty(status);
            this.totalMarks = new SimpleIntegerProperty(totalMarks);
        }

        public String getTitle() { return title.get(); }
        public SimpleStringProperty titleProperty() { return title; }

        public String getSubject() { return subject.get(); }
        public SimpleStringProperty subjectProperty() { return subject; }

        public String getLecturedBy() { return lecturedBy.get(); }
        public SimpleStringProperty lecturedByProperty() { return lecturedBy; }

        public String getClassName() { return className.get(); }
        public SimpleStringProperty classNameProperty() { return className; }

        public String getSemester() { return semester.get(); }
        public SimpleStringProperty semesterProperty() { return semester; }

        public String getExamDate() { return examDate.get(); }
        public SimpleStringProperty examDateProperty() { return examDate; }

        public String getDuration() { return duration.get(); }
        public SimpleStringProperty durationProperty() { return duration; }

        public String getStatus() { return status.get(); }
        public SimpleStringProperty statusProperty() { return status; }

        public int getTotalMarks() { return totalMarks.get(); }
    }
}