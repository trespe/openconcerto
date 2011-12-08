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

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.sql.sqlobject.ElementComboBox;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;


public class CommandeClientElementSQLElement extends ComptaSQLConfElement {

    public CommandeClientElementSQLElement() {
        super("COMMANDE_CLIENT_ELEMENT", "un element de commande", "éléments de commande");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_STYLE");
        l.add("CODE");
        l.add("NOM");
        l.add("PA_HT");
        l.add("PV_HT");
        l.add("ID_TAXE");
        l.add("POIDS");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        l.add("NOM");
        l.add("PA_HT");
        l.add("PV_HT");
        return l;
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

                this.addSQLObject(new ElementComboBox(), "ID_STYLE", "left");

                this.addRequiredSQLObject(new DeviseField(), "PA_HT", "left");
                this.addSQLObject(new DeviseField(), "PV_HT", "right");

                this.addSQLObject(new JTextField(), "POIDS", "left");
                this.addSQLObject(new ElementComboBox(), "ID_TAXE", "right");
            }
        };
    }
}
