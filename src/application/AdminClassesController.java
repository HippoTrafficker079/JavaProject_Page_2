package application;

import javafx.application.Platform;


import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import application.dao.CourseOfferingDAO;

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
    
    @FXML private Label totalStudentsLabel;
    @FXML private Label totalTeachersLabel;
    @FXML private Label totalCoursesLabel;
    @FXML private Label todayAttendanceLabel;
    @FXML private Label todayPresentSubLabel;

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
        loadMetricCardsFromDb();
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
            	ViewNavigationHistory.navigateForward(contentArea, createCourseView);
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
            	ViewNavigationHistory.navigateForward(contentArea, createView);
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

    private VBox createClassCard(ClassesController.ClassItem item) {
        return ClassCardFactory.createCard(item, true, this::handleViewClass);
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
            	ViewNavigationHistory.navigateForward(contentArea, detailView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void loadMetricCardsFromDb() {
        CourseOfferingDAO dao = new CourseOfferingDAO();
        CourseOfferingDAO.TopLevelMetrics m = dao.getTopLevelMetrics();

        if (totalStudentsLabel != null) totalStudentsLabel.setText(String.valueOf(m.totalStudents()));
        if (totalTeachersLabel != null) totalTeachersLabel.setText(String.valueOf(m.totalTeachers()));
        if (totalCoursesLabel != null) totalCoursesLabel.setText(String.valueOf(m.totalCourses()));

        if (todayAttendanceLabel != null) {
            todayAttendanceLabel.setText(m.todayTotalCount() == 0
                    ? "0.0%"
                    : String.format("%.1f%%", m.todayAttendancePct()));
        }
        if (todayPresentSubLabel != null) {
            todayPresentSubLabel.setText(m.todayTotalCount() == 0
                    ? "No sessions today"
                    : "Present: " + m.todayPresentCount() + " / " + m.todayTotalCount());
        }
    }
    
}