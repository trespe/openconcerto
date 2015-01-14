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
 
 package org.openconcerto.sql.utils;

import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.changer.Change;
import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.utils.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author Sylvain CUAZ
 */
public abstract class ChangeBase {

    private final DBRoot root;

    public ChangeBase(final DBRoot r) {
        this.root = r;
    }

    public ChangeBase() throws IOException {
        this(new PropsConfiguration(new File("changeBase.properties"), Change.props).getRoot());
    }

    protected final SQLSyntax getSyntax() {
        return SQLSyntax.get(this.getRoot());
    }

    protected final void call(String converter) {
        this.call(converter, new String[0]);
    }

    protected final void call(String[] nameNparams) {
        final String[] params = new String[nameNparams.length - 1];
        System.arraycopy(nameNparams, 1, params, 0, params.length);
        this.call(nameNparams[0], params);
    }

    /**
     * Appelle la méthode spécifiée.
     * 
     * @param converter la méthode à appeler.
     * @param params parameters to pass to the converter.
     */
    protected final void call(String converter, String[] params) {
        final Class<? extends Changer> c = this.getChange() == null ? null : this.getChange().findClass(converter);
        if (c != null) {
            try {
                this.getChange().exec(getRoot(), c, params);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            try {
                final Class[] types = new Class[params.length];
                Arrays.fill(types, String.class);
                this.call(this.getClass().getMethod(converter, types), params);
            } catch (NoSuchMethodException exn) {
                final String copies = CollectionUtils.join(Collections.nCopies(params.length, "String"), ", ");
                System.err.println("Le convertisseur '" + converter + "(" + copies + ")' n'existe pas.");
            }
        }
    }

    abstract protected Change getChange();

    private void call(Method converter, Object[] params) {
        try {
            converter.invoke(this, params);
        } catch (Exception exn) {
            exn.printStackTrace();
        }
    }

    protected final SQLBase getBase() {
        return this.getRoot().getBase();
    }

    protected final DBRoot getRoot() {
        return this.root;
    }
}
