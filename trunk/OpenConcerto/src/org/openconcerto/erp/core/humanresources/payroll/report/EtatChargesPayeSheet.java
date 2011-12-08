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
import org.openconcerto.sql.model.SQLRowValues;
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

public class EtatChargesPayeSheet extends SheetInterface {

    private static int debutFill, endFill;
    private final static SQLTable tableFichePaye = base.getTable("FICHE_PAYE");
    private final static SQLTable tableFichePayeElement = base.getTable("FICHE_PAYE_ELEMENT");
    private final static SQLTable tableMois = base.getTable("MOIS");
    private final static SQLTable tableCaisse = Configuration.getInstance().getBase().getTable("CAISSE_COTISATION");
    private final static SQLTable tableRubCot = Configuration.getInstance().getBase().getTable("RUBRIQUE_COTISATION");

    private final static DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
    private int moisDu, moisAu;
    private String annee;

    public static void setSize(int debut, int fin) {
        debutFill = debut;
        endFill = fin;

    }

    static {
        setSize(7, 66);
    }

    public static String TEMPLATE_ID = "Etat des charges";
    public static String TEMPLATE_PROPERTY_NAME = "LocationEtatChargesPaye";

    @Override
    public String getTemplateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected String getYear() {
        return "";
    }

    public EtatChargesPayeSheet(int moisDu, int moisAu, String annee) {
        super();

        this.printer = PrinterNXProps.getInstance().getStringProperty("EtatChargesPayePrinter");
        this.modele = "EtatChargesPaye.ods";
        this.moisAu = moisAu;
        this.moisDu = moisDu;
        this.annee = annee;

        this.nbRowsPerPage = 68;

        createMap();
    }

    private void makeEntete(int row) {
        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        this.mCell.put("A" + row, rowSociete.getObject("NOM"));
        this.mCell.put("F" + row, "Edition du " + dateFormat.format(new Date()));
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
        // sel.addSelect(tableSalarie.getField("ID"));

        Where w = (new Where(tableFichePayeElement.getField("ID_FICHE_PAYE"), "=", tableFichePaye.getField("ID")));
        Where w6 = (new Where(tableFichePayeElement.getField("SOURCE"), "=", "RUBRIQUE_COTISATION"));
        // Where w2 = (new Where(tableFichePaye.getField("ID_SALARIE"), "=",
        // tableSalarie.getField("ID")));
        Where w3 = (new Where(tableFichePaye.getField("ID_MOIS"), new Integer(this.moisDu), new Integer(this.moisAu)));
        Where w4 = (new Where(tableFichePaye.getField("ANNEE"), "=", new Integer(this.annee)));
        Where w5 = (new Where(tableFichePaye.getField("VALIDE"), "=", Boolean.TRUE));

        sel.setWhere(w);
        // sel.andWhere(w2);
        sel.andWhere(w3);
        sel.andWhere(w4);
        sel.andWhere(w5);
        sel.andWhere(w6);
        sel.setDistinct(true);
        String req = sel.asString();

        System.err.println(req);

        // Liste des rubriques de chaque salaries
        List l = (List) base.getDataSource().execute(req, new ArrayListHandler());

        // Association idCaisse, Map rowValsRubCotCumulé
        Map mapCaisse = new HashMap();
        Map mapFiche = new HashMap();

        // Cumuls des rubriques de cotisations par caisse
        for (int i = 0; i < l.size(); i++) {
            Object[] tmp = (Object[]) l.get(i);
            mapFiche.put(tmp[0], "");
            int idFicheElt = Integer.parseInt(tmp[1].toString());

            SQLRow rowFicheElt = tableFichePayeElement.getRow(idFicheElt);
            SQLRow rowRub = tableRubCot.getRow(rowFicheElt.getInt("IDSOURCE"));

            // On recupere la map de la caisse
            Map mapValueRub;
            if (mapCaisse.containsKey(new Integer(rowRub.getInt("ID_CAISSE_COTISATION")))) {
                mapValueRub = (Map) mapCaisse.get(new Integer(rowRub.getInt("ID_CAISSE_COTISATION")));
            } else {
                mapValueRub = new HashMap();
                mapCaisse.put(new Integer(rowRub.getInt("ID_CAISSE_COTISATION")), mapValueRub);
            }

            // on recupere la rowvalues de la rubrique
            SQLRowValues rowVals;
            if (mapValueRub.containsKey(rowFicheElt.getObject("IDSOURCE"))) {
                rowVals = (SQLRowValues) mapValueRub.get(rowFicheElt.getObject("IDSOURCE"));
                // on cumule les données
                if (rowFicheElt.getObject("NB_BASE") != null) {
                    Object o = rowVals.getObject("NB_BASE");
                    float base = (o == null) ? 0.0F : ((Float) o).floatValue();
                    base += rowFicheElt.getFloat("NB_BASE");
                    rowVals.put("NB_BASE", new Float(base));
                }
                if (rowFicheElt.getObject("MONTANT_PAT") != null) {
                    Object o = rowVals.getObject("MONTANT_PAT");
                    float montant = (o == null) ? 0.0F : ((Float) o).floatValue();
                    montant += rowFicheElt.getFloat("MONTANT_PAT");
                    rowVals.put("MONTANT_PAT", new Float(montant));
                }
                if (rowFicheElt.getObject("MONTANT_SAL_DED") != null) {
                    Object o = rowVals.getObject("MONTANT_SAL_DED");
                    float montant = (o == null) ? 0.0F : ((Float) o).floatValue();
                    montant += rowFicheElt.getFloat("MONTANT_SAL_DED");
                    rowVals.put("MONTANT_SAL_DED", new Float(montant));
                }
            } else {
                rowVals = new SQLRowValues(tableFichePayeElement);
                rowVals.loadAllSafe(rowFicheElt);
                float montantPat, montantSal;

                Object o = rowVals.getObject("MONTANT_PAT");
                montantPat = (o == null) ? 0.0F : ((Float) o).floatValue();

                o = rowVals.getObject("MONTANT_SAL_DED");
                montantSal = (o == null) ? 0.0F : ((Float) o).floatValue();

                if (montantPat != 0 || montantSal != 0) {
                    mapValueRub.put(rowFicheElt.getObject("IDSOURCE"), rowVals);
                }
            }

        }

        // Fill
        int posLine = 1;
        int firstLine = 1;

        System.err.println("Dump fiche " + mapFiche);
        System.err.println("NB Pages = " + mapCaisse.keySet().size());

        for (int n = 0; n < mapCaisse.keySet().size(); n++) {

            // entete
            makeEntete(posLine);
            posLine += (debutFill - 1);

            Map mapValue = (Map) mapCaisse.get(mapCaisse.keySet().toArray()[n]);
            float totalMontantSal = 0.0F;
            float totalMontantPat = 0.0F;

            SQLRow rowCaisse = tableCaisse.getRow(Integer.parseInt(mapCaisse.keySet().toArray()[n].toString()));
            this.mCell.put("A" + posLine, "Caisse " + rowCaisse.getObject("NOM"));
            this.mCell.put("B" + posLine, "");
            this.mCell.put("C" + posLine, "");
            this.mCell.put("D" + posLine, "");
            this.mCell.put("E" + posLine, "");
            this.mCell.put("F" + posLine, "");
            this.mapStyleRow.put(new Integer(posLine), "Titre 1");

            posLine++;

            for (int i = 0; i < mapValue.keySet().size(); i++) {
                SQLRowValues rowVals = (SQLRowValues) mapValue.get(mapValue.keySet().toArray()[i]);

                this.mCell.put("A" + posLine, rowVals.getObject("NOM"));
                this.mCell.put("B" + posLine, rowVals.getObject("NB_BASE"));

                Float oTxSal = (Float) rowVals.getObject("TAUX_SAL");
                float txSal = (oTxSal == null) ? 0.0F : oTxSal.floatValue();
                Float oTxPat = (Float) rowVals.getObject("TAUX_PAT");
                float txPat = (oTxPat == null) ? 0.0F : oTxPat.floatValue();
                this.mCell.put("C" + posLine, new Float(txSal + txPat));

                System.err.println(rowVals.getObject("MONTANT_SAL_DED").getClass());
                Float oMontantSal = (Float) rowVals.getObject("MONTANT_SAL_DED");
                float montantSal = (oMontantSal == null) ? 0.0F : oMontantSal.floatValue();
                Float oMontantPat = (Float) rowVals.getObject("MONTANT_PAT");
                float montantPat = (oMontantPat == null) ? 0.0F : oMontantPat.floatValue();
                this.mCell.put("D" + posLine, new Float(montantPat));
                this.mCell.put("E" + posLine, new Float(montantSal));
                this.mCell.put("F" + posLine, new Float(montantSal + montantPat));
                totalMontantPat += montantPat;
                totalMontantSal += montantSal;

                this.mapStyleRow.put(new Integer(posLine), "Normal");
                posLine++;
            }

            this.mCell.put("A" + posLine, "Total");
            this.mCell.put("B" + posLine, "");
            this.mCell.put("C" + posLine, "");
            this.mCell.put("D" + posLine, new Float(totalMontantPat));
            this.mCell.put("E" + posLine, new Float(totalMontantSal));
            this.mCell.put("F" + posLine, new Float(totalMontantPat + totalMontantSal));
            this.mapStyleRow.put(new Integer(posLine), "Titre 1");

            // pied de page
            posLine = firstLine + endFill - 1;
            posLine += 2;
            makeBasPage(posLine);

            posLine++;
            firstLine = posLine;
        }

        this.nbPage = mapCaisse.size();

        System.err.println("Nombre de page " + this.nbPage);

        // on conserve la page d'origine du model

        if (this.nbPage > 0) {
            this.nbPage--;
        }
    }
}
