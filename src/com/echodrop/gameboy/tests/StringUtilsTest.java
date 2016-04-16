package com.echodrop.gameboy.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.echodrop.gameboy.util.StringUtils;

public class StringUtilsTest {

	@Test
	public void reverseTest() {
		String expected = "xelA";
		String actual = StringUtils.reverse(expected);
		assertEquals(expected, actual);

		expected = "niposliaT";
		actual = StringUtils.reverse(expected);
		assertEquals(expected, actual);
	}

}
