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
 * ILabel created on 30 sept. 2004
 * 
 */
package org.openconcerto.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.JLabel;

import org.openconcerto.ui.PrefManager;

/**
 * @author ILM Informatique 30 sept. 2004
 *
 */
public class ILabel extends JLabel {
	private boolean antialiased = true;
	/**
	 * @param text
	 * @param icon
	 * @param horizontalAlignment
	 */
	public ILabel(String text, Icon icon, int horizontalAlignment) {
		super(text, icon, horizontalAlignment);
		init();
	}

	/**
	 * @param text
	 * @param horizontalAlignment
	 */
	public ILabel(String text, int horizontalAlignment) {
		super(text, horizontalAlignment);
		init();
	}

	/**
	 * @param text
	 */
	public ILabel(String text) {
		super(text);
		init();
	}

	/**
	 * @param image
	 * @param horizontalAlignment
	 */
	public ILabel(Icon image, int horizontalAlignment) {
		super(image, horizontalAlignment);
		init();
	}

	/**
	 * @param image
	 */
	public ILabel(Icon image) {
		super(image);
		init();
	}

	/**
	 * 
	 */
	public ILabel() {
		super();
		init();
	}
	private final void init(){
	    this.setFont(PrefManager.fontText);
	}
	public void paint(Graphics g) {
	    
		if (this.isAntialiased()) {

			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
		super.paint(g);
	}

	/**
	 * @return
	 */
	public boolean isAntialiased() {
		return antialiased;
	}

	/**
	 * @param b
	 */
	public void setAntialiased(boolean b) {
		antialiased = b;
	}

}
