/**
 * Launcher.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.ui.processing;

import java.io.IOException;
import java.util.logging.Level;

import com.echodrop.gameboy.debugger.TailspinDebugger;
import com.echodrop.gameboy.exceptions.MapperNotImplementedException;
import com.echodrop.gameboy.graphics.GPU;
import com.echodrop.gameboy.interfaces.IGraphicsObserver;
import com.echodrop.gameboy.logging.SimpleConsoleLogger;
import com.echodrop.gameboy.util.FileUtils;

import processing.core.PApplet;
import processing.core.PImage;

/**
 * A simple Processing UI for testing
 * @author anewkirk
 */
public class Launcher extends PApplet implements IGraphicsObserver {
	
	private GPU gpu;
	private byte[][] screen;
	private TailspinDebugger tdb;
	private PImage buffer;
	
	public Launcher() throws IOException, MapperNotImplementedException {
		this.tdb = new TailspinDebugger();
		this.gpu = tdb.getSystem().getGpu();
		gpu.registerObserver(this);
		tdb.getSystem().initLogging(Level.OFF, new SimpleConsoleLogger());
		
		this.buffer = new PImage(160, 144);
		screen = gpu.getFrameBuffer();
		
		// TODO: remove hardcoded values
		byte[] drmario = FileUtils.readBytes("roms/drmario.gb");
		byte[] bootstrap = FileUtils.readBytes("bios.gb");
		tdb.getSystem().getMem().loadBootstrap(bootstrap);
		tdb.getSystem().getMem().loadRom(drmario);
		
		thread("startEmu");
	}

	public static void main(String[] args) {
		PApplet.main("com.echodrop.gameboy.ui.processing.Launcher");
	}
	
	@Override
    public void settings(){
		size(160, 144);
    }
	
	@Override
    public void draw(){
		image(buffer, 0, 0);
    }

	@Override
	public void updateDisplay() {
		this.screen = gpu.getFrameBuffer();
		buffer.loadPixels();
		if(screen != null) {
			for(int i = 0; i < 160; i++) {
				for(int j = 0; j < 144; j++) {
					int c = 0;
					switch(screen[i][j]) {
					case 0:
						c = 255;
						break;
					case 1:
						c = (255/3) * 2;
						break;
					case 2: 
						c = 155/3;
						break;
					case 3:
						c = 0;
						break;
					}
					buffer.pixels[j*160 + i] = color(c);
				}
			}
			buffer.updatePixels();
		}
	}
	
	public void startEmu() {
		tdb.getSystem().getProcessor().beginDispatch();
	}

}
