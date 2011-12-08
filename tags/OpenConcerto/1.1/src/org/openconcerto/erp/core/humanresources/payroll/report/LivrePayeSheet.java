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
 
 package org.openconcerto.erp.core.humanresources.payroll.report;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.generationDoc.SheetInterface;
import org.openconcerto.erp.generationDoc.SheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.Tuple2;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class LivrePayeSheet extends SheetInterface {

    // TODO Incorrect si aucune fiche valider
    private static int debutFill, endFill;
    private static int nbCol;
    private final static SQLTable tableSalarie = base.getTable("SALARIE");
    private final static SQLTable tableFichePaye = base.getTable("FICHE_PAYE");
    private final static SQLTable tableFichePayeElement = base.getTable("FICHE_PAYE_ELEMENT");
    private final static SQLTable tableMois = base.getTable("MOIS");
    private final static SQLTable tableRubCot = Configuration.getInstance().getBase().getTable("RUBRIQUE_COTISATION");
    private final static SQLTable tableRubNet = Configuration.getInstance().getBase().getTable("RUBRIQUE_NET");
    private final static SQLTable tableRubBrut = Configuration.getInstance().getBase().getTable("RUBRIQUE_BRUT");

    private final static DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
    private int moisDu, moisAu;
    private String annee;

    public static void setSize(int debut, int fin, int col) {
        debutFill = debut;
        endFill = fin;
        nbCol = col;
    }

    static {
        setSize(8, 65, 3);
    }

    private static final Tuple2<String, String> tuple = Tuple2.create("LocationLivrePaye", "Livre de paye");

    public static Tuple2<String, String> getTuple2Location() {
        return tuple;
    }

    public LivrePayeSheet(int moisDu, int moisAu, String annee) {
        super();
        this.printer = PrinterNXProps.getInstance().getStringProperty("LivrePayePrinter");
        this.modele = "LivrePaye.ods";
        this.locationOO = SheetXml.getLocationForTuple(tuple, false) + File.separator + annee;
        this.locationPDF = SheetXml.getLocationForTuple(tuple, true) + File.separator + annee;
        this.moisAu = moisAu;
        this.moisDu = moisDu;
        this.annee = annee;

        this.nbRowsPerPage = 67;

        createMap();
    }

    private void makeEntete(int row) {
        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        this.mCell.put("A" + row, rowSociete.getObject("NOM"));
        this.mCell.put("D" + row, "Edition du " + dateFormat.format(new Date()));
        // this.mCell.put("D" + (row + 2), "Impression Journaux");
        System.err.println("MAKE ENTETE");
    }

    private void makeBasPage(int row) {
        SQLRow rowMoisDu = tableMois.getRow(this.moisDu);
        SQLRow rowMoisAu = tableMois.getRow(this.moisAu);

        this.mCell.put("A" + row, "Période de " + rowMoisDu.getString("NOM") + " à " + rowMoisAu.getString("NOM") + " " + this.annee);
    }

    protected void createMap() {

        this.mapReplace = new HashMap();
        this.mCell = new HashMap();
        this.mapStyleRow = new HashMap();

        SQLSelect sel = new SQLSelect(base);
        sel.addSelect(tableFichePaye.getField("ID"));
        sel.addSelect(tableFichePayeElement.getField("ID"));
        sel.addSelect(tableSalarie.getField("ID"));

        Where w = (new Where(tableFichePayeElement.getField("ID_FICHE_PAYE"), "=", tableFichePaye.getField("ID")));
        Where w2 = (new Where(tableFichePaye.getField("ID_SALARIE"), "=", tableSalarie.getField("ID")));
        Where w3 = (new Where(tableFichePaye.getField("ID_MOIS"), new Integer(this.moisDu), new Integer(this.moisAu)));
        Where w4 = (new Where(tableFichePaye.getField("ANNEE"), "=", new Integer(this.annee)));
        Where w5 = (new Where(tableFichePaye.getField("VALIDE"), "=", Boolean.TRUE));

        sel.setWhere(w);
        sel.andWhere(w2);
        sel.andWhere(w3);
        sel.andWhere(w4);
        sel.andWhere(w5);
        String req = sel.asString();

        System.err.println(req);

        // Liste des rubriques de chaque salaries
        List l = (List) base.getDataSource().execute(req, new ArrayListHandler());

        // Association idSal, map Value(idRub, val)
        Map mapSalarieBrut = new HashMap();
        Map mapSalarieNet = new HashMap();
        Map mapSalarieCot = new HashMap();

        Map mapTotalCot = new HashMap();
        Map mapTotalNet = new HashMap();
        Map mapTotalbrut = new HashMap();

        Map mapRubriqueBrut = new HashMap();
        Map mapRubriqueNet = new HashMap();
        Map mapRubriqueCot = new HashMap();
        Map mapSal = new HashMap();

        // Cumuls des rubriques par salaries
        for (int i = 0; i < l.size(); i++) {
            Object[] tmp = (Object[]) l.get(i);
            // int idFiche = new Integer(tmp[0].toString()).intValue();
            int idFicheElt = Integer.parseInt(tmp[1].toString());
            int idSal = Integer.parseInt(tmp[2].toString());

            Map mapValue = new HashMap();
            Map mapTotal = new HashMap();

            // SQLRow rowFiche = tableFichePaye.getRow(idFiche);
            SQLRow rowFicheElt = tableFichePayeElement.getRow(idFicheElt);

            mapSal.put(new Integer(idSal), "");

            if (rowFicheElt.getString("SOURCE").equalsIgnoreCase("RUBRIQUE_BRUT")) {

                mapRubriqueBrut.put(new Integer(rowFicheElt.getInt("IDSOURCE")), "");
                mapTotal = mapTotalbrut;
                if (mapSalarieBrut.get(new Integer(idSal)) == null) {
                    mapSalarieBrut.put(new Integer(idSal), mapValue);
                } else {
                    mapValue = (Map) mapSalarieBrut.get(new Integer(idSal));
                }
            } else {
                if (rowFicheElt.getString("SOURCE").equalsIgnoreCase("RUBRIQUE_COTISATION")) {
                    mapRubriqueCot.put(new Integer(rowFicheElt.getInt("IDSOURCE")), "");
                    mapTotal = mapTotalCot;
                    if (mapSalarieCot.get(new Integer(idSal)) == null) {
                        mapSalarieCot.put(new Integer(idSal), mapValue);
                    } else {
                        mapValue = (Map) mapSalarieCot.get(new Integer(idSal));
                    }
                } else {
                    if (rowFicheElt.getString("SOURCE").equalsIgnoreCase("RUBRIQUE_NET")) {
                        mapRubriqueNet.put(new Integer(rowFicheElt.getInt("IDSOURCE")), "");
                        mapTotal = mapTotalNet;
                        if (mapSalarieNet.get(new Integer(idSal)) == null) {
                            mapSalarieNet.put(new Integer(idSal), mapValue);
                        } else {
                            mapValue = (Map) mapSalarieNet.get(new Integer(idSal));
                        }
                    }
                }
            }

            if (rowFicheElt.getObject("MONTANT_SAL_AJ") != null) {
                Object o = mapValue.get(new Integer(rowFicheElt.getInt("IDSOURCE")));
                Object oTot = mapTotal.get(new Integer(rowFicheElt.getInt("IDSOURCE")));

                float montant = (o == null) ? 0.0F : ((Float) o).floatValue();
                float montantTotal = (oTot == null) ? 0.0F : ((Float) oTot).floatValue();
                montant += rowFicheElt.getFloat("MONTANT_SAL_AJ");
                montantTotal += rowFicheElt.getFloat("MONTANT_SAL_AJ");

                mapValue.put(new Integer(rowFicheElt.getInt("IDSOURCE")), new Float(montant));
                mapTotal.put(new Integer(rowFicheElt.getInt("IDSOURCE")), new Float(montantTotal));
            }
            if (rowFicheElt.getObject("MONTANT_SAL_DED") != null) {
                Object o = mapValue.get(new Integer(rowFicheElt.getInt("IDSOURCE")));
                Object oTot = mapTotal.get(new Integer(rowFicheElt.getInt("IDSOURCE")));

                float montant = (o == null) ? 0.0F : ((Float) o).floatValue();
                float montantTot = (oTot == null) ? 0.0F : ((Float) oTot).floatValue();
                montant -= rowFicheElt.getFloat("MONTANT_SAL_DED");
                montantTot -= rowFicheElt.getFloat("MONTANT_SAL_DED");

                mapValue.put(new Integer(rowFicheElt.getInt("IDSOURCE")), new Float(montant));
                mapTotal.put(new Integer(rowFicheElt.getInt("IDSOURCE")), new Float(montantTot));
            }
        }

        // Dump
        /*
         * for (int j = 0; j < mapSalarieBrut.keySet().size(); j++) {
         * System.err.println(mapSalarieBrut.get(mapSalarieBrut.keySet().toArray()[j])); }
         */

        // Fill
        int posLine = 1;
        int firstLine = 1;

        System.err.println("NB Sal = " + mapSal.keySet().size());
        System.err.println("NB Pages = " + Math.ceil((double) (mapSal.keySet().size() + 1) / nbCol));
        for (int n = 0; n < Math.ceil((double) (mapSal.keySet().size() + 1) / nbCol); n++) {

            // entete
            makeEntete(posLine);
            posLine += (debutFill - 1);

            int numFirstSal = (n * nbCol);

            if (numFirstSal < mapSal.keySet().size()) {
                SQLRow rowSal = tableSalarie.getRow(((Integer) mapSal.keySet().toArray()[numFirstSal]).intValue());
                this.mCell.put("B" + (posLine - 2), rowSal.getObject("NOM"));
                this.mCell.put("B" + (posLine - 1), rowSal.getObject("PRENOM"));
            } else {
                if (numFirstSal == mapSal.keySet().size()) {
                    System.err.println("Cumuls B");
                    this.mCell.put("B" + (posLine - 2), "Cumuls");
                    this.mCell.put("B" + (posLine - 1), "");
                }
            }
            if (numFirstSal + 1 < mapSal.keySet().size()) {
                SQLRow rowSal = tableSalarie.getRow(((Integer) mapSal.keySet().toArray()[numFirstSal + 1]).intValue());
                this.mCell.put("C" + (posLine - 2), rowSal.getObject("NOM"));
                this.mCell.put("C" + (posLine - 1), rowSal.getObject("PRENOM"));
            } else {
                if (numFirstSal + 1 == mapSal.keySet().size()) {
                    System.err.println("Cumuls C");
                    this.mCell.put("C" + (posLine - 2), "Cumuls");
                    this.mCell.put("C" + (posLine - 1), "");
                }
            }
            if (numFirstSal + 2 < mapSal.keySet().size()) {
                SQLRow rowSal = tableSalarie.getRow(((Integer) mapSal.keySet().toArray()[numFirstSal + 2]).intValue());
                this.mCell.put("D" + (posLine - 2), rowSal.getObject("NOM"));
                this.mCell.put("D" + (posLine - 1), rowSal.getObject("PRENOM"));
            } else {
                if (numFirstSal + 2 == mapSal.keySet().size()) {
                    System.err.println("Cumuls D");
                    this.mCell.put("D" + (posLine - 2), "Cumuls");
                    this.mCell.put("D" + (posLine - 1), "");
                }
            }
            for (int i = 0; i < mapRubriqueBrut.keySet().size(); i++) {

                int idRub = ((Number) mapRubriqueBrut.keySet().toArray()[i]).intValue();
                SQLRow rowRub = tableRubBrut.getRow(idRub);

                this.mCell.put("A" + posLine, rowRub.getObject("NOM"));

                this.mCell.put("B" + posLine, fillLine(mapSalarieBrut, idRub, mapSal, numFirstSal, mapTotalbrut));
                this.mCell.put("C" + posLine, fillLine(mapSalarieBrut, idRub, mapSal, numFirstSal + 1, mapTotalbrut));
                this.mCell.put("D" + posLine, fillLine(mapSalarieBrut, idRub, mapSal, numFirstSal + 2, mapTotalbrut));

                posLine++;
            }

            for (int i = 0; i < mapRubriqueCot.keySet().size(); i++) {

                int idRub = ((Number) mapRubriqueCot.keySet().toArray()[i]).intValue();
                SQLRow rowRub = tableRubCot.getRow(idRub);

                this.mCell.put("A" + posLine, rowRub.getObject("NOM"));

                this.mCell.put("B" + posLine, fillLine(mapSalarieCot, idRub, mapSal, numFirstSal, mapTotalCot));
                this.mCell.put("C" + posLine, fillLine(mapSalarieCot, idRub, mapSal, numFirstSal + 1, mapTotalCot));
                this.mCell.put("D" + posLine, fillLine(mapSalarieCot, idRub, mapSal, numFirstSal + 2, mapTotalCot));

                posLine++;
            }

            for (int i = 0; i < mapRubriqueNet.keySet().size(); i++) {

                int idRub = ((Number) mapRubriqueNet.keySet().toArray()[i]).intValue();
                SQLRow rowRub = tableRubNet.getRow(idRub);

                this.mCell.put("A" + posLine, rowRub.getObject("NOM"));

                this.mCell.put("B" + posLine, fillLine(mapSalarieNet, idRub, mapSal, numFirstSal, mapTotalNet));
                this.mCell.put("C" + posLine, fillLine(mapSalarieNet, idRub, mapSal, numFirstSal + 1, mapTotalNet));
                this.mCell.put("D" + posLine, fillLine(mapSalarieNet, idRub, mapSal, numFirstSal + 2, mapTotalNet));

                posLine++;
            }

            // pied de page
            posLine = firstLine + endFill - 1;
            posLine += 2;
            makeBasPage(posLine);

            posLine++;
            firstLine = posLine;
        }

        this.nbPage = new Double(Math.ceil((double) (mapSal.keySet().size() + 1) / (nbCol))).intValue();

        System.err.println("Nombre de page " + this.nbPage);

        // on conserve la page d'origine du model

        if (this.nbPage > 0) {
            this.nbPage--;
        }
    }

    private Object fillLine(Map mapSalRub, int idRub, Map mapSal, int numSal, Map mapTotal) {

        Object value = null;
        if (numSal < mapSal.keySet().size()) {
            Map m = (Map) mapSalRub.get(mapSal.keySet().toArray()[numSal]);

            value = new Float(0);
            if (m != null && m.get(new Integer(idRub)) != null) {
                value = m.get(new Integer(idRub));
            }
        } else {

            if (numSal == mapSal.keySet().size()) {
                value = mapTotal.get(new Integer(idRub));
            }
        }
        return value;
    }
}
