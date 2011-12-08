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
 
 package org.openconcerto.erp.preferences;

import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.preferences.DefaultPreferencePanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.print.attribute.standard.PrinterName;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public abstract class AbstractImpressionPreferencePanel extends DefaultPreferencePanel {

    private static final String TIRET = " -";

    private static final String defaut = "Définir l'imprimante suivante par défaut pour tous - ";

    // Stocke le nom de l'imprimante définit
    private Map<String, String> mapName = new HashMap<String, String>();
    // Stocke le JLabel associe
    private Map<String, JLabel> mapJLabel = new HashMap<String, JLabel>();
    private static final String parcourir = "...";

    private static final String keyDefault = "DefaultPrinter";

    public AbstractImpressionPreferencePanel() {
        super();
    }

    public void uiInit(Map<String, String> mapLabel) {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Printer par défaut
        final GridBagConstraints cDefault = new DefaultGridBagConstraints();
        JLabel labelDefaut = new JLabel(defaut);
        this.mapJLabel.put(keyDefault, new JLabel(TIRET));
        final JButton buttonDefault = new JButton(parcourir);
        buttonDefault.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setPrinter(keyDefault);
            }
        });

        final JButton buttonSetDefaut = new JButton("Appliquer");
        buttonSetDefaut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (String key : AbstractImpressionPreferencePanel.this.mapName.keySet()) {
                    AbstractImpressionPreferencePanel.this.mapName.put(key, AbstractImpressionPreferencePanel.this.mapName.get(keyDefault));
                }
                for (Entry<String, JLabel> entry : AbstractImpressionPreferencePanel.this.mapJLabel.entrySet()) {
                    entry.getValue().setText(AbstractImpressionPreferencePanel.this.mapName.get(keyDefault) + TIRET);
                }
            }
        });

        cDefault.gridx = GridBagConstraints.RELATIVE;
        JPanel panelDefaut = new JPanel(new GridBagLayout());
        panelDefaut.add(labelDefaut, cDefault);
        cDefault.weightx = 1;
        panelDefaut.add(this.mapJLabel.get(keyDefault), cDefault);
        cDefault.weightx = 0;
        panelDefaut.add(buttonDefault, cDefault);
        panelDefaut.add(buttonSetDefaut, cDefault);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;

        this.add(panelDefaut, c);

        c.anchor = GridBagConstraints.NORTHWEST;
        List<String> list = new ArrayList<String>(mapLabel.keySet());
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        c.weightx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        for (int i = 0; i < list.size(); i++) {
            final String key = list.get(i);

            c.gridx = 0;
            c.gridy++;
            if (i == list.size() - 1) {
                c.weighty = 1;
            }
            this.add(new JLabel(mapLabel.get(key) + " - "), c);
            c.gridx++;

            this.mapJLabel.put(key, new JLabel(TIRET));
            this.add(this.mapJLabel.get(key), c);

            final JButton button = new JButton(parcourir);
            c.gridx++;
            this.add(button, c);

            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setPrinter(key);
                }
            });

        }

        setValues();
    }

    private void setPrinter(final String s) {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final PrinterJob pjob = PrinterJob.getPrinterJob();
                if (pjob.printDialog()) {
                    final String printerName = pjob.getPrintService().getAttribute(PrinterName.class).toString();
                    AbstractImpressionPreferencePanel.this.mapName.put(s, printerName);
                    AbstractImpressionPreferencePanel.this.mapJLabel.get(s).setText((printerName == null ? "" : printerName) + TIRET);
                }
            };
        });
    }

    public void storeValues() {
        for (Entry<String, String> entry : this.mapName.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase(keyDefault)) {
                PrinterNXProps.getInstance().setProperty(entry.getKey(), entry.getValue());
            }
        }
        PrinterNXProps.getInstance().store();
    }

    public void restoreToDefaults() {
        for (Entry<String, JLabel> entry : this.mapJLabel.entrySet()) {
            entry.getValue().setText(TIRET);
        }
        for (String key : this.mapName.keySet()) {
            this.mapName.put(key, "");
        }
    }

    public void setValues() {

        for (String key : this.mapJLabel.keySet()) {
            this.mapName.put(key, PrinterNXProps.getInstance().getStringProperty(key));
        }

        for (Entry<String, JLabel> entry : this.mapJLabel.entrySet()) {
            final String printerName = this.mapName.get(entry.getKey());
            entry.getValue().setText((printerName == null ? "" : printerName) + TIRET);
        }
    }
}
