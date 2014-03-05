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
 
 /*
 * Créé le 30 mars 2012
 */
package org.openconcerto.erp.core.humanresources.employe.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.humanresources.employe.panel.ObjectifEditPanel;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class CommercialSQLComponent extends BaseSQLComponent {

    private JTextField textInitiale;
    private JTextField textPrenom, textNom;
    private ObjectifEditPanel objectifPanel = new ObjectifEditPanel(getSelectedID());

    DocumentListener listener = new SimpleDocumentListener() {

        @Override
        public void update(DocumentEvent e) {
            updateInititale();
        }
    };

    public CommercialSQLComponent(SQLElement elt) {
        super(elt);
    }

    @Override
    public void select(SQLRowAccessor r) {
        super.select(r);
        if (r == null) {
            objectifPanel.setIdCommercial(-1);
        } else {
            objectifPanel.setIdCommercial(r.getID());
        }
    }

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

    private final JTabbedPane pane = new JTabbedPane();

    public void addViews() {
        this.setLayout(new GridBagLayout());

        GridBagConstraints c = new DefaultGridBagConstraints();
        JPanel panelInfos = new JPanel(new GridBagLayout());
        // Titre personnel
        final JLabel label = new JLabel(getLabelFor("ID_TITRE_PERSONNEL"));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        panelInfos.add(label, c);
        ElementComboBox selTitre = new ElementComboBox(false, 6);

        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        panelInfos.add(selTitre, c);

        // Nom
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        final JLabel label2 = new JLabel(getLabelFor("NOM"));
        label2.setHorizontalAlignment(SwingConstants.RIGHT);
        panelInfos.add(label2, c);
        this.textNom = new JTextField(21);
        c.gridx++;
        c.weightx = 1;
        panelInfos.add(this.textNom, c);
        this.textNom.getDocument().addDocumentListener(this.listener);

        // Prenom
        c.gridx++;
        c.weightx = 0;
        final JLabel label3 = new JLabel(getLabelFor("PRENOM"));
        label3.setHorizontalAlignment(SwingConstants.RIGHT);
        panelInfos.add(label3, c);
        this.textPrenom = new JTextField(21);
        c.gridx++;
        c.weightx = 1;
        panelInfos.add(this.textPrenom, c);
        this.textPrenom.getDocument().addDocumentListener(this.listener);

        // // Initiales
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        final JLabel label4 = new JLabel("Initiales");
        label4.setHorizontalAlignment(SwingConstants.RIGHT);
        panelInfos.add(label4, c);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.textInitiale = new JTextField(4);
        this.textInitiale.setEditable(false);
        panelInfos.add(this.textInitiale, c);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Fonction
        c.gridx++;
        c.weightx = 0;
        final JLabel label5 = new JLabel(getLabelFor("FONCTION"));
        label5.setHorizontalAlignment(SwingConstants.RIGHT);
        panelInfos.add(label5, c);
        JTextField textFonction = new JTextField();
        c.gridx++;
        c.weightx = 1;
        panelInfos.add(textFonction, c);

        // Tel Standard
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        final JLabel label6 = new JLabel(getLabelFor("TEL_STANDARD"));
        label6.setHorizontalAlignment(SwingConstants.RIGHT);
        panelInfos.add(label6, c);
        c.gridx++;
        c.weightx = 1;
        JTextField textTel = new JTextField();
        panelInfos.add(textTel, c);

        // Tel direct
        c.gridx++;
        c.weightx = 0;
        final JLabel label7 = new JLabel(getLabelFor("TEL_DIRECT"));
        label7.setHorizontalAlignment(SwingConstants.RIGHT);
        panelInfos.add(label7, c);
        JTextField textTelD = new JTextField();
        c.gridx++;
        c.weightx = 1;
        panelInfos.add(textTelD, c);

        // Tel Mobile
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        final JLabel label8 = new JLabel(getLabelFor("TEL_MOBILE"));
        label8.setHorizontalAlignment(SwingConstants.RIGHT);
        panelInfos.add(label8, c);
        c.gridx++;
        c.weightx = 1;
        JTextField textTelM = new JTextField();
        panelInfos.add(textTelM, c);

        // Tel Perso
        c.gridx++;
        c.weightx = 0;
        final JLabel label9 = new JLabel(getLabelFor("TEL_PERSONEL"));
        label9.setHorizontalAlignment(SwingConstants.RIGHT);
        panelInfos.add(label9, c);
        JTextField textTelP = new JTextField();
        c.gridx++;
        c.weightx = 1;
        panelInfos.add(textTelP, c);

        // Tel Fax
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        final JLabel label10 = new JLabel(getLabelFor("FAX"));
        label10.setHorizontalAlignment(SwingConstants.RIGHT);
        panelInfos.add(label10, c);
        c.gridx++;
        c.weightx = 1;
        JTextField textFax = new JTextField();
        panelInfos.add(textFax, c);

        // Tel Email
        c.gridx++;
        c.weightx = 0;
        final JLabel label11 = new JLabel(getLabelFor("EMAIL"));
        label11.setHorizontalAlignment(SwingConstants.RIGHT);
        panelInfos.add(label11, c);
        JTextField textMail = new JTextField();
        c.gridx++;
        c.weightx = 1;
        panelInfos.add(textMail, c);

        // Modules
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel addP = ComptaSQLConfElement.createAdditionalPanel();
        addP.setOpaque(false);
        this.setAdditionalFieldsPanel(new FormLayouter(addP, 1));
        panelInfos.add(addP, c);

        // User

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        final JLabel labelUser = new JLabel(getLabelFor("ID_USER_COMMON"));
        labelUser.setHorizontalAlignment(SwingConstants.RIGHT);
        panelInfos.add(labelUser, c);
        c.gridx++;
        c.weightx = 1;

        c.gridwidth = GridBagConstraints.REMAINDER;
        ElementComboBox comboUser = new ElementComboBox(true, 25);
        panelInfos.add(comboUser, c);

        c.weighty = 1;
        c.gridy++;
        JPanel panelLayouter = new JPanel();
        panelLayouter.setOpaque(false);
        panelInfos.add(panelLayouter, c);

        GridBagConstraints cFull = new DefaultGridBagConstraints();
        cFull.weightx = 1;
        cFull.weighty = 1;
        cFull.fill = GridBagConstraints.BOTH;

        if (getMode() == Mode.MODIFICATION) {
            pane.add("Informations", panelInfos);

            pane.add("Objectifs", objectifPanel);
            panelInfos.setOpaque(false);
            this.add(pane, cFull);
        } else {
            this.add(panelInfos, cFull);
        }

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

    public JTabbedPane getModificationTabbedPane() {
        return this.pane;
    }
}
