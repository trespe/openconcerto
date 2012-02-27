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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;

import java.util.ArrayList;
import java.util.List;

public class ClientNormalSQLElement extends ComptaSQLConfElement {

    public ClientNormalSQLElement() {
        super("CLIENT", "un client", "clients");
    }

    protected boolean showMdr = true;

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
            l.add("CODE");
            // l.add("FORME_JURIDIQUE");
        l.add("NOM");
        if (getTable().getFieldsName().contains("LOCALISATION")) {
            l.add("LOCALISATION");
        }

        l.add("RESPONSABLE");

        l.add("ID_ADRESSE");
        l.add("TEL");
        l.add("FAX");
        l.add("MAIL");
            l.add("NUMERO_TVA");
            l.add("SIRET");
            l.add("ID_COMPTE_PCE");
            l.add("ID_MODE_REGLEMENT");
        l.add("INFOS");
        return l;
    }

    @Override
    public synchronized ListSQLRequest createListRequest() {
        return new ListSQLRequest(getTable(), getListFields()) {
            @Override
            protected void customizeToFetch(SQLRowValues graphToFetch) {
                super.customizeToFetch(graphToFetch);
                graphToFetch.grow("ID_MODE_REGLEMENT").put("AJOURS", null).put("LENJOUR", null);
            }
        };

    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        // l.add("FORME_JURIDIQUE");
        l.add("NOM");
        if (getTable().getFieldsName().contains("LOCALISATION")) {
            l.add("LOCALISATION");
        } else {
            l.add("CODE");
        }
        return l;
    }

    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_ADRESSE");
        l.add("ID_ADRESSE_L");
        l.add("ID_ADRESSE_F");
        l.add("ID_MODE_REGLEMENT");

        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new ClientNormalSQLComponent(this);
    }
   

}
