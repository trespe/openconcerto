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
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.sql.model.SQLField;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

public class DeviseNumericRemiseCellEditor extends DeviseNumericCellEditor implements MouseListener {
    private BigDecimal ht = BigDecimal.ZERO;
    private BigDecimal ttc = BigDecimal.ZERO;

    public DeviseNumericRemiseCellEditor(SQLField field) {
        super(field);
        this.textField.addMouseListener(this);
    }

    public void setHT(BigDecimal d) {
        this.ht = d;
    }

    public void setTTC(BigDecimal d) {
        this.ttc = d;
    }

    public void mousePressed(MouseEvent e) {
        if (textField.getText().trim().length() > 0 && e.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu menuDroit = new JPopupMenu();
            final AbstractAction htToPercent = new AbstractAction("Convertir le montant HT en %") {

                public void actionPerformed(ActionEvent e) {

                    String s = textField.getText().trim();
                    if (s.length() > 0) {

                        BigDecimal prixHTRemise = new BigDecimal(s);
                        BigDecimal divide = prixHTRemise.divide(ht, MathContext.DECIMAL128).movePointRight(2);
                        divide = divide.setScale(precision, RoundingMode.HALF_UP);
                        textField.setText(divide.toString());
                    }
                }
            };
            htToPercent.setEnabled(ht.signum() != 0);
            menuDroit.add(htToPercent);

            final AbstractAction ttcToPercent = new AbstractAction("Convertir le montant TTC en %") {

                public void actionPerformed(ActionEvent e) {

                    String s = textField.getText().trim();
                    if (s.length() > 0) {

                        BigDecimal prixTTCRemise = new BigDecimal(s);
                        BigDecimal divide = prixTTCRemise.divide(ttc, MathContext.DECIMAL128).movePointRight(2);
                        divide = divide.setScale(precision, RoundingMode.HALF_UP);
                        textField.setText(divide.toString());
                    }
                }
            };
            ttcToPercent.setEnabled(ttc.signum() != 0);
            menuDroit.add(ttcToPercent);

            menuDroit.pack();
            menuDroit.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
            menuDroit.setVisible(true);
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
