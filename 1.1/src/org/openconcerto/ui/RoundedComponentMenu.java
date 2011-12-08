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
 
 /*
 * Created on 2 sept. 2003
 *
 */
package org.openconcerto.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.Vector;

import javax.swing.JComponent;

/**
 * @author ilm
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class RoundedComponentMenu extends JComponent {
	private String name;
	private Vector items = new Vector();
	/**
	 * 
	 */
	public RoundedComponentMenu(String name) {
		super();
		this.name=name;
		
	}
	
	public void addMenuItem(RoundedMenuItem m){
		items.add(m);
	}
	

	/* (non-Javadoc)
	 * @see java.awt.Component#getPreferredSize()
	 */
	public Dimension getPreferredSize() {
		Dimension d=new Dimension(600,100*this.items.size());
		return d;
	}

	/* (non-Javadoc)
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 */
	public void paint(Graphics g) {
		// fond
		g.setColor(bgColor);
		int h=this.getHeight();
		int w=this.getWidth();
		g.fillRect(0,0,w,h);
		// decoupe blanches
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.setColor(fgColor);
		int m=(int)(1.8*h);
		g.fillOval(100,-m/3,m,m);
		// titre
		g.setColor(Color.BLACK);
		int fontSize=48;
		Font font=new Font("Arial",Font.BOLD,fontSize);
		g.setFont(font);
		g.drawString(name,50,30+fontSize);
		
		int d=60;
		int k=0;
		for (int y=-00;y<80*items.size();y+=80){
			float xx = m*m-4f*y*y;
			float x= -(int) ( (Math.sqrt(xx))/2 );
			System.out.println("xx:"+xx+"x"+x+"y:"+y);
			g.setColor(Color.WHITE);
			g.fillOval((int)x+m/2+100-(d*3)/5,y+m/6-d/2,d,d);
			g.setColor(Color.BLACK);			
			g.drawOval((int)x+m/2+100-(d*3)/5,y+m/6-d/2,d,d);
			g.setColor(Color.lightGray);
			String label="Sous memu";
			Rectangle2D rect = font.getStringBounds(label, ((Graphics2D) g).getFontRenderContext());
	
			g.drawString(((RoundedMenuItem)items.elementAt(k)).getName(),(int)(x+m/2+160),(int)(y+m/6-d/2+(rect.getHeight())/2));
			k++;
		}
	}
	
	//private Color bgColor=new Color(240,240,200);
	private Color bgColor=new Color(174,4,21);
	private Color fgColor=Color.white;
}
