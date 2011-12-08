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
 
 package org.openconcerto.sql.users.rights;

import org.openconcerto.sql.users.rights.UserRightsManager.RightTuple;

import java.util.ArrayList;
import java.util.List;

public class LockAdminUserRight extends MacroRight {
    public static final String NAME = "LOCK_ADMIN";
    public static final String LOCK_MENU_ADMIN = "LOCK_MENU_ADMIN";

    public LockAdminUserRight() {
        super(NAME);
    }
    
    @Override
    public List<RightTuple> expand(UserRightsManager mngr, String rightCode, String object, boolean haveRight) {
        final List<RightTuple> res = new ArrayList<RightTuple>();
        res.add(new RightTuple(LOCK_MENU_ADMIN, haveRight));
        res.add(TableAllRights.createRight(mngr.getRoot().findTable("USER_COMMON"), haveRight));
        res.add(TableAllRights.createRight(mngr.getRoot().findTable("USER_RIGHT"), haveRight));
        return res;
    }
}
