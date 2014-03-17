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
 
 package org.openconcerto.erp.core.supplychain.supplier.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.rights.ComptaUserRight;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.GestionDevise;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class EcheanceFournisseurSQLElement extends ComptaSQLConfElement {

    public EcheanceFournisseurSQLElement() {
        super("ECHEANCE_FOURNISSEUR", "une échéance fournisseur", "échéances fournisseurs");

        PredicateRowAction actionShowSource = new PredicateRowAction(new AbstractAction("Voir la source") {

            public void actionPerformed(ActionEvent e) {
                SQLRow row = IListe.get(e).fetchSelectedRow();
                MouvementSQLElement.showSource(row.getInt("ID_MOUVEMENT"));
            }
        }, false);
        actionShowSource.setPredicate(IListeEvent.getNonEmptySelectionPredicate());
        getRowActions().add(actionShowSource);
        if (UserRightsManager.getCurrentUserRights().haveRight(ComptaUserRight.MENU)) {

            RowAction actionCancel = new RowAction(new AbstractAction("Annuler la régularisation en comptabilité") {

                public void actionPerformed(ActionEvent e) {

                    int answer = JOptionPane.showConfirmDialog(null, "Etes vous sûr de vouloir annuler la régularisation ?");
                    if (answer == JOptionPane.YES_OPTION) {
                        SQLRow row = IListe.get(e).getSelectedRow().asRow();
                        SQLRowValues rowVals = row.createEmptyUpdateRow();
                        rowVals.put("REG_COMPTA", Boolean.FALSE);
                        try {
                            rowVals.commit();
                        } catch (SQLException e1) {
                            ExceptionHandler.handle("Une erreur est survenue lors de l'annulation de la régularisation.", e1);
                        }
                    }
                }
            }, false) {
                @Override
                public boolean enabledFor(List<SQLRowAccessor> selection) {
                    if (selection != null && selection.size() == 1) {
                        SQLRowAccessor row = selection.get(0);
                        return row.getBoolean("REG_COMPTA");
                    } else {
                        return true;
                    }
                }
            };
            getRowActions().add(actionCancel);

            RowAction actionRegul = new RowAction(new AbstractAction("Régularisation en comptabilité") {

                public void actionPerformed(ActionEvent e) {

                    SQLRow row = IListe.get(e).fetchSelectedRow();
                    String price = GestionDevise.currencyToString(row.getLong("MONTANT"));
                    SQLRow rowFournisseur = row.getForeignRow("ID_FOURNISSEUR");

                    String nomFour = rowFournisseur.getString("NOM");
                    String piece = "";
                    SQLRow rowMvt = row.getForeignRow("ID_MOUVEMENT");
                    if (rowMvt != null) {
                        SQLRow rowPiece = rowMvt.getForeignRow("ID_PIECE");
                        piece = rowPiece.getString("NOM");
                    }
                    int answer = JOptionPane.showConfirmDialog(null, "Etes vous sûr de vouloir régulariser l'échéance de " + nomFour + " d'un montant de " + price
                            + "€ avec une saisie au kilometre?\nNom de la piéce : " + piece + ".");
                    if (answer == JOptionPane.YES_OPTION) {

                        SQLRowValues rowVals = row.createEmptyUpdateRow();
                        rowVals.put("REG_COMPTA", Boolean.TRUE);
                        try {
                            rowVals.commit();
                        } catch (SQLException e1) {
                            ExceptionHandler.handle("Une erreur est survenue lors de la régularisation.", e1);
                        }
                    }
                }
            }, false) {
                @Override
                public boolean enabledFor(List<SQLRowAccessor> selection) {
                    if (selection != null && selection.size() == 1) {
                        SQLRowAccessor row = selection.get(0);
                        return !row.getBoolean("REG_COMPTA");
                    } else {
                        return true;
                    }
                }
            };
            getRowActions().add(actionRegul);

        }
    }

    @Override
    public synchronized ListSQLRequest createListRequest() {
        return new ListSQLRequest(this.getTable(), this.getListFields()) {
            @Override
            protected void customizeToFetch(SQLRowValues graphToFetch) {
                super.customizeToFetch(graphToFetch);
                graphToFetch.put("REG_COMPTA", null);
                graphToFetch.put("REGLE", null);
            }
        };
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();

        l.add("MONTANT");
        l.add("DATE");
        l.add("ID_MOUVEMENT");
        l.add("ID_FOURNISSEUR");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("ID_FOURNISSEUR");
        l.add("MONTANT");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private DeviseField montant;
            private JDate date;
            private JTextField idMouvement;

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                this.montant = new DeviseField();
                this.date = new JDate();
                this.idMouvement = new JTextField();
                final ElementComboBox fournisseur = new ElementComboBox();

                // Mouvement
                JLabel labelMouvement = new JLabel("Mouvement");
                this.add(labelMouvement, c);

                c.weightx = 1;
                c.gridx++;
                this.add(this.idMouvement, c);

                // Date
                JLabel labelDate = new JLabel("Date");
                c.gridx++;
                this.add(labelDate, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.date, c);

                // Fournisseur
                JLabel labelFournisseur = new JLabel("Fournisseur");
                c.gridy++;
                c.gridx = 0;

                this.add(labelFournisseur, c);

                c.gridx++;
                c.weightx = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(fournisseur, c);

                // montant
                c.gridwidth = 1;
                JLabel labelMontant = new JLabel("Montant");
                c.gridy++;
                c.gridx = 0;
                this.add(labelMontant, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.montant, c);

                this.addSQLObject(this.montant, "MONTANT");
                this.addSQLObject(this.date, "DATE");
                this.addRequiredSQLObject(fournisseur, "ID_FOURNISSEUR");
                this.addSQLObject(this.idMouvement, "ID_MOUVEMENT");
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".commitment";
    }
}
