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

import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.model.GestionChequesModel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldRef;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.ui.JDate;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.TableSorter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;


public class ListeDesChequesADecaisserPanel extends JPanel {

    private GestionChequesModel model;
    private JLabel labelDecaisse = new JLabel("Sélectionner les chéques à décaisser, en date du ");
    private JDate dateDecaisse = new JDate();
    private JLabel labelMontant = new JLabel("");
    private JTable table;

    private TableSorter s;
    private EditFrame edit;
    private final JButton boutonValide = new JButton("Valider le décaissement");
    private JCheckBox checkImpression = new JCheckBox("Impression du relevé");

    public ListeDesChequesADecaisserPanel() {

        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.insets = new Insets(2, 2, 1, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.BOTH;

        // Model
        SQLTable tableChequeFournisseur = Configuration.getInstance().getDirectory().getElement("CHEQUE_FOURNISSEUR").getTable();
        SQLTable tableFourn = Configuration.getInstance().getDirectory().getElement("FOURNISSEUR").getTable();

        List<FieldRef> fields = new ArrayList<FieldRef>();
        fields.add(tableChequeFournisseur.getField("MONTANT"));
        fields.add(tableFourn.getField("TYPE"));
        fields.add(tableFourn.getField("NOM"));
        fields.add(tableChequeFournisseur.getField("ID_MOUVEMENT"));
        fields.add(tableChequeFournisseur.getField("DATE_ACHAT"));
        fields.add(tableChequeFournisseur.getField("DATE_MIN_DECAISSE"));
        fields.add(tableChequeFournisseur.getField("DECAISSE"));

        this.model = new GestionChequesModel(tableChequeFournisseur, fields, tableChequeFournisseur.getField("DECAISSE"), tableChequeFournisseur.getField("DATE_MIN_DECAISSE"));

        // JTable
        this.s = new TableSorter(this.model);
        this.table = new JTable(this.s);
        this.s.setTableHeader(this.table.getTableHeader());

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

        this.table.getColumnModel().getColumn(0).setCellRenderer(new GestionChequesRenderer(this.s));
        this.table.getColumnModel().getColumn(5).setCellRenderer(new GestionChequesRenderer(this.s));

        this.add(new JScrollPane(this.table), c);

        // Date
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;

        this.dateDecaisse.setValue(new Date());

        // Libellés de rappel
        c.gridy++;
        c.gridx = GridBagConstraints.RELATIVE;

        c.gridwidth = 1;
        this.add(this.labelDecaisse, c);
        c.gridwidth = 1;
        this.add(this.dateDecaisse, c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(this.labelMontant, c);

        // bouton selection
        final JButton boutonSelectAll = new JButton("Sélectionner tous les chèques");
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(boutonSelectAll, c);

        boutonSelectAll.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                model.selectionDecaisseAll();
            }
        });

        // CheckBox Impression
        c.gridx++;
        this.add(this.checkImpression, c);

        // Bouton valide
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        this.add(this.boutonValide, c);

        this.boutonValide.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                model.valideDepot(dateDecaisse.getDate(), GestionChequesModel.MODE_ACHAT, checkImpression.isSelected());
                model.fireTableDataChanged();
            }
        });

        // Bouton fermer
        JButton buttonFermer = new JButton("Fermer");
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        this.add(buttonFermer, c);
        buttonFermer.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                JFrame frame = (JFrame) SwingUtilities.getRoot(ListeDesChequesADecaisserPanel.this);
                frame.setVisible(false);
                frame.dispose();
            }
        });

    }

    public GestionChequesModel getModel() {
        return this.model;
    }

    /**
     * Mis à jour des labels
     */
    private void setLabels() {

        int nbChq = this.model.getNbChequeSelected();

        if (nbChq == 0) {

            this.labelDecaisse.setText("Sélectionner les chéques à décaisser, en date du ");
            this.labelMontant.setText("");
            this.boutonValide.setVisible(false);
            this.checkImpression.setVisible(false);
        } else {
            long montantTot = this.model.getMontantTotalSelected();
            this.boutonValide.setVisible(true);
            this.checkImpression.setVisible(true);
            if (nbChq == 1) {

                this.labelDecaisse.setText("Décaissement de " + nbChq + " chéque, en date du ");
                this.labelMontant.setText(", pour un montant total de " + GestionDevise.currencyToString(montantTot) + " €");
            } else {

                this.labelDecaisse.setText("Décaissement de " + nbChq + " chéques, en date du ");
                this.labelMontant.setText(", pour un montant total de " + GestionDevise.currencyToString(montantTot) + " €");
            }
        }

    }

    private void actionDroitTable(MouseEvent mE) {
        JPopupMenu menuDroit = new JPopupMenu();
        final int row = this.table.getSelectedRow();

        final AbstractAction abstractAction = new AbstractAction("Voir la source") {
            public void actionPerformed(ActionEvent e) {

                // TODO si id_mouvement == 1 afficher ChequeSQLElement
                if (row >= 0 && row < table.getRowCount()) {
                    final SQLElement chequeFournisseurElt = Configuration.getInstance().getDirectory().getElement("CHEQUE_FOURNISSEUR");
                    SQLTable tableCheque = chequeFournisseurElt.getTable();

                    SQLRow rowCheque = tableCheque.getRow(model.getIdAtRow(s.modelIndex(row)));
                    int idMouvement = rowCheque.getInt("ID_MOUVEMENT");

                    if (idMouvement > 1) {
                        MouvementSQLElement.showSource(idMouvement);
                    } else {
                        if (edit == null) {
                            edit = new EditFrame(chequeFournisseurElt, EditFrame.MODIFICATION);
                            edit.pack();
                        }
                        edit.selectionId(rowCheque.getID());
                        edit.setVisible(true);
                    }
                }
            }
        };
        abstractAction.setEnabled(row >= 0 && row < this.table.getRowCount());
        menuDroit.add(abstractAction);

        menuDroit.add(new AbstractAction("Sélectionner tout") {
            public void actionPerformed(ActionEvent e) {

                model.selectionDecaisseAll();
            }
        });

        menuDroit.add(new AbstractAction("Désélectionner tout") {
            public void actionPerformed(ActionEvent e) {

                model.deselectionAll();
            }
        });

        menuDroit.show(mE.getComponent(), mE.getX(), mE.getY());
    }
}
