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
 
 package org.openconcerto.erp.core.sales.credit.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.sales.credit.component.AvoirClientSQLComponent;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class AvoirClientSQLElement extends ComptaSQLConfElement {

    public AvoirClientSQLElement() {
        super("AVOIR_CLIENT", "une facture d'avoir", "factures d'avoir");
    }

    public List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("ID_CLIENT");
        l.add("NOM");
        l.add("DATE");
        l.add("MONTANT_HT");
        l.add("MONTANT_TTC");
        l.add("MONTANT_SOLDE");
        l.add("MONTANT_RESTANT");
        l.add("SOLDE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("NOM");
        l.add("NUMERO");
        l.add("MONTANT_TTC");
        l.add("MONTANT_SOLDE");
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
        return new AvoirClientSQLComponent();
    }

    @Override
    protected void archive(SQLRow row, boolean cutLinks) throws SQLException {

        super.archive(row, cutLinks);

        // Mise à jour des stocks
        SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
        SQLSelect sel = new SQLSelect(eltMvtStock.getTable().getBase());
        sel.addSelect(eltMvtStock.getTable().getField("ID"));
        Where w = new Where(eltMvtStock.getTable().getField("IDSOURCE"), "=", row.getID());
        Where w2 = new Where(eltMvtStock.getTable().getField("SOURCE"), "=", getTable().getName());
        sel.setWhere(w.and(w2));

        List l = (List) eltMvtStock.getTable().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());
        if (l != null) {
            for (int i = 0; i < l.size(); i++) {
                Object[] tmp = (Object[]) l.get(i);
                eltMvtStock.archive(((Number) tmp[0]).intValue());
            }
        }
    }

}
