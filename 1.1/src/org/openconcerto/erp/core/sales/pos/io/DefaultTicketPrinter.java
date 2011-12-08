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
 
 package org.openconcerto.erp.core.sales.pos.io;

import org.openconcerto.erp.core.sales.pos.model.TicketLine;

import java.util.ArrayList;
import java.util.List;

public abstract class DefaultTicketPrinter implements TicketPrinter {
    protected List<String> strings = new ArrayList<String>();
    protected List<Integer> modes = new ArrayList<Integer>();

    public DefaultTicketPrinter() {

    }

    public void addToBuffer(String t) {
        addToBuffer(t, NORMAL);
    }

    public void addToBuffer(String t, int mode) {
        this.strings.add(t);
        this.modes.add(mode);
    }

    public static String formatLeft(int l, String string) {
        if (string.length() > l) {
            string = string.substring(0, l);
        }
        StringBuffer str = new StringBuffer(l);
        str.append(string);
        final int stop = l - string.length();
        for (int i = 0; i < stop; i++) {
            str.append(' ');
        }
        return str.toString();
    }

    public static String formatRight(int l, String string) {
        if (string.length() > l) {
            string = string.substring(0, l);
        }

        StringBuffer str = new StringBuffer(l);

        final int stop = l - string.length();
        for (int i = 0; i < stop; i++) {
            str.append(' ');
        }
        str.append(string);
        return str.toString();
    }

    public void addToBuffer(TicketLine line) {
        final String style = line.getStyle();
        final int mode;
        if (style == null) {
            mode = TicketPrinter.NORMAL;
        } else if (style.equals("bold_large")) {
            mode = TicketPrinter.BOLD_LARGE;
        } else if (style.equals("bold")) {
            mode = TicketPrinter.BOLD;
        } else if (style.equals("underline")) {
            mode = TicketPrinter.UNDERLINE;
        } else {
            mode = TicketPrinter.NORMAL;
        }
        addToBuffer(line.getText(), mode);
    }
}
