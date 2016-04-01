package com.echodrop.gameboy.core;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.echodrop.gameboy.graphics.GPU;
import com.echodrop.gameboy.logging.CliHandler;

/**
 * This class represents a combination of the components required for the
 * emulator to run
 * 
 * @author echo_drop
 */
public class GameBoy {

	private Z80 processor;
	private GPU gpu;
	private MMU mem;

	private static final Logger logger = Logger.getLogger(GameBoy.class.getName());

	public GameBoy() {
		this.setMem(new MMU(this));
		this.setProcessor(new Z80(this));
		this.setGpu(new GPU(this));
		this.initLogging();
	}

	/**
	 * Sets up system-wide logging
	 */
	public void initLogging() {
		// disable default handler in root logger
		Logger globalLogger = Logger.getLogger("");
		Handler[] handlers = globalLogger.getHandlers();
		for (Handler handler : handlers) {
			globalLogger.removeHandler(handler);
		}

		logger.setLevel(Level.ALL);
		logger.addHandler(new CliHandler());
		
		mem.initLogging();
		processor.initLogging();
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
	
	public Logger getLogger() {
		return logger;
	}

}
