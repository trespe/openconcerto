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

import java.io.IOException;

public abstract class AbstractModule {

    private final ModuleFactory factory;

    public AbstractModule(final ModuleFactory f) throws IOException {
        this.factory = f;
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

    protected void install() {

    }

    protected abstract void start();

    protected abstract void stop();

    protected void uninstall() {

    }
}
