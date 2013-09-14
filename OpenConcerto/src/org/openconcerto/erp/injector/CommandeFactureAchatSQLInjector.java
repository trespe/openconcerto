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

public class CommandeFactureAchatSQLInjector extends SQLInjector {
    public CommandeFactureAchatSQLInjector(final DBRoot root) {
        super(root, "COMMANDE", "SAISIE_ACHAT", true);
        final SQLTable tableCommande = getSource();
        final SQLTable tableAchat = getDestination();
        map(tableCommande.getField("ID_FOURNISSEUR"), tableAchat.getField("ID_FOURNISSEUR"));
        map(tableCommande.getField("NOM"), tableAchat.getField("NOM"));
        map(tableCommande.getField("INFOS"), tableAchat.getField("INFOS"));
        map(tableCommande.getField("NUMERO"), tableAchat.getField("NUMERO_COMMANDE"));
        map(tableCommande.getField("T_TTC"), tableAchat.getField("MONTANT_TTC"));
        map(tableCommande.getField("T_HT"), tableAchat.getField("MONTANT_HT"));
        map(tableCommande.getField("T_TVA"), tableAchat.getField("MONTANT_TVA"));
    }
}
