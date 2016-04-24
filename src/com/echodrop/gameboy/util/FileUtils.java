/**
 * FileUtils.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileUtils {
	
	public static byte[] readBytes(String filename) throws IOException {
		return Files.readAllBytes(Paths.get(filename));
	}

}
