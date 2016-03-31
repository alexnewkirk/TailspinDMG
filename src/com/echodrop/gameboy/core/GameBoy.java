package com.echodrop.gameboy.core;

import com.echodrop.gameboy.graphics.GPU;
import com.echodrop.gameboy.interfaces.IMMU;

public class GameBoy {
	
	private Z80 processor;
	private GPU gpu;
	private IMMU mem;
	
	public GameBoy() {
		
		this.setMem(new GBMMU(this));
		this.setProcessor(new Z80(this));
		this.setGpu(new GPU(this));
	}

	public IMMU getMem() {
		return mem;
	}

	public void setMem(IMMU mem) {
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
