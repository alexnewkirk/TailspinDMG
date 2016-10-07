package com.echodrop.gameboy.ui.jfx;

import java.io.FileInputStream;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class JfxUi extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader();
		String fxmlPath = "layout/UiLayout.fxml";
		FileInputStream fxmlStream = new FileInputStream(fxmlPath);
		AnchorPane root = (AnchorPane) loader.load(fxmlStream);
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		
		primaryStage.setTitle("TailspinDMG 0.1");
		
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
