package com.echodrop.gameboy.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.echodrop.gameboy.interfaces.IMMU;

public class GBMMU implements IMMU {
	
	private GameBoy system;
	
	private boolean biosMapped = true;
	
	private MemoryRegion bios;
	private MemoryRegion rom;
	private MemoryRegion workingRam;
	private MemoryRegion externalRam;
	private MemoryRegion zeroPage;
	
	
	public GBMMU(GameBoy system) {
		this.system = system;
		this.initialize();
	}
	
	
	/**
	 * ==========================
	 * GameBoy Memory Regions: ]
	 *  [ 16-bit address space ]
	 *  [   0x0000 to 0xFFFF   ]
	 * ==========================
	 * 
	 *  [0x0000 - 0x00FF]
	 *  BIOS, while its mapped into memory. After the BIOS runs
	 *  it is removed from memory (first instruction after 0x00FF)
	 *  
	 *  [0x0000 - 0x3FFF]
	 *  Bank 0 of Cartridge ROM
	 *    -[0100-014F] Header
	 *    
	 *  [0x4000 - 0x7FFF]
	 *  Bank 1 of Cartridge ROM
	 *  
	 *  [0x8000 - 0x9FFF]
	 *  VRAM
	 *  
	 *  [0xC000 - 0xDFFF]
	 *  WRAM
	 *  
	 *  [0xE000 - 0xFDFF]
	 *  WRAM shadow
	 *  
	 *  [FE00-FE9F] 
	 *  Sprite data
	 *  
	 *  [FF00-FF7F]
	 *  Mem-mapped I/O
	 *  
	 *  [FF80-FFFF]
	 *  Zero-page
	 * 
	 */
	
	public void initialize() {
		
		bios = new MemoryRegion((char)0x0000, (char)0x00ff);
		rom = new MemoryRegion((char)0x0000, (char)0x7fff);
		workingRam = new MemoryRegion((char)0xc000, (char)0xdfff);
		zeroPage = new MemoryRegion((char)0xff80, (char) 0xffff);
		externalRam = new MemoryRegion((char) 0xa000, (char)0xbfff);
		
		//load bios
		byte[] gbBios = loadBios();
		
		//System.out.println(bios.contents.length);
		for(int i = 0; i < gbBios.length -1; i++) {
//			System.out.println(Integer.toHexString(gbBios[i] & 0xFF));
			bios.setMem((char)i, (byte)(gbBios[i] & 0xFF));
		}

	}
	
	public byte[] loadBios() {
		Path path = Paths.get("bios.gb");
	    try {
			return Files.readAllBytes(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return null;
	}
	
	public byte[] loadRom(String filename) {
		Path path = Paths.get(filename);
		try {
			return Files.readAllBytes(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	    return null;
	}
	
	@Override
	public byte readByte(char address) {
		MemoryRegion r = findMemoryRegion(address);
		return r.getMem(address);
	}

	public MemoryRegion findMemoryRegion(char address) {

		//mask off the last 4 bits of the address
		//to find which memory region it's located in
		switch(address & 0xF000) {
		
		case 0x0000:
			if(biosMapped) {
				
				if(address < 0x100) {
					
					return bios;
					
				} else {
					
					//reached the end of the bios
					biosMapped = false;
					
				}
			} 
			
			return rom;
		
		//ROM Bank 0
		case 0x1000:
		case 0x2000:
		case 0x3000:
			return rom;
			
		//ROM bank 1
		case 0x4000:
		case 0x5000:
		case 0x6000:
		case 0x7000:
			return rom;
		
		//VRAM
		case 0x8000:
		case 0x9000:
			return system.getGpu().getVram();
		
		//External RAM
		case 0xA000:
		case 0xB000:
			return externalRam;
		
		//Working RAM
		case 0xC000:
		case 0xD000:
			return workingRam;
		
		//WRAM shadow
		case 0xE000:
			return workingRam;
		
		//WRAM shadow, I/O, OAM, Zero-page
		case 0xF000:
			switch(address & 0x0F00) {
			
			//OAM
			case 0xE00:
				if(address < 0xFEA0) {
					return system.getGpu().getOam();
				}
				
			//Zero-page
			case 0xF00:
				if(address >= 0xFF80) {
					return zeroPage;
				} else {
					//I/O control, unimplemented
					System.err.println("IO call made");
				}
			}
			
			//wram shadow. should be called for 0x000 - 0xD00
			default:
				return workingRam;
			
		}
			
	}

	@Override
	public char readWord(char address) {
		char result = (char) (readByte(address) << 8 | readByte((char)(address + 1)) & 0xFF);
		return result;
	}

	@Override
	public void writeByte(char address, byte data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeWord(char address, char data) {
		// TODO Auto-generated method stub
		
	}


}
