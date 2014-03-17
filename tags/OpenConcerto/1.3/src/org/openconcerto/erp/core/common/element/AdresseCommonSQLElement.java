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

import org.openconcerto.erp.core.common.component.AdresseCommonSQLComponent;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;

import java.util.ArrayList;
import java.util.List;

public class AdresseCommonSQLElement extends ConfSQLElement {

    public AdresseCommonSQLElement() {
        super("ADRESSE_COMMON", "une adresse", "adresses");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("RUE");
        l.add("VILLE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("RUE");
        l.add("VILLE");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new AdresseCommonSQLComponent(this);
    }

    @Override
    protected String createCode() {
        return "common.address";
    }
}
