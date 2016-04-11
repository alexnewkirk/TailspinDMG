package com.echodrop.gameboy.debugger;

import com.echodrop.gameboy.core.TailspinGB;
import com.echodrop.gameboy.interfaces.IInternalByteValue;

public class MemoryBlock implements IInternalByteValue {
	
	private TailspinGB system;
	private char address;
	
	public MemoryBlock(TailspinGB system, char address) {
		this.system = system;
		this.address = address;
	}

	@Override
	public byte getValue() {
		return system.getMem().readByte(address);
	}

}
