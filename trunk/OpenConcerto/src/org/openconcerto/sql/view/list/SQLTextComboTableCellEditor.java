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

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.list.selection.BaseListStateModel;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventObject;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;

public class SQLTextComboTableCellEditor extends AbstractCellEditor implements TableCellEditor {

    private SQLRequestComboBox comboBox;

    private Where w;
    // Stock Value of Combo to fix problem with undefined
    int val = 1;

    boolean addUndefined;

    private IListFrame listFrame = null;

    public SQLTextComboTableCellEditor(final SQLElement elt, final boolean addUndefined) {
        this(elt, addUndefined, false);
    }

    /**
     * 
     * @param elt Element à afficher dans la combo
     * @param addUndefined ajout de l'indéfini
     * @param chooseInListe possibilité de choisir via une IListe
     */
    public SQLTextComboTableCellEditor(final SQLElement elt, final boolean addUndefined, boolean chooseInListe) {
        super();

        this.addUndefined = addUndefined;

        this.comboBox = new SQLRequestComboBox(addUndefined);

        // Mimic JTable.GenericEditor behavior
        this.comboBox.getTextComp().setBorder(new EmptyBorder(0, 0, 0, 18));
        this.comboBox.setBorder(new LineBorder(Color.black));
        this.comboBox.getPulseComponents().iterator().next().setBorder(null);

        ComboSQLRequest c = elt.getComboRequest(true);
        this.comboBox.uiInit(c);

        if (chooseInListe) {
            this.comboBox.getActions().add(0, new AbstractAction("Tout afficher") {
                @Override
                public void actionPerformed(ActionEvent e) {

                    if (SQLTextComboTableCellEditor.this.listFrame == null) {
                        SQLTextComboTableCellEditor.this.listFrame = new IListFrame(new ListeAddPanel(elt));

                        SQLTextComboTableCellEditor.this.listFrame.getPanel().getListe().getSelection().addPropertyChangeListener("userSelectedID", new PropertyChangeListener() {
                            @Override
                            public void propertyChange(PropertyChangeEvent evt) {
                                final int newID = ((Number) evt.getNewValue()).intValue();
                                SQLTextComboTableCellEditor.this.comboBox.setValue(newID == BaseListStateModel.INVALID_ID ? null : newID);
                            }
                        });
                        SQLTextComboTableCellEditor.this.listFrame.getPanel().getListe().selectID(SQLTextComboTableCellEditor.this.comboBox.getSelectedId());
                    }
                    FrameUtil.show(SQLTextComboTableCellEditor.this.listFrame);
                }
            });
        }
    }

    public SQLRequestComboBox getCombo() {
        return this.comboBox;
    }

    public void addAction(Action a) {
        this.comboBox.getActions().add(a);
    }

    public boolean isCellEditable(EventObject e) {

        if (e instanceof MouseEvent) {
            return ((MouseEvent) e).getClickCount() >= 2;
        }
        return super.isCellEditable(e);
    }

    public Object getCellEditorValue() {
        return this.val;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (value != null) {
            this.val = (Integer) value;
            this.comboBox.setValue(this.val);
        }
        this.comboBox.grabFocus();

        this.comboBox.addValueListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!SQLTextComboTableCellEditor.this.comboBox.isUpdating()) {
                    SQLTextComboTableCellEditor.this.val = SQLTextComboTableCellEditor.this.comboBox.getSelectedId();
                }
            }
        });
        // Filtre sur une valeur specifique
        if (this.fieldWhere != null && table instanceof RowValuesTable) {
            RowValuesTable rowVals = (RowValuesTable) table;
            SQLRowValues rowValues = rowVals.getRowValuesTableModel().getRowValuesAt(row);
            if (rowValues.isForeignEmpty(this.fieldWhere.getName())) {

                if (this.w != null) {
                    this.comboBox.getRequest().setWhere(this.w);
                } else {
                    this.comboBox.getRequest().setWhere(null);
                }
            } else {
                final Where w2 = new Where(this.fieldWhere, "=", rowValues.getForeign(this.fieldWhere.getName()).getID());
                if (this.w != null) {
                    this.comboBox.getRequest().setWhere(this.w.and(w2));
                } else {
                    this.comboBox.getRequest().setWhere(w2);
                }
            }
        }
        return this.comboBox;

    }

    public int getComboSelectedId() {
        return SQLTextComboTableCellEditor.this.comboBox.getSelectedId();
    }

    private SQLField fieldWhere;

    public void setDynamicWhere(SQLField field) {
        this.fieldWhere = field;
    }

    public void setWhere(Where w) {
        this.w = w;
        this.comboBox.getRequest().setWhere(w);
    }

    public void addSelectionListener(PropertyChangeListener l) {
        this.comboBox.addValueListener(l);
    }
}
