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
package org.openconcerto.utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ILM Informatique 3 avr. 2004
 *  
 */
public class ColorFactory {
    private static final List couleur = new ArrayList();

    private static int n = 0;
   
	static {
		couleur.add(new Color(204, 204, 255));
		couleur.add(new Color(204, 255, 204));
		couleur.add(new Color(204, 255, 255));
		couleur.add(new Color(255, 204, 204));
		couleur.add(new Color(255, 204, 255));
		couleur.add(new Color(255, 255, 204));
	}

	/**
	 * Retourne une couleur predefinie.
	 * @param i un nombre arbitraire.
	 * @return une jolie couleur ;-).
	 */
	static public Color getColor(int i) {
		return (Color) couleur.get(i % couleur.size());
	}

	/**
	 * Retourne une couleur predefinie.
	 * @return une jolie couleur ;-).
	 */
	public static Color getColor() {
		return getColor(n++);
	}
}

