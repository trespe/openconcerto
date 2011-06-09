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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;

import java.awt.event.ActionEvent;

import javax.swing.JButton;

public class ListeGestCommEltPanel extends ListeAddPanel {

    private static SQLTableModelSourceOnline copy(SQLElement elem, Where w) {
        final SQLTableModelSourceOnline res = elem.getTableSource(true);
        res.getReq().setWhere(w);
        return res;
    }

    private EditFrame editModifyFrame, editReadOnlyFrame;

    private ListeGestCommEltPanel(SQLElement elem, boolean showAdd, Where w) {
        // TODO verifier que l'element contient la clef etrangere ID_MOUVEMENT
        this(elem, new IListe(copy(elem, w)), showAdd);
    }

    public ListeGestCommEltPanel(SQLElement elem, IListe l) {
        this(elem, l, false);
    }
        
    public ListeGestCommEltPanel(SQLElement elem, IListe l, boolean showAdd) {
        super(elem, l);
        this.setAddVisible(showAdd);
        this.setOpaque(false);
    }

    public ListeGestCommEltPanel(SQLElement elem, boolean showAdd) {
        this(elem, showAdd, null);
    }

    public ListeGestCommEltPanel(SQLElement elem) {
        this(elem, false);
    }

    public ListeGestCommEltPanel(SQLElement elem, Where w) {
        this(elem, false, w);
    }

    protected void handleAction(JButton source, ActionEvent evt) {

        SQLRow row = this.getElement().getTable().getRow(this.getListe().getSelectedId());

        if (row != null && row.getID() > 1) {
            final int idMvt = row.getInt("ID_MOUVEMENT");
            if (source == this.buttonEffacer) {

                if (idMvt > 1) {
                    PanelFrame frame = new PanelFrame(new SuppressionEcrituresPanel(idMvt), "Suppression");
                    frame.pack();
                    frame.setLocationRelativeTo(null);
                    frame.setResizable(false);
                    frame.setVisible(true);
                } else {
                    super.handleAction(source, evt);
                }
            } else {
                if (source == this.buttonModifier) {

                    if (MouvementSQLElement.isEditable(idMvt)) {
                        if (this.editModifyFrame == null) {
                            this.editModifyFrame = new EditFrame(this.element, EditPanel.MODIFICATION);
                        }
                        this.editModifyFrame.selectionId(this.getListe().getSelectedId());
                        this.editModifyFrame.setVisible(true);
                    } else {
                        if (this.editReadOnlyFrame == null) {
                            this.editReadOnlyFrame = new EditFrame(this.element, EditPanel.READONLY);
                        }
                        this.editReadOnlyFrame.selectionId(this.getListe().getSelectedId());
                        this.editReadOnlyFrame.setVisible(true);
                    }
                } else {
                    super.handleAction(source, evt);
                }
            }
        } else {
            super.handleAction(source, evt);
        }
    }
}
