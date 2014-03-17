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
 
 package org.openconcerto.erp.core.sales.invoice.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.sales.account.VenteFactureSituationSQLComponent;
import org.openconcerto.erp.core.sales.account.VenteFactureSoldeSQLComponent;
import org.openconcerto.erp.core.sales.invoice.component.SaisieVenteFactureSQLComponent;
import org.openconcerto.erp.core.sales.invoice.report.VenteFactureXmlSheet;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.shipment.component.BonDeLivraisonSQLComponent;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.erp.generationEcritures.GenerationMvtRetourNatexis;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.erp.rights.NXRights;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.sql.view.EditPanelListener;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.i18n.TranslationManager;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

// Depuis le 1er juillet 2003, la règlementation fiscale impose la délivrance d'une facture pour
// tous les versements d'acomptes, même lorsqu'ils ne donnent pas lieu à exigibilité de la TVA
// (article 289 I -1-c du CGI).
// Avant la loi de finances rectificative pour 2002, il n'y avait obligation de délivrer une facture
// pour les acomptes que lorsque la TVA était exigible sur ces versements. Depuis l'entrée en
// vigueur de cette loi, initialement fixée au 1er juillet 2003, et reportée par tolérance
// administrative au 1er janvier 2004, il faut désormais délivrer une facture pour tous les acomptes
// perçus.
// L'obligation nouvelle de facturer tous les versements d'acomptes ne modifie pas les règles
// d'exigibilité de la TVA.
// La date du versement de l'acompte doit être indiquée sur la facture d'acompte si elle est
// différente de la date de délivrance de cette facture, et si elle est connue à cette date.

// La facture d'acompte peut ne pas mentionner l'ensemble des mentions obligatoires lorsque les
// informations nécessaires à son établissement ne sont pas connues au moment de son émission (par
// exemple, quantité ou prix exact du produit).
public class SaisieVenteFactureSQLElement extends ComptaSQLConfElement {

    public static final String TABLENAME = "SAISIE_VENTE_FACTURE";

    public SaisieVenteFactureSQLElement() {
        super(TABLENAME, "une facture", "factures");

        addComponentFactory(VenteFactureSituationSQLComponent.ID, new ITransformer<Tuple2<SQLElement, String>, SQLComponent>() {

            @Override
            public SQLComponent transformChecked(Tuple2<SQLElement, String> input) {

                return new VenteFactureSituationSQLComponent(SaisieVenteFactureSQLElement.this);
            }
        });
        addComponentFactory(VenteFactureSoldeSQLComponent.ID, new ITransformer<Tuple2<SQLElement, String>, SQLComponent>() {

            @Override
            public SQLComponent transformChecked(Tuple2<SQLElement, String> input) {

                return new VenteFactureSoldeSQLComponent(SaisieVenteFactureSQLElement.this);
            }
        });

        final boolean affact = UserManager.getInstance().getCurrentUser().getRights().haveRight(NXRights.ACCES_RETOUR_AFFACTURAGE.getCode());
        List<RowAction> l = new ArrayList<RowAction>(5);
            PredicateRowAction actionBL = new PredicateRowAction(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    TransfertBaseSQLComponent.openTransfertFrame(IListe.get(e).copySelectedRows(), "BON_DE_LIVRAISON");
                }
            }, false, "sales.invoice.create.delivery");
            actionBL.setPredicate(IListeEvent.getSingleSelectionPredicate());
            l.add(actionBL);
        PredicateRowAction actionAvoir = new PredicateRowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                TransfertBaseSQLComponent.openTransfertFrame(IListe.get(e).copySelectedRows(), "AVOIR_CLIENT");
            }
        }, false, "sales.invoice.create.credit");
        actionAvoir.setPredicate(IListeEvent.getSingleSelectionPredicate());
        l.add(actionAvoir);
        RowAction actionClone = new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                SQLElement eltFact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
                EditFrame editFrame = new EditFrame(eltFact, EditPanel.CREATION);

                ((SaisieVenteFactureSQLComponent) editFrame.getSQLComponent()).loadFactureExistante(IListe.get(e).getSelectedId());
                editFrame.setVisible(true);
            }
        }, false, "sales.invoice.clone") {
            public boolean enabledFor(IListeEvent evt) {
                List<SQLRowAccessor> l = evt.getSelectedRows();
                if (l != null && l.size() == 1) {
                    SQLRowAccessor r = l.get(0);
                    return !r.getBoolean("PARTIAL") && !r.getBoolean("SOLDE");
                }
                return false;
            }
        };

        l.add(actionClone);
        getRowActions().addAll(l);


        PredicateRowAction actionClient = new PredicateRowAction(new AbstractAction("Détails client") {
            EditFrame edit;
            private SQLElement eltClient = Configuration.getInstance().getDirectory().getElement(((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("CLIENT"));

            public void actionPerformed(ActionEvent e) {
                if (edit == null) {
                    edit = new EditFrame(eltClient, EditMode.READONLY);
                }
                edit.selectionId(IListe.get(e).fetchSelectedRow().getInt("ID_CLIENT"));
                edit.setVisible(true);
            }
        }, false, "sales.invoice.info.show");
        actionClient.setPredicate(IListeEvent.getSingleSelectionPredicate());
        getRowActions().add(actionClient);

        PredicateRowAction actionCommande = new PredicateRowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SaisieVenteFactureSQLElement elt = (SaisieVenteFactureSQLElement) Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
                elt.transfertCommande(IListe.get(e).getSelectedId());
            }
        }, false, "sales.invoice.create.supplier.order");
        actionCommande.setPredicate(IListeEvent.getSingleSelectionPredicate());
        getRowActions().add(actionCommande);

        MouseSheetXmlListeListener mouseSheetXmlListeListener = new MouseSheetXmlListeListener(VenteFactureXmlSheet.class);
        getRowActions().addAll(mouseSheetXmlListeListener.getRowActions());
        // this.frame.getPanel().getListe().addRowActions(mouseListener.getRowActions());

    }

    @Override
    public CollectionMap<String, String> getShowAs() {
        CollectionMap<String, String> map = new CollectionMap<String, String>();
        map.put(null, "NUMERO");
        return map;
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("DATE");
        l.add("NOM");
        l.add("ID_CLIENT");
            l.add("ID_MODE_REGLEMENT");
        l.add("ID_COMMERCIAL");

        l.add("T_HA");
        l.add("T_HT");
        l.add("T_TTC");
        l.add("INFOS");

                l.add("DATE_ENVOI");
            l.add("DATE_REGLEMENT");
        return l;
    }

    @Override
    public synchronized ListSQLRequest createListRequest() {
        return new ListSQLRequest(this.getTable(), this.getListFields()) {
            @Override
            protected void customizeToFetch(SQLRowValues graphToFetch) {
                super.customizeToFetch(graphToFetch);
                graphToFetch.put("ACOMPTE", null);
                graphToFetch.put("PARTIAL", null);
                graphToFetch.put("SOLDE", null);
                graphToFetch.put("COMPLEMENT", null);

                graphToFetch.put("PREVISIONNELLE", null);
                    graphToFetch.grow("ID_MODE_REGLEMENT").put("AJOURS", null).put("LENJOUR", null);
                SQLRowValues value = new SQLRowValues(graphToFetch.getTable().getTable("MOUVEMENT"));
                value.put("ID_PIECE", null);
                graphToFetch.put("ID_MOUVEMENT", value);
                graphToFetch.put("T_AVOIR_TTC", null);
            }
        };
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        return l;
    }

    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_MODE_REGLEMENT");
        return l;
    }

    @Override
    protected Set<String> getChildren() {
        Set<String> set = new HashSet<String>();
        set.add("SAISIE_VENTE_FACTURE_ELEMENT");
        return set;
    }

    @Override
    public Set<String> getReadOnlyFields() {
        Set<String> s = new HashSet<String>(1);
        s.add("CONTROLE_TECHNIQUE");
        return s;
    }

    @Override
    public Set<String> getInsertOnlyFields() {
        Set<String> s = new HashSet<String>(1);
        s.add("ACOMPTE");
        return s;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new SaisieVenteFactureSQLComponent();
    }

    @Override
    protected void archive(SQLRow row, boolean cutLinks) throws SQLException {

        // On retire l'avoir
        if (row.getInt("ID_AVOIR_CLIENT") > 1) {
            SQLElement eltAvoir = Configuration.getInstance().getDirectory().getElement("AVOIR_CLIENT");
            SQLRow rowAvoir = eltAvoir.getTable().getRow(row.getInt("ID_AVOIR_CLIENT"));

            Long montantSolde = (Long) rowAvoir.getObject("MONTANT_SOLDE");

            Long avoirTTC = (Long) row.getObject("T_AVOIR_TTC");

            long montant = montantSolde - avoirTTC;
            if (montant < 0) {
                montant = 0;
            }

            SQLRowValues rowVals = rowAvoir.createEmptyUpdateRow();

            // Soldé
            rowVals.put("SOLDE", Boolean.FALSE);
            rowVals.put("MONTANT_SOLDE", montant);
            Long restant = (Long) rowAvoir.getObject("MONTANT_TTC") - montantSolde;
            rowVals.put("MONTANT_RESTANT", restant);
            try {
                rowVals.update();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        super.archive(row, cutLinks);

        SQLPreferences prefs = new SQLPreferences(getTable().getDBRoot());
        if (prefs.getBoolean(GestionArticleGlobalPreferencePanel.STOCK_FACT, true)) {

            // Mise à jour des stocks
            SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
            SQLSelect sel = new SQLSelect();
            sel.addSelect(eltMvtStock.getTable().getField("ID"));
            Where w = new Where(eltMvtStock.getTable().getField("IDSOURCE"), "=", row.getID());
            Where w2 = new Where(eltMvtStock.getTable().getField("SOURCE"), "=", getTable().getName());
            sel.setWhere(w.and(w2));

            @SuppressWarnings("rawtypes")
            List l = (List) eltMvtStock.getTable().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());
            if (l != null) {
                for (int i = 0; i < l.size(); i++) {
                    Object[] tmp = (Object[]) l.get(i);
                    eltMvtStock.archive(((Number) tmp[0]).intValue());
                }
            }
        }
    }

    public void transfertBL(int idFacture) {
        final SQLElement elt = Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON");
        final EditFrame editAvoirFrame = new EditFrame(elt);
        editAvoirFrame.setIconImage(new ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());

        final BonDeLivraisonSQLComponent comp = (BonDeLivraisonSQLComponent) editAvoirFrame.getSQLComponent();
        final SQLInjector inject = SQLInjector.getInjector(this.getTable(), elt.getTable());
        SQLRowValues createRowValuesFrom = inject.createRowValuesFrom(idFacture);
        SQLRow rowFacture = getTable().getRow(idFacture);
        String string = rowFacture.getString("NOM");
        createRowValuesFrom.put("NOM", string + (string.trim().length() == 0 ? "" : ", ") + rowFacture.getString("NUMERO"));
        comp.select(createRowValuesFrom);
        // comp.loadFactureItem(idFacture);

        editAvoirFrame.pack();
        editAvoirFrame.setState(JFrame.NORMAL);
        editAvoirFrame.setVisible(true);

    }

    /**
     * Transfert en commande fournisseur
     * */
    public void transfertCommande(int idFacture) {

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");
        SQLTable tableCmdElt = Configuration.getInstance().getDirectory().getElement("COMMANDE_ELEMENT").getTable();
        SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement("ARTICLE");
        List<SQLRow> rows = getTable().getRow(idFacture).getReferentRows(elt.getTable());
        CollectionMap<SQLRow, List<SQLRowValues>> map = new CollectionMap<SQLRow, List<SQLRowValues>>();
        SQLRow rowDeviseF = null;
        for (SQLRow sqlRow : rows) {
            // on récupére l'article qui lui correspond
            SQLRowValues rowArticle = new SQLRowValues(eltArticle.getTable());
            for (SQLField field : eltArticle.getTable().getFields()) {
                if (sqlRow.getTable().getFieldsName().contains(field.getName())) {
                    rowArticle.put(field.getName(), sqlRow.getObject(field.getName()));
                }
            }

            int idArticle = ReferenceArticleSQLElement.getIdForCNM(rowArticle, true);
            SQLRow rowArticleFind = eltArticle.getTable().getRow(idArticle);
            SQLInjector inj = SQLInjector.getInjector(rowArticle.getTable(), tableCmdElt);
            SQLRowValues rowValsElt = new SQLRowValues(inj.createRowValuesFrom(rowArticleFind));
            rowValsElt.put("ID_STYLE", sqlRow.getObject("ID_STYLE"));
            rowValsElt.put("QTE", sqlRow.getObject("QTE"));
            rowValsElt.put("T_POIDS", rowValsElt.getLong("POIDS") * rowValsElt.getInt("QTE"));

            // gestion de la devise
            rowDeviseF = sqlRow.getForeignRow("ID_DEVISE");
            SQLRow rowDeviseHA = rowArticleFind.getForeignRow("ID_DEVISE_HA");
            BigDecimal qte = new BigDecimal(rowValsElt.getInt("QTE"));
            if (rowDeviseF != null && !rowDeviseF.isUndefined()) {
                if (rowDeviseF.getID() == rowDeviseHA.getID()) {
                    rowValsElt.put("PA_DEVISE", rowArticleFind.getObject("PA_DEVISE"));
                    rowValsElt.put("PA_DEVISE_T", ((BigDecimal) rowArticleFind.getObject("PA_DEVISE")).multiply(qte, MathContext.DECIMAL128));
                    rowValsElt.put("ID_DEVISE", rowDeviseF.getID());
                } else {
                    BigDecimal taux = (BigDecimal) rowDeviseF.getObject("TAUX");
                    rowValsElt.put("PA_DEVISE", taux.multiply((BigDecimal) rowValsElt.getObject("PA_HT")));
                    rowValsElt.put("PA_DEVISE_T", ((BigDecimal) rowValsElt.getObject("PA_DEVISE")).multiply(qte, MathContext.DECIMAL128));
                    rowValsElt.put("ID_DEVISE", rowDeviseF.getID());
                }
            }

            BigDecimal prixHA = (BigDecimal) rowValsElt.getObject("PA_HT");
            rowValsElt.put("T_PA_HT", prixHA.multiply(qte, MathContext.DECIMAL128));

            rowValsElt.put("T_PA_HT", prixHA.multiply(qte, MathContext.DECIMAL128));
            rowValsElt
                    .put("T_PA_TTC", ((BigDecimal) rowValsElt.getObject("T_PA_HT")).multiply(new BigDecimal(rowValsElt.getForeign("ID_TAXE").getFloat("TAUX") / 100.0 + 1.0), MathContext.DECIMAL128));

            map.put(rowArticleFind.getForeignRow("ID_FOURNISSEUR"), rowValsElt);

        }
        MouvementStockSQLElement.createCommandeF(map, rowDeviseF);

    }
}
