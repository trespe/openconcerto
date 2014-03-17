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

import java.util.List;

public interface ITextComboCache {

    /**
     * Can this cache be used.
     * 
     * @return <code>true</code> if this cache can be used.
     */
    public boolean isValid();

    /**
     * Force le chargement du cache (en synchrone) et le renvoi
     */
    public List<String> loadCache(final boolean readCache);

    /**
     * Retourne les éléments du cache, et le charge de manière synchrone si n'a jamais été chargé
     */
    public List<String> getCache();

    /**
     * Ajoute un élément au cache s'il n'existe pas déja
     */
    public void addToCache(String string);

    /**
     * Efface un élément du cache
     */
    public void deleteFromCache(String string);

}
