package com.echodrop.gameboy.exceptions;

public class MemoryAccessException extends RuntimeException {

	public MemoryAccessException(char address) {
		super("Invalid memory access at: " + Integer.toHexString(address & 0xFFFF));
	}
}
