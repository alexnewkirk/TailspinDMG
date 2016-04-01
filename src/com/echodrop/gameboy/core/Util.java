package com.echodrop.gameboy.core;

/**
 * Contains miscellaneous utilities that are used throughout
 * the codebase.
 * 
 * @author echo_drop
 */
public class Util {
	
	public static byte[] wordToBytes(char word) {
//		String hex = Integer.toHexString(word & 0xFFFF);
//		System.out.println(hex);
//		byte b2 = (byte)Integer.parseInt(hex.substring(0, 2), 16);
//		byte b1 = (byte)Integer.parseInt(hex.substring(2), 16);
		
		byte b2 = (byte)(word >>> 8);
		byte b1 = (byte)(word & 0xFF);
		
		byte[] result = {b1, b2};
		return result;
	}
	
	public static char bytesToWord(byte b1, byte b2) {
		String hex = Integer.toHexString(b2 & 0xFF) + Integer.toHexString(b1 & 0xFF);
		char result = (char)Integer.parseInt(hex, 16);
		return result;
	}

}
