package application;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private ImageView logoImageView;
    @FXML private Button roleTeacherBtn;
    @FXML private Button roleAdminBtn;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;

    // Password & Eye Toggle
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private Label eyeIconLabel;

    // Confirm Password & Eye Toggle
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField visibleConfirmPasswordField;
    @FXML private Label confirmEyeIconLabel;

    private String selectedRole = "Teacher"; // Default
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @FXML
    public void initialize() {
        if (logoImageView != null && getClass().getResourceAsStream("ytulogo.png") != null) {
            logoImageView.setImage(new Image(getClass().getResourceAsStream("ytulogo.png")));
        }

        // Synchronize masked and unmasked Password fields
        if (passwordField != null && visiblePasswordField != null) {
            passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isPasswordVisible) {
                    visiblePasswordField.setText(newVal);
                }
            });

            visiblePasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (isPasswordVisible) {
                    passwordField.setText(newVal);
                }
            });
        }

        // Synchronize masked and unmasked Confirm Password fields
        if (confirmPasswordField != null && visibleConfirmPasswordField != null) {
            confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isConfirmPasswordVisible) {
                    visibleConfirmPasswordField.setText(newVal);
                }
            });

            visibleConfirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (isConfirmPasswordVisible) {
                    confirmPasswordField.setText(newVal);
                }
            });
        }
    }

    @FXML
    public void handleSelectTeacher(ActionEvent event) {
        selectedRole = "Teacher";
        roleTeacherBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 18px; -fx-padding: 8px; -fx-cursor: hand; -fx-font-size: 12px;");
        roleAdminBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-background-radius: 18px; -fx-padding: 8px; -fx-cursor: hand; -fx-font-size: 12px;");
    }

    @FXML
    public void handleSelectAdmin(ActionEvent event) {
        selectedRole = "Admin";
        roleAdminBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 18px; -fx-padding: 8px; -fx-cursor: hand; -fx-font-size: 12px;");
        roleTeacherBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-background-radius: 18px; -fx-padding: 8px; -fx-cursor: hand; -fx-font-size: 12px;");
    }

    @FXML
    public void togglePasswordVisibility(MouseEvent event) {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            visiblePasswordField.setText(passwordField.getText());
            visiblePasswordField.setVisible(true);
            passwordField.setVisible(false);
            eyeIconLabel.setText("👁‍🗨");
            eyeIconLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 0 0 0 6px;");
        } else {
            passwordField.setText(visiblePasswordField.getText());
            passwordField.setVisible(true);
            visiblePasswordField.setVisible(false);
            eyeIconLabel.setText("👁");
            eyeIconLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 0 0 0 6px;");
        }
    }

    @FXML
    public void toggleConfirmPasswordVisibility(MouseEvent event) {
        isConfirmPasswordVisible = !isConfirmPasswordVisible;
        if (isConfirmPasswordVisible) {
            visibleConfirmPasswordField.setText(confirmPasswordField.getText());
            visibleConfirmPasswordField.setVisible(true);
            confirmPasswordField.setVisible(false);
            confirmEyeIconLabel.setText("👁‍🗨");
            confirmEyeIconLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 0 0 0 6px;");
        } else {
            confirmPasswordField.setText(visibleConfirmPasswordField.getText());
            confirmPasswordField.setVisible(true);
            visibleConfirmPasswordField.setVisible(false);
            confirmEyeIconLabel.setText("👁");
            confirmEyeIconLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 0 0 0 6px;");
        }
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String pass = isPasswordVisible ? visiblePasswordField.getText() : passwordField.getText();
        String confirmPass = isConfirmPasswordVisible ? visibleConfirmPasswordField.getText() : confirmPasswordField.getText();

        // Validate matching passwords if Confirm Password was entered
        if (confirmPass != null && !confirmPass.isEmpty() && !confirmPass.equals(pass)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Password Mismatch");
            alert.setHeaderText(null);
            alert.setContentText("Password and Confirm Password do not match.");
            alert.showAndWait();
            return;
        }

        try {
            if ("Teacher".equals(selectedRole)) {
                assignLecturerSession();
            }

            String targetFxml = selectedRole.equals("Admin") ? "AdminMainLayout.fxml" : "MainLayout.fxml";

            Stage stage = (Stage) usernameField.getScene().getWindow();

            boolean wasMaximized = stage.isMaximized();
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(targetFxml));
            Parent root = loader.load();

            Scene scene = new Scene(root, currentWidth > 0 ? currentWidth : 1024, currentHeight > 0 ? currentHeight : 700);

            if (getClass().getResource("style.css") != null) {
                scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
            }

            stage.setScene(scene);
            stage.setTitle("YTU Student Information Management System - " + (selectedRole.equals("Admin") ? "System Administrator" : "Lecturer"));
            stage.show();

            if (wasMaximized) {
                Platform.runLater(() -> stage.setMaximized(true));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void assignLecturerSession() {
        String user = (usernameField != null && usernameField.getText() != null)
                ? usernameField.getText().trim().toLowerCase()
                : "";

        if (user.contains("ce") && !user.contains("ceit")) {
            UserSession.lecturerName = "Dr. Aung Naing";
            UserSession.setDepartmentByCode("CE");
        } else if (user.contains("me")) {
            UserSession.lecturerName = "Dr. Su Su";
            UserSession.setDepartmentByCode("ME");
        } else if (user.contains("ep")) {
            UserSession.lecturerName = "Prof. Kyaw Zin";
            UserSession.setDepartmentByCode("EP");
        } else if (user.contains("ec")) {
            UserSession.lecturerName = "Dr. Tin Maung";
            UserSession.setDepartmentByCode("EC");
        } else if (user.contains("mc")) {
            UserSession.lecturerName = "Dr. Myo Tint";
            UserSession.setDepartmentByCode("MC");
        } else {
            UserSession.lecturerName = "Dr. Hla Myint";
            UserSession.setDepartmentByCode("CEIT");
        }
    }
}