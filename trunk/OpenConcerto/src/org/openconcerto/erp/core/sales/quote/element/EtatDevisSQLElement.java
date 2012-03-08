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

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.utils.CollectionMap;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;

public class EtatDevisSQLElement extends ComptaSQLConfElement {

    public static final int REFUSE = 3;
    public static final int ACCEPTE = 4;
    public static final int EN_ATTENTE = 2;

    public EtatDevisSQLElement() {
        super("ETAT_DEVIS", "un état de devis", "états de devis");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    @Override
    public CollectionMap<String, String> getShowAs() {
        final CollectionMap<String, String> res = new CollectionMap<String, String>();
        res.put(null, "NOM");
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
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".state";
    }
}
