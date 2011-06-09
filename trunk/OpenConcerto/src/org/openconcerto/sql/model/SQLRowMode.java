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
 
 package org.openconcerto.sql.model;

import org.openconcerto.sql.model.SQLSelect.ArchiveMode;

import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

/**
 * Allow to specify which rows we're interested in.
 * 
 * @author Sylvain CUAZ
 */
public class SQLRowMode {

    public static final SQLRowMode DATA = new SQLRowMode(SQLSelect.UNARCHIVED, true, true);
    public static final SQLRowMode DEFINED = new SQLRowMode(SQLSelect.BOTH, true, true);
    public static final SQLRowMode VALID = new SQLRowMode(SQLSelect.UNARCHIVED, true, false);
    public static final SQLRowMode EXIST = new SQLRowMode(SQLSelect.BOTH, true, false);
    public static final SQLRowMode INEXISTANT = new SQLRowMode(SQLSelect.BOTH, false, false);
    /** any row matches : no access to the db */
    public static final SQLRowMode NO_CHECK = INEXISTANT;

    private final ArchiveMode archiveMode;
    private final boolean existing;
    private final boolean undefined;

    public SQLRowMode(ArchiveMode archiveMode, boolean existing, boolean undefined) {
        this.archiveMode = archiveMode;
        this.existing = existing;
        this.undefined = undefined;
    }

    public ArchiveMode getArchiveMode() {
        return this.archiveMode;
    }

    public boolean wantExisting() {
        return this.existing;
    }

    public boolean excludeUndefined() {
        return this.undefined;
    }

    /**
     * Checks whether <code>r</code> conforms to this mode.
     * 
     * @param r the row to test, may be <code>null</code>.
     * @return <code>true</code> if <code>r</code> conforms, always <code>false</code> for
     *         <code>null</code>.
     */
    public boolean check(SQLRow r) {
        if (this == NO_CHECK)
            return true;
        if (r == null)
            return false;

        // check undef first since it doesn't require values and thus a query
        if (this.excludeUndefined() && r.isUndefined())
            return false;

        if (this.getArchiveMode() == SQLSelect.ARCHIVED && !r.isArchived())
            return false;
        if (this.getArchiveMode() == SQLSelect.UNARCHIVED && r.isArchived())
            return false;

        if (this.wantExisting() != r.exists())
            return false;

        return true;
    }

    public SQLRow filter(SQLRow r) {
        return this.check(r) ? r : null;
    }

    public void filter(Collection rows) {
        CollectionUtils.filter(rows, new Predicate() {
            public boolean evaluate(Object object) {
                return check((SQLRow) object);
            }
        });
    }

}
