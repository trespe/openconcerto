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
 
 package org.openconcerto.erp.model;

import org.openconcerto.erp.core.finance.accounting.model.PlanComptableEModel;
import org.openconcerto.erp.core.finance.accounting.ui.GestionPlanComptableEFrame;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.component.combo.ISearchableCombo;
import org.openconcerto.ui.component.combo.SearchMode;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JTable;

public class ISQLCompteSelector extends ElementComboBox {

    // FIXME visualisation num compte

    private static GestionPlanComptableEFrame frame = null;
    private static ISQLCompteSelector ref = null;
    private final static SQLElement compteElt = Configuration.getInstance().getDirectory().getElement("COMPTE_PCE");

    private boolean addButtonVisible = false;

    public ISQLCompteSelector() {
        super();
    }

    public ISQLCompteSelector(boolean addButtonVisible) {
        this();
        this.addButtonVisible = addButtonVisible;
    }

    JButton selectCompte = new JButton("Sélection Compte");

    public void setSelectButtonEnabled(boolean b) {
        this.selectCompte.setEnabled(b);
    }

    @Override
    protected void uiLayout() {
        super.uiLayout();

        this.setButtonsVisible(false);
        this.setAddIconVisible(this.addButtonVisible);

        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridx = GridBagConstraints.EAST;

        // TODO use the standard list button or at least disable when we're disabled
        this.add(selectCompte, c);

        // if only digits assume we're looking for a hierarchical account number, otherwise perform
        // a standard "contains" search
        this.combo.setCompletionMode(new SearchMode() {
            private final Pattern pattrn = Pattern.compile("^\\d+$");

            @Override
            public ComboMatcher matcher(String search) {
                final boolean onlyDigits = this.pattrn.matcher(search).matches();
                if (onlyDigits)
                    return new ComboMatcher(search) {
                        @Override
                        public boolean match(String item) {
                            return item.startsWith(this.getSearch());
                        }
                    };
                else
                    return ISearchableCombo.MODE_CONTAINS.matcher(search);
            }
        });
        selectCompte.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                ref = ISQLCompteSelector.this;

                if (frame == null) {
                    frame = new GestionPlanComptableEFrame();
                    Vector v = frame.getPanelPCE().getTables();

                    for (int i = 0; i < v.size(); i++) {

                        final JTable tab = (JTable) v.get(i);
                        tab.addMouseListener(new MouseAdapter() {
                            public void mouseClicked(MouseEvent e) {
                                System.err.println("Mouse clicked");
                                if (e.getButton() == MouseEvent.BUTTON1) {
                                    PlanComptableEModel model = (PlanComptableEModel) tab.getModel();
                                    ref.setValue(model.getId(tab.getSelectedRow()));
                                }
                            }
                        });
                    }
                    frame.pack();
                } else {
                    Vector v = frame.getPanelPCE().getTables();

                    for (int i = 0; i < v.size(); i++) {

                        final JTable tab = (JTable) v.get(i);
                        tab.addMouseListener(new MouseAdapter() {
                            public void mouseClicked(MouseEvent e) {
                                System.err.println("Mouse clicked");
                                if (e.getButton() == MouseEvent.BUTTON1) {
                                    PlanComptableEModel model = (PlanComptableEModel) tab.getModel();
                                    ref.setValue(model.getId(tab.getSelectedRow()));
                                }
                            }
                        });
                    }
                    frame.pack();
                }

                frame.setVisible(true);
            }
        });

        // new Thread(this).start();
        // this.loadCache();

        // this.setValue(this.def);
    }

    public void init() {
        this.init(compteElt);
    }
}
