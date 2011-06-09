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
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class Change {

    static public final Properties props;
    static {
        props = new Properties();
        props.put(PropsConfiguration.JDBC_CONNECTION + "allowMultiQueries", "true");
    }

    protected final DBRoot root;

    protected Change(final DBRoot root) {
        this.root = root;
    }

    public Change() throws IOException {
        this(new PropsConfiguration(new File("changeBase.properties"), props).getRoot());
    }

    protected final DBRoot getRoot() {
        return this.root;
    }

    protected final SQLBase getBase() {
        return this.getRoot().getBase();
    }

    protected final void exec(String... nameNparams) throws SQLException {
        if (nameNparams.length == 0)
            throw new IllegalArgumentException("usage: " + this.getClass().getName() + " changer params...");

        final String[] params = new String[nameNparams.length - 1];
        System.arraycopy(nameNparams, 1, params, 0, params.length);
        this.exec(nameNparams[0], params);
    }

    protected final void exec(String changerName, String... params) throws SQLException {
        final Class<? extends Changer> changer = this.findClass(changerName);
        if (changer == null)
            throw new IllegalArgumentException(changerName + " not found.");

        exec(changer, params);
    }

    public void exec(final Class<? extends Changer> changer, String... params) throws SQLException {
        if (params.length == 0)
            Changer.change(this.root, changer);
        else
            for (final String table : params)
                Changer.change(this.root.getTable(table), changer);
    }

    public final Class<? extends Changer> findClass(final String converter) {
        final String normalized = StringUtils.firstUp(converter);
        Class<? extends Changer> res = null;
        for (final String pkg : this.getPackages()) {
            res = this.findClass(pkg, normalized);
            if (res != null)
                return res;
        }
        return null;
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
        try {
            final Class<?> c = Class.forName(pkgName + '.' + converter);
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
