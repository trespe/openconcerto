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
 
 package org.openconcerto.ui.light;

import java.io.Serializable;

public class ColumnSpec implements Serializable {
    // Must stay immutable

    private String id;
    private Class valueClass;
    private String columnName;
    // Default value (to add a new line)
    private Object defaultValue;
    private int width;
    private boolean editable;
    private LightUIElement editors;

    public ColumnSpec(String id, Class valueClass, String columnName, Object defaultValue, int width, boolean editable, LightUIElement editors) {
        this.id = id;
        this.valueClass = valueClass;
        this.columnName = columnName;
        this.defaultValue = defaultValue;
        this.width = width;
        this.editable = editable;
        this.editors = editors;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Class getValueClass() {
        return valueClass;
    }

    public void setValueClass(Class valueClass) {
        this.valueClass = valueClass;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public LightUIElement getEditor() {
        return editors;
    }

    public void setEditors(LightUIElement editors) {
        this.editors = editors;
    }

}
