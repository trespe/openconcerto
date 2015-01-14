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
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
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
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.Tuple2;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Attribute;
import org.jdom.Element;

public class OOXMLField extends OOXMLElement {

    private String op = "";

    public OOXMLField(Element eltField, SQLRowAccessor row, SQLElement sqlElt, int id, SQLRow rowLanguage, OOXMLCache cache) {
        super(eltField, sqlElt, id, rowLanguage, cache);

        String base = eltField.getAttributeValue("base");
        this.op = eltField.getAttributeValue("op");

        this.row = row;
        if ((this.row == null || !this.row.getTable().getSchema().getName().equalsIgnoreCase("Common")) && base != null && base.equalsIgnoreCase("COMMON")) {
            this.row = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        }
        if (this.row == null) {
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

        if (this.row != null && !this.row.isUndefined()) {

            // if
            // (this.row.getTable().getName().equalsIgnoreCase(this.elt.getAttributeValue("table")))
            // {
            String field = this.elt.getAttributeValue("name");

            final SQLField sqlField = (field == null || field.trim().length() == 0) ? null : this.row.getTable().getField(field);
            boolean isForeignField = (sqlField == null) ? false : this.row.getTable().getForeignKeys().contains(sqlField);

            // le champ est une clef etrangere, on recupere la valeur du sous composant
            if (isForeignField && this.elt.getChild("field") != null) {

                String condField = this.elt.getAttributeValue("conditionField");

                if (condField != null && this.row.getTable().getField(condField).getType().getJavaType() == Boolean.class) {
                    final Boolean boolean1 = this.row.getBoolean(condField);
                    if (!(boolean1 != null && boolean1)) {
                        return null;
                    }
                }

                SQLRowAccessor foreignRow = cache.getForeignRow(this.row, sqlField);
                if (foreignRow != null && foreignRow.getID() > 1) {
                    final List<Element> children = this.elt.getChildren("field");
                    if (children.size() > 1) {
                        if (isValid()) {

                            String result = "";
                            for (Element ssComposant : children) {
                                OOXMLField childElt = new OOXMLField(ssComposant, foreignRow, this.sqlElt, this.id, this.rowLanguage, cache);
                                final Object valueComposantO = childElt.getValue();
                                result += (valueComposantO == null) ? "" : valueComposantO.toString() + " ";
                            }
                            String cellSize = this.elt.getAttributeValue("cellSize");
                            if (cellSize != null && cellSize.trim().length() != 0) {
                                result = splitStringCell(cellSize, result);
                            }
                            return result.trim();
                        } else {
                            return "";
                        }
                    } else {
                        if (isValid()) {
                            OOXMLField childElt = new OOXMLField(this.elt.getChild("field"), foreignRow, this.sqlElt, this.id, this.rowLanguage, cache);
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

                    Number o2;
                    if (field2 != null && field2.trim().length() > 0) {
                        o2 = (Number) this.row.getObject(field2);
                    } else {
                        o2 = Double.parseDouble(this.elt.getAttributeValue("number"));
                    }

                    if (typeComp != null && typeComp.trim().length() > 0) {

                        // Devise en Long transformée en double
                        if (typeComp.equalsIgnoreCase("Devise")) {

                            BigDecimal result = calcul(o, o2, this.op);
                            if (o instanceof Long) {
                                long resultLong = Math.round(result.doubleValue());
                                return Double.valueOf(GestionDevise.currencyToString(resultLong, false));
                            } else {
                                return result;
                            }
                        }
                    }

                    return calcul(o, o2, this.op);

                }

                // Liste des valeurs à ne pas afficher
                List<String> listOfExcludedValues = null;
                if (this.elt.getAttributeValue("valuesExpected") != null) {
                    listOfExcludedValues = SQLRow.toList(this.elt.getAttributeValue("valuesExpected"));
                }
                List<Element> excludeValue = this.elt.getChildren("exclude");
                if (excludeValue != null && excludeValue.size() > 0) {
                    if (listOfExcludedValues == null) {
                        listOfExcludedValues = new ArrayList<String>();
                    } else {
                        listOfExcludedValues = new ArrayList<String>(listOfExcludedValues);
                    }

                    for (Element element : excludeValue) {
                        String attributeValue = element.getAttributeValue("value");
                        listOfExcludedValues.add(attributeValue);
                    }
                }

                // Champ boolean
                String condField = this.elt.getAttributeValue("conditionField");
                String condValue = this.elt.getAttributeValue("conditionExpValue");

                boolean bIsCondValid = condValue == null || !this.row.getObject(condField).toString().equalsIgnoreCase(condValue);
                if (condValue == null) {
                    boolean bIsBooleanCondValid = false;
                    if (condField == null) {
                        bIsBooleanCondValid = true;
                    } else {
                        if (this.row.getTable().getField(condField).getType().getJavaType() == Boolean.class) {
                            final Boolean boolean1 = this.row.getBoolean(condField);
                            if (boolean1 != null && boolean1) {
                                bIsBooleanCondValid = true;
                            }
                        }
                    }
                    bIsCondValid = bIsCondValid && bIsBooleanCondValid;
                } else {
                    System.err.println();
                }
                if (bIsCondValid) {

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
                    if (listOfExcludedValues == null || ((!listOfExcludedValues.contains(stringValue)) && stringValue.trim().length() > 0)) {
                        String prefix = this.elt.getAttributeValue("prefix");
                        String suffix = this.elt.getAttributeValue("suffix");
                        String display = this.elt.getAttributeValue("display");
                        String cellSize = this.elt.getAttributeValue("cellSize");
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
                            if (cellSize != null && cellSize.trim().length() != 0) {
                                result = splitStringCell(cellSize, result);
                            }
                            return result;
                        } else {
                            if (display == null || !display.equalsIgnoreCase("false")) {
                                if (cellSize != null && cellSize.trim().length() != 0 && o != null) {
                                    return splitStringCell(cellSize, o.toString());
                                } else {
                                    return (o == null) ? "" : o;
                                }
                            } else {
                                return "";
                            }
                        }
                    }
                }
            }
        }

        return null;

    }

    protected String splitStringCell(String cellSize, String result) {
        try {
            int nbCar = Integer.parseInt(cellSize);
            result = StringUtils.splitString(result, nbCar);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return result;
    }

    protected Object getSpecialValue(String typeComp) {

        SpreadSheetCellValueProvider provider = SpreadSheetCellValueProviderManager.get(typeComp);
        if (provider != null) {
            final SpreadSheetCellValueContext context = new SpreadSheetCellValueContext(this.row);
            List<Attribute> attrs = this.elt.getAttributes();
            for (Attribute attr : attrs) {
                context.put(attr.getName(), attr.getValue());
            }
            return provider.getValue(context);
        }

        String field = this.elt.getAttributeValue("name");
        final Object result = this.row.getObject(field);

        // Liste des valeurs à ne pas afficher
        List<String> listOfExpectedValues = null;
        if (this.elt.getAttributeValue("valuesExpected") != null) {
            listOfExpectedValues = SQLRow.toList(this.elt.getAttributeValue("valuesExpected"));
        }

        String stringValue = (result == null) ? "" : result.toString();
        if (typeComp != null && typeComp.trim().length() > 0) {

            // Type spécial

            // Devise en Long transformée en double
            if (typeComp.equalsIgnoreCase("NumeroEcheance")) {
                SQLSelect sel = new SQLSelect(this.row.getTable().getBase());
                sel.addSelect(this.row.getTable().getKey(), "COUNT");
                Where w = new Where(this.row.getTable().getField("DATE"), "<=", this.row.getDate("DATE").getTime());
                sel.setWhere(w);
                return this.row.getTable().getBase().getDataSource().executeScalar(sel.asString());
            } else if (typeComp.equalsIgnoreCase("Devise")) {
                Number prix = (Number) result;
                if (listOfExpectedValues != null) {
                    for (String string : listOfExpectedValues) {
                        Long l = Long.parseLong(string);
                        if (l.longValue() == prix.longValue()) {
                            return "";
                        }
                    }
                }
                if (result instanceof Long) {
                    return new Double(GestionDevise.currencyToString(prix.longValue(), false));
                } else {
                    return result;
                }
            } else if (typeComp.equalsIgnoreCase("globalAcompte")) {
                Long prix = (Long) result;
                int pourcent = this.row.getInt("POURCENT_ACOMPTE");
                long l = Math.round(prix.longValue() / (pourcent / 100.0));
                return new Double(GestionDevise.currencyToString(l, false));
            } else if (typeComp.equalsIgnoreCase("CumulPrec")) {

                final long cumulPrecedent = getCumulPrecedent(this.row);
                return new Double(GestionDevise.currencyToString(cumulPrecedent, false));
            } else if (typeComp.equalsIgnoreCase("DeviseLettre")) {
                // Devise exprimée en lettre
                Long prix = (Long) result;
                return getLettreFromDevise(prix.longValue(), Nombre.FR, Tuple2.create(" euros ", " cents"));
            } else if (typeComp.equalsIgnoreCase("DeviseLettreEng")) {
                // Devise exprimée en lettre
                Long prix = (Long) result;
                SQLRowAccessor tarif = this.row.getForeign("ID_TARIF");
                if (tarif.isUndefined()) {
                    return getLettreFromDevise(prix.longValue(), Nombre.EN, Tuple2.create(" euros ", " cents"));
                } else {
                    SQLRowAccessor rowDevise = tarif.getForeign("ID_DEVISE");
                    if (rowDevise.isUndefined()) {
                        return getLettreFromDevise(prix.longValue(), Nombre.EN, Tuple2.create(" euros ", " cents"));
                    } else {
                        return getLettreFromDevise(prix.longValue(), Nombre.EN, Tuple2.create(" " + rowDevise.getString("LIBELLE") + " ", " " + rowDevise.getString("LIBELLE_CENT") + " "));
                    }
                }
            } else if (typeComp.equalsIgnoreCase("VilleFull")) {
                return this.row.getString("CODE_POSTAL") + " " + this.row.getString("VILLE");
            } else if (typeComp.equalsIgnoreCase("Ville")) {
                stringValue = (result == null) ? "" : result.toString();
                return stringValue;
            } else if (typeComp.equalsIgnoreCase("Traduction")) {
                return getTraduction();
            } else if (typeComp.equalsIgnoreCase("VilleCP")) {
                // Code postal de la ville
                return this.row.getString("CODE_POSTAL");
            } else if (typeComp.equalsIgnoreCase("DateEcheance")) {
                // Retourne la date d'échéance
                int idModeReglement = this.row.getInt("ID_MODE_REGLEMENT");
                Date d = (Date) this.row.getObject("DATE");
                return getDateEcheance(idModeReglement, d, this.elt.getAttributeValue("datePattern"));
            } else if (typeComp.equalsIgnoreCase("Jour")) {
                int day = this.row.getInt(field);
                stringValue = "le " + String.valueOf(day);
                if (day == 31) {
                    return "fin de mois";
                } else if (day == 0) {
                    return "Date de facture";
                } else {
                    return stringValue;
                }
            } else if (typeComp.equalsIgnoreCase("Date")) {

                String datePattern = this.elt.getAttributeValue("datePattern");
                if (datePattern == null || datePattern.trim().length() == 0) {
                    datePattern = "dd/MM/yyyy";
                }
                SimpleDateFormat format = new SimpleDateFormat(datePattern);
                if (result != null) {
                    Date d = (Date) result;
                    return format.format(d);
                } else {
                    return "";
                }
            } else if (typeComp.equalsIgnoreCase("initiale")) {
                stringValue = (result == null) ? "" : result.toString();
                if (stringValue.trim().length() > 0) {
                    stringValue = String.valueOf(stringValue.charAt(0));
                }
                return stringValue;
            } else if (typeComp.equalsIgnoreCase("initiale2")) {
                stringValue = (result == null) ? "" : result.toString();
                if (stringValue.trim().length() > 0) {
                    stringValue = String.valueOf(stringValue.substring(0, 2));
                }
                return stringValue;
            }

        }

        return (result == null) ? "" : result;
    }

    private Object getTraduction() {
        if (this.rowLanguage == null || this.rowLanguage.isUndefined()) {
            return null;
        }
        int id = ReferenceArticleSQLElement.getIdForCNM(row.asRowValues(), false);
        SQLTable table = Configuration.getInstance().getBase().getTable("ARTICLE_DESIGNATION");
        SQLSelect sel = new SQLSelect(table.getBase());
        sel.addSelectStar(table);
        Where w = new Where(table.getField("ID_ARTICLE"), "=", id);
        w = w.and(new Where(table.getField("ID_LANGUE"), "=", this.rowLanguage.getID()));
        sel.setWhere(w);
        List<SQLRow> rows = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel));
        if (rows != null && rows.size() > 0) {
            return rows.get(0).getString(this.elt.getAttributeValue("name"));
        } else {
            return this.row.getObject(this.elt.getAttributeValue("name"));
        }
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

        SQLRowAccessor rowAff = rowFact.getForeign("ID_AFFAIRE");
        Calendar date = rowFact.getDate("DATE");
        if (rowAff != null && !rowAff.isUndefined()) {
            if (rowAff.getBoolean("CCI")) {

                List<SQLRow> rows = rowAff.asRow().getReferentRows(rowFact.getTable());
                for (SQLRow sqlRow : rows) {
                    if (sqlRow.getID() != rowFact.getID() && sqlRow.getDate("DATE").before(date)) {
                        cumul += sqlRow.getLong("T_HT");
                    }
                }
            }
        } else {

            // On recupere les missions associées
            SQLTable tableElt = Configuration.getInstance().getRoot().findTable("SAISIE_VENTE_FACTURE_ELEMENT");
            Collection<? extends SQLRowAccessor> factElts = rowFact.getReferentRows(tableElt);

            for (SQLRowAccessor row : factElts) {

                final SQLRowAccessor foreign = row.getForeign("ID_MISSION");
                if (foreign.getID() > 1) {
                    Collection<? extends SQLRowAccessor> rowsElt = foreign.getReferentRows(tableElt);
                    for (SQLRowAccessor row2 : rowsElt) {
                        SQLRowAccessor rowFacture = row2.getForeign("ID_SAISIE_VENTE_FACTURE");
                        if (rowFacture.getDate("DATE").before(date)) {
                            cumul += row2.getLong("T_PV_HT");
                        }
                    }
                }
            }
        }

        return cumul;
    }

    private static long getMontantGlobal(SQLRowAccessor rowFact) {

        long cumul = 0;
        final BigDecimal cent = new BigDecimal(100);

        // On recupere les missions associées
        SQLTable tableElt = Configuration.getInstance().getRoot().findTable("SAISIE_VENTE_FACTURE_ELEMENT");
        Collection<? extends SQLRowAccessor> factElts = rowFact.getReferentRows(tableElt);

        for (SQLRowAccessor row : factElts) {
            BigDecimal p0 = row.getBigDecimal("MONTANT_INITIAL");
            Long l0 = (Long) row.getObject("INDICE_0");
            Long lN = (Long) row.getObject("INDICE_N");
            final BigDecimal o = row.getBigDecimal("POURCENT_ACOMPTE");
            final BigDecimal o2 = row.getBigDecimal("POURCENT_REMISE");
            double lA = (o == null) ? 0 : ((BigDecimal) o).doubleValue();
            BigDecimal lremise = (o2 == null) ? BigDecimal.ZERO : o2;
            BigDecimal p;
            if (l0 != 0) {
                BigDecimal d;
                double coeff = ((double) lN) / ((double) l0);

                d = new BigDecimal(0.15).add(new BigDecimal(0.85).multiply(new BigDecimal(coeff), MathContext.DECIMAL128));
                p = d.multiply(p0, MathContext.DECIMAL128);
            } else {
                p = p0;
            }
            // if (lA >= 0 && lA != 100) {
            // p = Math.round(p * (lA / 100.0));
            // }
            if (lremise.signum() != 0 && lremise.compareTo(BigDecimal.ZERO) > 0 && lremise.compareTo(cent) < 100) {
                p = p.multiply(cent.subtract(lremise).movePointLeft(2), MathContext.DECIMAL128);
            }
            cumul += p.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValue();
        }

        // Echantillons
        SQLTable tableEchElt = Configuration.getInstance().getRoot().findTable("ECHANTILLON_ELEMENT");
        Collection<? extends SQLRowAccessor> echElts = rowFact.getReferentRows(tableEchElt);
        for (SQLRowAccessor sqlRowAccessor : echElts) {
            cumul += sqlRowAccessor.getBigDecimal("T_PV_HT").setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValue();
        }
        return cumul;
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
     * transforme une devise exprimée en chiffres en lettres
     * 
     * @param value
     * @return la devise exprimée en lettres
     */
    private static String getLettreFromDevise(long value, int langue, Tuple2<String, String> deviseName) {

        StringBuffer result = new StringBuffer();

        Long decimal = Long.valueOf(value % 100);
        Long entier = Long.valueOf(value / 100);

        Nombre n1 = new Nombre(entier.intValue(), langue);
        Nombre n2 = new Nombre(decimal.intValue(), langue);

        // result.append(n1.getText() + " euros");
        result.append(n1.getText() + " " + deviseName.get0().trim());

        if (decimal.intValue() > 0) {
            // result.append(" et " + n2.getText() + " cents");
            result.append((langue == Nombre.FR ? " et " : " and ") + n2.getText() + deviseName.get1());
        }
        if (result != null && result.length() > 0) {
            return result.toString().replaceFirst(String.valueOf(result.charAt(0)), String.valueOf(result.charAt(0)).toUpperCase());
        } else {
            return result.toString();
        }
    }

    private static BigDecimal calcul(Object o1, Object o2, String op) {

        BigDecimal d1;
        if (o1 != null && o1 instanceof BigDecimal) {
            d1 = (BigDecimal) o1;
        } else {
            d1 = (o1 == null) ? BigDecimal.ZERO : new BigDecimal(o1.toString());
        }

        BigDecimal d2;
        if (o2 != null && o2 instanceof BigDecimal) {
            d2 = (BigDecimal) o2;
        } else {
            d2 = (o2 == null) ? BigDecimal.ZERO : new BigDecimal(o2.toString());
        }

        if (op.equalsIgnoreCase("+")) {
            return d1.add(d2);
        } else {
            if (op.equalsIgnoreCase("-")) {
                return d1.subtract(d2);
            } else {
                if (op.equalsIgnoreCase("*")) {
                    return d1.multiply(d2);
                } else {
                    if (op.equalsIgnoreCase("/") && d2.compareTo(BigDecimal.ZERO) != 0) {
                        return d1.divide(d2);
                    }
                }
            }
        }
        return BigDecimal.ZERO;
    }

}
