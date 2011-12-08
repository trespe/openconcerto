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
 
 package org.openconcerto.sql.ui;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.PolymorphFK;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.sql.sqlobject.itemview.RowItemViewComponent;
import org.openconcerto.utils.checks.MutableValueObject;
import org.openconcerto.utils.model.ListComboBoxModel;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.MutableComboBoxModel;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 * A class to edit a PolymorphFK.
 * 
 * @author Sylvain CUAZ
 */
public class IPolyCombo extends JPanel implements MutableValueObject<SQLRow>, RowItemViewComponent {

    /**
     * Creates a new instance from a list of tablenames using the default Configuration to find the
     * SQLElement.
     * 
     * @param root the context from which <code>tables</code> are found.
     * @param tables a list of tablenames, comma separated with no spaces, eg "RECEPTEUR,SOURCE".
     * @return the corresponding IPolyCombo.
     */
    public static final IPolyCombo create(DBRoot root, String tables) {
        final List<String> tablesL = SQLRow.toList(tables);
        final List<SQLElement> elements = new ArrayList<SQLElement>(tablesL.size());
        for (final String tableName : tablesL) {
            elements.add(Configuration.getInstance().getDirectory().getElement(root.getTableDesc(tableName)));
        }
        return new IPolyCombo(elements);
    }

    private PolymorphFK field;

    private final PropertyChangeSupport supp;

    private final JComboBox tableCombo;
    // <SQLElement, TableRowCombo>
    private final Map<SQLElement, ElementComboBox> idCombos;
    // <String, SQLElement>
    // necessary when reading the table name from DB to find the corresponding element
    private final Map<String, SQLElement> name2elem;

    /**
     * Creates a IPolyCombo to edit the passed list of SQLElement. No 2 elements can share the same
     * SQLTable.
     * 
     * @param elements a list of SQLElement.
     */
    public IPolyCombo(final List<SQLElement> elements) {
        this.supp = new PropertyChangeSupport(this);

        this.idCombos = new HashMap<SQLElement, ElementComboBox>();
        this.name2elem = new HashMap<String, SQLElement>();
        for (final SQLElement element : elements) {
            final ElementComboBox c = new ElementComboBox();
            c.addValueListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    fireValueChange();
                }
            });
            this.idCombos.put(element, c);
            this.name2elem.put(element.getTable().getName(), element);
        }
        if (this.name2elem.size() != this.idCombos.size())
            throw new IllegalArgumentException("size mismatch: " + this.idCombos.keySet() + " / " + this.name2elem.keySet());

        this.tableCombo = new JComboBox(new ListComboBoxModel(elements));
        // put the empty element at the top
        ((MutableComboBoxModel) this.tableCombo.getModel()).insertElementAt(null, 0);
        this.tableCombo.setRenderer(new BasicComboBoxRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                final SQLElement element = (SQLElement) value;
                c.setText(element == null ? SQLRequestComboBox.UNDEFINED_STRING : element.getSingularName());
                return c;
            }
        });
        // each time we change the table combo, we must change the second combo.
        this.tableCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateIdCombo();
            }
        });
    }

    protected final void fireValueChange() {
        this.supp.firePropertyChange("value", null, this.getValue());
    }

    protected void updateIdCombo() {
        if (this.getComponentCount() > 1)
            this.remove(1);
        if (this.getIdCombo() != null)
            this.add(this.getIdCombo());
        this.revalidate();
        this.fireValueChange();
    }

    /**
     * The currently selected SQLElement, eg a RecepteurElement.
     * 
     * @return the selected SQLElement or <code>null</code>.
     */
    private final SQLElement getSelectedElement() {
        return (SQLElement) this.tableCombo.getSelectedItem();
    }

    /**
     * The combo needed to edit the selected element.
     * 
     * @return a TableRowCombo containing rows of the selected element or <code>null</code>.
     */
    private final ElementComboBox getIdCombo() {
        return this.idCombos.get(this.getSelectedElement());
    }

    public void init(SQLRowItemView v) {
        final SQLField f = v.getField();
        this.field = new PolymorphFK(f.getTable(), PolymorphFK.tableField2propName(f));

        this.setLayout(new GridLayout(1, 2));
        this.add(this.tableCombo);
        // no need to add idCombo it's done by updateIdCombo()

        for (final SQLElement element : this.idCombos.keySet()) {
            final ElementComboBox c = this.idCombos.get(element);
            c.init(element);
        }

        this.resetValue();
    }

    public final void resetValue() {
        this.setValue(null);
    }

    public void setValue(SQLRow foreignRow) {
        final String tableName = foreignRow == null ? null : foreignRow.getTable().getName();
        this.tableCombo.setSelectedItem(this.name2elem.get(tableName));
        if (this.getIdCombo() != null)
            this.getIdCombo().setValue(foreignRow.getID());

    }

    public SQLRow getValue() {
        if (this.getIdCombo() == null)
            return null;
        else
            return this.getIdCombo().getSelectedRow();
    }

    public void addValueListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(l);
    }

    public void rmValueListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener(l);
    }

    public final PolymorphFK getPolymorphFK() {
        return this.field;
    }

    public void setEnabled(boolean b) {
        this.tableCombo.setEnabled(b);
        for (final JComponent idCombo : this.idCombos.values())
            idCombo.setEnabled(b);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " for " + this.field;
    }
}
