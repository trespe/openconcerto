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
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.tree.TreePath;

// FIXME bug Layout

public class RubriqueCotisationSQLElement extends ConfSQLElement {

    public RubriqueCotisationSQLElement() {
        super("RUBRIQUE_COTISATION", "une rubrique de cotisation", "rubriques de cotisation");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        l.add("NOM");
        l.add("ID_CAISSE_COTISATION");
        l.add("BASE");
        l.add("TX_PAT");
        l.add("TX_SAL");
        l.add("BRUT");
        l.add("PART_CSG");
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

            private ElementComboBox selCaisseCot = new ElementComboBox();
            private SQLJavaEditor formuleBase, formuleTxSal, formuleTxPat;

            @Override
            protected void addViews(GridBagConstraints c) {
                // Caisse de cotisation
                c.gridx = 0;
                JLabel labelSelCaisse = new JLabel(getLabelFor("ID_CAISSE_COTISATION"));
                labelSelCaisse.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelSelCaisse, c);

                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(this.selCaisseCot, c);
                c.gridwidth = 1;

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

                // Formule Tx Sal
                cPanel.gridy++;
                cPanel.gridx = 1;

                this.formuleTxSal = new SQLJavaEditor(mapTree);
                this.formuleTxSal.setVarAssign("SAL");
                final JRadioButton radioTxSal = new JRadioButton(getLabelFor("TX_SAL"));
                panelCalcul.add(radioTxSal, cPanel);
                cPanel.gridx++;
                panelCalcul.add(this.formuleTxSal, cPanel);

                // Formule Tx Pat
                cPanel.gridy++;
                cPanel.gridx = 1;
                this.formuleTxPat = new SQLJavaEditor(mapTree);
                this.formuleTxPat.setVarAssign("PAT");
                final JRadioButton radioTxPat = new JRadioButton(getLabelFor("TX_PAT"));
                panelCalcul.add(radioTxPat, cPanel);
                cPanel.gridx++;
                panelCalcul.add(this.formuleTxPat, cPanel);

                ButtonGroup group1 = new ButtonGroup();
                group1.add(radioBase);
                group1.add(radioTxPat);
                group1.add(radioTxSal);
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

                // Imposable
                cPanel.gridx = 1;
                cPanel.weightx = 0;
                cPanel.gridy++;
                JCheckBox checkImpo = new JCheckBox(getLabelFor("IMPOSABLE"));
                panelProp.add(checkImpo, cPanel);

                // Part csg
                cPanel.gridy++;
                JCheckBox checkPartPatr = new JCheckBox(getLabelFor("PART_CSG"));
                panelProp.add(checkPartPatr, cPanel);

                // Brut
                cPanel.gridy++;
                JCheckBox checkBrut = new JCheckBox(getLabelFor("BRUT"));
                panelProp.add(checkBrut, cPanel);

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
                                            int start = formuleTxSal.getSelectionStart();
                                            String tmp = formuleTxSal.getText();

                                            formuleTxSal.setText(tmp.substring(0, start) + n.getTextValue() + tmp.substring(start, tmp.length()));
                                        }
                                    }
                                }

                            }
                        }
                    }
                });

                this.addRequiredSQLObject(this.selCaisseCot, "ID_CAISSE_COTISATION");
                this.addSQLObject(this.formuleBase, "BASE");
                this.addSQLObject(this.formuleTxPat, "TX_PAT");
                this.addSQLObject(this.formuleTxSal, "TX_SAL");
                this.addSQLObject(checkBrut, "BRUT");
                this.addSQLObject(checkPartPatr, "PART_CSG");
                this.addSQLObject(checkImpo, "IMPOSABLE");
                this.addRequiredSQLObject(comboSelTypeImpression, "ID_IMPRESSION_RUBRIQUE");

                selSalarie.addValueListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {

                        formuleBase.setSalarieID(selSalarie.getSelectedId());
                        formuleTxPat.setSalarieID(selSalarie.getSelectedId());
                        formuleTxSal.setSalarieID(selSalarie.getSelectedId());
                    }
                });
            }
        };
    }
}
