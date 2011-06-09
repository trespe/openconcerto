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


import org.openconcerto.erp.generationDoc.gestcomm.PointageXmlSheet;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Calendar;


import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

public class GenerationPointagePanel extends JPanel implements ActionListener {
    private final JButton gen = new JButton("Générer");
    private final JButton close = new JButton("Fermer");
    private final SQLRequestComboBox combo = new SQLRequestComboBox(false);

    private final int currentYear = Calendar.getInstance().get(Calendar.YEAR);
    private final SpinnerModel model = new SpinnerNumberModel(this.currentYear, this.currentYear - 5, this.currentYear + 5, 1);
    private final JSpinner spinYear = new JSpinner(this.model);

    final JTextArea infos = new JTextArea();

    public GenerationPointagePanel() {
        super(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        SQLElement moisElt = Configuration.getInstance().getDirectory().getElement("MOIS");

        // Mois
        this.add(new JLabel("Mois"), c);
        c.gridx++;

        ComboSQLRequest comboReq = new ComboSQLRequest(moisElt.getComboRequest());
        this.combo.uiInit(comboReq);
        this.add(this.combo, c);

        // Annee
        this.add(new JLabel("Année"), c);
        c.gridx++;

        this.add(this.spinYear, c);

       
        JPanel panelButton = new JPanel();
        panelButton.add(this.gen);
        panelButton.add(this.close);
        this.gen.setEnabled(false);

        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        c.weighty = 0;
        c.gridy++;
        this.add(panelButton, c);

        this.combo.addValueListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                gen.setEnabled(combo.getSelectedRow() != null);
            }
        });

        this.gen.addActionListener(this);
        this.close.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.gen) {

            int mois = this.combo.getValue() - 2;
            int year = Integer.valueOf(this.spinYear.getValue().toString());
            PointageXmlSheet sheet = new PointageXmlSheet(mois, year);
            sheet.genere(true, false);
        } else {
            if (e.getSource() == this.close) {
                ((JFrame) SwingUtilities.getRoot(this)).dispose();
            }
        }
    }
}
