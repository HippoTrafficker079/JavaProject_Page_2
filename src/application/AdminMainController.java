package application;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class AdminMainController {

    @FXML private StackPane contentArea;
    @FXML private ImageView logoImageView;
    @FXML private Button btnDashboard;
    @FXML private Button btnClasses;
    @FXML private Button btnExams;

    private Node classesView;
    private Node dashboardView;
    @FXML
    public void initialize() {
        try {
            if (getClass().getResourceAsStream("ytulogo.png") != null) {
                Image logo = new Image(getClass().getResourceAsStream("ytulogo.png"));
                logoImageView.setImage(logo);
            }

            // Load Admin Classes View
            classesView = FXMLLoader.load(getClass().getResource("AdminClassesView.fxml"));

            if (classesView != null) {
                contentArea.getChildren().setAll(classesView);
                if (btnClasses != null) {
                    updateActiveButton(btnClasses);
                }
            }
            
         // Load Dashboard Classes View
            dashboardView = FXMLLoader.load(getClass().getResource("AdminDashboardView.fxml"));
            if (dashboardView != null) {
                contentArea.getChildren().setAll(dashboardView);
                if (btnDashboard != null) {
                    updateActiveButton(btnDashboard);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void switchTab(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        updateActiveButton(clickedButton);
        if (clickedButton == btnDashboard && dashboardView != null) {
            contentArea.getChildren().setAll(dashboardView);
        }
        else if (clickedButton == btnClasses && classesView != null) {
            contentArea.getChildren().setAll(classesView);
        }
    }

    private void updateActiveButton(Button activeBtn) {
        if (btnDashboard != null) btnDashboard.getStyleClass().remove("active");
        if (btnClasses != null) btnClasses.getStyleClass().remove("active");
        if (btnExams != null) btnExams.getStyleClass().remove("active");

        if (activeBtn != null && !activeBtn.getStyleClass().contains("active")) {
            activeBtn.getStyleClass().add("active");
        }
    }
}