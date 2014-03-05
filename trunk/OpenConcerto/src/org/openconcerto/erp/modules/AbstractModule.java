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

import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.DBRoot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractModule {

    private final ModuleFactory factory;
    private File localDir;

    public AbstractModule(final ModuleFactory f) throws IOException {
        this.factory = f;
        this.localDir = null;
    }

    public final ModuleFactory getFactory() {
        return this.factory;
    }

    /**
     * The name presented in the UI.
     * 
     * @return an arbitrary name to display.
     */
    public final String getName() {
        return this.getFactory().getName();
    }

    public final String getDescription() {
        return this.getFactory().getDescription();
    }

    public final int getVersion() {
        return this.getFactory().getMajorVersion();
    }

    final void setLocalDirectory(final File f) {
        if (f == null)
            throw new NullPointerException("Null dir");
        if (this.localDir != null)
            throw new IllegalStateException("Already set to " + this.localDir);
        this.localDir = f;
    }

    /**
     * The directory this module should use while running. During installation use
     * {@link DBContext#getLocalDirectory()}.
     * 
     * @return the directory for this module.
     */
    protected final File getLocalDirectory() {
        return this.localDir;
    }

    /**
     * Should create permanent items. NOTE: all structure items created through <code>ctxt</code>
     * will be dropped automatically, and similarly all files created in
     * {@link DBContext#getLocalDirectory()} will be deleted automatically, i.e. no action is
     * necessary in {@link #uninstall(DBRoot)}.
     * 
     * @param ctxt to create database objects.
     */
    protected void install(DBContext ctxt) {

    }

    /**
     * Should add elements for the tables of this module. It's also the place to
     * {@link SQLElement#setAction(String, SQLElement.ReferenceAction) set actions} for foreign keys
     * of this module. NOTE: this method is called as long as the module is installed in the
     * database, even if it is stopped.
     * 
     * @param dir the directory where to add elements.
     */
    protected void setupElements(SQLElementDirectory dir) {
    }

    protected void setupMenu(MenuContext menuContext) {
    }

    /**
     * Called before start() to add fields to {@link SQLComponent}.
     * 
     * @param ctxt context to modify sql components.
     */
    protected void setupComponents(ComponentsContext ctxt) {
    }

    protected abstract void start();

    public List<ModulePreferencePanelDesc> getPrefDescriptors() {
        return Collections.emptyList();
    }

    public final Map<Boolean, List<ModulePreferencePanelDesc>> getPrefDescriptorsByLocation() {
        final Map<Boolean, List<ModulePreferencePanelDesc>> res = new HashMap<Boolean, List<ModulePreferencePanelDesc>>();
        for (final ModulePreferencePanelDesc desc : getPrefDescriptors()) {
            final Boolean key = desc.isLocal();
            final List<ModulePreferencePanelDesc> l;
            if (!res.containsKey(key)) {
                l = new ArrayList<ModulePreferencePanelDesc>();
                res.put(key, l);
            } else {
                l = res.get(key);
            }
            l.add(desc);
        }
        return res;
    }

    protected abstract void stop();

    protected void uninstall(DBRoot root) {

    }
}
