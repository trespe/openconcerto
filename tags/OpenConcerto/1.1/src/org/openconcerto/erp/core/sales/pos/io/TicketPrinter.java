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

public interface TicketPrinter {
    public static final int NORMAL = 0;
    public static final int BOLD = 1;
    public static final int BOLD_LARGE = 2;
    public static final int UNDERLINE = 3;
    public static final int BARCODE = 10; // CODE 39

    public void addToBuffer(String t);

    public void addToBuffer(String t, int mode);

    public void printBuffer() throws Exception;

    public void openDrawer() throws Exception;

    public void addToBuffer(TicketLine line);

}
