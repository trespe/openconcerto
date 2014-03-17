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

import static java.util.Collections.singletonList;
import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.SQLUtils;

import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Set;

/**
 * Rename the primary key of a table to "ID".
 * 
 * @author Sylvain
 */
public class RenamePK extends Changer<SQLTable> {

    public RenamePK(DBSystemRoot b) {
        super(b);
    }

    @Override
    protected EnumSet<SQLSystem> getCompatibleSystems() {
        return EnumSet.of(SQLSystem.MYSQL, SQLSystem.POSTGRESQL);
    }

    protected void changeImpl(SQLTable t) throws SQLException {
        renamePrimary(t, "ID");
    }

    private void renamePrimary(final SQLTable t, final String newName) throws SQLException {
        final SQLSystem system = t.getBase().getServer().getSQLSystem();

        if (t.getKey() != null) {
            final String keyName = t.getKey().getName();
            if (!keyName.equals(newName) && keyName.startsWith("ID")) {
                getStream().println(t);
                if (system == SQLSystem.MYSQL) {
                    final Set<Link> referentLinks = this.getSystemRoot().getGraph().getReferentLinks(t);
                    SQLUtils.executeAtomic(t.getDBSystemRoot().getDataSource(), new ConnectionHandlerNoSetup<Object, SQLException>() {
                        @Override
                        public Object handle(SQLDataSource ds) throws SQLException {
                            for (final Link refLink : referentLinks) {
                                final String dropIndex = new AlterTable(refLink.getSource()).dropForeignConstraint(refLink.getName()).asString();
                                getStream().println(dropIndex);
                                getDS().execute(dropIndex);
                            }

                            final String alter = SQLSelect.quote("ALTER TABLE %n CHANGE COLUMN %n %i " + getSyntax().getPrimaryIDDefinitionShort(), t, t.getKey(), newName);
                            getStream().println(alter);
                            getDS().execute(alter);
                            t.fetchFields();

                            for (final Link l : referentLinks) {
                                final String addFK = new AlterTable(l.getSource()).addForeignConstraint(l.getCols(), l.getContextualName(), false, singletonList(newName)).asString();
                                getStream().println(addFK);
                                getDS().execute(addFK);
                            }

                            return null;
                        }
                    });
                } else if (system == SQLSystem.POSTGRESQL) {
                    final String alter = t.getBase().quote("ALTER TABLE %f  RENAME COLUMN %n TO %i", t, t.getKey(), newName);
                    getStream().println(alter);
                    this.getDS().execute(alter);
                } else
                    throw new UnsupportedOperationException("for " + system);
            }
        }
    }

}
