/**
 * MemoryRegion.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.core;

import com.echodrop.gameboy.exceptions.MemoryAccessException;
import com.echodrop.gameboy.util.StringUtils;

/**
 * Represents a logical block of memory in the emulator's RAM
 */
public class MemoryRegion {

	// Size of memory region in bytes
	public int size;
	private char start;
	private byte[] contents;
	private String name;

	/**
	 * @param start
	 *            The address in emulated ram where the MemoryRegion begins.
	 * @param end
	 *            The address in emulated ram where the MemoryRegion begins.
	 * @param name
	 *            A human readable name for the memory region
	 */
	public MemoryRegion(char start, char end, String name) {
		this.setName(name);
		this.start = start;
		this.size = end - start + 1;
		this.contents = new byte[size];
	}

	/**
	 * @return byte value at the specified address
	 */
	public byte getMem(char addr) {
		int index = addr - start;
		if (index < 0 || index > contents.length) {
			throw new MemoryAccessException(addr, this);
		}
		return contents[index];
	}

	/**
	 * Sets the specified address to the value of content
	 */
	public void setMem(char addr, byte content) {
		int index = addr - start;
		if (index < 0 || index > contents.length) {
			throw new MemoryAccessException(addr, this);
		}
		contents[index] = content;
	}

	/**
	 * @return Human-readable name
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		String table = "        00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F\n";
		table += "        -----------------------------------------------\n";

		for (int i = 0; i < contents.length / 16; i++) {

			table += "0x" + StringUtils.zeroLeftPad(Integer.toHexString((i * 16 + start)), 4) + "| ";

			for (int j = 0; j < 16; j++) {
				table += StringUtils.zeroLeftPad(Integer.toHexString(getMem((char) ((start + i * 16 + j))) & 0xFF), 2)
						+ " ";
			}
			table += "\n";
		}

		return name + "\n" + table;
	}

}