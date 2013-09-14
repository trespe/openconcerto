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
 
 package org.openconcerto.erp.core.sales.invoice.ui;

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.LayoutHints;

public class InvoiceEditGroup extends Group {

    public InvoiceEditGroup() {
        super("sales.invoice");
        final Group g = new Group("sales.invoice.identifier");
        g.addItem("sales.invoice.number");
        g.addItem("sales.invoice.date");
        g.addItem("sales.invoice.label", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        g.addItem("sales.invoice.saleman");
        this.add(g);

        final Group gCustomer = new Group("sales.invoice.customer");
        gCustomer.addItem("sales.invoice.customer", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        add(gCustomer);

        final Group gAddress = new Group("sales.invoice.address");
        gAddress.addItem("sales.invoice.address.alternative");
        add(gAddress);

        final Group gElements = new Group("sales.invoice.items");
        gElements.addItem("sales.invoice.items.list", LayoutHints.DEFAULT_LIST_HINTS);
        add(gElements);

        final Group gInfos = new Group("sales.invoice.info");
        gInfos.addItem("sales.invoice.info.general", LayoutHints.DEFAULT_VERY_LARGE_TEXT_HINTS);
        add(gInfos);

    }

}
