/**
 * FileSizeException.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.exceptions;

public class RomFileSizeException extends RuntimeException {

	private static final long serialVersionUID = -451029893765070554L;
	private int expectedSize;
	private int actualSize;
	
	public RomFileSizeException(int expectedSize, int actualSize) {
		this.expectedSize = expectedSize;
		this.actualSize = actualSize;
	}
	
	@Override
	public String toString() {
		return "Expected size: " + expectedSize + " bytes. Actual size: " + actualSize + " bytes";
	}

}
