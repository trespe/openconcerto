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
 
 
// Support for a PropertyEditor that uses text.

package org.openconcerto.utils.beans;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyEditor;

import javax.swing.JTextField;

import sun.beans.editors.IntEditor;

class PropertyText extends JTextField implements FocusListener, ActionListener, PropertyView {

	// FIXME do not use sun.beans.*
	static private String getSensibleValue(PropertyEditor ed) {
		if (ed.getClass() == IntEditor.class) {
			return "0";
		} else {
			return "";
		}
	}

	private PropertyEditor editor;

	PropertyText(PropertyEditor pe) {
		super(pe.getAsText());
		this.editor = pe;
		this.addActionListener(this);
		this.addFocusListener(this);
	}

	/** Doit être appelée quand l'interface change pour prévenir le modele */
	private void updateEditor() {
		Object old = editor.getValue();
		// TODO vérifier que le texte est valide (int)
		editor.setAsText(this.getText());
		this.firePropertyChange("propViewValue", old, this.editor.getValue());
	}

	/** Est appelée par notre controller quand le modele change */
	public void update(Object val) {
		if (val == null) {
			this.setText(getSensibleValue(this.editor));
			this.updateEditor();
		} else {
			Object old = editor.getValue();
			this.editor.setValue(val);
			this.setText(this.editor.getAsText());
			this.firePropertyChange("propViewValue", old, this.editor.getValue());
		}
	}

	//----------------------------------------------------------------------
	// listener methods.

	public void focusGained(FocusEvent e) {
	}

	public void focusLost(FocusEvent e) {
		this.updateEditor();
	}
	public void actionPerformed(ActionEvent e) {
		this.updateEditor();
	}

	public void addListener(PropertyController controller) {
		this.addPropertyChangeListener("propViewValue", controller);
	}

	public void removeListener(PropertyController controller) {
		this.removePropertyChangeListener("propViewValue", controller);
	}

}
