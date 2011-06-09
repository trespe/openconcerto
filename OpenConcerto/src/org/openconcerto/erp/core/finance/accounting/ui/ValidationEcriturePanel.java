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
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class ValidationEcriturePanel extends JPanel {

    private final JDate dateValid;
    private final JLabel labelNbValid;
    private final JButton buttonValid;
    private final JCheckBox checkCloture = new JCheckBox("Clôturer cette période");;
    private final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();

    public ValidationEcriturePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridx = GridBagConstraints.RELATIVE;
        // date limite
        JLabel labelDate = new JLabel("Validation jusqu'au ");
        this.add(labelDate, c);

        this.dateValid = new JDate();
        this.add(this.dateValid, c);
        this.dateValid.addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                dateChanged();
            }
        });

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.labelNbValid = new JLabel();
        this.labelNbValid.setText("Sélectionnez une date.");
        this.add(this.labelNbValid, c);

        // Cloture
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 0;
        c.gridy++;
        this.add(this.checkCloture, c);

        c.gridy++;
        c.gridwidth = 1;
        c.gridx = 0;
        this.buttonValid = new JButton("Valider");
        this.add(this.buttonValid, c);

        this.buttonValid.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                validActionned();
            }
        });

        c.gridx++;
        JButton buttonClose = new JButton("Annuler");
        this.add(buttonClose, c);

        buttonClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFrame f = (JFrame) SwingUtilities.getRoot(ValidationEcriturePanel.this);
                f.dispose();
            }
        });
    }

// TODO from UCDetector: Change visibility of Method "ValidationEcriturePanel.dateChanged()" to private
    protected final void dateChanged() { // NO_UCD
        if (this.dateValid.getDate() != null) {
            nbValidationEcriture(this.dateValid.getDate());
            this.buttonValid.setEnabled(true);
        } else {
            this.labelNbValid.setText("Sélectionnez une date.");
            this.buttonValid.setEnabled(false);
        }
    }

    protected final void validActionned() {
        EcritureSQLElement.validationEcrituresBefore(this.dateValid.getDate(), this.checkCloture.isSelected());
        JFrame f = (JFrame) SwingUtilities.getRoot(ValidationEcriturePanel.this);
        f.dispose();
    }

    private void nbValidationEcriture(Date d) {
        final SQLTable tableEcriture = this.base.getTable("ECRITURE");

        final SQLSelect selEcriture = new SQLSelect(this.base);
        selEcriture.addSelectFunctionStar("count");
        Where w = new Where(tableEcriture.getField("DATE"), "<=", d);
        Where w2 = new Where(tableEcriture.getField("VALIDE"), "!=", Boolean.TRUE);
        selEcriture.setWhere(w.and(w2));

        final int nbEcritures = ((Number) this.base.getDataSource().executeScalar(selEcriture.asString())).intValue();
        if (nbEcritures != 0) {
            this.labelNbValid.setText("Validation de " + nbEcritures + " écritures.");
        } else {
            this.labelNbValid.setText("Aucune écritures à valider.");
        }
    }
}
