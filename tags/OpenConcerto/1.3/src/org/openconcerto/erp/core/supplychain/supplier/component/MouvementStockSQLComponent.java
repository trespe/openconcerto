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
 
 package org.openconcerto.erp.core.supplychain.supplier.component;

import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class MouvementStockSQLComponent extends BaseSQLComponent {

    private SQLTextCombo textLib;
    private JTextField textQte;
    private JDate date;

    public MouvementStockSQLComponent(SQLElement element) {
        super(element);
    }

    public void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Libellé
        JLabel labelLib = new JLabel(getLabelFor("NOM"), SwingConstants.RIGHT);
        this.add(labelLib, c);

        c.gridx++;
        c.weightx = 1;
        this.textLib = new SQLTextCombo();
        this.add(this.textLib, c);

        // Date
        c.gridx++;
        c.weightx = 0;
        JLabel labelDate = new JLabel(getLabelFor("DATE"), SwingConstants.RIGHT);
        this.add(labelDate, c);

        c.gridx++;
        this.date = new JDate(true);
        this.add(this.date, c);

        // Article
        final ElementComboBox articleSelect = new ElementComboBox();

        c.gridx = 0;
        c.gridy++;
        JLabel labelArticle = new JLabel(getLabelFor("ID_ARTICLE"), SwingConstants.RIGHT);
        this.add(labelArticle, c);

        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        this.add(articleSelect, c);

        // QTE
        c.gridwidth = 1;
        c.weightx = 0;
        c.gridy++;
        c.gridx = 0;
        c.anchor = GridBagConstraints.EAST;
        JLabel labelQte = new JLabel(getLabelFor("QTE"), SwingConstants.RIGHT);
        this.add(labelQte, c);

        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        this.textQte = new JTextField(6);
        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        this.add(this.textQte, c);
        c.gridy++;
        c.weighty = 1;
        final JPanel comp = new JPanel();
        comp.setOpaque(false);
        this.add(comp, c);
        DefaultGridBagConstraints.lockMinimumSize(this.textQte);
        DefaultGridBagConstraints.lockMaximumSize(this.textQte);
        this.addRequiredSQLObject(this.textQte, "QTE");
        this.addSQLObject(this.textLib, "NOM");
        this.addRequiredSQLObject(articleSelect, "ID_ARTICLE");
        this.addRequiredSQLObject(this.date, "DATE");
    }

    @Override
    public int insert(SQLRow order) {
        int id = super.insert(order);
        ((MouvementStockSQLElement) getElement()).updateStock(Arrays.asList(id), false);
        return id;
    }

    @Override
    public void update() {
        int id = getSelectedID();
        ((MouvementStockSQLElement) getElement()).updateStock(Arrays.asList(id), true);
        super.update();
        ((MouvementStockSQLElement) getElement()).updateStock(Arrays.asList(id), false);
    }

}
