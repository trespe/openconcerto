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
 * Créé le 21 mai 2005
 * 
 */
package org.openconcerto.sql.navigator;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.list.selection.ListSelectionState;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.ListSelectionModel;

public class RowsSQLBrowserColumn extends SQLBrowserColumn<SQLRow, RowsSQLListModel> {

    // les Id selectionnés
    protected final List<Integer> ids = new ArrayList<Integer>();
    private final List<SQLField> children;
    private final ListSelectionState state;

    public RowsSQLBrowserColumn(SQLElement element) {
        this(element, true);
    }

    public RowsSQLBrowserColumn(SQLElement element, boolean searchable) {
        super(new RowsSQLListModel(element), searchable);
        this.children = new ArrayList<SQLField>();
        this.state = ListSelectionState.manage(this.list.getSelectionModel(), new ListStateModel(this.getModel()));
    }

    protected int getSelectionMode() {
        return ListSelectionModel.SINGLE_INTERVAL_SELECTION;
    }

    @Override
    protected ListSelectionState getSelectionState() {
        return this.state;
    }

    // *** selection

    public void addSelectionListener(PropertyChangeListener listener) {
        this.addPropertyChangeListener("selection", listener);
    }

    public void removeSelectionListener(PropertyChangeListener listener) {
        this.removePropertyChangeListener("selection", listener);
    }

    protected void selectionCleared() {
        this.ids.clear();
        this.highlight(Collections.<SQLRow> emptySet());
    }

    protected SQLBrowserColumn selectionChanged(ListSelectionModel m) {
        final List<SQLRow> values = this.getSelectedRows();
        final List<Integer> selectedIds = new ArrayList<Integer>();
        for (final SQLRow obj : values) {
            selectedIds.add(Integer.valueOf(obj.getID()));
        }
        this.ids.clear();
        this.ids.addAll(selectedIds);
        // ne rien surligner quand on change de sélection
        this.highlight(Collections.<SQLRow> emptySet());

        final SQLBrowserColumn<?, ?> res;
        if (this.next() != null)
            res = this.next();
        else
            res = this.createNextCol();
        if (res != null)
            res.setParentIDs(this.ids);
        return res;
    }

    public void setSelectedRow(SQLRow o) {
        this.list.setSelectedValue(o, true);
    }

    private SQLBrowserColumn<?, ?> createNextCol() {
        final SQLBrowserColumn<?, ?> res;
        final List<SQLField> refFields = this.getChildrenReferentFields();
        // si on n'a pas de fils ou pas d'id, ne pas créer de colonne
        if (refFields.size() == 0 || this.getSelectedIDs().isEmpty()) {
            res = null;
        } else if (refFields.size() == 1) {
            final SQLField refField = refFields.get(0);
            res = new RowsSQLBrowserColumn(getElement(refField), this.isSearchable());
        } else {
            final List<SQLElement> elements = new ArrayList<SQLElement>(refFields.size());
            for (final SQLField refField : refFields) {
                elements.add(this.getElement(refField));
            }
            res = new ElementsSQLBrowserColumn(this.getElement(), elements);
        }
        return res;
    }

    private SQLElement getElement(final SQLField refField) {
        return Configuration.getInstance().getDirectory().getElement(refField.getTable());
    }

    public List<Integer> getSelectedIDs() {
        return this.ids;
    }

    public List<SQLRow> getSelectedRows() {
        if (this.isAllSelected()) {
            return this.getModel().getRealItems();
        }
        return CollectionUtils.castToList(this.list.getSelectedValues(), SQLRow.class);
    }

    public final ListStateModel getStateModel() {
        return ((ListStateModel) this.getSelectionState().getModel());
    }

    public List<SQLField> getChildrenReferentFields() {
        return this.children;
    }

    public final SQLElement getElement() {
        return this.getModel().getElement();
    }

    public final SQLTable getTable() {
        return this.getElement().getTable();
    }

    protected String getHeaderName() {
        final String headerName = this.getElement().getPluralName();
        // Mets la 1ere lettre en majuscule
        return headerName.substring(0, 1).toUpperCase() + headerName.substring(1);
    }

    public SQLRow getFirstSelectedRow() {
        final int firstId = getFirstId();
        if (firstId > 0) {
            return this.getTable().getRow(firstId);
        }
        return null;
    }

    private int getFirstId() {
        if (this.ids.size() > 0)
            return this.ids.get(0).intValue();
        else
            return -1;
    }

    protected void die() {
        this.deselect();
        this.getModel().die();
    }

    protected void live() {
        final ITransformer<SQLElement, Collection<SQLField>> transf = this.getParentBrowser().getChildrenTransformer();
        final Collection<SQLField> res = transf == null ? null : transf.transformChecked(this.getElement());
        this.children.clear();
        if (res == null)
            this.children.addAll(this.getElement().getChildrenReferentFields());
        else
            this.children.addAll(res);
    }

    void highlight(Set<SQLRow> rows) {
        this.getModel().setHighlighted(rows);

        final RowsSQLBrowserColumn p = this.previousRowsColumn();
        if (p != null)
            if (this.getModel().getHighlighted().isEmpty())
                p.highlight(this.getSelectedParents());
            else
                p.highlight(this.getHighlightedParents());

        final int firstH = this.getModel().getFirstHighlighted();
        if (firstH >= 0)
            this.list.ensureIndexIsVisible(firstH);
    }

    static Color yellow = new Color(252, 253, 148);

    protected void render(JLabel comp, SQLRow value) {
        super.render(comp, value);
        if (this.getModel().getHighlighted().contains(value)) {
            comp.setBackground(yellow);
            comp.setForeground(Color.BLACK);
        }
    }

    // *** getParents

    // parents of the selection
    private Set<SQLRow> getSelectedParents() {
        return getParents(this.getSelectedRows());
    }

    // parents of the highlighted rows
    private Set<SQLRow> getHighlightedParents() {
        return getParents(this.getModel().getHighlighted());
    }

    static private Set<SQLRow> getParents(Collection<SQLRow> rows) {
        final Set<SQLRow> res = new HashSet<SQLRow>();
        for (final SQLRow r : rows) {
            final SQLRow parent = SQLBrowser.getElement(r.getTable()).getForeignParent(r);
            if (parent != null)
                res.add(parent);
        }
        return res;
    }
}
