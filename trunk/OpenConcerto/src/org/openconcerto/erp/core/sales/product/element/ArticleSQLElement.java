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
 
 package org.openconcerto.erp.core.sales.product.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.model.PrixHT;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.GestionDevise;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

// MAYBE mettre un champ marge??

public class ArticleSQLElement extends ComptaSQLConfElement {

    public ArticleSQLElement() {
        super("ARTICLE", "un article", "articles");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();

        l.add("CODE");
        l.add("NOM");
        l.add("POIDS");
        l.add("PA_HT");
        l.add("PV_HT");
        l.add("ID_TAXE");
        l.add("PV_TTC");
        l.add("VALEUR_METRIQUE_1");
        l.add("VALEUR_METRIQUE_2");
        l.add("VALEUR_METRIQUE_3");
        l.add("PRIX_METRIQUE_HA_1");
        l.add("PRIX_METRIQUE_VT_1");
        l.add("ID_STOCK");

        String val = DefaultNXProps.getInstance().getStringProperty("ArticleService");
        Boolean b = Boolean.valueOf(val);
        if (b != null && b.booleanValue()) {
            l.add("SERVICE");
        }
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        l.add("NOM");
        return l;
    }

    @Override
    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_STOCK");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {

        return new BaseSQLComponent(this) {

            private DeviseField textPVHT, textPVTTC, textPAHT;
            private JTextField textNom, textCode;
            private JTextField textPoids;
            private DocumentListener htDocListener, ttcDocListener;
            private PropertyChangeListener taxeListener;
            final ElementComboBox comboSelTaxe = new ElementComboBox(false);

            public void addViews() {

                this.textPVHT = new DeviseField();
                this.textPVTTC = new DeviseField();
                this.textPAHT = new DeviseField();
                this.textCode = new JTextField();
                this.textNom = new JTextField();
                this.textPoids = new JTextField();

                this.ttcDocListener = new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {
                        setTextHT();
                    }

                    public void insertUpdate(DocumentEvent e) {
                        setTextHT();
                    }

                    public void removeUpdate(DocumentEvent e) {
                        setTextHT();
                    }
                };

                this.htDocListener = new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {
                        setTextTTC();
                    }

                    public void insertUpdate(DocumentEvent e) {
                        setTextTTC();
                    }

                    public void removeUpdate(DocumentEvent e) {
                        setTextTTC();
                    }

                };

                this.taxeListener = new PropertyChangeListener() {

                    public void propertyChange(PropertyChangeEvent evt) {

                        if (textPVHT.getText().trim().length() > 0) {
                            setTextTTC();
                        } else {
                            setTextHT();
                        }
                    }
                };

                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Code
                this.add(new JLabel(getLabelFor("CODE")), c);
                c.gridx++;
                this.add(this.textCode, c);

                // Nom
                c.gridx++;
                this.add(new JLabel(getLabelFor("NOM")), c);
                c.gridx++;
                this.add(this.textNom, c);

                // Stock
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridheight = 2;
                JPanel panelStock = new JPanel();
                this.addView("ID_STOCK", REQ + ";" + DEC + ";" + SEP);
                ElementSQLObject eltStock = (ElementSQLObject) this.getView("ID_STOCK");
                panelStock.add(eltStock.getComp());
                panelStock.setBorder(BorderFactory.createTitledBorder(getLabelFor("ID_STOCK")));
                this.add(panelStock, c);

                // PA
                c.gridx = 0;
                c.gridy++;
                c.gridwidth = 1;
                c.gridheight = 1;
                this.add(new JLabel(getLabelFor("PA_HT")), c);
                c.gridx++;
                this.add(this.textPAHT, c);

                // Poids
                c.gridx++;
                JLabel labelPds = new JLabel(getLabelFor("POIDS"));
                this.add(labelPds, c);
                c.gridx++;
                labelPds.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(this.textPoids, c);

                // PV HT
                c.gridx = 0;
                c.gridy++;
                this.add(new JLabel(getLabelFor("PV_HT")), c);
                c.gridx++;
                this.add(this.textPVHT, c);

                // Taxe
                c.gridx++;
                JLabel labelTaxe = new JLabel(getLabelFor("ID_TAXE"));
                this.add(labelTaxe, c);
                c.gridx++;
                labelTaxe.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(this.comboSelTaxe, c);

                // PV_TTC
                c.gridx++;
                this.add(new JLabel(getLabelFor("PV_TTC")), c);
                c.gridx++;
                this.add(this.textPVTTC, c);

                c.gridy++;
                c.weighty = 1;
                JPanel spacer = new JPanel();
                c.fill = GridBagConstraints.BOTH;
                this.add(spacer, c);

                this.addRequiredSQLObject(this.textNom, "NOM");
                this.addRequiredSQLObject(this.textCode, "CODE");

                this.addRequiredSQLObject(this.textPAHT, "PA_HT");
                this.addSQLObject(this.textPoids, "POIDS");

                this.addRequiredSQLObject(this.textPVHT, "PV_HT");
                this.addRequiredSQLObject(this.comboSelTaxe, "ID_TAXE");
                this.addRequiredSQLObject(this.textPVTTC, "PV_TTC");

                this.textPVHT.getDocument().addDocumentListener(this.htDocListener);
                this.textPVTTC.getDocument().addDocumentListener(this.ttcDocListener);
                this.comboSelTaxe.addValueListener(this.taxeListener);
            }

            private void setTextHT() {
                this.textPVHT.getDocument().removeDocumentListener(this.htDocListener);

                String textTTC = this.textPVTTC.getText().trim();
                PrixTTC ttc = new PrixTTC(GestionDevise.parseLongCurrency(textTTC));
                int id = this.comboSelTaxe.getSelectedId();
                if (id > 1) {
                    SQLRow ligneTaxe = getTable().getBase().getTable("TAXE").getRow(id);
                    float taux = (ligneTaxe.getFloat("TAUX")) / 100.0F;
                    this.textPVHT.setText(GestionDevise.currencyToString(ttc.calculLongHT(taux)));
                }
                this.textPVHT.getDocument().addDocumentListener(this.htDocListener);
            }

            private void setTextTTC() {
                this.textPVTTC.getDocument().removeDocumentListener(this.ttcDocListener);

                String textHT = this.textPVHT.getText().trim();
                PrixHT ht = new PrixHT(GestionDevise.parseLongCurrency(textHT));
                int id = this.comboSelTaxe.getSelectedId();
                if (id > 1) {
                    SQLRow ligneTaxe = getTable().getBase().getTable("TAXE").getRow(id);
                    float taux = (ligneTaxe.getFloat("TAUX")) / 100.0F;
                    this.textPVTTC.setText(GestionDevise.currencyToString(ht.calculLongTTC(taux)));
                }
                this.textPVTTC.getDocument().addDocumentListener(this.ttcDocListener);
            }

            @Override
            protected SQLRowValues createDefaults() {
                SQLRowValues rowVals = new SQLRowValues(getTable());
                rowVals.put("ID_MODE_VENTE_ARTICLE", ReferenceArticleSQLElement.A_LA_PIECE);
                return rowVals;
            }
        };
    }
}
