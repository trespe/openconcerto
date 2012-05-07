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

import org.openconcerto.sql.model.DBStructureItem;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.utils.ReflectUtils;
import org.openconcerto.utils.StreamUtils;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.EnumSet;

/**
 * A class the change an item of a database (eg delete archived rows).
 * 
 * @author Sylvain
 * 
 * @param <L> the type of item this operates on, eg SQLTable.
 */
public abstract class Changer<L extends DBStructureItem> {

    public static final void change(DBStructureItem<?> root, Class<? extends Changer> clazz) throws SQLException {
        final DBSystemRoot base = root.getAnc(DBSystemRoot.class);
        final Constructor<? extends Changer> ctor;
        try {
            ctor = clazz.getConstructor(DBSystemRoot.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(clazz + " doesn't have a DBSystemRoot constructor.");
        }
        Changer<?> changer;
        try {
            changer = ctor.newInstance(base);
        } catch (Exception e) {
            throw new IllegalArgumentException(ctor + " couldn't create a new intance with " + base, e);
        }
        changer.setUpFromSystemProperties();
        changer.changeAll(root);
    }

    private final DBSystemRoot base;
    private PrintStream stream;
    private boolean quiet;

    public Changer(DBSystemRoot b) {
        if (!this.getCompatibleSystems().contains(b.getServer().getSQLSystem()))
            throw new IllegalArgumentException(b + " of " + b.getServer() + " doesn't belong to " + this.getCompatibleSystems());

        this.base = b;
        this.setQuiet(false);
    }

    public void setUpFromSystemProperties() {
        this.setQuiet(Boolean.getBoolean("org.openconcerto.sql.changer.quiet"));
    }

    public final DBSystemRoot getSystemRoot() {
        return this.base;
    }

    public final boolean isQuiet() {
        return this.quiet;
    }

    public final void setQuiet(boolean quiet) {
        this.quiet = quiet;
        if (this.quiet)
            this.stream = new PrintStream(StreamUtils.NULL_OS);
        else
            this.stream = System.err;
    }

    public final void change(L root) throws SQLException {
        this.changeAll(root);
    }

    public final void changeAll(DBStructureItem<?> root) throws SQLException {
        if (!root.getAnc(DBSystemRoot.class).equals(this.getSystemRoot()))
            throw new IllegalArgumentException(root + " not in " + this.getSystemRoot());
        if (getMinLevel() != null && root.getHopsTo(this.getMinLevel()) < 0)
            throw new IllegalArgumentException(root + " must be at least " + this.getMinLevel());
        if (getMaxLevel() != null && root.getHopsTo(this.getMaxLevel()) > 0)
            throw new IllegalArgumentException(root + " must be at most " + this.getMaxLevel());

        getStream().println("Beginning " + this);
        long t1 = System.currentTimeMillis();
        try {
            for (final L t : root.getDescs(this.getL())) {
                this.changeImpl(t);
            }
        } finally {
            long t2 = System.currentTimeMillis();
            getStream().println("Done " + this + " in " + (t2 - t1) + "ms.");
        }
    }

    @SuppressWarnings("unchecked")
    private final Class<L> getL() {
        return (Class<L>) ReflectUtils.getTypeArguments(this.getClass(), Changer.class).get(0);
    }

    // *** for subclasses

    // * overridable

    protected abstract void changeImpl(L input) throws SQLException;

    protected EnumSet<SQLSystem> getCompatibleSystems() {
        return EnumSet.allOf(SQLSystem.class);
    }

    /**
     * The minimum level at which this changer should be applied. For example this could be a
     * changer at the table level, but always to be applied to all tables of a root.
     * 
     * @return the level, eg DBRoot.class.
     */
    protected Class<? extends DBStructureItem<?>> getMinLevel() {
        return null;
    }

    /**
     * The maximum level at which this changer should be applied. For example this could be a
     * changer at the table level, which can only be applied to a specific table at a time.
     * 
     * @return the level, eg SQLTable.class.
     */
    protected Class<? extends DBStructureItem<?>> getMaxLevel() {
        return null;
    }

    // * getters

    protected final PrintStream getStream() {
        return this.stream;
    }

    public final SQLDataSource getDS() {
        return this.getSystemRoot().getDataSource();
    }

    protected final String getAddFK(final Link foreignLink) {
        return new AlterTable(foreignLink.getSource()).addForeignConstraint(foreignLink, false).asString();
    }

    protected final SQLSyntax getSyntax() {
        return SQLSyntax.get(this.getSystemRoot().getServer().getSQLSystem());
    }
}
