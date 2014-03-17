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

import org.openconcerto.utils.CompareUtils;

import java.beans.DefaultPersistenceDelegate;
import java.beans.PersistenceDelegate;
import java.util.Comparator;
import java.util.regex.Pattern;

import net.jcip.annotations.Immutable;

@Immutable
public class ModuleReference {

    static public final PersistenceDelegate PERSIST_DELEGATE = new DefaultPersistenceDelegate(new String[] { "ID", "version" });

    static public final Comparator<ModuleReference> COMP_ID_ASC_VERSION_DESC = new Comparator<ModuleReference>() {
        @Override
        public int compare(ModuleReference o1, ModuleReference o2) {
            int res = o1.getID().compareTo(o2.getID());
            if (res != 0)
                return res;
            else
                return -o1.getVersion().compareTo(o2.getVersion());
        }
    };

    static String checkMatch(final Pattern p, final String s, final String name) {
        if (!p.matcher(s).matches())
            throw new IllegalArgumentException(name + " doesn't match " + p.pattern());
        return s;
    }

    static final Pattern javaIdentifiedPatrn = Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");
    static final Pattern qualifiedPatrn = Pattern.compile(javaIdentifiedPatrn.pattern() + "(\\." + javaIdentifiedPatrn.pattern() + ")*");

    static final Pattern idPatrn = qualifiedPatrn;

    private final String id;
    private final ModuleVersion version;

    /**
     * Create a new instance.
     * 
     * @param id module ID, cannot be <code>null</code>.
     * @param version module version, can be <code>null</code>.
     */
    public ModuleReference(String id, ModuleVersion version) {
        if (id == null)
            throw new NullPointerException();
        this.id = checkMatch(idPatrn, id.trim(), "ID");
        this.version = version;
    }

    public final String getID() {
        return this.id;
    }

    @Deprecated
    public final String getId() {
        return this.getID();
    }

    public final ModuleVersion getVersion() {
        return this.version;
    }

    @Override
    public String toString() {
        return "Module " + this.id + " " + (this.version == null ? "no version" : this.version);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id.hashCode();
        result = prime * result + (this.version == null ? 0 : this.version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ModuleReference other = (ModuleReference) obj;
        return this.id.equals(other.id) && CompareUtils.equals(this.version, other.version);
    }
}
