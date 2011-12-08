package org.jopenchart;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class SolidAreaRenderer extends AreaRenderer {
	private Color bgColor;

	public SolidAreaRenderer(Color color) {
		bgColor = color;
	}

	@Override
	public void render(Graphics2D g) {
		Rectangle r = g.getClipBounds();
		g.setColor(bgColor);
		g.fillRect(r.x, r.y, r.width, r.height);
	}

}
