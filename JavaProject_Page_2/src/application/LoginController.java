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

    private static final String ADMIN_EMAIL = "admin.master@ytu.edu.mm";
    private static final String ADMIN_PASSWORD = "ytu@admin123";

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
            eyeIconLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 15px; -fx-cursor: hand;");
            Platform.runLater(() -> {
                visiblePasswordField.requestFocus();
                visiblePasswordField.deselect();
                visiblePasswordField.positionCaret(visiblePasswordField.getText().length());
            });
        } else {
            passwordField.setText(visiblePasswordField.getText());
            passwordField.setVisible(true);
            visiblePasswordField.setVisible(false);
            eyeIconLabel.setText("👁");
            eyeIconLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px; -fx-cursor: hand;");
            Platform.runLater(() -> {
                passwordField.requestFocus();
                passwordField.deselect();
                passwordField.positionCaret(passwordField.getText().length());
            });
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
            confirmEyeIconLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 15px; -fx-cursor: hand;");
            Platform.runLater(() -> {
                visibleConfirmPasswordField.requestFocus();
                visibleConfirmPasswordField.deselect();
                visibleConfirmPasswordField.positionCaret(visibleConfirmPasswordField.getText().length());
            });
        } else {
            confirmPasswordField.setText(visibleConfirmPasswordField.getText());
            confirmPasswordField.setVisible(true);
            visibleConfirmPasswordField.setVisible(false);
            confirmEyeIconLabel.setText("👁");
            confirmEyeIconLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px; -fx-cursor: hand;");
            Platform.runLater(() -> {
                confirmPasswordField.requestFocus();
                confirmPasswordField.deselect();
                confirmPasswordField.positionCaret(confirmPasswordField.getText().length());
            });
        }
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String user = (usernameField != null && usernameField.getText() != null)
                ? usernameField.getText().trim()
                : "";
        String email = (emailField != null && emailField.getText() != null)
                ? emailField.getText().trim()
                : "";
        String pass = isPasswordVisible
                ? (visiblePasswordField != null ? visiblePasswordField.getText() : "")
                : (passwordField != null ? passwordField.getText() : "");
        String confirmPass = isConfirmPasswordVisible
                ? (visibleConfirmPasswordField != null ? visibleConfirmPasswordField.getText() : "")
                : (confirmPasswordField != null ? confirmPasswordField.getText() : "");

        if (confirmPass != null && !confirmPass.isEmpty() && !confirmPass.equals(pass)) {
            showWarning("Password Mismatch", "Password and Confirm Password do not match.");
            return;
        }

        if (!email.isEmpty() && !email.toLowerCase().endsWith("@ytu.edu.mm")) {
            showWarning("Invalid YTU Email", "Please use an official YTU email ending with @ytu.edu.mm.");
            return;
        }

        boolean hasAnyInput = !user.isEmpty() || !email.isEmpty() || !pass.isEmpty();

        if ("Admin".equals(selectedRole)) {
            if (hasAnyInput) {
                boolean emailMatches = email.isEmpty() || email.equalsIgnoreCase(ADMIN_EMAIL);
                boolean passMatches = pass.isEmpty() || pass.equals(ADMIN_PASSWORD);
                if (!emailMatches || !passMatches) {
                    showWarning("Invalid Admin Credentials",
                            "Use " + ADMIN_EMAIL + " and your assigned Admin password (or leave fields blank for quick demo login).");
                    return;
                }
            }
            assignAdminDepartmentOnly(user, email);
        } else {
            if (hasAnyInput) {
                TeacherDirectoryStore.TeacherAccount matched = TeacherDirectoryStore.findByCredentials(
                        !email.isEmpty() ? email : user
                );

                if (matched != null) {
                    if (!email.isEmpty() && !matched.email.equalsIgnoreCase(email)) {
                        showWarning("Email Mismatch",
                                "Expected YTU email for " + matched.fullName + ": " + matched.email);
                        return;
                    }
                    if (!pass.isEmpty() && !matched.password.equals(pass)) {
                        showWarning("Incorrect Password",
                                "The password entered for " + matched.fullName + " does not match.");
                        return;
                    }
                    UserSession.lecturerName = matched.fullName;
                    UserSession.setDepartmentByCode(matched.deptCode);
                } else {
                    assignSessionByUsername(user, email);
                }
            } else {
                UserSession.lecturerName = "Dr. Hla Myint";
                UserSession.setDepartmentByCode("CEIT");
            }
        }

        // TODO (DB Integration): Authenticate against user_account + teacher + department tables
        // and populate UserSession.lecturerName and UserSession.setDepartmentByCode(deptCode).

        try {
            String targetFxml = "Admin".equals(selectedRole) ? "AdminMainLayout.fxml" : "MainLayout.fxml";
            Stage stage = (Stage) usernameField.getScene().getWindow();

            boolean wasMaximized = stage.isMaximized();
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(targetFxml));
            Parent root = loader.load();

            Scene scene = new Scene(root, currentWidth > 0 ? currentWidth : 1024, currentHeight > 0 ? currentHeight : 700);
            TableSelectionHelper.installOnScene(scene);
            if (getClass().getResource("style.css") != null) {
                scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
            }

            stage.setScene(scene);
            stage.setTitle("YTU Student Information Management System - "
                    + ("Admin".equals(selectedRole) ? "System Administrator" : "Lecturer"));
            stage.show();

            if (wasMaximized) {
                Platform.runLater(() -> stage.setMaximized(true));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void assignSessionByUsername(String user, String email) {
        String combined = ((user != null ? user : "") + " " + (email != null ? email : "")).trim().toLowerCase();

        if (combined.contains("ceit")) {
            UserSession.lecturerName = "Dr. Hla Myint";
            UserSession.setDepartmentByCode("CEIT");
        } else if (combined.contains("mech")) {
            UserSession.lecturerName = "Tae Takemi";
            UserSession.setDepartmentByCode("Mech");
        } else if (combined.contains("mce")) {
            UserSession.lecturerName = "Dr. Myo Tint";
            UserSession.setDepartmentByCode("McE");
        } else if (combined.contains("che")) {
            UserSession.lecturerName = "Dr. Khin Marlar";
            UserSession.setDepartmentByCode("ChE");
        } else if (combined.contains("tex")) {
            UserSession.lecturerName = "Daw Thiri Nandar";
            UserSession.setDepartmentByCode("Tex");
        } else if (combined.contains("tele")) {
            UserSession.lecturerName = "U Zaw Min";
            UserSession.setDepartmentByCode("Tele");
        } else if (combined.contains("met")) {
            UserSession.lecturerName = "Dr. Aye Aye Mon";
            UserSession.setDepartmentByCode("Met");
        } else if (combined.contains("arch")) {
            UserSession.lecturerName = "U Kyaw Swar";
            UserSession.setDepartmentByCode("Arch");
        } else if (combined.contains("ep")) {
            UserSession.lecturerName = "Prof. Kyaw Zin";
            UserSession.setDepartmentByCode("EP");
        } else if (combined.contains("ec")) {
            UserSession.lecturerName = "Dr. Tin Maung";
            UserSession.setDepartmentByCode("EC");
        } else if (combined.contains("mn")) {
            UserSession.lecturerName = "U Than Htike";
            UserSession.setDepartmentByCode("Mn");
        } else if (combined.contains("pe")) {
            UserSession.lecturerName = "Dr. Min Thu";
            UserSession.setDepartmentByCode("PE");
        } else if (combined.contains("fe")) {
            UserSession.lecturerName = "Daw Ei Ei Phyo";
            UserSession.setDepartmentByCode("FE");
        } else if (combined.equals("c") || combined.startsWith("c_") || combined.contains("civil")) {
            UserSession.lecturerName = "Dr. Aung Naing";
            UserSession.setDepartmentByCode("C");
        } else {
            UserSession.lecturerName = "Dr. Hla Myint";
            UserSession.setDepartmentByCode("CEIT");
        }
    }
    
    private void assignAdminDepartmentOnly(String user, String email) {
        String combined = ((user != null ? user : "") + " " + (email != null ? email : "")).trim().toLowerCase();
        if (combined.contains("mech")) UserSession.setDepartmentByCode("Mech");
        else if (combined.contains("mce")) UserSession.setDepartmentByCode("McE");
        else if (combined.contains("che")) UserSession.setDepartmentByCode("ChE");
        else if (combined.contains("tex")) UserSession.setDepartmentByCode("Tex");
        else if (combined.contains("tele")) UserSession.setDepartmentByCode("Tele");
        else if (combined.contains("met")) UserSession.setDepartmentByCode("Met");
        else if (combined.contains("arch")) UserSession.setDepartmentByCode("Arch");
        else if (combined.contains("ep")) UserSession.setDepartmentByCode("EP");
        else if (combined.contains("ec")) UserSession.setDepartmentByCode("EC");
        else if (combined.contains("mn")) UserSession.setDepartmentByCode("Mn");
        else if (combined.contains("pe")) UserSession.setDepartmentByCode("PE");
        else if (combined.contains("fe")) UserSession.setDepartmentByCode("FE");
        else if (combined.equals("c") || combined.startsWith("c_") || combined.contains("civil")) UserSession.setDepartmentByCode("C");
        else UserSession.setDepartmentByCode("CEIT");
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}