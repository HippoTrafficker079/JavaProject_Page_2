package application;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class AdminDashboardController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> semesterCombo;

    @FXML private BarChart<String, Number> enrollmentChart;
    @FXML private CategoryAxis xAxisEnrollment;
    @FXML private NumberAxis yAxisEnrollment;

    @FXML private TableView<RiskStudent> riskTable;
    @FXML private TableColumn<RiskStudent, String> colRollNo;
    @FXML private TableColumn<RiskStudent, String> colName;
    @FXML private TableColumn<RiskStudent, String> colSubject;
    @FXML private TableColumn<RiskStudent, String> colGpa;
    @FXML private TableColumn<RiskStudent, String> colAttendance;
    @FXML private TableColumn<RiskStudent, String> colRiskLevel;
    @FXML private Label riskCountLabel;

    private final ObservableList<RiskStudent> riskData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        initSemesterCombo();
        initEnrollmentChart();
        initRiskTable();
    }

    private void initSemesterCombo() {
        if (semesterCombo != null) {
            semesterCombo.setItems(FXCollections.observableArrayList(
                "Academic Year 2024 - 2025 (Sem II)",
                "Academic Year 2024 - 2025 (Sem I)",
                "Academic Year 2023 - 2024 (Sem II)"
            ));
            semesterCombo.getSelectionModel().selectFirst();
        }
    }

    private void initEnrollmentChart() {
        if (enrollmentChart == null) return;

        if (yAxisEnrollment != null) {
            yAxisEnrollment.setAutoRanging(false);
            yAxisEnrollment.setLowerBound(0.0);
            yAxisEnrollment.setUpperBound(125.0);
            yAxisEnrollment.setTickUnit(25.0);
            yAxisEnrollment.setMinorTickVisible(false);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("1st Year", 92));
        series.getData().add(new XYChart.Data<>("2nd Year", 84));
        series.getData().add(new XYChart.Data<>("3rd Year", 68));
        series.getData().add(new XYChart.Data<>("4th Year", 56));
        series.getData().add(new XYChart.Data<>("5th Year", 48));

        enrollmentChart.getData().setAll(series);

        String[] colors = { "#1d4ed8", "#2563eb", "#3b82f6", "#60a5fa", "#93c5fd" };

        Platform.runLater(() -> {
            for (int i = 0; i < series.getData().size(); i++) {
                XYChart.Data<String, Number> data = series.getData().get(i);
                Node bar = data.getNode();
                if (bar != null) {
                    bar.setStyle("-fx-bar-fill: " + colors[i % colors.length] + "; -fx-background-radius: 4px 4px 0 0;");
                    attachValueLabel(data);
                }
            }
        });
    }

    private void attachValueLabel(XYChart.Data<String, Number> data) {
        Node bar = data.getNode();
        Label lbl = new Label(String.valueOf(data.getYValue().intValue()));
        lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        lbl.setManaged(false);

        Runnable position = () -> {
            if (bar.getParent() instanceof Group) {
                Group group = (Group) bar.getParent();
                if (!group.getChildren().contains(lbl)) {
                    group.getChildren().add(lbl);
                }
                lbl.autosize();
                var b = bar.getBoundsInParent();
                lbl.relocate(
                    Math.round(b.getMinX() + (b.getWidth() - lbl.getWidth()) / 2.0),
                    Math.round(b.getMinY() - lbl.getHeight() - 3.0)
                );
            }
        };

        bar.parentProperty().addListener((obs, o, n) -> position.run());
        bar.boundsInParentProperty().addListener((obs, o, n) -> position.run());
        position.run();
    }

    private void initRiskTable() {
        if (riskTable == null) return;

        riskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colRollNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().rollNo));
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name));
        colSubject.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().subject));
        colGpa.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().gpa));
        colAttendance.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().attendance));
        colRiskLevel.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().risk));

        colRiskLevel.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    String style = item.equals("High")
                        ? "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;"
                        : "-fx-background-color: #fef3c7; -fx-text-fill: #d97706;";
                    badge.setStyle(style + " -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2px 8px; -fx-background-radius: 10px;");
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        riskData.addAll(
            new RiskStudent("B-CEIT-123", "Aung Thant", "IT201 · DSA", "1.76", "62%", "High"),
            new RiskStudent("B-CEIT-107", "May Zin", "IT202 · DBMS", "1.85", "68%", "High"),
            new RiskStudent("B-CEIT-039", "Ko Thant", "IT305 · OS", "2.10", "69%", "Medium"),
            new RiskStudent("B-CEIT-075", "Aung Thura", "IT203 · CN", "1.95", "72%", "Medium")
        );

        FilteredList<RiskStudent> filtered = new FilteredList<>(riskData, p -> true);
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                String q = newVal == null ? "" : newVal.trim().toLowerCase();
                filtered.setPredicate(s -> q.isEmpty()
                    || s.rollNo.toLowerCase().contains(q)
                    || s.name.toLowerCase().contains(q)
                    || s.subject.toLowerCase().contains(q));
                if (riskCountLabel != null) {
                    riskCountLabel.setText(filtered.size() + " students flagged");
                }
            });
        }

        riskTable.setItems(filtered);
    }

    public static class RiskStudent {
        public final String rollNo, name, subject, gpa, attendance, risk;
        public RiskStudent(String rollNo, String name, String subject, String gpa, String attendance, String risk) {
            this.rollNo = rollNo;
            this.name = name;
            this.subject = subject;
            this.gpa = gpa;
            this.attendance = attendance;
            this.risk = risk;
        }
    }
}