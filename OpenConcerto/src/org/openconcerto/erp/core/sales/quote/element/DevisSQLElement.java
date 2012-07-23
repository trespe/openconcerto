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
 
 package org.openconcerto.erp.core.sales.quote.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.sales.invoice.component.SaisieVenteFactureSQLComponent;
import org.openconcerto.erp.core.sales.order.component.CommandeClientSQLComponent;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.quote.component.DevisSQLComponent;
import org.openconcerto.erp.core.sales.quote.report.DevisXmlSheet;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.utils.CollectionMap;

import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

public class DevisSQLElement extends ComptaSQLConfElement {

    public static final String TABLENAME = "DEVIS";

    public DevisSQLElement() {
        super(TABLENAME, "un devis", "devis");

        getRowActions().addAll(getDevisRowActions());
    }

    private List<RowAction> getDevisRowActions() {

        List<RowAction> rowsActions = new ArrayList<RowAction>();

        // List<RowAction> list = new ArrayList<RowAction>();
        // Transfert vers facture
        RowAction factureAction = getDevis2FactureAction();

        rowsActions.add(factureAction);

        // Voir le document
        RowAction actionTransfertCmd = getDevis2CmdFournAction();
        rowsActions.add(actionTransfertCmd);

        // Transfert vers commande
        RowAction commandeAction = getDevis2CmdCliAction();

        rowsActions.add(commandeAction);

        RowAction accepteEtCmdAction = getAcceptAndCmdClientAction();
        rowsActions.add(accepteEtCmdAction);

        // Marqué accepté
        RowAction accepteAction = getAcceptAction();

        rowsActions.add(accepteAction);

        // Marqué accepté
        RowAction refuseAction = getRefuseAction();

        rowsActions.add(refuseAction);

        // // Dupliquer
        RowAction cloneAction = getCloneAction();

        rowsActions.add(cloneAction);

        MouseSheetXmlListeListener mouseSheetXmlListeListener = new MouseSheetXmlListeListener(DevisXmlSheet.class);
        mouseSheetXmlListeListener.setGenerateHeader(true);
        mouseSheetXmlListeListener.setShowHeader(true);

        rowsActions.addAll(mouseSheetXmlListeListener.getRowActions());

        return rowsActions;
    }

    public RowAction getCloneAction() {
        return new RowAction(new AbstractAction("Créer à partir de") {
            private EditFrame editFrame;

            public void actionPerformed(ActionEvent e) {
                SQLRow selectedRow = IListe.get(e).getSelectedRow();

                if (this.editFrame == null) {
                    SQLElement eltFact = Configuration.getInstance().getDirectory().getElement("DEVIS");
                    this.editFrame = new EditFrame(eltFact, EditPanel.CREATION);
                }

                ((DevisSQLComponent) this.editFrame.getSQLComponent()).loadDevisExistant(selectedRow.getID());
                this.editFrame.setVisible(true);
            }
        }, true) {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                return (selection != null && selection.size() == 1);
            };
        };
    }

    public RowAction getRefuseAction() {
        return new RowAction(new AbstractAction("Marquer comme refusé") {
            public void actionPerformed(ActionEvent e) {
                SQLRowValues rowVals = IListe.get(e).getSelectedRow().createEmptyUpdateRow();
                rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.REFUSE);
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
    }

    public RowAction getAcceptAction() {
        return new RowAction(new AbstractAction("Marquer comme accepté") {
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
    }

    public RowAction getDevis2FactureAction() {
        return new RowAction(new AbstractAction("Transfert vers facture") {
            public void actionPerformed(ActionEvent e) {
                DevisSQLElement elt = (DevisSQLElement) Configuration.getInstance().getDirectory().getElement("DEVIS");
                elt.transfertFacture(IListe.get(e).getSelectedRow().getID());
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
    }

    public RowAction getDevis2CmdFournAction() {
        return new RowAction(new AbstractAction("Transférer en commande") {
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
    }

    public RowAction getDevis2CmdCliAction() {
        return new RowAction(new AbstractAction("Transfert vers commande client") {
            public void actionPerformed(ActionEvent e) {
                DevisSQLElement elt = (DevisSQLElement) Configuration.getInstance().getDirectory().getElement("DEVIS");
                elt.transfertCommandeClient(IListe.get(e).getSelectedRow().getID());
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
    }

    public RowAction getAcceptAndCmdClientAction() {
        return new RowAction(new AbstractAction("Marquer comme accepté et Transfert en commande client") {
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
                DevisSQLElement elt = (DevisSQLElement) Configuration.getInstance().getDirectory().getElement("DEVIS");
                elt.transfertCommandeClient(selectedRow.getID());
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
    }

    protected List<String> getComboFields() {
        List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        return l;
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

    protected List<String> getListFields() {
        List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("DATE");
        l.add("ID_CLIENT");
        l.add("OBJET");
        l.add("ID_COMMERCIAL");
        l.add("T_HT");
        l.add("T_TTC");
        l.add("INFOS");
        return l;
    }

    @Override
    public CollectionMap<String, String> getShowAs() {

        CollectionMap<String, String> map = new CollectionMap<String, String>();
        map.put(null, "NUMERO");
        return map;
    }

    @Override
    public synchronized ListSQLRequest createListRequest() {
        return new ListSQLRequest(getTable(), getListFields()) {
            @Override
            protected void customizeToFetch(SQLRowValues graphToFetch) {
                super.customizeToFetch(graphToFetch);
                graphToFetch.put("ID_ETAT_DEVIS", null);
            }
        };
    }

    @Override
    protected Set<String> getChildren() {
        Set<String> set = new HashSet<String>();
        set.add("DEVIS_ELEMENT");
        return set;
    }

    @Override
    protected List<String> getPrivateFields() {
        List<String> s = new ArrayList<String>(1);
        s.add("ID_ADRESSE");
        return s;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
            return new DevisSQLComponent(this);
    }

    /**
     * Transfert d'un devis en facture
     * 
     * @param devisID
     */
    public void transfertFacture(int devisID) {

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        EditFrame editFactureFrame = new EditFrame(elt);
        editFactureFrame.setIconImage(new ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());

        SaisieVenteFactureSQLComponent comp = (SaisieVenteFactureSQLComponent) editFactureFrame.getSQLComponent();

        comp.setDefaults();
        comp.loadDevis(devisID);

        editFactureFrame.pack();
        editFactureFrame.setState(JFrame.NORMAL);
        editFactureFrame.setVisible(true);
    }

    // /**
    // * Transfert d'un devis en commande
    // *
    // * @param devisID
    // * @deprecated
    // */
    // public void transfertCommande(int devisID) {
    //
    // SQLElement elt = Configuration.getInstance().getDirectory().getElement("COMMANDE");
    // EditFrame editFactureFrame = new EditFrame(elt);
    // editFactureFrame.setIconImage(new
    // ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());
    //
    // CommandeSQLComponent comp = (CommandeSQLComponent) editFactureFrame.getSQLComponent();
    //
    // comp.setDefaults();
    // comp.loadDevis(devisID);
    //
    // editFactureFrame.pack();
    // editFactureFrame.setState(JFrame.NORMAL);
    // editFactureFrame.setVisible(true);
    // }

    /**
     * Transfert d'un devis en commande
     * 
     * @param devisID
     */
    public void transfertCommandeClient(int devisID) {

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
        EditFrame editFactureFrame = new EditFrame(elt);
        editFactureFrame.setIconImage(new ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());

        CommandeClientSQLComponent comp = (CommandeClientSQLComponent) editFactureFrame.getSQLComponent();

        comp.setDefaults();
        comp.loadDevis(devisID);

        editFactureFrame.pack();
        editFactureFrame.setState(JFrame.NORMAL);
        editFactureFrame.setVisible(true);
    }

}
