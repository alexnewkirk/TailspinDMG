/**
 * Util.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.core;

/**
 * Contains miscellaneous utilities that are used throughout the codebase.
 */
public class Util {

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
	}

	/**
	 * Left circular bit shift
	 */
	public static byte leftRotate(byte b) {
		String bin = Integer.toBinaryString(b & 0xFF);
		String shifted = bin.substring(1) + bin.charAt(0);
		return (byte) Integer.parseInt(shifted, 2);
	}

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
	
	public static boolean readBit(int bit, byte data) {
		//XXX needs error checking
		StringBuilder sb = new StringBuilder();
		sb.append(zeroLeftPad(Integer.toBinaryString(data & 0xFF), 8));
		//sb = sb.reverse();
		return sb.charAt(bit) == '1';
	}
	
	public static byte[] mapRow(byte palette, byte b1, byte b2) {
		
		StringBuilder paletteSb = new StringBuilder();
		byte[] row = new byte[8];
		String bin1 = zeroLeftPad(Integer.toBinaryString(b1 & 0xFF), 8);
		String bin2 = zeroLeftPad(Integer.toBinaryString(b2 & 0xFF), 8);
		paletteSb.append(zeroLeftPad(Integer.toBinaryString(palette & 0xFF), 8));
		
		for(int i = 0; i < 8; i++) {
			byte color = (byte)(Integer.parseInt("" + bin2.charAt(i) + bin1.charAt(i), 2) & 0xFF);
			row[i] = color;//(byte)(Integer.parseInt(paletteSb.charAt(color) & 0xFF;
			//XXX this isnt being mapped through the palette
		}
		
		return row;
	}
	
	/**
	 * Retrieves specified tile from memory
	 */
	public static byte[] getTile(MMU mem, boolean tileset, int tileNumber) {
		char memOffset = (char)(tileset ? 0x8000 : 0x9000);
		byte[] tile = new byte[16];
		for(int i = 0; i < 16; i++) {		
			tile[i] = mem.readByte((char)(memOffset + (tileNumber * 16) + i));
		}
		return tile;
	}
	
	public static byte[][] mapTile(byte palette, byte[] tileData) {
		byte[][] tile = new byte[8][8];
		byte firstHalf;
		byte secondHalf;
		
		int xCount = 0;
		for(int i = 0; i < 16; i += 2) {
			firstHalf = tileData[i];
			secondHalf = tileData[i+1];
			byte[] row = mapRow(palette, secondHalf, firstHalf);
			
			for(int j = 0; j < 8; j++) {
				tile[xCount][j] = row[j];
			}
			xCount++;
		}

		return tile;
	}


}
