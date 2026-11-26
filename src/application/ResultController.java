package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ResultController {

    @FXML private VBox rootPane;
    @FXML private Label examTitleLabel;
    @FXML private Label examSubtitleLabel;
    @FXML private Label avgScoreLabel;
    @FXML private Label passRateLabel;
    @FXML private Label totalGradedLabel;
    @FXML private Label maxMarksInfoLabel;

    @FXML private ComboBox<String> rollNoFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TextField searchField;

    @FXML private TableView<Result> resultsTable;
    @FXML private TableColumn<Result, String> colRollNo;
    @FXML private TableColumn<Result, String> colName;
    @FXML private TableColumn<Result, Double> colMarks;
    @FXML private TableColumn<Result, String> colStatus;

    private Exam currentExam;
    private ClassesController.ClassItem parentClassItem;
    private ObservableList<Result> masterResultData;
    private FilteredList<Result> filteredData;

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

    public void initData(Exam exam) {
        initData(exam, null);
    }

    public void initData(Exam exam, ClassesController.ClassItem parentClass) {
        this.currentExam = exam;
        this.parentClassItem = parentClass;

        if (exam != null) {
            examTitleLabel.setText("Result: " + exam.getTitle() + " - " + exam.getSubject());
            examSubtitleLabel.setText("Class: " + exam.getClassName() + " | Date: " + exam.getExamDate() + " | Total Marks: " + exam.getTotalMarks());

            updateMaxMarksInfo(exam);
            loadResultsForExam(exam);
            setupStatusDropdownForExam(exam);
        }

        if (rollNoFilterCombo != null) {
            ObservableList<String> rollNos = FXCollections.observableArrayList("All Roll Nos");
            if (masterResultData != null) {
                for (Result r : masterResultData) {
                    if (!rollNos.contains(r.getRollNo())) {
                        rollNos.add(r.getRollNo());
                    }
                }
            }
            rollNoFilterCombo.setItems(rollNos);
            rollNoFilterCombo.setValue("All Roll Nos");
            rollNoFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }

        colRollNo.setCellValueFactory(cell -> cell.getValue().rollNoProperty());
        colName.setCellValueFactory(cell -> cell.getValue().studentNameProperty());
        colMarks.setCellValueFactory(cell -> cell.getValue().totalMarksProperty().asObject());

        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
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
        SortedList<Result> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(resultsTable.comparatorProperty());
        resultsTable.setItems(sortedData);
        lockTableColumns(resultsTable, 0.18, 0.38, 0.22, 0.216);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }

        if (exam != null && masterResultData != null && !masterResultData.isEmpty()) {
            double totalScoreSum = 0;
            int passCount = 0;
            int totalStudents = masterResultData.size();

            for (Result r : masterResultData) {
                totalScoreSum += r.getTotalMarks();
                String st = r.getStatus().toLowerCase();
                if (!st.equals("fail") && !st.equals("poor")) {
                    passCount++;
                }
            }

            if (totalStudents > 0) {
                double avg = totalScoreSum / totalStudents;
                double passPct = ((double) passCount / totalStudents) * 100.0;
                avgScoreLabel.setText(String.format("%.1f", avg));
                passRateLabel.setText(String.format("%.0f%%", passPct));
            } else {
                avgScoreLabel.setText("0.0");
                passRateLabel.setText("0%");
            }
            totalGradedLabel.setText(String.valueOf(totalStudents));
        } else {
            avgScoreLabel.setText("Pending");
            passRateLabel.setText("N/A");
            totalGradedLabel.setText("0");
        }
    }

    private void updateMaxMarksInfo(Exam exam) {
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

    private void setupStatusDropdownForExam(Exam exam) {
        if (statusFilterCombo == null) return;

        String title = exam.getTitle().toLowerCase();
        ObservableList<String> statuses = FXCollections.observableArrayList("All");

        if (title.contains("tutorial")) {
            statuses.addAll("Pass", "Fail");
        } else if (title.contains("quiz")) {
            statuses.addAll("Outstanding", "Great", "Good", "Poor");
        } else {
            statuses.addAll("Excellent", "Pass", "Fail");
        }

        statusFilterCombo.setItems(statuses);
        statusFilterCombo.setValue("All");
        statusFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadResultsForExam(Exam exam) {
        masterResultData = FXCollections.observableArrayList();
        String title = exam.getTitle().toLowerCase();

        if (title.contains("tutorial")) {
            masterResultData.add(createResult("III-CEIT-1", "Mg Mg", 45.0, title));
            masterResultData.add(createResult("III-CEIT-2", "Thant Zin", 38.0, title));
            masterResultData.add(createResult("III-CEIT-3", "Su Su", 15.0, title));
            masterResultData.add(createResult("III-CEIT-4", "Aung Aung", 42.0, title));
            masterResultData.add(createResult("III-CEIT-5", "Hla Hla", 25.0, title));
        } else if (title.contains("quiz")) {
            masterResultData.add(createResult("III-CEIT-1", "Mg Mg", 20.0, title));
            masterResultData.add(createResult("III-CEIT-2", "Thant Zin", 18.0, title));
            masterResultData.add(createResult("III-CEIT-3", "Su Su", 10.0, title));
            masterResultData.add(createResult("III-CEIT-4", "Aung Aung", 5.0, title));
            masterResultData.add(createResult("III-CEIT-5", "Hla Hla", 16.0, title));
        } else {
            masterResultData.add(createResult("III-CEIT-1", "Mg Mg", 92.0, title));
            masterResultData.add(createResult("III-CEIT-2", "Thant Zin", 78.0, title));
            masterResultData.add(createResult("III-CEIT-3", "Su Su", 45.0, title));
            masterResultData.add(createResult("III-CEIT-5", "Hla Hla", 95.0, title));
            masterResultData.add(createResult("III-CEIT-10", "Kyaw Kyaw", 60.0, title));
        }
    }

    private Result createResult(String rollNo, String name, double totalMarks, String examTitle) {
        String status;
        if (examTitle.contains("tutorial")) {
            status = (totalMarks >= 20.0) ? "Pass" : "Fail";
        } else if (examTitle.contains("quiz")) {
            if (totalMarks >= 20.0) status = "Outstanding";
            else if (totalMarks >= 15.0) status = "Great";
            else if (totalMarks >= 7.0) status = "Good";
            else status = "Poor";
        } else {
            if (totalMarks >= 90.0) status = "Excellent";
            else if (totalMarks >= 50.0) status = "Pass";
            else status = "Fail";
        }
        return new Result(rollNo, name, totalMarks, status, 0, 0, 0, 0, currentExam != null ? currentExam.getTitle() : "Exam");
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
                    controller.initData(parentClassItem, false);
                }
                if (MainController.instance != null) {
                    MainController.instance.loadCustomView(detailView);
                } else {
                    if (contentArea != null) {
                        contentArea.getChildren().setAll(detailView);
                    }
                }
            } else if (MainController.instance != null) {
                MainController.instance.showExams();
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
}