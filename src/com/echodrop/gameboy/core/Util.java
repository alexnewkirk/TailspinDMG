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
		String hex1 = Integer.toHexString(b2 & 0xFF);
		
		if(hex1.length() < 2) {
			hex1 = "0" + hex1;
		}
		
		String hex2 = Integer.toHexString(b1 & 0xFF);
		
		if(hex2.length() < 2) {
			hex2 = "0" + hex2;
		}
		
		char result = (char)Integer.parseInt(hex1 + hex2, 16);
		return result;
	}
	
	//left circular bit shift
	//This method might be the worst piece of code I've ever written
	public static byte leftRotate(byte b) {
		String bin = Integer.toBinaryString(b & 0xFF);
		String shifted = bin.substring(1) + bin.charAt(0);
		return (byte)Integer.parseInt(shifted, 2);
	}

}
