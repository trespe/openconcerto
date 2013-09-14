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
 
 package org.openconcerto.erp.core.common.component;

import org.openconcerto.erp.core.common.ui.VilleRowItemView;
import org.openconcerto.map.ui.ITextComboVilleViewer;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.component.ITextArea;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class AdresseSQLComponent extends BaseSQLComponent {
    private final JLabel labelDest = new JLabel(getLabelFor("DEST"));
    private final JTextArea destinataire = new JTextArea();

    public AdresseSQLComponent(SQLElement elt) {
        super(elt);
    }

    public void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        this.add(labelDest, c);
        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 1;

        this.add(destinataire, c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weighty = 0;
        // Rue
        final JLabel labelRue = new JLabel(getLabelFor("RUE"), SwingConstants.RIGHT);
        final ITextArea textRue = new ITextArea();
        this.add(labelRue, c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx++;
        c.weightx = 1;
        this.add(textRue, c);

        // Ville
        final JLabel labelVille = new JLabel(getLabelFor("VILLE"), SwingConstants.RIGHT);
        final ITextComboVilleViewer textVille = new ITextComboVilleViewer();
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = 1;

        this.add(labelVille, c);
        c.gridwidth = 1;
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        DefaultGridBagConstraints.lockMinimumSize(textVille);
        this.add(textVille, c);

        // Cedex
        final JLabel labelCedex = new JLabel(getLabelFor("CEDEX"), SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(labelCedex, c);
        final JTextField cedex = new JTextField(6);
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        this.add(cedex, c);

        // Pays
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        final JLabel labelPays = new JLabel(getLabelFor("PAYS"), SwingConstants.RIGHT);
        this.add(labelPays, c);
        final JTextField pays = new JTextField(16);
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        this.add(pays, c);

        this.addSQLObject(textRue, "RUE");
        this.addView(new VilleRowItemView(textVille), "CODE_POSTAL,VILLE", REQ);
        this.addView(cedex, "CEDEX");
        this.addView(destinataire, "DEST");

        this.addRequiredSQLObject(pays, "PAYS");

    }

    protected SQLRowValues createDefaults() {
        final SQLRowValues rowVals = new SQLRowValues(getTable());
        rowVals.put("PAYS", "France");
        setDestinataireVisible(false);
        return rowVals;
    }

    public void setDestinataireVisible(boolean b) {
        this.destinataire.setVisible(b);
        this.labelDest.setVisible(b);
    }

}
