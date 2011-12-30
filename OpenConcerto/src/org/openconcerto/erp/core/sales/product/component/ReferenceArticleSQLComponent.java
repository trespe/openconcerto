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
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.product.ui.ArticleDesignationTable;
import org.openconcerto.erp.core.sales.product.ui.ArticleTarifTable;
import org.openconcerto.erp.model.PrixHT;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.preferences.DefaultProps;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ReferenceArticleSQLComponent extends BaseSQLComponent {

    private DeviseField textPVHT, textPVTTC, textPAHT;
    private DeviseField textMetrique1VT, textMetrique1HA;

    final JCheckBox boxService = new JCheckBox(getLabelFor("SERVICE"));
    final JCheckBox checkObs = new JCheckBox(getLabelFor("OBSOLETE"));
    private JTextField textNom, textCode;
    private JTextField textPoids;
    private JTextField textValMetrique1, textValMetrique2, textValMetrique3;
    private DocumentListener htDocListener, ttcDocListener, detailsListener;
    PropertyChangeListener propertyChangeListener;
    private PropertyChangeListener taxeListener;
    final ElementComboBox comboSelTaxe = new ElementComboBox(false, 25);
    final ElementComboBox comboSelModeVente = new ElementComboBox(false, 25);
    private JLabel labelMetriqueHA1 = new JLabel(getLabelFor("PRIX_METRIQUE_HA_1"));
    private JLabel labelMetriqueVT1 = new JLabel(getLabelFor("PRIX_METRIQUE_VT_1"));

    ArticleDesignationTable tableDes = new ArticleDesignationTable();
    ArticleTarifTable tableTarifVente = new ArticleTarifTable(this);

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

    final DeviseField textMarge = new DeviseField();

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
                Long vt = (Long) ReferenceArticleSQLComponent.this.textPVHT.getUncheckedValue();
                Long ha = (Long) ReferenceArticleSQLComponent.this.textPAHT.getUncheckedValue();
                if (vt != null && ha != null) {
                    if (vt != 0 && ha != 0) {
                        // double d = (double) vt / (double) ha;
                        long margeHT = vt - ha;

                        double value;
                        if (DefaultNXProps.getInstance().getBooleanValue(TotalPanel.MARGE_MARQUE, false)) {
                            if (vt > 0) {

                                value = Math.round((double) margeHT / (double) vt * 10000.0);
                            } else {
                                value = 0;
                            }
                        } else {
                            value = Math.round((double) margeHT / (double) ha * 10000.0);
                        }

                        if (value > 0) {
                            ReferenceArticleSQLComponent.this.textMarge.setText(GestionDevise.currencyToString(Double.valueOf(value).longValue()));
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
        if (this.textMarge.getText().trim().length() > 0) {

            Long ha = (Long) this.textPAHT.getUncheckedValue();
            if (ha != null && this.textMarge.getText().trim().length() > 0) {
                final String replaceAll = this.textMarge.getText().replaceAll(",", ".");
                double d = Double.parseDouble(replaceAll.replaceAll(" ", ""));
                if (DefaultNXProps.getInstance().getBooleanValue(TotalPanel.MARGE_MARQUE, false)) {
                    final double e = 1.0 - (d / 100.0);
                    if (e == 0) {
                        this.textPVHT.setText("0");
                    } else {
                        this.textPVHT.setText(GestionDevise.currencyToString(Math.round(ha / e)));
                    }
                } else {
                    this.textPVHT.setText(GestionDevise.currencyToString(Math.round(ha * ((d / 100.0) + 1))));
                }
            }
        }
    }

    public ReferenceArticleSQLComponent(SQLElement elt) {
        super(elt);
    }

    @Override
    public void select(SQLRowAccessor r) {
        // TODO Auto-generated method stub

        super.select(r);
        if (r != null && r.getID() > getTable().getUndefinedID()) {
            this.checkObs.setVisible(true);
            this.tableTarifVente.setArticleValues(r);
            this.tableTarifVente.insertFrom("ID_ARTICLE", r.getID());
            this.tableDes.insertFrom("ID_ARTICLE", r.getID());
        }
    }

    public void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        this.textPVHT = new DeviseField();
        this.textPVTTC = new DeviseField();
        this.textPAHT = new DeviseField();
        this.textPVHT.getDocument().addDocumentListener(this.listenerMargeTextVT);

        // Init metrique devise field
        this.textMetrique1HA = new DeviseField();
        this.textMetrique1VT = new DeviseField();

        // init metrique value field
        this.textValMetrique1 = new JTextField();
        this.textValMetrique2 = new JTextField();
        this.textValMetrique3 = new JTextField();

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
        final JPanel addP = new JPanel();
        this.setAdditionalFieldsPanel(new FormLayouter(addP, 2));
        this.add(addP, c);       

        JTabbedPane pane = new JTabbedPane();
        c.gridy++;
        c.weightx = 1;
        c.weighty = 0.7;

        pane.add("Tarifs de vente", createTarifPanel());
        pane.add("Exportation", createExportationPanel());
        pane.add("Achat", createAchatPanel());
        pane.add("Stock", createStockPanel());
        pane.add("Descriptif", createDescriptifPanel());
        pane.add("Désignations multilingues", createDesignationPanel());
        c.fill = GridBagConstraints.BOTH;
        this.add(pane, c);

        c.gridy++;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new TitledSeparator("Informations complémentaires"), c);

        c.gridy++;
        c.weighty = 0.3;
        JTextArea infos = new JTextArea();
        c.fill = GridBagConstraints.BOTH;
        this.add(infos, c);

        this.addSQLObject(infos, "INFOS");
        this.addSQLObject(this.textMetrique1HA, "PRIX_METRIQUE_HA_1");
        this.addSQLObject(this.textMetrique1VT, "PRIX_METRIQUE_VT_1");
        this.addSQLObject(this.textValMetrique1, "VALEUR_METRIQUE_1");
        this.addSQLObject(this.textValMetrique2, "VALEUR_METRIQUE_2");
        this.addSQLObject(this.textValMetrique3, "VALEUR_METRIQUE_3");
        this.addSQLObject(this.comboSelModeVente, "ID_MODE_VENTE_ARTICLE");
        this.addSQLObject(this.boxService, "SERVICE");
        this.addSQLObject(comboSelFamille, "ID_FAMILLE_ARTICLE");

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

    private Component createDescriptifPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new DefaultGridBagConstraints();

        // Obsolete
        c.fill = GridBagConstraints.NONE;
        c.weightx = 1;
        this.checkObs.setOpaque(false);
        panel.add(this.checkObs, c);

        this.checkObs.setVisible(false);
        this.addView(this.checkObs, "OBSOLETE");

        ITextArea area = new ITextArea();
        JLabel sep = new JLabel("Descriptif complet");
        c.gridy++;
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
                // TODO Raccord de méthode auto-généré
                fieldQteMin.setEnabled(boxStock.isSelected());
                fieldQteAchat.setEnabled(boxStock.isSelected());
            }
        });

        boxStock.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Raccord de méthode auto-généré
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

        c.gridy++;
        c.weighty = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        final JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        panel.add(spacer, c);
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
                rowVals.put("PRIX_METRIQUE_VT_1", Long.valueOf(0));
                rowVals.put("PV_HT", Long.valueOf(0));
                rowVals.put("PV_TTC", Long.valueOf(0));
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

        // PA devise
        JPanel pDevise = new JPanel();
        pDevise.add(new JLabel("Devise du fournisseur"));
        final ElementComboBox boxDevise = new ElementComboBox();

        pDevise.add(boxDevise);
        this.addView(boxDevise, "ID_DEVISE_HA");

        pDevise.add(new JLabel("Prix d'achat devise"));
        final DeviseField fieldHAD = new DeviseField();
        pDevise.add(fieldHAD);
        this.addView(fieldHAD, "PA_DEVISE");
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(pDevise, c);
        fieldHAD.getDocument().addDocumentListener(new SimpleDocumentListener() {

            @Override
            public void update(DocumentEvent e) {

                Long ha = (Long) fieldHAD.getUncheckedValue();
                if (ha != null && fieldHAD.getText().trim().length() > 0) {
                    BigDecimal taux = BigDecimal.ONE;
                    if (boxDevise != null && boxDevise.getSelectedRow() != null && !boxDevise.getSelectedRow().isUndefined()) {
                        taux = (BigDecimal) boxDevise.getSelectedRow().getObject("TAUX");
                        textPAHT.setValue(GestionDevise.currencyToString(taux.multiply(new BigDecimal(ha)).longValue()));
                    }
                }
            }
        });

        // PA
        JPanel p = new JPanel();
        p.add(new JLabel(getLabelFor("PA_HT")));
        p.add(this.textPAHT);
        this.textPAHT.getDocument().addDocumentListener(this.listenerMargeTextHA);

        // Marge
        p.add(new JLabel("Marge"));
        Set<SQLField> set = new HashSet<SQLField>();
        set.add(getTable().getField("PA_HT"));
        this.textMarge.init("Marge", set);
        p.add(this.textMarge);
        this.textMarge.getDocument().addDocumentListener(this.listenerMargeTextMarge);
        p.add(new JLabel("%           "));

        // Poids
        JLabel labelPds = new JLabel(getLabelFor("POIDS"));
        p.add(labelPds);
        labelPds.setHorizontalAlignment(SwingConstants.RIGHT);
        p.add(this.textPoids);

        // Service
        String sService = props.getStringProperty("ArticleService");
        Boolean bService = Boolean.valueOf(sService);
        if (bService != null && bService.booleanValue()) {
            p.add(this.boxService);
        }

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;

        this.add(p, c);

        // PV HT
        JPanel p2 = new JPanel();
        p2.add(new JLabel(getLabelFor("PV_HT")));
        p2.add(this.textPVHT);

        // Taxe
        JLabel labelTaxe = new JLabel(getLabelFor("ID_TAXE"));
        p2.add(labelTaxe);
        labelTaxe.setHorizontalAlignment(SwingConstants.RIGHT);

        p2.add(this.comboSelTaxe);

        // PV_TTC
        p2.add(new JLabel(getLabelFor("PV_TTC")));
        p2.add(this.textPVTTC);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(p2, c);

        this.addRequiredSQLObject(this.textPAHT, "PA_HT");
        this.addRequiredSQLObject(this.textPVHT, "PV_HT");
        this.addRequiredSQLObject(this.comboSelTaxe, "ID_TAXE");
        this.addRequiredSQLObject(this.textPVTTC, "PV_TTC");

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
                // TODO Auto-generated method stub
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
        JLabel labelDetails = new JLabel("Article détaillé");
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
        this.add(new JLabel(getLabelFor("ID_MODE_VENTE_ARTICLE")), c);

        c.gridx++;
        this.add(this.comboSelModeVente, c);

        // Prix metrique
        c.gridx = 0;
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
        JLabel labelMetrique1 = new JLabel(getLabelFor("VALEUR_METRIQUE_1"));
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
        JLabel labelMetrique2 = new JLabel(getLabelFor("VALEUR_METRIQUE_2"));
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
        JLabel labelMetrique3 = new JLabel(getLabelFor("VALEUR_METRIQUE_3"));
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
        JLabel labelPiece = new JLabel("Article pièce");
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
            break;
        default:
            this.labelMetriqueHA1.setEnabled(false);
            this.labelMetriqueVT1.setEnabled(false);
            this.textMetrique1HA.setEnabled(false);
            this.textMetrique1VT.setEnabled(false);

            this.textMetrique1HA.setValue(this.textPAHT.getText().trim());
            this.textMetrique1VT.setValue(this.textPVHT.getText().trim());
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
        return id;
    }

    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues rowVals = new SQLRowValues(getTable());

        SQLRow row = getTable().getRow(getTable().getUndefinedID());

        rowVals.put("ID_TAXE", row.getInt("ID_TAXE"));
        rowVals.put("ID_MODE_VENTE_ARTICLE", ReferenceArticleSQLElement.A_LA_PIECE);
        selectModeVente(ReferenceArticleSQLElement.A_LA_PIECE);
        rowVals.put("VALEUR_METRIQUE_1", Float.valueOf("1.0"));
        rowVals.put("PA_HT", Long.valueOf(0));
        rowVals.put("POIDS", Float.valueOf(0));

        // SQLTable tableTarif = getTable().getTable("TARIF");
        // SQLTable tableArticleTarif = getTable().getTable("ARTICLE_TARIF");
        // SQLSelect sel = new SQLSelect(getTable().getBase());
        // sel.addSelectStar(tableTarif);
        // this.table.getRowValuesTable().getRowValuesTableModel().clearRows();
        // System.err.println(sel.asString());
        // List<SQLRow> rows = (List<SQLRow>)
        // Configuration.getInstance().getBase().getDataSource().execute(sel.asString(),
        // SQLRowListRSH.createFromSelect(sel));
        // for (SQLRow sqlRow : rows) {
        // SQLRowValues rowVals2 = new SQLRowValues(tableArticleTarif);
        // rowVals2.put("PRIX_REVENTE_HT", Long.valueOf(0));
        // rowVals2.put("PRIX_FINAL_TTC", Long.valueOf(0));
        // rowVals2.put("ID_TARIF", sqlRow.getID());
        // this.table.getRowValuesTable().getRowValuesTableModel().addRow(rowVals2);
        // }

        return rowVals;
    }

    private void setTextHT() {
        this.textPVHT.getDocument().removeDocumentListener(this.htDocListener);

        String textTTC = this.textPVTTC.getText().trim();
        PrixTTC ttc = new PrixTTC(GestionDevise.parseLongCurrency(textTTC));
        int id = this.comboSelTaxe.getSelectedId();
        if (id > 1) {

            Float resultTaux = TaxeCache.getCache().getTauxFromId(id);
            float taux = (resultTaux == null) ? 0.0F : resultTaux.floatValue() / 100.0F;

            // float taux = (TaxeCache.getCache().getTauxFromId(id)) / 100.0F;
            this.textPVHT.setText(GestionDevise.currencyToString(ttc.calculLongHT(taux)));
        }
        this.textPVHT.getDocument().addDocumentListener(this.htDocListener);
    }

    private void setTextTTC() {
        System.out.println("setTTC");
        this.textPVTTC.getDocument().removeDocumentListener(this.ttcDocListener);

        String textHT = this.textPVHT.getText().trim();
        PrixHT ht = new PrixHT(GestionDevise.parseLongCurrency(textHT));
        int id = this.comboSelTaxe.getSelectedId();
        if (id > 1) {

            Float resultTaux = TaxeCache.getCache().getTauxFromId(id);
            float taux = (resultTaux == null) ? 0.0F : resultTaux.floatValue() / 100.0F;

            // float taux = (TaxeCache.getCache().getTauxFromId(id)) / 100.0F;
            this.textPVTTC.setText(GestionDevise.currencyToString(ht.calculLongTTC(taux)));
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

            this.textPAHT.setText(GestionDevise.currencyToString(ReferenceArticleSQLElement.getPrixHAFromDetails(rowVals)));

            this.textPVHT.setText(GestionDevise.currencyToString(ReferenceArticleSQLElement.getPrixVTFromDetails(rowVals)));

            this.tableTarifVente.fireModification();
        }
    }

    public int getSelectedTaxe() {
        return this.comboSelTaxe.getSelectedId();
    }

    public SQLRowValues getDetailsRowValues() {
        SQLRowValues rowVals = new SQLRowValues(getTable());
        rowVals.put(this.textMetrique1HA.getField().getName(), this.textMetrique1HA.getUncheckedValue());

        rowVals.put(this.textMetrique1VT.getField().getName(), this.textMetrique1VT.getUncheckedValue());

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
