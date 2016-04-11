/**
 * ConsoleDriver.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.main;

import com.echodrop.gameboy.core.TailspinGB;

/**
 * Creates an instance of the emulator, initializes it's state, and starts the
 * emulation.
 * 
 * This class will later be replaced with an actual UI.
 */
public class ConsoleDriver {

	public static void main(String[] args) {
		TailspinGB g = new TailspinGB();
		g.reset();
		g.getMem().loadRom("tetris.gb");
		g.getProcessor().beginDispatch();
	}
	
}
