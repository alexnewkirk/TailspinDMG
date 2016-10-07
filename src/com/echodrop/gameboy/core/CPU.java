/**
 * CPU.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.core;

import java.util.HashMap;
import java.util.logging.Logger;

import com.echodrop.gameboy.exceptions.InstructionNotImplementedException;
import com.echodrop.gameboy.util.NumberUtils;
import com.echodrop.gameboy.util.RegisterUtils;
import com.echodrop.gameboy.util.StringUtils;

/**
 * Emulation core for Sharp LR35902 microprocessor
 */
public class CPU {

	private TailspinGB system;
	private static final Logger logger = Logger.getLogger(CPU.class.getName());

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

	public CPU(TailspinGB system) {
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

		/* Grab next instruction and increment instruction pointer */
		byte opcode = mem.readByte(pc++);

		logger.fine("Opcode: 0x" + Integer.toHexString(opcode & 0xFF));

		/* mask instruction pointer to 16 bits */
		pc &= 65535;

		/* Execute the instruction */
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

			/*
			 * Increment clocks by the amount of time that passed during the
			 * instruction
			 */

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
		setA(new Register((byte) 0x0, "A"));
		setB(new Register((byte) 0x0, "B"));
		setC(new Register((byte) 0x0, "C"));
		setD(new Register((byte) 0x0, "D"));
		setE(new Register((byte) 0x0, "E"));
		setH(new Register((byte) 0x0, "H"));
		setL(new Register((byte) 0x0, "L"));

		setZeroFlag(false);
		setOperationFlag(false);
		setHalfCarryFlag(false);
		setFullCarryFlag(false);

		pc = 0;
		sp = 0;

		setClockT(new Register((byte) 0x0, "Clock T"));
		setClockM(new Register((byte) 0x0, "Clock M"));
	}

	public void initLogging() {
		logger.setParent(system.getLogger());
	}

	/**
	 * Writes a 16-bit value to two 8-bit registers as if they were a single
	 * unit
	 */
	private void writeDualRegister(Register r1, Register r2, char value) {
		byte[] bytes = NumberUtils.wordToBytes(value);
		r2.setValue(bytes[0]);
		r1.setValue(bytes[1]);
	}

	/**
	 * Reads a 16-bit value from two 8-bit registers as if they were a single
	 * unit
	 */
	private char readDualRegister(Register r1, Register r2) {
		return NumberUtils.bytesToWord(r2.getValue(), r1.getValue());
	}

	/**
	 * Pushes a memory address onto the stack
	 */
	private void push(char address) {
		byte[] b = NumberUtils.wordToBytes(address);
		byte b1 = b[0];
		byte b2 = b[1];
		sp--;
		mem.writeByte(sp, b2);
		sp--;
		mem.writeByte(sp, b1);
	}

	/**
	 * Pops a memory address off the stack
	 */
	private char pop() {
		byte b2 = mem.readByte(sp);
		sp++;
		byte b1 = mem.readByte(sp);
		sp++;
		return NumberUtils.bytesToWord(b2, b1);
	}

	/**
	 * Builds basic opcode table
	 */
	private void loadOpCodes() {
		opCodes.put((byte) 0x00, new OpCode("NOP", () -> nop(), (byte) 4));
		opCodes.put((byte) 0x17, new OpCode("RL A", () -> rl(getA()), (byte) 4));
		opCodes.put((byte) 0x2F, new OpCode("CPL", () -> complement(), (byte) 4));
		opCodes.put((byte) 0xFB, new OpCode("EI", () -> setInterruptsEnabled(true), (byte) 4));
		opCodes.put((byte) 0xF3, new OpCode("DI", () -> setInterruptsEnabled(false), (byte) 4));
		opCodes.put((byte) 0xBE, new OpCode("CP (HL)", () -> compareAddress(getH(), getL()), (byte) 8));
		opCodes.put((byte) 0xAF, new OpCode("XOR A", () -> xor(getA()), (byte) 4));
		opCodes.put((byte) 0xA9, new OpCode("XOR C", () -> xor(getC()), (byte) 4));
		opCodes.put((byte) 0xE6, new OpCode("AND n", () -> and(), (byte) 8));
		opCodes.put((byte) 0xA1, new OpCode("AND C", () -> and(getC()), (byte) 4));
		opCodes.put((byte) 0xA7, new OpCode("AND A", () -> and(getA()), (byte) 4));
		opCodes.put((byte) 0xB1, new OpCode("OR C", () -> or(getC()), (byte) 4));
		opCodes.put((byte) 0xB0, new OpCode("OR B", () -> or(getB()), (byte) 4));
		opCodes.put((byte) 0x86, new OpCode("ADD A,(HL)", () -> addAddress(getA(), getH(), getL()), (byte) 8));
		opCodes.put((byte) 0x87, new OpCode("ADD A,A", () -> add(getA()), (byte) 4));
		opCodes.put((byte) 0x19, new OpCode("ADD HL, DE", () -> add(getH(), getL(), getD(), getE()), (byte) 8));
		opCodes.put((byte) 0x90, new OpCode("SUB B", () -> subtract(getB()), (byte) 4));
		opCodes.put((byte) 0x31, new OpCode("LD SP, nn", () -> ldSpNn(), (byte) 12));
		opCodes.put((byte) 0x21, new OpCode("LD HL, nn", () -> loadSixteen(getH(), getL()), (byte) 12));
		opCodes.put((byte) 0x3E, new OpCode("LD A, n", () -> loadImmediate(getA()), (byte) 8));
		opCodes.put((byte) 0x7B, new OpCode("LD A, E", () -> load(getA(), getE()), (byte) 4));
		opCodes.put((byte) 0x5F, new OpCode("LD E, A", () -> load(getE(), getA()), (byte) 4));
		opCodes.put((byte) 0x0E, new OpCode("LD C, n", () -> loadImmediate(getC()), (byte) 8));
		opCodes.put((byte) 0x16, new OpCode("LD D, n", () -> loadImmediate(getD()), (byte) 8));
		opCodes.put((byte) 0x5E, new OpCode("LD E, (HL)", () -> loadFromAddress(getE(), getH(), getL()), (byte) 8));
		opCodes.put((byte) 0x7C, new OpCode("LD A,H", () -> load(getA(), getH()), (byte) 4));
		opCodes.put((byte) 0x11, new OpCode("LD DE, nn", () -> loadSixteen(getD(), getE()), (byte) 12));
		opCodes.put((byte) 0x01, new OpCode("LD BC, nn", () -> loadSixteen(getB(), getC()), (byte) 12));
		opCodes.put((byte) 0x1A, new OpCode("LD A, (DE)", () -> loadFromAddress(getA(), getD(), getE()), (byte) 8));
		opCodes.put((byte) 0x7E, new OpCode("LD A, (HL)", () -> loadFromAddress(getA(), getH(), getL()), (byte) 8));
		opCodes.put((byte) 0x56, new OpCode("LD D, (HL)", () -> loadFromAddress(getD(), getH(), getL()), (byte) 8));
		opCodes.put((byte) 0x77, new OpCode("LD (HL), A", () -> loadToAddress(getH(), getL(), getA()), (byte) 8));
		opCodes.put((byte) 0x12, new OpCode("LD (DE), A", () -> loadToAddress(getD(), getE(), getA()), (byte) 8));
		opCodes.put((byte) 0x36, new OpCode("LD (HL), n", () -> loadImmediateToAddress(getH(), getL()), (byte) 12));
		opCodes.put((byte) 0x32, new OpCode("LDD (HL), A", () -> loadDecrement(getH(), getL(), getA()), (byte) 8));
		opCodes.put((byte) 0x22, new OpCode("LDI (HL), A", () -> loadToAddressInc(getH(), getL(), getA()), (byte) 8));
		opCodes.put((byte) 0x4f, new OpCode("LD C, A", () -> load(getC(), getA()), (byte) 4));
		opCodes.put((byte) 0x06, new OpCode("LD B, n", () -> loadImmediate(getB()), (byte) 8));
		opCodes.put((byte) 0x67, new OpCode("LD H, A", () -> load(getH(), getA()), (byte) 4));
		opCodes.put((byte) 0x57, new OpCode("LD D, A", () -> load(getD(), getA()), (byte) 4));
		opCodes.put((byte) 0x1E, new OpCode("LD E, n", () -> loadImmediate(getE()), (byte) 8));
		opCodes.put((byte) 0x47, new OpCode("LD B, A", () -> load(getB(), getA()), (byte) 4));
		opCodes.put((byte) 0x7D, new OpCode("LD A, L", () -> load(getA(), getL()), (byte) 4));
		opCodes.put((byte) 0x78, new OpCode("LD A, B", () -> load(getA(), getB()), (byte) 4));
		opCodes.put((byte) 0xEA, new OpCode("LD nn A", () -> loadToImmediateAddress(getA()), (byte) 16));
		opCodes.put((byte) 0x79, new OpCode("LD A, C", () -> load(getA(), getC()), (byte) 4));
		opCodes.put((byte) 0x2e, new OpCode("LD L, n", () -> loadImmediate(getL()), (byte) 8));
		opCodes.put((byte) 0xE0, new OpCode("LDH (n), A", () -> loadToImmediateEightBitAddress(getA()), (byte) 12));
		opCodes.put((byte) 0xF0, new OpCode("LDH A, (n)", () -> loadFromEightImmediateAddress(getA()), (byte) 12));
		opCodes.put((byte) 0x2A,
				new OpCode("LD A, (HL+)", () -> loadIncrementFromAddress(getA(), getH(), getL()), (byte) 8));
		opCodes.put((byte) 0x73, new OpCode("LD (HL), E", () -> loadToAddress(getH(), getL(), getE()), (byte) 8));
		opCodes.put((byte) 0xFA, new OpCode("LD A, (a16)", () -> loadFromSixteenImmediateAddress(getA()), (byte)16));
		opCodes.put((byte) 0xE2, new OpCode("LDH (C), A", () -> loadToRegisterAddress(getC(), getA()), (byte) 8));
		opCodes.put((byte) 0x9F, new OpCode("SBC A, A", () -> subtractWithCarry(getA()), (byte) 8));
		opCodes.put((byte) 0x0C, new OpCode("INC C", () -> increment(getC()), (byte) 4));
		opCodes.put((byte) 0x1C, new OpCode("INC E", () -> increment(getE()), (byte) 4));
		opCodes.put((byte) 0x23, new OpCode("INC HL", () -> increment(getH(), getL()), (byte) 8));
		opCodes.put((byte) 0x3C, new OpCode("INC A", () -> increment(getA()), (byte) 4));
		opCodes.put((byte) 0x2C, new OpCode("INC L", () -> increment(getL()), (byte) 4));
		opCodes.put((byte) 0x14, new OpCode("INC D", () -> increment(getD()), (byte) 4));
		opCodes.put((byte) 0x04, new OpCode("INC B", () -> increment(getB()), (byte) 4));
		opCodes.put((byte) 0x13, new OpCode("INC DE", () -> increment(getD(), getE()), (byte) 8));
		opCodes.put((byte) 0x24, new OpCode("INC H", () -> increment(getH()), (byte) 4));
		opCodes.put((byte) 0x05, new OpCode("DEC B", () -> decrement(getB()), (byte) 4));
		opCodes.put((byte) 0x3D, new OpCode("DEC A", () -> decrement(getA()), (byte) 4));
		opCodes.put((byte) 0x1D, new OpCode("DEC E", () -> decrement(getE()), (byte) 4));
		opCodes.put((byte) 0x15, new OpCode("DEC D", () -> decrement(getD()), (byte) 4));
		opCodes.put((byte) 0x0D, new OpCode("DEC C", () -> decrement(getC()), (byte) 4));
		opCodes.put((byte) 0x0B, new OpCode("DEC BC", () -> decrement(getB(), getC()), (byte) 8));
		opCodes.put((byte) 0xc5, new OpCode("PUSH BC", () -> pushFrom(getB(), getC()), (byte) 16));
		opCodes.put((byte) 0xD5, new OpCode("PUSH DE", () -> pushFrom(getD(), getE()), (byte) 16));
		opCodes.put((byte) 0xE5, new OpCode("PUSH HL", () -> pushFrom(getH(), getL()), (byte) 16));
		//opCodes.put((byte) 0xF5, new OpCode("PUSH AF", () -> pushFrom(getA(), getF()), (byte) 16));
		opCodes.put((byte) 0xc1, new OpCode("POP BC", () -> popTo(getB(), getC()), (byte) 12));
		opCodes.put((byte) 0xD1, new OpCode("POP DE", () -> popTo(getD(), getE()), (byte) 12));
		opCodes.put((byte) 0xE1, new OpCode("POP HL", () -> popTo(getH(), getL()), (byte) 12));
		opCodes.put((byte) 0xCD, new OpCode("CALL nn", () -> callNn(), (byte) 24));
		opCodes.put((byte) 0xC9, new OpCode("RET", () -> ret(), (byte) 16));
		opCodes.put((byte) 0xC0, new OpCode("RET NZ", () -> retnz(), (byte) 20, (byte) 8));
		opCodes.put((byte) 0xFE, new OpCode("CP n", () -> compare(), (byte) 8));
		opCodes.put((byte) 0x28, new OpCode("JR Z, n", () -> jrZn(), (byte) 12, (byte) 8));
		opCodes.put((byte) 0x18, new OpCode("JR n", () -> jrN(), (byte) 12));
		opCodes.put((byte) 0xC3, new OpCode("JP nn", () -> jumpToImmediate(), (byte) 16));
		opCodes.put((byte) 0xE9, new OpCode("JP (HL)", () -> jumpToAddress(getH(), getL()), (byte) 4));
		opCodes.put((byte) 0xCA, new OpCode("JP Z a16", () -> jpzToSixteenImmediateAddress(), (byte) 16, (byte) 12));
		opCodes.put((byte) 0x20, new OpCode("JR NZ, n", () -> jrNzN(), (byte) 12, (byte) 8));
		opCodes.put((byte) 0xEF, new OpCode("RST 28H", () -> rst((byte) 0x28), (byte) 16));
	}

	/**
	 * Builds extended opcode table (CB prefixed opcodes)
	 */
	private void loadCbOpCodes() {
		cbOpCodes.put((byte) 0x7c, new OpCode("BIT 7 H", () -> bit7h(), (byte) 8));
		cbOpCodes.put((byte) 0x11, new OpCode("RL C", () -> rl(getC()), (byte) 8));
		cbOpCodes.put((byte) 0x87, new OpCode("RES 0, A", () -> res(0, getA()), (byte) 8));
		cbOpCodes.put((byte) 0x37, new OpCode("SWAP A", () -> swap(getA()), (byte) 8));
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

	/***************************************************************************
	 * From here down, you'll find the definitions for each opcode listed in the
	 * tables above.
	 * *************************************************************************/

	/**
	 * Jumps to address pointed to by 16-bit immediate
	 */
	private void jumpToImmediate() {
		byte b2 = mem.readByte(pc);
		pc++;
		byte b1 = mem.readByte(pc);
		char address = NumberUtils.bytesToWord(b2, b1);
		pc = address;
	}
	
	/**
	 * Jumps to address stored in 16 bit register
	 */
	private void jumpToAddress(Register r1, Register r2) {
		char address = readDualRegister(r1, r2);
		pc = address;
	}

	/**
	 * Adds value at address pointed to by s1s2 to destination.
	 */
	private void addAddress(Register destination, Register s1, Register s2) {
		operationFlag = false;
		byte memAtDual = mem.readByte(readDualRegister(s1, s2));
		setFullCarryFlag(NumberUtils.byteAdditionOverflow(destination.getValue(), memAtDual));
		setHalfCarryFlag(NumberUtils.byteAdditionNibbleOverflow(destination.getValue(), memAtDual));
		destination.setValue(destination.getValue() + memAtDual);
		setZeroFlag((destination.getValue() == 0));
	}

	/**
	 * Subtracts the value of a register from A
	 */
	private void subtract(Register r) {
		setHalfCarryFlag(NumberUtils.byteSubtractionNibbleBorrow(getA().getValue(), r.getValue()));
		setFullCarryFlag(NumberUtils.byteSubtractionBorrow(getA().getValue(), r.getValue()));
		getA().setValue(getA().getValue() - r.getValue());
		setZeroFlag(getA().getValue() == 0);
		operationFlag = true;
	}

	/**
	 * Subtracts the value of r + the carry flag from A
	 */
	private void subtractWithCarry(Register r) {
		byte toSub = (byte) (r.getValue() + (isFullCarryFlag() ? 1 : 0));
		setHalfCarryFlag(NumberUtils.byteSubtractionNibbleBorrow(getA().getValue(), toSub));
		getA().setValue(getA().getValue() - toSub);
		setZeroFlag(getA().getValue() == 0);
		setOperationFlag(true);
		if (getA().getValue() < 0) {
			setFullCarryFlag(true);
		}
	}

	/**
	 * Decrements a register
	 */
	private void decrement(Register r) {
		setHalfCarryFlag(NumberUtils.byteSubtractionNibbleBorrow(r.getValue(), (byte) 1));
		r.setValue(r.getValue() - 1);
		setZeroFlag(r.getValue() == 0);
		setOperationFlag(true);
	}

	/**
	 * Swaps high and low nibbles of a register
	 * 
	 * @param r register to swap
	 */
	private void swap(Register r) {
		String bin = StringUtils.zeroLeftPad(Integer.toBinaryString(r.getValue()), 8);
		String upper = bin.substring(0, 4);
		String lower = bin.substring(4);
		byte result = (byte) (Integer.parseInt(lower + upper, 2));
		r.setValue(result);
		setZeroFlag(r.getValue() == 0);
		setHalfCarryFlag(false);
		setFullCarryFlag(false);
		setOperationFlag(false);
	}

	/**
	 * Decrements a dual register
	 */
	private void decrement(Register r1, Register r2) {
		char value = readDualRegister(r1, r2);
		value--;
		writeDualRegister(r1, r2, value);
	}

	/**
	 * XOR value of r with A, result in A
	 */
	private void xor(Register r) {
		getA().setValue(getA().getValue() ^ r.getValue());
		setZeroFlag(getA().getValue() == 0);
		setHalfCarryFlag(false);
		setFullCarryFlag(false);
		setOperationFlag(false);
	}

	/**
	 * Bitwise OR A with r. Result in A.
	 */
	private void or(Register r) {
		getA().setValue(getA().getValue() | r.getValue());
		setZeroFlag(getA().getValue() == 0);
		setHalfCarryFlag(false);
		setFullCarryFlag(false);
		setOperationFlag(false);
	}

	/**
	 * Bitwise AND A with 8-bit immediate. Result in A.
	 */
	private void and() {
		byte val = mem.readByte(pc);
		pc++;
		getA().setValue(getA().getValue() & val);
		setZeroFlag(getA().getValue() == 0);
		setOperationFlag(false);
		setHalfCarryFlag(true);
		setFullCarryFlag(false);
	}

	/**
	 * Bitwise AND A with r. Result in A.
	 */
	private void and(Register r) {
		getA().setValue(getA().getValue() & r.getValue());
		setZeroFlag(getA().getValue() == 0);
		setOperationFlag(false);
		setHalfCarryFlag(true);
		setFullCarryFlag(false);
	}

	private void setFullCarryFlag() {
		setOperationFlag(false);
		setHalfCarryFlag(false);
		setFullCarryFlag(true);
	}

	/**
	 * Increments a register
	 */
	private void increment(Register r) {
		setHalfCarryFlag(NumberUtils.byteAdditionNibbleOverflow(r.getValue(), (byte) 1));
		r.setValue(r.getValue() + 1);
		setZeroFlag(r.getValue() == 0);
		setOperationFlag(false);
	}

	/**
	 * Increments a dual register
	 */
	private void increment(Register r1, Register r2) {
		char dual = readDualRegister(r1, r2);
		writeDualRegister(r1, r2, (char) (dual + 1));
	}

	/**
	 * Loads a value from one register into another
	 */
	private void load(Register destination, Register source) {
		destination.setValue(source.getValue());
	}

	/**
	 * Load 8-bit immediate into specified register
	 */
	private void loadImmediate(Register destination) {
		destination.setValue(mem.readByte(pc));
		pc++;
	}

	/**
	 * Loads a sixteen-bit immediate into a dual register
	 */
	private void loadSixteen(Register r1, Register r2) {
		char value = mem.readWord((char) pc);
		writeDualRegister(r1, r2, value);
		pc += 2;
	}

	/**
	 * Loads the value of src into the memory address pointed to by d1d2, then
	 * decrements d1d2.
	 */
	private void loadDecrement(Register d1, Register d2, Register src) {
		char address = readDualRegister(d1, d2);
		mem.writeByte(address, src.getValue());
		address--;
		writeDualRegister(d1, d2, address);
	}

	/**
	 * Loads the value of source into the address pointed to by 0xFF00 +
	 * destination
	 */
	private void loadToRegisterAddress(Register destination, Register source) {
		char address = (char) (0xFF00 + destination.getValue());
		mem.writeByte(address, source.getValue());
	}

	/**
	 * Loads the value of src into the memory address pointed to by d1d2
	 */
	private void loadToAddress(Register d1, Register d2, Register source) {
		char address = readDualRegister(d1, d2);
		mem.writeByte(address, source.getValue());
	}

	/**
	 * Loads 8-bit immediate into the address pointed to by d1d2
	 */
	private void loadImmediateToAddress(Register d1, Register d2) {
		char address = readDualRegister(d1, d2);
		byte value = mem.readByte(pc);
		pc++;
		mem.writeByte(address, value);
	}

	/**
	 * Write the value of source into the address pointed to by 0xFF00 + an
	 * 8-bit immediate
	 */
	private void loadToImmediateEightBitAddress(Register source) {
		byte immediate = mem.readByte(pc);
		pc++;
		char address = (char) (0xFF00 + immediate);
		mem.writeByte(address, source.getValue());
	}

	/**
	 * Loads the value pointed to by s1s2 into destination
	 */
	private void loadFromAddress(Register destination, Register s1, Register s2) {
		char address = readDualRegister(s1, s2);
		destination.setValue(mem.readByte(address));
	}

	/**
	 * Loads the value of source into the address pointed to by d1d2
	 */
	private void loadToAddressInc(Register d1, Register d2, Register source) {
		char dual = readDualRegister(d1, d2);
		mem.writeByte(dual, source.getValue());
		writeDualRegister(d1, d2, (char) (dual + 1));
	}

	/**
	 * Loads the value of source into the address pointed to by a 16-bit
	 * immediate.
	 */
	private void loadToImmediateAddress(Register source) {
		char address = mem.readWord(pc);
		mem.writeByte(address, source.getValue());
		pc += 2;
	}

	/**
	 * Loads value at address 0xFF00 + an 8-bit immediate into destination
	 */
	private void loadFromEightImmediateAddress(Register destination) {
		byte immediate = mem.readByte(pc);
		pc++;
		char address = (char) (0xFF00 + immediate);
		destination.setValue(mem.readByte(address));
	}
	
	/**
	 * Loads value from a 16-bit immediate address into destination
	 */
	private void loadFromSixteenImmediateAddress(Register destination) {
		char address = mem.readWord(pc);
		pc += 2;
		destination.setValue(mem.readByte(address));
	}
	
	/**
	 * Jumps to a sixteen-bit immediate address if the zero flag is not set
	 */
	private void jpzToSixteenImmediateAddress() {
		if(isZeroFlag()) {
			char address = mem.readWord(pc);
			pc = address;
		}
	}

	/**
	 * Loads the value at the address pointed to by s1s2 into destination.
	 * Increments s1s2
	 */
	private void loadIncrementFromAddress(Register destination, Register s1, Register s2) {
		char address = readDualRegister(s1, s2);
		destination.setValue(mem.readByte(address));
		address++;
		writeDualRegister(s1, s2, address);

	}

	/**
	 * Pushes the 16-bit value in r1r2 onto the stack
	 */
	private void pushFrom(Register r1, Register r2) {
		char stackValue = readDualRegister(r1, r2);
		push(stackValue);
	}

	/**
	 * Pops a 16-bit value off the stack and stores it in r1r2
	 */
	private void popTo(Register r1, Register r2) {
		char stackValue = pop();
		writeDualRegister(r1, r2, stackValue);
	}

	/**
	 * Flips every bit in register A
	 */
	private void complement() {
		String bin = StringUtils.zeroLeftPad(Integer.toBinaryString(getA().getValue()), 8);
		String res = "";
		for (int i = 0; i < 8; i++) {
			res += (bin.charAt(i) == '0' ? '1' : '0');
		}
		getA().setValue(Integer.parseInt(res, 2));
		setOperationFlag(true);
		setHalfCarryFlag(true);
	}

	/**
	 * Performs a left-rotate-through-carry on r
	 */
	private void rl(Register r) {
		setFullCarryFlag(RegisterUtils.leftRotateThroughCarry(r, isFullCarryFlag()));
		setZeroFlag(r.getValue() == 0);
		setOperationFlag(false);
		setHalfCarryFlag(false);
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
	 * Compare 8-bit immediate to A
	 */
	private void compare() {
		byte immediate = mem.readByte(pc);
		pc++;
		setOperationFlag(true);
		setHalfCarryFlag(NumberUtils.byteSubtractionNibbleBorrow(getA().getValue(), immediate));

		if (getA().getValue() == immediate) {
			setZeroFlag(true);
		} else {
			setZeroFlag(false);
			if (getA().getValue() < immediate) {
				setFullCarryFlag(true);
			}
		}
	}

	/**
	 * Compares the value pointed to by r1r2 to register A
	 */
	private void compareAddress(Register r1, Register r2) {
		operationFlag = true;
		byte memAtDual = mem.readByte(readDualRegister(r1, r2));
		setZeroFlag(getA().getValue() == memAtDual);
		setHalfCarryFlag(NumberUtils.byteSubtractionNibbleBorrow(getA().getValue(), memAtDual));
		setFullCarryFlag(NumberUtils.byteSubtractionBorrow(getA().getValue(), memAtDual));
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
	 * Return if Z flag is reset
	 */
	private void retnz() {
		if(!isZeroFlag()) {
			char address = pop();
			logger.fine("RET NZ called, returning to " + Integer.toHexString(address & 0xFFFF));
			pc = address;
		}
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
	 * Enable interrupts
	 */
	private void setInterruptsEnabled(boolean enabled) {
		/* this will be important later, but for now it's a nop */
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
	 * 
	 * //TODO: Generalize to bit(bitno, regiter)
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
	
	private void res(int bitNumber, Register r) {
		r.setValue(RegisterUtils.resetBit(bitNumber, r));
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

	private void rst(byte resetVector) {
		push(pc);
		pc = (char) resetVector;
	}

	private void add(Register r) {
		byte a = getA().getValue();
		boolean fullCarry = NumberUtils.byteAdditionOverflow(a, a);
		setFullCarryFlag(fullCarry);
		boolean halfCarry = NumberUtils.byteAdditionNibbleOverflow(a, a);
		setHalfCarryFlag(halfCarry);
		setOperationFlag(false);
		setZeroFlag(a + a == 0);
		getA().setValue(a + a);
	}

	private void add(Register a1, Register a2, Register b1, Register b2) {
		int a = readDualRegister(a1, a2);
		int b = readDualRegister(b1, b2);
		setOperationFlag(false);
		// TODO:Half/full carry flags
		writeDualRegister(a1, a2, (char) (a + b));
	}

}
