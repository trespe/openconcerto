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
 
 package org.openconcerto.erp.core.finance.accounting.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.core.finance.accounting.ui.SuppressionEcrituresPanel;
import org.openconcerto.erp.rights.ComptaUserRight;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.utils.Tuple2;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

public class ListeDesEcrituresAction extends CreateFrameAbstractAction {

    public ListeDesEcrituresAction() {
        super();
        this.putValue(Action.NAME, "Liste des écritures");
    }

    public JFrame createFrame() {
        final long time = Calendar.getInstance().getTimeInMillis();

        final SQLElement element = Configuration.getInstance().getDirectory().getElement("ECRITURE");

        final SQLTableModelSourceOnline src;

        if (!UserManager.getInstance().getCurrentUser().getRights().haveRight(ComptaUserRight.ACCES_NOT_RESCTRICTED_TO_411)) {
            src = element.getTableSource(true);
            src.getReq().setWhere(new Where(element.getTable().getField("COMPTE_NUMERO"), "LIKE", "411%"));
        } else {
            src = element.getTableSource();
        }

        final IListFrame frame = new IListFrame(new ListeAddPanel(element, new IListe(src)) {

            @Override
            protected GridBagConstraints createConstraints() {
                final GridBagConstraints res = super.createConstraints();
                res.gridwidth = GridBagConstraints.REMAINDER;
                res.gridy = 1;
                return res;
            }

            @Override
            protected void handleAction(JButton source, ActionEvent evt) {
                if (source == this.buttonEffacer && getListe().getSelectedRow() != null) {
                    // Si on supprime une ecriture on doit supprimer toutes les ecritures du
                    // mouvement associé
                    System.err.println("Archivage des écritures");
                    // archiveMouvement(row.getInt("ID_MOUVEMENT"));
                    JFrame frame = new PanelFrame(new SuppressionEcrituresPanel(getListe().getSelectedRow().getInt("ID_MOUVEMENT")), "Suppression d'ecritures");
                    frame.pack();
                    frame.setResizable(false);
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                } else {
                    super.handleAction(source, evt);
                }
            }
        });
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getPanel().setSearchFullMode(true);

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 1;

        Map<String, Tuple2<Date, Date>> m = new HashMap<String, Tuple2<Date, Date>>();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date d = cal.getTime();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date d2 = cal.getTime();
        m.put("Mois courant", new Tuple2<Date, Date>(d, cal.getTime()));

        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, -6);
        m.put("Les 6 derniers mois", new Tuple2<Date, Date>(cal.getTime(), d2));

        // final IListFilterDatePanel comp = new IListFilterDatePanel(frame.getPanel().getListe(),
        // element.getTable().getField("DATE"), m);
        //
        // List<SQLField> l = new ArrayList<SQLField>();
        // l.add(element.getTable().getField("DEBIT"));
        // l.add(element.getTable().getField("CREDIT"));

        // IListTotalPanel comp2 = new IListTotalPanel(frame.getPanel().getListe(), l);
        // frame.getPanel().add(comp, c);
        // c.gridx++;
        // frame.getPanel().add(comp2, c);

        // Renderer
        JTable table = frame.getPanel().getListe().getJTable();

        frame.getPanel().setCloneVisible(false);
        frame.getPanel().setAddVisible(false);
        frame.getPanel().setModifyVisible(false);
        frame.getPanel().setReloadVisible(true);
        frame.getPanel().getListe().setSQLEditable(false);

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu menuDroit = new JPopupMenu();
                    menuDroit.add(new AbstractAction("Contrepassation") {
                        public void actionPerformed(ActionEvent event) {
                            EcritureSQLElement.contrePassationPiece(frame.getPanel().getListe().getSelectedId());
                        }
                    });
                    menuDroit.add(new AbstractAction("Voir la source") {
                        public void actionPerformed(ActionEvent event) {

                            SQLRow row = frame.getPanel().getListe().getSelectedRow();

                            MouvementSQLElement.showSource(row.getInt("ID_MOUVEMENT"));
                        }
                    });

                    menuDroit.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
                }
            }
        });

        frame.getPanel().getListe().getModel().invokeLater(new Runnable() {
            public void run() {
                int rowCount = frame.getPanel().getListe().getModel().getRowCount() - 1;
                if (rowCount > 0) {
                    frame.getPanel().getListe().getJTable().setRowSelectionInterval(rowCount, rowCount);
                }
                System.err.println("Load ecritures : " + (Calendar.getInstance().getTimeInMillis() - time) + " ms");
            }
        });

        return frame;
    }
}
