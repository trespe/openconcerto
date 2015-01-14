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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import org.openconcerto.erp.core.common.ui.DeviseCellEditor;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.list.CellDynamicModifier;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableControlPanel;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Vector;

import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;

public class AssociationAnalytiqueItemModel {

    private final DeviseKmRowValuesRenderer deviseRenderer = new DeviseKmRowValuesRenderer();
    private final DeviseCellEditor deviseCellEditor = new DeviseCellEditor();

    RowValuesTableModel model;

    public AssociationAnalytiqueItemModel() {

        final SQLElement elt = Configuration.getInstance().getDirectory().getElement("ASSOCIATION_ANALYTIQUE");

        final List<SQLTableElement> list = new Vector<SQLTableElement>();
        final SQLTable tableElement = elt.getTable();

        final SQLTableElement tableElementNomCompte = new SQLTableElement(tableElement.getField("ID_POSTE_ANALYTIQUE"));
        list.add(tableElementNomCompte);

        final SQLTableElement tableElementPourcent = new SQLTableElement(tableElement.getField("POURCENT"));
        list.add(tableElementPourcent);

        final SQLTableElement tableElementMontant = new SQLTableElement(tableElement.getField("MONTANT"), Long.class, this.deviseCellEditor);
        list.add(tableElementMontant);

        this.model = new RowValuesTableModel(elt, list, tableElement.getField("ID_POSTE_ANALYTIQUE"), false);

        tableElementMontant.addModificationListener(tableElementPourcent);
        tableElementPourcent.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row) {
                long montant = row.getLong("MONTANT");

                long total = row.getForeign("ID_ECRITURE").getLong("DEBIT") - row.getForeign("ID_ECRITURE").getLong("CREDIT");

                BigDecimal pourcent = new BigDecimal(montant).divide(new BigDecimal(total), MathContext.DECIMAL128).abs().movePointRight(2)
                        .setScale(tableElementPourcent.getDecimalDigits(), RoundingMode.HALF_UP);
                return pourcent;
            }

        });

        tableElementPourcent.addModificationListener(tableElementMontant);
        tableElementMontant.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row) {
                BigDecimal percent = row.getBigDecimal("POURCENT");

                long total = row.getForeign("ID_ECRITURE").getLong("DEBIT") - row.getForeign("ID_ECRITURE").getLong("CREDIT");

                BigDecimal montant = percent.movePointLeft(2).multiply(new BigDecimal(total)).setScale(0, RoundingMode.HALF_UP);

                return montant.longValue();
            }

        });

        tableElementMontant.setRenderer(this.deviseRenderer);

    }

    public RowValuesTableModel getModel() {
        return this.model;
    }

}
