package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class Main extends Application {

	@Override
    public void start(Stage primaryStage) {
        try {
            // Window 1: Main Login (Log in as Admin here)
            Parent root1 = FXMLLoader.load(getClass().getResource("LoginView.fxml"));
            Scene scene1 = new Scene(root1, 1060, 660);
            if (getClass().getResource("style.css") != null) {
                scene1.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
            }
            primaryStage.setTitle("YTU SIMS - Window 1 (Admin)");
            primaryStage.setScene(scene1);
            primaryStage.show();

            // Window 2: Second Login (Log in as Lecturer here to see live updates)
            Stage secondStage = new Stage();
            Parent root2 = FXMLLoader.load(getClass().getResource("LoginView.fxml"));
            Scene scene2 = new Scene(root2, 1060, 660);
            if (getClass().getResource("style.css") != null) {
                scene2.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
            }
            secondStage.setTitle("YTU SIMS - Window 2 (Lecturer)");
            secondStage.setScene(scene2);
            secondStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	public static void main(String[] args) {
		launch(args);
	}
}