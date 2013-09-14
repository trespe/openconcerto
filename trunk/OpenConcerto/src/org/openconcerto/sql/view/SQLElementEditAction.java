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
 
 package org.openconcerto.sql.view;

import static org.openconcerto.sql.TM.getTM;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.view.SQLMenuItemHelper.GenericSQLElementAction;

public class SQLElementEditAction extends GenericSQLElementAction<EditFrame> {

    public SQLElementEditAction(SQLElement elem) {
        super(elem, getTM().trM("editAction.name", "element", elem.getName()));
    }

    @Override
    protected EditFrame instantiateFrame() {
        return new EditFrame(getElem());
    }
}
