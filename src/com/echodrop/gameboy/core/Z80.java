/**
 * Z80.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.core;

import java.util.HashMap;
import java.util.logging.Logger;

import com.echodrop.gameboy.exceptions.InstructionNotImplementedException;

/**
 * Emulation core for Z80 microprocessor
 *
 */
public class Z80 {

	private TailspinGB system;
	private static final Logger logger = Logger.getLogger(Z80.class.getName());

	/* CPU registers */
	private Register a;
	private Register b;
	private Register c;
	private Register d;
	private Register e;
	private Register h;
	private Register l;

	/*
	 * Flag register, stored as boolean values for convenience
	 */
	private boolean zeroFlag;
	private boolean operationFlag;
	private boolean halfCarryFlag;
	private boolean fullCarryFlag;

	/**
	 * This flag is not present in the actual hardware, it's here for
	 * convenience. Set to true if a conditional instruction is not run, and the
	 * op's smaller time value should be added to the clock. Reset after each
	 * instruction.
	 */
	private boolean conditionalNotExecFlag;

	/* Special registers */
	private char pc; // program counter
	private char sp; // stack pointer

	/* Clocks */
	private Register clockT;
	private Register clockM;

	/* Memory Management Unit */
	private MMU mem;

	private boolean running;

	/* Opcode tables */
	private HashMap<Byte, OpCode> opCodes;
	private HashMap<Byte, OpCode> cbOpCodes;

	public Z80(TailspinGB system) {
		this.initialize();
		this.system = system;
		this.mem = system.getMem();
		this.opCodes = new HashMap<Byte, OpCode>();
		this.cbOpCodes = new HashMap<Byte, OpCode>();
		this.loadOpCodes();
		this.loadCbOpCodes();
		this.running = false;
	}

	/**
	 * Start emulation loop
	 */
	public void beginDispatch() {
		this.running = true;

		while (running) {
			step();
		}
	}

	/**
	 * Advances the emulation state by one instruction
	 */
	public void step() {
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

			if (isConditionalNotExecFlag()) {
				clockIncrement = instruction.getConditional_time();
			} else {
				clockIncrement = instruction.getMTime();
			}

			getClockT().setValue(getClockT().getValue() + clockIncrement / 4);
			getClockM().setValue(getClockM().getValue() + clockIncrement);

			system.getGpu().incrementModeClock((byte) (clockIncrement / 4));

		} else {
			logger.severe("Unimplemented instruction: " + Integer.toHexString(opcode & 0xFF));
			throw new InstructionNotImplementedException(opcode, (char) (pc - 1));
		}

		system.getGpu().clockStep();
		setConditionalNotExecFlag(false);
	}

	/**
	 * Resets the CPU to its initial state
	 */
	public void initialize() {
		setA(new Register((byte) 0x0));
		setB(new Register((byte) 0x0));
		setC(new Register((byte) 0x0));
		setD(new Register((byte) 0x0));
		setE(new Register((byte) 0x0));
		setH(new Register((byte) 0x0));
		setL(new Register((byte) 0x0));

		setZeroFlag(false);
		setOperationFlag(false);
		setHalfCarryFlag(false);
		setFullCarryFlag(false);

		pc = 0;
		sp = 0;

		setClockT(new Register((byte) 0x0));
		setClockM(new Register((byte) 0x0));
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
		r2.setValue(bytes[0]);
		r1.setValue(bytes[1]);
	}

	/**
	 * Reads a 16-bit value from two 8-bit registers as if they were a single
	 * unit
	 */
	private char readDualRegister(Register r1, Register r2) {
		return Util.bytesToWord(r2.getValue(), r1.getValue());
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
		opCodes.put((byte) 0x77, new OpCode("LD (HL), A", () -> ldHlA(), (byte) 8));
		opCodes.put((byte) 0xE0, new OpCode("LDH (n), A", () -> ldHnA(), (byte) 12));
		opCodes.put((byte) 0x11, new OpCode("LD DE, nn", () -> ldDeNn(), (byte) 12));
		opCodes.put((byte) 0x1A, new OpCode("LD A, (DE)", () -> ldAde(), (byte) 8));
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
		opCodes.put((byte) 0xF0, new OpCode("LDH A, (n)", () -> ldHaN(), (byte) 12));
		opCodes.put((byte) 0x1D, new OpCode("DEC E", () -> decE(), (byte) 4));
		opCodes.put((byte) 0x24, new OpCode("INC H", () -> incH(), (byte) 4));
		opCodes.put((byte) 0x73, new OpCode("LD (HL), E", () -> ldHlE(), (byte) 8));
		opCodes.put((byte) 0x90, new OpCode("SUB B", () -> subB(), (byte) 4));
		opCodes.put((byte) 0x15, new OpCode("DEC D", () -> decD(), (byte) 4));
		opCodes.put((byte) 0x16, new OpCode("LD D, n", () -> ldDn(), (byte) 8));
		opCodes.put((byte) 0x7C, new OpCode("LD A,H", () -> ldAh(), (byte) 4));
		opCodes.put((byte) 0xBE, new OpCode("CP (HL)", () -> cpHl(), (byte) 8));
		opCodes.put((byte) 0x7D, new OpCode("LD A, L", () -> ldAl(), (byte) 4));
		opCodes.put((byte) 0x78, new OpCode("LD A, B", () -> ldAb(), (byte) 4));
		opCodes.put((byte) 0x86, new OpCode("ADD A,(HL)", () -> addAhL(), (byte) 8));
	}

	/**
	 * Builds extended opcode table (CB prefixed opcodes)
	 */
	private void loadCbOpCodes() {
		cbOpCodes.put((byte) 0x7c, new OpCode("BIT 7 H", () -> bit7h(), (byte) 8));
		cbOpCodes.put((byte) 0x11, new OpCode("RL C", () -> rlC(), (byte) 8));
	}

	public Logger getLogger() {
		return logger;
	}

	public char getPc() {
		return this.pc;
	}

	public char getSp() {
		return this.sp;
	}

	public Register getA() {
		return a;
	}

	public Register getB() {
		return b;
	}

	public Register getC() {
		return c;
	}

	public Register getD() {
		return d;
	}

	public Register getE() {
		return e;
	}

	public Register getH() {
		return h;
	}

	public Register getL() {
		return l;
	}

	public boolean isZeroFlag() {
		return zeroFlag;
	}

	public boolean isOperationFlag() {
		return operationFlag;
	}

	public boolean isHalfCarryFlag() {
		return halfCarryFlag;
	}

	public boolean isFullCarryFlag() {
		return fullCarryFlag;
	}

	public boolean isConditionalNotExecFlag() {
		return conditionalNotExecFlag;
	}

	public Register getClockM() {
		return clockM;
	}

	private void setClockM(Register clockM) {
		this.clockM = clockM;
	}

	public Register getClockT() {
		return clockT;
	}

	private void setClockT(Register clockT) {
		this.clockT = clockT;
	}

	private void setConditionalNotExecFlag(boolean conditionalNotExecFlag) {
		this.conditionalNotExecFlag = conditionalNotExecFlag;
	}

	private void setFullCarryFlag(boolean fullCarryFlag) {
		this.fullCarryFlag = fullCarryFlag;
	}

	private void setHalfCarryFlag(boolean halfCarryFlag) {
		this.halfCarryFlag = halfCarryFlag;
	}

	private void setOperationFlag(boolean operationFlag) {
		this.operationFlag = operationFlag;
	}

	private void setZeroFlag(boolean zeroFlag) {
		this.zeroFlag = zeroFlag;
	}

	private void setL(Register l) {
		this.l = l;
	}

	private void setH(Register h) {
		this.h = h;
	}

	private void setE(Register e) {
		this.e = e;
	}

	private void setD(Register d) {
		this.d = d;
	}

	private void setC(Register c) {
		this.c = c;
	}

	private void setB(Register b) {
		this.b = b;
	}

	private void setA(Register a) {
		this.a = a;
	}

	/**
	 * From here down, you'll find the definitions for each opcode listed in the
	 * tables above.
	 */

	/**
	 * Add value pointed to by HL to Am,
	 */
	private void addAhL() {
		operationFlag = false;
		getA().setValue(getA().getValue() + mem.readByte(readDualRegister(h, l)));
		zeroFlag = (getA().getValue() == 0);

		/**
		 * XXX Full carry and half carry flags not implemented
		 */
	}

	/**
	 * Compare A to address pointed to by HL
	 */
	private void cpHl() {
		operationFlag = true;
		zeroFlag = (getA().getValue() == mem.readByte(readDualRegister(h, l)));

		/**
		 * XXX Full carry and half carry flags not implemented
		 */
	}

	/**
	 * Copy H into A
	 */
	private void ldAh() {
		getA().setValue(getH().getValue());
		logger.finer("Copied H (" + Integer.toHexString(getH().getValue() & 0xFF) + ") into A");
	}

	/**
	 * Copy B into A
	 */
	private void ldAb() {
		getA().setValue(getB().getValue());
		logger.finer("Copied B (" + Integer.toHexString(getH().getValue() & 0xFF) + ") into A");
	}

	/**
	 * Copy L into A
	 */
	private void ldAl() {
		getA().setValue(getL().getValue());
		logger.finer("Copied L (" + Integer.toHexString(getH().getValue() & 0xFF) + ") into A");
	}

	/**
	 * Load 8-bit immediate into D
	 */
	private void ldDn() {
		getD().setValue(mem.readByte(pc));
		logger.finer("Loaded " + Integer.toHexString(getD().getValue() & 0xFF) + " into D");
		pc++;
	}

	/**
	 * Subtract B from A
	 */
	private void subB() {

		getA().setValue(getA().getValue() - getB().getValue());
		setZeroFlag(getA().getValue() == 0);
		operationFlag = true;

		logger.finer("Subtracted B from A");

		/**
		 * XXX Full carry and half carry flags not implemented
		 */
	}

	/**
	 * Copy E to address pointed to by HL
	 */
	private void ldHlE() {
		char address = readDualRegister(getH(), getL());
		mem.writeByte(address, getE().getValue());
		logger.finer(
				"Wrote E (" + Integer.toHexString(getE().getValue() & 0xFF) + ") to " + Integer.toHexString(address));
	}

	/**
	 * Increment H
	 */
	private void incH() {
		getH().setValue(getH().getValue() + 1);
		setZeroFlag(getH().getValue() == 0);
		setOperationFlag(false);

		/**
		 * XXX Half carry flag not implemented
		 */
		logger.finer("Incremented H");
		logger.warning("INC H called, half carry flag not implemented");
	}

	/**
	 * Decrement E
	 */
	private void decE() {
		getE().setValue(getE().getValue() - 1);
		setZeroFlag(getE().getValue() == 0);
		setOperationFlag(true);

		/**
		 * XXX Half carry flag not implemented
		 */
		logger.warning("DEC E called, half carry flag not implemented");
	}

	/**
	 * Decrement D
	 */
	private void decD() {
		getD().setValue(getD().getValue() - 1);
		setZeroFlag(getD().getValue() == 0);
		setOperationFlag(true);

		/**
		 * XXX Half carry flag not implemented
		 */
		logger.warning("DEC D called, half carry flag not implemented");
	}

	/**
	 * Load A from address pointed to by 0xFF00 + 8-bit immediate
	 */
	private void ldHaN() {
		byte immediate = mem.readByte(pc);
		pc++;
		char address = (char) (0xFF00 + immediate);
		getA().setValue(mem.readByte(address));
		logger.finer("Loaded " + getA() + " into A from " + Integer.toHexString(address & 0xFFFF));
	}

	/**
	 * Load 8-bit immediate into E
	 */
	private void ldEn() {
		getE().setValue(mem.readByte(pc));
		logger.finer("Loaded " + Integer.toHexString(getE().getValue() & 0xFF) + " into E");
		pc++;
	}

	/**
	 * Increment B
	 */
	private void incB() {
		getB().setValue(getB().getValue() + 1);
		setZeroFlag(getB().getValue() == 0);

		setOperationFlag(false);

		/**
		 * XXX Half carry flag not implemented
		 */
		logger.finer("Incremented B");
		logger.warning("INC B called, half carry flag not implemented");
	}

	/**
	 * Copy A into H
	 */
	private void ldHa() {
		getH().setValue(getA().getValue());
		logger.finer("Copied A (" + Integer.toHexString(getA().getValue() & 0xFF) + ") into H");
	}

	/**
	 * Copy A into D
	 */
	private void ldDa() {
		getD().setValue(getA().getValue());
		logger.finer("Copied A (" + Integer.toHexString(getA().getValue() & 0xFF) + ") into D");
	}

	/**
	 * Relative jmp by signed immediate
	 */
	private void jrN() {
		byte immediate = mem.readByte(pc);
		pc++;
		pc += immediate;
		logger.fine("Jmping by " + immediate);
	}

	/**
	 * Load 8-bit immediate into L
	 */
	private void ldLn() {
		getL().setValue(mem.readByte(pc));
		logger.finer("Loaded " + Integer.toHexString(getL().getValue() & 0xFF) + " into L");
		pc++;
	}

	/**
	 * Relative jmp by signed immediate if last result was zero
	 */
	private void jrZn() {
		byte immediate = mem.readByte(pc);
		pc++;

		if (isZeroFlag()) {
			pc += immediate;
			logger.fine("Zero flag set, jmping by " + immediate);
		} else {
			// Use the smaller clock duration since the jmp was not executed
			setConditionalNotExecFlag(true);
			logger.fine("Zero flag not set, no jmp");
		}
	}

	/**
	 * Decrement A
	 */
	private void decA() {
		logger.finer("Decrementing A (" + Integer.toHexString(getA().getValue() & 0xFF) + ")");
		getA().setValue(getA().getValue() - 1);
		logger.finer("A = " + Integer.toHexString(getA().getValue() & 0xFF));

		setOperationFlag(true);
		setZeroFlag((getA().getValue() == 0));

		/**
		 * XXX Half carry flag not implemented
		 */
		logger.warning("DEC A called, half carry flag not implemented");
	}

	/**
	 * Decrement C
	 */
	private void decC() {
		logger.finer("Decrementing C (" + Integer.toHexString(getC().getValue() & 0xFF) + ")");
		getC().setValue(getC().getValue() - 1);
		logger.finer("C = " + Integer.toHexString(getC().getValue() & 0xFF));

		setOperationFlag(true);
		setZeroFlag((getC().getValue() == 0));

		/**
		 * XXX Half carry flag not implemented
		 */
		logger.warning("DEC C called, half carry flag not implemented");
	}

	/**
	 * Compare 8-bit immediate to A
	 */
	private void cpN() {
		byte immediate = mem.readByte(pc);
		pc++;
		setOperationFlag(true);

		if (getA().getValue() == immediate) {
			setZeroFlag(true);
		} else {
			setZeroFlag(false);
			if (getA().getValue() < immediate) {
				setFullCarryFlag(true);
			}
		}

		/**
		 * XXX Half carry flag not implemented
		 */
		logger.warning("CP n called, half carry flag not implemented");
	}

	/**
	 * save A at given address
	 */
	private void ldNnA() {
		char address = mem.readWord(pc);
		pc += 2;
		logger.finer("Loaded A (" + Integer.toHexString(getA().getValue() & 0xFF) + ") into address "
				+ Integer.toHexString(address & 0xFFFF));
	}

	/**
	 * Copy value of E into A
	 */
	private void ldAe() {
		getA().setValue(getE().getValue());
		logger.finer("Copied E (" + Integer.toHexString(getE().getValue() & 0xFF) + ") into A");
	}

	/**
	 * Increment DE
	 */
	private void incDe() {
		logger.finer("Incrementing DE (" + Integer.toHexString(readDualRegister(getD(), getE())) + ")");
		char de = readDualRegister(getD(), getE());
		writeDualRegister(getD(), getE(), (char) (de + 1));
		logger.finer("de = " + Integer.toHexString(readDualRegister(getD(), getE()) & 0xFFFF));
	}

	/**
	 * return
	 */
	private void ret() {
		char address = pop();
		logger.fine("RET called, returning to " + Integer.toHexString(address & 0xFFFF));
		pc = address;
	}

	/**
	 * increment HL
	 */
	private void incHl() {
		logger.finer("Incrementing HL (" + Integer.toHexString(readDualRegister(getH(), getL())) + ")");
		char hl = readDualRegister(getH(), getL());
		writeDualRegister(getH(), getL(), (char) (hl + 1));
		logger.finer("HL = " + Integer.toHexString(readDualRegister(getH(), getL()) & 0xFFFF));
	}

	/**
	 * Save A to address pointed to by HL, and increment HL
	 */
	private void ldiHlA() {
		char hl = readDualRegister(getH(), getL());

		mem.writeByte(hl, getA().getValue());
		logger.finer("Wrote A(" + Integer.toHexString(getA().getValue() & 0xFF) + ") to HL ("
				+ Integer.toHexString(hl & 0xFFFF) + ")");

		writeDualRegister(getH(), getL(), (char) (hl + 1));
		logger.finer("And incremented HL. HL = " + Integer.toHexString(readDualRegister(getH(), getL()) & 0xFFFF));
	}

	/**
	 * Decrement B
	 */
	private void decB() {
		getB().setValue(getB().getValue() - 1);
		setZeroFlag(getB().getValue() == 0);
		setOperationFlag(true);

		/**
		 * XXX Half carry flag not implemented
		 */
		logger.warning("DEC B called, half carry flag not implemented");
	}

	/**
	 * Pop 16-bit value from the stack and store it in BC
	 */
	private void popBc() {
		char address = pop();
		writeDualRegister(getB(), getC(), address);
		logger.finer("Popped value: " + Integer.toHexString(address & 0xFFFF) + " from stack and stored in BC");
	}

	/**
	 * rotate A left
	 */
	private void rlA() {
		logger.finer("Rotating A (" + Integer.toBinaryString(getA().getValue() & 0xFF) + ") left");
		getA().setValue(Util.leftRotate(getA().getValue()));
		logger.finer("A = " + Integer.toBinaryString(getA().getValue() & 0xFF));
		setZeroFlag(false);
		setOperationFlag(false);
		setHalfCarryFlag(false);

		/**
		 * XXX Full carry flag not implemented
		 */
		logger.warning("RL A called, full carry flag not implemented");
	}

	/**
	 * rotate C left
	 */
	private void rlC() {
		logger.finer("Rotating C (" + Integer.toBinaryString(getC().getValue() & 0xFF) + ") left");
		getC().setValue(Util.leftRotate(getC().getValue()));
		logger.finer("C = " + Integer.toBinaryString(getC().getValue() & 0xFF));
		setZeroFlag(getC().getValue() == 0);
		setOperationFlag(false);
		setHalfCarryFlag(false);

		/**
		 * XXX Full carry flag not implemented
		 */
		logger.warning("RL C called, full carry flag not implemented");
	}

	/**
	 * push BC onto stack
	 */
	private void pushBc() {
		char address = readDualRegister(getB(), getC());
		push(address);
		logger.finer("Pushed BC (" + Integer.toHexString(address) + ") onto stack");
	}

	/**
	 * Load 8-bit immediate into B
	 */
	private void ldBn() {
		getB().setValue(mem.readByte(pc));
		logger.finer("Loaded " + Integer.toHexString(getB().getValue() & 0xFF) + " into B");
		pc++;
	}

	/**
	 * Copy A to C
	 */
	private void ldCa() {
		getC().setValue(getA().getValue());
		logger.finer("Copied A (" + Integer.toHexString(getA().getValue() & 0xFF) + ") to C");
	}

	/**
	 * Call routine at nn
	 */
	private void callNn() {
		char address = mem.readWord(pc);
		push((char) (pc + 2));
		logger.fine("Pushed address " + Integer.toHexString((pc + 2) & 0xFFFF) + " to stack");
		pc = address;
		logger.fine("Calling subroutine at 0x" + Integer.toHexString(address & 0xFFFF));
	}

	/**
	 * Load A from address pointed to by DE
	 */
	private void ldAde() {
		char address = readDualRegister(getD(), getE());
		getA().setValue(mem.readByte(address));
		logger.finer("Loaded " + Integer.toHexString(getA().getValue() & 0xFF) + " into A from "
				+ "address pointed to by DE (" + Integer.toHexString(address) + ")");
	}

	/**
	 * Load 16-bit immediate into DE
	 */
	private void ldDeNn() {
		char immediate = mem.readWord((char) pc);
		pc += 2;
		writeDualRegister(getD(), getE(), immediate);
		logger.finer("Loaded " + Integer.toHexString(immediate) + " into DE");
	}

	/**
	 * Save A at address pointed to by 0xFF00 + 8-bit immediate
	 */
	private void ldHnA() {
		byte immediate = mem.readByte(pc);
		pc++;
		char address = (char) (0xFF00 + immediate);
		mem.writeByte(address, getA().getValue());
		logger.finer(
				"Wrote A (" + Integer.toHexString(getA().getValue() & 0xFF) + ") to " + Integer.toHexString(address));
	}

	/**
	 * Copy A to address pointed to by HL
	 */
	private void ldHlA() {
		char address = readDualRegister(getH(), getL());
		mem.writeByte(address, getA().getValue());
		logger.finer(
				"Wrote A (" + Integer.toHexString(getA().getValue() & 0xFF) + ") to " + Integer.toHexString(address));
	}

	/**
	 * Increment C
	 */
	private void incC() {
		getC().setValue(getC().getValue() + 1);
		setZeroFlag(getC().getValue() == 0);
		setOperationFlag(false);

		/**
		 * XXX Half carry flag not implemented
		 */
		logger.finer("Incremented C");
		logger.warning("INC C called, half carry flag not implemented");
	}

	/**
	 * Save A at address pointed to by 0xFF00 + C
	 */
	private void ldhCa() {
		char address = (char) (0xFF00 + getC().getValue());
		mem.writeByte(address, getA().getValue());
		logger.finer("Wrote A (" + Integer.toHexString(getA().getValue()) + ") to " + Integer.toHexString(address));
	}

	/**
	 * Subtract A and carry flag from A
	 */
	private void sbcAa() {
		String hex = Integer.toHexString(getA().getValue());
		logger.finer("Subtracted " + hex + " from " + hex);
		getA().setValue(getA().getValue() - getA().getValue());
		setZeroFlag(getA().getValue() == 0);
		setOperationFlag(true);
		if (getA().getValue() < 0) {
			setFullCarryFlag(true);
		}

		/**
		 * XXX Half carry flag not implemented
		 */
		logger.warning("SBC A, A called, half carry flag not implemented");
	}

	/**
	 * Enable interrupts
	 */
	private void eI() {
		/* this will be important later, for now it's essentially a nop */
		logger.finer("Enable interrupts");
		logger.warning("Interrupts not yet implemented");
	}

	/**
	 * No operation
	 */
	private void nop() {
		logger.finer("no op");
	}

	/**
	 * Test bit 7 of register H
	 */
	private void bit7h() {
		String bin = Integer.toBinaryString(getH().getValue() & 0xFF);
		if (bin.toCharArray()[7] == '0') {
			setZeroFlag(true);
		} else {
			setZeroFlag(false);
		}
		logger.finer("Testing bit 7 of " + bin + ": zeroFlag = " + isZeroFlag());
	}

	/**
	 * Loads a 16 bit immediate into SP
	 */
	private void ldSpNn() {
		sp = mem.readWord((char) (pc));
		logger.finer("Loaded value: " + Integer.toHexString(sp) + " into SP");
		pc += 2;
	}

	/**
	 * XOR A against A
	 */
	private void xorA() {
		getA().setValue(getA().getValue() ^ getA().getValue());
		setZeroFlag(getA().getValue() == 0);
		setHalfCarryFlag(false);
		setFullCarryFlag(false);
		setOperationFlag(false);
		logger.finer("A = " + getA());
	}

	/**
	 * Loads a 16 bit immediate into HL
	 */
	private void ldHlNn() {
		char value = mem.readWord((char) pc);
		writeDualRegister(getH(), getL(), value);
		logger.finer("Loaded value:" + Integer.toHexString(value) + " into HL");
		pc += 2;
	}

	/**
	 * Save A to address pointed to by HL, and decrement HL
	 */
	private void lddHlA() {
		char address = readDualRegister(getH(), getL());
		mem.writeByte(address, getA().getValue());
		logger.finer("Wrote A (" + Integer.toHexString(getA().getValue()) + ") to address in HL ("
				+ Integer.toHexString(address) + ")");
		address--;
		writeDualRegister(getH(), getL(), address);
		logger.finer("and decremented HL to " + Integer.toHexString(readDualRegister(getH(), getL())));
	}

	/**
	 * Relative jump by signed immediate (single byte) if last result was not
	 * zero
	 */
	private void jrNzN() {
		if (!isZeroFlag()) {
			byte n = (byte) (mem.readByte(pc) & 0xFF);

			/*
			 * we want to jump from the instruction location, not the location
			 * of n. thus, increment pc before jumping.
			 */
			pc++;

			// geronimo!
			pc += n;
			logger.finer("Jmping by " + n);
		} else {
			// Use the smaller clock duration since the jmp was not executed
			setConditionalNotExecFlag(true);

			// If theres no jump, we still want to skip the immediate
			pc++;
			logger.finer("Zero flag set, no jmp");
		}
	}

	/**
	 * load 8-bit immediate into C
	 */
	private void ldCn() {
		getC().setValue(mem.readByte(pc));
		logger.finer("Loaded " + Integer.toHexString(getC().getValue() & 0xFF) + " into C");
		pc++;
	}

	/**
	 * load 8-bit immediate into A
	 */
	private void ldAn() {
		getA().setValue(mem.readByte(pc));
		logger.finer("Loaded " + Integer.toHexString(getA().getValue() & 0xFF) + " into A");
		pc++;
	}

}
