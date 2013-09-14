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
 * Créé le 2 mai 2005
 */
package org.openconcerto.sql.view.listview;

import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.utils.checks.EmptyChangeSupport;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidObject;
import org.openconcerto.utils.checks.ValidState;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;

/**
 * A SQLRowItemView that edits a list.
 * 
 * @author Sylvain CUAZ
 */
public class ListSQLView extends JPanel implements SQLRowItemView {

    private final SQLComponent parent;
    private final String name;
    private final ItemPool pool;
    private final List<ListItemSQLView> items;

    private final PropertyChangeSupport supp;
    private final EmptyChangeSupport helper;

    private final JButton addBtn;
    private final JPanel itemsPanel;
    private final GridBagConstraints itemsConstraints;

    public ListSQLView(SQLComponent parent, String name, ItemPoolFactory factory/*
                                                                                 * , ListItemSQLView
                                                                                 * style
                                                                                 */) {
        this.parent = parent;
        this.name = name;
        this.pool = factory.create(this);

        this.supp = new PropertyChangeSupport(this);
        this.items = new ArrayList<ListItemSQLView>();

        this.addValidListener(new ValidListener() {
            public void validChange(ValidObject src, ValidState newValue) {
                // compChanged(); FIXME
            }
        });

        this.addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                ListSQLView.this.addBtn.setEnabled(getPool().availableItem());
            }
        });

        this.helper = new EmptyChangeSupport(this);

        this.addBtn = new JButton("Ajout");
        // for when an item is removed the following ones go up
        this.itemsPanel = new JPanel(new GridBagLayout());
        this.itemsConstraints = new GridBagConstraints();
        this.uiInit();
    }

    protected void uiInit() {
        this.setLayout(new GridBagLayout());

        final GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(1, 2, 1, 2);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        c.gridy = 0;

        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        this.add(this.itemsPanel, c);
        c.weighty = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHEAST;
        this.add(this.addBtn, c);
        this.addBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addNewItem();
            }
        });

        this.itemsConstraints.gridx = 0;
        this.itemsConstraints.gridy = 0;
        this.itemsConstraints.weightx = 1;
        this.itemsConstraints.fill = GridBagConstraints.BOTH;
    }

    protected final ItemPool getPool() {
        return this.pool;
    }

    protected final ListItemSQLView addNewItem() {
        final ListItemSQLView newItem = new ListItemSQLView(this, this.getPool().getNewItem());
        this.items.add(newItem);
        newItem.getRowItemView().addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                ListSQLView.this.supp.firePropertyChange("value", null, null);
            }
        });
        this.readd(newItem);
        return newItem;
    }

    private final void readd(ListItemSQLView newItem) {
        this.itemsPanel.add(newItem, this.itemsConstraints);
        this.itemsConstraints.gridy++;
        this.supp.firePropertyChange("value", null, null);
        this.helper.fireEmptyChange(this.isEmpty());
    }

    protected final void removeItem(ListItemSQLView viewToRemove) {
        if (!this.items.contains(viewToRemove))
            throw new IllegalArgumentException("not mine " + viewToRemove);
        // viewToRemove.setEnabled(false);
        viewToRemove.setVisible(false);
        this.revalidate();
        this.getPool().removeItem(viewToRemove.getRowItemView());
        this.supp.firePropertyChange("value", null, null);
        this.helper.fireEmptyChange(this.isEmpty());
    }

    public final SQLComponent getSQLParent() {
        return this.parent;
    }

    //

    public void show(SQLRowAccessor r) {
        this.itemsPanel.removeAll();
        this.itemsPanel.revalidate();
        this.itemsConstraints.gridy = 0;
        this.items.clear();
        this.getPool().show(r);
    }

    public void update(final SQLRowValues vals) {
        this.getPool().update(vals);
    }

    public void insert(SQLRowValues vals) {
        this.getPool().insert(vals);
    }

    //

    public void setEditable(final boolean enabled) {
        this.addBtn.setEnabled(enabled);
        forAllDo(new Cl() {
            public void execute(ListItemSQLView input) {
                input.setEditable(enabled);
            }
        });
    }

    private final void forAllDo(final Cl c) {
        CollectionUtils.forAllDo(this.items, c);
    }

    static private abstract class Cl implements Closure {
        public abstract void execute(ListItemSQLView input);

        public final void execute(Object input) {
            this.execute((ListItemSQLView) input);
        }
    }

    public List<ListItemSQLView> getExistantViews() {
        return this.items;
    }

    public String getSQLName() {
        return this.name;
    }

    public String getDescription() {
        return this.getSQLName();
    }

    public SQLField getField() {
        // FIXME
        Thread.dumpStack();
        return null;
    }

    public void resetValue() {
        this.getPool().reset();
    }

    public Component getComp() {
        return this;
    }

    @Override
    public boolean isEmpty() {
        return this.getPool().getItems().isEmpty();
    }

    @Override
    public void addEmptyListener(EmptyListener l) {
        this.helper.addEmptyListener(l);
    }

    @Override
    public void removeEmptyListener(EmptyListener l) {
        this.helper.removeEmptyListener(l);
    }

    public void addValueListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(l);
    }

    @Override
    public ValidState getValidState() {
        return this.getPool().isValidated();
    }

    public void addValidListener(ValidListener l) {
        this.getPool().addValidListener(l);
    }

    @Override
    public void removeValidListener(ValidListener l) {
        this.getPool().removeValidListener(l);
    }

}
