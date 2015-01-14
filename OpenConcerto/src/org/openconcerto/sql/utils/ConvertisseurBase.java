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

import org.openconcerto.sql.changer.Change;
import org.openconcerto.sql.changer.Convert;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLField.Properties;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.DecimalUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author ILM Informatique 29 avr. 2004
 */
public class ConvertisseurBase extends ChangeBase {

    public void dissociateIDs() throws SQLException {
        int offset = 0;
        this.getBase().getDataSource().getConnection().setAutoCommit(false);

        final Iterator iter = this.getBase().getTables().iterator();
        while (iter.hasNext()) {
            final SQLTable t = (SQLTable) iter.next();
            if (t.getKey() != null) {
                this.mvIDs(t, offset, offset + 1);
                offset += 10000000;
            }
        }
    }

    private void mvIDs(SQLTable t, int idOffset, int undefID) throws SQLException {
        if (idOffset == 0)
            System.err.println("0 id offset for " + t);
        else if (t.isRowable()) {
            final List<String> statements = new ArrayList<String>();
            statements.add("SET FOREIGN_KEY_CHECKS=0");
            final Set<SQLField> keys = new HashSet<SQLField>(this.getBase().getGraph().getReferentKeys(t));
            keys.add(t.getKey());
            final Iterator<SQLField> refKeysIter = keys.iterator();
            while (refKeysIter.hasNext()) {
                final SQLField refKey = refKeysIter.next();
                final String update = "UPDATE " + refKey.getTable().getName() + " SET " + refKey.getName() + "=" + refKey.getName() + "+ (" + idOffset + ")";
                statements.add(update);

                String alter = "ALTER TABLE " + SQLBase.quoteIdentifier(refKey.getTable().getName()) + " MODIFY COLUMN " + SQLBase.quoteIdentifier(refKey.getName()) + " ";
                if (refKey != t.getKey()) {
                    alter += getSyntax().getIDType() + " DEFAULT " + undefID;
                } else {
                    alter += getSyntax().getPrimaryIDDefinition();
                }
                statements.add(alter);
            }

            statements.add("SET FOREIGN_KEY_CHECKS=1");
            System.err.println(CollectionUtils.join(statements, "\n"));
            final Statement stmt = this.getBase().getDataSource().getConnection().createStatement();
            for (final String s : statements) {
                stmt.addBatch(s);
            }
            stmt.executeBatch();
            this.getBase().getDataSource().getConnection().commit();
        }
    }

    public void associateIDs() throws SQLException {
        final int undefinedID = 1;
        this.getBase().getDataSource().getConnection().setAutoCommit(false);

        final Iterator iter = this.getBase().getTables().iterator();
        while (iter.hasNext()) {
            final SQLTable t = (SQLTable) iter.next();
            final int offset = t.getUndefinedID();
            this.mvIDs(t, -offset + undefinedID, undefinedID);
        }
    }

    public final void floatToDecimal() {
        for (final String tn : this.getBase().getTableNames()) {
            this.floatToDecimal(tn);
        }
    }

    public final void floatToDecimal(String tableName) {
        final SQLTable t = this.getBase().getTable(tableName);
        System.out.print(tableName + "... ");
        final Set<Class> virgule = new HashSet<Class>();
        virgule.add(Float.class);
        virgule.add(Double.class);
        // don't mess with the fk and other special fields
        for (final SQLField f : t.getLocalContentFields()) {
            if (virgule.contains(f.getType().getJavaType())) {
                System.out.print(f);
                final SQLSelect sel = new SQLSelect(t.getBase(), true).addSelect(f, "max");
                final double max = ((Number) this.getBase().getDataSource().executeScalar(sel.asString())).doubleValue();
                final int maxIntLength = DecimalUtils.intDigits(BigDecimal.valueOf(max));
                final int defaultIntPart = 8;
                final int intPart = maxIntLength <= defaultIntPart ? defaultIntPart : maxIntLength;
                final String type = this.getSyntax().getDecimalIntPart(intPart, 8);
                final AlterTable alter = new AlterTable(f.getTable());
                alter.alterColumn(f.getName(), Collections.singleton(Properties.TYPE), type, null, null);
                this.getBase().getDataSource().execute(alter.asString());
            }
        }
        System.out.println(" done");
    }

    public ConvertisseurBase(DBRoot root) {
        super(root);
    }

    public ConvertisseurBase() throws IOException {
        super();
    }

    @Override
    protected Change getChange() {
        return new Convert();
    }

    static public void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("usage: ConvertisseurBase convertisseur...");
            System.exit(0);
        }
        ConvertisseurBase conv = new ConvertisseurBase();
        conv.call(args);
    }

}
