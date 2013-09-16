/*
 * Créé le 6 janv. 2012
 */
package org.openconcerto.modules.project;

import org.openconcerto.sql.Configuration;

public class OrderToInvoiceSQLInjector extends org.openconcerto.erp.injector.CommandeFactureClientSQLInjector {

    public OrderToInvoiceSQLInjector() {
        super(Configuration.getInstance().getRoot());
        map(getSource().getField("ID_AFFAIRE"), getDestination().getField("ID_AFFAIRE"));
    }
}
