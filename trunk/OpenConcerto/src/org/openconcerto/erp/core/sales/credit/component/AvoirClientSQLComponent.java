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
 
 package org.openconcerto.erp.core.sales.credit.component;

import static org.openconcerto.utils.CollectionUtils.createSet;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.component.SocieteCommonSQLElement;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.AbstractArticleItemTable;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.payment.component.ModeDeReglementSQLComponent;
import org.openconcerto.erp.core.sales.credit.element.AvoirClientSQLElement;
import org.openconcerto.erp.core.sales.credit.ui.AvoirItemTable;
import org.openconcerto.erp.core.sales.invoice.component.SaisieVenteFactureSQLComponent;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.erp.generationDoc.gestcomm.AvoirClientXmlSheet;
import org.openconcerto.erp.generationEcritures.GenerationMvtAvoirClient;
import org.openconcerto.erp.model.ISQLCompteSelector;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.checks.ValidState;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class AvoirClientSQLComponent extends TransfertBaseSQLComponent implements ActionListener {

    private JTextField textNom;
    private JDate date;
    private JUniqueTextField textNumero;
    private JCheckBox checkImpr, checkVisu;
    private AbstractArticleItemTable table;
    private JCheckBox boxAdeduire = new JCheckBox(getLabelFor("A_DEDUIRE"));
    private ElementSQLObject eltModeRegl;
    private final SQLTable tableClient = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("CLIENT");
    private final SQLElement clientElt = Configuration.getInstance().getDirectory().getElement(this.tableClient);
    private final ElementComboBox comboClient = new ElementComboBox();
    private ElementComboBox comboPole = new ElementComboBox();
    private ElementComboBox comboCommercial = new ElementComboBox();
    private ElementComboBox comboVerificateur = new ElementComboBox();
    private final ElementComboBox comboAdresse = new ElementComboBox();
    private final ElementComboBox comboBanque = new ElementComboBox();
    private JLabel labelCompteServ;
    private ISQLCompteSelector compteSelService;
    private static final SQLTable TABLE_PREFS_COMPTE = Configuration.getInstance().getBase().getTable("PREFS_COMPTE");
    private static final SQLRow ROW_PREFS_COMPTE = TABLE_PREFS_COMPTE.getRow(2);

    private SQLRowValues defaultContactRowValues;

    private JCheckBox checkCompteServiceAuto;

    private PropertyChangeListener listenerModeReglDefaut = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent arg0) {
            int idCli = AvoirClientSQLComponent.this.comboClient.getSelectedId();
            if (idCli > 1) {
                SQLRow rowCli = AvoirClientSQLComponent.this.tableClient.getRow(idCli);
                SQLElement sqleltModeRegl = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
                int idModeRegl = rowCli.getInt("ID_MODE_REGLEMENT");
                if (idModeRegl > 1 && AvoirClientSQLComponent.this.eltModeRegl.isCreated() && getMode() == Mode.INSERTION) {
                    SQLRow rowModeRegl = sqleltModeRegl.getTable().getRow(idModeRegl);
                    SQLRowValues rowValsModeRegl = rowModeRegl.createUpdateRow();
                    rowValsModeRegl.clearPrimaryKeys();
                    AvoirClientSQLComponent.this.eltModeRegl.setValue(rowValsModeRegl);
                }
            }
        }
    };
    private final ElementComboBox boxTarif = new ElementComboBox();
    private PropertyChangeListener changeClientListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            // compteSel.removeValueListener(changeCompteListener);

            if (AvoirClientSQLComponent.this.comboClient.getValue() != null) {
                Integer id = AvoirClientSQLComponent.this.comboClient.getValue();
                AvoirClientSQLComponent.this.defaultContactRowValues.put("ID_CLIENT", id);
                if (id > 1) {
                    SQLRow row = AvoirClientSQLComponent.this.clientElt.getTable().getRow(id);

                    if (comboClient.getElement().getTable().getFieldsName().contains("ID_TARIF")) {

                        // SQLRowAccessor foreignRow = row.getForeignRow("ID_TARIF");
                        // if (foreignRow.isUndefined() &&
                        // !row.getForeignRow("ID_DEVISE").isUndefined()) {
                        // SQLRowValues rowValsD = new SQLRowValues(foreignRow.getTable());
                        // rowValsD.put("ID_DEVISE", row.getObject("ID_DEVISE"));
                        // foreignRow = rowValsD;
                        //
                        // }
                        // tableBonItem.setTarif(foreignRow, true);
                        SQLRowAccessor foreignRow = row.getForeignRow("ID_TARIF");
                        if (!foreignRow.isUndefined() && (boxTarif.getSelectedRow() == null || boxTarif.getSelectedId() != foreignRow.getID())
                                && JOptionPane.showConfirmDialog(null, "Appliquer les tarifs associés au client?") == JOptionPane.YES_OPTION) {
                            boxTarif.setValue(foreignRow.getID());
                            // SaisieVenteFactureSQLComponent.this.tableFacture.setTarif(foreignRow,
                            // true);
                        } else {
                            boxTarif.setValue(foreignRow.getID());
                        }
                    }

                        Where w = new Where(SaisieVenteFactureSQLComponent.TABLE_ADRESSE.getKey(), "=", row.getInt("ID_ADRESSE"));

                        w = w.or(new Where(SaisieVenteFactureSQLComponent.TABLE_ADRESSE.getKey(), "=", row.getInt("ID_ADRESSE_L")));
                        w = w.or(new Where(SaisieVenteFactureSQLComponent.TABLE_ADRESSE.getKey(), "=", row.getInt("ID_ADRESSE_F")));
                        List<SQLRow> list = row.getReferentRows(SaisieVenteFactureSQLComponent.TABLE_ADRESSE.getField("ID_CLIENT"));
                        for (SQLRow row2 : list) {
                            w = w.or(new Where(SaisieVenteFactureSQLComponent.TABLE_ADRESSE.getKey(), "=", row2.getID()));
                        }

                        comboAdresse.getRequest().setWhere(w);
                } else {
                        comboAdresse.getRequest().setWhere(null);
                }
            }
        }
    };


    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues vals = new SQLRowValues(this.getTable());
        vals.put("A_DEDUIRE", Boolean.TRUE);
        this.eltModeRegl.setEditable(false);
        this.eltModeRegl.setCreated(false);


        // Selection du compte de service
        int idCompteVenteService = ROW_PREFS_COMPTE.getInt("ID_COMPTE_PCE_VENTE_SERVICE");
        if (idCompteVenteService <= 1) {
            try {
                idCompteVenteService = ComptePCESQLElement.getIdComptePceDefault("VentesServices");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        vals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(AvoirClientSQLElement.class, new Date()));
        vals.put("MONTANT_TTC", Long.valueOf(0));
        vals.put("MONTANT_SERVICE", Long.valueOf(0));
        vals.put("MONTANT_HT", Long.valueOf(0));
        vals.put("MONTANT_TVA", Long.valueOf(0));
        vals.put("ID_COMPTE_PCE_SERVICE", idCompteVenteService);

        return vals;
    }

    public AvoirClientSQLComponent() {
        super(Configuration.getInstance().getDirectory().getElement("AVOIR_CLIENT"));
    }

    public void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Champ Module
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel addP = new JPanel();
        this.setAdditionalFieldsPanel(new FormLayouter(addP, 1));
        this.add(addP, c);

        c.gridy++;
        c.gridwidth = 1;

        this.textNom = new JTextField();
        this.date = new JDate(true);
        this.date.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                fireValidChange();

            }
        });
        this.comboClient.addValueListener(this.listenerModeReglDefaut);
        this.comboClient.addValueListener(this.changeClientListener);

        // Ligne 1: Numero
        this.add(new JLabel(getLabelFor("NUMERO"), SwingConstants.RIGHT), c);
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.gridx++;
        textNumero = new JUniqueTextField(16);
        DefaultGridBagConstraints.lockMinimumSize(textNumero);
        this.add(this.textNumero, c);

        // Date
        c.gridx++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Date", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(this.date, c);

        // Ligne 2: Libellé
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("NOM"), SwingConstants.RIGHT), c);
        c.gridx++;
        // c.weightx = 1;
        this.add(this.textNom, c);

        // Commercial
        c.gridx++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("ID_COMMERCIAL"), SwingConstants.RIGHT), c);
        c.gridx++;
        // c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.comboCommercial = new ElementComboBox();
        this.comboCommercial.setMinimumSize(this.comboCommercial.getPreferredSize());
        this.add(this.comboCommercial, c);

        this.addSQLObject(this.comboCommercial, "ID_COMMERCIAL");

        // Ligne 3: Motif
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("MOTIF"), SwingConstants.RIGHT), c);
        c.gridx++;
        c.gridwidth = 3;
        // c.weightx = 1;

        JTextField textMotif = new JTextField();
        this.add(textMotif, c);

        // Client
        c.gridx = 0;
        c.gridy++;
        // c.weightx = 0;
        c.gridwidth = 1;
        this.add(new JLabel(getLabelFor("ID_CLIENT"), SwingConstants.RIGHT), c);
        c.gridx++;
        c.gridwidth = 3;
        // c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(this.comboClient, c);
            // Adresse spe
            c.gridx = 0;
            c.gridy++;
            c.fill = GridBagConstraints.HORIZONTAL;
            // c.weightx = 0;
            c.gridwidth = 1;
            this.add(new JLabel(getLabelFor("ID_ADRESSE"), SwingConstants.RIGHT), c);

            c.gridx++;
            c.gridwidth = 3;
            // c.weightx = 1;
            c.fill = GridBagConstraints.NONE;
            this.add(this.comboAdresse, c);
        final ComptaPropsConfiguration comptaPropsConfiguration = ((ComptaPropsConfiguration) Configuration.getInstance());

        // Contact
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        // c.weightx = 0;
        final JLabel labelContact = new JLabel(getLabelFor("ID_CONTACT"), SwingConstants.RIGHT);
        this.add(labelContact, c);

        c.gridx++;
        c.gridwidth = 3;
        // c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        final ElementComboBox selectContact = new ElementComboBox();
        this.add(selectContact, c);

        this.addView(selectContact, "ID_CONTACT");
        this.defaultContactRowValues = new SQLRowValues(selectContact.getRequest().getPrimaryTable());
        selectContact.getAddComp().setDefaults(this.defaultContactRowValues);



        // Compte Service
        this.checkCompteServiceAuto = new JCheckBox(getLabelFor("COMPTE_SERVICE_AUTO"));
        this.addSQLObject(this.checkCompteServiceAuto, "COMPTE_SERVICE_AUTO");
        this.compteSelService = new ISQLCompteSelector();

        this.labelCompteServ = new JLabel("Compte Service");
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        // c.weightx = 0;
        this.labelCompteServ.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(this.labelCompteServ, c);

        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        // c.weightx = 1;
        this.add(this.compteSelService, c);

        this.addRequiredSQLObject(this.compteSelService, "ID_COMPTE_PCE_SERVICE");

        String valServ = DefaultNXProps.getInstance().getStringProperty("ArticleService");
        Boolean bServ = Boolean.valueOf(valServ);
        if (!bServ) {
            this.labelCompteServ.setVisible(false);
            this.compteSelService.setVisible(false);
        }

        this.checkCompteServiceAuto.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCompteServiceVisible(!AvoirClientSQLComponent.this.checkCompteServiceAuto.isSelected());
            }
        });

        // setCompteServiceVisible(!(bServ != null && !bServ.booleanValue()));


        // Tarif
        if (this.getTable().getFieldsName().contains("ID_TARIF")) {
            // TARIF
            c.gridy++;
            c.gridx = 0;
            c.weightx = 0;
            c.weighty = 0;
            c.gridwidth = 1;
            this.add(new JLabel("Tarif à appliquer"), c);
            c.gridx++;
            c.gridwidth = GridBagConstraints.REMAINDER;

            c.weightx = 1;
            this.add(boxTarif, c);
            this.addView(boxTarif, "ID_TARIF");
            boxTarif.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    table.setTarif(boxTarif.getSelectedRow(), false);
                }
            });
        }

        // Table
            this.table = new AvoirItemTable();
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.gridheight = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 1;
        // c.weightx = 0;
        this.add(this.table, c);
        this.addView(this.table.getRowValuesTable(), "");

        // Panel du bas
        final JPanel panelBottom = getBottomPanel();
        c.gridy++;
        c.weighty = 0;
        this.add(panelBottom, c);

        // Infos

        c.gridheight = 1;
        c.gridx = 0;
        c.gridy++;
        this.add(new JLabel(getLabelFor("INFOS")), c);

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0;
        c.gridwidth = 4;
        ITextArea infos = new ITextArea(4, 4);
        infos.setBorder(null);
        JScrollPane scrollPane = new JScrollPane(infos);
        DefaultGridBagConstraints.lockMinimumSize(scrollPane);
        this.add(scrollPane, c);

        //
        // Impression
        JPanel panelGestDoc = new JPanel();

        this.checkImpr = new JCheckBox("Imprimer");
        this.checkVisu = new JCheckBox("Visualiser");
        panelGestDoc.add(this.checkImpr);
        panelGestDoc.add(this.checkVisu);
        c.fill = GridBagConstraints.NONE;
        c.gridy++;
        c.anchor = GridBagConstraints.EAST;
        this.add(panelGestDoc, c);

        this.addSQLObject(this.textNom, "NOM");
        this.addSQLObject(this.boxAdeduire, "A_DEDUIRE");
        this.addSQLObject(textMotif, "MOTIF");
            this.addSQLObject(this.comboAdresse, "ID_ADRESSE");

        this.addRequiredSQLObject(this.textNumero, "NUMERO");
        this.addRequiredSQLObject(this.date, "DATE");
        this.addRequiredSQLObject(this.comboClient, "ID_CLIENT");

        this.boxAdeduire.addActionListener(this);

        DefaultGridBagConstraints.lockMinimumSize(comboClient);
        DefaultGridBagConstraints.lockMinimumSize(this.comboAdresse);
        DefaultGridBagConstraints.lockMinimumSize(this.comboBanque);
        DefaultGridBagConstraints.lockMinimumSize(comboCommercial);

    }

    private JPanel getBottomPanel() {

        // UI

        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        // Colonne 1
        this.boxAdeduire.setOpaque(false);
        this.boxAdeduire.setMinimumSize(new Dimension(430, this.boxAdeduire.getPreferredSize().height));
        this.boxAdeduire.setPreferredSize(new Dimension(430, this.boxAdeduire.getPreferredSize().height));
        panel.add(this.boxAdeduire, c);
        this.addView("ID_MODE_REGLEMENT", DEC + ";" + SEP);
        this.eltModeRegl = (ElementSQLObject) this.getView("ID_MODE_REGLEMENT");

        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.weighty = 1;
        this.eltModeRegl.setOpaque(false);
        panel.add(this.eltModeRegl, c);

        // Colonne 2 : port et remise

        final JPanel panelPortEtRemise = new JPanel();
        panelPortEtRemise.setOpaque(false);
        panelPortEtRemise.setLayout(new GridBagLayout());

        final GridBagConstraints cFrais = new DefaultGridBagConstraints();

        DeviseField textPortHT = new DeviseField(5);
        DeviseField textRemiseHT = new DeviseField(5);

        // Frais de port
        cFrais.gridheight = 1;
        cFrais.fill = GridBagConstraints.VERTICAL;
        cFrais.weighty = 1;
        cFrais.gridx = 1;

        JLabel labelPortHT = new JLabel(getLabelFor("PORT_HT"));
        labelPortHT.setHorizontalAlignment(SwingConstants.RIGHT);
        cFrais.gridy++;
        panelPortEtRemise.add(labelPortHT, cFrais);
        cFrais.gridx++;
        DefaultGridBagConstraints.lockMinimumSize(textPortHT);
        panelPortEtRemise.add(textPortHT, cFrais);

        // Remise
        JLabel labelRemiseHT = new JLabel(getLabelFor("REMISE_HT"));
        labelRemiseHT.setHorizontalAlignment(SwingConstants.RIGHT);
        cFrais.gridy++;
        cFrais.gridx = 1;
        panelPortEtRemise.add(labelRemiseHT, cFrais);
        cFrais.gridx++;
        DefaultGridBagConstraints.lockMinimumSize(textRemiseHT);
        panelPortEtRemise.add(textRemiseHT, cFrais);
        cFrais.gridy++;

        c.gridx++;
        c.gridy = 0;
        c.gridheight = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        panel.add(panelPortEtRemise, c);

        // Colonne 3 : totaux
        final DeviseField fieldHT = new DeviseField();
        final DeviseField fieldTVA = new DeviseField();
        final DeviseField fieldService = new DeviseField();
        final DeviseField fieldTTC = new DeviseField();
        // SQL
        addSQLObject(textPortHT, "PORT_HT");
        final DeviseField fieldDevise = new DeviseField();
        if (getTable().getFieldsName().contains("T_DEVISE"))
            addSQLObject(fieldDevise, "T_DEVISE");
        addSQLObject(textRemiseHT, "REMISE_HT");
        addRequiredSQLObject(fieldHT, "MONTANT_HT");
        addRequiredSQLObject(fieldTVA, "MONTANT_TVA");
        addRequiredSQLObject(fieldTTC, "MONTANT_TTC");
        addRequiredSQLObject(fieldService, "MONTANT_SERVICE");
        //
        JTextField poids = new JTextField();
        if (getTable().getFieldsName().contains("T_POIDS"))
            addSQLObject(poids, "T_POIDS");
        final TotalPanel totalTTC = new TotalPanel(this.table.getRowValuesTable(), this.table.getPrixTotalHTElement(), this.table.getPrixTotalTTCElement(), this.table.getHaElement(),
                this.table.getQteElement(), fieldHT, fieldTVA, fieldTTC, textPortHT, textRemiseHT, fieldService, this.table.getPrixServiceElement(), fieldDevise,
                this.table.getTableElementTotalDevise(), poids, this.table.getPoidsTotalElement());
        totalTTC.setOpaque(false);
        c.gridx++;
        c.gridy = 0;
        c.gridheight = 2;
        c.fill = GridBagConstraints.BOTH;
        panel.add(totalTTC, c);

        // Listeners
        textPortHT.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }

            public void removeUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }

            public void insertUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }
        });

        textRemiseHT.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }

            public void removeUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }

            public void insertUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }
        });
        return panel;
    }

    @Override
    public synchronized ValidState getValidState() {
        final ValidState selfState;
        final Date value = this.date.getValue();
        if (value != null && value.after(SocieteCommonSQLElement.getDateDebutExercice())) {
            selfState = ValidState.getTrueInstance();
        } else {
            selfState = ValidState.createCached(false, "La date est incorrecte, cette période est cloturée.");
        }
        return super.getValidState().and(selfState);
    }

    private void setCompteServiceVisible(boolean b) {
        this.compteSelService.setVisible(b);
        this.labelCompteServ.setVisible(b);
    }

    private String getInitialesFromVerif(SQLRow row) {
        String s = "";

        if (row != null) {
            String prenom = row.getString("PRENOM");
            if (prenom != null && prenom.length() > 0) {
                s += prenom.toUpperCase().charAt(0);
            }
            String nom = row.getString("NOM");
            if (nom != null && nom.length() > 0) {
                s += nom.toUpperCase().charAt(0);
            }
        }

        return s;
    }

    private void createCompteServiceAuto(int id) {
        SQLRow rowPole = this.comboPole.getSelectedRow();
        SQLRow rowVerif = this.comboVerificateur.getSelectedRow();
        String verifInitiale = getInitialesFromVerif(rowVerif);
        int idCpt = ComptePCESQLElement.getId("706" + rowPole.getString("CODE") + verifInitiale, "Service " + rowPole.getString("NOM") + " " + rowVerif.getString("NOM"));
        SQLRowValues rowVals = this.getTable().getRow(id).createEmptyUpdateRow();
        rowVals.put("ID_COMPTE_PCE_SERVICE", idCpt);
        try {
            rowVals.update();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int insert(SQLRow order) {

        int id = getSelectedID();
        final SQLTable tableNum = this.getTable().getBase().getTable("NUMEROTATION_AUTO");
        if (this.textNumero.checkValidation()) {

            id = super.insert(order);
            this.table.updateField("ID_AVOIR_CLIENT", id);
            final SQLRow row = getTable().getRow(id);

            // incrémentation du numéro auto
            if (NumerotationAutoSQLElement.getNextNumero(AvoirClientSQLElement.class, row.getDate("DATE").getTime()).equalsIgnoreCase(this.textNumero.getText().trim())) {
                SQLRowValues rowVals = new SQLRowValues(tableNum);
                int val = tableNum.getRow(2).getInt("AVOIR_START");
                val++;
                rowVals.put("AVOIR_START", Integer.valueOf(val));

                try {
                    rowVals.update(2);
                } catch (SQLException e) {

                    e.printStackTrace();
                }
            }

            SQLRowValues rowVals2 = row.createUpdateRow();
            Long l = rowVals2.getLong("MONTANT_SOLDE");
            Long l2 = rowVals2.getLong("MONTANT_TTC");

            rowVals2.put("MONTANT_RESTANT", l2 - l);

            try {
                rowVals2.update();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            updateStock(id);

            GenerationMvtAvoirClient gen = new GenerationMvtAvoirClient(id);
            gen.genereMouvement();

            // generation du document
            createAvoirClient(row);

        } else {
            ExceptionHandler.handle("Impossible de modifier, numéro d'avoir existant.");
            Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
        }
        return id;
    }

    private void createAvoirClient(final SQLRow row) {

        final AvoirClientXmlSheet bSheet = new AvoirClientXmlSheet(row);
        try {
            bSheet.createDocumentAsynchronous();
            bSheet.showPrintAndExportAsynchronous(checkVisu.isSelected(), checkImpr.isSelected(), true);
        } catch (Exception e) {
            ExceptionHandler.handle("Impossible de créer l'avoir", e);
        }

    }

    @Override
    public void select(SQLRowAccessor r) {
        if (r != null) {
            this.textNumero.setIdSelected(r.getID());
        }
        this.comboClient.rmValueListener(listenerModeReglDefaut);
        this.comboClient.rmValueListener(changeClientListener);

        if (r != null) {
            this.table.insertFrom("ID_AVOIR_CLIENT", r.getID());

            // Les contacts sont filtrés en fonction du client (ID_AFFAIRE.ID_CLIENT), donc si
            // l'ID_CONTACT est changé avant ID_AFFAIRE le contact ne sera pas présent dans la combo
            // => charge en deux fois les valeurs
            final SQLRowValues rVals = r.asRowValues();
            final SQLRowValues vals = new SQLRowValues(r.getTable());

            vals.load(rVals, createSet("ID_CLIENT"));
            // vals a besoin de l'ID sinon incohérence entre ID_AFFAIRE et ID (eg for
            // reloadTable())
            // ne pas supprimer l'ID de rVals pour qu'on puisse UPDATE
            vals.setID(rVals.getID());
            super.select(vals);
            rVals.remove("ID_CLIENT");
            super.select(rVals);
        } else {
            super.select(r);
        }
        this.comboClient.addValueListener(listenerModeReglDefaut);
        this.comboClient.addValueListener(changeClientListener);
    }

    @Override
    public void update() {
        if (this.textNumero.checkValidation()) {
            super.update();
            this.table.updateField("ID_AVOIR_CLIENT", getSelectedID());

            // On efface les anciens mouvements de stocks
            SQLRow row = getTable().getRow(getSelectedID());
            SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
            SQLSelect sel = new SQLSelect(eltMvtStock.getTable().getBase());
            sel.addSelect(eltMvtStock.getTable().getField("ID"));
            Where w = new Where(eltMvtStock.getTable().getField("IDSOURCE"), "=", row.getID());
            Where w2 = new Where(eltMvtStock.getTable().getField("SOURCE"), "=", getTable().getName());
            sel.setWhere(w.and(w2));

            List l = (List) eltMvtStock.getTable().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());
            if (l != null) {
                for (int i = 0; i < l.size(); i++) {
                    Object[] tmp = (Object[]) l.get(i);
                    try {
                        eltMvtStock.archive(((Number) tmp[0]).intValue());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }

            SQLRowValues rowVals2 = getTable().getRow(getSelectedID()).createUpdateRow();
            Long l2 = rowVals2.getLong("MONTANT_SOLDE");
            Long l3 = rowVals2.getLong("MONTANT_TTC");

            rowVals2.put("MONTANT_RESTANT", l3 - l2);

            try {
                rowVals2.update();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // On met à jour le stock
            updateStock(getSelectedID());

            int idMvt = row.getInt("ID_MOUVEMENT");

            // on supprime tout ce qui est lié à la facture d'avoir
            System.err.println("Archivage des fils");
            EcritureSQLElement eltEcr = (EcritureSQLElement) Configuration.getInstance().getDirectory().getElement("ECRITURE");
            eltEcr.archiveMouvementProfondeur(idMvt, false);

            GenerationMvtAvoirClient gen = new GenerationMvtAvoirClient(getSelectedID(), idMvt);
            gen.genereMouvement();

            createAvoirClient(row);
        } else {
            ExceptionHandler.handle("Impossible de modifier, numéro d'avoir existant.");
            Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
            return;
        }
    }

    public void loadFactureItem(int idFacture) {

        SQLElement facture = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        SQLElement factureElt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");

        loadItem(this.table, facture, idFacture, factureElt);

    }

    /**
     * Mise à jour des stocks pour chaque article composant la facture d'avoir
     */
    private void updateStock(int id) {

        SQLElement eltArticleAvoir = Configuration.getInstance().getDirectory().getElement("AVOIR_CLIENT_ELEMENT");
        SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
        SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement(sqlTableArticle);
        SQLRow rowAvoir = getTable().getRow(id);

        // On récupére les articles qui composent la facture
        SQLSelect selEltAvoir = new SQLSelect(eltArticleAvoir.getTable().getBase());
        selEltAvoir.addSelect(eltArticleAvoir.getTable().getField("ID"));
        selEltAvoir.setWhere(new Where(eltArticleAvoir.getTable().getField("ID_AVOIR_CLIENT"), "=", id));

        List lEltAvoir = (List) eltArticleAvoir.getTable().getBase().getDataSource().execute(selEltAvoir.asString(), new ArrayListHandler());

        if (lEltAvoir != null) {
            for (int i = 0; i < lEltAvoir.size(); i++) {

                // Elt qui compose facture
                Object[] tmp = (Object[]) lEltAvoir.get(i);
                int idEltFact = ((Number) tmp[0]).intValue();
                SQLRow rowEltAvoir = eltArticleAvoir.getTable().getRow(idEltFact);

                // on récupére l'article qui lui correspond
                SQLRowValues rowArticle = new SQLRowValues(eltArticle.getTable());
                for (SQLField field : eltArticle.getTable().getFields()) {
                    if (rowEltAvoir.getTable().getFieldsName().contains(field.getName())) {
                        rowArticle.put(field.getName(), rowEltAvoir.getObject(field.getName()));
                    }
                }
                // rowArticle.loadAllSafe(rowEltFact);
                int idArticle = ReferenceArticleSQLElement.getIdForCNM(rowArticle, true);

                // on crée un mouvement de stock pour chacun des articles
                SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
                SQLRowValues rowVals = new SQLRowValues(eltMvtStock.getTable());
                rowVals.put("QTE", rowEltAvoir.getInt("QTE"));
                rowVals.put("NOM", "Avoir client N°" + rowAvoir.getString("NUMERO"));
                rowVals.put("IDSOURCE", id);
                rowVals.put("SOURCE", getTable().getName());
                rowVals.put("ID_ARTICLE", idArticle);
                rowVals.put("DATE", rowAvoir.getObject("DATE"));
                try {
                    SQLRow row = rowVals.insert();
                    MouvementStockSQLElement.updateStock(Arrays.asList(row.getID()));
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }
        }
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
