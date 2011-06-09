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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.warning.JLabelWarning;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class SuppressionEcrituresPanel extends JPanel {

    public SuppressionEcrituresPanel(final int idMvt) {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLTable tableMouvement = base.getTable("MOUVEMENT");
        SQLRow rowMvt = tableMouvement.getRow(idMvt);
        JLabel label = new JLabel("Vous allez supprimer la piéce n°" + rowMvt.getInt("ID_PIECE"));
        JLabelWarning warning = new JLabelWarning();
        JPanel panelLabel = new JPanel();
        panelLabel.add(warning);
        panelLabel.add(label);

        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(panelLabel, c);

        // TODO afficher les numeros de mouvement implique
        int[] idS = getMouvement(rowMvt.getInt("ID_PIECE"));
        if (idS == null) {
            ExceptionHandler.handle("Aucun mouvement associé à la piéce n°" + ((rowMvt != null) ? rowMvt.getInt("ID_PIECE") : "mouvement nul"));
        } else {
            StringBuffer s = new StringBuffer();
            s.append("Elle est composée par les mouvements : (");
            JLabel labelMouv = new JLabel();
            // c.gridwidth = 1;
            c.gridy++;
            c.gridx = 0;
            this.add(labelMouv, c);
            s.append(idS[0]);
            for (int i = 1; i < idS.length; i++) {

                s.append(", ");
                s.append(idS[i]);
            }
            s.append(')');
            labelMouv.setText(s.toString());
        }

        JButton buttonOK = new JButton("OK");
        JButton buttonCancel = new JButton("Annuler");

        c.gridwidth = 1;
        c.gridy++;
        c.gridx = 0;
        this.add(buttonOK, c);
        c.gridx++;
        this.add(buttonCancel, c);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                EcritureSQLElement elt = (EcritureSQLElement) Configuration.getInstance().getDirectory().getElement("ECRITURE");
                elt.archiveMouvement(idMvt);
                ((JFrame) SwingUtilities.getRoot(SuppressionEcrituresPanel.this)).dispose();
            }
        });
        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((JFrame) SwingUtilities.getRoot(SuppressionEcrituresPanel.this)).dispose();
            }
        });
    }

    private int[] getMouvement(int idPiece) {

        int[] idS = null;

        SQLBase b = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLTable tableMvt = b.getTable("MOUVEMENT");

        SQLSelect sel = new SQLSelect(b);
        sel.addSelect(tableMvt.getField("NUMERO"));
        sel.setWhere("MOUVEMENT.ID_PIECE", "=", idPiece);

        List l = (List) b.getDataSource().execute(sel.asString(), new ArrayListHandler());

        if (l.size() > 0) {
            idS = new int[l.size()];
        }

        for (int i = 0; i < l.size(); i++) {
            Object[] tmp = (Object[]) l.get(i);
            idS[i] = Integer.parseInt(tmp[0].toString());
        }

        return idS;
    }

}
