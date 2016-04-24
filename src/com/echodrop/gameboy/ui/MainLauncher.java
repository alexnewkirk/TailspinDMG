/**
 * MainLauncher.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.ui;

import java.io.IOException;
import java.util.logging.Level;

import com.echodrop.gameboy.debugger.TailspinDebugger;
import com.echodrop.gameboy.logging.CliHandler;
import com.echodrop.gameboy.util.FileUtils;

/**
 * A simple driver class with hardcoded filenames, for convenience
 */
public class MainLauncher {
	
	public static void main(String[] args) throws IOException {
		TailspinDebugger tdb = new TailspinDebugger();
		tdb.getSystem().initLogging(Level.OFF, new CliHandler());
		tdb.getSystem().getMem().loadBios(FileUtils.readBytes("bios.gb"));
		tdb.getSystem().getMem().loadRom(FileUtils.readBytes("tetris.gb"));
		tdb.enableVideoMode();
		tdb.getSystem().getProcessor().beginDispatch();
		
	}

}
