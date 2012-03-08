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

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureItemSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.utils.CollectionMap;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;

public class BonDeLivraisonItemSQLElement extends ComptaSQLConfElement {

    public BonDeLivraisonItemSQLElement() {
        super("BON_DE_LIVRAISON_ELEMENT", "un element de bon de livraison", "éléments de bon de livraison");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        l.add("NOM");
        l.add("PV_HT");
        l.add("QTE");
        l.add("QTE_LIVREE");
        l.add("QTE_A_LIVRER");
        l.add("ID_TAXE");
        l.add("POIDS");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        l.add("NOM");
        l.add("PV_HT");
        l.add("QTE");
        return l;
    }

    @Override
    public CollectionMap<String, String> getShowAs() {
        final CollectionMap<String, String> res = new CollectionMap<String, String>();
        res.put("ID_BON_DE_LIVRAISON", "NUMERO");
        return res;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new UISQLComponent(this) {

            public void addViews() {

                this.addRequiredSQLObject(new JTextField(), "NOM", "left");
                this.addRequiredSQLObject(new JTextField(), "CODE", "right");

                this.addSQLObject(new ElementComboBox(), "ID_BON_DE_LIVRAISON", "left");

                this.addRequiredSQLObject(new DeviseField(), "PV_HT", "left");
                this.addSQLObject(new JTextField(), "QTE", "right");

                this.addSQLObject(new JTextField(), "POIDS", "left");
                this.addSQLObject(new ElementComboBox(), "ID_TAXE", "right");
            }

            public int insert(SQLRow order) {
                int id = super.insert(order);

                SQLRow rowBon = getTable().getRow(id);
                SQLRow rowFact = getTable().getBase().getTable("BON_DE_LIVRAISON_ELEMENT").getRow(rowBon.getInt("ID_BON_DE_LIVRAISON_ELEMENT"));

                int qteLivree = rowFact.getInt("QTE_LIVREE") + rowFact.getInt("QTE_LIVREE");

                SQLRowValues rowVals = new SQLRowValues(new SaisieVenteFactureItemSQLElement().getTable());
                rowVals.put("QTE_LIVREE", new Integer(qteLivree));

                try {
                    rowVals.update(rowFact.getID());
                } catch (SQLException e) {

                    e.printStackTrace();
                }

                return id;
            }
        };

    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".item";
    }
}
