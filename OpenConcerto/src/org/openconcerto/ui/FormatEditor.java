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
 
 package org.openconcerto.ui;

import org.openconcerto.utils.FormatGroup;

import java.awt.Color;
import java.text.Format;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.text.JTextComponent;

/**
 * A cell editor with a text field that converts between object and text using formats.
 * 
 * @author Sylvain CUAZ
 */
public class FormatEditor extends DefaultCellEditor {

    public FormatEditor(Format... formats) {
        this(Arrays.asList(formats));
    }

    public FormatEditor(final List<? extends Format> formats) {
        this(new FormatGroup(formats));
    }

    /**
     * Creates an editor, which will try to parse with the given formats. To initialize the cell,
     * the first format will be used.
     * 
     * @param formats a List of Format.
     * @throws IllegalArgumentException if formats is empty.
     */
    public FormatEditor(final FormatGroup formats) {
        super(new JTextField());

        this.delegate = new EditorDelegate() {

            public void setValue(final Object newVal) {
                final String txt;
                if (newVal == null) {
                    txt = "";
                } else {
                    txt = formats.format(newVal);
                }
                ((JTextComponent) getComponent()).setText(txt);
                ((JComponent) getComponent()).setBorder(new LineBorder(Color.BLACK));
                super.setValue(newVal);
            }

            public boolean stopCellEditing() {
                final String s = ((JTextComponent) getComponent()).getText();
                if (s.length() > 0) {
                    try {
                        this.value = formats.parseObject(s);
                    } catch (ParseException e) {
                        ((JComponent) getComponent()).setBorder(new LineBorder(Color.RED));
                        return false;
                    } catch (Exception e) {
                        ((JComponent) getComponent()).setBorder(new LineBorder(Color.YELLOW));
                        return false;
                    }
                } else {
                    this.value = null;
                }
                return super.stopCellEditing();
            }
        };
    }

}
