package application;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AdminExamsController {

    @FXML private VBox rootPane;
    @FXML private Label examsDeptLabel;
    @FXML private Label draftsCountLabel;
    @FXML private Label finishedCountLabel;
    @FXML private Label totalExamsCountLabel;
    @FXML private Label classListBadge;

    @FXML private ComboBox<String> academicYearCombo;
    @FXML private ComboBox<String> semesterCombo;
    @FXML private TextField searchField;

    @FXML private GridPane cardsGrid;
    @FXML private Label paginationInfoLabel;
    @FXML private HBox paginationControlsBox;

    private static final int ITEMS_PER_PAGE = 6;
    private int currentPage = 1;

    private final ObservableList<ClassesController.ClassItem> masterClassData = ClassesController.SHARED_CLASS_DATA;
    private List<ClassesController.ClassItem> filteredClassData = new ArrayList<>();

    public static final DateTimeFormatter DISPLAY_DATE_FMT =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    public static final ObservableList<AdminExamRecord> deptExamMasterData = FXCollections.observableArrayList(
            new AdminExamRecord("Tutorial 1", "Advanced Database Systems", "Sadayo Kawakami", "CEIT-3101", "First Semester", "10 Jun 2026", "1 hour", "Finished", 50, "CEIT-3101"),
            new AdminExamRecord("Quiz 1", "Advanced Database Systems", "Sadayo Kawakami", "CEIT-3101", "First Semester", "18 Jun 2026", "30 mins", "Published", 20, "CEIT-3101"),
            new AdminExamRecord("Final Exam", "Advanced Database Systems", "Sadayo Kawakami", "CEIT-3101", "First Semester", "15 Dec 2026", "3 hours", "Published", 100, "CEIT-3101"),
            new AdminExamRecord("Tutorial 1", "Object-Oriented Programming with Java", "Sojiro Sakura", "CEIT-3102", "First Semester", "12 Jun 2026", "1 hour", "Finished", 50, "CEIT-3102"),
            new AdminExamRecord("Quiz 1", "Object-Oriented Programming with Java", "Sojiro Sakura", "CEIT-3102", "First Semester", "28 Nov 2026", "30 mins", "Draft", 20, "CEIT-3102"),
            new AdminExamRecord("Final Exam", "Object-Oriented Programming with Java", "Sojiro Sakura", "CEIT-3102", "First Semester", "18 Dec 2026", "3 hours", "Published", 100, "CEIT-3102"),
            new AdminExamRecord("Tutorial 1", "Advanced Thermodynamics", "Tae Takemi", "ME-3101", "First Semester", "14 Jun 2026", "2 hours", "Finished", 50, "ME-3101"),
            new AdminExamRecord("Final Exam", "Advanced Thermodynamics", "Tae Takemi", "ME-3101", "First Semester", "20 Dec 2026", "3 hours", "Draft", 100, "ME-3101")
    );

    @FXML
    public void initialize() {
        if (examsDeptLabel != null) {
            examsDeptLabel.setText(UserSession.getFormattedDepartment());
        }

        initDropdowns();
        ClassesController.initSampleClassData();
        autoUpdateExpiredStatuses();

        ClassesController.SHARED_CLASS_DATA.addListener(
                (ListChangeListener<ClassesController.ClassItem>) change -> Platform.runLater(this::applyFilter));

        deptExamMasterData.addListener(
                (ListChangeListener<AdminExamRecord>) change -> Platform.runLater(this::updateMetricCards));

        if (academicYearCombo != null) {
            academicYearCombo.valueProperty().addListener((obs, o, n) -> applyFilter());
        }
        if (semesterCombo != null) {
            semesterCombo.valueProperty().addListener((obs, o, n) -> applyFilter());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        }

        updateMetricCards();
        applyFilter();
    }

    private void initDropdowns() {
        if (academicYearCombo != null) {
            academicYearCombo.setItems(FXCollections.observableArrayList(
                    "All Years", "First Year", "Second Year", "Third Year", "Fourth Year", "Fifth Year"));
            academicYearCombo.getSelectionModel().selectFirst();
        }
        if (semesterCombo != null) {
            semesterCombo.setItems(FXCollections.observableArrayList(
                    "All Semesters", "First Semester", "Second Semester"));
            semesterCombo.getSelectionModel().selectFirst();
        }
    }

    /**
     * Automatically marks any exam whose scheduled date has passed (strictly before today)
     * as "Finished".
     */
    public static void autoUpdateExpiredStatuses() {
        LocalDate today = LocalDate.now();
        for (AdminExamRecord exam : deptExamMasterData) {
            LocalDate parsed = parseExamDate(exam.getExamDate());
            if (parsed != null && parsed.isBefore(today) && !"Finished".equalsIgnoreCase(exam.getStatus())) {
                exam.statusProperty().set("Finished");
            }
        }
    }

    public static LocalDate parseExamDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        String trimmed = dateStr.trim();
        DateTimeFormatter[] formatters = {
                DISPLAY_DATE_FMT,
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        };
        for (DateTimeFormatter fmt : formatters) {
            try {
                return LocalDate.parse(trimmed, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    /**
     * Checks whether an exam belongs to a given ClassItem (matches offeringKey, course code, or subject title).
     */
    public static boolean examBelongsToClass(AdminExamRecord exam, ClassesController.ClassItem item) {
        if (exam == null || item == null) return false;
        if (exam.getOfferingKey() != null && !exam.getOfferingKey().isBlank()) {
            if (exam.getOfferingKey().equalsIgnoreCase(item.getOfferingKey())) {
                return true;
            }
            if (exam.getOfferingKey().equalsIgnoreCase(item.code)) {
                return true;
            }
        }
        if (exam.getClassName() != null && exam.getClassName().equalsIgnoreCase(item.code)) {
            return true;
        }
        return exam.getSubject() != null && item.title != null
                && exam.getSubject().equalsIgnoreCase(item.title);
    }

    private void updateMetricCards() {
        autoUpdateExpiredStatuses();
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
    public void handleResetFilters(ActionEvent event) {
        if (academicYearCombo != null) academicYearCombo.setValue("All Years");
        if (semesterCombo != null) semesterCombo.setValue("All Semesters");
        if (searchField != null) searchField.clear();
        applyFilter();
    }

    private void applyFilter() {
        String selectedYear = academicYearCombo != null ? academicYearCombo.getValue() : "All Years";
        String selectedSem = semesterCombo != null ? semesterCombo.getValue() : "All Semesters";
        String keyword = (searchField == null || searchField.getText() == null)
                ? "" : searchField.getText().trim().toLowerCase();

        filteredClassData = masterClassData.stream().filter(c -> {
            boolean matchesYear = (selectedYear == null || selectedYear.equals("All Years")
                    || c.year.equalsIgnoreCase(selectedYear));
            boolean matchesSem = (selectedSem == null || selectedSem.equals("All Semesters")
                    || c.semester.equalsIgnoreCase(selectedSem));
            boolean matchesSearch = keyword.isEmpty()
                    || c.code.toLowerCase().contains(keyword)
                    || c.title.toLowerCase().contains(keyword)
                    || c.department.toLowerCase().contains(keyword)
                    || c.major.toLowerCase().contains(keyword)
                    || c.lecturer.toLowerCase().contains(keyword);
            return matchesYear && matchesSem && matchesSearch;
        }).collect(Collectors.toList());

        currentPage = 1;
        renderCurrentPage();
    }

    private void renderCurrentPage() {
        if (cardsGrid == null) return;
        cardsGrid.getChildren().clear();

        int totalItems = filteredClassData.size();
        int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        if (currentPage > totalPages) currentPage = totalPages;

        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);

        for (int i = startIndex; i < endIndex; i++) {
            int relativeIndex = i - startIndex;
            int col = relativeIndex % 3;
            int row = relativeIndex / 3;

            ClassesController.ClassItem item = filteredClassData.get(i);
            VBox card = ClassCardFactory.createExamCard(item, true, this::handleOpenClassExamDetail);
            cardsGrid.add(card, col, row);
        }

        if (paginationInfoLabel != null) {
            paginationInfoLabel.setText(totalItems == 0
                    ? "Showing 0 - 0 of 0 classes"
                    : "Showing " + (startIndex + 1) + " - " + endIndex + " of " + totalItems + " classes");
        }
        if (classListBadge != null) {
            classListBadge.setText(totalItems + " available");
        }

        renderPaginationButtons(totalPages);
    }

    private void renderPaginationButtons(int totalPages) {
        if (paginationControlsBox == null) return;
        paginationControlsBox.getChildren().clear();

        String navStyle = "-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-text-fill: #334155; "
                + "-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; "
                + "-fx-min-width: 34px; -fx-min-height: 34px; -fx-padding: 5px 11px; -fx-cursor: hand;";
        String activeStyle = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 13.5px; "
                + "-fx-font-weight: bold; -fx-background-radius: 6px; -fx-min-width: 34px; -fx-min-height: 34px; "
                + "-fx-padding: 5px 12px; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-text-fill: #1e293b; "
                + "-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; "
                + "-fx-min-width: 34px; -fx-min-height: 34px; -fx-padding: 5px 12px; -fx-cursor: hand;";

        // Previous Button '<'
        Button prevBtn = new Button("<");
        prevBtn.setStyle(navStyle);
        prevBtn.setDisable(currentPage <= 1);
        prevBtn.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                renderCurrentPage();
            }
        });
        paginationControlsBox.getChildren().add(prevBtn);

        // Page Number Buttons '1', '2', ...
        for (int p = 1; p <= totalPages; p++) {
            final int pageNum = p;
            Button pageBtn = new Button(String.valueOf(pageNum));
            pageBtn.setStyle(pageNum == currentPage ? activeStyle : inactiveStyle);
            pageBtn.setOnAction(e -> {
                currentPage = pageNum;
                renderCurrentPage();
            });
            paginationControlsBox.getChildren().add(pageBtn);
        }

        // Next Button '>'
        Button nextBtn = new Button(">");
        nextBtn.setStyle(navStyle);
        nextBtn.setDisable(currentPage >= totalPages);
        nextBtn.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                renderCurrentPage();
            }
        });
        paginationControlsBox.getChildren().add(nextBtn);
    }

    private void handleOpenClassExamDetail(ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        String offeringKey = (String) clickedBtn.getUserData();

        ClassesController.ClassItem selectedClass = masterClassData.stream()
                .filter(c -> c.getOfferingKey().equalsIgnoreCase(offeringKey))
                .findFirst()
                .orElse(null);

        if (selectedClass == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ClassExamDetailView.fxml"));
            Parent detailView = loader.load();

            ClassExamDetailController controller = loader.getController();
            if (controller != null) {
                controller.initData(selectedClass, true);
            }

            StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
            if (contentArea != null) {
            	ViewNavigationHistory.navigateForward(contentArea, detailView);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Loading Error", "Failed to load ClassExamDetailView.fxml:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleScheduleNewExam() {
        if (masterClassData.isEmpty()) {
            showAlert("No Classes Found", "Create at least one class before scheduling an exam.", Alert.AlertType.WARNING);
            return;
        }
        showScheduleExamDialog(null, () -> {
            autoUpdateExpiredStatuses();
            updateMetricCards();
        });
    }

    /**
     * Reusable dialog for scheduling a new exam either from the Admin Exams overview
     * (choose any class) or from inside a specific Class Exam Detail page (pre-locked to that class).
     */
    public static void showScheduleExamDialog(ClassesController.ClassItem preselectedClass, Runnable onSaved) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Schedule Department Exam");
        dialog.setHeaderText(preselectedClass != null
                ? "Schedule Exam for " + preselectedClass.code + " - " + preselectedClass.title
                : "Create & Assign New Exam");

        ComboBox<String> classPicker = new ComboBox<>();
        ObservableList<String> classLabels = FXCollections.observableArrayList();
        for (ClassesController.ClassItem c : ClassesController.SHARED_CLASS_DATA) {
            classLabels.add(c.code + " — " + c.title + " (" + c.major + ", " + c.year + ")");
        }
        classPicker.setItems(classLabels);
        classPicker.setMaxWidth(Double.MAX_VALUE);
        if (preselectedClass != null) {
            int idx = ClassesController.SHARED_CLASS_DATA.indexOf(preselectedClass);
            if (idx >= 0) classPicker.getSelectionModel().select(idx);
            classPicker.setDisable(true);
        } else if (!classLabels.isEmpty()) {
            classPicker.getSelectionModel().selectFirst();
        }

        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList(
                "Tutorial 1", "Tutorial 2", "Quiz 1", "Quiz 2", "Assignment", "Final Exam"));
        typeBox.setEditable(true);
        typeBox.setValue("Tutorial 1");
        typeBox.setMaxWidth(Double.MAX_VALUE);

        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(7));
        datePicker.setMaxWidth(Double.MAX_VALUE);

        TextField durationField = new TextField("1 hour");
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("Published", "Draft"));
        statusBox.setValue("Published");
        statusBox.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(14, 18, 10, 18));
        grid.addRow(0, new Label("Class / Subject:"), classPicker);
        grid.addRow(1, new Label("Exam Title:"), typeBox);
        grid.addRow(2, new Label("Exam Date:"), datePicker);
        grid.addRow(3, new Label("Duration:"), durationField);
        grid.addRow(4, new Label("Initial Status:"), statusBox);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            int selIdx = classPicker.getSelectionModel().getSelectedIndex();
            ClassesController.ClassItem chosenClass = (preselectedClass != null)
                    ? preselectedClass
                    : (selIdx >= 0 && selIdx < ClassesController.SHARED_CLASS_DATA.size()
                            ? ClassesController.SHARED_CLASS_DATA.get(selIdx) : null);

            if (chosenClass == null) return;

            String title = typeBox.getEditor().getText() != null && !typeBox.getEditor().getText().trim().isEmpty()
                    ? typeBox.getEditor().getText().trim()
                    : (typeBox.getValue() != null ? typeBox.getValue().trim() : "Tutorial 1");
            LocalDate chosenDate = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
            String formattedDate = chosenDate.format(DISPLAY_DATE_FMT);
            String duration = durationField.getText() != null && !durationField.getText().trim().isEmpty()
                    ? durationField.getText().trim() : "1 hour";

            // If the selected exam date has already passed, force status to Finished immediately
            String initialStatus = chosenDate.isBefore(LocalDate.now())
                    ? "Finished"
                    : (statusBox.getValue() != null ? statusBox.getValue() : "Published");

            int maxMarks = title.toLowerCase().contains("quiz") ? 20
                    : (title.toLowerCase().contains("tutorial") ? 50 : 100);

            String cleanLecturer = chosenClass.lecturer != null
                    ? chosenClass.lecturer.replace(" (Assigned)", "").trim()
                    : "Unassigned";

            deptExamMasterData.add(0, new AdminExamRecord(
                    title,
                    chosenClass.title,
                    cleanLecturer,
                    chosenClass.code,
                    chosenClass.semester,
                    formattedDate,
                    duration,
                    initialStatus,
                    maxMarks,
                    chosenClass.getOfferingKey()
            ));

            if (onSaved != null) {
                onSaved.run();
            }
        });
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
        private final String offeringKey;

        public AdminExamRecord(String title, String subject, String lecturedBy, String className,
                               String semester, String examDate, String duration, String status, int totalMarks) {
            this(title, subject, lecturedBy, className, semester, examDate, duration, status, totalMarks, className);
        }

        public AdminExamRecord(String title, String subject, String lecturedBy, String className,
                               String semester, String examDate, String duration, String status,
                               int totalMarks, String offeringKey) {
            this.title = new SimpleStringProperty(title);
            this.subject = new SimpleStringProperty(subject);
            this.lecturedBy = new SimpleStringProperty(lecturedBy);
            this.className = new SimpleStringProperty(className);
            this.semester = new SimpleStringProperty(semester);
            this.examDate = new SimpleStringProperty(examDate);
            this.duration = new SimpleStringProperty(duration);
            this.status = new SimpleStringProperty(status);
            this.totalMarks = new SimpleIntegerProperty(totalMarks);
            this.offeringKey = offeringKey;
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
        public String getOfferingKey() { return offeringKey; }
    }
}