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

public class DevisFactureSQLInjector extends SQLInjector {
    public DevisFactureSQLInjector(final DBRoot root) {
        super(root, "DEVIS", "SAISIE_VENTE_FACTURE", true);
        final SQLTable tableDevis = getSource();
        final SQLTable tableFacture = getDestination();
        map(tableDevis.getField("PORT_HT"), tableFacture.getField("PORT_HT"));
        map(tableDevis.getField("REMISE_HT"), tableFacture.getField("REMISE_HT"));
        map(tableDevis.getField("ID_CLIENT"), tableFacture.getField("ID_CLIENT"));
        map(tableDevis.getField("OBJET"), tableFacture.getField("NOM"));
        map(tableDevis.getField("ID_COMMERCIAL"), tableFacture.getField("ID_COMMERCIAL"));
        map(tableDevis.getField("ID_DEVIS"), tableFacture.getField("ID_DEVIS"));
        map(tableDevis.getField("INFOS"), tableFacture.getField("INFOS"));
    }

    @Override
    protected void merge(SQLRowAccessor srcRow, SQLRowValues rowVals) {
        super.merge(srcRow, rowVals);

        // Merge elements
        final SQLTable tableElementSource = getSource().getTable("DEVIS_ELEMENT");
        final SQLTable tableElementDestination = getSource().getTable("SAISIE_VENTE_FACTURE_ELEMENT");
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
                createRowValuesFrom.put("ID_SAISIE_VENTE_FACTURE", rowVals);
            }
        }
    }
}
