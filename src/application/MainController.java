package application;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class MainController {

    public static MainController instance;

    @FXML private StackPane contentArea;
    @FXML private ImageView logoImageView;

    @FXML private Button btnDashboard;
    @FXML private Button btnClasses;
    @FXML private Button btnManageStudent;
    @FXML private Button btnManageTeacher;
    @FXML private Button btnAttendance;
    @FXML private Button btnExams;

    private Node dashboardView;
    private Node classesView;
    private Node examsView;

    @FXML
    public void initialize() {
        instance = this;
        try {
            if (getClass().getResourceAsStream("ytulogo.png") != null) {
                Image logo = new Image(getClass().getResourceAsStream("ytulogo.png"));
                logoImageView.setImage(logo);
            }

            // Pre-load views for seamless navigation
            dashboardView = FXMLLoader.load(getClass().getResource("DashboardView.fxml"));
            classesView = FXMLLoader.load(getClass().getResource("ClassesView.fxml"));
            examsView = FXMLLoader.load(getClass().getResource("ExamsView.fxml"));

            // Set default view to Dashboard on startup
            if (dashboardView != null) {
                contentArea.getChildren().setAll(dashboardView);
                if (btnDashboard != null) btnDashboard.getStyleClass().add("active");
            } else if (examsView != null) {
                contentArea.getChildren().setAll(examsView);
                if (btnExams != null) btnExams.getStyleClass().add("active");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Unified tab switcher for all sidebar buttons
    @FXML
    public void switchTab(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        updateActiveButton(clickedButton);

        if (clickedButton == btnDashboard && dashboardView != null) {
            contentArea.getChildren().setAll(dashboardView);
        } else if (clickedButton == btnClasses && classesView != null) {
            contentArea.getChildren().setAll(classesView);
        } else if (clickedButton == btnExams && examsView != null) {
            contentArea.getChildren().setAll(examsView);
        }
    }

    @FXML
    public void showDashboardView(ActionEvent event) {
        if (dashboardView != null && contentArea != null) {
            contentArea.getChildren().setAll(dashboardView);
            updateActiveButton(btnDashboard);
        }
    }

    @FXML
    public void showClassesView(ActionEvent event) {
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
        if (examsView != null && contentArea != null) {
            contentArea.getChildren().setAll(examsView);
            updateActiveButton(btnExams);
        }
    }

    @FXML
    public void showStudentView(ActionEvent event) {
        updateActiveButton(btnManageStudent);
    }

    @FXML
    public void showTeacherView(ActionEvent event) {
        updateActiveButton(btnManageTeacher);
    }

    @FXML
    public void showAttendanceView(ActionEvent event) {
        updateActiveButton(btnAttendance);
    }

    // Allows any sub-controller (like ResultController) to push views into the main StackPane securely
    public void loadCustomView(Node view) {
        if (contentArea != null && view != null) {
            contentArea.getChildren().setAll(view);
        }
    }

    private void updateActiveButton(Button activeBtn) {
        if (btnDashboard != null) btnDashboard.getStyleClass().remove("active");
        if (btnClasses != null) btnClasses.getStyleClass().remove("active");
        if (btnManageStudent != null) btnManageStudent.getStyleClass().remove("active");
        if (btnManageTeacher != null) btnManageTeacher.getStyleClass().remove("active");
        if (btnAttendance != null) btnAttendance.getStyleClass().remove("active");
        if (btnExams != null) btnExams.getStyleClass().remove("active");

        if (activeBtn != null) {
            activeBtn.getStyleClass().add("active");
        }
    }
}