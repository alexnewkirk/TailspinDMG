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
	private Register f;

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

	/* Opcode tables */
	private HashMap<Byte, Opcode> opCodes;
	private HashMap<Byte, Opcode> cbOpCodes;

	private boolean running;

	public CPU(TailspinGB system) {
		this.initialize();
		this.system = system;
		this.mem = system.getMem();
		this.opCodes = new HashMap<Byte, Opcode>();
		this.cbOpCodes = new HashMap<Byte, Opcode>();
		this.loadOpCodes();
		this.loadCbOpCodes();
		this.running = false;
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
		setF(new Register((byte) 0x0, "F"));

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
		Opcode instruction;
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
			logger.fine(instruction.getMnemonic());
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
		opCodes.put((byte) 0x00, new Opcode("NOP", () -> nop(), (byte) 4));
		opCodes.put((byte) 0x17, new Opcode("RLA", () -> rl(getA()), (byte) 4));
		opCodes.put((byte) 0x1F, new Opcode("RRA", () -> rr(getA()), (byte) 4));
		opCodes.put((byte) 0x2F, new Opcode("CPL", () -> complement(), (byte) 4));
		opCodes.put((byte) 0xFB, new Opcode("EI", () -> setInterruptsEnabled(true), (byte) 4));
		opCodes.put((byte) 0xF3, new Opcode("DI", () -> setInterruptsEnabled(false), (byte) 4));
		opCodes.put((byte) 0xBE, new Opcode("CP (HL)", () -> compareAddress(getH(), getL()), (byte) 8));
		opCodes.put((byte) 0xAF, new Opcode("XOR A", () -> xor(getA()), (byte) 4));
		opCodes.put((byte) 0xA9, new Opcode("XOR C", () -> xor(getC()), (byte) 4));
		opCodes.put((byte) 0xE6, new Opcode("AND n", () -> and(), (byte) 8));
		opCodes.put((byte) 0xA1, new Opcode("AND C", () -> and(getC()), (byte) 4));
		opCodes.put((byte) 0xA7, new Opcode("AND A", () -> and(getA()), (byte) 4));
		opCodes.put((byte) 0xB1, new Opcode("OR C", () -> or(getC()), (byte) 4));
		opCodes.put((byte) 0xB0, new Opcode("OR B", () -> or(getB()), (byte) 4));
		opCodes.put((byte) 0xB3, new Opcode("OR E", () -> or(getE()), (byte) 4));
		opCodes.put((byte) 0x86, new Opcode("ADD A,(HL)", () -> addAddress(getA(), getH(), getL()), (byte) 8));
		opCodes.put((byte) 0x87, new Opcode("ADD A,A", () -> add(getA()), (byte) 4));
		opCodes.put((byte) 0x19, new Opcode("ADD HL, DE", () -> add(getH(), getL(), getD(), getE()), (byte) 8));
		opCodes.put((byte) 0x90, new Opcode("SUB B", () -> subtract(getB()), (byte) 4));
		opCodes.put((byte) 0x7B, new Opcode("LD A, E", () -> load(getA(), getE()), (byte) 4));
		opCodes.put((byte) 0x7A, new Opcode("LD A, D", () -> load(getA(), getD()), (byte) 4));
		opCodes.put((byte) 0x7F, new Opcode("LD A, A", () -> load(getA(), getA()), (byte) 4));
		opCodes.put((byte) 0x5F, new Opcode("LD E, A", () -> load(getE(), getA()), (byte) 4));
		opCodes.put((byte) 0x4f, new Opcode("LD C, A", () -> load(getC(), getA()), (byte) 4));
		opCodes.put((byte) 0x67, new Opcode("LD H, A", () -> load(getH(), getA()), (byte) 4));
		opCodes.put((byte) 0x79, new Opcode("LD A, C", () -> load(getA(), getC()), (byte) 4));
		opCodes.put((byte) 0x57, new Opcode("LD D, A", () -> load(getD(), getA()), (byte) 4));
		opCodes.put((byte) 0x47, new Opcode("LD B, A", () -> load(getB(), getA()), (byte) 4));
		opCodes.put((byte) 0x7C, new Opcode("LD A, H", () -> load(getA(), getH()), (byte) 4));
		opCodes.put((byte) 0x7D, new Opcode("LD A, L", () -> load(getA(), getL()), (byte) 4));
		opCodes.put((byte) 0x6F, new Opcode("LD L, A", () -> load(getL(), getA()), (byte) 4));
		opCodes.put((byte) 0x78, new Opcode("LD A, B", () -> load(getA(), getB()), (byte) 4));
		opCodes.put((byte) 0x0E, new Opcode("LD C, n", () -> load(getC(), read8Immediate()), (byte) 8));
		opCodes.put((byte) 0x16, new Opcode("LD D, n", () -> load(getD(), read8Immediate()), (byte) 8));
		opCodes.put((byte) 0x26, new Opcode("LD H, n", () -> load(getH(), read8Immediate()), (byte) 8));
		opCodes.put((byte) 0x3E, new Opcode("LD A, n", () -> load(getA(), read8Immediate()), (byte) 8));
		opCodes.put((byte) 0x06, new Opcode("LD B, n", () -> load(getB(), read8Immediate()), (byte) 8));
		opCodes.put((byte) 0x1E, new Opcode("LD E, n", () -> load(getE(), read8Immediate()), (byte) 8));
		opCodes.put((byte) 0x2e, new Opcode("LD L, n", () -> load(getL(), read8Immediate()), (byte) 8));
		opCodes.put((byte) 0x31, new Opcode("LD SP, nn", () -> sp = read16Immediate(), (byte) 12));
		opCodes.put((byte) 0x21, new Opcode("LD HL, nn", () -> load(getH(), getL(), read16Immediate()), (byte) 12));
		opCodes.put((byte) 0x11, new Opcode("LD DE, nn", () -> load(getD(), getE(), read16Immediate()), (byte) 12));
		opCodes.put((byte) 0x01, new Opcode("LD BC, nn", () -> load(getB(), getC(), read16Immediate()), (byte) 12));
		opCodes.put((byte) 0x5E, new Opcode("LD E, (HL)", () -> load(getE(), getH(), getL(), false), (byte) 8));
		opCodes.put((byte) 0x6E, new Opcode("LD L, (HL)", () -> load(getL(), getH(), getL(), false), (byte) 8));
		opCodes.put((byte) 0x1A, new Opcode("LD A, (DE)", () -> load(getA(), getD(), getE(), false), (byte) 8));
		opCodes.put((byte) 0x7E, new Opcode("LD A, (HL)", () -> load(getA(), getH(), getL(), false), (byte) 8));
		opCodes.put((byte) 0x4E, new Opcode("LD C, (HL)", () -> load(getC(), getH(), getL(), false), (byte) 8));
		opCodes.put((byte) 0x46, new Opcode("LD B, (HL)", () -> load(getB(), getH(), getL(), false), (byte) 8));
		opCodes.put((byte) 0x56, new Opcode("LD D, (HL)", () -> load(getD(), getH(), getL(), false), (byte) 8));
		opCodes.put((byte) 0x77, new Opcode("LD (HL), A", () -> load(getH(), getL(), getA(), true), (byte) 8));
		opCodes.put((byte) 0x73, new Opcode("LD (HL), E", () -> load(getH(), getL(), getE(), true), (byte) 8));
		opCodes.put((byte) 0x70, new Opcode("LD (HL), B", () -> load(getH(), getL(), getB(), true), (byte) 8));
		opCodes.put((byte) 0x71, new Opcode("LD (HL), C", () -> load(getH(), getL(), getC(), true), (byte) 8));
		opCodes.put((byte) 0x12, new Opcode("LD (DE), A", () -> load(getD(), getE(), getA(), true), (byte) 8));
		opCodes.put((byte) 0x36, new Opcode("LD (HL), n", () -> load(getH(), getL(), read8Immediate()), (byte) 12));
		opCodes.put((byte) 0x32, new Opcode("LDD (HL), A", () -> loadDecrement(getH(), getL(), getA()), (byte) 8));
		opCodes.put((byte) 0x22,
				new Opcode("LDI (HL), A", () -> loadIncrement(getH(), getL(), getA(), true), (byte) 8));
		opCodes.put((byte) 0xEA, new Opcode("LD nn A", () -> load(read16Immediate(), getA()), (byte) 16));
		opCodes.put((byte) 0xE0,
				new Opcode("LDH (n), A", () -> load((char) (0xFF00 + read8Immediate()), getA()), (byte) 12));
		opCodes.put((byte) 0xF0,
				new Opcode("LDH A, (n)", () -> load(getA(), (char) (0xFF00 + read8Immediate())), (byte) 12));
		opCodes.put((byte) 0x2A,
				new Opcode("LD A, (HL+)", () -> loadIncrement(getA(), getH(), getL(), false), (byte) 8));
		opCodes.put((byte) 0xFA, new Opcode("LD A, (a16)", () -> load(getA(), read16Immediate()), (byte) 16));
		opCodes.put((byte) 0xE2, new Opcode("LDH (C), A", () -> ldh(getC(), getA()), (byte) 8));
		opCodes.put((byte) 0x9F, new Opcode("SBC A, A", () -> subtractWithCarry(getA()), (byte) 8));
		opCodes.put((byte) 0x0C, new Opcode("INC C", () -> increment(getC()), (byte) 4));
		opCodes.put((byte) 0x1C, new Opcode("INC E", () -> increment(getE()), (byte) 4));
		opCodes.put((byte) 0x3C, new Opcode("INC A", () -> increment(getA()), (byte) 4));
		opCodes.put((byte) 0x2C, new Opcode("INC L", () -> increment(getL()), (byte) 4));
		opCodes.put((byte) 0x14, new Opcode("INC D", () -> increment(getD()), (byte) 4));
		opCodes.put((byte) 0x04, new Opcode("INC B", () -> increment(getB()), (byte) 4));
		opCodes.put((byte) 0x24, new Opcode("INC H", () -> increment(getH()), (byte) 4));
		opCodes.put((byte) 0x23, new Opcode("INC HL", () -> increment(getH(), getL()), (byte) 8));
		opCodes.put((byte) 0x13, new Opcode("INC DE", () -> increment(getD(), getE()), (byte) 8));
		opCodes.put((byte) 0x05, new Opcode("DEC B", () -> decrement(getB()), (byte) 4));
		opCodes.put((byte) 0x3D, new Opcode("DEC A", () -> decrement(getA()), (byte) 4));
		opCodes.put((byte) 0x1D, new Opcode("DEC E", () -> decrement(getE()), (byte) 4));
		opCodes.put((byte) 0x15, new Opcode("DEC D", () -> decrement(getD()), (byte) 4));
		opCodes.put((byte) 0x0D, new Opcode("DEC C", () -> decrement(getC()), (byte) 4));
		opCodes.put((byte) 0x0B, new Opcode("DEC BC", () -> decrement(getB(), getC()), (byte) 8));
		opCodes.put((byte) 0x1B, new Opcode("DEC DE", () -> decrement(getD(), getE()), (byte) 8));
		opCodes.put((byte) 0xc5, new Opcode("PUSH BC", () -> pushFrom(getB(), getC()), (byte) 16));
		opCodes.put((byte) 0xD5, new Opcode("PUSH DE", () -> pushFrom(getD(), getE()), (byte) 16));
		opCodes.put((byte) 0xE5, new Opcode("PUSH HL", () -> pushFrom(getH(), getL()), (byte) 16));
		opCodes.put((byte) 0xF5, new Opcode("PUSH AF", () -> pushFrom(getA(), getF()), (byte) 16));
		opCodes.put((byte) 0xC1, new Opcode("POP BC", () -> popTo(getB(), getC()), (byte) 12));
		opCodes.put((byte) 0xD1, new Opcode("POP DE", () -> popTo(getD(), getE()), (byte) 12));
		opCodes.put((byte) 0xE1, new Opcode("POP HL", () -> popTo(getH(), getL()), (byte) 12));
		opCodes.put((byte) 0xF1, new Opcode("POP AF", () -> popTo(getA(), getF()), (byte) 12));
		opCodes.put((byte) 0xCD, new Opcode("CALL nn", () -> call(), (byte) 24));
		opCodes.put((byte) 0xC9, new Opcode("RET", () -> ret(true), (byte) 16));
		opCodes.put((byte) 0xC0, new Opcode("RET NZ", () -> ret(!isZeroFlag()), (byte) 20, (byte) 8));
		opCodes.put((byte) 0xD0, new Opcode("RET NC", () -> ret(!isFullCarryFlag()), (byte) 20, (byte) 8));
		opCodes.put((byte) 0xC8, new Opcode("RET Z", () -> ret(isZeroFlag()), (byte) 20, (byte) 8));
		opCodes.put((byte) 0xFE, new Opcode("CP n", () -> compare(), (byte) 8));
		opCodes.put((byte) 0x28,
				new Opcode("JR Z, n", () -> relativeJump(isZeroFlag(), read8Immediate()), (byte) 12, (byte) 8));
		opCodes.put((byte) 0x18, new Opcode("JR n", () -> relativeJump(true, read8Immediate()), (byte) 12));
		opCodes.put((byte) 0xC3, new Opcode("JP nn", () -> pc = read16Immediate(), (byte) 16));
		opCodes.put((byte) 0xE9, new Opcode("JP (HL)", () -> jump(true, readDualRegister(getH(), getL())), (byte) 4));
		opCodes.put((byte) 0xCA,
				new Opcode("JP Z a16", () -> jump(isZeroFlag(), read16Immediate()), (byte) 16, (byte) 12));
		opCodes.put((byte) 0xC2,
				new Opcode("JP NZ a16", () -> jump(!isZeroFlag(), read16Immediate()), (byte) 16, (byte) 12));
		opCodes.put((byte) 0x20,
				new Opcode("JR NZ, n", () -> relativeJump(!isZeroFlag(), read8Immediate()), (byte) 12, (byte) 8));
		opCodes.put((byte) 0xEF, new Opcode("RST 28H", () -> rst((byte) 0x28), (byte) 16));
	}

	/**
	 * Builds extended opcode table (CB prefixed opcodes)
	 */
	private void loadCbOpCodes() {
		cbOpCodes.put((byte) 0x7C, new Opcode("BIT 7 H", () -> bit(7, getH()), (byte) 8));
		cbOpCodes.put((byte) 0x7F, new Opcode("BIT 7 F", () -> bit(7, getF()), (byte) 8));
		cbOpCodes.put((byte) 0x11, new Opcode("RL C", () -> rl(getC()), (byte) 8));
		cbOpCodes.put((byte) 0x87, new Opcode("RES 0, A", () -> res(0, getA()), (byte) 8));
		cbOpCodes.put((byte) 0x37, new Opcode("SWAP A", () -> swap(getA()), (byte) 8));

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

	public Register getF() {
		return f;
	}

	public boolean isZeroFlag() {
		return RegisterUtils.readBit(7, getF());
	}

	public boolean isOperationFlag() {
		return RegisterUtils.readBit(6, getF());
	}

	public boolean isHalfCarryFlag() {
		return RegisterUtils.readBit(5, getF());
	}

	public boolean isFullCarryFlag() {
		return RegisterUtils.readBit(4, getF());
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

	public int getOpcodeCount() {
		return this.opCodes.size();
	}

	public int getCbOpcodeCount() {
		return this.cbOpCodes.size();
	}

	public int getTotalOpcodeCount() {
		return getCbOpcodeCount() + getOpcodeCount();
	}

	private void setClockT(Register clockT) {
		this.clockT = clockT;
	}

	private void setConditionalNotExecFlag(boolean conditionalNotExecFlag) {
		this.conditionalNotExecFlag = conditionalNotExecFlag;
	}

	private void setFullCarryFlag(boolean fullCarryFlag) {
		getF().setValue(RegisterUtils.setBit(4, getF(), fullCarryFlag));
	}

	private void setHalfCarryFlag(boolean halfCarryFlag) {
		getF().setValue(RegisterUtils.setBit(5, getF(), halfCarryFlag));
	}

	private void setOperationFlag(boolean operationFlag) {
		getF().setValue(RegisterUtils.setBit(6, getF(), operationFlag));
	}

	private void setZeroFlag(boolean zeroFlag) {
		getF().setValue(RegisterUtils.setBit(7, getF(), zeroFlag));
	}

	private void setF(Register f) {
		this.f = f;
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
	 * Reads 8 bits from memory beginning at pc, and increments pc
	 */
	private byte read8Immediate() {
		byte d8 = mem.readByte(pc);
		pc++;
		return d8;
	}

	/**
	 * Reads 16 bits from memory beginning at pc, and increments pc
	 */
	private char read16Immediate() {
		byte b2 = mem.readByte(pc);
		pc++;
		byte b1 = mem.readByte(pc);
		pc++;
		return NumberUtils.bytesToWord(b2, b1);
	}

	/**
	 * Adds value at address pointed to by s1s2 to destination.
	 */
	private void addAddress(Register destination, Register s1, Register s2) {
		setOperationFlag(false);
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
		setOperationFlag(true);
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
	 * @param r
	 *            register to swap
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

	private void load(Register destination, Register source) {
		destination.setValue(source.getValue());
	}

	private void load(Register destination, char sourceAddress) {
		destination.setValue(mem.readByte(sourceAddress));
	}

	private void load(Register destination, byte value) {
		destination.setValue(value);
	}

	private void load(Register d1, Register d2, char value) {
		writeDualRegister(d1, d2, value);
	}

	private void load(Register addressDest1, Register addressDest2, byte value) {
		mem.writeByte(readDualRegister(addressDest1, addressDest2), value);
	}

	private void load(Register r1, Register r2, Register r3, boolean loadToAddress) {
		if (loadToAddress) {
			mem.writeByte(readDualRegister(r1, r2), r3.getValue());
		} else {
			r1.setValue(mem.readByte(readDualRegister(r2, r3)));
		}
	}

	private void load(char address, Register source) {
		mem.writeByte(address, source.getValue());
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
	 * Loads the value of source into the address pointed to by d1d2
	 */
	private void loadIncrement(Register r1, Register r2, Register r3, boolean loadToAddress) {
		if (loadToAddress) {
			char dual = readDualRegister(r1, r2);
			mem.writeByte(dual, r3.getValue());
			writeDualRegister(r1, r2, (char) (dual + 1));
		} else {
			char dual = readDualRegister(r2, r3);
			r1.setValue(mem.readByte(dual));
			dual++;
			writeDualRegister(r2, r3, dual);
		}
	}

	/**
	 * Loads the value of source into the address pointed to by 0xFF00 +
	 * destination
	 */
	private void ldh(Register destination, Register source) {
		char address = (char) (0xFF00 + destination.getValue());
		mem.writeByte(address, source.getValue());
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
	 * Performs a right-rotate-through-carry on r
	 */
	private void rr(Register r) {
		setFullCarryFlag(RegisterUtils.rightRotateThroughCarry(r, isFullCarryFlag()));
		setZeroFlag(r.getValue() == 0);
		setOperationFlag(false);
		setHalfCarryFlag(false);
	}

	// XXX: Should these two be generalized into one function?
	// Check if there are more "compare" instructions

	/**
	 * Compare 8-bit immediate to A
	 */
	private void compare() {
		byte immediate = read8Immediate();
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
		setOperationFlag(true);
		byte memAtDual = mem.readByte(readDualRegister(r1, r2));
		setZeroFlag(getA().getValue() == memAtDual);
		setHalfCarryFlag(NumberUtils.byteSubtractionNibbleBorrow(getA().getValue(), memAtDual));
		setFullCarryFlag(NumberUtils.byteSubtractionBorrow(getA().getValue(), memAtDual));
	}

	/**
	 * Returns to last address pushed onto the stack if condition evaluates to
	 * true
	 */
	private void ret(boolean condition) {
		if (condition) {
			char address = pop();
			logger.fine("RET called, returning to " + Integer.toHexString(address & 0xFFFF));
			pc = address;
		} else {
			setConditionalNotExecFlag(true);
		}
	}

	/**
	 * Call routine at 16-bit immediate
	 */
	private void call() {
		char address = read16Immediate();
		push(pc);
		logger.fine("Pushed address " + Integer.toHexString(pc & 0xFFFF) + " to stack");
		pc = address;
		logger.fine("Calling subroutine at 0x" + Integer.toHexString(address & 0xFFFF));
	}

	/**
	 * Enable interrupts
	 */
	private void setInterruptsEnabled(boolean enabled) {
		/* this will be important later, but for now it's a nop */
		// TODO: Implement CPU interrupts
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
	 * Tests bit number bitno of register r
	 */
	private void bit(int bitno, Register r) {
		boolean bitOn = RegisterUtils.readBit(bitno, r);
		if (!bitOn) {
			setZeroFlag(true);
		} else {
			setZeroFlag(false);
		}
		setOperationFlag(false);
		setHalfCarryFlag(true);
		logger.finer("Testing bit " + bitno + " of " + r + ": zeroFlag = " + isZeroFlag());
	}

	/**
	 * Resets specified bit of register r
	 */
	private void res(int bitNumber, Register r) {
		r.setValue(RegisterUtils.setBit(bitNumber, r, false));
	}

	/**
	 * Absolute jump to 16-bit immediate address if condition is met.
	 */
	private void jump(boolean condition, char address) {
		if (condition) {
			logger.finer("Jumping to " + StringUtils.charToReadableHex(address));
			pc = address;
		} else {
			setConditionalNotExecFlag(true);
			logger.finer("Jump condition not met; no jmp");
		}
	}

	/**
	 * Relative jump by SIGNED 8-bit immediate if condition is met.
	 */
	private void relativeJump(boolean condition, byte value) {
		if (condition) {
			logger.finer("Relative jump by " + StringUtils.byteToReadableHex(value));
			pc += value;
		} else {
			setConditionalNotExecFlag(true);
			logger.finer("Jump condition not met; no jmp");
		}
	}

	/**
	 * Jump to reset vector
	 */
	private void rst(byte resetVector) {
		push(pc);
		pc = (char) resetVector;
	}

	/**
	 * Adds value of r to the accumulator
	 */
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