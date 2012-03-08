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
 
 /*
 * ConvertisseurBaseObs created on 29 avr. 2004
 */
package org.openconcerto.sql.changer.convert;

import static java.util.Collections.singletonList;
import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTable.Index;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.model.graph.SQLKey;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.ChangeTable;
import org.openconcerto.sql.utils.ChangeTable.ClauseType;
import org.openconcerto.sql.utils.ChangeTable.DeferredClause;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddFK extends Changer<DBRoot> {

    public AddFK(DBSystemRoot b) {
        super(b);
    }

    protected void changeImpl(DBRoot root) throws SQLException {
        this.getStream().print(root + "... ");
        final Set<SQLTable> tables = root.getDescs(SQLTable.class);

        if (this.getSystemRoot().getServer().getSQLSystem() == SQLSystem.MYSQL)
            toInno(tables);

        for (final SQLTable t : tables) {
            final Set<Link> foreignLinks = t.getDBSystemRoot().getGraph().getForeignLinks(t);
            final Set<List<String>> realFKs = new HashSet<List<String>>();
            for (final Link link : foreignLinks) {
                // if name is null, link is virtual
                if (link.getName() != null)
                    realFKs.add(link.getCols());
            }
            final Set<List<String>> allFKs = new HashSet<List<String>>(realFKs);
            for (final String virtualFK : SQLKey.foreignKeys(t))
                allFKs.add(singletonList(virtualFK));

            if (allFKs.size() > 0) {
                // indexes already created
                final Set<List<String>> indexes = new HashSet<List<String>>();
                for (final Index i : t.getIndexes()) {
                    indexes.add(i.getCols());
                }

                final AlterTable alter = new AlterTable(t);
                for (final List<String> cols : allFKs) {
                    if (!realFKs.contains(cols)) {
                        final SQLField key = t.getField(cols.get(0));
                        final SQLTable foreignT = SQLKey.keyToTable(key);
                        alter.addForeignConstraint(cols, new SQLName(foreignT.getName()), false, singletonList(t.getKey().getName()));
                        System.err.println("ajout de " + key);
                    }
                    // MySQL automatically creates an index with a foreign key,
                    // but ours replace it
                    if (!indexes.contains(cols)) {
                        alter.addOutsideClause(new DeferredClause() {
                            @Override
                            public String asString(ChangeTable<?> ct, SQLName tableName) {
                                return getSyntax().getCreateIndex("_fki", tableName, cols);
                            }

                            @Override
                            public ClauseType getType() {
                                return ClauseType.ADD_INDEX;
                            }
                        });
                        System.err.println("ajout d'index pour " + cols);
                    } else {
                        System.err.println("pas besoin d'index pour " + cols);
                    }
                }
                if (!alter.isEmpty()) {
                    this.getDS().execute(alter.asString());
                    t.getSchema().updateVersion();
                }
            }
        }
    }

    private void toInno(Set<SQLTable> tables) {
        for (final SQLTable t : tables) {
            if (!this.getDS().execute1("show table status like '" + t.getName() + "'").get("Engine").equals("InnoDB"))
                this.getDS().execute("ALTER TABLE " + t.getName() + " ENGINE = InnoDB;");
        }
    }

}
