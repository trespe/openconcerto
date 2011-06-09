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

import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.beans.Bean;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.AbstractTableModel;

/**
 * Un modèle de table listant des CBean. Ne permet d'afficher qu'un seul type de bean.
 * 
 * @author ILM Informatique 5 juil. 2004
 */
public class CBeanTableModel extends AbstractTableModel implements PropertyChangeListener, ListDataListener {

    private Class beanClass;

    // [String]
    private List header;
    // [Method]
    private List getters;

    private ListModel model;

    private List beans;

    /**
     * Crée un modèle de table pour cette classe.
     * 
     * @param clazz la classe des éléments à accéder.
     * @param listModel la liste de beans à afficher.
     * @throws IllegalArgumentException si clazz n'est pas un CBean, ou si l'introspection échoue.
     */
    public CBeanTableModel(Class clazz, ListModel listModel) {
        this(clazz, listModel, null);
    }

    /**
     * Crée un modèle de table pour cette classe.
     * 
     * @param clazz la classe des éléments à accéder.
     * @param listModel la liste de beans à afficher.
     * @param props quelles propriétés afficher, peut être <code>null</code> pour demander au
     *        BeanInfo.
     * @throws IllegalArgumentException si clazz n'est pas un CBean, ou si l'introspection échoue.
     */
    public CBeanTableModel(Class clazz, ListModel listModel, PropertyDescriptor[] props) {
        if (!Bean.class.isAssignableFrom(clazz))
            throw new IllegalArgumentException("class " + clazz + "  is not a Bean");
        this.beanClass = clazz;
        if (props == null)
            this.setUp(clazz);
        else
            this.setUp(props);

        this.beans = new ArrayList();
        this.model = listModel;
        // initialiser this.beans
        this.modelChanged();
        this.model.addListDataListener(this);
    }

    private void setUp(Class clazz) {
        PropertyDescriptor[] properties = null;
        try {
            BeanInfo bi = Introspector.getBeanInfo(clazz);
            properties = bi.getPropertyDescriptors();
        } catch (IntrospectionException exn) {
            throw (IllegalArgumentException) ExceptionUtils.createExn(IllegalArgumentException.class, "Couldn't introspect " + clazz, exn);
        }
        this.setUp(properties);
    }

    private void setUp(PropertyDescriptor[] properties) {
        this.header = new ArrayList();
        this.getters = new ArrayList();

        for (int i = 0; i < properties.length; i++) {
            // Don't display hidden or expert properties.
            if (properties[i].isHidden() || properties[i].isExpert()) {
                continue;
            }
            final String name = properties[i].getDisplayName();
            this.header.add(name);
            if (properties[i].getReadMethod() == null)
                throw new IllegalArgumentException("Getter de " + name + "null en colonne:" + i);
            this.getters.add(properties[i].getReadMethod());
        }
    }

    private Method getGetter(int columnIndex) {
        return (Method) this.getters.get(columnIndex);
    }

    public ListModel getList() {
        return this.model;
    }

    public List getBeans() {
        return this.beans;
    }

    public List getBeans(int[] rows) {
        List result = new ArrayList(rows.length);
        for (int i = 0; i < rows.length; i++) {
            int row = rows[i];
            result.add(this.beans.get(row));
        }
        return result;
    }

    public int getRowCount() {
        return this.beans == null ? 0 : this.beans.size();
    }

    public int getColumnCount() {
        return this.header.size();
    }

    public Class getColumnClass(int columnIndex) {
        return getGetter(columnIndex).getReturnType();
    }

    public String getColumnName(int columnIndex) {
        return (String) this.header.get(columnIndex);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        Object result = null;
        try {
            result = getGetter(columnIndex).invoke(this.beans.get(rowIndex), new Object[0]);
        } catch (IllegalArgumentException e) {
            // impossible (pas d'arguments)
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // impossible (un getter est public)
            e.printStackTrace();
        } catch (InvocationTargetException exn) {
            throw (IllegalStateException) ExceptionUtils.createExn(IllegalStateException.class, "Can't get value with " + this.beans.get(rowIndex), exn);
        }
        return result;
    }

    /**
     * Appelé lorsque un de nos beans change.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        int row = this.beans.indexOf(evt.getSource());
        this.fireTableRowsUpdated(row, row);
    }

    // *** our list has changed

    public void contentsChanged(ListDataEvent e) {
        this.modelChanged();
    }

    public void intervalAdded(ListDataEvent e) {
        this.addedToModel(e.getIndex0(), e.getIndex1());
    }

    public void intervalRemoved(ListDataEvent e) {
        this.removedFromModel(e.getIndex0(), e.getIndex1());
    }

    /**
     * Reload all the beans from the list model.
     */
    private void modelChanged() {
        // vérification
        this.check(0, this.model.getSize() - 1);
        // ne plus écouter les anciens beans
        this.rm(0, this.beans.size() - 1);
        // écouter les nouveaux
        this.add(0, this.model.getSize() - 1);
        this.fireTableDataChanged();
    }

    private void removedFromModel(int first, int last) {
        this.rm(first, last);
        this.fireTableRowsDeleted(first, last);
    }

    private void addedToModel(int first, int last) {
        this.check(first, last);
        this.add(first, last);
        this.fireTableRowsInserted(first, last);
    }

    private void check(int first, int last) {
        // vérification
        for (int i = first; i <= last; i++) {
            Bean bean = (Bean) this.model.getElementAt(i);
            if (bean.getClass() != this.beanClass)
                throw new IllegalArgumentException("items[" + i + "]: '" + bean + "'(" + bean.getClass() + ") is not a " + this.beanClass.getName());
        }
    }

    // *** these don't fire*

    private void rm(int first, int last) {
        // ne plus écouter les anciens beans
        for (int i = last; i >= first; i--) {
            Bean bean = (Bean) this.beans.remove(i);
            bean.removePropertyChangeListener(this);
        }
    }

    private void add(int first, int last) {
        // écouter les nouveaux
        for (int j = first; j <= last; j++) {
            Bean bean = (Bean) this.model.getElementAt(j);
            bean.addPropertyChangeListener(this);
            this.beans.add(bean);
        }
    }

}
