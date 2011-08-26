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
 
 package org.openconcerto.ui.preferences;

import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Calendar;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.EventListenerList;

public abstract class DefaultPreferencePanel extends JPanel implements PreferencePanel, ActionListener {

    private long showTime = Calendar.getInstance().getTimeInMillis();

    // State of the panel
    private boolean hasBeenModified = false;

    EventListenerList listeners = new EventListenerList();

    // Listener sur le panel
    public ContainerListener containerListener = new ContainerAdapter() {
        @Override
        public void componentAdded(ContainerEvent e) {
            addListener(e.getChild());
        }
    };

    public DefaultPreferencePanel() {
        this.setOpaque(false);
        this.addContainerListener(this.containerListener);
        fireModifyChange(false);
    }

    @Override
    public void uiInit() {
    }

    public final void apply() {
        storeValues();
        fireModifyChange(false);
    }

    abstract public void storeValues();

    abstract public void restoreToDefaults();

    abstract public String getTitleName();

    public boolean equals(Object obj) {
        if (obj instanceof DefaultPreferencePanel) {
            DefaultPreferencePanel p = (DefaultPreferencePanel) obj;
            return p.getTitleName().equals(getTitleName());
        }
        return super.equals(obj);
    }

    public int hashCode() {
        return getTitleName().hashCode();
    }

    @Override
    public void addModifyChangeListener(PreferencePanelListener l) {
        this.listeners.add(PreferencePanelListener.class, l);
        fireModifyChange(this.hasBeenModified);
    }

    public void fireModifyChange(boolean b) {
        long time = Calendar.getInstance().getTimeInMillis();

        if (this.showTime + 2000 < time) {
            this.hasBeenModified = b;
            for (PreferencePanelListener l : this.listeners.getListeners(PreferencePanelListener.class)) {

                l.valueModifyChanged(b);
            }
        }
    }

    @Override
    public boolean isModified() {
        return this.hasBeenModified;
    }

    /**
     * add listeners on the components of the panel in order to fire any modifications
     * 
     * @param c
     */
    private void addListener(Component c) {
        // System.err.println("Component Added " + c);
        // if (ValueObject.class.isAssignableFrom(c.getClass())) {
        // ValueObject<?> obj = (ValueObject<?>) c;
        // obj.addValueListener(new PropertyChangeListener() {
        // @Override
        // public void propertyChange(PropertyChangeEvent evt) {
        // fireModifyChange(true);
        // }
        // });
        // } else {

        if (JTextField.class.isAssignableFrom(c.getClass())) {
            JTextField text = (JTextField) c;
            text.getDocument().addDocumentListener(new SimpleDocumentListener() {
                public void update(javax.swing.event.DocumentEvent e) {

                    fireModifyChange(true);
                };
            });
        } else {
            if (JCheckBox.class.isAssignableFrom(c.getClass())) {
                final JCheckBox box = (JCheckBox) c;
                box.addActionListener(this);
            } else {
                if (JPanel.class.isAssignableFrom(c.getClass())) {
                    JPanel panel = (JPanel) c;
                    Component[] cs = panel.getComponents();
                    for (int i = 0; i < cs.length; i++) {
                        addListener(cs[i]);
                    }
                } else {
                    if (JScrollPane.class.isAssignableFrom(c.getClass())) {

                        JScrollPane panel = (JScrollPane) c;
                        Component[] cs = panel.getViewport().getComponents();

                        for (int i = 0; i < cs.length; i++) {
                            addListener(cs[i]);
                        }

                    } else {
                        if (JRadioButton.class.isAssignableFrom(c.getClass())) {
                            JRadioButton radio = (JRadioButton) c;
                            radio.addActionListener(this);
                        } else {
                            if (JLabel.class.isAssignableFrom(c.getClass())) {
                                JLabel label = (JLabel) c;
                                label.addPropertyChangeListener(new PropertyChangeListener() {
                                    @Override
                                    public void propertyChange(PropertyChangeEvent evt) {
                                        if (evt.getPropertyName().equalsIgnoreCase("text")) {
                                            fireModifyChange(true);
                                        }
                                    }
                                });
                            } else {
                                if (JComboBox.class.isAssignableFrom(c.getClass())) {
                                    JComboBox combo = (JComboBox) c;
                                    combo.addActionListener(this);
                                }
                            }
                        }
                    }
                }
            }
        }
        // }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        fireModifyChange(true);
    }

    // * ValidObject

    @Override
    public boolean isValidated() {
        return true;
    }

    @Override
    public String getValidationText() {
        return null;
    }

    @Override
    public void addValidListener(ValidListener l) {
        // nothing to do
    }

    @Override
    public void removeValidListener(ValidListener l) {
        // nothing to do
    }
}
