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
 
 package org.openconcerto.erp.utils.correct;

import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelect.ArchiveMode;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.sql.SQLException;

/**
 * Find unarchived empty MOUVEMENT.
 * 
 * @author Sylvain CUAZ
 */
public class FindEmptyMouvement extends Changer<DBRoot> {

    private boolean completelyEmpty;

    public FindEmptyMouvement(DBSystemRoot b) {
        super(b);
        this.setCompletelyEmpty(false);
    }

    public void setCompletelyEmpty(boolean completelyEmpty) {
        this.completelyEmpty = completelyEmpty;
    }

    @Override
    public void setUpFromSystemProperties() {
        super.setUpFromSystemProperties();
        this.setCompletelyEmpty(Boolean.getBoolean("findMvt.completelyEmpty"));
    }

    @Override
    protected void changeImpl(DBRoot societeRoot) throws SQLException {
        final SQLTable ecritureT = societeRoot.getTable("ECRITURE");
        final SQLField ecritureMvtFF = ecritureT.getField("ID_MOUVEMENT");
        final SQLTable mvtT = ecritureT.getForeignTable(ecritureMvtFF.getName());

        final SQLSelect sel = new SQLSelect();
        sel.addSelect(mvtT.getKey(), "count");
        sel.addBackwardJoin("LEFT", null, ecritureMvtFF, null);
        sel.setArchivedPolicy(ecritureT, this.completelyEmpty ? ArchiveMode.BOTH : ArchiveMode.UNARCHIVED);
        sel.setWhere(Where.isNull(ecritureT.getKey()));

        final Number count = (Number) getDS().executeScalar(sel.asString());
        getStream().println("Found " + count + (this.completelyEmpty ? " completely " : "") + " empty " + mvtT.getName());
    }
}
