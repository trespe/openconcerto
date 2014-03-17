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
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.sales.invoice.component.SaisieVenteFactureSQLComponent;
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
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

public class DevisSQLElement extends ComptaSQLConfElement {

    public static final String TABLENAME = "DEVIS";

    public DevisSQLElement() {
        this("un devis", "devis");
    }

    public DevisSQLElement(String singular, String plural) {
        super(TABLENAME, singular, plural);
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
        return new RowAction(new AbstractAction() {
            private EditFrame editFrame;

            public void actionPerformed(ActionEvent e) {
                SQLRowAccessor selectedRow = IListe.get(e).getSelectedRow();

                if (this.editFrame == null) {
                    SQLElement eltFact = Configuration.getInstance().getDirectory().getElement("DEVIS");
                    this.editFrame = new EditFrame(eltFact, EditPanel.CREATION);
                }

                ((DevisSQLComponent) this.editFrame.getSQLComponent()).loadDevisExistant(selectedRow.getID());
                this.editFrame.setVisible(true);
            }
        }, true, "sales.quote.clone") {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                return (selection != null && selection.size() == 1);
            };
        };
    }

    public RowAction getRefuseAction() {
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SQLRowValues rowVals = IListe.get(e).getSelectedRow().asRow().createEmptyUpdateRow();
                rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.REFUSE);
                try {
                    rowVals.update();
                } catch (SQLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }, false, "sales.quote.refuse") {
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
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SQLRow selectedRow = IListe.get(e).getSelectedRow().asRow();
                SQLRowValues rowVals = selectedRow.createEmptyUpdateRow();
                rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.ACCEPTE);
                try {
                    rowVals.update();
                } catch (SQLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }, false, "sales.quote.accept") {
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
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                TransfertBaseSQLComponent.openTransfertFrame(IListe.get(e).copySelectedRows(), "SAISIE_VENTE_FACTURE");
            }
        }, true, "sales.quote.create.invoice") {
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
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                final SQLRow selectedRow = IListe.get(e).fetchSelectedRow();
                ComptaPropsConfiguration.getInstanceCompta().getNonInteractiveSQLExecutor().execute(new Runnable() {

                    @Override
                    public void run() {
                        transfertCommande(selectedRow);

                    }
                });

            }
        }, false, "sales.quote.create.supplier.order") {
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
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                final List<SQLRowValues> copySelectedRows = IListe.get(e).copySelectedRows();
                transfertCommandeClient(copySelectedRows);
            }

        }, true, "sales.quote.create.customer.order") {
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
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SQLRow selectedRow = IListe.get(e).fetchSelectedRow();
                SQLRowValues rowVals = selectedRow.createEmptyUpdateRow();
                rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.ACCEPTE);
                try {
                    rowVals.update();
                } catch (SQLException e1) {
                    ExceptionHandler.handle("Erreur la de la mise à jour de l'état du devis!", e1);

                }
                transfertCommandeClient(IListe.get(e).copySelectedRows());
            }
        }, false, "sales.quote.accept.create.customer.order") {
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

    private void transfertCommandeClient(final List<SQLRowValues> copySelectedRows) {

        SwingWorker<Boolean, Object> worker = new SwingWorker<Boolean, Object>() {
            @Override
            protected Boolean doInBackground() throws Exception {

                final SQLTable tableTransfert = getTable().getTable("TR_DEVIS");
                SQLRowValues rowVals = new SQLRowValues(tableTransfert);
                rowVals.put("ID_DEVIS", new SQLRowValues(getTable()).put("NUMERO", null));
                rowVals.put("ID_COMMANDE", null);
                rowVals.put("ID", null);

                final List<Number> lID = new ArrayList<Number>();
                for (SQLRowValues sqlRowValues : copySelectedRows) {
                    lID.add(sqlRowValues.getID());
                }

                SQLRowValuesListFetcher fetch = SQLRowValuesListFetcher.create(rowVals);
                fetch.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

                    @Override
                    public SQLSelect transformChecked(SQLSelect input) {
                        Where w = new Where(tableTransfert.getField("ID_DEVIS"), lID);
                        w = w.and(new Where(tableTransfert.getField("ID_COMMANDE_CLIENT"), "IS NOT", (Object) null));
                        input.setWhere(w);
                        return input;
                    }
                });

                List<SQLRowValues> rows = fetch.fetch();
                if (rows != null && rows.size() > 0) {
                    String numero = "";

                    for (SQLRowValues sqlRow : rows) {
                        numero += sqlRow.getForeign("ID_DEVIS").getString("NUMERO") + " ,";
                    }

                    numero = numero.substring(0, numero.length() - 2);
                    String label = "Attention ";
                    if (rows.size() > 1) {
                        label += " les devis " + numero + " ont déjà été transféré en commande!";
                    } else {
                        label += " le devis " + numero + " a déjà été transféré en commande!";
                    }
                    label += "\n Voulez vous continuer?";

                    int ans = JOptionPane.showConfirmDialog(null, label, "Transfert devis en commande", JOptionPane.YES_NO_OPTION);
                    if (ans == JOptionPane.NO_OPTION) {
                        return Boolean.FALSE;
                    }

                }
                return Boolean.TRUE;

            }

            @Override
            protected void done() {

                try {
                    Boolean b = get();
                    if (b != null && b) {
                        TransfertBaseSQLComponent.openTransfertFrame(copySelectedRows, "COMMANDE_CLIENT");
                    }
                } catch (Exception e) {
                    ExceptionHandler.handle("Erreur lors du transfert des devis en commande!", e);
                }
                super.done();
            }
        };
        worker.execute();
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
            rowValsElt.put("T_PA_HT", ((BigDecimal) rowValsElt.getObject("PA_HT")).multiply(new BigDecimal(rowValsElt.getInt("QTE"), MathContext.DECIMAL128)));
            rowValsElt
                    .put("T_PA_TTC", ((BigDecimal) rowValsElt.getObject("T_PA_HT")).multiply(new BigDecimal(rowValsElt.getForeign("ID_TAXE").getFloat("TAUX") / 100.0 + 1.0), MathContext.DECIMAL128));

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

}
