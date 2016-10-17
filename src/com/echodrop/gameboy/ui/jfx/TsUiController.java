package com.echodrop.gameboy.ui.jfx;

import java.net.URL;
import java.nio.IntBuffer;
import java.util.ResourceBundle;

import com.echodrop.gameboy.debugger.TailspinDebugger;
import com.echodrop.gameboy.graphics.GPU;
import com.echodrop.gameboy.interfaces.IGraphicsObserver;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;

public class TsUiController implements Initializable, IGraphicsObserver {

	@FXML
	private Canvas canvas;

	@FXML
	private AnchorPane displayPane;

	@FXML
	private Button stepButton;

	@FXML
	private Button resetButton;

	@FXML
	private Button startButton;

	@FXML
	private Button stopButton;

	@FXML
	private MenuItem aboutButton;

	private final int PIXEL_SIZE = 2;
	private final int W = 320;
	private final int H = 288;

	private int color0 = toInt(Color.WHITE);
	private int color1 = toInt(new Color(0.66f, 0.66f, 0.66f, 1));
	private int color2 = toInt(new Color(0.33f, 0.33f, 0.33f, 1));
	private int color3 = toInt(new Color(0, 0, 0, 1));

	private int[] buffer;
	private byte[][] screen;

	private TailspinDebugger tdb;
	private GPU gpu;
	private PixelWriter pw;
	private EmulatorService es;
	private WritablePixelFormat<IntBuffer> pixelFormat;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		this.pw = canvas.getGraphicsContext2D().getPixelWriter();
		this.pixelFormat = PixelFormat.getIntArgbPreInstance();
		this.buffer = new int[W * H];

		initButtons();
	}

	private void initButtons() {
		stepButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				if (!es.isRunning()) {
					tdb.getSystem().getProcessor().step();
				}
			}
		});

		aboutButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				Alert alert = new Alert(AlertType.NONE);
				alert.setTitle("About TailspinDMG");
				alert.setHeaderText(null);
				alert.setContentText("some info here");
				alert.initStyle(StageStyle.UTILITY);
			}
		});

		startButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				if (!es.isRunning()) {
					es.restart();
				}
			}
		});

		stopButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				if (es.isRunning()) {
					es.cancel();
				}
			}
		});

		resetButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				// FIXME: clear the screen on reset

				if (es.isRunning()) {
					es.cancel();
				}

				tdb.getSystem().reset();
			}
		});
	}

	private int toInt(Color c) {
		return (255 << 24) | ((int) (c.getRed() * 255) << 16) | ((int) (c.getGreen() * 255) << 8)
				| ((int) (c.getBlue() * 255));
	}

	@Override
	public void updateDisplay() {
		screen = gpu.getFrameBuffer();
		for (int i = 0; i < W; i += PIXEL_SIZE) {
			for (int j = 0; j < H; j += PIXEL_SIZE) {
				byte cell = screen[i / 2][j / 2];
				int c = 0;
				switch (cell) {
				case 0:
					c = color0;
					break;
				case 1:
					c = color1;
					break;
				case 2:
					c = color2;
					break;
				case 3:
					c = color3;
					break;
				}
				int current = j * W + i;
				buffer[current] = c;
				buffer[current + 1] = c;
				buffer[current + W] = c;
				buffer[current + W + 1] = c;
			}
		}

		Platform.runLater(() -> pw.setPixels(0, 0, W, H, pixelFormat, buffer, 0, W));
	}

	public void setEmuService(EmulatorService es) {
		this.es = es;
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
