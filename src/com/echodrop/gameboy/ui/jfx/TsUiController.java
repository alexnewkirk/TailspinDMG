package com.echodrop.gameboy.ui.jfx;

import java.net.URL;
import java.util.ResourceBundle;

import com.echodrop.gameboy.debugger.TailspinDebugger;
import com.echodrop.gameboy.graphics.GPU;
import com.echodrop.gameboy.interfaces.IGraphicsObserver;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

public class TsUiController implements Initializable, IGraphicsObserver {

	@FXML
	private Canvas canvas;

	@FXML
	private Button stepButton;

	@FXML
	private Button resetButton;

	@FXML
	private Button continueButton;

	private byte[][] screen;
	private int pixelSize = 4;
	private PixelWriter pw;
	private TailspinDebugger tdb;
	private GPU gpu;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		stepButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				tdb.getSystem().getProcessor().step();
			}
		});

		this.pw = canvas.getGraphicsContext2D().getPixelWriter();
	}

	@Override
	public void updateDisplay() {
		screen = gpu.getFrameBuffer();

		for (int i = 0; i < 160; i++) {
			for (int j = 0; j < 144; j++) {
				Color p = null;
				switch (screen[i][j]) {
				case 0:
					p = Color.WHITE;
					break;
				case 1:
					p = Color.LIGHTGRAY;
					break;
				case 2:
					p = Color.DARKGRAY;
					break;
				case 3:
					p = Color.BLACK;
					break;
				default:
					// This should never happen, but it'll be easy to
					// spot if something goes wrong
					p = Color.RED;
					break;
				}

				pw.setColor(i, j, p);
			}
		}
	}

	public void setTdb(TailspinDebugger tdb) {
		this.tdb = tdb;
		this.setGpu(tdb.getSystem().getGpu());
	}

	private void setGpu(GPU gpu) {
		this.gpu = gpu;
		this.gpu.registerObserver(this);
	}

}
