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
 
 package org.openconcerto.erp.injector;

import java.math.BigDecimal;
import java.util.Collection;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;

public class FactureAvoirSQLInjector extends SQLInjector {
    public FactureAvoirSQLInjector(final DBRoot root) {
        super(root, "SAISIE_VENTE_FACTURE", "AVOIR_CLIENT", true);
        final SQLTable tableFacture = getSource();
        final SQLTable tableAvoir = getDestination();
        map(tableFacture.getField("ID_CLIENT"), tableAvoir.getField("ID_CLIENT"));
        map(tableFacture.getField("ID_ADRESSE"), tableAvoir.getField("ID_ADRESSE"));
        map(tableFacture.getField("ID_COMMERCIAL"), tableAvoir.getField("ID_COMMERCIAL"));
        map(tableFacture.getField("REMISE_HT"), tableAvoir.getField("REMISE_HT"));
        map(tableFacture.getField("PORT_HT"), tableAvoir.getField("PORT_HT"));
        map(tableFacture.getField("NUMERO"), tableAvoir.getField("NOM"));

    }

    @Override
    protected void merge(SQLRowAccessor srcRow, SQLRowValues rowVals) {
        super.merge(srcRow, rowVals);

        // Merge elements
        final SQLTable tableElementSource = getSource().getTable("SAISIE_VENTE_FACTURE_ELEMENT");
        final SQLTable tableElementDestination = getSource().getTable("AVOIR_CLIENT_ELEMENT");
        final Collection<? extends SQLRowAccessor> myListItem = srcRow.asRow().getReferentRows(tableElementSource);

        if (myListItem.size() != 0) {
            final SQLInjector injector = SQLInjector.getInjector(tableElementSource, tableElementDestination);
            for (SQLRowAccessor rowElt : myListItem) {
                final SQLRowValues createRowValuesFrom = injector.createRowValuesFrom(rowElt.asRow());
                if (createRowValuesFrom.getTable().getFieldsName().contains("POURCENT_ACOMPTE")) {
                    if (createRowValuesFrom.getObject("POURCENT_ACOMPTE") == null) {
                        createRowValuesFrom.put("POURCENT_ACOMPTE", new BigDecimal(100.0));
                    }
                }
                createRowValuesFrom.put("ID_AVOIR_CLIENT", rowVals);
            }
        }
    }
}
