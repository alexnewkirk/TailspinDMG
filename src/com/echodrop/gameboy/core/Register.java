/**
 * Register.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.core;

import com.echodrop.gameboy.interfaces.IInternalByteValue;

/**
 * Represents ann 8-bit internal register
 */
public class Register implements IInternalByteValue {

	private byte value;

	public Register(byte value) {
		this.setValue(value);
	}

	@Override
	public String toString() {
		return Integer.toHexString(getValue() & 0xFF);
	}

	@Override
	public byte getValue() {
		return value;
	}

	public void setValue(byte value) {
		this.value = value;
	}
	
	public void setValue(int value) {
		this.value = (byte) value;
	}

}