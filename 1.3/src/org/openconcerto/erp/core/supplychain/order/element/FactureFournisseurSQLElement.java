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
 
 package org.openconcerto.erp.core.supplychain.order.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.supplychain.order.component.FactureFournisseurSQLComponent;
import org.openconcerto.sql.element.SQLComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FactureFournisseurSQLElement extends ComptaSQLConfElement {

    public FactureFournisseurSQLElement() {
        super("FACTURE_FOURNISSEUR", "une facture fournisseur", "factures fournisseur");

    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("NOM");
        l.add("DATE");
        l.add("ID_FOURNISSEUR");
        l.add("T_HT");
        l.add("T_TTC");
        l.add("INFOS");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("NOM");
        l.add("DATE");
        return l;
    }

    @Override
    protected Set<String> getChildren() {
        Set<String> set = new HashSet<String>();
        set.add("FACTURE_FOURNISSEUR_ELEMENT");
        return set;
    }

    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.addAll(super.getPrivateFields());
        l.add("ID_MODE_REGLEMENT");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new FactureFournisseurSQLComponent();
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".invoice.purchase";
    }
}
