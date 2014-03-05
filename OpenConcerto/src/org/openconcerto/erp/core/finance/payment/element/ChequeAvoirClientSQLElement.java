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
import org.openconcerto.erp.generationEcritures.GenerationMvtReglementAvoirChequeClient;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.ShowAs;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
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

public class ChequeAvoirClientSQLElement extends ChequeSQLElement {

    public ChequeAvoirClientSQLElement() {
        super("CHEQUE_AVOIR_CLIENT", "un chéque de remboursement", "chéques remboursements");
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();

        l.add("MONTANT");
        l.add("ID_CLIENT");
        l.add("DATE_AVOIR");
        l.add("DATE_DECAISSE");
        l.add("DECAISSE");
        return l;
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE_AVOIR");
        l.add("MONTANT");
        l.add("ID_CLIENT");
        return l;
    }

    @Override
    public String getDoneFieldName() {
        return "DECAISSE";
    }

    @Override
    public String getDateFieldName() {
        return "DATE_DECAISSE";
    }

    @Override
    public String getMinDateFieldName() {
        return "DATE_MIN_DECAISSE";
    }

    @Override
    public SQLTableModelSourceOnline createDepositTableSource() {
        final List<String> l = new ArrayList<String>();
        l.add("MONTANT");
        // NOM
        l.add("ID_CLIENT");
        l.add("ID_MOUVEMENT");
        l.add("DATE_AVOIR");
        l.add(getMinDateFieldName());
        l.add(getDoneFieldName());

        final ShowAs showAs = new ShowAs(getTable().getDBRoot());

        final SQLTable mvtT = getTable().getForeignTable("ID_MOUVEMENT");
        showAs.show(mvtT, "ID_PIECE");
        showAs.show(mvtT.getForeignTable("ID_PIECE"), "NOM");

        final SQLTable clientERP = getTable().getForeignTable("ID_CLIENT");
        {
            showAs.show(clientERP, "NOM");
        }

        return this.createDepositTableSource(l, showAs, null);
    }

    @Override
    public void print(List<Integer> rows, boolean preview, Date d) {
    }

    @Override
    public void handle(final SQLRowAccessor rowCheque, final Date d, String label) throws Exception {
        GenerationMvtReglementAvoirChequeClient gen = new GenerationMvtReglementAvoirChequeClient(rowCheque.getForeignID("ID_MOUVEMENT"), rowCheque.getLong("MONTANT"), d, rowCheque.getID());
        gen.genere();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private final DeviseField textMontant = new DeviseField();

            @Override
            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Montant
                final JLabel labelMontant = new JLabel("Montant ");

                this.add(labelMontant, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textMontant, c);

                // Date
                final JLabel labelDate = new JLabel("Date ");
                c.weightx = 0;
                c.gridx++;
                labelDate.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelDate, c);

                final JDate dateAvoir = new JDate(true);
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(dateAvoir, c);

                c.gridy++;
                c.gridx = 0;
                final JLabel labelClientNom = new JLabel("Client ");
                this.add(labelClientNom, c);

                final ElementComboBox nomClient = new ElementComboBox();
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(nomClient, c);

                this.addRequiredSQLObject(nomClient, "ID_CLIENT");
                this.addRequiredSQLObject(this.textMontant, "MONTANT");
                this.addRequiredSQLObject(dateAvoir, "DATE_AVOIR");
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".cheque.due";
    }
}
