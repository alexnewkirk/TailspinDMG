package com.echodrop.gameboy.exceptions;

public class MemoryAccessException extends RuntimeException {
	private static final long serialVersionUID = -6219375668832275631L;

	public MemoryAccessException(char address) {
		super("Invalid memory access at: " + Integer.toHexString(address & 0xFFFF));
	}
}
