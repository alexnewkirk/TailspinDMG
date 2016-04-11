/**
 * TailspinDebugger.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.main;

import processing.core.PApplet;

public class ProcessingDriver {
	
	public static void main(String[] args) {
		PApplet.main(new String[] { "--present", "com.echodrop.gameboy.main.ProcessingScreen" });
	}

}
