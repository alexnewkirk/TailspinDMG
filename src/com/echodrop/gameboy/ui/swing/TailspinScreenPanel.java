/**
 * TailspinScreenPanel.java
 * 
 * @author anewkirk
 * 
 * Licensing information can be found in the root directory of the project.
 */

package com.echodrop.gameboy.ui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import com.echodrop.gameboy.graphics.GPU;
import com.echodrop.gameboy.interfaces.IGraphicsObserver;

public class TailspinScreenPanel extends JPanel implements IGraphicsObserver {

	private static final long serialVersionUID = -7955256380603121144L;
	private GPU gpu;
	private byte[][] screen;
	private int pixelSize = 4;
	private boolean fpsDisplay = true;
	private long startMillis;
	private int frameCount = 0;

	public TailspinScreenPanel(GPU gpu) {
		this.gpu = gpu;
		gpu.registerObserver(this);
		screen = gpu.getFrameBuffer();
		this.setBackground(Color.WHITE);
		setPreferredSize(new Dimension(160 * pixelSize, 144 * pixelSize));
		updateDisplay();
		startMillis = System.currentTimeMillis();
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (screen != null) {
			for (int i = 0; i < 160; i++) {
				for (int j = 0; j < 144; j++) {
					Color c = null;
					byte color = screen[i][j];
					switch (color) {
					case 0:
						c = Color.WHITE;
						break;
					case 1:
						c = Color.LIGHT_GRAY;
						break;
					case 2:
						c = Color.DARK_GRAY;
						break;
					case 3:
						c = Color.BLACK;
						break;
					}

					g.setColor(c);
					g.fillRect(i * pixelSize, j * pixelSize, pixelSize, pixelSize);
				}
			}
		}
		if (frameCount > 0) {
			Graphics2D g2d = (Graphics2D) g;
			g2d.setColor(Color.RED);
			g2d.setFont(new Font("Arial", Font.BOLD, 10));
			g2d.drawString("FPS: " + frameCount / (System.currentTimeMillis() - startMillis), 30, 30);
		}

	}

	@Override
	public void updateDisplay() {
		this.screen = gpu.getFrameBuffer();
		this.repaint();
		frameCount++;
	}

	public void toggleFpsDisplay() {
		fpsDisplay = !fpsDisplay;
	}

}
