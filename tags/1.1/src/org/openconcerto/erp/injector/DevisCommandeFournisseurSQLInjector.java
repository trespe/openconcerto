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

public class DevisCommandeFournisseurSQLInjector extends SQLInjector {
    public DevisCommandeFournisseurSQLInjector(final DBRoot root) {
        super(root, "DEVIS", "COMMANDE");

        final SQLTable tableDevis = getSource();
        final SQLTable tableCommande = getDestination();
        // map(tableDevis.getField("PORT_HT"), tableCommande.getField("PORT_HT"));
        // map(tableDevis.getField("REMISE_HT"), tableCommande.getField("REMISE_HT"));

        mapDefaultValues(tableCommande.getField("SOURCE"), tableDevis.getName());
        map(tableDevis.getKey(), tableCommande.getField("IDSOURCE"));
    }
}
