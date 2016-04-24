/**
 * DebugAction.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.debugger;

/**
 * Commands available for the Tailspin debugger
 */
public enum DebugAction {
	
	SETBRK,
	STEP,
	MEMDMP,
	CONTINUE,
	REGDMP,
	EXIT,
	LSBRK,
	RESET,
	HELP,
	NOLOG,
	LOGINFO,
	LOGALL,
	LOADROM,
	FRAMEDMP,
	CONDBRK,
	CLRBRK,
	VTILEDMP,
	TILEDMP,
	VIDEO,
	TILEWRITETEST,
	RENDER,
	LOADBIOS;
}
