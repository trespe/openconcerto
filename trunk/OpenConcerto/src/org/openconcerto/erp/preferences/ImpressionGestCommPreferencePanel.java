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
import org.openconcerto.ui.TitledSeparator;

import java.awt.GridBagConstraints;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JTextField;

// TODO choix du nombre de copies
public class ImpressionGestCommPreferencePanel extends AbstractImpressionPreferencePanel {

    private static final String keyDevis = "DevisPrinter";
    private static final String keyBon = "BonPrinter";
    private static final String keyFacture = "FacturePrinter";
    private static final String keyPropo = "PropositionPrinter";
    private static final String keyRelance = "RelancePrinter";
    private static final String keyCmd = "CmdPrinter";
    private static final String keyCmdCli = "CmdCliPrinter";
    private static final String keyQLPrinter = "QLPrinter";
    private final JTextField text = new JTextField(20);

    public ImpressionGestCommPreferencePanel() {
        super();

        final Map<String, String> map = new HashMap<String, String>();
        map.put(keyDevis, "Devis");
        map.put(keyBon, "Bon de livraison");
        map.put(keyFacture, "Facture");
        map.put(keyPropo, "Proposition");
        map.put(keyRelance, "Relance");
        map.put(keyCmd, "Commande");
        map.put(keyCmdCli, "Commande client");

        uiInit(map);
    }

    @Override
    public void uiInit(Map<String, String> mapLabel) {

        super.uiInit(mapLabel);

        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridy = 11;
        final TitledSeparator sep = new TitledSeparator("Imprimante ticket");
        c.weightx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(sep, c);

        c.weightx = 0;
        c.gridwidth = 1;
        c.gridy++;
        this.add(new JLabel("Adresse IP Brother QL : "), c);
        c.gridx++;
        c.weightx = 0;
        this.add(this.text, c);
    }

    public String getTitleName() {
        return "Impression gestion commerciale";
    }

    @Override
    public void storeValues() {
        super.storeValues();
        PrinterNXProps.getInstance().setProperty(keyQLPrinter, this.text.getText());
        PrinterNXProps.getInstance().store();
    }

    @Override
    public void setValues() {
        super.setValues();
        String ql = PrinterNXProps.getInstance().getProperty(keyQLPrinter);
        this.text.setText(ql);
    }

}
