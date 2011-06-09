package org.jopenchart.sample.devguide;

import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class GuideFrame extends JFrame {
	public GuideFrame() {
		super("Developer's Guide");
		Container pane = this.getContentPane();
		JTabbedPane tabbedPane = new JTabbedPane();
		pane.add(tabbedPane);
		final GuidePanel guidePanel1 = new GuidePanel();
		guidePanel1.addURL("http://chart.apis.google.com/chart?cht=lc&chs=200x125&chd=s:helloWorld&chxt=x,y&chxl=0:|Mar|Apr|May|June|July|1:||50+Kb");
		guidePanel1.addURL("http://chart.apis.google.com/chart?cht=lc&chs=200x100&chd=s:fohmnytenefohmnytene&chxt=x,y&chxl=0:|Apr|May|June|1:||50+Kb");
		tabbedPane.add("Introduction", new JScrollPane(guidePanel1));

		final GuidePanel guidePanel2 = new GuidePanel();

		guidePanel2.addTitle("Line charts");

		guidePanel2.addURL("http://chart.apis.google.com/chart?cht=lc&chs=200x125&chd=s:fooZaroo");
		guidePanel2
				.addURL("http://chart.apis.google.com/chart?cht=lxy&chs=200x125&chd=t:0,30,60,70,90,95,100|20,30,40,50,60,70,80|10,30,40,45,52|100,90,40,20,10|-1|5,33,50,55,7&chco=3072F3,ff0000,00aaaa&chls=2,4,1&chm=s,FF0000,0,-1,5|s,0000ff,1,-1,5|s,00aa00,2,-1,5");

		guidePanel2.addTitle("Bar charts");

		guidePanel2.addURL("http://chart.apis.google.com/chart?cht=bhs&chs=200x125&chd=s:HELL,WORL&chco=ff0000,00aa00");
		guidePanel2.addURL("http://chart.apis.google.com/chart?cht=bvs&chs=200x125&chd=s:hello,world&chco=cc0000,00aa00&chbh=20");
		guidePanel2.addURL("http://chart.apis.google.com/chart?cht=bhg&chs=200x125&chd=s:el,or&chco=cc0000,00aa00");
		guidePanel2.addURL("http://chart.apis.google.com/chart?cht=bvg&chs=200x125&chd=s:hello,world&chco=cc0000,00aa00");
		guidePanel2.addURL("http://chart.apis.google.com/chart?cht=bhs&chs=200x125&chd=s:hello");
		guidePanel2.addURL("http://chart.apis.google.com/chart?cht=bhs&chs=200x125&chd=s:hello&chbh=10");

		guidePanel2.addTitle("Pie charts");

		guidePanel2.addURL("http://chart.apis.google.com/chart?cht=p&chd=s:world5&chs=200x125&chl=A|B|C|D|E|F");
		guidePanel2.addURL("http://chart.apis.google.com/chart?cht=p3&chd=s:Uf9a&chs=200x100&chl=A|B|C|D");

		guidePanel2.addTitle("Venn diagrams");

		guidePanel2.addURL("http://chart.apis.google.com/chart?cht=v&chs=200x100&chd=t:100,80,60,30,30,30,10");

		guidePanel2.addTitle("Scatter plots");

		guidePanel2
				.addURL("http://chart.apis.google.com/chart?cht=s&chd=s:984sttvuvkQIBLKNCAIi,DEJPgq0uov17zwopQODS,AFLPTXaflptx159gsDrn&chxt=x,y&chxl=0:|0|2|3|4|5|6|7|8|9|10|1:|0|25|50|75|100&chs=200x125");
		JScrollPane scrollPane = new JScrollPane(guidePanel2);
		scrollPane.setBorder(null);
		tabbedPane.add("Charts type", scrollPane);

	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		GuideFrame f = new GuideFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(new Dimension(1024, 768));
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}
}
