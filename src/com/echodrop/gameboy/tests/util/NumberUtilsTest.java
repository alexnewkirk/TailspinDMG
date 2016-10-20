/**
 * NumberUtilsTest.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.tests.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.echodrop.gameboy.util.NumberUtils;

public class NumberUtilsTest {
	
	@Test
	public void ByteAdditionOverflowTest() {
		assertTrue(NumberUtils.byteAdditionOverflow((byte) 255, (byte)1));
		assertTrue(NumberUtils.byteAdditionOverflow((byte) -20, (byte)-40));
		
		assertFalse(NumberUtils.byteAdditionOverflow((byte) 250, (byte) 5));
	}
	
	@Test
	public void ByteAdditionNibbleOverflowTest() {
		
	}
	
	@Test
	public void BytesToWordTest() {
		
	}
	
	@Test
	public void WordToBytesTest() {
//		byte[] b = NumberUtils.wordToBytes((char)0xFFFE);
//		assertEquals(b[1], 0xFF);
//		assertEquals(b[0], 0xFE);
	}

}
