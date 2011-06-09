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
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.AbstractArticleItemTable;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.payment.component.ModeDeReglementSQLComponent;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureSQLElement;
import org.openconcerto.erp.core.sales.invoice.report.VenteFactureXmlSheet;
import org.openconcerto.erp.core.sales.invoice.ui.SaisieVenteFactureItemTable;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.erp.generationEcritures.GenerationMvtSaisieVenteFacture;
import org.openconcerto.erp.model.BanqueModifiedListener;
import org.openconcerto.erp.model.ISQLCompteSelector;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.ModeReglementDefautPrefPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.UndefinedRowValuesCache;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.ISQLElementWithCodeSelector;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.ui.DefaultGridBagConstraints;
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
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
    private JTextField textSource, textIdSource;
    private ElementComboBox comboClient, comboAdresse;
    private ISQLCompteSelector compteSel;
    private final SQLTable tableNum = this.factureElt.getTable().getBase().getTable("NUMEROTATION_AUTO");
    private JCheckBox checkImpr, checkVisu, checkCompteServiceAuto, checkPrevisionnelle, checkComplement, checkAcompte, checkCT;
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

    private PropertyChangeListener listenerPoleProduit = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {

            SQLTable tableBanque = Configuration.getInstance().getBase().getTable("BANQUE_POLE_PRODUIT");
            final ModeDeReglementSQLComponent modeReglComp = (ModeDeReglementSQLComponent) SaisieVenteFactureSQLComponent.this.eltModeRegl.getSQLChild();

            if (SaisieVenteFactureSQLComponent.this.comboCommercial.getSelectedId() > 1) {

                Where w = new Where(tableBanque.getField("ID_POLE_PRODUIT"), "=", SaisieVenteFactureSQLComponent.this.comboCommercial.getSelectedId());
                // w = w.and(new Where(tableBanque.getField("AFFACTURAGE"), "=",
                // boxAffacturage.isSelected()));
                System.err.println("Set where ID pole == " + SaisieVenteFactureSQLComponent.this.comboCommercial.getSelectedId());

                // if (getSelectedID() > 1) {
                // SQLRow rowSelected = getTable().getRow(getSelectedID());
                // int idPoleProduit = rowSelected.getInt("ID_POLE_PRODUIT");
                // if (idPoleProduit == comboCommercial.getSelectedId()) {
                // modeReglComp.setWhereBanque(w);
                // } else {
                // modeReglComp.setSelectedIdBanque(SQLRow.NONEXISTANT_ID);
                // modeReglComp.setWhereBanque(w);
                // }
                // } else {
                SQLRow row = getTable().getRow(getSelectedID());
                // if (!(row.getForeignRow("ID_MODE_REGLEMENT").getInt("ID_BANQUE_POLE_PRODUIT") ==
                // modeReglComp.getSelectedIdBanque() && row.getInt("ID_CLIENT") ==
                // comboClient.getSelectedId())) {
                // modeReglComp.setSelectedIdBanque(SQLRow.NONEXISTANT_ID);
                // }
                modeReglComp.setWhereBanque(w);
                // }
            } else {
                System.err.println("Set where ID pole NULL ");
                modeReglComp.setWhereBanque(null);
            }

        }
    };

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

        this.textSource = new JTextField();
        this.textIdSource = new JTextField();
        this.textAvoirTTC = new DeviseField();

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

        this.comboClient.addValueListener(this.changeClientListener);

        this.comboAdresse = new ElementComboBox();
        this.comboAdresse.setAddIconVisible(false);
        this.comboAdresse.setListIconVisible(false);
            JLabel labelAdresse = new JLabel(getLabelFor("ID_ADRESSE"));
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


        // Acompte
        this.checkAcompte = new JCheckBox(getLabelFor("ACOMPTE"));
        this.addView(this.checkAcompte, "ACOMPTE");

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
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;

        c.fill = GridBagConstraints.BOTH;
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

        JLabel labelPortHT = new JLabel(getLabelFor("PORT_HT"));
        labelPortHT.setHorizontalAlignment(SwingConstants.RIGHT);
        cFrais.gridy++;
        panelFrais.add(labelPortHT, cFrais);
        cFrais.gridx++;
        panelFrais.add(this.textPortHT, cFrais);

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
        // FIXME was required but not displayed for KD
        addSQLObject(fieldTHA, "T_HA");
        addRequiredSQLObject(fieldHT, "T_HT");
        addRequiredSQLObject(fieldTVA, "T_TVA");
        addRequiredSQLObject(this.fieldTTC, "T_TTC");
        addRequiredSQLObject(fieldService, "T_SERVICE");

        totalTTC = new TotalPanel(this.tableFacture.getRowValuesTable(), this.tableFacture.getPrixTotalHTElement(), this.tableFacture.getPrixTotalTTCElement(), this.tableFacture.getHaElement(),
                this.tableFacture.getQteElement(), fieldHT, fieldTVA, this.fieldTTC, this.textPortHT, this.textRemiseHT, fieldService, this.tableFacture.getPrixServiceElement(), null, fieldTHA);
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

        ITextArea infos = new ITextArea(4, 4);
        c.gridy++;

        final JScrollPane comp = new JScrollPane(infos);
        infos.setBorder(null);
        DefaultGridBagConstraints.lockMinimumSize(comp);
        this.add(comp, c);

        final JPanel panelImpression = new JPanel();
        this.checkImpr = new JCheckBox("Imprimer");
        panelImpression.add(this.checkImpr);
        this.checkVisu = new JCheckBox("Visualiser");
        panelImpression.add(this.checkVisu);

        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHEAST;
        this.add(panelImpression, c);

        this.addSQLObject(this.textSource, "SOURCE");
        this.addSQLObject(this.textAvoirTTC, "T_AVOIR_TTC");
        this.addSQLObject(this.textIdSource, "IDSOURCE");

        this.addRequiredSQLObject(dateSaisie, "DATE");
        this.addRequiredSQLObject(this.comboClient, "ID_CLIENT");
            this.addSQLObject(this.comboAdresse, "ID_ADRESSE");
        this.addRequiredSQLObject(this.textNumeroUnique, "NUMERO");
        this.addSQLObject(infos, "INFOS");
        this.addSQLObject(this.checkPrevisionnelle, "PREVISIONNELLE");
        this.addSQLObject(this.checkComplement, "COMPLEMENT");
        this.addSQLObject(this.selAvoir, "ID_AVOIR_CLIENT");
        this.addSQLObject(this.compteSelService, "ID_COMPTE_PCE_SERVICE");

        final JCheckBox boxAffacturage = new JCheckBox(getLabelFor("AFFACTURAGE"));
        this.addSQLObject(boxAffacturage, "AFFACTURAGE");

        final SQLTable tableBanque = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("BANQUE_POLE_PRODUIT");
        ModeDeReglementSQLComponent modeReglComp = (ModeDeReglementSQLComponent) this.eltModeRegl.getSQLChild();

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
                // compteSel.removeValueListener(changeCompteListener);

                if (SaisieVenteFactureSQLComponent.this.comboClient.getValue() != null) {
                    Integer id = SaisieVenteFactureSQLComponent.this.comboClient.getValue();

                    SaisieVenteFactureSQLComponent.this.defaultContactRowValues.put("ID_CLIENT", id);
                    if (id > 1) {

                        SQLRow row = SaisieVenteFactureSQLComponent.this.client.getTable().getRow(id);
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
                // compteSel.addValueListener(changeCompteListener);
            }
        };

        this.changeCompteListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                SQLSelect sel = new SQLSelect(getTable().getBase());
                sel.addSelect(SaisieVenteFactureSQLComponent.this.client.getTable().getKey());
                Where where = new Where(SaisieVenteFactureSQLComponent.this.client.getTable().getField("ID_COMPTE_PCE"), "=", SaisieVenteFactureSQLComponent.this.compteSel.getValue());
                // where = where.and(new Where(client.getTable().getKey(), "!=",
                // nomClient.getValue()));
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
        modeReglComp.addBanqueModifiedListener(new BanqueModifiedListener() {
            @Override
            public void idChange(int newId) {
                if (newId > 1) {
                    SQLRow rowBanque = tableBanque.getRow(newId);
                    boxAffacturage.setSelected(rowBanque.getBoolean("AFFACTURAGE"));
                } else {
                    boxAffacturage.setSelected(false);
                }
            }
        });
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
        Number n = (Number) this.fieldTTC.getUncheckedValue();
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
                    this.textAvoirTTC.setValue(GestionDevise.currencyToString(ttc));
                } else {
                    this.textAvoirTTC.setValue(GestionDevise.currencyToString(totalAvoir));
                }
                this.textTotalAvoir.setValue(GestionDevise.currencyToString(l));

            } else {
                this.textTotalAvoir.setValue(GestionDevise.currencyToString(0));
            }
        } else {
            if (n != null) {
                this.textTotalAvoir.setValue(GestionDevise.currencyToString(n.longValue()));
            } else {
                this.textTotalAvoir.setValue(GestionDevise.currencyToString(0));
            }
            this.textAvoirTTC.setValue(GestionDevise.currencyToString(0));
        }
    }

    @Override
    public synchronized boolean isValidated() {
        // TODO Auto-generated method stub
        boolean b = true;
        // if (fieldTTC != null && fieldTTC.getUncheckedValue() != null) {
        //
        // long l = ((Long) fieldTTC.getUncheckedValue());
        //
        // if (this.selAvoir != null && !this.selAvoir.isEmpty() && this.selAvoir.getSelectedId() >
        // 1) {
        // SQLElement eltAvoir =
        // Configuration.getInstance().getDirectory().getElement("AVOIR_CLIENT");
        // SQLRow rowAvoir = eltAvoir.getTable().getRow(this.selAvoir.getSelectedId());
        // l -= ((Number) rowAvoir.getObject("MONTANT_TTC")).longValue();
        // }
        // b = l >= 0;
        // }
        return super.isValidated() && b;
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
        this.comboClient.rmValueListener(this.listenerModeReglDefaut);
        this.rowSelected = r;
        if (r != null) {
            this.textNumeroUnique.setIdSelected(r.getID());

        }
            super.select(r);

        if (r != null) {
            this.tableFacture.getModel().clearRows();
            this.tableFacture.insertFrom("ID_SAISIE_VENTE_FACTURE", r.getID());
            Boolean b = (Boolean) r.getObject("ACOMPTE");
            if (b != null) {
                setAcompte(b);
            } else {
                setAcompte(false);
            }
        }
        this.comboClient.addValueListener(this.listenerModeReglDefaut);
        this.comboClient.addValueListener(this.changeClientListener);
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

            if (getMode() == Mode.INSERTION) {
                idSaisieVF = super.insert(order);

                // incrémentation du numéro auto
                if (NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class).equalsIgnoreCase(this.textNumeroUnique.getText().trim())) {
                    SQLRowValues rowVals = new SQLRowValues(this.tableNum);
                    int val = this.tableNum.getRow(2).getInt("FACT_START");
                    val++;
                    rowVals.put("FACT_START", Integer.valueOf(val));

                    try {
                        rowVals.update(2);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (JOptionPane.showConfirmDialog(this, "Attention en modifiant cette facture, vous supprimerez les chéques et les échéances associés. Continuer?", "Modification de facture",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

                    // On efface les anciens mouvements de stocks
                    SQLSelect sel = new SQLSelect(eltMvtStock.getTable().getBase());
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

            // Mise à jour des tables liées
            this.tableFacture.updateField("ID_SAISIE_VENTE_FACTURE", idSaisieVF);


            // generation du document
            VenteFactureXmlSheet sheet = new VenteFactureXmlSheet(rowFacture);
            sheet.genere(this.checkVisu.isSelected(), this.checkImpr.isSelected());

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
                    try {
                        rowVals.update();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
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
                    try {
                        rowVals.update();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                // Mise à jour du montant facturé de l'affaire
                if (rowFacture.getInt("ID_AFFAIRE") > 1) {

                    SQLRow rowAffaire = rowFacture.getForeignRow("ID_AFFAIRE");
                    SQLRowValues rowValsAffaire = rowAffaire.createEmptyUpdateRow();
                    long lMontantFacture = ((Number) rowAffaire.getObject("MONTANT_FACTURE")).longValue();

                    long lFacture = ((Number) rowFacture.getObject("T_HT")).longValue();

                    rowValsAffaire.put("MONTANT_FACTURE", Long.valueOf(lMontantFacture + (lFacture - lFactureOld)));
                    try {
                        rowValsAffaire.update();
                    } catch (SQLException e) {
                        ExceptionHandler.handle("Erreur lors de la mise à jour du montant facturé de l'affaire!");
                        e.printStackTrace();
                    }
                }

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
            this.select(injector.createRowValuesFrom(idDevis));
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
            rowVals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class));
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
            this.select(injector.createRowValuesFrom(idCmd));
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
    public void loadBonItems(int idBl) {

        SQLElement bon = Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON");
        SQLElement bonElt = Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON_ELEMENT");
        if (idBl > 1) {
            SQLInjector injector = SQLInjector.getInjector(bon.getTable(), this.getTable());
            this.select(injector.createRowValuesFrom(idBl));
            this.listenerModeReglDefaut.propertyChange(null);
        }
        loadItem(this.tableFacture, bon, idBl, bonElt);
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
        this.tableFacture.getModel().addNewRow();

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
        vals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class));
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
        System.err.println("Defaults " + vals);
        return vals;
    }

    public void setDefaults() {
        this.resetValue();
        this.textNumeroUnique.setText(NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class));
        this.tableFacture.getModel().clearRows();
    }

    public RowValuesTableModel getRowValuesTableModel() {
        return this.tableFacture.getModel();
    }

    /**
     * Définir la facture comme prévisionnelle. Pas de génération comptable, ni de mode de règlement
     * 
     * @param b
     */
    public void setPrevisonnelle(boolean b) {
        this.checkPrevisionnelle.setSelected(b);
        if (!b) {
            this.textNumeroUnique.setText(NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class));
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

        // boolean bOld = this.checkAcompte.isSelected();
        // System.err.println("---> Set acompte " + b + " Old Value " + bOld);
        this.checkAcompte.setSelected(b);
        this.checkAcompte.firePropertyChange("ValueChanged", !b, b);
    }

    public void setTypeInterventionText(String text) {
        this.textTypeMission.setValue(text);
    }

    public void setReferenceClientText(String text) {
        this.refClient.setText(text);
    }

    /**
     * Mise à jour des stocks pour chaque article composant la facture
     */
    private void updateStock(int id) {

        SQLElement eltArticleFact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");
        SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
        SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement(sqlTableArticle);
        SQLRow rowFacture = getTable().getRow(id);

        // FIXME Si la facture vient d'un bon de livraison le stock a deja été mis à jour
        // if (rowFacture.getString("SOURCE").equalsIgnoreCase("BON_DE_LIVRAISON")) {
        // return;
        // }

        // On récupére les articles qui composent la facture
        SQLSelect selEltfact = new SQLSelect(eltArticleFact.getTable().getBase());
        selEltfact.addSelect(eltArticleFact.getTable().getField("ID"));
        selEltfact.setWhere(new Where(eltArticleFact.getTable().getField("ID_SAISIE_VENTE_FACTURE"), "=", id));

        List lEltFact = (List) eltArticleFact.getTable().getBase().getDataSource().execute(selEltfact.asString(), new ArrayListHandler());

        if (lEltFact != null) {
            for (int i = 0; i < lEltFact.size(); i++) {

                // Elt qui compose facture
                Object[] tmp = (Object[]) lEltFact.get(i);
                int idEltFact = ((Number) tmp[0]).intValue();
                SQLRow rowEltFact = eltArticleFact.getTable().getRow(idEltFact);

                // on récupére l'article qui lui correspond
                SQLRowValues rowArticle = new SQLRowValues(eltArticle.getTable());
                for (SQLField field : eltArticle.getTable().getFields()) {
                    if (rowEltFact.getTable().getFieldsName().contains(field.getName())) {
                        rowArticle.put(field.getName(), rowEltFact.getObject(field.getName()));
                    }
                }
                // rowArticle.loadAllSafe(rowEltFact);
                int idArticle = ReferenceArticleSQLElement.getIdForCNM(rowArticle, true);

                // on crée un mouvement de stock pour chacun des articles
                SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
                SQLRowValues rowVals = new SQLRowValues(eltMvtStock.getTable());
                rowVals.put("QTE", -(rowEltFact.getInt("QTE")));
                rowVals.put("NOM", "Saisie vente facture N°" + rowFacture.getString("NUMERO"));
                rowVals.put("IDSOURCE", id);
                rowVals.put("SOURCE", getTable().getName());
                rowVals.put("ID_ARTICLE", idArticle);
                rowVals.put("DATE", rowFacture.getObject("DATE"));
                try {
                    SQLRow row = rowVals.insert();
                    MouvementStockSQLElement.updateStock(row.getID());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                // }
            }
        }
    }
}
