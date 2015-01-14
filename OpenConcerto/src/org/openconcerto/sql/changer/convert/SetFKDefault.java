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
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.utils.CompareUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Set missing default value for foreign fields.
 * 
 * @author Sylvain
 */
public class SetFKDefault extends Changer<SQLTable> {

    public SetFKDefault(DBSystemRoot b) {
        super(b);
    }

    @Override
    protected void changeImpl(SQLTable t) throws SQLException {
        final Set<Link> foreignLinks = t.getDBSystemRoot().getGraph().getForeignLinks(t);
        final AlterTable alter = new AlterTable(t);
        for (final Link link : foreignLinks) {
            final int colCount = link.getCols().size();
            final SQLTable target = link.getTarget();

            final List<String> correctDefaults;
            final SQLField singleField = link.getSingleField();
            if (singleField != null && link.getRefFields().get(0).isPrimaryKey()) {
                // common case
                final Number undefinedID = target.getUndefinedIDNumber();
                correctDefaults = Collections.singletonList(undefinedID == null ? null : singleField.getType().toString(undefinedID));
            } else {
                // general case
                correctDefaults = new ArrayList<String>(colCount);
                for (final SQLField rf : link.getRefFields()) {
                    // from DatabaseMetadata.getColumns() : COLUMN_DEF is a string, i.e. SQL value
                    correctDefaults.add(rf.getDefaultValue());
                }
            }
            assert correctDefaults.size() == colCount;

            int i = 0;
            for (final SQLField ff : link.getFields()) {
                final String correctDef = correctDefaults.get(i);
                // don't overwrite
                if (ff.getDefaultValue() == null && !CompareUtils.equals(ff.getDefaultValue(), correctDef)) {
                    getStream().println("will change " + ff + " from " + ff.getDefaultValue() + " to " + correctDef);
                    alter.alterColumnDefault(ff.getName(), correctDef);
                }
                i++;
            }
            assert i == colCount;
        }
        if (!alter.isEmpty()) {
            this.getDS().execute(alter.asString());
            t.getSchema().updateVersion();
        }
    }
}
