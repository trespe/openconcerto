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

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;

import java.util.Collections;
import java.util.List;

public abstract class ModuleElement extends ComptaSQLConfElement {

    private final AbstractModule module;

    public ModuleElement(AbstractModule module, String tableName) {
        // needs DEFERRED since createCode() uses module
        super(tableName, DEFERRED_CODE);
        this.module = module;
        // translations should be alongside the module
        this.setL18nLocation(module.getClass());
        // allow to access labels right away (see ModuleManager.registerSQLElements())
        for (final String id : this.getAdditionalIDsForMDPath()) {
            this.addToMDPath(ModuleManager.getMDVariant(new ModuleReference(id, null)));
        }
        this.addToMDPath(ModuleManager.getMDVariant(this.getFactory()));
    }

    /**
     * Additional modules to use for #getMDPath(). For this method to be useful, the returned
     * modules must contain an element with same {@link #getCode() code} than this. E.g. this
     * element adds some fields and modify some labels, but still want the default labels for other
     * fields.
     * 
     * @return module IDs.
     */
    protected List<String> getAdditionalIDsForMDPath() {
        return Collections.emptyList();
    }

    public final ModuleFactory getFactory() {
        return this.module.getFactory();
    }

    // This implementation returns a unique code to allow for different modules to use the same
    // table differently (at different times of course). Not final to allow subclasses to define a
    // code following some rules (https://code.google.com/p/openconcerto/wiki/Modules) or to share
    // code, see SQLElement#getCode()
    @Override
    protected String createCode() {
        return this.module.getFactory().getID() + '/' + createCodeSuffix();
    }

    protected String createCodeSuffix() {
        return getTable().getName();
    }
}
