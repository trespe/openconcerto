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
 
 //FIXME classe pas finie
// Support for PropertyEditor with custom editors.

package org.openconcerto.utils.beans;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.*;
import java.beans.*;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;

class PropertyDialog extends JDialog implements ActionListener {

	private JButton doneButton;
	private Component body;
	private final static int vPad = 5;
	private final static int hPad = 4;

	PropertyDialog(JFrame frame, PropertyEditor pe, int x, int y) {
		super(frame, pe.getClass().getName(), true);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(null);

		body = pe.getCustomEditor();
		add(body);

		doneButton = new JButton("Done");
		doneButton.addActionListener(this);
		add(doneButton);

		setLocation(x, y);
		show();
	}

	public void actionPerformed(ActionEvent evt) {
		// Button down.
		dispose();
	}

	public void doLayout() {
		Insets ins = getInsets();
		Dimension bodySize = body.getPreferredSize();
		Dimension buttonSize = doneButton.getPreferredSize();

		int width = ins.left + 2 * hPad + ins.right + bodySize.width;
		int height = ins.top + 3 * vPad + ins.bottom + bodySize.height + buttonSize.height;

		body.setBounds(ins.left + hPad, ins.top + vPad, bodySize.width, bodySize.height);

		doneButton.setBounds((width - buttonSize.width) / 2, ins.top + (2 * hPad) + bodySize.height, buttonSize.width, buttonSize.height);

		setSize(width, height);

	}

}
