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
 
 package org.openconcerto.erp.storage;

import java.util.ArrayList;
import java.util.List;

public class StorageEngines {
    private static final StorageEngines instance = new StorageEngines();

    public static StorageEngines getInstance() {
        return instance;
    }

    private List<StorageEngine> engines = new ArrayList<StorageEngine>();

    public List<StorageEngine> getActiveEngines() {
        // TODO use a map to store active engines;
        return engines;
    }

    public void addEngine(StorageEngine e) {
        engines.add(e);
    }

    public void removeEngine(StorageEngine e) {
        engines.remove(e);
    }
}
