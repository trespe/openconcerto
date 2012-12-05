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
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.Nombre;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Element;

public class OOXMLTableField extends OOXMLField {

    private String type;
    private int filterId, line;
    private String lineOption;
    private int idRef;
    private String style = "";

    public OOXMLTableField(Element eltField, SQLRowAccessor row, SQLElement sqlElt, int id, int filterId, SQLRow rowLanguage, int idRef, OOXMLCache cache) {
        super(eltField, row, sqlElt, id, rowLanguage, cache);
        this.type = eltField.getAttributeValue("type");
        this.lineOption = eltField.getAttributeValue("lineOption");
        this.filterId = filterId;
        String s = eltField.getAttributeValue("line");
        this.line = 1;
        this.idRef = idRef;

        if (s != null && s.trim().length() > 0) {
            this.line = Integer.valueOf(s);
        }

        String style = eltField.getAttributeValue("style");
        if (style != null && style.trim().length() > 0) {
            this.style = style;
        }
    }

    @Override
    public Object getValue() {
        Object value = null;

        SpreadSheetCellValueProvider provider = SpreadSheetCellValueProviderManager.get(type);
        if (provider != null) {
            final SpreadSheetCellValueContext context = new SpreadSheetCellValueContext(this.row);
            List<Attribute> attrs = this.elt.getAttributes();
            for (Attribute attr : attrs) {
                context.put(attr.getName(), attr.getValue());
            }
            value = provider.getValue(context);
        } else if (this.type.equalsIgnoreCase("LineReference")) {
            return idRef;
        } else if (this.type.equalsIgnoreCase("DescriptifArticle")) {
            value = getDescriptifArticle(this.row);
        } else if (this.type.equalsIgnoreCase("DateEcheance")) {
            value = getDateEcheance(this.row.getInt("ID_MODE_REGLEMENT"), (Date) this.row.getObject("DATE"), this.elt.getAttributeValue("datePattern"));
        } else if (this.type.equalsIgnoreCase("MontantRevise")) {
            value = getMontantRevise(this.row);
        } else if (this.type.equalsIgnoreCase("Localisation")) {
            value = getLocalisation(this.row);
        } else {
            OOXMLElement eltXml = new OOXMLElement(this.elt, this.sqlElt, this.id, this.row, this.rowLanguage, cache);
            value = eltXml.getValue();
            String cellSize = this.elt.getAttributeValue("cellSize");
            if (cellSize != null && cellSize.trim().length() != 0 && value != null) {
                value = splitStringCell(cellSize, value.toString());
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

        if (value != null && listOfExcludedValues != null && listOfExcludedValues.contains(value.toString())) {
            value = null;
        }

        return value;
    }

    public boolean isNeeding2Lines() {
        return (this.type.equalsIgnoreCase("DescriptifArticle") || this.type.equalsIgnoreCase("propositionFacture") || this.type.equalsIgnoreCase("DateEcheance") || this.type
                .equalsIgnoreCase("MontantRevise"));
    }

    public int getLine() {
        return this.line;
    }

    public boolean isLineOption() {
        return Boolean.valueOf(this.lineOption);
    }

    /**
     * @param row
     * @return Descriptif de l'article (Longueur, largeur, poids, surface)
     */
    private static String getDescriptifArticle(SQLRowAccessor row) {
        float longueur = row.getFloat("VALEUR_METRIQUE_1");
        float largeur = row.getFloat("VALEUR_METRIQUE_2");
        // float f = row.getFloat("POIDS");

        StringBuffer result = new StringBuffer();
        if (longueur != 0) {
            result.append(" Longueur: " + longueur + " m");
        }
        if (largeur != 0) {
            result.append(" Largeur: " + largeur + " m");
        }
        if (largeur != 0 && longueur != 0) {
            result.append(" Surface : " + Math.round(largeur * longueur * 1000) / 1000.0 + " m²");
        }
        // if (f != 0) {
        // result.append(" Poids : " + f + " kg");
        // }
        return result.toString();
    }

    /**
     * @param row
     * @return la date + la localisation
     */
    private static String getLocalisation(SQLRowAccessor row) {
        StringBuffer string = new StringBuffer();
        String site = row.getString("LOCAL_OBJET_INSPECTE");
        if (site != null) {
            string.append(site);
        }
        Object date = row.getObject("DATE");
        Object dateFin = row.getObject("DATE_FIN");
        if (date != null) {
            String stringDate = format.format((Date) date);
            if (dateFin != null) {
                String stringDateFin = format.format((Date) dateFin);
                string.append(" du " + stringDate + " au " + stringDateFin);
            } else {
                string.append(" le " + stringDate);
            }
        }

        return string.toString();
    }

    /**
     * @param row
     * @return la formule de calcul du montant revise
     */
    private String getMontantRevise(SQLRowAccessor row) {
        long indice0 = (Long) row.getObject("INDICE_0");
        long indiceN = (Long) row.getObject("INDICE_N");
        long montantInit = (Long) row.getObject("MONTANT_INITIAL");

        if (indice0 == indiceN || indice0 == 0) {
            return null;
        }

        Boolean clientPrive = false;
        try {
            SQLRowAccessor rowFact;
            if (row.getTable().getName().startsWith("SAISIE_VENTE_FACTURE")) {
                rowFact = cache.getForeignRow(row, row.getTable().getField("ID_SAISIE_VENTE_FACTURE"));
            } else {
                rowFact = cache.getForeignRow(row, row.getTable().getField("ID_AVOIR_CLIENT"));
            }
            if (rowFact != null) {
                SQLRowAccessor rowClient = cache.getForeignRow(rowFact, rowFact.getTable().getField("ID_CLIENT"));
                if (rowClient != null) {
                    clientPrive = rowClient.getBoolean("MARCHE_PRIVE");
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        StringBuffer result = new StringBuffer();
        result.append("Détail : ");

        if (clientPrive) {
            result.append(GestionDevise.currencyToString(indiceN));
            result.append(" / ");
            result.append(GestionDevise.currencyToString(indice0));
            result.append(" x ");
            result.append(GestionDevise.currencyToString(montantInit));
            result.append(" € HT");
            return result.toString();
        } else {
            result.append("(0,15 + 0,85 x (");
            result.append(GestionDevise.currencyToString(indiceN));
            result.append(" / ");
            result.append(GestionDevise.currencyToString(indice0));
            result.append(")) x ");
            result.append(GestionDevise.currencyToString(montantInit));
            result.append(" € HT");
            return result.toString();
        }
    }

    public List<String> getBlankStyle() {
        // Cellule pour un style défini
        String blankOnStyle = this.elt.getAttributeValue("blankOnStyle");
        List<String> listBlankStyle = new ArrayList<String>();
        if (blankOnStyle != null) {
            listBlankStyle = SQLRow.toList(blankOnStyle.trim());
        }
        return listBlankStyle;
    }

    public String getStyle() {

        return this.style;
    }
}
