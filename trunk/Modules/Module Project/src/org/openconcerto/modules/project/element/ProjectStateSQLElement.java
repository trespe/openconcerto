package org.openconcerto.modules.project.element;

import java.util.ArrayList;
import java.util.List;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.utils.CollectionMap;

public class ProjectStateSQLElement extends ComptaSQLConfElement {
    public static int A_TRAITER = 2;
    public static int EN_COURS = 3;
    public static int TRAITEMENT_TERMINE = 4;
    public static int A_FACTURER = 5;
    public static int DOSSIER_CLOS = 6;

    public ProjectStateSQLElement() {
        super("ETAT_AFFAIRE", "un état affaire", "états affaires");
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
        return createCodeFromPackage() + ".state";
    }
}
