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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.ui.TextAreaRenderer;
import org.openconcerto.ui.TextAreaTableCellEditor;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class SQLTableElement {

    private Class<?> typeClass;

    private String rowField;

    private final SQLField field;

    private CellDynamicModifier modifier;

    private String name;

    private TableCellEditor editor;
    private TableCellRenderer renderer;
    private SQLBase base;
    private boolean isEditable;
    private boolean addElement = false;
    private boolean chooseInListe = false;
    private int preferredSize = 70;
    private boolean addUndefined = false;

    private SQLTextComboTableCellEditor comboBox = null;
    private Where w;
    private SQLRowValues defaultRowValues = null;

    private List<SQLTableElement> modificationListener = new Vector<SQLTableElement>();

    private int EMPTY_ID = 1;
    public static final String UNDEFINED_STRING = "-- Indéfini --";

    public SQLTableElement(SQLField field, Class<?> classe, TableCellEditor editor) {
        this(field, classe);
        this.editor = editor;
    }

    public SQLTableElement(SQLField field, boolean addUndefined) {
        this(field);
        this.EMPTY_ID = field.getTable().getUndefinedID();
        this.addUndefined = addUndefined;
    }

    public SQLTableElement(SQLField field, boolean addUndefined, boolean addElement, SQLRowValues rowVals) {
        this(field);
        this.EMPTY_ID = field.getTable().getUndefinedID();
        this.addUndefined = addUndefined;
        this.addElement = addElement;
        this.defaultRowValues = rowVals;
    }

    public SQLTableElement(SQLField field, boolean addUndefined, boolean addElement, boolean chooseInListe) {
        this(field, addUndefined, addElement);
        this.chooseInListe = chooseInListe;
    }

    public SQLTableElement(SQLField field, boolean addUndefined, boolean addElement) {
        this(field);
        this.EMPTY_ID = field.getTable().getUndefinedID();
        this.addUndefined = addUndefined;
        this.addElement = addElement;
    }

    public int getPreferredSize() {
        return this.preferredSize;
    }

    public void setPreferredSize(int size) {
        this.preferredSize = size;
    }

    public SQLTableElement(SQLField field, Class<?> classe) {
        super();
        this.base = field.getTable().getBase();
        if (classe == null) {
            if (field.isKey()) {
                this.typeClass = Integer.class;
            } else {
                this.typeClass = String.class;
            }
        } else {

            this.typeClass = classe;
        }
        this.rowField = field.getName();
        this.field = field;

        this.name = Configuration.getInstance().getTranslator().getTitleFor(field);
        this.isEditable = (field != null);
    }

    public SQLTableElement(SQLField field) {
        this(field, field.getType().getJavaType());
    }

    public SQLTableElement(Class<?> class1, String name) {
        super();
        this.typeClass = class1;
        this.name = name;
        this.field = null;
        this.isEditable = false;
    }

    public SQLTableElement(String title) {
        this(null, title);
    }

    public String toString() {
        return "SQLTableElement: " + this.name + " class:" + this.typeClass + ", rowField:" + this.rowField + " field:" + this.field;
    }

    /**
     * Retourne le CellEditor correspondant
     * 
     * @return null si on ne veut pas en spécifier un spécial
     */
    public TableCellEditor getTableCellEditor(JTable table) {
        if (this.editor != null) {
            return this.editor;
        }

        if (this.field != null && this.field.isKey()) {
            final SQLTable fTable = this.base.getGraph().getForeignTable(this.field);
            final SQLElement element = Configuration.getInstance().getDirectory().getElement(fTable);

            this.comboBox = new SQLTextComboTableCellEditor(element, this.addUndefined, this.chooseInListe);

            // System.err.println("New Combo");
            if (this.addElement) {
                final AbstractAction abstractAction = new AbstractAction("Ajouter") {

                    @Override
                    public void actionPerformed(ActionEvent e) {

                        EditFrame frame = new EditFrame(element, EditFrame.CREATION);
                        if (SQLTableElement.this.defaultRowValues != null) {
                            frame.getPanel().getSQLComponent().select(SQLTableElement.this.defaultRowValues);
                        }
                        frame.pack();
                        frame.setVisible(true);
                    }
                };

                this.comboBox.addAction(abstractAction);
            }
            if (this.w != null) {
                this.comboBox.setWhere(this.w);
            }
            this.editor = this.comboBox;
            return this.comboBox;

        }
        if (this.field != null && this.field.getType().getJavaType() == String.class) {
            TextAreaTableCellEditor textEditor = new TextAreaTableCellEditor(table);// new
            // DefaultCellEditor(comboBox);
            textEditor.setLimitedSize(this.field.getType().getSize());
            return textEditor;
        }
        return null;
    }

    public void setWhereCombo(final Where w) {
        if (this.comboBox != null) {
            this.comboBox.setWhere(w);
        }
        this.w = w;
    }

    public TableCellRenderer getTableCellRenderer() {
        if (this.renderer != null) {
            return this.renderer;
        }
        if (this.field != null && this.field.isKey() && !this.field.isPrimaryKey()) {
            final SQLTable fTable = this.base.getGraph().getForeignTable(this.field);
            if (fTable == null) {
                throw new IllegalStateException("No foreign table for field:" + this.field.getFullName());
            }
            final SQLElement element = Configuration.getInstance().getDirectory().getElement(fTable);
            return new KeyTableCellRenderer(element);
        }

        if (this.field != null && this.field.getType().getJavaType() == String.class) {
            return new TextAreaRenderer();
        }

        return null;
    }

    public Class<?> getElementClass() {
        return this.typeClass;
    }

    public final SQLField getField() {
        return this.field;
    }

    /**
     * @return Returns the rowField.
     */
    protected String getRowField() {
        return this.rowField;
    }

    protected Object getDefaultNullValue() {
        return null;
    }

    public Object convertEditorValueToModel(final Object value, final SQLRowValues row) {

        // if (value instanceof IComboSelectionItem) {
        if (this.field != null && this.field.isKey()) {
            Number v = (Number) value;
            if (v == null || v.longValue() < SQLRow.MIN_VALID_ID)
                return this.field.getTable().getUndefinedID();
            else
                return v;
        }
        // }
        if (value == null && getDefaultNullValue() != null) {
            return getDefaultNullValue();
        } else {
            return value;
        }
    }

    public void fireModification(final SQLRowValues row) {
        // System.out.println("FireModification: from:" + this.name);
        for (int i = 0; i < this.modificationListener.size(); i++) {
            SQLTableElement element = this.modificationListener.get(i);
            // System.out.println("FireModification: from:" + this.name + " to:" + element.name);
            element.valueModified(row);
        }
    }

    private void valueModified(final SQLRowValues row) {
        if (this.modifier != null) {
            Object newValue = this.modifier.computeValueFrom(row);
            if (this.rowField != null) {
                row.put(this.rowField, newValue);
            }
            this.modifier.setValueFrom(row, newValue);
            fireModification(row);
        }

    }

    /**
     * @return Returns the modifier.
     */
    protected CellDynamicModifier getModifier() {
        return this.modifier;
    }

    public String getColumnName() {
        return this.name;
    }

    /**
     * Sets the editor.
     */
    public void setEditor(final TableCellEditor aTableCellEditor) {
        this.editor = aTableCellEditor;
    }

    /**
     * @param renderer The renderer to set.
     */
    public void setRenderer(TableCellRenderer renderer) {
        this.renderer = renderer;
    }

    // public boolean isCellEditable() {
    // return isEditable;
    // }

    public void setEditable(boolean b) {
        this.isEditable = b;
    }

    public void setModifier(CellDynamicModifier modifier2) {
        this.modifier = modifier2;

    }

    public Object getValueFrom(final SQLRowValues row) {
        Object result;
        if (this.getModifier() != null) {
            result = this.getModifier().getValueFrom(row);
        } else {
            result = row.getObject(this.getRowField());
        }
        return result;
    }

    public void setValueFrom(final SQLRowValues row, final Object value) {
        if (this.getModifier() != null) {
            this.getModifier().setValueFrom(row, value);
        }
        if (this.getRowField() != null) {
            row.put(this.getRowField(), value);
        }
        fireModification(row);
    }

    public void addModificationListener(SQLTableElement tableElement) {
        if (!this.modificationListener.contains(tableElement)) {
            this.modificationListener.add(tableElement);
        }
    }

    public boolean isCellEditable(SQLRowValues vals) {

        return this.isEditable;
    }

    /**
     * Clear de la map du CellDynamicModifier
     */
    public void clear() {
        if (this.modifier != null) {
            this.modifier.clear();
        }
    }
}
