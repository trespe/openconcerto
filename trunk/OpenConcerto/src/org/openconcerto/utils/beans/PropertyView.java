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
 
 package org.openconcerto.utils.beans;

/** Une vue représentant une proprièté d'un bean.
 * <p>
 * Note :
 * Les classes implémentant cette interface doivent être des JComponent.
 * </p>
 * @author ILM Informatique 30 juin 2004
 */
public interface PropertyView {

	/** Est appelée quand le modele change.
	 * @param val la nouvelle valeur de la proprièté.
	 */
	public void update(Object val);

	/** Le controller sera désormais informé lorsque cette vue sera modifiée.
	 * @param controller va être informé lorsque cette vue sera modifiée.
	 */
	public void addListener(PropertyController controller);

	/** Le controller ne sera plus informé lorsque cette vue sera modifiée.
	 * @param controller le controller à retiré.
	 */
	public void removeListener(PropertyController controller);

	/** Est ce que cette vue est editable.
	 * @param b <code>true</code> si cette vue est editable.
	 */
	public void setEditable(boolean b);
}
