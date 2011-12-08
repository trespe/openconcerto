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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;

/**
 * A module factory created from a {@link ModulePackager packaged} module.
 * 
 * @author Sylvain CUAZ
 */
public final class JarModuleFactory extends ModuleFactory {

    private static Properties getProperties(final File jar) throws IOException {
        final JarFile jarFile = new JarFile(jar);
        return readAndClose(jarFile.getInputStream(jarFile.getEntry(ModulePackager.MODULE_PROPERTIES_PATH)));
    }

    /**
     * A class loader that also search dependent modules.
     * 
     * @author Sylvain CUAZ
     */
    private final class ModuleClassLoader extends URLClassLoader {
        private final ClassLoader[] dependencies;

        public ModuleClassLoader(Map<String, AbstractModule> alreadyCreated) {
            super(getClassPath(), JarModuleFactory.class.getClassLoader());
            this.dependencies = new ClassLoader[getRequiredIDs().size()];
            int i = 0;
            for (final String requiredID : getRequiredIDs()) {
                final AbstractModule m = alreadyCreated.get(requiredID);
                if (m == null)
                    throw new IllegalStateException("Missing required module : " + requiredID);
                this.dependencies[i++] = m.getClass().getClassLoader();
            }
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            try {
                return super.loadClass(name, resolve);
            } catch (ClassNotFoundException e) {
                // OK, we'll search our dependencies
            }
            for (final ClassLoader cl : this.dependencies) {
                try {
                    final Class<?> res = cl.loadClass(name);
                    if (resolve)
                        resolveClass(res);
                    return res;
                } catch (ClassNotFoundException e) {
                    // OK, we'll search the rest of our dependencies
                }
            }
            throw new ClassNotFoundException(name);
        }
    }

    private final File jar;

    public JarModuleFactory(final File jar) throws IOException {
        super(getProperties(jar));
        this.jar = jar;
    }

    protected final URL[] getClassPath() {
        try {
            return new URL[] { this.jar.toURI().toURL() };
        } catch (MalformedURLException e) {
            // shouldn't happen since we create the URL from an URI
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected ClassLoader getRsrcClassLoader() {
        return new URLClassLoader(getClassPath()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                throw new ClassNotFoundException(name + " is hidden from this loader, it only loads properties");
            }
        };
    }

    @Override
    public AbstractModule createModule(Map<String, AbstractModule> alreadyCreated) throws Exception {
        return createModule(new ModuleClassLoader(alreadyCreated).loadClass(this.getMainClass()));
    }

    @Override
    public String toString() {
        return super.toString() + " from " + this.jar;
    }
}
