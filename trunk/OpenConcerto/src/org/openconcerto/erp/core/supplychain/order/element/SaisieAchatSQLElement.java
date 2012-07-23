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
import org.openconcerto.erp.core.supplychain.credit.component.AvoirFournisseurSQLComponent;
import org.openconcerto.erp.core.supplychain.order.component.SaisieAchatSQLComponent;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.view.EditFrame;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

public class SaisieAchatSQLElement extends ComptaSQLConfElement {

    public SaisieAchatSQLElement() {
        super("SAISIE_ACHAT", "une saisie d'achat", "saisies d'achats");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_MOUVEMENT");
        l.add("DATE");
        l.add("NOM");
        l.add("ID_FOURNISSEUR");
        l.add("MONTANT_HT");
        l.add("MONTANT_TTC");
        l.add("INFOS");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("MONTANT_TTC");
        return l;
    }

    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_MODE_REGLEMENT");
        l.add("ID_MOUVEMENT");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new SaisieAchatSQLComponent(this);
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".purchase";
    }

    public void transfertAvoir(int idFacture) {
        final SQLElement elt = Configuration.getInstance().getDirectory().getElement("AVOIR_FOURNISSEUR");
        final EditFrame editAvoirFrame = new EditFrame(elt);
        editAvoirFrame.setIconImage(new ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());

        final AvoirFournisseurSQLComponent comp = (AvoirFournisseurSQLComponent) editAvoirFrame.getSQLComponent();
        final SQLInjector inject = SQLInjector.getInjector(this.getTable(), elt.getTable());
        comp.select(inject.createRowValuesFrom(idFacture));

        editAvoirFrame.pack();
        editAvoirFrame.setState(JFrame.NORMAL);
        editAvoirFrame.setVisible(true);

    }
}
