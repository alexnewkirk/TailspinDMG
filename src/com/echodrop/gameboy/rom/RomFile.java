package com.echodrop.gameboy.rom;

import com.echodrop.gameboy.util.NumberUtils;
import com.echodrop.gameboy.util.StringUtils;

public class RomFile {
	
	public String title;
	public String mfgCode;
	public byte cartridgeType;
	public long romSize;
	public byte ramSize;
	public byte destCode;
	public byte headerChecksum;
	public char cartridgeChecksum;
	
	public byte[] romData;
	public byte[] ram;
	
	public RomFile(byte[] rom) {
		this.romData = rom;
		this.title = new String(readSection(0x134, 0x143));
		this.mfgCode = new String(readSection(0x13f, 0x142));
		this.cartridgeType = romData[0x147];
		this.romSize = 32000 << romData[0x148];
		this.ramSize = romData[0x149];
		this.destCode = romData[0x14A];;
		this.headerChecksum = romData[0x14D];
		
		byte[] globalChecksumBytes = readSection(0x14E, 0x150);
		this.cartridgeChecksum = NumberUtils.bytesToWord(globalChecksumBytes[0], globalChecksumBytes[1]);
	}
	
	public byte[] readSection(int start, int end) {
		if(romData != null) {
			byte[] sectionBytes = new byte[end - start];
			for(int i = start; i < end; i++) {
				sectionBytes[i-start] = romData[i];
			}
			return sectionBytes;
		} else {
			throw new NullPointerException("ROM data is null, could not read section");
		}
	}
	
	@Override
	public String toString() {
		return "ROM HEADER INFO:\n" + title + "\nMFG CODE: " + mfgCode +
				"\nCARTRIDGE TYPE: " + cartridgeType + "\nROM SIZE: " + 
				romSize + "\nRAM SIZE: " + ramSize + "\nDESTINATION CODE: " +
				destCode + "\nHEADER CHECKSUM: " + StringUtils.byteToReadableHex(headerChecksum) + 
				"\nCARTRIDGE CHECKSUM: " + StringUtils.charToReadableHex(cartridgeChecksum);
	}
}
