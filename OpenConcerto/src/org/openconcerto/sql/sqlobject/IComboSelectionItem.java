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
 * IComboSelectionItem created on 24 oct. 2003
 */
package org.openconcerto.sql.sqlobject;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.ui.component.combo.VarDesc;

/**
 * @author ILM Informatique
 */
public class IComboSelectionItem implements VarDesc, Comparable<IComboSelectionItem> {

    public static final int NO_FLAG = 0;
    public static final int WARNING_FLAG = 1;
    public static final int ERROR_FLAG = 2;
    public static final int IMPORTANT_FLAG = 3;

    private final SQLRowAccessor row;
    private final int fId;
    private final String fLabel;
    private int flag;

    public IComboSelectionItem(final int id, final String label) {
        this(null, id, label);
    }

    public IComboSelectionItem(final SQLRowAccessor row, final String label) {
        this(row, row.getID(), label);
    }

    private IComboSelectionItem(final SQLRowAccessor row, final int id, final String label) {
        this.row = row;
        this.fId = id;
        this.fLabel = label;
        this.setFlag(NO_FLAG);
    }

    public IComboSelectionItem(final IComboSelectionItem o) {
        this.row = o.row;
        this.fId = o.getId();
        this.fLabel = o.getLabel();
        this.setFlag(o.getFlag());
    }

    public final int getId() {
        return this.fId;
    }

    public final SQLRowAccessor getRow() {
        return this.row;
    }

    public final String getLabel() {
        return this.fLabel;
    }

    public final int getFlag() {
        return this.flag;
    }

    public final void setFlag(final int flag) {
        this.flag = flag;
    }

    @Override
    public final String toString() {
        return this.getLabel();
    }

    public final String dump() {
        return "[" + this.getId() + "] " + this.getLabel();
    }

    @Override
    public boolean equals(final Object o) {
        final boolean result;
        if (o instanceof IComboSelectionItem) {
            final IComboSelectionItem item = (IComboSelectionItem) o;
            // have to test also the label, otherwise when a row is modified (and obviously don't
            // change id) Swing won't update it.
            result = item.getId() == getId() && item.getLabel().equals(this.getLabel());
        } else {
            result = false;
        }
        return result;
    }

    @Override
    public int hashCode() {
        return this.fLabel.hashCode() + getId();
    }

    public String asString(final int precision) {
        return this.getLabel();
    }

    @Override
    public int compareTo(final IComboSelectionItem o) {
        return this.getLabel().compareTo(o.getLabel());
    }

}
