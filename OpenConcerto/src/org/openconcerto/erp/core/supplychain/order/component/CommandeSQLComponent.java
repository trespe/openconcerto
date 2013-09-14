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
 
 package org.openconcerto.erp.core.supplychain.order.component;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.AbstractVenteArticleItemTable;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.supplychain.order.element.CommandeSQLElement;
import org.openconcerto.erp.core.supplychain.order.ui.CommandeItemTable;
import org.openconcerto.erp.generationDoc.gestcomm.CommandeXmlSheet;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.DefaultElementSQLObject;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.ui.AutoHideListener;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.preferences.DefaultProps;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class CommandeSQLComponent extends TransfertBaseSQLComponent {

    private CommandeItemTable table = new CommandeItemTable();
    private JUniqueTextField numeroUniqueCommande;
    private final SQLTable tableNum = getTable().getBase().getTable("NUMEROTATION_AUTO");
    private final JCheckBox checkImpression = new JCheckBox("Imprimer");
    private final JCheckBox checkVisu = new JCheckBox("Visualiser");
    private final ITextArea infos = new ITextArea(3, 3);
    private ElementComboBox fourn = new ElementComboBox();
    final JCheckBox boxLivrClient = new JCheckBox("Livrer directement le client");
    private DefaultElementSQLObject compAdr;
    final JPanel panelAdrSpec = new JPanel(new GridBagLayout());

    public CommandeSQLComponent() {
        super(Configuration.getInstance().getDirectory().getElement("COMMANDE"));
    }

    public ElementComboBox getBoxFournisseur() {
        return this.fourn;
    }

    public void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Numero du commande
        c.gridx = 0;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("NUMERO"), SwingConstants.RIGHT), c);

        this.numeroUniqueCommande = new JUniqueTextField(16);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        DefaultGridBagConstraints.lockMinimumSize(numeroUniqueCommande);
        this.add(this.numeroUniqueCommande, c);

        // Date
        JLabel labelDate = new JLabel(getLabelFor("DATE"));
        labelDate.setHorizontalAlignment(SwingConstants.RIGHT);
        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(labelDate, c);

        JDate dateCommande = new JDate(true);
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        this.add(dateCommande, c);

        // Fournisseur
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("ID_FOURNISSEUR"), SwingConstants.RIGHT), c);

        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(this.fourn, c);

        if (!getTable().getFieldsName().contains("LIVRER")) {
            // Commande en cours
            JCheckBox boxEnCours = new JCheckBox(getLabelFor("EN_COURS"));
            c.gridx = 2;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridwidth = GridBagConstraints.REMAINDER;
            this.add(boxEnCours, c);
            c.gridwidth = 1;
            this.addRequiredSQLObject(boxEnCours, "EN_COURS");
        }

        // Fournisseur
        if (getTable().contains("ID_CONTACT_FOURNISSEUR")) {
            c.gridx = 0;
            c.gridy++;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(new JLabel(getLabelFor("ID_CONTACT_FOURNISSEUR"), SwingConstants.RIGHT), c);

            c.gridx = GridBagConstraints.RELATIVE;
            c.gridwidth = 1;
            c.weightx = 1;
            c.weighty = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            final ElementComboBox boxContactFournisseur = new ElementComboBox();
            final SQLElement contactElement = Configuration.getInstance().getDirectory().getElement("CONTACT_FOURNISSEUR");
            boxContactFournisseur.init(contactElement, contactElement.getComboRequest(true));
            this.add(boxContactFournisseur, c);
            this.addView(boxContactFournisseur, "ID_CONTACT_FOURNISSEUR", REQ);

            fourn.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent arg0) {
                    // TODO Raccord de méthode auto-généré
                    if (fourn.getSelectedRow() != null) {
                        boxContactFournisseur.getRequest().setWhere(new Where(contactElement.getTable().getField("ID_FOURNISSEUR"), "=", fourn.getSelectedRow().getID()));
                    } else {
                        boxContactFournisseur.getRequest().setWhere(null);
                    }
                }
            });
        }
        // Adresse de livraison
        if (getTable().getFieldsName().contains("ID_ADRESSE")) {
            if (getTable().getFieldsName().contains("LIVRAISON_F")) {
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                c.fill = GridBagConstraints.HORIZONTAL;
                this.add(new JLabel(getLabelFor("ID_ADRESSE")), c);
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                // c.gridy++;
                this.addView("ID_ADRESSE");
                final DefaultElementSQLObject comp = (DefaultElementSQLObject) this.getView("ID_ADRESSE").getComp();

                if (getTable().getFieldsName().contains("LIVRAISON_F")) {
                    final JCheckBox boxLivr = new JCheckBox("Livré par le fournisseur");
                    this.add(boxLivr, c);
                    this.addSQLObject(boxLivr, "LIVRAISON_F");
                    boxLivr.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (boxLivr.isSelected() && !comp.isCreated()) {
                                comp.setCreated(true);
                                if (CommandeSQLComponent.this.getTable().contains("ID_AFFAIRE")) {

                                    SQLRowValues rowVals = getLivraisonAdr(((ElementComboBox) CommandeSQLComponent.this.getView("ID_AFFAIRE").getComp()).getSelectedRow());

                                    comp.setValue(rowVals);
                                }

                            } else {
                                if (!boxLivr.isSelected()) {
                                    comp.setCreated(false);
                                }
                            }
                        }
                    });
                }

                c.gridy++;
                this.add(comp, c);
                this.add(this.getView("ID_ADRESSE").getComp(), c);
            } else {

                c.gridy++;
                c.gridx = 0;
                this.add(new JLabel("Livraison"), c);
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(boxLivrClient, c);
                c.gridwidth = 1;

                final GridBagConstraints cAdr = new DefaultGridBagConstraints();

                panelAdrSpec.add(new JLabel(getLabelFor("ID_CLIENT"), SwingConstants.RIGHT), cAdr);
                final ElementComboBox boxClient = new ElementComboBox(true);
                cAdr.weightx = 1;
                cAdr.gridx++;
                panelAdrSpec.add(boxClient, cAdr);
                this.addView(boxClient, "ID_CLIENT");

                cAdr.gridy++;
                cAdr.weightx = 0;
                cAdr.gridx = 0;
                panelAdrSpec.add(new JLabel("Adresse", SwingConstants.RIGHT), cAdr);
                final SQLRequestComboBox boxAdr = new SQLRequestComboBox(true);
                boxAdr.uiInit(Configuration.getInstance().getDirectory().getElement(getTable().getTable("ADRESSE")).getComboRequest(true));
                boxClient.addModelListener("wantedID", new PropertyChangeListener() {

                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (boxClient.getSelectedRow() != null && !boxClient.getSelectedRow().isUndefined()) {
                            Where w = new Where(boxAdr.getRequest().getPrimaryTable().getField("ID_CLIENT"), "=", boxClient.getSelectedRow().getID());
                            w = w.or(new Where(boxAdr.getRequest().getPrimaryTable().getKey(), "=", boxClient.getSelectedRow().getInt("ID_ADRESSE")));
                            w = w.or(new Where(boxAdr.getRequest().getPrimaryTable().getKey(), "=", boxClient.getSelectedRow().getInt("ID_ADRESSE_F")));
                            w = w.or(new Where(boxAdr.getRequest().getPrimaryTable().getKey(), "=", boxClient.getSelectedRow().getInt("ID_ADRESSE_L")));
                            boxAdr.getRequest().setWhere(w);
                        } else {
                            boxAdr.getRequest().setWhere(null);
                        }
                    }
                });
                cAdr.weightx = 1;
                cAdr.gridx++;
                panelAdrSpec.add(boxAdr, cAdr);

                cAdr.gridx = 0;
                cAdr.gridy++;
                cAdr.weightx = 0;
                if (getMode() == Mode.MODIFICATION) {
                    panelAdrSpec.add(new JLabel(getLabelFor("ID_ADRESSE")), cAdr);
                }
                cAdr.gridx++;
                cAdr.gridwidth = GridBagConstraints.REMAINDER;
                this.addView("ID_ADRESSE");
                compAdr = (DefaultElementSQLObject) this.getView("ID_ADRESSE").getComp();

                cAdr.gridy++;
                if (getMode() == Mode.MODIFICATION) {
                    panelAdrSpec.add(compAdr, cAdr);
                }
                boxAdr.addValueListener(new PropertyChangeListener() {

                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        SQLRow row = boxAdr.getSelectedRow();
                        if (row != null && !row.isUndefined()) {
                            compAdr.setCreated(true);
                            SQLRowValues asRowValues = new SQLRowValues(row.asRowValues());
                            compAdr.setValue(asRowValues);
                        }
                    }
                });

                c.gridy++;
                c.gridx = 0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1;
                this.add(panelAdrSpec, c);
                c.gridwidth = 1;
                c.weightx = 0;

                boxLivrClient.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        panelAdrSpec.setVisible(boxLivrClient.isSelected());

                        if (!boxLivrClient.isSelected()) {
                            boxClient.setValue((Integer) null);
                            boxAdr.setValue((Integer) null);
                            compAdr.setCreated(false);
                        }
                    }
                });
                panelAdrSpec.setVisible(false);
            }
        }
        c.gridwidth = 1;

        // Champ Module
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel addP = ComptaSQLConfElement.createAdditionalPanel();
        this.setAdditionalFieldsPanel(new FormLayouter(addP, 2));
        this.add(addP, c);

        c.gridy++;
        c.gridwidth = 1;

        final ElementComboBox boxDevise = new ElementComboBox();
        if (DefaultNXProps.getInstance().getBooleanValue(AbstractVenteArticleItemTable.ARTICLE_SHOW_DEVISE, false)) {
            // Devise
            c.gridx = 0;
            c.gridy++;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(new JLabel(getLabelFor("ID_DEVISE"), SwingConstants.RIGHT), c);

            c.gridx = GridBagConstraints.RELATIVE;
            c.gridwidth = 1;
            c.weightx = 1;
            c.weighty = 0;
            c.fill = GridBagConstraints.NONE;
            this.add(boxDevise, c);
            this.addView(boxDevise, "ID_DEVISE");
        }

        // Reference
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.EAST;
        this.add(new JLabel(getLabelFor("NOM"), SwingConstants.RIGHT), c);

        final JTextField textNom = new JTextField();
        c.gridx++;
        c.weightx = 1;
        this.add(textNom, c);

        String field;
            field = "ID_COMMERCIAL";
        // Commercial
        c.weightx = 0;
        c.gridx++;
        this.add(new JLabel(getLabelFor(field), SwingConstants.RIGHT), c);

        ElementComboBox commSel = new ElementComboBox(false, 25);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        this.add(commSel, c);
        addRequiredSQLObject(commSel, field);

        // Table d'élément
        c.fill = GridBagConstraints.BOTH;
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.weighty = 1;
        c.gridwidth = 4;
        this.add(this.table, c);
        if (DefaultNXProps.getInstance().getBooleanValue(AbstractVenteArticleItemTable.ARTICLE_SHOW_DEVISE, false)) {

            boxDevise.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    table.setDevise(boxDevise.getSelectedRow());

                }
            });
        }

        this.fourn.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                table.setFournisseur(fourn.getSelectedRow());
            }
        });
        // Bottom
        c.gridy++;
        c.weighty = 0;
        this.add(getBottomPanel(), c);

        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        JPanel panelOO = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelOO.add(this.checkImpression, c);
        panelOO.add(this.checkVisu, c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(panelOO, c);

        addRequiredSQLObject(this.fourn, "ID_FOURNISSEUR");
        addSQLObject(textNom, "NOM");
        addRequiredSQLObject(dateCommande, "DATE");
        // addRequiredSQLObject(radioEtat, "ID_ETAT_DEVIS");
        addRequiredSQLObject(this.numeroUniqueCommande, "NUMERO");
        addSQLObject(this.infos, "INFOS");

        this.numeroUniqueCommande.setText(NumerotationAutoSQLElement.getNextNumero(CommandeSQLElement.class));

        // radioEtat.setValue(EtatDevisSQLElement.EN_ATTENTE);
        // this.numeroUniqueDevis.addLabelWarningMouseListener(new MouseAdapter() {
        // public void mousePressed(MouseEvent e) {
        //
        // if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
        // numeroUniqueDevis.setText(NumerotationAutoSQLElement.getNextNumeroDevis());
        // }
        // }
        // });

        DefaultGridBagConstraints.lockMinimumSize(this.fourn);
        DefaultGridBagConstraints.lockMinimumSize(commSel);
    }

    protected SQLRowValues getLivraisonAdr(SQLRow rowAffaire) {
        if (rowAffaire != null) {
            SQLRow rowClient = rowAffaire.getForeignRow("ID_CLIENT");
            SQLRow rowAdrL = rowClient.getForeignRow("ID_ADRESSE_L");
            if (rowAdrL == null || rowAdrL.isUndefined()) {
                rowAdrL = rowClient.getForeignRow("ID_ADRESSE");
            }
            SQLRowValues rowVals = rowAdrL.asRowValues();
            rowVals.clearPrimaryKeys();
            return rowVals;
        } else {
            return new SQLRowValues(getTable().getTable("ADRESSE"));
        }
    }

    private JPanel getBottomPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Colonne 1 : Infos
        c.gridx = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        panel.add(new TitledSeparator(getLabelFor("INFOS")), c);

        c.gridy++;
        c.weighty = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        final JScrollPane scrollPane = new JScrollPane(this.infos);
        scrollPane.setBorder(null);
        panel.add(scrollPane, c);

        // Colonne 2 : Poids & autres
        DefaultProps props = DefaultNXProps.getInstance();
        Boolean b = props.getBooleanValue("ArticleShowPoids");
        final JTextField textPoidsTotal = new JTextField(8);
        JTextField poids = new JTextField();
        if (b) {
            final JPanel panelPoids = new JPanel();

            panelPoids.add(new JLabel(getLabelFor("T_POIDS")), c);

            textPoidsTotal.setEnabled(false);
            textPoidsTotal.setHorizontalAlignment(JTextField.RIGHT);
            textPoidsTotal.setDisabledTextColor(Color.BLACK);

            panelPoids.add(textPoidsTotal, c);

            c.gridx++;
            c.gridy = 0;
            c.weightx = 0;
            c.weighty = 0;
            c.gridwidth = 1;
            c.gridheight = 2;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHEAST;
            panel.add(panelPoids, c);
            DefaultGridBagConstraints.lockMinimumSize(panelPoids);
            addSQLObject(textPoidsTotal, "T_POIDS");
        } else {
            addSQLObject(poids, "T_POIDS");
        }

        DeviseField textPortHT = new DeviseField();
        ElementComboBox comboTaxePort = new ElementComboBox();

        if (getTable().contains("PORT_HT")) {
            addRequiredSQLObject(textPortHT, "PORT_HT");
            final JPanel panelPoids = new JPanel(new GridBagLayout());
            GridBagConstraints cPort = new DefaultGridBagConstraints();
            cPort.gridx = 0;
            cPort.weightx = 0;
            panelPoids.add(new JLabel(getLabelFor("PORT_HT")), cPort);
            textPortHT.setHorizontalAlignment(JTextField.RIGHT);
            cPort.gridx++;
            cPort.weightx = 1;
            panelPoids.add(textPortHT, cPort);

            cPort.gridy++;
            cPort.gridx = 0;
            cPort.weightx = 0;
            addRequiredSQLObject(comboTaxePort, "ID_TAXE_PORT");
            panelPoids.add(new JLabel(getLabelFor("ID_TAXE_PORT")), cPort);
            cPort.gridx++;
            cPort.weightx = 0;
            panelPoids.add(comboTaxePort, cPort);

            c.gridx++;
            c.gridy = 0;
            c.weightx = 0;
            c.weighty = 0;
            c.gridwidth = 1;
            c.gridheight = 2;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHEAST;
            panel.add(panelPoids, c);
            DefaultGridBagConstraints.lockMinimumSize(panelPoids);
        }
        // Total

        DeviseField textRemiseHT = new DeviseField();
        DeviseField fieldHT = new DeviseField();
        DeviseField fieldTVA = new DeviseField();
        DeviseField fieldTTC = new DeviseField();
        DeviseField fieldDevise = new DeviseField();
        DeviseField fieldService = new DeviseField();
        fieldHT.setOpaque(false);
        fieldTVA.setOpaque(false);
        fieldTTC.setOpaque(false);
        fieldService.setOpaque(false);
        addRequiredSQLObject(fieldDevise, "T_DEVISE");
        addRequiredSQLObject(fieldHT, "T_HT");
        addRequiredSQLObject(fieldTVA, "T_TVA");

        addRequiredSQLObject(fieldTTC, "T_TTC");
        addRequiredSQLObject(fieldService, "T_SERVICE");
        final TotalPanel totalTTC = new TotalPanel(this.table, fieldHT, fieldTVA, fieldTTC, textPortHT, textRemiseHT, fieldService, null, fieldDevise, null, null,
                (getTable().contains("ID_TAXE_PORT") ? comboTaxePort : null));

        c.gridx++;
        c.gridy--;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 2;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0;
        DefaultGridBagConstraints.lockMinimumSize(totalTTC);

        panel.add(totalTTC, c);

        c.gridy += 3;
        c.gridheight = 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        panel.add(getModuleTotalPanel(), c);

        table.getModel().addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                textPoidsTotal.setText(String.valueOf(table.getPoidsTotal()));
            }
        });

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

        comboTaxePort.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                // TODO Raccord de méthode auto-généré
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

    protected JPanel getModuleTotalPanel() {

        return AutoHideListener.listen(new JPanel());
    }

    public int insert(SQLRow order) {

        int idCommande = getSelectedID();
        // on verifie qu'un devis du meme numero n'a pas été inséré entre temps
        if (this.numeroUniqueCommande.checkValidation()) {

            idCommande = super.insert(order);
            this.table.updateField("ID_COMMANDE", idCommande);

            // Création des articles
            this.table.createArticle(idCommande, this.getElement());

            // generation du document
            final CommandeXmlSheet sheet = new CommandeXmlSheet(getTable().getRow(idCommande));
            sheet.createDocumentAsynchronous();
            sheet.showPrintAndExportAsynchronous(this.checkVisu.isSelected(), this.checkImpression.isSelected(), true);

            // incrémentation du numéro auto
            if (NumerotationAutoSQLElement.getNextNumero(CommandeSQLElement.class).equalsIgnoreCase(this.numeroUniqueCommande.getText().trim())) {
                SQLRowValues rowVals = new SQLRowValues(this.tableNum);
                int val = this.tableNum.getRow(2).getInt(NumerotationAutoSQLElement.getLabelNumberFor(CommandeSQLElement.class));
                val++;
                rowVals.put(NumerotationAutoSQLElement.getLabelNumberFor(CommandeSQLElement.class), new Integer(val));

                try {
                    rowVals.update(2);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            ExceptionHandler.handle("Impossible d'ajouter, numéro de commande client existant.");
            Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
        }

        return idCommande;
    }

    @Override
    public void select(SQLRowAccessor r) {
        if (r != null) {
            this.numeroUniqueCommande.setIdSelected(r.getID());
        }
        if (!getTable().contains("LIVRAISON_F") && r != null && !r.isUndefined()) {

            SQLRowAccessor adr = r.getForeign("ID_ADRESSE");
            boxLivrClient.setSelected(adr != null && !adr.isUndefined());
            panelAdrSpec.setVisible(boxLivrClient.isSelected());

            if (!boxLivrClient.isSelected()) {
                compAdr.setCreated(false);
            }
        }

        super.select(r);
        if (r != null) {
            this.table.insertFrom("ID_COMMANDE", r.getID());
        }
    }

    @Override
    public void update() {
        if (!this.numeroUniqueCommande.checkValidation()) {
            ExceptionHandler.handle("Impossible d'ajouter, numéro de commande client existant.");
            Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
            return;
        }

        super.update();
        this.table.updateField("ID_COMMANDE", getSelectedID());
        this.table.createArticle(getSelectedID(), this.getElement());

        // generation du document
        final CommandeXmlSheet sheet = new CommandeXmlSheet(getTable().getRow(getSelectedID()));
        sheet.createDocumentAsynchronous();
        sheet.showPrintAndExportAsynchronous(this.checkVisu.isSelected(), this.checkImpression.isSelected(), true);

    }

    public void setDefaults() {
        this.resetValue();
        this.numeroUniqueCommande.setText(NumerotationAutoSQLElement.getNextNumero(CommandeSQLElement.class));
        this.table.getModel().clearRows();
    }

    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues rowVals = new SQLRowValues(getTable());
        rowVals.put("T_POIDS", 0.0F);
        rowVals.put("EN_COURS", Boolean.TRUE);

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
            rowVals.put("ID_COMMERCIAL", rowsComm.getID());
        }
        rowVals.put("T_HT", Long.valueOf(0));
        rowVals.put("T_SERVICE", Long.valueOf(0));
        rowVals.put("T_DEVISE", Long.valueOf(0));
        rowVals.put("T_TVA", Long.valueOf(0));
        rowVals.put("T_TTC", Long.valueOf(0));
        rowVals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(CommandeSQLElement.class));

        if (getTable().contains("ID_TAXE_PORT")) {
            rowVals.put("ID_TAXE_PORT", TaxeCache.getCache().getIdFromTaux(19.6F));
        }

        return rowVals;
    }

    @Override
    public RowValuesTable getRowValuesTable() {
        return this.table.getRowValuesTable();
    }

    /**
     * Chargement des éléments d'une commande dans la table
     * 
     * @param idCommande
     * 
     */
    public void loadCommande(int idCommande) {

        SQLElement commande = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
        SQLElement commandeElt = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT_ELEMENT");

        if (idCommande > 1) {
            SQLInjector injector = SQLInjector.getInjector(commande.getTable(), this.getTable());
            this.select(injector.createRowValuesFrom(idCommande));
        }

        loadItem(this.table, commande, idCommande, commandeElt);
    }

    /**
     * Chargement des éléments d'un devis dans la table
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

        loadItem(this.table, devis, idDevis, devisElt);
    }

    /**
     * Chargement des éléments d'une facture dans la table
     * 
     * @param idFact
     * 
     */
    public void loadFacture(int idFact) {

        SQLElement facture = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        SQLElement factureElt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");

        if (idFact > 1) {
            SQLInjector injector = SQLInjector.getInjector(facture.getTable(), this.getTable());
            this.select(injector.createRowValuesFrom(idFact));
        }

        loadItem(this.table, facture, idFact, factureElt);
    }

}
