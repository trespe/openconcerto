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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.payment.element.ChequeAEncaisserSQLElement;
import org.openconcerto.erp.rights.ComptaTotalUserRight;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.ui.JDate;
import org.openconcerto.utils.GestionDevise;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

public class ListeDesChequesAEncaisserPanel extends ChequeListPanel {

    public ListeDesChequesAEncaisserPanel(final ChequeAEncaisserSQLElement elem) {
        super(elem);
    }

    @Override
    protected JButton createPreviewBtn() {
        return new JButton("Aperçu du relevé");
    }

    @Override
    protected JTextComponent createLabelText() {
        return new JTextField();
    }

    @Override
    protected JButton createSubmitBtn(final JDate dateDepot, final JCheckBox checkImpression, final JTextComponent text) {
        final JButton res = new JButton("Valider le dépôt");
        res.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String s = text.getText();
                getModel().valideDepot(dateDepot.getDate(), checkImpression.isSelected(), s);
                text.setText("");
            }
        });
        return res;
    }

    @Override
    protected void actionDroitTable() {
        super.actionDroitTable();
        if (UserManager.getInstance().getCurrentUser().getRights().haveRight(ComptaTotalUserRight.MENU)) {

            getListe().addRowAction(new AbstractAction("Régularisation en comptabilité") {

                public void actionPerformed(ActionEvent e) {
                    final SQLRow rowCheque = IListe.get(e).fetchSelectedRow();

                    String price = GestionDevise.currencyToString(rowCheque.getLong("MONTANT"));
                    SQLRow rowClient = rowCheque.getForeignRow("ID_CLIENT");
                    String nomClient = rowClient.getString("NOM");
                    String piece = "";
                    SQLRow rowMvt = rowCheque.getForeignRow("ID_MOUVEMENT");
                    if (rowMvt != null) {
                        SQLRow rowPiece = rowMvt.getForeignRow("ID_PIECE");
                        piece = rowPiece.getString("NOM");
                    }
                    int answer = JOptionPane.showConfirmDialog(ListeDesChequesAEncaisserPanel.this, "Etes vous sûr de vouloir régulariser ce cheque de " + nomClient + " d'un montant de " + price
                            + "€ avec une saisie au kilometre?\nNom de la piéce : " + piece + "\nAttention, cette opération est irréversible.");
                    if (answer == JOptionPane.YES_OPTION) {

                        SQLRowValues rowVals = rowCheque.asRowValues();
                        rowVals.put("REG_COMPTA", Boolean.TRUE);
                        try {
                            rowVals.commit();
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    @Override
    protected String getDepositLabel() {
        return "Sélectionner les chéques à déposer, en date du ";
    }
}
