/**
 * Breakpoint.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.debugger;

import com.echodrop.gameboy.core.Register;
import com.echodrop.gameboy.interfaces.IInternalByteValue;
import com.echodrop.gameboy.util.StringUtils;

/**
 * Represents a breakpoint for the Tailspin debugger
 */
public class Breakpoint {

	/**
	 * Set to true if the breakpoint will trigger based on an equality condition
	 */
	private boolean conditional;

	/**
	 * If the breakpoint is conditional, this is the InternalByteValue that it
	 * will check its target value against.
	 */
	private IInternalByteValue watched;

	/**
	 * The target value to compare watched against
	 */
	private byte targetValue;

	/**
	 * The memory address at which the breakpoint will be triggered
	 */
	private char address;

	public Breakpoint(boolean conditional, Register watched, byte targetValue, char address) {
		this.conditional = conditional;
		this.watched = watched;
		this.targetValue = targetValue;
		this.setAddress(address);
	}

	public Breakpoint() {
	}

	public boolean isConditional() {
		return conditional;
	}

	public void setConditional(boolean conditional) {
		this.conditional = conditional;
	}

	public IInternalByteValue getWatched() {
		return watched;
	}

	public void setWatched(IInternalByteValue watched) {
		this.watched = watched;
	}

	public byte getTargetValue() {
		return targetValue;
	}

	public void setTargetValue(byte targetValue) {
		this.targetValue = targetValue;
	}

	/**
	 * @return true if conditions are met for this breakpoint to trigger
	 */
	public boolean trigger(char pc) {
		if (getAddress() == pc) {
			if (isConditional()) {
				if (getWatched().getValue() == targetValue) {
					return true;
				}
			} else {
				return true;
			}
		}
		return false;
	}

	public char getAddress() {
		return address;
	}

	public void setAddress(char address) {
		this.address = address;
	}

	@Override
	public String toString() {
		String result = StringUtils.charToReadableHex(getAddress());
		if (isConditional()) {
			result += "\n";
			if(getWatched() instanceof Register) {
				result += "Register: " + ((Register) getWatched()).getName() + "\n";
			}
			result += "Target value: " + StringUtils.byteToReadableHex(getTargetValue());
		}
		return result;
	}

}
