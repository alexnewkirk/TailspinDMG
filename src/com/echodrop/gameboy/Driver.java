package com.echodrop.gameboy;

import com.echodrop.gameboy.core.GameBoy;


/**
 * Creates an instance of the emulator, initializes it's 
 * state, and starts the emulation
 * @author echo_drop
 *
 */
public class Driver {
	
	public static void main(String[] args) {
		
		GameBoy g = new GameBoy();
		
		g.reset();
		
		g.getMem().loadRom("tetris.gb");
		
		g.getProcessor().beginDispatch();
		
	}

}
