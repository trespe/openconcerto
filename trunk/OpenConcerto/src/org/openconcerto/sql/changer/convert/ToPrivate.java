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
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.SQLKey;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.utils.AlterTable;

import java.sql.SQLException;
import java.util.EnumSet;

/**
 * Convert one normal table to a table referenced by a private field. I.e. if you have table PARENT
 * and two tables CHILD1, CHILD2 you may want to include CHILD2 into CHILD1 {@link SQLComponent} to
 * reduce clutter. In that case {@link #PRIVATE_TABLE} is CHILD2, {@link #PARENT_FIELD} is
 * "ID_PARENT" and {@link #PRIVATE_COUNT} must be the maximum CHILD2 there is per PARENT.
 * 
 * @author Sylvain CUAZ
 */
public class ToPrivate extends Changer<SQLTable> {

    static public final String PRIVATE_TABLE = "privateTable";
    static public final String PARENT_FIELD = "parentField";
    static public final String PRIVATE_COUNT = "privateCount";

    private String privateTableName;
    private String parentFieldName;
    private int privateCount;

    public ToPrivate(DBSystemRoot b) {
        super(b);
    }

    @Override
    protected EnumSet<SQLSystem> getCompatibleSystems() {
        return EnumSet.of(SQLSystem.MYSQL);
    }

    @Override
    public void setUpFromSystemProperties() {
        super.setUpFromSystemProperties();
        this.privateTableName = System.getProperty(PRIVATE_TABLE);
        this.parentFieldName = System.getProperty(PARENT_FIELD);
        this.privateCount = Integer.parseInt(System.getProperty(PRIVATE_COUNT, "1"));
    }

    public final String getPrivateField(int index) {
        if (index < 0)
            throw new IllegalArgumentException("negative: " + index);
        else if (index == 0)
            return SQLKey.PREFIX + this.privateTableName;
        else
            return SQLKey.PREFIX + this.privateTableName + "_" + (index + 1);
    }

    @Override
    protected void changeImpl(final SQLTable tableToUpdate) throws SQLException {
        final SQLTable privateTable = tableToUpdate.getTable(this.privateTableName);

        // SELECT count(t1.ID_SITE)
        // FROM INSTALLATIONS_ECL t1
        // where t1.ARCHIVE = 0
        // group by t1.ID_SITE
        // having count(t1.ID_SITE) > 4
        // order by count(t1.ID_SITE) DESC
        // LIMIT 1;
        final SQLField parentField = privateTable.getField(this.parentFieldName);
        final SQLSelect checkCount = new SQLSelect(privateTable.getBase());
        checkCount.addSelect(parentField, "count");
        checkCount.addGroupBy(parentField);
        final String countExpr = "count(" + SQLBase.quoteIdentifier(parentField.getName()) + ")";
        checkCount.setHaving(Where.createRaw(countExpr + " > " + this.privateCount, parentField));
        checkCount.addRawOrder(countExpr + " DESC");
        checkCount.setLimit(1);
        final Number n = (Number) getDS().executeScalar(checkCount.asString());
        if (n != null) {
            checkCount.addSelect(parentField);
            checkCount.setLimit(null);
            throw new IllegalStateException("More than " + this.privateCount + " rows : " + n + "\n" + checkCount.asString());
        }

        final AlterTable alter = new AlterTable(tableToUpdate);
        for (int i = 0; i < this.privateCount; i++) {
            final String privateFieldName = this.getPrivateField(i);
            if (!tableToUpdate.contains(privateFieldName)) {
                getStream().println("Creating " + privateFieldName + "...");
                alter.addForeignColumn(privateFieldName, privateTable);
            }
        }
        if (!alter.isEmpty()) {
            getDS().execute(alter.asString());
            tableToUpdate.getSchema().updateVersion();
            tableToUpdate.fetchFields();
            getStream().println("done.");
        }

        // select "ID_SITE", "ID count",
        // case when "ID count" < 3 then NULL else SUBSTRING_INDEX(SUBSTRING_INDEX("IDs", '|', 3),
        // '|', -1) end as "ID_3"
        // from (
        // SELECT "ID_SITE", COUNT("ID") as "ID count", cast( group_concat("ID" separator '|') as
        // char) as "IDs"
        // FROM INSTALLATIONS_ECL t1
        // where t1.ARCHIVE = 0
        // group by t1.ID_SITE
        // ) aggregatedIDs;
        final String sep = "','";
        final SQLSelect groupIDs = new SQLSelect(privateTable.getBase());
        groupIDs.addSelect(parentField);
        groupIDs.addSelect(parentField, "count", "ID count");
        final String quotedID = SQLBase.quoteIdentifier(privateTable.getKey().getName());
        groupIDs.addRawSelect("cast( group_concat(" + quotedID + " separator " + sep + ") as char)", "IDs");
        groupIDs.addGroupBy(parentField);

        final Number undefIDobj = privateTable.getUndefinedIDNumber();
        final String undefID = privateTable.getKey().getType().toString(undefIDobj);
        final String quotedCount = SQLBase.quoteIdentifier("ID count");
        final String quotedIDs = SQLBase.quoteIdentifier("IDs");

        final UpdateBuilder update = new UpdateBuilder(tableToUpdate);
        update.addTable("\n( " + groupIDs.asString() + " )", SQLBase.quoteIdentifier("aggregatedIDs"));
        final String join = tableToUpdate.getBase().quote("%i = %f", new SQLName("aggregatedIDs", parentField.getName()), tableToUpdate.getField(this.parentFieldName));
        Where dontOverwrite = null;
        final StringBuilder splitIDs = new StringBuilder(256);
        for (int i = 1; i <= this.privateCount; i++) {
            final String privateFieldName = getPrivateField(i - 1);
            dontOverwrite = new Where(tableToUpdate.getField(privateFieldName), Where.NULL_IS_DATA_EQ, undefIDobj).and(dontOverwrite);

            splitIDs.setLength(0);
            splitIDs.append("case when ").append(quotedCount).append(" < ").append(i).append(" then ").append(undefID);
            splitIDs.append(" else SUBSTRING_INDEX(SUBSTRING_INDEX(").append(quotedIDs).append(", " + sep + ", ").append(i).append("), " + sep + ", -1) end ");
            update.set(privateFieldName, splitIDs.toString());
        }
        update.setWhere(Where.createRaw(join).and(dontOverwrite));

        // ATTN for some reason the MySQL JDBC driver doesn't to execute this
        getStream().println(update.toString());
    }
}
