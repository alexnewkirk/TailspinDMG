package com.echodrop.gameboy.exceptions;

public class FileSizeException extends RuntimeException {

	private static final long serialVersionUID = -451029893765070554L;
	private int expectedSize;
	private int actualSize;
	
	public FileSizeException(int expectedSize, int actualSize) {
		this.expectedSize = expectedSize;
		this.actualSize = actualSize;
	}
	
	@Override
	public String toString() {
		return "Expected size: " + expectedSize + " bytes. Actual size: " + actualSize + " bytes";
	}

}
