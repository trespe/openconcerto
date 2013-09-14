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

import java.awt.Color;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LightUIElement implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 3272357171610073289L;

    public static final int TYPE_LABEL = 0;
    public static final int TYPE_TEXT_FIELD = 1;
    public static final int TYPE_DATE = 2;
    public static final int TYPE_COMBOBOX = 3;
    public static final int TYPE_LIST = 4;
    public static final int TYPE_CHECKBOX = 5;
    public static final int TYPE_TABBED_UI = 6;
    public static final int TYPE_COMBOBOX_ELEMENT = 7;
    public static final int TYPE_DESCRIPTOR = 8;
    public static final int TYPE_BUTTON = 20;
    public static final int TYPE_BUTTON_WITH_CONTEXT = 21;
    public static final int TYPE_BUTTON_CANCEL = 22;
    public static final int TYPE_BUTTON_UNMANAGED = 23;

    public static final int VALUE_TYPE_STRING = 0;
    public static final int VALUE_TYPE_INTEGER = 1;
    public static final int VALUE_TYPE_DATE = 2;
    public static final int VALUE_TYPE_REF = 3;
    public static final int VALUE_TYPE_LIST = 4;

    // Type
    private int type;
    // Layout
    private int gridWidth;
    private boolean fillWidth;
    // Values
    private String id;
    private String label;
    private String value;
    private Serializable rawContent;
    private int valueType;
    private int minInputSize;
    private List<LightUIDescriptor> tabs;
    // Colors
    private Color color;
    // Icon
    private String icon;
    private boolean required;

    private String toolTip;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getGridWidth() {
        return gridWidth;
    }

    public void setGridWidth(int gridWidth) {
        this.gridWidth = gridWidth;
    }

    public boolean isFillWidth() {
        return fillWidth;
    }

    public void setFillWidth(boolean fillWidth) {
        this.fillWidth = fillWidth;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getValueType() {
        return valueType;
    }

    public void setValueType(int valueType) {
        this.valueType = valueType;
    }

    public int getMinInputSize() {
        return minInputSize;
    }

    public void setMinInputSize(int minInputSize) {
        this.minInputSize = minInputSize;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public List<LightUIDescriptor> getTabs() {
        return tabs;
    }

    public String getToolTip() {
        return toolTip;
    }

    public void setToolTip(String toolTip) {
        this.toolTip = toolTip;
    }

    public void addTab(LightUIDescriptor desc) {
        if (this.tabs == null) {
            this.tabs = new ArrayList<LightUIDescriptor>(5);
        }
        this.tabs.add(desc);
    }

    public void dump(PrintStream out) {
        String type = "?";
        if (this.type == TYPE_CHECKBOX) {
            type = "checkbox";
        } else if (this.type == TYPE_COMBOBOX) {
            type = "combobox";
        } else if (this.type == TYPE_LABEL) {
            type = "label";
        } else if (this.type == TYPE_TEXT_FIELD) {
            type = "textfield";
        } else if (this.type == TYPE_LIST) {
            type = "list";
        } else if (this.type == TYPE_TABBED_UI) {
            type = "tabs";
        } else if (this.type == TYPE_BUTTON) {
            type = "button";
        } else if (this.type == TYPE_BUTTON_WITH_CONTEXT) {
            type = "button with context";
        } else if (this.type == TYPE_BUTTON_CANCEL) {
            type = "cancel button";
        } else if (this.type == TYPE_COMBOBOX_ELEMENT) {
            type = "combo element";
        }
        String valueType = "?";
        if (this.valueType == VALUE_TYPE_STRING) {
            valueType = "string";
        } else if (this.valueType == VALUE_TYPE_INTEGER) {
            valueType = "int";
        } else if (this.valueType == VALUE_TYPE_REF) {
            valueType = "ref";
        } else if (this.valueType == VALUE_TYPE_LIST) {
            valueType = "list";
        }

        out.println("LightUIElement" + " " + type + " id:" + this.id + " w:" + gridWidth + " fill:" + fillWidth + " value:" + value + "(" + valueType + ") label:" + label);

    }

    @Override
    public String toString() {
        return super.toString() + " " + id;
    }

    public Serializable getRawContent() {
        return rawContent;
    }

    public void setRawContent(Serializable rawContent) {
        this.rawContent = rawContent;
    }

}
