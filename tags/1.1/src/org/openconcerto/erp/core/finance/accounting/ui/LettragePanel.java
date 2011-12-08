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
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.DeviseNiceTableCellRenderer;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.core.finance.accounting.model.LettrageModel;
import org.openconcerto.erp.model.ISQLCompteSelector;
import org.openconcerto.erp.rights.ComptaUserRight;
import org.openconcerto.erp.utils.UpperCaseFormatFilter;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.warning.JLabelWarning;
import org.openconcerto.utils.text.DocumentFilterList;
import org.openconcerto.utils.text.DocumentFilterList.FilterType;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.MaskFormatter;

public class LettragePanel extends JPanel {

    private ListPanelEcritures ecriturePanel;
    private JTextField codeLettrage;
    private ISQLCompteSelector selCompte;
    private JCheckBox boxValidEcriture, boxAddSousCompte;
    private JPanel warningPanel, warningSolde;

    private final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
    private final SQLTable tableEcr = this.base.getTable("ECRITURE");
    private final SQLTable tableComptePCE = this.base.getTable("COMPTE_PCE");

    private final static int allEcriture = 0;
    private final static int ecritureLettree = 1;
    private final static int ecritureNonLettree = 2;

    private int modeSelect;
    private LettrageModel model;
    private JButton buttonLettrer;
    private JDate dateDeb, dateFin, dateLettrage;

    public LettragePanel() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();

        this.modeSelect = allEcriture;

        // Selection du compte à lettrer
        JLabel labelPointageCompte = new JLabel("Lettrage du compte");
        labelPointageCompte.setHorizontalAlignment(SwingConstants.RIGHT);

        this.add(labelPointageCompte, c);
        this.selCompte = new ISQLCompteSelector();
        this.selCompte.init();
        this.selCompte.setValue(ComptePCESQLElement.getId("5"));

        c.gridx++;
        c.weightx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(this.selCompte, c);

        c.gridwidth = 1;

        // Gestion du lettrage
        c.insets = new Insets(2, 2, 1, 2);
        TitledSeparator sepGestionLettrage = new TitledSeparator("Gestion du lettrage");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(sepGestionLettrage, c);

        // Code de lettrage
        JLabel labelCode = new JLabel("Code lettrage");
        labelCode.setHorizontalAlignment(SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        this.add(labelCode, c);

        this.codeLettrage = new JTextField(10);
        DocumentFilterList.add((AbstractDocument) this.codeLettrage.getDocument(), new UpperCaseFormatFilter(), FilterType.SIMPLE_FILTER);
        c.gridx++;
        c.weightx = 1;
        this.add(this.codeLettrage, c);
        this.codeLettrage.setText(NumerotationAutoSQLElement.getNextCodeLettrage());

        // Warning si aucun code rentré
        createPanelWarning();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0;
        c.gridx++;
        this.add(this.warningPanel, c);

        // Date de lettrage
        JLabel labelDate = new JLabel("Date");
        labelDate.setHorizontalAlignment(SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        this.add(labelDate, c);
        this.dateLettrage = new JDate(true);
        c.gridx++;
        this.add(this.dateLettrage, c);

        // Warning si solde non nul
        c.gridx++;
        createPanelWarningSolde();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0;
        this.add(this.warningSolde, c);

        c.gridwidth = 1;

        TitledSeparator sepPeriode = new TitledSeparator("Filtre ");
        c.gridy++;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(sepPeriode, c);

        JPanel panelSelectEcritures = createPanelSelectionEcritures();
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0;

        JLabel labelEcr = new JLabel("Ecritures");
        labelEcr.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(labelEcr, c);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        this.add(panelSelectEcritures, c);

        JPanel panelPeriode = new JPanel();
        // Date de début
        this.dateDeb = new JDate();
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        JLabel labelPerio = new JLabel("Période du ");
        labelPerio.setHorizontalAlignment(SwingConstants.RIGHT);
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(labelPerio, c);

        panelPeriode.add(this.dateDeb);
        this.dateDeb.addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                changeListRequest();
            }
        });

        // Date de fin
        this.dateFin = new JDate(true);
        panelPeriode.add(new JLabel("au"));
        this.dateFin.addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                changeListRequest();
            }
        });

        panelPeriode.add(this.dateFin);
        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(panelPeriode, c);

        c.gridx = 0;
        c.gridy++;
        this.boxAddSousCompte = new JCheckBox("Ajouter les sous comptes");
        this.boxAddSousCompte.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                changeListRequest();
            }
        });

        this.add(this.boxAddSousCompte, c);

        TitledSeparator sepEcriture = new TitledSeparator("Ecritures ");
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(sepEcriture, c);

        // Liste des ecritures
        final EcritureSQLElement ecritureElem = Configuration.getInstance().getDirectory().getElement(EcritureSQLElement.class);
        this.ecriturePanel = new ListPanelEcritures(ecritureElem, new IListe(ecritureElem.createLettrageTableSource()));
        c.gridx = 0;
        c.gridy++;
        c.weighty = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.ecriturePanel.getListe().setPreferredSize(new Dimension(this.ecriturePanel.getListe().getPreferredSize().width, 200));
        this.add(this.ecriturePanel, c);

        // JTable Totaux
        c.gridy++;
        c.gridx = 0;
        c.weighty = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 3;
        c.gridheight = 3;
        this.model = new LettrageModel(this.selCompte.getSelectedId());
        JTable table = new JTable(this.model);

        // AlternateTableCellRenderer.setAllColumns(table);
        for (int i = 0; i < table.getColumnCount(); i++) {
            // if (table.getColumnClass(i) == Long.class || table.getColumnClass(i) ==
            // BigInteger.class) {
            table.getColumnModel().getColumn(i).setCellRenderer(new DeviseNiceTableCellRenderer());
            // }
        }
        JScrollPane sPane = new JScrollPane(table);

        // TODO Gerer la taille des colonnes
        Dimension d = new Dimension(table.getPreferredSize().width, table.getPreferredSize().height + table.getTableHeader().getPreferredSize().height + 4);
        sPane.setPreferredSize(d);
        this.add(sPane, c);

        // Legende
        c.gridx = 4;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        this.add(createPanelLegende(), c);

        c.gridheight = 1;
        final JButton buttonDelettrer = new JButton("Délettrer");
        this.buttonLettrer = new JButton("Lettrer");
        // Validation des ecritures pointées
        this.boxValidEcriture = new JCheckBox("Valider les écritures lettrées");
        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(this.boxValidEcriture, c);

        JPanel panelButton = new JPanel();

        // Boutton lettrer

        panelButton.add(this.buttonLettrer, c);

        // Boutton Delettrer

        panelButton.add(buttonDelettrer, c);

        c.gridy++;
        c.gridx = 5;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        this.add(panelButton, c);

        c.gridy++;
        c.anchor = GridBagConstraints.SOUTHEAST;
        JButton buttonClose = new JButton("Fermer");
        buttonClose.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ((Window) SwingUtilities.getRoot((Component) e.getSource())).dispose();

            }
        });
        this.add(buttonClose, c);
        this.buttonLettrer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                int[] rowIndex = LettragePanel.this.ecriturePanel.getListe().getJTable().getSelectedRows();

                // System.err.println("Action lettrage sur " + i);
                actionLettrage(rowIndex);

            }
        });

        buttonDelettrer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                int[] rowIndex = LettragePanel.this.ecriturePanel.getListe().getJTable().getSelectedRows();
                actionDelettrage(rowIndex);
            }
        });

        // Changement de compte
        this.selCompte.addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {

                changeListRequest();
            };
        });

        // Action Souris sur la IListe

        addActionMenuDroit();
        this.ecriturePanel.getListe().getJTable().addMouseListener(new MouseAdapter() {

            public void mouseReleased(MouseEvent e) {
                System.err.println("Mouse released");
                int[] selectedRows = LettragePanel.this.ecriturePanel.getListe().getJTable().getSelectedRows();
                int[] idRows = new int[selectedRows.length];
                for (int i = 0; i < idRows.length; i++) {
                    idRows[i] = LettragePanel.this.ecriturePanel.getListe().idFromIndex(selectedRows[i]);
                }

                LettragePanel.this.model.updateSelection(idRows);
                LettragePanel.this.warningSolde.setVisible(LettragePanel.this.model.getSoldeSelection() != 0);
                buttonDelettrer.setEnabled(LettragePanel.this.model.getSoldeSelection() == 0);
                LettragePanel.this.buttonLettrer.setEnabled(LettragePanel.this.model.getSoldeSelection() == 0);
            }
        });

        // action sur la IListe
        this.ecriturePanel.getListe().getJTable().addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {

                System.err.println("Key released");
                int[] selectedRows = LettragePanel.this.ecriturePanel.getListe().getJTable().getSelectedRows();
                int[] idRows = new int[selectedRows.length];
                for (int i = 0; i < idRows.length; i++) {
                    idRows[i] = LettragePanel.this.ecriturePanel.getListe().idFromIndex(selectedRows[i]);
                }

                LettragePanel.this.model.updateSelection(idRows);
                LettragePanel.this.warningPanel.setVisible((LettragePanel.this.codeLettrage.getText().trim().length() == 0));
                LettragePanel.this.warningSolde.setVisible(LettragePanel.this.model.getSoldeSelection() != 0);
            }
        });

        // Gestion du code
        this.codeLettrage.getDocument().addDocumentListener(new SimpleDocumentListener() {

            @Override
            public void update(DocumentEvent e) {
                // TODO Auto-generated method stub
                LettragePanel.this.warningPanel.setVisible((LettragePanel.this.codeLettrage.getText().trim().length() == 0));
                LettragePanel.this.buttonLettrer.setEnabled((LettragePanel.this.codeLettrage.getText().trim().length() != 0));
            }

        });

        changeListRequest();
        this.warningPanel.setVisible((this.codeLettrage.getText().trim().length() == 0));
        this.buttonLettrer.setEnabled((this.codeLettrage.getText().trim().length() != 0));
    }

    /* Menu clic Droit */
    private void addActionMenuDroit() {
        // JPopupMenu menu = new JPopupMenu();

        this.ecriturePanel.getListe().addRowAction(new AbstractAction("Voir la source") {
            public void actionPerformed(ActionEvent e) {

                SQLRow rowEcr = LettragePanel.this.ecriturePanel.getListe().getSelectedRow();
                MouvementSQLElement.showSource(rowEcr.getInt("ID_MOUVEMENT"));
            }
        });

        // if (this.codeLettrage.getText().trim().length() != 0) {
        final AbstractAction abstractAction = new AbstractAction("Lettrer") {
            public void actionPerformed(ActionEvent e) {

                int[] rowIndex = LettragePanel.this.ecriturePanel.getListe().getJTable().getSelectedRows();
                actionLettrage(rowIndex);
            }
        };
        this.ecriturePanel.getListe().addRowAction(abstractAction);
        // }
        this.codeLettrage.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                abstractAction.setEnabled(LettragePanel.this.codeLettrage.getText().trim().length() > 0);
            }
        });

        this.ecriturePanel.getListe().addRowAction(new AbstractAction("Délettrer") {
            public void actionPerformed(ActionEvent e) {

                int[] rowIndex = LettragePanel.this.ecriturePanel.getListe().getJTable().getSelectedRows();
                actionDelettrage(rowIndex);
            }
        });

        // menu.show(mE.getComponent(), mE.getPoint().x, mE.getPoint().y);
    }

    /* Panel Warning no numero releve */
    private void createPanelWarning() {

        this.warningPanel = new JPanel();
        this.warningPanel.setLayout(new GridBagLayout());
        // this.warningPanel.setBorder(BorderFactory.createTitledBorder("Warning"));

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;

        final JLabel warningNoCodeImg = new JLabelWarning();
        warningNoCodeImg.setHorizontalAlignment(SwingConstants.RIGHT);
        this.warningPanel.add(warningNoCodeImg, c);
        final JLabel warningNoCodeText = new JLabel("Impossible de lettrer tant que le code de lettrage n'est pas saisi!");
        c.gridx++;
        this.warningPanel.add(warningNoCodeText, c);
    }

    /* Panel Warning solde invalide */
    private void createPanelWarningSolde() {

        this.warningSolde = new JPanel();
        this.warningSolde.setLayout(new GridBagLayout());
        // this.warningPanel.setBorder(BorderFactory.createTitledBorder("Warning"));

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;

        final JLabel warningNoCodeImg = new JLabelWarning();
        warningNoCodeImg.setHorizontalAlignment(SwingConstants.RIGHT);
        this.warningSolde.add(warningNoCodeImg, c);
        final JLabel warningNoCodeText = new JLabel("Impossible de lettrer tant que le solde sélectionné n'est pas nul!");
        c.gridx++;
        this.warningSolde.add(warningNoCodeText, c);
    }

    // Lettre la ligne passée en parametre
    private void actionLettrage(int[] rowIndex) {
        String codeLettre = this.codeLettrage.getText().trim();

        List<SQLRow> rowsSelected = new ArrayList<SQLRow>(rowIndex.length);

        long solde = 0;
        for (int i = 0; i < rowIndex.length; i++) {
            int id = this.ecriturePanel.getListe().idFromIndex(rowIndex[i]);
            SQLRow row = this.tableEcr.getRow(id);
            rowsSelected.add(row);

            solde += ((Long) row.getObject("DEBIT")).longValue();
            solde -= ((Long) row.getObject("CREDIT")).longValue();
        }

        if (solde == 0) {

            for (SQLRow row2 : rowsSelected) {

                SQLRowValues rowVals = new SQLRowValues(this.tableEcr);

                // Lettrage
                // On lettre ou relettre la ligne avec le code saisi
                if (codeLettre.length() > 0) {

                    // Si la ligne est en brouillard on valide le mouvement associé
                    if (this.boxValidEcriture.isSelected() && (!row2.getBoolean("VALIDE"))) {
                        EcritureSQLElement.validationEcritures(row2.getInt("ID_MOUVEMENT"));
                    }

                    rowVals.put("LETTRAGE", codeLettre);
                    rowVals.put("DATE_LETTRAGE", this.dateLettrage.getDate());
                    try {
                        rowVals.update(row2.getID());
                    } catch (SQLException e1) {

                        e1.printStackTrace();
                    }
                }
            }
            // Mise à jour du code de lettrage
            SQLElement elt = Configuration.getInstance().getDirectory().getElement("NUMEROTATION_AUTO");
            SQLRowValues rowVals = elt.getTable().getRow(2).createEmptyUpdateRow();
            rowVals.put("CODE_LETTRAGE", codeLettre);
            try {
                rowVals.update();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            this.codeLettrage.setText(NumerotationAutoSQLElement.getNextCodeLettrage());

            this.model.updateTotauxCompte();
        }
    }

    protected MaskFormatter createFormatter() {
        MaskFormatter formatter = null;
        try {
            formatter = new MaskFormatter("UUU");
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return formatter;
    }

    // Pointe la ligne passée en parametre
    private void actionDelettrage(int[] rowIndex) {

        List<SQLRow> rowsSelected = new ArrayList<SQLRow>(rowIndex.length);

        long solde = 0;
        for (int i = 0; i < rowIndex.length; i++) {
            int id = this.ecriturePanel.getListe().idFromIndex(rowIndex[i]);
            SQLRow row = this.tableEcr.getRow(id);
            rowsSelected.add(row);

            solde += ((Long) row.getObject("DEBIT")).longValue();
            solde -= ((Long) row.getObject("CREDIT")).longValue();
        }

        if (solde == 0) {
            for (SQLRow row : rowsSelected) {

                SQLRowValues rowVals = new SQLRowValues(this.tableEcr);

                // Dépointage
                if (row.getString("LETTRAGE").trim().length() != 0) {

                    rowVals.put("LETTRAGE", "");
                    rowVals.put("DATE_LETTRAGE", null);
                    try {
                        rowVals.update(row.getID());
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        this.model.updateTotauxCompte();
    }

    /*
     * MaJ de la requete pour remplir la IListe en fonction du compte sélectionner et du mode de
     * sélection
     */
    private void changeListRequest() {
        Object idCpt = this.selCompte.getSelectedId();

        SQLRow row = this.tableComptePCE.getRow(Integer.valueOf(idCpt.toString()));

        // filtre de selection
        Where w = new Where(this.tableEcr.getField("ID_COMPTE_PCE"), "=", this.tableComptePCE.getKey());

        if (!UserManager.getInstance().getCurrentUser().getRights().haveRight(ComptaUserRight.ACCES_NOT_RESCTRICTED_TO_411)) {
            // TODO Show Restricted acces in UI
            w = w.and(new Where(this.tableEcr.getField("COMPTE_NUMERO"), "LIKE", "411%"));
        }

        if (row != null) {
            String num = row.getString("NUMERO");
            Where w2;
            if (this.boxAddSousCompte.isSelected()) {
                w2 = new Where(this.tableComptePCE.getField("NUMERO"), "LIKE", num + "%");
            } else {
                w2 = new Where(this.tableComptePCE.getField("NUMERO"), "=", num);
            }
            w = w.and(w2);
        } else {
            w = w.and(new Where(this.tableComptePCE.getKey(), "=", idCpt));
        }

        // final Calendar cal = Calendar.getInstance();
        // cal.setTimeInMillis(this.rangeSlide.getValue(0));
        //
        // Date dInf = cal.getTime();
        // cal.setTimeInMillis(this.rangeSlide.getValue(1));
        // Date dSup = cal.getTime();

        // w = w.and(new Where(this.tableEcr.getField("DATE"), dInf, dSup));

        Date d1 = this.dateDeb.getValue();
        Date d2 = this.dateFin.getValue();

        if (d1 == null && d2 != null) {
            w = w.and(new Where(this.tableEcr.getField("DATE"), "<=", d2));
        } else {
            if (d1 != null && d2 == null) {
                w = w.and(new Where(this.tableEcr.getField("DATE"), ">=", d1));
            } else {
                if (d1 != null && d2 != null) {
                    w = w.and(new Where(this.tableEcr.getField("DATE"), d1, d2));
                }
            }
        }

        if (this.modeSelect == ecritureLettree) {
            w = w.and(new Where(this.tableEcr.getField("LETTRAGE"), "!=", ""));
        } else {
            if (this.modeSelect == ecritureNonLettree) {

                Where wLettre = new Where(this.tableEcr.getField("LETTRAGE"), "=", "");
                String s = null;
                wLettre = wLettre.or(new Where(this.tableEcr.getField("LETTRAGE"), "=", s));
                w = w.and(wLettre);
            }
        }

        this.ecriturePanel.getListe().getRequest().setWhere(w);
        this.ecriturePanel.getListe().setSQLEditable(false);

        this.model.setIdCompte(Integer.parseInt(idCpt.toString()));
    }

    /*
     * Panel de sélection du mode d'affichage des ecritures
     */
    private JPanel createPanelSelectionEcritures() {

        JPanel panelSelectEcritures = new JPanel();

        GridBagConstraints cPanel = new GridBagConstraints();
        cPanel.anchor = GridBagConstraints.NORTHWEST;
        cPanel.fill = GridBagConstraints.HORIZONTAL;
        cPanel.gridheight = 1;
        cPanel.gridwidth = 1;
        cPanel.gridx = 0;
        cPanel.gridy = 0;
        cPanel.weightx = 0;
        cPanel.weighty = 0;

        panelSelectEcritures.setLayout(new GridBagLayout());

        final JRadioButton buttonBoth = new JRadioButton("Toutes");
        panelSelectEcritures.add(buttonBoth, cPanel);
        cPanel.gridx++;
        final JRadioButton buttonNonLettre = new JRadioButton("Non lettrées");
        panelSelectEcritures.add(buttonNonLettre, cPanel);
        cPanel.gridx++;
        final JRadioButton buttonLettre = new JRadioButton("Lettrées");
        panelSelectEcritures.add(buttonLettre, cPanel);

        ButtonGroup group = new ButtonGroup();
        group.add(buttonBoth);
        group.add(buttonNonLettre);
        group.add(buttonLettre);
        buttonBoth.setSelected(true);

        buttonLettre.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (buttonLettre.isSelected()) {
                    LettragePanel.this.modeSelect = ecritureLettree;
                    changeListRequest();
                }
            }
        });

        buttonNonLettre.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (buttonNonLettre.isSelected()) {
                    LettragePanel.this.modeSelect = ecritureNonLettree;
                    changeListRequest();
                }
            }
        });

        buttonBoth.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (buttonBoth.isSelected()) {
                    LettragePanel.this.modeSelect = allEcriture;
                    changeListRequest();
                }
            }
        });

        return panelSelectEcritures;
    }

    /*
     * Creation du panel de la legende
     */
    private JPanel createPanelLegende() {
        JPanel panelLegende = new JPanel();

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.weightx = 0;
        c.weighty = 0;
        c.insets = new Insets(2, 0, 0, 0);

        GridBagConstraints cPanel = new GridBagConstraints();
        cPanel.anchor = GridBagConstraints.NORTHWEST;
        cPanel.fill = GridBagConstraints.HORIZONTAL;
        cPanel.gridheight = 1;
        cPanel.gridwidth = 1;
        cPanel.gridx = 0;
        cPanel.gridy = GridBagConstraints.RELATIVE;
        cPanel.weightx = 0;
        cPanel.weighty = 0;
        cPanel.insets = new Insets(0, 0, 0, 0);

        panelLegende.setLayout(new GridBagLayout());
        panelLegende.setBorder(BorderFactory.createTitledBorder("Légendes"));

        JPanel ecritureValidPanel = new JPanel();
        ecritureValidPanel.setLayout(new GridBagLayout());
        ecritureValidPanel.setBackground(Color.WHITE);
        ecritureValidPanel.add(new JLabel("Ecritures validées"), cPanel);
        panelLegende.add(ecritureValidPanel, c);

        JPanel ecritureNonValidPanel = new JPanel();
        ecritureNonValidPanel.setLayout(new GridBagLayout());
        ecritureNonValidPanel.setBackground(PointageRenderer.getCouleurEcritureNonValide());
        ecritureNonValidPanel.add(new JLabel("Ecritures non validées"), cPanel);
        panelLegende.add(ecritureNonValidPanel, c);

        JPanel ecritureNonValidTodayPanel = new JPanel();
        ecritureNonValidTodayPanel.setLayout(new GridBagLayout());
        ecritureNonValidTodayPanel.setBackground(PointageRenderer.getCouleurEcritureToDay());
        ecritureNonValidTodayPanel.add(new JLabel("Ecritures non validées du jour"), cPanel);
        panelLegende.add(ecritureNonValidTodayPanel, c);

        JPanel ecriturePointePanel = new JPanel();
        ecriturePointePanel.setLayout(new GridBagLayout());
        ecriturePointePanel.setBackground(PointageRenderer.getCouleurEcriturePointee());
        ecriturePointePanel.add(new JLabel("Ecritures lettrées"), cPanel);
        panelLegende.add(ecriturePointePanel, c);

        return panelLegende;
    }
}
