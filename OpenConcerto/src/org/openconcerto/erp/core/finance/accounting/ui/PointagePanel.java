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
import org.openconcerto.erp.core.common.ui.DeviseNiceTableCellRenderer;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.core.finance.accounting.model.PointageModel;
import org.openconcerto.erp.model.ISQLCompteSelector;
import org.openconcerto.erp.rights.ComptaUserRight;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.users.UserManager;

import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.warning.JLabelWarning;

import java.awt.Color;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class PointagePanel extends JPanel {

    private ListPanelEcritures ecriturePanel;
    private JTextField codePointage;
    private ISQLCompteSelector selCompte;
    private final JDate datePointee;
    private JCheckBox boxValidEcriture;
    private JPanel warningPanel;

    private final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
    private final SQLTable tableEcr = this.base.getTable("ECRITURE");
    private final SQLTable tableCpt = this.base.getTable("COMPTE_PCE");

    private final static int allEcriture = 0;
    private final static int ecriturePointee = 1;
    private final static int ecritureNotPointee = 2;

    private int modeSelect;
    private PointageModel model;
    private JButton buttonPointer;
    private JDate dateFin, dateDeb;

    public PointagePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        this.modeSelect = allEcriture;

        // Selection du compte à pointer
        // TODO Ajouter selection d'un Journal

        JLabel labelPointageCompte = new JLabel("Pointage du compte");
        labelPointageCompte.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(labelPointageCompte, c);

        this.selCompte = new ISQLCompteSelector();

        this.selCompte.init();
        new SwingWorker<Integer, Object>() {

            @Override
            protected Integer doInBackground() throws Exception {

                return ComptePCESQLElement.getId("5");
            }

            @Override
            protected void done() {
                try {
                    PointagePanel.this.selCompte.setValue(get());
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }.execute();

        // c.fill = GridBagConstraints.NONE;
        c.weightx = 1;
        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(this.selCompte, c);

        // Gestion du pointage
        c.insets = new Insets(2, 2, 1, 2);
        TitledSeparator sepGestionPointage = new TitledSeparator("Gestion du pointage");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy++;
        c.gridx = 0;
        this.add(sepGestionPointage, c);

        // Panel Selection du mode d'affichage des ecritures

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;

        // Numero de releve
        // c.anchor = GridBagConstraints.EAST;
        JLabel labelReleve = new JLabel("N° de relevé");
        labelReleve.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(labelReleve, c);

        this.codePointage = new JTextField(10);
        c.gridx++;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        this.add(this.codePointage, c);

        // Warning si aucun code rentré
        c.gridx++;
        c.fill = GridBagConstraints.HORIZONTAL;
        createPanelWarning();
        c.gridwidth = 1;
        c.weightx = 1;
        this.add(this.warningPanel, c);

        // Date de pointage
        // MAYBE si date invalide grisée le bouton pointer
        JLabel labelDate = new JLabel("Date de pointage");
        labelDate.setHorizontalAlignment(SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        this.add(labelDate, c);

        this.datePointee = new JDate(true);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 1;
        c.gridx++;
        c.gridwidth = 1;
        this.add(this.datePointee, c);

        TitledSeparator sepPeriode = new TitledSeparator("Filtre ");
        c.gridy++;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(sepPeriode, c);

        JPanel panelSelectEcritures = createPanelSelectionEcritures();
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        c.gridx = 0;
        final JLabel labelEcr = new JLabel("Ecritures");
        labelEcr.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(labelEcr, c);
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        this.add(panelSelectEcritures, c);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        JPanel panelPeriode = new JPanel();
        // Date de début
        this.dateDeb = new JDate();
        final JLabel periodLabel = new JLabel("Période du ");
        periodLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        c.gridwidth = 1;
        this.add(periodLabel, c);

        c.gridx++;

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

        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(panelPeriode, c);

        TitledSeparator sepEcriture = new TitledSeparator("Ecritures ");
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(sepEcriture, c);

        // Liste des ecritures
        this.ecriturePanel = new ListPanelEcritures();
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
        c.gridwidth = 4;
        c.gridheight = 3;
        this.model = new PointageModel(this.selCompte.getSelectedId());
        JTable table = new JTable(this.model);

        // AlternateTableCellRenderer.setAllColumns(table);
        final DeviseNiceTableCellRenderer cellRenderer = new DeviseNiceTableCellRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            // if (table.getColumnClass(i) == Long.class || table.getColumnClass(i) ==
            // BigInteger.class) {

            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
            // }else{
            //
            // }
        }
        JScrollPane sPane = new JScrollPane(table);

        // TODO Gerer la taille des colonnes
        Dimension d = new Dimension(table.getPreferredSize().width, table.getPreferredSize().height + table.getTableHeader().getPreferredSize().height + 4);
        sPane.setPreferredSize(d);
        this.add(sPane, c);

        // Legende
        c.gridx += 4;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        this.add(createPanelLegende(), c);

        // Validation des ecritures pointées
        this.boxValidEcriture = new JCheckBox("Valider les écritures pointées");
        c.gridx++;
        c.gridheight = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(this.boxValidEcriture, c);

        // Bouton Pointer
        c.anchor = GridBagConstraints.SOUTHEAST;
        this.buttonPointer = new JButton("Pointer");
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.gridx = 5;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;

        this.add(this.buttonPointer, c);

        // Bouton Depointer
        JButton buttonDepointer = new JButton("Dépointer");
        c.gridx++;
        c.weightx = 0;
        this.add(buttonDepointer, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 0;
        c.gridx = 5;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        JButton buttonClose = new JButton("Fermer");
        buttonClose.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ((Window) SwingUtilities.getRoot(PointagePanel.this)).dispose();
            }
        });
        this.add(buttonClose, c);
        this.buttonPointer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                int[] rowIndex = PointagePanel.this.ecriturePanel.getListe().getJTable().getSelectedRows();

                for (int i = 0; i < rowIndex.length; i++) {
                    System.err.println("Action pointage sur " + i);
                    actionPointage(rowIndex[i]);
                }
            }
        });

        buttonDepointer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                int[] rowIndex = PointagePanel.this.ecriturePanel.getListe().getJTable().getSelectedRows();

                for (int i = 0; i < rowIndex.length; i++) {
                    System.err.println("Action depointage sur " + i);
                    actionDepointage(rowIndex[i]);
                }
            }
        });

        // Changement de compte
        this.selCompte.addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {

                changeListRequest();
            };
        });

        // Action Souris sur la IListe
        this.ecriturePanel.getListe().getJTable().addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {

                System.err.println("Mouse Pressed");
                if ((e.getClickCount() == 2) && (e.getButton() == 1)) {

                    System.err.println("Double clicked");
                    int rowIndex = PointagePanel.this.ecriturePanel.getListe().getJTable().rowAtPoint(e.getPoint());
                    int id = PointagePanel.this.ecriturePanel.getListe().idFromIndex(rowIndex);

                    SQLRow row = PointagePanel.this.tableEcr.getRow(id);
                    if (row.getString("POINTEE").trim().length() == 0) {
                        actionPointage(rowIndex);
                    } else {
                        actionDepointage(rowIndex);
                    }
                }

                if (e.getButton() == 3) {

                    System.err.println("Right click");
                    actionMenuDroit(e);
                }
            }

            public void mouseReleased(MouseEvent e) {
                System.err.println("Mouse released");
                int[] selectedRows = PointagePanel.this.ecriturePanel.getListe().getJTable().getSelectedRows();
                int[] idRows = new int[selectedRows.length];
                for (int i = 0; i < idRows.length; i++) {
                    idRows[i] = PointagePanel.this.ecriturePanel.getListe().idFromIndex(selectedRows[i]);
                }

                PointagePanel.this.model.updateSelection(idRows);
            }
        });

        // action sur la IListe
        this.ecriturePanel.getListe().getJTable().addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {

                System.err.println("Key released");
                int[] selectedRows = PointagePanel.this.ecriturePanel.getListe().getJTable().getSelectedRows();
                int[] idRows = new int[selectedRows.length];
                for (int i = 0; i < idRows.length; i++) {
                    idRows[i] = PointagePanel.this.ecriturePanel.getListe().idFromIndex(selectedRows[i]);
                }

                PointagePanel.this.model.updateSelection(idRows);
            }
        });

        // Gestion du code de releve
        this.codePointage.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {

                PointagePanel.this.warningPanel.setVisible((PointagePanel.this.codePointage.getText().trim().length() == 0));
                PointagePanel.this.buttonPointer.setEnabled((PointagePanel.this.codePointage.getText().trim().length() != 0));
            }

            public void removeUpdate(DocumentEvent e) {

                PointagePanel.this.warningPanel.setVisible((PointagePanel.this.codePointage.getText().trim().length() == 0));
                PointagePanel.this.buttonPointer.setEnabled((PointagePanel.this.codePointage.getText().trim().length() != 0));
            }

            public void insertUpdate(DocumentEvent e) {

                PointagePanel.this.warningPanel.setVisible((PointagePanel.this.codePointage.getText().trim().length() == 0));
                PointagePanel.this.buttonPointer.setEnabled((PointagePanel.this.codePointage.getText().trim().length() != 0));
            }
        });

        changeListRequest();
        this.warningPanel.setVisible((this.codePointage.getText().trim().length() == 0));
        this.buttonPointer.setEnabled((this.codePointage.getText().trim().length() != 0));
    }

    /* Menu clic Droit */
    private void actionMenuDroit(final MouseEvent mE) {
        JPopupMenu menu = new JPopupMenu();

        menu.add(new AbstractAction("Voir la source") {
            public void actionPerformed(ActionEvent e) {

                int rowIndex = PointagePanel.this.ecriturePanel.getListe().getJTable().rowAtPoint(mE.getPoint());
                int id = PointagePanel.this.ecriturePanel.getListe().idFromIndex(rowIndex);

                SQLTable ecriture = PointagePanel.this.base.getTable("ECRITURE");
                SQLRow rowEcr = ecriture.getRow(id);

                MouvementSQLElement.showSource(rowEcr.getInt("ID_MOUVEMENT"));
            }
        });

        if (this.codePointage.getText().trim().length() != 0) {
            menu.add(new AbstractAction("Pointer") {
                public void actionPerformed(ActionEvent e) {

                    int rowIndex = PointagePanel.this.ecriturePanel.getListe().getJTable().rowAtPoint(mE.getPoint());
                    actionPointage(rowIndex);
                }
            });
        }

        menu.add(new AbstractAction("Dépointer") {
            public void actionPerformed(ActionEvent e) {

                int rowIndex = PointagePanel.this.ecriturePanel.getListe().getJTable().rowAtPoint(mE.getPoint());
                actionDepointage(rowIndex);
            }
        });

        menu.show(mE.getComponent(), mE.getPoint().x, mE.getPoint().y);
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
        // warningNoCodeImg.setHorizontalAlignment(SwingConstants.RIGHT);
        this.warningPanel.add(warningNoCodeImg, c);
        final JLabel warningNoCodeText = new JLabel("Impossible de pointer tant que le numéro de relevé n'est pas saisi!");
        c.gridx++;
        this.warningPanel.add(warningNoCodeText, c);
    }

    // Pointe la ligne passée en parametre
    private void actionPointage(int rowIndex) {
        String codePoint = this.codePointage.getText().trim();

        int id = this.ecriturePanel.getListe().idFromIndex(rowIndex);

        SQLRow row = this.tableEcr.getRow(id);
        SQLRowValues rowVals = new SQLRowValues(this.tableEcr);

        // Pointage
        // On pointe ou repointe la ligne avec la date et le numero de releve saisis
        if ((!this.datePointee.isEmpty()) && (codePoint.length() > 0)) {

            // Si la ligne est en brouillard on valide le mouvement associé
            if (this.boxValidEcriture.isSelected() && (!row.getBoolean("VALIDE"))) {
                EcritureSQLElement.validationEcritures(row.getInt("ID_MOUVEMENT"));
            }

            rowVals.put("POINTEE", codePoint);
            rowVals.put("DATE_POINTEE", new java.sql.Date(this.datePointee.getDate().getTime()));

            try {
                rowVals.update(id);
            } catch (SQLException e1) {

                e1.printStackTrace();
            }
        }
        this.model.updateTotauxCompte();
    }

    // Pointe la ligne passée en parametre
    private void actionDepointage(int rowIndex) {

        int id = this.ecriturePanel.getListe().idFromIndex(rowIndex);

        SQLRow row = this.tableEcr.getRow(id);
        SQLRowValues rowVals = new SQLRowValues(this.tableEcr);

        // Dépointage
        if (row.getString("POINTEE").trim().length() != 0) {

            rowVals.put("POINTEE", "");
            rowVals.put("DATE_POINTEE", null);

            try {
                rowVals.update(id);
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        this.model.updateTotauxCompte();
    }

    /*
     * MaJ de la requete pour remplir la IListe en fonction du compte sélectionner et du mode de
     * sélection
     */
    private void changeListRequest() {

        // Champs à afficher
        List<String> listEcriture = new ArrayList<String>();

        listEcriture.add("POINTEE");
        listEcriture.add("DATE_POINTEE");
        listEcriture.add("ID_MOUVEMENT");
        listEcriture.add("NOM");
        listEcriture.add("DATE");
        listEcriture.add("DEBIT");
        listEcriture.add("CREDIT");

        Object idCpt = this.selCompte.getSelectedId();

        // filtre de selection

        Where w = new Where(this.tableEcr.getField("ID_COMPTE_PCE"), "=", idCpt);

        if (!UserManager.getInstance().getCurrentUser().getRights().haveRight(ComptaUserRight.ACCES_NOT_RESCTRICTED_TO_411)) {
            // TODO Show Restricted acces in UI
            w = w.and(new Where(this.tableEcr.getField("COMPTE_NUMERO"), "LIKE", "411%"));
        }

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

        if (this.modeSelect == ecriturePointee) {
            w = w.and(new Where(this.tableEcr.getField("POINTEE"), "!=", ""));
        } else {
            if (this.modeSelect == ecritureNotPointee) {
                w = w.and(new Where(this.tableEcr.getField("POINTEE"), "=", ""));
            }
        }

        this.ecriturePanel.setRequest(new ListSQLRequest(this.tableEcr, listEcriture, w) {
            @Override
            protected void customizeToFetch(SQLRowValues graphToFetch) {
                super.customizeToFetch(graphToFetch);
                graphToFetch.put("VALIDE", null);
            }
        });
        this.ecriturePanel.getListe().setSQLEditable(false);

        // MaJ du renderer
        final PointageRenderer rend = new PointageRenderer();
        for (int i = 0; i < this.ecriturePanel.getListe().getJTable().getColumnCount(); i++) {
            this.ecriturePanel.getListe().getJTable().getColumnModel().getColumn(i).setCellRenderer(rend);
        }

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
        final JRadioButton buttonNotPointe = new JRadioButton("Non pointées");
        panelSelectEcritures.add(buttonNotPointe, cPanel);
        cPanel.gridx++;
        final JRadioButton buttonPointe = new JRadioButton("Pointées");
        panelSelectEcritures.add(buttonPointe, cPanel);

        ButtonGroup group = new ButtonGroup();
        group.add(buttonBoth);
        group.add(buttonNotPointe);
        group.add(buttonPointe);
        buttonBoth.setSelected(true);

        buttonPointe.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (buttonPointe.isSelected()) {
                    PointagePanel.this.modeSelect = ecriturePointee;
                    changeListRequest();
                }
            }
        });

        buttonNotPointe.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (buttonNotPointe.isSelected()) {
                    PointagePanel.this.modeSelect = ecritureNotPointee;
                    changeListRequest();
                }
            }
        });

        buttonBoth.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (buttonBoth.isSelected()) {
                    PointagePanel.this.modeSelect = allEcriture;
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
        ecritureNonValidPanel.setBackground(PointageRenderer.GetCouleurEcritureNonValide());
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
        ecriturePointePanel.add(new JLabel("Ecritures pointées"), cPanel);
        panelLegende.add(ecriturePointePanel, c);

        return panelLegende;
    }
}
