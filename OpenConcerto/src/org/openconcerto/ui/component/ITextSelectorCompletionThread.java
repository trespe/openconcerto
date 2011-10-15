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
 
 package org.openconcerto.ui.component;

import org.openconcerto.ui.DefaultListModel;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.SwingUtilities;

public class ITextSelectorCompletionThread extends Thread {
    private final ITextSelector combo;
    private final boolean showAll;
    private final String t;
    private boolean stopNow;

    public ITextSelectorCompletionThread(final ITextSelector combo, final boolean showAll, final String t) {
        this.combo = combo;
        this.showAll = showAll;
        this.t = t;
        this.stopNow = false;
    }

    public void run() {
        computeAutoCompletion();
    }

    public synchronized void stopNow() {
        this.stopNow = true;
    }

    private synchronized boolean isStopped() {
        return stopNow;
    }

    private void computeAutoCompletion() {
        final List<String> l;
        if (!showAll) {
            l = getPossibleValues(); // Liste de IComboSelection
        } else {
            l = getFirsValues();// combo.getCache();
        }

        final DefaultListModel model = combo.getModel();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                if (isStopped()) {
                    return;
                }

                // on vide le model
                model.removeAllElements();
                model.addAll(l);
                final String trimedText = t.trim();
                final int size = l.size();
                if (size > 0) {
                    boolean match = false;
                    for (int i = 0; i < size; i++) {
                        if (l.get(i).trim().equalsIgnoreCase(trimedText)) {
                            match = true;
                        }
                    }
                    if (match) {
                        combo.hideCompletionPopup();
                    } else {
                        combo.showCompletionPopup();
                    }
                } else {
                    combo.hideCompletionPopup();
                }

            }
        });

    }

    private List<String> getFirsValues() {
        final List<String> result = new ArrayList<String>();
        final List cache = combo.getCache();
        final int maximumResult = combo.getMaximumResult();
        for (int index = 0; index < cache.size(); index++) {
            final String item = (String) cache.get(index);

            result.add(item);

            if (result.size() > maximumResult) {
                break;
            }
        }
        return result;
    }

    /**
     * Retourne une liste de IComboSelectionItem, qui sont les selections possibles pour le text
     * passé
     */
    private List<String> getPossibleValues() {
        final List<String> result = new ArrayList<String>();

        final String aText = t.trim().toLowerCase();

        final int minimumSearch = combo.getMinimumSearch();

        if (aText.length() >= minimumSearch) {
            final List<String> values = cut(aText);
            final int stop = values.size();
            final List cache = combo.getCache();
            final int completionMode = combo.getCompletionMode();
            final int maximumResult = combo.getMaximumResult();

            for (int index = 0; index < cache.size(); index++) {
                final String item = (String) cache.get(index);
                boolean ok = false;
                // On s'arrête au plus vite
                if (index % 50 == 0) {
                    if (isStopped()) {
                        return result;
                    }
                }
                // Recherche case insensitive
                final String lowerCaseItem = item.toLowerCase();

                for (int j = 0; j < stop; j++) {
                    final String lowerCaseValue = values.get(j).toLowerCase();

                    if (completionMode == ITextSelector.MODE_CONTAINS) {

                        if (lowerCaseItem.indexOf(lowerCaseValue) >= 0) {
                            // ajout a la combo");
                            ok = true;
                        } else {
                            ok = false;
                            break;
                        }
                    } else {
                        if (lowerCaseItem.startsWith(lowerCaseValue)) {
                            // ajout a la combo");
                            ok = true;
                        } else {
                            ok = false;
                            break;
                        }
                    }
                }
                // FIXME: mettre dans les prefs removeDuplicate
                boolean removeDuplicate = true;
                if (ok && removeDuplicate) {
                    for (int i = 0; i < result.size(); i++) {
                        if (isStopped()) {
                            return result;
                        }
                        String element = result.get(i);
                        if (element.equalsIgnoreCase(item)) {
                            ok = false;
                            break;
                        }
                    }
                }

                if (ok) {
                    result.add(item);
                }

                if (result.size() > maximumResult) {
                    break;
                }
            }

        }

        return result;
    }

    private static final List<String> cut(final String value) {
        final List<String> v = new ArrayList<String>();
        final StringTokenizer tokenizer = new StringTokenizer(value);
        while (tokenizer.hasMoreElements()) {
            String element = (String) tokenizer.nextElement();
            v.add(element);
        }
        return v;
    }
}
