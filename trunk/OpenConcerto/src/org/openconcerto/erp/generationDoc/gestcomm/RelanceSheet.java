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
 
 package org.openconcerto.erp.generationDoc.gestcomm;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.payment.element.ModeDeReglementSQLElement;
import org.openconcerto.erp.generationDoc.AbstractJOOReportsSheet;
import org.openconcerto.erp.generationDoc.AbstractSheetXml;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.Tuple2;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelanceSheet extends AbstractJOOReportsSheet {

    private SQLRow rowRelance;

    private static final Tuple2<String, String> tuple = Tuple2.create("LocationRelance", "Relance");

    public static Tuple2<String, String> getTuple2Location() {
        return tuple;
    }

    /**
     * @return une Map contenant les valeurs à remplacer dans la template
     */
    protected Map<String, Object> createMap() {

        final SQLRow rowSoc = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        final SQLRow rowSocAdresse = rowSoc.getForeignRow("ID_ADRESSE_COMMON");

        final Map<String, Object> map = new HashMap<String, Object>();

        // Infos societe
        map.put("SocieteType", rowSoc.getString("TYPE"));
        map.put("SocieteNom", rowSoc.getString("NOM"));
        map.put("SocieteAdresse", rowSocAdresse.getString("RUE"));
        map.put("SocieteCodePostal", getVilleCP(rowSocAdresse.getString("VILLE")));

        String ville = getVille(rowSocAdresse.getString("VILLE"));
        final Object cedex = rowSocAdresse.getObject("CEDEX");
        final boolean hasCedex = rowSocAdresse.getBoolean("HAS_CEDEX");

        if (hasCedex) {
            ville += " CEDEX";
            if (cedex != null && cedex.toString().trim().length() > 0) {
                ville += " " + cedex.toString().trim();
            }
        }

        map.put("SocieteVille", ville);

        SQLRow rowClient;
        final SQLRow clientRowNX = this.rowRelance.getForeignRow("ID_CLIENT");
            rowClient = clientRowNX;
        SQLRow rowAdresse = rowClient.getForeignRow("ID_ADRESSE");

        // Client compte
        SQLRow rowCompteClient = clientRowNX.getForeignRow("ID_COMPTE_PCE");
        String numero = rowCompteClient.getString("NUMERO");
        map.put("ClientNumeroCompte", numero);

        // Infos Client
        map.put("ClientType", rowClient.getString("FORME_JURIDIQUE"));
        map.put("ClientNom", rowClient.getString("NOM"));
        map.put("ClientAdresse", rowAdresse.getString("RUE"));
        map.put("ClientCodePostal", getVilleCP(rowAdresse.getString("VILLE")));
        String villeCli = getVille(rowAdresse.getString("VILLE"));
        final Object cedexCli = rowAdresse.getObject("CEDEX");
        final boolean hasCedexCli = rowAdresse.getBoolean("HAS_CEDEX");

        if (hasCedexCli) {
            villeCli += " CEDEX";
            if (cedexCli != null && cedexCli.toString().trim().length() > 0) {
                villeCli += " " + cedexCli.toString().trim();
            }
        }

        map.put("ClientVille", villeCli);

        // Date relance
        Date d = (Date) this.rowRelance.getObject("DATE");
        map.put("RelanceDate", dateFormat.format(d));

        SQLRow rowFacture = this.rowRelance.getForeignRow("ID_SAISIE_VENTE_FACTURE");

        // Infos facture
        Long lTotal = (Long) rowFacture.getObject("T_TTC");
        Long lRestant = (Long) this.rowRelance.getObject("MONTANT");
        Long lVerse = new Long(lTotal.longValue() - lRestant.longValue());
        map.put("FactureNumero", rowFacture.getString("NUMERO"));
        map.put("FactureTotal", GestionDevise.currencyToString(lTotal.longValue(), true));
        map.put("FactureRestant", GestionDevise.currencyToString(lRestant.longValue(), true));
        map.put("FactureVerse", GestionDevise.currencyToString(lVerse.longValue(), true));
        map.put("FactureDate", dateFormat2.format((Date) rowFacture.getObject("DATE")));

        Date dFacture = (Date) rowFacture.getObject("DATE");
        SQLRow modeRegRow = rowFacture.getForeignRow("ID_MODE_REGLEMENT");
        Date dateEch = ModeDeReglementSQLElement.calculDate(modeRegRow.getInt("AJOURS"), modeRegRow.getInt("LENJOUR"), dFacture);
        map.put("FactureDateEcheance", dateFormat2.format(dateEch));

        SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());
        sel.addSelect(this.rowRelance.getTable().getKey());
        sel.setWhere(new Where(this.rowRelance.getTable().getField("ID_SAISIE_VENTE_FACTURE"), "=", this.rowRelance.getInt("ID_SAISIE_VENTE_FACTURE")));
        sel.addFieldOrder(this.rowRelance.getTable().getField("DATE").getFullName());
        List<Map> listResult = Configuration.getInstance().getBase().getDataSource().execute(sel.asString());
        if (listResult != null && listResult.size() > 0) {
            Map o = listResult.get(0);
            Number n = (Number) o.get(this.rowRelance.getTable().getKey().getName());
            SQLRow rowOldRelance = this.rowRelance.getTable().getRow(n.intValue());
            Date dOldRelance = (Date) rowOldRelance.getObject("DATE");
            map.put("DatePremiereRelance", dateFormat2.format(dOldRelance));
        } else {
            map.put("DatePremiereRelance", "");
        }

        return map;
    }

    public RelanceSheet(SQLRow row) {
        this.rowRelance = row;
        Date d = (Date) this.rowRelance.getObject("DATE");
        String year = yearFormat.format(d);
        SQLRow rowLettre = this.rowRelance.getForeignRow("ID_TYPE_LETTRE_RELANCE");

        final String string = rowLettre.getString("MODELE");
        System.err.println(this.locationTemplate + "/" + string);
        init(year, string, "RelancePrinter", tuple);
    }

    protected boolean savePDF() {
        return true;
    }

    public String getFileName() {
        String fileName = "Relance_" + AbstractSheetXml.getValidFileName(this.rowRelance.getString("NUMERO"));
        return fileName;
    }
}
