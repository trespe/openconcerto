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
 
 package org.openconcerto.erp.core.sales.quote.ui;

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.LayoutHints;

public class QuoteEditGroup extends Group {

    public QuoteEditGroup() {
        super("sales.quote");
        final Group g = new Group("sales.quote.identifier");
        g.addItem("sales.quote.number");
        g.addItem("sales.quote.date");
        g.addItem("sales.quote.label", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        g.addItem("sales.quote.saleman");
        this.add(g);

        final Group gCustomer = new Group("sales.quote.customer");
        gCustomer.addItem("sales.quote.customer", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        add(gCustomer);

        final Group gAddress = new Group("sales.quote.address");
        gAddress.addItem("sales.quote.address.alternative");
        add(gAddress);

        final Group gElements = new Group("sales.quote.items");
        gElements.addItem("sales.quote.items.list", LayoutHints.DEFAULT_LIST_HINTS);
        add(gElements);

        final Group gInfos = new Group("sales.quote.info");
        gInfos.addItem("sales.quote.info.general", LayoutHints.DEFAULT_VERY_LARGE_TEXT_HINTS);
        add(gInfos);

    }

}
