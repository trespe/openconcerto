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
 
 package org.openconcerto.erp.generationDoc;

import org.openconcerto.erp.core.finance.payment.element.ModeDeReglementSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.utils.GestionDevise;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jdom.Element;

public class OOXMLElement {
    protected Element elt;
    protected SQLElement sqlElt;
    protected int id;
    protected SQLRowAccessor row = null;

    public OOXMLElement(Element elt, SQLElement sqlElt, int id) {
        this.elt = elt;
        this.sqlElt = sqlElt;
        this.id = id;
    }

    public OOXMLElement(Element elt, SQLElement sqlElt, int id, SQLRowAccessor row) {
        this.elt = elt;
        this.sqlElt = sqlElt;
        this.id = id;
        this.row = row;
    }

    public Object getValue() {
        Object res = "";

        final String attributeValue = this.elt.getAttributeValue("type");

        if (attributeValue.equalsIgnoreCase("InitialesUtilisateur")) {
            return getInitialesUser();
        }

        if (attributeValue.equalsIgnoreCase("InitialesUtilisateurModif")) {
            return getInitialesUserModify();
        }

        if (attributeValue.equalsIgnoreCase("InitialesUtilisateurCreate")) {
            return getInitialesUserCreate();
        }

        if (attributeValue.equalsIgnoreCase("propositionFacture")) {
            return getProposition(row);
        }

        if (attributeValue.equalsIgnoreCase("ListeVerificateur")) {
            return getListeVerificateur(row);
        }

        if (attributeValue.equalsIgnoreCase("TotalHTTable")) {
            return getTotalHTTable(row);
        }

        if (attributeValue.equalsIgnoreCase("codesMissions")) {
            return getCodesMissions(this.id);
        }

        if (attributeValue.equalsIgnoreCase("DateEcheance")) {
            int idModeReglement = row.getInt("ID_MODE_REGLEMENT");
            Date d = (Date) row.getObject("DATE");
            return getDateEcheance(idModeReglement, d);
        }

        final List<Element> eltFields = this.elt.getChildren("field");

        if (eltFields != null) {
            if (eltFields.size() > 1) {
                String result = "";
                for (Element eltField : eltFields) {

                    OOXMLField field = new OOXMLField(eltField, this.row, this.sqlElt, this.id);

                    Object value = field.getValue();
                    if (value != null) {
                        result += value.toString() + " ";
                    }
                }
                res = result;
            } else {
                OOXMLField field = new OOXMLField(eltFields.get(0), this.row, this.sqlElt, this.id);
                res = field.getValue();
            }
        }
        return res;
    }

    private String getInitialesUserModify() {

        SQLRowAccessor rowUser = this.row.getForeign("ID_USER_COMMON_MODIFY");
        String s = rowUser.getString("NOM");
        String s2 = rowUser.getString("PRENOM");

        StringBuffer result = new StringBuffer(4);
        if (s2 != null && s2.trim().length() > 0) {
            result.append(s2.charAt(0));
        }
        if (s != null && s.trim().length() > 0) {
            result.append(s.charAt(0));
        }
        return result.toString();
    }

    private String getInitialesUserCreate() {
        SQLRowAccessor rowUser = this.row.getForeign("ID_USER_COMMON_CREATE");
        String s = rowUser.getString("NOM");
        String s2 = rowUser.getString("PRENOM");

        StringBuffer result = new StringBuffer(4);
        if (s2 != null && s2.trim().length() > 0) {
            result.append(s2.charAt(0));
        }
        if (s != null && s.trim().length() > 0) {
            result.append(s.charAt(0));
        }
        return result.toString();
    }

    private String getInitialesUser() {

        String s = UserManager.getInstance().getCurrentUser().getLastName();
        String s2 = UserManager.getInstance().getCurrentUser().getName();

        StringBuffer result = new StringBuffer(4);
        if (s2 != null && s2.trim().length() > 0) {
            result.append(s2.charAt(0));
        }
        if (s != null && s.trim().length() > 0) {
            result.append(s.charAt(0));
        }
        return result.toString();
    }

    /**
     * String proposition préventec
     * 
     * @param row
     * @return une string du format Proposition + numero + du + date
     */
    protected String getProposition(SQLRowAccessor row) {
        String source = row.getString("SOURCE");
        int idSource = row.getInt("IDSOURCE");

        if (source.equalsIgnoreCase("PROPOSITION") && idSource > 1) {
            SQLElement eltProp = Configuration.getInstance().getDirectory().getElement("PROPOSITION");
            SQLRow rowProp = eltProp.getTable().getRow(idSource);
            return getStringProposition(rowProp);
        }
        return "";
    }

    public static DateFormat format = new SimpleDateFormat("dd/MM/yyyy");

    protected String getStringProposition(SQLRowAccessor rowProp) {

        return "Notre proposition " + rowProp.getString("NUMERO") + " du " + format.format(rowProp.getObject("DATE"));
    }

    /**
     * Liste des vérificateur séparée par des virgules, premiere lettre du prenom + nom
     * 
     * @param rowFact
     * @return
     */
    protected String getListeVerificateur(SQLRowAccessor rowFact) {
        SQLTable tableElt = Configuration.getInstance().getRoot().findTable("SAISIE_VENTE_FACTURE_ELEMENT");
        SQLTable tablePourcent = Configuration.getInstance().getRoot().findTable("POURCENT_SERVICE");
        Collection<? extends SQLRowAccessor> rows = rowFact.getReferentRows(tableElt);
        List<SQLRowAccessor> l = new ArrayList<SQLRowAccessor>();

        StringBuffer result = new StringBuffer();
        for (SQLRowAccessor row : rows) {

            Collection<? extends SQLRowAccessor> s = row.getReferentRows(tablePourcent);

            for (SQLRowAccessor row2 : s) {
                SQLRowAccessor rowVerif = row2.getForeign("ID_VERIFICATEUR");

                if (rowVerif != null && !l.contains(rowVerif)) {
                    String prenom = rowVerif.getString("PRENOM");
                    if (prenom.length() > 1) {
                        prenom = String.valueOf(prenom.charAt(0));
                    }
                    result.append(prenom + ".");
                    String nom = rowVerif.getString("NOM");
                    if (nom.length() > 1) {
                        nom = String.valueOf(nom.charAt(0));
                    }

                    result.append(nom + ", ");
                    l.add(rowVerif);
                }
            }
        }
        if (result.length() > 0) {
            result.deleteCharAt(result.length() - 1);
            result.deleteCharAt(result.length() - 1);
        }

        return result.toString();
    }

    public Double getTotalHTTable(SQLRowAccessor rowFact) {

        SQLTable tableElt = Configuration.getInstance().getRoot().findTable("SAISIE_VENTE_FACTURE_ELEMENT");
        Collection<? extends SQLRowAccessor> set = rowFact.getReferentRows(tableElt);
        long total = 0;
        for (SQLRowAccessor row : set) {
            total += row.getLong("T_PV_HT");
        }

        return new Double(GestionDevise.currencyToString(total, false));
    }

    /**
     * 
     * @param idAffaire
     * @return la liste des noms des missions séparés par des virgules
     */
    protected String getCodesMissions(int idAffaire) {

        SQLElement eltAffaire = Configuration.getInstance().getDirectory().getElement("AFFAIRE");
        SQLElement eltAffaireElt = Configuration.getInstance().getDirectory().getElement("AFFAIRE_ELEMENT");
        List<SQLRow> s = eltAffaire.getTable().getRow(idAffaire).getReferentRows(eltAffaireElt.getTable());

        String codes = "";
        List<String> l = new ArrayList<String>(s.size());
        for (SQLRow row : s) {

            final String string = row.getString("NOM");
            if (!l.contains(string)) {
                l.add(string);
                String code = string;
                codes += code + ", ";
            }
        }
        if (codes.trim().length() > 0) {
            codes = codes.trim().substring(0, codes.trim().length() - 1);
        }

        return codes;

    }

    /**
     * Calcul la date d'échéance d'un élément par rapport au mode de reglement et à la date
     * d'émission
     * 
     * @param idModeRegl
     * @param currentDate
     * @return la date d'échéance au format dd/MM/yy
     */
    protected String getDateEcheance(int idModeRegl, Date currentDate) {
        final DateFormat format2 = new SimpleDateFormat("dd/MM/yyyy");
        SQLElement eltModeRegl = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
        SQLRow row = eltModeRegl.getTable().getRow(idModeRegl);
        int aJ = row.getInt("AJOURS");
        int nJ = row.getInt("LENJOUR");
        if (aJ + nJ == 0) {
            if (row.getBoolean("DATE_FACTURE")) {
                return Configuration.getInstance().getTranslator().getLabelFor(row.getTable().getField("DATE_FACTURE"));
            } else {
                return " ";
            }
        }
        String s = format2.format(ModeDeReglementSQLElement.calculDate(aJ, nJ, currentDate));
        System.err.println(s);
        return s;
        // return format2.format(ModeDeReglementSQLElement.calculDate(aJ, nJ, currentDate));
    }

    public boolean isTypeReplace() {
        // remplacement d'un pattern contenu dans la cellule
        return this.elt.getAttributeValue("type").equalsIgnoreCase("Replace");
    }

    public String getReplacePattern() {
        return this.elt.getAttributeValue("replacePattern");
    }

    public boolean isMultilineAuto() {
        // gestion manuel du multiligne
        final String multiLineValue = this.elt.getAttributeValue("controleMultiline");
        return (multiLineValue == null) ? true : !multiLineValue.equalsIgnoreCase("false");
    }

}
