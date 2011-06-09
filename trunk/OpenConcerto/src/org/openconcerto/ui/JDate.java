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

import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.checks.ValidListener;

import java.beans.PropertyChangeListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.calendar.JXMonthView;

/**
 * Un composant d'édition de date acceptant les formats "dd/MM/yy" et "d MMMM yyyy".
 * 
 * @author Sylvain CUAZ
 */
public final class JDate extends JXDatePicker implements ValueWrapper<Date>, TextComponent {

    private final boolean fillWithCurrentDate;

    /**
     * Créé un composant d'édition de date, vide.
     */
    public JDate() {
        this(false);
    }

    /**
     * Créé un composant d'édition de date.
     * 
     * @param fillWithCurrentDate <code>true</code> si on veut préremplir avec la date
     *        d'aujourd'hui, sinon vide.
     */
    public JDate(boolean fillWithCurrentDate) {
        super();
        this.fillWithCurrentDate = fillWithCurrentDate;

        this.setFormats(new String[] { "dd/MM/yy", "d MMMM yyyy" });
        final JXMonthView monthView = new JXMonthView();
        monthView.setFirstDayOfWeek(Calendar.getInstance().getFirstDayOfWeek());
        monthView.setTraversable(true);
        this.setMonthView(monthView);

        this.resetValue();
    }

    public final void resetValue() {
        if (this.fillWithCurrentDate) {
            this.setValue(new Date());
        } else {
            this.setValue(null);
        }
    }

    public final void setValue(Date date) {
        this.setDate(date);
    }

    public final Date getValue() {
        return this.getDate();
    }

    public final boolean isEmpty() {
        return this.getValue() == null;
    }

    public final void addValueListener(PropertyChangeListener l) {
        this.getEditor().addPropertyChangeListener("value", l);
    }

    public void rmValueListener(PropertyChangeListener l) {
        this.getEditor().removePropertyChangeListener("value", l);
    }

    public JComponent getComp() {
        return this;
    }

    public boolean isValidated() {
        return true;
    }

    public void addValidListener(ValidListener l) {
        // nothing to do
    }

    public String getValidationText() {
        return null;
    }

    public JTextComponent getTextComp() {
        return getEditor();
    }
}
