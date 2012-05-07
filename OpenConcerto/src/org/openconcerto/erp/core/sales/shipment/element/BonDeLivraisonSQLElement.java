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
 
 package org.openconcerto.erp.core.sales.shipment.element;

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.sales.invoice.component.SaisieVenteFactureSQLComponent;
import org.openconcerto.erp.core.sales.shipment.component.BonDeLivraisonSQLComponent;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.ui.preferences.DefaultProps;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class BonDeLivraisonSQLElement extends ComptaSQLConfElement {

    // TODO bug rafraichissement on resize frame
    // TODO afficher uniquement les factures non livrees dans la combo
    // MAYBE mettre un niceCellRenderer dans les rowValuesTable

    public BonDeLivraisonSQLElement() {
        super("BON_DE_LIVRAISON", "un bon de livraison", "Bons de livraison");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("DATE");
        l.add("ID_CLIENT");
        DefaultProps props = DefaultNXProps.getInstance();
        Boolean b = props.getBooleanValue("ArticleShowPoids");
        if (b) {
            l.add("TOTAL_POIDS");
        }
        l.add("NOM");
        l.add("INFOS");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("DATE");
        return l;
    }

    @Override
    protected Set<String> getChildren() {
        Set<String> set = new HashSet<String>();
        set.add("BON_DE_LIVRAISON_ELEMENT");
        return set;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BonDeLivraisonSQLComponent();
    }

    @Override
    public synchronized ListSQLRequest createListRequest() {
        return new ListSQLRequest(this.getTable(), this.getListFields()) {
            @Override
            protected void customizeToFetch(SQLRowValues graphToFetch) {
                super.customizeToFetch(graphToFetch);
                graphToFetch.put("ID_SAISIE_VENTE_FACTURE", null);
            }
        };
    }

    /**
     * Transfert d'un BL en facture
     * 
     * @param blID
     */
    public void transfertFacture(int blID) {

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        EditFrame editFactureFrame = new EditFrame(elt);
        editFactureFrame.setIconImage(new ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());

        SaisieVenteFactureSQLComponent comp = (SaisieVenteFactureSQLComponent) editFactureFrame.getSQLComponent();

        comp.setDefaults();
        comp.loadBonItems(blID);
        editFactureFrame.pack();
        editFactureFrame.setState(JFrame.NORMAL);
        editFactureFrame.setVisible(true);
    }

    @Override
    protected void archive(SQLRow row, boolean cutLinks) throws SQLException {
        // TODO Raccord de méthode auto-généré
        super.archive(row, cutLinks);

        SQLPreferences prefs = new SQLPreferences(getTable().getDBRoot());
        if (!prefs.getBoolean(GestionArticleGlobalPreferencePanel.STOCK_FACT, true)) {

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
    }
}
