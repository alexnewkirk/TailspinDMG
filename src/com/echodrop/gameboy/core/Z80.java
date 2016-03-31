package com.echodrop.gameboy.core;

import java.util.HashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.echodrop.gameboy.logging.CliHandler;

/**
 * Emulation core for Z80 microprocessor
 * 
 * @author echo_drop
 *
 */
public class Z80 {

	/**
	 * An 8 bit register
	 */
	class Register {

		byte value;

		public Register(byte value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return Integer.toHexString(value & 0xFF);
		}

	}

	private GameBoy system;
	private static final Logger logger = Logger.getLogger(Z80.class.getName());

	// CPU registers
	Register a;
	Register b;
	Register c;
	Register d;
	Register e;
	Register h;
	Register l;

	// Flag register, stored as boolean
	// values for convenience
	boolean zeroFlag;
	boolean operationFlag;
	boolean halfCarryFlag;
	boolean fullCarryFlag;

	// This flag is not present in the actual
	// hardware, it's here for convenience.
	// Set to true if a conditional instruction
	// is not run, and the op's smaller t value
	// should be added to the timer. Reset
	// after each instruction.
	boolean conditionalNotExecFlag;

	// Special registers
	public char pc; // program counter
	char sp; // stack pointer

	// Clocks
	Register clock_t;
	Register clock_m;

	// Clock registers
	Register t;
	Register m;

	// Memory Management Unit
	MMU mem;

	boolean running;

	// Opcode tables
	HashMap<Byte, OpCode> opCodes;
	HashMap<Byte, OpCode> cbOpCodes;

	public Z80(GameBoy system) {
		this.initialize();

		this.system = system;
		this.mem = system.getMem();

		this.opCodes = new HashMap<Byte, OpCode>();
		this.cbOpCodes = new HashMap<Byte, OpCode>();
		this.loadOpCodes();
		this.loadCbOpCodes();

		this.initLogging();

		this.running = true;
	}

	public void beginDispatch() {

		while (running) {

			logger.info("Instruction pointer: " + Integer.toHexString(pc));

			// Grab next instruction and increment instruction pointer
			byte opcode = mem.readByte(pc++);

			logger.fine("Opcode: 0x" + Integer.toHexString(opcode & 0xFF));

			// mask instruction pointer to 16 bits
			pc &= 65535;

			// Execute the instruction
			OpCode instruction;
			if ((opcode & 0xFF) == 0xCB) {
				logger.fine("CB prefixed opcode detected");
				opcode = mem.readByte((char) (pc));
				logger.fine("Opcode: 0x" + Integer.toHexString(opcode & 0xFF));
				instruction = cbOpCodes.get(opcode);
				pc++;
			} else {
				instruction = opCodes.get(opcode);
			}

			if (instruction != null) {
				logger.fine(instruction.getDisassembly());
				instruction.exec();

				// Increment clocks by the amount of time
				// that passed during the instruction
				clock_t.value += instruction.getM_time() / 4;
				clock_m.value += instruction.getM_time();
			} else {
				logger.severe("Unimplemented instruction: " + Integer.toHexString(opcode & 0xFF));
				System.exit(1);
			}

			System.out.println();
		}
	}

	/**
	 * Resets the CPU to its initial state
	 */
	public void initialize() {
		a = new Register((byte) 0x0);
		b = new Register((byte) 0x0);
		c = new Register((byte) 0x0);
		d = new Register((byte) 0x0);
		e = new Register((byte) 0x0);
		h = new Register((byte) 0x0);
		l = new Register((byte) 0x0);

		zeroFlag = false;
		operationFlag = false;
		halfCarryFlag = false;
		fullCarryFlag = false;

		pc = 0;
		sp = 0;

		t = new Register((byte) 0x0);
		m = new Register((byte) 0x0);

		clock_t = new Register((byte) 0x0);
		clock_m = new Register((byte) 0x0);
	}

	private void initLogging() {
		// disable default handler in root logger
		Logger globalLogger = Logger.getLogger("");
		Handler[] handlers = globalLogger.getHandlers();
		for (Handler handler : handlers) {
			globalLogger.removeHandler(handler);
		}

		logger.setLevel(Level.ALL);
		logger.addHandler(new CliHandler());
	}

	/**
	 * Writes a 16-bit value to two 8-bit registers as if they were a single
	 * unit
	 */
	private void writeDualRegister(Register r1, Register r2, char value) {
		byte[] bytes = Util.wordToBytes(value);
		r1.value = bytes[0];
		r2.value = bytes[1];
	}

	/**
	 * Reads a 16-bit value from two 8-bit registers as if they were a single
	 * unit
	 */
	private char readDualRegister(Register r1, Register r2) {
		return Util.bytesToWord(r1.value, r2.value);
	}

	/**
	 * Builds basic opcode table
	 */
	private void loadOpCodes() {
		opCodes.put((byte) 0x00, new OpCode("NOP", () -> nop(), (byte) 4));
		opCodes.put((byte) 0x31, new OpCode("LD SP, NN", () -> ldSpNn(), (byte) 12));
		opCodes.put((byte) 0xAF, new OpCode("XOR A", () -> xorA(), (byte) 4));
		opCodes.put((byte) 0x21, new OpCode("LD HL, NN", () -> ldHlNn(), (byte) 12));
		opCodes.put((byte) 0x32, new OpCode("LDD HL, A", () -> lddHlA(), (byte) 8));
		opCodes.put((byte) 0x20, new OpCode("JR NZ, N", () -> jrNzN(), (byte) 12, (byte) 8));
		opCodes.put((byte) 0xFB, new OpCode("EI", () -> eI(), (byte) 4));
		opCodes.put((byte) 0x0E, new OpCode("LDD C, N", () -> ldCn(), (byte) 8));
		opCodes.put((byte) 0x9F, new OpCode("SBC A, A", () -> sbcAa(), (byte) 8));
		opCodes.put((byte) 0x3E, new OpCode("LD A, N", () -> ldAn(), (byte) 8));
		opCodes.put((byte) 0xE2, new OpCode("LDH (C), A", () -> ldhCa(), (byte) 8));
		opCodes.put((byte) 0x0C, new OpCode("INC C", () -> incC(), (byte) 4));
		opCodes.put((byte) 0x77, new OpCode("LD (HL),A", () -> ldHlA(), (byte) 8));
	}

	/**
	 * Builds extended opcode table (CB prefixed opcodes)
	 */
	private void loadCbOpCodes() {
		cbOpCodes.put((byte) 0x7c, new OpCode("BIT 7 H", () -> bit7h(), (byte) 8));
		cbOpCodes.put((byte) 0x9F, new OpCode("RES 3 A", () -> res3a(), (byte) 8));
	}

	public Logger getLogger() {
		return logger;
	}

	/**
	 *
	 * From here down you'll find the definitions for each opcode listed in the
	 * tables above.
	 *
	 */

	// Copy A to address pointed to by HL
	private void ldHlA() {
		char address = readDualRegister(h, l);
		mem.writeByte(address, a.value);
		logger.finer("Wrote A (" + Integer.toHexString(a.value) + ") to " + Integer.toHexString(address));
	}

	// Increment C
	private void incC() {
		c.value++;
		if (c.value == 0) {
			zeroFlag = true;
		}

		operationFlag = false;

		/**
		 * 
		 * HALF CARRY FLAG NOT IMPLEMENTED
		 * 
		 * 
		 * 
		 */
		logger.finer("Incremented C");
	}

	// Save A at address pointed to by
	// 0xFF00 + C
	private void ldhCa() {
		char address = (char) (0xFF00 + c.value);
		mem.writeByte(address, a.value);
		logger.finer("Wrote A (" + Integer.toHexString(a.value) + ") to " + Integer.toHexString(address));
	}

	// Subtract A and carry flag from A
	private void sbcAa() {
		String hex = Integer.toHexString(a.value);
		logger.finer("Subtracted " + hex + " from " + hex);

		a.value -= a.value;
		if (a.value == 0) {
			zeroFlag = true;
		}
		operationFlag = true;

		/**
		 * 
		 * HALF CARRY FLAG NOT IMPLEMENTED
		 * 
		 * 
		 * 
		 */

		if (a.value < 0) {
			fullCarryFlag = true;
		}

	}

	// enable interrupts
	private void eI() {
		// this will be important later, for now it's
		// essentially a nop
		logger.finer("Interrupts enabled");
	}

	// no operation
	private void nop() {
		logger.finer("no op");
	}

	// test bit 7 of register H
	private void bit7h() {
		String bin = Integer.toBinaryString(h.value & 0xFF);
		if (bin.toCharArray()[7] == '0') {
			zeroFlag = true;
		} else {
			zeroFlag = false;
		}
		logger.finer("Testing bit 7 of " + bin + ": zeroFlag = " + zeroFlag);
	}

	// Loads a 16 bit immediate into SP
	private void ldSpNn() {
		sp = mem.readWord((char) (pc));
		logger.finer("Loaded value: " + Integer.toHexString(sp) + " into SP");

		pc += 2;
	}

	// XOR A against A
	private void xorA() {
		a.value ^= a.value;

		if (a.value == 0) {
			zeroFlag = true;
		}

		halfCarryFlag = false;
		fullCarryFlag = false;
		operationFlag = false;

		logger.finer("A = " + a);
	}

	// Loads a 16 bit immediate into HL
	private void ldHlNn() {
		char value = mem.readWord((char) pc);

		writeDualRegister(h, l, value);

		logger.finer("Loaded value:" + Integer.toHexString(value) + " into HL");

		pc += 2;
	}

	// Save A to address pointed to by HL, and decrement HL
	private void lddHlA() {

		char address = readDualRegister(h, l);
		mem.writeByte(address, a.value);

		logger.finer("Wrote A (" + Integer.toHexString(a.value) + ") to address in HL (" + Integer.toHexString(address)
				+ ")");

		address--;

		writeDualRegister(h, l, address);

		logger.finer("and decremented HL to " + Integer.toHexString(readDualRegister(h, l)));
	}

	// Relative jump by signed immediate (single byte) if last result was not
	// zero
	private void jrNzN() {
		if (!zeroFlag) {
			byte n = (byte) (mem.readByte(pc) & 0xFF);
			pc += n;
			logger.finer("Jmping by " + n);
		} else {
			logger.finer("Zero flag set, no jmp");
		}
	}

	// load 8-bit immediate into C
	private void ldCn() {
		c.value = mem.readByte(pc);
		logger.finer("Loaded " + Integer.toHexString(c.value & 0xFF) + " into C");
		pc++;
	}

	// load 8-bit immediate into A
	private void ldAn() {
		a.value = mem.readByte(pc);
		logger.finer("Loaded " + Integer.toHexString(a.value & 0xFF) + " into A");
		pc++;
	}

	// reset bit 3 of A
	private void res3a() {
		String bin = Integer.toBinaryString(a.value & 0xff);
		logger.finer("Resetting bit 3 of A");
		logger.finer("A before reset: " + bin);
		logger.finer("A after reset: ");
	}

}
