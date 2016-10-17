package com.echodrop.gameboy.logging;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javafx.application.Platform;
import javafx.scene.control.ListView;

public class SimpleListViewLogger extends Handler {

	private ListView<String> lv;

	public SimpleListViewLogger(ListView<String> lv) {
		this.lv = lv;
	}

	@Override
	public void close() throws SecurityException {
		// Implementing Handler

	}

	@Override
	public void flush() {
		// Implementing Handler

	}

	@Override
	public void publish(LogRecord arg0) {
		Platform.runLater(() -> lv.getItems().add(arg0.getMessage()));
	}

}
