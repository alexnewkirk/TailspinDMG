package com.echodrop.gameboy.tests.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.echodrop.gameboy.core.Register;
import com.echodrop.gameboy.util.RegisterUtils;

public class RegisterUtilsTests {

	@Test
	public void readBitTest() {
		
		byte input = (byte)0x91;
		Register r = new Register(input, "test_input");
		assertTrue(RegisterUtils.readBit(3, r));
		assertTrue(RegisterUtils.readBit(0, r));
		assertTrue(!RegisterUtils.readBit(1, r));
		assertTrue(!RegisterUtils.readBit(2, r));
		assertTrue(!RegisterUtils.readBit(4, r));
		assertTrue(!RegisterUtils.readBit(5, r));
		assertTrue(!RegisterUtils.readBit(6, r));
		assertTrue(RegisterUtils.readBit(7, r));
		
	}

}
