package com.echodrop.gameboy.ui.jfx;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

import com.echodrop.gameboy.debugger.TailspinDebugger;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.sg.prism.NGNode;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;

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
	@FXML
	private TableView<String> memoryView;
	@FXML
	private TableView<Map> registerView;

	private TailspinDebugger tdb;
	private EmulatorService es;
	private TsUiController mainController;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		initControls();
		updateRegisterView();
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

	private void updateRegisterView() {

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
