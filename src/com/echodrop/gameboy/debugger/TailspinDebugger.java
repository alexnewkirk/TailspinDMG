package com.echodrop.gameboy.debugger;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.logging.Level;

import com.echodrop.gameboy.core.MemoryRegion;
import com.echodrop.gameboy.core.Register;
import com.echodrop.gameboy.core.TailspinGB;
import com.echodrop.gameboy.core.Util;

/**
 * A simple command line debugger for the Tailspin emulator
 * 
 * @author echo_drop
 *
 */
public class TailspinDebugger {

	private static ArrayList<Breakpoint> breakpoints;
	private static Scanner sc = new Scanner(System.in);
	private static TailspinGB system = new TailspinGB();
	private static boolean running;
	private static final String SPACER = "------------------------------";
	private static ArrayList<Register> availableRegisters = new ArrayList<Register>();

	private static void init() {
		breakpoints = new ArrayList<Breakpoint>();
		running = true;
		system.reset();
	}

	public static void main(String[] args) {

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
			case SETBREAK:
				char bp;
				if (currentCommand.getArg() == null) {
					bp = system.getProcessor().getPc();
				} else {
					bp = currentCommand.getArg();
				}
				breakpoints.add(new Breakpoint(false, null, (byte)0, bp));
				System.out.println(SPACER);
				System.out.println("[!] Breakpoint added at 0x" + Integer.toHexString(bp & 0xFFFF).toUpperCase());
				System.out.println(SPACER);
				break;
			case CONTINUE:
				if(atBreakPoint()) {
					system.getProcessor().step();
				}
				while (!atBreakPoint()) {
					System.out.println(SPACER);
					system.getProcessor().step();
					System.out.println(SPACER);
				}
				char breakpoint = system.getProcessor().getPc();
				System.out.println("Reached breakpoint: " + Integer.toHexString(breakpoint & 0xFFFF).toUpperCase());
				break;
			case REGDUMP:
				System.out.println(SPACER);
				regDump();
				System.out.println(SPACER);
				break;
			case MEMDUMP:
				memDump();
				break;
			case LSBREAK:
				System.out.println(SPACER);
				for (Breakpoint b : breakpoints) {
					System.out.println(b);
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
			case FRAMEDUMP:
				framedump();
				break;
			case RMBREAK:
				breakpoints.remove(new Character(system.getProcessor().getPc()));
				break;
			case CONDBREAK:
				Breakpoint cBreak = readCondBreakpoint();
				breakpoints.add(cBreak);
				System.out.println("[+] Added breakpoint: ");
				System.out.println(cBreak);
				break;
			}

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
				System.out.print(Integer.toUnsignedString((fb[i][j]), 16) + " ");
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
		System.out.println("framedump: display the current framebuffer state");
		System.out.println("rmbreak: removes current instruction pointer from breakpoints");
	}

	private static void memDump() {

		ArrayList<String> options = new ArrayList<String>();
		options.add("BIOS");
		options.add("Working RAM");
		options.add("External RAM");
		options.add("Zero-page memory");
		options.add("ROM");
		options.add("OAM");
		options.add("VRAM");

		
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
		System.out.println(
				"GPU Background Palette: " + Util.byteToReadableHex(system.getGpu().getBackgroundPalette().value));
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
	
	private static Breakpoint readCondBreakpoint() {
		
		boolean valid = false;
		
		Breakpoint result = new Breakpoint();
		
		result.setConditional(true);
		
		while(!valid) {
			
			System.out.print("[breakpoint address in hex]> ");
			try {
				String input = sc.nextLine().toUpperCase();
				input = input.replace("0X", "");
				input = input.trim();
				result.setAddress((char)Integer.parseInt(input, 16));
			} catch(NumberFormatException e) {
				continue;
			}
			
			ArrayList<String> options = new ArrayList<String>();
			options.add("A");
			options.add("B");
			options.add("C");
			options.add("D");
			options.add("E");
			options.add("H");
			options.add("L");
			
			int registerSelected = getMenuSelection(options);
			
			result.setWatched(availableRegisters.get(registerSelected));
			
			Byte targetValue = null;
			
			while(targetValue == null) {
				System.out.print("[target value for register " + options.get(registerSelected) + "]> ");
				try {
					targetValue = (byte)Integer.parseInt(sc.nextLine(), 16);
				} catch (NumberFormatException e){
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
		
		while(choice < 0 || choice > options.size() - 1) {
			for(String s : options) {
				System.out.println("[" + options.indexOf(s) + "] " + s);
			}
			
			System.out.print("> ");
			
			try {
				choice = sc.nextInt();
			} catch(NumberFormatException e) {
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

}
