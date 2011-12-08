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
 
 package org.openconcerto.erp.core.sales.pos.ui;

import org.openconcerto.erp.core.sales.pos.io.TicketPrinter;
import org.openconcerto.erp.core.sales.pos.model.TicketLine;

import javax.swing.JTextArea;

public class TextAreaTicketPrinter extends JTextArea implements TicketPrinter {

    private StringBuffer text = new StringBuffer();
    private String retour = "\n";

    public TextAreaTicketPrinter() {
        super();
        this.setEditable(false);
    }

    @Override
    public void addToBuffer(String t) {
        this.text.append(t);
        this.text.append(this.retour);

    }

    @Override
    public void addToBuffer(String t, int mode) {
        final boolean barCode = mode == TicketPrinter.BARCODE;
        if (barCode) {
            this.text.append("* ");
        }
        this.text.append(t);
        if (barCode) {
            this.text.append(" *");
        }

        this.text.append(this.retour);
    }

    @Override
    public void printBuffer() throws Exception {
        setText(this.text.toString());
    }

    public void clear() {
        this.text = new StringBuffer();
        setText("");
    }

    @Override
    public void openDrawer() throws Exception {

    }

    @Override
    public void addToBuffer(TicketLine line) {
        addToBuffer(line.getText(), TicketPrinter.NORMAL);
    }
}
