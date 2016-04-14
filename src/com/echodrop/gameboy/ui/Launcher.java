package com.echodrop.gameboy.ui;

import java.util.logging.Level;

import javax.swing.UIManager;

import com.echodrop.gameboy.core.TailspinGB;
import com.echodrop.gameboy.logging.CliHandler;

public class Launcher {

	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					e.printStackTrace();
				}

				TailspinGB tgb = new TailspinGB();
				// TODO: Add debug window in the gui instead of command line
				// handler
				tgb.initLogging(Level.ALL, new CliHandler());
				DebuggerGUI dg = new DebuggerGUI(tgb);
				dg.setVisible(true);
			}
		});
	}

}
