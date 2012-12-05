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

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLType;
import org.openconcerto.utils.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.dbutils.ResultSetHandler;

/**
 * Serialize rows into the CSV format. Lines are separated by newlines, <code>null</code> is \N, all
 * other values are double quoted.
 * 
 * @author Sylvain
 * @see SQLSyntax#loadData(java.io.File, org.openconcerto.sql.model.SQLTable)
 */
public class CSVHandler implements ResultSetHandler {

    private final List<String> names;
    private final List<SQLType> types;

    public CSVHandler(Collection<SQLField> fields) {
        super();
        int size = fields.size();
        this.names = new ArrayList<String>(size);
        this.types = new ArrayList<SQLType>(size);
        for (final SQLField field : fields) {
            this.names.add(field.getName());
            this.types.add(field.getType());
        }
    }

    public CSVHandler(List<String> names, List<SQLType> types) {
        super();
        if (names.size() != types.size())
            throw new IllegalArgumentException("Different sizes : " + names + " != " + types);
        this.names = names;
        this.types = types;
    }

    @Override
    public String handle(ResultSet rs) throws SQLException {
        final int colCount = this.names.size();
        final StringBuilder sb = new StringBuilder(16 * 1024);

        for (int i = 0; i < colCount; i++) {
            if (i > 0)
                sb.append(',');
            sb.append(StringUtils.doubleQuote(this.names.get(i)));
        }
        sb.append('\n');

        while (rs.next()) {
            for (int i = 0; i < colCount; i++) {
                if (i > 0)
                    sb.append(',');
                final Object obj = rs.getObject(i + 1);
                sb.append(this.types.get(i).toCSV(obj));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.names.hashCode();
        result = prime * result + this.types.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final CSVHandler other = (CSVHandler) obj;
        return this.names.equals(other.names) && this.types.equals(other.types);
    }
}
