package application;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminClassesController {

    @FXML private VBox rootPane;
    @FXML private ComboBox<String> academicYearCombo;
    @FXML private ComboBox<String> semesterCombo;
    @FXML private TextField searchField;
    @FXML private Button resetFilterBtn;
    @FXML private Button createCourseBtn;
    @FXML private Button createClassBtn;
    @FXML private Label myClassesCountLabel;
    @FXML private Label classListBadge;

    @FXML private GridPane cardsGrid;
    @FXML private Label paginationInfoLabel;
    @FXML private HBox paginationControlsBox;
    @FXML private Label adminClassesDeptLabel;

    @FXML private HBox tabClassListBox;
    @FXML private Label tabClassListLabel;
    @FXML private HBox tabDailyLogBox;
    @FXML private Label tabDailyLogLabel;
    @FXML private VBox classListSection;
    @FXML private VBox dailyLogSection;

    @FXML private DatePicker dailyDatePicker;
    @FXML private TextField dailySearchField;
    @FXML private CheckBox chkPresent;
    @FXML private CheckBox chkAbsent;
    @FXML private CheckBox chkExcuse;
    @FXML private Label dailyLogFooterLabel;

    @FXML private TableView<ClassDetailController.DailyLogEntry> dailyLogTable;
    @FXML private TableColumn<ClassDetailController.DailyLogEntry, Integer> colNoLog;
    @FXML private TableColumn<ClassDetailController.DailyLogEntry, String> colRollLog;
    @FXML private TableColumn<ClassDetailController.DailyLogEntry, String> colNameLog;
    @FXML private TableColumn<ClassDetailController.DailyLogEntry, String> colDateLog;
    @FXML private TableColumn<ClassDetailController.DailyLogEntry, String> colSubjLog;
    @FXML private TableColumn<ClassDetailController.DailyLogEntry, String> colStatusLog;
    @FXML private TableColumn<ClassDetailController.DailyLogEntry, String> colRemarksLog;

    private final ObservableList<ClassDetailController.DailyLogEntry> masterDailyLogData = FXCollections.observableArrayList();
    private FilteredList<ClassDetailController.DailyLogEntry> filteredDailyLogData;
    private final Map<String, String> originalDailyStatuses = new HashMap<>();

    private static final int ITEMS_PER_PAGE = 6;
    private int currentPage = 1;

    private final ObservableList<ClassesController.ClassItem> masterClassData = ClassesController.SHARED_CLASS_DATA;
    private List<ClassesController.ClassItem> filteredClassData = new ArrayList<>();

    @FXML
    public void initialize() {
        initDropdowns();
        ClassesController.initSampleClassData();

        ClassesController.SHARED_CLASS_DATA.addListener(
            (ListChangeListener<ClassesController.ClassItem>) change -> Platform.runLater(this::applyFilter)
        );

        if (academicYearCombo != null) {
            academicYearCombo.valueProperty().addListener((obs, o, n) -> applyFilter());
        }
        if (semesterCombo != null) {
            semesterCombo.valueProperty().addListener((obs, o, n) -> applyFilter());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        }
        if (adminClassesDeptLabel != null) {
            adminClassesDeptLabel.setText(UserSession.getFormattedDepartment());
        }

        applyFilter();
        setupDailyLogTab(true);
    }

    private void initDropdowns() {
        if (academicYearCombo != null) {
            academicYearCombo.setItems(FXCollections.observableArrayList(
                "All Years", "First Year", "Second Year", "Third Year", "Fourth Year", "Fifth Year"
            ));
            academicYearCombo.getSelectionModel().selectFirst();
        }
        if (semesterCombo != null) {
            semesterCombo.setItems(FXCollections.observableArrayList(
                "All Semesters", "First Semester", "Second Semester"
            ));
            semesterCombo.getSelectionModel().selectFirst();
        }
    }

    @FXML
    public void handleResetFilters(ActionEvent event) {
        if (academicYearCombo != null) academicYearCombo.setValue("All Years");
        if (semesterCombo != null) semesterCombo.setValue("All Semesters");
        if (searchField != null) searchField.clear();
        applyFilter();
    }

    @FXML
    public void handleCreateCourse(ActionEvent event) {
        try {
            Button clickedButton = (Button) event.getSource();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AdminCreateCourseView.fxml"));
            Parent createCourseView = loader.load();

            StackPane contentArea = (StackPane) clickedButton.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(createCourseView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleCreateClass(ActionEvent event) {
        try {
            Button clickedButton = (Button) event.getSource();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AdminCreateClassView.fxml"));
            Parent createView = loader.load();

            StackPane contentArea = (StackPane) clickedButton.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(createView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void applyFilter() {
        String selectedYear = academicYearCombo != null ? academicYearCombo.getValue() : "All Years";
        String selectedSem = semesterCombo != null ? semesterCombo.getValue() : "All Semesters";
        String keyword = (searchField == null || searchField.getText() == null) ? "" : searchField.getText().trim().toLowerCase();

        filteredClassData = masterClassData.stream().filter(c -> {
            boolean matchesYear = (selectedYear == null || selectedYear.equals("All Years") || c.year.equalsIgnoreCase(selectedYear));
            boolean matchesSem = (selectedSem == null || selectedSem.equals("All Semesters") || c.semester.equalsIgnoreCase(selectedSem));
            boolean matchesSearch = keyword.isEmpty() ||
                    c.code.toLowerCase().contains(keyword) ||
                    c.title.toLowerCase().contains(keyword) ||
                    c.department.toLowerCase().contains(keyword) ||
                    c.className.toLowerCase().contains(keyword) ||
                    c.lecturer.toLowerCase().contains(keyword);

            return matchesYear && matchesSem && matchesSearch;
        }).collect(Collectors.toList());

        currentPage = 1;
        renderCurrentPage();
    }

    private void renderCurrentPage() {
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
            VBox card = createClassCard(item);
            cardsGrid.add(card, col, row);
        }

        if (totalItems == 0) {
            paginationInfoLabel.setText("Showing 0 - 0 of 0 classes");
        } else {
            paginationInfoLabel.setText("Showing " + (startIndex + 1) + " - " + endIndex + " of " + totalItems + " classes");
        }
        if (myClassesCountLabel != null) {
            myClassesCountLabel.setText(String.valueOf(totalItems));
        }
        if (classListBadge != null) {
            classListBadge.setText(totalItems + " available");
        }

        renderPaginationButtons(totalPages);
    }

    private void renderPaginationButtons(int totalPages) {
        paginationControlsBox.getChildren().clear();

        Button prevBtn = new Button("<");
        prevBtn.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 3px 8px; -fx-cursor: hand;");
        prevBtn.setDisable(currentPage <= 1);
        prevBtn.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                renderCurrentPage();
            }
        });
        paginationControlsBox.getChildren().add(prevBtn);

        for (int p = 1; p <= totalPages; p++) {
            final int pageNum = p;
            Button pageBtn = new Button(String.valueOf(pageNum));
            if (pageNum == currentPage) {
                pageBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 3px 10px; -fx-cursor: hand;");
            } else {
                pageBtn.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-text-fill: #1e293b; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 3px 10px; -fx-cursor: hand;");
            }
            pageBtn.setOnAction(e -> {
                currentPage = pageNum;
                renderCurrentPage();
            });
            paginationControlsBox.getChildren().add(pageBtn);
        }

        Button nextBtn = new Button(">");
        nextBtn.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 3px 8px; -fx-cursor: hand;");
        nextBtn.setDisable(currentPage >= totalPages);
        nextBtn.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                renderCurrentPage();
            }
        });
        paginationControlsBox.getChildren().add(nextBtn);
    }

    private VBox createClassCard(ClassesController.ClassItem item) {
        return ClassCardFactory.createCard(item, true, this::handleViewClass, null);
    }

    @FXML
    private void handleViewClass(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String classCode = (String) clickedButton.getUserData();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AdminClassDetailView.fxml"));
            Parent detailView = loader.load();

            AdminClassDetailController detailController = loader.getController();
            if (detailController != null) {
                detailController.initClassData(classCode);
            }

            StackPane contentArea = (StackPane) clickedButton.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(detailView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void switchToClassListTab() {
        if (classListSection != null) { classListSection.setVisible(true); classListSection.setManaged(true); }
        if (dailyLogSection != null) { dailyLogSection.setVisible(false); dailyLogSection.setManaged(false); }
        if (tabClassListBox != null) tabClassListBox.setStyle("-fx-border-color: transparent transparent #1b315e transparent; -fx-border-width: 0 0 3.5 0; -fx-padding: 0 6px 6px 2px; -fx-cursor: hand;");
        if (tabClassListLabel != null) tabClassListLabel.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 14px; -fx-font-weight: 800;");
        if (tabDailyLogBox != null) tabDailyLogBox.setStyle("-fx-border-color: transparent; -fx-border-width: 0 0 3.5 0; -fx-padding: 0 6px 6px 2px; -fx-cursor: hand;");
        if (tabDailyLogLabel != null) tabDailyLogLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: bold;");
    }

    @FXML
    public void switchToDailyLogTab() {
        if (classListSection != null) { classListSection.setVisible(false); classListSection.setManaged(false); }
        if (dailyLogSection != null) { dailyLogSection.setVisible(true); dailyLogSection.setManaged(true); }
        if (tabDailyLogBox != null) tabDailyLogBox.setStyle("-fx-border-color: transparent transparent #1b315e transparent; -fx-border-width: 0 0 3.5 0; -fx-padding: 0 6px 6px 2px; -fx-cursor: hand;");
        if (tabDailyLogLabel != null) tabDailyLogLabel.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 14px; -fx-font-weight: 800;");
        if (tabClassListBox != null) tabClassListBox.setStyle("-fx-border-color: transparent; -fx-border-width: 0 0 3.5 0; -fx-padding: 0 6px 6px 2px; -fx-cursor: hand;");
        if (tabClassListLabel != null) tabClassListLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: bold;");
        if (dailyLogTable != null) dailyLogTable.refresh();
    }

    private void setupDailyLogTab(boolean isAdminEditable) {
        if (dailyDatePicker != null) {
            dailyDatePicker.setConverter(new StringConverter<>() {
                private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                @Override public String toString(LocalDate d) { return d != null ? fmt.format(d) : ""; }
                @Override public LocalDate fromString(String s) {
                    try { return (s != null && !s.isBlank()) ? LocalDate.parse(s, fmt) : null; }
                    catch (Exception e) { return null; }
                }
            });
            dailyDatePicker.setValue(LocalDate.of(2026, 11, 16));
        }

        if (dailyLogTable == null) return;
        dailyLogTable.setEditable(false);

        colNoLog.setCellValueFactory(c -> c.getValue().noProperty().asObject());
        colRollLog.setCellValueFactory(c -> c.getValue().rollNoProperty());
        colNameLog.setCellValueFactory(c -> c.getValue().studentNameProperty());
        colDateLog.setCellValueFactory(c -> c.getValue().dateProperty());
        colSubjLog.setCellValueFactory(c -> c.getValue().subjectProperty());
        colRemarksLog.setCellValueFactory(c -> c.getValue().remarksProperty());
        colStatusLog.setCellValueFactory(c -> c.getValue().statusProperty());

        colStatusLog.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                styleStatusCell(this, item, empty);
            }
        });

        dailyLogTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ClassDetailController.DailyLogEntry item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("row-present", "row-absent", "row-excused");
                if (item != null && !empty && item.getStatus() != null) {
                    String st = item.getStatus().toLowerCase();
                    if (st.equals("present")) getStyleClass().add("row-present");
                    else if (st.equals("absent")) getStyleClass().add("row-absent");
                    else getStyleClass().add("row-excused");
                }
            }
        });

        // TODO (DB Integration): Load from `attendance` + `class_session`
        masterDailyLogData.setAll(
            new ClassDetailController.DailyLogEntry(1, "III-CEIT-1", "Aung Thura", "2026-11-16", "Data Structures and Algorithms", "Present", "On Time"),
            new ClassDetailController.DailyLogEntry(2, "III-CEIT-2", "Su Myat Mya", "2026-11-16", "Data Structures and Algorithms", "Present", "On Time"),
            new ClassDetailController.DailyLogEntry(3, "III-CEIT-3", "Min Khant", "2026-11-16", "Database Systems", "Excused", "Medical Leave"),
            new ClassDetailController.DailyLogEntry(4, "III-CEIT-4", "Hnin Wai", "2026-11-16", "Computer Networks", "Present", "On Time"),
            new ClassDetailController.DailyLogEntry(5, "III-CEIT-5", "Ko Thant", "2026-11-16", "Operating Systems", "Absent", "Unexcused"),
            new ClassDetailController.DailyLogEntry(6, "III-CEIT-6", "Zaw Hein", "2026-11-16", "Software Engineering", "Present", "On Time")
        );
        for (ClassDetailController.DailyLogEntry e : masterDailyLogData) {
            originalDailyStatuses.put(e.getRollNo() + "_" + e.getDate(), e.getStatus());
        }

        filteredDailyLogData = new FilteredList<>(masterDailyLogData, b -> true);
        dailyLogTable.setItems(filteredDailyLogData);
        applyDailyFilters();
    }

    private void styleStatusCell(TableCell<ClassDetailController.DailyLogEntry, String> cell, String item, boolean empty) {
        if (empty || item == null) {
            cell.setText(null);
            cell.setGraphic(null);
        } else if (item.equalsIgnoreCase("Present")) {
            cell.setText("✔ Present");
            cell.setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
        } else if (item.equalsIgnoreCase("Absent")) {
            cell.setText("✖ Absent");
            cell.setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
        } else {
            cell.setText("● " + item);
            cell.setStyle("-fx-text-fill: #b45309; -fx-font-weight: bold;");
        }
    }

    @FXML
    public void applyDailyFilters() {
        if (filteredDailyLogData == null) return;
        LocalDate selDate = dailyDatePicker != null ? dailyDatePicker.getValue() : null;
        String dateStr = selDate != null ? selDate.toString() : "";
        String q = (dailySearchField != null && dailySearchField.getText() != null)
                ? dailySearchField.getText().trim().toLowerCase() : "";
        boolean p = chkPresent != null && chkPresent.isSelected();
        boolean a = chkAbsent != null && chkAbsent.isSelected();
        boolean e = chkExcuse != null && chkExcuse.isSelected();
        boolean any = p || a || e;

        filteredDailyLogData.setPredicate(entry -> {
            boolean matchDate = dateStr.isEmpty() || entry.getDate().equals(dateStr);
            boolean matchSearch = q.isEmpty()
                    || entry.getStudentName().toLowerCase().contains(q)
                    || entry.getRollNo().toLowerCase().contains(q)
                    || entry.getSubject().toLowerCase().contains(q);
            boolean matchStatus = !any
                    || (p && entry.getStatus().equalsIgnoreCase("Present"))
                    || (a && entry.getStatus().equalsIgnoreCase("Absent"))
                    || (e && (entry.getStatus().equalsIgnoreCase("Excused") || entry.getStatus().equalsIgnoreCase("Excuse")));
            return matchDate && matchSearch && matchStatus;
        });

        if (dailyLogFooterLabel != null) {
            int n = filteredDailyLogData.size();
            dailyLogFooterLabel.setText("Showing " + n + " attendance record" + (n == 1 ? "" : "s"));
        }
    }

    @FXML
    public void handleResetDailyFilters() {
        if (dailyDatePicker != null) dailyDatePicker.setValue(null);
        if (dailySearchField != null) dailySearchField.clear();
        if (chkPresent != null) chkPresent.setSelected(false);
        if (chkAbsent != null) chkAbsent.setSelected(false);
        if (chkExcuse != null) chkExcuse.setSelected(false);
        applyDailyFilters();
    }

    @FXML
    public void handleResetDailyStatus() {
        for (ClassDetailController.DailyLogEntry entry : masterDailyLogData) {
            String key = entry.getRollNo() + "_" + entry.getDate();
            if (originalDailyStatuses.containsKey(key)) entry.setStatus(originalDailyStatuses.get(key));
        }
        if (dailyLogTable != null) dailyLogTable.refresh();
    }

    @FXML
    public void handleMarkAllPresent() {
        for (ClassDetailController.DailyLogEntry entry : filteredDailyLogData) {
            entry.setStatus("Present");
        }
        if (dailyLogTable != null) dailyLogTable.refresh();
    }

    @FXML
    public void handleSaveDailyChanges() {
        for (ClassDetailController.DailyLogEntry entry : masterDailyLogData) {
            originalDailyStatuses.put(entry.getRollNo() + "_" + entry.getDate(), entry.getStatus());
        }
        // TODO (DB Integration): Persist updated attendance statuses to the database.
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Attendance Saved");
        alert.setHeaderText(null);
        alert.setContentText("Daily attendance changes have been saved!");
        alert.showAndWait();
    }

    @FXML
    public void handleToggleDailyStatus() {
        ClassDetailController.DailyLogEntry selected = dailyLogTable != null
                ? dailyLogTable.getSelectionModel().getSelectedItem()
                : null;

        if (selected == null) {
            Alert warn = new Alert(Alert.AlertType.WARNING);
            warn.setTitle("No Row Selected");
            warn.setHeaderText(null);
            warn.setContentText("Please select a student row in the Daily Attendance Log first.");
            warn.showAndWait();
            return;
        }

        String current = selected.getStatus() != null ? selected.getStatus() : "Present";
        if (current.equalsIgnoreCase("Present")) {
            selected.setStatus("Absent");
            selected.setRemarks("Unexcused");
        } else if (current.equalsIgnoreCase("Absent")) {
            selected.setStatus("Excused");
            selected.setRemarks("Medical Leave");
        } else {
            selected.setStatus("Present");
            selected.setRemarks("On Time");
        }

        dailyLogTable.refresh();
    }
}