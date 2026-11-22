package application;

import javafx.application.Platform;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public class SubjectWeightageStore {

    public static class Weightage {
        public final int quiz;
        public final int assignment;
        public final int tutorial;
        public final int finalExam;

        public Weightage(int quiz, int assignment, int tutorial, int finalExam) {
            this.quiz = quiz;
            this.assignment = assignment;
            this.tutorial = tutorial;
            this.finalExam = finalExam;
        }
    }

    private static final Map<String, Weightage> SUBJECT_WEIGHTAGES = new HashMap<>();
    private static final List<BiConsumer<String, Weightage>> LISTENERS = new CopyOnWriteArrayList<>();

    public static String normalizeSubjectKey(String subject) {
        if (subject == null) return "DEFAULT";
        String s = subject.trim().toLowerCase();
        if (s.startsWith("data structure")) return "data structures";
        return s;
    }

    public static Weightage getForSubject(String subject) {
        return SUBJECT_WEIGHTAGES.getOrDefault(
            normalizeSubjectKey(subject),
            new Weightage(5, 15, 20, 60)
        );
    }

    public static void setForSubject(String subject, int quiz, int assignment, int tutorial, int finalExam) {
        String key = normalizeSubjectKey(subject);
        Weightage updated = new Weightage(quiz, assignment, tutorial, finalExam);
        SUBJECT_WEIGHTAGES.put(key, updated);

        Platform.runLater(() -> {
            for (BiConsumer<String, Weightage> listener : LISTENERS) {
                listener.accept(key, updated);
            }
        });
    }

    public static void addListener(BiConsumer<String, Weightage> listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(BiConsumer<String, Weightage> listener) {
        LISTENERS.remove(listener);
    }
}