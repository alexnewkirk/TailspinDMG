package com.echodrop.gameboy.graphics;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.echodrop.gameboy.core.GameBoy;
import com.echodrop.gameboy.core.MemoryRegion;
import com.echodrop.gameboy.core.Register;
import com.echodrop.gameboy.exceptions.MemoryAccessException;
import com.echodrop.gameboy.interfaces.IGraphicsObserver;

/**
 * Emulation core for GameBoy GPU
 * 
 * 
 * Tiles are 8x8 pixels
 * 
 * 0x9800-9BFF is tilemap 0, 0x9C00-9FFF is tilemap 1
 * 
 * 0x9000-0x97FF is tileset 0, tiles 0-127
 * 
 * 0x8800-0x8FFF is tileset 0, -1 to -127 - which are shared with tileset 1 as
 * tiles 128-255
 * 
 * 0x8000-0x87FF is tileset 1, tiles 0-127
 * 
 * Tilemaps are 32 x 32 tiles
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

	private byte[][] frameBuffer;

	// GPU state
	private byte mode;

	// advanced after each CPU instruction with the Z80 clock_t
	private int modeClock;

	private List<IGraphicsObserver> observers;

	private static final Logger logger = Logger.getLogger(GPU.class.getName());

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
		this.line = new Register((byte) 0);
		this.modeClock = 0;

		this.backgroundPalette = new Register((byte) 0x010B);

		this.scrollX = new Register((byte) 0);
		this.scrollY = new Register((byte) 0);
		this.lcdControl = new Register((byte) 0);

		this.setVram(new MemoryRegion((char) 0x8000, (char) 0x9FFF, "vram"));
		this.setOam(new MemoryRegion((char) 0x8000, (char) 0x9FFF, "oam"));

		this.setFrameBuffer(new byte[160][144]);
		for (int i = 0; i < 160; i++) {
			for (int j = 0; j < 144; j++) {
				frameBuffer[i][j] = 0;
			}
		}
	}

	public void initLogging() {
		logger.setParent(system.getLogger());
	}

	/**
	 * Called after each CPU instruction
	 */
	public void clockStep() {

		logger.info("GPU Clock Step: line:" + line + " mode:" + mode + " modeClock: " + modeClock);

		switch (mode) {

		// HBLANK
		case 0:
			if (modeClock >= 204) {
				modeClock = 0;
				line.value++;

				if (line.value == 143) {
					// Change mode to VBLANK
					logger.info("[!] GPU MODE SWITCHING TO VBLANK (mode 1)");
					mode = 1;

					// update screen after last HBLANK
					notifyAllObservers();

				} else {
					// Change mode to OAM read
					logger.info("[!] GPU MODE SWITCHING TO OAM READ (mode 3)");
					mode = 2;
				}
			}

			break;

		// VBLANK
		case 1:
			if (modeClock >= 456) {
				modeClock = 0;
				line.value++;
				;

				if (line.value > 153) {
					// change mode to OAM read
					logger.info("[!] GPU MODE SWITCHING TO OAM READ (mode 2)");
					mode = 2;
					line.value = 0;
				}
			}

			break;

		// OAM read
		case 2:
			if (modeClock >= 80) {
				modeClock = 0;
				// change to vram read mode
				mode = 3;
				logger.info("[!] GPU MODE SWITCHING TO VRAM READ (mode 3)");
			}

			break;

		// VRAM read
		case 3:
			if (modeClock >= 172) {
				// change mode to HBLANK
				modeClock = 0;
				logger.info("\n[!] GPU MODE SWITCHING TO HBLANK (mode 0)\n");
				mode = 0;

				// Write scanline to framebuffer
				renderScanLine();

			}
			break;
		}

	}

	public byte readByte(char address) {
		logger.info("GPU memory access: " + Integer.toHexString(address & 0xFFFF));
		switch (address) {

		// LCD control register
		case 0xFF00:
			return lcdControl.value;

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
		
		logger.severe("Invalid memory access in GPU: " + Integer.toHexString(address));
		throw new MemoryAccessException(address);
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
		logger.info("[!] renderScanLine() called"); 
	}

	public void incrementModeClock(byte time) {
		this.modeClock += time;
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

	private void setFrameBuffer(byte[][] frameBuffer) {
		this.frameBuffer = frameBuffer;
	}
	
	public byte[][] getFrameBuffer() {
		return this.frameBuffer;
	}

}
