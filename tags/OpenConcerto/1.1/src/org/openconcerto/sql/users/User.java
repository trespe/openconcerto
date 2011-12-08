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

import org.openconcerto.sql.users.rights.UserRights;

public class User {
    private final int id;
    private String name, lastName, nickName;
    private final UserRights userRights;

    public User(int id, String name) {
        this.id = id;
        this.name = name;
        this.userRights = new UserRights(this.getId());
    }

    public String getName() {
        return this.name;
    }

    public UserRights getRights() {
        return this.userRights;
    }

    public int getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return this.getFullName() + " /" + getId();
    }

    public void setLastName(String string) {
        this.lastName = string;
    }

    public void setNickName(String string) {
        this.nickName = string;
    }

    public String getLastName() {
        return this.lastName;
    }

    public String getNickName() {
        return this.nickName;
    }

    public String getFullName() {
        return getLastName() + " " + getName();
    }

}
