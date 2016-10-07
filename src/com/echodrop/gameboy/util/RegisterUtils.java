package com.echodrop.gameboy.util;

import com.echodrop.gameboy.core.Register;

public class RegisterUtils {
	
	/**
	 * Reads the value of a specific bit from data
	 * 
	 * @return true if the specified bit is 1
	 */
	public static boolean readBit(int bit, Register r) {
		String bin = StringUtils.zeroLeftPad(Integer.toBinaryString(r.getValue() & 0xFF), 8);
		return bin.charAt(bit) == '1';
	}
	
	public static byte resetBit(int bit, Register r) {
		String bin = StringUtils.zeroLeftPad(Integer.toBinaryString(r.getValue() & 0xFF), 8);
		return (byte) Integer.parseInt('0' + bin.substring(1), 2);
	
	}
	
	/**
	 * Left circular bit shift
	 */
	public static byte leftRotate(Register r) {
		String bin = StringUtils.zeroLeftPad(Integer.toBinaryString(r.getValue() & 0xFF), 8);
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

}
