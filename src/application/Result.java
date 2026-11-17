package application;



import javafx.beans.property.*;

public class Result {
    private final StringProperty rollNo;
    private final StringProperty studentName;
    private final DoubleProperty totalMarks;
    private final StringProperty status;
    
    // Detailed exam breakdown scores
    private final DoubleProperty assignmentMarks;
    private final DoubleProperty tutorialMarks;
    private final DoubleProperty finalExamMarks;
    private final DoubleProperty quizMarks;
    private final StringProperty examName;

    public Result(String rollNo, String studentName, double totalMarks, String status, 
                  double assignmentMarks, double tutorialMarks, double finalExamMarks, double quizMarks, String examName) {
        this.rollNo = new SimpleStringProperty(rollNo);
        this.studentName = new SimpleStringProperty(studentName);
        this.totalMarks = new SimpleDoubleProperty(totalMarks);
        this.status = new SimpleStringProperty(status);
        this.assignmentMarks = new SimpleDoubleProperty(assignmentMarks);
        this.tutorialMarks = new SimpleDoubleProperty(tutorialMarks);
        this.finalExamMarks = new SimpleDoubleProperty(finalExamMarks);
        this.quizMarks = new SimpleDoubleProperty(quizMarks);
        this.examName = new SimpleStringProperty(examName);
    }

    // Table property getters
    public String getRollNo() { return rollNo.get(); }
    public StringProperty rollNoProperty() { return rollNo; }

    public String getStudentName() { return studentName.get(); }
    public StringProperty studentNameProperty() { return studentName; }

    public double getTotalMarks() { return totalMarks.get(); }
    public DoubleProperty totalMarksProperty() { return totalMarks; }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }

    // Detail property getters
    public double getAssignmentMarks() { return assignmentMarks.get(); }
    public double getTutorialMarks() { return tutorialMarks.get(); }
    public double getFinalExamMarks() { return finalExamMarks.get(); }
    public double getQuizMarks() { return quizMarks.get(); }
    public String getExamName() { return examName.get(); }
}
