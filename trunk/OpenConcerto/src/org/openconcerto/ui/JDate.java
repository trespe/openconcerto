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
import org.openconcerto.utils.FormatGroup;
import org.openconcerto.utils.TimeUtils;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.i18n.TM.MissingMode;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;
import javax.swing.KeyStroke;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.InternationalFormatter;
import javax.swing.text.JTextComponent;

import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXDatePickerFormatter;

/**
 * Un composant d'édition de date acceptant les formats "dd/MM/yy" et "d MMMM yyyy".
 * 
 * @author Sylvain CUAZ
 */
public final class JDate extends JXDatePicker implements ValueWrapper<Date>, TextComponent {

    static {
        final String formats = TM.getInstance().translate(MissingMode.NULL, "jdate.formats");
        // else keep SwingX defaults
        if (formats != null) {
            final String[] array = formats.split("\n");
            final UIDefaults lafDefaults = UIManager.getLookAndFeelDefaults();
            if (array.length > 0)
                lafDefaults.put("JXDatePicker.longFormat", array[0]);
            if (array.length > 1)
                lafDefaults.put("JXDatePicker.mediumFormat", array[1]);
            if (array.length > 2)
                lafDefaults.put("JXDatePicker.shortFormat", array[2]);
            if (array.length > 3)
                Log.get().warning("Some formats ignored " + formats);
        }
    }

    private static boolean CommitEachValidEditDefault = false;

    public static void setCommitEachValidEditDefault(final boolean commitEachValidEditDefault) {
        CommitEachValidEditDefault = commitEachValidEditDefault;
    }

    public static boolean getCommitEachValidEditDefault() {
        return CommitEachValidEditDefault;
    }

    private final boolean fillWithCurrentDate;
    private final boolean commitEachValidEdit;
    private final Calendar cal;

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
     * @see #getCommitEachValidEditDefault()
     */
    public JDate(final boolean fillWithCurrentDate) {
        this(fillWithCurrentDate, getCommitEachValidEditDefault());
    }

    /**
     * Create a date editing component.
     * 
     * @param fillWithCurrentDate <code>true</code> if the initial value should be the current date,
     *        <code>false</code> if the initial value should be <code>null</code>.
     * @param commitEachValidEdit <code>true</code> if each valid edit should change our
     *        {@link #getValue() value}, <code>false</code> to wait for user action (typically when
     *        leaving or hitting enter).
     * @see DefaultFormatter#setCommitsOnValidEdit(boolean)
     */
    public JDate(final boolean fillWithCurrentDate, final boolean commitEachValidEdit) {
        super();
        this.fillWithCurrentDate = fillWithCurrentDate;
        this.commitEachValidEdit = commitEachValidEdit;

        final InputMap inputMap = this.getEditor().getInputMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "dayToFuture");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "dayToPast");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK), "monthToFuture");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK), "monthToPast");
        final ActionMap actionMap = this.getEditor().getActionMap();
        this.cal = Calendar.getInstance(this.getMonthView().getLocale());
        actionMap.put("dayToPast", createChangeDateAction(Calendar.DAY_OF_YEAR, -1));
        actionMap.put("dayToFuture", createChangeDateAction(Calendar.DAY_OF_YEAR, 1));
        actionMap.put("monthToPast", createChangeDateAction(Calendar.MONTH, -1));
        actionMap.put("monthToFuture", createChangeDateAction(Calendar.MONTH, 1));

        this.resetValue();
    }

    public final boolean fillsWithCurrentDate() {
        return this.fillWithCurrentDate;
    }

    protected final Calendar getCal() {
        return this.cal;
    }

    protected AbstractAction createChangeDateAction(final int field, final int amount) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                Date currentVal = getDate();
                if (currentVal == null && fillsWithCurrentDate())
                    currentVal = new Date();
                if (currentVal != null) {
                    getCal().setTime(currentVal);
                    getCal().add(field, amount);
                    setValue(getCal().getTime());
                }
            }
        };
    }

    @Override
    public void setFormats(final DateFormat[] formats) {
        final InternationalFormatter formatter = new InternationalFormatter(new FormatGroup(formats)) {
            @Override
            public Object stringToValue(final String text) throws ParseException {
                // JXDatePickerFormatter used to handle null date ; InternationalFormatter only use
                // the formats which obviously fail to parse "" and so revert the empty value.
                if (text == null || text.isEmpty())
                    return null;
                return super.stringToValue(text);
            }
        };
        formatter.setCommitsOnValidEdit(this.commitEachValidEdit);
        this.getEditor().setFormatterFactory(new DefaultFormatterFactory(formatter));
    }

    @Override
    public DateFormat[] getFormats() {
        final AbstractFormatterFactory factory = this.getEditor().getFormatterFactory();
        if (factory != null) {
            final AbstractFormatter formatter = factory.getFormatter(this.getEditor());
            if (formatter instanceof JXDatePickerFormatter) {
                return ((JXDatePickerFormatter) formatter).getFormats();
            } else if (formatter instanceof InternationalFormatter) {
                final Format format = ((InternationalFormatter) formatter).getFormat();
                if (format instanceof DateFormat) {
                    return new DateFormat[] { (DateFormat) format };
                } else if (format instanceof FormatGroup) {
                    final List<Format> formats = new ArrayList<Format>(((FormatGroup) format).getFormats());
                    final Iterator<Format> iter = formats.iterator();
                    while (iter.hasNext()) {
                        final Format element = iter.next();
                        if (!(element instanceof DateFormat))
                            iter.remove();
                    }
                    return formats.toArray(new DateFormat[formats.size()]);
                }
            }
        }
        return null;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // replace JXDatePickerFormatter by InternationalFormatter
        this.setFormats(this.getFormats());
        // can't change BasicDatePickerUI behavior, so do it here
        for (final Component child : this.getComponents()) {
            if (child instanceof JButton) {
                ((JComponent) child).setOpaque(false);
                ((JButton) child).setContentAreaFilled(false);
            }
        }
    }

    /**
     * Reset the component as if it has just been created. If {@link #fillsWithCurrentDate()} then
     * the date at the time this method is called will be used (not the date of creation).
     */
    @Override
    public final void resetValue() {
        if (this.fillsWithCurrentDate()) {
            this.setValue(new Date());
        } else {
            this.setValue(null);
        }
    }

    /**
     * Set the value after clearing the time part.
     * 
     * @param date the new value.
     */
    @Override
    public final void setValue(final Date date) {
        final Date timeless;
        if (date == null) {
            timeless = null;
        } else {
            getCal().setTime(date);
            TimeUtils.clearTime(getCal());
            timeless = getCal().getTime();
        }
        this.setDate(timeless);
    }

    @Override
    public final Date getValue() {
        return this.getDate();
    }

    public final boolean isEmpty() {
        return this.getValue() == null;
    }

    @Override
    public final void addValueListener(final PropertyChangeListener l) {
        this.getEditor().addPropertyChangeListener("value", l);
    }

    @Override
    public void rmValueListener(final PropertyChangeListener l) {
        this.getEditor().removePropertyChangeListener("value", l);
    }

    @Override
    public JComponent getComp() {
        return this;
    }

    @Override
    public ValidState getValidState() {
        return ValidState.getTrueInstance();
    }

    @Override
    public void addValidListener(final ValidListener l) {
        // nothing to do
    }

    @Override
    public void removeValidListener(final ValidListener l) {
        // nothing to do
    }

    @Override
    public JTextComponent getTextComp() {
        return getEditor();
    }
}
