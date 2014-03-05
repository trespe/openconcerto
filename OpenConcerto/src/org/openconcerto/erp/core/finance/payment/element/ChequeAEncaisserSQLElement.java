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
 
 package org.openconcerto.erp.core.finance.payment.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.generationDoc.gestcomm.ReleveChequeSheet;
import org.openconcerto.erp.generationEcritures.GenerationMvtReglementChequeClient;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.ShowAs;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class ChequeAEncaisserSQLElement extends ChequeSQLElement {

    public ChequeAEncaisserSQLElement() {
        super("CHEQUE_A_ENCAISSER", "un chéque client", "chéques clients");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();

        l.add("ID_MOUVEMENT");
        l.add("ID_CLIENT");
        l.add("DATE");
        l.add("ETS");
        l.add("NUMERO");
        l.add("DATE_VENTE");
        l.add("DATE_DEPOT");
        l.add("MONTANT");
        l.add("REG_COMPTA");
        l.add("ENCAISSE");
        return l;
    }

    @Override
    public String getDoneFieldName() {
        return "ENCAISSE";
    }

    @Override
    public String getDateFieldName() {
        return "DATE_DEPOT";
    }

    @Override
    public String getMinDateFieldName() {
        return "DATE_MIN_DEPOT";
    }

    @Override
    public void print(final List<Integer> listeCheque, final boolean preview, final Date d) {
        ReleveChequeSheet sheet = new ReleveChequeSheet(listeCheque, d, preview);
        sheet.createDocumentAsynchronous();
        sheet.showPrintAndExportAsynchronous(true, false, true);
    }

    @Override
    public void handle(SQLRowAccessor rowCheque, Date d, String label) throws Exception {
        GenerationMvtReglementChequeClient gen = new GenerationMvtReglementChequeClient(rowCheque.getForeignID("ID_MOUVEMENT"), rowCheque.getLong("MONTANT"), d, rowCheque.getID(), label);
        gen.genere();
    }

    @Override
    public SQLTableModelSourceOnline createDepositTableSource() {
        final List<String> l = new ArrayList<String>();
        l.add("ETS");
        l.add("NUMERO");
        l.add("DATE");
        l.add("ID_MOUVEMENT");
        l.add("DATE_VENTE");
        l.add(getMinDateFieldName());
        l.add("ID_CLIENT");
        l.add("MONTANT");

        final ShowAs showAs = new ShowAs(getTable().getDBRoot());

        final SQLTable mvtT = getTable().getForeignTable("ID_MOUVEMENT");
        showAs.show(mvtT, "ID_PIECE");
        showAs.show(mvtT.getForeignTable("ID_PIECE"), "NOM");

        final SQLTable clientERP = getTable().getForeignTable("ID_CLIENT");
        {
            showAs.show(clientERP, "NOM");
        }

        return this.createDepositTableSource(l, showAs, new Where(getTable().getField("REG_COMPTA"), "=", Boolean.FALSE));
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE_VENTE");
        l.add("MONTANT");
        l.add("ID_CLIENT");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private DeviseField textMontant = new DeviseField();

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Montant
                JLabel labelMontant = new JLabel("Montant ");

                this.add(labelMontant, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textMontant, c);

                // Date
                JLabel labelDate = new JLabel("Date ");
                c.weightx = 0;
                c.gridx++;
                labelDate.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelDate, c);

                JDate dateVente = new JDate(true);
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(dateVente, c);

                // Date echeance
                JLabel labelDateEcheance = new JLabel("Date d'échéance");
                c.weightx = 0;
                c.gridy++;
                c.gridwidth = 1;
                c.gridx = 0;
                labelDate.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelDateEcheance, c);

                JDate dateEcheance = new JDate(true);
                c.gridx++;
                c.gridwidth = 1;
                this.add(dateEcheance, c);

                c.gridy++;
                c.gridx = 0;
                JLabel labelClientNom = new JLabel("Client ");
                this.add(labelClientNom, c);

                ElementComboBox nomClient = new ElementComboBox();
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(nomClient, c);

                this.addRequiredSQLObject(nomClient, "ID_CLIENT");
                this.addRequiredSQLObject(this.textMontant, "MONTANT");
                this.addRequiredSQLObject(dateVente, "DATE_VENTE");
                this.addRequiredSQLObject(dateEcheance, "DATE_MIN_DEPOT");
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".cheque";
    }
}
