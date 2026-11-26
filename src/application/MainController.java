package application;

import Attendance.AttendanceController;

import Student.StudentsController;
import application.dto.Student;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.layout.Region;
import javafx.util.Duration;

public class MainController {

    public static MainController instance;
    
    @FXML private Region sidebarIndicator;
    
    @FXML private StackPane contentArea;
    @FXML private ImageView logoImageView;

    @FXML private Button btnDashboard;
    @FXML private Button btnClasses;
    @FXML private Button btnExams;
    @FXML private Button btnStudents;
    @FXML private Button btnAttendance;

    private Node dashboardView;
    private Node classesView;
    private Node examsView;
    private Node studentsView;
    private Node attendanceView;

    private StudentsController studentsController;
    private AttendanceController attendanceController;

    @FXML
    public void initialize() {
        instance = this;
        try {
            if (getClass().getResourceAsStream("ytulogo.png") != null) {
                Image logo = new Image(getClass().getResourceAsStream("ytulogo.png"));
                logoImageView.setImage(logo);
            }

            dashboardView = FXMLLoader.load(getClass().getResource("DashboardView.fxml"));
            classesView = FXMLLoader.load(getClass().getResource("ClassesView.fxml"));
            examsView = FXMLLoader.load(getClass().getResource("ExamsView.fxml"));

            // Load Attendance Tab (Lecturer Mode)
            FXMLLoader attLoader = new FXMLLoader(getClass().getResource("/Attendance/AttendanceView.fxml"));
            attendanceView = attLoader.load();
            attendanceController = attLoader.getController();
            if (attendanceController != null) {
                attendanceController.initRole(false);
            }

            // Load Students Tab (Lecturer Mode) with deep-link to Attendance Dashboard
            FXMLLoader stdLoader = new FXMLLoader(getClass().getResource("/Student/StudentsView.fxml"));
            studentsView = stdLoader.load();
            studentsController = stdLoader.getController();
            if (studentsController != null) {
                studentsController.initRole(false, this::openStudentInAttendanceTab);
            }

            if (dashboardView != null) {
                contentArea.getChildren().setAll(dashboardView);
                updateActiveButton(btnDashboard);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openStudentInAttendanceTab(Student student) {
        if (attendanceView == null || attendanceController == null) return;
        ViewNavigationHistory.clear();
        updateActiveButton(btnAttendance);
        contentArea.getChildren().setAll(attendanceView);
        attendanceController.openIndividualStudent(student);
    }

    @FXML
    public void switchTab(ActionEvent event) {
        ViewNavigationHistory.clear();
        Button clickedButton = (Button) event.getSource();
        updateActiveButton(clickedButton);

        if (clickedButton == btnDashboard && dashboardView != null) {
            contentArea.getChildren().setAll(dashboardView);
        } else if (clickedButton == btnClasses && classesView != null) {
            contentArea.getChildren().setAll(classesView);
        } else if (clickedButton == btnExams && examsView != null) {
            contentArea.getChildren().setAll(examsView);
        } else if (clickedButton == btnStudents && studentsView != null) {
            if (studentsController != null) studentsController.refreshAll();
            contentArea.getChildren().setAll(studentsView);
        } else if (clickedButton == btnAttendance && attendanceView != null) {
            if (attendanceController != null) attendanceController.handleRefreshAll();
            contentArea.getChildren().setAll(attendanceView);
        }
    }

    @FXML
    public void showDashboardView(ActionEvent event) {
        ViewNavigationHistory.clear();
        if (dashboardView != null && contentArea != null) {
            contentArea.getChildren().setAll(dashboardView);
            updateActiveButton(btnDashboard);
        }
    }

    @FXML
    public void showClassesView(ActionEvent event) {
        ViewNavigationHistory.clear();
        if (classesView != null && contentArea != null) {
            contentArea.getChildren().setAll(classesView);
            updateActiveButton(btnClasses);
        }
    }

    @FXML
    public void showExamsView(ActionEvent event) {
        showExams();
    }

    public void showExams() {
        ViewNavigationHistory.clear();
        if (examsView != null && contentArea != null) {
            contentArea.getChildren().setAll(examsView);
            updateActiveButton(btnExams);
        }
    }

    public void loadCustomView(Node view) {
        if (contentArea != null && view != null) {
            contentArea.getChildren().setAll(view);
        }
    }

    private void updateActiveButton(Button activeBtn) {
        if (btnDashboard != null) btnDashboard.getStyleClass().remove("active");
        if (btnClasses != null) btnClasses.getStyleClass().remove("active");
        if (btnExams != null) btnExams.getStyleClass().remove("active");
        if (btnStudents != null) btnStudents.getStyleClass().remove("active");
        if (btnAttendance != null) btnAttendance.getStyleClass().remove("active");

        if (activeBtn != null && !activeBtn.getStyleClass().contains("active")) {
            activeBtn.getStyleClass().add("active");
        }
        moveSidebarIndicator(activeBtn);
    }
    
    private void moveSidebarIndicator(Button targetBtn) {
        if (sidebarIndicator == null || targetBtn == null) return;
        Platform.runLater(() -> {
            double targetY = targetBtn.getBoundsInParent().getMinY()
                    + (targetBtn.getHeight() - sidebarIndicator.getPrefHeight()) / 2.0;
            TranslateTransition tt = new TranslateTransition(Duration.millis(180), sidebarIndicator);
            tt.setToY(targetY);
            tt.play();
        });
    }
}