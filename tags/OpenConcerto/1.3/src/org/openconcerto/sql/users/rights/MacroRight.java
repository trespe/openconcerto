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

import java.util.List;

/**
 * A right composed of other rights.
 * 
 * @author Sylvain
 */
public abstract class MacroRight {

    private final String code;

    public MacroRight(final String code) {
        this.code = code;
    }

    public final String getCode() {
        return this.code;
    }

    /**
     * Expand this macro right into its constituents.
     * 
     * @param mngr the right manager.
     * @param rightCode the code of the right, eg "ALL_RIGHTS_TABLE".
     * @param object the object, can be <code>null</code>, eg "TENSION".
     * @param haveRight whether the right is given or removed, eg <code>true</code>.
     * @return the list of rights.
     */
    public abstract List<RightTuple> expand(UserRightsManager mngr, String rightCode, String object, boolean haveRight);
}
