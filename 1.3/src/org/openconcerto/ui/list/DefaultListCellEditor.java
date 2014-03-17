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
 
 package org.openconcerto.ui.list;


import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTextField;

public class DefaultListCellEditor extends DefaultCellEditor implements ListCellEditor{ 
    public DefaultListCellEditor(final JCheckBox checkBox){ 
        super(checkBox); 
    } 
 
    public DefaultListCellEditor(final JComboBox comboBox){ 
        super(comboBox); 
    } 
 
    public DefaultListCellEditor(final JTextField textField){ 
        super(textField); 
    } 
 
    public Component getListCellEditorComponent(JList list, Object value, boolean isSelected, int index){ 
        delegate.setValue(value); 
        return editorComponent; 
    } 
}
