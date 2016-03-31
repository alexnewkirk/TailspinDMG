package com.echodrop.gameboy.interfaces;

public interface IMMU {
	
	public byte readByte(char address);
	
	public char readWord(char address);
	
	public void writeByte(char address, byte data);
	
	public void writeWord(char address, char data);

}
