package com.echodrop.gameboy;

import com.echodrop.gameboy.core.GameBoy;

public class Driver {
	
	public static void main(String[] args) {
		
		GameBoy g = new GameBoy();
		g.getProcessor().beginDispatch();
		
	}

}
