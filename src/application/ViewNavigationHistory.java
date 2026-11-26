package application;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.ArrayDeque;
import java.util.Deque;

public class ViewNavigationHistory {

    private static final Deque<Node> HISTORY = new ArrayDeque<>();

    /**
     * Saves the currently displayed view (including its exact scroll position, filters, and page)
     * before switching to nextView.
     */
    public static void navigateForward(StackPane contentArea, Node nextView) {
        if (contentArea == null || nextView == null) return;
        if (!contentArea.getChildren().isEmpty()) {
            Node current = contentArea.getChildren().get(0);
            if (current != null && current != nextView) {
                HISTORY.push(current);
            }
        }
        contentArea.getChildren().setAll(nextView);
    }

    /**
     * Restores the exact previous view where the user left off.
     * Returns true if a previous view was restored, false if history was empty.
     */
    public static boolean goBack(StackPane contentArea) {
        if (contentArea == null || HISTORY.isEmpty()) {
            return false;
        }
        Node previous = HISTORY.pop();
        contentArea.getChildren().setAll(previous);
        return true;
    }

    /**
     * Call when switching top-level sidebar tabs (Dashboard / Classes / Exams).
     */
    public static void clear() {
        HISTORY.clear();
    }
}