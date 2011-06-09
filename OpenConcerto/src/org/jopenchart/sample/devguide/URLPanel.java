package org.jopenchart.sample.devguide;

import java.awt.Graphics;
import java.awt.Image;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class URLPanel extends JPanel {

	Image im;

	URLPanel(final String surl) {
		new Thread(new Runnable() {
			public void run() {

				try {

					URL url = new URL(surl);
					ImageIcon i = new ImageIcon(url);
					im = i.getImage();
					System.out.println("loaded:" + url);
					repaint();

				} catch (Exception e) {

					e.printStackTrace();
				}

			}
		}).start();
	}

	protected void paintComponent(Graphics g) {

		if (im != null && im.getHeight(null) > 0) {
			System.out.println("paint:"+im.getWidth(null)+","+im.getWidth(null));
			System.out.println(this.getSize());
			g.drawImage(im, 0, 0, null);
		}
	}

}
