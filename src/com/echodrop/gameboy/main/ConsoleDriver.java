package com.echodrop.gameboy.main;

import com.echodrop.gameboy.core.TailspinGB;

/**
 * Creates an instance of the emulator, initializes it's 
 * state, and starts the emulation.
 * 
 * This class will later be replaced with an actual UI.
 * @author echo_drop
 *
 */
public class ConsoleDriver {
	
	public static void main(String[] args) {
		
		TailspinGB g = new TailspinGB();
		
		g.reset();
		
		g.getMem().loadRom("tetris.gb");
		
		g.getProcessor().beginDispatch();
		
	}

}
