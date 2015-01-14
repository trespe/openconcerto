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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;

public class ContactSQLElement extends ContactSQLElementBase {

    static public class ContactFournisseurSQLElement extends ContactSQLElement {
        public ContactFournisseurSQLElement() {
            super("CONTACT_FOURNISSEUR");
        }
    }

    static public class ContactAdministratifSQLElement extends ContactSQLElement {
        public ContactAdministratifSQLElement() {
            super("CONTACT_ADMINISTRATIF");
        }
    }

    public ContactSQLElement() {
        this("CONTACT");

    }

    protected ContactSQLElement(String tableName) {
        super(tableName);
        this.setL18nLocation(Gestion.class);
        PredicateRowAction action = new PredicateRowAction(new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                sendMail(IListe.get(e).getSelectedRows());

            }
        }, true, "customerrelationship.customer.email.send");
        action.setPredicate(IListeEvent.getNonEmptySelectionPredicate());
        getRowActions().add(action);
    }
}
