package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class AdminClassDetailController {

    @FXML private VBox rootPane;
    @FXML private Label classNameLabel;
    @FXML private ComboBox<String> assignTeacherCombo;
    @FXML private TextField scheduleField;

    // Metric Labels
    @FXML private Label totalStudentsLabel;
    @FXML private Label activeStudentsLabel;
    @FXML private Label atRiskLabel;
    @FXML private Label inactiveLabel;

    // Filter Controls
    @FXML private ComboBox<String> rollNoCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField searchField;
    @FXML private Label studentCountFooterLabel;

    // Student List Table
    @FXML private TableView<ClassDetailController.StudentModel> studentTable;
    @FXML private TableColumn<ClassDetailController.StudentModel, String> colId;
    @FXML private TableColumn<ClassDetailController.StudentModel, String> colRollNo;
    @FXML private TableColumn<ClassDetailController.StudentModel, String> colName;
    @FXML private TableColumn<ClassDetailController.StudentModel, String> colEmail;
    @FXML private TableColumn<ClassDetailController.StudentModel, String> colAttendance;
    @FXML private TableColumn<ClassDetailController.StudentModel, Double> colGpa;
    @FXML private TableColumn<ClassDetailController.StudentModel, String> colStatus;

    private final ObservableList<ClassDetailController.StudentModel> masterStudents = FXCollections.observableArrayList();
    private FilteredList<ClassDetailController.StudentModel> filteredStudents;

    @FXML
    public void initialize() {
        if (assignTeacherCombo != null) {
            assignTeacherCombo.setItems(FXCollections.observableArrayList(
                "Dr. Hla Myint", "Dr. Aung Naing", "Dr. Su Su", "Prof. Kyaw Zin", "Dr. Tin Maung", "Dr. Myo Tint"
            ));
            assignTeacherCombo.setValue("Dr. Hla Myint");
        }

        if (colId != null) colId.setCellValueFactory(cell -> cell.getValue().idProperty());
        if (colRollNo != null) colRollNo.setCellValueFactory(cell -> cell.getValue().rollNoProperty());
        if (colName != null) colName.setCellValueFactory(cell -> cell.getValue().nameProperty());
        if (colEmail != null) colEmail.setCellValueFactory(cell -> cell.getValue().emailProperty());
        if (colAttendance != null) colAttendance.setCellValueFactory(cell -> cell.getValue().attendanceProperty());
        if (colGpa != null) colGpa.setCellValueFactory(cell -> cell.getValue().gpaProperty().asObject());
        if (colStatus != null) {
            colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
            colStatus.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        Label badge = new Label(status);
                        String base = "-fx-padding: 3px 10px; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 11px;";
                        switch (status.toLowerCase()) {
                            case "excellent" -> badge.setStyle(base + " -fx-background-color: #dcfce7; -fx-text-fill: #16a34a;");
                            case "good"      -> badge.setStyle(base + " -fx-background-color: #e0f2fe; -fx-text-fill: #0284c7;");
                            case "at risk"   -> badge.setStyle(base + " -fx-background-color: #fef3c7; -fx-text-fill: #d97706;");
                            default          -> badge.setStyle(base + " -fx-background-color: #fee2e2; -fx-text-fill: #dc2626;");
                        }
                        setGraphic(badge);
                        setText(null);
                    }
                }
            });
        }

        if (statusCombo != null) {
            statusCombo.setItems(FXCollections.observableArrayList("All Status", "Excellent", "Good", "At Risk", "Inactive"));
            statusCombo.setValue("All Status");
            statusCombo.valueProperty().addListener((obs, o, n) -> applyStudentFilters());
        }

        if (rollNoCombo != null) {
            rollNoCombo.valueProperty().addListener((obs, o, n) -> applyStudentFilters());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> applyStudentFilters());
        }

        filteredStudents = new FilteredList<>(masterStudents, s -> true);
        if (studentTable != null) {
            SortedList<ClassDetailController.StudentModel> sorted = new SortedList<>(filteredStudents);
            sorted.comparatorProperty().bind(studentTable.comparatorProperty());
            studentTable.setItems(sorted);
            studentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }

        // Load default sample data in case initClassData wasn't triggered yet
        if (masterStudents.isEmpty()) {
            loadSampleStudents();
        }
    }

    public void initClassData(String classCode) {
        if (classNameLabel != null) {
            classNameLabel.setText("Admin Class Management (" + classCode + ")");
        }
        if (scheduleField != null) {
            scheduleField.setText("Mon, Wed 12:00 - 15:00");
        }
        loadSampleStudents();
    }

    private void loadSampleStudents() {
        masterStudents.setAll(
            new ClassDetailController.StudentModel("256730", "III-CEIT-1", "Aung Thura", "aungthura@gmail.com", "92%", 3.6),
            new ClassDetailController.StudentModel("256731", "III-CEIT-2", "Su Myat Mya", "sumyat@gmail.com", "82%", 3.6),
            new ClassDetailController.StudentModel("256732", "III-CEIT-3", "Min Khant", "minkhant@gmail.com", "74%", 2.4),
            new ClassDetailController.StudentModel("256733", "III-CEIT-4", "Hnin Wai", "hninwai@gmail.com", "95%", 3.9),
            new ClassDetailController.StudentModel("256734", "III-CEIT-5", "Ko Thant", "kothant@gmail.com", "48%", 2.1),
            new ClassDetailController.StudentModel("256735", "III-CEIT-6", "Zaw Hein", "zawhein@gmail.com", "98%", 4.0),
            new ClassDetailController.StudentModel("256736", "III-CEIT-7", "May Thu", "maythu@gmail.com", "68%", 3.2),
            new ClassDetailController.StudentModel("256737", "III-CEIT-8", "Aimon M", "aimonm@gmail.com", "50%", 1.8),
            new ClassDetailController.StudentModel("256738", "III-CEIT-9", "Kyaw Zin", "kyawzin@gmail.com", "85%", 3.4),
            new ClassDetailController.StudentModel("256739", "III-CEIT-10", "Nandar Lin", "nandarlin@gmail.com", "91%", 3.7)
        );
        refreshRollNoOptions();
        updateMetricsAndFooter();
    }

    private void refreshRollNoOptions() {
        if (rollNoCombo == null) return;
        String previous = rollNoCombo.getValue();
        ObservableList<String> rolls = FXCollections.observableArrayList("All Roll Nos");
        for (ClassDetailController.StudentModel s : masterStudents) {
            if (!rolls.contains(s.getRollNo())) {
                rolls.add(s.getRollNo());
            }
        }
        rollNoCombo.setItems(rolls);
        rollNoCombo.setValue(previous != null && rolls.contains(previous) ? previous : "All Roll Nos");
    }

    @FXML
    public void handleFilterStudents(ActionEvent event) {
        applyStudentFilters();
    }

    @FXML
    public void handleResetFilters(ActionEvent event) {
        if (rollNoCombo != null) rollNoCombo.setValue("All Roll Nos");
        if (statusCombo != null) statusCombo.setValue("All Status");
        if (searchField != null) searchField.clear();
        applyStudentFilters();
    }

    private void applyStudentFilters() {
        if (filteredStudents == null) return;
        String selRoll = rollNoCombo != null ? rollNoCombo.getValue() : "All Roll Nos";
        String selStatus = statusCombo != null ? statusCombo.getValue() : "All Status";
        String q = (searchField != null && searchField.getText() != null)
                ? searchField.getText().trim().toLowerCase()
                : "";

        filteredStudents.setPredicate(s -> {
            boolean matchRoll = selRoll == null || selRoll.equals("All Roll Nos") || s.getRollNo().equalsIgnoreCase(selRoll);
            boolean matchStatus = selStatus == null || selStatus.equals("All Status") || s.getStatus().equalsIgnoreCase(selStatus);
            boolean matchSearch = q.isEmpty()
                    || s.getId().toLowerCase().contains(q)
                    || s.getRollNo().toLowerCase().contains(q)
                    || s.getName().toLowerCase().contains(q)
                    || s.getEmail().toLowerCase().contains(q);
            return matchRoll && matchStatus && matchSearch;
        });

        updateMetricsAndFooter();
    }

    private void updateMetricsAndFooter() {
        int total = masterStudents.size();
        int active = 0;
        int atRisk = 0;
        int inactive = 0;

        for (ClassDetailController.StudentModel s : masterStudents) {
            String st = s.getStatus() != null ? s.getStatus().toLowerCase() : "";
            if (st.equals("excellent") || st.equals("good")) {
                active++;
            } else if (st.equals("at risk")) {
                atRisk++;
            } else {
                inactive++;
            }
        }

        if (totalStudentsLabel != null) totalStudentsLabel.setText(String.valueOf(total));
        if (activeStudentsLabel != null) activeStudentsLabel.setText(String.valueOf(active));
        if (atRiskLabel != null) atRiskLabel.setText(String.valueOf(atRisk));
        if (inactiveLabel != null) inactiveLabel.setText(String.valueOf(inactive));

        if (studentCountFooterLabel != null && filteredStudents != null) {
            int shown = filteredStudents.size();
            studentCountFooterLabel.setText(shown == 0
                    ? "Showing 0 of " + total + " students"
                    : "Showing 1 - " + shown + " of " + total + " students");
        }
    }

    @FXML
    public void handleModifyStudent(ActionEvent event) {
        ClassDetailController.StudentModel selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Student Selected");
            alert.setHeaderText(null);
            alert.setContentText("Please select a student from the Student List table to modify.");
            alert.showAndWait();
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modify Student Details");
        dialog.setHeaderText("Edit student record for " + selected.getRollNo() + " (" + selected.getName() + ")");

        TextField nameField = new TextField(selected.getName());
        TextField emailField = new TextField(selected.getEmail());
        TextField attendanceField = new TextField(selected.getAttendance());
        TextField gpaField = new TextField(String.valueOf(selected.getGpa()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Student Name:"), nameField);
        grid.addRow(1, new Label("Email:"), emailField);
        grid.addRow(2, new Label("Attendance:"), attendanceField);
        grid.addRow(3, new Label("GPA:"), gpaField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    double parsedGpa = Double.parseDouble(gpaField.getText().trim());
                    int idx = masterStudents.indexOf(selected);
                    if (idx >= 0) {
                        // Recreate StudentModel so auto-computed Status updates cleanly if GPA/Attendance changed
                        ClassDetailController.StudentModel updated = new ClassDetailController.StudentModel(
                            selected.getId(),
                            selected.getRollNo(),
                            nameField.getText().trim(),
                            emailField.getText().trim(),
                            attendanceField.getText().trim(),
                            parsedGpa
                        );
                        masterStudents.set(idx, updated);
                        studentTable.refresh();
                        applyStudentFilters();
                    }
                } catch (NumberFormatException ex) {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Invalid GPA");
                    err.setHeaderText(null);
                    err.setContentText("Please enter a valid numeric GPA (e.g., 3.5).");
                    err.showAndWait();
                }
            }
        });
    }

    @FXML
    public void handleRemoveStudent(ActionEvent event) {
        ClassDetailController.StudentModel selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Student Selected");
            alert.setHeaderText(null);
            alert.setContentText("Please select a student from the Student List table to remove.");
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove " + selected.getName() + " (" + selected.getRollNo() + ") from this class?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                masterStudents.remove(selected);
                refreshRollNoOptions();
                updateMetricsAndFooter();
            }
        });
    }

    @FXML
    public void handleSaveAdminChanges(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Admin Assignment Saved");
        alert.setHeaderText(null);
        alert.setContentText("Successfully assigned teacher (" + assignTeacherCombo.getValue() + ") and updated class schedule!");
        alert.showAndWait();
    }

    @FXML
    public void handleOpenCreateClassDialog(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Create New Class");
        alert.setHeaderText("University Class Creation Wizard");
        alert.setContentText("Admin Modal Wizard: Form to input Course Title, Code, Major, Year, Semester, and default Teacher allocation.");
        alert.showAndWait();
    }

    @FXML
    public void handleAdminAddStudent(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Enroll Student");
        dialog.setHeaderText("Add New Student to Class");

        int nextNum = masterStudents.size() + 1;
        TextField idField = new TextField(String.valueOf(256730 + nextNum - 1));
        TextField rollField = new TextField("III-CEIT-" + nextNum);
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Kyaw Swar");
        TextField emailField = new TextField();
        emailField.setPromptText("e.g. kyawswar@gmail.com");
        TextField attendanceField = new TextField("90%");
        TextField gpaField = new TextField("3.5");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Student ID:"), idField);
        grid.addRow(1, new Label("Roll No:"), rollField);
        grid.addRow(2, new Label("Student Name:"), nameField);
        grid.addRow(3, new Label("Email:"), emailField);
        grid.addRow(4, new Label("Attendance:"), attendanceField);
        grid.addRow(5, new Label("GPA:"), gpaField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK && !nameField.getText().trim().isEmpty()) {
                try {
                    double parsedGpa = Double.parseDouble(gpaField.getText().trim());
                    masterStudents.add(new ClassDetailController.StudentModel(
                        idField.getText().trim(),
                        rollField.getText().trim(),
                        nameField.getText().trim(),
                        emailField.getText().trim(),
                        attendanceField.getText().trim(),
                        parsedGpa
                    ));
                    refreshRollNoOptions();
                    updateMetricsAndFooter();
                } catch (NumberFormatException ex) {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Invalid GPA");
                    err.setHeaderText(null);
                    err.setContentText("Please enter a valid numeric GPA (e.g., 3.5).");
                    err.showAndWait();
                }
            }
        });
    }

    @FXML
    private void handleBackToClasses(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AdminClassesView.fxml"));
            Parent classesView = loader.load();

            StackPane contentArea = (StackPane) classNameLabel.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(classesView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}