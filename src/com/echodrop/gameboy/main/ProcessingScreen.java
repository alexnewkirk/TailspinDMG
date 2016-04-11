/**
 * TailspinDebugger.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.main;

import com.echodrop.gameboy.core.TailspinGB;
import com.echodrop.gameboy.graphics.GPU;
import com.echodrop.gameboy.interfaces.IGraphicsObserver;

import processing.core.PApplet;

public class ProcessingScreen extends PApplet implements IGraphicsObserver {

	private byte[][] screen;
	private GPU gpu;
	private Thread emuThread;

	public void settings() {
		size(160, 144);
	}

	public void register(GPU gpu) {
		this.gpu = gpu;
		gpu.registerObserver(this);
	}

	public void setup() {
		background(255, 255, 255);
		TailspinGB system = new TailspinGB();
		setGpu(system.getGpu());
		gpu.registerObserver(this);
		system.getMem().loadBios();
		system.getMem().loadRom("tetris.gb");
		emuThread = new Thread() {

			@Override
			public void run() {

				system.getProcessor().beginDispatch();

			}
		};
		emuThread.start();
	}

	public void draw() {
		if (screen != null) {
			loadPixels();
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					pixels[j * (width - 1) + i] = screen[i][j];
				}
			}
			updatePixels();
		}
	}

	@Override
	public void update() {
		setScreen(getGpu().getFrameBuffer());
		System.out.println("[!] SCREEN UPDATED");
	}

	public void setScreen(byte[][] screen) {
		this.screen = screen;
	}

	public GPU getGpu() {
		return gpu;
	}

	public void setGpu(GPU gpu) {
		this.gpu = gpu;
	}

}
