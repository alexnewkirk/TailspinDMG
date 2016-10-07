package com.echodrop.gameboy.util;

import com.echodrop.gameboy.core.MMU;

public class GraphicsUtils {

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
	
}
