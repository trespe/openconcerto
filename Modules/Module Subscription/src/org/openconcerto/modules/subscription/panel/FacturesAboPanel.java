/*
 * Créé le 18 mai 2012
 */
package org.openconcerto.modules.subscription.panel;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.swing.JOptionPane;

import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureSQLElement;
import org.openconcerto.erp.core.sales.invoice.report.VenteFactureXmlSheet;
import org.openconcerto.erp.generationEcritures.GenerationMvtSaisieVenteFacture;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.IResultSetHandler;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.list.RowAction;

public class FacturesAboPanel extends AboPanel {

    public FacturesAboPanel() {
        super(Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE"), Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT"), "FACTURE");
    }

    private final SQLTable tableNum = Configuration.getInstance().getRoot().findTable("NUMEROTATION_AUTO");

    @Override
    protected void validItem(SQLRowAccessor sqlRowAccessor) {
        // Affectation d'un numero
        SQLRowValues rowVals = sqlRowAccessor.asRowValues();
        String nextNumero = NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class);

        rowVals.put("NUMERO", nextNumero);

        SQLRowValues rowValsNum = new SQLRowValues(tableNum);

        String labelNumberFor = NumerotationAutoSQLElement.getLabelNumberFor(SaisieVenteFactureSQLElement.class);
        int val = tableNum.getRow(2).getInt(labelNumberFor);
        val++;
        rowValsNum.put(labelNumberFor, Integer.valueOf(val));

        if (!checkUniciteNumero(nextNumero, sqlRowAccessor.getID())) {
            JOptionPane.showMessageDialog(null, "Impossible de valider les factures. La numérotation automatique n'est pas correcte.");
            return;
        }

        try {
            rowValsNum.update(2);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Validation
        rowVals.put("CREATION_AUTO_VALIDER", Boolean.TRUE);
        rowVals.put("PREVISIONNELLE", Boolean.FALSE);
        try {
            rowVals.update();
        } catch (SQLException exn) {
            exn.printStackTrace();
        }

        int idMvt = 1;
        // Création des écritures associées
        if (sqlRowAccessor.getObject("ID_MOUVEMENT") != null) {

            idMvt = sqlRowAccessor.getInt("ID_MOUVEMENT");

            // on supprime tout ce qui est lié à la facture
            System.err.println("Archivage des fils");
            EcritureSQLElement eltEcr = (EcritureSQLElement) Configuration.getInstance().getDirectory().getElement("ECRITURE");
            eltEcr.archiveMouvementProfondeur(idMvt, false);
        }

        System.err.println("Regeneration des ecritures");
        if (idMvt > 1) {
            new GenerationMvtSaisieVenteFacture(sqlRowAccessor.getID(), idMvt);
        } else {
            new GenerationMvtSaisieVenteFacture(sqlRowAccessor.getID());
        }
        System.err.println("Fin regeneration");
    }

    private boolean checkUniciteNumero(String num, int idFact) {
        SQLTable tableFacture = Configuration.getInstance().getRoot().findTable("SAISIE_VENTE_FACTURE");
        final SQLSelect selNum = new SQLSelect(tableFacture.getDBSystemRoot(), true);
        selNum.addSelect(tableFacture.getKey(), "COUNT");
        final Where w = new Where(tableFacture.getField("NUMERO"), "=", num);
        selNum.setWhere(w);
        selNum.andWhere(new Where(tableFacture.getKey(), "!=", idFact));

        final String req = selNum.asString();
        final Number l = (Number) tableFacture.getBase().getDataSource().execute(req, new IResultSetHandler(SQLDataSource.SCALAR_HANDLER, false));
        return (l == null || l.intValue() == 0);

    }

    @Override
    protected void injectRow(SQLRow row, SQLRowValues rowVals, Date dateNew, SQLRow rowAbonnement) {
        // TODO Raccord de méthode auto-généré
        super.injectRow(row, rowVals, dateNew, rowAbonnement);
        rowVals.put("NUMERO", "ABO--" + NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class));
        rowVals.put("ID_ADRESSE", row.getObject("ID_ADRESSE"));
        rowVals.put("ID_COMPTE_PCE_SERVICE", row.getObject("ID_COMPTE_PCE_SERVICE"));
        rowVals.put("PORT_HT", row.getObject("PORT_HT"));
        rowVals.put("REMISE_HT", row.getObject("REMISE_HT"));

        rowVals.put("NOM", row.getObject("NOM"));
        rowVals.put("ID_CONTACT", row.getObject("ID_CONTACT"));
        rowVals.put("ID_COMPTE_PCE_VENTE", row.getObject("ID_COMPTE_PCE_VENTE"));
        rowVals.put("ID_DEVIS", row.getObject("ID_DEVIS"));
        rowVals.put("INFOS", row.getObject("INFOS"));
        rowVals.put("CREATION_AUTO_VALIDER", Boolean.FALSE);
        rowVals.put("PREVISIONNELLE", Boolean.TRUE);

        // Mode de reglement
        SQLRow rowMdr = row.getForeignRow("ID_MODE_REGLEMENT");
        SQLRowValues rowValsMdr = rowMdr.asRowValues();
        rowValsMdr.clearPrimaryKeys();
        rowVals.put("ID_MODE_REGLEMENT", rowValsMdr);
    }

    @Override
    protected List<RowAction> getAdditionnalRowActions() {
        return new MouseSheetXmlListeListener(VenteFactureXmlSheet.class).getRowActions();
    }

}
