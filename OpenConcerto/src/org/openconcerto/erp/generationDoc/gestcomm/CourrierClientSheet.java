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
import org.openconcerto.erp.generationDoc.AbstractSheetXml;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.Tuple2;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourrierClientSheet extends AbstractJOOReportsSheet {

    private SQLRow rowCourrier;
    private DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");

    private static final Tuple2<String, String> tuple = Tuple2.create("LocationCourrier", "Courrier");

    public static Tuple2<String, String> getTuple2Location() {
        return tuple;
    }

    public CourrierClientSheet(SQLRow row) {
        super();
        this.rowCourrier = row;
        Date d = (Date) this.rowCourrier.getObject("DATE");
        String year = yearFormat.format(d);
        init(year, "Courrier.odt", "CourrierPrinter", tuple);
    }

    /**
     * @return une Map contenant les valeurs à remplacer dans la template
     */
    protected Map createMap() {

        Map<String, Object> m = new HashMap<String, Object>();

        SQLElement eltAffaire = Configuration.getInstance().getDirectory().getElement("AFFAIRE");
        SQLRow rowAffaire = eltAffaire.getTable().getRow(this.rowCourrier.getInt("ID_AFFAIRE"));

        SQLElement eltProp = Configuration.getInstance().getDirectory().getElement("PROPOSITION");
        final int idProp = rowAffaire.getInt("ID_PROPOSITION");
        SQLRow rowProp = eltProp.getTable().getRow(idProp);

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("MODELE_COURRIER_CLIENT");
        SQLRow rowModele = elt.getTable().getRow(this.rowCourrier.getInt("ID_MODELE_COURRIER_CLIENT"));
        String contenu = rowModele.getString("CONTENU");

        // Missions
        SQLTable tableElt = Configuration.getInstance().getRoot().findTable("AFFAIRE_ELEMENT");
        List<SQLRow> rowsMission = rowAffaire.getReferentRows(tableElt);
        String listeMissions = "";
        for (SQLRow rowMission : rowsMission) {
            listeMissions += rowMission.getString("NOM") + ", ";
        }

        listeMissions = listeMissions.trim();
        if (listeMissions.length() != 0) {
            listeMissions = listeMissions.substring(0, listeMissions.length() - 1);
        }

        contenu = contenu.replaceAll("%mission", listeMissions);
        m.put("Corps", contenu);
        m.put("Date", dateFormat.format(this.rowCourrier.getDate("DATE").getTime()));
        if (idProp > 1) {
            m.put("Numero", rowProp.getString("NUMERO"));
        } else {
            m.put("Numero", "");
        }

        m.put("NumeroAffaire", rowAffaire.getString("NUMERO"));
        m.put("NomAffaire", rowAffaire.getString("OBJET"));
        m.put("Objet", this.rowCourrier.getString("NOM"));

        // Initiale Comm
        SQLElement eltComm = Configuration.getInstance().getDirectory().getElement("COMMERCIAL");
        SQLRow rowCommercial = eltComm.getTable().getRow(rowProp.getInt("ID_COMMERCIAL"));
        String initialeComm = getInitiales(rowCommercial);

        m.put("IR", initialeComm);

        // initiale Secr
        SQLElement eltSecretaire = Configuration.getInstance().getDirectory().getElement("SECRETAIRE");
        SQLRow rowSecretaire = eltSecretaire.getTable().getRow(this.rowCourrier.getInt("ID_SECRETAIRE"));
        String initialeSecr = getInitiales(rowSecretaire);

        m.put("IS", initialeSecr);
        SQLElement eltContact = Configuration.getInstance().getDirectory().getElement("CONTACT");
        SQLElement eltTitre = Configuration.getInstance().getDirectory().getElement("TITRE_PERSONNEL");
        SQLRow rowContactCom = eltContact.getTable().getRow(rowAffaire.getInt("ID_CONTACT_COM"));
        SQLRow rowTitre = eltTitre.getTable().getRow(rowContactCom.getInt("ID_TITRE_PERSONNEL"));
        String correspondant = rowTitre.getString("NOM");

        String contact = rowTitre.getString("NOM");
        contact += " " + rowContactCom.getString("PRENOM");
        contact += " " + rowContactCom.getString("NOM");

        m.put("Correspondant", correspondant);
        m.put("Contact", contact);

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
        } else {
            // Client
            SQLRow rowClient;
                rowClient = rowAffaire.getForeignRow("ID_CLIENT");
            m.put("clientNom", rowClient.getString("FORME_JURIDIQUE") + " " + rowClient.getString("NOM"));

            SQLRow rowAdresseClient = rowClient.getForeignRow("ID_ADRESSE");

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
        String fileName = "Courrier_" + AbstractSheetXml.getValidFileName(this.rowCourrier.getString("NUMERO"));
        return fileName;
    }

}
