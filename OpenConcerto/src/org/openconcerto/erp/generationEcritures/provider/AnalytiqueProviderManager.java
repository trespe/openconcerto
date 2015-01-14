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
 
 package org.openconcerto.erp.generationEcritures.provider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AnalytiqueProviderManager {
    private final static AnalytiqueProviderManager instance = new AnalytiqueProviderManager();
    private final Map<String, AnalytiqueProvider> map = new HashMap<String, AnalytiqueProvider>();

    public static void put(String id, AnalytiqueProvider provider) {
        instance.putProvider(id, provider);
    }

    public static AnalytiqueProvider get(String id) {
        return instance.getProvider(id);
    }

    public static Collection<AnalytiqueProvider> getAll() {
        return instance.getAllProvider();
    }

    private synchronized void putProvider(String id, AnalytiqueProvider provider) {
        map.put(id, provider);

    }

    private synchronized AnalytiqueProvider getProvider(String id) {
        return map.get(id);
    }

    private synchronized Collection<AnalytiqueProvider> getAllProvider() {
        return map.values();
    }
}
