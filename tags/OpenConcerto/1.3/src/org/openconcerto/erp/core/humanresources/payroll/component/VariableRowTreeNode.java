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
 
 package org.openconcerto.erp.core.humanresources.payroll.component;

import org.openconcerto.sql.model.SQLRow;

public class VariableRowTreeNode extends FormuleTreeNode {

    private SQLRow row;

    public VariableRowTreeNode(SQLRow row) {
        this.row = row;
    }

    public String getName() {
        return (this.row == null) ? "" : this.row.getString("NOM");
    }

    public String toString() {
        return (this.row == null) ? "" : this.row.getString("NOM");
    }

    public String getTextValue() {
        return (this.row == null) ? "" : this.row.getString("NOM");
    }

    public String getTextInfosValue() {
        return (this.row == null) ? "" : this.row.getString("INFOS");
    }

    public SQLRow getRow() {
        return this.row;
    }

    public void setRow(SQLRow row) {
        this.row = row;
    }

    public int getID() {

        return this.row == null ? 1 : row.getID();
    }
}
