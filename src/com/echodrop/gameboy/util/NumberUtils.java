/**
 * Util.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.util;

import com.echodrop.gameboy.core.MMU;
import com.echodrop.gameboy.core.Register;

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
	}

	/**
	 * Left circular bit shift
	 */
	public static byte leftRotate(byte b) {
		String bin = StringUtils.zeroLeftPad(Integer.toBinaryString(b & 0xFF), 8);
		String shifted = bin.substring(1) + bin.charAt(0);
		return (byte) Integer.parseInt(shifted, 2);
	}

	/**
	 * Rotates a register left, through the carry flag
	 * 
	 * @param toRotate
	 *            register to be rotated
	 * @param carryFlag
	 *            current state of carry flag
	 * @return new state of carry flag
	 */
	public static boolean leftRotateThroughCarry(Register toRotate, boolean carryFlag) {
		String bin = StringUtils.zeroLeftPad(Integer.toBinaryString(toRotate.getValue() & 0xFF), 8)
				+ (carryFlag ? '1' : '0');
		String shifted = bin.substring(1) + bin.charAt(0);
		toRotate.setValue(Integer.parseInt(shifted.substring(0, 8), 2));
		return shifted.charAt(8) == '1';
	}

	/**
	 * Reads the value of a specific bit from data
	 * 
	 * @return true if the specified bit is 1
	 */
	public static boolean readBit(int bit, byte data) {
		// XXX needs error checking
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.zeroLeftPad(Integer.toBinaryString(data & 0xFF), 8));
		// sb = sb.reverse();
		return sb.charAt(bit) == '1';
	}

	/**
	 * Maps a row of pixels from a tile, to be rendered to the screen.
	 * 
	 * @param palette
	 *            the value of a background color palette register
	 * @param b1
	 *            the first byte of the row to be mapped
	 * @param b2
	 *            the second byte of a row to be mapped
	 * @return an array of length 8 containing mapped pixels
	 */
	public static byte[] mapRow(byte palette, byte b1, byte b2) {
		byte[] row = new byte[8];

		String bin1 = Integer.toBinaryString(b1 & 0xFF);
		bin1 = StringUtils.zeroLeftPad(bin1, 8);

		String bin2 = Integer.toBinaryString(b2 & 0xFF);
		bin2 = StringUtils.zeroLeftPad(bin2, 8);

		for (int i = 0; i < 8; i++) {
			byte color = (byte) (Integer.parseUnsignedInt("" + bin2.charAt(i) + bin1.charAt(i), 2) & 0xFF);
			row[i] = color;// (byte)(Integer.parseInt(paletteSb.charAt(color) &
							// 0xFF;
			// XXX this isnt being mapped through the palette
		}
		return row;
	}

	/**
	 * Retrieves specified tile from memory
	 */
	public static byte[] getTile(MMU mem, boolean tileset, int tileNumber) {
		char memOffset = (char) (tileset ? 0x8000 : 0x9000);
		byte[] tile = new byte[16];
		for (int i = 0; i < 16; i++) {
			tile[i] = mem.readByte((char) (memOffset + (tileNumber * 16) + i));
		}
		return tile;
	}

	/**
	 * Maps an entire 8x8 tile and returns a 2 dimensional array of size 8x8
	 * 
	 * @param palette
	 *            the palette register value for the colors to be mapped through
	 * @param tileData
	 *            a byte array of length 16, containing 1 entire tile
	 * @return
	 */
	public static byte[][] mapTile(byte palette, byte[] tileData) {
		byte[][] tile = new byte[8][8];
		byte firstHalf;
		byte secondHalf;

		int xCount = 0;
		for (int i = 0; i < 16; i += 2) {
			firstHalf = tileData[i];
			secondHalf = tileData[i + 1];
			byte[] row = mapRow(palette, secondHalf, firstHalf);

			for (int j = 0; j < 8; j++) {
				tile[xCount][j] = row[j];
			}
			xCount++;
		}
		return tile;
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
