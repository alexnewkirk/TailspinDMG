package com.echodrop.gameboy.ui.jfx;

import java.net.URL;
import java.util.ResourceBundle;

import com.echodrop.gameboy.debugger.TailspinDebugger;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

public class DebuggerController implements Initializable {

	@FXML
	private Button stepButton;
	@FXML
	private Button continueButton;
	@FXML
	private Button stopButton;
	@FXML
	private Button resetButton;
	@FXML
	private ListView<String> logView;

	private TailspinDebugger tdb;
	private EmulatorService es;
	private TsUiController mainController;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		initControls();
	}

	private void initControls() {
		stepButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				if (!es.isRunning()) {
					tdb.getSystem().getProcessor().step();
				}
			}
		});

		continueButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				mainController.startEmu();
			}
		});

		stopButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				mainController.stopEmu();
			}
		});

		resetButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				mainController.resetEmu();
			}
		});
	}

	public void setTdb(TailspinDebugger tdb) {
		this.tdb = tdb;
	}

	public void setEs(EmulatorService es) {
		this.es = es;
	}

	public void setMainController(TsUiController mainController) {
		this.mainController = mainController;
	}
	
	public ListView<String> getLogView() {
		return this.logView;
	}

}
