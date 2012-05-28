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
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProviderManager;
import org.openconcerto.sql.users.User;
import org.openconcerto.sql.users.UserManager;

public class UserCurrentInitialsValueProvider extends UserInitialsValueProvider {

    @Override
    public Object getValue(SpreadSheetCellValueContext context) {
        final User currentUser = UserManager.getInstance().getCurrentUser();
        final String firstName = currentUser.getFirstName();
        final String name = currentUser.getName();
        return getInitials(firstName, name);
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("InitialesUtilisateur", new UserCurrentInitialsValueProvider());
        SpreadSheetCellValueProviderManager.put("user.current.initials", new UserCurrentInitialsValueProvider());
    }
}
