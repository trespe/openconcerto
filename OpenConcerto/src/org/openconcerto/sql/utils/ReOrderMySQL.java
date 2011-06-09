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
import org.openconcerto.sql.model.SQLTable;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

final class ReOrderMySQL extends ReOrder {

    public ReOrderMySQL(final SQLTable t, final Spec spec) {
        super(t, spec);
    }

    // SELECT ( max(ORDRE)-min(ORDRE) ) / ( count(*) -1) FROM ADRESSE A where ORDRE between 3 and (3
    // + 11 ) into @inc;
    // UPDATE ADRESSE SET ORDRE = -ORDRE where ORDRE > 0 and ORDRE between 3 and (3+11);
    // SET @o := 3 - @inc ;
    // UPDATE ADRESSE SET ORDRE = ( @o := @o +@inc ) where ORDRE < 0 ORDER BY ORDRE DESC;

    public List<String> getSQL(Connection conn) {
        final SQLField oF = this.t.getOrderField();

        final List<String> res = new ArrayList<String>();
        res.add("SELECT " + getInc() + " into @inc");
        res.add(this.t.getBase().quote("UPDATE %f SET %n =  -%n " + this.getWhere(), this.t, oF, oF));
        // on commence à 0
        res.add("SET @o := " + this.getFirst() + "- @inc");
        res.add(getLoop(oF, "<= -" + this.getFirst(), oF, "DESC"));
        if (this.isAll()) {
            res.add(getLoop(oF, "is null", this.t.getKey(), "ASC"));
        }

        return res;
    }

    // MAYBE factor with pg
    private String getLoop(final SQLField oF, String cond, final SQLField orderBy, final String way) {
        return this.t.getBase().quote("UPDATE %f SET %n = ( @o := @o + @inc ) where %n " + cond + " ORDER BY %n " + way, this.t, oF, oF, orderBy);
    }
}
