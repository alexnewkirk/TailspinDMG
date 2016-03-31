package com.echodrop.gameboy.graphics;

import com.echodrop.gameboy.core.GameBoy;
import com.echodrop.gameboy.core.MemoryRegion;

/**
 * Emulation core for GameBoy GPU
 * @author echo_drop
 *
 */
public class GPU {

	private GameBoy system;
	
	private MemoryRegion vram;
	private MemoryRegion oam;
	
	public GPU(GameBoy system) {
		this.system = system;
		this.initialize();
	}
	
	public void initialize() {
		this.vram = new MemoryRegion((char)0x8000, (char)0x9FFF, "vram");
		this.setOam(new MemoryRegion((char)0x8000, (char)0x9FFF, "oam"));
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
