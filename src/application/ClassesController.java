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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import application.dao.CourseOfferingDAO;
import application.dao.DatabaseConnector;
import application.dao.DepartmentDAO;

public class ClassesController {

    @FXML
    private Label profileNameLabel;
    @FXML
    private VBox rootPane;
    @FXML
    private ComboBox<String> academicYearCombo;
    @FXML
    private ComboBox<String> semesterCombo;
    @FXML
    private TextField searchField;
    @FXML
    private Button resetFilterBtn;
    @FXML
    private Label myClassesCountLabel;
    @FXML
    private Label classListBadge;
    @FXML
    private Label classesDeptLabel;

    @FXML
    private GridPane cardsGrid;
    @FXML
    private Label paginationInfoLabel;
    @FXML
    private HBox paginationControlsBox;
    
    @FXML private Label totalStudentsLabel;
    @FXML private Label totalTeachersLabel;
    @FXML private Label totalCoursesLabel;
    @FXML private Label todayAttendanceLabel;
    @FXML private Label todayPresentSubLabel;

    private static final int ITEMS_PER_PAGE = 6;
    private int currentPage = 1;

    // Shared observable list so Admin, Lecturer Classes, and Lecturer Dashboard
    // stay synchronized
    public static final ObservableList<ClassItem> SHARED_CLASS_DATA = FXCollections.observableArrayList();
    private final ObservableList<ClassItem> masterClassData = SHARED_CLASS_DATA;
    private List<ClassItem> filteredClassData = new ArrayList<>();

    @FXML
    public void initialize() {
        if (classesDeptLabel != null) {
            classesDeptLabel.setText(UserSession.getFormattedDepartment());
        }
        if (profileNameLabel != null) {
            profileNameLabel.setText("Lecturer (" + UserSession.lecturerName + ")");
        }

        initDropdowns();
        initSampleClassData();

        // Live listener: updates Lecturer's class cards immediately when Admin creates
        // a new class
        SHARED_CLASS_DATA.addListener(
                (ListChangeListener<ClassItem>) change -> Platform.runLater(this::applyFilter));

        if (academicYearCombo != null) {
            academicYearCombo.valueProperty().addListener((obs, o, n) -> applyFilter());
        }
        if (semesterCombo != null) {
            semesterCombo.valueProperty().addListener((obs, o, n) -> applyFilter());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        }

        applyFilter();
        loadMetricCardsFromDb();
    }

    private void initDropdowns() {
        if (academicYearCombo != null) {
            academicYearCombo.setItems(FXCollections.observableArrayList(
                    "All Years", "First Year", "Second Year", "Third Year", "Fourth Year", "Fifth Year"));
            academicYearCombo.setValue("All Years");
        }

        if (semesterCombo != null) {
            semesterCombo.setItems(FXCollections.observableArrayList(
                    "All Semesters", "First Semester", "Second Semester"));
            semesterCombo.setValue("All Semesters");
        }
    }

    public static void initSampleClassData() {
        if (!SHARED_CLASS_DATA.isEmpty())
            return;
        SHARED_CLASS_DATA.clear();
        try (Connection conn = DatabaseConnector.getConnection()) {
            DepartmentDAO departmentDAO = new DepartmentDAO();
            int currentUserDeptId = departmentDAO.getDeptIdByUserId(UserSession.userId);
            CourseOfferingDAO offeringDAO = new CourseOfferingDAO();
            List<ClassItem> loadedClasses = offeringDAO.getActiveClassesForDepartment(currentUserDeptId);

            SHARED_CLASS_DATA.addAll(loadedClasses);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @FXML
    public void handleResetFilters(ActionEvent event) {
        if (academicYearCombo != null)
            academicYearCombo.setValue("All Years");
        if (semesterCombo != null)
            semesterCombo.setValue("All Semesters");
        if (searchField != null)
            searchField.clear();
        applyFilter();
    }

    private void applyFilter() {
        String activeDept = UserSession.departmentCode;
        String activeLecturer = UserSession.lecturerName;
        String selectedYear = academicYearCombo != null ? academicYearCombo.getValue() : "All Years";
        String selectedSem = semesterCombo != null ? semesterCombo.getValue() : "All Semesters";
        String keyword = (searchField == null || searchField.getText() == null) ? ""
                : searchField.getText().trim().toLowerCase();

        filteredClassData = masterClassData.stream().filter(c -> {
            String cleanClassLecturer = c.lecturer.replace(" (Assigned)", "").trim();
            boolean hasLecturer = activeLecturer != null && !activeLecturer.isEmpty();
            boolean matchesLecturer = !hasLecturer || cleanClassLecturer.equalsIgnoreCase(activeLecturer.trim());
            boolean matchesDept = hasLecturer
                    || (activeDept == null || activeDept.isEmpty() || c.major.equalsIgnoreCase(activeDept));
            boolean matchesYear = (selectedYear == null || selectedYear.equals("All Years")
                    || c.year.equalsIgnoreCase(selectedYear));
            boolean matchesSem = (selectedSem == null || selectedSem.equals("All Semesters")
                    || c.semester.equalsIgnoreCase(selectedSem));
            boolean matchesSearch = keyword.isEmpty() ||
                    c.code.toLowerCase().contains(keyword) ||
                    c.title.toLowerCase().contains(keyword) ||
                    c.department.toLowerCase().contains(keyword) ||
                    c.className.toLowerCase().contains(keyword);

            return matchesDept && matchesLecturer && matchesYear && matchesSem && matchesSearch;
        }).collect(Collectors.toList());

        currentPage = 1;
        renderCurrentPage();
    }

    private void renderCurrentPage() {
        cardsGrid.getChildren().clear();

        int totalItems = filteredClassData.size();
        int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        if (totalPages == 0)
            totalPages = 1;
        if (currentPage > totalPages)
            currentPage = totalPages;

        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);

        for (int i = startIndex; i < endIndex; i++) {
            int relativeIndex = i - startIndex;
            int col = relativeIndex % 3;
            int row = relativeIndex / 3;

            ClassItem item = filteredClassData.get(i);
            VBox card = createClassCard(item);
            cardsGrid.add(card, col, row);
        }

        if (totalItems == 0) {
            paginationInfoLabel.setText("Showing 0 - 0 of 0 classes");
        } else {
            paginationInfoLabel
                    .setText("Showing " + (startIndex + 1) + " - " + endIndex + " of " + totalItems + " classes");
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

    private VBox createClassCard(ClassItem item) {
        return ClassCardFactory.createCard(item, false, this::handleViewClass);
    }

    @FXML
    private void handleViewClass(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        Object userData = clickedButton.getUserData();

        int offeringId = -1;
        String fallbackKey = null;

        if (userData instanceof Integer id) {
            offeringId = id;
        } else if (userData instanceof ClassesController.ClassItem item) {
            offeringId = item.getOfferingId();
            fallbackKey = item.getOfferingKey();
        } else if (userData != null) {
            fallbackKey = userData.toString();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ClassDetailView.fxml"));
            Parent detailView = loader.load();

            ClassDetailController detailController = loader.getController();
            if (detailController != null) {
                detailController.initClassData(offeringId, fallbackKey);
            }

            StackPane contentArea = (StackPane) clickedButton.getScene().lookup("#contentArea");
            if (contentArea != null) {
            	ViewNavigationHistory.navigateForward(contentArea, detailView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ClassItem {
        int offeringId;
        String code;
        String title;
        String major;
        String year;
        String semester;
        String department;
        String className;
        String studentCount;
        String schedule;
        String lecturer;
        String badgeColor;
        String icon;

        public ClassItem(int offeringId, String code, String title, String major, String year, String semester,
                String className, String studentCount, String schedule, String lecturer,
                String badgeColor, String icon) {
            this.offeringId = offeringId;
            this.code = code;
            this.title = title;
            this.major = major;
            this.year = year;
            this.semester = semester;
            this.department = (major != null && major.equalsIgnoreCase("CEIT"))
                    ? "Computer Engineering & IT"
                    : major + " Engineering";
            this.className = className;
            this.studentCount = studentCount;
            this.schedule = schedule;
            this.lecturer = lecturer;
            this.badgeColor = badgeColor;
            this.icon = icon;
        }

        public int getOfferingId() {
            return offeringId;
        }

        public String getOfferingKey() {
            return ClassRosterStore.buildKey(code, major, year, semester);
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