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
import org.openconcerto.sql.changer.correct.CorrectOrder;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLTable;

import java.sql.SQLException;

public class AddOrder extends Changer<SQLTable> {

    public AddOrder(DBSystemRoot b) {
        super(b);
    }

    protected void changeImpl(SQLTable t) throws SQLException {
        this.getStream().print(t + "... ");
        if (t.isOrdered()) {
            this.getStream().println("already ordered");
        } else {
            final String update = SQLSelect.quote("ALTER TABLE %f ADD COLUMN %i " + getSyntax().getOrderDefinition(), t, SQLSyntax.ORDER_NAME);
            this.getDS().execute(update);
            t.fetchFields();
            this.getStream().println("field " + SQLSyntax.ORDER_NAME + " added");
            // tout est a la valeur par défaut : NULL
            final CorrectOrder co = new CorrectOrder(t.getDBSystemRoot());
            co.setForce(true);
            co.change(t);
        }

    }

}
