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
 
 package org.openconcerto.erp.generationDoc.provider;

import org.openconcerto.erp.generationDoc.SpreadSheetCellValueContext;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProvider;

public abstract class UserInitialsValueProvider implements SpreadSheetCellValueProvider {

    @Override
    public abstract Object getValue(SpreadSheetCellValueContext context);

    public String getInitials(String firstName, String name) {
        String initials = "";
        if (firstName != null) {
            final String tFirstName = firstName.trim();
            if (!tFirstName.isEmpty()) {
                initials += tFirstName.charAt(0);
            }
        }
        if (name != null) {
            final String tName = name.trim();
            if (!tName.isEmpty()) {
                initials += tName.charAt(0);
            }
        }
        return initials.toUpperCase();
    }

}
