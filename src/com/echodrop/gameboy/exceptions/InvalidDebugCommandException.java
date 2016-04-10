package com.echodrop.gameboy.exceptions;

public class InvalidDebugCommandException extends RuntimeException {

	private static final long serialVersionUID = -6126298428212151592L;
	
	public InvalidDebugCommandException(String command) {
		super("Invalid debug command: " + command);
	}

}
