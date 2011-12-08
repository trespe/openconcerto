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
 
 /*
 * Créé le 26 juil. 2011
 */
package org.openconcerto.erp.injector;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLTable;

public class ArticleCommandeEltSQLInjector extends SQLInjector {
    private static final SQLTable articleTable = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("ARTICLE");
    private static final SQLTable commandeEltTable = Configuration.getInstance().getDirectory().getElement("COMMANDE_ELEMENT").getTable();

    public ArticleCommandeEltSQLInjector(final DBRoot root) {
        super(articleTable, commandeEltTable);
        createDefaultMap();
        if (articleTable.getFieldsName().contains("ID_DEVISE")) {
            remove(articleTable.getField("ID_DEVISE"), commandeEltTable.getField("ID_DEVISE"));
        }
    }
}
