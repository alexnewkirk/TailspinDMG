/**
 * StringUtils.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

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
	
	public static String charToAssemblyLiteral(char c) {
		byte[] bytes = NumberUtils.wordToBytes(c);
		String b1 = zeroLeftPad(Integer.toHexString(bytes[0] & 0xFF), 2);
		String b2 = zeroLeftPad(Integer.toHexString(bytes[1] & 0xFF), 2);
		return "$" + b1 + b2;
	}

	/**
	 * Left-pads a string with zeros until it is of length size
	 */
	public static String zeroLeftPad(String s, int size) {
		String result = s;
		String zero = "";
		for(int i = result.length(); i < size; i++) {
			zero += '0';
		}
		return zero + result;
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
