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
import org.openconcerto.erp.generationDoc.SpreadSheetGeneratorGestComm;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.map.model.Ville;
import org.jopendocument.link.Component;
import org.jopendocument.link.OOConnexion;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.Tuple2;

import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;


import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class FichePayeSheet extends SheetInterface {
    // TODO Ajouter un champ DIF dans le modele pour le remplir a la main --> Droit individuel à la
    // formation
    private final static SQLTable tableFiche = base.getTable("FICHE_PAYE");
    private final static SQLTable tableFicheElt = base.getTable("FICHE_PAYE_ELEMENT");
    private final static SQLTable tableMois = base.getTable("MOIS");
    private final static SQLTable tableAdresse = base.getTable("ADRESSE");
    private final static SQLTable tableAdresseCommon = Configuration.getInstance().getBase().getTable("ADRESSE_COMMON");
    private final static SQLTable tableSalarie = base.getTable("SALARIE");
    private final static SQLTable tableEtatCivil = base.getTable("ETAT_CIVIL");
    private final static SQLTable tableInfosPaye = base.getTable("INFOS_SALARIE_PAYE");
    private final static SQLTable tableReglementPaye = base.getTable("REGLEMENT_PAYE");
    private final static SQLTable tableContrat = base.getTable("CONTRAT_SALARIE");
    private final static SQLTable tableModeRegl = base.getTable("MODE_REGLEMENT_PAYE");
    private final static SQLTable tableCumulsConges = base.getTable("CUMULS_CONGES");
    private final static SQLTable tableCumulsPaye = base.getTable("CUMULS_PAYE");
    private final static SQLTable tableVarPeriode = base.getTable("VARIABLE_SALARIE");
    private final static SQLTable tableConventionC = base.getTable("IDCC");
    private final static DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);

    private Map styleMapRow;

    public Map getStyleMapRow() {
        return this.styleMapRow;
    }

    public FichePayeSheet(int idFiche) {
        super(idFiche, tableFiche);
        init();
    }

    public FichePayeSheet(SQLRow rowFiche) {
        super(rowFiche);
        init();
    }

    // Nom du fichier généré
    public static String getFileName(int id, int type) {
        return getFileName(tableFiche.getRow(id), type);
    }

    // génération d'une fiche de paye
    public static void generation(int id) {
        generation(tableFiche.getRow(id));
    }

    public static void generation(SQLRow row) {
        generation(row, true);
    }

    public static void generation(SQLRow row, boolean visu) {
        FichePayeSheet fSheet = new FichePayeSheet(row.getID());
        new SpreadSheetGeneratorGestComm(fSheet, FichePayeSheet.getFileName(row, FichePayeSheet.typeNoExtension), false, visu);
    }

    // impression d'une fiche de paye
    public static void impression(int id) {
        impression(tableFiche.getRow(id));
    }

    public static void impression(SQLRow row) {
        final File f = getFile(row, typeOO);
        if (f.exists()) {
            try {
                final OOConnexion ooConnexion = ComptaPropsConfiguration.getOOConnexion();
                if (ooConnexion == null) {
                    return;
                }
                final Component doc = ooConnexion.loadDocument(f, true);

                Map<String, Object> map = new HashMap<String, Object>();
                map.put("Name", PrinterNXProps.getInstance().getStringProperty("FichePayePrinter"));
                doc.printDocument(map);
                doc.close();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                ExceptionHandler.handle("Impossible de charger le document OpenOffice", e);
            }
        }
    }

    // visualisation d'une fiche
    public static void visualisation(SQLRow r, int type) {
        final File f = getFile(r, type);
        if (f.exists()) {
            try {
                final OOConnexion ooConnexion = ComptaPropsConfiguration.getOOConnexion();
                if (ooConnexion == null) {
                    return;
                }
                ooConnexion.loadDocument(f, false);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                ExceptionHandler.handle("Impossible de charger le document OpenOffice", e);
            }
        }
    }

    public static File getFile(SQLRow r, int type) {
        return new File(getLocation(r, type), getFileName(r.getID(), type));
    }

    public static String getFileName(SQLRow r, int type) {

        SQLRow rowSal = tableSalarie.getRow(r.getInt("ID_SALARIE"));
        SQLRow rowMois = tableMois.getRow(r.getInt("ID_MOIS"));
        if (type == FichePayeSheet.typeOO) {
            return ("FichePaye_" + rowSal.getString("CODE") + "_" + rowMois.getString("NOM") + "_" + r.getString("ANNEE") + ".ods");
        } else {
            if (type == FichePayeSheet.typePDF) {
                return ("FichePaye_" + rowSal.getString("CODE") + "_" + rowMois.getString("NOM") + "_" + r.getString("ANNEE") + ".pdf");
            } else {
                return ("FichePaye_" + rowSal.getString("CODE") + "_" + rowMois.getString("NOM") + "_" + r.getString("ANNEE"));
            }
        }
    }

    // Emplacement des fichiers générés
    public static String getLocation(int id, int type) {

        return getLocation(tableFiche.getRow(id), type);
    }

    private static final Tuple2<String, String> tuple = Tuple2.create("LocationFichePaye", "Fiche de paye");

    public static Tuple2<String, String> getTuple2Location() {
        return tuple;
    }

    public static String getLocation(SQLRow r, int type) {

        return SheetXml.getLocationForTuple(tuple, !(type == FichePayeSheet.typeOO)) + File.separator + r.getString("ANNEE");
    }

    private void init() {
        this.modele = "FichePaye.ods";
        this.printer = PrinterNXProps.getInstance().getStringProperty("FichePayePrinter");
        this.locationOO = getLocation(this.row, typeOO);
        this.locationPDF = getLocation(this.row, typePDF);
    }

    protected void createMap() {

        this.styleMapRow = new HashMap();
        this.mapReplace = new HashMap();

        this.mCell = new HashMap();

        // Infos societe
        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        this.mCell.put("B1", rowSociete.getObject("TYPE") + " " + rowSociete.getObject("NOM"));
        SQLRow rowAdrSociete = tableAdresseCommon.getRow(rowSociete.getInt("ID_ADRESSE_COMMON"));
        this.mCell.put("B2", rowAdrSociete.getObject("RUE"));
        this.mCell.put("B3", getVilleCP(rowAdrSociete.getString("VILLE")) + " " + getVille(rowAdrSociete.getString("VILLE")));

        this.mCell.put("D5", rowSociete.getObject("NUM_SIRET"));
        this.mCell.put("D6", rowSociete.getObject("NUM_APE"));
        this.mapReplace.put("D8", rowSociete.getObject("NUMERO_URSSAF"));

        // Infos Salarie
        SQLRow rowSal = tableSalarie.getRow(this.row.getInt("ID_SALARIE"));
        SQLRow rowEtatCivil = tableEtatCivil.getRow(rowSal.getInt("ID_ETAT_CIVIL"));
        this.mCell.put("G8", rowSal.getObject("NOM") + " " + rowSal.getObject("PRENOM"));
        SQLRow rowAdrSal = tableAdresse.getRow(rowEtatCivil.getInt("ID_ADRESSE"));
        this.mCell.put("G9", rowAdrSal.getObject("RUE"));
        this.mCell.put("G11", getVilleCP(rowAdrSal.getString("VILLE")) + " " + getVille(rowAdrSal.getString("VILLE")));

        this.mCell.put("D13", rowEtatCivil.getObject("NUMERO_SS"));

        SQLRow rowInfosPaye = tableInfosPaye.getRow(rowSal.getInt("ID_INFOS_SALARIE_PAYE"));
        SQLRow rowContrat = tableContrat.getRow(rowInfosPaye.getInt("ID_CONTRAT_SALARIE"));

        if (this.row.getString("NATURE_EMPLOI").trim().length() == 0) {
            this.mCell.put("D14", rowContrat.getObject("NATURE"));
        } else {
            this.mCell.put("D14", this.row.getString("NATURE_EMPLOI"));
        }

        SQLRow rowCC;
        if (this.row.getInt("ID_IDCC") > 1) {
            rowCC = tableConventionC.getRow(this.row.getInt("ID_IDCC"));
        } else {
            rowCC = tableConventionC.getRow(rowInfosPaye.getInt("ID_IDCC"));
        }
        this.mCell.put("D15", rowCC.getString("NOM"));

        // Bulletin du
        // Bulletin de paie du
        Date du = (Date) this.row.getObject("DU");
        Date au = (Date) this.row.getObject("AU");
        this.mCell.put("F1", "Bulletin de paie du " + dateFormat.format(du) + " au " + dateFormat.format(au));

        // Paiement le
        SQLRow rowRegl;
        if (this.row.getInt("ID_REGLEMENT_PAYE") <= 1) {
            rowRegl = tableReglementPaye.getRow(rowSal.getInt("ID_REGLEMENT_PAYE"));
        } else {
            rowRegl = tableReglementPaye.getRow(this.row.getInt("ID_REGLEMENT_PAYE"));
        }
        SQLRow rowModeRegl = tableModeRegl.getRow(rowRegl.getInt("ID_MODE_REGLEMENT_PAYE"));

        Calendar c = Calendar.getInstance();

        c.set(Calendar.MONTH, this.row.getInt("ID_MOIS") - 2);
        c.set(Calendar.YEAR, Integer.parseInt(this.row.getString("ANNEE")));

        if (rowRegl.getInt("LE") != 31) {

            c.set(Calendar.MONTH, c.get(Calendar.MONTH) + 1);
        }

        int max = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        int day = Math.min(rowRegl.getInt("LE"), max);

        c.set(Calendar.DAY_OF_MONTH, day);

        this.mCell.put("H3", dateFormat.format(c.getTime()));
        this.mCell.put("I3", "Par " + rowModeRegl.getObject("NOM"));

        // Congés
        // "G3";
        SQLRow rowConges;
        if (this.row.getInt("ID_CUMULS_CONGES") <= 1) {
            rowConges = tableCumulsConges.getRow(rowSal.getInt("ID_CUMULS_CONGES"));
        } else {
            rowConges = tableCumulsConges.getRow(this.row.getInt("ID_CUMULS_CONGES"));
        }

        SQLRow rowVarSal;
        if (this.row.getInt("ID_VARIABLE_SALARIE") <= 1) {
            rowVarSal = tableVarPeriode.getRow(rowSal.getInt("ID_VARIABLE_SALARIE"));
        } else {
            rowVarSal = tableVarPeriode.getRow(this.row.getInt("ID_VARIABLE_SALARIE"));
        }

        float congesPris = rowVarSal.getFloat("CONGES_PRIS");
        float congesRestant = rowConges.getFloat("RESTANT") - congesPris;
        float congesAcquis = rowConges.getFloat("ACQUIS") + this.row.getFloat("CONGES_ACQUIS");
        this.mCell.put("G14", new Float(congesPris));
        this.mCell.put("H14", new Float(congesRestant));
        this.mCell.put("I14", new Float(congesAcquis));

        // Element Devis
        SQLSelect selElt = new SQLSelect(base);
        selElt.addSelect(tableFicheElt.getField("ID"));
        selElt.setWhere("FICHE_PAYE_ELEMENT.ID_FICHE_PAYE", "=", this.row.getID());

        String req = selElt.asString() + " ORDER BY \"FICHE_PAYE_ELEMENT\".\"POSITION\"";
        List l = (List) base.getDataSource().execute(req, new ArrayListHandler());
        int pos = 20;
        for (Iterator i = l.iterator(); i.hasNext();) {
            Object[] o = (Object[]) i.next();
            SQLRow rowTmp = tableFicheElt.getRow(Integer.parseInt(o[0].toString()));

            if (rowTmp.getBoolean("IMPRESSION") && rowTmp.getBoolean("IN_PERIODE")) {

                Object nomTmp = rowTmp.getObject("NOM");
                this.mCell.put("B" + pos, nomTmp);

                // Base
                Object baseTmp = rowTmp.getObject("NB_BASE");

                if (baseTmp != null) {
                    float base = ((Float) baseTmp).floatValue();
                    if (base != 0) {
                        this.mCell.put("E" + pos, baseTmp);
                    } else {
                        this.mCell.put("E" + pos, "");
                    }
                } else {
                    this.mCell.put("E" + pos, baseTmp);
                }

                // Taux Sal
                Object tauxSalTmp = rowTmp.getObject("TAUX_SAL");

                if (tauxSalTmp != null) {
                    float tauxSal = ((Float) tauxSalTmp).floatValue();
                    if (tauxSal != 0) {
                        this.mCell.put("F" + pos, tauxSalTmp);
                    } else {
                        this.mCell.put("F" + pos, "");
                    }
                } else {
                    this.mCell.put("F" + pos, tauxSalTmp);
                }

                // Montant Sal Aj
                Object montantSalAjTmp = rowTmp.getObject("MONTANT_SAL_AJ");
                if (montantSalAjTmp != null) {
                    float montantSalAj = ((Float) montantSalAjTmp).floatValue();
                    if (montantSalAj != 0) {
                        this.mCell.put("G" + pos, montantSalAjTmp);
                    } else {
                        this.mCell.put("G" + pos, "");
                    }
                } else {
                    this.mCell.put("G" + pos, montantSalAjTmp);
                }

                // Montant Sal ded
                Object montantSalDedTmp = rowTmp.getObject("MONTANT_SAL_DED");
                if (montantSalDedTmp != null) {
                    float montantSalDed = ((Float) montantSalDedTmp).floatValue();
                    if (montantSalDed != 0) {
                        this.mCell.put("H" + pos, montantSalDedTmp);
                    } else {
                        this.mCell.put("H" + pos, "");
                    }
                } else {
                    this.mCell.put("H" + pos, montantSalDedTmp);
                }

                // Taux Pat
                Object tauxPatTmp = rowTmp.getObject("TAUX_PAT");
                if (tauxPatTmp != null) {
                    float tauxPat = ((Float) tauxPatTmp).floatValue();
                    if (tauxPat != 0) {
                        this.mCell.put("I" + pos, tauxPatTmp);
                    } else {
                        this.mCell.put("I" + pos, "");
                    }
                } else {
                    this.mCell.put("I" + pos, tauxPatTmp);
                }

                // Montant Pat
                Object montantPatTmp = rowTmp.getObject("MONTANT_PAT");
                if (montantPatTmp != null) {
                    float montantPat = ((Float) montantPatTmp).floatValue();
                    if (montantPat != 0) {
                        this.mCell.put("J" + pos, montantPatTmp);
                    } else {
                        this.mCell.put("J" + pos, "");
                    }
                } else {
                    this.mCell.put("J" + pos, montantPatTmp);
                }

                if (rowTmp.getString("SOURCE").equalsIgnoreCase("RUBRIQUE_COMM")) {
                    this.mapStyleRow.put(new Integer(pos), "Titre 1");
                } else {
                    this.mapStyleRow.put(new Integer(pos), "Normal");
                }

                pos++;
            }
        }

        // Totaux
        float netApayerCumul = this.row.getFloat("NET_A_PAYER");
        float salBrutCumul = this.row.getFloat("SAL_BRUT");
        float cotSalCumul = this.row.getFloat("COT_SAL");
        float cotPatCumul = this.row.getFloat("COT_PAT");
        float netImpCumul = this.row.getFloat("NET_IMP");
        this.mCell.put("I61", this.row.getObject("NET_A_PAYER"));
        this.mCell.put("D61", this.row.getObject("SAL_BRUT"));
        this.mCell.put("E61", this.row.getObject("COT_SAL"));
        this.mCell.put("F61", this.row.getObject("COT_PAT"));
        this.mCell.put("H61", this.row.getObject("NET_IMP"));

        SQLRow rowCumulsPaye;

        if (this.row.getInt("ID_CUMULS_PAYE") == 1) {
            rowCumulsPaye = tableCumulsPaye.getRow(rowSal.getInt("ID_CUMULS_PAYE"));
        } else {
            rowCumulsPaye = tableCumulsPaye.getRow(this.row.getInt("ID_CUMULS_PAYE"));
        }

        netApayerCumul += rowCumulsPaye.getFloat("NET_A_PAYER_C");
        salBrutCumul += rowCumulsPaye.getFloat("SAL_BRUT_C");
        cotSalCumul += rowCumulsPaye.getFloat("COT_SAL_C");
        cotPatCumul += rowCumulsPaye.getFloat("COT_PAT_C");
        netImpCumul += rowCumulsPaye.getFloat("NET_IMP_C");

        this.mCell.put("D62", new Float(salBrutCumul));
        this.mCell.put("E62", new Float(cotSalCumul));
        this.mCell.put("F62", new Float(cotPatCumul));
        this.mCell.put("H62", new Float(netImpCumul));
    }

    private static String getVille(final String name) {

        Ville ville = Ville.getVilleFromVilleEtCode(name);
        if (ville == null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(null, "La ville " + "\"" + name + "\"" + " est introuvable! Veuillez corriger l'erreur!");
                }
            });
            return null;
        }
        return ville.getName();
    }

    private static String getVilleCP(String name) {
        Ville ville = Ville.getVilleFromVilleEtCode(name);
        if (ville == null) {

            return null;
        }
        return ville.getCodepostal();
    }

}
