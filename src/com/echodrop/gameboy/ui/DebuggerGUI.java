package com.echodrop.gameboy.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import com.echodrop.gameboy.core.TailspinGB;

public class DebuggerGUI extends SwingScreen {

	private static final long serialVersionUID = 1231747124160210873L;
	private static final JFileChooser fc = new JFileChooser();
	private TailspinGB system;

	public DebuggerGUI(TailspinGB system) {
		super(system);
		this.system = system;
		initUI();
		
		fc.setCurrentDirectory(new File("."));
	}

	private void initUI() {
		buildMenuBar();
	}

	private void buildMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu debugMenu = new JMenu("Debug");
		JMenu fileMenu = new JMenu("File");
		JMenu emuMenu = new JMenu("Emulator");
		JMenu viewMenu = new JMenu("View");

		JMenuItem loadBios = new JMenuItem("Load DMG ROM");
		loadBios.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				int success = fc.showOpenDialog(null);
				if(success == JFileChooser.APPROVE_OPTION) {
					system.getMem().loadBios(fc.getSelectedFile().getPath());
				}
			}
		});

		emuMenu.add(loadBios);

		menuBar.add(fileMenu);
		menuBar.add(emuMenu);
		menuBar.add(viewMenu);
		menuBar.add(debugMenu);

		setJMenuBar(menuBar);
	}

}
