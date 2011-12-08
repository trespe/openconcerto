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

import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTable.Index;
import org.openconcerto.sql.model.graph.Link;

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
            if (foreignLinks.size() > 0) {
                // indexes already created
                final Set<List<String>> indexes = new HashSet<List<String>>();
                for (final Index i : t.getIndexes()) {
                    indexes.add(i.getCols());
                }

                for (final Link link : foreignLinks) {
                    if (link.getName() == null) {
                        final String s = this.getAddFK(link);
                        System.err.println(s);
                        this.getDS().execute(s);
                    } else {
                        System.err.println("pas besoin pour " + link);
                    }
                    // MySQL automatically creates an index with a foreign key,
                    // but ours replace it
                    if (!indexes.contains(link.getCols())) {
                        final String s = this.getSyntax().getCreateIndex("_fki", link.getSource().getSQLName(), link.getCols());
                        System.err.println(s);
                        this.getDS().execute(s);
                    } else {
                        System.err.println("pas besoin d'index pour " + link);
                    }
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
