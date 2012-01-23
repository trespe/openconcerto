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
 
 package org.openconcerto.erp.core.sales.invoice.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class SaisieVenteFactureItemSQLElement extends ComptaSQLConfElement {

    public SaisieVenteFactureItemSQLElement() {
        super("SAISIE_VENTE_FACTURE_ELEMENT", "un article facturé", "articles facturés");
    }

    public SaisieVenteFactureItemSQLElement(String tableName, String singular, String plural) {
        super(tableName, singular, plural);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.BaseSQLElement#getComboFields()
     */
    protected List<String> getComboFields() {
        List<String> l = new ArrayList<String>();
        l.add("CODE");
        l.add("NOM");
        l.add("PV_HT");
        l.add("ID_TAXE");
        l.add("POIDS");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.BaseSQLElement#getListFields()
     */
    protected List<String> getListFields() {
        List<String> l = new ArrayList<String>();
        l.add("ID_SAISIE_VENTE_FACTURE");
        l.add("CODE");
        l.add("NOM");
        String articleAdvanced = DefaultNXProps.getInstance().getStringProperty("ArticleModeVenteAvance");
        Boolean bArticleAdvanced = Boolean.valueOf(articleAdvanced);
        if (bArticleAdvanced) {
            l.add("PRIX_METRIQUE_VT_1");
            l.add("ID_MODE_VENTE_ARTICLE");
        }
        l.add("PA_HT");
        l.add("PV_HT");

        l.add("QTE");
        l.add("T_PV_HT");
        l.add("T_PV_TTC");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Code
                JLabel labelCode = new JLabel(getLabelFor("CODE"));
                c.weightx = 0;
                this.add(labelCode, c);

                JTextField textCode = new JTextField();
                c.weightx = 1;
                c.gridx++;
                this.add(textCode, c);

                // Libelle
                JLabel labelNom = new JLabel(getLabelFor("NOM"));
                c.weightx = 0;
                c.gridx++;
                this.add(labelNom, c);

                JTextField textNom = new JTextField();
                c.weightx = 1;
                c.gridx++;
                this.add(textNom, c);

                // Montant HT
                JLabel labelMontantHT = new JLabel(getLabelFor("PV_HT"));
                c.weightx = 0;
                c.gridy++;
                c.gridx = 0;
                this.add(labelMontantHT, c);

                DeviseField textMontantHT = new DeviseField();
                c.weightx = 1;
                c.gridx++;
                this.add(textMontantHT, c);

                // Taxe
                c.gridx++;
                c.weightx = 0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                ElementComboBox comboTaxe = new ElementComboBox();
                this.add(comboTaxe, c);

                // Poids
                JLabel labelPoids = new JLabel(getLabelFor("POIDS"));
                c.weightx = 0;
                c.gridy++;
                c.gridx = 0;
                c.gridwidth = 1;
                this.add(labelPoids, c);

                JTextField textPoids = new JTextField();
                c.weightx = 1;
                c.gridx++;
                this.add(textPoids, c);

                // Quantité
                JLabel labelQte = new JLabel(getLabelFor("QTE"));
                c.weightx = 0;
                c.gridx++;
                this.add(labelQte, c);

                JTextField textQte = new JTextField();
                c.weightx = 1;
                c.gridx++;
                this.add(textQte, c);

                this.addRequiredSQLObject(textCode, "CODE");
                this.addRequiredSQLObject(textNom, "NOM");
                this.addRequiredSQLObject(textMontantHT, "PV_HT");
                this.addSQLObject(textPoids, "POIDS");
                this.addSQLObject(textQte, "QTE");
                this.addRequiredSQLObject(comboTaxe, "ID_TAXE");
            }
        };
    }
}
