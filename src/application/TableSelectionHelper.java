package application;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;

public class TableSelectionHelper {

    public static void installOnScene(Scene scene) {
        if (scene == null) return;

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            Node target = (event.getTarget() instanceof Node) ? (Node) event.getTarget() : null;

            // Keep selection if user clicked inside any TableView or on any action Button
            if (isInsideType(target, TableView.class) || isInsideType(target, ButtonBase.class)) {
                return;
            }

            if (scene.getRoot() != null) {
                clearAllTableSelections(scene.getRoot());
            }
        });
    }

    private static boolean isInsideType(Node node, Class<?> type) {
        Node current = node;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static void clearAllTableSelections(Node node) {
        if (node instanceof TableView<?> table) {
            table.getSelectionModel().clearSelection();
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                clearAllTableSelections(child);
            }
        }
    }
}