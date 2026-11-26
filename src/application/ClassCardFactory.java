package application;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassCardFactory {

    private static final Pattern SHARED_TIME_PATTERN =
            Pattern.compile("^([A-Za-z,\\s]+?)\\s+(\\d{1,2}:\\d{2}\\s*[-–]\\s*\\d{1,2}:\\d{2}.*)$");

    public static VBox createCard(
            ClassesController.ClassItem item,
            boolean isAdminView,
            EventHandler<ActionEvent> onPrimaryAction) {

        VBox card = new VBox(8.0);
        card.getStyleClass().add("panel-card");
        card.setMinWidth(0.0);
        card.setMinHeight(350.0);
        card.setPrefHeight(350.0);
        card.setMaxHeight(350.0);
        card.setPadding(new Insets(14.0, 16.0, 14.0, 16.0));

        // 1. HEADER: Fixed 56px height so 1-line and 2-line titles never shift layout
        HBox topHBox = new HBox(12.0);
        topHBox.setAlignment(Pos.TOP_LEFT);
        topHBox.setMinHeight(56.0);
        topHBox.setPrefHeight(56.0);
        topHBox.setMaxHeight(56.0);

        StackPane iconPane = new StackPane();
        iconPane.setMinSize(44.0, 44.0);
        iconPane.setMaxSize(44.0, 44.0);
        iconPane.setStyle("-fx-background-color: " + item.badgeColor + "; -fx-background-radius: 10px;");

        Label iconLabel = new Label(item.icon);
        iconLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        iconPane.getChildren().add(iconLabel);

        VBox titleBox = new VBox(2.0);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        titleBox.setMinWidth(0.0);

        Label codeLabel = new Label(item.code);
        codeLabel.setStyle("-fx-font-size: 16.5px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Label subLabel = new Label(item.title);
        subLabel.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 600; -fx-text-fill: #334155;");
        subLabel.setWrapText(true);
        subLabel.setAlignment(Pos.TOP_LEFT);
        subLabel.setMinHeight(34.0);
        subLabel.setPrefHeight(34.0);
        subLabel.setMaxHeight(34.0);

        titleBox.getChildren().addAll(codeLabel, subLabel);
        topHBox.getChildren().addAll(iconPane, titleBox);

        Separator divider = new Separator();
        divider.setStyle("-fx-opacity: 0.7;");

        // 2. DETAILS LIST (Department -> Year -> Students Below Year -> Lecturer)
        VBox detailsBox = new VBox(4.0);

        Label deptLabel = new Label("🏛  " + item.department);
        deptLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");

        Label yearLabel = new Label("🎓  " + formatYearOnly(item.year));
        yearLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");

        String formattedStudents = item.studentCount != null
                ? item.studentCount.replace("students", "Students")
                : "0 Students";
        Label stdLabel = new Label("👥  " + formattedStudents);
        stdLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");

        String lecturerText = isAdminView
                ? item.lecturer + (item.lecturer.contains("(Assigned)") ? "" : " (Assigned)")
                : item.lecturer.replace(" (Assigned)", "");
        Label lectLabel = new Label("👤  " + lecturerText);
        lectLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");

        detailsBox.getChildren().addAll(deptLabel, yearLabel, stdLabel, lectLabel);

        // 3. FIXED-HEIGHT TIMETABLE BOX (Fits 2 full pill rows; scrolls cleanly if 3+ rows)
        VBox timetableBox = new VBox(6.0);
        timetableBox.setMinWidth(0.0);
        timetableBox.setMinHeight(98.0);
        timetableBox.setPrefHeight(98.0);
        timetableBox.setMaxHeight(98.0);
        timetableBox.setStyle(
            "-fx-background-color: #f0f7ff; " +
            "-fx-background-radius: 8px; " +
            "-fx-border-color: #dbeafe; " +
            "-fx-border-radius: 8px; " +
            "-fx-padding: 8px 10px;"
        );

        Label ttHeader = new Label("🕒  TIMETABLE");
        ttHeader.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #1e40af;");

        FlowPane pillsPane = buildTimetablePills(item.schedule);
        pillsPane.setStyle("-fx-background-color: transparent;");

        ScrollPane pillsScroll = new ScrollPane(pillsPane);
        pillsScroll.setFitToWidth(true);
        pillsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pillsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        pillsScroll.setStyle(
            "-fx-background: transparent; " +
            "-fx-background-color: transparent; " +
            "-fx-padding: 0; " +
            "-fx-border-color: transparent;"
        );
        VBox.setVgrow(pillsScroll, Priority.ALWAYS);

        timetableBox.getChildren().addAll(ttHeader, pillsScroll);

        Region verticalSpacer = new Region();
        VBox.setVgrow(verticalSpacer, Priority.ALWAYS);

     // 4. ACTION BUTTON (Passes unique offeringKey so cross-major offerings stay separate)
        HBox btnHBox = new HBox(8.0);
        btnHBox.setAlignment(Pos.CENTER_RIGHT);
        btnHBox.setMinHeight(32.0);
        btnHBox.setPrefHeight(32.0);

        String offeringKey = item.getOfferingKey();

        Button primaryBtn = new Button(isAdminView ? "Manage Class" : "View Class");
        primaryBtn.getStyleClass().add("view-class-button");
        primaryBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6px 14px;");
        primaryBtn.setUserData(offeringKey);
        if (onPrimaryAction != null) {
            primaryBtn.setOnAction(onPrimaryAction);
        }
        btnHBox.getChildren().add(primaryBtn);

        card.getChildren().addAll(topHBox, divider, detailsBox, timetableBox, verticalSpacer, btnHBox);
        return card;
    
    }

    private static FlowPane buildTimetablePills(String schedule) {
        FlowPane flow = new FlowPane(5.0, 5.0);
        flow.setMinWidth(0.0);

        if (schedule == null || schedule.isBlank()) {
            flow.getChildren().add(createPill("TBA", "Schedule Pending", "#64748b"));
            return flow;
        }

        if (schedule.contains("|")) {
            for (String segment : schedule.split("\\|")) {
                String trimmed = segment.trim();
                if (trimmed.isEmpty()) continue;
                int spaceIdx = trimmed.indexOf(' ');
                if (spaceIdx > 0) {
                    String day = trimmed.substring(0, spaceIdx).trim().toUpperCase();
                    String time = trimmed.substring(spaceIdx + 1).trim();
                    flow.getChildren().add(createPill(day, time, getDayColor(day)));
                } else {
                    flow.getChildren().add(createPill("DAY", trimmed, "#2563eb"));
                }
            }
            return flow;
        }

        Matcher m = SHARED_TIME_PATTERN.matcher(schedule.trim());
        if (m.matches()) {
            String[] days = m.group(1).split(",");
            String timeSpan = m.group(2).trim();
            for (String rawDay : days) {
                String day = rawDay.trim().toUpperCase();
                if (!day.isEmpty()) {
                    flow.getChildren().add(createPill(day, timeSpan, getDayColor(day)));
                }
            }
            return flow;
        }

        String clean = schedule.split("·")[0].trim();
        for (String part : clean.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            int spaceIdx = trimmed.indexOf(' ');
            if (spaceIdx > 0) {
                String day = trimmed.substring(0, spaceIdx).trim().toUpperCase();
                String time = trimmed.substring(spaceIdx + 1).trim();
                flow.getChildren().add(createPill(day, time, getDayColor(day)));
            } else {
                flow.getChildren().add(createPill("TIME", trimmed, "#2563eb"));
            }
        }
        return flow;
    }

    private static HBox createPill(String day, String time, String bgColor) {
        HBox pill = new HBox(4.0);
        pill.setAlignment(Pos.CENTER_LEFT);
        pill.setStyle(
            "-fx-background-color: " + bgColor + "; " +
            "-fx-background-radius: 5px; " +
            "-fx-padding: 3px 7px;"
        );

        Label dayLbl = new Label(day);
        dayLbl.setStyle("-fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 10px;");

        Label timeLbl = new Label(time);
        timeLbl.setStyle("-fx-text-fill: rgba(255,255,255,0.95); -fx-font-weight: 600; -fx-font-size: 10px;");

        pill.getChildren().addAll(dayLbl, timeLbl);
        return pill;
    }

    private static String getDayColor(String day) {
        return switch (day.toUpperCase()) {
            case "MON" -> "#0284c7";
            case "TUE" -> "#2563eb";
            case "WED" -> "#7c3aed";
            case "THU" -> "#0d9488";
            case "FRI" -> "#ea580c";
            default    -> "#475569";
        };
    }

    private static String formatYearOnly(String year) {
        if (year == null || year.isBlank()) return "Year 1";
        return switch (year.trim()) {
            case "First Year"  -> "Year 1";
            case "Second Year" -> "Year 2";
            case "Third Year"  -> "Year 3";
            case "Fourth Year" -> "Year 4";
            case "Fifth Year"  -> "Year 5";
            default            -> year.trim();
        };
    }
    
    public static VBox createExamCard(
            ClassesController.ClassItem item,
            boolean isAdminView,
            EventHandler<ActionEvent> onOpenExamDetail) {

        VBox card = new VBox(8.0);
        card.getStyleClass().add("panel-card");
        card.setMinWidth(0.0);
        card.setMinHeight(240.0);
        card.setPrefHeight(240.0);
        card.setMaxHeight(240.0);
        card.setPadding(new Insets(14.0, 16.0, 14.0, 16.0));

        // 1. HEADER
        HBox topHBox = new HBox(12.0);
        topHBox.setAlignment(Pos.TOP_LEFT);
        topHBox.setMinHeight(56.0);
        topHBox.setPrefHeight(56.0);
        topHBox.setMaxHeight(56.0);

        StackPane iconPane = new StackPane();
        iconPane.setMinSize(44.0, 44.0);
        iconPane.setMaxSize(44.0, 44.0);
        iconPane.setStyle("-fx-background-color: " + item.badgeColor + "; -fx-background-radius: 10px;");

        Label iconLabel = new Label(item.icon);
        iconLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        iconPane.getChildren().add(iconLabel);

        VBox titleBox = new VBox(2.0);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        titleBox.setMinWidth(0.0);

        Label codeLabel = new Label(item.code);
        codeLabel.setStyle("-fx-font-size: 16.5px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Label subLabel = new Label(item.title);
        subLabel.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 600; -fx-text-fill: #334155;");
        subLabel.setWrapText(true);
        subLabel.setAlignment(Pos.TOP_LEFT);
        subLabel.setMinHeight(34.0);
        subLabel.setPrefHeight(34.0);
        subLabel.setMaxHeight(34.0);

        titleBox.getChildren().addAll(codeLabel, subLabel);
        topHBox.getChildren().addAll(iconPane, titleBox);

        Separator divider = new Separator();
        divider.setStyle("-fx-opacity: 0.7;");

        // 2. CLASS DETAILS
        VBox detailsBox = new VBox(4.0);

        Label deptLabel = new Label("🏛  " + item.department);
        deptLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");

        Label yearSemLabel = new Label("🎓  " + formatYearOnly(item.year) + " · " + item.semester);
        yearSemLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");

        String formattedStudents = item.studentCount != null
                ? item.studentCount.replace("students", "Students")
                : "0 Students";
        Label stdLabel = new Label("👥  " + formattedStudents);
        stdLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");

        String lecturerText = isAdminView
                ? item.lecturer + (item.lecturer.contains("(Assigned)") ? "" : " (Assigned)")
                : item.lecturer.replace(" (Assigned)", "");
        Label lectLabel = new Label("👤  " + lecturerText);
        lectLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");

        detailsBox.getChildren().addAll(deptLabel, yearSemLabel, stdLabel, lectLabel);

        Region verticalSpacer = new Region();
        VBox.setVgrow(verticalSpacer, Priority.ALWAYS);

        // 3. FULL-WIDTH EXAM DETAIL BUTTON
        Button openBtn = new Button("View Exam & Grading Detail");
        openBtn.getStyleClass().add("view-class-button");
        openBtn.setMaxWidth(Double.MAX_VALUE);
        openBtn.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8px 14px;");
        openBtn.setUserData(item.getOfferingKey());
        if (onOpenExamDetail != null) {
            openBtn.setOnAction(onOpenExamDetail);
        }

        card.getChildren().addAll(topHBox, divider, detailsBox, verticalSpacer, openBtn);
        return card;
    }
}