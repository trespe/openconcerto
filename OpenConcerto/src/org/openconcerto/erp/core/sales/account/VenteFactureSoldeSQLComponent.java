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
 
 package org.openconcerto.erp.core.sales.account;

import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.Acompte;
import org.openconcerto.erp.core.common.ui.AcompteField;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureSQLElement;
import org.openconcerto.erp.core.sales.invoice.ui.FactureSituationItemTable;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.utils.Tuple2;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JCheckBox;

public class VenteFactureSoldeSQLComponent extends VenteFactureSituationSQLComponent {
    public static final String ID = "sales.invoice.partial.balance";

    public VenteFactureSoldeSQLComponent(SQLElement element) {
        super(element, new VenteFactureSoldeEditGroup());
    }

    @Override
    protected void addViews() {

        super.addViews();
        getEditor("sales.invoice.partial.amount").setEnabled(false);
        JCheckBox box = new JCheckBox("Solde");
        box.setSelected(true);
        this.addView(box, "SOLDE");

    }

    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues rowVals = new SQLRowValues(getTable());
        rowVals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, new Date()));
        rowVals.put("SOLDE", Boolean.TRUE);
        return rowVals;
    }

    @Override
    public int insert(SQLRow order) {
        return super.insert(order);
    }

    @Override
    public void importFrom(List<SQLRowValues> rows) {

        super.importFrom(rows);
        AcompteField field = (AcompteField) getEditor("sales.invoice.partial.amount");

        Tuple2<Long, Long> t = getTotalFacture(rows);
        final Acompte a = new Acompte(null, new BigDecimal(t.get0() - t.get1()).movePointLeft(2));
        field.setValue(a);
        final FactureSituationItemTable table = ((FactureSituationItemTable) getEditor("sales.invoice.partial.items.list"));
        table.calculPourcentage(a);
    }

    public Tuple2<Long, Long> getTotalFacture(List<SQLRowValues> context) {

        long totalFacture = 0;
        long totalCommande = 0;
        Set<SQLRowAccessor> facture = new HashSet<SQLRowAccessor>();
        for (SQLRowAccessor sqlRowAccessor : context) {
            totalFacture += getFacture(sqlRowAccessor, facture);
            totalCommande += sqlRowAccessor.getLong("T_HT");
        }

        return Tuple2.create(totalCommande, totalFacture);
    }

    public long getFacture(SQLRowAccessor sqlRowAccessor, Set<SQLRowAccessor> alreadyAdded) {
        Collection<? extends SQLRowAccessor> rows = sqlRowAccessor.getReferentRows(sqlRowAccessor.getTable().getTable("TR_COMMANDE_CLIENT"));
        long l = 0;
        for (SQLRowAccessor sqlRowAccessor2 : rows) {
            if (!sqlRowAccessor2.isForeignEmpty("ID_SAISIE_VENTE_FACTURE")) {
                SQLRowAccessor rowFacture = sqlRowAccessor2.getForeign("ID_SAISIE_VENTE_FACTURE");
                if (!alreadyAdded.contains(rowFacture)) {
                    alreadyAdded.add(rowFacture);
                    l += rowFacture.getLong("T_HT");
                }
            }
        }
        return l;
    }

}
