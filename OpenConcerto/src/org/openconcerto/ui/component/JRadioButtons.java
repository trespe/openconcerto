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
 
 package org.openconcerto.ui.component;

import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.checks.EmptyChangeSupport;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.EmptyObj;
import org.openconcerto.utils.checks.MutableValueObject;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * A group of radio buttons (only one can be selected at any time). The buttons are identified by an
 * ID and are created when {@link #init()} is called. A special empty button is not shown, it is
 * selected when init() finishes.
 * 
 * @author Sylvain CUAZ
 * @param <V> type of value
 */
public class JRadioButtons<V> extends JPanel implements MutableValueObject<V>, EmptyObj {

    public static final class JStringRadioButtons extends JRadioButtons<String> {
        public JStringRadioButtons(final Collection<String> choices) {
            // keep order even if the passed collection is a set (it might still be ordered)
            super(CollectionUtils.fillMap(new LinkedHashMap<String, String>(), choices));
        }
    }

    private final PropertyChangeSupport supp;
    private final EmptyChangeSupport emptySupp;
    // {id => JRadioButton}
    private final Map<V, JRadioButton> choices;
    private final ButtonGroup group;
    // listeners to add to each of our buttons
    private final List<MouseListener> mouseListeners;

    private V emptyID;
    private V value;

    public JRadioButtons() {
        this(null);
    }

    public JRadioButtons(final Map<V, String> choices) {
        super();
        this.supp = new PropertyChangeSupport(this);
        this.emptySupp = new EmptyChangeSupport(this);
        // record the order of the buttons
        this.choices = new LinkedHashMap<V, JRadioButton>();
        this.group = new ButtonGroup();
        this.emptyID = null;
        this.mouseListeners = new ArrayList<MouseListener>();
        this.value = null;

        this.setOpaque(false);

        if (choices != null)
            this.init(null, choices);
    }

    /**
     * Init this component. If a value of <code>choices</code> is <code>null</code> the ID will be
     * used as the label.
     * 
     * @param emptyID the id meaning that no button (on screen) is selected, can be
     *        <code>null</code>.
     * @param choices the map from id to label of the buttons to display, ids cannot be
     *        <code>null</code>.
     */
    public final void init(final V emptyID, final Map<V, String> choices) {
        this.emptyID = emptyID;
        for (final Map.Entry<V, String> e : choices.entrySet()) {
            final V choice = e.getKey();
            if (choice == null)
                throw new IllegalArgumentException("A key is null in " + choices);
            final String choiceLabel = e.getValue();

            this.addBtn(choiceLabel == null ? choice.toString() : choiceLabel, choice);
        }
        // pour pouvoir "déselectionner"
        this.addBtn("--- ??? ---", this.emptyID);

        // Group the radio buttons.
        final Iterator<V> choicesIter = this.choices.keySet().iterator();
        while (choicesIter.hasNext()) {
            final V id = choicesIter.next();
            final JRadioButton btn = this.choices.get(id);
            this.group.add(btn);
            // maintain the value, otherwise we'd have to create a map from ButtonModel to ID
            // since this.group.getSelection() returns a ButtonModel
            btn.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    // ATTN we get 2 events for each change : first DESELECTED then SELECTED
                    // MAYBE ignore one but ATTN DESELECTED can be alone with
                    // ButtonGroup#clearSelection()
                    if (e.getStateChange() == ItemEvent.SELECTED)
                        valueChanged(id);
                    // DESELECTED
                    else if (JRadioButtons.this.value == id)
                        valueChanged(null);
                }
            });
            // ne pas mettre l'indéfini dans l'interface
            if (id != this.emptyID) {
                this.add(btn);
            }
        }

        // initialise la valeur à indéfini
        this.resetValue();
    }

    private void valueChanged(V id) {
        this.value = id;
        this.supp.firePropertyChange("value", null, id);
        this.emptySupp.fireEmptyChange(isEmpty());
    }

    private final void addBtn(String btnLabel, V id) {
        final JRadioButton btn = new JRadioButton(btnLabel);
        btn.setOpaque(false);
        for (final MouseListener l : this.mouseListeners) {
            btn.addMouseListener(l);
        }
        this.choices.put(id, btn);
    }

    public final void setEnabled(boolean b) {
        super.setEnabled(b);
        for (final JRadioButton btn : this.choices.values()) {
            btn.setEnabled(b);
        }
    }

    // ** valueObject

    /**
     * Set the selected button.
     * 
     * @param id the id of the button to select, <code>null</code> means the empty ID.
     */
    public void setValue(V id) {
        // treat unknown value as empty
        // MAYBE add a boolean to throw an exception
        if (id == null || !this.choices.containsKey(id))
            id = this.emptyID;
        this.choices.get(id).setSelected(true);
    }

    public final V getValue() {
        return this.value;
    }

    /**
     * Selects the empty ID.
     */
    public final void resetValue() {
        this.setValue(null);
    }

    public final void addValueListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(l);
    }

    public final void rmValueListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener(l);
    }

    // ** emptyObj

    public boolean isEmpty() {
        return this.getValue() == null || this.getValue().equals(this.emptyID);
    }

    public void addEmptyListener(EmptyListener l) {
        this.emptySupp.addEmptyListener(l);
    }

    // ** mouseListeners

    @Override
    public void addMouseListener(MouseListener l) {
        this.mouseListeners.add(l);
        for (final JRadioButton radio : this.choices.values()) {
            radio.addMouseListener(l);
        }
    }

    @Override
    public void removeMouseListener(MouseListener l) {
        this.mouseListeners.remove(l);
        for (final JRadioButton radio : this.choices.values()) {
            radio.removeMouseListener(l);
        }
    }
}
