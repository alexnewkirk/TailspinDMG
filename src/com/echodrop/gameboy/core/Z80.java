package com.echodrop.gameboy.core;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Emulation core for Z80 microprocessor
 * 
 * @author echo_drop
 *
 */
public class Z80 {

	private GameBoy system;
	private static final Logger logger = Logger.getLogger(Z80.class.getName());

	// CPU registers
	private Register a;
	private Register b;
	private Register c;
	private Register d;
	private Register e;
	private Register h;
	private Register l;

	// Flag register, stored as boolean
	// values for convenience
	private boolean zeroFlag;
	private boolean operationFlag;
	private boolean halfCarryFlag;
	private boolean fullCarryFlag;

	// This flag is not present in the actual
	// hardware, it's here for convenience.
	// Set to true if a conditional instruction
	// is not run, and the op's smaller t value
	// should be added to the timer. Reset
	// after each instruction.
	private boolean conditionalNotExecFlag;

	// Special registers
	private char pc; // program counter
	private char sp; // stack pointer

	// Clocks
	private Register clock_t;
	private Register clock_m;

	// Memory Management Unit
	private MMU mem;

	private boolean running;

	// Opcode tables
	private HashMap<Byte, OpCode> opCodes;
	private HashMap<Byte, OpCode> cbOpCodes;

	public Z80(GameBoy system) {
		this.initialize();

		this.system = system;
		this.mem = system.getMem();

		this.opCodes = new HashMap<Byte, OpCode>();
		this.cbOpCodes = new HashMap<Byte, OpCode>();
		this.loadOpCodes();
		this.loadCbOpCodes();

		this.running = true;
	}

	public void beginDispatch() {

		while (running) {

			logger.info("Instruction pointer: 0x" + Integer.toHexString(pc));

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

				byte clockIncrement = 0;

				if (conditionalNotExecFlag) {
					clockIncrement = instruction.getConditional_time();
				} else {
					clockIncrement = instruction.getM_time();
				}

				clock_t.value += clockIncrement / 4;
				clock_m.value += clockIncrement;

				system.getGpu().incrementModeClock(clockIncrement);

			} else {
				logger.severe("Unimplemented instruction: " + Integer.toHexString(opcode & 0xFF));
				System.exit(1);
			}

			system.getGpu().clockStep();

			conditionalNotExecFlag = false;
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

		clock_t = new Register((byte) 0x0);
		clock_m = new Register((byte) 0x0);
	}

	public void initLogging() {
		logger.setParent(system.getLogger());
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
	 * Pushes a memory address onto the stack
	 */
	private void push(char address) {

		byte[] b = Util.wordToBytes(address);
		byte b1 = b[0];
		byte b2 = b[1];

		sp--;
		mem.writeByte(sp, b1);

		sp--;
		mem.writeByte(sp, b2);
	}

	/**
	 * Pops a memory address off the stack
	 */
	private char pop() {

		byte b2 = mem.readByte(sp);
		sp++;

		byte b1 = mem.readByte(sp);
		sp++;

		return Util.bytesToWord(b1, b2);

	}

	/**
	 * Builds basic opcode table
	 */
	private void loadOpCodes() {
		opCodes.put((byte) 0x00, new OpCode("NOP", () -> nop(), (byte) 4));
		opCodes.put((byte) 0x31, new OpCode("LD SP, nn", () -> ldSpNn(), (byte) 12));
		opCodes.put((byte) 0xAF, new OpCode("XOR A", () -> xorA(), (byte) 4));
		opCodes.put((byte) 0x21, new OpCode("LD HL, nn", () -> ldHlNn(), (byte) 12));
		opCodes.put((byte) 0x32, new OpCode("LDD HL, A", () -> lddHlA(), (byte) 8));
		opCodes.put((byte) 0x20, new OpCode("JR NZ, n", () -> jrNzN(), (byte) 12, (byte) 8));
		opCodes.put((byte) 0xFB, new OpCode("EI", () -> eI(), (byte) 4));
		opCodes.put((byte) 0x0E, new OpCode("LDD C, n", () -> ldCn(), (byte) 8));
		opCodes.put((byte) 0x9F, new OpCode("SBC A, A", () -> sbcAa(), (byte) 8));
		opCodes.put((byte) 0x3E, new OpCode("LD A, n", () -> ldAn(), (byte) 8));
		opCodes.put((byte) 0xE2, new OpCode("LDH (C), A", () -> ldhCa(), (byte) 8));
		opCodes.put((byte) 0x0C, new OpCode("INC C", () -> incC(), (byte) 4));
		opCodes.put((byte) 0x77, new OpCode("LD (HL),A", () -> ldHlA(), (byte) 8));
		opCodes.put((byte) 0xE0, new OpCode("LDH (n),A", () -> ldHnA(), (byte) 12));
		opCodes.put((byte) 0x11, new OpCode("LD DE, nn", () -> ldDeNn(), (byte) 12));
		opCodes.put((byte) 0x1A, new OpCode("LD A,(DE)", () -> ldAde(), (byte) 8));
		opCodes.put((byte) 0xCD, new OpCode("CALL nn", () -> callNn(), (byte) 24));
		opCodes.put((byte) 0x4f, new OpCode("LD C, A", () -> ldCa(), (byte) 4));
		opCodes.put((byte) 0x06, new OpCode("LD B, n", () -> ldBn(), (byte) 8));
		opCodes.put((byte) 0xc5, new OpCode("PUSH BC", () -> pushBc(), (byte) 16));
		opCodes.put((byte) 0x17, new OpCode("RL A", () -> rlA(), (byte) 4));
		opCodes.put((byte) 0xc1, new OpCode("POP BC", () -> popBc(), (byte) 12));
		opCodes.put((byte) 0x05, new OpCode("DEC B", () -> decB(), (byte) 4));
		opCodes.put((byte) 0x22, new OpCode("LDI (HL), A", () -> ldiHlA(), (byte) 8));
		opCodes.put((byte) 0x23, new OpCode("INC HL", () -> incHl(), (byte) 8));
		opCodes.put((byte) 0xC9, new OpCode("RET", () -> ret(), (byte) 16));
		opCodes.put((byte) 0x13, new OpCode("INC DE", () -> incDe(), (byte) 8));
		opCodes.put((byte) 0x7B, new OpCode("LD A, E", () -> ldAe(), (byte) 4));
		opCodes.put((byte) 0xFE, new OpCode("CP n", () -> cpN(), (byte) 8));
		opCodes.put((byte) 0xEA, new OpCode("LD nn A", () -> ldNnA(), (byte) 16));
		opCodes.put((byte) 0x3D, new OpCode("DEC A", () -> decA(), (byte) 4));
		opCodes.put((byte) 0x28, new OpCode("JR Z, n", () -> jrZn(), (byte) 12, (byte) 8));
		opCodes.put((byte) 0x0D, new OpCode("DEC C", () -> decC(), (byte) 4));
		opCodes.put((byte) 0x2e, new OpCode("LD L, n", () -> ldLn(), (byte) 8));
		opCodes.put((byte) 0x18, new OpCode("JR n", () -> jrN(), (byte) 12));
		opCodes.put((byte) 0x67, new OpCode("LD H, A", () -> ldHa(), (byte) 4));
		opCodes.put((byte) 0x57, new OpCode("LD D, A", () -> ldDa(), (byte) 4));
		opCodes.put((byte) 0x04, new OpCode("INC B", () -> incB(), (byte) 4));
		opCodes.put((byte) 0x1E, new OpCode("LD E, n", () -> ldEn(), (byte) 8));
		opCodes.put((byte) 0xF0, new OpCode("LDH A,(n)", () -> ldHaN(), (byte) 12));
	}

	/**
	 * Builds extended opcode table (CB prefixed opcodes)
	 */
	private void loadCbOpCodes() {
		cbOpCodes.put((byte) 0x7c, new OpCode("BIT 7 H", () -> bit7h(), (byte) 8));
		cbOpCodes.put((byte) 0x9F, new OpCode("RES 3 A", () -> res3a(), (byte) 8));
		cbOpCodes.put((byte) 0x11, new OpCode("RL C", () -> rlC(), (byte) 8));
	}

	public Logger getLogger() {
		return logger;
	}

	/**
	 *
	 * From here down, you'll find the definitions for each opcode listed in the
	 * tables above.
	 *
	 */

	// Load A from address pointed to by 0xFF00 + 8-bit immediate
	private void ldHaN() {
		byte immediate = mem.readByte(pc);
		pc++;

		char address = (char) (0xFF00 + immediate);
		a.value = mem.readByte(address);

		logger.finer("Loaded " + a + " into A from " + Integer.toHexString(address & 0xFFFF));
	}

	// Load 8-bit immediate into E
	private void ldEn() {
		e.value = mem.readByte(pc);
		logger.finer("Loaded " + Integer.toHexString(e.value & 0xFF) + " into E");
		pc++;
	}

	// Increment B
	private void incB() {
		b.value++;
		if (b.value == 0) {
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
		logger.finer("Incremented B");
		logger.warning("INC B called, half carry flag not implemented");
	}

	// Copy A into H
	private void ldHa() {
		h.value = a.value;
		logger.finer("Copied A (" + Integer.toHexString(a.value & 0xFF) + ") into H");
	}

	// Copy A into D
	private void ldDa() {
		d.value = a.value;
		logger.finer("Copied A (" + Integer.toHexString(a.value & 0xFF) + ") into D");
	}

	// Relative jmp by signed immediate
	private void jrN() {
		byte immediate = mem.readByte(pc);
		pc++;

		pc += immediate;

		logger.fine("Jmping by " + immediate);
	}

	// Load 8-bit immediate into L
	private void ldLn() {
		l.value = mem.readByte(pc);
		logger.finer("Loaded " + Integer.toHexString(l.value & 0xFF) + " into L");
		pc++;
	}

	// Relative jmp by signed immediate if last result was zero
	private void jrZn() {
		byte immediate = mem.readByte(pc);
		pc++;

		if (zeroFlag) {
			pc += immediate;
			logger.fine("Zero flag set, jmping by " + immediate);
		} else {
			//Use the smaller clock duration since the jmp was not executed
			conditionalNotExecFlag = true;
			logger.fine("Zero flag not set, no jmp");
		}
	}

	// Decrement A
	private void decA() {
		logger.finer("Decrementing A (" + Integer.toHexString(a.value & 0xFF) + ")");
		a.value--;
		logger.finer("A = " + Integer.toHexString(a.value & 0xFF));

		operationFlag = true;
		zeroFlag = (a.value == 0);

		/**
		 * 
		 * 
		 * Half carry flag not implemented
		 * 
		 * 
		 */
		logger.warning("DEC A called, half carry flag not implemented");
	}

	// Decrement C
	private void decC() {
		logger.finer("Decrementing C (" + Integer.toHexString(c.value & 0xFF) + ")");
		c.value--;
		logger.finer("C = " + Integer.toHexString(c.value & 0xFF));

		operationFlag = true;
		zeroFlag = (c.value == 0);

		/**
		 * 
		 * 
		 * Half carry flag not implemented
		 * 
		 * 
		 */
		logger.warning("DEC C called, half carry flag not implemented");
	}

	// Compare 8-bit immediate to A
	private void cpN() {
		byte immediate = mem.readByte(pc);
		pc++;

		if (a.value == immediate) {
			zeroFlag = true;
		} else if (a.value < immediate) {
			fullCarryFlag = true;
		}

		operationFlag = true;

		/**
		 * 
		 * 
		 * Half carry flag not implemented
		 * 
		 * 
		 */
		logger.warning("CP n called, half carry flag not implemented");
	}

	// save A at given address
	private void ldNnA() {

		char address = mem.readWord(pc);
		pc += 2;

		logger.finer("Loaded A (" + Integer.toHexString(a.value & 0xFF) + ") into address "
				+ Integer.toHexString(address & 0xFFFF));
	}

	// Copy value of E into A
	private void ldAe() {
		a.value = e.value;
		logger.finer("Copied E (" + Integer.toHexString(e.value & 0xFF) + ") into A");
	}

	// Increment DE
	private void incDe() {
		logger.finer("Incrementing DE (" + Integer.toHexString(readDualRegister(d, e)) + ")");
		char de = readDualRegister(d, e);
		writeDualRegister(d, e, (char) (de + 1));
		logger.finer("de = " + Integer.toHexString(readDualRegister(d, e) & 0xFFFF));
	}

	// return
	private void ret() {
		char address = pop();
		logger.fine("RET called, returning to " + Integer.toHexString(address & 0xFFFF));
		pc = address;
	}

	// increment HL
	private void incHl() {
		logger.finer("Incrementing HL (" + Integer.toHexString(readDualRegister(h, l)) + ")");
		char hl = readDualRegister(h, l);
		writeDualRegister(h, l, (char) (hl + 1));
		logger.finer("HL = " + Integer.toHexString(readDualRegister(h, l) & 0xFFFF));
	}

	// Save A to address pointed to by HL, and increment HL
	private void ldiHlA() {
		char hl = readDualRegister(h, l);

		mem.writeByte(hl, a.value);
		logger.finer("Wrote A(" + Integer.toHexString(a.value & 0xFF) + ") to HL (" + Integer.toHexString(hl & 0xFFFF)
				+ ")");

		writeDualRegister(h, l, (char) (hl + 1));
		logger.finer("And incremented HL. HL = " + Integer.toHexString(readDualRegister(h, l) & 0xFFFF));
	}

	// Decrement B
	private void decB() {
		b.value--;
		if (b.value == 0) {
			zeroFlag = true;
		}
		operationFlag = true;

		/**
		 * 
		 * 
		 * Half carry flag not implemented
		 * 
		 * 
		 */
		logger.warning("DEC B called, half carry flag not implemented");
	}

	// Pop 16-bit value from the stack and store it in BC
	private void popBc() {
		char address = pop();

		writeDualRegister(b, c, address);

		logger.finer("Popped value: " + Integer.toHexString(address & 0xFFFF) + " from stack and stored in BC");
	}

	// rotate A left
	private void rlA() {
		logger.finer("Rotating A (" + Integer.toBinaryString(a.value & 0xFF) + ") left");
		a.value = Util.leftRotate(a.value);
		logger.finer("A = " + Integer.toBinaryString(a.value & 0xFF));

		zeroFlag = false;
		operationFlag = false;
		halfCarryFlag = false;

		/**
		 * 
		 * 
		 * Full carry flag not implemented
		 * 
		 * 
		 */
		logger.warning("RL A called, full carry flag not implemented");
	}

	// rotate C left
	private void rlC() {
		logger.finer("Rotating C (" + Integer.toBinaryString(c.value & 0xFF) + ") left");
		c.value = Util.leftRotate(c.value);
		logger.finer("C = " + Integer.toBinaryString(c.value & 0xFF));

		if (c.value == 0) {
			zeroFlag = true;
		}

		operationFlag = false;
		halfCarryFlag = false;

		/**
		 * 
		 * 
		 * Full carry flag not implemented
		 * 
		 * 
		 */
		logger.warning("RL C called, full carry flag not implemented");
	}

	// push BC onto stack
	private void pushBc() {
		char address = readDualRegister(b, c);

		push(address);

		logger.finer("Pushed BC (" + Integer.toHexString(address) + ") onto stack");
	}

	// Load 8-bit immediate into B
	private void ldBn() {
		b.value = mem.readByte(pc);
		logger.finer("Loaded " + Integer.toHexString(b.value & 0xFF) + " into B");
		pc++;
	}

	// Copy A to C
	private void ldCa() {
		c.value = a.value;
		logger.finer("Copied A (" + Integer.toHexString(a.value & 0xFF) + ") to C");
	}

	// Call routine at nn
	private void callNn() {

		char address = mem.readWord(pc);

		push((char) (pc + 2));
		logger.fine("Pushed address " + Integer.toHexString((pc + 2) & 0xFFFF) + " to stack");

		pc = address;
		logger.fine("Calling subroutine at 0x" + Integer.toHexString(address & 0xFFFF));
	}

	// Load A from address pointed to by DE
	private void ldAde() {
		char address = readDualRegister(d, e);
		a.value = mem.readByte(address);
		logger.finer("Loaded " + Integer.toHexString(a.value & 0xFF) + " into A from " + "address pointed to by DE ("
				+ Integer.toHexString(address) + ")");
	}

	// Load 16-bit immediate into DE
	private void ldDeNn() {
		char immediate = mem.readWord((char) pc);
		pc += 2;

		writeDualRegister(d, e, immediate);

		logger.finer("Loaded " + Integer.toHexString(immediate) + " into DE");
	}

	// Save A at address pointed to by 0xFF00 + 8-bit immediate
	private void ldHnA() {
		byte immediate = mem.readByte(pc);
		pc++;

		char address = (char) (0xFF00 + immediate);
		mem.writeByte(address, a.value);
		logger.finer("Wrote A (" + Integer.toHexString(a.value & 0xFF) + ") to " + Integer.toHexString(address));
		;
	}

	// Copy A to address pointed to by HL
	private void ldHlA() {
		char address = readDualRegister(h, l);
		mem.writeByte(address, a.value);
		logger.finer("Wrote A (" + Integer.toHexString(a.value & 0xFF) + ") to " + Integer.toHexString(address));
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
		logger.warning("INC C called, half carry flag not implemented");
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
		logger.warning("SBC A, A called, half carry flag not implemented");

		if (a.value < 0) {
			fullCarryFlag = true;
		}

	}

	// enable interrupts
	private void eI() {
		// this will be important later, for now it's
		// essentially a nop
		logger.finer("Enable interrupts");
		logger.warning("Interrupts not yet implemented");
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

			// we want to jump from the instruction location, not the
			// location of n. thus, increment pc before jumping.
			pc++;

			// ...and jump! geronimo!
			pc += n;

			logger.finer("Jmping by " + n);
		} else {
			// Use the smaller clock duration since the jmp was not executed
			conditionalNotExecFlag = true;

			// If theres no jump, we still want to skip the immediate
			pc++;
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
		/***
		 * 
		 * NOT IMPLEMENTED
		 * 
		 * 
		 */
		logger.warning("RES 3 A called, not implemented");
	}

}
