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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

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
        return ClassCardFactory.createCard(item, true, this::handleViewClass, this::handleEditClass);
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
    private void handleEditClass(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String offeringKey = (String) clickedButton.getUserData();

        ClassesController.ClassItem item = masterClassData.stream()
                .filter(c -> c.getOfferingKey().equalsIgnoreCase(offeringKey))
                .findFirst()
                .orElse(null);

        if (item == null) return;

        Map<String, String> colorPalette = new LinkedHashMap<>();
        colorPalette.put("Royal Blue (#2563eb)", "#2563eb");
        colorPalette.put("Violet (#7c3aed)", "#7c3aed");
        colorPalette.put("Sky Blue (#0284c7)", "#0284c7");
        colorPalette.put("Purple (#9333ea)", "#9333ea");
        colorPalette.put("Crimson (#dc2626)", "#dc2626");
        colorPalette.put("Emerald (#16a34a)", "#16a34a");
        colorPalette.put("Orange (#ea580c)", "#ea580c");
        colorPalette.put("Teal (#0891b2)", "#0891b2");
        colorPalette.put("Indigo (#4f46e5)", "#4f46e5");
        colorPalette.put("Amber (#d97706)", "#d97706");
        colorPalette.put("Rose (#db2777)", "#db2777");

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Class — " + item.code);
        dialog.setHeaderText(null);

        ButtonType saveBtnType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        // Parse existing enrolled & max counts from "X / Y students"
        int enrolledCount = 0;
        int currentMax = 60;
        if (item.studentCount != null && item.studentCount.contains("/")) {
            String[] parts = item.studentCount.replaceAll("[^0-9/]", "").split("/");
            if (parts.length == 2) {
                try { enrolledCount = Integer.parseInt(parts[0]); } catch (NumberFormatException ignored) {}
                try { currentMax = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
            }
        }

        // Controls
        TextField codeField = new TextField(item.code);
        codeField.setDisable(true);

        TextField titleField = new TextField(item.title);
        titleField.setPromptText("Class / Course Title");

        ComboBox<String> yearBox = new ComboBox<>(FXCollections.observableArrayList(
                "First Year", "Second Year", "Third Year", "Fourth Year", "Fifth Year"));
        yearBox.setValue(item.year);
        yearBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> semBox = new ComboBox<>(FXCollections.observableArrayList(
                "First Semester", "Second Semester"));
        semBox.setValue(item.semester);
        semBox.setMaxWidth(Double.MAX_VALUE);

        TextField maxStdField = new TextField(String.valueOf(currentMax));
        maxStdField.setPromptText("e.g. 60");

        ComboBox<String> symbolBox = new ComboBox<>(FXCollections.observableArrayList(
                "</>", "💾", "🌐", "🖥", "⚙", "<*>", "📘", "☕", "🔌", "🧠", "🔒", "☁", "🏗", "🔥", "📡", "⚡", "🤖", "🧪"));
        symbolBox.setEditable(true);
        symbolBox.setValue(item.icon != null ? item.icon : "</>");
        symbolBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> colorBox = new ComboBox<>(FXCollections.observableArrayList(colorPalette.keySet()));
        String matchedColorLabel = colorPalette.entrySet().stream()
                .filter(e -> e.getValue().equalsIgnoreCase(item.badgeColor))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("Royal Blue (#2563eb)");
        colorBox.setValue(matchedColorLabel);
        colorBox.setMaxWidth(Double.MAX_VALUE);

        // Live Badge Preview
        StackPane previewPane = new StackPane();
        previewPane.setMinSize(44, 44);
        previewPane.setMaxSize(44, 44);
        Label previewLbl = new Label(symbolBox.getValue());
        previewLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        previewPane.getChildren().add(previewLbl);

        Runnable refreshPreview = () -> {
            String sym = symbolBox.getEditor().getText() != null && !symbolBox.getEditor().getText().trim().isEmpty()
                    ? symbolBox.getEditor().getText().trim()
                    : (symbolBox.getValue() != null ? symbolBox.getValue().trim() : "📘");
            String hex = colorPalette.getOrDefault(colorBox.getValue(), "#2563eb");
            previewLbl.setText(sym);
            previewPane.setStyle("-fx-background-color: " + hex + "; -fx-background-radius: 10px;");
        };
        refreshPreview.run();

        symbolBox.valueProperty().addListener((obs, o, n) -> refreshPreview.run());
        symbolBox.getEditor().textProperty().addListener((obs, o, n) -> refreshPreview.run());
        colorBox.valueProperty().addListener((obs, o, n) -> refreshPreview.run());

        HBox previewRow = new HBox(12, previewPane, new Label("Live Card Badge Preview"));
        previewRow.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16, 20, 10, 20));
        grid.setPrefWidth(430);

        grid.add(new Label("Course Code:"), 0, 0);    grid.add(codeField, 1, 0);
        grid.add(new Label("Class Name:"), 0, 1);     grid.add(titleField, 1, 1);
        grid.add(new Label("Academic Year:"), 0, 2);  grid.add(yearBox, 1, 2);
        grid.add(new Label("Semester:"), 0, 3);       grid.add(semBox, 1, 3);
        grid.add(new Label("Max Students:"), 0, 4);   grid.add(maxStdField, 1, 4);
        grid.add(new Label("Card Symbol:"), 0, 5);    grid.add(symbolBox, 1, 5);
        grid.add(new Label("Badge Color:"), 0, 6);    grid.add(colorBox, 1, 6);
        grid.add(previewRow, 1, 7);

        dialog.getDialogPane().setContent(grid);

        final int finalEnrolled = enrolledCount;
        dialog.showAndWait().ifPresent(result -> {
            if (result != saveBtnType) return;

            String newTitle = titleField.getText() != null ? titleField.getText().trim() : "";
            String newYear = yearBox.getValue();
            String newSem = semBox.getValue();
            String newSymbol = symbolBox.getEditor().getText() != null && !symbolBox.getEditor().getText().trim().isEmpty()
                    ? symbolBox.getEditor().getText().trim()
                    : (symbolBox.getValue() != null ? symbolBox.getValue().trim() : "</>");
            String newColor = colorPalette.getOrDefault(colorBox.getValue(), "#2563eb");

            if (newTitle.isEmpty()) {
                Alert warn = new Alert(Alert.AlertType.WARNING, "Class Name cannot be empty.");
                warn.setHeaderText(null);
                warn.showAndWait();
                return;
            }

            int newMax;
            try {
                newMax = Integer.parseInt(maxStdField.getText().trim());
                if (newMax <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                Alert warn = new Alert(Alert.AlertType.WARNING, "Max Students must be a positive number.");
                warn.setHeaderText(null);
                warn.showAndWait();
                return;
            }

            try {
                CourseOfferingDAO dao = new CourseOfferingDAO();
                dao.updateOfferingMetadata(
                        item.code, item.year, item.semester,
                        newTitle, newYear, newSem, newMax, newSymbol, newColor
                );
            } catch (SQLException e) {
                e.printStackTrace();
                Alert err = new Alert(Alert.AlertType.ERROR, "Failed to update class in database:\n" + e.getMessage());
                err.setHeaderText(null);
                err.showAndWait();
                return;
            }

            // Update card in memory (and sync title across any offerings sharing the same course_code)
            item.title = newTitle;
            item.year = newYear;
            item.semester = newSem;
            item.studentCount = finalEnrolled + " / " + newMax + " students";
            item.icon = newSymbol;
            item.badgeColor = newColor;

            for (ClassesController.ClassItem other : masterClassData) {
                if (other.code.equalsIgnoreCase(item.code)) {
                    other.title = newTitle;
                }
            }

            CourseCatalogStore.reload();
            applyFilter();
        });
    }
}