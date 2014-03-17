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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.ResourceBundle;

import net.jcip.annotations.ThreadSafe;

/**
 * A factory that creates a module that does nothing by default.
 * 
 * @author Sylvain
 */
@ThreadSafe
public class VirtualModuleFactory extends ModuleFactory {

    private final Map<Object, Dependency> depends;
    private final ListResourceBundle bundle;

    public VirtualModuleFactory(ModuleReference ref, String contact) {
        this(ref, contact, (List<Dependency>) null);
    }

    public VirtualModuleFactory(final String id, final ModuleVersion vers, final String contact) {
        this(id, vers, contact, Collections.<Dependency> emptyList());
    }

    public VirtualModuleFactory(final String id, final ModuleVersion vers, final String contact, final List<Dependency> depends) {
        this(new ModuleReference(id, vers), contact, depends);
    }

    public VirtualModuleFactory(final ModuleReference ref, final String contact, final List<Dependency> depends) {
        this(ref, contact, depends == null ? null : createMap(depends));
    }

    private VirtualModuleFactory(ModuleReference ref, String contact, Map<Object, Dependency> depends) {
        super(ref, contact);
        this.depends = depends == null ? Collections.<Object, Dependency> emptyMap() : Collections.unmodifiableMap(depends);
        this.bundle = new ListResourceBundle() {
            @Override
            protected Object[][] getContents() {
                return new Object[][] { { NAME_KEY, getID() },
                        //
                        { DESC_KEY, VirtualModuleFactory.this.getClass().getSimpleName() + " for " + getID() } };
            }
        };
    }

    @Override
    protected ResourceBundle getResourceBundle() {
        return this.bundle;
    }

    @Override
    protected final Map<Object, Dependency> getDependencies() {
        return this.depends;
    }

    @Override
    public AbstractModule createModule(File moduleDir, Map<Object, AbstractModule> alreadyCreated) throws Exception {
        return new AbstractModule(this) {
            @Override
            protected void stop() {
            }

            @Override
            protected void start() {
            }
        };
    }
}
