package application;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AdminCreateCourseController {

    @FXML private VBox rootPane;
    @FXML private TextField courseCodeField;
    @FXML private TextField courseNameField;
    @FXML private ComboBox<String> deptCombo;
    @FXML private ComboBox<String> courseTypeCombo;
    @FXML private TextField creditField;
    @FXML private CheckBox isActiveCheck;

    @FXML private ComboBox<String> prerequisiteCombo;
    @FXML private FlowPane prerequisitesPane;

    @FXML private TableView<CourseCatalogStore.CourseItem> courseTable;
    @FXML private TableColumn<CourseCatalogStore.CourseItem, String> colCode;
    @FXML private TableColumn<CourseCatalogStore.CourseItem, String> colName;
    @FXML private TableColumn<CourseCatalogStore.CourseItem, String> colDept;
    @FXML private TableColumn<CourseCatalogStore.CourseItem, String> colType;
    @FXML private TableColumn<CourseCatalogStore.CourseItem, Double> colCredit;
    @FXML private TableColumn<CourseCatalogStore.CourseItem, String> colStatus;
    @FXML private TableColumn<CourseCatalogStore.CourseItem, String> colPrereqs;

    private final List<String> selectedPrerequisites = new ArrayList<>();
    private FilteredList<String> filteredPrereqItems;
    private boolean updatingPrereqText = false;

    @FXML
    public void initialize() {
        if (deptCombo != null) {
            deptCombo.setItems(FXCollections.observableArrayList(
                "CEIT", "C", "Mech", "EP", "EC", "McE", "ChE", "Tex", "Mn", "PE", "Arch", "Tele", "Met", "FE"
            ));
            deptCombo.setValue(UserSession.departmentCode != null ? UserSession.departmentCode : "CEIT");
        }

        if (courseTypeCombo != null) {
            courseTypeCombo.setItems(FXCollections.observableArrayList("Bachelor", "Master", "PHD"));
            courseTypeCombo.setValue("Bachelor");
        }

        setupSearchablePrerequisiteCombo();
        setupCatalogTable();
        renderPrerequisiteChips();
    }

    private void setupSearchablePrerequisiteCombo() {
        if (prerequisiteCombo == null) return;

        filteredPrereqItems = new FilteredList<>(
            FXCollections.observableArrayList(CourseCatalogStore.getAllCourseLabels()),
            s -> true
        );
        prerequisiteCombo.setItems(filteredPrereqItems);

        prerequisiteCombo.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (updatingPrereqText) return;
            String filter = newVal == null ? "" : newVal.trim().toLowerCase();
            Platform.runLater(() -> {
                filteredPrereqItems.setPredicate(item ->
                    filter.isEmpty() || item.toLowerCase().contains(filter)
                );
                if (!filter.isEmpty() && !filteredPrereqItems.isEmpty() && prerequisiteCombo.isFocused()) {
                    prerequisiteCombo.show();
                }
            });
        });

        prerequisiteCombo.setOnAction(e -> {
            String sel = prerequisiteCombo.getSelectionModel().getSelectedItem();
            if (sel != null) {
                updatingPrereqText = true;
                prerequisiteCombo.getEditor().setText(sel);
                updatingPrereqText = false;
            }
        });
    }

    private void refreshPrerequisiteChoices() {
        if (prerequisiteCombo == null) return;
        filteredPrereqItems = new FilteredList<>(
            FXCollections.observableArrayList(CourseCatalogStore.getAllCourseLabels()),
            s -> true
        );
        prerequisiteCombo.setItems(filteredPrereqItems);
    }

    private void setupCatalogTable() {
        if (courseTable == null) return;

        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().courseCode));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().courseName));
        colDept.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().deptCode));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().courseType));
        colCredit.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().credit).asObject());
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive ? "Active" : "Inactive"));
        colPrereqs.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrerequisitesDisplay()));

        courseTable.setItems(CourseCatalogStore.COURSES);
    }

    @FXML
    public void handleAddPrerequisite() {
        String input = prerequisiteCombo.getEditor().getText();
        if (input == null || input.isBlank()) return;

        CourseCatalogStore.CourseItem matched = CourseCatalogStore.findByLabelOrCode(input);
        if (matched == null) {
            showAlert(Alert.AlertType.WARNING, "Course Not Found",
                    "Please select an existing course from the searchable Prerequisite list.");
            return;
        }

        String currentCode = courseCodeField.getText() != null ? courseCodeField.getText().trim() : "";
        if (!currentCode.isEmpty() && matched.courseCode.equalsIgnoreCase(currentCode)) {
            showAlert(Alert.AlertType.WARNING, "Invalid Prerequisite",
                    "A course cannot be a prerequisite of itself.");
            return;
        }

        if (!selectedPrerequisites.contains(matched.courseCode)) {
            selectedPrerequisites.add(matched.courseCode);
            renderPrerequisiteChips();
        }

        updatingPrereqText = true;
        prerequisiteCombo.getEditor().clear();
        prerequisiteCombo.getSelectionModel().clearSelection();
        updatingPrereqText = false;
        if (filteredPrereqItems != null) filteredPrereqItems.setPredicate(s -> true);
    }

    private void renderPrerequisiteChips() {
        if (prerequisitesPane == null) return;
        prerequisitesPane.getChildren().clear();

        if (selectedPrerequisites.isEmpty()) {
            Label emptyLbl = new Label("No prerequisites added (open enrollment).");
            emptyLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11.5px;");
            prerequisitesPane.getChildren().add(emptyLbl);
            return;
        }

        for (String prereqCode : selectedPrerequisites) {
            CourseCatalogStore.CourseItem item = CourseCatalogStore.findByLabelOrCode(prereqCode);
            String labelText = item != null ? item.getDisplayLabel() : prereqCode;

            HBox chip = new HBox(6.0);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.setStyle(
                "-fx-background-color: #dbeafe; -fx-border-color: #93c5fd; " +
                "-fx-background-radius: 14px; -fx-border-radius: 14px; -fx-padding: 4px 10px;"
            );

            Label lbl = new Label(labelText);
            lbl.setStyle("-fx-text-fill: #1e40af; -fx-font-weight: bold; -fx-font-size: 11.5px;");

            Label removeBtn = new Label("✕");
            removeBtn.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
            removeBtn.setOnMouseClicked(e -> {
                selectedPrerequisites.remove(prereqCode);
                renderPrerequisiteChips();
            });

            chip.getChildren().addAll(lbl, removeBtn);
            prerequisitesPane.getChildren().add(chip);
        }
    }

    @FXML
    public void handleSaveCourse() {
        String code = courseCodeField.getText() != null ? courseCodeField.getText().trim().toUpperCase() : "";
        String name = courseNameField.getText() != null ? courseNameField.getText().trim() : "";
        String dept = deptCombo.getValue() != null ? deptCombo.getValue() : "CEIT";
        String type = courseTypeCombo.getValue() != null ? courseTypeCombo.getValue() : "Bachelor";
        String creditStr = creditField.getText() != null ? creditField.getText().trim() : "3.0";
        boolean active = isActiveCheck == null || isActiveCheck.isSelected();

        if (code.isEmpty() || name.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Required Fields",
                    "Please enter both Course Code and Course Name.");
            return;
        }

        if (CourseCatalogStore.existsByCode(code)) {
            showAlert(Alert.AlertType.WARNING, "Duplicate Course Code",
                    "A course with code \"" + code + "\" already exists.");
            return;
        }

        double credit;
        try {
            credit = Double.parseDouble(creditStr);
            if (credit <= 0 || credit > 99.9) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Invalid Credit",
                    "Please enter a valid numeric credit value (e.g. 3.0 or 3.5).");
            return;
        }

        CourseCatalogStore.addCourse(code, name, dept, type, credit, active, selectedPrerequisites);
        refreshPrerequisiteChoices();
        if (courseTable != null) courseTable.refresh();

        showAlert(Alert.AlertType.INFORMATION, "Course Created",
                code + " - " + name + " has been added to the catalog and is now available in Create New Class!");
        handleClearForm();
    }

    @FXML
    public void handleClearForm() {
        if (courseCodeField != null) courseCodeField.clear();
        if (courseNameField != null) courseNameField.clear();
        if (creditField != null) creditField.setText("3.0");
        if (courseTypeCombo != null) courseTypeCombo.setValue("Bachelor");
        if (isActiveCheck != null) isActiveCheck.setSelected(true);
        selectedPrerequisites.clear();
        renderPrerequisiteChips();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AdminClassesView.fxml"));
            Parent classesView = loader.load();
            StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(classesView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}