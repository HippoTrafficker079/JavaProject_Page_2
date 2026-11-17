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
			String css = this.getClass().getResource("style.css").toExternalForm();
			Parent root = FXMLLoader.load(getClass().getResource("Loginview.fxml"));

			// Increased startup window size to match reference proportions
			Scene scene = new Scene(root, 1060, 660);
			scene.getStylesheets().add(css);

			primaryStage.setTitle("YTU Student Information Management System"); 
			primaryStage.setScene(scene);

			// Prevent shrinking the window smaller than what fits all UI columns cleanly
			primaryStage.setMinWidth(1060);
			primaryStage.setMinHeight(660);

			primaryStage.centerOnScreen();
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}