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
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;

import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class ListeDesReferencesAction extends CreateFrameAbstractAction {

    public ListeDesReferencesAction() {
        super();
        this.putValue(Action.NAME, "Liste des références");
    }

    public JFrame createFrame() {
        SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
        final SQLElement elementArticle = Configuration.getInstance().getDirectory().getElement(sqlTableArticle);
        List<String> listF = new ArrayList<String>();
        listF.add("CODE");
        listF.add("NOM");
        listF.add("ID_MODE_VENTE_ARTICLE");
        listF.add("POIDS");
        listF.add("VALEUR_METRIQUE_1");
        listF.add("VALEUR_METRIQUE_2");
        listF.add("VALEUR_METRIQUE_3");
        listF.add("PRIX_METRIQUE_HA_1");
        listF.add("PRIX_METRIQUE_VT_1");
        // listF.add("PRIX_METRIQUE_HA_3");
        listF.add("ID_TAXE");
        listF.add("PA_HT");
        listF.add("PV_HT");
        listF.add("PV_TTC");
        ListSQLRequest req = new ListSQLRequest(elementArticle.getTable(), listF) {
            @Override
            protected List<SQLRowValues> getValues(Where w) {
                final List<SQLRowValues> v = new ArrayList<SQLRowValues>();
                // TODO Auto-generated method stub
                Where w2 = new Where(elementArticle.getTable().getField("OBSOLETE"), "!=", Boolean.TRUE);
                if (w == null) {
                    w = w2;
                } else {
                    w = w.and(w2);
                }
                List<SQLRowValues> all = super.getValues(w);
                HashSet<String> codes = new HashSet<String>();
                for (int i = 0; i < all.size(); i++) {
                    final SQLRowValues element = all.get(i);
                    final String code = element.getString("CODE");

                    if (!codes.contains(code)) {
                        v.add(element);
                        codes.add(code);
                    }
                }
                return v;
            }
        };

        final IListe list = new IListe(elementArticle.initTableSource(new SQLTableModelSourceOnline(req)));

        IListFrame frame = new IListFrame(new ListeAddPanel(elementArticle, list) {
            @Override
            protected void handleAction(JButton source, ActionEvent evt) {
                // TODO Auto-generated method stub

                if (source == this.buttonEffacer) {
                    SQLRow row = this.getListe().getSelectedRow();
                    if (row != null && row.getID() > 1) {

                        final SQLTable table = row.getTable();
                        SQLSelect sel = new SQLSelect(table.getBase());
                        sel.addSelect(table.getKey());
                        sel.setWhere(new Where(table.getField("CODE"), "=", row.getString("CODE")));
                        List<SQLRow> listRow = (List<SQLRow>) table.getBase().getDataSource().execute(sel.asString(), new SQLRowListRSH(table, true));

                        int size = (listRow != null) ? listRow.size() : 0;

                        String message = "Voulez vraiment supprimer ";
                        message += (size > 1) ? "les " + size + " articles référencés " : "l'article référencé ";
                        message += "par le code " + row.getString("CODE") + "?";
                        if (JOptionPane.showConfirmDialog(this, message, "Suppression d'une référence", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

                            for (SQLRow rowInList : listRow) {
                                try {
                                    // elt.archive(rowInList.getID());
                                    SQLRowValues rowVals = rowInList.createEmptyUpdateRow();
                                    rowVals.put("OBSOLETE", Boolean.TRUE);
                                    rowVals.update();
                                } catch (SQLException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } else {
                    super.handleAction(source, evt);
                }

            }
        }) {
            @Override
            protected void setTitle(boolean displayRowCount, boolean displayItemCount) {
                final int rowCount = list.getRowCount();
                String title = "Liste des références";
                title += ", " + getPlural("ligne", rowCount);
                final int total = list.getTotalRowCount();
                if (total != rowCount)
                    title += " / " + total;
                setTitle(title);
            }

        };

        return frame;
    }
}
