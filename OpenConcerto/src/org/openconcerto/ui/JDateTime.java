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
 
 package org.openconcerto.ui;

import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.checks.ValidListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Allow to edit a date with a time.
 * 
 * @author Sylvain CUAZ
 */
public final class JDateTime extends JPanel implements ValueWrapper<Date> {

    private final JDate date;
    private final JTime time;
    private Date value;
    private final PropertyChangeSupport supp;

    /**
     * Create the component with the current hour.
     */
    public JDateTime() {
        this(false, false);
    }

    /**
     * Create the component.
     * 
     * @param fillWithCurrentDate <code>true</code> if this should be filled with the current date
     *        and hour, else empty.
     */
    public JDateTime(final boolean fillWithCurrentDate) {
        this(fillWithCurrentDate, fillWithCurrentDate);
    }

    /**
     * Create the component.
     * 
     * @param fillWithCurrentDate <code>true</code> if this should be filled with the current date,
     *        else empty.
     * @param fillWithCurrentTime <code>true</code> if this should be filled with the current hour,
     *        else empty.
     */
    public JDateTime(final boolean fillWithCurrentDate, final boolean fillWithCurrentTime) {
        super();
        this.setOpaque(false);
        this.date = new JDate(fillWithCurrentDate);
        this.time = new JTime(fillWithCurrentTime);
        this.supp = new PropertyChangeSupport(this);

        final PropertyChangeListener l = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateValue();
            }
        };
        this.date.addValueListener(l);
        this.time.addValueListener(l);

        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.8;
        c.weighty = 0;
        this.add(this.date, c);
        c.gridx++;
        c.weightx = 0.2;
        this.add(this.time, c);

        this.resetValue();
    }

    protected void updateValue() {
        if (this.date.getValue() == null) {
            this.value = null;
        } else if (this.time.getValue() == null) {
            this.value = new Date(this.date.getValue().getTime());
        } else {
            this.value = new Date(this.date.getValue().getTime() + this.time.getTimeInMillis());
        }
        this.supp.firePropertyChange("value", null, this.value);
    }

    @Override
    public final void resetValue() {
        this.date.resetValue();
        this.time.resetValue();
    }

    @Override
    public final void setValue(final Date val) {
        this.time.setValue(val);
        this.date.setValue(val == null ? null : new Date(val.getTime() - this.time.getTimeInMillis()));
    }

    @Override
    public final Date getValue() {
        return this.value;
    }

    @Override
    public final void addValueListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener("value", l);
    }

    @Override
    public void rmValueListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener("value", l);
    }

    @Override
    public JComponent getComp() {
        return this;
    }

    @Override
    public boolean isValidated() {
        return true;
    }

    @Override
    public void addValidListener(ValidListener l) {
        // nothing to do
    }

    @Override
    public String getValidationText() {
        return null;
    }
}
