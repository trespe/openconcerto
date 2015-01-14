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
 
 package org.openconcerto.ui.component.combo;

import org.openconcerto.ui.component.combo.SearchMode.ComboMatcher;
import org.openconcerto.utils.IFutureTask;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.model.ISearchable;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.swing.SwingUtilities;

public class ISearchableComboCompletionThread<T> extends Thread {
    private final ISearchableCombo<T> combo;
    private final String t;
    private boolean stopNow;

    public ISearchableComboCompletionThread(final ISearchableCombo<T> combo, final String t) {
        this.combo = combo;
        this.t = t;
        this.stopNow = false;
    }

    private ISearchableCombo<T> getCombo() {
        return this.combo;
    }

    @Override
    public void run() {
        computeAutoCompletion();
    }

    public synchronized void stopNow() {
        this.stopNow = true;
    }

    private synchronized boolean isStopped() {
        return this.stopNow;
    }

    private void computeAutoCompletion() {
        final boolean showAll = this.t == null;
        final List<ISearchableComboItem<T>> l;
        if (!showAll) {
            l = getMatchingValues();
        } else {
            l = getMaxValues();
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (isStopped()) {
                    return;
                }
                getCombo().setMatchingCompletions(l, showAll);
            }
        });

    }

    private List<ISearchableComboItem<T>> getMaxValues() {
        final List<ISearchableComboItem<T>> allVals = this.getCombo().getModelValues();
        return allVals.subList(0, Math.min(this.getCombo().getMaximumResult(), allVals.size()));
    }

    private List<ISearchableComboItem<T>> getMatchingValues() {
        final List<ISearchableComboItem<T>> result = new ArrayList<ISearchableComboItem<T>>();

        final int minimumSearch = getCombo().getMinimumSearch();
        final String aText = this.t.trim();
        final String normalizedText = aText.length() < minimumSearch ? "" : aText;

        Boolean searched = null;
        // If there was a search and now the text is below minimum, we must unset the search
        // Efficient since setSearch() only carry out its action if the search changes
        if (getCombo().getCache() instanceof ISearchable) {
            final ISearchable searchableListModel = (ISearchable) getCombo().getCache();
            if (searchableListModel.isSearchable()) {
                // Wait for the new values, which will be added to the model by a listener
                final FutureTask<Object> noOp = IFutureTask.createNoOp();
                searched = searchableListModel.setSearch(normalizedText, noOp);
                try {
                    noOp.get();
                } catch (InterruptedException e) {
                    throw new RTInterruptedException(e);
                } catch (ExecutionException e) {
                    throw new IllegalStateException("No op couldn't be executed", e);
                }
            }
        }
        if (!normalizedText.isEmpty() && searched != Boolean.FALSE) {
            final List<ISearchableComboItem<T>> cache = getCombo().getModelValues();
            // don't filter twice
            final ComboMatcher search = Boolean.TRUE.equals(searched) ? null : getCombo().getCompletionMode().matcher(normalizedText.toLowerCase());
            final int maximumResult = getCombo().getMaximumResult();

            for (int index = 0; index < cache.size(); index++) {
                final ISearchableComboItem<T> itemO = cache.get(index);
                final String item = itemO.asString();
                // On s'arrête au plus vite
                if (index % 50 == 0) {
                    if (isStopped()) {
                        return result;
                    }
                }
                // Recherche case insensitive
                boolean ok = search == null || search.match(item.toLowerCase());

                // FIXME: mettre dans les prefs removeDuplicate
                final boolean removeDuplicate = true;
                if (ok && removeDuplicate) {
                    for (int i = 0; i < result.size(); i++) {
                        if (isStopped()) {
                            return result;
                        }
                        final ISearchableComboItem<T> element = result.get(i);
                        if (element.asString().equalsIgnoreCase(item)) {
                            ok = false;
                            break;
                        }
                    }
                }

                if (ok) {
                    result.add(itemO);
                }

                if (result.size() > maximumResult) {
                    break;
                }
            }

        }

        return result;
    }

    static final List<String> cut(final String value) {
        final List<String> v = new ArrayList<String>();
        final StringTokenizer tokenizer = new StringTokenizer(value);
        while (tokenizer.hasMoreElements()) {
            final String element = (String) tokenizer.nextElement();
            v.add(element);
        }
        return v;
    }

}
