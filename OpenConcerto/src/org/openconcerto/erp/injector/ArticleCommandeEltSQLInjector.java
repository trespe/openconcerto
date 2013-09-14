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

public class ArticleCommandeEltSQLInjector extends SQLInjector {

    public ArticleCommandeEltSQLInjector(final DBRoot root) {
        super(root.getTable("ARTICLE"), root.getTable("COMMANDE_ELEMENT"), false);
        final SQLTable tableArticle = getSource();
        final SQLTable tableCommandeElement = getDestination();
        createDefaultMap();
        if (tableCommandeElement.contains("ID_ARTICLE")) {
            map(tableArticle.getKey(), tableCommandeElement.getField("ID_ARTICLE"));
        }
        if (tableArticle.contains("ID_DEVISE")) {
            remove(tableArticle.getField("ID_DEVISE"), tableCommandeElement.getField("ID_DEVISE"));
        }
    }
}
