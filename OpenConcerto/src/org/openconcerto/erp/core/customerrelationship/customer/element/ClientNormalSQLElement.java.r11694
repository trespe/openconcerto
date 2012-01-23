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
 
 package org.openconcerto.erp.core.customerrelationship.customer.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.customerrelationship.customer.ui.AdresseClientItemTable;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.payment.component.ModeDeReglementSQLComponent;
import org.openconcerto.erp.model.ISQLCompteSelector;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.ModeReglementDefautPrefPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.UndefinedRowValuesCache;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.ITextWithCompletion;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.ITextArea;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ClientNormalSQLElement extends ComptaSQLConfElement {

    public ClientNormalSQLElement() {
        super("CLIENT", "un client", "clients");
    }

    protected boolean showMdr = true;

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
            l.add("CODE");
            l.add("FORME_JURIDIQUE");
        l.add("NOM");
            l.add("RESPONSABLE");
        l.add("ID_ADRESSE");
        l.add("TEL");
        l.add("FAX");
        l.add("MAIL");
            l.add("NUMERO_TVA");
            l.add("SIRET");
            l.add("ID_COMPTE_PCE");
            l.add("ID_MODE_REGLEMENT");
        l.add("INFOS");
        return l;
    }

    @Override
    public synchronized ListSQLRequest createListRequest() {
        return new ListSQLRequest(getTable(), getListFields()) {
            @Override
            protected void customizeToFetch(SQLRowValues graphToFetch) {
                super.customizeToFetch(graphToFetch);
                graphToFetch.grow("ID_MODE_REGLEMENT").put("AJOURS", null).put("LENJOUR", null);
            }
        };

    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("FORME_JURIDIQUE");
        l.add("NOM");
        l.add("CODE");
        return l;
    }

    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_ADRESSE");
        l.add("ID_ADRESSE_L");
        l.add("ID_ADRESSE_F");
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
            int idDefaultCompteClient = 1;
            JCheckBox checkAdrLivraison, checkAdrFacturation;

            private ElementComboBox boxPays = null;
            final ElementComboBox boxTarif = new ElementComboBox();

            private JTabbedPane tabbedAdresse = new JTabbedPane() {
                public void insertTab(String title, Icon icon, Component component, String tip, int index) {
                    if (component instanceof JComponent) {
                        ((JComponent) component).setOpaque(false);
                    }
                    super.insertTab(title, icon, component, tip, index);
                }

            };
            ElementSQLObject componentPrincipale, componentLivraison, componentFacturation;
            AdresseClientItemTable adresseTable = new AdresseClientItemTable();
            JCheckBox boxGestionAutoCompte;

            private JCheckBox boxAffacturage, boxComptant;
            private DeviseField fieldMontantFactMax;
            ISQLCompteSelector compteSel;
            JComponent textNom;
            // ITextWithCompletion textNom;
            final ElementComboBox comboPole = new ElementComboBox();
            DecimalFormat format = new DecimalFormat("000");

            private SQLTable contactTable = Configuration.getInstance().getDirectory().getElement("CONTACT").getTable();
            private ContactItemTable table;
            private SQLRowValues defaultContactRowVals = new SQLRowValues(UndefinedRowValuesCache.getInstance().getDefaultRowValues(this.contactTable));
            private SQLRowItemView eltModeRegl;
            private JUniqueTextField textCode;
            private JLabel labelCpt;

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                this.addView("ID_MODE_REGLEMENT", REQ + ";" + DEC + ";" + SEP);
                this.eltModeRegl = this.getView("ID_MODE_REGLEMENT");
                final ElementSQLObject comp = (ElementSQLObject) this.eltModeRegl.getComp();
                final ModeDeReglementSQLComponent modeReglComp = (ModeDeReglementSQLComponent) comp.getSQLChild();

                // Raison sociale
                JLabel labelRS = new JLabel(getLabelFor("FORME_JURIDIQUE"));
                labelRS.setHorizontalAlignment(SwingConstants.RIGHT);
                SQLTextCombo textType = new SQLTextCombo();

                this.add(labelRS, c);
                c.gridx++;
                c.weightx = 0.5;
                c.fill = GridBagConstraints.BOTH;
                DefaultGridBagConstraints.lockMinimumSize(textType);
                this.add(textType, c);

                // Code
                JLabel labelCode = new JLabel("Code");
                labelCode.setHorizontalAlignment(SwingConstants.RIGHT);
                this.textCode = new JUniqueTextField();
                c.gridx++;
                c.weightx = 0;
                c.weighty = 0;
                c.gridwidth = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                this.add(labelCode, c);
                c.gridx++;
                c.weightx = 0.5;
                c.gridwidth = 1;
                DefaultGridBagConstraints.lockMinimumSize(textCode);
                this.add(this.textCode, c);
                // Nom
                JLabel labelNom = new JLabel("Nom");
                labelNom.setHorizontalAlignment(SwingConstants.RIGHT);
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelNom, c);
                c.gridx++;
                c.gridwidth = 1;
                c.weightx = 0.5;

                    this.textNom = new ITextArea();
                DefaultGridBagConstraints.lockMinimumSize(textNom);
                this.add(this.textNom, c);

                if (getTable().getFieldsName().contains("ID_PAYS")) {
                    c.gridx++;
                    c.weightx = 0;
                    this.add(new JLabel(getLabelFor("ID_PAYS")), c);
                    boxPays = new ElementComboBox(true, 25);
                    c.gridx++;
                    c.weightx = 0.5;
                    DefaultGridBagConstraints.lockMinimumSize(boxPays);
                    this.add(boxPays, c);
                    this.addView(boxPays, "ID_PAYS");
                }

                // Numero intracomm
                JLabel labelIntraComm = new JLabel("N° TVA");
                labelIntraComm.setHorizontalAlignment(SwingConstants.RIGHT);
                c.gridy++;
                c.gridx = 0;
                c.gridwidth = 1;
                c.weightx = 0;
                this.add(labelIntraComm, c);

                JTextField textNumIntracomm = new JTextField();
                c.gridx++;
                c.weightx = 0.5;
                DefaultGridBagConstraints.lockMinimumSize(textNumIntracomm);
                this.add(textNumIntracomm, c);
                JLabel labelSIREN = new JLabel(getLabelFor("SIRET"));
                labelSIREN.setHorizontalAlignment(SwingConstants.RIGHT);
                c.gridx++;
                c.gridwidth = 1;
                c.weightx = 0;
                this.add(labelSIREN, c);

                JComponent textSiren;
                    textSiren = new JTextField();
                c.gridx++;
                c.weightx = 0.5;
                DefaultGridBagConstraints.lockMinimumSize(textSiren);
                this.add(textSiren, c);

                // Responsable
                final JLabel responsable = new JLabel(this.getLabelFor("RESPONSABLE"));
                responsable.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textResp = new JTextField();
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                c.weighty = 0;
                c.gridwidth = 1;
                this.add(responsable, c);
                c.gridx++;
                c.weightx = 0.5;
                DefaultGridBagConstraints.lockMinimumSize(textResp);
                this.add(textResp, c);

                JLabel labelRIB = new JLabel(getLabelFor("RIB"));
                labelRIB.setHorizontalAlignment(SwingConstants.RIGHT);
                c.gridx++;
                c.gridwidth = 1;
                c.weightx = 0;
                this.add(labelRIB, c);

                JTextField textRib = new JTextField();
                c.gridx++;
                c.weightx = 0.5;
                DefaultGridBagConstraints.lockMinimumSize(textRib);
                this.add(textRib, c);


                // tel
                JLabel labelTel = new JLabel("N° de téléphone");
                labelTel.setHorizontalAlignment(SwingConstants.RIGHT);
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                this.add(labelTel, c);

                final JTextField textTel = new JTextField();
                c.gridx++;
                c.weightx = 0.5;
                DefaultGridBagConstraints.lockMinimumSize(textTel);
                this.add(textTel, c);
                textTel.getDocument().addDocumentListener(new DocumentListener() {

                    public void changedUpdate(DocumentEvent e) {
                        defaultContactRowVals.put("TEL_DIRECT", textTel.getText());
                    }

                    public void insertUpdate(DocumentEvent e) {
                        defaultContactRowVals.put("TEL_DIRECT", textTel.getText());
                    }

                    public void removeUpdate(DocumentEvent e) {
                        defaultContactRowVals.put("TEL_DIRECT", textTel.getText());
                    }

                });

                // email
                JLabel labelMail = new JLabel("E-mail");
                labelMail.setHorizontalAlignment(SwingConstants.RIGHT);
                c.gridx++;
                c.weightx = 0;
                this.add(labelMail, c);

                JTextField textMail = new JTextField();
                c.gridx++;
                c.weightx = 0.5;
                DefaultGridBagConstraints.lockMinimumSize(textMail);
                this.add(textMail, c);

                // Portable
                JLabel labelPortable = new JLabel("N° de portable");
                labelPortable.setHorizontalAlignment(SwingConstants.RIGHT);
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                this.add(labelPortable, c);

                JTextField textPortable = new JTextField();
                c.gridx++;
                c.weightx = 0.5;
                DefaultGridBagConstraints.lockMinimumSize(textPortable);
                this.add(textPortable, c);

                // Fax
                JLabel labelFax = new JLabel("N° de fax");
                labelFax.setHorizontalAlignment(SwingConstants.RIGHT);
                c.gridx++;
                c.weightx = 0;
                this.add(labelFax, c);

                final JTextField textFax = new JTextField();
                c.gridx++;
                c.weightx = 0.5;
                DefaultGridBagConstraints.lockMinimumSize(textFax);
                this.add(textFax, c);

                textFax.getDocument().addDocumentListener(new DocumentListener() {

                    public void changedUpdate(DocumentEvent e) {
                        defaultContactRowVals.put("FAX", textFax.getText());
                    }

                    public void insertUpdate(DocumentEvent e) {
                        defaultContactRowVals.put("FAX", textFax.getText());
                    }

                    public void removeUpdate(DocumentEvent e) {
                        defaultContactRowVals.put("FAX", textFax.getText());
                    }

                });

                // Secteur activité
                final boolean customerIsKD;

                // Adresse
                c.gridx = 0;
                c.gridy++;
                c.weightx = 1;
                c.weighty = 0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                TitledSeparator sep = new TitledSeparator("Adresse");
                this.add(sep, c);

                // Adr principale
                this.addView("ID_ADRESSE", REQ + ";" + DEC + ";" + SEP);
                this.componentPrincipale = (ElementSQLObject) this.getView("ID_ADRESSE");
                this.componentPrincipale.setOpaque(false);
                this.tabbedAdresse.add(getLabelFor("ID_ADRESSE"), this.componentPrincipale);

                // Adr facturation
                JPanel panelFacturation = new JPanel(new GridBagLayout());
                panelFacturation.setOpaque(false);
                GridBagConstraints cPanelF = new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 1, 2, 1), 0, 0);

                this.addView("ID_ADRESSE_F", DEC + ";" + SEP);
                this.componentFacturation = (ElementSQLObject) this.getView("ID_ADRESSE_F");
                this.componentFacturation.setOpaque(false);
                panelFacturation.add(this.componentFacturation, cPanelF);
                this.checkAdrFacturation = new JCheckBox("Adresse de facturation identique à la principale");
                this.checkAdrFacturation.setOpaque(false);
                cPanelF.gridy++;
                panelFacturation.add(this.checkAdrFacturation, cPanelF);
                    this.tabbedAdresse.add(getLabelFor("ID_ADRESSE_F"), panelFacturation);
                // Adr livraison
                JPanel panelLivraison = new JPanel(new GridBagLayout());
                panelLivraison.setOpaque(false);
                GridBagConstraints cPanelL = new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 1, 2, 1), 0, 0);

                this.addView("ID_ADRESSE_L", DEC + ";" + SEP);
                this.componentLivraison = (ElementSQLObject) this.getView("ID_ADRESSE_L");
                this.componentLivraison.setOpaque(false);
                panelLivraison.add(this.componentLivraison, cPanelL);

                this.checkAdrLivraison = new JCheckBox("Adresse de livraison identique à l'adresse principale");
                this.checkAdrLivraison.setOpaque(false);
                cPanelL.gridy++;
                panelLivraison.add(this.checkAdrLivraison, cPanelL);
                    this.tabbedAdresse.add(getLabelFor("ID_ADRESSE_L"), panelLivraison);
                String labelAdrSuppl = "Adresses supplémentaires";
                this.tabbedAdresse.add(labelAdrSuppl, this.adresseTable);

                c.gridx = 0;
                c.gridy++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.tabbedAdresse.setOpaque(false);
                this.add(this.tabbedAdresse, c);

                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.HORIZONTAL;

                // Contact

                TitledSeparator sepContact = new TitledSeparator("Contacts client");
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
                c.weighty = 0.7;
                this.add(this.table, c);
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridwidth = 1;
                c.weighty = 0;


                // Mode de régelement

                TitledSeparator reglSep = new TitledSeparator(getLabelFor("ID_MODE_REGLEMENT"));
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridy++;
                c.gridx = 0;
                this.add(reglSep, c);

                c.gridy++;
                c.gridx = 0;
                this.add(comp, c);

                if (!showMdr) {
                    reglSep.setVisible(false);
                    comp.setCreated(false);
                    comp.setVisible(false);
                }

                if (getTable().getFieldsName().contains("ID_TARIF")) {

                    // Tarif
                    TitledSeparator tarifSep = new TitledSeparator("Tarif spécial à appliquer");
                    c.gridwidth = GridBagConstraints.REMAINDER;
                    c.gridy++;
                    c.gridx = 0;
                    this.add(tarifSep, c);

                    c.gridy++;
                    c.gridx = 0;
                    c.gridwidth = 1;
                    c.weightx = 0;
                    this.add(new JLabel(getLabelFor("ID_TARIF")), c);
                    c.gridx++;
                    c.weightx = 1;
                    c.gridwidth = GridBagConstraints.REMAINDER;

                    this.add(boxTarif, c);
                    this.addView(boxTarif, "ID_TARIF");
                }
                if (getTable().getFieldsName().contains("ID_LANGUE")) {
                    // Tarif
                    TitledSeparator langueSep = new TitledSeparator("Langue à appliquer sur les documents");
                    c.gridwidth = GridBagConstraints.REMAINDER;
                    c.gridy++;
                    c.gridx = 0;
                    this.add(langueSep, c);

                    c.gridy++;
                    c.gridx = 0;
                    c.gridwidth = 1;
                    c.weightx = 0;
                    this.add(new JLabel(getLabelFor("ID_LANGUE")), c);
                    c.gridx++;
                    c.weightx = 1;
                    c.gridwidth = GridBagConstraints.REMAINDER;
                    final ElementComboBox boxLangue = new ElementComboBox();
                    this.add(boxLangue, c);
                    this.addView(boxLangue, "ID_LANGUE");

                    boxPays.addValueListener(new PropertyChangeListener() {

                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            SQLRow row = boxPays.getSelectedRow();
                            if (row != null) {
                                boxTarif.setValue(row.getInt("ID_TARIF"));
                                boxLangue.setValue(row.getInt("ID_LANGUE"));
                            }
                        }
                    });
                }
                // Add on
                final JPanel addOnPanel = getAddOnPanel(this);
                if (addOnPanel != null) {
                    c.gridy++;
                    this.add(addOnPanel, c);
                }
                // Compte associé
                this.compteSel = new ISQLCompteSelector(true);
                this.boxGestionAutoCompte = new JCheckBox("Gestion Automatique des comptes");
                TitledSeparator sepCompte = new TitledSeparator("Compte associé");
                this.labelCpt = new JLabel(getLabelFor("ID_COMPTE_PCE"));

                if (!Boolean.valueOf(DefaultNXProps.getInstance().getProperty("HideCompteClient"))) {

                    c.gridx = 0;
                    c.gridy++;
                    c.weightx = 1;
                    c.weighty = 0;
                    c.gridwidth = GridBagConstraints.REMAINDER;

                    this.add(sepCompte, c);

                    c.gridwidth = 1;
                    c.gridy++;
                    c.gridx = 0;
                    c.weightx = 0;
                    this.add(this.labelCpt, c);

                    c.gridwidth = GridBagConstraints.REMAINDER;
                    c.gridx++;
                    c.weightx = 1;

                    this.add(this.compteSel, c);

                    this.boxGestionAutoCompte.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {

                            setCompteVisible(!(boxGestionAutoCompte.isSelected() && getSelectedID() <= 1));
                        }
                    });
                }
                // Infos
                TitledSeparator infosSep = new TitledSeparator(getLabelFor("INFOS"));
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridy++;
                c.gridx = 0;
                this.add(infosSep, c);
                ITextArea textInfos = new ITextArea();
                c.gridy++;
                c.weighty = 0.3;
                c.fill = GridBagConstraints.BOTH;
                this.add(textInfos, c);

                this.checkAdrLivraison.addActionListener(new ActionListener() {

                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        boolean b = checkAdrLivraison.isSelected();

                        componentLivraison.setEditable(!b);
                        componentLivraison.setCreated(!b);
                    };
                });

                this.checkAdrFacturation.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {

                        boolean b = checkAdrFacturation.isSelected();

                        componentFacturation.setEditable(!b);

                        componentFacturation.setCreated(!b);
                    }
                });

                this.addSQLObject(textType, "FORME_JURIDIQUE");
                this.addView(this.textNom, "NOM", REQ);
                this.addSQLObject(this.textCode, "CODE");
                this.addSQLObject(textFax, "FAX");
                this.addSQLObject(textSiren, "SIRET");
                this.addSQLObject(textMail, "MAIL");
                this.addSQLObject(textTel, "TEL");
                this.addSQLObject(textPortable, "TEL_P");
                this.addSQLObject(textNumIntracomm, "NUMERO_TVA");
                this.addSQLObject(textResp, "RESPONSABLE");
                this.addSQLObject(textInfos, "INFOS");
                this.addSQLObject(this.compteSel, "ID_COMPTE_PCE");

                this.checkAdrFacturation.setSelected(true);
                this.checkAdrLivraison.setSelected(true);

            }

            private void setCompteVisible(boolean b) {

                this.labelCpt.setVisible(b);
                this.compteSel.setVisible(b);
            }

            @Override
            public void update() {
                super.update();
                final int selectedID = getSelectedID();
                this.table.updateField("ID_CLIENT", selectedID);
                this.adresseTable.updateField("ID_CLIENT", selectedID);
                if (this.boxGestionAutoCompte.isSelected()) {

                    SQLRow row = getTable().getRow(selectedID);
                    if (row.getInt("ID_COMPTE_PCE") <= 1) {
                        createCompteClientAuto(selectedID);
                    } else {
                        SQLRow rowCpt = row.getForeignRow("ID_COMPTE_PCE");
                        String num = rowCpt.getString("NUMERO");
                        String initialClient = "";
                        String text;
                        if (this.textNom instanceof ITextArea) {
                            ITextArea textNomArea = (ITextArea) this.textNom;
                            text = textNomArea.getText();
                        } else {
                            ITextWithCompletion textNomArea = (ITextWithCompletion) this.textNom;
                            text = textNomArea.getValue();
                        }
                        // final String text = (String) this.textNom.getText();
                        if (text != null && text.trim().length() > 1) {
                            initialClient += text.trim().toUpperCase().charAt(0);
                        }

                        String compte = "411" + initialClient;
                        if (!num.startsWith(compte)) {
                            int answer = JOptionPane.showConfirmDialog(null, "Voulez vous changer le compte associé au client, le nom a changé?", "Modification compte client",
                                    JOptionPane.YES_NO_OPTION);

                            if (answer == JOptionPane.YES_OPTION) {
                                createCompteClientAuto(selectedID);
                            }
                        }
                    }

                }
            }

            @Override
            public void select(SQLRowAccessor r) {

                int idAdrL = 1;
                int idAdrF = 1;
                if (r != null && r.getID() > 1) {
                    final SQLRow row = ClientNormalSQLElement.this.getTable().getRow(r.getID());
                    idAdrL = row.getInt("ID_ADRESSE_L");
                    idAdrF = row.getInt("ID_ADRESSE_F");
                }
                super.select(r);

                this.checkAdrLivraison.setSelected(idAdrL == 1);
                this.checkAdrFacturation.setSelected(idAdrF == 1);
                if (r != null) {
                    this.table.insertFrom("ID_CLIENT", r.getID());
                    this.adresseTable.insertFrom("ID_CLIENT", r.getID());
                    this.defaultContactRowVals.put("TEL_DIRECT", r.getString("TEL"));
                    this.defaultContactRowVals.put("FAX", r.getString("FAX"));
                    this.textCode.setIdSelected(r.getID());
                }
            }

            private void createCompteClientAuto(int idClient) {
                SQLRowValues rowVals = getTable().getRow(idClient).createEmptyUpdateRow();
                String initialClient = "";
                String text;
                if (this.textNom instanceof ITextArea) {
                    ITextArea textNomArea = (ITextArea) this.textNom;
                    text = textNomArea.getText();
                } else {
                    ITextWithCompletion textNomArea = (ITextWithCompletion) this.textNom;
                    text = textNomArea.getValue();
                }
                if (text != null && text.trim().length() > 1) {
                    initialClient += text.trim().toUpperCase().charAt(0);
                }

                String compte = "411" + initialClient;

                SQLTable table = Configuration.getInstance().getDirectory().getElement("COMPTE_PCE").getTable();
                SQLSelect selCompte = new SQLSelect(this.getTable().getBase());
                selCompte.addSelectFunctionStar("COUNT");
                selCompte.setArchivedPolicy(SQLSelect.BOTH);
                selCompte.setWhere(new Where(table.getField("NUMERO"), "LIKE", compte + "%"));
                System.err.println(selCompte.asString());
                Object o = Configuration.getInstance().getBase().getDataSource().executeScalar(selCompte.asString());

                int nb = 0;
                if (o != null) {
                    Long i = (Long) o;
                    nb = i.intValue();
                }

                int idCpt = ComptePCESQLElement.getId(compte + this.format.format(nb), text);
                rowVals.put("ID_COMPTE_PCE", idCpt);
                try {
                    rowVals.update();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public int insert(SQLRow order) {

                int id = super.insert(order);
                this.table.updateField("ID_CLIENT", id);
                this.adresseTable.updateField("ID_CLIENT", id);
                if (this.boxGestionAutoCompte.isSelected()) {
                    createCompteClientAuto(id);
                }
                return id;
            }

            @Override
            protected SQLRowValues createDefaults() {
                SQLRowValues vals = new SQLRowValues(this.getTable());
                SQLRow r;

                vals.put("MARCHE_PUBLIC", Boolean.TRUE);

                // Mode de règlement par defaut
                try {
                    r = ModeReglementDefautPrefPanel.getDefaultRow(true);
                    SQLElement eltModeReglement = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
                    if (r.getID() > 1) {
                        SQLRowValues rowVals = eltModeReglement.createCopy(r, null);
                        System.err.println(rowVals.getInt("ID_TYPE_REGLEMENT"));
                        vals.put("ID_MODE_REGLEMENT", rowVals);
                    }
                } catch (SQLException e) {
                    System.err.println("Impossible de sélectionner le mode de règlement par défaut du client.");
                    e.printStackTrace();
                }

                // Select Compte client par defaut
                final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
                final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
                final SQLRow rowPrefsCompte = SQLBackgroundTableCache.getInstance().getCacheForTable(tablePrefCompte).getRowFromId(2);

                this.idDefaultCompteClient = rowPrefsCompte.getInt("ID_COMPTE_PCE_CLIENT");
                if (this.idDefaultCompteClient <= 1) {
                    try {
                        this.idDefaultCompteClient = ComptePCESQLElement.getIdComptePceDefault("Clients");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                vals.put("ID_COMPTE_PCE", this.idDefaultCompteClient);

                return vals;
            }
        };
    }

    protected JPanel getAddOnPanel(BaseSQLComponent c) {
        return null;
    }

}
