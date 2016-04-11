/**
 * DebugCommandType.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.debugger;

/**
 * Command types available for the Tailspin debugger
 */
public enum DebugCommandType {
	
	SETBRK,
	STEP,
	MEMDMP,
	CONTINUE,
	REGDMP,
	EXIT,
	LSBRK,
	RESET,
	HELP,
	LOGNONE,
	LOGINFO,
	LOGALL,
	LOADROM,
	FRAMEDMP,
	CONDBRK,
	CLRBRK;

}
