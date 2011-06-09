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

import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Link;

/**
 * Construct a DROP TABLE statement.
 * 
 * @author Sylvain
 */
public final class DropTable extends ChangeTable<DropTable> {

    private final SQLTable t;

    public DropTable(SQLTable t) {
        super(t.getServer().getSQLSystem().getSyntax(), t.getName());
        this.t = t;
    }

    private final AlterTable getAlterTable() {
        final AlterTable alterTable = new AlterTable(this.t);
        for (final Link foreignLink : this.t.getDBSystemRoot().getGraph().getForeignLinks(this.t)) {
            if (foreignLink.getName() == null)
                throw new IllegalStateException(foreignLink + " is not a real constraint, use AddFK");
            alterTable.dropForeignConstraint(foreignLink.getName());
        }
        return alterTable;
    }

    public final String asString() {
        return this.asString(this.t.getDBRoot().getName());
    }

    @Override
    public String asString(String rootName) {
        return this.t.getBase().quote("DROP TABLE %f ;", this.t);
    }

    @Override
    protected String asString(String rootName, ConcatStep step) {
        switch (step) {
        case DROP_FOREIGN:
            return this.getAlterTable().asString(rootName, step);
        case ALTER_TABLE:
            return this.asString(rootName);
        default:
            return null;
        }
    }

    @Override
    public DropTable addColumn(String name, String definition) {
        // FIXME make a superclass w/o it
        throw new UnsupportedOperationException();
    }

}
