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
 
 package org.openconcerto.erp.core.common.component;

import org.openconcerto.erp.core.common.ui.AbstractArticleItemTable;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;

import java.util.List;


public abstract class TransfertBaseSQLComponent extends BaseSQLComponent {

    public TransfertBaseSQLComponent(SQLElement element) {
        super(element);
    }

    /**
     * Chargement d'élément à partir d'une autre table ex : les éléments d'un BL dans une facture
     * 
     * @param table ItemTable du component de destination (ex : tableFacture)
     * @param elt element source (ex : BL)
     * @param id id de la row source
     * @param itemsElt elements des items de la source (ex : BL_ELEMENT)
     */
    public void loadItem(AbstractArticleItemTable table, SQLElement elt, int id, SQLElement itemsElt) {

        loadItem(table, elt, id, itemsElt, true);
    }

    public void loadItem(AbstractArticleItemTable table, SQLElement elt, int id, SQLElement itemsElt, boolean clear) {
        List<SQLRow> myListItem = elt.getTable().getRow(id).getReferentRows(itemsElt.getTable());

        if (myListItem.size() != 0) {
            SQLInjector injector = SQLInjector.createDefaultInjector(itemsElt.getTable(), table.getSQLElement().getTable());
            if (clear) {
                table.getModel().clearRows();
            }
            for (SQLRow rowElt : myListItem) {

                table.getModel().addRow(injector.createRowValuesFrom(rowElt));
                int rowIndex = table.getModel().getRowCount() - 1;
                table.getModel().fireTableModelModified(rowIndex);
            }
        } else {
            if (clear) {
                table.getModel().clearRows();
                table.getModel().addNewRowAt(0);
            }
        }
        table.getModel().fireTableDataChanged();
        table.repaint();
    }
}
