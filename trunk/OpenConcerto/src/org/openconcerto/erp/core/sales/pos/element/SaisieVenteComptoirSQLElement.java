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
 
 package org.openconcerto.erp.core.sales.pos.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.erp.generationEcritures.GenerationMvtSaisieVenteComptoir;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.erp.preferences.ModeReglementDefautPrefPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.ISQLElementWithCodeSelector;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.warning.JLabelWarning;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.EmptyObj;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class SaisieVenteComptoirSQLElement extends ComptaSQLConfElement {

    public SaisieVenteComptoirSQLElement() {
        super("SAISIE_VENTE_COMPTOIR", "une saisie d'une vente comptoir", "saisies de  ventes comptoir");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("NOM");
        l.add("ID_CLIENT");
        l.add("MONTANT_HT");
        l.add("MONTANT_TTC");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("MONTANT_TTC");
        return l;
    }

    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_MODE_REGLEMENT");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private SQLTextCombo textNom;
            private DeviseField textMontantTTC;
            private DeviseField textMontantService;
            private ElementComboBox comboFournisseur;
            private JTextField textEcheance;

            private ElementComboBox comboTaxe;
            private DeviseField textMontantHT;
            private JCheckBox checkCommande;
            private JDate dateSaisie;

            private ISQLElementWithCodeSelector nomArticle;
            private Date dateEch;
            private JLabel labelEcheancejours = new JLabel("jours");
            private ElementComboBox comboAvoir;
            private ElementComboBox comboClient;

            private final JLabel labelWarning = new JLabelWarning("le montant du service ne peut pas dépasser le total HT!");
            private ValidState validState = ValidState.getTrueInstance();
            // FIXME: use w
            private Where w;
            private DocumentListener docTTCListen;
            private PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent evt) {

                    if ((nomArticle.getValidState().isValid()) && (!nomArticle.isEmpty())) {
                        int idArticle = nomArticle.getValue().intValue();

                        SQLTable tableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
                        SQLRow rowArticle = tableArticle.getRow(idArticle);
                        if (rowArticle != null) {
                            comboTaxe.setValue(rowArticle.getInt("ID_TAXE"));

                            textMontantTTC.setText(((BigDecimal) rowArticle.getObject("PV_TTC")).toString());
                        }
                        System.out.println("value article Changed");

                    }
                }
            };

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                this.docTTCListen = new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {
                        calculMontant();
                    }

                    public void removeUpdate(DocumentEvent e) {
                        calculMontant();
                    }

                    public void insertUpdate(DocumentEvent e) {
                        calculMontant();
                    }
                };

                /***********************************************************************************
                 * * RENSEIGNEMENTS
                 **********************************************************************************/
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridheight = 1;
                TitledSeparator sep = new TitledSeparator("Renseignements");
                this.add(sep, c);
                c.insets = new Insets(2, 2, 1, 2);

                c.gridwidth = 1;

                // Libellé vente
                JLabel labelNom = new JLabel(getLabelFor("NOM"));
                labelNom.setHorizontalAlignment(SwingConstants.RIGHT);
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelNom, c);

                this.textNom = new SQLTextCombo();
                c.gridx++;
                c.weightx = 1;
                c.gridwidth = 2;
                this.add(this.textNom, c);

                // Date
                JLabel labelDate = new JLabel(getLabelFor("DATE"));

                this.dateSaisie = new JDate(true);
                c.gridwidth = 1;
                c.gridx += 2;
                c.weightx = 0;
                labelDate.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelDate, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.dateSaisie, c);

                // article
                c.gridy++;
                c.gridx = 0;

                SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
                SQLElement article = Configuration.getInstance().getDirectory().getElement(sqlTableArticle);
                this.nomArticle = new ISQLElementWithCodeSelector(article, article.getTable().getField("CODE"));

                JLabel labelNomArticle = new JLabel(getLabelFor("ID_ARTICLE"));
                c.weightx = 0;
                labelNomArticle.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelNomArticle, c);

                c.gridx++;
                c.weightx = 0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(this.nomArticle, c);

                this.nomArticle.addValueListener(this.propertyChangeListener);

                // client
                this.comboClient = new ElementComboBox();
                this.comboClient.addValueListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {

                        if (comboClient.isEmpty()) {
                            w = new Where(getTable().getBase().getTable("AVOIR_CLIENT").getField("ID_CLIENT"), "=", -1);
                        } else {
                            w = new Where(getTable().getBase().getTable("AVOIR_CLIENT").getField("ID_CLIENT"), "=", comboClient.getSelectedId());
                        }
                    }
                });

                JLabel labelNomClient = new JLabel(getLabelFor("ID_CLIENT"));
                c.gridy++;
                c.gridx = 0;
                c.gridwidth = 1;
                c.weightx = 0;
                labelNomClient.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelNomClient, c);

                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 0;
                this.add(this.comboClient, c);

                // Selection d'un avoir si le client en possede

                this.comboAvoir = new ElementComboBox();

                JLabel labelAvoirClient = new JLabel(getLabelFor("ID_AVOIR_CLIENT"));
                c.gridy++;
                c.gridx = 0;
                c.gridwidth = 1;
                c.weightx = 0;
                labelAvoirClient.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelAvoirClient, c);

                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 0;
                this.add(this.comboAvoir, c);

                /***********************************************************************************
                 * * MONTANT
                 **********************************************************************************/
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridx = 0;
                c.gridy++;
                c.weightx = 1;
                sep = new TitledSeparator("Montant en Euros");
                c.insets = new Insets(10, 2, 1, 2);
                this.add(sep, c);
                c.insets = new Insets(2, 2, 1, 2);

                c.gridwidth = 1;

                // Montant TTC
                JLabel labelMontantTTC = new JLabel(getLabelFor("MONTANT_TTC"));
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                labelMontantTTC.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelMontantTTC, c);

                this.textMontantTTC = new DeviseField();
                c.gridx++;
                c.weightx = 0;
                this.add(this.textMontantTTC, c);

                this.textMontantTTC.getDocument().addDocumentListener(this.docTTCListen);

                // Choix TVA
                c.gridx++;
                c.weightx = 0;
                c.gridwidth = 1;
                this.comboTaxe = new ElementComboBox(false);

                c.fill = GridBagConstraints.NONE;

                this.add(this.comboTaxe, c);

                // Montant HT
                c.fill = GridBagConstraints.HORIZONTAL;
                JLabel labelMontantHT = new JLabel(getLabelFor("MONTANT_HT"));
                c.weightx = 0;
                c.gridy++;
                c.gridx = 0;
                c.gridwidth = 1;
                labelMontantHT.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelMontantHT, c);

                this.textMontantHT = new DeviseField();
                c.gridx++;
                c.weightx = 1;
                this.add(this.textMontantHT, c);

                this.textMontantHT.setEditable(false);
                this.textMontantHT.setEnabled(false);

                // Montant Service
                final JCheckBox checkService = new JCheckBox("dont ");
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                this.add(checkService, c);

                checkService.addActionListener(new ActionListener() {

                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        boolean b = checkService.isSelected();
                        textMontantService.setEditable(b);
                        textMontantService.setEnabled(b);

                        if (!b) {
                            textMontantService.setText("");
                            // montantServiceValide = true;
                        }
                    };
                });

                this.textMontantService = new DeviseField();
                c.gridx++;
                c.weightx = 1;
                this.add(this.textMontantService, c);

                JLabel labelMontantService = new JLabel("de service HT");
                c.weightx = 0;
                c.gridx++;
                this.add(labelMontantService, c);

                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(this.labelWarning, c);
                this.labelWarning.setVisible(false);

                /***********************************************************************************
                 * * MODE DE REGLEMENT
                 **********************************************************************************/
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridx = 0;
                c.gridy++;
                sep = new TitledSeparator("Mode de règlement");
                c.insets = new Insets(10, 2, 1, 2);
                this.add(sep, c);
                c.insets = new Insets(2, 2, 1, 2);

                c.gridx = 0;
                c.gridy++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.addView("ID_MODE_REGLEMENT", REQ + ";" + DEC + ";" + SEP);
                ElementSQLObject eltModeRegl = (ElementSQLObject) this.getView("ID_MODE_REGLEMENT");
                this.add(eltModeRegl, c);

                /***********************************************************************************
                 * * COMMANDE
                 **********************************************************************************/
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridx = 0;
                c.gridy++;
                sep = new TitledSeparator("Commande");
                c.insets = new Insets(10, 2, 1, 2);
                this.add(sep, c);
                c.insets = new Insets(2, 2, 1, 2);

                this.checkCommande = new JCheckBox("Produit à commander");

                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                c.gridwidth = 2;
                this.add(this.checkCommande, c);

                this.checkCommande.addActionListener(new ActionListener() {

                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        boolean b = checkCommande.isSelected();
                        comboFournisseur.setEnabled(b);
                        comboFournisseur.setEditable(b);

                        textEcheance.setEditable(b);
                        textEcheance.setEnabled(b);

                        updateValidState();
                    };
                });

                // Fournisseurs
                JLabel labelFournisseur = new JLabel(getLabelFor("ID_FOURNISSEUR"));
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                c.gridwidth = 1;
                this.add(labelFournisseur, c);

                this.comboFournisseur = new ElementComboBox();

                c.gridx++;
                c.weightx = 1;
                c.gridwidth = 4;
                this.add(this.comboFournisseur, c);

                // Echeance
                JLabel labelEcheance = new JLabel("Echeance");
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                c.gridwidth = 1;
                labelEcheance.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelEcheance, c);

                c.gridx++;
                c.weightx = 1;
                this.textEcheance = new JTextField();
                this.add(this.textEcheance, c);
                this.textEcheance.addKeyListener(new KeyAdapter() {

                    public void keyReleased(KeyEvent e) {
                        calculDate();
                    }
                });

                c.gridx++;
                c.weightx = 0;
                this.add(this.labelEcheancejours, c);

                /***********************************************************************************
                 * * INFORMATIONS COMPLEMENTAIRES
                 **********************************************************************************/
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridx = 0;
                c.gridy++;
                sep = new TitledSeparator("Informations complémentaires");
                c.insets = new Insets(10, 2, 1, 2);
                this.add(sep, c);
                c.insets = new Insets(2, 2, 1, 2);

                ITextArea textInfos = new ITextArea();

                c.gridx = 0;
                c.gridy++;
                c.gridheight = GridBagConstraints.REMAINDER;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1;
                c.weighty = 1;
                c.fill = GridBagConstraints.BOTH;
                this.add(textInfos, c);

                this.addSQLObject(this.textNom, "NOM");
                this.addRequiredSQLObject(this.comboClient, "ID_CLIENT");
                this.addSQLObject(this.nomArticle, "ID_ARTICLE");
                this.addRequiredSQLObject(this.textMontantTTC, "MONTANT_TTC");
                this.addRequiredSQLObject(this.textMontantHT, "MONTANT_HT");
                this.addSQLObject(this.textMontantService, "MONTANT_SERVICE");
                this.addRequiredSQLObject(this.dateSaisie, "DATE");
                this.addSQLObject(textInfos, "INFOS");
                this.addSQLObject(this.comboFournisseur, "ID_FOURNISSEUR");
                this.addSQLObject(this.textEcheance, "ECHEANCE");
                this.addRequiredSQLObject(this.comboTaxe, "ID_TAXE");
                this.addSQLObject(this.comboAvoir, "ID_AVOIR_CLIENT");
                this.comboTaxe.setButtonsVisible(false);
                this.comboTaxe.setValue(2);

                checkService.setSelected(false);
                this.textMontantService.setEditable(false);
                this.textMontantService.setEnabled(false);

                this.checkCommande.setSelected(false);
                this.comboFournisseur.setEditable(false);
                this.comboFournisseur.setEnabled(false);
                this.textEcheance.setEditable(false);
                this.textEcheance.setEnabled(false);

                this.comboTaxe.addValueListener(new PropertyChangeListener() {

                    public void propertyChange(PropertyChangeEvent evt) {
                        calculMontant();
                    }
                });

                final SimpleDocumentListener docL = new SimpleDocumentListener() {
                    @Override
                    public void update(DocumentEvent e) {
                        updateValidState();
                    }
                };
                this.textMontantService.getDocument().addDocumentListener(docL);
                this.textMontantHT.getDocument().addDocumentListener(docL);
                this.comboFournisseur.addEmptyListener(new EmptyListener() {
                    @Override
                    public void emptyChange(EmptyObj src, boolean newValue) {
                        updateValidState();
                    }
                });

                /*
                 * this.nomClient.addValueListener(new PropertyChangeListener() {
                 * 
                 * public void propertyChange(PropertyChangeEvent evt) { if (nomClient.isValid()) {
                 * System.err.println("Changed Combo Avoir"); comboAvoir = new ElementComboBox(new
                 * AvoirClientSQLElement(new Where(new
                 * AvoirClientSQLElement().getTable().getField("ID_CLIENT"), "=",
                 * nomClient.getValue()))); if (comboAvoir != null) { comboAvoir.setEnabled(true); }
                 * } else { comboAvoir.setEnabled(false); } } });
                 */

            }

            private void calculMontant() {

                float taux;
                // PrixHT pHT;
                PrixTTC pTTC;
                // taux de la TVA selectionnee
                int idTaxe = this.comboTaxe.getSelectedId();
                if (idTaxe > 1) {
                    SQLRow ligneTaxe = getTable().getBase().getTable("TAXE").getRow(idTaxe);
                    if (ligneTaxe != null) {
                        taux = (ligneTaxe.getFloat("TAUX")) / 100.0F;

                        // calcul des montants HT ou TTC
                        if (this.textMontantTTC.getText().trim().length() > 0) {

                            if (this.textMontantTTC.getText().trim().equals("-")) {
                                pTTC = new PrixTTC(0);
                            } else {
                                pTTC = new PrixTTC(GestionDevise.parseLongCurrency(this.textMontantTTC.getText()));
                            }

                            // affichage
                            updateTextHT(GestionDevise.currencyToString(pTTC.calculLongHT(taux)));
                        } else {
                            updateTextHT("");
                        }
                    }
                }
            }

            private boolean isMontantServiceValid() {
                String montant = this.textMontantService.getText().trim();
                String montantHT = this.textMontantHT.getText().trim();

                boolean b;
                if (montant.length() == 0) {
                    b = true;
                } else {
                    if (montantHT.length() == 0) {
                        b = false;
                    } else {
                        b = (GestionDevise.parseLongCurrency(montantHT) >= GestionDevise.parseLongCurrency(montant));
                    }
                }

                this.labelWarning.setVisible(!b);
                System.err.println("Montant service is valid ? " + b + " --> HT val " + montantHT + " --> service val " + montant);

                return b;
            }

            /*
             * private void calculMontantHT() {
             * 
             * float taux; PrixHT pHT;
             * 
             * if (!this.comboTaxe.isEmpty()) { // taux de la TVA selectionnee SQLRow ligneTaxe =
             * Configuration.getInstance().getBase().getTable("TAXE").getRow(((Integer)
             * this.comboTaxe.getValue()).intValue()); taux = (ligneTaxe.getFloat("TAUX")) / 100; //
             * calcul des montants HT ou TTC if (this.textMontantHT.getText().trim().length() > 0) {
             * 
             * if (this.textMontantHT.getText().trim().equals("-")) { pHT = new PrixHT(0); } else {
             * pHT = new PrixHT(Float.parseFloat(this.textMontantHT.getText())); } // affichage
             * updateTextTTC(String.valueOf(pHT.CalculTTC(taux))); } else updateTextTTC(""); } }
             */

            private void updateTextHT(final String prixHT) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {

                        textMontantHT.setText(prixHT);
                    }
                });
            }

            @Override
            protected SQLRowValues createDefaults() {
                SQLRowValues vals = new SQLRowValues(this.getTable());
                SQLRowAccessor r;

                try {
                    r = ModeReglementDefautPrefPanel.getDefaultRow(true);
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

            // private void updateTextTTC(final String prixTTC) {
            // SwingUtilities.invokeLater(new Runnable() {
            //
            // public void run() {
            //
            // if (docTTCListen != null) {
            //
            // textMontantTTC.getDocument().removeDocumentListener(docTTCListen);
            // }
            //
            // textMontantTTC.setText(prixTTC);
            //
            // // textTaxe.setText(prixTVA);
            // textMontantTTC.getDocument().addDocumentListener(docTTCListen);
            // }
            // });
            // }

            private void calculDate() {

                int aJ = 0;

                // on récupére les valeurs saisies
                if (this.textEcheance.getText().trim().length() != 0) {

                    try {
                        aJ = Integer.parseInt(this.textEcheance.getText());
                    } catch (Exception e) {
                        System.out.println("Erreur de format sur TextField Ajour " + this.textEcheance.getText());
                    }
                }

                Calendar cal = Calendar.getInstance();

                // on fixe le temps sur ToDay + Ajour
                cal.setTime(new Date());
                long tempsMil = aJ * 86400000;
                cal.setTimeInMillis(cal.getTimeInMillis() + tempsMil);

                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
                this.dateEch = cal.getTime();
                this.labelEcheancejours.setText("jours, soit le " + dateFormat.format(this.dateEch));
            }

            private void updateValidState() {
                this.setValidState(this.computeValidState());
            }

            private ValidState computeValidState() {
                ValidState res = ValidState.getTrueInstance();
                if (!this.isMontantServiceValid())
                    res = res.and(ValidState.createCached(false, this.labelWarning.getText()));
                if (this.checkCommande.isSelected())
                    res = res.and(ValidState.createCached(!this.comboFournisseur.isEmpty(), "Fournisseur non renseigné"));
                return res;
            }

            private final void setValidState(ValidState validState) {
                if (!validState.equals(this.validState)) {
                    this.validState = validState;
                    this.fireValidChange();
                }
            }

            @Override
            public synchronized ValidState getValidState() {
                return super.getValidState().and(this.validState);
            }

            public int insert(SQLRow order) {

                // On teste si l'article n'existe pas, on le crée
                if (Integer.parseInt(this.nomArticle.getValue().toString()) == -1) {

                    createArticle();
                }

                if (this.textNom.getTextComp().getText().trim().length() <= 0) {
                    this.textNom.getTextComp().setText(this.nomArticle.getTextMain());
                }
                final int id = super.insert(order);
                // on verifie si le produit est à commander
                if (this.checkCommande.isSelected()) {
                    createCommande(id);
                }

                Configuration.getInstance().getNonInteractiveSQLExecutor().execute(new Runnable() {

                    @Override
                    public void run() {

                        final SQLRow rowVC = getTable().getRow(id);
                        // Mise à jour des stocks
                        final SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
                        final SQLRowValues rowVals = new SQLRowValues(eltMvtStock.getTable());
                        rowVals.put("QTE", -1);
                        rowVals.put("NOM", "Saisie vente comptoir");
                        rowVals.put("IDSOURCE", id);
                        rowVals.put("SOURCE", getTable().getName());
                        rowVals.put("ID_ARTICLE", rowVC.getInt("ID_ARTICLE"));
                        rowVals.put("DATE", rowVC.getObject("DATE"));

                        try {
                            final SQLRow row = rowVals.insert();
                            CollectionMap<SQLRow, List<SQLRowValues>> map = ((MouvementStockSQLElement) Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK")).updateStock(
                                    Arrays.asList(row.getID()), false);
                            MouvementStockSQLElement.createCommandeF(map, null);
                            new GenerationMvtSaisieVenteComptoir(id);
                        } catch (SQLException e) {
                            ExceptionHandler.handle("Erreur lors de la création des mouvements", e);
                        }
                    }
                });

                return id;
            }

            private void createCommande(final int id) {

                System.out.println("Ajout d'une commande");
                SQLRow rowSaisie = SaisieVenteComptoirSQLElement.this.getTable().getRow(id);
                Map<String, Object> m = new HashMap<String, Object>();
                // NOM, DATE, ECHEANCE, IDSOURCE, SOURCE, ID_CLI, ID_FOURN, ID_ARTICLE
                m.put("NOM", rowSaisie.getObject("NOM"));
                m.put("DATE", rowSaisie.getObject("DATE"));
                m.put("DATE_ECHEANCE", new java.sql.Date(this.dateEch.getTime()));

                m.put("IDSOURCE", new Integer(id));
                m.put("SOURCE", "SAISIE_VENTE_COMPTOIR");

                m.put("ID_CLIENT", rowSaisie.getObject("ID_CLIENT"));
                m.put("ID_FOURNISSEUR", rowSaisie.getObject("ID_FOURNISSEUR"));
                m.put("ID_ARTICLE", rowSaisie.getObject("ID_ARTICLE"));

                SQLTable tableCmd = getTable().getBase().getTable("COMMANDE_CLIENT");

                SQLRowValues valCmd = new SQLRowValues(tableCmd, m);

                try {
                    if (valCmd.getInvalid() == null) {
                        // ajout de l'ecriture
                        valCmd.insert();
                    }
                } catch (Exception e) {
                    System.err.println("Erreur à l'insertion dans la table " + valCmd.getTable().getName());
                    e.printStackTrace();
                }
            }

            private void createArticle() {
                System.out.println("Création de l'article");

                SQLTable articleTable = getTable().getBase().getTable("ARTICLE");
                String tNomArticle = this.nomArticle.getTextMain();
                String codeArticle = this.nomArticle.getTextOpt();
                int idTaxe = this.comboTaxe.getSelectedId();
                BigDecimal prix = new BigDecimal(this.textMontantHT.getText());
                BigDecimal prixTTC = new BigDecimal(this.textMontantTTC.getText());

                if (tNomArticle.trim().length() == 0) {
                    tNomArticle = "Nom Indefini";
                }
                if (codeArticle.trim().length() == 0) {
                    codeArticle = "Indefini";
                }

                SQLRowValues vals = new SQLRowValues(articleTable);
                vals.put("NOM", tNomArticle);
                vals.put("CODE", codeArticle);
                vals.put("PA_HT", prix);
                vals.put("PV_HT", prix);
                vals.put("PV_TTC", prixTTC);
                vals.put("ID_TAXE", new Integer(idTaxe));
                vals.put("CREATION_AUTO", Boolean.TRUE);

                try {
                    SQLRow row = vals.insert();
                    this.nomArticle.loadCache();
                    this.nomArticle.setValue(row.getID());
                } catch (SQLException e) {

                    e.printStackTrace();
                }
            }

            public void update() {

                if (JOptionPane.showConfirmDialog(this, "Attention en modifiant cette vente comptoir, vous supprimerez les chéques et les échéances associés. Continuer?",
                        "Modification de vente comptoir", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    // on efface les mouvements de stocks associés
                    SQLRow row = getTable().getRow(this.getSelectedID());
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

                    if (this.textNom.getTextComp().getText().trim().length() <= 0) {
                        this.textNom.getTextComp().setText(this.nomArticle.getTextMain());
                    }

                    // On teste si l'article n'existe pas, on le crée
                    if (Integer.parseInt(this.nomArticle.getValue().toString()) == -1) {

                        createArticle();
                    }

                    // TODO check echeance, ---> creation article, commande??
                    super.update();

                    row = getTable().getRow(this.getSelectedID());
                    int idMvt = row.getInt("ID_MOUVEMENT");
                    System.out.println(row.getID() + "__________***************** UPDATE " + idMvt);

                    // on supprime tout ce qui est lié à la facture
                    EcritureSQLElement eltEcr = (EcritureSQLElement) Configuration.getInstance().getDirectory().getElement("ECRITURE");
                    eltEcr.archiveMouvementProfondeur(idMvt, false);

                    // Mise à jour des stocks
                    SQLRowValues rowVals = new SQLRowValues(eltMvtStock.getTable());
                    rowVals.put("QTE", -1);
                    rowVals.put("NOM", "Saisie vente comptoir");
                    rowVals.put("IDSOURCE", getSelectedID());
                    rowVals.put("SOURCE", getTable().getName());
                    rowVals.put("ID_ARTICLE", row.getInt("ID_ARTICLE"));
                    rowVals.put("DATE", row.getObject("DATE"));
                    try {
                        SQLRow rowNew = rowVals.insert();
                        CollectionMap<SQLRow, List<SQLRowValues>> map = ((MouvementStockSQLElement) Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK")).updateStock(
                                Arrays.asList(rowNew.getID()), false);
                        MouvementStockSQLElement.createCommandeF(map, null);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    new GenerationMvtSaisieVenteComptoir(this.getSelectedID(), idMvt);
                }
            }

            @Override
            public void select(SQLRowAccessor r) {
                this.textMontantTTC.getDocument().removeDocumentListener(this.docTTCListen);
                this.nomArticle.removeValueListener(this.propertyChangeListener);

                super.select(r);

                this.textMontantTTC.getDocument().addDocumentListener(this.docTTCListen);
                this.nomArticle.addValueListener(this.propertyChangeListener);
            }
        };
    }

    @Override
    protected void archive(SQLRow row, boolean cutLinks) throws SQLException {

        super.archive(row, cutLinks);

        // Mise à jour des stocks
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
                eltMvtStock.archive(((Number) tmp[0]).intValue());
            }
        }
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".sale";
    }
}
