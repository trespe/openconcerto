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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Translation manager, listen to locale of {@link UserProps} and load the corresponding bundle.
 * 
 * @author Sylvain
 */
public class UserPropsTM extends org.openconcerto.utils.i18n.TM {

    protected UserPropsTM() {
    }

    @Override
    protected void init() {
        final UserProps userProps = UserProps.getInstance();
        userProps.addListener(UserProps.LOCALE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateLocale((UserProps) evt.getSource());
            }
        });
        updateLocale(userProps);
    }

    protected void updateLocale(final UserProps userProps) {
        this.setLocale(userProps.getLocale());
    }
}
