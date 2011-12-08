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
 
 package org.openconcerto.erp.core.supplychain.credit.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.supplychain.credit.component.AvoirFournisseurSQLComponent;
import org.openconcerto.sql.element.SQLComponent;

import java.util.ArrayList;
import java.util.List;


public class AvoirFournisseurSQLElement extends ComptaSQLConfElement {

    public AvoirFournisseurSQLElement() {
        super("AVOIR_FOURNISSEUR", "une facture d'avoir fournisseur", "factures d'avoir fournisseur");
    }

    public List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("ID_FOURNISSEUR");
        l.add("NOM");
        l.add("DATE");
        l.add("MONTANT_HT");
        l.add("MONTANT_TTC");
        l.add("SOLDE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("NOM");
        l.add("NUMERO");
        l.add("MONTANT_TTC");
        return l;
    }

    @Override
    protected List<String> getPrivateFields() {
        List<String> l = new ArrayList<String>();
        l.add("ID_MODE_REGLEMENT");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new AvoirFournisseurSQLComponent();
    }

}
