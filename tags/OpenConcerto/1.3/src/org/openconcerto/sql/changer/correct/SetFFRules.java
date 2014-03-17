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
 
 package org.openconcerto.sql.changer.correct;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.model.graph.Link.Rule;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.ChangeTable;
import org.openconcerto.sql.utils.ChangeTable.ConcatStep;
import org.openconcerto.sql.utils.ChangeTable.FCSpec;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;

import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Change foreign field delete rule to allow deletions. E.g. parent foreign field is CASCADE.
 * 
 * @author Sylvain CUAZ
 */
public class SetFFRules extends Changer<SQLTable> {

    private boolean cascadeNormalFF;

    public SetFFRules(DBSystemRoot b) {
        super(b);
        this.cascadeNormalFF = false;
    }

    // for systems who don't support SET_DEFAULT
    public final SetFFRules setCascadeNormalFF(boolean cascadeNormalFF) {
        this.cascadeNormalFF = cascadeNormalFF;
        return this;
    }

    @Override
    protected void changeImpl(final SQLTable t) throws SQLException {
        if (Configuration.getInstance() == null || Configuration.getInstance().getDirectory() == null)
            throw new IllegalStateException("no directory");
        final SQLElement elem = Configuration.getInstance().getDirectory().getElement(t);
        if (elem == null)
            return;

        getStream().println(t);
        final AlterTable alterTable = new AlterTable(t);
        final String parentFF = elem.getParentForeignFieldName();
        if (parentFF != null) {
            setDeleteRule(t, parentFF, alterTable, Rule.CASCADE);
        }
        // MySQL doesn't support SET_DEFAULT
        final Rule normalRule;
        if (t.getServer().getSQLSystem() != SQLSystem.MYSQL) {
            normalRule = Rule.SET_DEFAULT;
        } else {
            normalRule = this.cascadeNormalFF ? Rule.CASCADE : Rule.NO_ACTION;
        }
        for (final String ff : elem.getNormalForeignFields()) {
            setDeleteRule(t, ff, alterTable, normalRule);
        }
        // NO_ACTION is more permissive and MySQL though it accepts RESTRICT, always return
        // NO_ACTION
        for (final String privateFF : elem.getPrivateForeignFields()) {
            setDeleteRule(t, privateFF, alterTable, Rule.NO_ACTION);
        }
        for (final String ff : elem.getSharedForeignFields()) {
            setDeleteRule(t, ff, alterTable, Rule.NO_ACTION);
        }
        if (!alterTable.isEmpty()) {
            // MySQL cannot drop and add in the same statement
            if (getSyntax().getSystem() == SQLSystem.MYSQL) {
                SQLUtils.executeAtomic(getDS(), new SQLFactory<Object>() {
                    @Override
                    public Object create() throws SQLException {
                        for (final List<String> l : ChangeTable.cat(Collections.singleton(alterTable), t.getDBRoot().getName(), EnumSet.of(ConcatStep.ADD_FOREIGN))) {
                            for (final String sql : l)
                                getDS().execute(sql);
                        }
                        return null;
                    }
                });
            } else {
                getDS().execute(alterTable.asString());
            }
            getStream().println("Done.");
            t.getSchema().updateVersion();
        }
    }

    private void setDeleteRule(final SQLTable t, final String ffName, final AlterTable alterTable, final Rule rule) throws SQLException {
        if (rule != Rule.CASCADE && rule != Rule.NO_ACTION && rule != Rule.SET_DEFAULT)
            throw new IllegalArgumentException("SET_NULL is usually impossible, RESTRICT means NO_ACTION for MySQL : " + rule);
        final SQLField ff = t.getField(ffName);
        final Link l = t.getDBSystemRoot().getGraph().getForeignLink(ff);
        if (l.getDeleteRule() != rule) {
            alterTable.dropForeignConstraint(l.getName());
            // ATTN this is not robust, see Index#getCols()
            final boolean hasIndex = t.getIndexes(l.getCols()).size() > 0;
            alterTable.addForeignConstraint(new FCSpec(l.getCols(), l.getContextualName(), l.getRefCols(), l.getUpdateRule(), rule), !hasIndex);
            getStream().println("Will change " + ff + " to " + rule);
        }
    }
}
