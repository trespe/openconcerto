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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.SwingUtilities;

public class ISearchableComboCompletionThread<T> extends Thread {
    private final ISearchableCombo<T> combo;
    private final boolean showAll;
    private final String t;
    private boolean stopNow;

    public ISearchableComboCompletionThread(final ISearchableCombo<T> combo, final boolean showAll, final String t) {
        this.combo = combo;
        this.showAll = showAll;
        this.t = t;
        this.stopNow = false;
    }

    private ISearchableCombo<T> getCombo() {
        return this.combo;
    }

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
        final List<ISearchableComboItem<T>> l;
        if (!this.showAll) {
            l = getMatchingValues();
        } else {
            l = getMaxValues();
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (isStopped()) {
                    return;
                }
                getCombo().setMatchingCompletions(l, ISearchableComboCompletionThread.this.showAll);
            }
        });

    }

    private List<ISearchableComboItem<T>> getMaxValues() {
        final List<ISearchableComboItem<T>> allVals = this.getCombo().getModelValues();
        return allVals.subList(0, Math.min(this.getCombo().getMaximumResult(), allVals.size()));
    }

    private List<ISearchableComboItem<T>> getMatchingValues() {
        final List<ISearchableComboItem<T>> result = new ArrayList<ISearchableComboItem<T>>();

        final String aText = this.t.trim().toLowerCase();

        final int minimumSearch = getCombo().getMinimumSearch();

        if (aText.length() >= minimumSearch) {
            final List<ISearchableComboItem<T>> cache = getCombo().getModelValues();
            final ComboMatcher search = getCombo().getCompletionMode().matcher(aText);
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
                boolean ok = search.match(item.toLowerCase());

                // FIXME: mettre dans les prefs removeDuplicate
                boolean removeDuplicate = true;
                if (ok && removeDuplicate) {
                    for (int i = 0; i < result.size(); i++) {
                        if (isStopped()) {
                            return result;
                        }
                        ISearchableComboItem element = result.get(i);
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
            String element = (String) tokenizer.nextElement();
            v.add(element);
        }
        return v;
    }

}
