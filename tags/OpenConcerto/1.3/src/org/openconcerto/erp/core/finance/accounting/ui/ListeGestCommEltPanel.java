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
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.JButton;

public class ListeGestCommEltPanel extends ListeAddPanel {

    private EditFrame editModifyFrame, editReadOnlyFrame;

    private ListeGestCommEltPanel(SQLElement elem, boolean showAdd, Where w, String variant) {
        // TODO verifier que l'element contient la clef etrangere ID_MOUVEMENT
        this(elem, new IListe(elem.createTableSource(w)), showAdd);
    }

    public ListeGestCommEltPanel(SQLElement elem, IListe l) {
        this(elem, l, false);
    }

    public ListeGestCommEltPanel(SQLElement elem, IListe l, boolean showAdd) {
        super(elem, l);
        this.setAddVisible(showAdd);
        this.setOpaque(false);
        if (elem.getTable().getName().equals("SAISIE_VENTE_FACTURE")) {
            this.btnMngr.setAdditional(this.buttonEffacer, new ITransformer<JButton, String>() {

                @Override
                public String transformChecked(JButton input) {

                    SQLRowAccessor row = getListe().fetchSelectedRow();

                    if (row.getBoolean("PARTIAL") && !isLastPartialInvoice(row)) {
                        return "Vous ne pouvez pas supprimer cette facture intermédiaire.\n Des factures antérieures ont été établies !";
                    }
                    return null;
                }
            });
            this.btnMngr.setAdditional(this.buttonModifier, new ITransformer<JButton, String>() {

                @Override
                public String transformChecked(JButton input) {

                    SQLRowAccessor row = getListe().fetchSelectedRow();

                    if (row.getBoolean("PARTIAL") || row.getBoolean("SOLDE")) {
                        return "Vous ne pouvez pas modifier une facture intermédiaire.";
                    }
                    return null;
                }
            });
        }
    }

    public boolean isLastPartialInvoice(SQLRowAccessor sqlRowAccessor) {
        Collection<? extends SQLRowAccessor> rows = sqlRowAccessor.getReferentRows(sqlRowAccessor.getTable().getTable("TR_COMMANDE_CLIENT"));
        for (SQLRowAccessor sqlRowAccessor2 : rows) {
            SQLRowAccessor rowCmd = sqlRowAccessor2.getForeign("ID_COMMANDE_CLIENT");
            if (rowCmd != null && !rowCmd.isUndefined()) {
                Collection<? extends SQLRowAccessor> rowSFacts = rowCmd.getReferentRows(sqlRowAccessor.getTable().getTable("TR_COMMANDE_CLIENT"));
                for (SQLRowAccessor sqlRowAccessor3 : rowSFacts) {
                    if (!sqlRowAccessor3.isForeignEmpty("ID_SAISIE_VENTE_FACTURE")) {
                        SQLRowAccessor rowFact = sqlRowAccessor3.getForeign("ID_SAISIE_VENTE_FACTURE");
                        if (rowFact.getDate("DATE").after(sqlRowAccessor.getDate("DATE"))) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public ListeGestCommEltPanel(SQLElement elem, IListe l, String variant) {
        // TODO verifier que l'element contient la clef etrangere ID_MOUVEMENT
        super(elem, l, variant);
        this.setAddVisible(false);
        this.setOpaque(false);
    }

    public ListeGestCommEltPanel(SQLElement elem, boolean showAdd) {
        this(elem, showAdd, null, null);
    }

    public ListeGestCommEltPanel(SQLElement elem) {
        this(elem, false);
    }

    public ListeGestCommEltPanel(SQLElement elem, Where w, String variant) {
        this(elem, false, w, variant);
    }

    public ListeGestCommEltPanel(SQLElement elem, Where w) {
        this(elem, false, w, null);
    }

    protected void handleAction(JButton source, ActionEvent evt) {

        SQLRow row = this.getElement().getTable().getRow(this.getListe().getSelectedId());

        if (row != null && row.getID() > 1) {
            final SQLRowAccessor mvt = row.getForeign("ID_MOUVEMENT");
            if (source == this.buttonEffacer) {

                if (mvt != null && !mvt.isUndefined()) {
                    PanelFrame frame = new PanelFrame(new SuppressionEcrituresPanel(mvt.getID()), "Suppression");
                    frame.pack();
                    frame.setLocationRelativeTo(null);
                    frame.setResizable(false);
                    frame.setVisible(true);
                } else {
                    super.handleAction(source, evt);
                }
            } else {
                if (source == this.buttonModifier) {

                    if (mvt == null || mvt.isUndefined() | MouvementSQLElement.isEditable(mvt.getID())) {
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
