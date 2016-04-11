package com.echodrop.gameboy.debugger;

import com.echodrop.gameboy.core.Register;
import com.echodrop.gameboy.core.Util;
import com.echodrop.gameboy.interfaces.IInternalByteValue;

public class Breakpoint {

	private boolean conditional;
	private IInternalByteValue watched;
	private byte targetValue;
	private char address;
	
	public Breakpoint(boolean conditional, Register watched, byte targetValue, char address) {
		this.conditional = conditional;
		this.watched = watched;
		this.targetValue = targetValue;
		this.setAddress(address);
	}
	
	public Breakpoint() {}

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
	
	public boolean trigger(char pc) {
		if(getAddress() == pc) {
			
			if(isConditional()) {
				if(watched.getValue() == targetValue) {
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
		String result = Util.charToReadableHex(getAddress());
		if(isConditional()) {
			result += "\n";
			result += "Register: " + getWatched() + "\n";
			result += "Target value: " + Util.byteToReadableHex(getTargetValue());
		}
		return result;
	}

}
