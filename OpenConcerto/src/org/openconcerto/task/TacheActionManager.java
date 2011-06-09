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
 
 package org.openconcerto.task;

import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;

public class TacheActionManager {

    Map<String, List<Class<? extends AbstractAction>>> m = new HashMap<String, List<Class<? extends AbstractAction>>>();

    private static TacheActionManager instance = new TacheActionManager();

    public void addTacheAction(String name, Class<? extends AbstractAction> c) {
        List<Class<? extends AbstractAction>> l = this.m.get(name);
        if (l == null) {
            l = new ArrayList<Class<? extends AbstractAction>>();
            this.m.put(name, l);
        }
        l.add(c);
    }

    public synchronized static TacheActionManager getInstance() {
        return instance;
    }

    public List<AbstractAction> getActionsForTaskRow(SQLRowAccessor row) {
        List<AbstractAction> l = new ArrayList<AbstractAction>();

        String type = row.getString("TYPE");
        if (type.trim().length() > 0) {
            final List<Class<? extends AbstractAction>> listClass = this.m.get(type);

            if (listClass != null) {
                for (Class<? extends AbstractAction> class1 : listClass) {

                    try {

                        if (class1 != null) {
                            Constructor<? extends AbstractAction> ctor = class1.getConstructor(SQLRowAccessor.class);
                            l.add(ctor.newInstance(row));
                        }
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return l;
    }
}
