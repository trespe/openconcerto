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

import org.openconcerto.utils.GestionDevise;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;

public class BalanceCellRenderer extends CompteCellRenderer {

    private int colNumeroCompte;

    public BalanceCellRenderer(int colNumeroCompte) {
        this.colNumeroCompte = colNumeroCompte;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        String numeroCompte = table.getValueAt(row, this.colNumeroCompte).toString().trim();
        String numeroCompteSuiv = null;

        if (row < table.getRowCount() - 1) {
            numeroCompteSuiv = table.getValueAt(row + 1, this.colNumeroCompte).toString().trim();
        }

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!isSelected) {
            // si le numéro compte est composé d'un seul chiffre
            final int length = numeroCompte.length();
            if (length == 1) {
                this.setBackground(couleurCompteClasse);
                this.setForeground(Color.BLACK);
            } else if ((numeroCompteSuiv != null) && (length < numeroCompteSuiv.length()) && (numeroCompte.equalsIgnoreCase(numeroCompteSuiv.substring(0, length)))) {
                // si le compte est racine
                // on affiche le numero et le libellé pas le reste
                if (length == 2) { // compte racine à 2 chiffres
                    this.setBackground(couleurCompte2);
                    this.setForeground(Color.BLACK);
                } else if (length == 3) {
                    // compte racine à 3 chiffres
                    this.setBackground(couleurCompte3);
                    this.setForeground(Color.BLACK);
                } else {
                    // autre compte racine
                    this.setBackground(couleurCompteRacine);
                    this.setForeground(Color.BLACK);
                }
            } else {
                // compte normaux
                this.setBackground(Color.WHITE);
                this.setForeground(Color.BLACK);
            }

        }

        if (value != null && value.getClass() == Long.class) {
            this.setText(GestionDevise.currencyToString(((Long) value).longValue()));
        }

        return this;
    }
}
