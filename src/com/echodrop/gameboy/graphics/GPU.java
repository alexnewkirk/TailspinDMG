package com.echodrop.gameboy.graphics;

import com.echodrop.gameboy.core.GameBoy;
import com.echodrop.gameboy.core.MemoryRegion;

public class GPU {

	private GameBoy system;
	
	private MemoryRegion vram;
	private MemoryRegion oam;
	
	public GPU(GameBoy system) {
		this.system = system;
		this.vram = new MemoryRegion((char)0x8000, (char)0x9FFF);
		this.setOam(new MemoryRegion((char)0x8000, (char)0x9FFF));
	}

	public MemoryRegion getVram() {
		return vram;
	}

	public void setVram(MemoryRegion vram) {
		this.vram = vram;
	}

	public MemoryRegion getOam() {
		return oam;
	}

	public void setOam(MemoryRegion oam) {
		this.oam = oam;
	}
}
