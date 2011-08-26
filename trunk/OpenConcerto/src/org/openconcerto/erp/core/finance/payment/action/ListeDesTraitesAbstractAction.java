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
 
 package org.openconcerto.erp.core.finance.payment.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.common.ui.ListeViewPanel;
import org.openconcerto.erp.core.finance.payment.element.TypeReglementSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;

import javax.swing.Action;
import javax.swing.JFrame;

abstract class ListeDesTraitesAbstractAction extends CreateFrameAbstractAction {

    private final SQLTable table;
    private final SQLField echeanceField;

    public ListeDesTraitesAbstractAction(final String name, final String tableName, String echeanceField) {
        super();
        this.putValue(Action.NAME, name);
        this.table = Configuration.getInstance().getRoot().findTable(tableName);
        this.echeanceField = this.table.getField(echeanceField);
    }

    @Override
    public final JFrame createFrame() {
        final SQLElement elt = Configuration.getInstance().getDirectory().getElement(this.table);
        final SQLField modeReglF = this.table.getField("ID_MODE_REGLEMENT");
        final SQLTable modeReglT = this.table.getForeignTable(modeReglF.getName());

        Where wPrev = new Where(modeReglF, "=", modeReglT.getKey());
        wPrev = wPrev.and(new Where(modeReglT.getField("ID_TYPE_REGLEMENT"), ">=", TypeReglementSQLElement.TRAITE));
        wPrev = wPrev.and(new Where(this.echeanceField, ">", 1));
        final SQLTableModelSourceOnline src = elt.getTableSource(true);
        src.getReq().setWhere(wPrev);

        IListFrame frame = new IListFrame(new ListeViewPanel(elt, new IListe(src)));
        frame.getPanel().setAddVisible(false);
        frame.getPanel().getListe().setSQLEditable(false);

        return frame;
    }
}
