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

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLTable;

public class DevisFactureSQLInjector extends SQLInjector {
    public DevisFactureSQLInjector(final DBRoot root) {
        super(root, "DEVIS", "SAISIE_VENTE_FACTURE");

        final SQLTable tableDevis = getSource();
        final SQLTable tableFacture = getDestination();
        map(tableDevis.getField("PORT_HT"), tableFacture.getField("PORT_HT"));
        map(tableDevis.getField("REMISE_HT"), tableFacture.getField("REMISE_HT"));
        map(tableDevis.getField("ID_CLIENT"), tableFacture.getField("ID_CLIENT"));
        map(tableDevis.getField("OBJET"), tableFacture.getField("NOM"));
        map(tableDevis.getField("ID_COMMERCIAL"), tableFacture.getField("ID_COMMERCIAL"));
        mapDefaultValues(tableFacture.getField("SOURCE"), "DEVIS");
        map(tableDevis.getField("ID_DEVIS"), tableFacture.getField("IDSOURCE"));
        map(tableDevis.getField("INFOS"), tableFacture.getField("INFOS"));
    }
}
