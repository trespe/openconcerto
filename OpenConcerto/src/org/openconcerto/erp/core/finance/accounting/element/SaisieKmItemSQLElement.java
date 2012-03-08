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
 
 package org.openconcerto.erp.core.finance.accounting.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;

public class SaisieKmItemSQLElement extends ComptaSQLConfElement {
    public SaisieKmItemSQLElement() {
        super("SAISIE_KM_ELEMENT", "un élément de saisie au kilomètre", "éléments de saisie au kilomètre");
    }

    public List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("NOM");
        l.add("NOM_ECRITURE");
        l.add("DEBIT");
        l.add("CREDIT");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DEBIT");
        l.add("CREDIT");
        return l;
    }

    public SQLComponent createComponent() {
        return new UISQLComponent(this) {
            public void addViews() {
                this.addRequiredSQLObject(new JTextField(), "NUMERO", "left");
                this.addRequiredSQLObject(new JTextField(), "NOM", "right");
                this.addSQLObject(new JTextField(), "NOM_ECRITURE", "left");
                this.addSQLObject(new DeviseField(), "DEBIT", "left");
                this.addSQLObject(new DeviseField(), "CREDIT", "right");
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".userentry.item";
    }
}
