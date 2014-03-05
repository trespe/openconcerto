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

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.preferences.SQLPreferences;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import net.jcip.annotations.ThreadSafe;

/**
 * Allow to create modules.
 * 
 * @author Sylvain CUAZ
 */
@ThreadSafe
public abstract class ModuleFactory {

    // create an ID for each dependency
    static protected final Map<Object, Dependency> createMap(List<Dependency> l) {
        if (l == null || l.size() == 0)
            return Collections.<Object, Dependency> emptyMap();
        // be predictable, keep order
        final Map<Object, Dependency> res = new LinkedHashMap<Object, Dependency>(l.size());
        for (final Dependency d : l) {
            res.put(String.valueOf(res.size()), d);
        }
        return res;
    }

    public static final String NAME_KEY = "name";
    public static final String DESC_KEY = "description";

    private final ModuleReference ref;
    // TODO add moduleAPIVersion;
    private final String contact;

    protected ModuleFactory(final ModuleReference ref, final String contact) {
        if (ref.getVersion() == null)
            throw new IllegalArgumentException("No version " + ref);
        this.ref = ref;
        this.contact = contact;
    }

    public final ModuleReference getReference() {
        return this.ref;
    }

    public final String getID() {
        return this.getReference().getID();
    }

    public final String getContact() {
        return this.contact;
    }

    public final ModuleVersion getVersion() {
        return this.getReference().getVersion();
    }

    public final int getMajorVersion() {
        return this.getVersion().getMajor();
    }

    public final int getMinorVersion() {
        return this.getVersion().getMinor();
    }

    // should be immutable
    protected abstract Map<Object, Dependency> getDependencies();

    public final boolean conflictsWith(final ModuleFactory f) {
        // a module can only be installed in one version
        if (this.getID().equals(f.getID()))
            return !this.equals(f);
        else
            return this.conflictsWithOtherID(f) || f.conflictsWithOtherID(this);
    }

    // e.g. two different modules want to use the same table
    protected boolean conflictsWithOtherID(final ModuleFactory f) {
        return false;
    }

    public final boolean conflictsWith(final Collection<ModuleFactory> factories) {
        boolean res = false;
        final Iterator<ModuleFactory> iter = factories.iterator();
        while (iter.hasNext() && !res) {
            final ModuleFactory f = iter.next();
            res = this.conflictsWith(f);
        }
        return res;
    }

    protected abstract ResourceBundle getResourceBundle();

    public final String getName() {
        return this.getResourceBundle().getString(NAME_KEY);
    }

    public final String getDescription() {
        return this.getResourceBundle().getString(DESC_KEY);
    }

    /**
     * Create a module.
     * 
     * @param moduleDir the directory the module can write to.
     * @param alreadyCreated the already created modules for each dependency.
     * @return a new instance.
     * @throws Exception if the module couldn't be created.
     */
    public abstract AbstractModule createModule(final File moduleDir, final Map<Object, AbstractModule> alreadyCreated) throws Exception;

    // not sure if Class or Constructor are thread-safe
    protected synchronized final AbstractModule createModule(final Class<?> c, final File localDir) throws Exception {
        final AbstractModule res = (AbstractModule) c.getConstructor(ModuleFactory.class).newInstance(this);
        res.setLocalDirectory(localDir);
        return res;
    }

    public final Preferences getLocalPreferences() {
        return this.getPreferences(true, null);
    }

    public final Preferences getSQLPreferences(final DBRoot root) {
        return this.getPreferences(false, root);
    }

    public final Preferences getPreferences(final boolean local, final DBRoot root) {
        final Preferences rootPrefs = local ? Preferences.userRoot() : new SQLPreferences(root);
        // ID is a package name, transform to path to avoid bumping into the size limit
        return rootPrefs.node(ModulePreferencePanel.getAppPrefPath() + this.getID().replace('.', '/'));
    }

    @Override
    public String toString() {
        final String className = getClass().isAnonymousClass() ? getClass().getName() : getClass().getSimpleName();
        assert className.length() > 0;
        return className + " " + getID() + " (" + getMajorVersion() + "." + getMinorVersion() + ")";
    }
}
