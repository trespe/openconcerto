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
 
 package org.openconcerto.erp.core.finance.payment.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.model.GestionChequesModel;
import org.openconcerto.erp.rights.ComptaTotalUserRight;
import org.openconcerto.openoffice.OOUtils;
import org.openconcerto.openoffice.XMLFormatVersion;
import org.openconcerto.openoffice.spreadsheet.SpreadSheet;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.AliasedTable;
import org.openconcerto.sql.model.FieldRef;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.IListPanel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.table.ViewTableModel;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.TableModelSelectionAdapter;
import org.openconcerto.utils.TableSorter;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class ListeDesChequesAEncaisserPanel extends JPanel {

    private GestionChequesModel model;
    private JLabel labelDepot = new JLabel("Sélectionner les chéques à déposer, en date du ");
    private JDate dateDepot = new JDate();
    private JLabel labelMontant = new JLabel("");
    private JTable table;
    private TableSorter s;
    private EditFrame edit = null;
    private final JCheckBox checkImpression = new JCheckBox("Impression du relevé");
    private final JButton boutonValide = new JButton("Valider le dépôt");

    public ListeDesChequesAEncaisserPanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;

        // Model
        SQLTable tableChequeClient = Configuration.getInstance().getDirectory().getElement("CHEQUE_A_ENCAISSER").getTable();
        SQLTable tableClient = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("CLIENT");

        List<FieldRef> fields = new ArrayList<FieldRef>();

        fields.add(tableChequeClient.getField("ETS"));
        fields.add(tableChequeClient.getField("NUMERO"));
        fields.add(tableChequeClient.getField("DATE"));
        fields.add(tableChequeClient.getField("ID_MOUVEMENT"));
        fields.add(tableChequeClient.getField("DATE_VENTE"));
        fields.add(tableChequeClient.getField("DATE_MIN_DEPOT"));

                fields.add(tableClient.getField("FORME_JURIDIQUE"));
            fields.add(tableClient.getField("NOM"));
        fields.add(tableChequeClient.getField("MONTANT"));
        fields.add(tableChequeClient.getField("ENCAISSE"));

        this.model = new GestionChequesModel(tableChequeClient, fields, tableChequeClient.getField("ENCAISSE"), tableChequeClient.getField("DATE_MIN_DEPOT"));

        // JTable
        this.s = new TableSorter(this.model);
        this.table = new JTable(this.s);
        this.s.setTableHeader(this.table.getTableHeader());

        // TODO Ajouter un JTableStateManager
        this.model.addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                setLabels();
            }
        });

        this.table.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {

                if (e.getButton() == MouseEvent.BUTTON3) {
                    actionDroitTable(e);
                }
            }
        });

        this.model.selectionDecaisseAll();

        this.table.getColumnModel().getColumn(5).setCellRenderer(new GestionChequesRenderer(this.s));
            this.table.getColumnModel().getColumn(8).setCellRenderer(new GestionChequesRenderer(this.s));
        this.add(new JScrollPane(this.table), c);

        // Labels de rappel
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridx = 0;
        this.dateDepot.setValue(new Date());
        c.gridy++;
        c.gridwidth = 1;
        this.add(this.labelDepot, c);

        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        this.add(this.dateDepot, c);
        c.gridwidth = 1;
        c.gridx++;

        this.add(this.labelMontant, c);

        JPanel panelButton = new JPanel();
        // Bouton selection
        final JButton boutonSelectAll = new JButton("Sélectionner tous les chèques");
        boutonSelectAll.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                ListeDesChequesAEncaisserPanel.this.model.selectionDecaisseAll();
            }
        });

        final JButton printPreview = new JButton("Aperçu du relevé");

        printPreview.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                ListeDesChequesAEncaisserPanel.this.model.printPreview(GestionChequesModel.MODE_VENTE);
            }
        });

        panelButton.add(printPreview);
        panelButton.add(boutonSelectAll);

        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        this.add(panelButton, c);

        //
        c.anchor = GridBagConstraints.WEST;
        c.gridy++;
        c.weightx = 0;
        c.gridx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(this.checkImpression, c);

        // libellé de l'écriture
        final JLabelBold labelLib = new JLabelBold("Libellé des écritures");

        c.gridx++;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(labelLib, c);

        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        final JTextField text = new JTextField();
        c.gridwidth = 2;
        this.add(text, c);
        c.weightx = 0;
        c.gridwidth = 1;

        // Export
        c.gridx += 2;
        JButton buttonExport = new JButton(new ImageIcon(IListPanel.class.getResource("save.png")));
        buttonExport.setFocusPainted(false);
        buttonExport.setOpaque(false);
        buttonExport.setContentAreaFilled(false);
        buttonExport.setBorderPainted(false);
        buttonExport.setPreferredSize(new Dimension(20, 20));
        buttonExport.setMaximumSize(new Dimension(20, 20));
        this.add(buttonExport, c);
        buttonExport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    final File f = new File(Configuration.getInstance().getWD(), "ChequeAEncaisser" + ".sxc");

                    Object[] options = { "Tout", "Selection", "Annuler" };
                    int answer = JOptionPane.showOptionDialog(ListeDesChequesAEncaisserPanel.this, "Exporter l'ensemble ou uniquement la sélection de la liste?", "Export de la liste",
                            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, "Tout");

                    if (answer == JOptionPane.NO_OPTION) {
                        SpreadSheet.export(getExportModel(true), f, XMLFormatVersion.getDefault());
                    } else {
                        if (answer == JOptionPane.YES_OPTION) {
                            SpreadSheet.export(getExportModel(false), f, XMLFormatVersion.getDefault());
                        } else {
                            return;
                        }
                    }
                    final int i = JOptionPane.showConfirmDialog(ListeDesChequesAEncaisserPanel.this, "La liste est exportée au format OpenOffice classeur\n Désirez vous l'ouvrir avec OpenOffice?",
                            "Ouvir le fichier", JOptionPane.YES_NO_OPTION);
                    if (i == JOptionPane.YES_OPTION) {
                        OOUtils.open(f);
                    }
                } catch (Exception ex) {
                    ExceptionHandler.handle(ListeDesChequesAEncaisserPanel.this, "Erreur lors de la sauvegarde", ex);
                }
            }
        });

        JPanel panelButtonValidClose = new JPanel();

        // Bouton valider
        this.checkImpression.setSelected(true);

        this.boutonValide.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                final String s = text.getText();
                ListeDesChequesAEncaisserPanel.this.model.valideDepot(ListeDesChequesAEncaisserPanel.this.dateDepot.getDate(), GestionChequesModel.MODE_VENTE,
                        ListeDesChequesAEncaisserPanel.this.checkImpression.isSelected(), s);
                text.setText("");
            }
        });

        // Bouton Fermer
        JButton buttonFermer = new JButton("Fermer");
        panelButtonValidClose.add(this.boutonValide);
        panelButtonValidClose.add(buttonFermer);
        c.gridx += 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        this.add(panelButtonValidClose, c);
        buttonFermer.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                JFrame frame = (JFrame) SwingUtilities.getRoot(ListeDesChequesAEncaisserPanel.this);
                frame.setVisible(false);
                frame.dispose();
            }
        });

    }

    private TableModel getExportModel(boolean selection) {
        return new TableModelSelectionAdapter(new ViewTableModel(this.table), selection ? this.table.getSelectedRows() : null) {
            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                final Object value = super.getValueAt(rowIndex, columnIndex);
                if (value instanceof Long || value instanceof BigInteger) {
                    return new Double(((Number) value).longValue() / 100.0);
                } else {
                    return value;
                }
            }
        };
    }

    public GestionChequesModel getModel() {
        return this.model;
    }

    private void actionDroitTable(MouseEvent mE) {
        JPopupMenu menuDroit = new JPopupMenu();
        final int row = this.table.getSelectedRow();
        final SQLElement chequeElt = Configuration.getInstance().getDirectory().getElement("CHEQUE_A_ENCAISSER");

        AbstractAction actionSource = new AbstractAction("Voir la source") {
            public void actionPerformed(ActionEvent e) {
                if (row >= 0 && row < ListeDesChequesAEncaisserPanel.this.table.getRowCount()) {
                    final SQLRow rowCheque = chequeElt.getTable().getRow(ListeDesChequesAEncaisserPanel.this.model.getIdAtRow(ListeDesChequesAEncaisserPanel.this.s.modelIndex(row)));
                    int idMouvement = rowCheque.getInt("ID_MOUVEMENT");
                    // System.out.println("ID_MVT :: " + chqTmp.getIdMvt());

                    if (idMouvement > 1) {
                        MouvementSQLElement.showSource(idMouvement);
                    } else {
                        if (ListeDesChequesAEncaisserPanel.this.edit == null) {
                            ListeDesChequesAEncaisserPanel.this.edit = new EditFrame(chequeElt, EditFrame.MODIFICATION);
                            ListeDesChequesAEncaisserPanel.this.edit.pack();
                        }
                        ListeDesChequesAEncaisserPanel.this.edit.selectionId(rowCheque.getID());
                        ListeDesChequesAEncaisserPanel.this.edit.setVisible(true);
                    }
                }
            }
        };
        actionSource.setEnabled(row >= 0 && row < this.table.getRowCount());
        menuDroit.add(actionSource);

        menuDroit.add(new AbstractAction("Sélectionner tout") {
            public void actionPerformed(ActionEvent e) {

                ListeDesChequesAEncaisserPanel.this.model.selectionDecaisseAll();
            }
        });

        menuDroit.add(new AbstractAction("Désélectionner tout") {
            public void actionPerformed(ActionEvent e) {

                ListeDesChequesAEncaisserPanel.this.model.deselectionAll();
            }
        });

        if (UserManager.getInstance().getCurrentUser().getRights().haveRight(ComptaTotalUserRight.TOTAL)) {

            menuDroit.add(new AbstractAction("Régularisation en comptabilité") {

                public void actionPerformed(ActionEvent e) {
                    final SQLRow rowCheque = chequeElt.getTable().getRow(ListeDesChequesAEncaisserPanel.this.model.getIdAtRow(ListeDesChequesAEncaisserPanel.this.s.modelIndex(row)));

                    String price = GestionDevise.currencyToString(rowCheque.getLong("MONTANT"));
                    SQLRow rowClient = rowCheque.getForeignRow("ID_CLIENT");
                    String nomClient = rowClient.getString("NOM");
                    String piece = "";
                    SQLRow rowMvt = rowCheque.getForeignRow("ID_MOUVEMENT");
                    if (rowMvt != null) {
                        SQLRow rowPiece = rowMvt.getForeignRow("ID_PIECE");
                        piece = rowPiece.getString("NOM");
                    }
                    int answer = JOptionPane.showConfirmDialog(ListeDesChequesAEncaisserPanel.this, "Etes vous sûr de vouloir régulariser ce cheque de " + nomClient + " d'un montant de " + price
                            + "€ avec une saisie au kilometre?\nNom de la piéce : " + piece + "\nAttention, cette opération est irréversible.");
                    if (answer == JOptionPane.YES_OPTION) {

                        SQLRowValues rowVals = rowCheque.asRowValues();
                        rowVals.put("REG_COMPTA", Boolean.TRUE);
                        try {
                            rowVals.commit();
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            });
        }

        menuDroit.show(mE.getComponent(), mE.getX(), mE.getY());
    }

    /**
     * Mis à jour des labels de rappel
     */
    private void setLabels() {

        int nbChq = this.model.getNbChequeSelected();

        if (nbChq == 0) {

            this.labelDepot.setText("Sélectionner les chéques à déposer, en date du ");
            this.labelMontant.setText("");
            this.boutonValide.setVisible(false);
            this.checkImpression.setVisible(false);
        } else {
            long montantTot = this.model.getMontantTotalSelected();
            this.boutonValide.setVisible(true);
            this.checkImpression.setVisible(true);
            if (nbChq == 1) {

                this.labelDepot.setText("Dépot de " + nbChq + " chéque, en date du ");
                this.labelMontant.setText(", pour un montant total de " + GestionDevise.currencyToString(montantTot) + " €");
            } else {

                this.labelDepot.setText("Dépot de " + nbChq + " chéques, en date du ");
                this.labelMontant.setText(", pour un montant total de " + GestionDevise.currencyToString(montantTot) + " €");
            }
        }

    }
}
