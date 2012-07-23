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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.JNiceButton;
import org.openconcerto.erp.core.humanresources.payroll.component.VariableRowTreeNode;
import org.openconcerto.erp.core.humanresources.payroll.ui.FichePayeRenderer;
import org.openconcerto.erp.generationEcritures.GenerationMvtFichePaye;
import org.openconcerto.erp.model.FichePayeModel;
import org.openconcerto.erp.model.RubriquePayeTree;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.TreesOfSQLRows;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.utils.checks.ValidState;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class FichePayeSQLElement extends ComptaSQLConfElement {

    public FichePayeSQLElement() {
        super("FICHE_PAYE", "une fiche de paye", "fiches de paye");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_SALARIE");
        l.add("ID_MOIS");
        l.add("ANNEE");
        l.add("NET_A_PAYER");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_SALARIE");
        l.add("ID_MOIS");
        l.add("ANNEE");
        return l;
    }

    /*
     * protected List getPrivateFields() { final List l = new ArrayList(); l.add("ID_CUMULS_PAYE");
     * return l; }
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    // FIXME Place des rubriques importantes --> [Brut -- cotisations -- cotisations with CSG --
    // Net]
    // FIXME Date periode must be correct
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private FichePayeModel model;
            private ElementComboBox comboSelProfil, selSalCombo;
            private EditFrame edit = null;
            private ElementComboBox selMois;
            private int dernMois, dernAnnee;
            private JTextField textAnnee;
            JDate dateDu, dateAu;
            private JScrollPane paneTreeLeft;
            private JPanel pDate;
            private JButton buttonValider, buttonGenCompta;

            public void addViews() {

                this.dernMois = 0;
                this.dernAnnee = 0;

                this.setLayout(new GridBagLayout());

                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Tree elt Fiche de Paye On the left
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 1;
                c.weighty = 1;
                c.gridheight = GridBagConstraints.REMAINDER;
                final RubriquePayeTree tree = new RubriquePayeTree();
                tree.expandRow(0);
                this.paneTreeLeft = new JScrollPane(tree);
                // this.add(this.paneTreeLeft, c);

                // Panel Fiche paye on the right
                // Salarie
                JPanel panelRight = new JPanel();
                panelRight.setLayout(new GridBagLayout());
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 1;
                c.weighty = 0;
                c.gridheight = 1;
                c.gridwidth = 2;
                this.selSalCombo = new ElementComboBox();
                // c.gridx++;
                panelRight.add(this.selSalCombo, c);

                // Mois
                c.gridy++;
                // c.gridx++;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 1;
                c.weighty = 0;
                c.gridheight = 1;
                c.gridwidth = 3;
                JLabel labelMois = new JLabel("Fiche de paye du mois de");
                this.selMois = new ElementComboBox(true, 20);
                // this.selMois.setEditable(true);
                // /this.selMois.setEnabled(true);

                JLabel labelDu = new JLabel("Du");
                JLabel labelAu = new JLabel("Au");
                this.dateDu = new JDate();
                this.dateAu = new JDate();

                // JTextField textMois = new JTextField();
                JLabel labelAnnee = new JLabel("Année");
                this.textAnnee = new JTextField();
                {
                    this.pDate = new JPanel();
                    this.pDate.setOpaque(false);
                    this.pDate.add(labelMois);
                    this.pDate.add(this.selMois);
                    this.pDate.add(labelAnnee);
                    this.pDate.add(this.textAnnee);
                    this.pDate.add(labelDu);
                    this.pDate.add(this.dateDu);
                    this.pDate.add(labelAu);
                    this.pDate.add(this.dateAu);
                    panelRight.add(this.pDate, c);
                }
                c.gridx += 2;
                c.weightx = 1;
                c.gridwidth = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                panelRight.add(new JPanel(), c);

                // Action Button
                c.gridx++;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0;
                c.weighty = 0;

                JPanel pButtons = new JPanel();
                pButtons.setOpaque(false);
                JButton buttonUp = new JNiceButton(IListFrame.class.getResource("fleche_haut.png"));
                JButton buttonDown = new JNiceButton(IListFrame.class.getResource("fleche_bas.png"));
                JButton buttonRemove = new JNiceButton(SQLComponent.class.getResource("delete.png"));
                {
                    pButtons.add(buttonUp);
                    pButtons.add(buttonDown);
                    pButtons.add(buttonRemove);
                }
                panelRight.add(pButtons, c);

                // Table
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 1;
                c.weighty = 1;
                c.gridx = 1;
                c.gridy++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.model = new FichePayeModel(1);
                final JTable table = new JTable(this.model);
                panelRight.add(new JScrollPane(table), c);
                FichePayeRenderer rend = new FichePayeRenderer();
                table.setDefaultRenderer(String.class, rend);
                table.setDefaultRenderer(Float.class, rend);

                // Import profil
                c.gridx = 1;
                c.gridy++;
                c.weightx = 0;
                c.weighty = 0;
                c.gridwidth = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                JLabel labelProfil = new JLabel("Importer depuis un profil prédéfini");
                panelRight.add(labelProfil, c);
                c.gridwidth = 1;

                this.comboSelProfil = new ElementComboBox();
                // this.comboSelProfil = new ElementComboBox();
                this.comboSelProfil.setListIconVisible(false);
                c.gridx++;
                c.gridwidth = 1;

                // this.comboSelProfil.init(eltProfil.getTable().getField("NOM"), null);
                panelRight.add(this.comboSelProfil, c);

                JButton buttonImportProfil = new JButton("Importer");
                c.gridx++;
                panelRight.add(buttonImportProfil, c);

                // Total Periode
                JPanel panelTotal = new JPanel();
                panelTotal.setBorder(BorderFactory.createTitledBorder("Total période"));
                panelTotal.setLayout(new GridBagLayout());
                GridBagConstraints cPanel = new DefaultGridBagConstraints();

                // Salaire brut
                JLabel labelBrut = new JLabel(getLabelFor("SAL_BRUT"));
                panelTotal.add(labelBrut, cPanel);
                JTextField textSalBrut = new JTextField(10);
                cPanel.gridx++;
                cPanel.weightx = 0;
                panelTotal.add(textSalBrut, cPanel);
                textSalBrut.setEditable(false);
                textSalBrut.setEnabled(false);

                // acompte
                cPanel.gridx++;
                JLabel labelAcompte = new JLabel(getLabelFor("ACOMPTE"));
                panelTotal.add(labelAcompte, cPanel);
                JTextField textAcompte = new JTextField(10);
                cPanel.gridx++;
                panelTotal.add(textAcompte, cPanel);
                // textAcompte.setEditable(false);
                // textAcompte.setEnabled(false);

                // Conges Acquis
                cPanel.gridx++;
                JLabel labelCongesAcquis = new JLabel(getLabelFor("CONGES_ACQUIS"));
                panelTotal.add(labelCongesAcquis, cPanel);
                JTextField textCongesAcquis = new JTextField(10);
                cPanel.gridx++;
                panelTotal.add(textCongesAcquis, cPanel);

                // cotisation salariale
                cPanel.gridx = 0;
                cPanel.gridy++;
                JLabel labelCotSal = new JLabel(getLabelFor("COT_SAL"));
                panelTotal.add(labelCotSal, cPanel);
                JTextField textCotSal = new JTextField(10);
                cPanel.gridx++;
                panelTotal.add(textCotSal, cPanel);
                textCotSal.setEditable(false);
                textCotSal.setEnabled(false);

                // cotisation patronale
                cPanel.gridx++;
                JLabel labelCotPat = new JLabel(getLabelFor("COT_PAT"));
                panelTotal.add(labelCotPat, cPanel);
                JTextField textCotPat = new JTextField(10);
                cPanel.gridx++;
                panelTotal.add(textCotPat, cPanel);
                textCotPat.setEditable(false);
                textCotPat.setEnabled(false);

                JLabel labelCSG = new JLabel(getLabelFor("CSG"));
                cPanel.gridx++;
                panelTotal.add(labelCSG, cPanel);
                JTextField textCSG = new JTextField(10);
                cPanel.gridx++;
                panelTotal.add(textCSG, cPanel);
                textCSG.setEditable(false);
                textCSG.setEnabled(false);

                // net imposable
                cPanel.gridx = 0;
                cPanel.gridy++;
                JLabel labelNetImp = new JLabel(getLabelFor("NET_IMP"));
                panelTotal.add(labelNetImp, cPanel);
                JTextField textNetImp = new JTextField(10);
                cPanel.gridx++;
                panelTotal.add(textNetImp, cPanel);
                textNetImp.setEditable(false);
                textNetImp.setEnabled(false);

                cPanel.gridx++;
                JLabel labelNetAPayer = new JLabel(getLabelFor("NET_A_PAYER"));
                panelTotal.add(labelNetAPayer, cPanel);
                JTextField textNetAPayer = new JTextField(10);
                cPanel.gridx++;
                panelTotal.add(textNetAPayer, cPanel);
                textNetAPayer.setEditable(false);
                textNetAPayer.setEnabled(false);

                c.gridx = 1;
                c.gridy++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                panelRight.add(panelTotal, c);

                // Cumuls

                c.gridx = 1;
                c.gridy++;
                c.gridwidth = 1;
                c.fill = GridBagConstraints.NONE;
                this.buttonValider = new JButton("Valider");
                // panelRight.add(buttonValider, c);

                c.gridx++;
                c.gridwidth = 1;
                this.buttonGenCompta = new JButton("Generer la comptabilité");
                // panelRight.add(buttonGenCompta, c);

                c.gridx = 0;
                c.gridy = 0;
                c.gridwidth = 1;
                c.gridheight = 1;
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 1;
                c.weighty = 1;
                this.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.paneTreeLeft, panelRight), c);

                // Listeners
                this.buttonGenCompta.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int[] i = new int[1];
                        i[0] = getSelectedID();

                        SQLRow rowMois = getTable().getBase().getTable("MOIS").getRow(selMois.getSelectedId());

                        new GenerationMvtFichePaye(i, rowMois.getString("NOM"), textAnnee.getText());
                    }
                });

                this.buttonValider.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        System.err.println("Validation de la fiche de paye");
                        validationFiche();
                    }
                });

                buttonUp.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        int newRowSelected = model.upRow(table.getSelectedRow());
                        if (newRowSelected >= 0) {
                            table.setRowSelectionInterval(newRowSelected, newRowSelected);
                        }
                    }
                });

                buttonDown.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        int newRowSelected = model.downRow(table.getSelectedRow());
                        if (newRowSelected >= 0) {
                            table.setRowSelectionInterval(newRowSelected, newRowSelected);
                        }
                    }
                });

                buttonRemove.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        model.removeRow(table.getSelectedRow());
                    }
                });

                tree.addMouseListener(new MouseAdapter() {

                    public void mousePressed(MouseEvent mE) {

                        TreePath path = tree.getClosestPathForLocation(mE.getPoint().x, mE.getPoint().y);

                        final Object obj = path.getLastPathComponent();

                        if (obj == null) {
                            return;
                        }

                        if (mE.getClickCount() == 2 && mE.getButton() == MouseEvent.BUTTON1) {

                            if (obj instanceof VariableRowTreeNode) {
                                model.addRowAt(((VariableRowTreeNode) obj).getRow(), table.getSelectedRow());
                            }
                        } else {

                            if (mE.getButton() == 3) {

                                if (obj instanceof VariableRowTreeNode) {

                                    final SQLRow row = ((VariableRowTreeNode) obj).getRow();

                                    JPopupMenu menuDroit = new JPopupMenu();

                                    menuDroit.add(new AbstractAction("Editer") {
                                        public void actionPerformed(ActionEvent e) {

                                            if (edit != null) {
                                                edit.dispose();
                                            }
                                            edit = new EditFrame(Configuration.getInstance().getDirectory().getElement(row.getTable()), EditFrame.MODIFICATION);
                                            edit.selectionId(row.getID(), 0);
                                            edit.pack();
                                            edit.setVisible(true);
                                        }
                                    });

                                    menuDroit.add(new AbstractAction("Nouvelle rubrique") {
                                        public void actionPerformed(ActionEvent e) {

                                            if (edit != null) {
                                                edit.dispose();
                                            }
                                            edit = new EditFrame(Configuration.getInstance().getDirectory().getElement(row.getTable()));
                                            edit.pack();
                                            edit.setVisible(true);
                                        }
                                    });

                                    menuDroit.show(mE.getComponent(), mE.getPoint().x, mE.getPoint().y);

                                }
                            }
                        }
                    }
                });

                this.dateDu.addValueListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (!dateDu.isEmpty()) {
                            Date d = dateDu.getValue();

                            if (d != null) {
                                Calendar cal = Calendar.getInstance();
                                cal.setTime(d);
                                if (selMois.getSelectedId() > 1 && cal.get(Calendar.MONTH) + 2 != selMois.getSelectedId()) {

                                    cal.set(Calendar.DAY_OF_MONTH, 1);
                                    cal.set(Calendar.MONTH, selMois.getSelectedId() - 2);
                                    System.err.println("Du " + cal.getTime());
                                    dateDu.setValue(cal.getTime());
                                }
                            }
                        }
                    }
                });

                this.dateAu.addValueListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (!dateAu.isEmpty()) {
                            Date d = dateAu.getValue();
                            if (d != null) {
                                Calendar cal = Calendar.getInstance();
                                cal.setTime(d);
                                if (selMois.getSelectedId() > 1 && cal.get(Calendar.MONTH) + 2 != selMois.getSelectedId()) {

                                    // TODO checker l'annee
                                    // TODO ajouter dans le isValidated du au compris dans le mois
                                    // selectionne

                                    // Calendar.getInstance().set(Calendar.DAY_OF_MONTH, maxDay);
                                    cal.set(Calendar.DAY_OF_MONTH, 1);
                                    cal.set(Calendar.MONTH, selMois.getSelectedId() - 2);
                                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                                    System.err.println("Au " + cal.getTime());
                                    dateAu.setValue(cal.getTime());
                                }
                            }
                        }
                    }
                });

                this.addRequiredSQLObject(this.textAnnee, "ANNEE");
                this.addRequiredSQLObject(this.selMois, "ID_MOIS");
                this.addSQLObject(this.comboSelProfil, "ID_PROFIL_PAYE");

                this.addSQLObject(textCongesAcquis, "CONGES_ACQUIS");
                this.addSQLObject(textCotPat, "COT_PAT");
                this.addSQLObject(textCotSal, "COT_SAL");
                this.addSQLObject(textCSG, "CSG");
                this.addSQLObject(textNetAPayer, "NET_A_PAYER");
                this.addSQLObject(textNetImp, "NET_IMP");
                this.addSQLObject(textSalBrut, "SAL_BRUT");
                this.addSQLObject(textAcompte, "ACOMPTE");
                this.addSQLObject(this.selSalCombo, "ID_SALARIE");

                this.addRequiredSQLObject(this.dateDu, "DU");
                this.addRequiredSQLObject(this.dateAu, "AU");

                this.selSalCombo.setEditable(false);
                this.selSalCombo.setEnabled(false);
                this.selSalCombo.setButtonsVisible(false);
                // this.selSalCombo.setVisible(false);

                buttonImportProfil.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {

                        model.loadFromProfil(comboSelProfil.getSelectedId());
                    }
                });
            }

            private boolean isDateValid() {

                String yearS = this.textAnnee.getText().trim();
                int annee = (yearS.length() == 0) ? 0 : Integer.parseInt(yearS);

                int mois = this.selMois.getSelectedId();
                // System.err.println("année " + annee + " dernAnnee " + this.dernAnnee + " mois " +
                // mois + " dernMois " + this.dernMois);

                return ((this.dernAnnee == 0) ? true : annee > this.dernAnnee) || ((this.dernMois == 0 || this.dernMois == 13) ? true : mois > this.dernMois);
            }

            @Override
            public synchronized ValidState getValidState() {
                // FIXME add fireValidChange()
                return super.getValidState().and(ValidState.createCached(isDateValid(), "Date invalide"));
            }

            public int insert(SQLRow order) {

                int id = super.insert(order);
                this.model.updateFields(id);
                return id;
            }

            @Override
            public void update() {
                super.update();
                this.model.updateFields(this.getSelectedID());
            }

            @Override
            public void select(SQLRowAccessor r) {

                // System.err.println("SELECT FICHE ID -> " + r.getID());
                super.select(r);

                if (r != null && r.getID() > 1) {
                    this.model.setFicheID(r.getID());

                    SQLTable tableSal = getTable().getBase().getTable("SALARIE");
                    SQLRow rowSal = tableSal.getRow(r.getInt("ID_SALARIE"));

                    this.dernMois = rowSal.getInt("DERNIER_MOIS");
                    this.dernAnnee = rowSal.getInt("DERNIERE_ANNEE");

                    this.selSalCombo.setVisible(((Boolean) r.getObject("VALIDE")).booleanValue());
                    this.paneTreeLeft.setVisible(!((Boolean) r.getObject("VALIDE")).booleanValue());
                    this.buttonValider.setVisible(!((Boolean) r.getObject("VALIDE")).booleanValue());
                    setpDateEnabled(!((Boolean) r.getObject("VALIDE")).booleanValue());
                }
                this.selSalCombo.setEditable(false);
                this.selSalCombo.setEnabled(false);
                this.selMois.setButtonsVisible(false);
                this.selSalCombo.setButtonsVisible(false);
            }

            private void setpDateEnabled(boolean b) {

                // System.err.println("Set date enable --> " + b);
                this.selMois.setEditable(b);
                // this.selMois.setEnabled(b);

                this.textAnnee.setEditable(b);
                this.textAnnee.setEnabled(b);

                this.dateDu.setEditable(b);
                this.dateDu.setEnabled(b);

                this.dateAu.setEditable(b);
                this.dateAu.setEnabled(b);
            }

            private void validationFiche() {

                this.update();
                FichePayeSQLElement.validationFiche(this.getSelectedID());
            }

            protected SQLRowValues createDefaults() {

                System.err.println("**********Set Defaults on FichePaye.date");
                SQLRowValues rowVals = new SQLRowValues(getTable());
                Calendar cal = Calendar.getInstance();
                rowVals.put("ID_MOIS", cal.get(Calendar.MONTH) + 2);
                rowVals.put("ANNEE", cal.get(Calendar.YEAR));

                cal.set(Calendar.DAY_OF_MONTH, 1);
                rowVals.put("DU", new java.sql.Date(cal.getTime().getTime()));

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                rowVals.put("AU", new java.sql.Date(cal.getTime().getTime()));
                return rowVals;
            }

        };
    }

    @Override
    protected void archive(TreesOfSQLRows trees, boolean cutLinks) throws SQLException {
        // TODO Auto-generated method stub
        super.archive(trees, cutLinks);
        for (SQLRow row : trees.getRows()) {
            if (row != null && row.getID() > 1) {
                if (JOptionPane.showConfirmDialog(null, "Soustraire les cumuls de cette fiche à celle en cours?", "Suppression d'une fiche de paye", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    // on effectue le cumul
                    // System.err.println("Calcul des cumuls");

                    SQLRow rowSal = row.getForeignRow("ID_SALARIE");

                    SQLRow rowCumuls = rowSal.getForeignRow("ID_CUMULS_PAYE");

                    float salBrut = rowCumuls.getFloat("SAL_BRUT_C") - row.getFloat("SAL_BRUT");
                    float cgs = rowCumuls.getFloat("CSG_C") - row.getFloat("CSG");
                    float cotSal = rowCumuls.getFloat("COT_SAL_C") - row.getFloat("COT_SAL");
                    float cotPat = rowCumuls.getFloat("COT_PAT_C") - row.getFloat("COT_PAT");
                    float netImp = rowCumuls.getFloat("NET_IMP_C") - row.getFloat("NET_IMP");
                    float netAPayer = rowCumuls.getFloat("NET_A_PAYER_C") - row.getFloat("NET_A_PAYER") - row.getFloat("ACOMPTE");

                    SQLRowValues rowValsCumul = rowCumuls.createEmptyUpdateRow();
                    rowValsCumul.put("SAL_BRUT_C", new Float(salBrut));
                    rowValsCumul.put("COT_SAL_C", new Float(cotSal));
                    rowValsCumul.put("COT_PAT_C", new Float(cotPat));
                    rowValsCumul.put("NET_IMP_C", new Float(netImp));
                    rowValsCumul.put("NET_A_PAYER_C", new Float(netAPayer));
                    rowValsCumul.put("CSG_C", new Float(cgs));

                    try {
                        rowValsCumul.update();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    // Calcul des congés
                    SQLRow rowCumulConge = rowSal.getForeignRow("ID_CUMULS_CONGES");
                    SQLRow rowVarSal = row.getForeignRow("ID_VARIABLE_SALARIE");
                    float congeCumule = rowCumulConge.getFloat("ACQUIS");
                    float congeRestant = rowCumulConge.getFloat("RESTANT");

                    float prisPeriode = rowVarSal.getFloat("CONGES_PRIS");

                    congeRestant += prisPeriode;
                    congeCumule -= row.getFloat("CONGES_ACQUIS");

                    SQLRowValues rowValsCumulsConges = new SQLRowValues(rowCumulConge.getTable());
                    rowValsCumulsConges.put("ACQUIS", new Float(congeCumule));
                    rowValsCumulsConges.put("RESTANT", new Float(congeRestant));

                    try {
                        rowValsCumulsConges.update(rowCumulConge.getID());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    /**
     * Validation de la fiche de paye en cours pour un salarié
     * 
     * @param id id de la fiche à valider
     */
    public static synchronized void validationFiche(int id) {
        final int oldID = id;

        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLTable tableSal = base.getTable("SALARIE");
        SQLTable tableCumulConge = base.getTable("CUMULS_CONGES");
        SQLTable tableVariableSal = base.getTable("VARIABLE_SALARIE");
        SQLTable tableFiche = base.getTable("FICHE_PAYE");
        SQLTable tableFicheElt = base.getTable("FICHE_PAYE_ELEMENT");
        SQLTable tableCumuls = base.getTable("CUMULS_PAYE");

        final SQLRowValues rowValsSal = new SQLRowValues(tableSal);

        final SQLRow rowFiche = tableFiche.getRow(oldID);
        final SQLRow rowSal = tableSal.getRow(rowFiche.getInt("ID_SALARIE"));

        // On teste que la fiche est entrée dans une période non cloturée
        if (!checkDateValid(id)) {
            String msg = "Impossible de créer la fiche de paye de " + rowSal.getString("NOM") + " " + rowSal.getString("PRENOM") + ".\n La période est cloturée.";
            JOptionPane.showMessageDialog(null, msg, "Création paye impossible", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // valider les elements de la fiche
        validElements(id);

        // Sauvegarde des valeurs de la fiche (cumuls, conges, ...)
        stockValidValues(oldID);

        // creer et associer une nouvelle fiche au salarie
        final SQLRowValues rowValsNewFiche = new SQLRowValues(tableFiche);

        try {
            SQLRow r = rowValsNewFiche.insert();
            rowValsNewFiche.put("ID", r.getID());
            // System.err.println("rowValsNewFiche -----> " + r.getID());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // mis a jour de la periode
        int mois = rowFiche.getInt("ID_MOIS");
        int annee = rowFiche.getInt("ANNEE");
        rowValsNewFiche.put("ID_MOIS", mois);
        rowValsNewFiche.put("ANNEE", annee);
        rowValsNewFiche.put("DU", rowFiche.getObject("DU"));
        rowValsNewFiche.put("AU", rowFiche.getObject("AU"));
        rowValsNewFiche.put("ID_PROFIL_PAYE", rowFiche.getInt("ID_PROFIL_PAYE"));

        /*
         * int ancMois = rowFiche.getInt("ID_MOIS"); int ancAnnee = rowFiche.getInt("ANNEE");
         * 
         * rowValsSal.put("DERNIER_MOIS", ancMois); rowValsSal.put("DERNIERE_ANNEE", ancAnnee);
         * 
         * try { rowValsSal.update(rowFiche.getInt("ID_SALARIE")); } catch (SQLException e1) {
         * e1.printStackTrace(); }
         * 
         * int mois = ancMois - 2; mois = (mois + 1) % 12; mois += 2; int annee = ancAnnee; if (mois
         * == 2) { annee++; } rowValsNewFiche.put("ID_MOIS", mois); rowValsNewFiche.put("ANNEE",
         * annee); rowValsNewFiche.put("ID_PROFIL_PAYE", rowFiche.getInt("ID_PROFIL_PAYE"));
         * 
         * try { rowValsNewFiche.update(); } catch (SQLException e) { e.printStackTrace(); }
         */

        rowValsNewFiche.put("ID_SALARIE", rowSal.getID());
        rowValsSal.put("ID_FICHE_PAYE", rowValsNewFiche.getID());

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    // System.err.println("Update Salarie");
                    rowValsSal.update(rowSal.getID());

                    rowValsNewFiche.update();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        });
        // On recupere la liste des elements de la fiche de paye
        SQLSelect selAllElt = new SQLSelect(tableFiche.getBase());
        selAllElt.addSelect(tableFicheElt.getField("ID"));
        selAllElt.addSelect(tableFicheElt.getField("POSITION"));
        selAllElt.setWhere(tableFicheElt.getField("ID_FICHE_PAYE"), "=", oldID);
        // selAllElt.setArchivedPolicy(SQLSelect.BOTH);
        selAllElt.setDistinct(true);
        selAllElt.addRawOrder("\"FICHE_PAYE_ELEMENT\".\"POSITION\"");
        String req = selAllElt.asString();
        // System.err.println(req);
        Object[] ob = ((ArrayList) base.getDataSource().execute(req, new ArrayListHandler())).toArray();

        // System.err.println("Copie de " + ob.length + " éléments");
        for (int i = 0; i < ob.length; i++) {
            Object[] tmp = (Object[]) ob[i];

            SQLRow rowTmpElt = tableFicheElt.getRow(Integer.parseInt(tmp[0].toString()));

            String source = rowTmpElt.getString("SOURCE");
            int idSource = rowTmpElt.getInt("IDSOURCE");
            int pos = rowTmpElt.getInt("POSITION");

            SQLRowValues rowValsTmp = new SQLRowValues(tableFicheElt);
            rowValsTmp.put("SOURCE", source);
            rowValsTmp.put("IDSOURCE", idSource);
            rowValsTmp.put("POSITION", pos);
            rowValsTmp.put("ID_FICHE_PAYE", rowValsNewFiche.getID());

            try {
                rowValsTmp.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // on effectue le cumul
        // System.err.println("Calcul des cumuls");
        int idCumuls = rowSal.getInt("ID_CUMULS_PAYE");
        SQLRow rowCumuls = tableCumuls.getRow(idCumuls);

        float salBrut = rowCumuls.getFloat("SAL_BRUT_C") + rowFiche.getFloat("SAL_BRUT");
        float cgs = rowCumuls.getFloat("CSG_C") + rowFiche.getFloat("CSG");
        float cotSal = rowCumuls.getFloat("COT_SAL_C") + rowFiche.getFloat("COT_SAL");
        float cotPat = rowCumuls.getFloat("COT_PAT_C") + rowFiche.getFloat("COT_PAT");
        float netImp = rowCumuls.getFloat("NET_IMP_C") + rowFiche.getFloat("NET_IMP");
        float netAPayer = rowCumuls.getFloat("NET_A_PAYER_C") + rowFiche.getFloat("NET_A_PAYER") + rowFiche.getFloat("ACOMPTE");

        SQLRowValues rowValsCumul = new SQLRowValues(tableCumuls);
        rowValsCumul.put("SAL_BRUT_C", new Float(salBrut));
        rowValsCumul.put("COT_SAL_C", new Float(cotSal));
        rowValsCumul.put("COT_PAT_C", new Float(cotPat));
        rowValsCumul.put("NET_IMP_C", new Float(netImp));
        rowValsCumul.put("NET_A_PAYER_C", new Float(netAPayer));
        rowValsCumul.put("CSG_C", new Float(cgs));

        try {
            SQLRow r = rowValsCumul.insert();
            rowValsCumul.put("ID", r.getID());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // System.err.println("Mis a jour de la fiche de paye");
        rowValsSal.put("ID_CUMULS_PAYE", rowValsCumul.getID());

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    rowValsSal.update(rowFiche.getInt("ID_SALARIE"));
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        });
        // Calcul des congés
        final SQLRow rowCumulConge = tableCumulConge.getRow(rowSal.getInt("ID_CUMULS_CONGES"));
        final SQLRow rowVarSal = tableVariableSal.getRow(rowSal.getInt("ID_VARIABLE_SALARIE"));
        float congeCumule = rowCumulConge.getFloat("ACQUIS");
        float congeRestant = rowCumulConge.getFloat("RESTANT");

        float prisPeriode = rowVarSal.getFloat("CONGES_PRIS");
        congeRestant -= prisPeriode;

        SQLTable tableInfosSalPaye = base.getTable("INFOS_SALARIE_PAYE");
        final SQLRow rowSalInfosPaye = tableInfosSalPaye.getRow(rowSal.getInt("ID_INFOS_SALARIE_PAYE"));

        congeCumule += rowFiche.getFloat("CONGES_ACQUIS");

        final SQLRowValues rowValsCumulsConges = new SQLRowValues(tableCumulConge);

        // Si on passe le 1 juin
        // FIXME est ce que les conges nonpris sont à indemniser
        /*
         * if (mois == 7) { congeRestant = congeCumule; congeCumule = 0; }
         */

        rowValsCumulsConges.put("ACQUIS", new Float(congeCumule));
        rowValsCumulsConges.put("RESTANT", new Float(congeRestant));

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    rowValsCumulsConges.update(rowCumulConge.getID());
                    rowValsNewFiche.put("CONGES_ACQUIS", new Float(rowSalInfosPaye.getFloat("CONGES_PAYES")));
                    rowValsNewFiche.update();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
        final SQLRowValues rowValsOldFiche = new SQLRowValues(tableFiche);
        rowValsOldFiche.put("VALIDE", Boolean.TRUE);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    rowValsOldFiche.update(rowFiche.getID());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
        // Remise à 0 des variables sal
        final SQLRowValues rowVals = new SQLRowValues(tableVariableSal);

        for (Iterator i = tableVariableSal.getContentFields().iterator(); i.hasNext();) {

            String field = i.next().toString().trim();
            field = field.substring(field.indexOf('.') + 1, field.length() - 1);
            rowVals.put(field, new Float(0));
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    rowVals.update(rowVarSal.getID());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void clotureMensuelle(int idMois, int annee, int idSal) {

        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLRowValues rowValsSal = new SQLRowValues(base.getTable("SALARIE"));
        SQLRow rowSal = base.getTable("SALARIE").getRow(idSal);
        int ancMois = rowSal.getInt("DERNIER_MOIS");
        int ancAnnee = rowSal.getInt("DERNIERE_ANNEE");

        // on verifie que le mois et l'année sont > aux anciennes valeurs
        if (ancAnnee < annee && ancMois < idMois) {

            rowValsSal.put("DERNIER_MOIS", idMois);
            rowValsSal.put("DERNIERE_ANNEE", annee);

            try {
                rowValsSal.update(idSal);
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        /*
         * int mois = ancMois - 2; mois = (mois + 1) % 12; mois += 2; int annee = ancAnnee; if (mois
         * == 2) { annee++; } rowValsNewFiche.put("ID_MOIS", mois); rowValsNewFiche.put("ANNEE",
         * annee); rowValsNewFiche.put("ID_PROFIL_PAYE", rowFiche.getInt("ID_PROFIL_PAYE"));
         * 
         * try { rowValsNewFiche.update(); } catch (SQLException e) { e.printStackTrace(); }
         */
    }

    // stocke les éléments validés (cumuls congés, paye, ...)
    private static void stockValidValues(final int id) {

        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLTable tableSal = base.getTable("SALARIE");
        SQLTable tableCumulConge = base.getTable("CUMULS_CONGES");
        SQLTable tableVariableSal = base.getTable("VARIABLE_SALARIE");
        SQLTable tableReglPaye = base.getTable("REGLEMENT_PAYE");
        SQLTable tableCumuls = base.getTable("CUMULS_PAYE");
        SQLTable tableFiche = base.getTable("FICHE_PAYE");
        SQLTable tableContrat = base.getTable("CONTRAT_SALARIE");
        SQLTable tableInfos = base.getTable("INFOS_SALARIE_PAYE");

        SQLRow rowFiche = tableFiche.getRow(id);
        SQLRowValues rowValsFiche = new SQLRowValues(tableFiche);

        SQLRow rowSal = tableSal.getRow(rowFiche.getInt("ID_SALARIE"));

        // On stocke les cumuls des conges dans la fiche
        SQLRowValues rowValsCumulsCongesFiche = new SQLRowValues(tableCumulConge);
        rowValsCumulsCongesFiche.loadAllSafe(tableCumulConge.getRow(rowSal.getInt("ID_CUMULS_CONGES")));

        // On stocke les cumuls de paye dans la fiche
        SQLRowValues rowValsCumulsPayeFiche = new SQLRowValues(tableCumuls);
        rowValsCumulsPayeFiche.loadAllSafe(tableCumuls.getRow(rowSal.getInt("ID_CUMULS_PAYE")));

        // On stocke les cumuls de paye dans la fiche
        SQLRowValues rowValsVarSalFiche = new SQLRowValues(tableVariableSal);
        rowValsVarSalFiche.loadAllSafe(tableVariableSal.getRow(rowSal.getInt("ID_VARIABLE_SALARIE")));

        // On stocke le mode de reglement
        SQLRowValues rowValsReglPaye = new SQLRowValues(tableReglPaye);
        rowValsReglPaye.loadAllSafe(tableReglPaye.getRow(rowSal.getInt("ID_REGLEMENT_PAYE")));

        try {
            SQLRow rVarSal = rowValsVarSalFiche.insert();
            SQLRow rCumulsCong = rowValsCumulsCongesFiche.insert();
            SQLRow rCumulPaye = rowValsCumulsPayeFiche.insert();
            SQLRow rReglPaye = rowValsReglPaye.insert();
            SQLRow rInfosSalPaye = tableInfos.getRow(rowSal.getInt("ID_INFOS_SALARIE_PAYE"));
            SQLRow rContrat = tableContrat.getRow(rInfosSalPaye.getInt("ID_CONTRAT_SALARIE"));

            rowValsFiche.put("ID_VARIABLE_SALARIE", rVarSal.getID());
            rowValsFiche.put("ID_CUMULS_CONGES", rCumulsCong.getID());
            rowValsFiche.put("ID_CUMULS_PAYE", rCumulPaye.getID());
            rowValsFiche.put("ID_REGLEMENT_PAYE", rReglPaye.getID());
            rowValsFiche.put("NATURE_EMPLOI", rContrat.getString("NATURE"));
            rowValsFiche.put("ID_IDCC", rInfosSalPaye.getInt("ID_IDCC"));

            rowValsFiche.update(id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // FIXME retirer les rubriques non imprime ou pas dans de la periode de la fiche validee ??
    private static void validElements(int id) {

        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        System.err.println("Validation des éléments de la fiche.");
        String trueString = "1";
        if (Configuration.getInstance().getBase().getServer().getSQLSystem() == SQLSystem.POSTGRESQL) {
            trueString = "true";
        }
        String req = "UPDATE \"FICHE_PAYE_ELEMENT\" SET \"VALIDE\" = " + trueString + " WHERE \"FICHE_PAYE_ELEMENT\".\"ID_FICHE_PAYE\" = " + id;

        base.getDataSource().execute(req);

        System.err.println("Validation terminée.");
    }

    private static boolean checkDateValid(int idFiche) {

        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();

        SQLRow rowFiche = base.getTable("FICHE_PAYE").getRow(idFiche);
        SQLRow rowSal = base.getTable("SALARIE").getRow(rowFiche.getInt("ID_SALARIE"));
        int moisClot = rowSal.getInt("DERNIER_MOIS");
        int anneeClot = rowSal.getInt("DERNIERE_ANNEE");

        int mois = rowFiche.getInt("ID_MOIS");
        int annee = rowFiche.getInt("ANNEE");
        return ((anneeClot == 0) ? true : annee > anneeClot) || ((moisClot == 0 || moisClot == 13) ? true : mois > moisClot);
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".payslip";
    }
}
