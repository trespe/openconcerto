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
import org.openconcerto.erp.core.humanresources.payroll.component.RubriqueSQLComponent;
import org.openconcerto.erp.core.humanresources.payroll.component.VariableTree;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;

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
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.tree.TreePath;

// TODO FRAIS NATURE
// FIXME rubrique.code must have a java var syntax
public class RubriqueNetSQLElement extends ConfSQLElement {

    public RubriqueNetSQLElement() {
        super("RUBRIQUE_NET", "une rubrique de net", "rubriques de net");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        l.add("BASE");
        l.add("TAUX");
        l.add("MONTANT");

        l.add("BRUT");
        l.add("FRAIS_PERS");
        l.add("IMPOSABLE");

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
        return new RubriqueSQLComponent(this) {

            private SQLJavaEditor formuleBase, formuleTaux, formuleMontant;

            @Override
            protected void addViews(GridBagConstraints c) {
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

                final JRadioButton radioBase = new JRadioButton(getLabelFor("BASE"));
                panelCalcul.add(radioBase, cPanel);
                cPanel.gridx++;
                panelCalcul.add(this.formuleBase, cPanel);

                JSeparator sep1 = new JSeparator();
                cPanel.gridwidth = GridBagConstraints.REMAINDER;
                cPanel.gridy++;
                cPanel.gridx = 1;
                cPanel.weightx = 1;
                panelCalcul.add(sep1, cPanel);
                cPanel.gridwidth = 1;
                cPanel.weightx = 0;

                // Formule Taux
                cPanel.gridy++;
                cPanel.gridx = 1;
                this.formuleTaux = new SQLJavaEditor(mapTree);
                this.formuleTaux.setVarAssign("TAUX");
                final JRadioButton radioTaux = new JRadioButton(getLabelFor("TAUX"));
                panelCalcul.add(radioTaux, cPanel);
                cPanel.gridx++;
                panelCalcul.add(this.formuleTaux, cPanel);

                JSeparator sep2 = new JSeparator();
                cPanel.gridwidth = GridBagConstraints.REMAINDER;
                cPanel.gridy++;
                cPanel.gridx = 1;
                cPanel.weightx = 1;
                panelCalcul.add(sep2, cPanel);
                cPanel.gridwidth = 1;
                cPanel.weightx = 0;

                // Formule Montant
                cPanel.gridy++;
                cPanel.gridx = 1;
                this.formuleMontant = new SQLJavaEditor(mapTree);
                this.formuleMontant.setVarAssign("MONTANT");
                final JRadioButton radioMontant = new JRadioButton(getLabelFor("MONTANT"));
                panelCalcul.add(radioMontant, cPanel);
                cPanel.gridx++;
                panelCalcul.add(this.formuleMontant, cPanel);

                JSeparator sep3 = new JSeparator();
                cPanel.gridwidth = GridBagConstraints.REMAINDER;
                cPanel.gridy++;
                cPanel.gridx = 1;
                cPanel.weightx = 1;
                panelCalcul.add(sep3, cPanel);
                cPanel.gridwidth = 1;
                cPanel.weightx = 0;

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

                ButtonGroup groupRadio = new ButtonGroup();
                groupRadio.add(radioBase);
                groupRadio.add(radioMontant);
                groupRadio.add(radioTaux);
                radioBase.setSelected(true);

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

                                        if (radioMontant.isSelected()) {
                                            int start = formuleMontant.getSelectionStart();
                                            String tmp = formuleMontant.getText();

                                            formuleMontant.setText(tmp.substring(0, start) + n.getTextValue() + tmp.substring(start, tmp.length()));
                                        } else {
                                            int start = formuleTaux.getSelectionStart();
                                            String tmp = formuleTaux.getText();

                                            formuleTaux.setText(tmp.substring(0, start) + n.getTextValue() + tmp.substring(start, tmp.length()));
                                        }
                                    }
                                }

                            }
                        }
                    }
                });

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
                cPanel.weighty = 1;
                cPanel.fill = GridBagConstraints.NONE;
                JPanel panelPeriodeVal = new JPanel();
                panelPeriodeVal.setBorder(BorderFactory.createTitledBorder("Période de validité"));
                panelPeriodeVal.add(eltInfosPaye);
                panelProp.add(panelPeriodeVal, cPanel);

                cPanel.weightx = 0;
                cPanel.weighty = 0;
                cPanel.gridheight = 1;
                cPanel.fill = GridBagConstraints.HORIZONTAL;

                // Type
                JLabel labelSelTypeRubrique = new JLabel("Type");
                labelSelTypeRubrique.setHorizontalAlignment(SwingConstants.RIGHT);
                cPanel.gridy++;
                cPanel.gridx = 1;
                cPanel.weightx = 0;
                cPanel.anchor = GridBagConstraints.WEST;
                panelProp.add(labelSelTypeRubrique, cPanel);
                cPanel.anchor = GridBagConstraints.NORTHWEST;

                ElementComboBox comboSelType = new ElementComboBox(true, 20);

                cPanel.gridx++;
                panelProp.add(comboSelType, cPanel);

                // Impression
                JLabel labelSelTypeRubriqueImpression = new JLabel("Impression");
                labelSelTypeRubriqueImpression.setHorizontalAlignment(SwingConstants.RIGHT);
                cPanel.gridx++;
                cPanel.anchor = GridBagConstraints.WEST;
                panelProp.add(labelSelTypeRubriqueImpression, cPanel);
                cPanel.anchor = GridBagConstraints.NORTHWEST;

                ElementComboBox comboSelTypeImpression = new ElementComboBox(true, 20);
                cPanel.gridx++;
                c.fill = GridBagConstraints.NONE;
                c.weightx = 0;
                panelProp.add(comboSelTypeImpression, cPanel);

                // Imposable
                cPanel.gridx = 1;
                cPanel.weightx = 1;
                cPanel.gridy++;
                c.fill = GridBagConstraints.HORIZONTAL;
                cPanel.gridwidth = GridBagConstraints.REMAINDER;
                JCheckBox checkImpo = new JCheckBox(getLabelFor("IMPOSABLE"));
                panelProp.add(checkImpo, cPanel);

                // Frais Pers
                cPanel.gridy++;
                JCheckBox checkFraisPers = new JCheckBox(getLabelFor("FRAIS_PERS"));
                panelProp.add(checkFraisPers, cPanel);

                // Brut
                cPanel.gridy++;
                JCheckBox checkBrut = new JCheckBox(getLabelFor("BRUT"));
                panelProp.add(checkBrut, cPanel);

                // Tabbed Pane
                JTabbedPane tab = new JTabbedPane();
                tab.add("Calcul", new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, paneTree, panelCalcul));
                tab.add("Propriétés", panelProp);

                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridx = 0;
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 1;
                c.weighty = 1;
                this.add(tab, c);

                this.addSQLObject(this.formuleBase, "BASE");
                this.addSQLObject(this.formuleTaux, "TAUX");
                this.addSQLObject(this.formuleMontant, "MONTANT");
                this.addSQLObject(checkBrut, "BRUT");
                this.addSQLObject(checkFraisPers, "FRAIS_PERS");
                this.addSQLObject(checkImpo, "IMPOSABLE");
                this.addRequiredSQLObject(comboSelTypeImpression, "ID_IMPRESSION_RUBRIQUE");
                this.addRequiredSQLObject(comboSelType, "ID_TYPE_RUBRIQUE_NET");

                comboSelType.setButtonsVisible(false);
                comboSelTypeImpression.setButtonsVisible(false);

                selSalarie.addValueListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        // TODO Auto-generated method stub
                        formuleBase.setSalarieID(selSalarie.getSelectedId());
                        formuleTaux.setSalarieID(selSalarie.getSelectedId());
                        formuleMontant.setSalarieID(selSalarie.getSelectedId());
                    }
                });
            }
        };
    }

    @Override
    protected String createCode() {
        return "humanresources.payroll.category";
    }
}
