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
 
 package org.openconcerto.sql.users;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableModifiedListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserManager {
    private static UserManager instance;

    public synchronized static final UserManager getInstance() {
        if (instance == null && Configuration.getInstance() != null) {
            final SQLTable table = Configuration.getInstance().getRoot().findTable("USER_COMMON");
            if (table != null) {
                instance = new UserManager(table);
            }
        }
        return instance;
    }

    public static final int getUserID() {
        final UserManager mngr = getInstance();
        return mngr == null || mngr.getCurrentUser() == null ? SQLRow.NONEXISTANT_ID : mngr.getCurrentUser().getId();
    }

    private final SQLTable t;
    private final Map<Integer, User> byID;
    private boolean dirty;
    private User currentUser;

    private UserManager(final SQLTable t) {
        // to keep the ORDER for #getAllUser()
        this.byID = new LinkedHashMap<Integer, User>();
        this.currentUser = null;
        this.t = t;
        this.t.addTableModifiedListener(new SQLTableModifiedListener() {
            @Override
            public void tableModified(SQLTableEvent evt) {
                UserManager.this.dirty = true;
            }
        });
        fillUsers();
    }

    private synchronized void fillUsers() {
        this.byID.clear();

        final SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(new SQLRowValues(this.t).setAllToNull());
        fetcher.setOrdered(true);
        for (final SQLRowValues v : fetcher.fetch()) {
            final User u = new User(v.getID(), v.getString("NOM"));
            u.setLastName(v.getString("PRENOM"));
            u.setNickName(v.getString("SURNOM"));
            this.byID.put(v.getID(), u);
        }

        this.dirty = false;
    }

    public final SQLTable getTable() {
        return this.t;
    }

    private final Map<Integer, User> getUsers() {
        if (this.dirty) {
            fillUsers();
        }
        return this.byID;
    }

    public final User getCurrentUser() {
        return this.currentUser;
    }

    public void setCurrentUser(final int id) {
        this.currentUser = getUser(Integer.valueOf(id));
    }

    public synchronized List<User> getAllUser() {
        return new ArrayList<User>(this.getUsers().values());
    }

    public synchronized User getUser(final Integer v) {
        if (this.getUsers().containsKey(v))
            return this.getUsers().get(v);
        else
            throw new IllegalStateException("Bad user! " + v);
    }

}
