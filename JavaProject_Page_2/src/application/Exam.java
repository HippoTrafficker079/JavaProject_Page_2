package application;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Exam {
    private final StringProperty title;
    private final StringProperty subject;
    private final StringProperty className;
    private final StringProperty examDate;
    private final StringProperty duration;
    private final StringProperty status;
    private final IntegerProperty totalMarks;

    public Exam(String title, String subject, String className, String examDate, String duration, String status, int totalMarks) {
        this.title = new SimpleStringProperty(title);
        this.subject = new SimpleStringProperty(subject);
        this.className = new SimpleStringProperty(className);
        this.examDate = new SimpleStringProperty(examDate);
        this.duration = new SimpleStringProperty(duration);
        this.status = new SimpleStringProperty(status);
        this.totalMarks = new SimpleIntegerProperty(totalMarks);
    }

    public String getTitle() { return title.get(); }
    public StringProperty titleProperty() { return title; }

    public String getSubject() { return subject.get(); }
    public StringProperty subjectProperty() { return subject; }

    public String getClassName() { return className.get(); }
    public StringProperty classNameProperty() { return className; }

    public String getExamDate() { return examDate.get(); }
    public StringProperty examDateProperty() { return examDate; }

    public String getDuration() { return duration.get(); }
    public StringProperty durationProperty() { return duration; }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }

    public int getTotalMarks() { return totalMarks.get(); }
    public IntegerProperty totalMarksProperty() { return totalMarks; }
}
