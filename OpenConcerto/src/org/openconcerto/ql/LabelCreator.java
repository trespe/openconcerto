/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.ql;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

public class LabelCreator {

	private final List<LabelLine> lines = new ArrayList<LabelLine>();
	private final List<LabelImage> images = new ArrayList<LabelImage>();
	private Font defaultFont = new Font("Serif", Font.PLAIN, 50);
	private int width;
	private int leftMargin;
	private int topMargin;

	public LabelCreator() {
		this(720);
	}

	public LabelCreator(int width) {
		this.width = width;
	}

	public void addLineNormal(String text) {
		this.lines.add(new LabelLine(text, this.defaultFont));
	}

	public void addLineBold(String text) {
		this.lines.add(new LabelLine(text, this.defaultFont.deriveFont(Font.BOLD)));
	}

	public void addLine(LabelLine line) {
		this.lines.add(line);
	}

	public void addImage(BufferedImage img, int x, int y) {
		this.images.add(new LabelImage(img, x, y));

	}

	public void setTopMargin(int topMargin) {
		this.topMargin = topMargin;
	}

	public void setLeftMargin(int leftMargin) {
		this.leftMargin = leftMargin;
		if (leftMargin > this.width) {
			throw new IllegalArgumentException("Left margin too big");
		}
	}

	public BufferedImage getImage() {
		BufferedImage im = new BufferedImage(this.width, this.width,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = im.createGraphics();

		int currentX = this.leftMargin;
		int currentY = this.topMargin;
		int maxHeight = currentY;

		// Images
		for (int i = 0; i < this.images.size(); i++) {
			final LabelImage labelImage = this.images.get(i);
			maxHeight = Math.max(maxHeight, labelImage.y
					+ labelImage.img.getHeight());
		}
		for (int i = 0; i < this.lines.size(); i++) {
			final LabelLine line = this.lines.get(i);
			g2.setFont(line.font);

			Rectangle2D rect = g2.getFontMetrics()
					.getStringBounds(line.txt, g2);

			int h = (int) rect.getHeight();
			int w = (int) rect.getWidth();

			while (currentX + w > this.width) {

				final float fSize = line.font.getSize2D() - 1;
				if (fSize < 3) {
					break;
				}
				line.font = line.font.deriveFont(fSize);
				g2.setFont(line.font);
				rect = g2.getFontMetrics().getStringBounds(line.txt, g2);
				h = (int) rect.getHeight();
				w = (int) rect.getWidth();

			}

			line.x = currentX;
			line.y = currentY + g2.getFontMetrics().getAscent();
			currentY += h;

		}

		g2.dispose();

		int finalHeight = currentY + this.topMargin;
		im = new BufferedImage(this.width, finalHeight, BufferedImage.TYPE_INT_ARGB);
		g2 = im.createGraphics();
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, this.width, finalHeight);
		g2.setColor(Color.BLACK);

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		for (int i = 0; i < this.images.size(); i++) {
			final LabelImage labelImage = this.images.get(i);
			g2.drawImage(labelImage.img, labelImage.x, labelImage.y, null);

		}

		for (int i = 0; i < this.lines.size(); i++) {
			final LabelLine line = this.lines.get(i);
			g2.setFont(line.font);
			g2.drawString(line.txt, line.x, line.y);

		}

		g2.dispose();
		return im;
	}

	public void setDefaultFont(Font defaultFont) {
		this.defaultFont = defaultFont;
	}

	public static void main(String[] args) {
		final LabelCreator c = new LabelCreator(720);
		c.setLeftMargin(10);
		c.setTopMargin(10);
		c.setDefaultFont(new Font("Verdana", Font.PLAIN, 50));

		c.addLineBold("ILM Informatique");
		c.addLineNormal("22 place de la libération");
		c.addLineNormal("80100 ABBEVILLE");

		try {
			ImageIO.write(c.getImage(), "PNG", new File("out.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		final QLPrinter prt = new QLPrinter("192.168.1.103");
		try {
			prt.print(c.getImage());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
