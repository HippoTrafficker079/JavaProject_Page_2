package application;

import javafx.beans.property.SimpleDoubleProperty;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import java.io.IOException;

import javafx.event.ActionEvent;
public class ClassDetailController {

    @FXML private Label classNameLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label activeStudentsLabel;
    @FXML private Label atRiskLabel;
    @FXML private Label inactiveLabel;
    @FXML private Label footerCountLabel;
    
    @FXML private ComboBox<String> rollNoFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TextField searchField;

    @FXML private TableView<StudentModel> studentTable;
    @FXML private TableColumn<StudentModel, String> colId;
    @FXML private TableColumn<StudentModel, String> colRollNo;
    @FXML private TableColumn<StudentModel, String> colName;
    @FXML private TableColumn<StudentModel, String> colEmail;
    @FXML private TableColumn<StudentModel, String> colAttendance;
    @FXML private TableColumn<StudentModel, Double> colGpa;
    @FXML private TableColumn<StudentModel, String> colStatus;

    private ObservableList<StudentModel> masterStudentData;
    private FilteredList<StudentModel> filteredData;

    @FXML
    public void initialize() {
        if (rollNoFilterCombo != null) {
            ObservableList<String> rollNos = FXCollections.observableArrayList("All Roll Nos");
            for (int i = 1; i <= 30; i++) {
                rollNos.add("III-CEIT-" + i);
            }
            rollNoFilterCombo.setItems(rollNos);
            rollNoFilterCombo.setValue("All Roll Nos");
        }

        if (statusFilterCombo != null) {
            statusFilterCombo.setItems(FXCollections.observableArrayList("All Status", "Excellent", "Good", "At Risk", "Inactive"));
            statusFilterCombo.setValue("All Status");
        }

        if (colId != null) colId.setCellValueFactory(cell -> cell.getValue().idProperty());
        if (colRollNo != null) colRollNo.setCellValueFactory(cell -> cell.getValue().rollNoProperty());
        if (colName != null) colName.setCellValueFactory(cell -> cell.getValue().nameProperty());
        if (colEmail != null) colEmail.setCellValueFactory(cell -> cell.getValue().emailProperty());
        if (colAttendance != null) colAttendance.setCellValueFactory(cell -> cell.getValue().attendanceProperty());
        if (colGpa != null) colGpa.setCellValueFactory(cell -> cell.getValue().gpaProperty().asObject());

        if (colStatus != null) {
            colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
            colStatus.setCellFactory(column -> new TableCell<StudentModel, String>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        Label badge = new Label(status);
                        badge.setStyle("-fx-padding: 3px 10px; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 11px;");
                        
                        if (status.equalsIgnoreCase("Excellent")) {
                            badge.setStyle(badge.getStyle() + " -fx-background-color: #dcfce7; -fx-text-fill: #16a34a;");
                        } else if (status.equalsIgnoreCase("Good")) {
                            badge.setStyle(badge.getStyle() + " -fx-background-color: #e0f2fe; -fx-text-fill: #0284c7;");
                        } else if (status.equalsIgnoreCase("At Risk")) {
                            badge.setStyle(badge.getStyle() + " -fx-background-color: #fef3c7; -fx-text-fill: #d97706;");
                        } else { // Inactive
                            badge.setStyle(badge.getStyle() + " -fx-background-color: #fee2e2; -fx-text-fill: #dc2626;");
                        }
                        setGraphic(badge);
                    }
                }
            });
        }
    }

    @FXML
    public void handleFilter() {
        if (filteredData == null) return;
        String selectedRoll = rollNoFilterCombo != null ? rollNoFilterCombo.getValue() : "All Roll Nos";
        String selectedStatus = statusFilterCombo != null ? statusFilterCombo.getValue() : "All Status";
        String searchKeyword = searchField != null ? searchField.getText().toLowerCase().trim() : "";

        filteredData.setPredicate(student -> {
            boolean matchesRoll = (selectedRoll.equals("All Roll Nos") || student.getRollNo().equalsIgnoreCase(selectedRoll));
            boolean matchesStatus = (selectedStatus.equals("All Status") || student.getStatus().equalsIgnoreCase(selectedStatus));
            boolean matchesSearch = searchKeyword.isEmpty() ||
                student.getName().toLowerCase().contains(searchKeyword) ||
                student.getEmail().toLowerCase().contains(searchKeyword) ||
                student.getId().toLowerCase().contains(searchKeyword) ||
                student.getRollNo().toLowerCase().contains(searchKeyword);

            return matchesRoll && matchesStatus && matchesSearch;
        });
    }

    @FXML
    public void handleReset() {
        if (rollNoFilterCombo != null) rollNoFilterCombo.setValue("All Roll Nos");
        if (statusFilterCombo != null) statusFilterCombo.setValue("All Status");
        if (searchField != null) searchField.clear();
        if (filteredData != null) filteredData.setPredicate(p -> true);
    }

    public void initClassData(String classCode) {
        String subjectName = "";
        switch (classCode) {
            case "IT201": subjectName = "Data Structures"; break;
            case "IT202": subjectName = "Database Systems"; break;
            case "IT203": subjectName = "Computer Networks"; break;
            case "IT204": subjectName = "Web Technologies"; break;
            case "IT205": subjectName = "Operating Systems"; break;
            case "IT206": subjectName = "Software Engineering"; break;
            default: subjectName = "General Class"; break;
        }

        classNameLabel.setText("Students (" + classCode + ") - " + subjectName);
        loadSampleStudentsForClass(classCode);
    }

    private void loadSampleStudentsForClass(String classCode) {
        // Attendance percentages dictate Status automatically based on rule:
        // <= 50% -> Inactive, < 75% -> At Risk, otherwise Good/Excellent
        masterStudentData = FXCollections.observableArrayList(
            new StudentModel("256730", "III-CEIT-1", "Aung Thura", "aungthura@gmail.com", "92%", 3.6),
            new StudentModel("256731", "III-CEIT-2", "Su Myat Mya", "sumyat@gmail.com", "82%", 3.6),
            new StudentModel("256732", "III-CEIT-3", "Min Khant", "minkhant@gmail.com", "74%", 2.4), // At Risk (< 75%)
            new StudentModel("256733", "III-CEIT-4", "Hnin Wai", "hninwai@gmail.com", "95%", 3.9),
            new StudentModel("256734", "III-CEIT-5", "Ko Thant", "kothant@gmail.com", "48%", 2.1), // Inactive (<= 50%)
            new StudentModel("256735", "III-CEIT-6", "Zaw Hein", "zawhein@gmail.com", "98%", 4.0),
            new StudentModel("256736", "III-CEIT-7", "May Thu", "maythu@gmail.com", "68%", 3.2), // At Risk (< 75%)
            new StudentModel("256737", "III-CEIT-8", "Aimon M", "aimonm@gmail.com", "50%", 1.8),  // Inactive (<= 50%)
            new StudentModel("256738", "III-CEIT-9", "Kyaw Zin", "kyawzin@gmail.com", "85%", 3.4),
            new StudentModel("256739", "III-CEIT-10", "Thiri San", "thirisan@gmail.com", "40%", 1.5) // Inactive
        );

        filteredData = new FilteredList<>(masterStudentData, b -> true);
        SortedList<StudentModel> sortedData = new SortedList<>(filteredData);
        if (studentTable != null) {
            sortedData.comparatorProperty().bind(studentTable.comparatorProperty());
            studentTable.setItems(sortedData);
            studentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }
        if (footerCountLabel != null) {
            footerCountLabel.setText("Showing 1 - 8 of " + masterStudentData.size() + " students");
        }
    }

    @FXML
    private void handleBackToClasses() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ClassesView.fxml"));
            Parent classesView = loader.load();
            
            StackPane contentArea = (StackPane) classNameLabel.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(classesView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class StudentModel {
        private final SimpleStringProperty id;
        private final SimpleStringProperty rollNo;
        private final SimpleStringProperty name;
        private final SimpleStringProperty email;
        private final SimpleStringProperty attendance;
        private final SimpleDoubleProperty gpa;
        private final SimpleStringProperty status;

        public StudentModel(String id, String rollNo, String name, String email, String attendanceStr, double gpa) {
            this.id = new SimpleStringProperty(id);
            this.rollNo = new SimpleStringProperty(rollNo);
            this.name = new SimpleStringProperty(name);
            this.email = new SimpleStringProperty(email);
            this.attendance = new SimpleStringProperty(attendanceStr);
            this.gpa = new SimpleDoubleProperty(gpa);
            
            // Calculate status dynamically based on attendance percentage
            int attVal = Integer.parseInt(attendanceStr.replace("%", "").trim());
            String calculatedStatus;
            if (attVal <= 50) {
                calculatedStatus = "Inactive";
            } else if (attVal < 75) {
                calculatedStatus = "At Risk";
            } else if (gpa >= 3.8) {
                calculatedStatus = "Excellent";
            } else {
                calculatedStatus = "Good";
            }
            this.status = new SimpleStringProperty(calculatedStatus);
        }

        public String getId() { return id.get(); }
        public SimpleStringProperty idProperty() { return id; }

        public String getRollNo() { return rollNo.get(); }
        public SimpleStringProperty rollNoProperty() { return rollNo; }

        public String getName() { return name.get(); }
        public SimpleStringProperty nameProperty() { return name; }

        public String getEmail() { return email.get(); }
        public SimpleStringProperty emailProperty() { return email; }

        public String getAttendance() { return attendance.get(); }
        public SimpleStringProperty attendanceProperty() { return attendance; }

        public double getGpa() { return gpa.get(); }
        public SimpleDoubleProperty gpaProperty() { return gpa; }

        public String getStatus() { return status.get(); }
        public SimpleStringProperty statusProperty() { return status; }
    }
    
    @FXML
    public void handleRemoveStudent(ActionEvent event) {
        StudentModel selected = studentTable.getSelectionModel().getSelectedItem();
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
                masterStudentData.remove(selected);

                // Refresh Roll No dropdown items
                if (rollNoFilterCombo != null) {
                    String currentRoll = rollNoFilterCombo.getValue();
                    ObservableList<String> rolls = FXCollections.observableArrayList("All Roll Nos");
                    for (StudentModel s : masterStudentData) {
                        if (!rolls.contains(s.getRollNo())) {
                            rolls.add(s.getRollNo());
                        }
                    }
                    rollNoFilterCombo.setItems(rolls);
                    rollNoFilterCombo.setValue(currentRoll != null && rolls.contains(currentRoll) ? currentRoll : "All Roll Nos");
                }

                // Update metric cards and footer count
                int total = masterStudentData.size();
                int active = 0, atRisk = 0, inactive = 0;
                for (StudentModel s : masterStudentData) {
                    String st = s.getStatus().toLowerCase();
                    if (st.equals("excellent") || st.equals("good")) active++;
                    else if (st.equals("at risk")) atRisk++;
                    else inactive++;
                }
                if (totalStudentsLabel != null) totalStudentsLabel.setText(String.valueOf(total));
                if (activeStudentsLabel != null) activeStudentsLabel.setText(String.valueOf(active));
                if (atRiskLabel != null) atRiskLabel.setText(String.valueOf(atRisk));
                if (inactiveLabel != null) inactiveLabel.setText(String.valueOf(inactive));

                if (footerCountLabel != null && filteredData != null) {
                    int shown = filteredData.size();
                    footerCountLabel.setText(shown == 0
                            ? "Showing 0 of " + total + " students"
                            : "Showing 1 - " + shown + " of " + total + " students");
                }
            }
        });
    }
}