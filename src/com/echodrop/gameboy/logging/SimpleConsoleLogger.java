/**
 * CliHandler.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.logging;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class SimpleConsoleLogger extends Handler {

	@Override
	public void close() throws SecurityException {
		//Implementing Handler
	}

	@Override
	public void flush() {
		//Implementing Handler
	}

	@Override
	public void publish(LogRecord record) {
		System.out.println();
		if (record.getLevel() == Level.SEVERE || record.getLevel() == Level.WARNING) {
			System.err.println(record.getMessage());
		} else {
			System.out.println(record.getMessage());

		}
	}

}
