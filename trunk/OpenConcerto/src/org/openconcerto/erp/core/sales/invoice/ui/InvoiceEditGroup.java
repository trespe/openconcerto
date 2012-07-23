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
        g.add("NUMERO");
        g.add("DATE");
        g.add("NOM", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        g.add("ID_COMMERCIAL");
        this.add(g);

        final Group gCustomer = new Group("sales.invoice.customer");
        gCustomer.add("ID_CLIENT", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        add(gCustomer, LayoutHints.DEFAULT_LARGE_FIELD_HINTS);

        final Group gAddress = new Group("sales.invoice.address");
        gAddress.add("ID_ADRESSE");
        add(gAddress, LayoutHints.DEFAULT_LARGE_FIELD_HINTS);

        final Group gElements = new Group("sales.invoice.elements");
        gElements.add("(SAISIE_VENTE_FACTURE_ELEMENT)*", LayoutHints.DEFAULT_LIST_HINTS);
        add(gElements, LayoutHints.DEFAULT_LIST_HINTS);

        final Group gInfos = new Group("sales.invoice.info");
        gInfos.add("INFOS", new LayoutHints(true, false, true, true, true, false));
        add(gInfos);

    }

}
