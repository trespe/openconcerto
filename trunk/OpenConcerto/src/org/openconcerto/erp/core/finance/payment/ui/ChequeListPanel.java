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
import org.openconcerto.erp.core.finance.payment.element.ChequeType;
import org.openconcerto.erp.model.GestionChequesModel;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.cc.IPredicate;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

abstract class ChequeListPanel extends ListeAddPanel {

    private GestionChequesModel model;
    private JLabel labelDepot;
    private JLabel labelMontant;
    private JCheckBox checkImpression;
    private JButton boutonValide;

    protected ChequeListPanel(final SQLElement elem) {
        super(elem, new IListe(((ChequeType) elem).createDepositTableSource()));

        this.setReadWriteButtonsVisible(false);
        this.getListe().setSQLEditable(false);

        // Model
        this.model = new GestionChequesModel(getListe(), (ChequeType) elem);

        this.model.addSelectionListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                setLabels();
            }
        });

        actionDroitTable();

        this.model.selectionDecaisseAll();
    }

    protected JPanel createBottomPanel() {
        final JPanel res = new JPanel();
        // 5*2
        res.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Labels de rappel
        this.labelDepot = new JLabel(getDepositLabel());
        res.add(this.labelDepot, c);

        // Date
        c.gridx++;
        final JDate dateDepot = new JDate();
        dateDepot.setValue(new Date());
        res.add(dateDepot, c);

        c.gridx++;
        this.labelMontant = new JLabel("");
        res.add(this.labelMontant, c);

        final JButton printPreview = createPreviewBtn();
        c.gridx++;
        if (printPreview != null) {
            printPreview.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    getModel().printPreview();
                }
            });
            res.add(printPreview, c);
        }

        // Bouton selection
        final JButton boutonSelectAll = new JButton("Sélectionner tous les chèques");
        boutonSelectAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getModel().selectionDecaisseAll();
            }
        });

        c.gridx++;
        res.add(boutonSelectAll, c);

        // CheckBox Impression
        c.gridy++;
        c.weightx = 0;
        c.gridx = 0;
        if (hasPrint()) {
            this.checkImpression = new JCheckBox("Impression du relevé");
            this.checkImpression.setSelected(true);
            res.add(this.checkImpression, c);
        }
        c.gridx++;

        // libellé de l'écriture
        final JTextComponent text = createLabelText();
        if (text != null) {
            c.fill = GridBagConstraints.NONE;
            final JLabelBold labelLib = new JLabelBold("Libellé des écritures");
            res.add(labelLib, c);
            c.gridx++;

            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridwidth = 2;
            res.add(text, c);
            c.gridx += c.gridwidth;
            c.weightx = 0;
            c.gridwidth = 1;
        } else {
            c.gridx += 3;
        }

        // Bouton valider
        this.boutonValide = createSubmitBtn(dateDepot, this.checkImpression, text);

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        res.add(this.boutonValide, c);

        return res;
    }

    protected JButton createPreviewBtn() {
        return null;
    }

    protected boolean hasPrint() {
        return true;
    }

    protected JTextComponent createLabelText() {
        return null;
    }

    protected abstract JButton createSubmitBtn(final JDate dateDepot, JCheckBox checkImpression, final JTextComponent text);

    @Override
    protected final void addComponents(Container container, GridBagConstraints c) {
        super.addComponents(container, c);
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.weighty = 0;
        container.add(this.createBottomPanel(), c);
    }

    public final GestionChequesModel getModel() {
        return this.model;
    }

    protected void actionDroitTable() {
        AbstractAction actionSource = new AbstractAction("Voir la source") {

            private EditFrame edit = null;

            public void actionPerformed(ActionEvent e) {
                final SQLRow rowCheque = IListe.get(e).fetchSelectedRow();
                int idMouvement = rowCheque.getInt("ID_MOUVEMENT");
                // System.out.println("ID_MVT :: " + chqTmp.getIdMvt());

                if (idMouvement > 1) {
                    MouvementSQLElement.showSource(idMouvement);
                } else {
                    if (this.edit == null) {
                        this.edit = new EditFrame(getElement(), EditFrame.MODIFICATION);
                        this.edit.pack();
                    }
                    this.edit.selectionId(rowCheque.getID());
                    this.edit.setVisible(true);
                }
            }
        };
        getListe().addRowAction(actionSource);
        getListe().addIListeAction(new PredicateRowAction(new AbstractAction("Sélectionner tout") {
            public void actionPerformed(ActionEvent e) {
                ChequeListPanel.this.model.selectionDecaisseAll();
            }
        }, false, true).setPredicate(IPredicate.truePredicate()));
        getListe().addIListeAction(new PredicateRowAction(new AbstractAction("Désélectionner tout") {
            public void actionPerformed(ActionEvent e) {
                ChequeListPanel.this.model.deselectionAll();
            }
        }, false, true).setPredicate(IPredicate.truePredicate()));

    }

    protected abstract String getDepositLabel();

    /**
     * Mis à jour des labels de rappel
     */
    private void setLabels() {

        int nbChq = this.getModel().getNbChequeSelected();

        if (nbChq == 0) {
            this.labelDepot.setText(getDepositLabel());
            this.labelMontant.setText("");
            this.boutonValide.setVisible(false);
        } else {
            long montantTot = this.getModel().getMontantTotalSelected();
            this.boutonValide.setVisible(true);
            if (nbChq == 1) {
                this.labelDepot.setText("Dépot de " + nbChq + " chéque, en date du ");
            } else {
                this.labelDepot.setText("Dépot de " + nbChq + " chéques, en date du ");
            }
            this.labelMontant.setText(", pour un montant total de " + GestionDevise.currencyToString(montantTot) + " €");
        }
        if (hasPrint())
            this.checkImpression.setVisible(nbChq > 0);
    }
}
