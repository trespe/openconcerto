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
package org.openconcerto.sql.utils;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.changer.Change;
import org.openconcerto.sql.changer.Correct;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.CollectionUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author ILM Informatique 29 avr. 2004
 */
public class CorrectBase extends ChangeBase {

    public void restoreIntegrity(boolean existant, int modus) {
        final Iterator i = this.getBase().getTableNames().iterator();
        while (i.hasNext()) {
            final String table = (String) i.next();
            restoreIntegrity(existant, modus, table);
        }
    }

    // 0: archive, 1: put undefined, 2:delete
    public void restoreIntegrity(boolean inexistant, int modus, String table) {
        System.out.println("*** " + table);
        final Iterator li = this.getBase().getTable(table).checkIntegrity().iterator();
        while (li.hasNext()) {
            SQLRow row = (SQLRow) li.next();
            SQLField foreignKey = (SQLField) li.next();
            SQLRow pb = (SQLRow) li.next();
            if (!inexistant || !pb.exists()) {
                final String update;
                final String where = row.getTable().getKey().getName() + "=" + row.getID();
                if (modus == 2) {
                    update = "DELETE FROM " + row.getTable().getName() + " WHERE " + where;
                } else if (modus == 1) {
                    update = "UPDATE " + row.getTable().getName() + " SET " + foreignKey.getName() + "=1 WHERE " + where;
                } else {
                    update = "UPDATE " + row.getTable().getName() + " SET ARCHIVE=1 WHERE " + where;
                }
                System.out.println(update);
                this.getBase().getDataSource().execute(update);
            }
        }
    }

    public void correctPoint2NonExistant() {
        this.restoreIntegrity(true, 1);
    }

    public void deletePoint2NonExistant() {
        this.restoreIntegrity(true, 2);
    }

    // ATTN this method need the directory to be filled (ie no headless)
    // correct multiple rows pointing to the same private row,
    // eg ID_OBSERVATION_2 of ECLAIRAGE[123] and ECLAIRAGE[756] points to OBSERVATION[6665]
    // OBSERVATION[6665] is cloned and affected to the second ECLAIRAGE
    public void correctSharedPrivates() {
        final SQLElementDirectory dir = Configuration.getInstance().getDirectory();
        for (final SQLTable table : dir.getTables()) {
            final SQLElement element = dir.getElement(table);
            System.err.println(table);
            final Iterator iter = element.getPrivateForeignFields().iterator();
            while (iter.hasNext()) {
                // eg ID_LIAISON_NEUTRE
                final String f = (String) iter.next();
                // eg LiaisonElement
                final SQLElement foreignElement = element.getPrivateElement(f);
                String sel = "SELECT " + f + ", ARCHIVE FROM " + element.getTable().getName() + " WHERE ARCHIVE=0" + " GROUP BY " + f;
                sel += " HAVING count(" + f + ") > 1 and " + f + "!= " + foreignElement.getTable().getUndefinedID();
                final List sharedPrivateIDs = this.getBase().getDataSource().executeCol(sel);
                if (sharedPrivateIDs.size() > 0) {
                    System.err.println(sharedPrivateIDs);
                    final Iterator idIter = sharedPrivateIDs.iterator();
                    while (idIter.hasNext()) {
                        // eg 12
                        final Number sharedPrivateID = (Number) idIter.next();
                        // eg LIAISON[12]
                        final SQLRow foreignRow = foreignElement.getTable().getRow(sharedPrivateID.intValue());
                        final List invalidRows = foreignRow.getReferentRows(element.getTable().getField(f));
                        // laisser le 1er
                        final Iterator invalidRowsIter = invalidRows.listIterator(1);
                        while (invalidRowsIter.hasNext()) {
                            // eg TRANSFO[1325]
                            final SQLRow invalidR = (SQLRow) invalidRowsIter.next();
                            try {
                                final SQLRow clone = foreignElement.copy(foreignRow);
                                System.err.println("Updating " + invalidR + " by putting " + clone.getID() + " in " + f);
                                final SQLRowValues vals = invalidR.createEmptyUpdateRow().put(f, clone.getID());
                                vals.update();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    public void deleteArchived() throws SQLException {
        final Iterator iter = this.getBase().getTables().iterator();
        while (iter.hasNext()) {
            final SQLTable t = (SQLTable) iter.next();
            if (t.isArchivable()) {
                System.out.println("*** " + t);
                this.getBase().getDataSource().execute("DELETE FROM " + t.getName() + " where ARCHIVE=1");
            }
        }
    }

    // standardize key types
    public void standardizeKeys() throws SQLException {
        this.getBase().getDataSource().getConnection().setAutoCommit(false);

        final Iterator iter = this.getBase().getTables().iterator();
        while (iter.hasNext()) {
            final SQLTable t = (SQLTable) iter.next();
            if (t.isRowable()) {
                final List<String> statements = new ArrayList<String>();
                statements.add("SET FOREIGN_KEY_CHECKS=0");
                // primary key + all keys that link to it
                final Set<SQLField> keys = new HashSet<SQLField>(this.getBase().getGraph().getReferentKeys(t));
                keys.add(t.getKey());
                final Iterator<SQLField> refKeysIter = keys.iterator();
                while (refKeysIter.hasNext()) {
                    final SQLField refKey = refKeysIter.next();

                    String alter = "ALTER TABLE \"" + refKey.getTable().getName();
                    if (refKey != t.getKey()) {
                        alter += "\" MODIFY COLUMN \"" + refKey.getName() + "\" " + this.getSyntax().getIDType();
                        alter += " DEFAULT " + refKey.getDefaultValue();
                    } else {
                        alter += "\" MODIFY COLUMN \"" + refKey.getName() + "\" " + this.getSyntax().getPrimaryIDDefinitionShort();
                    }
                    statements.add(alter);
                }

                statements.add("SET FOREIGN_KEY_CHECKS=1");
                System.err.println(CollectionUtils.join(statements, " ; "));
                this.getBase().getDataSource().execute(CollectionUtils.join(statements, " ; "));
                this.getBase().getDataSource().getConnection().commit();
            }
        }
        System.err.println("Done.");
    }

    public CorrectBase(DBRoot root) {
        super(root);
    }

    public CorrectBase() throws IOException {
        super();
    }

    @Override
    protected Change getChange() {
        return new Correct(this.getRoot());
    }

    static public void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("usage: " + CorrectBase.class.getName() + " method...");
            System.exit(0);
        }
        CorrectBase conv = new CorrectBase();
        conv.call(args);
    }

}
