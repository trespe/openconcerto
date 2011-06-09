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
 
 package org.openconcerto.erp.core.humanresources.payroll.element;

import org.openconcerto.erp.core.common.ui.SQLJavaEditor;
import org.openconcerto.erp.core.humanresources.payroll.component.FormuleTreeNode;
import org.openconcerto.erp.core.humanresources.payroll.component.VariableTree;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.warning.JLabelWarning;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.TreePath;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class RubriqueCommSQLElement extends ConfSQLElement {

    public RubriqueCommSQLElement() {
        super("RUBRIQUE_COMM", "une rubrique de commentaire", "rubriques de commentaire");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        l.add("NOM");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        l.add("NOM");
        return l;
    }

    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_PERIODE_VALIDITE");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private JTextField textCode = new JTextField();
            private JTextField textLibelle = new JTextField();
            private SQLJavaEditor formuleBase, formuleTxSal, formuleTxPat, formuleMontantSalAd, formuleMontantSalDed, formuleMontantPat;
            private JLabel labelWarningBadName, labelBadName;
            private boolean validCode;

            public void addViews() {

                this.labelWarningBadName = new JLabelWarning();
                this.labelBadName = new JLabel("Code déjà attribué.");
                this.validCode = true;

                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Code
                JLabel labelCode = new JLabel(getLabelFor("CODE"));
                labelCode.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelCode, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.textCode, c);
                c.weightx = 0;
                this.textCode.getDocument().addDocumentListener(new DocumentListener() {

                    public void insertUpdate(DocumentEvent e) {
                        isValidCodeName();
                    }

                    public void removeUpdate(DocumentEvent e) {
                        isValidCodeName();
                    }

                    public void changedUpdate(DocumentEvent e) {
                        isValidCodeName();
                    }
                });

                c.gridx++;
                this.add(this.labelWarningBadName, c);
                this.labelWarningBadName.setVisible(false);
                c.gridx++;
                this.add(this.labelBadName, c);
                this.labelBadName.setVisible(false);

                // Libelle
                c.gridy++;
                c.gridx = 0;

                JLabel labelNom = new JLabel(getLabelFor("NOM"));
                labelNom.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelNom, c);

                c.gridx++;
                c.gridwidth = 1;
                c.weightx = 1;
                this.add(this.textLibelle, c);
                c.weightx = 0;
                c.gridwidth = 1;

                JCheckBox checkAffichNom = new JCheckBox(getLabelFor("NOM_VISIBLE"));
                c.gridx++;
                c.gridwidth = 1;
                c.weightx = 0;
                this.add(checkAffichNom, c);
                checkAffichNom.setSelected(true);

                /***********************************************************************************
                 * PANEL CALCUL
                 **********************************************************************************/
                JPanel panelCalcul = new JPanel();
                panelCalcul.setLayout(new GridBagLayout());
                final GridBagConstraints cPanel = new DefaultGridBagConstraints();

                final VariableTree tree = new VariableTree();
                JScrollPane paneTree = new JScrollPane(tree);

                cPanel.gridheight = GridBagConstraints.REMAINDER;
                cPanel.weighty = 1;
                cPanel.weightx = 1;
                cPanel.fill = GridBagConstraints.BOTH;
                // panelCalcul.add(paneTree, cPanel);
                cPanel.fill = GridBagConstraints.HORIZONTAL;
                cPanel.weighty = 0;
                cPanel.weightx = 0;
                cPanel.gridheight = 1;

                // Formule base
                cPanel.gridx++;
                final Map<String, List<?>> mapTree = VariablePayeSQLElement.getMapTree();
                this.formuleBase = new SQLJavaEditor(mapTree);
                this.formuleBase.setVarAssign("BASE");

                final JRadioButton radioBase = new JRadioButton(getLabelFor("NB_BASE"));
                panelCalcul.add(radioBase, cPanel);
                cPanel.gridx++;
                panelCalcul.add(this.formuleBase, cPanel);

                // Formule Tx Sal
                cPanel.gridy++;
                cPanel.gridx = 1;

                this.formuleTxSal = new SQLJavaEditor(mapTree);
                this.formuleTxSal.setVarAssign("SAL");
                final JRadioButton radioTxSal = new JRadioButton(getLabelFor("TAUX_SAL"));
                panelCalcul.add(radioTxSal, cPanel);
                cPanel.gridx++;
                panelCalcul.add(this.formuleTxSal, cPanel);

                // Montant Sal Ad
                cPanel.gridy++;
                cPanel.gridx = 1;
                this.formuleMontantSalAd = new SQLJavaEditor(mapTree);
                this.formuleMontantSalAd.setVarAssign("MONTANT");
                final JRadioButton radioMontantsalAd = new JRadioButton(getLabelFor("MONTANT_SAL_AJ"));
                panelCalcul.add(radioMontantsalAd, cPanel);
                cPanel.gridx++;
                panelCalcul.add(this.formuleMontantSalAd, cPanel);

                // Montant Sal Ded
                cPanel.gridy++;
                cPanel.gridx = 1;
                this.formuleMontantSalDed = new SQLJavaEditor(mapTree);
                this.formuleMontantSalDed.setVarAssign("MONTANT");
                final JRadioButton radioMontantSalDed = new JRadioButton(getLabelFor("MONTANT_SAL_DED"));
                panelCalcul.add(radioMontantSalDed, cPanel);
                cPanel.gridx++;
                panelCalcul.add(this.formuleMontantSalDed, cPanel);

                // Formule Tx Pat
                cPanel.gridy++;
                cPanel.gridx = 1;
                this.formuleTxPat = new SQLJavaEditor(mapTree);
                this.formuleTxPat.setVarAssign("PAT");
                final JRadioButton radioTxPat = new JRadioButton(getLabelFor("TAUX_PAT"));
                panelCalcul.add(radioTxPat, cPanel);
                cPanel.gridx++;
                panelCalcul.add(this.formuleTxPat, cPanel);

                // Montant Pat
                cPanel.gridy++;
                cPanel.gridx = 1;
                this.formuleMontantPat = new SQLJavaEditor(mapTree);
                this.formuleMontantPat.setVarAssign("MONTANT");
                final JRadioButton radioMontantPat = new JRadioButton(getLabelFor("MONTANT_PAT"));
                panelCalcul.add(radioMontantPat, cPanel);
                cPanel.gridx++;
                panelCalcul.add(this.formuleMontantPat, cPanel);

                ButtonGroup group1 = new ButtonGroup();
                group1.add(radioBase);
                group1.add(radioTxPat);
                group1.add(radioTxSal);
                group1.add(radioMontantPat);
                group1.add(radioMontantsalAd);
                group1.add(radioMontantSalDed);
                radioBase.setSelected(true);

                // Salarie
                cPanel.gridy++;
                cPanel.gridx = 1;
                JLabel labelSelSal = new JLabel("Salarié");
                labelSelSal.setHorizontalAlignment(SwingConstants.RIGHT);
                panelCalcul.add(labelSelSal, cPanel);

                SQLElement eltSal = new SalarieSQLElement();
                final ElementComboBox selSalarie = new ElementComboBox(false);

                cPanel.gridx++;
                selSalarie.init(eltSal);
                panelCalcul.add(selSalarie, cPanel);

                /***********************************************************************************
                 * PANEL PROPRIETE
                 **********************************************************************************/
                JPanel panelProp = new JPanel();
                panelProp.setLayout(new GridBagLayout());
                cPanel.gridx = 0;
                cPanel.gridy = 0;
                cPanel.weightx = 0;
                cPanel.weighty = 0;
                cPanel.gridwidth = 1;
                cPanel.gridheight = 1;
                cPanel.fill = GridBagConstraints.HORIZONTAL;
                cPanel.anchor = GridBagConstraints.NORTHWEST;
                cPanel.insets = new Insets(2, 2, 1, 2);

                // Periode d'application
                this.addView("ID_PERIODE_VALIDITE", REQ + ";" + DEC + ";" + SEP);
                ElementSQLObject eltInfosPaye = (ElementSQLObject) this.getView("ID_PERIODE_VALIDITE");
                cPanel.gridy = 0;
                cPanel.gridx = 0;
                cPanel.gridheight = GridBagConstraints.REMAINDER;
                JPanel panelPeriodeVal = new JPanel();
                panelPeriodeVal.setBorder(BorderFactory.createTitledBorder("Période de validité"));
                panelPeriodeVal.add(eltInfosPaye);
                cPanel.fill = GridBagConstraints.NONE;
                cPanel.weightx = 0;
                cPanel.weighty = 1;
                panelProp.add(panelPeriodeVal, cPanel);
                cPanel.weightx = 0;
                cPanel.weighty = 0;
                cPanel.gridheight = 1;
                cPanel.fill = GridBagConstraints.HORIZONTAL;

                // Impression
                JLabel labelSelTypeImpression = new JLabel("Impression");
                cPanel.gridy++;
                cPanel.gridx = 1;
                cPanel.weightx = 0;
                panelProp.add(labelSelTypeImpression, cPanel);

                ElementComboBox comboSelTypeImpression = new ElementComboBox(false);
                cPanel.gridx++;
                cPanel.weightx = 1;
                panelProp.add(comboSelTypeImpression, cPanel);

                // Tabbed Pane
                JTabbedPane tab = new JTabbedPane();
                tab.add("Calcul", new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, paneTree, panelCalcul));
                tab.add("Propriétés", panelProp);

                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridy++;
                c.gridx = 0;
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 1;
                c.weighty = 1;
                this.add(tab, c);

                tree.addMouseListener(new MouseAdapter() {

                    public void mousePressed(MouseEvent e) {

                        if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                            TreePath path = tree.getClosestPathForLocation(e.getPoint().x, e.getPoint().y);

                            final Object obj = path.getLastPathComponent();

                            if (obj == null) {
                                return;
                            } else {
                                if (obj instanceof FormuleTreeNode) {
                                    FormuleTreeNode n = (FormuleTreeNode) obj;

                                    if (radioBase.isSelected()) {
                                        int start = formuleBase.getSelectionStart();
                                        String tmp = formuleBase.getText();

                                        formuleBase.setText(tmp.substring(0, start) + n.getTextValue() + tmp.substring(start, tmp.length()));
                                    } else {

                                        if (radioTxPat.isSelected()) {
                                            int start = formuleTxPat.getSelectionStart();
                                            String tmp = formuleTxPat.getText();

                                            formuleTxPat.setText(tmp.substring(0, start) + n.getTextValue() + tmp.substring(start, tmp.length()));
                                        } else {
                                            if (radioMontantPat.isSelected()) {
                                                int start = formuleMontantPat.getSelectionStart();
                                                String tmp = formuleMontantPat.getText();

                                                formuleMontantPat.setText(tmp.substring(0, start) + n.getTextValue() + tmp.substring(start, tmp.length()));
                                            } else {
                                                if (radioMontantsalAd.isSelected()) {
                                                    int start = formuleMontantSalAd.getSelectionStart();
                                                    String tmp = formuleMontantSalAd.getText();

                                                    formuleMontantSalAd.setText(tmp.substring(0, start) + n.getTextValue() + tmp.substring(start, tmp.length()));
                                                } else {
                                                    if (radioMontantSalDed.isSelected()) {
                                                        int start = formuleMontantSalDed.getSelectionStart();
                                                        String tmp = formuleMontantSalDed.getText();

                                                        formuleMontantSalDed.setText(tmp.substring(0, start) + n.getTextValue() + tmp.substring(start, tmp.length()));
                                                    }
                                                }
                                            }

                                        }
                                    }
                                }

                            }
                        }
                    }
                });

                this.addRequiredSQLObject(this.textCode, "CODE");
                this.addSQLObject(this.textLibelle, "NOM");
                this.addSQLObject(this.formuleBase, "NB_BASE");
                this.addSQLObject(this.formuleTxPat, "TAUX_PAT");
                this.addSQLObject(this.formuleTxSal, "TAUX_SAL");
                this.addSQLObject(this.formuleMontantPat, "MONTANT_PAT");
                this.addSQLObject(this.formuleMontantSalAd, "MONTANT_SAL_AJ");
                this.addSQLObject(this.formuleMontantSalDed, "MONTANT_SAL_DED");
                this.addRequiredSQLObject(comboSelTypeImpression, "ID_IMPRESSION_RUBRIQUE");
                this.addRequiredSQLObject(checkAffichNom, "NOM_VISIBLE");

                selSalarie.addValueListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {

                        formuleBase.setSalarieID(selSalarie.getSelectedId());
                        formuleTxPat.setSalarieID(selSalarie.getSelectedId());
                        formuleTxSal.setSalarieID(selSalarie.getSelectedId());
                        formuleMontantPat.setSalarieID(selSalarie.getSelectedId());
                        formuleMontantSalAd.setSalarieID(selSalarie.getSelectedId());
                        formuleMontantSalDed.setSalarieID(selSalarie.getSelectedId());
                    }
                });

            }

            public synchronized boolean isValidated() {
                return super.isValidated() && this.validCode;
            }

            private void isValidCodeName() {
                System.err.println("Changed valid");
                // on vérifie que la variable n'existe pas déja
                SQLSelect selAllCodeName = new SQLSelect(getTable().getBase());

                selAllCodeName.addSelect(RubriqueCommSQLElement.this.getTable().getField("ID"));
                selAllCodeName.setWhere("RUBRIQUE_COMM.CODE", "=", this.textCode.getText().trim());

                int idSelected = this.getSelectedID();
                if (idSelected > 1) {
                    selAllCodeName.andWhere(new Where(RubriqueCommSQLElement.this.getTable().getField("ID"), "!=", idSelected));
                }
                String reqAllCodeName = selAllCodeName.asString();

                Object[] objCodeID = ((List) getTable().getBase().getDataSource().execute(reqAllCodeName, new ArrayListHandler())).toArray();

                SQLSelect selAllVarName = new SQLSelect(getTable().getBase());

                SQLTable tableVar = getTable().getBase().getTable("VARIABLE_PAYE");
                selAllVarName.addSelect(tableVar.getField("ID"));
                selAllVarName.setWhere("VARIABLE_PAYE.NOM", "=", this.textCode.getText().trim());
                String reqAllVarName = selAllVarName.asString();
                Object[] objVarID = ((List) getTable().getBase().getDataSource().execute(reqAllVarName, new ArrayListHandler())).toArray();

                // System.err.println("nb var same " + objVarID.length + " --- nb code same " +
                // objCodeID.length);
                if ((objCodeID.length > 0) || (objVarID.length > 0)) {
                    this.labelWarningBadName.setVisible(true);
                    this.labelBadName.setVisible(true);
                    this.validCode = false;
                } else {

                    List l = VariablePayeSQLElement.getForbiddenVarName();
                    for (int i = 0; i < l.size(); i++) {
                        if (l.get(i).toString().trim().equals(this.textCode.getText().trim())) {
                            this.labelWarningBadName.setVisible(true);
                            this.labelBadName.setVisible(true);
                            this.validCode = false;
                            return;
                        }
                    }

                    this.labelWarningBadName.setVisible(false);
                    this.labelBadName.setVisible(false);
                    this.validCode = true;
                }
            }

            @Override
            public void select(SQLRowAccessor r) {
                super.select(r);
                isValidCodeName();
            }
        };
    }
}
