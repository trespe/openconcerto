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
 
 package org.openconcerto.erp.core.supplychain.credit.component;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.MontantPanel;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.supplychain.credit.element.AvoirFournisseurSQLElement;
import org.openconcerto.erp.generationEcritures.GenerationMvtAvoirFournisseur;
import org.openconcerto.erp.model.ISQLCompteSelector;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.Date;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class AvoirFournisseurSQLComponent extends TransfertBaseSQLComponent implements ActionListener {

    private JTextField textNom;
    private JDate date;
    private ElementComboBox selectFournisseur;
    private MontantPanel montantPanel;
    private JTextField textNumero = new JTextField(16);
    private JCheckBox boxAdeduire = new JCheckBox(getLabelFor("A_DEDUIRE"));
    private JCheckBox boxImmo = new JCheckBox(getLabelFor("IMMO"));
    private ElementSQLObject eltModeRegl;
    private PropertyChangeListener listenerModeReglDefaut = new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent evt) {

            SQLRow rowSelected = AvoirFournisseurSQLComponent.this.selectFournisseur.getSelectedRow();

            if (rowSelected != null && !rowSelected.isUndefined()) {
                rowSelected = rowSelected.asRow();
                AvoirFournisseurSQLComponent.this.montantPanel.setUE(rowSelected.getBoolean("UE"));
                int idModeRegl = rowSelected.getInt("ID_MODE_REGLEMENT");

                if (idModeRegl > 1 && AvoirFournisseurSQLComponent.this.eltModeRegl != null) {
                    SQLElement sqlEltModeRegl = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
                    SQLRow rowModeRegl = sqlEltModeRegl.getTable().getRow(idModeRegl);
                    SQLRowValues rowVals = rowModeRegl.createUpdateRow();
                    rowVals.clearPrimaryKeys();
                    AvoirFournisseurSQLComponent.this.eltModeRegl.setValue(rowVals);
                }

            } else {
                AvoirFournisseurSQLComponent.this.montantPanel.setUE(false);
            }
        }

    };

    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues vals = new SQLRowValues(this.getTable());
        vals.put("A_DEDUIRE", Boolean.TRUE);

        // Select Compte charge par defaut
        final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
        final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);
        // compte Achat
        int idCompteAchat = rowPrefsCompte.getInt("ID_COMPTE_PCE_ACHAT");
        if (idCompteAchat <= 1) {
            try {
                idCompteAchat = ComptePCESQLElement.getIdComptePceDefault("Achats");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        vals.put("ID_COMPTE_PCE", idCompteAchat);
        vals.put("ID_TAXE", 2);
        vals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(AvoirFournisseurSQLElement.class, new Date()));
        return vals;
    }

    public AvoirFournisseurSQLComponent() {

        super(Configuration.getInstance().getDirectory().getElement("AVOIR_FOURNISSEUR"));
    }

    public void addViews() {
        this.setLayout(new GridBagLayout());

        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Champ Module
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel addP = ComptaSQLConfElement.createAdditionalPanel();
        this.setAdditionalFieldsPanel(new FormLayouter(addP, 1));
        this.add(addP, c);

        c.gridy++;
        c.gridwidth = 1;

        this.textNom = new JTextField();
        this.date = new JDate(true);
        this.selectFournisseur = new ElementComboBox();
        this.montantPanel = new MontantPanel();

        // Numero
        this.add(new JLabel(getLabelFor("NUMERO"), SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        DefaultGridBagConstraints.lockMinimumSize(this.textNumero);
        this.add(this.textNumero, c);

        // Date
        c.gridx++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Date", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(this.date, c);

        // Libellé
        c.gridwidth = 1;
        c.weightx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        final JLabel labelLib = new JLabel(getLabelFor("NOM"), SwingConstants.RIGHT);
        this.add(labelLib, c);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 3;
        this.add(this.textNom, c);

        // Fournisseur
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0;
        c.gridwidth = 1;
        final JLabel labelFourn = new JLabel(getLabelFor("ID_FOURNISSEUR"), SwingConstants.RIGHT);

        this.add(labelFourn, c);

        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0;
        this.add(this.selectFournisseur, c);

        c.gridx = 0;
        c.gridy++;
        c.weighty = 0;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;

        this.montantPanel = new MontantPanel();
        this.add(this.montantPanel, c);

        // /

        TitledSeparator title = new TitledSeparator("Comptabilité");
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy++;
        c.gridwidth = 4;
        this.add(title, c);

        // Ligne 5 Compte de charges
        c.gridy++;
        c.weightx = 0;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        final ISQLCompteSelector compteSel = new ISQLCompteSelector();
        this.add(new JLabel(getLabelFor("ID_COMPTE_PCE")), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx++;
        c.weightx = 1;
        this.add(compteSel, c);

        // Immo
        c.gridwidth = 1;

        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 0;
        c.gridy++;
        c.weighty = 0;
        this.add(this.boxImmo, c);

        // Mode de règlement
        // this.addView("ID_MODE_REGLEMENT", DEC + ";" + SEP);
        // eltModeRegl = (ElementSQLObject) this.getView("ID_MODE_REGLEMENT");
        //
        // JPanel panelBottom = new JPanel();
        // panelBottom.add(eltModeRegl);

        /*******************************************************************************************
         * * CALCUL DES TOTAUX
         ******************************************************************************************/

        // c.anchor = GridBagConstraints.WEST;
        // c.gridwidth = GridBagConstraints.REMAINDER;
        // c.gridx = 0;
        // c.gridy++;
        // c.weighty = 0;
        // this.add(this.boxAdeduire, c);
        //
        // c.anchor = GridBagConstraints.WEST;
        // c.gridwidth = GridBagConstraints.REMAINDER;
        // c.gridx = 0;
        // c.gridy++;
        // c.weighty = 0;
        // this.add(panelBottom, c);
        // Filler
        JPanel p = new JPanel();

        p.setOpaque(false);
        c.gridy++;
        c.weighty = 1;
        this.add(p, c);

        this.addSQLObject(this.textNom, "NOM");
        // this.addSQLObject(this.boxAdeduire, "A_DEDUIRE");
        this.addRequiredSQLObject(this.textNumero, "NUMERO");
        this.addRequiredSQLObject(this.date, "DATE");
        this.addRequiredSQLObject(this.selectFournisseur, "ID_FOURNISSEUR");
        this.addRequiredSQLObject(this.montantPanel.getMontantHT(), "MONTANT_HT");
        this.addRequiredSQLObject(this.montantPanel.getMontantTVA(), "MONTANT_TVA");
        this.addRequiredSQLObject(this.montantPanel.getMontantTTC(), "MONTANT_TTC");
        this.addRequiredSQLObject(this.montantPanel.getChoixTaxe(), "ID_TAXE");
        this.addSQLObject(compteSel, "ID_COMPTE_PCE");
        this.addSQLObject(this.boxImmo, "IMMO");
        this.selectFournisseur.addValueListener(this.listenerModeReglDefaut);
        // this.boxAdeduire.addActionListener(this);

        this.montantPanel.setChoixTaxe(2);
    }

    @Override
    public int insert(SQLRow order) {

        int id = getSelectedID();

        id = super.insert(order);

        final SQLTable tableNum = this.getTable().getBase().getTable("NUMEROTATION_AUTO");
        final SQLRow row = getTable().getRow(id);

        // incrémentation du numéro auto
        if (NumerotationAutoSQLElement.getNextNumero(AvoirFournisseurSQLElement.class, row.getDate("DATE").getTime()).equalsIgnoreCase(this.textNumero.getText().trim())) {
            SQLRowValues rowVals = new SQLRowValues(tableNum);
            int val = tableNum.getRow(2).getInt("AVOIR_F_START");
            val++;
            rowVals.put("AVOIR_F_START", Integer.valueOf(val));

            try {
                rowVals.update(2);
            } catch (SQLException e) {

                e.printStackTrace();
            }
        }

        GenerationMvtAvoirFournisseur gen = new GenerationMvtAvoirFournisseur(id);
        gen.genereMouvement();

        return id;
    }

    @Override
    public void select(SQLRowAccessor r) {
        this.selectFournisseur.rmValueListener(this.listenerModeReglDefaut);
        super.select(r);
        if (r != null && r.getID() > 1) {
            int i = r.getInt("ID_FOURNISSEUR");
            if (i > 1) {
                SQLRowAccessor rowFourn = r.getForeign("ID_FOURNISSEUR");
                if (rowFourn != null && rowFourn.getID() > 1) {
                    AvoirFournisseurSQLComponent.this.montantPanel.setUE(rowFourn.getBoolean("UE"));
                }
            }
        }
        this.selectFournisseur.addValueListener(this.listenerModeReglDefaut);
    }

    @Override
    public void update() {
        super.update();

        SQLRow row = getTable().getRow(getSelectedID());

        int idMvt = row.getInt("ID_MOUVEMENT");

        // on supprime tout ce qui est lié à la facture d'avoir
        System.err.println("Archivage des fils");
        EcritureSQLElement eltEcr = (EcritureSQLElement) Configuration.getInstance().getDirectory().getElement("ECRITURE");
        eltEcr.archiveMouvementProfondeur(idMvt, false);

        GenerationMvtAvoirFournisseur gen = new GenerationMvtAvoirFournisseur(getSelectedID(), idMvt);
        gen.genereMouvement();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.boxAdeduire) {
            if (this.eltModeRegl != null) {
                this.eltModeRegl.setEditable(!this.boxAdeduire.isSelected());
                this.eltModeRegl.setCreated(!this.boxAdeduire.isSelected());
            }
        }
    }

}
