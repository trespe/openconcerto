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

import org.openconcerto.ui.table.TableCellRendererUtils;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;

public class PlanComptableCellRenderer extends CompteCellRenderer {

    private transient final int colNumeroCompte;

    public PlanComptableCellRenderer(final int colNumeroCompte) {
        super();
        this.colNumeroCompte = colNumeroCompte;
    }

    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        TableCellRendererUtils.setBackgroundColor(this, table, isSelected);
        if (!isSelected) {
            final String numeroCompte = table.getValueAt(row, this.colNumeroCompte).toString().trim();

            // si le numéro compte est composé d'un seul chiffre
            final int compteLength = numeroCompte.length();
            if (compteLength == 1) {
                this.setBackground(couleurCompteClasse);
            } else {

                if (row < table.getRowCount() - 1) {
                    final String numeroCompteSuiv = table.getValueAt(row + 1, this.colNumeroCompte).toString().trim();

                    if ((compteLength < numeroCompteSuiv.length()) && (numeroCompte.equalsIgnoreCase(numeroCompteSuiv.substring(0, compteLength)))) {
                        // si le compte est racine
                        // on affiche le numero et le libellé pas le reste
                        // compte racine à 2 chiffres
                        if (compteLength == 2) {
                            this.setBackground(couleurCompte2);
                        } else if (compteLength == 3) {
                            // compte racine à 3 chiffres
                            this.setBackground(couleurCompte3);
                        } else {
                            // autre compte racine
                            this.setBackground(couleurCompteRacine);
                        }
                    }
                } else {
                    // compte normaux
                    this.setBackground(Color.WHITE);
                }
            }
            this.setForeground(Color.BLACK);
        }
        return this;
    }
}
