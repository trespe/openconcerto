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
 
 // FIXME classe pas finie
// Support for drawing a property value in a Canvas.

package org.openconcerto.utils.beans;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.*;
import java.beans.*;

import javax.swing.JComponent;
import javax.swing.JFrame;


class PropertyCanvas extends JComponent implements MouseListener, PropertyView {
	private JFrame fFrame;
	private JFrame frame;

	private PropertyEditor editor;

	PropertyCanvas(JFrame frame, PropertyEditor pe) {
		this.fFrame = frame;
		this.editor = pe;
		addMouseListener(this);
	}

	protected void paintComponent(Graphics g) {
		Rectangle box = new Rectangle(2, 2, getSize().width - 4, getSize().height - 4);
		editor.paintValue(g, box);
	}

	private static boolean ignoreClick = false;

	public void mouseClicked(MouseEvent evt) {
		if (!ignoreClick) {
			try {
				ignoreClick = true;
				int x = fFrame.getLocation().x - 30;
				int y = fFrame.getLocation().y + 50;
				new PropertyDialog(fFrame, editor, x, y);
			} finally {
				ignoreClick = false;
			}
		}
	}

	public void mousePressed(MouseEvent evt) {
	}

	public void mouseReleased(MouseEvent evt) {
	}

	public void mouseEntered(MouseEvent evt) {
	}

	public void mouseExited(MouseEvent evt) {
	}

	/* (non-Javadoc)
	 * @see ciol.view.property.PropertyView#update(java.lang.Object)
	 */
	public void update(Object val) {
		// FIXME mettre a jour le custom editor
		this.editor.setValue(val);
		this.repaint();
	}

	/* (non-Javadoc)
	 * @see ciol.view.property.PropertyView#addListener(ciol.view.property.PropertyController)
	 */
	public void addListener(PropertyController controller) {
		// FIXME Auto-generated method stub
		// faire un fire lorsque le PropertyDialog est disposed
	}

	/* (non-Javadoc)
	 * @see ciol.view.property.PropertyView#removeListener(ciol.view.property.PropertyController)
	 */
	public void removeListener(PropertyController controller) {
		// FIXME Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see ciol.view.property.PropertyView#setEditable(boolean)
	 */
	public void setEditable(boolean b) {
		// FIXME fermer le dialog, ne rien faire quand on clique
	}

}
