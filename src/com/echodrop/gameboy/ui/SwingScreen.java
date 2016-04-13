package com.echodrop.gameboy.ui;

import javax.swing.JFrame;

import com.echodrop.gameboy.core.TailspinGB;

public class SwingScreen extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -662755352759934861L;

	private TailspinScreenPanel tsp;

	public SwingScreen(TailspinGB system) {
		tsp = new TailspinScreenPanel(system.getGpu());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		add(tsp);
		setLocationRelativeTo(null);
		setResizable(false);
		pack();
	}

}
