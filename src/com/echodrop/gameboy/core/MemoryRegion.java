package com.echodrop.gameboy.core;

public class MemoryRegion {

	char start;
	char end;
	public int size; // in bytes
	byte[] contents;
	String name;

	public MemoryRegion(char start, char end, String name) {
		this.name = name;
		this.start = start;
		this.end = end;
		this.size = end - start + 1;
		this.contents = new byte[size];
	}

	public byte getMem(char addr) {
		return contents[addr - start];
	}

	public void setMem(char addr, byte content) {
		// System.out.print("Writing " + Integer.toHexString(content & 0xFF) + "
		// to ");
		// System.out.println(Integer.toHexString(addr) + " in " + name);
		// System.out.println("Size of " + name + " is: " + size);
		contents[addr - start] = content;
	}

	@Override
	public String toString() {
		String table = "        00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F\n";
		table += "        -----------------------------------------------\n";

		for (int i = 0; i < contents.length / 16; i++) {

			table += "0x" + Util.zeroLeftPad(Integer.toHexString((i * 16 + start)), 4) + "| ";

			for (int j = 0; j < 16; j++) {
				table += Util.zeroLeftPad(Integer.toHexString(getMem((char) ((start + i * 16 + j))) & 0xFF), 2) + " ";
			}
			table += "\n";
		}

		return table;
	}

}