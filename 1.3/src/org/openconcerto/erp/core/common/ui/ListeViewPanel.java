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

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;

import java.awt.event.ActionEvent;

import javax.swing.JButton;

public class ListeViewPanel extends ListeAddPanel {
    protected EditFrame editFrame;

    public ListeViewPanel(SQLElement component) {
        super(component);
        this.buttonAjouter.setVisible(false);
        this.buttonClone.setVisible(false);
        this.buttonEffacer.setVisible(false);
        this.buttonModifier.setText("Voir");
    }

    public ListeViewPanel(SQLElement component, IListe list) {
        super(component, list);
        this.buttonAjouter.setVisible(false);
        this.buttonClone.setVisible(false);
        this.buttonEffacer.setVisible(false);
        this.buttonModifier.setText("Voir");
    }

    protected void handleAction(JButton source, ActionEvent evt) {
        if (source == this.buttonModifier) {
            if (this.editFrame == null) {
                this.editFrame = new EditFrame(this.element, EditPanel.READONLY);
            }
            this.editFrame.selectionId(this.getListe().getSelectedId(), -1);
            this.editFrame.setVisible(true);
        } else {
            super.handleAction(source, evt);
        }
    }

    public void setTextButton(String s) {
        this.buttonModifier.setText(s);
    }
}
