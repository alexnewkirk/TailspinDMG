package com.echodrop.gameboy.graphics;

import java.util.ArrayList;
import java.util.List;

import com.echodrop.gameboy.core.GameBoy;
import com.echodrop.gameboy.core.MemoryRegion;
import com.echodrop.gameboy.core.Register;
import com.echodrop.gameboy.interfaces.IGraphicsObserver;

/**
 * Emulation core for GameBoy GPU
 * 
 * 
 * Tiles are 8x8px
 * 
 * 0x9800-9BFF is tilemap 0 0x9C00-9FFF is tilemap 1
 * 
 * 9000-97FF tileset 0, tiles 0-127
 * 
 * 8800-8FFF tileset 0, -1 to -127 which are also tileset 1, tiles 128-255
 * 
 * 8000-87FF tileset 1, tiles 0-127
 * 
 * tilemaps are 32 x 32 tiles
 * 
 * 
 * @author echo_drop
 */
public class GPU {

	private GameBoy system;
	private MemoryRegion vram;
	private MemoryRegion oam;
	private Register scrollX;
	private Register scrollY;

	// Current scanline (there are 144 total, plus 10 vblank)
	private Register line;

	private Register backgroundPalette;

	private Register lcdControl;

	private byte[][] screen;
	private byte[][] frameBuffer;

	// GPU state
	private byte mode;

	// advanced after each CPU instruction with the Z80 clock_t
	private Register modeClock;

	private List<IGraphicsObserver> observers;

	public GPU(GameBoy system) {
		this.system = system;
		this.initialize();
	}

	/**
	 * Sets the GPU to its initial state
	 */
	public void initialize() {

		this.observers = new ArrayList<IGraphicsObserver>();

		this.mode = 0;
		this.modeClock = new Register((byte) 0);

		this.backgroundPalette = new Register((byte) 0x010B);

		this.scrollX = new Register((byte) 0);
		this.scrollY = new Register((byte) 0);
		this.line = new Register((byte) 0);
		this.lcdControl = new Register((byte) 0);

		this.setVram(new MemoryRegion((char) 0x8000, (char) 0x9FFF, "vram"));
		this.setOam(new MemoryRegion((char) 0x8000, (char) 0x9FFF, "oam"));

		this.setScreen(new byte[160][144]);
		this.setFrameBuffer(new byte[160][144]);
		for (int i = 0; i < 160; i++) {
			for (int j = 0; j < 144; j++) {
				screen[i][j] = 0;
				frameBuffer[i][j] = 0;
			}
		}
	}

	/**
	 * Called after each CPU instruction
	 */
	public void clockStep() {

		switch (mode) {

		// HBLANK
		case 0:
			if (modeClock.value >= 204) {
				modeClock.value = 0;
				line.value++;

				if (line.value == 143) {
					// Change mode to VBLANK
					mode = 1;

					// update screen
					notifyAllObservers();

				} else {
					// Change mode to OAM read
					mode = 2;
				}
			}

			break;

		// VBLANK
		case 1:
			if (modeClock.value >= 456) {
				modeClock.value = 0;
				line.value++;

				if (line.value > 153) {
					// change mode to OAM read
					mode = 2;
					line.value = 0;
				}
			}

			break;

		// OAM read
		case 2:
			if (modeClock.value >= 80) {
				modeClock.value = 0;
				// change to vram read mode
				mode = 3;
			}

			break;

		// VRAM read
		case 3:
			if (modeClock.value >= 172) {
				// change mode to HBLANK
				modeClock.value = 0;
				mode = 0;

				// Write scanline to framebuffer
				renderScanLine();

			}
		}

	}

	public byte readByte(char address) {
		switch (address) {

		// LCD control register
		case 0xFF00:
			break;

		// SCY register
		case 0xFF42:
			return scrollY.value;

		// SCX register
		case 0xFF43:
			return scrollX.value;

		// Current scanline register
		case 0xFF44:
			return line.value;
		}
		System.err.println("Invalid memory access in GPU: " + Integer.toHexString(address));
		return 0;
	}

	public char readWord(char address) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void writeByte(char address, byte data) {
		switch (address) {

		// LCD control register
		case 0xFF00:
			break;

		// SCY register
		case 0xFF42:
			scrollY.value = data;
			break;

		// SCX register
		case 0xFF43:
			scrollX.value = data;
			break;

		// current scanline register
		case 0xFF44:
			line.value = data;
			break;

		}
	}
	
	public void registerObserver(IGraphicsObserver o) {
		observers.add(o);
	}

	private void notifyAllObservers() {
		for (IGraphicsObserver o : observers) {
			o.update();
		}
	}

	private void renderScanLine() {
		// todo
	}

	public void incrementModeClock(byte time) {
		this.modeClock.value += time;
	}

	public MemoryRegion getVram() {
		return vram;
	}

	private void setVram(MemoryRegion vram) {
		this.vram = vram;
	}

	public MemoryRegion getOam() {
		return oam;
	}

	private void setOam(MemoryRegion oam) {
		this.oam = oam;
	}

	public byte[][] getScreen() {
		return screen;
	}

	private void setScreen(byte[][] screen) {
		this.screen = screen;
	}

	private void setFrameBuffer(byte[][] frameBuffer) {
		this.frameBuffer = frameBuffer;
	}

}
