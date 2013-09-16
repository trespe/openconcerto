/*
 * Créé le 17 mai 2012
 */
package org.openconcerto.modules.subscription.element;

import java.util.ArrayList;
import java.util.List;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.utils.CollectionMap;

public class SubscriptionSQLElement extends ComptaSQLConfElement {

    public SubscriptionSQLElement() {
        super("ABONNEMENT", "un abonnement", "abonnements");
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("DATE");
        l.add("NOM");
        l.add("ID_CLIENT");
        l.add("DESCRIPTION");
        return l;
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("NOM");
        l.add("ID_CLIENT");
        return l;
    }

    @Override
    public CollectionMap<String, String> getShowAs() {
        return CollectionMap.singleton(null, "NUMERO");
    }

    @Override
    public SQLComponent createComponent() {
        return new SubscriptionSQLComponent(this);
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage();
    }
}
