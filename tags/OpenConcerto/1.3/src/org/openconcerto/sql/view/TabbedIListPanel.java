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
 
 package org.openconcerto.sql.view;

import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.view.list.IListe;

import java.awt.event.ActionEvent;

import javax.swing.JButton;

public class TabbedIListPanel extends IListPanel {

    public TabbedIListPanel(SQLElement elem) {
        super(elem);

    }

    private TabbedListeModifyPanel tabbedPanel;

    public TabbedIListPanel(SQLElement element, IListe liste, TabbedListeModifyPanel panel) {
        super(element, liste);
        tabbedPanel = panel;
    }

    @Override
    public SQLComponent getModifComp() {
        return this.tabbedPanel.getModifComp();
    }

    protected void handleAction(JButton source, ActionEvent evt) {
        if (source == this.buttonModifier) {
            System.err.println("TabbedIListPanel.handleAction() Modify");
            tabbedPanel.showEditTab();
        } else if (source == this.buttonAjouter) {
            System.err.println("TabbedIListPanel.handleAction() Add");
            tabbedPanel.showAddTab(tabbedPanel.getBrowser().getFirstSelectedRows());
        } else {
            super.handleAction(source, evt);
        }
    }
    
    @Override
    protected boolean modifyIsImmediate() {
        // tab is displayed
        return false;
    }

    @Override
    protected void listSelectionChanged(int id) {
        super.listSelectionChanged(id);
        if (tabbedPanel == null)
            return;
        // keep the current scrolling value, to restore it afterwards
        System.err.println("yooooooooo");
        System.err.println("TabbedListeModifyPanel.me" + tabbedPanel);
        tabbedPanel.setSelectedId(id);
        // tabbedPanel.getEditorComponent().select(id);
        // tabbedPanel.getEditorComponent().setEditable(id != -1);
        // if(getElement().getTable().getRow(id)!=null){
        // tabbedPanel.updateEditor(id>1,
        // getElement().getDescription(getElement().getTable().getRow(id)));
        // }else{
        // tabbedPanel.updateEditor(id>1, "...");
        // }
    }

}
