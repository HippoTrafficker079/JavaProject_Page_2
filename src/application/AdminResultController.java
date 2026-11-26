package application;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class AdminResultController {

    @FXML private VBox rootPane;
    @FXML private Label examTitleLabel;
    @FXML private Label examSubtitleLabel;
    @FXML private Label lecturedByBadge;
    @FXML private Label avgScoreLabel;
    @FXML private Label passRateLabel;
    @FXML private Label totalGradedLabel;
    @FXML private Label maxMarksInfoLabel;
    @FXML private Label verificationStatusLabel;
    @FXML private Button verifyPublishBtn;

    @FXML private ComboBox<String> rollNoFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TextField searchField;

    @FXML private TableView<AdminResultRow> resultsTable;
    @FXML private TableColumn<AdminResultRow, String> colRollNo;
    @FXML private TableColumn<AdminResultRow, String> colName;
    @FXML private TableColumn<AdminResultRow, String> colLecturedBy;
    @FXML private TableColumn<AdminResultRow, Double> colMarks;
    @FXML private TableColumn<AdminResultRow, String> colStatus;

    private AdminExamsController.AdminExamRecord currentExam;
    private ClassesController.ClassItem parentClassItem;
    private final ObservableList<AdminResultRow> masterResultData = FXCollections.observableArrayList();
    private FilteredList<AdminResultRow> filteredData;
    private boolean isLocked = false;

    @FXML
    public void initialize() {
        if (rootPane != null) {
            rootPane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                Node target = (event.getTarget() instanceof Node) ? (Node) event.getTarget() : null;
                if (isInsideNodeType(target, TableView.class) || isInsideNodeType(target, ButtonBase.class)) {
                    return;
                }
                if (resultsTable != null) {
                    resultsTable.getSelectionModel().clearSelection();
                }
                rootPane.requestFocus();
            });
        }
    }

    public void initData(AdminExamsController.AdminExamRecord exam) {
        initData(exam, null);
    }

    public void initData(AdminExamsController.AdminExamRecord exam, ClassesController.ClassItem parentClass) {
        this.currentExam = exam;
        this.parentClassItem = parentClass;

        if (exam != null) {
            examTitleLabel.setText("Result Audit: " + exam.getTitle() + " - " + exam.getSubject());
            examSubtitleLabel.setText("Class: " + exam.getClassName() + " | Date: " + exam.getExamDate() + " | Total Marks: " + exam.getTotalMarks());
            if (lecturedByBadge != null) {
                lecturedByBadge.setText("👨‍🏫 Lectured By: " + exam.getLecturedBy());
            }
            updateMaxMarksInfo(exam);
            loadResultsForExam(exam);
            setupStatusDropdownForExam(exam);
        }

        if (rollNoFilterCombo != null) {
            ObservableList<String> rollNos = FXCollections.observableArrayList("All Roll Nos");
            for (AdminResultRow row : masterResultData) {
                if (!rollNos.contains(row.getRollNo())) {
                    rollNos.add(row.getRollNo());
                }
            }
            rollNoFilterCombo.setItems(rollNos);
            rollNoFilterCombo.setValue("All Roll Nos");
            rollNoFilterCombo.valueProperty().addListener((obs, o, n) -> applyFilters());
        }

        colRollNo.setCellValueFactory(c -> c.getValue().rollNoProperty());
        colName.setCellValueFactory(c -> c.getValue().studentNameProperty());
        colLecturedBy.setCellValueFactory(c -> c.getValue().lecturedByProperty());
        colMarks.setCellValueFactory(c -> c.getValue().totalMarksProperty().asObject());

        colStatus.setCellValueFactory(c -> c.getValue().statusProperty());
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(status);
                    badge.setStyle("-fx-padding: 3px 10px; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 11px;");
                    if (status.equalsIgnoreCase("Excellent") || status.equalsIgnoreCase("Outstanding") || status.equalsIgnoreCase("Great")) {
                        badge.setStyle(badge.getStyle() + " -fx-background-color: #dcfce7; -fx-text-fill: #16a34a;");
                    } else if (status.equalsIgnoreCase("Pass") || status.equalsIgnoreCase("Good")) {
                        badge.setStyle(badge.getStyle() + " -fx-background-color: #e0f2fe; -fx-text-fill: #0284c7;");
                    } else {
                        badge.setStyle(badge.getStyle() + " -fx-background-color: #fee2e2; -fx-text-fill: #dc2626;");
                    }
                    setGraphic(badge);
                }
            }
        });

        filteredData = new FilteredList<>(masterResultData, b -> true);
        SortedList<AdminResultRow> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(resultsTable.comparatorProperty());
        resultsTable.setItems(sortedData);

        lockTableColumns(resultsTable, 0.16, 0.28, 0.24, 0.15, 0.166);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        }

        recalculateMetrics();
    }

    private void recalculateMetrics() {
        if (masterResultData.isEmpty()) {
            avgScoreLabel.setText("0.0");
            passRateLabel.setText("0%");
            totalGradedLabel.setText("0");
            return;
        }
        double sum = 0;
        int passCount = 0;
        for (AdminResultRow r : masterResultData) {
            sum += r.getTotalMarks();
            String st = r.getStatus().toLowerCase();
            if (!st.equals("fail") && !st.equals("poor")) {
                passCount++;
            }
        }
        avgScoreLabel.setText(String.format("%.1f", sum / masterResultData.size()));
        passRateLabel.setText(String.format("%.0f%%", ((double) passCount / masterResultData.size()) * 100.0));
        totalGradedLabel.setText(String.valueOf(masterResultData.size()));
    }

    private void updateMaxMarksInfo(AdminExamsController.AdminExamRecord exam) {
        if (maxMarksInfoLabel == null) return;
        String title = exam.getTitle().toLowerCase();
        if (title.contains("tutorial")) {
            maxMarksInfoLabel.setText("!! Maximum Marks for Tutorial: 50 Marks (Passing score: 20+)");
        } else if (title.contains("quiz")) {
            maxMarksInfoLabel.setText("!! Maximum Marks for Quiz: 20 Marks (Outstanding = 20 | Great = 15-19 | Good = 7-14 | Poor = Below 7)");
        } else {
            maxMarksInfoLabel.setText("!! Maximum Marks for Final Exam: 100 Marks (Excellent = 90+ | Pass = 50-89 | Fail = Below 50)");
        }
    }

    private void setupStatusDropdownForExam(AdminExamsController.AdminExamRecord exam) {
        if (statusFilterCombo == null) return;
        String title = exam.getTitle().toLowerCase();
        ObservableList<String> statuses = FXCollections.observableArrayList("All");
        if (title.contains("tutorial")) statuses.addAll("Pass", "Fail");
        else if (title.contains("quiz")) statuses.addAll("Outstanding", "Great", "Good", "Poor");
        else statuses.addAll("Excellent", "Pass", "Fail");

        statusFilterCombo.setItems(statuses);
        statusFilterCombo.setValue("All");
        statusFilterCombo.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void loadResultsForExam(AdminExamsController.AdminExamRecord exam) {
        masterResultData.clear();
        String title = exam.getTitle().toLowerCase();
        String lecturer = exam.getLecturedBy();

        if (title.contains("tutorial")) {
            masterResultData.addAll(
                    createResult("III-CEIT-1", "Mg Mg", lecturer, 45.0, title),
                    createResult("III-CEIT-2", "Thant Zin", lecturer, 38.0, title),
                    createResult("III-CEIT-3", "Su Su", lecturer, 15.0, title),
                    createResult("III-CEIT-4", "Aung Aung", lecturer, 42.0, title),
                    createResult("III-CEIT-5", "Hla Hla", lecturer, 25.0, title)
            );
        } else if (title.contains("quiz")) {
            masterResultData.addAll(
                    createResult("III-CEIT-1", "Mg Mg", lecturer, 20.0, title),
                    createResult("III-CEIT-2", "Thant Zin", lecturer, 18.0, title),
                    createResult("III-CEIT-3", "Su Su", lecturer, 10.0, title),
                    createResult("III-CEIT-4", "Aung Aung", lecturer, 5.0, title),
                    createResult("III-CEIT-5", "Hla Hla", lecturer, 16.0, title)
            );
        } else {
            masterResultData.addAll(
                    createResult("III-CEIT-1", "Mg Mg", lecturer, 92.0, title),
                    createResult("III-CEIT-2", "Thant Zin", lecturer, 78.0, title),
                    createResult("III-CEIT-3", "Su Su", lecturer, 45.0, title),
                    createResult("III-CEIT-5", "Hla Hla", lecturer, 95.0, title),
                    createResult("III-CEIT-10", "Kyaw Kyaw", lecturer, 60.0, title)
            );
        }
    }

    private AdminResultRow createResult(String rollNo, String name, String lecturedBy, double marks, String examTitle) {
        return new AdminResultRow(rollNo, name, lecturedBy, marks, computeStatus(marks, examTitle));
    }

    private String computeStatus(double totalMarks, String examTitle) {
        if (examTitle.contains("tutorial")) {
            return (totalMarks >= 20.0) ? "Pass" : "Fail";
        } else if (examTitle.contains("quiz")) {
            if (totalMarks >= 20.0) return "Outstanding";
            if (totalMarks >= 15.0) return "Great";
            if (totalMarks >= 7.0) return "Good";
            return "Poor";
        } else {
            if (totalMarks >= 90.0) return "Excellent";
            if (totalMarks >= 50.0) return "Pass";
            return "Fail";
        }
    }

    @FXML
    public void handleOverrideMarks() {
        if (isLocked) {
            Alert warn = new Alert(Alert.AlertType.WARNING, "These results have been officially verified and locked.", ButtonType.OK);
            warn.setHeaderText(null);
            warn.showAndWait();
            return;
        }

        AdminResultRow selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert warn = new Alert(Alert.AlertType.WARNING, "Please select a student row to override their score.", ButtonType.OK);
            warn.setHeaderText(null);
            warn.showAndWait();
            return;
        }

        TextInputDialog dialog = new TextInputDialog(String.valueOf(selected.getTotalMarks()));
        dialog.setTitle("Admin Score Override");
        dialog.setHeaderText("Override Marks for " + selected.getRollNo() + " (" + selected.getStudentName() + ")");
        dialog.setContentText("New Total Marks (Max " + currentExam.getTotalMarks() + "):");

        dialog.showAndWait().ifPresent(input -> {
            try {
                double newMarks = Double.parseDouble(input.trim());
                if (newMarks < 0 || newMarks > currentExam.getTotalMarks()) {
                    throw new NumberFormatException();
                }
                selected.totalMarksProperty().set(newMarks);
                selected.statusProperty().set(computeStatus(newMarks, currentExam.getTitle().toLowerCase()));
                resultsTable.refresh();
                recalculateMetrics();
            } catch (NumberFormatException ex) {
                Alert err = new Alert(Alert.AlertType.ERROR, "Enter a valid score between 0 and " + currentExam.getTotalMarks() + ".", ButtonType.OK);
                err.setHeaderText(null);
                err.showAndWait();
            }
        });
    }

    @FXML
    public void handleVerifyAndLock() {
        isLocked = true;
        if (currentExam != null) {
            currentExam.statusProperty().set("Finished");
        }
        if (verificationStatusLabel != null) {
            verificationStatusLabel.setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold; -fx-font-size: 12px;");
            verificationStatusLabel.setText("✔ Officially Verified & Published by System Administrator");
        }
        if (verifyPublishBtn != null) {
            verifyPublishBtn.setDisable(true);
            verifyPublishBtn.setText("✔ Verified & Locked");
        }
    }

    private void applyFilters() {
        String selectedRoll = rollNoFilterCombo != null ? rollNoFilterCombo.getValue() : "All Roll Nos";
        String searchQuery = searchField != null && searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String selectedStatus = statusFilterCombo != null ? statusFilterCombo.getValue() : "All";

        filteredData.setPredicate(result -> {
            boolean matchesRoll = (selectedRoll == null || selectedRoll.equals("All Roll Nos"))
                    || result.getRollNo().equalsIgnoreCase(selectedRoll);
            boolean matchesSearch = searchQuery.isEmpty()
                    || result.getStudentName().toLowerCase().contains(searchQuery)
                    || result.getRollNo().toLowerCase().contains(searchQuery)
                    || result.getStatus().toLowerCase().contains(searchQuery);
            boolean matchesStatus = (selectedStatus == null || selectedStatus.equals("All"))
                    || result.getStatus().equalsIgnoreCase(selectedStatus);
            return matchesRoll && matchesSearch && matchesStatus;
        });
    }

    @FXML
    public void handleBack() {
    	
        try {
        	
        	StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
        	if (ViewNavigationHistory.goBack(contentArea)) {
        	    return;
        	}
            if (parentClassItem != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("ClassExamDetailView.fxml"));
                Parent detailView = loader.load();
                ClassExamDetailController controller = loader.getController();
                if (controller != null) {
                    controller.initData(parentClassItem, true);
                }
                if (contentArea != null) {
                    contentArea.getChildren().setAll(detailView);
                }
            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("AdminExamsView.fxml"));
                Parent examsView = loader.load();
                if (contentArea != null) {
                    contentArea.getChildren().setAll(examsView);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleResetFilters(ActionEvent event) {
        if (rollNoFilterCombo != null) rollNoFilterCombo.setValue("All Roll Nos");
        if (statusFilterCombo != null) statusFilterCombo.setValue("All");
        if (searchField != null) searchField.clear();
        applyFilters();
    }

    private boolean isInsideNodeType(Node node, Class<?> type) {
        Node current = node;
        while (current != null) {
            if (type.isInstance(current)) return true;
            current = current.getParent();
        }
        return false;
    }

    private void lockTableColumns(TableView<?> table, double... ratios) {
        if (table == null || table.getColumns().size() != ratios.length) return;
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        for (int i = 0; i < ratios.length; i++) {
            TableColumn<?, ?> col = table.getColumns().get(i);
            col.setResizable(false);
            col.setReorderable(false);
            col.setSortable(false);
            col.minWidthProperty().bind(table.widthProperty().multiply(ratios[i]));
            col.maxWidthProperty().bind(table.widthProperty().multiply(ratios[i]));
            col.prefWidthProperty().bind(table.widthProperty().multiply(ratios[i]));
        }
    }

    public static class AdminResultRow {
        private final SimpleStringProperty rollNo;
        private final SimpleStringProperty studentName;
        private final SimpleStringProperty lecturedBy;
        private final SimpleDoubleProperty totalMarks;
        private final SimpleStringProperty status;

        public AdminResultRow(String rollNo, String name, String lecturedBy, double totalMarks, String status) {
            this.rollNo = new SimpleStringProperty(rollNo);
            this.studentName = new SimpleStringProperty(name);
            this.lecturedBy = new SimpleStringProperty(lecturedBy);
            this.totalMarks = new SimpleDoubleProperty(totalMarks);
            this.status = new SimpleStringProperty(status);
        }

        public String getRollNo() { return rollNo.get(); }
        public SimpleStringProperty rollNoProperty() { return rollNo; }

        public String getStudentName() { return studentName.get(); }
        public SimpleStringProperty studentNameProperty() { return studentName; }

        public String getLecturedBy() { return lecturedBy.get(); }
        public SimpleStringProperty lecturedByProperty() { return lecturedBy; }

        public double getTotalMarks() { return totalMarks.get(); }
        public SimpleDoubleProperty totalMarksProperty() { return totalMarks; }

        public String getStatus() { return status.get(); }
        public SimpleStringProperty statusProperty() { return status; }
    }
}