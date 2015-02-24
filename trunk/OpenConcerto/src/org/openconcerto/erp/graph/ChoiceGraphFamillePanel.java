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
 
 /*
 * Créé le 23 avr. 2012
 */
package org.openconcerto.erp.graph;

import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.JDate;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ChoiceGraphFamillePanel extends JPanel {

    public ChoiceGraphFamillePanel() {
        super(new GridBagLayout());

        JLabel labelCom = new JLabel("Afficher le graphique pour la période du");
        GridBagConstraints c = new DefaultGridBagConstraints();
        this.add(labelCom, c);
        c.gridx++;
        final JDate d1 = new JDate();
        this.add(d1, c);
        c.gridx++;
        JLabel labelYear = new JLabel("au");
        final JDate date2 = new JDate();

        this.add(labelYear, c);
        c.gridx++;
        this.add(date2, c);

        final JButton buttonValid = new JButton(new AbstractAction("Valider") {

            @Override
            public void actionPerformed(ActionEvent e) {

                GraphFamilleArticlePanel p = new GraphFamilleArticlePanel(d1.getDate(), date2.getDate());
                FrameUtil.show(new PanelFrame(p, "Répartition du CA par famille"));

            }

        });
        c.gridx++;
        this.add(buttonValid, c);

        buttonValid.setEnabled(false);
        d1.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                buttonValid.setEnabled(d1.getDate() != null && date2.getDate() != null);
            }
        });
        date2.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                buttonValid.setEnabled(d1.getDate() != null && date2.getDate() != null);
            }
        });
    }

}
