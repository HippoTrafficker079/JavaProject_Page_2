package application;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
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

public class ExamsController {

    @FXML private VBox rootPane;
    @FXML private Label profileNameLabel;
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

    // Preserved for backward compatibility with any external references
    public static ObservableList<Exam> examMasterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (examsDeptLabel != null) {
            examsDeptLabel.setText(UserSession.getFormattedDepartment());
        }
        if (profileNameLabel != null) {
            profileNameLabel.setText("Lecturer (" + UserSession.lecturerName + ")");
        }

        initDropdowns();
        ClassesController.initSampleClassData();
        AdminExamsController.autoUpdateExpiredStatuses();

        ClassesController.SHARED_CLASS_DATA.addListener(
                (ListChangeListener<ClassesController.ClassItem>) change -> Platform.runLater(() -> {
                    applyFilter();
                    updateMetricCards();
                }));

        AdminExamsController.deptExamMasterData.addListener(
                (ListChangeListener<AdminExamsController.AdminExamRecord>) change -> Platform.runLater(this::updateMetricCards));

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
        updateMetricCards();
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

    private List<ClassesController.ClassItem> getLecturerClasses() {
        String activeDept = UserSession.departmentCode;
        String activeLecturer = UserSession.lecturerName;

        return masterClassData.stream().filter(c -> {
            String cleanClassLecturer = c.lecturer != null ? c.lecturer.replace(" (Assigned)", "").trim() : "";
            boolean hasLecturer = activeLecturer != null && !activeLecturer.isBlank();
            boolean matchesLecturer = !hasLecturer || cleanClassLecturer.equalsIgnoreCase(activeLecturer.trim());
            boolean matchesDept = hasLecturer
                    || (activeDept == null || activeDept.isBlank() || c.major.equalsIgnoreCase(activeDept));
            return matchesDept && matchesLecturer;
        }).collect(Collectors.toList());
    }

    private void updateMetricCards() {
        AdminExamsController.autoUpdateExpiredStatuses();
        List<ClassesController.ClassItem> myClasses = getLecturerClasses();

        int drafts = 0;
        int finished = 0;
        int total = 0;

        for (AdminExamsController.AdminExamRecord ex : AdminExamsController.deptExamMasterData) {
            boolean belongsToMe = myClasses.stream()
                    .anyMatch(cls -> AdminExamsController.examBelongsToClass(ex, cls));
            if (!belongsToMe) continue;

            total++;
            if ("Draft".equalsIgnoreCase(ex.getStatus())) drafts++;
            else if ("Finished".equalsIgnoreCase(ex.getStatus())) finished++;
        }

        if (draftsCountLabel != null) draftsCountLabel.setText(String.valueOf(drafts));
        if (finishedCountLabel != null) finishedCountLabel.setText(String.valueOf(finished));
        if (totalExamsCountLabel != null) totalExamsCountLabel.setText(String.valueOf(total));
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

        filteredClassData = getLecturerClasses().stream().filter(c -> {
            boolean matchesYear = (selectedYear == null || selectedYear.equals("All Years")
                    || c.year.equalsIgnoreCase(selectedYear));
            boolean matchesSem = (selectedSem == null || selectedSem.equals("All Semesters")
                    || c.semester.equalsIgnoreCase(selectedSem));
            boolean matchesSearch = keyword.isEmpty()
                    || c.code.toLowerCase().contains(keyword)
                    || c.title.toLowerCase().contains(keyword)
                    || c.department.toLowerCase().contains(keyword)
                    || c.major.toLowerCase().contains(keyword);
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
            VBox card = ClassCardFactory.createExamCard(item, false, this::handleOpenClassExamDetail);
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
                controller.initData(selectedClass, false);
            }

            StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
            if (contentArea != null) {
                ViewNavigationHistory.navigateForward(contentArea, detailView);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load ClassExamDetailView.fxml:\n" + e.getMessage());
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }
}