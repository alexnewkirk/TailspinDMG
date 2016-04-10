package com.echodrop.gameboy.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import com.echodrop.gameboy.exceptions.MemoryAccessException;

/**
 * Emulation core for GameBoy MMU.
 * 
 * @author echo_drop
 */
public class MMU {

	private TailspinGB system;

	private static final Logger logger = Logger.getLogger(MMU.class.getName());

	// After the bios runs, it unmaps itself from memory
	private boolean biosMapped = true;

	// Memory map
	private MemoryRegion bios;
	private MemoryRegion rom;
	private MemoryRegion workingRam;
	private MemoryRegion externalRam;
	private MemoryRegion zeroPage;

	public MMU(TailspinGB system) {
		this.system = system;
		this.initialize();
	}

	/**
	 * ========================== GameBoy Memory Regions: ] [ 16-bit address
	 * space ] [ 0x0000 to 0xFFFF ] ==========================
	 * 
	 * [0x0000 - 0x00FF] BIOS, while its mapped into memory. After the BIOS runs
	 * it is removed from memory (first instruction after 0x00FF)
	 * 
	 * [0x0000 - 0x3FFF] Bank 0 of Cartridge ROM -[0100-014F] Header
	 * 
	 * [0x4000 - 0x7FFF] Bank 1 of Cartridge ROM
	 * 
	 * [0x8000 - 0x9FFF] VRAM
	 * 
	 * [0xC000 - 0xDFFF] WRAM
	 * 
	 * [0xE000 - 0xFDFF] WRAM shadow
	 * 
	 * [FE00-FE9F] Sprite data
	 * 
	 * [FF00-FF7F] Mem-mapped I/O
	 * 
	 * [FF80-FFFF] Zero-page
	 * 
	 */

	/**
	 * Sets MMU to initial state
	 */
	public void initialize() {
		bios = new MemoryRegion((char) 0x0000, (char) 0x00ff, "bios");
		rom = new MemoryRegion((char) 0x0000, (char) 0x7fff, "rom");
		workingRam = new MemoryRegion((char) 0xc000, (char) 0xdfff, "workingRam");
		zeroPage = new MemoryRegion((char) 0xff80, (char) 0xffff, "zeroPage");
		externalRam = new MemoryRegion((char) 0xa000, (char) 0xbfff, "externalRam");
	}

	public void initLogging() {
		logger.setParent(system.getLogger());
	}

	/**
	 * Loads the DMG bios into memory
	 */
	public void loadBios() {
		Path path = Paths.get("bios.gb");
		byte[] gbBios = null;
		try {
			gbBios = Files.readAllBytes(path);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < gbBios.length - 1; i++) {
			bios.setMem((char) i, (byte) (gbBios[i] & 0xFF));
		}

		logger.info("BIOS loaded: " + gbBios.length + " bytes");

	}

	/**
	 * Loads a rom binary of the specified filename into memory
	 */
	public void loadRom(String filename) {
		Path path = Paths.get(filename);
		byte[] romData = null;
		try {
			romData = Files.readAllBytes(path);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < romData.length - 1; i++) {
			rom.setMem((char) i, (byte) (romData[i] & 0xFF));
		}
		logger.info("ROM loaded: " + filename + " : " + romData.length + " bytes\n");
	}

	/**
	 * Returns the MemoryRegion that the specified address will be located in.
	 * 
	 * Based off of the write-up at:
	 * http://imrannazar.com/GameBoy-Emulation-in-JavaScript:-Memory
	 */
	public MemoryRegion findMemoryRegion(char address) {

		// mask off the last 4 bits of the address
		// to find which memory region it's located in
		switch (address & 0xF000) {

		case 0x0000:
			if (biosMapped) {

				if (address < 0x100) {

					return bios;

				} else {

					/**
					 * 
					 * This will be important later
					 *
					 */
					// reached the end of the bios
					// biosMapped = false;

				}
			}

			return rom;

		// ROM Bank 0
		case 0x1000:
		case 0x2000:
		case 0x3000:
			return rom;

		// ROM bank 1
		case 0x4000:
		case 0x5000:
		case 0x6000:
		case 0x7000:
			return rom;

		// VRAM
		case 0x8000:
		case 0x9000:
			return system.getGpu().getVram();

		// External RAM
		case 0xA000:
		case 0xB000:
			return externalRam;

		// Working RAM
		case 0xC000:
		case 0xD000:
			return workingRam;

		// WRAM shadow
		case 0xE000:
			return workingRam;

		// WRAM shadow, I/O, OAM, Zero-page
		case 0xF000:
			switch (address & 0x0F00) {

			// OAM
			case 0xE00:
				if (address < 0xFEA0) {
					return system.getGpu().getOam();
				}

				// Zero-page
			case 0xF00:
				if (address >= 0xFF80) {
					return zeroPage;
				} else {
					// I/O. This should never happen
					logger.severe("I/O read or write attempted by MMU at " + Integer.toHexString(address & 0xFFFF));
					//throw new MemoryAccessException(address);
				}
			}

			// wram shadow. should be called for 0x000 - 0xD00
		default:
			return workingRam;

		}

	}

	/**
	 * Reads an 8-bit value from the address specified.
	 */
	public byte readByte(char address) {
		if (address >= 0xFF00 && address <= 0xFF7F) {
			return system.getGpu().readByte(address);
		}
		
		MemoryRegion r = findMemoryRegion(address);
		return r.getMem(address);
	}

	/**
	 * Reads a 16-bit value from the address specified.
	 */
	public char readWord(char address) {

		if (address >= 0xFF00 && address <= 0xFF7F) {
			return system.getGpu().readWord(address);
		}

		byte b1 = readByte(address);
		byte b2 = readByte((char) (address + 1));

		return Util.bytesToWord(b1, b2);
	}

	/**
	 * Writes an 8-bit value into the address specified.
	 */
	public void writeByte(char address, byte data) {
		if (address >= 0xFF00 && address <= 0xFF7F) {
			system.getGpu().writeByte(address, data);
		} else {
			MemoryRegion r = findMemoryRegion(address);
			r.setMem(address, data);
		}
	}

}
