/**
 * MemoryBlock.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.debugger;

import com.echodrop.gameboy.core.TailspinGB;
import com.echodrop.gameboy.interfaces.IInternalByteValue;

/**
 * Represents the value of a single memory address in the emulated RAM
 */
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
