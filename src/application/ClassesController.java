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

public class ClassesController {

    @FXML private Label profileNameLabel;
    @FXML private VBox rootPane;
    @FXML private ComboBox<String> academicYearCombo;
    @FXML private ComboBox<String> semesterCombo;
    @FXML private TextField searchField;
    @FXML private Button resetFilterBtn;
    @FXML private Label myClassesCountLabel;
    @FXML private Label classListBadge;
    @FXML private Label classesDeptLabel;

    @FXML private GridPane cardsGrid;
    @FXML private Label paginationInfoLabel;
    @FXML private HBox paginationControlsBox;

    private static final int ITEMS_PER_PAGE = 6;
    private int currentPage = 1;

    // Shared observable list so Admin, Lecturer Classes, and Lecturer Dashboard stay synchronized
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

        // Live listener: updates Lecturer's class cards immediately when Admin creates a new class
        SHARED_CLASS_DATA.addListener(
            (ListChangeListener<ClassItem>) change -> Platform.runLater(this::applyFilter)
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

        applyFilter();
    }

    private void initDropdowns() {
        if (academicYearCombo != null) {
            academicYearCombo.setItems(FXCollections.observableArrayList(
                "All Years", "First Year", "Second Year", "Third Year", "Fourth Year", "Fifth Year"
            ));
            academicYearCombo.setValue("All Years");
        }

        if (semesterCombo != null) {
            semesterCombo.setItems(FXCollections.observableArrayList(
                "All Semesters", "First Semester", "Second Semester"
            ));
            semesterCombo.setValue("All Semesters");
        }
    }

    public static void initSampleClassData() {
        if (!SHARED_CLASS_DATA.isEmpty()) return;

        // Page 1: CEIT Core Courses (Dr. Hla Myint)
        SHARED_CLASS_DATA.add(new ClassItem("IT201", "Data Structures and Algorithms", "CEIT", "Second Year", "First Semester", "Year 2 - Class 2A", "56 students", "Mon, Wed 12:00 - 15:00", "Dr. Hla Myint", "#2563eb", "</>"));
        SHARED_CLASS_DATA.add(new ClassItem("IT202", "Database Systems", "CEIT", "Second Year", "First Semester", "Year 2 - Class 3A", "52 students", "Tue, Thu 10:00 - 12:00", "Dr. Hla Myint", "#7c3aed", "💾"));
        SHARED_CLASS_DATA.add(new ClassItem("IT203", "Computer Networks", "CEIT", "Second Year", "First Semester", "Year 2 - Class 2A", "48 students", "Mon, Wed 12:00 - 15:00", "Dr. Hla Myint", "#0284c7", "🌐"));
        SHARED_CLASS_DATA.add(new ClassItem("IT204", "Web Technologies", "CEIT", "Second Year", "First Semester", "Year 2 - Class 2A", "48 students", "Tue, Thu 13:00 - 15:00", "Dr. Hla Myint", "#9333ea", "🖥"));
        SHARED_CLASS_DATA.add(new ClassItem("IT205", "Operating Systems", "CEIT", "Second Year", "First Semester", "Year 2 - Class 3A", "60 students", "Fri 09:00 - 12:00", "Dr. Hla Myint", "#dc2626", "⚙"));
        SHARED_CLASS_DATA.add(new ClassItem("IT206", "Software Engineering", "CEIT", "Second Year", "First Semester", "Year 2 - Class 3A", "48 students", "Fri 12:00 - 16:00", "Dr. Hla Myint", "#16a34a", "<*>"));

        // Page 2: YTU CEIT Upper-Year & Second Semester Courses (Dr. Hla Myint)
        SHARED_CLASS_DATA.add(new ClassItem("CEIT2104", "Object-Oriented Programming", "CEIT", "Second Year", "Second Semester", "Year 2 - Class 2A", "54 students", "Mon, Thu 09:00 - 11:00", "Dr. Hla Myint", "#2563eb", "☕"));
        SHARED_CLASS_DATA.add(new ClassItem("CEIT3101", "Computer Architecture & Organization", "CEIT", "Third Year", "First Semester", "Year 3 - Class 1A", "50 students", "Tue, Thu 09:00 - 11:00", "Dr. Hla Myint", "#ea580c", "🔌"));
        SHARED_CLASS_DATA.add(new ClassItem("CEIT3202", "Microprocessors & Embedded Systems", "CEIT", "Third Year", "Second Semester", "Year 3 - Class 1A", "46 students", "Mon, Wed 13:00 - 15:00", "Dr. Hla Myint", "#0891b2", "📟"));
        SHARED_CLASS_DATA.add(new ClassItem("CEIT4103", "Artificial Intelligence & Machine Learning", "CEIT", "Fourth Year", "First Semester", "Year 4 - Class 1A", "44 students", "Wed, Fri 10:00 - 12:00", "Dr. Hla Myint", "#059669", "🧠"));
        SHARED_CLASS_DATA.add(new ClassItem("CEIT4205", "Cybersecurity & Network Defense", "CEIT", "Fourth Year", "Second Semester", "Year 4 - Class 1A", "42 students", "Tue, Fri 13:00 - 15:00", "Dr. Hla Myint", "#4f46e5", "🔒"));
        SHARED_CLASS_DATA.add(new ClassItem("CEIT5101", "Cloud Computing & Distributed Systems", "CEIT", "Fifth Year", "First Semester", "Year 5 - Class 1A", "38 students", "Mon, Thu 13:00 - 15:00", "Dr. Hla Myint", "#d97706", "☁"));

        // Seeded Teachers from seed.sql (Sadayo Kawakami & Sojiro Sakura)
        SHARED_CLASS_DATA.add(new ClassItem("CEIT-3101", "Advanced Database Systems", "CEIT", "Third Year", "First Semester", "Year 3 - Class 1A", "45 students", "Mon, Wed 08:00 - 09:50", "Sadayo Kawakami", "#7c3aed", "💾"));
        SHARED_CLASS_DATA.add(new ClassItem("CEIT-3102", "Object-Oriented Programming with Java", "CEIT", "Third Year", "First Semester", "Year 3 - Class 1A", "45 students", "Tue, Thu 09:00 - 10:50", "Sojiro Sakura", "#2563eb", "☕"));

        // Other YTU Departments
        SHARED_CLASS_DATA.add(new ClassItem("C301", "Structural Mechanics", "C", "Third Year", "First Semester", "Year 3 - Class 1A", "45 students", "Mon, Wed 09:00 - 11:00", "Dr. Aung Naing", "#ea580c", "🏗"));
        SHARED_CLASS_DATA.add(new ClassItem("Mech201", "Thermodynamics", "Mech", "Second Year", "First Semester", "Year 2 - Class 1B", "50 students", "Tue, Thu 08:30 - 10:30", "Dr. Su Su", "#0891b2", "🔥"));
        SHARED_CLASS_DATA.add(new ClassItem("EC401", "Digital Signal Processing", "EC", "Fourth Year", "Second Semester", "Year 4 - Class 1A", "40 students", "Mon, Fri 13:00 - 15:00", "Dr. Tin Maung", "#4f46e5", "📡"));
        SHARED_CLASS_DATA.add(new ClassItem("EP302", "Power Transmission Systems", "EP", "Third Year", "Second Semester", "Year 3 - Class 2B", "54 students", "Wed, Thu 10:00 - 12:00", "Prof. Kyaw Zin", "#d97706", "⚡"));
        SHARED_CLASS_DATA.add(new ClassItem("McE202", "Robotics & Kinematics", "McE", "Second Year", "First Semester", "Year 2 - Class 1A", "38 students", "Tue, Fri 14:00 - 16:00", "Dr. Myo Tint", "#059669", "🤖"));
        SHARED_CLASS_DATA.add(new ClassItem("ChE301", "Chemical Reaction Eng.", "ChE", "Third Year", "Second Semester", "Year 3 - Class 1A", "42 students", "Mon, Thu 11:00 - 13:00", "Dr. Khin Marlar", "#db2777", "🧪"));
    }

    @FXML
    public void handleResetFilters(ActionEvent event) {
        if (academicYearCombo != null) academicYearCombo.setValue("All Years");
        if (semesterCombo != null) semesterCombo.setValue("All Semesters");
        if (searchField != null) searchField.clear();
        applyFilter();
    }

    private void applyFilter() {
        String activeDept = UserSession.departmentCode;
        String activeLecturer = UserSession.lecturerName;
        String selectedYear = academicYearCombo != null ? academicYearCombo.getValue() : "All Years";
        String selectedSem = semesterCombo != null ? semesterCombo.getValue() : "All Semesters";
        String keyword = (searchField == null || searchField.getText() == null) ? "" : searchField.getText().trim().toLowerCase();

        filteredClassData = masterClassData.stream().filter(c -> {
            String cleanClassLecturer = c.lecturer.replace(" (Assigned)", "").trim();
            boolean hasLecturer = activeLecturer != null && !activeLecturer.isEmpty();
            boolean matchesLecturer = !hasLecturer || cleanClassLecturer.equalsIgnoreCase(activeLecturer.trim());
            boolean matchesDept = hasLecturer || (activeDept == null || activeDept.isEmpty() || c.major.equalsIgnoreCase(activeDept));
            boolean matchesYear = (selectedYear == null || selectedYear.equals("All Years") || c.year.equalsIgnoreCase(selectedYear));
            boolean matchesSem = (selectedSem == null || selectedSem.equals("All Semesters") || c.semester.equalsIgnoreCase(selectedSem));
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
        return ClassCardFactory.createCard(item, false, this::handleViewClass, null);
    }

    @FXML
    private void handleViewClass(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String offeringKey = (String) clickedButton.getUserData();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ClassDetailView.fxml"));
            Parent detailView = loader.load();

            ClassDetailController detailController = loader.getController();
            if (detailController != null) {
                detailController.initClassData(offeringKey);
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

        public String getOfferingKey() {
            return ClassRosterStore.buildKey(code, major, year, semester);
        }
    }
}