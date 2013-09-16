package org.openconcerto.modules.project;

import org.openconcerto.sql.Configuration;

public class QuoteToInvoiceSQLInjector extends org.openconcerto.erp.injector.DevisFactureSQLInjector {

    public QuoteToInvoiceSQLInjector() {
        super(Configuration.getInstance().getRoot());
        map(getSource().getField("ID_AFFAIRE"), getDestination().getField("ID_AFFAIRE"));
    }
}
