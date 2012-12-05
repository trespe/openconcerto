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
 
 package org.openconcerto.erp.core.sales.quote.report;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.generationDoc.AbstractJOOReportsSheet;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.utils.GestionDevise;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DevisTextSheet extends AbstractJOOReportsSheet {

    private SQLRow row;

    public static final String TEMPLATE_ID = DevisXmlSheet.TEMPLATE_ID;

    @Override
    public String getDefaultTemplateID() {
        return TEMPLATE_ID;
    }

    @Override
    public String getDefaultLocationProperty() {
        return DevisXmlSheet.TEMPLATE_PROPERTY_NAME;
    }

    /**
     * @return une Map contenant les valeurs à remplacer dans la template
     */
    protected Map createMap() {

        Map<String, Object> m = new HashMap<String, Object>();

        SQLRow rowClient;
        final SQLRow clientRowNX = this.row.getForeignRow("ID_CLIENT");
            rowClient = clientRowNX;
        SQLRow rowAdresse = rowClient.getForeignRow("ID_ADRESSE");

        // Client compte
        SQLRow rowCompteClient = clientRowNX.getForeignRow("ID_COMPTE_PCE");
        String numero = rowCompteClient.getString("NUMERO");
        m.put("ClientNumeroCompte", numero);

        // Infos Client
        m.put("ClientType", rowClient.getString("FORME_JURIDIQUE"));
        m.put("ClientNom", rowClient.getString("NOM"));
        m.put("ClientTel", rowClient.getString("TEL"));
        m.put("ClientTelP", rowClient.getString("TEL_P"));
        m.put("ClientFax", rowClient.getString("FAX"));
        m.put("ClientMail", rowClient.getString("MAIL"));
        String villeCli = getVille(rowAdresse.getString("VILLE"));
        final Object cedexCli = rowAdresse.getObject("CEDEX");
        final boolean hasCedexCli = rowAdresse.getBoolean("HAS_CEDEX");

        if (hasCedexCli) {
            villeCli += " CEDEX";
            if (cedexCli != null && cedexCli.toString().trim().length() > 0) {
                villeCli += " " + cedexCli.toString().trim();
            }
        }

        final String adr = rowAdresse.getString("RUE") + "\n" + getVilleCP(rowAdresse.getString("VILLE")) + " " + villeCli;
        m.put("ClientAdresse", adr);
        if (this.row.getBoolean("ADRESSE_IDENTIQUE")) {
            m.put("ClientInterv", "");
        } else {
            SQLRow rowIntervention = this.row.getForeignRow("ID_ADRESSE");
            String villeInter = getVille(rowIntervention.getString("VILLE"));
            final Object cedexInter = rowIntervention.getObject("CEDEX");
            final boolean hasCedexInter = rowIntervention.getBoolean("HAS_CEDEX");

            if (hasCedexInter) {
                villeInter += " CEDEX";
                if (cedexInter != null && cedexInter.toString().trim().length() > 0) {
                    villeInter += " " + cedexInter.toString().trim();
                }
            }

            final String adrInter = rowIntervention.getString("RUE") + "\n" + getVilleCP(rowIntervention.getString("VILLE")) + " " + villeInter;

            m.put("ClientInterv", adrInter);
        }

        SQLRow rowContact = this.row.getForeignRow("ID_CONTACT");
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        m.put("Date", format.format(this.row.getDate("DATE").getTime()));

        if (rowContact != null && rowContact.getID() > 1) {
            m.put("RespNom", rowContact.getObject("NOM"));
            m.put("RespPrenom", rowContact.getObject("PRENOM"));
            m.put("RespMobile", rowContact.getObject("TEL_MOBILE"));
            m.put("RespTel", rowContact.getObject("TEL_DIRECT"));
            m.put("RespFax", rowContact.getObject("FAX"));
            m.put("RespMail", rowContact.getObject("EMAIL"));
        }

        m.put("MontantHT", GestionDevise.currencyToString(this.row.getLong("T_HT")));
        m.put("MontantTTC", GestionDevise.currencyToString(this.row.getLong("T_TTC")));
        m.put("TVA", this.row.getForeignRow("ID_TAXE").getString("TAUX"));
        m.put("Numero", this.row.getString("NUMERO"));
        m.put("Référence", this.row.getString("OBJET"));
        final SQLRow foreignRow = this.row.getForeignRow("ID_COMMERCIAL");
        m.put("Technicien", foreignRow.getString("PRENOM") + " " + foreignRow.getString("NOM"));
        return m;
    }

    public DevisTextSheet(SQLRow row) {
        super();
        this.row = row;
        this.askOverwriting = true;
        Date d = (Date) this.row.getObject("DATE");
        String year = yearFormat.format(d);
        init(year, "Devis.odt", "DevisPrinter");
    }

    protected boolean savePDF() {
        return true;
    }

    @Override
    protected String getName() {
        String fileName = "Devis_" + this.row.getString("NUMERO");
        return fileName;
    }

    public String getTemplateId() {
        return "sales.quote.text";
    }
}
