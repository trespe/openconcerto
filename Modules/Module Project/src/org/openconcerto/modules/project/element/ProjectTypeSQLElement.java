package org.openconcerto.modules.project.element;

import java.util.ArrayList;
import java.util.List;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.utils.CollectionMap;

public class ProjectTypeSQLElement extends ComptaSQLConfElement {

    public ProjectTypeSQLElement() {
        super("TYPE_AFFAIRE", "un type d'affaire", "types d'affaire");
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    @Override
    public CollectionMap<String, String> getShowAs() {
        return CollectionMap.singleton(null, getComboFields());
    }

    @Override
    public SQLComponent createComponent() {
        return new UISQLComponent(this) {
            @Override
            protected void addViews() {
                this.addView("NOM");
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".kind";
    }
}
