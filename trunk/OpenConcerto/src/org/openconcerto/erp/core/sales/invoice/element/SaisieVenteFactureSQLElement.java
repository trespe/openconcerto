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
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.utils.ExceptionHandler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class SaisieVenteFactureSQLElement extends ComptaSQLConfElement {

    public SaisieVenteFactureSQLElement() {
        super("SAISIE_VENTE_FACTURE", "une facture", "factures");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("DATE");
        l.add("NOM");
        l.add("ID_CLIENT");
            l.add("ID_MODE_REGLEMENT");
                l.add("DATE_ENVOI");
            l.add("DATE_REGLEMENT");
        return l;
    }

    @Override
    public synchronized ListSQLRequest getListRequest() {
        return new ListSQLRequest(this.getTable(), this.getListFields()) {
            @Override
            protected void customizeToFetch(SQLRowValues graphToFetch) {
                super.customizeToFetch(graphToFetch);
                graphToFetch.put("ACOMPTE", null);
                graphToFetch.put("COMPLEMENT", null);
                graphToFetch.put("AFFACTURAGE", null);
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

        // Mise à jour du montant facturé dans l'affaire
        if (row.getObject("ID_AFFAIRE") != null && row.getInt("ID_AFFAIRE") > 1) {
            int idAffaire = row.getInt("ID_AFFAIRE");

            SQLRow rowAffaire = Configuration.getInstance().getDirectory().getElement("AFFAIRE").getTable().getRow(idAffaire);
            SQLRowValues rowValsAffaire = rowAffaire.createEmptyUpdateRow();
            long lMontantFacture = ((Number) rowAffaire.getObject("MONTANT_FACTURE")).longValue();

            long lFacture = ((Number) row.getObject("T_HT")).longValue();

            rowValsAffaire.put("MONTANT_FACTURE", new Long(lMontantFacture - lFacture));
            try {
                rowValsAffaire.update();
            } catch (SQLException e) {
                ExceptionHandler.handle("Erreur lors de la mise à jour du montant facturé de l'affaire!");
                e.printStackTrace();
            }
        }

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
}
