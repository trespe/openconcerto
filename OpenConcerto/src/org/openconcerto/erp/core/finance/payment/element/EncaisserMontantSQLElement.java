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
 
 package org.openconcerto.erp.core.finance.payment.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.payment.component.EncaisserMontantSQLComponent;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.TreesOfSQLRows;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.PathBuilder;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.CollectionUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class EncaisserMontantSQLElement extends ComptaSQLConfElement {

    public EncaisserMontantSQLElement() {
        super("ENCAISSER_MONTANT", "un encaissement de montant", "encaissements de montant");
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
            l.add("DATE");
            l.add("NOM");
            l.add("ID_CLIENT");
            // l.add("ID_MOUVEMENT");
            l.add("ID_MODE_REGLEMENT");
            l.add("MONTANT");
        return l;
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("MONTANT");
        return l;
    }

    @Override
    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_MODE_REGLEMENT");
        return l;
    }

    @Override
    public Set<String> getReadOnlyFields() {

        return Collections.singleton("ID_CLIENT");
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    @Override
    public SQLComponent createComponent() {
        return new EncaisserMontantSQLComponent(this);
    }

    @Override
    protected void archive(TreesOfSQLRows trees, boolean cutLinks) throws SQLException {

        // On rétablit les échéances
        for (SQLRow row : trees.getRows()) {
            for (SQLRow rowEncaisse : row.getReferentRows()) {

                SQLRow rowEch = rowEncaisse.getForeignRow("ID_ECHEANCE_CLIENT");
                // SI une echeance est associée (paiement non comptant)
                if (rowEch.getID() > 1) {
                    SQLRowValues rowVals = rowEch.createEmptyUpdateRow();
                    rowVals.put("REGLE", Boolean.FALSE);
                    if (rowEch.getBoolean("REGLE")) {
                        rowVals.put("MONTANT", rowEncaisse.getLong("MONTANT_REGLE"));
                    } else {
                        rowVals.put("MONTANT", rowEch.getLong("MONTANT") + rowEncaisse.getLong("MONTANT_REGLE"));
                    }
                    rowVals.update();
                }
                Configuration.getInstance().getDirectory().getElement(rowEncaisse.getTable()).archive(rowEncaisse);
            }

            // On supprime les mouvements
            SQLSelect sel = new SQLSelect(getTable().getBase());

            SQLTable tableMvt = getTable().getTable("MOUVEMENT");
            EcritureSQLElement eltEcr = (EcritureSQLElement) Configuration.getInstance().getDirectory().getElement(tableMvt.getTable("ECRITURE"));
            sel.addSelectStar(tableMvt);
            Where w = new Where(tableMvt.getField("SOURCE"), "=", getTable().getName());
            w = w.and(new Where(tableMvt.getField("IDSOURCE"), "=", row.getID()));
            sel.setWhere(w);
            List<SQLRow> list = (List<SQLRow>) getTable().getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel, tableMvt));
            for (SQLRow sqlRow : list) {
                eltEcr.archiveMouvementProfondeur(sqlRow.getID(), true);
            }
        }

        super.archive(trees, cutLinks);
    }
}
