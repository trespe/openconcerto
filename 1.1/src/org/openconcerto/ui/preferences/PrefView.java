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

import org.openconcerto.ui.JDate;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.component.text.TextComponentUtils;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.ui.valuewrapper.ValueWrapperFactory;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.PrefType;
import org.openconcerto.utils.text.DocumentFilterList;
import org.openconcerto.utils.text.DocumentFilterList.FilterType;
import org.openconcerto.utils.text.LimitedSizeDocumentFilter;

import java.util.Date;
import java.util.prefs.Preferences;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;

/**
 * A view for one preference value in {@link JavaPrefPreferencePanel}.
 * 
 * @author Sylvain CUAZ
 * 
 * @param <T> type of value
 * @see #setDefaultValue(Object)
 */
public class PrefView<T> {

    private final PrefType<T> type;
    private final int length;
    private final ValueWrapper<T> vw;
    private T defaultValue;
    private final String name, prefKey;

    public PrefView(PrefType<T> type, String name, String prefKey) {
        this(type, -1, name, prefKey);
    }

    public PrefView(PrefType<T> type, int length, String name, String prefKey) {
        super();
        this.type = type;
        this.length = length;
        this.name = name;
        this.prefKey = prefKey;
        this.vw = this.createVW();
        this.setDefaultValue(this.type.getDefaultValue());
    }

    public final PrefType<T> getType() {
        return this.type;
    }

    public final Class<T> getViewClass() {
        return this.getType().getTypeClass();
    }

    public final int getLength() {
        return this.length;
    }

    public final String getName() {
        return this.name;
    }

    public final String getPrefKey() {
        return this.prefKey;
    }

    protected JComponent createComponent() {
        final JComponent comp;
        if (Boolean.class.isAssignableFrom(this.getViewClass())) {
            // ATTN hack to view the focus (should try to paint around the button)
            comp = new JCheckBox(" ");
        } else if (Date.class.isAssignableFrom(this.getViewClass())) {
            comp = new JDate();
        } else if (String.class.isAssignableFrom(this.getViewClass()) && this.getLength() >= 512) {
            comp = new ITextArea();
        } else {
            comp = new JTextField();
        }
        return comp;
    }

    protected ValueWrapper<T> createVW() {
        final JComponent comp = createComponent();
        if (this.getLength() > 0) {
            final Document doc = TextComponentUtils.getDocument(comp);
            if (doc instanceof AbstractDocument)
                DocumentFilterList.add((AbstractDocument) doc, new LimitedSizeDocumentFilter(this.getLength()), FilterType.SIMPLE_FILTER);
        }
        return ValueWrapperFactory.create(comp, this.getViewClass());
    }

    public final ValueWrapper<T> getVW() {
        return this.vw;
    }

    public void init(final JavaPrefPreferencePanel prefPanel) {
    }

    /**
     * Set the default value for this preference. Initially this is set to
     * {@link PrefType#getDefaultValue()}.
     * 
     * @param defaultValue the new default value.
     * @return this.
     */
    public final PrefView<T> setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public final T getDefaultValue() {
        return this.defaultValue;
    }

    final void resetViewValue() {
        this.getVW().setValue(this.getDefaultValue());
    }

    final void setViewValue(final Preferences prefs) {
        this.getVW().setValue(getPrefValue(prefs));
    }

    private final T getPrefValue(final Preferences prefs) {
        return this.getType().get(prefs, getPrefKey(), this.getDefaultValue());
    }

    final boolean equalsToPrefValue(final Preferences prefs) {
        return CompareUtils.equals(getPrefValue(prefs), this.getVW().getValue());
    }

    final void setPrefValue(final Preferences prefs) {
        final T val = this.getVW().getValue();
        // e.g. empty text field
        if (val == null)
            prefs.remove(getPrefKey());
        else
            this.getType().put(prefs, getPrefKey(), val);
    }

}
