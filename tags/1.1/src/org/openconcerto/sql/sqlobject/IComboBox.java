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
 
 package org.openconcerto.sql.sqlobject;

import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableListener;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.view.IListener;
import org.openconcerto.ui.FontUtils;
import org.openconcerto.utils.model.ListComboBoxModel;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * @author Sylvain Cuaz
 */
public class IComboBox extends JComboBox implements SQLTableListener {

    private final ComboSQLRequest fillFromReq;
    private final Set listeners;
    private boolean updating;

    public IComboBox(final ComboSQLRequest fillFromReq) {
        this.listeners = new HashSet();
        this.updating = false;
        this.fillFromReq = fillFromReq;
        this.fillFromReq.getPrimaryTable().addTableListener(this);

        FontUtils.setFontFor(this, fillFromReq.getSeparatorsChars());
        final ListCellRenderer oldRenderer = this.getRenderer();
        this.setRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final Component res = oldRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                FontUtils.setFontFor(res, "ComboBox", fillFromReq.getSeparatorsChars());
                return res;
            }
        });
        this.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                synchronized (this) {
                    if (!IComboBox.this.updating)
                        fireSelection();
                }
            }
        });
        this.setMaximumRowCount(30);

        // n'en tient pas compte
        // this.setMaximumSize(new Dimension(300,40));
        // TODO ne pas y mettre en dur (% de l'écran par ex)
        this.setPreferredSize(new Dimension(600, 22));
        this.setModel(new ListComboBoxModel());
    }

    public synchronized void fillComboFromRequest() {
        this.updating = true;
        this.getListModel().removeAllElements();
        final List v = this.fillFromReq.getComboItems();
        this.getListModel().addAll(v);
        this.updating = false;
    }

    public synchronized void removeItems(final int[] toRemove, final int toRemoveCount) {
        this.updating = true;
        for (int i = toRemoveCount - 1; i >= 0; i--) {
            int indexToRemove = toRemove[i];
            this.getListModel().removeElementAt(indexToRemove);
        }
        this.updating = false;
    }

    public ListComboBoxModel getListModel() {
        return (ListComboBoxModel) this.getModel();
    }

    public int getSelectedId() {
        IComboSelectionItem item = (IComboSelectionItem) this.getSelectedItem();
        return item == null ? -1 : item.getId();
    }

    public synchronized void setSelectedId(int id) {
        for (int i = 0; i < this.getItemCount(); i++) {
            final IComboSelectionItem obj = (IComboSelectionItem) this.getItemAt(i);
            if (obj.getId() == id)
                this.setSelectedItem(obj);
        }
    }

    public String toString() {
        return this.getSelectedItem() + "(" + getSelectedId() + ")";
    }

    public void selectNext() {
        int i = this.getSelectedIndex();
        if (i < this.getItemCount() - 1)
            this.setSelectedIndex(i + 1);
    }

    public void selectPrevious() {
        int i = this.getSelectedIndex();
        if (i > 0)
            this.setSelectedIndex(i - 1);
    }

    public void rowModified(SQLTable table, int id) {
        // MAYBE moins bourrin
        this.fillComboFromRequest();
    }

    public void rowAdded(SQLTable table, int id) {
        // MAYBE moins bourrin
        this.fillComboFromRequest();
    }

    public void rowDeleted(SQLTable table, int id) {
        // MAYBE moins bourrin
        this.fillComboFromRequest();
    }

    public synchronized void addIListener(IListener listener) {
        this.listeners.add(listener);
    }

    public synchronized void removeIListener(IListener listener) {
        this.listeners.remove(listener);
    }

    private synchronized void fireSelection() {
        Iterator iter = this.listeners.iterator();
        while (iter.hasNext()) {
            IListener l = (IListener) iter.next();
            l.selectionId(this.getSelectedId(), -1);
        }
    }

    public void setEditable(boolean b) {
        // ne pas faire setEditable(false), sinon plus de textField
        if (b)
            super.setEditable(b);
        super.setEnabled(b);
    }

    public final ComboSQLRequest getRequest() {
        return this.fillFromReq;
    }

}
