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
 
 package org.openconcerto.sql.changer.convert;

import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.model.DBStructureItem;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.utils.ReOrder;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Offset all IDs of a table and optionally the order. The undefined row is not changed.
 * 
 * @author Sylvain CUAZ
 */
public class ChangeIDs extends Changer<SQLTable> {

    public static final String OFFSET = "ids.offset";
    public static final String CHANGE_ORDER = "order.change";

    private Long offset = null;
    private boolean changeOrder = false;

    public ChangeIDs(DBSystemRoot b) {
        super(b);
    }

    public final void setOffset(Long offset) {
        this.offset = offset;
    }

    public final void setChangeOrder(boolean changeOrder) {
        this.changeOrder = changeOrder;
    }

    @Override
    public void setUpFromSystemProperties() {
        super.setUpFromSystemProperties();
        this.setOffset(Long.getLong(OFFSET));
        this.setChangeOrder(Boolean.getBoolean(CHANGE_ORDER));
    }

    @Override
    protected Class<? extends DBStructureItem<?>> getMaxLevel() {
        return SQLTable.class;
    }

    protected void changeImpl(final SQLTable t) throws SQLException {
        this.getStream().print(t + "... ");
        if (this.offset == null)
            throw new IllegalStateException("offset not given");
        final long offset = this.offset.longValue();

        final Object[] range = selMinMax(t.getKey());
        final long beforeStart = ((Number) range[0]).longValue();
        final long beforeEnd = ((Number) range[1]).longValue();
        final long afterStart = beforeStart + offset;
        final long afterEnd = beforeEnd + offset;

        if ((long) t.getUndefinedID() >= afterStart && (long) t.getUndefinedID() <= afterEnd)
            throw new IllegalStateException("Would overwrite undefined : " + afterStart + ", " + afterEnd);
        // easier that way since almost no systems support deferrable constraints
        if (beforeStart <= afterEnd && beforeEnd >= afterStart)
            throw new IllegalStateException("Overlap with existing IDs : " + afterStart + ", " + afterEnd);
        // this generally also avoids having an undefined ID not being the minimum
        if (afterStart < SQLRow.MIN_VALID_ID)
            throw new IllegalStateException("Too low : " + afterStart);

        SQLUtils.executeAtomic(getDS(), new SQLFactory<Object>() {
            @Override
            public Object create() throws SQLException {
                getDS().execute(getSyntax().disableFKChecks(t.getDBRoot()));

                updateField(t.getKey(), t, offset);

                final Set<Link> refLinks = t.getDBSystemRoot().getGraph().getReferentLinks(t);
                for (final Link refLink : refLinks) {
                    updateField(refLink.getLabel(), t, offset);
                }

                getDS().execute(getSyntax().enableFKChecks(t.getDBRoot()));

                if (t.isOrdered() && ChangeIDs.this.changeOrder) {
                    ReOrder.create(t).exec();
                    // -1 for the undefined
                    assert selMinMaxAsLong(t.getOrderField()).equals(Arrays.asList(1l, t.getRowCount() - 1l));
                    // afterStart-1 so that orders will have the same range as IDs
                    updateField(t.getOrderField(), t, afterStart - 1);
                }

                return null;
            }
        });

        this.getStream().println("done");
    }

    private List<Long> selMinMaxAsLong(final SQLField f) {
        final Object[] minMax = selMinMax(f);
        final List<Long> res = new ArrayList<Long>(2);
        res.add(((Number) minMax[0]).longValue());
        res.add(((Number) minMax[1]).longValue());
        return res;
    }

    private Object[] selMinMax(final SQLField f) {
        final SQLSelect sel = new SQLSelect(f.getTable().getBase(), true);
        // undefined never moves (that way don't have to change defaults of fk)
        sel.setExcludeUndefined(true);
        sel.addSelect(f, "min");
        sel.addSelect(f, "max");
        return getDS().executeA1(sel.asString());
    }

    private void updateField(final SQLField f, SQLTable t, long offset) {
        if (offset == 0)
            return;
        final UpdateBuilder update = new UpdateBuilder(f.getTable()).set(f.getName(), SQLBase.quoteIdentifier(f.getName()) + " + " + offset);
        update.setWhere(new Where(f.isKey() ? f : t.getKey(), "!=", t.getUndefinedID()));
        getDS().execute(update.asString());
    }

}
