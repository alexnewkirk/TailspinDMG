/**
 * DebuggerCLI.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.ui.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;

import com.echodrop.gameboy.core.CPU;
import com.echodrop.gameboy.core.MMU;
import com.echodrop.gameboy.core.MemoryRegion;
import com.echodrop.gameboy.core.Register;
import com.echodrop.gameboy.core.TailspinGB;
import com.echodrop.gameboy.debugger.Breakpoint;
import com.echodrop.gameboy.debugger.DebugAction;
import com.echodrop.gameboy.debugger.DebugCommand;
import com.echodrop.gameboy.debugger.MemoryBlock;
import com.echodrop.gameboy.debugger.TailspinDebugger;
import com.echodrop.gameboy.exceptions.MapperNotImplementedException;
import com.echodrop.gameboy.graphics.GPU;
import com.echodrop.gameboy.logging.SimpleConsoleLogger;
import com.echodrop.gameboy.util.FileUtils;
import com.echodrop.gameboy.util.GraphicsUtils;
import com.echodrop.gameboy.util.StringUtils;

public class DebuggerCLI {

	private static Scanner sc;
	private static TailspinDebugger tdb;
	private static TailspinGB system;
	private static final String DEFAULT_BIOS_PATH = "bios.gb";

	public static void main(String[] args) {
		sc = new Scanner(System.in);
		tdb = new TailspinDebugger();
		system = tdb.getSystem();

		try {
			byte[] bios = null;
			bios = FileUtils.readBytes(DEFAULT_BIOS_PATH);
			system.getMem().loadBootstrap(bios);
		} catch (IOException e) {
			System.err.print("[!] Unable to load BIOS from default path: " + DEFAULT_BIOS_PATH);
		}

		system.initLogging(Level.OFF, new SimpleConsoleLogger());
		System.out.println("[Tailspin Debugger]");
		
		int opcodeCount = system.getProcessor().getOpcodeCount();
		System.out.println("Opcodes Implemented: " + 
							 + opcodeCount +
							"/" + 244 + ": " + opcodeCount/244f*100 + "%");
		
		int cbOpcodeCount = system.getProcessor().getCbOpcodeCount();
		System.out.println("Extended Opcodes Implemented: " + 
				 + cbOpcodeCount +
				"/" + 255 + ": " + cbOpcodeCount/255f*100 + "%");
		
		System.out.println();
		System.out.println("Type 'help' for a list of commands.");
		System.out.println("hint: begin by loading a ROM.\n\n");

		while (true) {
			DebugCommand dc = readCommand();
			runCommand(dc);
		}
	}

	/**
	 * Executes a DebugCommand
	 */
	private static void runCommand(DebugCommand dc) {
		switch (dc.getCommand()) {
		case STEP:
			tdb.getSystem().getProcessor().step();
			break;
		case SETBRK:
			char bp;
			if (dc.getArg() == null) {
				bp = tdb.getSystem().getProcessor().getPc();
			} else {
				bp = dc.getArg();
			}
			tdb.addBreakpoint((new Breakpoint(false, null, (byte) 0, bp)));
			System.out.println("[!] Breakpoint added at 0x" + Integer.toHexString(bp & 0xFFFF).toUpperCase());
			break;
		case CONTINUE:
			if (tdb.atBreakPoint()) {
				tdb.getSystem().getProcessor().step();
			}
			long start = System.currentTimeMillis();

			while (!tdb.atBreakPoint()) {
				tdb.getSystem().getProcessor().step();
			}
			char breakpoint = system.getProcessor().getPc();
			System.out.println("[!] Reached breakpoint: 0x" + Integer.toHexString(breakpoint & 0xFFFF).toUpperCase()
					+ " in " + (System.currentTimeMillis() - start) / 1000f + " seconds.");
			break;
		case REGDMP:
			regDump();
			break;
		case MEMDMP:
			memDump();
			break;
		case LSBRK:
			for (Breakpoint b : tdb.getBreakpoints()) {
				System.out.println(b);
			}
			break;
		case RESET:
			tdb.clearBreakpoints();
			tdb.init();
			break;
		case LOGALL:
			tdb.getSystem().getLogger().setLevel(Level.ALL);
			System.out.println("[~] Log level: All");
			break;
		case NOLOG:
			tdb.getSystem().getLogger().setLevel(Level.OFF);
			System.out.println("[~] Log level: Off");
			break;
		case LOGINFO:
			tdb.getSystem().getLogger().setLevel(Level.INFO);
			System.out.println("[~] Log level: Info");
			break;
		case LOADROM:
			byte[] rom;
			try {
				rom = FileUtils.readBytes(readFilename());
				tdb.getSystem().getMem().loadRom(rom);
			} catch (IOException e) {
				tdb.getSystem().getLogger().severe("[!] Unable to load rom: " + e.getMessage());
			} catch (MapperNotImplementedException me) {
				tdb.getSystem().getLogger().severe("[!] rom " + me.getMessage() + " uses an unsupported mapper.");
			}
			break;
		case LOADBIOS:
			byte[] bios;
			try {
				bios = FileUtils.readBytes(readFilename());
				tdb.getSystem().getMem().loadBootstrap(bios);
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
			break;
		case FRAMEDMP:
			framedump();
			break;
		case CONDBRK:
			Breakpoint cBreak = readCondBreakpoint();
			tdb.addBreakpoint(cBreak);
			System.out.println("[+] Added breakpoint: ");
			System.out.println(cBreak);
			break;
		case CLRBRK:
			tdb.clearBreakpoints();
			System.out.println("[!] Cleared all breakpoints");
			break;
		case VTILEDMP:
			tdb.vTileDump();
			break;
		case TILEDMP:
			tiledump();
			break;
		case VIDEO:
			tdb.enableVideoMode();
			break;
		case TILEWRITETEST:
			tdb.tileWriteTest();
			break;
		case RENDER:
			tdb.getSystem().getGpu().renderFrame();
			break;
		case HELP:
			showHelp();
			break;
		case EXIT:
			System.exit(0);
			break;
		default:
			break;
		}
	}

	/**
	 * Dumps the contents of the framebuffer in text mode to the console.
	 */
	private static void framedump() {
		byte[][] fb = tdb.getSystem().getGpu().getFrameBuffer();
		for (int i = 0; i < fb.length; i++) {
			for (int j = 0; j < fb[0].length; j++) {
				System.out.print((fb[i][j]) & 7);
			}
			System.out.println();
		}
	}

	/**
	 * Dumps tileset 1 in text mode to the console
	 */
	private static void tiledump() {
		for (int i = 0; i < 256; i++) {
			System.out.println("Tile " + i + ":");
			byte[] tile = GraphicsUtils.getTile(tdb.getSystem().getMem(), true, i);
			byte[][] tileData = GraphicsUtils.mapTile(tdb.getSystem().getGpu().getBackgroundPalette().getValue(), tile);
			// row
			int rowCount = 0;
			for (int k = 0; k < 16; k += 2) {
				// pixel within row
				for (int l = 0; l < 8; l++) {
					System.out.print(tileData[rowCount][l]);
				}
				rowCount++;
				System.out.println();
			}
			sc.nextLine();
		}
	}

	/**
	 * Prints the values of all system registers
	 */
	private static void regDump() {
		CPU p = tdb.getSystem().getProcessor();
		GPU g = tdb.getSystem().getGpu();

		System.out.println("PC:" + StringUtils.charToReadableHex(p.getPc()));
		System.out.println("SP:" + StringUtils.charToReadableHex(p.getSp()));
		System.out.println("A: " + StringUtils.byteToReadableHex(p.getA().getValue()));
		System.out.println("B: " + StringUtils.byteToReadableHex(p.getB().getValue()));
		System.out.println("C: " + StringUtils.byteToReadableHex(p.getC().getValue()));
		System.out.println("D: " + StringUtils.byteToReadableHex(p.getD().getValue()));
		System.out.println("E: " + StringUtils.byteToReadableHex(p.getE().getValue()));
		System.out.println("H: " + StringUtils.byteToReadableHex(p.getH().getValue()));
		System.out.println("L: " + StringUtils.byteToReadableHex(p.getL().getValue()));
		System.out.println("T clock: " + StringUtils.byteToReadableHex(p.getClockT().getValue()));
		System.out.println("M clock: " + StringUtils.byteToReadableHex(p.getClockM().getValue()));
		System.out.println("Zero flag: " + p.isZeroFlag());
		System.out.println("Operation flag: " + p.isOperationFlag());
		System.out.println("Half Carry flag: " + p.isHalfCarryFlag());
		System.out.println("Full Carry flag: " + p.isFullCarryFlag());
		System.out.println("Conditional non-exec flag: " + p.isConditionalNotExecFlag());
		System.out.println("BIOS mapped: " + tdb.getSystem().getMem().isBiosMapped());
		System.out.println("GPU ScrollX: " + StringUtils.byteToReadableHex(g.getScrollX().getValue()));
		System.out.println("GPU ScrollY: " + StringUtils.byteToReadableHex(g.getScrollY().getValue()));
		System.out.println("GPU Scanline: " + StringUtils.byteToReadableHex(g.getLine().getValue()));
		System.out.println(
				"GPU Background Palette: " + StringUtils.byteToReadableHex(g.getBackgroundPalette().getValue()));
		System.out.println("GPU LCD Control: " + StringUtils.byteToReadableHex(g.getLcdControl().getValue()));
		System.out.println("GPU Mode: " + StringUtils.byteToReadableHex(g.getMode().getValue()));
		System.out.println("GPU Modeclock: " + g.getModeClock());
	}

	private static void memDump() {
		ArrayList<String> options = new ArrayList<String>();
		MemoryRegion selected = null;
		MMU m = tdb.getSystem().getMem();
		GPU g = tdb.getSystem().getGpu();
		options.add("BIOS (0x0000 - 0x00FF)");
		options.add("Working RAM (0xC000 - 0xDFFF)");
		options.add("External RAM");
		options.add("Zero-page memory (0xFF80 - 0xFFFF)");
		options.add("ROM (0x0000 - 0x3FFF)");
		options.add("OAM (0xFE00 - 0xFE9F");
		options.add("VRAM (0x8000 - 0x9FFF)");

		int choice = getMenuSelection(options);

		switch (choice) {
		case 0:
			selected = tdb.getSystem().getMem().getBios();
			break;
		case 1:
			selected = m.getWorkingRam();
			break;
		case 2:
			selected = m.getExternalRam();
			break;
		case 3:
			selected = m.getZeroPage();
			break;
		case 4:
			selected = m.getRomBank0();
			break;
		case 5:
			selected = g.getOam();
			break;
		case 6:
			selected = g.getVram();
			break;
		}

		if (selected != null) {
			System.out.println(selected);
		}
	}

	private static int getMenuSelection(ArrayList<String> options) {
		int choice = -1;

		while (choice < 0 || choice > options.size() - 1) {
			for (String s : options) {
				System.out.println("[" + options.indexOf(s) + "] " + s);
			}
			System.out.print("> ");
			try {
				choice = Integer.parseInt(sc.nextLine());
			} catch (NumberFormatException e) {
				System.out.println("[!] Invalid input, try again.");
			}
		}
		return choice;
	}

	private static DebugCommand readCommand() {
		DebugCommand c = null;

		while (true) {
			System.out
					.print("\n[tdbg@" + StringUtils.charToReadableHex(tdb.getSystem().getProcessor().getPc()) + "] > ");
			String input = sc.nextLine().toUpperCase();
			DebugAction commandType = null;
			Character argument = null;
			for (DebugAction dct : DebugAction.values()) {
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
			if (!input.isEmpty()) {
				System.out.println("[!] Invalid command: '" + input + "'");
			}
		}
	}

	/**
	 * Prompts user for a memory address
	 */
	private static char readHexAddress() {
		Character result = null;
		while (result == null) {
			System.out.print("[memory address in hex] > ");
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

	/**
	 * Prompts the user for the info needed to create a new conditional
	 * breakpoint
	 * 
	 * @return
	 */
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
				Register r = null;
				CPU processor = system.getProcessor();
				switch (registerSelected) {

				case 0:
					r = processor.getA();
					break;
				case 1:
					r = processor.getB();
					break;
				case 2:
					r = processor.getC();
					break;
				case 3:
					r = processor.getD();
					break;
				case 4:
					r = processor.getE();
					break;
				case 5:
					r = processor.getH();
					break;
				case 6:
					r = processor.getL();
					break;
				}
				result.setWatched(r);
				break;
			case 1:
				result.setWatched(new MemoryBlock(tdb.getSystem(), readHexAddress()));
				break;
			}

			Byte targetValue = null;
			while (targetValue == null) {
				System.out.print("[target value] > ");
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

	/**
	 * Prompts the user for a filename that ends in '.gb'. Does /not/ validate
	 * said filename.
	 * 
	 * @return
	 */
	private static String readFilename() {
		String filename = "";

		while (filename.isEmpty()) {
			System.out.print("[tdbg] Enter filename >");
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
		System.out.println("nolog: set emulator logging mode to Level.OFF");
		System.out.println("reset: initialize emulator");
		System.out.println("loadrom: load a new gameboy rom into the emulator");
		System.out.println("loadbios: load gameboy boot rom into the emulator");
		System.out.println("lsbrk: list all breakpoints");
		System.out.println("regdmp: display values of all registers");
		System.out.println("memdmp: display memory dump of emulator's current state");
		System.out.println("framedmp: display text representation of current framebuffer state");
		System.out.println("condbrk: add a new conditional breakpoint");
		System.out.println("clrbrk: clear all breakpoints");
		System.out.println("tiledmp: display tileset data in text format");
		System.out.println("vtiledmp: render tileset to framebuffer");
		System.out.println("video: enable video mode");
		System.out.println("render: draw framebuffer to screen");
	}

}
