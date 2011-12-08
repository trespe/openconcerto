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

import java.util.List;

// akin to Pattern
public interface SearchMode {

    // akin to Matcher
    public abstract class ComboMatcher {

        private final String search;

        public ComboMatcher(String s) {
            super();
            this.search = s;
        }

        public final String getSearch() {
            return this.search;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + " for " + this.getSearch();
        }

        // called for each combo item
        public abstract boolean match(String item);
    }

    // called for each search
    public ComboMatcher matcher(String search);

    static final class DefaultSearchMode implements SearchMode {
        private final boolean contains;

        DefaultSearchMode(boolean contains) {
            super();
            this.contains = contains;
        }

        @Override
        public ComboMatcher matcher(String s) {
            final List<String> values = ISearchableComboCompletionThread.cut(s);
            final int stop = values.size();
            return new ComboMatcher(s) {
                @Override
                public boolean match(String item) {
                    boolean ok = false;
                    for (int j = 0; j < stop; j++) {
                        final String lowerCaseValue = values.get(j);

                        if (DefaultSearchMode.this.contains) {
                            if (item.indexOf(lowerCaseValue) >= 0) {
                                // ajout a la combo");
                                ok = true;
                            } else {
                                ok = false;
                                break;
                            }
                        } else {
                            if (item.startsWith(lowerCaseValue)) {
                                // ajout a la combo");
                                ok = true;
                            } else {
                                ok = false;
                                break;
                            }
                        }
                    }
                    return ok;
                }
            };
        }
    }

}
