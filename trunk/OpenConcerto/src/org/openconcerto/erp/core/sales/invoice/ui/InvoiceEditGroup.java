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
        super("sales.invoice.edit");
        final Group g = new Group("sales.invoice.identifier");
        g.addItem("NUMERO");
        g.addItem("DATE");
        g.addItem("NOM", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        g.addItem("ID_COMMERCIAL");
        this.add(g);

        final Group gCustomer = new Group("sales.invoice.customer");
        gCustomer.addItem("ID_CLIENT", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        add(gCustomer);

        final Group gAddress = new Group("sales.invoice.address");
        gAddress.addItem("ID_ADRESSE");
        add(gAddress);

        final Group gElements = new Group("sales.invoice.elements");
        gElements.addItem("(SAISIE_VENTE_FACTURE_ELEMENT)*", LayoutHints.DEFAULT_LIST_HINTS);
        add(gElements);

        final Group gInfos = new Group("sales.invoice.info");
        gInfos.addItem("INFOS", new LayoutHints(true, false, true, true, true, false));
        add(gInfos);

    }

}
