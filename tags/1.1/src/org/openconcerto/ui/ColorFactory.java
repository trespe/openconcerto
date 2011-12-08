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
 * ColorFactory created on 3 avr. 2004
 * 
 */
package org.openconcerto.ui;
import java.awt.Color;
import java.util.Vector;

/**
 * @author ILM Informatique 3 avr. 2004
 *
 */
public class ColorFactory {
	private static final Vector couleur = new Vector();
	private static final ColorFactory instance = new ColorFactory();

	private ColorFactory() {
		couleur.add(Color.RED);
		couleur.add(Color.ORANGE);
		couleur.add(Color.GREEN);
		couleur.add(Color.CYAN);
		couleur.add(Color.BLUE);
		couleur.add(Color.MAGENTA);
		couleur.add(Color.PINK);
	}

	public synchronized static ColorFactory getInstance() {
		return instance;
	}
	/*
	 * retourne une couleur predefinie
	 * */
	public Color getColor(int n) {
		return (Color) couleur.elementAt(n % couleur.size());
	}
}
