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
 
 package org.openconcerto.sql;

import org.openconcerto.sql.preferences.UserProps;

import java.util.Locale;

/**
 * Translation manager, listen to locale of {@link UserProps} and apply it to lower translation
 * managers.
 * 
 * @author Sylvain
 */
public class TM extends UserPropsTM {

    static private TM INSTANCE;

    // to be called as soon as UserProps has the correct Locale to initialize other translation
    // managers
    static public TM getInstance() {
        if (INSTANCE == null)
            INSTANCE = new TM();
        return INSTANCE;
    }

    // useful for static import
    static public TM getTM() {
        return getInstance();
    }

    static public final String tr(final String key, final Object... args) {
        return getInstance().translate(key, args);
    }

    private TM() {
    }

    @Override
    protected void updateLocale(final UserProps userProps) {
        final Locale locale = userProps.getLocale();
        this.setLocale(locale);
        org.openconcerto.utils.i18n.TM.getInstance().setLocale(locale);
        org.openconcerto.ui.TM.getInstance().setLocale(locale);
    }
}
