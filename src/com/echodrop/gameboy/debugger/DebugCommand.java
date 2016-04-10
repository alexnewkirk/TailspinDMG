package com.echodrop.gameboy.debugger;

public class DebugCommand {
	
	private DebugCommandType command;
	private Character arg;
	
	public DebugCommand(DebugCommandType command, Character arg) {
		this.setCommand(command);
		this.setArg(arg);
	}
	
	public DebugCommand(DebugCommandType command) {
		this.setCommand(command);
	}

	public DebugCommandType getCommand() {
		return command;
	}

	public void setCommand(DebugCommandType command) {
		this.command = command;
	}

	public Character getArg() {
		return arg;
	}

	public void setArg(Character arg) {
		this.arg = arg;
	}
	
	@Override
	public String toString() {
		if(arg != null) {
			return command.toString() + " 0x" + Integer.toHexString(arg & 0xFFFF).toUpperCase();
		}
		return command.toString();
	}

}
