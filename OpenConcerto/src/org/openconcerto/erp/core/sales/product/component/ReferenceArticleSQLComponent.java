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
 
 package org.openconcerto.erp.core.sales.product.component;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.CodeFournisseurItemTable;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.product.element.UniteVenteArticleSQLElement;
import org.openconcerto.erp.core.sales.product.ui.ArticleDesignationTable;
import org.openconcerto.erp.core.sales.product.ui.ArticleTarifTable;
import org.openconcerto.erp.model.ISQLCompteSelector;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.preferences.DefaultProps;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ReferenceArticleSQLComponent extends BaseSQLComponent {

    private JTextField textPVHT, textPVTTC, textPAHT;
    private JTextField textMetrique1VT, textMetrique1HA;

    private final JCheckBox boxService = new JCheckBox(getLabelFor("SERVICE"));
    private final JCheckBox checkObs = new JCheckBox(getLabelFor("OBSOLETE"));
    private JTextField textNom, textCode;
    private JTextField textPoids;
    private JTextField textValMetrique1, textValMetrique2, textValMetrique3;
    private DocumentListener htDocListener, ttcDocListener, detailsListener;
    private PropertyChangeListener propertyChangeListener;
    private PropertyChangeListener taxeListener;
    private final ElementComboBox comboSelTaxe = new ElementComboBox(false, 10);
    private final ElementComboBox comboSelModeVente = new ElementComboBox(false, 25);
    private JLabel labelMetriqueHA1 = new JLabel(getLabelFor("PRIX_METRIQUE_HA_1"), SwingConstants.RIGHT);
    private JLabel labelMetriqueVT1 = new JLabel(getLabelFor("PRIX_METRIQUE_VT_1"), SwingConstants.RIGHT);

    private ArticleDesignationTable tableDes = new ArticleDesignationTable();
    private ArticleTarifTable tableTarifVente = new ArticleTarifTable(this);
    private final JTextField textMarge = new JTextField(15);

    private DocumentListener pieceHAArticle = new DocumentListener() {

        public void changedUpdate(DocumentEvent arg0) {
            ReferenceArticleSQLComponent.this.textMetrique1HA.setText(ReferenceArticleSQLComponent.this.textPAHT.getText());
        }

        public void insertUpdate(DocumentEvent arg0) {
            ReferenceArticleSQLComponent.this.textMetrique1HA.setText(ReferenceArticleSQLComponent.this.textPAHT.getText());
        }

        public void removeUpdate(DocumentEvent arg0) {
            ReferenceArticleSQLComponent.this.textMetrique1HA.setText(ReferenceArticleSQLComponent.this.textPAHT.getText());
        }

    };
    private DocumentListener pieceVTArticle = new DocumentListener() {

        public void changedUpdate(DocumentEvent arg0) {
            ReferenceArticleSQLComponent.this.textMetrique1VT.setText(ReferenceArticleSQLComponent.this.textPVHT.getText());
        }

        public void insertUpdate(DocumentEvent arg0) {
            ReferenceArticleSQLComponent.this.textMetrique1VT.setText(ReferenceArticleSQLComponent.this.textPVHT.getText());
        }

        public void removeUpdate(DocumentEvent arg0) {
            ReferenceArticleSQLComponent.this.textMetrique1VT.setText(ReferenceArticleSQLComponent.this.textPVHT.getText());
        }

    };

    private DocumentListener listenerMargeTextMarge = new SimpleDocumentListener() {
        @Override
        public void update(DocumentEvent e) {
            ReferenceArticleSQLComponent.this.textPVHT.getDocument().removeDocumentListener(ReferenceArticleSQLComponent.this.listenerMargeTextVT);
            updateVtFromMarge();
            ReferenceArticleSQLComponent.this.textPVHT.getDocument().addDocumentListener(ReferenceArticleSQLComponent.this.listenerMargeTextVT);
        }

    };

    private DocumentListener listenerMargeTextVT = new SimpleDocumentListener() {
        @Override
        public void update(DocumentEvent e) {
            ReferenceArticleSQLComponent.this.textMarge.getDocument().removeDocumentListener(ReferenceArticleSQLComponent.this.listenerMargeTextMarge);
            if (ReferenceArticleSQLComponent.this.textPVHT.getText().trim().length() > 0 && ReferenceArticleSQLComponent.this.textPAHT.getText().trim().length() > 0) {
                BigDecimal vt = new BigDecimal(ReferenceArticleSQLComponent.this.textPVHT.getText());

                BigDecimal ha = new BigDecimal(ReferenceArticleSQLComponent.this.textPAHT.getText());

                if (vt != null && ha != null) {
                    if (vt.signum() != 0 && ha.signum() != 0) {
                        BigDecimal margeHT = vt.subtract(ha);

                        BigDecimal value;

                        if (DefaultNXProps.getInstance().getBooleanValue(TotalPanel.MARGE_MARQUE, false)) {
                            if (vt.compareTo(BigDecimal.ZERO) > 0) {
                                value = margeHT.divide(vt, MathContext.DECIMAL128).multiply(BigDecimal.valueOf(100), MathContext.DECIMAL128);
                            } else {
                                value = BigDecimal.ZERO;
                            }
                        } else {
                            value = margeHT.divide(ha, MathContext.DECIMAL128).multiply(BigDecimal.valueOf(100), MathContext.DECIMAL128);
                        }

                        if (value.compareTo(BigDecimal.ZERO) > 0) {
                            ReferenceArticleSQLComponent.this.textMarge.setText(value.setScale(6, RoundingMode.HALF_UP).toString());
                        } else {
                            ReferenceArticleSQLComponent.this.textMarge.setText("0");
                        }
                    }
                }
            }
            ReferenceArticleSQLComponent.this.textMarge.getDocument().addDocumentListener(ReferenceArticleSQLComponent.this.listenerMargeTextMarge);
        }
    };

    private DocumentListener listenerMargeTextHA = new SimpleDocumentListener() {
        @Override
        public void update(DocumentEvent e) {
            ReferenceArticleSQLComponent.this.textPVHT.getDocument().removeDocumentListener(ReferenceArticleSQLComponent.this.listenerMargeTextVT);
            updateVtFromMarge();
            ReferenceArticleSQLComponent.this.textPVHT.getDocument().addDocumentListener(ReferenceArticleSQLComponent.this.listenerMargeTextVT);
        }
    };

    private void updateVtFromMarge() {
        if (this.textPAHT.getText().trim().length() > 0) {

            BigDecimal ha = new BigDecimal(this.textPAHT.getText());
            if (ha != null && this.textMarge.getText().trim().length() > 0) {

                BigDecimal d = new BigDecimal(this.textMarge.getText());
                if (DefaultNXProps.getInstance().getBooleanValue(TotalPanel.MARGE_MARQUE, false)) {
                    final BigDecimal e = BigDecimal.ONE.subtract(d.divide(BigDecimal.valueOf(100), MathContext.DECIMAL128));
                    if (e.signum() == 0) {
                        this.textPVHT.setText("0");
                    } else {
                        this.textPVHT.setText(ha.divide(e, MathContext.DECIMAL128).setScale(getTable().getField("PV_HT").getType().getDecimalDigits(), RoundingMode.HALF_UP).toString());
                    }
                } else {
                    BigDecimal result = ha.multiply(d.divide(BigDecimal.valueOf(100), MathContext.DECIMAL128).add(BigDecimal.ONE));
                    this.textPVHT.setText(result.setScale(getTable().getField("PV_HT").getType().getDecimalDigits(), RoundingMode.HALF_UP).toString());
                }
            }
        }
    }

    public ReferenceArticleSQLComponent(SQLElement elt) {
        super(elt);
    }

    @Override
    public void select(SQLRowAccessor r) {
        super.select(r);
        if (r != null && r.getID() > getTable().getUndefinedID()) {
            this.checkObs.setVisible(true);
            this.tableTarifVente.setArticleValues(r);
            this.tableTarifVente.insertFrom("ID_ARTICLE", r.getID());
            this.tableDes.insertFrom("ID_ARTICLE", r.getID());
            if (this.codeFournisseurTable != null) {
                this.codeFournisseurTable.insertFrom("ID_ARTICLE", r.getID());
            }
        }
    }

    public void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        this.textPVHT = new JTextField(15);
        this.textPVTTC = new JTextField(15);
        this.textPAHT = new JTextField(15);
        this.textPVHT.getDocument().addDocumentListener(this.listenerMargeTextVT);

        // Init metrique devise field
        this.textMetrique1HA = new JTextField(15);
        this.textMetrique1VT = new JTextField(15);

        // init metrique value field
        this.textValMetrique1 = new JTextField(15);
        this.textValMetrique2 = new JTextField(15);
        this.textValMetrique3 = new JTextField(15);

        this.textCode = new JTextField();
        this.textNom = new JTextField();
        this.textPoids = new JTextField(6);

        // Code
        JLabel codelabel = new JLabel(getLabelFor("CODE"));
        codelabel.setHorizontalAlignment(SwingConstants.RIGHT);
        DefaultGridBagConstraints.lockMinimumSize(codelabel);
        this.add(codelabel, c);
        c.gridx++;
        c.weightx = 1;
        DefaultGridBagConstraints.lockMinimumSize(textCode);
        this.add(this.textCode, c);

        // Famille
        c.gridx++;
        c.gridwidth = 1;
        c.weightx = 0;
        JLabel labelFamille = new JLabel(getLabelFor("ID_FAMILLE_ARTICLE"));
        labelFamille.setHorizontalAlignment(SwingConstants.RIGHT);
        DefaultGridBagConstraints.lockMinimumSize(labelFamille);
        this.add(labelFamille, c);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 1;
        final ElementComboBox comboSelFamille = new ElementComboBox(false, 25);
        this.addSQLObject(comboSelFamille, "ID_FAMILLE_ARTICLE");
        DefaultGridBagConstraints.lockMinimumSize(comboSelFamille);
        this.add(comboSelFamille, c);

        // Nom
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        JLabel labelNom = new JLabel(getLabelFor("NOM"));
        labelNom.setHorizontalAlignment(SwingConstants.RIGHT);
        DefaultGridBagConstraints.lockMinimumSize(labelNom);
        this.add(labelNom, c);
        c.gridx++;
        c.weightx = 1;
        DefaultGridBagConstraints.lockMinimumSize(textNom);
        this.add(this.textNom, c);

        // Code barre
        c.gridx++;
        c.weightx = 0;
        JLabel labelCodeBarre = new JLabel(getLabelFor("CODE_BARRE"));
        labelCodeBarre.setHorizontalAlignment(SwingConstants.RIGHT);
        DefaultGridBagConstraints.lockMinimumSize(labelCodeBarre);
        this.add(labelCodeBarre, c);
        c.gridx++;
        c.weightx = 1;
        JTextField fieldCodeBarre = new JTextField();
        DefaultGridBagConstraints.lockMinimumSize(fieldCodeBarre);
        this.add(fieldCodeBarre, c);
        this.addView(fieldCodeBarre, "CODE_BARRE");

        SQLPreferences prefs = new SQLPreferences(getTable().getDBRoot());
        // Gestion des unités de vente
        final boolean gestionUV = prefs.getBoolean(GestionArticleGlobalPreferencePanel.UNITE_VENTE, true);
        if (gestionUV) {
            c.gridy++;
            c.gridx = 0;
            c.weightx = 0;
            this.add(new JLabel(getLabelFor("ID_UNITE_VENTE"), SwingConstants.RIGHT), c);
            c.gridx++;
            c.weightx = 1;
            c.fill = GridBagConstraints.NONE;
            ElementComboBox boxUnite = new ElementComboBox();
            DefaultGridBagConstraints.lockMinimumSize(boxUnite);
            this.add(boxUnite, c);
            this.addView(boxUnite, "ID_UNITE_VENTE");
            c.fill = GridBagConstraints.HORIZONTAL;
        }
        DefaultProps props = DefaultNXProps.getInstance();

        // Article détaillé
        String modeVente = props.getStringProperty("ArticleModeVenteAvance");
        Boolean bModeVente = Boolean.valueOf(modeVente);
        boolean modeVenteAvance = (bModeVente == null || bModeVente.booleanValue());

        if (modeVenteAvance) {
            addModeVenteAvance(c);
        }

        getMontantPanel(c, props);

        // Champ Module
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel addP = ComptaSQLConfElement.createAdditionalPanel();
        this.setAdditionalFieldsPanel(new FormLayouter(addP, 2));
        this.add(addP, c);

        JTabbedPane pane = new JTabbedPane();
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;

        pane.add("Tarifs de vente", createTarifPanel());
        pane.add("Exportation", createExportationPanel());
        pane.add("Achat", createAchatPanel());
        pane.add("Stock", createStockPanel());
        pane.add("Descriptif", createDescriptifPanel());
        pane.add("Désignations multilingues", createDesignationPanel());
        pane.add("Comptabilité", createComptaPanel());
        pane.add(getLabelFor("INFOS"), createInfosPanel());

        c.fill = GridBagConstraints.BOTH;
        this.add(pane, c);

        this.addSQLObject(this.textMetrique1HA, "PRIX_METRIQUE_HA_1");
        this.addSQLObject(this.textMetrique1VT, "PRIX_METRIQUE_VT_1");
        this.addSQLObject(this.textValMetrique1, "VALEUR_METRIQUE_1");
        this.addSQLObject(this.textValMetrique2, "VALEUR_METRIQUE_2");
        this.addSQLObject(this.textValMetrique3, "VALEUR_METRIQUE_3");
        this.addSQLObject(this.comboSelModeVente, "ID_MODE_VENTE_ARTICLE");
        this.addSQLObject(this.boxService, "SERVICE");

        this.addRequiredSQLObject(this.textNom, "NOM");
        this.addRequiredSQLObject(this.textCode, "CODE");

        this.addSQLObject(this.textPoids, "POIDS");

        this.comboSelTaxe.setButtonsVisible(false);
        this.propertyChangeListener = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                System.err.println(ReferenceArticleSQLComponent.this.comboSelModeVente.getSelectedId());
                selectModeVente(ReferenceArticleSQLComponent.this.comboSelModeVente.getSelectedId());

            }
        };
        setListenerModeVenteActive(true);
        this.comboSelModeVente.setValue(ReferenceArticleSQLElement.A_LA_PIECE);
    }

    private Component createInfosPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new DefaultGridBagConstraints();

        c.weightx = 1;
        c.weighty = 1;
        ITextArea infos = new ITextArea();
        c.fill = GridBagConstraints.BOTH;
        panel.add(infos, c);

        this.addSQLObject(infos, "INFOS");

        return panel;
    }

    private Component createDescriptifPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new DefaultGridBagConstraints();

        // Obsolete
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        this.checkObs.setOpaque(false);
        panel.add(this.checkObs, c);

        this.checkObs.setVisible(false);
        this.addView(this.checkObs, "OBSOLETE");

        if (getTable().getFieldsName().contains("COLORIS")) {
            JTextField fieldColoris = new JTextField();
            c.gridy++;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 0;
            c.gridwidth = 1;
            panel.add(new JLabel(getLabelFor("COLORIS")), c);

            c.weightx = 1;
            c.gridx++;
            panel.add(fieldColoris, c);
            this.addView(fieldColoris, "COLORIS");
        }
        ITextArea area = new ITextArea();
        JLabel sep = new JLabel("Descriptif complet");
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(sep, c);

        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        panel.add(area, c);
        this.addView(area, "DESCRIPTIF");
        return panel;
    }

    private Component createDesignationPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new DefaultGridBagConstraints();

        // Ajout des
        c.gridwidth = 1;
        c.weightx = 0;
        c.gridy++;
        c.gridx = 0;
        panel.add(new JLabel("Ajouter une désignation "), c);

        final ElementComboBox boxDes = new ElementComboBox();
        boxDes.init(Configuration.getInstance().getDirectory().getElement("LANGUE"));

        c.gridx++;
        panel.add(boxDes, c);

        c.fill = GridBagConstraints.NONE;
        c.gridx++;
        JButton buttonAjouterDes = new JButton("Ajouter");
        buttonAjouterDes.setOpaque(false);
        panel.add(buttonAjouterDes, c);
        c.gridx++;
        JButton buttonSupprimerDes = new JButton("Supprimer");
        buttonSupprimerDes.setOpaque(false);
        panel.add(buttonSupprimerDes, c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 1;
        this.tableDes.setOpaque(false);
        panel.add(this.tableDes, c);

        // Listerners
        buttonAjouterDes.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                int id = boxDes.getSelectedId();
                if (id <= 1) {
                    return;
                }
                int nbRows = tableDes.getModel().getRowCount();

                for (int i = 0; i < nbRows; i++) {
                    SQLRowValues rowVals = tableDes.getModel().getRowValuesAt(i);
                    int idLangue = Integer.parseInt(rowVals.getObject("ID_LANGUE").toString());
                    if (idLangue == id) {
                        JOptionPane.showMessageDialog(null, "Impossible d'ajouter.\nLa langue est déjà présente dans la liste!");
                        return;
                    }
                }

                SQLRowValues rowVals = new SQLRowValues(Configuration.getInstance().getBase().getTable("ARTICLE_DESIGNATION"));
                if (getSelectedID() > 1) {
                    rowVals.put("ID_ARTICLE", getSelectedID());
                }
                rowVals.put("ID_LANGUE", id);
                rowVals.put("NOM", "");
                tableDes.getModel().addRow(rowVals);
            }
        });
        buttonSupprimerDes.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tableDes.removeSelectedRow();
            }
        });

        return panel;
    }

    private Component createStockPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new DefaultGridBagConstraints();
        DefaultProps props = DefaultNXProps.getInstance();
        String stockMin = props.getStringProperty("ArticleStockMin");
        Boolean bStockMin = !stockMin.equalsIgnoreCase("false");
        boolean gestionStockMin = (bStockMin == null || bStockMin.booleanValue());
        c.gridx = 0;
        c.gridy++;

        final JCheckBox boxStock = new JCheckBox(getLabelFor("GESTION_STOCK"));
        boxStock.setOpaque(false);
        panel.add(boxStock, c);
        this.addView(boxStock, "GESTION_STOCK");

        final JTextField fieldQteMin = new JTextField();
        final JTextField fieldQteAchat = new JTextField();
        boxStock.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                fieldQteMin.setEnabled(boxStock.isSelected());
                fieldQteAchat.setEnabled(boxStock.isSelected());
            }
        });

        c.gridwidth = 1;
        if (gestionStockMin) {
            c.gridx = 0;
            c.gridy++;
            c.weightx = 0;
            panel.add(new JLabel(getLabelFor("QTE_MIN")), c);
            c.gridx++;
            c.weightx = 1;
            panel.add(fieldQteMin, c);
            this.addView(fieldQteMin, "QTE_MIN");

            c.gridx = 0;
            c.gridy++;
            c.weightx = 0;
            panel.add(new JLabel(getLabelFor("QTE_ACHAT")), c);
            c.gridx++;
            c.weightx = 1;
            panel.add(fieldQteAchat, c);
            this.addView(fieldQteAchat, "QTE_ACHAT");
        }

        c.gridy++;
        c.weighty = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        final JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        panel.add(spacer, c);
        return panel;
    }

    private Component createComptaPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = 1;
        c.weighty = 0;
        c.weightx = 0;
        ISQLCompteSelector sel = new ISQLCompteSelector();
        c.fill = GridBagConstraints.BOTH;
        panel.add(new JLabel(getLabelFor("ID_COMPTE_PCE")), c);
        c.gridx++;
        c.weightx = 1;
        panel.add(sel, c);
        this.addView(sel, "ID_COMPTE_PCE");

        c.gridwidth = 1;
        c.gridy++;
        c.weighty = 0;
        c.gridx = 0;
        c.weightx = 0;
        ISQLCompteSelector selAchat = new ISQLCompteSelector();
        c.fill = GridBagConstraints.BOTH;
        panel.add(new JLabel(getLabelFor("ID_COMPTE_PCE_ACHAT")), c);
        c.gridx++;
        c.weightx = 1;
        panel.add(selAchat, c);
        this.addView(selAchat, "ID_COMPTE_PCE_ACHAT");

        c.gridy++;
        c.weighty = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        final JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        panel.add(spacer, c);
        return panel;
    }

    private SQLRowValues rowValuesDefaultCodeFournisseur;
    private CodeFournisseurItemTable codeFournisseurTable;

    private Component createAchatPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new DefaultGridBagConstraints();
        // Fournisseur
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        JLabel labelFournisseur = new JLabel(getLabelFor("ID_FOURNISSEUR"));
        labelFournisseur.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(labelFournisseur, c);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 2;
        final ElementComboBox comboSelFournisseur = new ElementComboBox(false, 25);
        panel.add(comboSelFournisseur, c);
        this.addView(comboSelFournisseur, "ID_FOURNISSEUR");

        SQLPreferences prefs = new SQLPreferences(ComptaPropsConfiguration.getInstanceCompta().getRootSociete());
        final boolean supplierCode = prefs.getBoolean(GestionArticleGlobalPreferencePanel.SUPPLIER_PRODUCT_CODE, false);

        if (getTable().getSchema().contains("CODE_FOURNISSEUR") && supplierCode) {
            this.rowValuesDefaultCodeFournisseur = new SQLRowValues(getTable().getTable("CODE_FOURNISSEUR"));
            this.codeFournisseurTable = new CodeFournisseurItemTable(this.rowValuesDefaultCodeFournisseur);
            c.gridy++;
            c.gridx = 0;
            c.gridwidth = 3;
            c.weighty = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.BOTH;
            panel.add(this.codeFournisseurTable, c);
            comboSelFournisseur.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    rowValuesDefaultCodeFournisseur.put("ID_FOURNISSEUR", comboSelFournisseur.getSelectedId());
                }
            });
        } else {
            c.gridy++;
            c.weighty = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.BOTH;
            final JPanel spacer = new JPanel();
            spacer.setOpaque(false);
            panel.add(spacer, c);
        }
        return panel;
    }

    private JPanel createExportationPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new DefaultGridBagConstraints();
        // Code douanier
        c.gridx = 0;
        c.weightx = 0;
        c.gridy++;
        c.gridwidth = 1;
        JLabel labelCodeD = new JLabel(getLabelFor("CODE_DOUANIER"));
        labelCodeD.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(labelCodeD, c);

        c.gridx++;
        JTextField fieldCodeDouanier = new JTextField();
        c.weightx = 1;
        panel.add(fieldCodeDouanier, c);
        this.addView(fieldCodeDouanier, "CODE_DOUANIER");

        // Pays d'origine
        c.gridx++;
        c.weightx = 0;
        JLabel labelPays = new JLabel(getLabelFor("ID_PAYS"));
        labelPays.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(labelPays, c);
        c.gridx++;
        c.weightx = 1;
        final ElementComboBox comboSelPays = new ElementComboBox(false);
        panel.add(comboSelPays, c);
        this.addView(comboSelPays, "ID_PAYS");
        c.gridy++;
        c.weighty = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        final JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        panel.add(spacer, c);
        return panel;
    }

    private JPanel createTarifPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new DefaultGridBagConstraints();

        // Ajout tarif
        c.gridwidth = 1;
        c.weightx = 0;
        c.gridy++;
        c.gridx = 0;
        panel.add(new JLabel("Ajouter le tarif "), c);

        final ElementComboBox boxTarif = new ElementComboBox();
        boxTarif.init(Configuration.getInstance().getDirectory().getElement("TARIF"));

        c.gridx++;
        panel.add(boxTarif, c);

        c.fill = GridBagConstraints.NONE;
        c.gridx++;
        JButton buttonAjouter = new JButton("Ajouter");
        buttonAjouter.setOpaque(false);
        panel.add(buttonAjouter, c);
        c.gridx++;
        JButton buttonSupprimer = new JButton("Supprimer");
        buttonSupprimer.setOpaque(false);
        panel.add(buttonSupprimer, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        this.tableTarifVente.setOpaque(false);
        panel.add(this.tableTarifVente, c);

        // Listeners
        buttonAjouter.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                SQLRow rowTarif = boxTarif.getSelectedRow();
                if (rowTarif == null || rowTarif.isUndefined()) {
                    return;
                }
                int nbRows = tableTarifVente.getModel().getRowCount();

                for (int i = 0; i < nbRows; i++) {
                    SQLRowValues rowVals = tableTarifVente.getModel().getRowValuesAt(i);
                    int idTarif = Integer.parseInt(rowVals.getObject("ID_TARIF").toString());
                    if (idTarif == rowTarif.getID()) {
                        JOptionPane.showMessageDialog(null, "Impossible d'ajouter.\nLe tarif est déjà présent dans la liste!");
                        return;
                    }
                }

                SQLRowValues rowVals = new SQLRowValues(Configuration.getInstance().getBase().getTable("ARTICLE_TARIF"));
                if (getSelectedID() > 1) {
                    rowVals.put("ID_ARTICLE", getSelectedID());
                }
                rowVals.put("ID_TARIF", rowTarif.getID());
                rowVals.put("ID_DEVISE", rowTarif.getInt("ID_DEVISE"));
                rowVals.put("ID_TAXE", rowTarif.getInt("ID_TAXE"));
                rowVals.put("PRIX_METRIQUE_VT_1", BigDecimal.ZERO);
                rowVals.put("PV_HT", BigDecimal.ZERO);
                rowVals.put("PV_TTC", BigDecimal.ZERO);
                tableTarifVente.getModel().addRow(rowVals);
            }
        });
        buttonSupprimer.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tableTarifVente.removeSelectedRow();
            }
        });
        return panel;
    }

    protected void getMontantPanel(final GridBagConstraints c, DefaultProps props) {
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        // PA devise
        JPanel pDevise = new JPanel(new GridBagLayout());
        GridBagConstraints cDevise = new DefaultGridBagConstraints();
        cDevise.insets = new Insets(0, 0, 0, 4);
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Devise du fournisseur"), c);
        final ElementComboBox boxDevise = new ElementComboBox(true, 15);
        cDevise.gridx++;
        cDevise.weightx = 1;
        pDevise.add(boxDevise, cDevise);
        this.addView(boxDevise, "ID_DEVISE_HA");
        DefaultGridBagConstraints.lockMinimumSize(boxDevise);

        cDevise.weightx = 0;
        cDevise.gridx++;
        pDevise.add(new JLabel("Prix d'achat devise"), cDevise);
        final JTextField fieldHAD = new JTextField(15);
        cDevise.weightx = 1;
        cDevise.gridx++;
        pDevise.add(fieldHAD, cDevise);
        this.addView(fieldHAD, "PA_DEVISE");
        DefaultGridBagConstraints.lockMinimumSize(fieldHAD);

        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(pDevise, c);
        fieldHAD.getDocument().addDocumentListener(new SimpleDocumentListener() {

            @Override
            public void update(DocumentEvent e) {

                if (!isFilling() && fieldHAD.getText().trim().length() > 0) {

                    BigDecimal ha = new BigDecimal(fieldHAD.getText());

                    BigDecimal taux = BigDecimal.ONE;
                    if (boxDevise != null && boxDevise.getSelectedRow() != null && !boxDevise.getSelectedRow().isUndefined()) {
                        taux = (BigDecimal) boxDevise.getSelectedRow().getObject("TAUX");
                        textPAHT.setText(taux.multiply(ha, MathContext.DECIMAL128).setScale(getTable().getField("PA_DEVISE").getType().getDecimalDigits(), RoundingMode.HALF_UP).toString());
                    }

                }
            }
        });

        // PA
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints cAchat = new DefaultGridBagConstraints();
        cAchat.insets = new Insets(0, 0, 0, 4);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("PA_HT"), SwingConstants.RIGHT), c);
        cAchat.gridx++;
        cAchat.weightx = 1;
        p.add(this.textPAHT, cAchat);
        this.textPAHT.getDocument().addDocumentListener(this.listenerMargeTextHA);

        // Marge
        cAchat.gridx++;
        cAchat.weightx = 0;
        p.add(new JLabel("Marge"), cAchat);
        cAchat.weightx = 1;
        cAchat.gridx++;
        p.add(this.textMarge, cAchat);
        this.textMarge.getDocument().addDocumentListener(this.listenerMargeTextMarge);
        cAchat.gridx++;
        cAchat.weightx = 0;
        p.add(new JLabel("%           "), cAchat);

        // Poids
        JLabel labelPds = new JLabel(getLabelFor("POIDS"));
        cAchat.gridx++;
        cAchat.weightx = 0;
        p.add(labelPds, cAchat);
        labelPds.setHorizontalAlignment(SwingConstants.RIGHT);
        cAchat.weightx = 1;
        cAchat.gridx++;
        p.add(this.textPoids, cAchat);
        DefaultGridBagConstraints.lockMinimumSize(this.textPoids);

        // Service
        String sService = props.getStringProperty("ArticleService");
        Boolean bService = Boolean.valueOf(sService);
        if (bService != null && bService.booleanValue()) {
            cAchat.gridx++;
            cAchat.weightx = 0;
            p.add(this.boxService, cAchat);
        }

        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;

        this.add(p, c);

        // PV HT
        c.gridx = 0;
        c.gridy++;

        JPanel p2 = new JPanel(new GridBagLayout());
        GridBagConstraints cVT = new DefaultGridBagConstraints();
        cVT.insets = new Insets(0, 0, 0, 4);

        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        this.add(new JLabel(getLabelFor("PV_HT"), SwingConstants.RIGHT), c);
        cVT.gridx++;
        cVT.weightx = 1;

        p2.add(this.textPVHT, cVT);

        // Taxe
        JLabel labelTaxe = new JLabel(getLabelFor("ID_TAXE"));
        cVT.gridx++;
        cVT.weightx = 0;
        p2.add(labelTaxe, cVT);
        labelTaxe.setHorizontalAlignment(SwingConstants.RIGHT);
        cVT.gridx++;
        // cVT.weightx = 1;

        p2.add(this.comboSelTaxe, cVT);

        // PV_TTC
        cVT.gridx++;
        cVT.weightx = 0;
        p2.add(new JLabel(getLabelFor("PV_TTC")), cVT);
        cVT.gridx++;
        cVT.weightx = 1;
        p2.add(this.textPVTTC, cVT);
        c.gridx = 1;

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(p2, c);

        this.addRequiredSQLObject(this.textPAHT, "PA_HT");
        this.addRequiredSQLObject(this.textPVHT, "PV_HT");
        DefaultGridBagConstraints.lockMinimumSize(this.textPVHT);
        this.addRequiredSQLObject(this.comboSelTaxe, "ID_TAXE");
        DefaultGridBagConstraints.lockMinimumSize(this.comboSelTaxe);
        DefaultGridBagConstraints.lockMaximumSize(this.comboSelTaxe);
        this.addRequiredSQLObject(this.textPVTTC, "PV_TTC");
        DefaultGridBagConstraints.lockMinimumSize(this.textPVTTC);
        DefaultGridBagConstraints.lockMinimumSize(this.textPAHT);
        DefaultGridBagConstraints.lockMinimumSize(this.textMarge);
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

        this.detailsListener = new SimpleDocumentListener() {

            @Override
            public void update(DocumentEvent e) {
                updatePiece();
            }

        };

        this.taxeListener = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (ReferenceArticleSQLComponent.this.textPVHT.getText().trim().length() > 0) {
                    setTextTTC();
                } else {
                    setTextHT();
                }
                tableTarifVente.fireModification();
            }
        };
        this.textPVHT.getDocument().addDocumentListener(this.htDocListener);
        this.textPVTTC.getDocument().addDocumentListener(this.ttcDocListener);
        this.comboSelTaxe.addValueListener(this.taxeListener);

        this.textMetrique1HA.getDocument().addDocumentListener(this.detailsListener);
        this.textMetrique1VT.getDocument().addDocumentListener(this.detailsListener);

        this.textValMetrique1.getDocument().addDocumentListener(this.detailsListener);
        this.textValMetrique2.getDocument().addDocumentListener(this.detailsListener);
        this.textValMetrique3.getDocument().addDocumentListener(this.detailsListener);

    }

    private void setListenerModeVenteActive(boolean b) {
        if (b) {
            this.comboSelModeVente.addValueListener(this.propertyChangeListener);
        } else {
            this.comboSelModeVente.removePropertyChangeListener(this.propertyChangeListener);
        }
    }

    /**
     * @param c
     * @param props
     */
    private void addModeVenteAvance(GridBagConstraints c) {
        DefaultProps props = DefaultNXProps.getInstance();
        JSeparator sep = new JSeparator();
        JLabel labelDetails = new JLabel("Article détaillé", SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(labelDetails, c);
        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        this.add(sep, c);

        // Mode de vente
        c.gridwidth = 1;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy++;
        this.add(new JLabel(getLabelFor("ID_MODE_VENTE_ARTICLE"), SwingConstants.RIGHT), c);
        c.weightx = 1;
        c.gridx++;
        this.add(this.comboSelModeVente, c);

        // Prix metrique
        c.gridx = 0;
        c.weightx = 0;
        c.gridy++;
        this.add(this.labelMetriqueHA1, c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.textMetrique1HA, c);

        c.gridx++;
        c.weightx = 0;
        this.add(this.labelMetriqueVT1, c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.textMetrique1VT, c);

        // Metrique 1
        c.weightx = 0;
        JLabel labelMetrique1 = new JLabel(getLabelFor("VALEUR_METRIQUE_1"), SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        this.add(labelMetrique1, c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.textValMetrique1, c);
        c.gridx++;
        c.weightx = 0;

        Boolean bMetrique1 = Boolean.valueOf(props.getStringProperty("ArticleLongueur"));
        labelMetrique1.setVisible(bMetrique1 == null || bMetrique1.booleanValue());
        this.textValMetrique1.setVisible(bMetrique1 == null || bMetrique1.booleanValue());

        // Metrique 2
        JLabel labelMetrique2 = new JLabel(getLabelFor("VALEUR_METRIQUE_2"), SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(labelMetrique2, c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.textValMetrique2, c);
        c.gridx++;
        c.weightx = 0;

        Boolean bMetrique2 = Boolean.valueOf(props.getStringProperty("ArticleLargeur"));
        labelMetrique2.setVisible(bMetrique2 == null || bMetrique2.booleanValue());
        this.textValMetrique2.setVisible(bMetrique2 == null || bMetrique2.booleanValue());

        // Metrique 3
        JLabel labelMetrique3 = new JLabel(getLabelFor("VALEUR_METRIQUE_3"), SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(labelMetrique3, c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.textValMetrique3, c);
        c.gridx++;

        Boolean bMetrique3 = Boolean.valueOf(props.getStringProperty("ArticlePoids"));
        labelMetrique3.setVisible(bMetrique3 == null || bMetrique3.booleanValue());
        this.textValMetrique3.setVisible(bMetrique3 == null || bMetrique3.booleanValue());

        // Article détaillé
        JSeparator sep2 = new JSeparator();
        JLabel labelPiece = new JLabel("Article pièce", SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(labelPiece, c);
        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        this.add(sep2, c);

    }

    @Override
    public void update() {
            super.update();
        this.tableTarifVente.updateField("ID_ARTICLE", getSelectedID());
        this.tableDes.updateField("ID_ARTICLE", getSelectedID());
        if (this.codeFournisseurTable != null) {
            this.codeFournisseurTable.updateField("ID_ARTICLE", getSelectedID());
        }
    }

    /**
     * Sélection d'un mode de vente pour l'article. Affiche les prix metriques requis et fixe les
     * valeurs.
     * 
     * @param id id du mode de vente
     */
    private void selectModeVente(int id) {

        this.labelMetriqueHA1.setEnabled(true);
        this.labelMetriqueVT1.setEnabled(true);
        this.textMetrique1HA.setEnabled(true);
        this.textMetrique1VT.setEnabled(true);

        this.textPAHT.getDocument().removeDocumentListener(this.pieceHAArticle);
        this.textPVHT.getDocument().removeDocumentListener(this.pieceVTArticle);

        switch (id) {
        case ReferenceArticleSQLElement.AU_METRE_CARRE:
            this.labelMetriqueHA1.setText("Prix d'achat HT au mètre carré");
            this.labelMetriqueVT1.setText("Prix de vente HT au mètre carré");
            break;
        case ReferenceArticleSQLElement.AU_METRE_LARGEUR:
        case ReferenceArticleSQLElement.AU_METRE_LONGUEUR:
            this.labelMetriqueHA1.setText("Prix d'achat HT au mètre");
            this.labelMetriqueVT1.setText("Prix de vente HT au mètre");
            break;

        case ReferenceArticleSQLElement.AU_POID_METRECARRE:
            this.labelMetriqueHA1.setText("Prix d'achat HT au kilo");
            this.labelMetriqueVT1.setText("Prix de vente HT au kilo");
            break;
        case -1:
            // No break need to enable the listener
        default:
            this.labelMetriqueHA1.setEnabled(false);
            this.labelMetriqueVT1.setEnabled(false);
            this.textMetrique1HA.setEnabled(false);
            this.textMetrique1VT.setEnabled(false);

            this.textMetrique1HA.setText(this.textPAHT.getText().trim());
            this.textMetrique1VT.setText(this.textPVHT.getText().trim());
            this.textPAHT.getDocument().addDocumentListener(this.pieceHAArticle);
            this.textPVHT.getDocument().addDocumentListener(this.pieceVTArticle);
            break;
        }
        this.tableTarifVente.fireModification();
    }

    @Override
    public int insert(SQLRow order) {
        int id = super.insert(order);
        this.tableTarifVente.updateField("ID_ARTICLE", id);
        this.tableDes.updateField("ID_ARTICLE", id);
        if (this.codeFournisseurTable != null) {
            this.codeFournisseurTable.updateField("ID_ARTICLE", id);
        }
        return id;
    }

    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues rowVals = new SQLRowValues(getTable());

        SQLRow row = getTable().getRow(getTable().getUndefinedID());

        rowVals.put("ID_TAXE", row.getInt("ID_TAXE"));
        rowVals.put("ID_UNITE_VENTE", UniteVenteArticleSQLElement.A_LA_PIECE);
        rowVals.put("ID_MODE_VENTE_ARTICLE", ReferenceArticleSQLElement.A_LA_PIECE);
        selectModeVente(ReferenceArticleSQLElement.A_LA_PIECE);
        rowVals.put("VALEUR_METRIQUE_1", Float.valueOf("1.0"));
        rowVals.put("PA_HT", BigDecimal.ZERO);
        rowVals.put("POIDS", Float.valueOf(0));

        return rowVals;
    }

    private void setTextHT() {
        this.textPVHT.getDocument().removeDocumentListener(this.htDocListener);
        String textTTC = this.textPVTTC.getText().trim();
        if (textTTC.length() > 0) {
            BigDecimal ttc = new BigDecimal(textTTC);
            int id = this.comboSelTaxe.getSelectedId();
            if (id > 1) {
                Float resultTaux = TaxeCache.getCache().getTauxFromId(id);
                float taux = (resultTaux == null) ? 0.0F : resultTaux.floatValue() / 100.0F;
                this.textPVHT.setText(ttc.divide(BigDecimal.valueOf(taux).add(BigDecimal.ONE), MathContext.DECIMAL128)
                        .setScale(getTable().getField("PV_HT").getType().getDecimalDigits(), RoundingMode.HALF_UP).toString());

            }
        }
        this.textPVHT.getDocument().addDocumentListener(this.htDocListener);
    }

    private void setTextTTC() {
        System.out.println("setTTC");
        this.textPVTTC.getDocument().removeDocumentListener(this.ttcDocListener);

        String textHT = this.textPVHT.getText().trim();
        if (textHT.length() > 0) {
            BigDecimal ht = new BigDecimal(textHT);
            int id = this.comboSelTaxe.getSelectedId();
            if (id > 1) {
                Float resultTaux = TaxeCache.getCache().getTauxFromId(id);
                float taux = (resultTaux == null) ? 0.0F : resultTaux.floatValue() / 100.0F;
                this.textPVTTC.setText(ht.multiply(BigDecimal.valueOf(taux).add(BigDecimal.ONE), MathContext.DECIMAL128)
                        .setScale(getTable().getField("PV_TTC").getType().getDecimalDigits(), RoundingMode.HALF_UP).toString());

            }
        }
        this.textPVTTC.getDocument().addDocumentListener(this.ttcDocListener);
    }

    /**
     * calcul du prix achat et vente ainsi que le poids total pour la piece
     */
    private void updatePiece() {
        if (this.comboSelModeVente.getSelectedId() > 1 && this.comboSelModeVente.getSelectedId() != ReferenceArticleSQLElement.A_LA_PIECE) {
            SQLRowValues rowVals = getDetailsRowValues();
            float poidsTot = ReferenceArticleSQLElement.getPoidsFromDetails(rowVals);
            this.textPoids.setText(String.valueOf(poidsTot));
            this.textPAHT.setText(ReferenceArticleSQLElement.getPrixHAFromDetails(rowVals).setScale(getTable().getField("PA_HT").getType().getDecimalDigits(), RoundingMode.HALF_UP).toString());
            this.textPVHT.setText(ReferenceArticleSQLElement.getPrixVTFromDetails(rowVals).setScale(getTable().getField("PV_HT").getType().getDecimalDigits(), RoundingMode.HALF_UP).toString());
            this.tableTarifVente.fireModification();
        }
    }

    public int getSelectedTaxe() {
        return this.comboSelTaxe.getSelectedId();
    }

    public SQLRowValues getDetailsRowValues() {
        final SQLRowValues rowVals = new SQLRowValues(getTable());
        String textHA = this.textMetrique1HA.getText();
        rowVals.put("PRIX_METRIQUE_HA_1", textHA.trim().length() == 0 ? BigDecimal.ZERO : new BigDecimal(textHA));

        String textVT = this.textMetrique1VT.getText();
        rowVals.put("PRIX_METRIQUE_VT_1", textVT.trim().length() == 0 ? BigDecimal.ZERO : new BigDecimal(textVT));

        put(rowVals, this.textValMetrique1);
        put(rowVals, this.textValMetrique2);
        put(rowVals, this.textValMetrique3);
        System.err.println("Unchecked value " + this.comboSelModeVente.getSelectedId());
        rowVals.put("ID_MODE_VENTE_ARTICLE", this.comboSelModeVente.getSelectedId());

        return rowVals;
    }

    private void put(SQLRowValues rowVals, JTextField comp) {
        Float f = (comp.getText() == null || comp.getText().trim().length() == 0) ? 0.0F : Float.valueOf(comp.getText());
        rowVals.put(this.getView(comp).getField().getName(), f);
    }

}
