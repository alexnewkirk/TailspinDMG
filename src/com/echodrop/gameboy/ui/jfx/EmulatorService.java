package com.echodrop.gameboy.ui.jfx;

import com.echodrop.gameboy.debugger.TailspinDebugger;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class EmulatorService extends Service<Void> {

	private TailspinDebugger tdb;

	public EmulatorService(TailspinDebugger tdb) {
		this.tdb = tdb;
	}

	@Override
	protected Task<Void> createTask() {
		return new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				while(!isCancelled()) {
					tdb.getSystem().getProcessor().step();
				}
				return null;
			}
		};
	}

}
