package com.echodrop.gameboy.core;

import com.echodrop.gameboy.graphics.GPU;

/**
 * This class represents a combination of the components
 * required for the emulator to run
 * @author echo_drop
 */
public class GameBoy {
	
	private Z80 processor;
	private GPU gpu;
	private MMU mem;
	
	public GameBoy() {
		this.setMem(new MMU(this));
		this.setProcessor(new Z80(this));
		this.setGpu(new GPU(this));
	}
	
	public void reset() {
		processor.initialize();
		gpu.initialize();
		mem.initialize();
		
		mem.loadBios();
	}

	public MMU getMem() {
		return mem;
	}

	public void setMem(MMU mem) {
		this.mem = mem;
	}

	public GPU getGpu() {
		return gpu;
	}

	public void setGpu(GPU gpu) {
		this.gpu = gpu;
	}

	public Z80 getProcessor() {
		return processor;
	}

	public void setProcessor(Z80 processor) {
		this.processor = processor;
	}

}
