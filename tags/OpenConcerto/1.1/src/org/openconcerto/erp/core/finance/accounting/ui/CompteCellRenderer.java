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

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class CompteCellRenderer extends DefaultTableCellRenderer {

    // couleur compte classe 1 chiffre
    // private final static Color couleurCompteClasse = new Color(251, 193, 130);
    public final static Color couleurCompteClasse = new Color(225, 254, 207);

    // couleur compte racine par defaut
    public final static Color couleurCompte3 = new Color(253, 243, 204);

    // couleur compte racine 2 chiffres
    // private final static Color couleurCompte2 = new Color(251, 226, 174);
    public final static Color couleurCompte2 = new Color(206, 247, 255);

    // couleur compte racine 3 chiffres
    // private final static Color couleurCompteRacine = new Color(252, 222, 188);
    public final static Color couleurCompteRacine = new Color(255, 232, 245);

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}
