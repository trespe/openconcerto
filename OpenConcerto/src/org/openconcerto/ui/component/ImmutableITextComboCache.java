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

import java.util.Arrays;
import java.util.List;

public class ImmutableITextComboCache implements ITextComboCache {

    private final List<String> cache;

    public ImmutableITextComboCache(final String... cache) {
        this(Arrays.asList(cache));
    }

    public ImmutableITextComboCache(final List<String> cache) {
        this.cache = cache;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public final void addToCache(String string) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void deleteFromCache(String string) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final List<String> getCache() {
        return this.loadCache(true);
    }

    @Override
    public List<String> loadCache(final boolean dsCache) {
        return this.cache;
    }
}
