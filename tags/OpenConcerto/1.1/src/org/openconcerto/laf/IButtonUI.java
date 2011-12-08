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
 * Created on 21 mai 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.openconcerto.laf;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalButtonUI;

public class IButtonUI  extends MetalButtonUI {

		public static final boolean HINT_DO_NOT_PAINT_TOOLBARBUTTON_IF_NO_MOUSE_OVER = true;
		/**
		 * The Cached UI delegate.
		 */
		private static final IButtonUI buttonUI = new IButtonUI();

		
		/** the stroke for the fcouse */
		static BasicStroke focusStroke=new BasicStroke(1.0f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,1.0f, new float[] {1.0f, 1.0f}, 1.0f);

		public IButtonUI() {
			
			//		timer=TimerFactory.getTimer();
		}

		protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect) {
			Graphics2D g2d=(Graphics2D)g;
			Rectangle focusRect = b.getBounds();

			
			/*g.setColor(Color.black);
			g2d.setStroke(focusStroke);

			g2d.drawLine(2 , 			 	 2, 		       2 + focusRect.width - 5, 2);
			g2d.drawLine(2, 2+focusRect.height - 5, 2 + focusRect.width - 5, 2+focusRect.height - 5);
			g2d.drawLine(2 , 			 	 2, 		       2 , 2+focusRect.height - 5);
			g2d.drawLine(2 + focusRect.width - 5, 			 	 2, 		       2+ focusRect.width - 5 , 2+focusRect.height - 5);
*/
		}

		/**
		 * Creates the UI delegate for the given component.
		 *
		 * @param c The component to create its UI delegate.
		 * @return The UI delegate for the given component.
		 */
		public static ComponentUI createUI(final JComponent c) {
			if (c instanceof JButton) {
				JButton b = (JButton) c;
				b.setRolloverEnabled(true);
			} else if (c instanceof JToggleButton) {
	                JToggleButton b = (JToggleButton) c;
	                b.setRolloverEnabled(true);
	        }

//	     If we used an transparent toolbutton skin we would have to add:       
			c.setOpaque(false);
	        c.addPropertyChangeListener("opaque",new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
	                c.setOpaque(false);
				}
			});
			return buttonUI;
		}

	    /**
	     * We don't want to paint the pressed state here - the skin does it for us. 
	     * @see javax.swing.plaf.basic.BasicButtonUI#paintButtonPressed(Graphics, AbstractButton)
	     */
		protected void paintButtonPressed(Graphics g, AbstractButton b) {
		}

		private static Color bordure=new Color(140,140,121);
		
		
		public void paint(Graphics g, JComponent c) {

			AbstractButton button = (AbstractButton) c;

			Rectangle rect=button.getBounds();
			rect.setLocation(1,1);
			rect.height-=3;
			rect.width-=3;
			
			
			//g.setColor(Color.RED);
			//g.fillRect(rect.x,rect.y,rect.width,rect.height);
			/*g.setColor(bordure);
			g.drawLine(rect.x+1,rect.y,rect.x+rect.width-1,rect.y);
			g.drawLine(rect.x+1,rect.y+rect.height,rect.x+rect.width-1,rect.y+rect.height);
			g.drawLine(rect.x,rect.y+1,rect.x,rect.y+rect.height-1);
			g.drawLine(rect.x+rect.width,rect.y+1,rect.x+rect.width,rect.y+rect.height-1);
			rect.grow(-1,-1);*/
			g.setColor(new Color(254,254,254));
			
			g.fillRect(rect.x,rect.y,rect.width,rect.height);
			
			if (button.getClientProperty("JToolBar.isToolbarButton") == Boolean.TRUE) {
	            //    toolbarIndexModel.setButton(button);
	              //  int index=toolbarIndexModel.getIndexForState();
				//	getSkinToolbar().draw(g, index, button.getWidth(),	button.getHeight());
//				}
			} else {
				
			//	buttonIndexModel.setButton(button);
	          //  buttonIndexModel.setCheckForDefaultButton(button instanceof JButton);
			//	int index=buttonIndexModel.getIndexForState();
			//	getSkinButton().draw(g,	index,	button.getWidth(),	button.getHeight());
			}
			
			
			super.paint(g, c);

			//		double el=timer.stop();
			//		System.out.println("Painting took "+el+" [msec] ");
		}
		
	
	    
	    public void update(Graphics g, JComponent c) {
	        paint(g, c);
	    }

		/* (non-Javadoc)
		 * @see javax.swing.plaf.basic.BasicButtonUI#getMinimumSize(javax.swing.JComponent)
		 */
		public Dimension getMinimumSize(JComponent c) {
			// TODO Auto-generated method stub
			return super.getMinimumSize(c);
		}

		/* (non-Javadoc)
		 * @see javax.swing.plaf.basic.BasicButtonUI#getPreferredSize(javax.swing.JComponent)
		 */
		public Dimension getPreferredSize(JComponent c) {
			 AbstractButton b = (AbstractButton)c;
			if( b.getIcon()!=null)
				return new Dimension(super.getPreferredSize(c).width,super.getPreferredSize(c).height);
			else{
				return new Dimension(super.getPreferredSize(c).width,super.getPreferredSize(c).height-6);
				
				
			}
		}
		

		/* (non-Javadoc)
		 * @see javax.swing.plaf.metal.MetalButtonUI#paintText(java.awt.Graphics, javax.swing.JComponent, java.awt.Rectangle, java.lang.String)
		 */
		protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text) {
			
			g.setFont(new Font("Tahoma",Font.PLAIN,10));
			textRect.translate(0,1);
			super.paintText(g, c, textRect, text);
		}
	}

