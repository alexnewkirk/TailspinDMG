/**
 * TailspinDebugger.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.debugger;

import java.util.ArrayList;

//import com.echodrop.gameboy.core.Register;
import com.echodrop.gameboy.core.TailspinGB;
import com.echodrop.gameboy.ui.swing.SwingScreen;
import com.echodrop.gameboy.util.GraphicsUtils;

/**
 * A simple command line debugger for the Tailspin emulator
 */
public class TailspinDebugger {

	private ArrayList<Breakpoint> breakpoints;
	private TailspinGB system;
	//private ArrayList<Register> availableRegisters = new ArrayList<Register>();
	private SwingScreen vid;

	public TailspinDebugger() {
		this.setSystem(new TailspinGB());
		init();
	}

	/**
	 * Initializes debugger
	 */
	public void init() {
		setBreakpoints(new ArrayList<Breakpoint>());
		getSystem().reset();
	}

	/**
	 * Writes a tile that looks like the letter 'A' into memory at 0x81a0. Used
	 * with vtiledmp to check that tiles are being rendered correctly
	 */
	public void tileWriteTest() {
		getSystem().getMem().writeByte((char) 0x81a0, (byte) 0x00);
		getSystem().getMem().writeByte((char) 0x81a1, (byte) 0x00);
		getSystem().getMem().writeByte((char) 0x81a2, (byte) 0x7e);
		getSystem().getMem().writeByte((char) 0x81a3, (byte) 0x7e);
		getSystem().getMem().writeByte((char) 0x81a4, (byte) 0x42);
		getSystem().getMem().writeByte((char) 0x81a5, (byte) 0x42);
		getSystem().getMem().writeByte((char) 0x81a6, (byte) 0x42);
		getSystem().getMem().writeByte((char) 0x81a7, (byte) 0x42);
		getSystem().getMem().writeByte((char) 0x81a8, (byte) 0x7e);
		getSystem().getMem().writeByte((char) 0x81a9, (byte) 0x7e);
		getSystem().getMem().writeByte((char) 0x81aa, (byte) 0x42);
		getSystem().getMem().writeByte((char) 0x81ab, (byte) 0x42);
		getSystem().getMem().writeByte((char) 0x81ac, (byte) 0x42);
		getSystem().getMem().writeByte((char) 0x81ad, (byte) 0x42);
		getSystem().getMem().writeByte((char) 0x81ae, (byte) 0x00);
		getSystem().getMem().writeByte((char) 0x81af, (byte) 0x00);
		getSystem().getGpu().notifyAllObservers();
	}

	/**
	 * Opens a Swing window that displays the emulation's framebuffer
	 */
	public void enableVideoMode() {
		if (vid == null) {
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					vid = new SwingScreen(getSystem());
					vid.setVisible(true);
				}
			});
		} else {
			vid.setVisible(true);
		}
	}

	public void addBreakpoint(Breakpoint bp) {
		getBreakpoints().add(bp);
	}

	public void clearBreakpoints() {
		getBreakpoints().clear();
	}

	/**
	 * Dumps tileset 1 to the framebuffer
	 * 
	 * @TODO: add support for tileset 0
	 */
	public void vTileDump() {
		enableVideoMode();
		byte[][] newFrameBuffer = new byte[160][144];
		for (int i = 0; i < 256; i++) {
			int tileX = i % 20;
			int tileY = i / 20;

			int x = tileX * 8;
			int y = tileY * 8;

			byte[][] tile = GraphicsUtils.mapTile(getSystem().getGpu().getBackgroundPalette().getValue(),
					GraphicsUtils.getTile(getSystem().getMem(), true, i));

			for (int j = 0; j < 8; j++) {
				for (int k = 0; k < 8; k++) {
					newFrameBuffer[x + j][y + k] = (byte) tile[k][j];
				}
			}
		}
		getSystem().getGpu().setFrameBuffer(newFrameBuffer);
	}

	public boolean atBreakPoint() {
		for (Breakpoint b : getBreakpoints()) {
			char pc = (char) (getSystem().getProcessor().getPc() & 0xFFFF);

			if (b.trigger(pc)) {
				return true;
			}
		}
		return false;
	}

	public TailspinGB getSystem() {
		return system;
	}

	public void setSystem(TailspinGB system) {
		this.system = system;
	}

	public ArrayList<Breakpoint> getBreakpoints() {
		return breakpoints;
	}

	public void setBreakpoints(ArrayList<Breakpoint> breakpoints) {
		this.breakpoints = breakpoints;
	}

}
