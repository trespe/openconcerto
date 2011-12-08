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
import org.openconcerto.erp.generationDoc.AbstractJOOReportsSheet;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourrierClientSheet extends AbstractJOOReportsSheet {

    private SQLRow rowCourrier;

    public static final String TEMPLATE_ID = "Courrier";
    public static final String TEMPLATE_PROPERTY_NAME = "LocationCourrier";

    public CourrierClientSheet(SQLRow row) {
        super();
        this.rowCourrier = row;
        Date d = (Date) this.rowCourrier.getObject("DATE");
        String year = yearFormat.format(d);
        init(year, "Courrier.odt", "CourrierPrinter");
    }

    /**
     * @return une Map contenant les valeurs à remplacer dans la template
     */
    protected Map<String, Object> createMap() {

        Map<String, Object> m = new HashMap<String, Object>();

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("MODELE_COURRIER_CLIENT");
        SQLRow rowModele = elt.getTable().getRow(this.rowCourrier.getInt("ID_MODELE_COURRIER_CLIENT"));
        String contenu = rowModele.getString("CONTENU");
        m.put("Objet", this.rowCourrier.getString("NOM"));
        m.put("Date", dateFormat.format(this.rowCourrier.getDate("DATE").getTime()));
        int idAdresse = this.rowCourrier.getInt("ID_ADRESSE");

        if (idAdresse > 1) {

            SQLRow rowAdresseClient = this.rowCourrier.getForeignRow("ID_ADRESSE");

            m.put("clientNom", rowAdresseClient.getString("DEST"));
            m.put("clientAdresse", rowAdresseClient.getString("RUE"));

            m.put("codePostal", getVilleCP(rowAdresseClient.getString("VILLE")));
            String villeCli = getVille(rowAdresseClient.getString("VILLE"));
            final Object cedexCli = rowAdresseClient.getObject("CEDEX");
            final boolean hasCedexCli = rowAdresseClient.getBoolean("HAS_CEDEX");

            if (hasCedexCli) {
                villeCli += " CEDEX";
                if (cedexCli != null && cedexCli.toString().trim().length() > 0) {
                    villeCli += " " + cedexCli.toString().trim();
                }
            }

            m.put("ville", villeCli);
        }
        return m;
    }

    public String getFileName() {
        return "Courrier_" + this.rowCourrier.getString("NUMERO");
    }

}
