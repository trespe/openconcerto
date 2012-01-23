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
 
 package org.openconcerto.erp.core.supplychain.order.element;

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.supplychain.order.component.CommandeSQLComponent;
import org.openconcerto.erp.core.supplychain.order.component.SaisieAchatSQLComponent;
import org.openconcerto.erp.core.supplychain.receipt.component.BonReceptionSQLComponent;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

public class CommandeSQLElement extends ComptaSQLConfElement {

    public CommandeSQLElement() {
        super("COMMANDE", "une commande fournisseur", "commandes fournisseur");

        // Transfert vers facture
        PredicateRowAction factureAction = new PredicateRowAction(new AbstractAction("Transfert vers facture") {
            public void actionPerformed(ActionEvent e) {

                CommandeSQLElement.this.transfertFacture(IListe.get(e).getSelectedRow().getID());
            }
        }, false);
        factureAction.setPredicate(IListeEvent.getSingleSelectionPredicate());
        getRowActions().add(factureAction);

    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("NOM");
        l.add("DATE");
        l.add("ID_FOURNISSEUR");
        l.add("T_HT");
        l.add("T_TTC");
        l.add("EN_COURS");
        l.add("INFOS");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("NOM");
        l.add("DATE");
        return l;
    }

    @Override
    protected Set<String> getChildren() {
        Set<String> set = new HashSet<String>();
        set.add("COMMANDE_ELEMENT");
        return set;
    }

    protected List<String> getPrivateFields() {
        if (getTable().getFieldsName().contains("ID_ADRESSE")) {
            final List<String> l = new ArrayList<String>();
            l.add("ID_ADRESSE");
            return l;
        } else {
            return super.getPrivateFields();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new CommandeSQLComponent();
    }

    /**
     * Transfert d'une commande en BR
     * 
     * @param commandeID
     */
    public void transfertBR(int commandeID) {

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("BON_RECEPTION");
        EditFrame editFactureFrame = new EditFrame(elt);
        editFactureFrame.setIconImage(new ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());

        BonReceptionSQLComponent comp = (BonReceptionSQLComponent) editFactureFrame.getSQLComponent();

        // comp.setDefaults();
        comp.loadCommande(commandeID);

        editFactureFrame.pack();
        editFactureFrame.setState(JFrame.NORMAL);
        editFactureFrame.setVisible(true);
    }

    /**
     * Transfert d'une commande en facture
     * 
     * @param commandeID
     */
    public void transfertFacture(int commandeID) {

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("SAISIE_ACHAT");
        EditFrame editFactureFrame = new EditFrame(elt);
        editFactureFrame.setIconImage(new ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());

        SaisieAchatSQLComponent comp = (SaisieAchatSQLComponent) editFactureFrame.getSQLComponent();

        // comp.setDefaults();
        comp.loadCommande(commandeID);

        editFactureFrame.pack();
        editFactureFrame.setState(JFrame.NORMAL);
        editFactureFrame.setVisible(true);
    }
}
