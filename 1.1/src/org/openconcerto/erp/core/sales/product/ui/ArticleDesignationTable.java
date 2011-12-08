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
 
 package org.openconcerto.erp.core.sales.product.ui;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTablePanel;
import org.openconcerto.sql.view.list.SQLTableElement;

import java.util.List;
import java.util.Vector;

public class ArticleDesignationTable extends RowValuesTablePanel {

    public ArticleDesignationTable() {
        init();
        uiInit();
    }

    /**
     * 
     */
    protected void init() {

        final SQLElement e = getSQLElement();

        final List<SQLTableElement> list = new Vector<SQLTableElement>();

        final SQLTableElement langue = new SQLTableElement(e.getTable().getField("ID_LANGUE"));
        langue.setEditable(false);
        list.add(langue);

        // Désignation
        final SQLTableElement tableElement_Nom = new SQLTableElement(e.getTable().getField("NOM"));
        list.add(tableElement_Nom);

        this.model = new RowValuesTableModel(e, list, e.getTable().getField("ID_LANGUE"), false);

        this.table = new RowValuesTable(this.model, null);

    }

    public SQLElement getSQLElement() {
        return Configuration.getInstance().getDirectory().getElement("ARTICLE_DESIGNATION");
    }

}
