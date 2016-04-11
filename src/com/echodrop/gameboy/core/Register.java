package com.echodrop.gameboy.core;

import com.echodrop.gameboy.interfaces.IInternalByteValue;

/**
 * An 8 bit register
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