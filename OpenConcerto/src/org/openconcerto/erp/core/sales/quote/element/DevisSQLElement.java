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
import org.openconcerto.erp.core.sales.quote.component.DevisSQLComponent;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.utils.CollectionMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

public class DevisSQLElement extends ComptaSQLConfElement {

    public static final String TABLENAME = "DEVIS";

    public DevisSQLElement() {
        super(TABLENAME, "un devis", "devis");
    }

    protected List<String> getComboFields() {
        List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        return l;
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
