package com.echodrop.gameboy.core;

/**
 * An 8 bit register
 */
public class Register {

	public byte value;

	public Register(byte value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return Integer.toHexString(value & 0xFF);
	}

}