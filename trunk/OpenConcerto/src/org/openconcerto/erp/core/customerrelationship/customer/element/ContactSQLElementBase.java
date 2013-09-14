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
 
 package org.openconcerto.erp.core.customerrelationship.customer.element;

import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.utils.CollectionMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ContactSQLElementBase extends ConfSQLElement {

    public ContactSQLElementBase() {
        this("CONTACT");
    }

    protected ContactSQLElementBase(String tableName) {
        super(tableName);
    }

    @Override
    protected String createCode() {
        return this.getTable().getName();
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("PRENOM");
        l.add("FONCTION");
        if (getTable().contains("ID_CLIENT")) {
            l.add("ID_CLIENT");
        }

        if (getTable().contains("ID_FOURNISSEUR")) {
            l.add("ID_FOURNISSEUR");
        }
        l.add("TEL_STANDARD");
        l.add("TEL_DIRECT");
        l.add("EMAIL");
        l.add("TEL_MOBILE");
        if (getTable().contains("ORIGINE")) {
            l.add("ORIGINE");
        }
        if (getTable().contains("TYPE_DEMANDE")) {
            l.add("TYPE_DEMANDE");
        }
        if (getTable().contains("N4DS")) {
            l.add("N4DS");
        }
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("PRENOM");
        l.add("FONCTION");
        if (getTable().contains("ID_SITE"))
            l.add("ID_SITE");
        if (getTable().contains("ID_CLIENT"))
            l.add("ID_CLIENT");
        if (getTable().contains("ID_FOURNISSEUR")) {
            l.add("ID_FOURNISSEUR");
        }
        return l;
    }

    @Override
    public CollectionMap<String, String> getShowAs() {
        return CollectionMap.singleton(null, "NOM", "TEL_STANDARD");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new ContactSQLComponent(this);
    }

    public static class ContactSQLComponent extends UISQLComponent {

        public ContactSQLComponent(SQLElement elt) {
            super(elt);
        }

        @Override
        protected Set<String> createRequiredNames() {
            final Set<String> s = new HashSet<String>();
            s.add("NOM");
            return s;
        }

        public void addViews() {
            if (getTable().contains("ID_CLIENT")) {
                this.addView("ID_CLIENT");
            }
            if (getTable().contains("ID_FOURNISSEUR")) {
                this.addView("ID_FOURNISSEUR");
            }
            if (getTable().contains("ID_TITRE_PERSONNEL")) {
                this.addView("ID_TITRE_PERSONNEL", "1");
                this.addView("NOM", "1");
                this.addView("PRENOM", "1");
            } else {
                this.addView("NOM", "left");
                this.addView("PRENOM", "right");
            }

            this.addView("FONCTION", "1");

            if (getTable().contains("ID_SITE"))
                this.addView("ID_SITE");

            if (getTable().contains("ACTIF"))
                this.addView("ACTIF");

            if (getTable().contains("SUFFIXE"))
                this.addView("SUFFIXE");

            this.addView("TEL_STANDARD", "left");
            this.addView("TEL_DIRECT", "right");

            this.addView("TEL_MOBILE", "left");
            this.addView("TEL_PERSONEL", "right");

            this.addView("FAX", "left");
            this.addView("EMAIL", "right");

            if (getTable().contains("SERVICE"))
                this.addView("SERVICE");

            if (getTable().contains("NOM_ASSISTANTE"))
                this.addView("NOM_ASSISTANTE");

            if (getTable().contains("EMAIL_ASSISTANTE"))
                this.addView("EMAIL_ASSISTANTE");

            if (getTable().contains("TEL_ASSISTANTE"))
                this.addView("TEL_ASSISTANTE");

            if (getTable().contains("NOM_RESPONSABLE"))
                this.addView("NOM_RESPONSABLE");

            if (getTable().contains("N4DS")) {
                addView("N4DS");
            }
        }

        public void setIdClient(int idClient) {
            if (idClient != SQLRow.NONEXISTANT_ID) {
                this.setDefaults(Collections.singletonMap("ID_CLIENT", idClient));
            } else {
                this.clearDefaults();
            }
        }
    }

}
