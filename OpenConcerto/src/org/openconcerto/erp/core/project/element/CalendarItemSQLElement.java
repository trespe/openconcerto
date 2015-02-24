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
 
 package org.openconcerto.erp.core.project.element;

import java.util.Arrays;
import java.util.List;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.SQLComponent;

public class CalendarItemSQLElement extends ComptaSQLConfElement {

    public CalendarItemSQLElement() {
        super("CALENDAR_ITEM");
    }

    @Override
    protected List<String> getListFields() {
        return Arrays.asList("SUMMARY", "START", "END", "STATUS");
    }

    @Override
    protected SQLComponent createComponent() {
        return null;
    }

    protected String createCode() {
        return "calendaritem";
    };

    @Override
    protected String getParentFFName() {
        return "ID_CALENDAR_ITEM_GROUP";
    }
}
