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

import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.generationDoc.SpreadSheetGeneratorCompta;
import org.openconcerto.erp.generationDoc.gestcomm.ReleveChequeEmisSheet;
import org.openconcerto.erp.generationEcritures.GenerationMvtReglementChequeFourn;
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

public class ChequeFournisseurSQLElement extends ChequeSQLElement {

    public ChequeFournisseurSQLElement() {
        super("CHEQUE_FOURNISSEUR", "un chéque fournisseur", "chéques fournisseurs");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_FOURNISSEUR");
        l.add("ETS");
        l.add("NUMERO");
        l.add("DATE");
        l.add("MONTANT");
        l.add("DATE_ACHAT");
        l.add("DATE_DECAISSE");
        l.add("DECAISSE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE_ACHAT");
        l.add("MONTANT");
        l.add("ID_FOURNISSEUR");
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
    public void print(List<Integer> listeCheque, boolean preview, Date d) {
        ReleveChequeEmisSheet sheet = new ReleveChequeEmisSheet(listeCheque);
        new SpreadSheetGeneratorCompta(sheet, "ReleveChequeEmis", false, true);
    }

    @Override
    public void handle(SQLRowAccessor rowCheque, Date d, String label) throws Exception {
        new GenerationMvtReglementChequeFourn(rowCheque.getForeignID("ID_MOUVEMENT"), rowCheque.getLong("MONTANT"), rowCheque.getID(), d);
    }

    @Override
    public SQLTableModelSourceOnline createDepositTableSource() {
        final List<String> l = new ArrayList<String>();
        l.add("MONTANT");
        // TYPE, NOM
        l.add("ID_FOURNISSEUR");
        l.add("ID_MOUVEMENT");

        l.add("ETS");
        l.add("NUMERO");
        l.add("DATE");

        l.add("DATE_ACHAT");
        l.add(getMinDateFieldName());
        l.add(getDoneFieldName());

        final ShowAs showAs = new ShowAs(getTable().getDBRoot());
        final SQLTable mvtT = getTable().getForeignTable("ID_MOUVEMENT");
        showAs.show(mvtT, "ID_PIECE");
        showAs.show(mvtT.getForeignTable("ID_PIECE"), "NOM");
        showAs.show(getTable().getForeignTable("ID_FOURNISSEUR"), "TYPE", "NOM");

        return this.createDepositTableSource(l, showAs, null);
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

                JDate dateAchat = new JDate(true);
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(dateAchat, c);

                c.gridy++;
                c.gridx = 0;
                JLabel labelFournisseurNom = new JLabel("Fournisseur ");
                this.add(labelFournisseurNom, c);

                final ElementComboBox nomFournisseur = new ElementComboBox();
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(nomFournisseur, c);

                this.addRequiredSQLObject(nomFournisseur, "ID_FOURNISSEUR");
                this.addRequiredSQLObject(this.textMontant, "MONTANT");
                this.addRequiredSQLObject(dateAchat, "DATE_ACHAT");
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".cheque.supplier";
    }
}
