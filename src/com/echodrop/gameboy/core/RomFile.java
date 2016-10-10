package com.echodrop.gameboy.core;

public class RomFile {
	
	public String title;
	public String mfgCode;
	public byte cartridgeType;
	public byte ramSize;
	public byte romSize;
	public boolean destCodeJp;
	public byte headerChecksum;
	public char cartridgeChecksum;
	
	public byte[] romData;
	public byte[] ram;
	
	public RomFile(byte[] rom) {
		byte[] titleBytes = new byte[16];
		for(int i = 0x134; i < 0x143; i++) {
			titleBytes[i-0x134] = rom[i];
		}
		this.title = new String(titleBytes);
		
		byte[] mfgCodeBytes = new byte[3];
		for(int i = 0x13f; i < 0x142; i++) {
			mfgCodeBytes[i-0x13f] = rom[i];
		}
		this.mfgCode = new String(mfgCodeBytes);
		
		
	}
	
	@Override
	public String toString() {
		return "ROM HEADER INFO:\n" + title + "\nMFG CODE: " + mfgCode +
				"\nCARTRIDGE TYPE: " + cartridgeType + "\nROM SIZE: " + 
				romSize + "\nRAM SIZE: " + ramSize + "\nDESTINATION CODE: ";
	}
}
