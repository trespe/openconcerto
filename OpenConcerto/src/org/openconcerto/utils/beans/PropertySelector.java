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
 
 // Support for PropertyEditors that use tags.

package org.openconcerto.utils.beans;

import java.awt.event.*;
import java.beans.*;

import javax.swing.JComboBox;

class PropertySelector extends JComboBox implements ActionListener, PropertyView {

	private PropertyEditor editor;

	PropertySelector(PropertyEditor pe) {
		super(pe.getTags());
		this.editor = pe;
		// on ecoute les selections
		this.addActionListener(this);
	}

	public void actionPerformed(ActionEvent e) {
		this.updateEditor();
	}

	/** Doit être appelée quand l'interface change pour prévenir le modele */
	private void updateEditor() {
		Object old = editor.getValue();
		editor.setAsText((String) this.getSelectedItem());
		this.firePropertyChange("propViewValue", old, this.editor.getValue());
	}

	public void update(Object val) {
		if (val == null) {
			this.setSelectedIndex(0);
			this.updateEditor();
		} else {
			Object old = editor.getValue();
			this.editor.setValue(val);
			this.setSelectedItem(this.editor.getAsText());
			this.firePropertyChange("propViewValue", old, this.editor.getValue());
		}
	}

	// MAYBE factoriser avec un aspect
	public void addListener(PropertyController controller) {
		this.addPropertyChangeListener("propViewValue", controller);
	}

	public void removeListener(PropertyController controller) {
		this.removePropertyChangeListener("propViewValue", controller);
	}

	// FIXME
	public void setEditable(boolean b) {
		this.setEnabled(b);
	}

}
