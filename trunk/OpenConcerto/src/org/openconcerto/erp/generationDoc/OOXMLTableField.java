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
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jdom.Element;

public class OOXMLTableField extends OOXMLField {

    private String type;
    private int filterId, line;

    public OOXMLTableField(Element eltField, SQLRowAccessor row, SQLElement sqlElt, int id, int filterId) {
        super(eltField, row, sqlElt, id);
        this.type = eltField.getAttributeValue("type");
        this.filterId = filterId;
        String s = eltField.getAttributeValue("line");
        this.line = 1;
        if (s != null && s.trim().length() > 0) {
            this.line = Integer.valueOf(s);
        }
    }

    @Override
    public Object getValue() {
        Object value = null;

        if (this.type.equalsIgnoreCase("DescriptifArticle")) {
            value = getDescriptifArticle(this.row);
        } else {
            if (this.type.equalsIgnoreCase("TotalFacture")) {
                value = getDejaFacture(this.row);
            } else {
                if (this.type.equalsIgnoreCase("propositionFacture")) {
                    value = getProposition(this.row);
                } else {
                    if (this.type.equalsIgnoreCase("pourcentRealise")) {
                        value = getPourcentRealise(this.row);
                    } else {
                        if (this.type.equalsIgnoreCase("DateEcheance")) {
                            value = getDateEcheance(this.row.getInt("ID_MODE_REGLEMENT"), (Date) this.row.getObject("DATE"));
                        } else {
                            if (this.type.equalsIgnoreCase("MontantRevise")) {
                                value = getMontantRevise(this.row);
                            } else {
                                if (this.type.equalsIgnoreCase("Localisation")) {
                                    value = getLocalisation(this.row);
                                } else {
                                    if (this.type.equalsIgnoreCase("Verificateurs")) {
                                        value = getVerificateur(this.row, this.filterId);
                                    } else {
                                        if (this.type.equalsIgnoreCase("CCIP")) {
                                            value = getCCIP(this.row);
                                        } else {
                                            if (this.type.equalsIgnoreCase("Echantillon")) {
                                                value = getStringEchantillon(this.row);
                                            } else {
                                                OOXMLElement eltXml = new OOXMLElement(this.elt, this.sqlElt, this.id, this.row);
                                                value = eltXml.getValue();
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

        return value;
    }

    private static Double getDejaFacture(SQLRowAccessor rowElt) {
        SQLRowAccessor rowAffElt = OOXMLCache.getForeignRow(rowElt, rowElt.getTable().getField("ID_AFFAIRE_ELEMENT"));
        List<? extends SQLRowAccessor> list = OOXMLCache.getReferentRows(rowAffElt, rowElt.getTable());
        SQLRowAccessor rowFact = OOXMLCache.getForeignRow(rowElt, rowElt.getTable().getField("ID_SAISIE_VENTE_FACTURE"));

        double total = rowAffElt.getFloat("TOTAL_HT_REALISE");
        for (SQLRowAccessor sqlRow : list) {
            SQLRowAccessor rowFact2 = OOXMLCache.getForeignRow(sqlRow, sqlRow.getTable().getField("ID_SAISIE_VENTE_FACTURE"));
            Calendar date = rowFact.getDate("DATE");
            Calendar date2 = rowFact2.getDate("DATE");
            if (date2.before(date)) {
                total += sqlRow.getFloat("T_PV_HT");
            }
        }
        return total / 100.0;
    }

    private static Double getPourcentRealise(SQLRowAccessor rowElt) {

        SQLRowAccessor rowAffElt = OOXMLCache.getForeignRow(rowElt, rowElt.getTable().getField("ID_AFFAIRE_ELEMENT"));
        if (rowAffElt.getID() <= 1) {
            return 100.0;
        }
        SQLRowAccessor rowFact = OOXMLCache.getForeignRow(rowElt, rowElt.getTable().getField("ID_SAISIE_VENTE_FACTURE"));
        List<? extends SQLRowAccessor> list = OOXMLCache.getReferentRows(rowAffElt, rowElt.getTable());

        double percent = rowAffElt.getFloat("POURCENT_REALISE");
        for (SQLRowAccessor sqlRow : list) {
            SQLRowAccessor rowFact2 = OOXMLCache.getForeignRow(sqlRow, sqlRow.getTable().getField("ID_SAISIE_VENTE_FACTURE"));
            Calendar date = rowFact.getDate("DATE");
            Calendar date2 = rowFact2.getDate("DATE");
            if (date2.before(date) || date2.equals(date)) {
                if (rowAffElt.getInt("NOMBRE") != 0) {
                    percent += sqlRow.getInt("QTE") * sqlRow.getFloat("POURCENT_ACOMPTE") / rowAffElt.getInt("NOMBRE");
                } else {
                    percent += sqlRow.getFloat("POURCENT_ACOMPTE");
                }
            }
        }
        return percent;
    }

    protected static String getStringEchantillon(SQLRowAccessor rowEch) {

        final int nbEch = rowEch.getInt("QTE");
        Nombre n = new Nombre(nbEch);
        Long ht = rowEch.getLong("PV_HT");

        return "Par " + n.getText() + " échantillon" + ((nbEch > 1) ? "s " : " ") + "au " + rowEch.getString("NOM") + ", soit " + nbEch + " x " + GestionDevise.currencyToString(ht) + " € HT";
    }

    public boolean isNeeding2Lines() {
        return (this.type.equalsIgnoreCase("DescriptifArticle") || this.type.equalsIgnoreCase("propositionFacture") || this.type.equalsIgnoreCase("DateEcheance") || this.type
                .equalsIgnoreCase("MontantRevise"));
    }

    public int getLine() {
        return this.line;
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
    private static String getMontantRevise(SQLRowAccessor row) {
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
                rowFact = OOXMLCache.getForeignRow(row, row.getTable().getField("ID_SAISIE_VENTE_FACTURE"));
            } else {
                rowFact = OOXMLCache.getForeignRow(row, row.getTable().getField("ID_AVOIR_CLIENT"));
            }
            if (rowFact != null) {
                SQLRowAccessor rowClient = OOXMLCache.getForeignRow(rowFact, rowFact.getTable().getField("ID_CLIENT"));
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

    /**
     * @param row
     * @return La liste des vérificateurs prévus pour la mission
     */
    public static String getVerificateur(SQLRowAccessor row, int filterID) {
        StringBuffer result = new StringBuffer();
        SQLTable tableElt = Configuration.getInstance().getRoot().findTable("POURCENT_SERVICE");
        Collection<? extends SQLRowAccessor> s = row.getReferentRows(tableElt);
        for (SQLRowAccessor row2 : s) {
            SQLRowAccessor rowVerif = row2.getForeign("ID_VERIFICATEUR");

            if (rowVerif != null && (filterID <= 1 || filterID == rowVerif.getID())) {
                String prenom = rowVerif.getString("PRENOM");
                if (prenom.length() > 1) {
                    prenom = String.valueOf(prenom.charAt(0));
                }
                result.append(prenom + "." + rowVerif.getString("NOM"));
                result.append(", ");
            }
        }

        if (result.length() > 0) {
            result.deleteCharAt(result.length() - 1);
            result.deleteCharAt(result.length() - 1);
        }
        return result.toString();
    }

    /**
     * @param row
     * @return La liste des mois CCIP
     */
    public static String getCCIP(SQLRowAccessor row) {
        StringBuffer result = new StringBuffer();
        SQLTable tableElt = Configuration.getInstance().getRoot().findTable("POURCENT_CCIP");
        Collection<? extends SQLRowAccessor> s = row.getReferentRows(tableElt);
        for (SQLRowAccessor row2 : s) {
            SQLRowAccessor rowMois = row2.getForeign("ID_MOIS");

            if (rowMois != null) {
                String nom = rowMois.getString("NOM");
                result.append(nom);
                result.append(",\n");
            }
        }

        if (result.length() > 0) {
            result.deleteCharAt(result.length() - 1);
            result.deleteCharAt(result.length() - 1);
        }
        return result.toString();
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
}
