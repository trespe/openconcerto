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
 
 package org.openconcerto.erp.core.humanresources.employe.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class CommercialSQLElement extends ComptaSQLConfElement {

    public CommercialSQLElement() {
        super("COMMERCIAL", "un commercial", "commerciaux");
    }

    public CommercialSQLElement(String tableName, String singular, String plural) {
        super(tableName, singular, plural);
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("PRENOM");
        l.add("FONCTION");
        l.add("TEL_STANDARD");
        l.add("TEL_DIRECT");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("PRENOM");
        l.add("FONCTION");

        return l;
    }

    @Override
    public CollectionMap<String, String> getShowAs() {
        final CollectionMap<String, String> res = new CollectionMap<String, String>();
        res.put(null, "NOM");
        return res;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            JTextField textInitiale;
            JTextField textPrenom, textNom;

            DocumentListener listener = new SimpleDocumentListener() {

                @Override
                public void update(DocumentEvent e) {
                    updateInititale();
                }
            };

            private void updateInititale() {
                String s = "";
                if (this.textPrenom.getText().trim().length() > 0) {
                    s += this.textPrenom.getText().trim().charAt(0);
                }
                if (this.textNom.getText().trim().length() > 0) {
                    s += this.textNom.getText().trim().charAt(0);
                }
                this.textInitiale.setText(s);
            }

            public void addViews() {
                this.setLayout(new GridBagLayout());

                GridBagConstraints c = new DefaultGridBagConstraints();

                // Titre personnel
                final JLabel label = new JLabel(getLabelFor("ID_TITRE_PERSONNEL"));
                label.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(label, c);
                ElementComboBox selTitre = new ElementComboBox(false, 6);

                c.gridx++;
                c.fill = GridBagConstraints.NONE;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(selTitre, c);

                // Nom
                c.gridx = 0;
                c.gridy++;
                c.gridwidth = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                final JLabel label2 = new JLabel(getLabelFor("NOM"));
                label2.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(label2, c);
                this.textNom = new JTextField(21);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textNom, c);
                this.textNom.getDocument().addDocumentListener(this.listener);

                // Prenom
                c.gridx++;
                c.weightx = 0;
                final JLabel label3 = new JLabel(getLabelFor("PRENOM"));
                label3.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(label3, c);
                this.textPrenom = new JTextField(21);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textPrenom, c);
                this.textPrenom.getDocument().addDocumentListener(this.listener);

                // // Initiales
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                final JLabel label4 = new JLabel("Initiales");
                label4.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(label4, c);
                c.gridx++;
                c.weightx = 1;
                c.fill = GridBagConstraints.NONE;
                this.textInitiale = new JTextField(2);
                this.textInitiale.setEditable(false);
                this.add(this.textInitiale, c);
                c.fill = GridBagConstraints.HORIZONTAL;

                // Fonction
                c.gridx++;
                c.weightx = 0;
                final JLabel label5 = new JLabel(getLabelFor("FONCTION"));
                label5.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(label5, c);
                JTextField textFonction = new JTextField();
                c.gridx++;
                c.weightx = 1;
                this.add(textFonction, c);

                // Tel Standard
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                final JLabel label6 = new JLabel(getLabelFor("TEL_STANDARD"));
                label6.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(label6, c);
                c.gridx++;
                c.weightx = 1;
                JTextField textTel = new JTextField();
                this.add(textTel, c);

                // Tel direct
                c.gridx++;
                c.weightx = 0;
                final JLabel label7 = new JLabel(getLabelFor("TEL_DIRECT"));
                label7.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(label7, c);
                JTextField textTelD = new JTextField();
                c.gridx++;
                c.weightx = 1;
                this.add(textTelD, c);

                // Tel Mobile
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                final JLabel label8 = new JLabel(getLabelFor("TEL_MOBILE"));
                label8.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(label8, c);
                c.gridx++;
                c.weightx = 1;
                JTextField textTelM = new JTextField();
                this.add(textTelM, c);

                // Tel Perso
                c.gridx++;
                c.weightx = 0;
                final JLabel label9 = new JLabel(getLabelFor("TEL_PERSONEL"));
                label9.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(label9, c);
                JTextField textTelP = new JTextField();
                c.gridx++;
                c.weightx = 1;
                this.add(textTelP, c);

                // Tel Fax
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                final JLabel label10 = new JLabel(getLabelFor("FAX"));
                label10.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(label10, c);
                c.gridx++;
                c.weightx = 1;
                JTextField textFax = new JTextField();
                this.add(textFax, c);

                // Tel Email
                c.gridx++;
                c.weightx = 0;
                final JLabel label11 = new JLabel(getLabelFor("EMAIL"));
                label11.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(label11, c);
                JTextField textMail = new JTextField();
                c.gridx++;
                c.weightx = 1;
                this.add(textMail, c);

                // Modules
                c.gridx = 0;
                c.gridy++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                final JPanel addP = new JPanel();
                this.setAdditionalFieldsPanel(new FormLayouter(addP, 1));
                this.add(addP, c);

                // User

                c.gridx = 0;
                c.gridy++;
                c.gridwidth = 1;
                c.weightx = 0;
                final JLabel labelUser = new JLabel(getLabelFor("ID_USER_COMMON"));
                labelUser.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelUser, c);
                c.gridx++;
                c.weightx = 1;

                c.gridwidth = GridBagConstraints.REMAINDER;
                ElementComboBox comboUser = new ElementComboBox(true, 25);
                this.add(comboUser, c);

                c.weighty = 1;
                c.gridy++;
                this.add(new JPanel(), c);

                this.addRequiredSQLObject(selTitre, "ID_TITRE_PERSONNEL");
                selTitre.setButtonsVisible(false);
                this.addRequiredSQLObject(this.textNom, "NOM");
                this.addRequiredSQLObject(this.textPrenom, "PRENOM");

                this.addSQLObject(textFonction, "FONCTION");

                this.addSQLObject(comboUser, "ID_USER_COMMON");

                this.addSQLObject(textTel, "TEL_STANDARD");
                this.addSQLObject(textTelD, "TEL_DIRECT");

                this.addSQLObject(textTelM, "TEL_MOBILE");
                this.addSQLObject(textTelP, "TEL_PERSONEL");

                this.addSQLObject(textFax, "FAX");
                this.addSQLObject(textMail, "EMAIL");

                // Locks
                DefaultGridBagConstraints.lockMinimumSize(this.textInitiale);
                DefaultGridBagConstraints.lockMinimumSize(selTitre);

            }
        };
    }
}
