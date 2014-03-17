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
 
 package org.openconcerto.erp.core.humanresources.payroll.ui;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class ListeDesRubriquesPanel extends JPanel {

    public ListeDesRubriquesPanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        final JTabbedPane tabbedPane = new JTabbedPane();
        addPane(tabbedPane, "RUBRIQUE_BRUT", "Brut");
        addPane(tabbedPane, "RUBRIQUE_COTISATION", "Cotisations");
        addPane(tabbedPane, "RUBRIQUE_NET", "Net");
        addPane(tabbedPane, "RUBRIQUE_COMM", "Commentaires");
        this.add(tabbedPane, c);
    }

    private void addPane(final JTabbedPane tabbedPane, final String tableName, final String title) {
        final ListeAddPanel listeBrut = new ListeAddPanel(Configuration.getInstance().getDirectory().getElement(tableName));
        listeBrut.getListe().setSQLEditable(false);
        tabbedPane.add(title, listeBrut);
    }
}
