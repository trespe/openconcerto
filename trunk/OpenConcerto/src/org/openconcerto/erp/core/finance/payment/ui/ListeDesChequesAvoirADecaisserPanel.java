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
 
 package org.openconcerto.erp.core.finance.payment.ui;

import org.openconcerto.erp.core.finance.payment.element.ChequeAvoirClientSQLElement;
import org.openconcerto.ui.JDate;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.text.JTextComponent;

public class ListeDesChequesAvoirADecaisserPanel extends ChequeListPanel {

    public ListeDesChequesAvoirADecaisserPanel(final ChequeAvoirClientSQLElement elem) {
        super(elem);
    }

    @Override
    protected boolean hasPrint() {
        return false;
    }

    @Override
    protected JButton createSubmitBtn(final JDate dateDepot, final JCheckBox checkImpression, final JTextComponent text) {
        final JButton res = new JButton("Valider le décaissement");
        res.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getModel().valideDepot(dateDepot.getDate(), false);
            }
        });
        return res;
    }

    @Override
    protected String getDepositLabel() {
        return "Sélectionner les chéques à décaisser, en date du ";
    }
}
