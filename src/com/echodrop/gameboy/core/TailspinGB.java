/**
 * TailspinGB.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.core;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.echodrop.gameboy.graphics.GPU;

/**
 * This class represents a combination of the components required for the
 * emulator to run
 */
public class TailspinGB {

	private static final Logger logger = Logger.getLogger(TailspinGB.class.getName());
	private CPU processor;
	private GPU gpu;
	private MMU mem;

	public TailspinGB() {
		this.setMem(new MMU(this));
		this.setProcessor(new CPU(this));
		this.setGpu(new GPU(this));
	}


	public void initLogging(Level logLevel, Handler handler) {
		// disable default handler in root logger
		Logger globalLogger = Logger.getLogger("");
		Handler[] handlers = globalLogger.getHandlers();

		for (Handler h : handlers) {
			globalLogger.removeHandler(h);
		}
		logger.setLevel(logLevel);
		logger.addHandler(handler);

		mem.initLogging();
		processor.initLogging();
		gpu.initLogging();
	}

	/**
	 * Initilaize each component of the emulator
	 */
	public void reset() {
		processor.initialize();
		gpu.initialize();
		mem.initialize();
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

	public CPU getProcessor() {
		return processor;
	}

	public void setProcessor(CPU processor) {
		this.processor = processor;
	}

	public Logger getLogger() {
		return logger;
	}

}
