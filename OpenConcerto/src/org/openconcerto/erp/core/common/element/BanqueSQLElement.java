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
 
 package org.openconcerto.erp.core.common.element;

import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.utils.ProductInfo;

import java.util.ArrayList;
import java.util.List;

public class BanqueSQLElement extends ComptaSQLConfElement {

    public static final String TABLENAME = ProductInfo.getInstance().getName().equals("OpenConcerto") ? "BANQUE" : "BANQUE_POLE_PRODUIT";

    public BanqueSQLElement() {
        super(TABLENAME, "une banque", "banques");
    }

    @Override
    protected List<String> getComboFields() {
        List<String> list = new ArrayList<String>();
        list.add("CODE");
        list.add("NOM");
        return list;
    }

    @Override
    protected List<String> getListFields() {
        List<String> list = new ArrayList<String>();
        list.add("CODE");
        list.add("NOM");

        list.add("ID_JOURNAL");
        list.add("ID_COMPTE_PCE");
        return list;
    }

    public SQLComponent createComponent() {
        return new UISQLComponent(this, 2) {
            public void addViews() {
                this.addView("CODE");
                this.addView("NOM");
                this.addView("NUMERO_RUE");
                this.addView("VOIE");
                this.addView("RUE");
                this.addView("VILLE");
                this.addView("BIC");
                this.addView("IBAN");
                this.addView("AFFACTURAGE");
                this.addView("ID_JOURNAL");
                this.addView("ID_COMPTE_PCE");
                this.addView("INFOS");

            }
        };
    }
}
