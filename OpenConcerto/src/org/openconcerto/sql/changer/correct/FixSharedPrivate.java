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
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelect.ArchiveMode;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Find the private foreign fields for the passed tables and copy the shared private rows. Eg if
 * CPI1 -> OBS1{DES='pb'} and CPI2 -> OBS1{DES='pb'} this clones OBS1 : CPI1 -> OBS1{DES='pb'} and
 * CPI2 -> OBS2{DES='pb'}.
 * 
 * @author Sylvain
 */
public class FixSharedPrivate extends Changer<SQLTable> {

    private final SQLElementDirectory dir;

    public FixSharedPrivate(DBSystemRoot b) {
        this(b, null);
    }

    public FixSharedPrivate(DBSystemRoot b, final SQLElementDirectory dir) {
        super(b);
        if (dir == null) {
            if (Configuration.getInstance() == null)
                throw new IllegalStateException("no conf");
            this.dir = Configuration.getInstance().getDirectory();
            if (this.dir == null)
                throw new IllegalStateException("no directory in conf");
        } else {
            this.dir = dir;
        }
        assert this.dir != null;
    }

    public final SQLElementDirectory getDir() {
        return this.dir;
    }
    
    @Override
    protected void changeImpl(final SQLTable t) throws SQLException {
        getStream().print(t);
        final SQLElement elem = this.getDir().getElement(t);
        if (elem == null) {
            getStream().println(" : no element");
            return;
        } else {
            getStream().println("... ");
        }

        for (final String pff : elem.getPrivateForeignFields()) {
            // eg Q18
            final SQLElement privateElement = elem.getPrivateElement(pff);
            final SQLTable privateTable = privateElement.getTable();
            // SELECT q.ID FROM Ideation_2007.Q18 q
            // JOIN Ideation_2007.MISSION m on m.ID_Q18 = q.ID
            // where q.ID != 1
            // GROUP BY q.ID
            // HAVING count(q.ID) > 1;
            final SQLSelect sel = new SQLSelect();
            sel.setArchivedPolicy(ArchiveMode.BOTH);
            sel.addSelect(privateTable.getKey());
            sel.addBackwardJoin("INNER", "m", t.getField(pff), null);
            final String req = sel.asString() + " GROUP BY " + privateTable.getKey().getFieldRef() + " HAVING count(" + privateTable.getKey().getFieldRef() + ")>1";

            @SuppressWarnings("unchecked")
            final List<Number> privateIDs = t.getDBSystemRoot().getDataSource().executeCol(req);
            if (privateIDs.size() > 0) {
                getStream().println("\t" + pff + " fixing " + privateIDs.size() + " ... ");
                final SQLField archF = t.getArchiveField();
                final SQLField privateArchF = privateTable.getArchiveField();
                if ((archF == null) != (privateArchF == null))
                    throw new IllegalStateException("Incoherent archive field : " + archF + " / " + privateArchF);
                SQLUtils.executeAtomic(t.getDBSystemRoot().getDataSource(), new SQLFactory<Object>() {
                    @Override
                    public Object create() throws SQLException {
                        // for each private pointed by more than one parent
                        for (final Number privateID : privateIDs) {
                            final SQLSelect fixSel = new SQLSelect();
                            fixSel.setArchivedPolicy(ArchiveMode.BOTH);
                            fixSel.addSelect(t.getKey());
                            if (archF != null)
                                fixSel.addSelect(archF);
                            fixSel.setWhere(new Where(t.getField(pff), "=", privateID));
                            final List<SQLRow> tIDs = SQLRowListRSH.execute(fixSel);
                            for (final SQLRow tID : tIDs) {
                                // the first one can keep its private
                                final SQLRowValues reallyPrivate;
                                if (tID == tIDs.get(0))
                                    reallyPrivate = new SQLRowValues(privateElement.getTable()).setID(privateID);
                                else
                                    reallyPrivate = privateElement.createCopy(privateID.intValue());
                                // keep archive coherence
                                if (archF != null)
                                    reallyPrivate.put(privateArchF.getName(), tID.getObject(archF.getName()));
                                new SQLRowValues(t).setID(tID.getIDNumber()).put(pff, reallyPrivate).update();
                            }
                        }
                        return null;
                    }
                });
            }
        }
        getStream().println(t + " done");
    }
}
