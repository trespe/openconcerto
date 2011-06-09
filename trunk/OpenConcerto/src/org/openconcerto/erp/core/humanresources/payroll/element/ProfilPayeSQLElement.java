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

import org.openconcerto.erp.core.common.ui.JNiceButton;
import org.openconcerto.erp.core.humanresources.payroll.component.VariableRowTreeNode;
import org.openconcerto.erp.core.humanresources.payroll.ui.ProfilPayeModel;
import org.openconcerto.erp.model.RubriquePayeTree;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.table.AlternateTableCellRenderer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.tree.TreePath;

public class ProfilPayeSQLElement extends ConfSQLElement {

    public ProfilPayeSQLElement() {
        super("PROFIL_PAYE", "un profil de paye", "profils de paye");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {

        return new BaseSQLComponent(this) {

            private ProfilPayeModel model;
            private JTextField nom;

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                JLabel labelNom = new JLabel(getLabelFor("NOM"));
                labelNom.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelNom, c);
                this.nom = new JTextField();
                c.gridx++;
                this.add(this.nom, c);

                // Arbre contenant les différentes rubriques existantes
                // Panel on the Left
                final RubriquePayeTree treeRubrique = new RubriquePayeTree();
                JPanel panelLeft = new JPanel();
                panelLeft.setLayout(new GridBagLayout());
                // c.gridy++;
                c.gridx = 0;
                c.weightx = 1;
                c.weighty = 1;
                c.fill = GridBagConstraints.BOTH;
                c.gridheight = GridBagConstraints.REMAINDER;

                panelLeft.add(new JScrollPane(treeRubrique), c);

                // Panel on the right
                JPanel panelRight = new JPanel();
                panelRight.setLayout(new GridBagLayout());
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridheight = 1;
                c.weightx = 0;
                c.weighty = 0;
                c.gridx = 0;
                c.gridy = 0;

                // Liste des rubriques du profil
                JLabel labelContenu = new JLabel("Contenu du profil");
                panelRight.add(labelContenu, c);

                // Bouton de gestion des rubriques du profil
                c.fill = GridBagConstraints.NONE;
                JButton boutonUp = new JNiceButton(IListFrame.class.getResource("fleche_haut.png"));
                c.gridx++;
                panelRight.add(boutonUp, c);

                JButton boutonDown = new JNiceButton(IListFrame.class.getResource("fleche_bas.png"));
                c.gridx++;
                panelRight.add(boutonDown, c);

                JButton boutonSuppr = new JNiceButton(ConfSQLElement.class.getResource("delete.png"));
                c.gridx++;
                panelRight.add(boutonSuppr, c);
                c.fill = GridBagConstraints.HORIZONTAL;

                this.model = new ProfilPayeModel(this.getSelectedID());
                final JTable tableProfil = new JTable(this.model);
                tableProfil.setDefaultRenderer(String.class, AlternateTableCellRenderer.createDefault());
                c.gridy++;
                c.gridx = 0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 1;
                c.weighty = 1;
                panelRight.add(new JScrollPane(tableProfil), c);

                c.gridx = 0;
                c.gridy = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 1;
                c.weighty = 1;
                this.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelLeft, panelRight), c);

                treeRubrique.addMouseListener(new MouseAdapter() {

                    public void mousePressed(MouseEvent mE) {
                        if (mE.getClickCount() == 2 && mE.getButton() == MouseEvent.BUTTON1) {

                            TreePath path = treeRubrique.getClosestPathForLocation(mE.getPoint().x, mE.getPoint().y);

                            final Object obj = path.getLastPathComponent();

                            if (obj == null) {
                                return;
                            } else {
                                if (obj instanceof VariableRowTreeNode) {
                                    model.addRowAt(((VariableRowTreeNode) obj).getRow(), tableProfil.getSelectedRow());
                                } else {
                                    return;
                                }
                            }
                        }
                    }
                });

                boutonUp.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        int selectRow = model.upRow(tableProfil.getSelectedRow());
                        if (selectRow > 0) {
                            tableProfil.setRowSelectionInterval(selectRow, selectRow);
                        }
                    }
                });

                boutonDown.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        int selectRow = model.downRow(tableProfil.getSelectedRow());
                        if (selectRow > 0) {
                            tableProfil.setRowSelectionInterval(selectRow, selectRow);
                        }
                    }
                });

                boutonSuppr.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        model.removeRow(tableProfil.getSelectedRow());
                    }
                });

                this.addRequiredSQLObject(this.nom, "NOM");
            }

            public int insert(org.openconcerto.sql.model.SQLRow order) {
                int id = super.insert(order);
                this.model.updateFields(id);
                return id;
            }

            public void update() {
                this.model.updateFields(this.getSelectedID());
                super.update();
            }

            @Override
            public void select(SQLRowAccessor r) {
                super.select(r);
                if (r != null) {
                    this.model.selectID(r.getID());
                }
            }
        };
    }
}
