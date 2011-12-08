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

public class BrFactureAchatSQLInjector extends SQLInjector {
    public BrFactureAchatSQLInjector(final DBRoot root) {
        super(root, "BON_RECEPTION", "SAISIE_ACHAT");

        final SQLTable tableBon = getSource();
        final SQLTable tableAchat = getDestination();
        map(tableBon.getField("ID_FOURNISSEUR"), tableAchat.getField("ID_FOURNISSEUR"));
        map(tableBon.getField("NOM"), tableAchat.getField("NOM"));
        map(tableBon.getField("INFOS"), tableAchat.getField("INFOS"));
        map(tableBon.getField("NUMERO"), tableAchat.getField("NUMERO_COMMANDE"));
        map(tableBon.getField("ID"), tableAchat.getField("IDSOURCE"));
        mapDefaultValues(tableAchat.getField("SOURCE"), tableBon.getName());
        map(tableBon.getField("TOTAL_TTC"), tableAchat.getField("MONTANT_TTC"));
        map(tableBon.getField("TOTAL_HT"), tableAchat.getField("MONTANT_HT"));
        map(tableBon.getField("TOTAL_TVA"), tableAchat.getField("MONTANT_TVA"));
    }
}
