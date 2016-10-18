package com.echodrop.gameboy.ui.jfx;

import java.io.FileInputStream;
import java.util.logging.Level;

import com.echodrop.gameboy.debugger.TailspinDebugger;
import com.echodrop.gameboy.logging.SimpleListViewLogger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class JfxUi extends Application {

	private TailspinDebugger tdb;
	private final String FXML_PATH = "layout/UiLayout.fxml";
	private final String WINDOW_TITLE = "TailspinDMG 0.2";

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader();
		FileInputStream fxmlStream = new FileInputStream(FXML_PATH);
		AnchorPane root = (AnchorPane) loader.load(fxmlStream);

		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.setResizable(false);
		primaryStage.setTitle(WINDOW_TITLE);
		primaryStage.show();

		TsUiController tsuic = (TsUiController) loader.getController();
		tdb = new TailspinDebugger();
		tsuic.setTdb(tdb);

		EmulatorService es = new EmulatorService(tdb);
		tsuic.setEmuService(es);

		tdb.getSystem().initLogging(Level.OFF, new SimpleListViewLogger(tsuic.getLogView()));
	}

	public static void main(String[] args) {
		// Enable hardware acceleration for gfx rendering
		System.setProperty("sun.java2d.opengl", "true");
		launch(args);
	}
}
