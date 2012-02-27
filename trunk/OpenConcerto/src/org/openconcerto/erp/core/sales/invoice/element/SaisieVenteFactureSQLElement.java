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
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.sales.credit.component.AvoirClientSQLComponent;
import org.openconcerto.erp.core.sales.invoice.component.SaisieVenteFactureSQLComponent;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.shipment.component.BonDeLivraisonSQLComponent;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.ExceptionHandler;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class SaisieVenteFactureSQLElement extends ComptaSQLConfElement {

    public static final String TABLENAME = "SAISIE_VENTE_FACTURE";

    public SaisieVenteFactureSQLElement() {
        super(TABLENAME, "une facture", "factures");
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
                graphToFetch.put("COMPLEMENT", null);

                graphToFetch.put("PREVISIONNELLE", null);
                    graphToFetch.grow("ID_MODE_REGLEMENT").put("AJOURS", null).put("LENJOUR", null);
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


        // Mise à jour des stocks
        SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
        SQLSelect sel = new SQLSelect(eltMvtStock.getTable().getBase());
        sel.addSelect(eltMvtStock.getTable().getField("ID"));
        Where w = new Where(eltMvtStock.getTable().getField("IDSOURCE"), "=", row.getID());
        Where w2 = new Where(eltMvtStock.getTable().getField("SOURCE"), "=", getTable().getName());
        sel.setWhere(w.and(w2));

        List l = (List) eltMvtStock.getTable().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());
        if (l != null) {
            for (int i = 0; i < l.size(); i++) {
                Object[] tmp = (Object[]) l.get(i);
                eltMvtStock.archive(((Number) tmp[0]).intValue());
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

    public void transfertAvoir(int idFacture) {
        final SQLElement elt = Configuration.getInstance().getDirectory().getElement("AVOIR_CLIENT");
        final EditFrame editAvoirFrame = new EditFrame(elt);
        editAvoirFrame.setIconImage(new ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());

        final AvoirClientSQLComponent comp = (AvoirClientSQLComponent) editAvoirFrame.getSQLComponent();
        final SQLInjector inject = SQLInjector.getInjector(this.getTable(), elt.getTable());
        comp.select(inject.createRowValuesFrom(idFacture));
        comp.loadFactureItem(idFacture);

        editAvoirFrame.pack();
        editAvoirFrame.setState(JFrame.NORMAL);
        editAvoirFrame.setVisible(true);

    }

    public void transfertCommande(int idFacture) {
        // final SQLElement elt = Configuration.getInstance().getDirectory().getElement("COMMANDE");
        // final EditFrame editAvoirFrame = new EditFrame(elt);
        // editAvoirFrame.setIconImage(new
        // ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());
        //
        // final CommandeSQLComponent comp = (CommandeSQLComponent)
        // editAvoirFrame.getSQLComponent();
        // final SQLInjector inject = SQLInjector.getInjector(this.getTable(), elt.getTable());
        // comp.select(inject.createRowValuesFrom(idFacture));
        // comp.loadFacture(idFacture);
        //
        // editAvoirFrame.pack();
        // editAvoirFrame.setState(JFrame.NORMAL);
        // editAvoirFrame.setVisible(true);

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
            // rowArticle.loadAllSafe(rowEltFact);
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
            if (rowDeviseF != null && !rowDeviseF.isUndefined()) {
                if (rowDeviseF.getID() == rowDeviseHA.getID()) {
                    rowValsElt.put("PA_DEVISE", rowArticleFind.getLong("PA_DEVISE"));
                    rowValsElt.put("PA_DEVISE_T", rowArticleFind.getLong("PA_DEVISE") * rowValsElt.getInt("QTE"));
                    rowValsElt.put("ID_DEVISE", rowDeviseF.getID());
                } else {
                    BigDecimal taux = (BigDecimal) rowDeviseF.getObject("TAUX");
                    rowValsElt.put("PA_DEVISE", taux.multiply(new BigDecimal(rowValsElt.getLong("PA_HT"))).longValue());
                    rowValsElt.put("PA_DEVISE_T", rowValsElt.getLong("PA_DEVISE") * rowValsElt.getInt("QTE"));
                    rowValsElt.put("ID_DEVISE", rowDeviseF.getID());
                }
            }

            rowValsElt.put("T_PA_HT", rowValsElt.getLong("PA_HT") * rowValsElt.getInt("QTE"));

            rowValsElt.put("T_PA_HT", rowValsElt.getLong("PA_HT") * rowValsElt.getInt("QTE"));
            rowValsElt.put("T_PA_TTC", rowValsElt.getLong("T_PA_HT") * (rowValsElt.getForeign("ID_TAXE").getFloat("TAUX") / 100.0 + 1.0));

            map.put(rowArticleFind.getForeignRow("ID_FOURNISSEUR"), rowValsElt);

        }
        MouvementStockSQLElement.createCommandeF(map, rowDeviseF);

    }
}
