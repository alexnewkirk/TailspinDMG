/**
 * NumberUtils.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.util;

/**
 * Contains miscellaneous utilities that are used throughout the codebase.
 */
public class NumberUtils {

	/**
	 * Splits a 16 bit value into two bytes
	 */
	public static byte[] wordToBytes(char word) {
		byte b2 = (byte) (word >>> 8);
		byte b1 = (byte) (word & 0xFF);
		byte[] result = { b1, b2 };
		return result;
	}

	/**
	 * Combines two bytes into a word
	 */
	public static char bytesToWord(byte b1, byte b2) {
		String hex1 = Integer.toHexString(b2 & 0xFF);

		if (hex1.length() < 2) {
			hex1 = "0" + hex1;
		}
		String hex2 = Integer.toHexString(b1 & 0xFF);

		if (hex2.length() < 2) {
			hex2 = "0" + hex2;
		}
		char result = (char) Integer.parseInt(hex1 + hex2, 16);
		return result;

//		return (char)((b2 * 256) | b1);
	}

	public static boolean byteAdditionOverflow(byte b1, byte b2) {
		int result = Byte.toUnsignedInt(b1) + Byte.toUnsignedInt(b2);
		return result > 255;
	}

	public static boolean byteAdditionNibbleOverflow(byte b1, byte b2) {
		int result = Byte.toUnsignedInt((byte) (b1 & 0x7)) + Byte.toUnsignedInt((byte) (b2 & 0x7));
		return result > 0x7;
	}
	
	public static boolean byteSubtractionBorrow(byte b1, byte b2) {
		int b = Byte.toUnsignedInt(b1) >> 7;
		if(b > 0) {
			int r = Byte.toUnsignedInt(b1) & 0x7F;
			if(Byte.toUnsignedInt(b2) > r) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean byteSubtractionNibbleBorrow(byte b1, byte b2) {
		int b = (Byte.toUnsignedInt(b1) >> 3) & 1;
		if(b > 0) {
			int r = Byte.toUnsignedInt(b1) & 7;
			if(Byte.toUnsignedInt(b2) > r) {
				return false;
			}
		}
		return true;
	}

}
