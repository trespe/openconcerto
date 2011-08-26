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

import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.AutoLayouter;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.checks.ValidChangeSupport;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidObject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JPanel;

/**
 * A {@link PreferencePanel} using java {@link Preferences}.
 * 
 * @author Sylvain CUAZ
 */
public abstract class JavaPrefPreferencePanel extends JPanel implements PreferencePanel {

    private final String title;
    private Preferences prefs;
    private final AutoLayouter layouter;
    private final Set<PrefView<?>> views;
    private boolean modified;
    private final ValidChangeSupport validSupp;
    private String invalidityCause;

    public JavaPrefPreferencePanel(final String title, final Preferences prefs) {
        this.title = title;
        this.prefs = prefs;
        this.layouter = new AutoLayouter(this);
        this.views = new HashSet<PrefView<?>>();
        this.modified = false;
        this.validSupp = new ValidChangeSupport(this);
        this.invalidityCause = null;
    }

    public final void setPrefs(Preferences prefs) {
        if (this.prefs != null)
            throw new IllegalStateException("Already set : " + this.prefs);
        this.prefs = prefs;
    }

    @Override
    public final String getTitleName() {
        return this.title;
    }

    public final String getPrefPath() {
        return this.prefs.absolutePath();
    }

    @Override
    public void uiInit() {
        if (this.prefs == null)
            throw new NullPointerException("prefs wasn't set");
        this.addViews();
        this.reset();
    }

    protected abstract void addViews();

    protected final void addView(final PrefView<?> view) {
        final ValueWrapper<?> vw = view.getVW();
        this.layouter.add(view.getName(), vw.getComp());
        this.views.add(view);
        view.init(this);

        vw.addValueListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                // needed since some components are not synchronous (e.g. ITextCombo)
                if (!view.equalsToPrefValue(JavaPrefPreferencePanel.this.prefs))
                    setModified(true);
            }
        });
        vw.addValidListener(new ValidListener() {
            @Override
            public void validChange(ValidObject src, boolean newValue) {
                JavaPrefPreferencePanel.this.validSupp.fireValidChange(isValidated());
            }
        });
    }

    /**
     * Reset UI to the stored values.
     */
    public final void reset() {
        for (final PrefView<?> v : this.views) {
            v.setViewValue(this.prefs);
        }
        this.setModified(false);
    }

    @Override
    public final void apply() {
        if (this.isModified()) {
            try {
                // only set preferences in apply() and not when UI changes since some implementation
                // flush preferences automatically (e.g. the default implementation)
                for (final PrefView<?> v : this.views) {
                    v.setPrefValue(this.prefs);
                }
                this.prefs.sync();
                // preferences were reloaded and another VM might have also changed them
                this.reset();
            } catch (BackingStoreException e) {
                throw new IllegalStateException("Couldn't store values", e);
            }
        }
    }

    @Override
    public final void restoreToDefaults() {
        for (final PrefView<?> v : this.views) {
            v.resetViewValue();
        }
    }

    @Override
    public final void addModifyChangeListener(PreferencePanelListener l) {
        // TODO
        throw new UnsupportedOperationException();
    }

    private final void setModified(boolean modified) {
        if (this.modified != modified) {
            this.modified = modified;
        }
    }

    @Override
    public final boolean isModified() {
        return this.modified;
    }

    // ValidObject

    @Override
    public final boolean isValidated() {
        boolean res = true;
        final List<String> pbs = new ArrayList<String>();
        for (final PrefView<?> v : this.views) {
            if (!v.getVW().isValidated()) {
                String explanation = "'" + v.getName() + "' n'est pas valide";
                final String txt = v.getVW().getValidationText();
                if (txt != null)
                    explanation += " (" + txt + ")";
                pbs.add(explanation);
                res = false;
            }
        }
        this.invalidityCause = res ? null : CollectionUtils.join(pbs, "\n");
        return res;
    }

    @Override
    public final String getValidationText() {
        return this.invalidityCause;
    }

    @Override
    public final void addValidListener(ValidListener l) {
        this.validSupp.addValidListener(l);
    }

    @Override
    public final void removeValidListener(ValidListener l) {
        this.validSupp.removeValidListener(l);
    }
}
