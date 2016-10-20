/**
 * GPU.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.graphics;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.echodrop.gameboy.core.MemoryRegion;
import com.echodrop.gameboy.core.Register;
import com.echodrop.gameboy.core.TailspinGB;
import com.echodrop.gameboy.exceptions.MemoryAccessException;
import com.echodrop.gameboy.interfaces.IGraphicsObserver;
import com.echodrop.gameboy.util.GraphicsUtils;
import com.echodrop.gameboy.util.RegisterUtils;

/**
 * Emulation core for GameBoy Graphics Processing Unit
 */
public class GPU {

	private static final Logger logger = Logger.getLogger(GPU.class.getName());
	private TailspinGB system;
	private MemoryRegion vram;
	private MemoryRegion oam;
	private Register scrollX;
	private Register scrollY;

	/**
	 * Current scanline (there are 144 total, plus 10 vblank)
	 */
	private Register line;
	private Register backgroundPalette;
	private Register lcdControl;
	private byte[][] frameBuffer;

	/**
	 * GPU state
	 */
	private Register mode;

	/**
	 * Advanced after each CPU instruction with the Z80 clock_t
	 */
	private int modeClock;
	private ArrayList<IGraphicsObserver> observers;

	public GPU(TailspinGB system) {
		this.system = system;
		this.initialize();
	}

	/**
	 * Sets the GPU to its initial state
	 */
	public void initialize() {
		this.observers = new ArrayList<IGraphicsObserver>();
		this.setMode(new Register((byte) 0, "GPU Mode"));
		this.setLine(new Register((byte) 0, "Scanline"));
		this.setModeClock(0);
		this.setBackgroundPalette(new Register((byte) 0x010B, "BG Palette"));
		this.setScrollX(new Register((byte) 0, "SCX"));
		this.setScrollY(new Register((byte) 0, "SCY"));
		this.setLcdControl(new Register((byte) 0, "LCDC"));
		this.setVram(new MemoryRegion((char) 0x8000, (char) 0x9FFF, "vram"));
		this.setOam(new MemoryRegion((char) 0xFE00, (char) 0xFE9F, "oam"));
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
	 * 
	 * Based on the write-up at
	 * http://imrannazar.com/GameBoy-Emulation-in-JavaScript:-The-CPU
	 */
	public void clockStep() {
		switch (getMode().getValue()) {
		
		// HBLANK
		case 0:
			if (getModeClock() >= 204) {
				setModeClock(0);
				getLine().setValue(getLine().getValue() + 1);
				if ((getLine().getValue() & 0xFF) == 143) {

					// Change mode to VBLANK
					logger.info("[!] GPU MODE SWITCHING TO VBLANK (mode 1)");
					mode.setValue(1);

					// update screen after last HBLANK
					// notifyAllObservers();
					renderFrame();

				} else {

					// Change mode to OAM read
					logger.info("[!] GPU MODE SWITCHING TO OAM READ (mode 2)");
					mode.setValue(2);
				}
			}

			break;

		// VBLANK
		case 1:
			if (getModeClock() >= 456) {
				setModeClock(0);
				getLine().setValue(getLine().getValue() + 1);
				if ((getLine().getValue() & 0xFF) > 153) {

					// change mode to OAM read
					logger.info("[!] GPU MODE SWITCHING TO OAM READ (mode 2)");
					mode.setValue(2);
					getLine().setValue(0);
				}
			}
			break;

		// OAM read
		case 2:
			if (getModeClock() >= 80) {
				setModeClock(0);

				// change to vram read mode
				mode.setValue(3);
				logger.info("[!] GPU MODE SWITCHING TO VRAM READ (mode 3)");
			}
			break;

		// VRAM read
		case 3:
			if (getModeClock() >= 172) {

				// change mode to HBLANK
				setModeClock(0);
				logger.info("\n[!] GPU MODE SWITCHING TO HBLANK (mode 0)\n");
				mode.setValue(0);

				// Write scanline to framebuffer
				// renderScanLine();
			}
			break;
		}
	}

	public byte readByte(char address) {
		logger.info("GPU memory access: " + Integer.toHexString(address & 0xFFFF));
		switch (address) {

		// LCD control register
		case 0xFF40:
			return getLcdControl().getValue();

		// SCY register
		case 0xFF42:
			return getScrollY().getValue();

		// SCX register
		case 0xFF43:
			return getScrollX().getValue();

		// Current scanline register
		case 0xFF44:
			return getLine().getValue();

		// Background palette
		case 0xFF47:
			return getBackgroundPalette().getValue();
		}

		logger.severe("Invalid memory access in GPU: " + Integer.toHexString(address));
		throw new MemoryAccessException(address);
	}

	public char readWord(char address) {
		// TODO Does the gpu need to be able to read 16-bit values?
		throw new RuntimeException("Attempted 16-bit read from GPU");
	}

	public void writeByte(char address, byte data) {
		switch (address) {

		// LCD control register
		case 0xFF40:
			getLcdControl().setValue(data);
			break;

		// SCY register
		case 0xFF42:
			getScrollY().setValue(data);
			break;

		// SCX register
		case 0xFF43:
			getScrollX().setValue(data);
			break;

		// current scanline register
		case 0xFF44:
			getLine().setValue(data);
			break;

		// current scanline register
		case 0xFF47:
			getBackgroundPalette().setValue(data);
			break;
		}
	}

	public void registerObserver(IGraphicsObserver o) {
		observers.add(o);
		logger.info("[+] Graphics observer registered: " + o);
	}

	public void notifyAllObservers() {
		logger.info("[~] GPU notifying all graphics observers");
		for (int i = 0; i < observers.size(); i++) {
			observers.get(i).updateDisplay();
			logger.fine("[+] Notifying observer: " + observers.get(i));
		}

	}

	// private void renderScanLine() {
	// boolean tileset = RegisterUtils.readBit(3, getLcdControl());
	// char address = (char) (RegisterUtils.readBit(3, getLcdControl()) ? 0x9C00
	// : 0x9800);
	// }

	public void renderFrame() {
		boolean tileset = RegisterUtils.readBit(3, getLcdControl());
		char address = (char) (RegisterUtils.readBit(4, getLcdControl()) ? 0x9C00 : 0x9800);
		byte[][] rendered = new byte[256][256];

		for (int i = 0; i < 1024; i++) {
			int x = (i % 32) * 8;
			int y = (i / 32) * 8;
			byte tileOffset = system.getMem().readByte((char) (address));
			byte[] tileData = GraphicsUtils.getTile(system.getMem(), tileset, tileOffset);
			byte[][] pixels = GraphicsUtils.mapTile(getBackgroundPalette().getValue(), tileData);

			for (int j = 0; j < 8; j++) {
				for (int k = 0; k < 8; k++) {
					rendered[x + k][y + j] = pixels[j][k];
				}
			}
			address++;
		}

		for (int i = 0; i < 160; i++) {
			for (int j = 0; j < 144; j++) {
				getFrameBuffer()[i][j] = rendered[i + getScrollX().getValue()][j + getScrollY().getValue()];
			}
		}
		notifyAllObservers();
	}

	public void incrementModeClock(byte time) {
		this.setModeClock(this.getModeClock() + time);
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

	public void setFrameBuffer(byte[][] frameBuffer) {
		this.frameBuffer = frameBuffer;
		notifyAllObservers();
	}

	public byte[][] getFrameBuffer() {
		return this.frameBuffer;
	}

	public Register getScrollX() {
		return scrollX;
	}

	private void setScrollX(Register scrollX) {
		this.scrollX = scrollX;
	}

	public Register getScrollY() {
		return scrollY;
	}

	private void setScrollY(Register scrollY) {
		this.scrollY = scrollY;
	}

	public Register getLine() {
		return line;
	}

	private void setLine(Register line) {
		this.line = line;
	}

	public Register getBackgroundPalette() {
		return backgroundPalette;
	}

	private void setBackgroundPalette(Register backgroundPalette) {
		this.backgroundPalette = backgroundPalette;
	}

	public Register getLcdControl() {
		return lcdControl;
	}

	private void setLcdControl(Register lcdControl) {
		this.lcdControl = lcdControl;
	}

	public int getModeClock() {
		return modeClock;
	}

	private void setModeClock(int modeClock) {
		this.modeClock = modeClock;
	}

	public Register getMode() {
		return mode;
	}

	private void setMode(Register mode) {
		this.mode = mode;
	}

}
