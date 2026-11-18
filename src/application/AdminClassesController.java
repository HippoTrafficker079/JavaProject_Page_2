package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminClassesController {

    @FXML private VBox rootPane;
    @FXML private ComboBox<String> academicYearCombo;
    @FXML private ComboBox<String> semesterCombo;
    @FXML private TextField searchField;
    @FXML private Button resetFilterBtn;
    @FXML private Button createClassBtn;
    @FXML private Label myClassesCountLabel;
    @FXML private Label classListBadge;

    @FXML private GridPane cardsGrid;
    @FXML private Label paginationInfoLabel;
    @FXML private HBox paginationControlsBox;
    @FXML private Label adminClassesDeptLabel;
    
    private static final int ITEMS_PER_PAGE = 6;
    private int currentPage = 1;

    private final ObservableList<ClassItem> masterClassData = FXCollections.observableArrayList();
    private List<ClassItem> filteredClassData = new ArrayList<>();

    @FXML
    public void initialize() {
        initDropdowns();
        initSampleClassData();

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
    }

    private void initDropdowns() {
        if (academicYearCombo != null) {
            academicYearCombo.setItems(FXCollections.observableArrayList("All Years", "First Year", "Second Year", "Third Year", "Fourth Year", "Fifth Year"));
            academicYearCombo.getSelectionModel().selectFirst();
        }
        if (semesterCombo != null) {
            semesterCombo.setItems(FXCollections.observableArrayList("All Semesters", "First Semester", "Second Semester"));
            semesterCombo.getSelectionModel().selectFirst();
        }
    }

    private void initSampleClassData() {
        masterClassData.add(new ClassItem("IT201", "Data Structures and Algorithms", "CEIT", "Second Year", "First Semester", "Year 2 - Class 2A", "56 students", "Mon, Wed 12:00 - 15:00", "Dr. Hla Myint", "#2563eb", "</>"));
        masterClassData.add(new ClassItem("IT202", "Database Systems", "CEIT", "Second Year", "First Semester", "Year 2 - Class 3A", "52 students", "Tue, Thu 10:00 - 12:00", "Dr. Aung Naing", "#7c3aed", "💾"));
        masterClassData.add(new ClassItem("IT203", "Computer Networks", "CEIT", "Second Year", "First Semester", "Year 2 - Class 2A", "48 students", "Mon, Wed 12:00 - 15:00", "Dr. Su Su", "#0284c7", "🌐"));
        masterClassData.add(new ClassItem("IT204", "Web Technologies", "CEIT", "Second Year", "First Semester", "Year 2 - Class 2A", "48 students", "Tue, Thu 13:00 - 15:00", "Prof. Kyaw Zin", "#9333ea", "🖥"));
        masterClassData.add(new ClassItem("IT205", "Operating Systems", "CEIT", "Second Year", "First Semester", "Year 2 - Class 3A", "60 students", "Fri 09:00 - 12:00", "Dr. Hla Myint", "#dc2626", "⚙"));
        masterClassData.add(new ClassItem("IT206", "Software Engineering", "CEIT", "Second Year", "First Semester", "Year 2 - Class 3A", "48 students", "Fri 12:00 - 16:00", "Dr. Aung Naing", "#16a34a", "<*>"));

        masterClassData.add(new ClassItem("CE301", "Structural Mechanics", "CE", "Third Year", "First Semester", "Year 3 - Class 1A", "45 students", "Mon, Wed 09:00 - 11:00", "Dr. Tin Maung", "#ea580c", "🏗"));
        masterClassData.add(new ClassItem("ME201", "Thermodynamics", "ME", "Second Year", "First Semester", "Year 2 - Class 1B", "50 students", "Tue, Thu 08:30 - 10:30", "Dr. Myo Tint", "#0891b2", "🔥"));
        masterClassData.add(new ClassItem("EC401", "Digital Signal Processing", "EC", "Fourth Year", "Second Semester", "Year 4 - Class 1A", "40 students", "Mon, Fri 13:00 - 15:00", "Dr. Nilar Thein", "#4f46e5", "📡"));
        masterClassData.add(new ClassItem("EP302", "Power Transmission Systems", "EP", "Third Year", "Second Semester", "Year 3 - Class 2B", "54 students", "Wed, Thu 10:00 - 12:00", "Dr. Soe Win", "#d97706", "⚡"));
        masterClassData.add(new ClassItem("MC202", "Robotics & Kinematics", "MC", "Second Year", "First Semester", "Year 2 - Class 1A", "38 students", "Tue, Fri 14:00 - 16:00", "Dr. Zayar Tun", "#059669", "🤖"));
        masterClassData.add(new ClassItem("ChE301", "Chemical Reaction Eng.", "ChE", "Third Year", "Second Semester", "Year 3 - Class 1A", "42 students", "Mon, Thu 11:00 - 13:00", "Dr. Mar Mar", "#db2777", "🧪"));
    }

    @FXML
    public void handleResetFilters(ActionEvent event) {
        if (academicYearCombo != null) {
            academicYearCombo.setValue("All Years");
        }
        if (semesterCombo != null) {
            semesterCombo.setValue("All Semesters");
        }
        if (searchField != null) {
            searchField.clear();
        }
        applyFilter();
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

            ClassItem item = filteredClassData.get(i);
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

    private VBox createClassCard(ClassItem item) {
        VBox card = new VBox(10.0);
        card.getStyleClass().add("panel-card");

        HBox topHBox = new HBox(10.0);
        topHBox.setAlignment(Pos.CENTER_LEFT);

        StackPane iconPane = new StackPane();
        iconPane.setStyle("-fx-background-color: " + item.badgeColor + "; -fx-background-radius: 8px; -fx-padding: 8px; -fx-min-width: 36px; -fx-min-height: 36px;");
        Label iconLabel = new Label(item.icon);
        iconLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        iconPane.getChildren().add(iconLabel);

        VBox titleBox = new VBox();
        Label codeLabel = new Label(item.code);
        codeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label subLabel = new Label(item.title);
        subLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(codeLabel, subLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dotsLabel = new Label("⋮");
        dotsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;");

        topHBox.getChildren().addAll(iconPane, titleBox, spacer, dotsLabel);

        VBox detailsBox = new VBox(4.0);
        Label deptLabel = new Label("🏛  " + item.department);
        deptLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        Label classLabel = new Label("🎓  " + item.className);
        classLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        Label stdLabel = new Label("👥  " + item.studentCount);
        stdLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        Label scheduleLabel = new Label("🕒  " + item.schedule);
        scheduleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        Label lectLabel = new Label("👤  " + item.lecturer + " (Assigned)");
        lectLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        detailsBox.getChildren().addAll(deptLabel, classLabel, stdLabel, scheduleLabel, lectLabel);

        HBox btnHBox = new HBox(8.0);
        btnHBox.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-size: 11px; -fx-background-radius: 6px; -fx-padding: 5px 10px; -fx-cursor: hand;");

        Button viewBtn = new Button("Manage Class");
        viewBtn.getStyleClass().add("view-class-button");
        viewBtn.setUserData(item.code);
        viewBtn.setOnAction(this::handleViewClass);

        btnHBox.getChildren().addAll(editBtn, viewBtn);

        card.getChildren().addAll(topHBox, new Separator(), detailsBox, btnHBox);
        return card;
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

    public static class ClassItem {
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

        public ClassItem(String code, String title, String major, String year, String semester,
                         String className, String studentCount, String schedule, String lecturer,
                         String badgeColor, String icon) {
            this.code = code;
            this.title = title;
            this.major = major;
            this.year = year;
            this.semester = semester;
            this.department = major.equalsIgnoreCase("CEIT") ? "Computer Engineering & IT" : major + " Engineering";
            this.className = className;
            this.studentCount = studentCount;
            this.schedule = schedule;
            this.lecturer = lecturer;
            this.badgeColor = badgeColor;
            this.icon = icon;
        }
    }
}