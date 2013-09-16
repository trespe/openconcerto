package org.openconcerto.modules.project;

import org.openconcerto.sql.Configuration;

public class QuoteToOrderSQLInjector extends org.openconcerto.erp.injector.DevisCommandeSQLInjector {

    public QuoteToOrderSQLInjector() {
        super(Configuration.getInstance().getRoot());
        map(getSource().getField("ID_AFFAIRE"), getDestination().getField("ID_AFFAIRE"));
    }
}
