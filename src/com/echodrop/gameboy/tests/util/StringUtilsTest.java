/**
 * StringUtilsTest.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.tests.util;

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
	
	@Test
	public void charToAssemblyLiteralTest() {
		String expected = "$feff";
		String actual = StringUtils.charToAssemblyLiteral((char)0xFFFE);
		System.out.println(actual);
		assertEquals(expected, actual);
	}

}
