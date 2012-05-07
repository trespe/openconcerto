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
 
 package org.openconcerto.erp.core.finance.accounting.report;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.generationDoc.AbstractListeSheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.erp.rights.ComptaUserRight;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.cc.ITransformer;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class GrandLivreSheetXML extends AbstractListeSheetXml {

    private static int debutFill, endFill;
    public static int MODEALL = 1;
    public static int MODELETTREE = 2;
    public static int MODENONLETTREE = 3;
    private final static SQLTable tableEcriture = base.getTable("ECRITURE");
    private final static SQLTable tableJournal = base.getTable("JOURNAL");
    private final static SQLTable tableMvt = base.getTable("MOUVEMENT");
    private final static SQLTable tableCompte = base.getTable("COMPTE_PCE");

    private final static DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
    private final static DateFormat dateFormatEcr = DateFormat.getDateInstance(DateFormat.SHORT);
    private SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();

    private Date dateDu, dateAu;
    private String compteDeb, compteEnd;
    private int lettrage;
    private boolean cumul = false;
    private boolean excludeCompteSolde = true;
    private boolean centralClient = false;
    private boolean centralFourn = false;
    int idJrnlExclude = -1;

    public static String TEMPLATE_ID = "GrandLivre";
    public static String TEMPLATE_PROPERTY_NAME = "LocationGrandLivre";

    public static void setSize(int debut, int fin) {
        debutFill = debut;
        endFill = fin;
    }

    @Override
    public String getDefaultTemplateId() {
        return TEMPLATE_ID;
    }

    Date date;

    @Override
    public String getName() {
        if (this.date == null) {
            this.date = new Date();
        }
        return "GrandLivre" + date.getTime();
    }

    public GrandLivreSheetXML(Date du, Date au, String compteDep, String compteEnd, int lettrage, boolean cumul, boolean excludeCptSolde, boolean centralClient, boolean centralFourn, int idJrnlExclude) {
        super();
        Calendar cal = Calendar.getInstance();
        cal.setTime(au);
        this.idJrnlExclude = idJrnlExclude;
        this.printer = PrinterNXProps.getInstance().getStringProperty("GrandLivrePrinter");
        this.dateAu = au;
        this.dateDu = du;
        this.compteDeb = compteDep.trim();
        this.compteEnd = compteEnd.trim();
        this.lettrage = lettrage;
        this.cumul = cumul;
        this.excludeCompteSolde = excludeCptSolde;
        this.centralClient = centralClient;
        this.centralFourn = centralFourn;

        // createMap();
    }

    private String toDay = dateFormat.format(new Date());
    private int size;

    // private void makeEntete(int rowDeb) {
    //
    // this.mCell.put("A" + rowDeb, this.rowSociete.getObject("NOM"));
    // this.mCell.put("G" + rowDeb, "Edition du " + this.toDay);
    // // this.mCell.put("D" + (rowDeb + 2), "Grand livre");
    // // System.err.println("MAKE ENTETE");
    // }
    //
    // private void makePiedPage(int row, String comptes) {
    // this.mCell.put("A" + row, "Compte : " + comptes);
    // this.mCell.put("E" + row, "Période du " + dateFormatEcr.format(this.dateDu) + " au " +
    // dateFormatEcr.format(this.dateAu));
    // }

    private void makeSousTotal(Map<String, Object> line, Map<Integer, String> style, int pos, long debit, long credit) {
        style.put(pos, "Titre 1");

        line.put("DATE", "");
        line.put("JOURNAL", "");
        line.put("MOUVEMENT", "");
        line.put("LIBELLE", "Sous total");
        line.put("DEBIT", Double.valueOf(GestionDevise.currencyToString(debit, false)));
        line.put("CREDIT", Double.valueOf(GestionDevise.currencyToString(credit, false)));
        line.put("SOLDE", Double.valueOf(GestionDevise.currencyToString(debit - credit, false)));
    }

    protected void createListeValues() {
        Date d = new Date();

        final SQLRowValues vals = new SQLRowValues(tableEcriture);
        vals.put("ID_COMPTE_PCE", null);
        vals.put("COMPTE_NUMERO", null);
        vals.put("COMPTE_NOM", null);
        vals.put("ID_JOURNAL", null);
        vals.put("JOURNAL_CODE", null);
        vals.putRowValues("ID_MOUVEMENT").put("NUMERO", null);
        vals.put("CREDIT", null);
        vals.put("DEBIT", null);
        vals.put("DATE", null);
        vals.put("NOM", null);

        final List<Integer> lCompteSolde;
        if (GrandLivreSheetXML.this.excludeCompteSolde) {
            lCompteSolde = getListeCompteSolde();
        } else {
            lCompteSolde = null;
        }
        Map<Integer, Long> mapCumul = getCumulsAnterieur(GrandLivreSheetXML.this.dateDu, lCompteSolde);

        final SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(vals);
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(SQLSelect sel) {

                Where w = (new Where(tableEcriture.getField("DATE"), GrandLivreSheetXML.this.dateDu, GrandLivreSheetXML.this.dateAu));

                if (GrandLivreSheetXML.this.compteDeb.equals(GrandLivreSheetXML.this.compteEnd)) {
                    w = w.and(new Where(tableEcriture.getField("COMPTE_NUMERO"), "=", GrandLivreSheetXML.this.compteDeb));
                } else {
                    w = w.and(new Where(tableEcriture.getField("COMPTE_NUMERO"), (Object) GrandLivreSheetXML.this.compteDeb, (Object) GrandLivreSheetXML.this.compteEnd));
                }
                w = w.and(new Where(tableEcriture.getField("ID_JOURNAL"), "!=", idJrnlExclude));
                w = w.and(new Where(tableEcriture.getField("ID_MOUVEMENT"), "=", tableMvt.getField("ID")));

                if (GrandLivreSheetXML.this.lettrage == MODELETTREE) {
                    Object o = null;
                    w = w.and(new Where(tableEcriture.getField("LETTRAGE"), "<>", o));
                    w = w.and(new Where(tableEcriture.getField("LETTRAGE"), "!=", ""));
                } else {
                    if (GrandLivreSheetXML.this.lettrage == MODENONLETTREE) {
                        Object o = null;
                        Where w2 = new Where(tableEcriture.getField("LETTRAGE"), "=", o);
                        w = w.and(w2.or(new Where(tableEcriture.getField("LETTRAGE"), "=", "")));
                    }
                }

                if (GrandLivreSheetXML.this.excludeCompteSolde) {
                    System.err.println("Exclude compte");

                    w = w.and(new Where(tableEcriture.getField("ID_COMPTE_PCE"), lCompteSolde).not());
                }

                if (!UserManager.getInstance().getCurrentUser().getRights().haveRight(ComptaUserRight.ACCES_NOT_RESCTRICTED_TO_411)) {
                    // TODO Show Restricted acces in UI
                    w = w.and(new Where(tableEcriture.getField("COMPTE_NUMERO"), "LIKE", "411%"));
                }

                sel.setWhere(w);
                sel.addRawOrder("\"ECRITURE\".\"COMPTE_NUMERO\"");
                sel.addRawOrder("\"ECRITURE\".\"DATE\"");
                sel.addRawOrder("\"MOUVEMENT\".\"NUMERO\"");
                System.err.println(sel.asString());
                return sel;
            }
        });

        List<SQLRowValues> list = fetcher.fetch();
        size = list.size();
        System.err.println("START CREATE Grand livre, NB ecritures  " + size);

        long totalDebit, totalCredit, sousTotalDebit, sousTotalCredit, totalCreditAntC, totalDebitAntC, totalCreditAntF, totalDebitAntF;

        totalDebit = 0;
        totalCredit = 0;
        sousTotalCredit = 0;
        sousTotalDebit = 0;
        totalCreditAntC = 0;
        totalDebitAntC = 0;
        totalCreditAntF = 0;
        totalDebitAntF = 0;
        SQLRowValues rowFirstEcr = null;
        int idCptFirstEcr = 1;

        boolean setTitle = true;
        boolean setLine = false;
        boolean setCumuls = true;
        boolean firstEcrCentC = true;
        boolean firstEcrCentF = true;
        String numCptFirstEcr = "411";
        String numCptClient = "411";
        String nomCptClient = "Clients";
        String numCptFourn = "401";
        String nomCptFourn = "Fournisseurs";
        int idCptClient = ComptePCESQLElement.getId(numCptClient, nomCptClient);
        int idCptFourn = ComptePCESQLElement.getId(numCptFourn, nomCptFourn);

        final String titre3 = "Titre 3";
        final String cumulAntString = "Cumuls antérieurs";
        // int j = 0;

        // Valeur de la liste
        // listAllSheetValues ;

        // Style des lignes
        // styleAllSheetValues;

        // Valeur à l'extérieur de la liste
        // mapAllSheetValues

        List<Map<String, Object>> tableauVals = new ArrayList<Map<String, Object>>();
        this.listAllSheetValues.put(0, tableauVals);

        Map<Integer, String> style = new HashMap<Integer, String>();
        this.styleAllSheetValues.put(0, style);

        // Affiche le nom du compte
        setTitle = true;
        // ligne vide avant de mettre le setTitle
        setLine = false;
        for (int i = 0; i < size;) {
            // System.err.println(i);
            // // System.err.println("START NEW PAGE; POS : " + posLine);
            //
            // /***************************************************************************************
            // * ENTETE
            // **************************************************************************************/
            // // makeEntete(posLine);
            // // posLine += debutFill - 1;

            /***************************************************************************************
             * CONTENU
             **************************************************************************************/
            final Double doubleZero = Double.valueOf("0");

            SQLRowValues rowEcr = list.get(i);

            int idCpt = rowEcr.getInt("ID_COMPTE_PCE");
            String nomCpt = rowEcr.getString("COMPTE_NOM");
            String numCpt = rowEcr.getString("COMPTE_NUMERO");

            Map<String, Object> ooLine = new HashMap<String, Object>();
            tableauVals.add(ooLine);
            // Cumuls antérieurs
            if (setCumuls && this.cumul && !setTitle) {

                style.put(tableauVals.size(), titre3);
                ooLine.put("DATE", "");
                ooLine.put("JOURNAL", "");
                ooLine.put("MOUVEMENT", "");

                ooLine.put("LIBELLE", cumulAntString);
                Long longSolde = mapCumul.get(idCpt);

                if (longSolde == null) {
                    longSolde = Long.valueOf(0);
                }
                long debitCumulAnt = 0;
                long creditCumulAnt = 0;

                if (longSolde > 0) {
                    debitCumulAnt = longSolde;
                } else {
                    creditCumulAnt = -longSolde;
                }
                ooLine.put("DEBIT", (debitCumulAnt == 0) ? doubleZero : Double.valueOf(GestionDevise.currencyToString(debitCumulAnt, false)));
                ooLine.put("CREDIT", (creditCumulAnt == 0) ? doubleZero : Double.valueOf(GestionDevise.currencyToString(creditCumulAnt, false)));
                ooLine.put("SOLDE", (longSolde == 0) ? doubleZero : Double.valueOf(GestionDevise.currencyToString(longSolde, false)));

                totalCredit += creditCumulAnt;
                totalDebit += debitCumulAnt;

                sousTotalCredit += creditCumulAnt;
                sousTotalDebit += debitCumulAnt;
                setCumuls = false;
            } else {
                // Titre
                if (setTitle) {
                    if (!setLine) {
                        style.put(tableauVals.size() - 1, "Titre 1");

                        // Si on centralise les comptes clients ou fournisseurs on affiche le
                        // compte 401 ou 411
                        if (this.centralClient && nomCpt.startsWith("411")) {
                            nomCpt = nomCptClient;
                            numCpt = numCptClient;
                            idCpt = idCptClient;
                        }
                        if (this.centralFourn && nomCpt.startsWith("401")) {
                            nomCpt = nomCptFourn;
                            numCpt = numCptFourn;
                            idCpt = idCptFourn;
                        }
                        ooLine.put("DATE", numCpt);
                        ooLine.put("JOURNAL", nomCpt);
                        ooLine.put("MOUVEMENT", "");
                        ooLine.put("LIBELLE", "");
                        ooLine.put("DEBIT", "");
                        ooLine.put("CREDIT", "");
                        ooLine.put("SOLDE", "");
                        setTitle = false;
                        setLine = true;

                        if (rowFirstEcr == null) {
                            rowFirstEcr = rowEcr;
                            idCptFirstEcr = rowEcr.getInt("ID_COMPTE_PCE");
                            numCptFirstEcr = rowEcr.getString("COMPTE_NUMERO");
                        }

                    } else {
                        style.put(tableauVals.size() - 1, "Normal");
                        setLine = false;
                    }
                } else {

                    // si on change de compte alors on applique le style Titre 1
                    if (rowFirstEcr != null && idCptFirstEcr != idCpt && (!this.centralFourn || (!(numCptFirstEcr.startsWith("401") && numCpt.startsWith("401"))))
                            && (!this.centralClient || (!(numCptFirstEcr.startsWith("411") && numCpt.startsWith("411"))))) {

                        rowFirstEcr = rowEcr;
                        idCptFirstEcr = rowFirstEcr.getInt("ID_COMPTE_PCE");
                        numCptFirstEcr = rowEcr.getString("COMPTE_NUMERO");
                        makeSousTotal(ooLine, style, tableauVals.size() - 1, sousTotalDebit, sousTotalCredit);

                        sousTotalCredit = 0;
                        sousTotalDebit = 0;
                        setTitle = true;
                        setCumuls = true;
                    } else {
                        long cred = rowEcr.getLong("CREDIT");
                        long deb = rowEcr.getLong("DEBIT");
                        // Centralisation fournisseur
                        if (this.centralFourn && numCpt.startsWith("401")) {
                            i++;

                            if (firstEcrCentF) {
                                // posLine++;
                                ooLine.put("LIBELLE", "Centralisation des comptes fournisseurs");
                                style.put(tableauVals.size() - 1, "Normal");
                                firstEcrCentF = false;
                            }

                            totalCreditAntF += cred;
                            totalDebitAntF += deb;
                            sousTotalCredit += cred;
                            sousTotalDebit += deb;
                            long solde = totalDebitAntF - totalCreditAntF;
                            ooLine.put("DEBIT", (totalDebitAntF == 0) ? doubleZero : new Double(GestionDevise.currencyToString(totalDebitAntF, false)));
                            ooLine.put("CREDIT", (totalCreditAntF == 0) ? doubleZero : new Double(GestionDevise.currencyToString(totalCreditAntF, false)));
                            ooLine.put("SOLDE", (solde == 0) ? doubleZero : new Double(GestionDevise.currencyToString(solde, false)));

                            continue;
                        }
                        // Centralisation client
                        if (this.centralClient && numCpt.startsWith("411")) {
                            i++;
                            if (firstEcrCentC) {
                                // posLine++;
                                ooLine.put("LIBELLE", "Centralisation des comptes clients");
                                style.put(tableauVals.size() - 1, "Normal");
                                firstEcrCentC = false;
                            }

                            totalCreditAntC += cred;
                            totalDebitAntC += deb;
                            sousTotalCredit += cred;
                            sousTotalDebit += deb;
                            long solde = totalDebitAntC - totalCreditAntC;
                            ooLine.put("DEBIT", (totalDebitAntC == 0) ? doubleZero : Double.valueOf(GestionDevise.currencyToString(totalDebitAntC, false)));
                            ooLine.put("CREDIT", (totalCreditAntC == 0) ? doubleZero : Double.valueOf(GestionDevise.currencyToString(totalCreditAntC, false)));
                            ooLine.put("SOLDE", (solde == 0) ? doubleZero : Double.valueOf(GestionDevise.currencyToString(solde, false)));

                            continue;
                        }

                        ooLine.put("DATE", dateFormatEcr.format((Date) rowEcr.getObject("DATE")));

                        ooLine.put("JOURNAL", rowEcr.getString("JOURNAL_CODE"));
                        ooLine.put("MOUVEMENT", rowEcr.getForeign("ID_MOUVEMENT").getObject("NUMERO"));
                        ooLine.put("LIBELLE", rowEcr.getObject("NOM"));

                        totalCredit += cred;
                        totalDebit += deb;

                        sousTotalCredit += cred;
                        sousTotalDebit += deb;
                        long solde = sousTotalDebit - sousTotalCredit;

                        ooLine.put("DEBIT", (deb == 0) ? doubleZero : Double.valueOf(GestionDevise.currencyToString(deb, false)));
                        ooLine.put("CREDIT", (cred == 0) ? doubleZero : Double.valueOf(GestionDevise.currencyToString(cred, false)));
                        ooLine.put("SOLDE", (solde == 0) ? doubleZero : Double.valueOf(GestionDevise.currencyToString(solde, false)));

                        style.put(tableauVals.size() - 1, "Normal");
                        i++;
                    }

                }

            }

            // if (i >= size && j < endFill - debutFill + 1) {
            // makeSousTotal(posLine, sousTotalDebit, sousTotalCredit);
            // }
            //
            // posLine = firstLine + endFill;
            /*
             * if (this.mapStyleRow.get(new Integer(posLine - 1)) != null) {
             * this.mapStyleRow.put(new Integer(posLine - 1), "Titre 2"); }
             */

            // Total
            // this.mCell.put("E" + posLine, (totalDebit == 0) ? doubleZero : new
            // Double(GestionDevise.currencyToString(totalDebit, false)));
            // this.mCell.put("F" + posLine, (totalCredit == 0) ? doubleZero : new
            // Double(GestionDevise.currencyToString(totalCredit, false)));
            // this.mCell.put("G" + posLine, (totalDebit - totalCredit == 0) ? doubleZero : new
            // Double(GestionDevise.currencyToString(totalDebit - totalCredit, false)));
            //
            // posLine += 2;
            //
            // // bas de page
            // makePiedPage(posLine, this.compteDeb + " à " + this.compteEnd);
            //
            // posLine++;
            // firstLine = posLine;
            // this.nbPage++;
            //
            // if (i >= size && j >= (endFill - debutFill + 1)) {
            //
            // makeEntete(posLine);
            // posLine += debutFill - 1;
            // makeSousTotal(posLine, sousTotalDebit, sousTotalCredit);
            // this.nbPage++;
            // }

        }

        // Total
        // this.mCell.put("E" + posLine, (totalDebit == 0) ? doubleZero : new
        // Double(GestionDevise.currencyToString(totalDebit, false)));
        // this.mCell.put("F" + posLine, (totalCredit == 0) ? doubleZero : new
        // Double(GestionDevise.currencyToString(totalCredit, false)));
        // this.mCell.put("G" + posLine, (totalDebit - totalCredit == 0) ? doubleZero : new
        // Double(GestionDevise.currencyToString(totalDebit - totalCredit, false)));
        Map<String, Object> sheetVals = new HashMap<String, Object>();
        this.mapAllSheetValues.put(0, sheetVals);

        if (size > 0) {
            Map<String, Object> ooLine = new HashMap<String, Object>();
            tableauVals.add(ooLine);
            makeSousTotal(ooLine, style, tableauVals.size() - 1, sousTotalDebit, sousTotalCredit);

            sheetVals.put("TOTAL_DEBIT", (totalDebit == 0) ? 0 : new Double(GestionDevise.currencyToString(totalDebit, false)));
            sheetVals.put("TOTAL_CREDIT", (totalCredit == 0) ? 0 : new Double(GestionDevise.currencyToString(totalCredit, false)));
            sheetVals.put("TOTAL_SOLDE", (totalDebit - totalCredit == 0) ? 0 : new Double(GestionDevise.currencyToString(totalDebit - totalCredit, false)));
        }

        sheetVals.put("TITRE_1", "GRAND LIVRE " + this.rowSociete.getString("TYPE") + " " + this.rowSociete.getString("NOM"));
        sheetVals.put("DATE_EDITION", new Date());
        sheetVals.put("TITRE_2", "Compte : " + this.compteDeb + " à " + this.compteEnd + ". Période du " + dateFormatEcr.format(this.dateDu) + " au " + dateFormatEcr.format(this.dateAu) + ".");
        // on conserve la page d'origine du model
        // if (this.nbPage > 0) {
        // this.nbPage--;
        // }

        Date end = new Date();
        System.err.println("///////// TAKE " + (end.getTime() - d.getTime()) + " millisecondes TO CREATE MAP");
    }

    private List<Integer> getListeCompteSolde() {
        SQLSelect sel = new SQLSelect(base);

        sel.addSelect(tableCompte.getField("ID"));
        sel.addSelect(tableEcriture.getField("DEBIT"), "SUM");
        sel.addSelect(tableEcriture.getField("CREDIT"), "SUM");

        Where w;
        if (this.compteDeb.equals(this.compteEnd)) {
            w = new Where(tableCompte.getField("NUMERO"), "=", this.compteDeb);
        } else {
            w = new Where(tableCompte.getField("NUMERO"), (Object) this.compteDeb, (Object) this.compteEnd);
        }

        w = w.and(new Where(tableEcriture.getField("ID_COMPTE_PCE"), "=", tableCompte.getField("ID")));

        if (this.cumul) {
            w = w.and(new Where(tableEcriture.getField("DATE"), "<=", this.dateAu));
        } else {
            w = w.and(new Where(tableEcriture.getField("DATE"), this.dateDu, this.dateAu));
        }
        w = w.and(new Where(tableEcriture.getField("ID_JOURNAL"), "!=", idJrnlExclude));
        if (this.lettrage == MODELETTREE) {
            Object o = null;
            w = w.and(new Where(tableEcriture.getField("LETTRAGE"), "<>", o));
            w = w.and(new Where(tableEcriture.getField("LETTRAGE"), "!=", ""));
        } else {
            if (this.lettrage == MODENONLETTREE) {
                Object o = null;
                Where w2 = new Where(tableEcriture.getField("LETTRAGE"), "=", o);
                w = w.and(w2.or(new Where(tableEcriture.getField("LETTRAGE"), "=", "")));

            }
        }

        sel.setWhere(w);

        String req = sel.asString() + " GROUP BY \"COMPTE_PCE\".\"ID\"";
        System.err.println(req);
        List<Object[]> l = (List) base.getDataSource().execute(req, new ArrayListHandler());
        List<Integer> list = new ArrayList<Integer>();
        for (Object[] o : l) {
            long credit = 0;
            if (o[2] != null) {
                credit = Long.valueOf(o[2].toString());
            }

            long debit = 0;
            if (o[1] != null) {
                debit = Long.valueOf(o[1].toString());
            }

            int id = Integer.valueOf(o[0].toString());
            long solde = debit - credit;
            if (solde == 0) {
                list.add(id);
            }
        }
        return list;
    }

    /**
     * @param d date limite des cumuls
     * @return Map<Integer id compte, Long solde(debit-credit)>
     */
    private Map<Integer, Long> getCumulsAnterieur(Date d, List<Integer> listCompteSolde) {
        SQLSelect sel = new SQLSelect(base);

        sel.addSelect(tableEcriture.getField("ID_COMPTE_PCE"));
        sel.addSelect(tableEcriture.getField("DEBIT"), "SUM");
        sel.addSelect(tableEcriture.getField("CREDIT"), "SUM");
        sel.addSelect(tableEcriture.getField("COMPTE_NUMERO"));
        // sel.addSelect(tableEcriture.getField("ID_MOUVEMENT"));
        Where w = (new Where(tableEcriture.getField("DATE"), "<", d));
        w = w.and(new Where(tableEcriture.getField("ID_MOUVEMENT"), "=", tableMvt.getKey()));

        if (this.compteDeb.equals(this.compteEnd)) {
            w = w.and(new Where(tableEcriture.getField("COMPTE_NUMERO"), "=", this.compteDeb));
        } else {
            w = w.and(new Where(tableEcriture.getField("COMPTE_NUMERO"), (Object) this.compteDeb, (Object) this.compteEnd));
        }

        if (this.lettrage == MODELETTREE) {
            Object o = null;
            w = w.and(new Where(tableEcriture.getField("LETTRAGE"), "<>", o));
            w = w.and(new Where(tableEcriture.getField("LETTRAGE"), "!=", ""));
        } else {
            if (this.lettrage == MODENONLETTREE) {
                Object o = null;
                Where w2 = new Where(tableEcriture.getField("LETTRAGE"), "=", o);
                w = w.and(w2.or(new Where(tableEcriture.getField("LETTRAGE"), "=", "")));
            }
        }

        w = w.and(new Where(tableEcriture.getField("ID_COMPTE_PCE"), "=", tableCompte.getField("ID")));
        w = w.and(new Where(tableEcriture.getField("ID_JOURNAL"), "!=", idJrnlExclude));
        if (listCompteSolde != null) {
            w = w.and(new Where(tableEcriture.getField("ID_COMPTE_PCE"), listCompteSolde).not());
        }

        sel.setWhere(w);

        String req = sel.asString() + " GROUP BY \"ECRITURE\".\"ID_COMPTE_PCE\", \"ECRITURE\".\"COMPTE_NUMERO\"";
        System.err.println(req);
        List<Object[]> l = (List) base.getDataSource().execute(req, new ArrayListHandler());
        Map<Integer, Long> map = new HashMap<Integer, Long>();

        int idCptFourn = ComptePCESQLElement.getId("401", "Fournisseurs");
        int idCptClient = ComptePCESQLElement.getId("411", "Clients");

        for (Object[] o : l) {

            long credit = 0;
            if (o[2] != null) {
                credit = Long.valueOf(o[2].toString());
            }

            long debit = 0;
            if (o[1] != null) {
                debit = Long.valueOf(o[1].toString());
            }

            int id = Integer.valueOf(o[0].toString());
            long solde = debit - credit;
            map.put(id, solde);
            if (o[3] != null) {
                String numero = o[3].toString();
                if (this.centralFourn && numero.startsWith("401")) {
                    Long lS = map.get(idCptFourn);
                    if (lS != null) {
                        lS += solde;
                    } else {
                        lS = new Long(solde);
                    }
                    map.put(idCptFourn, lS);
                }
                if (this.centralClient && numero.startsWith("411")) {
                    Long lS = map.get(idCptClient);
                    if (lS != null) {
                        lS += solde;
                    } else {
                        lS = new Long(solde);
                    }
                    map.put(idCptClient, lS);
                }
            }
        }

        return map;
    }

    @Override
    public String getTemplateId() {
        return TEMPLATE_ID;
    }

    public int getSize() {
        return size;
    }
}
