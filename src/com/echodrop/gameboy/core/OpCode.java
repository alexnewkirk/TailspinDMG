package com.echodrop.gameboy.core;

public class OpCode {
	
	private Runnable instruction;
	private String disassembly;
	private byte m_time;
	private byte conditionalTime;
	
	public OpCode(String disassembly, Runnable instruction, byte m_time) {
		this.setDisassembly(disassembly);
		this.setInstruction(instruction);
		this.setM_time(m_time);
	}
	
	public OpCode(String disassembly, Runnable instruction, byte m_time, byte conditional_time) {
		this(disassembly, instruction, m_time);
		this.setConditionalTime(conditionalTime);
	}
	
	
	public void exec() {
		instruction.run();
	}

	public Runnable getInstruction() {
		return instruction;
	}

	public void setInstruction(Runnable instruction) {
		this.instruction = instruction;
	}

	public String getDisassembly() {
		return disassembly;
	}

	public void setDisassembly(String disassembly) {
		this.disassembly = disassembly;
	}

	public byte getM_time() {
		return m_time;
	}

	public void setM_time(byte m_time) {
		this.m_time = m_time;
	}

	public byte getConditional_time() {
		return conditionalTime;
	}

	public void setConditionalTime(byte conditionalTime) {
		this.conditionalTime = conditionalTime;
	}

}
