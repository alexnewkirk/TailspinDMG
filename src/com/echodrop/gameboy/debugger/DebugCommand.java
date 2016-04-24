/**
 * DebugCommand.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.debugger;

/**
 * Represents a command for the Tailspin debugger to execute.
 */
public class DebugCommand {

	private DebugAction command;
	private Character arg;

	public DebugCommand(DebugAction command, Character arg) {
		this.setCommand(command);
		this.setArg(arg);
	}

	public DebugCommand(DebugAction command) {
		this.setCommand(command);
	}

	public DebugAction getCommand() {
		return command;
	}

	public void setCommand(DebugAction command) {
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
		if (arg != null) {
			return command.toString() + " 0x" + Integer.toHexString(arg & 0xFFFF).toUpperCase();
		}
		return command.toString();
	}

}
