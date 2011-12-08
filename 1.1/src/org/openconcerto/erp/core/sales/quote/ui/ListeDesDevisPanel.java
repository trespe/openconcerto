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
 
 package org.openconcerto.erp.core.sales.quote.ui;

import org.openconcerto.erp.core.sales.invoice.ui.DateEnvoiRenderer;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.quote.component.DevisSQLComponent;
import org.openconcerto.erp.core.sales.quote.element.DevisItemSQLElement;
import org.openconcerto.erp.core.sales.quote.element.DevisSQLElement;
import org.openconcerto.erp.core.sales.quote.element.EtatDevisSQLElement;
import org.openconcerto.erp.core.sales.quote.report.DevisXmlSheet;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.IListener;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelColumnPath;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.CollectionMap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

public class ListeDesDevisPanel extends JPanel {

    private JTabbedPane tabbedPane = new JTabbedPane();
    private Map<Integer, ListeAddPanel> map = new HashMap<Integer, ListeAddPanel>();
    private SQLElement eltDevis = Configuration.getInstance().getDirectory().getElement("DEVIS");
    private JButton buttonShow, buttonGen, buttonPrint, buttonFacture, buttonCmd, buttonClone;
    private EditFrame editFrame;

    public ListeDesDevisPanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.NONE;

        // Attente
        ListeAddPanel panelAttente = createPanel(EtatDevisSQLElement.EN_ATTENTE);
        this.map.put(this.tabbedPane.getTabCount(), panelAttente);
        this.tabbedPane.add("En attente", panelAttente);

        // Accepte
        ListeAddPanel panelAccepte = createPanel(EtatDevisSQLElement.ACCEPTE);
        this.map.put(this.tabbedPane.getTabCount(), panelAccepte);
        this.tabbedPane.add("Accepté", panelAccepte);

        // Refuse
        ListeAddPanel panelRefuse = createPanel(EtatDevisSQLElement.REFUSE);
        this.map.put(this.tabbedPane.getTabCount(), panelRefuse);
        this.tabbedPane.add("Refusé", panelRefuse);

        // Bouton generer
        this.buttonGen = new JButton("Générer le document");
        this.add(this.buttonGen, c);
        // this.buttonGen.setToolTipText("Générer");
        // this.buttonGen.setBorder(null);
        this.buttonGen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final ListeAddPanel selectedPanel = ListeDesDevisPanel.this.map.get(ListeDesDevisPanel.this.tabbedPane.getSelectedIndex());
                SQLRow row = selectedPanel.getListe().getSelectedRow();
                DevisXmlSheet sheet = new DevisXmlSheet(row);
                sheet.genere(true, false);
                // genereDoc(row);
            }
        });

        // Bouton voir le document
        this.buttonShow = new JButton("Voir le document");
        c.gridx++;
        this.add(this.buttonShow, c);
        // this.buttonShow.setToolTipText("Voir le document");
        // this.buttonShow.setBorder(null);
        this.buttonShow.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final ListeAddPanel selectedPanel = ListeDesDevisPanel.this.map.get(ListeDesDevisPanel.this.tabbedPane.getSelectedIndex());
                SQLRow row = selectedPanel.getListe().getSelectedRow();
                DevisXmlSheet sheet = new DevisXmlSheet(row);
                sheet.showDocument();
                // showDoc(row);
            }
        });

        // Bouton Impression
        this.buttonPrint = new JButton("Impression");
        c.gridx++;
        this.add(this.buttonPrint, c);
        // this.buttonPrint.setToolTipText("Impression");
        // this.buttonPrint.setBorder(null);
        this.buttonPrint.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final ListeAddPanel selectedPanel = ListeDesDevisPanel.this.map.get(ListeDesDevisPanel.this.tabbedPane.getSelectedIndex());
                SQLRow row = selectedPanel.getListe().getSelectedRow();
                DevisXmlSheet sheet = new DevisXmlSheet(row);
                sheet.fastPrintDocument();
                // printDoc(row);
            }
        });

        // Vers facture
        this.buttonFacture = new JButton("Transfert en facture");
        c.gridx++;
        this.add(this.buttonFacture, c);
        // this.buttonFacture.setToolTipText("Transfert vers facture");
        // this.buttonFacture.setBorder(null);
        this.buttonFacture.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final ListeAddPanel selectedPanel = ListeDesDevisPanel.this.map.get(ListeDesDevisPanel.this.tabbedPane.getSelectedIndex());
                SQLRow row = selectedPanel.getListe().getSelectedRow();
                transfertFacture(row);
            }
        });

        // Vers cmd
        this.buttonCmd = new JButton("Transfert en commande client");
        c.gridx++;
        this.add(this.buttonCmd, c);
        this.buttonCmd.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final ListeAddPanel selectedPanel = ListeDesDevisPanel.this.map.get(ListeDesDevisPanel.this.tabbedPane.getSelectedIndex());
                SQLRow row = selectedPanel.getListe().getSelectedRow();
                transfertCommandeClient(row);
            }
        });

        // Dupliquer
        this.buttonClone = new JButton("Créer à partir de");
        c.gridx++;
        this.add(this.buttonClone, c);
        this.buttonClone.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final ListeAddPanel selectedPanel = ListeDesDevisPanel.this.map.get(ListeDesDevisPanel.this.tabbedPane.getSelectedIndex());
                SQLRow row = selectedPanel.getListe().getSelectedRow();

                if (ListeDesDevisPanel.this.editFrame == null) {
                    SQLElement eltFact = Configuration.getInstance().getDirectory().getElement("DEVIS");
                    ListeDesDevisPanel.this.editFrame = new EditFrame(eltFact, EditPanel.CREATION);
                }

                ((DevisSQLComponent) ListeDesDevisPanel.this.editFrame.getSQLComponent()).loadDevisExistant(row.getID());
                ListeDesDevisPanel.this.editFrame.setVisible(true);
            }
        });

        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(this.tabbedPane, c);

        this.tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                final ListeAddPanel selectedPanel = ListeDesDevisPanel.this.map.get(ListeDesDevisPanel.this.tabbedPane.getSelectedIndex());
                selectedPanel.getListe().getModel().fireTableDataChanged();
            }
        });
        checkButton(-1);
    }

    private ListeAddPanel createPanel(int idFilter) {
        // Filter
        final SQLTableModelSourceOnline lAttente = this.eltDevis.getTableSource(true);
        final SQLTableModelColumnPath dateEnvoiCol;
        if (idFilter == EtatDevisSQLElement.ACCEPTE) {
            dateEnvoiCol = new SQLTableModelColumnPath(this.eltDevis.getTable().getField("DATE_ENVOI"));
            lAttente.getColumns().add(dateEnvoiCol);
            dateEnvoiCol.setRenderer(new DateEnvoiRenderer());
            dateEnvoiCol.setEditable(true);
        } else {
            dateEnvoiCol = null;
        }
        Where wAttente = new Where(this.eltDevis.getTable().getField("ID_ETAT_DEVIS"), "=", idFilter);
        lAttente.getReq().setWhere(wAttente);
        // one config file per idFilter since they haven't the same number of columns
        final ListeAddPanel pane = new ListeAddPanel(this.eltDevis, new IListe(lAttente), "idFilter" + idFilter);

        // Renderer
        JTable table = pane.getListe().getJTable();

        if (idFilter == EtatDevisSQLElement.ACCEPTE) {

            pane.getListe().setSQLEditable(true);
            // Edition des dates d'envois
            TableColumn columnDateEnvoi = pane.getListe().getJTable().getColumnModel().getColumn(table.getColumnCount() - 1);
            columnDateEnvoi.setCellEditor(new org.openconcerto.ui.table.TimestampTableCellEditor());
            final SQLTableModelSourceOnline src = (SQLTableModelSourceOnline) pane.getListe().getModel().getReq();
            for (SQLTableModelColumn column : src.getColumns()) {
                if (column != dateEnvoiCol && column.getClass().isAssignableFrom(SQLTableModelColumnPath.class)) {
                    ((SQLTableModelColumnPath) column).setEditable(false);
                }
            }
        }

        pane.getListe().getJTable().addMouseListener(new MouseSheetXmlListeListener(pane.getListe(), DevisXmlSheet.class) {
            @Override
            public List<AbstractAction> addToMenu() {
                List<AbstractAction> list = new ArrayList<AbstractAction>();
                final SQLRow row = pane.getListe().getSelectedRow();
                // Transfert vers facture
                AbstractAction factureAction = (new AbstractAction("Transfert vers facture") {
                    public void actionPerformed(ActionEvent e) {
                        transfertFacture(row);
                    }
                });

                // Voir le document
                AbstractAction actionTransfertCmd = new AbstractAction("Transférer en commande") {
                    public void actionPerformed(ActionEvent e) {
                        transfertCommande(row);
                    }
                };

                // Transfert vers commande
                AbstractAction commandeAction = (new AbstractAction("Transfert vers commande client") {
                    public void actionPerformed(ActionEvent e) {
                        transfertCommandeClient(row);
                    }
                });

                // Marqué accepté
                AbstractAction accepteAction = (new AbstractAction("Marquer comme accepté") {
                    public void actionPerformed(ActionEvent e) {
                        SQLRowValues rowVals = row.createEmptyUpdateRow();
                        rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.ACCEPTE);
                        try {
                            rowVals.update();
                        } catch (SQLException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                        row.getTable().fireTableModified(row.getID());
                    }
                });

                int type = pane.getListe().getSelectedRow().getInt("ID_ETAT_DEVIS");
                factureAction.setEnabled(type == EtatDevisSQLElement.ACCEPTE);
                commandeAction.setEnabled(type == EtatDevisSQLElement.ACCEPTE);
                if (type == EtatDevisSQLElement.EN_ATTENTE) {
                    list.add(accepteAction);
                }
                list.add(factureAction);
                list.add(commandeAction);
                list.add(actionTransfertCmd);
                return list;
            }
        });

        // activation des boutons
        pane.getListe().addIListener(new IListener() {
            public void selectionId(int id, int field) {
                checkButton(id);
            }
        });

        return pane;
    }

    private void checkButton(int id) {

        if (id > 1) {

            final ListeAddPanel selectedPanel = this.map.get(this.tabbedPane.getSelectedIndex());
            SQLRow row = selectedPanel.getListe().getSelectedRow();

            DevisXmlSheet sheet = new DevisXmlSheet(row);
            int etat = row.getInt("ID_ETAT_DEVIS");
            this.buttonFacture.setEnabled(etat == EtatDevisSQLElement.ACCEPTE);
            this.buttonCmd.setEnabled(etat == EtatDevisSQLElement.ACCEPTE);

            this.buttonPrint.setEnabled(sheet.isFileOOExist());
            this.buttonShow.setEnabled(sheet.isFileOOExist());
            this.buttonGen.setEnabled(true);
            this.buttonClone.setEnabled(true);

        } else {
            this.buttonClone.setEnabled(false);
            this.buttonFacture.setEnabled(false);
            this.buttonCmd.setEnabled(false);
            this.buttonGen.setEnabled(false);
            this.buttonPrint.setEnabled(false);
            this.buttonShow.setEnabled(false);
        }
    }

    /**
     * Transfert en facture
     * 
     * @param row
     */
    private void transfertFacture(SQLRow row) {
        DevisSQLElement elt = (DevisSQLElement) Configuration.getInstance().getDirectory().getElement("DEVIS");
        elt.transfertFacture(row.getID());
    }

    /**
     * Transfert en Commande
     * 
     * @param row
     */
    private void transfertCommandeClient(SQLRow row) {

        DevisSQLElement elt = (DevisSQLElement) Configuration.getInstance().getDirectory().getElement("DEVIS");
        elt.transfertCommandeClient(row.getID());
    }

    private void transfertCommande(SQLRow row) {
        DevisItemSQLElement elt = (DevisItemSQLElement) Configuration.getInstance().getDirectory().getElement("DEVIS_ELEMENT");
        SQLTable tableCmdElt = Configuration.getInstance().getDirectory().getElement("COMMANDE_ELEMENT").getTable();
        SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement("ARTICLE");
        List<SQLRow> rows = row.getReferentRows(elt.getTable());
        CollectionMap<SQLRow, List<SQLRowValues>> map = new CollectionMap<SQLRow, List<SQLRowValues>>();
        for (SQLRow sqlRow : rows) {
            // on récupére l'article qui lui correspond
            SQLRowValues rowArticle = new SQLRowValues(eltArticle.getTable());
            for (SQLField field : eltArticle.getTable().getFields()) {
                if (sqlRow.getTable().getFieldsName().contains(field.getName())) {
                    rowArticle.put(field.getName(), sqlRow.getObject(field.getName()));
                }
            }
            // rowArticle.loadAllSafe(rowEltFact);
            int idArticle = ReferenceArticleSQLElement.getIdForCNM(rowArticle, true);
            SQLRow rowArticleFind = eltArticle.getTable().getRow(idArticle);
            SQLInjector inj = SQLInjector.getInjector(rowArticle.getTable(), tableCmdElt);
            SQLRowValues rowValsElt = new SQLRowValues(inj.createRowValuesFrom(rowArticleFind));

            rowValsElt.put("QTE", sqlRow.getObject("QTE"));
            rowValsElt.put("T_POIDS", rowValsElt.getLong("POIDS") * rowValsElt.getInt("QTE"));
            rowValsElt.put("T_PA_HT", rowValsElt.getLong("PA_HT") * rowValsElt.getInt("QTE"));
            rowValsElt.put("T_PA_TTC", rowValsElt.getLong("T_PA_HT") * (rowValsElt.getForeign("ID_TAXE").getFloat("TAUX") / 100.0 + 1.0));

            map.put(rowArticleFind.getForeignRow("ID_FOURNISSEUR"), rowValsElt);

        }
        MouvementStockSQLElement.createCommandeF(map, row.getForeignRow("ID_TARIF").getForeignRow("ID_DEVISE"));
    }
}
