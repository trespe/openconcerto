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

public class FactureAvoirSQLInjector extends SQLInjector {
    public FactureAvoirSQLInjector(final DBRoot root) {
        super(root, "SAISIE_VENTE_FACTURE", "AVOIR_CLIENT");
        final SQLTable tableFacture = getSource();
        final SQLTable tableAvoir = getDestination();
        map(tableFacture.getField("ID_CLIENT"), tableAvoir.getField("ID_CLIENT"));
        map(tableFacture.getField("ID_ADRESSE"), tableAvoir.getField("ID_ADRESSE"));
        map(tableFacture.getField("ID_COMMERCIAL"), tableAvoir.getField("ID_COMMERCIAL"));
        map(tableFacture.getField("REMISE_HT"), tableAvoir.getField("REMISE_HT"));
        map(tableFacture.getField("PORT_HT"), tableAvoir.getField("PORT_HT"));
        map(tableFacture.getField("NUMERO"), tableAvoir.getField("NOM"));
        // map(tableFacture.getField("INFOS"), tableAvoir.getField("INFOS"));
    }
}
