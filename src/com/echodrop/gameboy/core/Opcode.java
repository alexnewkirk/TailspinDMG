/**
 * OpCode.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.core;

/**
 * Represents a GameBoy Z80 CPU instruction
 */
public class Opcode {
	
	private Runnable instruction;
	private String disassembly;
	private byte mTime;
	private byte conditionalTime;
	
	public Opcode(String disassembly, Runnable instruction, byte m_time) {
		this.setDisassembly(disassembly);
		this.setInstruction(instruction);
		this.setMTime(m_time);
	}
	
	public Opcode(String disassembly, Runnable instruction, byte m_time, byte conditional_time) {
		this(disassembly, instruction, m_time);
		this.setConditionalTime(conditionalTime);
	}
	
	/**
	 * Executes the CPU instruction
	 */
	public void exec() {
		instruction.run();
	}

	public Runnable getInstruction() {
		return instruction;
	}

	public void setInstruction(Runnable instruction) {
		this.instruction = instruction;
	}

	public String getMnemonic() {
		return disassembly;
	}

	public void setDisassembly(String disassembly) {
		this.disassembly = disassembly;
	}

	public byte getMTime() {
		return mTime;
	}

	public void setMTime(byte mTime) {
		this.mTime = mTime;
	}

	public byte getConditional_time() {
		return conditionalTime;
	}

	public void setConditionalTime(byte conditionalTime) {
		this.conditionalTime = conditionalTime;
	}

}
