package application;

import application.ClassesController.ClassItem;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class DashboardController {
	
	private String loggedInLecturer;
	@FXML private Label profileNameLabel;
    @FXML
    private BarChart<String, Number> performanceChart;

    @FXML
    private CategoryAxis xAxisPerformance;

    @FXML
    private NumberAxis yAxisPerformance;

    @FXML
    private VBox myCoursesContainer;
    
    @FXML private Label welcomeTitleLabel;
    @FXML private Label departmentBadgeLabel;
    
    
    
    @FXML
    public void initialize() {
        initPerformanceChart();
        initMyCoursesBlocks();
        if (welcomeTitleLabel != null) {
            welcomeTitleLabel.setText("Good Morning, " + UserSession.lecturerName + " 👋");
        }
        if (departmentBadgeLabel != null) {
            departmentBadgeLabel.setText("🏛️ Department: " + UserSession.getFormattedDepartment());
        }
        if (profileNameLabel != null) {
            profileNameLabel.setText("Lecturer (" + UserSession.lecturerName + ")");
        }
        this.loggedInLecturer = UserSession.lecturerName;
    }

    private void initMyCoursesBlocks() {
        if (myCoursesContainer == null) return;

        // Populate shared class data safely without instantiating ClassesView UI nodes
        ClassesController.initSampleClassData();

        renderCourseBlocks();

        // Automatically refresh the 5 blocks whenever ClassesView data changes
        ClassesController.SHARED_CLASS_DATA.addListener((ListChangeListener<ClassItem>) change -> renderCourseBlocks());
    }

    private void renderCourseBlocks() {
        if (myCoursesContainer == null) return;
        myCoursesContainer.getChildren().clear();

        String activeDept = UserSession.departmentCode;
        String activeLecturer = UserSession.lecturerName;
        int count = 0;

        for (ClassItem item : ClassesController.SHARED_CLASS_DATA) {
            if (activeDept != null && !activeDept.isEmpty() && !item.major.equalsIgnoreCase(activeDept)) {
                continue;
            }
            String cleanLecturer = item.lecturer.replace(" (Assigned)", "").trim();
            if (activeLecturer != null && !activeLecturer.isEmpty()
                    && !cleanLecturer.equalsIgnoreCase(activeLecturer.trim())) {
                continue;
            }

            myCoursesContainer.getChildren().add(createCourseBlock(item));
            count++;
            if (count >= 5) break;
        }
    }

    private HBox createCourseBlock(ClassItem item) {
        HBox block = new HBox(10.0);
        block.setAlignment(Pos.CENTER_LEFT);
        block.setStyle(
            "-fx-background-color: #ffffff; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-color: #e2e8f0; " +
            "-fx-border-radius: 10px; " +
            "-fx-padding: 8px 10px; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.03), 4, 0, 0, 1);"
        );

        // Colored rounded square icon matching ClassesView
        StackPane iconPane = new StackPane();
        iconPane.setStyle(
            "-fx-background-color: " + item.badgeColor + "; " +
            "-fx-background-radius: 8px; " +
            "-fx-min-width: 34px; -fx-min-height: 34px; " +
            "-fx-max-width: 34px; -fx-max-height: 34px;"
        );
        Label iconLabel = new Label(item.icon);
        iconLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        iconPane.getChildren().add(iconLabel);

        // Course code, title, and section/student metadata
        VBox infoBox = new VBox(1.5);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label codeLabel = new Label(item.code);
        codeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11.5px; -fx-text-fill: #0f172a;");

        Label titleLabel = new Label(item.title);
        titleLabel.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #475569;");

        // Extract short section (e.g., "2A" from "Year 2 - Class 2A") and capitalize Students
        String shortSection = item.className.contains("Class ")
                ? item.className.substring(item.className.indexOf("Class ") + 6)
                : item.className;
        String formattedStudents = item.studentCount.replace("students", "Students");

        Label metaLabel = new Label("🎓 " + shortSection + "  |  👥 " + formattedStudents);
        metaLabel.setStyle("-fx-font-size: 9.5px; -fx-text-fill: #64748b;");

        infoBox.getChildren().addAll(codeLabel, titleLabel, metaLabel);

        Label dotsLabel = new Label("⋮");
        dotsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        block.getChildren().addAll(iconPane, infoBox, dotsLabel);
        return block;
    }

    @FXML
    private void handleViewAllCourses(MouseEvent event) {
        Node source = (Node) event.getSource();
        Scene scene = source.getScene();
        if (scene == null) return;

        // Trigger the sidebar's Classes navigation button (#btnClasses) so active tab styling updates cleanly
        Node classesTabBtn = scene.lookup("#btnClasses");
        if (classesTabBtn instanceof Button) {
            ((Button) classesTabBtn).fire();
        }
    }

    private void initPerformanceChart() {
        if (performanceChart == null) return;

        if (yAxisPerformance != null) {
            yAxisPerformance.setAutoRanging(false);
            yAxisPerformance.setLowerBound(0.0);
            yAxisPerformance.setUpperBound(4.5);
            yAxisPerformance.setTickUnit(0.5);
            yAxisPerformance.setMinorTickVisible(false);
        }

        performanceChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("IT201", 3.68));
        series.getData().add(new XYChart.Data<>("IT203", 3.52));
        series.getData().add(new XYChart.Data<>("IT305", 3.61));
        series.getData().add(new XYChart.Data<>("IT321", 3.47));

        performanceChart.getData().add(series);

        String[] barColors = { "#5b7cfa", "#7c96fc", "#6c88fb", "#857ef8" };

        Platform.runLater(() -> {
            for (int i = 0; i < series.getData().size(); i++) {
                XYChart.Data<String, Number> data = series.getData().get(i);
                Node node = data.getNode();
                if (node != null) {
                    node.setStyle("-fx-bar-fill: " + barColors[i % barColors.length] + "; -fx-background-radius: 4px 4px 0 0;");
                    attachValueLabel(data);
                }
            }
            performanceChart.requestLayout();
        });
    }

    private void attachValueLabel(XYChart.Data<String, Number> data) {
        Node bar = data.getNode();
        Label valueLabel = new Label(String.format("%.2f", data.getYValue().doubleValue()));
        valueLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        valueLabel.setManaged(false);

        Runnable positionLabel = () -> {
            if (bar.getParent() instanceof Group) {
                Group plotGroup = (Group) bar.getParent();
                if (!plotGroup.getChildren().contains(valueLabel)) {
                    plotGroup.getChildren().add(valueLabel);
                }
                valueLabel.autosize();
                var bounds = bar.getBoundsInParent();
                valueLabel.relocate(
                    Math.round(bounds.getMinX() + (bounds.getWidth() - valueLabel.getWidth()) / 2.0),
                    Math.round(bounds.getMinY() - valueLabel.getHeight() - 3.0)
                );
            }
        };

        bar.parentProperty().addListener((obs, oldP, newP) -> positionLabel.run());
        bar.boundsInParentProperty().addListener((obs, oldB, newB) -> positionLabel.run());
        positionLabel.run();
    }
}