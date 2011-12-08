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
 
 package org.openconcerto.erp.core.sales.invoice.ui;

import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.generationDoc.gestcomm.EtatVentesXmlSheet;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.Tuple2;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class EtatVentesPanel extends JPanel implements ActionListener {

    private JDate du, au;
    private JButton buttonGen = new JButton("Créer");
    private JButton buttonClose = new JButton("Fermer");

    public EtatVentesPanel() {
        super(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        this.add(new JLabelBold("Etat des Ventes"), c);

        c.gridwidth = 1;
        c.gridy++;
        c.anchor = GridBagConstraints.WEST;

        // Période pédéfini
        final Map<String, Tuple2<Date, Date>> map = IListFilterDatePanel.getDefaultMap();
        if (map != null && map.keySet().size() > 0) {
            DefaultComboBoxModel model = new DefaultComboBoxModel();
            model.addElement("Tous");
            for (String s : map.keySet()) {
                model.addElement(s);
            }

            final JComboBox combo = new JComboBox(model);
            c.weightx = 0;
            this.add(combo, c);

            combo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String s = (String) combo.getSelectedItem();
                    setPeriode(map.get(s));
                }
            });
        }

        c.gridx++;
        this.add(new JLabel("du"), c);

        c.gridx++;
        this.du = new JDate(true);
        this.add(this.du, c);

        c.gridx++;
        this.add(new JLabel("au"), c);

        c.gridx++;
        this.au = new JDate(true);
        this.add(this.au, c);

        c.gridy++;
        c.gridx = 0;

        JPanel panelButton = new JPanel();
        panelButton.add(this.buttonGen);
        panelButton.add(this.buttonClose);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0;
        this.add(panelButton, c);
        this.buttonGen.addActionListener(this);
        this.buttonClose.addActionListener(this);
    }

    public void setPeriode(Tuple2<Date, Date> t) {
        if (t == null) {
            setPeriode(null, null);
        } else {

            setPeriode(t.get0(), t.get1());
        }
    }

    public void setDateDu(Date d) {
        d.setHours(0);
        d.setMinutes(0);
        this.du.setValue(d);
    }

    public void setDateAu(Date d) {
        d.setHours(23);
        d.setMinutes(59);
        this.au.setValue(d);
    }

    private void setPeriode(Date du, Date au) {

        setDateAu(au);
        setDateDu(du);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.buttonGen) {

            EtatVentesXmlSheet sheet = new EtatVentesXmlSheet(this.du.getDate(), this.au.getDate());
            try {
                // FIXME probleme de rendu avec le viewer
                sheet.createDocumentAsynchronous().get();
                sheet.openDocument(false);
            } catch (Exception e1) {
                e1.printStackTrace();
            }

        }
        ((JFrame) SwingUtilities.getRoot(this)).dispose();
    }
}
