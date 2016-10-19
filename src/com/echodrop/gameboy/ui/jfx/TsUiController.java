package com.echodrop.gameboy.ui.jfx;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.IntBuffer;
import java.util.ResourceBundle;
import java.util.logging.Level;

import com.echodrop.gameboy.debugger.TailspinDebugger;
import com.echodrop.gameboy.exceptions.MapperNotImplementedException;
import com.echodrop.gameboy.graphics.GPU;
import com.echodrop.gameboy.interfaces.IGraphicsObserver;
import com.echodrop.gameboy.util.FileUtils;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.MenuItem;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class TsUiController implements Initializable, IGraphicsObserver {

	private final int PIXEL_SIZE = 2;
	private final int W = 320;
	private final int H = 288;

	@FXML
	private Canvas canvas;
	@FXML
	private AnchorPane displayPane;
	@FXML
	private MenuItem loadRomMenuItem;
	@FXML
	private MenuItem loadBootstrapMenuItem;
	@FXML
	private MenuItem aboutMenuItem;
	@FXML
	private MenuItem startMenuItem;
	@FXML
	private MenuItem stopMenuItem;
	@FXML
	private MenuItem resetMenuItem;
	@FXML
	private MenuItem debuggerMenuItem;
	@FXML
	private MenuItem closeMenuItem;
	@FXML
	private MenuItem saveStateMenuItem;
	@FXML
	private MenuItem loadStateMenuItem;
	

	private int color0 = toInt(Color.WHITE);
	private int color1 = toInt(new Color(0.66f, 0.66f, 0.66f, 1));
	private int color2 = toInt(new Color(0.33f, 0.33f, 0.33f, 1));
	private int color3 = toInt(new Color(0, 0, 0, 1));

	private int[] buffer;
	private byte[][] screen;
	private byte[] bootstrap;
	private TailspinDebugger tdb;
	private PixelWriter pw;
	private EmulatorService es;
	private Stage debuggerStage;
	private WritablePixelFormat<IntBuffer> pixelFormat;
	private final FileChooser fileChooser = new FileChooser();
	private String bootstrapPath = "bios.gb";

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		this.pw = canvas.getGraphicsContext2D().getPixelWriter();
		this.pixelFormat = PixelFormat.getIntArgbPreInstance();
		this.buffer = new int[W * H];

		initControls();
	}

	private void initControls() {

		aboutMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("About TailspinDMG");
				alert.setHeaderText(null);
				alert.setContentText("some info here");
				alert.initStyle(StageStyle.UTILITY);
				alert.show();
			}
		});

		startMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				startEmu();
			}
		});

		stopMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				stopEmu();
			}
		});

		resetMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				resetEmu();
			}
		});

		loadRomMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				File rom = fileChooser.showOpenDialog(null);
				tdb.getSystem().getLogger().setLevel(Level.ALL);
				if (rom != null) {
					String filepath = rom.getPath();
					try {
						byte[] romData = FileUtils.readBytes(filepath);
						tdb.getSystem().reset();
						readBootstrap();
						registerWithGpu();
						tdb.getSystem().getMem().loadRom(romData);
					} catch (IOException e) {
						ioErrorAlert();
					} catch (MapperNotImplementedException e) {
						Alert mapperErrorAlert = new Alert(AlertType.ERROR);
						mapperErrorAlert.setContentText("Unsupported MBC");
					}
				}
			}
		});

		loadBootstrapMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				File bios = fileChooser.showOpenDialog(null);
				tdb.getSystem().getLogger().setLevel(Level.ALL);
				if (bios != null) {
					bootstrapPath = bios.getPath();
					readBootstrap();
				}
			}
		});
		
		debuggerMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				if(debuggerStage.isShowing()) {
					debuggerStage.hide();
				} else {
					debuggerStage.show();
				}
			}
		});
		
		closeMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				Platform.exit();
			}
		});
	}

	public void startEmu() {
		if (!es.isRunning()) {
			tdb.getSystem().getLogger().setLevel(Level.OFF);
			es.restart();
		}
	}

	public void stopEmu() {
		if (es.isRunning()) {
			es.cancel();
			tdb.getSystem().getLogger().setLevel(Level.ALL);
		}
	}

	public void resetEmu() {
		// FIXME: clear the screen on reset
		if (es.isRunning()) {
			es.cancel();
		}

		tdb.getSystem().reset();
	}

	@Override
	public void updateDisplay() {
		screen = tdb.getSystem().getGpu().getFrameBuffer();
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
		registerWithGpu();
	}
	
	public void setDebuggerStage(Stage debuggerStage) {
		this.debuggerStage = debuggerStage;
	}

	private void registerWithGpu() {
		GPU gpu = tdb.getSystem().getGpu();
		gpu.registerObserver(this);
	}

	private void readBootstrap() {
		try {
			this.bootstrap = FileUtils.readBytes(bootstrapPath);
			tdb.getSystem().getMem().loadBootstrap(bootstrap);
		} catch (IOException e) {
			ioErrorAlert();
		}
	}

	private void ioErrorAlert() {
		Alert ioErrorAlert = new Alert(AlertType.ERROR);
		ioErrorAlert.setContentText("Could not load file");
		ioErrorAlert.setHeaderText(null);
		ioErrorAlert.show();
	}

	private int toInt(Color c) {
		return (255 << 24) | ((int) (c.getRed() * 255) << 16) | ((int) (c.getGreen() * 255) << 8)
				| ((int) (c.getBlue() * 255));
	}

}
