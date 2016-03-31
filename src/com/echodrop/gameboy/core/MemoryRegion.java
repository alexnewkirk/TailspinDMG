package com.echodrop.gameboy.core;

public class MemoryRegion {
	
	char start;
	char end;
	public int size; //in bytes
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
//		System.out.print("Writing " + Integer.toHexString(content & 0xFF) + " to ");
//		System.out.println(Integer.toHexString(addr) + " in " + name);
//		System.out.println("Size of " + name + " is: " + size);
		contents[addr - start] = content;
	}
	
}