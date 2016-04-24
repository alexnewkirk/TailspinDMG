/**
 * NumberUtilsTest.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import com.echodrop.gameboy.util.NumberUtils;

public class NumberUtilsTest {

	@Test
	public void readBitTest() {
		
		byte input = (byte)0x91;
		assertTrue(NumberUtils.readBit(3, input));
		assertTrue(NumberUtils.readBit(0, input));
		assertTrue(!NumberUtils.readBit(1, input));
		assertTrue(!NumberUtils.readBit(2, input));
		assertTrue(!NumberUtils.readBit(4, input));
		assertTrue(!NumberUtils.readBit(5, input));
		assertTrue(!NumberUtils.readBit(6, input));
		assertTrue(NumberUtils.readBit(7, input));
		
	}
	
	@Test
	public void ByteAdditionOverflowTest() {
		assertTrue(NumberUtils.byteAdditionOverflow((byte) 255, (byte)1));
		assertTrue(NumberUtils.byteAdditionOverflow((byte) -20, (byte)-40));
		
		assertFalse(NumberUtils.byteAdditionOverflow((byte) 250, (byte) 5));
	}
	
	@Test
	public void ByteAdditionNibbleOverflowTest() {
		
	}

}
