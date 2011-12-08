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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;

public class VariableTreeNode extends FormuleTreeNode {

    private SQLField field;

    public VariableTreeNode(SQLField field) {
        this.field = field;
    }

    public String getName() {
        return this.field.getName();
    }

    public String toString() {
        return this.field.getName();
    }

    public String getTextValue() {
        return this.field.getName();
    }

    public String getTextInfosValue() {
        return Configuration.getInstance().getTranslator().getLabelFor(this.field);
    }
}
