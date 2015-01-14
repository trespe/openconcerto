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
 
 package org.openconcerto.utils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

// syntactic sugar to avoid repeating V
public class LinkedListMap<K, V> extends ListAbstractMap<K, LinkedList<V>, V> {

    public LinkedListMap() {
        super();
    }

    public LinkedListMap(Map<? extends K, ? extends Collection<? extends V>> m) {
        super(m);
    }

    @Override
    public LinkedList<V> createCollection(Collection<? extends V> v) {
        return new LinkedList<V>(v);
    }
}
