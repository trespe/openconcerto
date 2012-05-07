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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.accounting.model.PlanComptableGModel;
import org.openconcerto.erp.element.objet.ClasseCompte;
import org.openconcerto.erp.element.objet.Compte;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JMultiLineToolTip;
import org.openconcerto.ui.TitledSeparator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class PlanComptableGPanel extends JPanel {
    // FIXME: creer un panel commun a la gestion des plans comptables pour l'info du compte qui soit
    // listener afin que modifier les infos mette à jour le panel..

    private Vector classeComptes = new Vector();
    private JTabbedPane tabbedClasse = new JTabbedPane();
    private Vector tables = new Vector();
    private JRadioButton radioCompteBase = new JRadioButton("Base");
    private JRadioButton radioCompteAbrege = new JRadioButton("Abrégé");
    private JRadioButton radioCompteDeveloppe = new JRadioButton("Développé");
    private JTextArea textInfos = new JTextArea();
    private JPanel panelCompte = new JPanel();
    private JPanel panelInfosCompte = new JPanel();
    private JPanel panelDetails = new JPanel();
    private Vector actionClickDroit;

    public PlanComptableGPanel() {
        this.actionClickDroit = null;
        uiInit();
    }

    public PlanComptableGPanel(Vector actionClickDroit) {
        this.actionClickDroit = actionClickDroit;
        uiInit();
    }

    private void uiInit() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        this.setOpaque(false);
        this.panelCompte.setOpaque(false);
        this.panelInfosCompte.setOpaque(false);
        this.panelDetails.setOpaque(false);

        this.panelCompte.setLayout(new GridBagLayout());
        this.panelInfosCompte.setLayout(new GridBagLayout());
        this.panelDetails.setLayout(new GridBagLayout());

        /*******************************************************************************************
         * * RadioButton Selection du mode d'affichage du PCG abrege, base ou developpé Panel
         * Details
         ******************************************************************************************/
        this.panelDetails.add(this.radioCompteBase, c);
        this.radioCompteBase.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateCompteTable();
            }
        });

        c.gridy++;
        this.panelDetails.add(this.radioCompteAbrege, c);
        this.radioCompteAbrege.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateCompteTable();
            }
        });

        c.gridy++;
        this.panelDetails.add(this.radioCompteDeveloppe, c);
        this.radioCompteDeveloppe.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateCompteTable();
            }

        });

        ButtonGroup grp1 = new ButtonGroup();
        grp1.add(this.radioCompteBase);
        grp1.add(this.radioCompteAbrege);
        grp1.add(this.radioCompteDeveloppe);

        this.radioCompteBase.setSelected(true);

        /*******************************************************************************************
         * ** Panel Compte
         ******************************************************************************************/
        c.insets = new Insets(2, 2, 1, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        this.panelDetails.setBorder(BorderFactory.createTitledBorder("Détails"));
        this.panelCompte.add(this.panelDetails, c);

        /*******************************************************************************************
         * * Affichage du plan comptable
         ******************************************************************************************/
        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLTable classeCompteTable = base.getTable("CLASSE_COMPTE");

        SQLSelect selClasse = new SQLSelect(base);

        selClasse.addSelect(classeCompteTable.getField("ID"));
        selClasse.addSelect(classeCompteTable.getField("NOM"));
        selClasse.addSelect(classeCompteTable.getField("TYPE_NUMERO_COMPTE"));

        selClasse.addRawOrder("\"CLASSE_COMPTE\".\"TYPE_NUMERO_COMPTE\"");

        String reqClasse = selClasse.asString();
        Object obClasse = base.getDataSource().execute(reqClasse, new ArrayListHandler());

        List myListClasse = (List) obClasse;

        if (myListClasse.size() != 0) {
            for (int k = 0; k < myListClasse.size(); k++) {
                Object[] objTmp = (Object[]) myListClasse.get(k);
                ClasseCompte ccTmp = new ClasseCompte(Integer.parseInt(objTmp[0].toString()), objTmp[1].toString(), objTmp[2].toString());
                this.classeComptes.add(ccTmp);

                JTable tab = creerJTable(ccTmp);
                this.tables.add(tab);
                this.tabbedClasse.add(ccTmp.getNom(), new JScrollPane(tab));
            }
        }
        c.gridwidth = 4;
        c.gridheight = 6;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx++;
        c.gridy = 0;
        this.panelCompte.add(this.tabbedClasse, c);

        /*******************************************************************************************
         * * Informations sur le compte selectionné Panel Infos Compte
         ******************************************************************************************/
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridheight = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.weighty = 0;
        c.gridy = 0;
        c.gridx = 0;
        TitledSeparator sep = new TitledSeparator("Informations sur le compte");
        this.panelInfosCompte.add(sep, c);

        GridBagConstraints cInfos = new GridBagConstraints();
        cInfos.insets = new Insets(0, 0, 0, 0);
        cInfos.fill = GridBagConstraints.BOTH;
        cInfos.anchor = GridBagConstraints.NORTHWEST;
        cInfos.gridx = 0;
        cInfos.gridy = 0;
        cInfos.gridwidth = 1;
        cInfos.gridheight = 1;
        cInfos.weightx = 1;
        cInfos.weighty = 1;
        this.textInfos.setFont(this.getFont());
        this.textInfos.setEditable(false);
        JPanel infos = new JPanel(new GridBagLayout());
        infos.add(this.textInfos, cInfos);

        JScrollPane scrollInfos = new JScrollPane(infos);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = GridBagConstraints.REMAINDER;

        this.panelInfosCompte.add(scrollInfos, c);
        this.panelInfosCompte.setMinimumSize(new Dimension(100, 80));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, this.panelCompte, this.panelInfosCompte);
        split.setBorder(null);
        this.add(split, c);
    }

    // TODO from UCDetector: Change visibility of Method
    // "PlanComptableGPanel.creerJTable(ClasseCompte)" to private
    public JTable creerJTable(ClasseCompte ccTmp) { // NO_UCD

        final PlanComptableGModel model;
        if (this.radioCompteDeveloppe.isSelected()) {
            model = new PlanComptableGModel(ccTmp, 3);
        } else if (this.radioCompteAbrege.isSelected()) {
            model = new PlanComptableGModel(ccTmp, 2);
        } else {
            model = new PlanComptableGModel(ccTmp, 1);
        }

        final JTable table = new JTable(model) {
            public JToolTip createToolTip() {
                JMultiLineToolTip t = new JMultiLineToolTip();
                t.setFixedWidth(500);
                return t;
            }
        };

        table.getColumnModel().getColumn(0).setCellRenderer(new PlanComptableCellRenderer(0));
        table.getColumnModel().getColumn(1).setCellRenderer(new PlanComptableCellRenderer(0));

        // TODO calcul de la taille de la colone numero de compte
        table.getColumnModel().getColumn(0).setMaxWidth(90);

        table.getTableHeader().setReorderingAllowed(false);

        table.addMouseMotionListener(new MouseMotionAdapter() {
            int lastRow = -1;

            public void mouseMoved(final MouseEvent e) {

                final Point p = new Point(e.getX(), e.getY());

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        final int row = table.rowAtPoint(p);
                        if (lastRow != row) {
                            lastRow = row;
                            String strTmp = ((Compte) (model.getComptes().get(lastRow))).getInfos();

                            if (strTmp.length() != 0) {
                                table.setToolTipText(strTmp);
                            } else {
                                table.setToolTipText(null);
                            }

                        }
                    }
                });

            }
        });

        if (this.actionClickDroit != null) {
            // System.out.println("Ajout menu droit");
            table.addMouseListener(new MouseAdapter() {

                public void mousePressed(MouseEvent e) {

                    if (e.getButton() == MouseEvent.BUTTON3) {
                        actionDroitTable(e, table);
                    }
                }
            });
        }

        // Enable row selection (default)
        /**
         * table.setColumnSelectionAllowed(false); table.setRowSelectionAllowed(true);
         * 
         * table.setSelectionMode(table.getSelectionModel().MULTIPLE_INTERVAL_SELECTION);
         */

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow < 0) {
                    // Pas de selection
                    textInfos.setText("Pas de compte séléctionné");
                } else {
                    textInfos.setText(((Compte) (model.getComptes().get(selectedRow))).getInfos());
                }
            }
        });

        return table;
    }

    private void actionDroitTable(MouseEvent e, final JTable table) {
        JPopupMenu menuDroit = new JPopupMenu();

        for (int i = 0; i < this.actionClickDroit.size(); i++) {
            menuDroit.add((AbstractAction) this.actionClickDroit.get(i));
        }

        menuDroit.add(new AbstractAction("Tout sélectionner") {

            public void actionPerformed(ActionEvent e) {
                table.selectAll();
            }
        });

        menuDroit.show(e.getComponent(), e.getX(), e.getY());

        System.out.println("Click droit sur JTable");
    }

    public int getSelectedIndex() {
        return this.tabbedClasse.getSelectedIndex();
    }

    public Vector getTables() {
        return this.tables;
    }

    /**
     * 
     */
    private void updateCompteTable() {
        new Thread(new Runnable() {
            public void run() {
                // TODO: commencer par le compte sélectionné
                for (int i = 0; i < tabbedClasse.getTabCount(); i++) {

                    final JTable tab = creerJTable((ClasseCompte) classeComptes.get(i));
                    final int indexTab = i;

                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {

                            tables.set(indexTab, tab);
                            tabbedClasse.setComponentAt(indexTab, new JScrollPane(tab));
                        }
                    });
                }
            }
        }).start();
    }

}
