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
 
 package org.openconcerto.erp.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ModuleReference {
    private final String id;
    private final ModuleVersion version;

    public ModuleReference(String id, ModuleVersion version) {
        this.id = id;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public ModuleVersion getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return this.id + " " + this.version;
    }

    @Override
    public int hashCode() {
        return this.version.hashCode() + id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ModuleReference) {
            final ModuleReference ref = (ModuleReference) obj;
            return ref.id.equals(id) && ref.version.equals(version);
        }
        return super.equals(obj);
    }

    public static ModuleVersion getVersion(List<ModuleReference> refs, String id) {
        for (ModuleReference moduleReference : refs) {
            if (moduleReference.getId().equals(id)) {
                return moduleReference.getVersion();
            }
        }
        return null;
    }

    public static List<String> getIds(List<ModuleReference> refs) {
        final List<String> ids = new ArrayList<String>();
        for (ModuleReference moduleReference : refs) {
            if (!ids.contains(moduleReference.getId())) {
                ids.add(moduleReference.getId());
            }
        }
        return ids;
    }
}
