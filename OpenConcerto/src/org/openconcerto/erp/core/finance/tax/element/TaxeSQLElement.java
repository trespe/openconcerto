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
 
 package org.openconcerto.erp.core.finance.tax.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.model.ISQLCompteSelector;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.ProductInfo;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class TaxeSQLElement extends ComptaSQLConfElement {

    public TaxeSQLElement() {
        super("TAXE", "une taxe", "taxes");

        PredicateRowAction action = new PredicateRowAction(new AbstractAction("Définir par défaut") {

            @Override
            public void actionPerformed(ActionEvent e) {

                final SQLRowAccessor row = IListe.get(e).getSelectedRow();
                final SQLDataSource ds = row.getTable().getDBSystemRoot().getDataSource();

                try {
                    SQLUtils.executeAtomic(ds, new SQLUtils.SQLFactory<Object>() {
                        @Override
                        public Object create() throws SQLException {
                            // Transaction
                            UpdateBuilder upRemoveDefault = new UpdateBuilder(row.getTable());
                            upRemoveDefault.set("DEFAULT", "FALSE");

                            ds.execute(upRemoveDefault.asString());

                            UpdateBuilder upSetDefault = new UpdateBuilder(row.getTable());
                            upSetDefault.set("DEFAULT", "TRUE");
                            upSetDefault.setWhere(new Where(row.getTable().getKey(), "=", row.getID()));

                            ds.execute(upSetDefault.asString());

                            JOptionPane.showMessageDialog(null, "Rédémarrez " + ProductInfo.getInstance().getName() + " pour valider le changement de TVA par défaut.");

                            return null;
                        }
                    });
                } catch (Exception ex) {
                    throw new IllegalStateException("Erreur lors d'affectation de la nouvelle TVA par défaut", ex);
                }

            }
        }, true);
        action.setPredicate(IListeEvent.getSingleSelectionPredicate());

        this.getRowActions().add(action);

    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("TAUX");
        l.add("DEFAULT");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            @Override
            protected void addViews() {

                this.setLayout(new GridBagLayout());
                GridBagConstraints c = new DefaultGridBagConstraints();
                JLabel labelNom = new JLabel(getLabelFor("NOM"), SwingConstants.RIGHT);

                this.add(labelNom, c);
                c.gridx++;
                c.weightx = 1;
                c.fill = GridBagConstraints.NONE;
                JTextField fieldNom = new JTextField(40);
                DefaultGridBagConstraints.lockMinimumSize(fieldNom);
                this.add(fieldNom, c);
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                c.fill = GridBagConstraints.HORIZONTAL;
                JLabel labelTaux = new JLabel(getLabelFor("TAUX"), SwingConstants.RIGHT);
                this.add(labelTaux, c);
                c.gridx++;
                c.fill = GridBagConstraints.NONE;
                JTextField fieldTaux = new JTextField(6);
                DefaultGridBagConstraints.lockMinimumSize(fieldTaux);
                this.add(fieldTaux, c);

                JLabel labelCompteCol = new JLabel(getLabelFor("ID_COMPTE_PCE_COLLECTE"), SwingConstants.RIGHT);
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridx = 0;
                c.gridy++;
                this.add(labelCompteCol, c);
                c.gridx++;
                c.weightx = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                ISQLCompteSelector compteCol = new ISQLCompteSelector();
                this.add(compteCol, c);



                JLabel labelCompteDed = new JLabel(getLabelFor("ID_COMPTE_PCE_DED"), SwingConstants.RIGHT);
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                c.gridwidth = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                this.add(labelCompteDed, c);
                c.gridx++;
                c.weightx = 1;

                c.gridwidth = GridBagConstraints.REMAINDER;
                ISQLCompteSelector compteDed = new ISQLCompteSelector();
                this.add(compteDed, c);

                // Spacer
                c.gridy++;
                c.weighty = 1;
                c.anchor = GridBagConstraints.NORTHWEST;
                this.add(new JPanel(), c);

                this.addSQLObject(compteCol, "ID_COMPTE_PCE_COLLECTE");
                this.addSQLObject(compteDed, "ID_COMPTE_PCE_DED");

                this.addRequiredSQLObject(fieldNom, "NOM");
                this.addRequiredSQLObject(fieldTaux, "TAUX");
            }
        };
    }

}
