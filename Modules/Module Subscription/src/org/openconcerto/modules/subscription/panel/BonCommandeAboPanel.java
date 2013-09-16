/*
 * Créé le 18 mai 2012
 */
package org.openconcerto.modules.subscription.panel;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.sales.order.element.CommandeClientSQLElement;
import org.openconcerto.erp.core.sales.order.report.CommandeClientXmlSheet;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.list.RowAction;

public class BonCommandeAboPanel extends AboPanel {

    public BonCommandeAboPanel() {

        super(Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT"), Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT_ELEMENT"), "COMMANDE");

    }

    private SQLTable tableNum = Configuration.getInstance().getRoot().findTable("NUMEROTATION_AUTO");

    @Override
    protected void injectRow(SQLRow row, SQLRowValues rowVals, Date dateNew, SQLRow rowAbonnement) {
        // TODO Raccord de méthode auto-généré
        super.injectRow(row, rowVals, dateNew, rowAbonnement);
        rowVals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(CommandeClientSQLElement.class));
        // incrémentation du numéro auto
        final SQLRowValues rowValsNum = new SQLRowValues(this.tableNum);
        int val = this.tableNum.getRow(2).getInt(NumerotationAutoSQLElement.getLabelNumberFor(CommandeClientSQLElement.class));
        val++;
        rowValsNum.put(NumerotationAutoSQLElement.getLabelNumberFor(CommandeClientSQLElement.class), new Integer(val));
        try {
            rowValsNum.update(2);
        } catch (final SQLException e) {
            e.printStackTrace();
        }

        rowVals.put("NOM", row.getObject("NOM"));
        rowVals.put("T_POIDS", row.getObject("T_POIDS"));
    }

    @Override
    protected List<RowAction> getAdditionnalRowActions() {
        return new MouseSheetXmlListeListener(CommandeClientXmlSheet.class).getRowActions();
    }

}
