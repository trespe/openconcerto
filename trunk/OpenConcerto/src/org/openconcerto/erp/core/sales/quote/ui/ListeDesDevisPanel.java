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
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelColumnPath;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.CollectionMap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
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

        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(this.tabbedPane, c);

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
        // one config file per idFilter since they haven't the same number of
        // columns
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

        MouseSheetXmlListeListener mouseSheetXmlListeListener = new MouseSheetXmlListeListener(DevisXmlSheet.class) {
            @Override
            public List<RowAction> addToMenu() {
                List<RowAction> list = new ArrayList<RowAction>();
                // Transfert vers facture
                RowAction factureAction = new RowAction(new AbstractAction("Transfert vers facture") {
                    public void actionPerformed(ActionEvent e) {
                        transfertFacture(IListe.get(e).getSelectedRow());
                    }
                }, true) {
                    public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                        if (selection != null && selection.size() == 1) {
                            if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.ACCEPTE) {
                                return true;
                            }
                        }
                        return false;
                    };
                };

                list.add(factureAction);

                // Voir le document
                RowAction actionTransfertCmd = new RowAction(new AbstractAction("Transférer en commande") {
                    public void actionPerformed(ActionEvent e) {
                        transfertCommande(IListe.get(e).getSelectedRow());
                    }
                }, false) {
                    public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                        if (selection != null && selection.size() == 1) {
                            if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.ACCEPTE) {
                                return true;
                            }
                        }
                        return false;
                    };
                };
                list.add(actionTransfertCmd);

                // Transfert vers commande
                RowAction commandeAction = new RowAction(new AbstractAction("Transfert vers commande client") {
                    public void actionPerformed(ActionEvent e) {
                        transfertCommandeClient(IListe.get(e).getSelectedRow());
                    }
                }, true) {
                    public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                        if (selection != null && selection.size() == 1) {
                            if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.ACCEPTE) {
                                return true;
                            }
                        }
                        return false;
                    };
                };

                list.add(commandeAction);

                // Marqué accepté
                RowAction accepteAction = new RowAction(new AbstractAction("Marquer comme accepté") {
                    public void actionPerformed(ActionEvent e) {
                        SQLRowValues rowVals = IListe.get(e).getSelectedRow().createEmptyUpdateRow();
                        rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.ACCEPTE);
                        try {
                            rowVals.update();
                        } catch (SQLException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                        IListe.get(e).getSelectedRow().getTable().fireTableModified(IListe.get(e).getSelectedId());
                    }
                }, false) {
                    public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                        if (selection != null && selection.size() == 1) {
                            if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.EN_ATTENTE) {
                                return true;
                            }
                        }
                        return false;
                    };
                };

                list.add(accepteAction);

                RowAction accepteEtCmdAction = new RowAction(new AbstractAction("Marquer comme accepté et Transfert en commande client") {
                    public void actionPerformed(ActionEvent e) {
                        SQLRow selectedRow = IListe.get(e).getSelectedRow();
                        SQLRowValues rowVals = selectedRow.createEmptyUpdateRow();
                        rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.ACCEPTE);
                        try {
                            rowVals.update();
                        } catch (SQLException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                        selectedRow.getTable().fireTableModified(IListe.get(e).getSelectedId());
                        transfertCommandeClient(selectedRow);
                    }
                }, false) {
                    public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                        if (selection != null && selection.size() == 1) {
                            if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.EN_ATTENTE) {
                                return true;
                            }
                        }
                        return false;
                    };
                };
                list.add(accepteEtCmdAction);

                // // Dupliquer
                RowAction cloneAction = new RowAction(new AbstractAction("Créer à partir de") {
                    public void actionPerformed(ActionEvent e) {
                        SQLRow selectedRow = IListe.get(e).getSelectedRow();

                        if (ListeDesDevisPanel.this.editFrame == null) {
                            SQLElement eltFact = Configuration.getInstance().getDirectory().getElement("DEVIS");
                            ListeDesDevisPanel.this.editFrame = new EditFrame(eltFact, EditPanel.CREATION);
                        }

                        ((DevisSQLComponent) ListeDesDevisPanel.this.editFrame.getSQLComponent()).loadDevisExistant(selectedRow.getID());
                        ListeDesDevisPanel.this.editFrame.setVisible(true);
                    }
                }, true) {
                    public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                        return (selection != null && selection.size() == 1);
                    };
                };

                list.add(cloneAction);

                // int type =
                // pane.getListe().getSelectedRow().getInt("ID_ETAT_DEVIS");
                // factureAction.setEnabled(type ==
                // EtatDevisSQLElement.ACCEPTE);
                // commandeAction.setEnabled(type ==
                // EtatDevisSQLElement.ACCEPTE);
                // if (type == EtatDevisSQLElement.EN_ATTENTE) {
                // list.add(accepteAction);
                // }
                // list.add(factureAction);
                // list.add(commandeAction);
                // list.add(actionTransfertCmd);
                return list;
            }
        };
        mouseSheetXmlListeListener.setGenerateHeader(true);
        mouseSheetXmlListeListener.setShowHeader(true);
        pane.getListe().addIListeActions(mouseSheetXmlListeListener.getRowActions());

        // activation des boutons
        // pane.getListe().addIListener(new IListener() {
        // public void selectionId(int id, int field) {
        // checkButton(id);
        // }
        // });
        pane.getListe().setOpaque(false);

        pane.setOpaque(false);
        return pane;
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
            rowValsElt.put("ID_STYLE", sqlRow.getObject("ID_STYLE"));
            rowValsElt.put("QTE", sqlRow.getObject("QTE"));
            rowValsElt.put("T_POIDS", rowValsElt.getLong("POIDS") * rowValsElt.getInt("QTE"));
            rowValsElt.put("T_PA_HT", rowValsElt.getLong("PA_HT") * rowValsElt.getInt("QTE"));
            rowValsElt.put("T_PA_TTC", rowValsElt.getLong("T_PA_HT") * (rowValsElt.getForeign("ID_TAXE").getFloat("TAUX") / 100.0 + 1.0));

            map.put(rowArticleFind.getForeignRow("ID_FOURNISSEUR"), rowValsElt);

        }
        MouvementStockSQLElement.createCommandeF(map, row.getForeignRow("ID_TARIF").getForeignRow("ID_DEVISE"));
    }
}
