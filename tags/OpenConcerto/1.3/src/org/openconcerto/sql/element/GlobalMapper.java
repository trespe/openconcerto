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
 
 package org.openconcerto.sql.element;

import org.openconcerto.ui.group.Group;
import org.openconcerto.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalMapper {
    private final Map<String, Object> idToObject = new HashMap<String, Object>();
    private final Map<Object, List<String>> objectsToIds = new HashMap<Object, List<String>>();

    private static GlobalMapper instance = new GlobalMapper();

    public GlobalMapper() {

    }

    public static GlobalMapper getInstance() {
        return instance;
    }

    public void map(String id, Object obj) {
        idToObject.put(id, obj);
        List<String> l = objectsToIds.get(obj);
        if (l == null) {
            l = new ArrayList<String>(3);
            l.add(id);
        } else if (!l.contains(obj)) {
            l.add(id);
        }
    }

    public Object get(String id) {
        return idToObject.get(id);
    }

    public String getString(String id) {
        final Object object = idToObject.get(id);
        if (object instanceof String) {
            return (String) object;
        }
        return null;
    }

    public Group getGroup(String id) {
        final Object object = idToObject.get(id);
        if (object instanceof Group) {
            return (Group) object;
        }
        return null;
    }

    public List<String> getIds(Object o) {
        return objectsToIds.get(o);
    }

    public void dump() {
        System.out.println(this.getClass().getName());
        List<String> ids = new ArrayList<String>();
        ids.addAll(this.idToObject.keySet());
        Collections.sort(ids);
        for (String id : ids) {
            System.out.println(StringUtils.leftAlign(id, 40) + " : " + idToObject.get(id));
        }
        System.out.println(ids.size() + " identifiers found");
    }
}
