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
 
 package org.openconcerto.erp.core.sales.invoice.component;

import static org.openconcerto.utils.CollectionUtils.createSet;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.AbstractArticleItemTable;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.payment.component.ModeDeReglementSQLComponent;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureSQLElement;
import org.openconcerto.erp.core.sales.invoice.report.VenteFactureXmlSheet;
import org.openconcerto.erp.core.sales.invoice.ui.SaisieVenteFactureItemTable;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.erp.core.supplychain.stock.element.StockLabel;
import org.openconcerto.erp.generationEcritures.GenerationMvtSaisieVenteFacture;
import org.openconcerto.erp.model.BanqueModifiedListener;
import org.openconcerto.erp.model.ISQLCompteSelector;
import org.openconcerto.erp.panel.PanelOOSQLComponent;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.erp.preferences.ModeReglementDefautPrefPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.UndefinedRowValuesCache;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.ISQLElementWithCodeSelector;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class SaisieVenteFactureSQLComponent extends TransfertBaseSQLComponent {
    private AbstractArticleItemTable tableFacture;
    private JLabel labelAffaire = new JLabel("Affaire");
    private DeviseField textPortHT, textAvoirTTC, textRemiseHT, fieldTTC, textTotalAvoir;
    private SQLElement factureElt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
    private SQLTable tableAvoir = Configuration.getInstance().getDirectory().getElement("AVOIR_CLIENT").getTable();
    public static final SQLTable TABLE_ADRESSE = Configuration.getInstance().getDirectory().getElement("ADRESSE").getTable();
    private SQLTable tableClient = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("CLIENT");
    private final SQLElement client = Configuration.getInstance().getDirectory().getElement(this.tableClient);
    private JUniqueTextField textNumeroUnique;
    private ElementComboBox comboClient, comboAdresse;
    private ISQLCompteSelector compteSel;
    private final SQLTable tableNum = this.factureElt.getTable().getBase().getTable("NUMEROTATION_AUTO");
    private JCheckBox checkCompteServiceAuto, checkPrevisionnelle, checkComplement, checkAcompte, checkCT;
    private PanelOOSQLComponent panelOO;
    private ElementComboBox selAvoir, selAffaire;
    private ElementSQLObject eltModeRegl;
    private static final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLRow rowPrefsCompte = SQLBackgroundTableCache.getInstance().getCacheForTable(tablePrefCompte).getRowFromId(2);
    private ISQLElementWithCodeSelector contact;
    private SQLRowAccessor rowSelected;
    private SQLElement eltContact = Configuration.getInstance().getDirectory().getElement("CONTACT");
    private JTextField refClient = new JTextField();
    private SQLRowValues defaultContactRowValues = new SQLRowValues(this.eltContact.getTable());
    protected TotalPanel totalTTC;
    // Type intervention
    private SQLTextCombo textTypeMission = new SQLTextCombo();

    private PropertyChangeListener listenerModeReglDefaut = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent arg0) {

            int idCli = SaisieVenteFactureSQLComponent.this.comboClient.getSelectedId();
            if (idCli > 1) {
                SQLRow rowCli = SaisieVenteFactureSQLComponent.this.client.getTable().getRow(idCli);

                if (SaisieVenteFactureSQLComponent.this.rowSelected == null || SaisieVenteFactureSQLComponent.this.rowSelected.getID() <= 1
                        || SaisieVenteFactureSQLComponent.this.rowSelected.getInt("ID_CLIENT") != idCli) {
                    SQLElement sqleltModeRegl = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
                    int idModeRegl = rowCli.getInt("ID_MODE_REGLEMENT");
                    if (idModeRegl > 1) {
                        SQLRow rowModeRegl = sqleltModeRegl.getTable().getRow(idModeRegl);
                        SQLRowValues rowValsModeRegl = rowModeRegl.createUpdateRow();
                        rowValsModeRegl.clearPrimaryKeys();
                        SaisieVenteFactureSQLComponent.this.eltModeRegl.setValue(rowValsModeRegl);
                    }
                }
            }
            Where w = new Where(SaisieVenteFactureSQLComponent.this.tableAvoir.getField("SOLDE"), "=", Boolean.FALSE);
            if (SaisieVenteFactureSQLComponent.this.comboClient.isEmpty()) {
                w = w.and(new Where(getTable().getBase().getTable("AVOIR_CLIENT").getField("ID_CLIENT"), "=", -1));
            } else {
                w = w.and(new Where(getTable().getBase().getTable("AVOIR_CLIENT").getField("ID_CLIENT"), "=", SaisieVenteFactureSQLComponent.this.comboClient.getSelectedId()));
            }
            if (getSelectedID() > 1) {
                SQLRow row = getTable().getRow(getSelectedID());
                w = w.or(new Where(SaisieVenteFactureSQLComponent.this.tableAvoir.getKey(), "=", row.getInt("ID_AVOIR_CLIENT")));
            }

            SaisieVenteFactureSQLComponent.this.selAvoir.getRequest().setWhere(w);
            SaisieVenteFactureSQLComponent.this.selAvoir.fillCombo();
        }
    };

    private PropertyChangeListener changeCompteListener;
    private PropertyChangeListener changeClientListener;
    private ISQLCompteSelector compteSelService;
    private JLabel labelCompteServ;
    private ElementComboBox comboCommercial;
    private ElementComboBox comboVerificateur = new ElementComboBox();;


    public SaisieVenteFactureSQLComponent() {
        super(Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE"));
    }


    private int previousClient = -1;

    public void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        this.checkPrevisionnelle = new JCheckBox();
        this.checkComplement = new JCheckBox();
        this.fieldTTC = new DeviseField();

        final ComptaPropsConfiguration comptaPropsConfiguration = ((ComptaPropsConfiguration) Configuration.getInstance());

        this.textAvoirTTC = new DeviseField();

        // Champ Module
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel addP = ComptaSQLConfElement.createAdditionalPanel();
        this.setAdditionalFieldsPanel(new FormLayouter(addP, 1));
        this.add(addP, c);

        c.gridy++;
        c.gridwidth = 1;

        /*******************************************************************************************
         * * RENSEIGNEMENTS
         ******************************************************************************************/
        // Ligne 1 : Numero de facture
        JLabel labelNum = new JLabel(getLabelFor("NUMERO"));
        labelNum.setHorizontalAlignment(SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(labelNum, c);

        this.textNumeroUnique = new JUniqueTextField(16);
        c.gridx++;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        DefaultGridBagConstraints.lockMinimumSize(this.textNumeroUnique);
            this.add(textNumeroUnique, c);

        // Date
        c.gridx++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("DATE"), SwingConstants.RIGHT), c);

        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        final JDate dateSaisie = new JDate(true);

        // listener permettant la mise à jour du numéro de facture en fonction de la date
        // sélectionnée
        dateSaisie.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!isFilling() && dateSaisie.getValue() != null) {

                    final String nextNumero = NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, dateSaisie.getValue());

                    if (textNumeroUnique.getText().trim().length() > 0 && !nextNumero.equalsIgnoreCase(textNumeroUnique.getText())) {

                        int answer = JOptionPane.showConfirmDialog(SaisieVenteFactureSQLComponent.this, "Voulez vous actualiser le numéro de la facture?", "Changement du numéro de facture",
                                JOptionPane.YES_NO_OPTION);
                        if (answer == JOptionPane.NO_OPTION) {
                            return;
                        }
                    }

                    textNumeroUnique.setText(nextNumero);

                }

            }
        });
        this.add(dateSaisie, c);

        // Ligne 2 : reference
        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        JLabel labelLibelle = new JLabel(getLabelFor("NOM"));
        labelLibelle.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(labelLibelle, c);

        SQLTextCombo textLibelle = new SQLTextCombo();
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        this.add(textLibelle, c);

        this.addSQLObject(textLibelle, "NOM");
        c.fill = GridBagConstraints.HORIZONTAL;
        this.comboCommercial = new ElementComboBox(false);
        // Commercial
        String field;
            field = "ID_COMMERCIAL";
        c.gridx++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor(field), SwingConstants.RIGHT), c);

        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;

        this.add(this.comboCommercial, c);
        this.addRequiredSQLObject(this.comboCommercial, field);
        // Client
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("ID_CLIENT"), SwingConstants.RIGHT), c);

        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.comboClient = new ElementComboBox();

        this.add(this.comboClient, c);

        if (getTable().contains("ID_ECHEANCIER_CCI")) {
            // Echeancier
            c.gridx++;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(new JLabel(getLabelFor("ID_ECHEANCIER_CCI"), SwingConstants.RIGHT), c);

            c.gridx++;
            c.weightx = 1;
            c.fill = GridBagConstraints.NONE;
            final ElementComboBox echeancier = new ElementComboBox();
            final SQLElement contactElement = Configuration.getInstance().getDirectory().getElement("ECHEANCIER_CCI");
            echeancier.init(contactElement, contactElement.getComboRequest(true));
            DefaultGridBagConstraints.lockMinimumSize(echeancier);
            this.addView(echeancier, "ID_ECHEANCIER_CCI");

            selAffaire.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent arg0) {
                    // TODO Raccord de méthode auto-généré
                    if (selAffaire.getSelectedRow() != null) {
                        echeancier.getRequest().setWhere(new Where(contactElement.getTable().getField("ID_AFFAIRE"), "=", selAffaire.getSelectedRow().getID()));

                        if (!isFilling()) {
                            SQLRow rowPole = selAffaire.getSelectedRow().getForeignRow("ID_POLE_PRODUIT");
                            comboCommercial.setValue(rowPole);
                        }
                    } else {
                        echeancier.getRequest().setWhere(null);
                    }
                }
            });
            this.add(echeancier, c);

        }

        this.comboClient.addValueListener(this.changeClientListener);

        this.comboAdresse = new ElementComboBox();
        this.comboAdresse.setAddIconVisible(false);
        this.comboAdresse.setListIconVisible(false);
            JLabel labelAdresse = new JLabel(getLabelFor("ID_ADRESSE"), SwingConstants.RIGHT);
            c.gridy++;
            c.gridx = 0;
            c.gridwidth = 1;
            c.weightx = 0;
            labelAdresse.setHorizontalAlignment(SwingConstants.RIGHT);
            this.add(labelAdresse, c);

            c.gridx++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 1;
            this.add(this.comboAdresse, c);

        // Acompte
        this.checkAcompte = new JCheckBox(getLabelFor("ACOMPTE"));
        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        // this.add(this.checkAcompte, c);
        c.gridwidth = 1;
        this.addView(this.checkAcompte, "ACOMPTE");

        Boolean bCompteCli = Boolean.valueOf(DefaultNXProps.getInstance().getStringProperty("HideCompteFacture"));
        if (!bCompteCli) {
            // Ligne 5: Compte Client
            c.gridy++;
            c.gridx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridwidth = 1;
            c.weightx = 0;
            this.add(new JLabel("Compte", SwingConstants.RIGHT), c);

            c.gridx++;
            c.weightx = 1;
            this.compteSel = new ISQLCompteSelector();
            this.compteSel.init();
            c.gridwidth = 3;
            this.add(this.compteSel, c);
            this.compteSel.addValueListener(this.changeCompteListener);

        }


        // Compte Service
        this.checkCompteServiceAuto = new JCheckBox(getLabelFor("COMPTE_SERVICE_AUTO"));
        this.addSQLObject(this.checkCompteServiceAuto, "COMPTE_SERVICE_AUTO");
        this.compteSelService = new ISQLCompteSelector();

        this.labelCompteServ = new JLabel("Compte Service");
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        this.labelCompteServ.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(this.labelCompteServ, c);

        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        this.add(this.compteSelService, c);

        String valServ = DefaultNXProps.getInstance().getStringProperty("ArticleService");
        Boolean bServ = Boolean.valueOf(valServ);

        this.checkCompteServiceAuto.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCompteServiceVisible(!SaisieVenteFactureSQLComponent.this.checkCompteServiceAuto.isSelected());
            }
        });

        setCompteServiceVisible(!(bServ != null && !bServ.booleanValue()));


        final JPanel pAcompte = new JPanel();
        final DeviseField textAcompteHT = new DeviseField();
        pAcompte.add(new JLabel("Acompte HT"));
        pAcompte.add(textAcompteHT);

        pAcompte.add(new JLabel("soit"));
        final JTextField textAcompte = new JTextField(5);
        pAcompte.add(textAcompte);
        pAcompte.add(new JLabel("%"));
        c.gridx = 0;
        c.gridy++;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        this.add(pAcompte, c);
        c.anchor = GridBagConstraints.WEST;
        this.addView(textAcompte, "POURCENT_ACOMPTE");

        pAcompte.setVisible(false);

        /*******************************************************************************************
         * * DETAILS
         ******************************************************************************************/
            this.tableFacture = new SaisieVenteFactureItemTable();

        final ElementComboBox boxTarif = new ElementComboBox();
        if (this.getTable().getFieldsName().contains("ID_TARIF")) {
            // TARIF
            c.gridy++;
            c.gridx = 0;
            c.weightx = 0;
            c.weighty = 0;
            c.gridwidth = 1;
            this.add(new JLabel("Tarif à appliquer", SwingConstants.RIGHT), c);
            c.gridx++;
            c.gridwidth = GridBagConstraints.REMAINDER;

            c.weightx = 1;
            this.add(boxTarif, c);
            this.addView(boxTarif, "ID_TARIF");
            boxTarif.addModelListener("wantedID", new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    SQLRow selectedRow = boxTarif.getRequest().getPrimaryTable().getRow(boxTarif.getWantedID());
                    tableFacture.setTarif(selectedRow, false);
                }
            });
        }
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;

        ITextArea infos = new ITextArea(4, 4);
            this.add(this.tableFacture, c);

        // FIXME
        this.addView(this.tableFacture.getRowValuesTable(), "");

        /*******************************************************************************************
         * * MODE DE REGLEMENT
         ******************************************************************************************/
        JPanel panelBottom = new JPanel(new GridBagLayout());
        GridBagConstraints cBottom = new DefaultGridBagConstraints();
        cBottom.weightx = 1;
        cBottom.anchor = GridBagConstraints.NORTHWEST;
        // Mode de règlement
        this.addView("ID_MODE_REGLEMENT", REQ + ";" + DEC + ";" + SEP);
        this.eltModeRegl = (ElementSQLObject) this.getView("ID_MODE_REGLEMENT");
        panelBottom.add(this.eltModeRegl, cBottom);

        /*******************************************************************************************
         * * FRAIS DE PORT ET REMISE
         ******************************************************************************************/
        JPanel panelFrais = new JPanel();
        panelFrais.setLayout(new GridBagLayout());

        final GridBagConstraints cFrais = new DefaultGridBagConstraints();

        this.textPortHT = new DeviseField(5);
        DefaultGridBagConstraints.lockMinimumSize(textPortHT);
        addSQLObject(this.textPortHT, "PORT_HT");
        this.textRemiseHT = new DeviseField(5);
        DefaultGridBagConstraints.lockMinimumSize(textRemiseHT);
        addSQLObject(this.textRemiseHT, "REMISE_HT");

        // Frais de port
        cFrais.gridheight = 1;
        cFrais.gridx = 1;
        SQLRequestComboBox boxTaxePort = new SQLRequestComboBox(false, 8);
        if (getTable().contains("ID_TAXE_PORT")) {

            JLabel labelPortHT = new JLabel(getLabelFor("PORT_HT"));
            labelPortHT.setHorizontalAlignment(SwingConstants.RIGHT);
            cFrais.gridy++;
            panelFrais.add(labelPortHT, cFrais);
            cFrais.gridx++;
            panelFrais.add(this.textPortHT, cFrais);

            JLabel labelTaxeHT = new JLabel(getLabelFor("ID_TAXE_PORT"));
            labelTaxeHT.setHorizontalAlignment(SwingConstants.RIGHT);
            cFrais.gridx = 1;
            cFrais.gridy++;
            panelFrais.add(labelTaxeHT, cFrais);
            cFrais.gridx++;
            panelFrais.add(boxTaxePort, cFrais);
            this.addView(boxTaxePort, "ID_TAXE_PORT", REQ);

            boxTaxePort.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    // TODO Raccord de méthode auto-généré
                    totalTTC.updateTotal();
                }
            });
        }

        // Remise
        JLabel labelRemiseHT = new JLabel(getLabelFor("REMISE_HT"));
        labelRemiseHT.setHorizontalAlignment(SwingConstants.RIGHT);
        cFrais.gridy++;
        cFrais.gridx = 1;
        panelFrais.add(labelRemiseHT, cFrais);
        cFrais.gridx++;
        panelFrais.add(this.textRemiseHT, cFrais);
        cFrais.gridy++;

        cBottom.gridx++;
        cBottom.weightx = 1;
        cBottom.anchor = GridBagConstraints.NORTHEAST;
        cBottom.fill = GridBagConstraints.NONE;
        panelBottom.add(panelFrais, cBottom);

        /*******************************************************************************************
         * * CALCUL DES TOTAUX
         ******************************************************************************************/
        DeviseField fieldHT = new DeviseField();
        final DeviseField fieldTVA = new DeviseField();
        DeviseField fieldService = new DeviseField();
        DeviseField fieldTHA = new DeviseField();

        DeviseField fieldDevise = null;
        if (getTable().getFieldsName().contains("T_DEVISE")) {
            fieldDevise = new DeviseField();
            addSQLObject(fieldDevise, "T_DEVISE");
        }
        // FIXME was required but not displayed for KD
        addSQLObject(fieldTHA, "T_HA");
        addRequiredSQLObject(fieldHT, "T_HT");
        addRequiredSQLObject(fieldTVA, "T_TVA");
        addRequiredSQLObject(this.fieldTTC, "T_TTC");
        addRequiredSQLObject(fieldService, "T_SERVICE");
        JTextField poids = new JTextField();
        addSQLObject(poids, "T_POIDS");
        totalTTC = new TotalPanel(this.tableFacture, fieldHT, fieldTVA, this.fieldTTC, this.textPortHT, this.textRemiseHT, fieldService, fieldTHA, fieldDevise, poids, null, (getTable().contains(
                "ID_TAXE_PORT") ? boxTaxePort : null));
        DefaultGridBagConstraints.lockMinimumSize(totalTTC);
        cBottom.gridx++;
        cBottom.weightx = 1;
        cBottom.anchor = GridBagConstraints.EAST;
        cBottom.fill = GridBagConstraints.HORIZONTAL;
        panelBottom.add(totalTTC, cBottom);

        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.weighty = 0;
        this.add(panelBottom, c);

        // Ligne : Avoir
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(createPanelAvoir(), c);

        // Infos
            c.gridy++;
            c.gridx = 0;
            c.gridwidth = 4;
            c.fill = GridBagConstraints.BOTH;
            this.add(new TitledSeparator(getLabelFor("INFOS")), c);

            c.gridy++;

            final JScrollPane comp = new JScrollPane(infos);
            infos.setBorder(null);
            DefaultGridBagConstraints.lockMinimumSize(comp);
            this.add(comp, c);

        this.panelOO = new PanelOOSQLComponent(this);
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHEAST;
        this.add(this.panelOO, c);

        this.addSQLObject(this.textAvoirTTC, "T_AVOIR_TTC");

        this.addRequiredSQLObject(dateSaisie, "DATE");
        this.addRequiredSQLObject(this.comboClient, "ID_CLIENT");
            this.addSQLObject(this.comboAdresse, "ID_ADRESSE");
        this.addRequiredSQLObject(this.textNumeroUnique, "NUMERO");
        this.addSQLObject(infos, "INFOS");
        this.addSQLObject(this.checkPrevisionnelle, "PREVISIONNELLE");
        this.addSQLObject(this.checkComplement, "COMPLEMENT");
        this.addSQLObject(this.selAvoir, "ID_AVOIR_CLIENT");
        this.addSQLObject(this.compteSelService, "ID_COMPTE_PCE_SERVICE");
        final SQLTable tableBanque;
        ModeDeReglementSQLComponent modeReglComp;
        modeReglComp = (ModeDeReglementSQLComponent) this.eltModeRegl.getSQLChild();
        this.selAvoir.getRequest().setWhere(new Where(this.tableAvoir.getField("SOLDE"), "=", Boolean.FALSE));
        this.selAvoir.fillCombo();

        // Selection du compte de service
        int idCompteVenteService = rowPrefsCompte.getInt("ID_COMPTE_PCE_VENTE_SERVICE");
        if (idCompteVenteService <= 1) {
            try {
                idCompteVenteService = ComptePCESQLElement.getIdComptePceDefault("VentesServices");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.compteSelService.setValue(idCompteVenteService);

        // Lock

        DefaultGridBagConstraints.lockMinimumSize(this.comboClient);
        DefaultGridBagConstraints.lockMinimumSize(this.comboCommercial);
        DefaultGridBagConstraints.lockMinimumSize(this.comboAdresse);

        // Listeners

        this.comboClient.addValueListener(this.listenerModeReglDefaut);
        this.fieldTTC.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                refreshText();
            }

        });

        this.selAvoir.addValueListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                refreshText();
            }

        });

        this.checkAcompte.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {

                pAcompte.setVisible(SaisieVenteFactureSQLComponent.this.checkAcompte.isSelected());
            }
        });

        this.changeClientListener = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {

                if (SaisieVenteFactureSQLComponent.this.comboClient.getValue() != null) {
                    Integer id = SaisieVenteFactureSQLComponent.this.comboClient.getValue();

                    SaisieVenteFactureSQLComponent.this.defaultContactRowValues.put("ID_CLIENT", id);
                    if (id > 1) {

                        SQLRow row = SaisieVenteFactureSQLComponent.this.client.getTable().getRow(id);

                        if (SaisieVenteFactureSQLComponent.this.contact != null) {
                            Where w = new Where(SaisieVenteFactureSQLComponent.this.eltContact.getTable().getField("ID_CLIENT"), "=", SQLRow.NONEXISTANT_ID);
                                w = w.or(new Where(SaisieVenteFactureSQLComponent.this.eltContact.getTable().getField("ID_CLIENT"), "=", id));
                            SaisieVenteFactureSQLComponent.this.contact.setWhereOnRequest(w);
                        }
                            if (SaisieVenteFactureSQLComponent.this.comboAdresse != null) {

                                Where w = new Where(SaisieVenteFactureSQLComponent.TABLE_ADRESSE.getKey(), "=", row.getInt("ID_ADRESSE"));

                                w = w.or(new Where(SaisieVenteFactureSQLComponent.TABLE_ADRESSE.getKey(), "=", row.getInt("ID_ADRESSE_L")));
                                w = w.or(new Where(SaisieVenteFactureSQLComponent.TABLE_ADRESSE.getKey(), "=", row.getInt("ID_ADRESSE_F")));

                                SQLRow rowCli = row;

                                w = w.or(new Where(SaisieVenteFactureSQLComponent.TABLE_ADRESSE.getField("ID_CLIENT"), "=", rowCli.getID()));

                                SaisieVenteFactureSQLComponent.this.comboAdresse.getRequest().setWhere(w);
                            }
                    } else {
                            if (SaisieVenteFactureSQLComponent.this.comboAdresse != null) {
                                SaisieVenteFactureSQLComponent.this.comboAdresse.getRequest().setWhere(null);
                            }
                    }
                    SaisieVenteFactureSQLComponent.this.previousClient = id;
                }

            }
        };

        this.changeCompteListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                SQLSelect sel = new SQLSelect(getTable().getBase());
                sel.addSelect(SaisieVenteFactureSQLComponent.this.client.getTable().getKey());
                Where where = new Where(SaisieVenteFactureSQLComponent.this.client.getTable().getField("ID_COMPTE_PCE"), "=", SaisieVenteFactureSQLComponent.this.compteSel.getValue());
                sel.setWhere(where);

                String req = sel.asString();
                List l = getTable().getBase().getDataSource().execute(req);
                if (l != null) {
                    if (l.size() == 1) {
                        Map<String, Object> m = (Map<String, Object>) l.get(0);
                        Object o = m.get(SaisieVenteFactureSQLComponent.this.client.getTable().getKey().getName());
                        System.err.println("Only one value match :: " + o);
                        if (o != null) {
                            SaisieVenteFactureSQLComponent.this.comboClient.setValue(Integer.valueOf(((Number) o).intValue()));
                        }
                    }
                }
            }
        };
        this.textPortHT.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                totalTTC.updateTotal();
            }
        });

        this.textRemiseHT.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                totalTTC.updateTotal();
            }
        });
        this.comboClient.addValueListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (SaisieVenteFactureSQLComponent.this.isFilling())
                    return;
                final SQLRow row = ((SQLRequestComboBox) evt.getSource()).getSelectedRow();
                if (row != null) {
                    SaisieVenteFactureSQLComponent.this.defaultContactRowValues.put("ID_CLIENT", row.getIDNumber());
                    if (SaisieVenteFactureSQLComponent.this.client.getTable().contains("ID_TARIF")) {
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

                    int idCpt = row.getInt("ID_COMPTE_PCE");

                    if (idCpt <= 1) {
                        // Select Compte client par defaut
                        idCpt = rowPrefsCompte.getInt("ID_COMPTE_PCE_CLIENT");
                        if (idCpt <= 1) {
                            try {
                                idCpt = ComptePCESQLElement.getIdComptePceDefault("Clients");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (SaisieVenteFactureSQLComponent.this.compteSel != null) {
                        Integer i = SaisieVenteFactureSQLComponent.this.compteSel.getValue();
                        if (i == null || i.intValue() != idCpt) {
                            SaisieVenteFactureSQLComponent.this.compteSel.setValue(idCpt);
                        }
                    }
                }

            }
        });
    }

    private JPanel createPanelAvoir() {
        JPanel panelAvoir = new JPanel(new GridBagLayout());
        panelAvoir.setOpaque(false);
        GridBagConstraints cA = new DefaultGridBagConstraints();
        JLabel labelAvoir = new JLabel(getLabelFor("ID_AVOIR_CLIENT"));
        labelAvoir.setHorizontalAlignment(SwingConstants.RIGHT);
        cA.weightx = 1;
        labelAvoir.setHorizontalAlignment(SwingConstants.RIGHT);
        panelAvoir.add(labelAvoir, cA);
        cA.weightx = 0;
        cA.gridx++;
        this.selAvoir = new ElementComboBox();
        this.selAvoir.setAddIconVisible(false);
        panelAvoir.add(this.selAvoir, cA);
        final JLabel labelTotalAvoir = new JLabel("Total à régler");
        this.textTotalAvoir = new DeviseField();
        this.textTotalAvoir.setEditable(false);
        cA.gridx++;
        cA.weightx = 0;
        panelAvoir.add(labelTotalAvoir, cA);
        cA.gridx++;
        cA.weightx = 0;
        panelAvoir.add(this.textTotalAvoir, cA);
        this.textTotalAvoir.setHorizontalAlignment(SwingConstants.RIGHT);

        return panelAvoir;
    }

    private void setCompteServiceVisible(boolean b) {
        this.compteSelService.setVisible(b);
        this.labelCompteServ.setVisible(b);
    }

    private void refreshText() {
        Number n = this.fieldTTC.getValue();
        if (this.selAvoir.getSelectedId() > 1) {
            SQLTable tableAvoir = Configuration.getInstance().getDirectory().getElement("AVOIR_CLIENT").getTable();
            if (n != null) {
                long ttc = n.longValue();
                SQLRow rowAvoir = tableAvoir.getRow(this.selAvoir.getSelectedId());
                long totalAvoir = ((Number) rowAvoir.getObject("MONTANT_TTC")).longValue();
                totalAvoir -= ((Number) rowAvoir.getObject("MONTANT_SOLDE")).longValue();
                if (getSelectedID() > 1) {
                    SQLRow row = getTable().getRow(getSelectedID());
                    int idAvoirOld = row.getInt("ID_AVOIR_CLIENT");
                    if (idAvoirOld == rowAvoir.getID()) {
                        totalAvoir += Long.valueOf(row.getObject("T_AVOIR_TTC").toString());
                    }
                }

                long l = ttc - totalAvoir;
                if (l < 0) {
                    l = 0;
                    this.textAvoirTTC.setValue(ttc);
                } else {
                    this.textAvoirTTC.setValue(totalAvoir);
                }
                this.textTotalAvoir.setValue(l);

            } else {
                this.textTotalAvoir.setValue(0l);
            }
        } else {
            if (n != null) {
                this.textTotalAvoir.setValue(n.longValue());
            } else {
                this.textTotalAvoir.setValue(0l);
            }
            this.textAvoirTTC.setValue(0l);
        }
    }

    public int insert(SQLRow order) {
        return commit(order);
    }

    private void createCompteServiceAuto(int id) {
        SQLRow rowPole = this.comboCommercial.getSelectedRow();
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
    public void select(SQLRowAccessor r) {
        if (compteSel != null) {
            this.compteSel.rmValueListener(this.changeCompteListener);
        }
        if (this.comboClient != null)
            this.comboClient.rmValueListener(this.listenerModeReglDefaut);
        this.rowSelected = r;
        if (r != null) {
            this.textNumeroUnique.setIdSelected(r.getID());

        }
            super.select(r);

        if (r != null) {
            // this.tableFacture.getModel().clearRows();
            // this.tableFacture.insertFrom("ID_SAISIE_VENTE_FACTURE", r.getID());
            Boolean b = (Boolean) r.getObject("ACOMPTE");
            if (b != null) {
                setAcompte(b);
            } else {
                setAcompte(false);
            }
        }
        if (this.comboClient != null) {
            this.comboClient.addValueListener(this.listenerModeReglDefaut);
            this.comboClient.addValueListener(this.changeClientListener);
        }
        if (this.compteSel != null) {
            this.compteSel.addValueListener(this.changeCompteListener);
        } // nomClient.addValueListener(changeClientListener);
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

    public int commit(SQLRow order) {

        int idSaisieVF = -1;
        long lFactureOld = 0;
        SQLRow rowFactureOld = null;
        SQLRow rowFacture = null;
        SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
        if (this.textNumeroUnique.checkValidation()) {
            try {
                if (getMode() == Mode.INSERTION) {
                    idSaisieVF = super.insert(order);
                    rowFacture = getTable().getRow(idSaisieVF);
                    // incrémentation du numéro auto
                    if (NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, rowFacture.getDate("DATE").getTime()).equalsIgnoreCase(this.textNumeroUnique.getText().trim())) {
                        SQLRowValues rowVals = new SQLRowValues(this.tableNum);

                        String labelNumberFor = NumerotationAutoSQLElement.getLabelNumberFor(SaisieVenteFactureSQLElement.class);
                        int val = this.tableNum.getRow(2).getInt(labelNumberFor);
                        val++;
                        rowVals.put(labelNumberFor, Integer.valueOf(val));
                        rowVals.update(2);
                    }
                } else {
                    if (JOptionPane.showConfirmDialog(this, "Attention en modifiant cette facture, vous supprimerez les chéques et les échéances associés. Continuer?", "Modification de facture",
                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        SQLPreferences prefs = SQLPreferences.getMemCached(getTable().getDBRoot());
                        if (prefs.getBoolean(GestionArticleGlobalPreferencePanel.STOCK_FACT, true)) {
                            // On efface les anciens mouvements de stocks
                            SQLSelect sel = new SQLSelect();
                            sel.addSelect(eltMvtStock.getTable().getField("ID"));
                            Where w = new Where(eltMvtStock.getTable().getField("IDSOURCE"), "=", getSelectedID());
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
                        }
                        // On recupere l'ancien total HT
                        rowFactureOld = this.getTable().getRow(getSelectedID());
                        lFactureOld = ((Number) rowFactureOld.getObject("T_HT")).longValue();

                        super.update();

                        idSaisieVF = getSelectedID();
                    } else {
                        // Annulation par l'utilisateur
                        return idSaisieVF;
                    }
                }

                rowFacture = getTable().getRow(idSaisieVF);
                final ComptaPropsConfiguration comptaPropsConfiguration = ((ComptaPropsConfiguration) Configuration.getInstance());

                // Mise à jour des tables liées
                this.tableFacture.updateField("ID_SAISIE_VENTE_FACTURE", idSaisieVF);


                createDocument(rowFacture);

                int idMvt = -1;
                if (getMode() == Mode.MODIFICATION) {

                    idMvt = rowFacture.getInt("ID_MOUVEMENT");

                    // on supprime tout ce qui est lié à la facture
                    System.err.println("Archivage des fils");
                    EcritureSQLElement eltEcr = (EcritureSQLElement) Configuration.getInstance().getDirectory().getElement("ECRITURE");
                    eltEcr.archiveMouvementProfondeur(idMvt, false);
                }

                if (!this.checkPrevisionnelle.isSelected()) {
                    System.err.println("Regeneration des ecritures");
                    if (idMvt > 1) {
                        new GenerationMvtSaisieVenteFacture(idSaisieVF, idMvt);
                    } else {
                        new GenerationMvtSaisieVenteFacture(idSaisieVF);
                    }
                    System.err.println("Fin regeneration");

                    // Mise à jour des stocks

                    updateStock(idSaisieVF);

                    // On retire l'avoir
                    if (rowFactureOld != null && rowFactureOld.getInt("ID_AVOIR_CLIENT") > 1) {

                        SQLRow rowAvoir = rowFactureOld.getForeignRow("ID_AVOIR_CLIENT");

                        Long montantSolde = (Long) rowAvoir.getObject("MONTANT_SOLDE");
                        Long avoirTTC = (Long) rowFactureOld.getObject("T_AVOIR_TTC");

                        long montant = montantSolde - avoirTTC;
                        if (montant < 0) {
                            montant = 0;
                        }

                        SQLRowValues rowVals = rowAvoir.createEmptyUpdateRow();

                        // Soldé
                        rowVals.put("SOLDE", Boolean.FALSE);
                        rowVals.put("MONTANT_SOLDE", montant);
                        Long restant = (Long) rowAvoir.getObject("MONTANT_TTC") - montantSolde;
                        rowVals.put("MONTANT_RESTANT", restant);

                        rowVals.update();

                    }

                    final int idAvoir = rowFacture.getInt("ID_AVOIR_CLIENT");
                    // on solde l'avoir
                    if (idAvoir > 1) {

                        SQLRow rowAvoir = rowFacture.getForeignRow("ID_AVOIR_CLIENT");

                        Long montantTTC = (Long) rowAvoir.getObject("MONTANT_TTC");
                        Long montantSolde = (Long) rowAvoir.getObject("MONTANT_SOLDE");
                        Long factTTC = (Long) rowFacture.getObject("T_TTC");

                        long restant = montantTTC - montantSolde;

                        SQLRowValues rowVals = rowAvoir.createEmptyUpdateRow();
                        final long l2 = factTTC - restant;
                        // Soldé
                        if (l2 >= 0) {
                            rowVals.put("SOLDE", Boolean.TRUE);
                            rowVals.put("MONTANT_SOLDE", montantTTC);
                            rowVals.put("MONTANT_RESTANT", 0);
                        } else {
                            // Il reste encore de l'argent pour l'avoir
                            final long m = montantSolde + factTTC;
                            rowVals.put("MONTANT_SOLDE", m);
                            rowVals.put("MONTANT_RESTANT", montantTTC - m);
                        }

                        rowVals.update();

                    }


                }
            } catch (Exception e) {
                ExceptionHandler.handle("", e);
            }
        } else {
            ExceptionHandler.handle("Impossible d'ajouter, numéro de facture existant.");
            Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
        }
        return idSaisieVF;
    }

    public void createDocument(SQLRow row) {
        // generation du document
        final VenteFactureXmlSheet sheet = new VenteFactureXmlSheet(row);

        try {
            sheet.createDocumentAsynchronous();
            sheet.showPrintAndExportAsynchronous(panelOO.isVisualisationSelected(), panelOO.isImpressionSelected(), true);
        } catch (Exception e) {
            ExceptionHandler.handle("Impossible de générer la facture", e);
        }
    }

    @Override
    public void update() {
        commit(null);
    }

    /**
     * Création d'une facture à partir d'un devis
     * 
     * @param idDevis
     * 
     */
    public void loadDevis(int idDevis) {

        SQLElement devis = Configuration.getInstance().getDirectory().getElement("DEVIS");
        SQLElement devisElt = Configuration.getInstance().getDirectory().getElement("DEVIS_ELEMENT");

        if (idDevis > 1) {
            SQLInjector injector = SQLInjector.getInjector(devis.getTable(), this.getTable());
            SQLRow rowDevis = devis.getTable().getRow(idDevis);
            SQLRowValues createRowValuesFrom = injector.createRowValuesFrom(idDevis);
            String string = rowDevis.getString("OBJET");
            createRowValuesFrom.put("NOM", string + (string.trim().length() == 0 ? "" : ",") + rowDevis.getString("NUMERO"));
            this.select(createRowValuesFrom);
        }

            loadItem(this.tableFacture, devis, idDevis, devisElt);
    }

    /**
     * Création d'une facture à partir d'une facture existante
     * 
     * @param idFacture
     * 
     */
    public void loadFactureExistante(int idFacture) {

        SQLElement fact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        SQLElement factElt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");

        // On duplique la facture
        if (idFacture > 1) {
            SQLRow row = fact.getTable().getRow(idFacture);
            SQLRowValues rowVals = new SQLRowValues(fact.getTable());
            rowVals.put("ID_CLIENT", row.getInt("ID_CLIENT"));
            rowVals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, new Date()));
            rowVals.put("NOM", row.getObject("NOM"));
            this.select(rowVals);
        }

        // On duplique les elements de facture
        List<SQLRow> myListItem = fact.getTable().getRow(idFacture).getReferentRows(factElt.getTable());

        if (myListItem.size() != 0) {
            this.tableFacture.getModel().clearRows();

            for (SQLRow rowElt : myListItem) {

                SQLRowValues rowVals = rowElt.createUpdateRow();
                rowVals.clearPrimaryKeys();
                this.tableFacture.getModel().addRow(rowVals);
                int rowIndex = this.tableFacture.getModel().getRowCount() - 1;
                this.tableFacture.getModel().fireTableModelModified(rowIndex);
            }
        } else {
            this.tableFacture.getModel().clearRows();
        }
        this.tableFacture.getModel().fireTableDataChanged();
        this.tableFacture.repaint();
    }

    public List<SQLRowValues> createFactureAcompte(int idFacture, long acompte) {

        SQLElement fact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        SQLElement factElt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");

        // On duplique la facture
        if (idFacture > 1) {
            SQLRow row = fact.getTable().getRow(idFacture);
            SQLRowValues rowVals = new SQLRowValues(fact.getTable());
            rowVals.put("ID_CLIENT", row.getInt("ID_CLIENT"));
            rowVals.put("ID_AFFAIRE", row.getInt("ID_AFFAIRE"));
            rowVals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, new Date()));
            rowVals.put("NOM", "Acompte de " + GestionDevise.currencyToString(acompte) + "€");
            this.select(rowVals);
        }

        // On duplique les elements de facture
        List<SQLRow> myListItem = fact.getTable().getRow(idFacture).getReferentRows(factElt.getTable());
        List<SQLRowValues> result = new ArrayList<SQLRowValues>(myListItem.size());
        if (myListItem.size() != 0) {
            this.tableFacture.getModel().clearRows();

            double acc = ((double) acompte / (double) myListItem.size());
            long toAdd = 0;
            SQLTable tablePourcentCCIP = Configuration.getInstance().getRoot().findTable("POURCENT_CCIP");
            for (SQLRow rowElt : myListItem) {

                SQLRowValues rowValsUp = rowElt.createUpdateRow();
                SQLRowValues rowVals = rowElt.createUpdateRow();
                rowVals.clearPrimaryKeys();

                long val = rowVals.getLong("T_PV_HT");
                double value = (acc + toAdd) / val * 100.0;
                // Si l'acompte est supérieur au montant
                if (value > 100) {
                    value = 100;
                    toAdd += (acc - val);
                } else {
                    toAdd = 0;
                }
                BigDecimal pourcentAcompte = new BigDecimal(rowValsUp.getLong("POURCENT_ACOMPTE") - value);
                rowValsUp.put("POURCENT_ACOMPTE", pourcentAcompte);
                BigDecimal pourcentCurrentAcompte = new BigDecimal(value);
                rowVals.put("POURCENT_ACOMPTE", pourcentCurrentAcompte);
                List<SQLRow> rowsCCIP = rowElt.getReferentRows(tablePourcentCCIP);
                if (rowsCCIP.size() > 0) {
                    SQLRowValues rowValsCCIP = rowsCCIP.get(0).createUpdateRow();
                    rowValsCCIP.clearPrimaryKeys();
                    rowValsCCIP.put("ID_SAISIE_VENTE_FACTURE_ELEMENT", rowVals);
                    rowValsCCIP.put("NOM", "Acompte");
                    rowValsCCIP.put("POURCENT", pourcentCurrentAcompte);
                }
                System.err.println(value);
                this.tableFacture.getModel().addRow(rowVals);
                int rowIndex = this.tableFacture.getModel().getRowCount() - 1;
                this.tableFacture.getModel().fireTableModelModified(rowIndex);
            }
            if (toAdd > 0) {
                for (int i = 0; i < this.tableFacture.getModel().getRowCount() && toAdd > 0; i++) {
                    SQLRowValues rowVals = this.tableFacture.getModel().getRowValuesAt(i);
                    if (rowVals.getFloat("POURCENT_ACOMPTE") < 100) {
                        long val = rowVals.getLong("T_PV_HT");
                        double value = (acc + toAdd) / val * 100.0;
                        // Si l'acompte est supérieur au montant
                        if (value > 100) {
                            value = 100;
                            toAdd += (acc - val);
                        } else {
                            toAdd = 0;
                        }
                        rowVals.put("POURCENT_ACOMPTE", new BigDecimal(value));
                        this.tableFacture.getModel().fireTableModelModified(i);
                    }
                }
            }

            // FIXME Check total if pb with round
        } else {
            this.tableFacture.getModel().clearRows();
        }
        this.tableFacture.getModel().fireTableDataChanged();
        this.tableFacture.repaint();
        return result;
    }

    /**
     * Création d'une facture à partir d'une commande
     * 
     * @param idCmd
     * 
     */
    public void loadCommande(int idCmd) {

        SQLElement cmd = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
        SQLElement cmdElt = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT_ELEMENT");

        if (idCmd > 1) {
            SQLInjector injector = SQLInjector.getInjector(cmd.getTable(), this.getTable());
            SQLRow rowCmd = cmd.getTable().getRow(idCmd);
            SQLRowValues createRowValuesFrom = injector.createRowValuesFrom(idCmd);
            String string = rowCmd.getString("NOM");
            createRowValuesFrom.put("NOM", string + (string.trim().length() == 0 ? "" : ",") + rowCmd.getString("NUMERO"));
            this.select(createRowValuesFrom);
            this.listenerModeReglDefaut.propertyChange(null);
        }
        loadItem(this.tableFacture, cmd, idCmd, cmdElt);
    }

    /**
     * Création d'une facture à partir d'un bon de livraison
     * 
     * @param idBl
     * 
     */
    public void loadBonItems(SQLRowAccessor rowBL, boolean clear) {

        SQLElement bon = Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON");
        SQLElement bonElt = Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON_ELEMENT");

        loadItem(this.tableFacture, bon, rowBL.getID(), bonElt, clear);
    }

    public void addRowItem(SQLRowValues row) {
        this.tableFacture.getModel().addRow(row);
    }

    private static final SQLElement eltModeReglement = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");

    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues vals = new SQLRowValues(this.getTable());
        SQLRow r;

        try {
            r = ModeReglementDefautPrefPanel.getDefaultRow(true);
            if (r.getID() > 1) {
                SQLRowValues rowVals = eltModeReglement.createCopy(r, null);
                System.err.println(rowVals.getInt("ID_TYPE_REGLEMENT"));
                vals.put("ID_MODE_REGLEMENT", rowVals);
            }
        } catch (SQLException e) {
            System.err.println("Impossible de sélectionner le mode de règlement par défaut du client.");
            e.printStackTrace();
        }
        this.tableFacture.getModel().clearRows();

        // User
        // SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());
        SQLElement eltComm = Configuration.getInstance().getDirectory().getElement("COMMERCIAL");
        int idUser = UserManager.getInstance().getCurrentUser().getId();

        // sel.addSelect(eltComm.getTable().getKey());
        // sel.setWhere(new Where(eltComm.getTable().getField("ID_USER_COMMON"), "=", idUser));
        // List<SQLRow> rowsComm = (List<SQLRow>)
        // Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), new
        // SQLRowListRSH(eltComm.getTable()));

        SQLRow rowsComm = SQLBackgroundTableCache.getInstance().getCacheForTable(eltComm.getTable()).getFirstRowContains(idUser, eltComm.getTable().getField("ID_USER_COMMON"));

        if (rowsComm != null) {
            vals.put("ID_COMMERCIAL", rowsComm.getID());
        }

        // User
        final ComptaPropsConfiguration comptaPropsConfiguration = ((ComptaPropsConfiguration) Configuration.getInstance());
        vals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, new Date()));
        int idCompteVenteProduit = rowPrefsCompte.getInt("ID_COMPTE_PCE_VENTE_PRODUIT");
        if (idCompteVenteProduit <= 1) {
            try {
                idCompteVenteProduit = ComptePCESQLElement.getIdComptePceDefault("VentesProduits");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        vals.put("ID_COMPTE_PCE_VENTE", idCompteVenteProduit);
        if (this.checkCT != null) {
            vals.put("CONTROLE_TECHNIQUE", this.checkCT.isSelected());
        }

        int idCompteVenteService = rowPrefsCompte.getInt("ID_COMPTE_PCE_VENTE_SERVICE");
        if (idCompteVenteService <= 1) {
            try {
                idCompteVenteService = ComptePCESQLElement.getIdComptePceDefault("VentesServices");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (getTable().contains("ID_TAXE_PORT")) {
            Integer idFromTaux = TaxeCache.getCache().getIdFromTaux(19.6F);
            if (idFromTaux != null) {
                vals.put("ID_TAXE_PORT", idFromTaux);
            }
        }
        vals.put("ID_COMPTE_PCE_SERVICE", idCompteVenteService);
        System.err.println("Defaults " + vals);
        return vals;
    }

    public void setDefaults() {
        this.resetValue();

        this.textNumeroUnique.setText(NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, new java.util.Date()));
        this.tableFacture.getModel().clearRows();
    }

    public RowValuesTableModel getRowValuesTableModel() {
        return this.tableFacture.getModel();
    }

    /**
     * Définir la facture comme prévisionnelle. Pas de génération comptable, ni de mode de règlement
     * 
     * @deprecated mettre les valeurs dans une RowValues
     * @param b
     */
    @Deprecated
    public void setPrevisonnelle(boolean b) {
        this.checkPrevisionnelle.setSelected(b);
        if (!b) {
            this.textNumeroUnique.setText(NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, new Date()));
        }
    }

    /**
     * Définir la facture comme complémentaire. Règlement supérieur au montant de la facture
     * initiale
     * 
     * @param b
     */
    public void setComplement(boolean b) {
        this.checkComplement.setSelected(b);
    }

    /**
     * Définir la facture comme acompte.
     * 
     * @param b
     */
    public void setAcompte(boolean b) {
        this.checkAcompte.setSelected(b);
        this.checkAcompte.firePropertyChange("ValueChanged", !b, b);
    }

    public void setTypeInterventionText(String text) {
        this.textTypeMission.setValue(text);
    }

    public void setReferenceClientText(String text) {
        this.refClient.setText(text);
    }

    protected String getLibelleStock(SQLRow row, SQLRow rowElt) {
        return "Saisie vente facture N°" + row.getString("NUMERO");
    }

    /**
     * Mise à jour des stocks pour chaque article composant la facture
     * 
     * @throws SQLException
     */
    private void updateStock(int id) throws SQLException {

        SQLPreferences prefs = SQLPreferences.getMemCached(getTable().getDBRoot());
        if (prefs.getBoolean(GestionArticleGlobalPreferencePanel.STOCK_FACT, true)) {

            MouvementStockSQLElement mvtStock = (MouvementStockSQLElement) Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
            mvtStock.createMouvement(getTable().getRow(id), getTable().getTable("SAISIE_VENTE_FACTURE_ELEMENT"), new StockLabel() {

                @Override
                public String getLabel(SQLRow rowOrigin, SQLRow rowElt) {
                    return getLibelleStock(rowOrigin, rowElt);
                }
            }, false);

        }
    }

    @Override
    protected RowValuesTable getRowValuesTable() {
        return this.tableFacture.getRowValuesTable();
    }

}
