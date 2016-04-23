/**
 * MemoryAccessException.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.exceptions;

import com.echodrop.gameboy.core.MemoryRegion;
import com.echodrop.gameboy.util.StringUtils;

public class MemoryAccessException extends RuntimeException {
	private static final long serialVersionUID = -6219375668832275631L;

	public MemoryAccessException(char address) {
		super("Invalid memory access at: " + StringUtils.charToReadableHex(address));
	}
	
	public MemoryAccessException(char address, MemoryRegion region) {
		super("Invalid memory access in " + region.getName() + " at: " + StringUtils.charToReadableHex(address));
	}
}
