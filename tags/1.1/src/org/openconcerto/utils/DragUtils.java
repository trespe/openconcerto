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
 
 package org.openconcerto.utils;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

/**
 * @author ILM Informatique 9 sept. 2004
 */
public class DragUtils {

	//*** Gestion du feedback dans le DRAG_LAYER

	static private JComponent source;
	static private Point click;
	static private JImage dragged;

	static {
		init();
	}

	static private void init() {
		source = null;
		click = null;
		dragged = null;
	}

	static public boolean beingDragged(JComponent comp) {
		return comp == source;
	}

	/**
	 * Ajoute une image de cette vue dans le drag layer.
	 */
	static synchronized public void beginDrag(JComponent comp, Point click) {
		if (source != null)
			throw new IllegalStateException("already dragging");
		DragUtils.source = comp;
		DragUtils.click = click;
		addToDragLayer(getImage());
	}

	static synchronized public void beginDrag(JComponent comp, Point click, Image img) {
		if (source != null)
			throw new IllegalStateException("already dragging");
		DragUtils.source = comp;
		DragUtils.click = new Point();
		addToDragLayer(img);
	}

	static private Image getImage() {
		// crée une image du composant source
		BufferedImage img = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		//VolatileImage img = source.createVolatileImage(source.getWidth(), source.getHeight());
		//Image img = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleVolatileImage(source.getWidth(), source.getHeight());

		Graphics2D gr = img.createGraphics();
		AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC, 0.7f);
		gr.setComposite(ac);

		source.print(gr);
		return img;
	}

	private static void addToDragLayer(Image img) {
		// on convertit les coord de la source
		Point rootPanePoint = SwingUtilities.convertPoint(source, new Point(), source.getRootPane());

		// crée une JImage et la place/dimensionne car les JLayeredPane n'ont pas de layout manager
		dragged = new JImage(img);
		dragged.setLocation(rootPanePoint);
		dragged.setSize(dragged.getPreferredSize());
		source.getRootPane().getLayeredPane().add(dragged, JLayeredPane.DRAG_LAYER);
	}

	/**
	 * Bouge l'image de cette vue dans le drag layer.
	 * @param p nouvelle position
	 */
	static public void dragTo(Point p) {
		JRootPane root = source.getRootPane();
		JLayeredPane lp = root.getLayeredPane();
		// at any moment there's only one component in DRAG_LAYER

		Point rootPanePoint = SwingUtilities.convertPoint(source, p, lp);
		dragged.setLocation(rootPanePoint.x - click.x, rootPanePoint.y - click.y);
	}

	/**
	 * Vide le drag layer.
	 */
	static public void endDrag() {
		if (dragged != null) {
			JLayeredPane lp = (JLayeredPane) dragged.getParent();
			lp.remove(dragged);
			lp.validate();
			lp.repaint();
			init();
		}
	}

	/*static private void flushDragLayer(JRootPane root) {
		JLayeredPane layeredPane = root.getLayeredPane();
		// TODO chercher pour faire un removeAll() 
		Component[] draggedComp = layeredPane.getComponentsInLayer(JLayeredPane.DRAG_LAYER.intValue());
		for (int i = 0; i < draggedComp.length; i++) {
			Component component = draggedComp[i];
			component.getParent().remove(component);
		}
		// validate does not work when the drop does not complete
		layeredPane.repaint();
	}*/

}
