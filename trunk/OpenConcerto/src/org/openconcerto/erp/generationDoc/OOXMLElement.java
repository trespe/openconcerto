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
import org.openconcerto.utils.GestionDevise;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Element;

public class OOXMLElement {
    protected Element elt;
    protected SQLElement sqlElt;
    protected int id;
    protected SQLRowAccessor row;
    protected SQLRow rowLanguage;
    protected OOXMLCache cache;

    public OOXMLElement(Element elt, SQLElement sqlElt, int id, SQLRow rowLanguage, OOXMLCache cache) {
        this(elt, sqlElt, id, null, rowLanguage, cache);

    }

    public OOXMLElement(Element elt, SQLElement sqlElt, int id, SQLRowAccessor row, SQLRow rowLanguage, OOXMLCache cache) {
        this.elt = elt;
        this.sqlElt = sqlElt;
        this.id = id;
        this.row = row;
        this.rowLanguage = rowLanguage;
        this.cache = cache;
    }

    public Object getValue() {
        Object res = "";

        final String type = this.elt.getAttributeValue("type");
        SpreadSheetCellValueProvider provider = SpreadSheetCellValueProviderManager.get(type);
        if (provider != null) {
            final SpreadSheetCellValueContext context = new SpreadSheetCellValueContext(this.row);
            List<Attribute> attrs = this.elt.getAttributes();
            for (Attribute attr : attrs) {
                context.put(attr.getName(), attr.getValue());
            }
            res = provider.getValue(context);
        } else

        if (type.equalsIgnoreCase("TotalHTTable")) {
            res = getTotalHTTable(row);
        } else if (type.equalsIgnoreCase("DateEcheance")) {
            int idModeReglement = row.getInt("ID_MODE_REGLEMENT");
            Date d = (Date) row.getObject("DATE");
            res = getDateEcheance(idModeReglement, d, this.elt.getAttributeValue("datePattern"));
        } else {

            final List<Element> eltFields = this.elt.getChildren("field");

            if (eltFields != null) {
                if (eltFields.size() > 1) {
                    String result = "";
                    for (Element eltField : eltFields) {

                        OOXMLField field = new OOXMLField(eltField, this.row, this.sqlElt, this.id, this.rowLanguage, cache);

                        Object value = field.getValue();
                        if (value != null) {
                            result += value.toString() + " ";
                        }
                    }
                    res = result;
                } else {
                    OOXMLField field = new OOXMLField(eltFields.get(0), this.row, this.sqlElt, this.id, this.rowLanguage, cache);
                    res = field.getValue();
                }
            }
        }

        // Liste des valeurs à ne pas afficher
        List<String> listOfExcludedValues = null;

        List<Element> excludeValue = this.elt.getChildren("exclude");
        if (excludeValue != null && excludeValue.size() > 0) {

            listOfExcludedValues = new ArrayList<String>();

            for (Element element : excludeValue) {
                String attributeValue = element.getAttributeValue("value");
                listOfExcludedValues.add(attributeValue);
            }
        }

        if (res != null && listOfExcludedValues != null && listOfExcludedValues.contains(res.toString())) {
            res = null;
        }

        return res;
    }


    public static DateFormat format = new SimpleDateFormat("dd/MM/yyyy");

    protected String getStringProposition(SQLRowAccessor rowProp) {

        return "Notre proposition " + rowProp.getString("NUMERO") + " du " + format.format(rowProp.getObject("DATE"));
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
     * Calcul la date d'échéance d'un élément par rapport au mode de reglement et à la date
     * d'émission
     * 
     * @param idModeRegl
     * @param currentDate
     * @return la date d'échéance au format dd/MM/yy si datePattern !=null sinon une Date
     */
    protected Object getDateEcheance(int idModeRegl, Date currentDate, String datePattern) {
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
        Date calculDate = ModeDeReglementSQLElement.calculDate(aJ, nJ, currentDate);
        if (datePattern != null && datePattern.trim().length() > 0) {
            final DateFormat format2 = new SimpleDateFormat(datePattern);
            return format2.format(calculDate);
        } else {
            return calculDate;
        }
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
