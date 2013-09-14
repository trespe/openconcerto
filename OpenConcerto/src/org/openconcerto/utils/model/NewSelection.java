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
 
 package org.openconcerto.utils.model;

import org.openconcerto.utils.CollectionUtils;

import java.util.AbstractList;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;

public enum NewSelection {
    /**
     * Never change the selection. In other words the selection can be out of the list (as permitted
     * by {@link ComboBoxModel}).
     */
    NO,
    /**
     * The selection is cleared.
     */
    NONE,
    /**
     * Like {@link DefaultComboBoxModel#removeElementAt(int)} : try the index before the removal,
     * then the index after, finally clear the selection if all items were removed.
     */
    DIFFERENT_INDEX,
    /**
     * Try to select the same index, if it can't (i.e. the list is too small) the last index,
     * finally clear the selection if all items were removed.
     */
    SAME_INDEX;

    static public final class ReplaceView<T> {

        private final int to, from;
        private final int replacementCount;
        private final int addedCount;
        private final List<T> list;

        public ReplaceView(final List<T> l, final boolean replaced, final int from, final int to, final List<T> replacement, final int replacementCount) {
            if (!replaced && replacement == null)
                throw new IllegalArgumentException("Missing replacement");
            this.to = to;
            this.from = from;
            assert replacement == null || replacementCount == replacement.size();
            this.replacementCount = replacementCount;
            this.addedCount = getReplacementCount() - getRemovedCount();
            this.list = replaced ? l : new AbstractList<T>() {

                private final int size = l.size() + getAddedCount();

                @Override
                public T get(int index) {
                    if (index < from)
                        return l.get(index);
                    else if (index < from + getReplacementCount())
                        return replacement.get(index - from);
                    else
                        return l.get(index - getAddedCount());
                }

                @Override
                public int size() {
                    return this.size;
                }
            };
        }

        public final int getRemovedCount() {
            // from, to inclusive
            return this.to - this.from + 1;
        }

        public final int getReplacementCount() {
            return this.replacementCount;
        }

        public final int getAddedCount() {
            return this.addedCount;
        }

        public final List<T> getListView() {
            return this.list;
        }

        public T getOutsideReplacement(final int offset) {
            return getOutsideReplacement(offset, false);
        }

        public T getOutsideReplacement(int offset, final boolean lenient) {
            final int index = this.from + (offset < 0 ? 0 : getReplacementCount()) + offset;
            if (lenient)
                return CollectionUtils.getNoExn(this.getListView(), index);
            else
                return this.getListView().get(index);
        }
    }

    public final <T> T getNewSelection(DefaultIMutableListModel<T> model, final int selectedIndex, final int from, final int to, final int replacementCount) {
        return getNewSelection(model, selectedIndex, true, from, to, null, replacementCount);
    }

    public final <T> T getNewSelection(DefaultIMutableListModel<T> model, final int selectedIndex, final int from, final int to, final List<T> replacement) {
        return getNewSelection(model, selectedIndex, false, from, to, replacement, replacement.size());
    }

    private final <T> T getNewSelection(DefaultIMutableListModel<T> model, final int selectedIndex, final boolean listAlreadyChanged, final int from, final int to, final List<T> replacement,
            final int replacementCount) {
        if (this == NO)
            throw new UnsupportedOperationException();

        final ReplaceView<T> changedList = new ReplaceView<T>(model.getList(), listAlreadyChanged, from, to, replacement, replacementCount);
        final int newSize = changedList.getListView().size();

        if (this == NONE || newSize == 0)
            return null;

        final T newSel;
        if (this == SAME_INDEX) {
            // same index or closest
            final int newIndex = selectedIndex < newSize ? selectedIndex : newSize - 1;
            newSel = changedList.getListView().get(newIndex);
        } else {
            assert this == DIFFERENT_INDEX;
            if (from > 0) {
                // before
                newSel = changedList.getOutsideReplacement(-1);
            } else {
                // else after if possible
                newSel = changedList.getOutsideReplacement(0, true);
            }
        }
        return newSel;
    }

}
