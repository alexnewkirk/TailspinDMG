package com.echodrop.gameboy.core;

import java.util.HashMap;

import com.echodrop.gameboy.interfaces.IMMU;

/**
 * Emulation core for Z80 microprocessor
 * @author echo_drop
 *
 */
public class Z80 {
	
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
	
	//CPU registers
	Register a;
	Register b; 
	Register c;
	Register d;
	Register e;
	Register h;
	Register l;
	
	//Flag register, stored as boolean
	//values for convenience
	boolean zeroFlag;
	boolean operationFlag;
	boolean halfCarryFlag;
	boolean fullCarryFlag;
	
	//Special registers
	public char pc;
	char sp;
	
	//Clocks
	byte clock_t;
	byte clock_m;
	
	//Clock registers
	byte t;
	byte m;
	
	//Memory Management Unit
	IMMU mem;
	
	boolean running;
	
	//Opcode tables
	HashMap<Byte, Runnable> opCodes;
	HashMap<Byte, Runnable> cbOpCodes;
	
	
	public Z80(GameBoy system) {
		this.reset();
		
		this.system = system;
		this.mem = system.getMem();
		
		this.opCodes = new HashMap<Byte, Runnable>();
		this.cbOpCodes = new HashMap<Byte, Runnable>();
		this.loadOpcodes();
		this.loadCbOpcodes();
		
		this.running = true;
	}
	
	public void beginDispatch() {
		
		while(running) {
			
			//Increment instruction pointer 
			
			System.out.println("Instruction pointer: " + Integer.toHexString(pc));
			
			//Grab next instruction
			byte opcode = mem.readByte(pc++);
			
			System.out.println("Opcode: 0x" + Integer.toHexString(opcode));
			
			//mask instruction pointer to 16 bits
			pc &= 65535;
			
			//Execute the instruction
			
			Runnable instruction;
			if((opcode & 0xFF) == 0xCB) {
				System.out.println("CB prefixed opcode detected");
				opcode = mem.readByte((char)(pc));
				System.out.println("Opcode: 0x" + Integer.toHexString(opcode));
				instruction = cbOpCodes.get(opcode);
				pc++;
			} else {
				instruction = opCodes.get(opcode);
			}
			
			if(instruction != null) {
				instruction.run();
			} else {
				System.out.println("Unimplemented instruction: " + Integer.toHexString(opcode & 0xFF));
				System.exit(1);
			}
			System.out.println();
			
			
			
			//Increment clocks by the amount of time
			//that passed during the instruction
			clock_t += t;
			clock_m += m;
		}
		
	}
	
	/**
	 * Resets the CPU to its initial state
	 */
	public void reset() {
		a = new Register((byte)0x0);
		b = new Register((byte)0x0);
		c = new Register((byte)0x0);
		d = new Register((byte)0x0);
		e = new Register((byte)0x0);
		h = new Register((byte)0x0);
		l = new Register((byte)0x0);
		
		zeroFlag = false;
		operationFlag = false;
		halfCarryFlag = false;
		fullCarryFlag = false;
		
		pc = 0;
		sp = 0;
		
		t = 0;
		m = 0;
		
		clock_t = 0;
		clock_m = 0;
	}
	
	private void writeDualRegister(Register r1, Register r2, char value) {
		r2.value = (byte)((value & 0xFF));
		r1.value = (byte)((value >>> 8) & 0xFF);
		
		System.out.print("converted " + Integer.toHexString(value) + " to: ");
		System.out.println(Integer.toHexString(r1.value & 0xff) + " " + Integer.toHexString(r2.value & 0xFF));
	}
	
	private char readDualRegister(Register r1, Register r2) {
		return (char) (r1.value << 8 | r2.value & 0xFF);
	}
	
	private void loadOpcodes() {
		opCodes.put((byte)0x31, () -> ldSpNn());
		opCodes.put((byte)0xaf, () -> xorA());
		opCodes.put((byte)0x21, () -> ldHlNn());
		opCodes.put((byte)0x32, () -> lddHlA());
		opCodes.put((byte)0x20, () -> jrNzN());
		opCodes.put((byte)0x0, () -> nop());
		opCodes.put((byte)0xfb, () -> eI());
		opCodes.put((byte)0x0e, () -> ldCn());
		opCodes.put((byte)0x9f, () -> res3a());
	}
	
	private void loadCbOpcodes() {
		cbOpCodes.put((byte)0x7c, () -> bit7h());
	}
	
	//enable interrupts
	private void eI() {
		//for now this is essentially a noop,
		//it might be important but idk yet what it does
	}

	private void nop() {
		//no operation
	}

	//test bit 7 of register H
	private void bit7h() {
		String bin = Integer.toBinaryString(h.value & 0xFF);
		if(bin.toCharArray()[7] == '0') {
			zeroFlag = true;
		} else {
			zeroFlag = false;
		}
		System.out.println("Testing bit 7 of " + bin + ": zeroFlag = " + zeroFlag);
		
		//DEBUG, REMOVE ASAP
		//zeroFlag = true;
	}

	//Loads a 16 bit immediate into SP
	private void ldSpNn() {
		sp = mem.readWord((char)(pc));
		System.out.println("Loaded value: " + Integer.toHexString(sp) + " into SP");
		
		pc += 2;
	}
	
	//XOR A against A
	private void xorA() {
		a.value ^= a.value;
		System.out.println("A XOR A. A = " + a);
	}
	
	//Loads a 16 bit immediate into HL
	private void ldHlNn() {
		char value = mem.readWord((char)pc);
		
		writeDualRegister(h, l, value);
		
		System.out.println("Loaded value:" + Integer.toHexString(value) + " into HL");
		
		pc += 2;
	}
	
	//Save A to address pointed to by HL, and decrement HL
	private void lddHlA() {
		
		char address = readDualRegister(h, l);
		mem.writeByte(address, a.value);
		
		System.out.println("Wrote A (" + Integer.toHexString(a.value) +
				") to address in HL (" + Integer.toHexString(address) +
				")");
		
		address--;
		
		System.out.println("And decremented HL to " + Integer.toHexString(address));
	}
	
	//Relative jump by signed immediate (single byte) if last result was not zero
	private void jrNzN() {
		if(!zeroFlag) {
			byte n = (byte)(mem.readByte(pc) & 0xFF);
			pc += n;
			System.out.println("Jmping by " + n);
		} else {
			System.out.println("Zero flag set, no jmp");
		}
	}
	
	//load 8-bit immediate into C
	private void ldCn() {
		c.value = mem.readByte(pc);
		pc++;
	}
	
	//reset bit 3 of A
	private void res3a() {
		String bin = Integer.toBinaryString(a.value & 0xff);
		System.out.println("Resetting bit 3 of A");
		System.out.println("A before reset: " + bin);
		System.out.println("A after reset: ");
	}
	

}
