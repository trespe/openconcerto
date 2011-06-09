package org.jopenchart.sample.devguide;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;

import org.jopenchart.Chart;
import org.jopenchart.ChartPanel;
import org.jopenchart.URLParser;


public class GuidePanel extends JPanel {
	GuidePanel() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

	}

	public void addURL(String url) {
		JPanel panel = createPanel(url);
		this.add(panel);

	}

	private JPanel createPanel(String url) {
		JPanel p = new JPanel();
		p.setBackground(Color.WHITE);
		p.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 2, 2, 2);
		// Line 1
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;
		JTextArea textAreaUrl = new JTextArea("Google API URL: " + url.replaceAll("&", "\n&"));
		textAreaUrl.setLineWrap(true);
		p.add(textAreaUrl, c);
		// Line 2
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.gridheight = 2;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		JTextArea textCode = new JTextArea();
		textCode.setMinimumSize(new Dimension(300,300));
		textCode.setPreferredSize(new Dimension(300,300));
		textCode.setText("Source Code");
		p.add(new JScrollPane(textCode), c);

		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		c.gridx++;
		c.gridheight = 1;
		URLParser parser = new URLParser(url);
		Chart chart = parser.getChart();
		if (chart != null) {
			ChartPanel panel = new ChartPanel(chart);
			
			p.add(panel, c);
		
		c.gridy++;
		URLPanel uPanle = new URLPanel(url);
		
		uPanle.setPreferredSize(chart.getDimension());
		uPanle.setMinimumSize(chart.getDimension());
		System.out.println(uPanle.getSize());
		p.add(uPanle, c);
		}
		return p;
	}

	public void addTitle(String string) {
		JLabel label = new JLabel(string);
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		this.add(label);

	}

}
