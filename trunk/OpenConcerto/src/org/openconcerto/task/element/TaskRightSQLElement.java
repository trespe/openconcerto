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
 
 package org.openconcerto.task.element;

import static java.util.Arrays.asList;
import org.openconcerto.sql.element.SQLComponent;

import java.util.List;

public class TaskRightSQLElement extends TaskSQLElementBase {

    public TaskRightSQLElement() {
        super("TACHE_RIGHTS");
    }

    @Override
    protected void ffInited() {
        super.ffInited();
        this.setAction("ID_USER_COMMON_TO", ReferenceAction.CASCADE);
    }

    protected List<String> getListFields() {
        return asList(getParentFFName(), "ID_USER_COMMON_TO", "READ", "MODIFY", "ADD", "VALIDATE");
    }

    protected List<String> getComboFields() {
        return getListFields();
    }

    @Override
    protected String getParentFFName() {
        return "ID_USER_COMMON";
    }

    public SQLComponent createComponent() {
        return null;
    }
}
