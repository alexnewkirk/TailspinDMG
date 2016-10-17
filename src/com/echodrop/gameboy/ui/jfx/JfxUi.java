package com.echodrop.gameboy.ui.jfx;

import java.io.FileInputStream;
import java.util.logging.Level;

import com.echodrop.gameboy.debugger.TailspinDebugger;
import com.echodrop.gameboy.logging.SimpleConsoleLogger;
import com.echodrop.gameboy.util.FileUtils;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class JfxUi extends Application {

	@FXML
	private Canvas canvas;

	private TailspinDebugger tdb;
	private final String FXML_PATH = "layout/UiLayout.fxml";
	private final String WINDOW_TITLE = "TailspinDMG 0.1";

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader();
		FileInputStream fxmlStream = new FileInputStream(FXML_PATH);
		AnchorPane root = (AnchorPane) loader.load(fxmlStream);

		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.setResizable(false);
		primaryStage.setTitle(WINDOW_TITLE);

		TsUiController tsuic = (TsUiController) loader.getController();
		tdb = new TailspinDebugger();
		tsuic.setTdb(tdb);

		// TODO: make a Logger that displays to the jfx ui
		tdb.getSystem().initLogging(Level.OFF, new SimpleConsoleLogger());

		// TODO: remove hardcoded values
		byte[] drmario = FileUtils.readBytes("roms/drmario.gb");
		byte[] bootstrap = FileUtils.readBytes("bios.gb");
		tdb.getSystem().getMem().loadBootstrap(bootstrap);
		tdb.getSystem().getMem().loadRom(drmario);

		primaryStage.show();

		EmulatorService es = new EmulatorService(tdb);
		es.start();
	}

	public static void main(String[] args) {
		// enable hardware acceleration for gfx rendering
		System.setProperty("sun.java2d.opengl", "true");
		launch(args);
	}
}
