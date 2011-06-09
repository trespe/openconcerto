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
import org.openconcerto.erp.generationDoc.SheetInterface;
import org.openconcerto.erp.generationDoc.SheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.Tuple2;

import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class BalanceSheet extends SheetInterface {

    private static int debutFill, endFill;
    private final static SQLTable tableEcriture = base.getTable("ECRITURE");
    private final static SQLTable tableCompte = base.getTable("COMPTE_PCE");
    private boolean centralClient, centralFourn;
    private final static DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
    private final static DateFormat dateFormatEcr = DateFormat.getDateInstance(DateFormat.SHORT);

    private Date dateDu, dateAu;
    private String compteDeb, compteEnd;

    public static void setSize(int debut, int fin) {
        debutFill = debut;
        endFill = fin;
    }

    static {
        setSize(7, 69);

    }

    private static final Tuple2<String, String> tuple = Tuple2.create("LocationBalance", "Balance");

    public static Tuple2<String, String> getTuple2Location() {
        return tuple;
    }

    public BalanceSheet(Date du, Date au, String compteDeb, String compteEnd, boolean centralClient, boolean centralFourn) {
        super();

        Calendar cal = Calendar.getInstance();
        cal.setTime(au);

        // Initialisation des Valeur
        this.nbRowsPerPage = 72;
        this.printer = PrinterNXProps.getInstance().getStringProperty("BalancePrinter");
        this.modele = "Balance.ods";
        final String locationForTuple = SheetXml.getLocationForTuple(tuple, false);
        System.err.println("Emplacement balance :::: " + locationForTuple);
        this.locationOO = locationForTuple + File.separator + cal.get(Calendar.YEAR);
        this.locationPDF = SheetXml.getLocationForTuple(tuple, true) + File.separator + cal.get(Calendar.YEAR);
        this.dateAu = au;
        this.dateDu = du;
        this.compteDeb = compteDeb;
        this.compteEnd = compteEnd;
        this.centralClient = centralClient;
        this.centralFourn = centralFourn;
        createMap();
    }

    private void makeEntete(int rowDeb) {
        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        this.mCell.put("A" + rowDeb, rowSociete.getObject("NOM"));
        this.mCell.put("D" + rowDeb, "Edition du " + dateFormat.format(new Date()));
        // this.mCell.put("D" + (rowDeb + 2), "Grand livre");
        System.err.println("MAKE ENTETE");
    }

    private void makePiedPage(int row) {

        this.mCell.put("C" + row, "Période du " + dateFormatEcr.format(this.dateDu) + " au " + dateFormatEcr.format(this.dateAu));
        this.mCell.put("B" + row, "Du compte " + this.compteDeb + " à " + this.compteEnd);
    }

    private void makeSousTotalClasse(int row, long debit, long credit, String classe) {
        this.mCell.put("A" + row, "Total classe " + classe);
        this.mCell.put("B" + row, "");

        this.mCell.put("C" + row, new Double(GestionDevise.currencyToString(debit, false)));
        this.mCell.put("D" + row, new Double(GestionDevise.currencyToString(credit, false)));
        this.mCell.put("E" + row, new Double(GestionDevise.currencyToString(debit - credit, false)));

        this.mapStyleRow.put(new Integer(row), "Titre 1");
    }

    protected void createMap() {

        this.mapReplace = new HashMap();
        this.mCell = new HashMap();
        this.mapStyleRow = new HashMap();

        SQLSelect sel = new SQLSelect(base);
        sel.addSelect(tableCompte.getField("ID"));
        sel.addSelect(tableEcriture.getField("DEBIT"), "SUM");
        sel.addSelect(tableEcriture.getField("CREDIT"), "SUM");

        Where w = (new Where(tableEcriture.getField("DATE"), this.dateDu, this.dateAu));

        if (compteDeb.equals(this.compteEnd)) {
            w = w.and(new Where(tableCompte.getField("NUMERO"), "=", this.compteDeb));
        } else {
            w = w.and(new Where(tableCompte.getField("NUMERO"), (Object) this.compteDeb, (Object) this.compteEnd));
        }
        sel.setWhere(w);

        String req = sel.asString() + " AND \"ECRITURE\".\"ID_COMPTE_PCE\" = \"COMPTE_PCE\".\"ID\" GROUP BY  \"COMPTE_PCE\".\"NUMERO\", \"COMPTE_PCE\".\"ID\" ORDER BY \"COMPTE_PCE\".\"NUMERO\"";

        System.err.println(req);

        List l = (List) base.getDataSource().execute(req, new ArrayListHandler());

        int posLine = 1;
        int firstLine = 1;
        System.err.println("START CREATE Grand livre, NB ecritures  " + l.size());
        this.nbPage = 0;
        long totalDebit, totalCredit, sousTotalDebit, sousTotalCredit;

        totalDebit = 0;
        totalCredit = 0;
        sousTotalDebit = 0;
        sousTotalCredit = 0;

        long totalDebitClient = 0;
        long totalCreditClient = 0;

        long totalDebitFourn = 0;
        long totalCreditFourn = 0;

        String numCptClient = "411";
        String nomCptClient = "Clients";
        String numCptFourn = "401";
        String nomCptFourn = "Fournisseurs";
        boolean addedLine = false;
        int j = 0;
        String classe = "";
        for (int i = 0; i < l.size();) {

            System.err.println("START NEW PAGE; POS : " + posLine);

            /***************************************************************************************
             * ENTETE
             **************************************************************************************/
            makeEntete(posLine);
            posLine += debutFill - 1;

            /***************************************************************************************
             * CONTENU
             **************************************************************************************/
            for (j = 0; (j < endFill - debutFill + 1) && i < l.size(); j++) {
                Object[] o = (Object[]) l.get(i);
                int idCpt = Integer.parseInt(o[0].toString());
                SQLRow rowCpt = tableCompte.getRow(idCpt);

                String numeroCpt = rowCpt.getString("NUMERO");
                String nomCpt = rowCpt.getString("NOM");
                // Changement de classe de compte
                if (classe.trim().length() != 0 && numeroCpt.trim().length() > 0 && !classe.trim().equalsIgnoreCase(numeroCpt.substring(0, 1))) {

                    makeSousTotalClasse(posLine, sousTotalDebit, sousTotalCredit, classe);

                    sousTotalCredit = 0;
                    sousTotalDebit = 0;
                    classe = numeroCpt.substring(0, 1);

                } else {
                    if (classe.trim().length() == 0 && numeroCpt.trim().length() > 0) {
                        classe = numeroCpt.substring(0, 1);
                    }

                    long deb = new Double(o[1].toString()).longValue();
                    long cred = new Double(o[2].toString()).longValue();

                    totalCredit += cred;
                    sousTotalCredit += cred;
                    totalDebit += deb;
                    sousTotalDebit += deb;

                    // Centralisation compte client
                    if (this.centralClient && (numeroCpt.equalsIgnoreCase("411") || numeroCpt.startsWith("411"))) {
                        totalDebitClient += deb;
                        totalCreditClient += cred;
                        deb = totalDebitClient;
                        cred = totalCreditClient;
                    }

                    // Centralisation compte fournisseur
                    if (this.centralFourn && (numeroCpt.equalsIgnoreCase("401") || numeroCpt.startsWith("401"))) {
                        totalDebitFourn += deb;
                        totalCreditFourn += cred;
                        deb = totalDebitFourn;
                        cred = totalCreditFourn;
                    }

                    if (this.centralClient && !numeroCpt.equalsIgnoreCase("411") && numeroCpt.startsWith("411")) {
                        if (addedLine || !this.centralFourn) {
                            posLine--;
                            j--;
                        } else {
                            addedLine = true;
                        }
                        this.mCell.put("A" + posLine, numCptClient);
                        this.mCell.put("B" + posLine, nomCptClient);
                    } else {
                        if (this.centralFourn && !numeroCpt.equalsIgnoreCase("401") && numeroCpt.startsWith("401")) {

                            posLine--;
                            j--;

                            this.mCell.put("A" + posLine, numCptFourn);
                            this.mCell.put("B" + posLine, nomCptFourn);
                        } else {
                            this.mCell.put("A" + posLine, numeroCpt);
                            this.mCell.put("B" + posLine, nomCpt);
                        }
                    }

                    this.mCell.put("C" + posLine, new Double(GestionDevise.currencyToString(deb, false)));
                    this.mCell.put("D" + posLine, new Double(GestionDevise.currencyToString(cred, false)));
                    this.mCell.put("E" + posLine, new Double(GestionDevise.currencyToString(deb - cred, false)));

                    this.mapStyleRow.put(new Integer(posLine), "Normal");
                    i++;
                }

                posLine++;
            }

            if (i >= l.size() && j < endFill - debutFill + 1) {

                makeSousTotalClasse(posLine, sousTotalDebit, sousTotalCredit, classe);
            }

            posLine = firstLine + endFill;
            /*
             * if (this.mapStyleRow.get(new Integer(posLine - 1)) != null) {
             * this.mapStyleRow.put(new Integer(posLine - 1), "Titre 2"); }
             */

            // Total
            this.mCell.put("C" + posLine, ((totalDebit == 0) ? new Double(0) : new Double(GestionDevise.currencyToString(totalDebit, false))));
            this.mCell.put("D" + posLine, ((totalCredit == 0) ? new Double(0) : new Double(GestionDevise.currencyToString(totalCredit, false))));
            this.mCell.put("E" + posLine, (totalDebit - totalCredit == 0) ? new Double(0) : new Double(GestionDevise.currencyToString(totalDebit - totalCredit, false)));

            posLine += 2;

            // bas de page
            makePiedPage(posLine);

            posLine++;
            firstLine = posLine;
            this.nbPage++;

            if (i >= l.size() && j >= (endFill - debutFill + 1)) {

                makeEntete(posLine);
                posLine += debutFill - 1;

                makeSousTotalClasse(posLine, sousTotalDebit, sousTotalCredit, classe);

                this.nbPage++;
            }

        }

        // on conserve la page d'origine du model
        if (this.nbPage > 0) {
            this.nbPage--;
        }
    }
}
