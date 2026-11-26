package Student;

import application.ViewNavigationHistory;
import application.dto.Student;
import application.dto.StudentProfile;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.function.Consumer;

public class StudentDetailController {

    @FXML private VBox rootPane;
    @FXML private Button viewAttendanceBtn;
    @FXML private ImageView passportImageView;
    @FXML private Label photoPlaceholderLabel;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderBanner;

    @FXML private Label lblRollNo, lblDob, lblName, lblGender, lblMajor, lblNationality, lblYear, lblSemester, lblNrc;
    @FXML private Label lblPhone, lblEmail, lblCurrentAddress, lblPermanentAddress;
    @FXML private Label lblFatherName, lblFatherOcc, lblFatherPhone, lblFatherEmail;
    @FXML private Label lblMotherName, lblMotherOcc, lblMotherPhone, lblMotherEmail;
    @FXML private Label lblPrevSchool, lblYearCompleted, lblAdditionalInfo;

    private Student currentStudent;
    private Consumer<Student> onJumpToAttendanceCallback;

    public void initData(Student student, StudentProfile profile, Consumer<Student> onJumpToAttendance) {
        this.currentStudent = student;
        this.onJumpToAttendanceCallback = onJumpToAttendance;

        if (viewAttendanceBtn != null) {
            boolean canJump = onJumpToAttendance != null;
            viewAttendanceBtn.setVisible(canJump);
            viewAttendanceBtn.setManaged(canJump);
        }

        if (student == null) return;

        String name = safe(student.getName(), "Student Profile");
        String roll = safe(student.getRollNo(), "-");
        String major = safe(student.getMajor(), "-");
        String year = safe(student.getYear(), "-");
        String standing = profile != null ? safe(profile.getAcademicStanding(), safe(student.getSemester(), "-")) : safe(student.getSemester(), "-");
        String email = profile != null ? safe(profile.getEmail(), safe(student.getEmail(), "-")) : safe(student.getEmail(), "-");
        String address = profile != null ? safe(profile.getAddress(), safe(student.getCurrentAddress(), "-")) : safe(student.getCurrentAddress(), "-");
        String photo = profile != null && profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isBlank()
                ? profile.getProfileImageUrl()
                : student.getPhotoPath();

        if (lblHeaderName != null) lblHeaderName.setText(name);
        if (lblHeaderBanner != null) {
            lblHeaderBanner.setText(String.format(
                    "No. %d   |   %s   |   %s   |   %s   |   Standing: %s",
                    student.getDisplayNo(), roll, major, year, standing));
        }

        setText(lblRollNo, student.getRollNo(), "-");
        setText(lblDob, student.getDob(), "-");
        setText(lblName, student.getName(), "-");
        setText(lblGender, student.getGender(), "-");
        setText(lblMajor, major, "-");
        setText(lblNationality, student.getNationality(), "Myanmar");
        setText(lblYear, student.getYear(), "-");
        setText(lblSemester, standing, "-");
        setText(lblNrc, student.getNrc(), "-");

        setText(lblPhone, student.getPhoneNo(), "-");
        setText(lblEmail, email, "-");
        setText(lblCurrentAddress, address, "-");
        setText(lblPermanentAddress, student.getPermanentAddress(), "-");

        setText(lblFatherName, student.getFatherName(), "-");
        setText(lblFatherOcc, student.getFatherOccupation(), "-");
        setText(lblFatherPhone, student.getFatherPhone(), "-");
        setText(lblFatherEmail, student.getFatherEmail(), "-");

        setText(lblMotherName, student.getMotherName(), "-");
        setText(lblMotherOcc, student.getMotherOccupation(), "-");
        setText(lblMotherPhone, student.getMotherPhone(), "-");
        setText(lblMotherEmail, student.getMotherEmail(), "-");

        setText(lblPrevSchool, student.getPrevSchool(), "-");
        setText(lblYearCompleted, student.getYearCompleted(), "-");
        setText(lblAdditionalInfo, student.getAdditionalInfo(), "-");

        loadPhoto(photo);
    }

    private void loadPhoto(String path) {
        if (passportImageView == null) return;
        Image loaded = null;
        if (path != null && !path.isBlank() && !"null".equalsIgnoreCase(path.trim())) {
            try {
                String clean = path.trim();
                if (clean.startsWith("file:") || clean.startsWith("http:") || clean.startsWith("https:")) {
                    loaded = new Image(clean, 82, 106, false, true);
                } else {
                    File f = new File(clean);
                    if (f.exists()) {
                        loaded = new Image(f.toURI().toString(), 82, 106, false, true);
                    } else if (getClass().getResourceAsStream(clean) != null) {
                        loaded = new Image(getClass().getResourceAsStream(clean));
                    }
                }
            } catch (Exception ignored) {
            }
        }

        boolean hasImg = loaded != null && !loaded.isError();
        passportImageView.setImage(hasImg ? loaded : null);
        passportImageView.setVisible(hasImg);
        passportImageView.setManaged(hasImg);
        if (photoPlaceholderLabel != null) {
            photoPlaceholderLabel.setVisible(!hasImg);
            photoPlaceholderLabel.setManaged(!hasImg);
        }
    }

    @FXML
    public void handleViewAttendance() {
        if (currentStudent != null && onJumpToAttendanceCallback != null) {
            onJumpToAttendanceCallback.accept(currentStudent);
        }
    }

    @FXML
    public void handleBack() {
        StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
        ViewNavigationHistory.goBack(contentArea);
    }

    private void setText(Label lbl, String value, String fallback) {
        if (lbl != null) {
            lbl.setText(safe(value, fallback));
        }
    }

    private String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return fallback;
        }
        return value.trim();
    }
}