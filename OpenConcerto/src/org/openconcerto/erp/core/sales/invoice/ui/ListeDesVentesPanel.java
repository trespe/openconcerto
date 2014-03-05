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
 
 package org.openconcerto.erp.core.sales.invoice.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.common.ui.ListeViewPanel;
import org.openconcerto.erp.core.finance.accounting.ui.ListeGestCommEltPanel;
import org.openconcerto.erp.core.sales.invoice.component.SaisieVenteFactureSQLComponent;
import org.openconcerto.erp.core.sales.pos.ui.TextAreaTicketPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelColumnPath;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.PanelFrame;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.TableSorter;
import org.openconcerto.utils.cc.IClosure;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

public class ListeDesVentesPanel extends JPanel implements ActionListener {

    private ListeGestCommEltPanel listeFact;
    private JButton buttonEnvoye, buttonRegle, buttonDupliquer;
    private static SQLElement eltClient = Configuration.getInstance().getDirectory().getElement(((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("CLIENT"));
    JLabelBold textField = new JLabelBold("0");
    JLabelBold textField2 = new JLabelBold("0");

    public ListeDesVentesPanel() {

        super();

        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 1, 2);
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;

        JTabbedPane tabbedPane = new JTabbedPane();

        final SQLElement elementVF = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        // tab Vente facture
        final SQLElement eltFacture = elementVF;
        final SQLTableModelSourceOnline src = eltFacture.getTableSource(true);
        // Filter
        Where wPrev = new Where(eltFacture.getTable().getField("PREVISIONNELLE"), "=", Boolean.FALSE);
        if (src.getReq().getWhere() != null) {
            wPrev = wPrev.and(src.getReq().getWhere());
        }
        src.getReq().setWhere(wPrev);

        final ListeFactureRenderer rend = new ListeFactureRenderer();
        for (SQLTableModelColumn column : src.getColumns()) {
            if (column.getClass().isAssignableFrom(SQLTableModelColumnPath.class)) {
                ((SQLTableModelColumnPath) column).setEditable(false);
            }
            column.setRenderer(rend);
        }

        final SQLTableModelColumn dateEnvoiCol = src.getColumn(eltFacture.getTable().getField("DATE_ENVOI"));
            ((SQLTableModelColumnPath) dateEnvoiCol).setEditable(true);
        final SQLTableModelColumn dateReglCol = src.getColumn(eltFacture.getTable().getField("DATE_REGLEMENT"));
        if (dateReglCol != null)
            ((SQLTableModelColumnPath) dateReglCol).setEditable(true);

            // Edition des dates d'envois
            dateEnvoiCol.setColumnInstaller(new IClosure<TableColumn>() {
                @Override
                public void executeChecked(TableColumn columnDateEnvoi) {
                    columnDateEnvoi.setCellEditor(new org.openconcerto.ui.table.TimestampTableCellEditor());
                    columnDateEnvoi.setCellRenderer(new DateEnvoiRenderer());
                }
            });

        // Edition des dates de reglement
        if (dateReglCol != null) {
            dateReglCol.setColumnInstaller(new IClosure<TableColumn>() {
                @Override
                public void executeChecked(TableColumn columnDateReglement) {
                    columnDateReglement.setCellEditor(new org.openconcerto.ui.table.TimestampTableCellEditor());
                    columnDateReglement.setCellRenderer(new DateEnvoiRenderer());
                }
            });
        }

        this.listeFact = new ListeGestCommEltPanel(eltFacture, new IListe(src), true);
        this.listeFact.setOpaque(false);
        this.listeFact.getListe().setSQLEditable(true);
        final JTable tableFact = this.listeFact.getListe().getJTable();
        final SQLTableModelColumn numeroCol = src.getColumn(eltFacture.getTable().getField("NUMERO"));
        ((TableSorter) tableFact.getModel()).setSortingStatus(src.getColumns().indexOf(numeroCol), TableSorter.ASCENDING);

        JPanel panelFacture = new JPanel(new GridBagLayout());
        GridBagConstraints cFacture = new DefaultGridBagConstraints();
        this.buttonEnvoye = new JButton("Facture envoyée");
        cFacture.fill = GridBagConstraints.NONE;
        this.buttonEnvoye.addActionListener(this);
        this.buttonEnvoye.setEnabled(false);
        panelFacture.setOpaque(false);
        panelFacture.add(this.buttonEnvoye, cFacture);
        // Reglé
        this.buttonRegle = new JButton("Facture réglée");
        this.buttonRegle.addActionListener(this);
        this.buttonRegle.setEnabled(false);
        cFacture.gridx++;
        panelFacture.add(this.buttonRegle, cFacture);
        //
        this.buttonDupliquer = new JButton("Créer à partir de");
        cFacture.fill = GridBagConstraints.NONE;
        this.buttonDupliquer.addActionListener(this);
        this.buttonDupliquer.setEnabled(false);
        cFacture.gridx++;
        panelFacture.add(this.buttonDupliquer, cFacture);

        cFacture.gridy++;
        cFacture.gridx = 0;
        cFacture.weighty = 1;
        cFacture.weightx = 1;
        cFacture.gridwidth = GridBagConstraints.REMAINDER;
        cFacture.fill = GridBagConstraints.BOTH;
        panelFacture.add(this.listeFact, cFacture);

        List<SQLField> l = new ArrayList<SQLField>();
        l.add(eltFacture.getTable().getField("T_HT"));
        l.add(eltFacture.getTable().getField("T_TTC"));
        final IListTotalPanel total = new IListTotalPanel(this.listeFact.getListe(), l);
        cFacture.weighty = 0;
        cFacture.fill = GridBagConstraints.NONE;
        cFacture.gridy++;
        cFacture.anchor = GridBagConstraints.EAST;
        total.setOpaque(false);
        panelFacture.add(total, cFacture);


        IListFilterDatePanel filterDate = new IListFilterDatePanel(this.listeFact.getListe(), eltFacture.getTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());
        cFacture.weighty = 0;
        cFacture.fill = GridBagConstraints.HORIZONTAL;
        cFacture.gridy++;
        filterDate.setOpaque(false);
        panelFacture.add(filterDate, cFacture);
        tabbedPane.add("Ventes avec facture", panelFacture);

        this.listeFact.getListe().addIListener(new org.openconcerto.sql.view.IListener() {

            public void selectionId(int id, int field) {
                ListeDesVentesPanel.this.buttonEnvoye.setEnabled(id > 1);
                ListeDesVentesPanel.this.buttonRegle.setEnabled(id > 1);
                if (id > 1) {
                    final ITableModel model = ListeDesVentesPanel.this.listeFact.getListe().getModel();

                    SQLRowAccessor r = model.getRow(model.indexFromID(id)).getRow();
                    ListeDesVentesPanel.this.buttonDupliquer.setEnabled(!r.getBoolean("PARTIAL") && !r.getBoolean("SOLDE"));
                } else {
                    ListeDesVentesPanel.this.buttonDupliquer.setEnabled(false);
                }
            }
        });


            {
                // Tab Vente caisse
                ListeViewPanel panelTicket = new ListeViewPanel(Configuration.getInstance().getDirectory().getElement("TICKET_CAISSE")) {
                    @Override
                    protected void handleAction(JButton source, ActionEvent evt) {
                        if (source == this.buttonModifier) {
                            new PanelFrame(new TextAreaTicketPanel(this.getListe().fetchSelectedRow()), "Ticket").setVisible(true);
                        } else {
                            super.handleAction(source, evt);
                        }
                    }
                };

                JPanel panel = new JPanel(new GridBagLayout());
                GridBagConstraints cc = new DefaultGridBagConstraints();
                cc.weightx = 1;
                cc.weighty = 1;
                cc.fill = GridBagConstraints.BOTH;
                panel.add(panelTicket, cc);

                final List<SQLField> l2 = new ArrayList<SQLField>();
                l2.add(panelTicket.getElement().getTable().getField("TOTAL_HT"));
                l2.add(panelTicket.getElement().getTable().getField("TOTAL_TTC"));
                final IListTotalPanel total2 = new IListTotalPanel(panelTicket.getListe(), l2);
                cc.weighty = 0;
                cc.fill = GridBagConstraints.NONE;
                cc.gridy++;
                cc.anchor = GridBagConstraints.EAST;
                total2.setOpaque(false);
                panel.add(total2, cc);

                IListFilterDatePanel filterDate2 = new IListFilterDatePanel(panelTicket.getListe(), panelTicket.getElement().getTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());
                cc.weighty = 0;
                cc.fill = GridBagConstraints.HORIZONTAL;
                cc.gridy++;
                filterDate2.setOpaque(false);
                panel.add(filterDate2, cc);

                tabbedPane.add("Ventes caisse", panel);

            }
            // Tab Vente comptoir
            {
                final ListeGestCommEltPanel listeVC = new ListeGestCommEltPanel(Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_COMPTOIR"), true);
                listeVC.getListe().setSQLEditable(false);
                listeVC.setOpaque(false);

                final JTable table = listeVC.getListe().getJTable();
                for (int i = 0; i < table.getColumnCount(); i++) {
                    if (table.getColumnClass(i) == Long.class || table.getColumnClass(i) == BigInteger.class) {
                        table.getColumnModel().getColumn(i).setCellRenderer(rend);
                    }
                }

                JPanel panelComptoir = new JPanel(new GridBagLayout());
                GridBagConstraints cc = new DefaultGridBagConstraints();
                cc.weightx = 1;
                cc.weighty = 1;
                cc.fill = GridBagConstraints.BOTH;
                panelComptoir.add(listeVC, cc);

                final List<SQLField> l2 = new ArrayList<SQLField>();
                l2.add(listeVC.getElement().getTable().getField("MONTANT_HT"));
                l2.add(listeVC.getElement().getTable().getField("MONTANT_TTC"));
                final IListTotalPanel total2 = new IListTotalPanel(listeVC.getListe(), l2);
                cc.weighty = 0;
                cc.fill = GridBagConstraints.NONE;
                cc.gridy++;
                cc.anchor = GridBagConstraints.EAST;
                total2.setOpaque(false);
                panelComptoir.add(total2, cc);

                IListFilterDatePanel filterDate2 = new IListFilterDatePanel(listeVC.getListe(), listeVC.getElement().getTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());
                cc.weighty = 0;
                cc.fill = GridBagConstraints.HORIZONTAL;
                cc.gridy++;
                filterDate2.setOpaque(false);
                panelComptoir.add(filterDate2, cc);

                tabbedPane.add("Ventes comptoir", panelComptoir);
            }
        this.add(tabbedPane, c);
    }

    private EditFrame editFrame;

    public void actionPerformed(ActionEvent e) {

        final SQLRow selectedRow = this.listeFact.getListe().getSelectedRow().asRow();
        if (e.getSource() == this.buttonEnvoye) {
            SQLRowValues rowVals = selectedRow.createEmptyUpdateRow();
            rowVals.put("DATE_ENVOI", new Date());
            try {
                rowVals.update();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        if (e.getSource() == this.buttonRegle) {
            SQLRowValues rowVals = selectedRow.createEmptyUpdateRow();
            rowVals.put("DATE_REGLEMENT", new Date());
            try {
                rowVals.update();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } else {
            if (e.getSource() == this.buttonDupliquer) {
                if (this.editFrame == null) {
                    SQLElement eltFact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
                    this.editFrame = new EditFrame(eltFact, EditPanel.CREATION);
                }

                ((SaisieVenteFactureSQLComponent) this.editFrame.getSQLComponent()).loadFactureExistante(selectedRow.getID());
                this.editFrame.setVisible(true);
            }
        }
    }
}
