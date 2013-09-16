/*
 * Créé le 18 mai 2012
 */
package org.openconcerto.modules.subscription.panel;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.sales.quote.element.DevisSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;

public class DevisAboPanel extends AboPanel {

    public DevisAboPanel() {
        super(Configuration.getInstance().getDirectory().getElement("DEVIS"), Configuration.getInstance().getDirectory().getElement("DEVIS_ELEMENT"), "DEVIS");
    }

    private SQLTable tableNum = Configuration.getInstance().getRoot().findTable("NUMEROTATION_AUTO");

    @Override
    protected void injectRow(SQLRow row, SQLRowValues rowVals, Date dateNew, SQLRow rowAbonnement) {
        super.injectRow(row, rowVals, dateNew, rowAbonnement);
        String nextNumero = NumerotationAutoSQLElement.getNextNumero(DevisSQLElement.class);
        rowVals.put("NUMERO", nextNumero);

        // incrémentation du numéro auto
        final SQLRowValues rowValsNum = new SQLRowValues(this.tableNum);
        int val = this.tableNum.getRow(2).getInt(NumerotationAutoSQLElement.getLabelNumberFor(DevisSQLElement.class));
        val++;
        rowValsNum.put(NumerotationAutoSQLElement.getLabelNumberFor(DevisSQLElement.class), new Integer(val));
        try {
            rowValsNum.update(2);
        } catch (final SQLException e) {
            e.printStackTrace();
        }

        rowVals.put("ID_ETAT_DEVIS", org.openconcerto.erp.core.sales.quote.element.EtatDevisSQLElement.EN_ATTENTE);
        rowVals.put("REMISE_HT", row.getObject("REMISE_HT"));
        rowVals.put("INFOS", row.getObject("INFOS"));
        rowVals.put("PORT_HT", row.getObject("PORT_HT"));
        rowVals.put("OBJET", row.getObject("OBJET"));
        rowVals.put("T_POIDS", row.getObject("T_POIDS"));
        rowVals.put("ID_ADRESSE", row.getObject("ID_ADRESSE"));
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateNew);
        cal.add(Calendar.MONTH, 1);
        rowVals.put("DATE_VALIDITE", cal.getTime());
    }

}
