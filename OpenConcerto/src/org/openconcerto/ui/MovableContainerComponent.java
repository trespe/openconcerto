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
 * MovableContainerComponent created on 23 sept. 2004
 * 
 */
package org.openconcerto.ui;

import org.openconcerto.utils.JImage;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

/**
 * @author ILM Informatique 23 sept. 2004
 *
 */
public class MovableContainerComponent extends JComponent implements MouseListener, MouseMotionListener {

	private int size;
	private int offsetX;
	int currentIndex;
	private Vector list = new Vector();
	private JComponent draggedComponent = null;
	private int draggedComponentIndex;
	private Image componentImage;
	private JComponent imgComp;
	private Point pOffset;
	private JLayeredPane layerPane;
	public MovableContainerComponent(int size) {
		this.size = size;
		this.setLayout(new GridLayout(1, size));
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	/* (non-Javadoc)
	 * @see java.awt.Container#add(java.awt.Component)
	 */
	public Component add(Component comp) {
		list.add(comp);
		comp.addMouseListener(this);
		comp.addMouseMotionListener(this);
		return super.add(comp);
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e) {
		if(e.getY()<16)
		for (int i = 0; i < list.size(); i++) {
			JComponent obj = (JComponent) list.elementAt(i);
			if (obj == e.getSource()) {
				System.out.println("Le " + i + " eme est dragge");
				this.draggedComponent = (JComponent) obj;
				this.draggedComponentIndex = i;
				this.currentIndex = i;
				// La frame 
				//JComponent frame = SwingUtilities.getRoot(obj);
				//this.remove(obj);
				//glassPane = ((JFrame) frame).getGlassPane();
				componentImage = new BufferedImage(obj.getWidth(), obj.getHeight(), BufferedImage.TYPE_INT_ARGB);

				//obj.setBackground(Color.RED);
				Graphics gc = componentImage.getGraphics();
				//gc.setColor(Color.RED);

				obj.print(gc);

				//glassPane.getGraphics().drawImage( componentImage, e.getX()+pOffset.x,pOffset.y,null );
				layerPane = obj.getRootPane().getLayeredPane();

				this.offsetX = e.getX();
				pOffset = SwingUtilities.convertPoint(obj, 0, 0, layerPane);
				System.out.println(pOffset);

				imgComp = new JImage(componentImage);

				//imgComp = new JLabel("dddddd");
				imgComp.setLocation(pOffset);
				imgComp.setSize(imgComp.getPreferredSize());
				layerPane.add(imgComp, JLayeredPane.DRAG_LAYER);

				draggedComponent.setVisible(false);
				//this.remove(draggedComponent);
				//this.validate();
			}
		}

	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e) {
		//System.out.println("mouseReleased");
		this.draggedComponent.setVisible(true);
		this.draggedComponent = null;

		this.layerPane.remove(this.imgComp);
		this.layerPane.validate();
		this.layerPane.repaint();
		componentImage = null;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e) {
		

	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent e) {
		

	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	public void mouseDragged(MouseEvent e) {
		//System.out.println("mouseDragged: " + e.getSource());
		
		if (componentImage != null) {
			;
			pOffset = SwingUtilities.convertPoint((JComponent) e.getSource(), 0, 0, layerPane);

			int currentX = e.getX() + pOffset.x - this.offsetX;

			Point p = SwingUtilities.convertPoint(this, 0, 0, layerPane);

			// Min a droite
			if (currentX < p.x)
				currentX = p.x;

			// Max droite
			p = SwingUtilities.convertPoint(this, this.getWidth() - 2, 0, layerPane);
			p.x -= this.draggedComponent.getWidth();
			if (currentX >= p.x)
				currentX = p.x;

			int select = (currentX) / (this.getWidth() / size);
			//System.out.println(" select: " + select);
			if (/*select !=this.draggedComponentIndex &&*/
				select != currentIndex) {
				currentIndex = select;
				//System.out.println("Move en " + select);
				this.remove(this.draggedComponent);
				for (int i = 0; i < list.size(); i++) {
					JComponent obj = (JComponent) list.elementAt(i);
					this.remove(obj);
				}
				swap(this.draggedComponentIndex, select);

				for (int i = 0; i < list.size(); i++) {
					JComponent obj = (JComponent) list.elementAt(i);

					super.add(obj);
				}

				this.draggedComponentIndex = select;
				this.draggedComponent = (JComponent) list.elementAt(select);
				this.validate();
			}
			this.imgComp.setLocation(currentX, pOffset.y);
		}

	}

	/**
	 * Inverse les composants i et j
	 * @param i
	 * @param select
	 */
	private void swap(int i, int j) {
		System.out.println("Swap: " + i + "," + j);
		JComponent c1 = (JComponent) this.list.get(i);
		JComponent c2 = (JComponent) this.list.get(j);
		this.list.set(j, c1);
		this.list.set(i, c2);

	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	public void mouseMoved(MouseEvent e) {
		//System.out.println("mouseMoved");

	}

}
