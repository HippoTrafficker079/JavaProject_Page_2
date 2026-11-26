package Student;

import application.dto.AttendanceRecord;
import application.dto.StudentProfile;
import application.UserSession;
import application.ViewNavigationHistory;
import application.dao.AttendanceDAO;
import application.dao.DatabaseConnector;
import application.dao.DepartmentDAO;
import application.dto.Student;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class StudentsController {

    @FXML private VBox rootPane;
    @FXML private StackPane profileIconBg;
    @FXML private Label profileIconLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label deptBadgeLabel;

    @FXML private ComboBox<String> yearFilterCombo;
    @FXML private Label secondaryFilterLabel;
    @FXML private ComboBox<String> secondaryFilterCombo;
    @FXML private TextField searchField;

    @FXML private Label studentCountBadge;
    @FXML private Label footerCountLabel;

    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student, Integer> colNo;
    @FXML private TableColumn<Student, String> colRollNo;
    @FXML private TableColumn<Student, String> colName;
    @FXML private TableColumn<Student, String> colMajor;
    @FXML private TableColumn<Student, String> colYear;
    @FXML private TableColumn<Student, String> colPhone;

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final ObservableList<Student> studentRows = FXCollections.observableArrayList();

    private boolean isAdminMode = true;
    private int resolvedDeptId = 1;
    private Consumer<Student> onJumpToAttendanceCallback;

    @FXML
    public void initialize() {
        setupOutsideClickDeselect();
        setupTable();
    }

    public void initRole(boolean isAdmin, Consumer<Student> onJumpToAttendance) {
        this.isAdminMode = isAdmin;
        this.onJumpToAttendanceCallback = onJumpToAttendance;
        this.resolvedDeptId = resolveCurrentDeptId();

        configureRoleHeader();
        initFilters();
        refreshAll();
    }

    public void refreshAll() {
        loadStudents();
    }

    private void configureRoleHeader() {
        if (deptBadgeLabel != null) {
            deptBadgeLabel.setText(UserSession.getFormattedDepartment());
        }
        if (isAdminMode) {
            if (profileNameLabel != null) profileNameLabel.setText("System Administrator");
            if (profileIconLabel != null) {
                profileIconLabel.setText("⚙");
                profileIconLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #9333ea;");
            }
            if (profileIconBg != null) {
                profileIconBg.setStyle("-fx-background-color: #f3e8ff; -fx-background-radius: 12px; -fx-min-width: 24px; -fx-min-height: 24px;");
            }
            if (pageTitleLabel != null) pageTitleLabel.setText("Department Students (Admin)");
            if (pageSubtitleLabel != null) {
                pageSubtitleLabel.setText("View students in your department, inspect full profiles, and open individual attendance records.");
            }
            
            if (secondaryFilterLabel != null) secondaryFilterLabel.setText("Major / Dept");
        } else {
            if (profileNameLabel != null) profileNameLabel.setText("Lecturer (" + UserSession.lecturerName + ")");
            if (profileIconLabel != null) {
                profileIconLabel.setText("👨‍🏫");
                profileIconLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2563eb;");
            }
            if (profileIconBg != null) {
                profileIconBg.setStyle("-fx-background-color: #dbeafe; -fx-background-radius: 12px; -fx-min-width: 24px; -fx-min-height: 24px;");
            }
            if (pageTitleLabel != null) pageTitleLabel.setText("My Students");
            if (pageSubtitleLabel != null) {
                pageSubtitleLabel.setText("View students enrolled in your classes, inspect profiles, and check attendance.");
            }
           
            if (secondaryFilterLabel != null) secondaryFilterLabel.setText("Subject");
        }
    }

    private void initFilters() {
        if (yearFilterCombo != null) {
            yearFilterCombo.setItems(FXCollections.observableArrayList(
                    "All Years", "First year", "Second year", "Third year", "Fourth year", "Fifth year"));
            yearFilterCombo.setValue("All Years");
            yearFilterCombo.valueProperty().addListener((obs, o, n) -> loadStudents());
        }

        if (secondaryFilterCombo != null) {
            if (isAdminMode) {
                secondaryFilterCombo.setItems(FXCollections.observableArrayList(
                        "All Majors", "CEIT", "Mech", "EC", "EP", "McE", "ChE", "Arch", "C", "FE", "PE", "Mn", "Tele", "Met", "Tex"));
                secondaryFilterCombo.setValue("All Majors");
            } else {
                ObservableList<String> subjects = FXCollections.observableArrayList("All Subjects");
                subjects.addAll(fetchLecturerSubjects());
                secondaryFilterCombo.setItems(subjects);
                secondaryFilterCombo.setValue("All Subjects");
            }
            secondaryFilterCombo.valueProperty().addListener((obs, o, n) -> loadStudents());
        }

        if (searchField != null) {
            searchField.setOnAction(e -> loadStudents());
        }
    }

    private void setupTable() {
        if (studentTable == null) return;

        colNo.setCellValueFactory(
                cell -> new ReadOnlyObjectWrapper<>(studentTable.getItems().indexOf(cell.getValue()) + 1));
        colRollNo.setCellValueFactory(cell -> cell.getValue().rollNoProperty());
        colName.setCellValueFactory(cell -> cell.getValue().nameProperty());
        colMajor.setCellValueFactory(cell -> cell.getValue().majorProperty());
        colYear.setCellValueFactory(cell -> cell.getValue().yearProperty());
        colPhone.setCellValueFactory(cell -> cell.getValue().phoneNoProperty());

        studentTable.setItems(studentRows);
        studentTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && studentTable.getSelectionModel().getSelectedItem() != null) {
                openStudentProfile(studentTable.getSelectionModel().getSelectedItem());
            }
        });

        lockTableColumns(studentTable, 0.06, 0.15, 0.27, 0.12, 0.16, 0.236);
    }

    private void loadStudents() {
        String search = (searchField != null && searchField.getText() != null) ? searchField.getText().trim() : "";
        String year = yearFilterCombo != null ? yearFilterCombo.getValue() : "All Years";
        String secondary = secondaryFilterCombo != null ? secondaryFilterCombo.getValue() : "";

        String deptParam = isAdminMode
                ? ("All Majors".equalsIgnoreCase(secondary) ? "" : secondary)
                : "";
        String yearParam = "All Years".equalsIgnoreCase(year) ? "" : year;

        List<Student> rawList = attendanceDAO.studentListForSearchBar(search, deptParam, yearParam);
        List<Student> scoped = filterByRoleScope(rawList, isAdminMode ? "" : secondary);

        studentRows.setAll(scoped);
        int count = studentRows.size();
        if (studentCountBadge != null) {
            studentCountBadge.setText(count + (count == 1 ? " student" : " students"));
        }
        if (footerCountLabel != null) {
            footerCountLabel.setText("Showing " + count + (count == 1 ? " student" : " students"));
        }
    }

    private List<Student> filterByRoleScope(List<Student> candidates, String subjectFilter) {
        if (candidates == null) return List.of();
        if (isAdminMode) {
            Set<Integer> deptStudentIds = fetchStudentIdsForDept(resolvedDeptId);
            List<Student> out = new ArrayList<>();
            for (Student s : candidates) {
                if (deptStudentIds.contains(s.getNo())) {
                    out.add(s);
                }
            }
            return out;
        } else {
            Set<Integer> lecturerStudentIds = fetchStudentIdsForLecturer(UserSession.userId, subjectFilter);
            List<Student> out = new ArrayList<>();
            for (Student s : candidates) {
                if (lecturerStudentIds.contains(s.getNo())) {
                    out.add(s);
                }
            }
            return out;
        }
    }

    private void loadMetrics() {
        int totalCount = studentRows.size();
        int goodCount = 0;
        int atRiskCount = 0;

        String deptArg = isAdminMode ? UserSession.departmentCode : "";
        List<AttendanceRecord> matrix = attendanceDAO.AtteandanceMatrixSummary(
                "", deptArg, "", "", null, LocalDate.now(), false);

        for (AttendanceRecord r : matrix) {
            boolean inCurrentRows = studentRows.stream().anyMatch(st -> st.getNo() == r.getNo());
            if (!inCurrentRows) continue;
            if (r.isLowAttendance()) atRiskCount++; else goodCount++;
        }

    }

    @FXML
    public void handleSearch() {
        loadStudents();
        loadMetrics();
    }

    @FXML
    public void handleRefresh() {
        if (!isAdminMode && secondaryFilterCombo != null) {
            String prev = secondaryFilterCombo.getValue();
            ObservableList<String> subjects = FXCollections.observableArrayList("All Subjects");
            subjects.addAll(fetchLecturerSubjects());
            secondaryFilterCombo.setItems(subjects);
            secondaryFilterCombo.setValue(subjects.contains(prev) ? prev : "All Subjects");
        }
        refreshAll();
    }

    @FXML
    public void handleReset() {
        if (yearFilterCombo != null) yearFilterCombo.setValue("All Years");
        if (secondaryFilterCombo != null) {
            secondaryFilterCombo.setValue(isAdminMode ? "All Majors" : "All Subjects");
        }
        if (searchField != null) searchField.clear();
        if (studentTable != null) studentTable.getSelectionModel().clearSelection();
        refreshAll();
    }

    @FXML
    public void handleViewProfile() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a student row to view their full profile.", Alert.AlertType.WARNING);
            return;
        }
        openStudentProfile(selected);
    }

    @FXML
    public void handleViewAttendance() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a student row to inspect their Attendance Dashboard.", Alert.AlertType.WARNING);
            return;
        }
        if (onJumpToAttendanceCallback != null) {
            onJumpToAttendanceCallback.accept(selected);
        }
    }

    private void openStudentProfile(Student selected) {
        int uiRowNo = studentTable.getItems().indexOf(selected) + 1;
        StudentProfile fullProfile = attendanceDAO.getStudentProfile(selected.getNo());
        Student target = selected;
        target.setDisplayNo(uiRowNo > 0 ? uiRowNo : selected.getNo());

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("StudentDetailView.fxml"));
            Parent detailView = loader.load();

            StudentDetailController controller = loader.getController();
            if (controller != null) {
                controller.initData(target, fullProfile, onJumpToAttendanceCallback);
            }

            StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
            if (contentArea != null) {
                ViewNavigationHistory.navigateForward(contentArea, detailView);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Loading Error", "Failed to load StudentDetailView.fxml:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private Set<Integer> fetchStudentIdsForDept(int deptId) {
        Set<Integer> ids = new LinkedHashSet<>();
        String sql = """
                SELECT s.student_id
                FROM student s
                JOIN user_account ua ON s.user_id = ua.user_id
                WHERE ua.status = TRUE
                  AND s.dept_id = ?
                """;
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("student_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ids;
    }

    private Set<Integer> fetchStudentIdsForLecturer(int teacherUserId, String subjectFilter) {
        Set<Integer> ids = new LinkedHashSet<>();
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT s.student_id
                FROM student s
                JOIN user_account ua ON s.user_id = ua.user_id
                JOIN course_enrollment ce ON s.student_id = ce.student_id AND ce.status = 'ENROLLED'
                JOIN course_offering co ON ce.offering_id = co.offering_id
                JOIN course c ON co.course_id = c.course_id
                JOIN teacher t ON co.teacher_id = t.teacher_id
                WHERE ua.status = TRUE
                  AND t.user_id = ?
                """);
        boolean filterBySubj = subjectFilter != null && !subjectFilter.isBlank() && !"All Subjects".equalsIgnoreCase(subjectFilter);
        if (filterBySubj) {
            sql.append(" AND LOWER(TRIM(c.course_name)) = LOWER(TRIM(?)) ");
        }
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, teacherUserId);
            if (filterBySubj) ps.setString(2, subjectFilter.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("student_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ids;
    }

    private List<String> fetchLecturerSubjects() {
        List<String> list = new ArrayList<>();
        String sql = """
                SELECT DISTINCT c.course_name
                FROM course c
                JOIN course_offering co ON c.course_id = co.course_id
                JOIN teacher t ON co.teacher_id = t.teacher_id
                WHERE c.is_active = TRUE
                  AND t.user_id = ?
                ORDER BY c.course_name ASC
                """;
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, UserSession.userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("course_name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private int resolveCurrentDeptId() {
        DepartmentDAO deptDAO = new DepartmentDAO();
        try {
            return deptDAO.getDeptIdByUserId(UserSession.userId);
        } catch (Exception ignored) {
        }
        try (Connection conn = DatabaseConnector.getConnection()) {
            return deptDAO.getDeptIdByNameAndDept(conn, UserSession.departmentFullName, UserSession.departmentCode);
        } catch (Exception ignored) {
        }
        return 1;
    }

    private void setupOutsideClickDeselect() {
        if (rootPane == null) return;
        rootPane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            Node target = (event.getTarget() instanceof Node) ? (Node) event.getTarget() : null;
            if (isInsideNodeType(target, TableView.class) || isInsideNodeType(target, ButtonBase.class)) {
                return;
            }
            if (studentTable != null) {
                studentTable.getSelectionModel().clearSelection();
            }
            rootPane.requestFocus();
        });
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

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}