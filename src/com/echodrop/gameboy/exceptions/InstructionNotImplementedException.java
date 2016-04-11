/**
 * InstructionNotImplementedException.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.exceptions;

public class InstructionNotImplementedException extends RuntimeException {

	private static final long serialVersionUID = 2072035664604310473L;

	public InstructionNotImplementedException(byte instruction, char address) {
		super("Unimplemented instruction at " + Integer.toHexString(address & 0xFFFF) + ": "
				+ Integer.toHexString(instruction & 0xFF));
	}

}
