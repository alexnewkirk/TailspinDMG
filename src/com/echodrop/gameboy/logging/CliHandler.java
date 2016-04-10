package com.echodrop.gameboy.logging;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class CliHandler extends Handler {

	@Override
	public void close() throws SecurityException {
		// TODO Auto-generated method stub

	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub

	}

	@Override
	public void publish(LogRecord record) {
		if(record.getLevel() == Level.SEVERE ||
				record.getLevel() == Level.WARNING) {
			System.err.println(record.getMessage());
		} else {
			System.out.println(record.getMessage());
			
		}
		System.out.println();
	}

}
