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
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.component.ITextArea;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.LinkedHashSet;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class AdresseSQLComponent extends BaseSQLComponent {
    private final JLabel labelDest = new JLabel(getLabelFor("DEST"));
    private final ITextArea destinataire = new ITextArea();
    private boolean destVisible = false;

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
        final VilleRowItemView villeRIV = new VilleRowItemView(new ITextComboVilleViewer());
        villeRIV.init("VILLE", new LinkedHashSet<SQLField>(Arrays.asList(getField("CODE_POSTAL"), getField("VILLE"))));
        final JLabel labelVille = new JLabel(getLabelFor(villeRIV.getSQLName()), SwingConstants.RIGHT);
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = 1;

        this.add(labelVille, c);
        c.gridwidth = 1;
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        DefaultGridBagConstraints.lockMinimumSize((JComponent) villeRIV.getComp());
        this.add(villeRIV.getComp(), c);

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
        DefaultGridBagConstraints.lockMinimumSize(cedex);
        this.add(cedex, c);
        // Province / Etat
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        final JLabel labelProvince = new JLabel(getLabelFor("PROVINCE"), SwingConstants.RIGHT);
        this.add(labelProvince, c);
        final SQLTextCombo province = new SQLTextCombo();
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        DefaultGridBagConstraints.lockMinimumSize(province);
        this.add(province, c);

        // Pays
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        final JLabel labelPays = new JLabel(getLabelFor("PAYS"), SwingConstants.RIGHT);
        this.add(labelPays, c);
        final SQLTextCombo pays = new SQLTextCombo();
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        DefaultGridBagConstraints.lockMinimumSize(pays);
        this.add(pays, c);

        // Email
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        final JLabel labelEmail = new JLabel(getLabelFor("EMAIL_CONTACT"), SwingConstants.RIGHT);
        this.add(labelEmail, c);
        final JTextField email = new JTextField(25);
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        DefaultGridBagConstraints.lockMinimumSize(email);
        this.add(email, c);

        this.addSQLObject(textRue, "RUE");
        this.addInitedView(villeRIV, REQ);
        this.addView(cedex, "CEDEX");
        this.addView(destinataire, "DEST");
        this.addView(province, "PROVINCE");
        this.addRequiredSQLObject(pays, "PAYS");
        this.addView(email, "EMAIL_CONTACT");

    }

    protected SQLRowValues createDefaults() {
        final SQLRowValues rowVals = new SQLRowValues(getTable());
        rowVals.put("PAYS", "France");
        setDestinataireVisible(destVisible);
        return rowVals;
    }

    public void setDestinataireVisible(boolean b) {
        this.destVisible = b;
        this.destinataire.setVisible(b);
        this.labelDest.setVisible(b);
    }

}
