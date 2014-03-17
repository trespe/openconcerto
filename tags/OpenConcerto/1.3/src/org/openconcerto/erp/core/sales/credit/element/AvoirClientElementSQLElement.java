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
 
 package org.openconcerto.erp.core.sales.credit.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.sql.sqlobject.ElementComboBox;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;

public class AvoirClientElementSQLElement extends ComptaSQLConfElement {

    private static final String RIGHT = "right";
    private static final String LEFT = "left";

    public AvoirClientElementSQLElement() {
        super("AVOIR_CLIENT_ELEMENT", "un element d'avoir", "éléments d'avoir");
    }

    protected List<String> getListFields() {
        final List<String> list = new ArrayList<String>(7);
        list.add("ID_STYLE");
        list.add("CODE");
        list.add("NOM");
        list.add("PA_HT");
        list.add("PV_HT");
        list.add("ID_TAXE");
        list.add("POIDS");
        return list;
    }

    protected List<String> getComboFields() {
        final List<String> list = new ArrayList<String>(4);
        list.add("CODE");
        list.add("NOM");
        list.add("PA_HT");
        list.add("PV_HT");
        return list;
    }

    public SQLComponent createComponent() {
        return new UISQLComponent(this) {
            public void addViews() {
                this.addRequiredSQLObject(new JTextField(), "NOM", LEFT);
                this.addRequiredSQLObject(new JTextField(), "CODE", RIGHT);

                this.addSQLObject(new ElementComboBox(), "ID_STYLE", LEFT);

                this.addRequiredSQLObject(new DeviseField(), "PA_HT", LEFT);
                this.addSQLObject(new DeviseField(), "PV_HT", RIGHT);

                this.addSQLObject(new JTextField(), "POIDS", LEFT);
                this.addSQLObject(new ElementComboBox(), "ID_TAXE", RIGHT);
            }
        };
    }

    @Override
    protected String createCode() {
        return super.createCodeFromPackage() + ".item";
    }
}
