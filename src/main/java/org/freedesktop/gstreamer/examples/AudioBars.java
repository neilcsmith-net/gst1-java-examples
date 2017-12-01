package org.freedesktop.gstreamer.examples;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

class AudioBars extends JPanel {
	private static final int OFFSET = 10;
	private static final int BAR_WIDTH = OFFSET - 2;
	private static final int CANVAS_HEIGHT = 288;
	private static final int CANVAS_WIDTH = OFFSET * 8; // Assume less than 8 channels of audio

//	private static final int VOLUME_OFFSET = 60;
//	private static final int VOLUME_TO_LINE_LENGTH_MULTIPLIER = 2;

	private List<Rectangle> bars = new ArrayList<Rectangle>(0);

	public void setCurrentLevels(List<Double> levels) {
		List<Rectangle> newBars = new ArrayList<>(levels.size());
		for (int i = 0; i < levels.size(); i++) {
			double channelLevel = levels.get(i);
//			int barHeight = (int) ((channelLevel + VOLUME_OFFSET) * VOLUME_TO_LINE_LENGTH_MULTIPLIER);
            int barHeight = (int) (channelLevel * CANVAS_HEIGHT);
			int topOfBar = CANVAS_HEIGHT - barHeight;
			Rectangle rect = new Rectangle(OFFSET * i, topOfBar, BAR_WIDTH, barHeight);
			newBars.add(rect);
		}
		bars = newBars;
        repaint();
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;

		for (Rectangle rect : bars) {
			if (rect.getHeight() > 80) {
				g2.setColor(Color.RED);
			} else if (rect.getHeight() > 50) {
				g2.setColor(Color.YELLOW);
			} else {
				g2.setColor(Color.GREEN);
			}
			g2.fill(rect);
		}
	}
}