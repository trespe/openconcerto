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
 
 package org.openconcerto.sql.changer;

import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.model.DBStructureItem;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.utils.ProductInfo;
import org.openconcerto.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public abstract class Change {

    static public final Properties props;
    static {
        props = new Properties();
        props.put(PropsConfiguration.JDBC_CONNECTION + "allowMultiQueries", "true");
    }

    protected final void exec(String... nameNparams) throws SQLException, IOException {
        this.exec((DBStructureItem<?>) null, nameNparams);
    }

    protected final void exec(final DBStructureItem<?> root, String... nameNparams) throws SQLException, IOException {
        if (nameNparams.length == 0)
            throw new IllegalArgumentException("usage: " + this.getClass().getName() + " changer params...");

        final String[] params = new String[nameNparams.length - 1];
        System.arraycopy(nameNparams, 1, params, 0, params.length);
        this.exec(root, nameNparams[0], params);
    }

    // changerName can be a full class name (in this case it has to be in package) or simple class
    // name (in that case the packages returned by getPackages() will be searched).
    // params are SQLNames relative to this.root
    private final void exec(final DBStructureItem<?> root, String changerName, String... params) throws SQLException, IOException {
        final Class<? extends Changer> changer = this.findClass(changerName);
        if (changer == null)
            throw new IllegalArgumentException(changerName + " not found.");

        // allow to use SQL structure cache
        if (ProductInfo.getInstance() == null) {
            ProductInfo.setInstance(new ProductInfo(changer.getName()));
        }

        if (root == null)
            exec(changer, params);
        else
            exec(root, changer, params);
    }

    public void exec(final Class<? extends Changer> changer, String... params) throws IOException, SQLException {
        final String pathname = System.getProperty("confFile", "changeBase.properties");
        this.exec(new PropsConfiguration(new File(pathname), props).getRoot(), changer, params);
    }

    public void exec(final DBStructureItem<?> root, final Class<? extends Changer> changer, String... params) throws SQLException {
        if (params.length == 0) {
            Changer.change(root, changer);
        } else {
            final List<SQLName> names = new ArrayList<SQLName>(params.length);
            final Set<String> children = new HashSet<String>();
            for (final String name : params) {
                final SQLName n = SQLName.parse(name);
                names.add(n);
                children.add(n.getFirst());
            }
            if (root instanceof DBSystemRoot) {
                final DBSystemRoot sysRoot = (DBSystemRoot) root;
                if (!sysRoot.getChildrenNames().containsAll(children))
                    sysRoot.addRoots(new ArrayList<String>(children));
            }
            for (final SQLName name : names)
                Changer.change(root.getDescendant(name), changer);
        }
    }

    public final Class<? extends Changer> findClass(final String converter) {
        if (converter.indexOf('.') > 0) {
            return findClassFromFullName(converter);
        } else {
            final String normalized = StringUtils.firstUp(converter);
            Class<? extends Changer> res = null;
            for (final String pkg : this.getPackages()) {
                res = this.findClass(pkg, normalized);
                if (res != null)
                    return res;
            }
            return null;
        }
    }

    protected List<String> getPackages() {
        final List<String> res = new ArrayList<String>();
        Class<?> c = this.getClass();
        // this is a subclass of Change
        while (c != Change.class) {
            res.add(c.getPackage().getName() + "." + c.getSimpleName().toLowerCase());
            c = c.getSuperclass();
        }
        return res;
    }

    protected final Class<? extends Changer> findClass(String pkgName, final String converter) {
        return this.findClassFromFullName(pkgName + '.' + converter);
    }

    protected final Class<? extends Changer> findClassFromFullName(String className) {
        try {
            final Class<?> c = Class.forName(className);
            return c.asSubclass(Changer.class);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (NoClassDefFoundError e) {
            // java on Windows thinks pkg.myClass exists by looking at the fs
            // but it was looking at pkg.MyClass.class
            return null;
        }
    }

}
