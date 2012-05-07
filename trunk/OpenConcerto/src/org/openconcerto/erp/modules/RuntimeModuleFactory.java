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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import net.jcip.annotations.ThreadSafe;

/**
 * A module factory which classes are already in the classpath.
 * 
 * @author Sylvain CUAZ
 */
@ThreadSafe
public final class RuntimeModuleFactory extends ModuleFactory {

    public RuntimeModuleFactory(final File props) throws IOException {
        this(readAndClose(new FileInputStream(props)));
    }

    public RuntimeModuleFactory(final Properties props) throws IOException {
        super(props);
    }

    @Override
    protected ClassLoader getRsrcClassLoader() {
        return this.getClass().getClassLoader();
    }

    @Override
    public AbstractModule createModule(Map<String, AbstractModule> alreadyCreated) throws Exception {
        return createModule(Class.forName(getMainClass()));
    }
}
