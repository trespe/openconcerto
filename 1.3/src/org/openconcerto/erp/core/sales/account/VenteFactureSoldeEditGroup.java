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
 
 package org.openconcerto.erp.core.sales.account;

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.LayoutHints;

public class VenteFactureSoldeEditGroup extends Group {

    public VenteFactureSoldeEditGroup() {
        super("sales.invoice.partial.balance");
        final Group g = new Group("sales.invoice.partial.balance.identifier");
        g.addItem("sales.invoice.number");
        g.addItem("DATE");
        g.addItem("NOM", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        g.addItem("ID_COMMERCIAL");
        g.addItem("sales.invoice.partial.amount");

        this.add(g);

        final Group gCustomer = new Group("sales.invoice.partial.balance.customer");
        gCustomer.addItem("sales.invoice.customer", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        add(gCustomer);

        final Group gElements = new Group("sales.invoice.partial.balance.items");
        gElements.addItem("sales.invoice.partial.items.list", LayoutHints.DEFAULT_LIST_HINTS);
        add(gElements);

        final Group gMdr = new Group("sales.invoice.partial.balance.payment");
        gMdr.addItem("ID_MODE_REGLEMENT");
        add(gMdr);

        final Group gTotal = new Group("sales.invoice.partial.balance.total");
        gTotal.addItem("sales.invoice.partial.total.amount");
        add(gTotal);

        final Group gInfos = new Group("sales.invoice.partial.balance.infos");
        gInfos.addItem("INFOS", LayoutHints.DEFAULT_VERY_LARGE_TEXT_HINTS);
        add(gInfos);

    }

}
