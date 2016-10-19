package com.echodrop.gameboy.ui.jfx;

import java.io.FileInputStream;
import java.util.logging.Level;

import com.echodrop.gameboy.debugger.TailspinDebugger;
import com.echodrop.gameboy.logging.SimpleListViewLogger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class JfxUi extends Application {

	private TailspinDebugger tdb;
	private final String MAIN_FXML_PATH = "layout/UiLayout.fxml";
	private final String DEBUGGER_FXML_PATH = "layout/DebuggerLayout.fxml";
	private final String WINDOW_TITLE = "TailspinDMG 0.2";
	private final String DEBUGGER_WINDOW_TITLE = "Tailspin Debugger";

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader mainLoader = new FXMLLoader();
		FileInputStream mainFxmlStream = new FileInputStream(MAIN_FXML_PATH);
		AnchorPane mainRoot = (AnchorPane) mainLoader.load(mainFxmlStream);
		TsUiController tsuic = (TsUiController) mainLoader.getController();

		Scene mainScene = new Scene(mainRoot);
		primaryStage.setScene(mainScene);
		primaryStage.setResizable(false);
		primaryStage.setTitle(WINDOW_TITLE);
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent arg0) {
				Platform.exit();
			}
		});
		
		primaryStage.show();

		Stage debuggerStage = new Stage();
		FXMLLoader debuggerLoader = new FXMLLoader();
		FileInputStream debuggerFxmlStream = new FileInputStream(DEBUGGER_FXML_PATH);
		SplitPane debuggerRoot = (SplitPane) debuggerLoader.load(debuggerFxmlStream);
		DebuggerController dbgc = (DebuggerController) debuggerLoader.getController();

		Scene debuggerScene = new Scene(debuggerRoot);
		debuggerStage.setScene(debuggerScene);
		debuggerStage.setResizable(false);
		debuggerStage.setTitle(DEBUGGER_WINDOW_TITLE);

		tdb = new TailspinDebugger();
		tsuic.setTdb(tdb);
		dbgc.setTdb(tdb);

		EmulatorService es = new EmulatorService(tdb);
		tsuic.setEmuService(es);
		dbgc.setEs(es);

		dbgc.setMainController(tsuic);
		tsuic.setDebuggerStage(debuggerStage);

		SimpleListViewLogger log = new SimpleListViewLogger(dbgc.getLogView());
		tdb.getSystem().initLogging(Level.OFF, log);
	}

	public static void main(String[] args) {
		// Enable hardware acceleration for gfx rendering
		System.setProperty("sun.java2d.opengl", "true");
		launch(args);
	}
}
