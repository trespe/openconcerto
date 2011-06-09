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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.map.model.Ville;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.Nombre;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.jdom.Element;

public class OOXMLField extends OOXMLElement {

    private String op = "";

    public OOXMLField(Element eltField, SQLRowAccessor row, SQLElement sqlElt, int id) {
        super(eltField, sqlElt, id);

        String base = eltField.getAttributeValue("base");

        this.op = eltField.getAttributeValue("op");

        this.row = row;
        if ((this.row == null || !this.row.getTable().getSchema().getName().equalsIgnoreCase("Common")) && base != null && base.equalsIgnoreCase("COMMON")) {
            this.row = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        }
        if (row == null) {
            this.row = sqlElt.getTable().getRow(id);
        }

    }

    /**
     * permet d'obtenir la valeur d'un élément field
     * 
     * @param eltField
     * @param row
     * @param elt
     * @param id
     * @return la valeur du composant
     */
    public Object getValue() {

        if (this.row != null && this.row.getID() > 1) {

            // if
            // (this.row.getTable().getName().equalsIgnoreCase(this.elt.getAttributeValue("table")))
            // {
            String field = this.elt.getAttributeValue("name");

            final SQLField sqlField = this.row.getTable().getField(field);
            boolean isForeignField = this.row.getTable().getForeignKeys().contains(sqlField);

            // le champ est une clef etrangere, on recupere la valeur du sous composant
            if (isForeignField && this.elt.getChild("field") != null) {

                String condField = this.elt.getAttributeValue("conditionField");

                if (condField != null && this.row.getTable().getField(condField).getType().getJavaType() == Boolean.class) {
                    final Boolean boolean1 = this.row.getBoolean(condField);
                    if (!(boolean1 != null && boolean1)) {
                        return null;
                    }
                }

                SQLRowAccessor foreignRow = OOXMLCache.getForeignRow(this.row, sqlField);
                if (foreignRow != null && foreignRow.getID() > 1) {
                    final List<Element> children = this.elt.getChildren("field");
                    if (children.size() > 1) {
                        if (isValid()) {

                            String result = "";
                            for (Element ssComposant : children) {
                                OOXMLField childElt = new OOXMLField(ssComposant, foreignRow, this.sqlElt, this.id);
                                final Object valueComposantO = childElt.getValue();
                                result += (valueComposantO == null) ? "" : valueComposantO.toString() + " ";
                            }
                            return result.trim();
                        } else {
                            return "";
                        }
                    } else {
                        if (isValid()) {
                            OOXMLField childElt = new OOXMLField(this.elt.getChild("field"), foreignRow, this.sqlElt, this.id);
                            return childElt.getValue();
                        } else {
                            return "";
                        }
                    }
                } else {
                    return null;
                }
            } else {

                // sinon on recupere directement la valeur

                if (this.op != null && this.op.trim().length() > 0) {
                    String field2 = this.elt.getAttributeValue("name2");
                    String typeComp = this.elt.getAttributeValue("type");
                    Number o = (Number) this.row.getObject(field);
                    Number o2 = (Number) this.row.getObject(field2);

                    if (typeComp != null && typeComp.trim().length() > 0) {

                        // Devise en Long transformée en double
                        if (typeComp.equalsIgnoreCase("Devise")) {
                            long result = calcul(o, o2, this.op).longValue();
                            return Double.valueOf(GestionDevise.currencyToString(result, false));
                        }
                    }

                    return calcul(o, o2, this.op);

                }

                // Liste des valeurs à ne pas afficher
                List<String> listOfExpectedValues = null;
                if (this.elt.getAttributeValue("valuesExpected") != null) {
                    listOfExpectedValues = SQLRow.toList(this.elt.getAttributeValue("valuesExpected"));
                }

                // Champ boolean
                String condField = this.elt.getAttributeValue("conditionField");
                String condValue = this.elt.getAttributeValue("conditionExpValue");

                boolean bIsBooleanCondValid;

                if (condField == null) {
                    bIsBooleanCondValid = true;
                } else {
                    if (this.row.getTable().getField(condField).getType().getJavaType() == Boolean.class) {
                        final Boolean boolean1 = this.row.getBoolean(condField);
                        if (boolean1 != null && boolean1) {
                            bIsBooleanCondValid = true;
                        } else {
                            return null;
                            // bIsBooleanCondValid = false;
                        }
                    } else {
                        bIsBooleanCondValid = false;
                    }
                }

                boolean bIsCondValid = condValue == null || this.row.getObject(condField).toString().equalsIgnoreCase(condValue);
                if (bIsBooleanCondValid || !bIsCondValid) {

                    // Type du champ
                    String typeComp = this.elt.getAttributeValue("type");
                    Object o = getSpecialValue(typeComp);

                    String stringValue;
                    if (o != null) {
                        if (this.elt.getAttributeValue("upperCase") != null) {
                            o = o.toString().toUpperCase();
                        }
                        stringValue = o.toString();
                    } else {
                        Object o2 = this.row.getObject(field);

                        stringValue = (o2 == null) ? "" : o2.toString();
                    }

                    // on ne fait rien si le champ n'est pas à afficher
                    if (listOfExpectedValues == null || ((!listOfExpectedValues.contains(stringValue)) && stringValue.trim().length() > 0)) {
                        String prefix = this.elt.getAttributeValue("prefix");
                        String suffix = this.elt.getAttributeValue("suffix");
                        String display = this.elt.getAttributeValue("display");
                        if (prefix != null || suffix != null) {

                            String result = "";
                            if (prefix != null) {
                                if (prefix.contains("#n")) {
                                    prefix = prefix.replaceAll("#n", "\n");
                                }
                                result += prefix;
                            }

                            if (display == null || !display.equalsIgnoreCase("false")) {
                                result += stringValue;
                            }
                            if (suffix != null) {
                                result += suffix;
                            }

                            return result;
                        } else {
                            if (display == null || !display.equalsIgnoreCase("false")) {
                                return (o == null) ? "" : o;
                            } else {
                                return "";
                            }
                        }
                    }
                }
            }
        }

        return "";

    }

    protected Object getSpecialValue(String typeComp) {
        String field = this.elt.getAttributeValue("name");
        final Object object = this.row.getObject(field);

        // Liste des valeurs à ne pas afficher
        List<String> listOfExpectedValues = null;
        if (this.elt.getAttributeValue("valuesExpected") != null) {
            listOfExpectedValues = SQLRow.toList(this.elt.getAttributeValue("valuesExpected"));
        }

        String stringValue = (object == null) ? "" : object.toString();
        if (typeComp != null && typeComp.trim().length() > 0) {

            // Type spécial

            // Devise en Long transformée en double
            if (typeComp.equalsIgnoreCase("NumeroEcheance")) {
                SQLSelect sel = new SQLSelect(this.row.getTable().getBase());
                sel.addSelect(this.row.getTable().getKey(), "COUNT");
                Where w = new Where(this.row.getTable().getField("DATE"), "<=", this.row.getDate("DATE").getTime());
                w = w.and(new Where(this.row.getTable().getField("ID_AFFAIRE"), "=", this.row.getInt("ID_AFFAIRE")));
                sel.setWhere(w);

                return this.row.getTable().getBase().getDataSource().executeScalar(sel.asString());
            } else {
                if (typeComp.equalsIgnoreCase("Devise")) {
                    Number prix = (Number) object;
                    if (listOfExpectedValues != null) {
                        for (String string : listOfExpectedValues) {
                            Long l = Long.parseLong(string);
                            if (l.longValue() == prix.longValue()) {
                                return "";
                            }
                        }
                    }
                    return new Double(GestionDevise.currencyToString(prix.longValue(), false));
                } else {
                    if (typeComp.equalsIgnoreCase("globalAcompte")) {
                        Long prix = (Long) object;
                        int pourcent = this.row.getInt("POURCENT_ACOMPTE");
                        long l = Math.round(prix.longValue() / (pourcent / 100.0));
                        return new Double(GestionDevise.currencyToString(l, false));
                    } else {
                        if (typeComp.equalsIgnoreCase("DateEcheanceFiche")) {
                            Date d = getDateEch(this.row);
                            if (d != null) {
                                final DateFormat format2 = new SimpleDateFormat("dd/MM/yyyy");
                                return format2.format(d);
                            } else {
                                return "";
                            }
                        } else {
                            if (typeComp.equalsIgnoreCase("MoisEcheanceFiche")) {
                                return getMoisEch(this.row);
                            } else {
                                if (typeComp.equalsIgnoreCase("SituationAdminFiche")) {
                                    return getSituationAdmin(this.row);
                                } else {
                                    if (typeComp.equalsIgnoreCase("AcompteVerse")) {
                                        return getAcompteVerse(this.row);
                                    } else {
                                        if (typeComp.equalsIgnoreCase("CumulPrec")) {

                                            final long cumulPrecedent = getCumulPrecedent(this.row);
                                            return new Double(GestionDevise.currencyToString(cumulPrecedent, false));
                                        } else {
                                            if (typeComp.equalsIgnoreCase("Activite")) {
                                                return object.toString() + getActivite(this.id);
                                            } else {
                                                // Devise exprimée en lettre
                                                if (typeComp.equalsIgnoreCase("DeviseLettre")) {
                                                    Long prix = (Long) object;
                                                    return getLettreFromDevise(prix.longValue());
                                                } else {
                                                    // Proposition associée à la facture (Notre
                                                    // propo N°
                                                    // ...
                                                    // du
                                                    // ...)
                                                    if (typeComp.equalsIgnoreCase("propositionFacture")) {
                                                        return getStringProposition(this.row);
                                                    } else {

                                                        // Ville si null on retourne la valeur du
                                                        // champ
                                                        // de
                                                        // la
                                                        // base
                                                        if (typeComp.equalsIgnoreCase("Ville")) {
                                                            stringValue = (object == null) ? "" : object.toString();
                                                            final String ville = getVille(stringValue);
                                                            if (ville == null) {
                                                                return stringValue;
                                                            } else {
                                                                return ville;
                                                            }
                                                        } else {
                                                            // Code postal de la ville
                                                            if (typeComp.equalsIgnoreCase("ListeVerificateur")) {
                                                                return getListeVerificateur(this.row);
                                                            } else {

                                                                // Code postal de la ville
                                                                if (typeComp.equalsIgnoreCase("VilleCP")) {
                                                                    stringValue = (object == null) ? "" : object.toString();
                                                                    return getVilleCP(stringValue, this.row);
                                                                } else {

                                                                    // Retourne la date d'échéance
                                                                    if (typeComp.equalsIgnoreCase("DateEcheance")) {

                                                                        int idModeReglement = this.row.getInt("ID_MODE_REGLEMENT");
                                                                        Date d = (Date) this.row.getObject("DATE");
                                                                        return getDateEcheance(idModeReglement, d);
                                                                    } else {
                                                                        if (typeComp.equalsIgnoreCase("Jour")) {
                                                                            int day = this.row.getInt(field);
                                                                            stringValue = "le " + String.valueOf(day);
                                                                            if (day == 31) {
                                                                                return "fin de mois";
                                                                            } else {
                                                                                if (day == 0) {
                                                                                    return "Date de facture";
                                                                                } else {

                                                                                    return stringValue;
                                                                                }
                                                                            }
                                                                        } else {
                                                                            if (typeComp.equalsIgnoreCase("Date")) {

                                                                                String datePattern = this.elt.getAttributeValue("DatePattern");
                                                                                if (datePattern == null || datePattern.trim().length() == 0) {
                                                                                    datePattern = "dd/MM/yyyy";
                                                                                }
                                                                                SimpleDateFormat format = new SimpleDateFormat(datePattern);
                                                                                if (object != null) {
                                                                                    Date d = (Date) object;
                                                                                    return format.format(d);
                                                                                } else {
                                                                                    return "";
                                                                                }
                                                                            }
                                                                            if (typeComp.equalsIgnoreCase("initiale")) {
                                                                                stringValue = (object == null) ? "" : object.toString();
                                                                                if (stringValue.trim().length() > 0) {
                                                                                    stringValue = String.valueOf(stringValue.charAt(0));
                                                                                }
                                                                                return stringValue;
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return (object == null) ? "" : object;
    }

    public boolean isValid() {
        String condField = this.elt.getAttributeValue("conditionField");
        String condValue = this.elt.getAttributeValue("conditionExpValue");
        boolean bIsBooleanCondValid = condField == null || this.row.getTable().getField(condField).getType().getJavaType() == Boolean.class && this.row.getBoolean(condField);
        boolean bIsCondValid = condValue == null || this.row.getObject(condField).toString().equalsIgnoreCase(condValue);
        return bIsBooleanCondValid || !bIsCondValid;
    }

    private static long getCumulPrecedent(SQLRowAccessor rowFact) {

        long cumul = 0;

        // On recupere les missions associées
        SQLTable tableElt = Configuration.getInstance().getRoot().findTable("SAISIE_VENTE_FACTURE_ELEMENT");
        Collection<? extends SQLRowAccessor> factElts = rowFact.getReferentRows(tableElt);

        for (SQLRowAccessor row : factElts) {
            Collection<? extends SQLRowAccessor> rowsElt = row.getForeign("ID_MISSION").getReferentRows(tableElt);
            for (SQLRowAccessor row2 : rowsElt) {
                SQLRowAccessor rowFacture = row2.getForeign("ID_SAISIE_VENTE_FACTURE");
                if (rowFacture.getDate("DATE").before(rowFact.getDate("DATE"))) {
                    cumul += row2.getLong("T_PV_HT");
                }
            }
        }

        return cumul;
    }

    private static Date getDateEch(SQLRowAccessor rowFiche) {

        Date d = null;

        // On recupere les missions associées
        Collection<? extends SQLRowAccessor> factElts = rowFiche.getReferentRows(rowFiche.getTable().getTable("FICHE_RENDEZ_VOUS_ELEMENT"));

        if (factElts != null && factElts.size() > 0) {
            Object[] rows = factElts.toArray();
            int i = 0;
            Calendar date;
            while (i < factElts.size()) {
                date = ((SQLRow) rows[i]).getDate("DATE_ECHEANCE");
                if (date != null) {
                    d = date.getTime();
                    break;
                }
                i++;
            }
        }

        return d;
    }

    private static String getMoisEch(SQLRowAccessor rowFiche) {

        String mois = "";

        // On recupere les missions associées
        SQLTable tableElt = Configuration.getInstance().getRoot().findTable("FICHE_RENDEZ_VOUS_ELEMENT");
        Collection<? extends SQLRowAccessor> factElts = rowFiche.getReferentRows(tableElt);

        if (factElts != null && factElts.size() > 0) {
            Object[] rows = factElts.toArray();
            int i = 0;
            while (i < factElts.size()) {
                int idMois = ((SQLRow) rows[i]).getInt("ID_MOIS_PREV");
                if (idMois > 1) {
                    mois = ((SQLRow) rows[i]).getForeign("ID_MOIS_PREV").getString("NOM");
                    break;
                }
                i++;
            }
        }

        return mois;
    }

    private static String getSituationAdmin(SQLRowAccessor rowFiche) {

        SQLTable tableElt = Configuration.getInstance().getRoot().findTable("FICHE_RENDEZ_VOUS_ELEMENT");
        Collection<? extends SQLRowAccessor> rows = rowFiche.getReferentRows(tableElt);

        String text = "";
        List<String> l = new ArrayList<String>();
        for (SQLRowAccessor row : rows) {

            final String situation = row.getString("SITUATION_ADMIN");
            if (!l.contains(situation)) {
                text += situation + ", ";
                l.add(situation);
            }
        }

        if (text.length() > 0) {
            text = text.substring(0, text.length() - 2);
        }

        return text;
    }

    private static List<Integer> getListId(Collection<SQLRow> rowFactElts) {
        return getListId(rowFactElts, null);
    }

    private static List<Integer> getListId(Collection<SQLRow> rowFactElts, String field) {
        List<Integer> l = new ArrayList<Integer>();

        for (SQLRow row : rowFactElts) {
            if (field == null) {
                l.add(row.getID());
            } else {
                l.add(row.getInt(field));
            }
        }
        return l;
    }

    /**
     * 
     * @param idAffaire
     * @return la liste des activités séparées par des -
     */
    private static String getActivite(int idAffaire) {

        SQLElement eltAffaire = Configuration.getInstance().getDirectory().getElement("AFFAIRE");
        SQLElement eltAffaireElt = Configuration.getInstance().getDirectory().getElement("AFFAIRE_ELEMENT");
        List<SQLRow> s = eltAffaire.getTable().getRow(idAffaire).getReferentRows(eltAffaireElt.getTable());

        String codes = "";
        List<String> l = new ArrayList<String>(s.size());
        for (SQLRow row : s) {

            final String string = row.getString("ACTIVITE");
            if (!l.contains(string)) {
                l.add(string);
                String code = "-" + string;
                codes += code;
            }
        }

        return codes;
    }

    /**
     * transforme une devise exprimée en chiffres en lettres
     * 
     * @param value
     * @return la devise exprimée en lettres
     */
    private static String getLettreFromDevise(long value) {

        StringBuffer result = new StringBuffer();

        Long decimal = Long.valueOf(value % 100);
        Long entier = Long.valueOf(value / 100);

        Nombre n1 = new Nombre(entier.intValue());
        Nombre n2 = new Nombre(decimal.intValue());

        result.append(n1.getText() + " euros");

        if (decimal.intValue() > 0) {
            result.append(" et " + n2.getText() + " cents");
        }
        if (result != null && result.length() > 0) {
            return result.toString().replaceFirst(String.valueOf(result.charAt(0)), String.valueOf(result.charAt(0)).toUpperCase());
        } else {
            return result.toString();
        }
    }

    private static String getVille(final String name) {

        Ville ville = Ville.getVilleFromVilleEtCode(name);
        if (ville == null) {
            // SwingUtilities.invokeLater(new Runnable() {
            // public void run() {
            // JOptionPane.showMessageDialog(null, "La ville " + "\"" + name + "\"" + " est
            // introuvable! Veuillez corriger l'erreur!");
            // }
            // });
            return null;
        }
        return ville.getName();
    }

    private static String getVilleCP(String name, SQLRowAccessor row) {
        Ville ville = Ville.getVilleFromVilleEtCode(name);
        if (ville == null) {
            if (row.getTable().contains("CODE_POSTAL")) {
                Object o = row.getObject("CODE_POSTAL");
                return (o == null) ? null : o.toString();
            } else {

                return null;
            }
        }
        return ville.getCodepostal();
    }

    protected static void initCacheAffaireCT(SQLRow row) {
        SQLSelect sel = new SQLSelect(row.getTable().getBase());
        final SQLTable tableFactElt = row.getTable().getTable("SAISIE_VENTE_FACTURE_ELEMENT");
        final SQLTable tableAffElt = row.getTable().getTable("AFFAIRE_ELEMENT");

        sel.addSelectStar(tableFactElt);
        sel.addJoin("LEFT", tableFactElt.getField("ID_AFFAIRE_ELEMENT"));

        Where w = new Where(sel.getAlias(tableAffElt.getField("ID_AFFAIRE")), "=", row.getInt("ID_AFFAIRE"));
        w = w.or(new Where(sel.getAlias(tableFactElt.getField("ID_SAISIE_VENTE_FACTURE")), "=", row.getID()));
        sel.setWhere(w);
        List<SQLRowAccessor> l = (List<SQLRowAccessor>) row.getTable().getBase().getDataSource().execute(sel.asString(), new SQLRowListRSH(tableFactElt, true));
        System.err.println(l.size());
        for (SQLRowAccessor sqlRow : l) {

            // On cache les elt de factures references par les elements d'affaire
            SQLRowAccessor affElt = OOXMLCache.getForeignRow(sqlRow, sqlRow.getTable().getField("ID_AFFAIRE_ELEMENT"));
            Map<SQLRowAccessor, Map<SQLTable, List<SQLRowAccessor>>> cacheReferent = OOXMLCache.getCacheReferent();
            if (affElt != null) {

                Map<SQLTable, List<SQLRowAccessor>> m = cacheReferent.get(affElt);
                if (m == null) {
                    m = new HashMap<SQLTable, List<SQLRowAccessor>>();
                    cacheReferent.put(affElt, m);
                }
                List<SQLRowAccessor> list = m.get(sqlRow.getTable());
                if (list == null) {
                    list = new ArrayList();
                    m.put(sqlRow.getTable(), list);
                }
                list.add(sqlRow);
            }
            // On cache les elements de
            if (sqlRow.getInt("ID_SAISIE_VENTE_FACTURE") == row.getID()) {
                Map<SQLTable, List<SQLRowAccessor>> m = cacheReferent.get(row);
                if (m == null) {
                    m = new HashMap<SQLTable, List<SQLRowAccessor>>();
                    cacheReferent.put(row, m);
                }
                List<SQLRowAccessor> list = m.get(sqlRow.getTable());
                if (list == null) {
                    list = new ArrayList<SQLRowAccessor>();
                    m.put(sqlRow.getTable(), list);
                }
                list.add(sqlRow);

            }
        }
    }

    private static Double getAcompteVerse(SQLRowAccessor row) {

        SQLRowAccessor rowAff = OOXMLCache.getForeignRow(row, row.getTable().getField("ID_AFFAIRE"));
        List<? extends SQLRowAccessor> list = OOXMLCache.getReferentRows(rowAff, row.getTable());

        double total = 0.0;
        for (SQLRowAccessor sqlRow : list) {
            Calendar date = row.getDate("DATE");
            Calendar date2 = sqlRow.getDate("DATE");
            if (date2.before(date)) {
                total += sqlRow.getFloat("T_HT");
            }
        }

        // On cumul ce qui a déja était verse
        List<? extends SQLRowAccessor> listElt = OOXMLCache.getReferentRows(rowAff, rowAff.getTable().getTable("AFFAIRE_ELEMENT"));
        for (SQLRowAccessor sqlRow : listElt) {
            total += sqlRow.getFloat("TOTAL_HT_REALISE");
        }

        return total / 100.0;
    }

    private static Number calcul(Object o1, Object o2, String op) {

        double d1 = (o1 == null) ? 0 : Double.parseDouble(o1.toString());
        double d2 = (o2 == null) ? 0 : Double.parseDouble(o2.toString());

        if (op.equalsIgnoreCase("+")) {
            return d1 + d2;
        } else {
            if (op.equalsIgnoreCase("-")) {
                return d1 - d2;
            } else {
                if (op.equalsIgnoreCase("*")) {
                    return d1 * d2;
                } else {
                    if (op.equalsIgnoreCase("/") && d2 != 0) {
                        return d1 / d2;
                    }
                }
            }
        }
        return 0;
    }

}
