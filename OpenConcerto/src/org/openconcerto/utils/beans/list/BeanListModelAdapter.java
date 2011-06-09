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
 
 package org.openconcerto.utils.beans.list;

import org.openconcerto.utils.beans.Bean;
import org.openconcerto.utils.change.CollectionChangeEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractListModel;

/**
 * A class that expose a collection property of a bean as a ListModel. The collection should be a
 * List for better performance (another issue is iteration order).
 * 
 * @author Sylvain CUAZ
 */
public class BeanListModelAdapter extends AbstractListModel implements PropertyChangeListener {

    // cache of the bean's collection (also usefull for non list)
    protected final List<Object> items;
    private Bean bean;
    private final PropertyDescriptor desc;

    public BeanListModelAdapter(Bean b, PropertyDescriptor desc) {
        this.items = new ArrayList<Object>();
        this.desc = desc;
        this.setBean(b);
    }

    public Bean getBean() {
        return this.bean;
    }

    // TODO test than b and this.desc is compatible.
    public void setBean(Bean b) {
        if (this.bean != null) {
            this.bean.removePropertyChangeListener(this.desc.getName(), this);
        }
        this.bean = b;
        this.setItems();
        if (this.bean != null) {
            this.bean.addPropertyChangeListener(this.desc.getName(), this);
        }
    }

    private boolean isList() {
        return List.class.isAssignableFrom(this.desc.getReadMethod().getReturnType());
    }

    protected synchronized void setItems() {
        this.items.clear();
        if (this.bean != null) {
            Collection<?> col = null;
            try {
                col = (Collection) this.desc.getReadMethod().invoke(this.bean, new Object[0]);
            } catch (IllegalArgumentException e) {
                // can't happen no args
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // can't happen getter is public
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                throw new IllegalArgumentException("pb getting collection : " + e);
            }
            this.items.addAll(col);
        }
        this.fireContentsChanged(this, 0, this.getSize());
    }

    public int getSize() {
        return this.items.size();
    }

    public Object getElementAt(int index) {
        return this.items.get(index);
    }

    public List getList() {
        return Collections.unmodifiableList(this.items);
    }

    // our bean has changed its property
    public void propertyChange(PropertyChangeEvent evt) {
        if (!(evt instanceof CollectionChangeEvent)) {
            throw new IllegalStateException(evt + " not an instance of " + CollectionChangeEvent.class);
        }
        final CollectionChangeEvent e = (CollectionChangeEvent) evt;

        if (this.isList()) {
            synchronized (this) {
                if (e.isOnlyAddition()) {
                    final Iterator iter = e.getIntervalsAdded().iterator();
                    while (iter.hasNext()) {
                        final int[] interval = (int[]) iter.next();
                        for (int i = interval[0]; i <= interval[1]; i++) {
                            this.items.add(i, ((List) evt.getNewValue()).get(i));
                        }
                        this.fireIntervalAdded(this, interval[0], interval[1]);
                    }
                } else if (e.isOnlyRemoval()) {
                    // FIXME removeAll is too much if the same element is more than once
                    this.items.removeAll(e.getItemsRemoved());
                    final List intervalsRemoved = e.getIntervalsRemoved();
                    for (int j = intervalsRemoved.size() - 1; j >= 0; j--) {
                        final int[] interval = (int[]) intervalsRemoved.get(j);
                        this.fireIntervalRemoved(this, interval[0], interval[1]);
                    }
                } else
                    this.setItems();
            }
        } else {
            this.setItems();
        }

    }
}
