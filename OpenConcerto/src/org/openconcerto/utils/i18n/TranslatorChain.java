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
 
 package org.openconcerto.utils.i18n;

import java.util.ArrayList;
import java.util.List;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public final class TranslatorChain implements Translator {

    private final List<Translator> l;

    public TranslatorChain(final List<? extends Translator> l) {
        super();
        this.l = new ArrayList<Translator>(l);
    }

    @Override
    public String translate(String key, MessageArgs args) {
        String res = null;
        final int size = this.l.size();
        for (int i = 0; i < size && res == null; i++) {
            res = this.l.get(i).translate(key, args);
        }
        return res;
    }
}
