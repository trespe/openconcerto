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
 
 package org.openconcerto.erp.core.finance.accounting.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.map.model.Ville;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.utils.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JTextField;

public class AssociationAnalytiqueSQLElement extends ComptaSQLConfElement {

    public AssociationAnalytiqueSQLElement() {
        super("ASSOCIATION_ANALYTIQUE", "une association analytique", "associations analytiques");

    }

    @Override
    protected void ffInited() {
        super.ffInited();
        this.setAction("ID_ECRITURE", ReferenceAction.CASCADE);
        this.setAction("ID_SAISIE_KM_ELEMENT", ReferenceAction.CASCADE);
    }

    protected List<String> getListFields() {
        final List<String> list = new ArrayList<String>(2);
        list.add("ID_ECRITURE");
        list.add("ID_POSTE_ANALYTIQUE");
        // list.add("MONTANT");
        return list;
    }

    protected List<String> getComboFields() {
        final List<String> list = new ArrayList<String>(2);
        list.add("ID_ECRITURE");
        list.add("ID_POSTE_ANALYTIQUE");
        return list;
    }

    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {
            public void addViews() {
                this.addRequiredSQLObject(new JTextField(), "ID_ECRITURE");
                this.addRequiredSQLObject(new JTextField(), "ID_POSTE_ANALYTIQUE");
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".analytic.relation";
    }

    @Override
    protected SQLTableModelSourceOnline createTableSource() {
        SQLTableModelSourceOnline table = super.createTableSource();
        BaseSQLTableModelColumn debit = new BaseSQLTableModelColumn("Débit", BigDecimal.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {

                long montant = r.getLong("MONTANT");
                if (montant > 0) {
                    return new BigDecimal(montant).movePointLeft(2);
                } else {
                    return BigDecimal.ZERO;
                }
            }

            @Override
            public Set<FieldPath> getPaths() {
                Path p = new Path(getTable());
                return CollectionUtils.createSet(new FieldPath(p, "MONTANT"));
            }
        };

        table.getColumns().add(debit);

        BaseSQLTableModelColumn credit = new BaseSQLTableModelColumn("Crédit", BigDecimal.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {

                long montant = r.getLong("MONTANT");
                if (montant < 0) {
                    return new BigDecimal(-montant).movePointLeft(2);
                } else {
                    return BigDecimal.ZERO;
                }
            }

            @Override
            public Set<FieldPath> getPaths() {
                Path p = new Path(getTable());
                return CollectionUtils.createSet(new FieldPath(p, "MONTANT"));
            }
        };

        table.getColumns().add(credit);

        return table;
    }
}
