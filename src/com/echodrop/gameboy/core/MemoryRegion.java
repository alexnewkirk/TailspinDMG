package com.echodrop.gameboy.core;

public class MemoryRegion {
	
	char start;
	char end;
	public int size; //in bytes
	byte[] contents;
	
	public MemoryRegion(char start, char end) {
		this.start = start;
		this.end = end;
		this.size = end - start;
		this.contents = new byte[size];
	}
	
	public byte getMem(char addr) {
		return contents[addr - start];
	}
	
	public void setMem(char addr, byte content) {
		contents[addr - start] = content;
	}
	
}