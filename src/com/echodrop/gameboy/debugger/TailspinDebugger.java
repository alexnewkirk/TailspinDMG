package com.echodrop.gameboy.debugger;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;

import com.echodrop.gameboy.core.TailspinGB;
import com.echodrop.gameboy.core.Util;

/**
 * A simple command line debugger for the Tailspin emulator
 * 
 * @author echo_drop
 *
 */
public class TailspinDebugger {

	private static ArrayList<Character> breakpoints;
	private static Scanner sc = new Scanner(System.in);
	private static TailspinGB system = new TailspinGB();
	private static boolean running;
	private static final String SPACER = "------------------------------";

	private static void init() {
		breakpoints = new ArrayList<Character>();
		running = true;

		system.reset();
	}

	public static void main(String[] args) {

		init();

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
			case SETBREAK:
				char bp;
				if (currentCommand.getArg() == null) {
					bp = system.getProcessor().getPc();
				} else {
					bp = currentCommand.getArg();
				}
				breakpoints.add(bp);
				System.out.println(SPACER);
				System.out.println("[!] Breakpoint added at 0x" + Integer.toHexString(bp & 0xFFFF).toUpperCase());
				System.out.println(SPACER);
				break;
			case CONTINUE:
				while (!atBreakPoint()) {
					System.out.println(SPACER);
					system.getProcessor().step();
					System.out.println(SPACER);
				}
				System.out.println("Reached breakpoint: "
						+ Integer.toHexString(system.getProcessor().getPc() & 0xFFFF).toUpperCase());
				break;
			case REGDUMP:
				System.out.println(SPACER);
				regDump();
				System.out.println(SPACER);
				break;
			case MEMDUMP:
				System.out.println(SPACER);
				memDump();
				System.out.println(SPACER);
				break;
			case LSBREAK:
				System.out.println(SPACER);
				for (Character c : breakpoints) {
					System.out.println("0x" + Integer.toHexString(c & 0xFFFF).toUpperCase());
				}
				System.out.println(SPACER);
				break;
			case RESET:
				init();
				break;
			case STARTLOG:
				system.getLogger().setLevel(Level.ALL);
				break;
			case STOPLOG:
				system.getLogger().setLevel(Level.INFO);
				break;
			case LOADROM:
				system.getMem().loadRom(getRomFilename());
				break;

			}

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
		System.out.println("help: show this help info");
		System.out.println("step: advance emulator by one instruction");
		System.out.println("setbreak [memory address in hexadecimal]: set a new breakpoint at the specified address");
		System.out.println("continue: run emulator until next breakpoint is reached");
		System.out.println("exit: quit tdbg");
		System.out.println("startlog: set emulator logging mode to Level.ALL");
		System.out.println("stoplog: set emulator logging mode to Level.INFO");
		System.out.println("reset: initialize emulator");
		System.out.println("loadrom: load a new gameboy rom into the emulator");
		System.out.println("lsbreak: list all breakpoints");
		System.out.println("regdump: display values of all registers");
		System.out.println("memdump: display memory dump of emulator's current state");
		
	}

	private static void memDump() {
		System.out.println("[!] Memdump not yet implemented.");
	}

	private static void regDump() {
		System.out.println("PC: " + Util.charToReadableHex(system.getProcessor().getPc()));
		System.out.println("SP: " + Util.charToReadableHex(system.getProcessor().getSp()));
		System.out.println("A: " + Util.byteToReadableHex(system.getProcessor().getA().value));
		System.out.println("B: " + Util.byteToReadableHex(system.getProcessor().getB().value));
		System.out.println("C: " + Util.byteToReadableHex(system.getProcessor().getC().value));
		System.out.println("D: " + Util.byteToReadableHex(system.getProcessor().getD().value));
		System.out.println("E: " + Util.byteToReadableHex(system.getProcessor().getE().value));
		System.out.println("H: " + Util.byteToReadableHex(system.getProcessor().getH().value));
		System.out.println("L: " + Util.byteToReadableHex(system.getProcessor().getL().value));
		System.out.println("T clock: " + Util.byteToReadableHex(system.getProcessor().getClockT().value));
		System.out.println("M clock: " + Util.byteToReadableHex(system.getProcessor().getClockM().value));
		System.out.println("Zero flag: " + system.getProcessor().isZeroFlag());
		System.out.println("Operation flag: " + system.getProcessor().isOperationFlag());
		System.out.println("Half Carry flag: " + system.getProcessor().isHalfCarryFlag());
		System.out.println("Full Carry flag: " + system.getProcessor().isFullCarryFlag());
		System.out.println("Conditional non-exec flag: " + system.getProcessor().isConditionalNotExecFlag());
		System.out.println("GPU ScrollX: " + Util.byteToReadableHex(system.getGpu().getScrollX().value));
		System.out.println("GPU ScrollY: " + Util.byteToReadableHex(system.getGpu().getScrollY().value));
		System.out.println("GPU Scanline: " + Util.byteToReadableHex(system.getGpu().getLine().value));
		System.out.println("GPU Background Palette: " + Util.byteToReadableHex(system.getGpu().getBackgroundPalette().value));
		System.out.println("GPU LCD Control: " + Util.byteToReadableHex(system.getGpu().getLcdControl().value));
		System.out.println("GPU ScrollY: " + Util.byteToReadableHex(system.getGpu().getScrollY().value));
		System.out.println("GPU Mode: " + Util.byteToReadableHex(system.getGpu().getMode().value));
		System.out.println("GPU Modeclock: " + system.getGpu().getModeClock());
		
	}

	private static DebugCommand readCommand() {

		DebugCommand c = null;

		while (true) {
			System.out.print("\n[tdbg]> ");
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

		}

	}

	private static boolean atBreakPoint() {

		for (Character c : breakpoints) {
			char pc = (char) (system.getProcessor().getPc() & 0xFFFF);

			if ((c.charValue() & 0xFFFF) == (pc)) {
				return true;
			}
		}
		return false;
	}

}
