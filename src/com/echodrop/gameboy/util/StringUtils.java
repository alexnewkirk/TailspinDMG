package com.echodrop.gameboy.util;

public class StringUtils {
	
	/**
	 * Converts a byte value to a human-readable hexadecimal representation
	 */
	public static String byteToReadableHex(byte b) {
		return "0x" + zeroLeftPad(Integer.toHexString(b & 0xFF).toUpperCase(), 2);
	}

	/**
	 * Converts a char value to a human-readable hexadecimal representation
	 */
	public static String charToReadableHex(char c) {
		return "0x" + zeroLeftPad(Integer.toHexString(c & 0xFFFF).toUpperCase(), 4);
	}

	/**
	 * Left-pads a string with zeros until it is of length size
	 */
	public static String zeroLeftPad(String s, int size) {
		String result = s;
		while (result.length() < size) {
			result = "0" + result;
		}
		return result;
	}
	
	/**
	 * Reverses a string
	 */
	public static String reverse(String s) {
		String reversed = "";
		for (int i = s.length() - 1; i >= 0; i--) {
			reversed = s.charAt(i) + reversed;
		}
		return reversed;
	}

}
