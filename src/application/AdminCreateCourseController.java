package application;

import javafx.application.Platform;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import javafx.scene.control.ListView;
import javafx.scene.control.skin.ComboBoxListViewSkin;

public class AdminCreateCourseController {
	private final ObservableList<String> prereqBackingItems = FXCollections.observableArrayList();
	private boolean isUpdatingPrereq = false;
	
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

        prereqBackingItems.setAll(CourseCatalogStore.getAllCourseLabels());
        prerequisiteCombo.setItems(prereqBackingItems);

        TextField editor = prerequisiteCombo.getEditor();
     // When opening the dropdown after a selection (or when blank), show all courses instead of the old filtered list
        prerequisiteCombo.setOnShowing(e -> {
            if (isUpdatingPrereq) return;
            String text = editor.getText() != null ? editor.getText().trim() : "";
            boolean isSelectedCourse =
                    (prerequisiteCombo.getValue() != null && prerequisiteCombo.getValue().equals(text))
                    || CourseCatalogStore.findByLabelOrCode(text) != null;

            if (text.isEmpty() || isSelectedCourse) {
                isUpdatingPrereq = true;
                List<String> allCourses = CourseCatalogStore.getAllCourseLabels();
                if (prereqBackingItems.size() != allCourses.size()) {
                    prereqBackingItems.setAll(allCourses);
                }
                if (isSelectedCourse) {
                    prerequisiteCombo.getSelectionModel().select(text);
                    editor.setText(text);
                } else {
                    prerequisiteCombo.getSelectionModel().clearSelection();
                }
                isUpdatingPrereq = false;
            }
        });
        if (editor == null) return;

        // Backspace once on any selected course clears it straight to blank
        editor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.BACK_SPACE) {
                String text = editor.getText() != null ? editor.getText() : "";
                boolean isSelectedCourse =
                        (prerequisiteCombo.getValue() != null && prerequisiteCombo.getValue().equals(text))
                        || CourseCatalogStore.findByLabelOrCode(text) != null;

                if (!text.isEmpty() && isSelectedCourse) {
                    event.consume();
                    isUpdatingPrereq = true;
                    prerequisiteCombo.hide();
                    prerequisiteCombo.getSelectionModel().clearSelection();
                    prerequisiteCombo.setValue(null);
                    prereqBackingItems.setAll(CourseCatalogStore.getAllCourseLabels());
                    prerequisiteCombo.getSelectionModel().clearSelection();
                    editor.clear();
                    isUpdatingPrereq = false;

                    Platform.runLater(() -> {
                        if (prerequisiteCombo.isFocused() || editor.isFocused()) {
                            prerequisiteCombo.show();
                        }
                    });
                }
            }
        });

        // Live search while typing (mutates backing list without replacing it)
        editor.textProperty().addListener((obs, oldText, newText) -> {
            if (isUpdatingPrereq) return;

            String selected = prerequisiteCombo.getSelectionModel().getSelectedItem();
            if (selected != null && selected.equals(newText)) {
                return;
            }

            isUpdatingPrereq = true;
            String typed = newText == null ? "" : newText;
            String q = typed.trim().toLowerCase();

            List<String> matches = CourseCatalogStore.getAllCourseLabels().stream()
                    .filter(label -> q.isEmpty() || label.toLowerCase().contains(q))
                    .collect(java.util.stream.Collectors.toList());

            prerequisiteCombo.getSelectionModel().clearSelection();
            prereqBackingItems.setAll(matches);
            prerequisiteCombo.getSelectionModel().clearSelection();

            if (!typed.equals(editor.getText())) {
                editor.setText(typed);
                editor.positionCaret(typed.length());
            }
            isUpdatingPrereq = false;

            if (!matches.isEmpty() && !prerequisiteCombo.isShowing() && (prerequisiteCombo.isFocused() || editor.isFocused())) {
                prerequisiteCombo.show();
            }
        });
        lockComboPopupWidth(prerequisiteCombo);
    }

    private void refreshPrerequisiteChoices() {
        isUpdatingPrereq = true;
        prereqBackingItems.setAll(CourseCatalogStore.getAllCourseLabels());
        if (prerequisiteCombo != null) {
            prerequisiteCombo.getSelectionModel().clearSelection();
        }
        isUpdatingPrereq = false;
    }

    @FXML
    public void handleAddPrerequisite() {
        String input = prerequisiteCombo.getEditor() != null ? prerequisiteCombo.getEditor().getText() : "";
        if (input == null || input.isBlank()) {
            input = prerequisiteCombo.getValue();
        }
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

        isUpdatingPrereq = true;
        prerequisiteCombo.hide();
        prerequisiteCombo.getSelectionModel().clearSelection();
        prerequisiteCombo.setValue(null);
        prereqBackingItems.setAll(CourseCatalogStore.getAllCourseLabels());
        if (prerequisiteCombo.getEditor() != null) {
            prerequisiteCombo.getEditor().clear();
        }
        isUpdatingPrereq = false;
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
    private void lockComboPopupWidth(ComboBox<String> combo) {
        if (combo == null) return;

        Runnable bindWidth = () -> {
            if (combo.getSkin() instanceof ComboBoxListViewSkin<?> skin
                    && skin.getPopupContent() instanceof ListView<?> listView) {
                if (!listView.prefWidthProperty().isBound()) {
                    listView.minWidthProperty().bind(combo.widthProperty());
                    listView.prefWidthProperty().bind(combo.widthProperty());
                    listView.maxWidthProperty().bind(combo.widthProperty());
                }
            }
        };

        combo.skinProperty().addListener((obs, oldSkin, newSkin) -> bindWidth.run());
        combo.addEventHandler(ComboBox.ON_SHOWING, e -> bindWidth.run());
    }
}