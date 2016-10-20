package com.echodrop.gameboy.util;

import com.echodrop.gameboy.core.MMU;

public class GraphicsUtils {

	/**
	 * Maps a row of pixels from a tile, to be rendered to the screen.
	 * 
	 * @param palette current palette register
	 * @param b1 first byte of the row
	 * @param b2 second byte of the row
	 * @return an array of length 8 containing mapped pixels
	 */
	public static byte[] mapRow(byte palette, byte b1, byte b2) {
		byte[] row = new byte[8];

		for (int i = 0; i < 8; i++) {
			row[7 - i] = (byte) (((b2 & 1 << i) >> i) << 1 | ((b1 & 1 << i) >> i));
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
