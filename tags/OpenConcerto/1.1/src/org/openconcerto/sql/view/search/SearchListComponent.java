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
 
 package org.openconcerto.sql.view.search;

import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.utils.FormatGroup;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

public class SearchListComponent extends JPanel {

    private ITableModel model;

    private final List<SearchItemComponent> items;

    private final GridBagConstraints c;
    private Runnable r;

    private final Map<Class<?>, FormatGroup> formats;

    public SearchListComponent(ITableModel model) {
        super();
        this.r = null;
        this.items = new ArrayList<SearchItemComponent>();
        this.formats = new HashMap<Class<?>, FormatGroup>();

        this.setLayout(new GridBagLayout());
        this.c = new GridBagConstraints();
        this.c.gridx = 0;
        this.c.gridy = 0;
        this.c.insets = new Insets(0, 2, 0, 2);
        this.c.fill = GridBagConstraints.HORIZONTAL;
        this.c.weightx = 1;

        this.reset(model);
    }

    protected final TableModel getTableModel() {
        return this.model;
    }

    // never contains null
    final String[] getColumnNames(int index) {
        final SQLTableModelColumn col = this.model.getLinesSource().getParent().getColumn(index);
        final String[] res = new String[] { col.getName(), col.getToolTip(), col.getIdentifier() };
        assert res[res.length - 1] != null : "Null identifier for " + col;
        for (int i = res.length - 2; i >= 0; i--) {
            if (res[i] == null)
                res[i] = res[i + 1];
        }
        return res;
    }

    private SearchItemComponent getItem(int i) {
        return this.items.get(i);
    }

    // we're a JComponent so all of our methods must be called from the EDT
    // plus no need to synchronize
    private void checkEDT() {
        if (!SwingUtilities.isEventDispatchThread())
            Thread.dumpStack();
    }

    public void reset(ITableModel newModel) {
        this.model = newModel;
        this.reset();
    }

    private void reset() {
        this.checkEDT();
        this.removeAll();
        this.items.clear();
        this.c.gridy = 0;

        if (this.getTableModel() != null) {
            this.addNewSearchItem();
            this.updateSearch();
        } else
            this.revalidate();
    }

    public void addNewSearchItem() {
        this.checkEDT();

        final SearchItemComponent item = new SearchItemComponent(this);
        add(item, this.c);
        this.c.gridy++;
        this.items.add(item);

        this.revalidate();
    }

    public void updateSearch() {
        checkEDT();
        // ne pas passer par model.invokeLater() sinon la recherche risque de lagger
        // (attendre après la base)
        this.model.search(getSearchList(), this.r);
    }

    /**
     * Créé et retourne la searchList correspondant
     * 
     * @return la searchList remplie
     */
    private SearchList getSearchList() {
        final SearchList l = new SearchList();
        for (int i = 0; i < this.items.size(); i++) {
            final SearchItemComponent component = this.items.get(i);
            l.addSearchItem(component.getSearchItem());
        }
        return l;
    }

    public void removeSearchItem(SearchItemComponent item) {
        this.checkEDT();

        if (this.items.size() > 1) {
            this.items.remove(item);
            this.remove(item);
            this.revalidate();
            updateSearch();
        } else {
            getItem(0).resetState();
        }
    }

    public void clear() {
        this.reset();
    }

    public void setSearchString(String s) {
        this.setSearchString(s, null);
    }

    /**
     * Recherche <code>s</code> puis exécute r.
     * 
     * @param s la chaîne à rechercher.
     * @param r sera exécuter dans la thread de la recherche.
     */
    public void setSearchString(String s, Runnable r) {
        checkEDT();

        this.clear();
        // DO NOT assign r before clear() otherwise it will be run twice
        this.r = r;
        this.getItem(0).setText(s);
        this.r = null;
    }

    public void setSearchFullMode(boolean b) {
        this.checkEDT();

        for (int i = 0; i < this.items.size(); i++) {
            SearchItemComponent component = this.items.get(i);
            component.setSearchFullMode(b);
        }
    }

    public final void setFormats(Map<Class<?>, FormatGroup> formats) {
        this.formats.clear();
        this.formats.putAll(formats);
    }

    public final Map<Class<?>, FormatGroup> getFormats() {
        return this.formats;
    }
}
