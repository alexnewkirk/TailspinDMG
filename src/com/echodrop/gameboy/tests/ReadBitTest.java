package com.echodrop.gameboy.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import com.echodrop.gameboy.core.Util;

public class ReadBitTest {

	@Test
	public void test() {
		
		byte input = (byte)0x91;
		assertTrue(Util.readBit(3, input));
		
		assertTrue(Util.readBit(0, input));
		
		assertTrue(!Util.readBit(1, input));
		
		assertTrue(!Util.readBit(2, input));
		assertTrue(!Util.readBit(4, input));
		assertTrue(!Util.readBit(5, input));
		assertTrue(!Util.readBit(6, input));
		assertTrue(Util.readBit(7, input));
		
	}

}
