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
 
 package org.openconcerto.erp.core.sales.order.element;

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.sales.invoice.component.SaisieVenteFactureSQLComponent;
import org.openconcerto.erp.core.sales.order.component.CommandeClientSQLComponent;
import org.openconcerto.erp.core.sales.shipment.component.BonDeLivraisonSQLComponent;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.view.EditFrame;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;



public class CommandeClientSQLElement extends ComptaSQLConfElement {

    public CommandeClientSQLElement() {
        super("COMMANDE_CLIENT", "une commande client", "commandes clients");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.BaseSQLElement#getComboFields()
     */
    protected List<String> getComboFields() {
        List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.BaseSQLElement#getListFields()
     */
    protected List<String> getListFields() {
        List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("DATE");
        l.add("ID_CLIENT");
        // l.add("OBJET");
        l.add("T_HT");
        l.add("T_TTC");
        l.add("NOM");
        l.add("INFOS");
        return l;
    }

    @Override
    protected Set<String> getChildren() {
        Set<String> set = new HashSet<String>();
        set.add("COMMANDE_CLIENT_ELEMENT");
        return set;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new CommandeClientSQLComponent();
    }

    /**
     * Transfert d'un commande en BL
     * 
     * @param commandeID
     */
    public void transfertBonLivraison(int commandeID) {

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON");
        EditFrame editBonFrame = new EditFrame(elt);
        editBonFrame.setIconImage(new ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());

        BonDeLivraisonSQLComponent comp = (BonDeLivraisonSQLComponent) editBonFrame.getSQLComponent();

        // comp.setDefaults();
        comp.loadCommande(commandeID);

        editBonFrame.pack();
        editBonFrame.setState(JFrame.NORMAL);
        editBonFrame.setVisible(true);
    }

    /**
     * Transfert d'une commande en facture
     * 
     * @param commandeID
     */
    public void transfertFacture(int commandeID) {

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        EditFrame editFactureFrame = new EditFrame(elt);
        editFactureFrame.setIconImage(new ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());

        SaisieVenteFactureSQLComponent comp = (SaisieVenteFactureSQLComponent) editFactureFrame.getSQLComponent();

        // comp.setDefaults();
        comp.loadCommande(commandeID);

        editFactureFrame.pack();
        editFactureFrame.setState(JFrame.NORMAL);
        editFactureFrame.setVisible(true);
    }
}
