/**
 * MMU.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.core;

import java.util.logging.Logger;

import com.echodrop.gameboy.exceptions.MapperNotImplementedException;
import com.echodrop.gameboy.exceptions.MemoryAccessException;
import com.echodrop.gameboy.exceptions.RomFileSizeException;
import com.echodrop.gameboy.rom.RomFile;
import com.echodrop.gameboy.util.NumberUtils;

/**
 * Emulation core for GameBoy Memory Management Unit.
 * 
 */
public class MMU {

	private static final Logger logger = Logger.getLogger(MMU.class.getName());
	private TailspinGB system;

	/**
	 * After the bootstrap runs, it is unmapped from memory by setting this flag
	 * to false
	 */
	private boolean biosMapped = true;

	/* Memory Map */
	private MemoryRegion bios;
	private MemoryRegion romBank0; // Always contains the first 16k of the ROM
	private MemoryRegion romBank;
	private MemoryRegion workingRam;
	private MemoryRegion externalRam;
	private MemoryRegion zeroPage;
	private RomFile loadedRomFile;

	public MMU(TailspinGB system) {
		this.system = system;
		this.initialize();
	}

	/**
	 * Sets MMU to initial state
	 */
	public void initialize() {
		setBios(new MemoryRegion((char) 0x0000, (char) 0x00ff, "bios"));
		setRomBank0(new MemoryRegion((char) 0x0000, (char) 0x3fff, "romBank0"));
		setRomBank(new MemoryRegion((char) 0x4000, (char) 0x7FFF, "romBank"));
		setWorkingRam(new MemoryRegion((char) 0xc000, (char) 0xdfff, "workingRam"));
		setZeroPage(new MemoryRegion((char) 0xff80, (char) 0xffff, "zeroPage"));
		setExternalRam(new MemoryRegion((char) 0xa000, (char) 0xbfff, "externalRam"));
	}

	public void initLogging() {
		logger.setParent(system.getLogger());
	}

	/**
	 * Loads the DMG bootstrap into memory
	 */
	public void loadBootstrap(byte[] gbBios) {
		if (gbBios.length > 256) {
			throw new RomFileSizeException(256, gbBios.length);
		}
		for (int i = 0; i < gbBios.length; i++) {
			getBios().setMem((char) i, (byte) (gbBios[i] & 0xFF));
		}
		biosMapped = true;
		logger.info("Bootstrap loaded: " + gbBios.length + " bytes");
	}

	/**
	 * Loads a ROM binary with the specified filename into memory
	 * 
	 * @throws MapperNotImplementedException
	 *             if the ROM uses an unsupported MBC
	 */
	public void loadRom(byte[] romData) throws MapperNotImplementedException {

		RomFile rf = new RomFile(romData);
		loadedRomFile = rf;
		logger.info("Attempting to load ROM...");
		logger.info(rf.toString());

		// Load first 16kb regardless of what type of cartridge it is
		for (int i = 0; i < 0x4000; i++) {
			getRomBank0().setMem((char) i, (byte) (romData[i] & 0xFF));
		}

		// Switch on cartridge type to load the rest; for now only need to
		// support
		// ctype 0 and MBC1
		switch (loadedRomFile.cartridgeType) {
		case 0:
			// No MBC (32kb ROM)
			for (int i = 0x4000; i < 0x8000; i++) {
				getRomBank().setMem((char) i, (byte) (romData[i] & 0xFF));
			}
			break;
		case 1:
			// MBC1
			break;
		default:
			throw new MapperNotImplementedException();
		}

		logger.info("ROM data loaded: " + romData.length + " bytes");
	}

	/**
	 * Based on the write-up at:
	 * http://imrannazar.com/GameBoy-Emulation-in-JavaScript:-Memory
	 * 
	 * @return the MemoryRegion that the specified address will be located in.
	 */
	public MemoryRegion findMemoryRegion(char address) {
		/*
		 * mask off the last 4 bits of the address to find which memory region
		 * it's located in
		 */
		switch (address & 0xF000) {
		case 0x0000:
			if (biosMapped) {
				if (address < 0x100) {
					return getBios();
				}
			}
			return getRomBank0();

		// ROM Bank 0
		case 0x1000:
		case 0x2000:
		case 0x3000:
			return getRomBank0();

		// ROM bank 1
		case 0x4000:
		case 0x5000:
		case 0x6000:
		case 0x7000:
			return getRomBank();

		// VRAM
		case 0x8000:
		case 0x9000:
			return system.getGpu().getVram();

		// External RAM
		case 0xA000:
		case 0xB000:
			return getExternalRam();

		// Working RAM
		case 0xC000:
		case 0xD000:
			return getWorkingRam();

		// WRAM shadow
		case 0xE000:
			return getWorkingRam();

		// WRAM shadow, I/O, OAM, Zero-page
		case 0xF000:
			switch (address & 0x0F00) {

			// OAM
			case 0xE00:

				if (address >= 0xFEA0 && address <= 0xFEFF) {
					return null;
				} else if (address >= 0xFE00 && address <= 0xFE9F) {
					return system.getGpu().getOam();
				}

				// Zero-page
			case 0xF00:
				if (address >= 0xFF80) {
					return getZeroPage();
				} else {
					/* I/O. This should never happen */
					logger.severe("I/O read or write attempted by MMU at " + Integer.toHexString(address & 0xFFFF));
					throw new MemoryAccessException(address);
				}
			}

			// wram shadow. should be called for 0x000 - 0xD00
		default:
			return getWorkingRam();
		}
	}

	/**
	 * @return an 8-bit value from the address specified.
	 */
	public byte readByte(char address) {

		switch (address) {
		case 0xFF00:
			// D-pad
			return 0;
		case 0xFF01:
			// Link cable: data
			return 0;
		case 0xFF02:
			// Link cable: serial transfer control
			return (byte) 0x81; // 0b10000001, "START TRANSFER"
		default:
			if (address >= 0xFF33 && address <= 0xFF7F) {
				return system.getGpu().readByte(address);
			} else if (address >= 0xE000 && address <= 0xFDFF) {
				address -= 0x2000;
			}
			
			MemoryRegion r = findMemoryRegion(address);
			if (r != null) {
				return r.getMem(address);
			}
			return 0;

		}
		// if (address == 0xFF00) {
		// // D-pad
		// return 00;
		// } else if(address == 0xFF01) {
		// // Link cable: data
		// return 00;
		// }else if(address == 0xFF02) {
		// // Link cable: serial transfer control
		// return (byte)0x81; // 0b10000001, "START TRANSFER"
		// } else if (address >= 0xFF03 && address <= 0xFF7F) {
		// return system.getGpu().readByte(address);
		// }
		//
		// //Trap reads from ECHO RAM
		// if(address >= 0xE000 && address <= 0xFDFF) {
		// address -= 0x2000;
		// }
		//
		// MemoryRegion r = findMemoryRegion(address);
		// if (r != null) {
		// return r.getMem(address);
		// }
		// return 0;
	}

	/**
	 * @return a 16-bit value from the address specified.
	 */
	public char readWord(char address) {

		if (address >= 0xFF00 && address <= 0xFF7F) {
			return system.getGpu().readWord(address);
		}
		byte b1 = readByte(address);
		byte b2 = readByte((char) (address + 1));
		return NumberUtils.bytesToWord(b1, b2);
	}

	/**
	 * Writes an 8-bit value into the address specified.
	 */
	public void writeByte(char address, byte data) {
		if (address == 0xFF50 && data == 1) {
			biosMapped = false;
			logger.info("[!] BIOS unmapped from memory");
		} else if (address == 0xFF00) {
			// D-pad
		} else if (address == 0xFF01) {
			// Link-cable: data
			// TODO: out this to the logger instead of syso
			System.out.print((char) data);
		} else if (address == 0xFF02) {
			// Link-cable: serial transfer control

		} else if (address >= 0xFF03 && address <= 0xFF7F) {
			system.getGpu().writeByte(address, data);
		} else {
			// Trap writes to ECHO RAM
			if (address >= 0xE000 && address <= 0xFDFF) {
				address -= 0x2000;
			}

			MemoryRegion r = findMemoryRegion(address);
			if (r != null) {
				r.setMem(address, data);
			}
		}
	}

	public MemoryRegion getBios() {
		return bios;
	}

	private void setBios(MemoryRegion bios) {
		this.bios = bios;
	}

	public MemoryRegion getRomBank0() {
		return romBank0;
	}

	public MemoryRegion getRomBank() {
		return romBank;
	}

	public void setRomBank(MemoryRegion romBank) {
		this.romBank = romBank;
	}

	public MemoryRegion getWorkingRam() {
		return workingRam;
	}

	public MemoryRegion getExternalRam() {
		return externalRam;
	}

	public MemoryRegion getZeroPage() {
		return zeroPage;
	}

	private void setZeroPage(MemoryRegion zeroPage) {
		this.zeroPage = zeroPage;
	}

	private void setExternalRam(MemoryRegion externalRam) {
		this.externalRam = externalRam;
	}

	private void setWorkingRam(MemoryRegion workingRam) {
		this.workingRam = workingRam;
	}

	private void setRomBank0(MemoryRegion rom) {
		this.romBank0 = rom;
	}

	public boolean isBiosMapped() {
		return biosMapped;
	}

}
