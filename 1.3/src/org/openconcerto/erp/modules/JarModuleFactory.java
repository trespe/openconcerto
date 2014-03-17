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

import org.openconcerto.erp.config.Log;

import java.awt.AWTPermission;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyPermission;
import java.util.jar.JarFile;

import net.jcip.annotations.ThreadSafe;

/**
 * A module factory created from a {@link ModulePackager packaged} module.
 * 
 * @author Sylvain CUAZ
 */
@ThreadSafe
public final class JarModuleFactory extends PropsModuleFactory {

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
        private final List<Permission> perms;
        private final ClassLoader[] dependencies;

        public ModuleClassLoader(File moduleDir, Map<Object, AbstractModule> alreadyCreated) {
            super(getClassPath(), JarModuleFactory.class.getClassLoader());
            this.perms = new ArrayList<Permission>();
            this.perms.add(new PropertyPermission("*", "read"));
            final String absolutePath = moduleDir.getAbsolutePath().endsWith(File.separator) ? moduleDir.getAbsolutePath() : moduleDir.getAbsolutePath() + File.separator;
            this.perms.add(new FilePermission(absolutePath + "-", "read,write,delete"));
            // do not display a warning sign for each window created by a module
            this.perms.add(new AWTPermission("showWindowWithoutWarningBanner"));
            // URLClassLoader treats classes and resources differently, so that a class can be
            // allowed but a call to getResource() in it will be denied. This is because findClass()
            // only uses a doPrivileged(), whereas findResource() also checks the current protection
            // domains.
            // So we need to add all jars in the class path (we could also replace the system class
            // loader with "java.system.class.loader")
            ClassLoader current = getParent();
            while (current != null) {
                loadPerms(current);
                current = current.getParent();
            }

            this.dependencies = new ClassLoader[getDependencies().size()];
            int i = 0;
            for (final Object requiredID : getDependencies().keySet()) {
                final AbstractModule m = alreadyCreated.get(requiredID);
                if (m == null)
                    throw new IllegalStateException("Missing required module : " + requiredID);
                this.dependencies[i++] = m.getClass().getClassLoader();
            }
        }

        private final void loadPerms(final ClassLoader cl) {
            if (cl instanceof URLClassLoader) {
                for (final URL url : ((URLClassLoader) cl).getURLs()) {
                    final Enumeration<Permission> permissions = super.getPermissions(new CodeSource(url, (Certificate[]) null)).elements();
                    while (permissions.hasMoreElements())
                        this.perms.add(permissions.nextElement());
                }
            } else {
                Log.get().warning("Unknown type of class loader : " + cl + ", cannot be sure that this protection domain will be allowed to read resources from it");
            }
        }

        @Override
        protected PermissionCollection getPermissions(CodeSource codesource) {
            final PermissionCollection res = super.getPermissions(codesource);
            for (final Permission perm : this.perms) {
                res.add(perm);
            }
            return res;
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
            // File is immutable and URI is stack confined
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
    public AbstractModule createModule(final File moduleDir, final Map<Object, AbstractModule> alreadyCreated) throws Exception {
        return createModule(new ModuleClassLoader(moduleDir, alreadyCreated).loadClass(this.getMainClass()), moduleDir);
    }

    @Override
    public String toString() {
        return super.toString() + " from " + this.jar;
    }
}
