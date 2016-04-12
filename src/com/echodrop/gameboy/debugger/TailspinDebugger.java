/**
 * TailspinDebugger.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.debugger;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;

import com.echodrop.gameboy.core.MemoryRegion;
import com.echodrop.gameboy.core.Register;
import com.echodrop.gameboy.core.TailspinGB;
import com.echodrop.gameboy.core.Util;
import com.echodrop.gameboy.ui.SwingScreen;

/**
 * A simple command line debugger for the Tailspin emulator
 */
public class TailspinDebugger {

	private static ArrayList<Breakpoint> breakpoints;
	private static Scanner sc = new Scanner(System.in);
	private static TailspinGB system = new TailspinGB();
	private static boolean running;
	private static final String SPACER = "------------------------------";
	private static ArrayList<Register> availableRegisters = new ArrayList<Register>();
	private static boolean videoEnabled = false;
	private static SwingScreen vid;

	/**
	 * Initializes debugger
	 */
	private static void init() {
		breakpoints = new ArrayList<Breakpoint>();
		running = true;
		system.reset();
	}

	public static void main(String[] args) {
		System.out.println("[Tailspin Debugger]");
		System.out.println("Type 'help' for a list of commands.");
		System.out.println("hint: begin by loading a ROM.\n\n");
		init();
		loadRegisters();

		while (running) {
			DebugCommand currentCommand = readCommand();

			switch (currentCommand.getCommand()) {
			case HELP:
				System.out.println(SPACER);
				showHelp();
				System.out.println(SPACER);
				break;
			case EXIT:
				running = false;
				break;
			case STEP:
				System.out.println(SPACER);
				system.getProcessor().step();
				System.out.println(SPACER);
				break;
			case SETBRK:
				char bp;
				if (currentCommand.getArg() == null) {
					bp = system.getProcessor().getPc();
				} else {
					bp = currentCommand.getArg();
				}
				breakpoints.add(new Breakpoint(false, null, (byte) 0, bp));
				System.out.println(SPACER);
				System.out.println("[!] Breakpoint added at 0x" + Integer.toHexString(bp & 0xFFFF).toUpperCase());
				System.out.println(SPACER);
				break;
			case CONTINUE:
				if (atBreakPoint()) {
					system.getProcessor().step();
				}
				long start = System.currentTimeMillis();
				boolean spacer = system.getLogger().getLevel() != Level.OFF;
				while (!atBreakPoint()) {
					if (spacer) {
						System.out.println(SPACER);
					}

					system.getProcessor().step();

					if (spacer) {
						System.out.println(SPACER);
					}
				}
				char breakpoint = system.getProcessor().getPc();
				System.out.println(SPACER);
				System.out.println("[!] Reached breakpoint: 0x" + Integer.toHexString(breakpoint & 0xFFFF).toUpperCase()
						+ " in " + (System.currentTimeMillis() - start) / 1000f + " seconds.");
				System.out.println(SPACER);
				break;
			case REGDMP:
				System.out.println(SPACER);
				regDump();
				System.out.println(SPACER);
				break;
			case MEMDMP:
				memDump();
				break;
			case LSBRK:
				System.out.println(SPACER);
				for (Breakpoint b : breakpoints) {
					System.out.println(b);
				}
				System.out.println(SPACER);
				break;
			case RESET:
				breakpoints.clear();
				init();
				break;
			case LOGALL:
				system.getLogger().setLevel(Level.ALL);
				System.out.println("[~] Log level: All");
				break;
			case LOGNONE:
				system.getLogger().setLevel(Level.OFF);
				System.out.println("[~] Log level: Off");
				break;
			case LOGINFO:
				system.getLogger().setLevel(Level.INFO);
				System.out.println("[~] Log level: Info");
			case LOADROM:
				system.getMem().loadRom(getRomFilename());
				break;
			case FRAMEDMP:
				framedump();
				break;
			case CONDBRK:
				Breakpoint cBreak = readCondBreakpoint();
				breakpoints.add(cBreak);
				System.out.println(SPACER);
				System.out.println("[+] Added breakpoint: ");
				System.out.println(cBreak);
				System.out.println(SPACER);
				break;
			case CLRBRK:
				breakpoints.clear();
				System.out.println("[!] Cleared all breakpoints");
				break;
			case TILEDMP:
				tiledump();
				break;
			case VTILEDMP:
				vTileDump();
				break;
			case VIDEO:
				enableVideoMode();
				break;
			case TILEWRITETEST:
				tileWriteTest();
				break;
			case RENDER:
				system.getGpu().renderFrame();
				break;
			}
		}
	}

	private static void tileWriteTest() {
		system.getMem().writeByte((char) 0x81a0, (byte) 0x00);
		system.getMem().writeByte((char) 0x81a1, (byte) 0x00);
		system.getMem().writeByte((char) 0x81a2, (byte) 0x7e);
		system.getMem().writeByte((char) 0x81a3, (byte) 0x7e);
		system.getMem().writeByte((char) 0x81a4, (byte) 0x42);
		system.getMem().writeByte((char) 0x81a5, (byte) 0x42);
		system.getMem().writeByte((char) 0x81a6, (byte) 0x42);
		system.getMem().writeByte((char) 0x81a7, (byte) 0x42);
		system.getMem().writeByte((char) 0x81a8, (byte) 0x7e);
		system.getMem().writeByte((char) 0x81a9, (byte) 0x7e);
		system.getMem().writeByte((char) 0x81aa, (byte) 0x42);
		system.getMem().writeByte((char) 0x81ab, (byte) 0x42);
		system.getMem().writeByte((char) 0x81ac, (byte) 0x42);
		system.getMem().writeByte((char) 0x81ad, (byte) 0x42);
		system.getMem().writeByte((char) 0x81ae, (byte) 0x00);
		system.getMem().writeByte((char) 0x81af, (byte) 0x00);
		system.getGpu().notifyAllObservers();
	}

	private static void enableVideoMode() {
		if (!videoEnabled) {
			videoEnabled = true;

			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					vid = new SwingScreen(system);
					vid.setVisible(true);
				}
			});

		}
	}

	private static void vTileDump() {
		enableVideoMode();
		byte[][] newFrameBuffer = new byte[160][144];
		for (int i = 0; i < 256; i++) {
			int tileX = i % 20;
			int tileY = i / 20;

			int x = tileX * 8;
			int y = tileY * 8;

			byte[][] tile = Util.mapTile(system.getGpu().getBackgroundPalette().getValue(),
					Util.getTile(system.getMem(), true, i));

			for (int j = 0; j < 8; j++) {
				for (int k = 0; k < 8; k++) {
					newFrameBuffer[x + j][y + k] = (byte) tile[j][k];
				}
			}
		}
		system.getGpu().setFrameBuffer(newFrameBuffer);
	}

	private static void tiledump() {
		for (int i = 0; i < 256; i++) {
			System.out.println("Tile " + i + ":");
			byte[] tile = Util.getTile(system.getMem(), true, i);
			byte[][] tileData = Util.mapTile(system.getGpu().getBackgroundPalette().getValue(), tile);
			// row
			int rowCount = 0;
			for (int k = 0; k < 16; k += 2) {
				// pixel within row
				for (int l = 0; l < 8; l++) {
					// System.out.print(row[l]);
					System.out.print(tileData[rowCount][l]);
				}
				rowCount++;
				System.out.println();
			}
			sc.nextLine();
		}
	}

	private static void loadRegisters() {
		availableRegisters.add(system.getProcessor().getA());
		availableRegisters.add(system.getProcessor().getB());
		availableRegisters.add(system.getProcessor().getC());
		availableRegisters.add(system.getProcessor().getD());
		availableRegisters.add(system.getProcessor().getE());
		availableRegisters.add(system.getProcessor().getH());
		availableRegisters.add(system.getProcessor().getL());
	}

	private static void framedump() {
		byte[][] fb = system.getGpu().getFrameBuffer();
		for (int i = 0; i < fb.length; i++) {
			for (int j = 0; j < fb[0].length; j++) {
				System.out.print((fb[i][j]) & 7);
			}
			System.out.println();
		}
	}

	private static String getRomFilename() {
		String filename = "";

		while (filename.isEmpty() || !filename.contains(".gb")) {
			System.out.print("[tdbg] Enter ROM filename:");
			filename = sc.nextLine();
		}

		return filename;
	}

	private static void showHelp() {
		System.out.println("help: show this command list");
		System.out.println("step: advance emulator by one instruction");
		System.out.println("setbrk [memory address in hexadecimal]: set a new breakpoint at the specified address");
		System.out.println("setbrk: set a new breakpoint at the current memory address");
		System.out.println("continue: run emulator until next breakpoint is reached");
		System.out.println("exit: quit tdbg");
		System.out.println("logall: set emulator logging mode to Level.ALL");
		System.out.println("loginfo: set emulator logging mode to Level.INFO");
		System.out.println("lognone: set emulator logging mode to Level.OFF");
		System.out.println("reset: initialize emulator");
		System.out.println("loadrom: load a new gameboy rom into the emulator");
		System.out.println("lsbrk: list all breakpoints");
		System.out.println("regdmp: display values of all registers");
		System.out.println("memdmp: display memory dump of emulator's current state");
		System.out.println("framedmp: display the current framebuffer state");
		System.out.println("condbrk: add a new conditional breakpoint");
		System.out.println("clrbrk: clear all breakpoints");
		System.out.println("tiledmp: display tileset data in text format");
		System.out.println("vtiledmp: render tileset to framebuffer");
		System.out.println("video: enable video mode");
		System.out.println("render: draw framebuffer to screen");

	}

	private static void memDump() {

		ArrayList<String> options = new ArrayList<String>();
		options.add("BIOS (0x0000 - 0x00FF)");
		options.add("Working RAM (0xC000 - 0xDFFF)");
		options.add("External RAM");
		options.add("Zero-page memory (0xFF80 - 0xFFFF)");
		options.add("ROM (0x0000 - 0x3FFF)");
		options.add("OAM (0xFE00 - 0xFE9F");
		options.add("VRAM (0x8000 - 0x9FFF)");

		MemoryRegion selected = null;

		int choice = getMenuSelection(options);

		switch (choice) {
		case 0:
			selected = system.getMem().getBios();
			break;
		case 1:
			selected = system.getMem().getWorkingRam();
			break;
		case 2:
			selected = system.getMem().getExternalRam();
			break;
		case 3:
			selected = system.getMem().getZeroPage();
			break;
		case 4:
			selected = system.getMem().getRom();
			break;
		case 5:
			selected = system.getGpu().getOam();
			break;
		case 6:
			selected = system.getGpu().getVram();
			break;
		}

		if (selected != null) {
			System.out.println(selected);
		}

	}

	private static void regDump() {
		System.out.println("PC:" + Util.charToReadableHex(system.getProcessor().getPc()));
		System.out.println("SP:" + Util.charToReadableHex(system.getProcessor().getSp()));
		System.out.println("A: " + Util.byteToReadableHex(system.getProcessor().getA().getValue()));
		System.out.println("B: " + Util.byteToReadableHex(system.getProcessor().getB().getValue()));
		System.out.println("C: " + Util.byteToReadableHex(system.getProcessor().getC().getValue()));
		System.out.println("D: " + Util.byteToReadableHex(system.getProcessor().getD().getValue()));
		System.out.println("E: " + Util.byteToReadableHex(system.getProcessor().getE().getValue()));
		System.out.println("H: " + Util.byteToReadableHex(system.getProcessor().getH().getValue()));
		System.out.println("L: " + Util.byteToReadableHex(system.getProcessor().getL().getValue()));
		System.out.println("T clock: " + Util.byteToReadableHex(system.getProcessor().getClockT().getValue()));
		System.out.println("M clock: " + Util.byteToReadableHex(system.getProcessor().getClockM().getValue()));
		System.out.println("Zero flag: " + system.getProcessor().isZeroFlag());
		System.out.println("Operation flag: " + system.getProcessor().isOperationFlag());
		System.out.println("Half Carry flag: " + system.getProcessor().isHalfCarryFlag());
		System.out.println("Full Carry flag: " + system.getProcessor().isFullCarryFlag());
		System.out.println("Conditional non-exec flag: " + system.getProcessor().isConditionalNotExecFlag());
		System.out.println("GPU ScrollX: " + Util.byteToReadableHex(system.getGpu().getScrollX().getValue()));
		System.out.println("GPU ScrollY: " + Util.byteToReadableHex(system.getGpu().getScrollY().getValue()));
		System.out.println("GPU Scanline: " + Util.byteToReadableHex(system.getGpu().getLine().getValue()));
		System.out.println(
				"GPU Background Palette: " + Util.byteToReadableHex(system.getGpu().getBackgroundPalette().getValue()));
		System.out.println("GPU LCD Control: " + Util.byteToReadableHex(system.getGpu().getLcdControl().getValue()));
		System.out.println("GPU Mode: " + Util.byteToReadableHex(system.getGpu().getMode().getValue()));
		System.out.println("GPU Modeclock: " + system.getGpu().getModeClock());

	}

	private static DebugCommand readCommand() {
		DebugCommand c = null;

		while (true) {
			System.out.print("\n[tdbg@" + Util.charToReadableHex(system.getProcessor().getPc()) + "]> ");
			String input = sc.nextLine().toUpperCase();
			DebugCommandType commandType = null;
			Character argument = null;

			for (DebugCommandType dct : DebugCommandType.values()) {

				if (input.contains(dct.name())) {

					commandType = dct;

					String remaining = input.replace(dct.toString(), "");

					remaining = remaining.replace("0X", "");

					remaining = remaining.trim();

					if (!remaining.isEmpty()) {

						try {

							argument = (char) Integer.parseInt(remaining, 16);

						} catch (NumberFormatException e) {
							// invalid or no argument
						}

					}

					c = new DebugCommand(commandType, argument);
					return c;
				}
			}
			if(!input.isEmpty()) {
				System.out.println("[!] Invalid command: '" + input + "'");
			}
		}
	}

	private static Breakpoint readCondBreakpoint() {
		boolean valid = false;
		Breakpoint result = new Breakpoint();
		result.setConditional(true);

		while (!valid) {
			result.setAddress(readHexAddress());
			ArrayList<String> breakpointOptions = new ArrayList<String>();
			breakpointOptions.add("Watch register...");
			breakpointOptions.add("Watch memory address...");

			switch (getMenuSelection(breakpointOptions)) {
			case 0:
				ArrayList<String> registerOptions = new ArrayList<String>();
				registerOptions.add("A");
				registerOptions.add("B");
				registerOptions.add("C");
				registerOptions.add("D");
				registerOptions.add("E");
				registerOptions.add("H");
				registerOptions.add("L");
				int registerSelected = getMenuSelection(registerOptions);
				result.setWatched(availableRegisters.get(registerSelected));
				break;
			case 1:
				result.setWatched(new MemoryBlock(system, readHexAddress()));
				break;
			}

			Byte targetValue = null;
			while (targetValue == null) {
				System.out.print("[target value]> ");
				try {
					targetValue = (byte) Integer.parseInt(sc.nextLine(), 16);
				} catch (NumberFormatException e) {
					System.out.println("Invalid input, try again.");
				}
			}
			result.setTargetValue(targetValue);
			valid = true;
		}
		return result;
	}

	private static int getMenuSelection(ArrayList<String> options) {
		int choice = -1;

		while (choice < 0 || choice > options.size() - 1) {
			for (String s : options) {
				System.out.println("[" + options.indexOf(s) + "] " + s);
			}
			System.out.print("> ");
			try {
				choice = sc.nextInt();
			} catch (NumberFormatException e) {
				System.out.println("[!] Invalid input, try again.");
			} finally {
				sc.nextLine();
			}
		}
		return choice;
	}

	private static boolean atBreakPoint() {
		for (Breakpoint b : breakpoints) {
			char pc = (char) (system.getProcessor().getPc() & 0xFFFF);

			if (b.trigger(pc)) {
				return true;
			}
		}
		return false;
	}

	private static char readHexAddress() {
		Character result = null;

		while (result == null) {
			System.out.print("[memory address in hex]> ");
			String input = sc.nextLine().toUpperCase();
			input = input.replace("0X", "");
			input = input.trim();
			if (!input.isEmpty()) {
				try {
					result = (char) Integer.parseInt(input, 16);
				} catch (NumberFormatException e) {
					// invalid or no argument
				}
			}
		}
		return result;
	}

}
