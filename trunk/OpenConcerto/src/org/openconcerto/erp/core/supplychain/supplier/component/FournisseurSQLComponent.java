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
 * Créé le 20 janv. 2012
 */
package org.openconcerto.erp.core.supplychain.supplier.component;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.customerrelationship.customer.element.ContactItemTable;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.model.ISQLCompteSelector;
import org.openconcerto.erp.preferences.ModeReglementDefautPrefPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.UndefinedRowValuesCache;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.ITextArea;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.sql.SQLException;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class FournisseurSQLComponent extends BaseSQLComponent {

    public FournisseurSQLComponent(SQLElement elt) {
        super(elt);
    }

    JCheckBox checkEnlevement;
    ElementSQLObject comp2;
    TitledSeparator sep2;
    private ContactItemTable table;
    private SQLTable contactTable = Configuration.getInstance().getDirectory().getElement("CONTACT_FOURNISSEUR").getTable();
    private SQLRowValues defaultContactRowVals = new SQLRowValues(UndefinedRowValuesCache.getInstance().getDefaultRowValues(this.contactTable));

    public void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Code
        JLabel labelCode = new JLabel("Code");
        JTextField textCode = new JTextField();
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        this.add(labelCode, c);
        c.gridx++;
        c.gridwidth = 1;
        c.weightx = 0.5;
        this.add(textCode, c);

        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        // Raison sociale
        JLabel labelRS = new JLabel("Forme juridique");
        SQLTextCombo textType = new SQLTextCombo();
        JTextField textNom = new JTextField();

        this.add(labelRS, c);
        c.gridx++;
        c.weightx = 0.5;
        this.add(textType, c);

        // Tel
        JLabel labelTel = new JLabel(getLabelFor("TEL"));
        JTextField textTel = new JTextField();
        c.gridx++;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        this.add(labelTel, c);
        c.gridx++;
        c.gridwidth = 1;
        c.weightx = 0.5;
        this.add(textTel, c);

        // Nom
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        String fieldNom = "NOM";
        if (getTable().contains("NOM_SOCIETE")) {
            fieldNom = "NOM_SOCIETE";
        }
        this.add(new JLabel(getLabelFor(fieldNom)), c);
        c.gridx++;
        c.gridwidth = 1;
        c.weightx = 0.5;
        this.add(textNom, c);
        this.addRequiredSQLObject(textNom, fieldNom);

        // Fax
        c.gridx++;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("FAX")), c);
        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.5;
        JTextField textFax = new JTextField();
        this.add(textFax, c);

        // Mail
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("MAIL")), c);
        c.gridx++;
        c.gridwidth = 1;
        c.weightx = 0.5;
        JTextField textMail = new JTextField();
        this.add(textMail, c);
        this.addView(textMail, "MAIL");

        // Tel P
        c.gridx++;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("TEL_P")), c);
        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.5;
        JTextField textTelP = new JTextField();
        this.add(textTelP, c);
        this.addView(textTelP, "TEL_P");
        // Langue
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("ID_LANGUE")), c);
        c.gridx++;
        c.gridwidth = 1;
        c.weightx = 0.5;
        ElementComboBox langue = new ElementComboBox(true, 35);
        this.add(langue, c);
        this.addView(langue, "ID_LANGUE");

        // Resp
        c.gridx++;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("RESPONSABLE")), c);
        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.5;
        JTextField textResp = new JTextField();
        this.add(textResp, c);
        this.addView(textResp, "RESPONSABLE");

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        JCheckBox boxUE = new JCheckBox(getLabelFor("UE"));
        this.add(boxUE, c);

        // Champ Module
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel addP = new JPanel();
        this.setAdditionalFieldsPanel(new FormLayouter(addP, 2));
        this.add(addP, c);

        JPanel panelAdresse = new JPanel(new GridBagLayout());
        GridBagConstraints cAdr = new DefaultGridBagConstraints();
        // Adresse
        TitledSeparator sep = new TitledSeparator("Adresse");
        panelAdresse.add(sep, cAdr);

        this.sep2 = new TitledSeparator("Adresse d'enlévement");
        cAdr.gridx = GridBagConstraints.RELATIVE;
        panelAdresse.add(this.sep2, cAdr);

        //

        this.addView("ID_ADRESSE", REQ + ";" + DEC + ";" + SEP);

        cAdr.gridy++;
        panelAdresse.add((ElementSQLObject) this.getView("ID_ADRESSE"), cAdr);

        // Selection de 2eme adresse
        this.addView("ID_ADRESSE_E", DEC);
        cAdr.gridx = GridBagConstraints.RELATIVE;
        this.comp2 = (ElementSQLObject) this.getView("ID_ADRESSE_E");
        this.comp2.setCreated(true);
        panelAdresse.add(this.comp2, cAdr);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(panelAdresse, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        this.checkEnlevement = new JCheckBox("Adresse d'enlèvement identique");

        this.add(this.checkEnlevement, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        this.checkEnlevement.setSelected(true);
        this.sep2.setVisible(false);
        this.checkEnlevement.addActionListener(new ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                System.out.println("Click");
                if (checkEnlevement.isSelected()) {
                    System.out.println("Mode 1");
                    comp2.setEditable(false);
                    comp2.setCreated(false);
                    sep2.setVisible(false);
                } else {
                    System.out.println("Mode 2");
                    comp2.setEditable(true);
                    comp2.setCreated(true);
                    sep2.setVisible(true);
                }
            };
        });

        // Contact
        final TitledSeparator sepContact = new TitledSeparator("Contacts fournisseurs");
        c.weightx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 0;
        c.gridy++;
        this.add(sepContact, c);
        this.table = new ContactItemTable(this.defaultContactRowVals);
        this.table.setPreferredSize(new Dimension(this.table.getSize().width, 150));

        c.gridx = 0;
        c.gridy++;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 0.8;
        this.add(this.table, c);

        // Mode de régelement
        TitledSeparator reglSep = new TitledSeparator(getLabelFor("ID_MODE_REGLEMENT"));
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy++;
        c.gridx = 0;
        this.add(reglSep, c);

        c.gridy++;
        c.gridx = 0;
        this.addView("ID_MODE_REGLEMENT", REQ + ";" + DEC + ";" + SEP);
        ElementSQLObject eltModeRegl = (ElementSQLObject) this.getView("ID_MODE_REGLEMENT");
        this.add(eltModeRegl, c);

        // Compte associé

        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        TitledSeparator sepCompte = new TitledSeparator("Compte associé");
        this.add(sepCompte, c);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c2 = new DefaultGridBagConstraints();

        panel.add(new JLabel(getLabelFor("ID_COMPTE_PCE")), c2);
        ISQLCompteSelector compteSel = new ISQLCompteSelector(true);
        c2.gridx++;
        c2.weightx = 1;
        panel.add(compteSel, c2);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        this.add(panel, c);

        // INfos
        c.gridx = 0;
        c.gridy++;
        c.gridheight = 1;
        c.weighty = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(new TitledSeparator(getLabelFor("INFOS")), c);

        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        ITextArea infos = new ITextArea();
        final JScrollPane scrollPane = new JScrollPane(infos);
        scrollPane.setBorder(null);
        this.add(scrollPane, c);
        this.addView(infos, "INFOS");

        this.addSQLObject(textType, "TYPE");

        this.addSQLObject(textCode, "CODE");
        this.addSQLObject(textTel, "TEL");
        this.addSQLObject(textFax, "FAX");
        this.addSQLObject(boxUE, "UE");
        this.addRequiredSQLObject(compteSel, "ID_COMPTE_PCE");
        // this.addRequiredPrivateForeignField(adresse,"ID_ADRESSE");

        // this.addSQLObject(adresse, "VILLE");
        /***********************************************************************************
         * this.addSQLObject(new JTextField(), "NUMERO_CANTON"); this.addSQLObject(new JTextField(),
         * "NUMERO_COMMUNE");
         */

        // Select Compte fournisseur par defaut
        final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
        final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);
        int idCompteFourn = rowPrefsCompte.getInt("ID_COMPTE_PCE_FOURNISSEUR");
        if (idCompteFourn <= 1) {
            try {
                idCompteFourn = ComptePCESQLElement.getIdComptePceDefault("Fournisseurs");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        compteSel.setValue(idCompteFourn);
    }

    @Override
    public void select(SQLRowAccessor r) {
        if (r != null && r.isForeignEmpty("ID_ADRESSE_E")) {
            this.checkEnlevement.setSelected(true);
            this.comp2.setEditable(false);
            this.comp2.setCreated(false);
            this.sep2.setVisible(false);
        } else {
            this.checkEnlevement.setSelected(false);
            this.comp2.setEditable(true);
            this.comp2.setCreated(true);
            this.sep2.setVisible(true);
        }

        if (r != null) {
            this.table.insertFrom("ID_" + this.getTable().getName(), r.getID());
            this.defaultContactRowVals.put("TEL_DIRECT", r.getString("TEL"));
            this.defaultContactRowVals.put("FAX", r.getString("FAX"));
        }

        super.select(r);
    }

    @Override
    public void update() {
        // TODO Raccord de méthode auto-généré
        super.update();
        this.table.updateField("ID_" + this.getTable().getName(), getSelectedID());
    }

    @Override
    public int insert(SQLRow order) {
        // TODO Raccord de méthode auto-généré
        int id = super.insert(order);
        this.table.updateField("ID_" + this.getTable().getName(), id);
        return id;
    }

    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues vals = new SQLRowValues(this.getTable());
        SQLRowAccessor r;
        this.checkEnlevement.setSelected(true);
        this.sep2.setVisible(false);
        try {
            r = ModeReglementDefautPrefPanel.getDefaultRow(false);
            SQLElement eltModeReglement = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
            if (r.getID() > 1) {
                SQLRowValues rowVals = eltModeReglement.createCopy(r.getID());
                System.err.println(rowVals.getInt("ID_TYPE_REGLEMENT"));
                vals.put("ID_MODE_REGLEMENT", rowVals);
            }
        } catch (SQLException e) {
            System.err.println("Impossible de sélectionner le mode de règlement par défaut du client.");
            e.printStackTrace();
        }
        return vals;
    }

}
