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
 
 /*
 * Created on 5 nov. 2004
 */
package org.openconcerto.sql.element;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLType;
import org.openconcerto.sql.request.MutableRowItemView;
import org.openconcerto.sql.request.RowNotFound;
import org.openconcerto.sql.request.SQLForeignRowItemView;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.request.SQLRowView;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.SQLSearchableTextCombo;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.sql.sqlobject.itemview.SimpleRowItemView;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.ui.DisplayabilityListener;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.component.ComboLockedMode;
import org.openconcerto.ui.component.text.TextBehaviour;
import org.openconcerto.ui.component.text.TextComponentUtils;
import org.openconcerto.ui.coreanimation.Animator;
import org.openconcerto.ui.valuewrapper.ValueWrapperFactory;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.EmptyObj;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidObject;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;

/**
 * Base class for SQLComponent. This implements the ValidObject by defining that a component is
 * valid when all its objects are valid and all its required objects are not empty : <br/>
 * <img src="doc-files/listeners.png"/>. Also this class uses SQLRowItemView to represent items of a
 * row.
 * 
 * @author ilm
 */
public abstract class BaseSQLComponent extends SQLComponent implements Scrollable {
    protected static final String REQ = "required";
    protected static final String DEC = "notdecorated";
    protected static final String SEP = "noseparator";

    private final SQLRowView requete;

    private final Set<SQLRowItemView> required;
    // contains the SQL name of required SQLRowItemView
    private final Set<String> requiredNames;

    // [ValidListener]
    private final List<ValidListener> listeners;

    private boolean editable;
    private boolean alwaysEditable;
    private final Set<SQLField> hide;
    private String invalidityCause;

    public BaseSQLComponent(SQLElement element) {
        super(element);
        // Obligatoire pour L&F Nymbus
        this.setOpaque(true);

        // pouvoir prendre le focus
        // ATTN marche pas toujours : qd disabled, clic sur ITextArea ne remonte pas jusqu'à nous.
        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                BaseSQLComponent.this.requestFocusInWindow();
            }
        });
        this.required = new HashSet<SQLRowItemView>();
        this.requiredNames = this.createRequiredNames();
        this.listeners = new ArrayList<ValidListener>();
        this.hide = new HashSet<SQLField>();
        this.editable = true;
        this.setNonExistantEditable(false);
        this.requete = new SQLRowView(this.getTable());
        this.invalidityCause = "";
    }

    private final SQLRowView getRequest() {
        return this.requete;
    }

    private SQLField getField(String field) {
        return this.getTable().getField(field);
    }

    // *** add*

    public Component addView(String field) {
        return this.addView(field, null);
    }

    public Component addView(String field, String spec) {
        return this.addViewJComponent(field, spec);
    }

    private Component addViewJComponent(String field, String spec) {
        if (getElement().getPrivateElement(field) != null) {
            // private
            final SQLComponent comp = this.getElement().getPrivateElement(field).createComponent();

            // TODO add a callback so that RowItemView is notified when added to a SQLComponent
            // avoid parsing twice spec, and the 'if instanceof ElementSQLObject'
            final SpecParser parser = new SpecParser(spec);
            DefaultElementSQLObject dobj = new DefaultElementSQLObject(this, comp);
            dobj.setDecorated(parser.isDecorated());
            dobj.showSeparator(parser.showSeparator());
            return this.addView((MutableRowItemView) dobj, field, spec);
        } else if (getField(field).isKey()) {
            // foreign
            return this.addView(new ElementComboBox(), field, spec);
        } else {
            final JComponent comp;
            final SQLType type = getField(field).getType();
            if (Boolean.class.isAssignableFrom(type.getJavaType())) {
                // TODO hack to view the focus (should try to paint around the button)
                comp = new JCheckBox(" ");
            } else if (Date.class.isAssignableFrom(type.getJavaType())) {
                comp = new JDate();
            } else if (String.class.isAssignableFrom(type.getJavaType()) && type.getSize() >= 512)
                comp = new SQLSearchableTextCombo(ComboLockedMode.UNLOCKED, true);
            else
                // regular
                comp = new SQLTextCombo();
            return this.addView(comp, field, spec);
        }
    }

    public final void addSQLObject(JComponent obj, String field) {
        this.addSQLObject(obj, field, null);
    }

    public Component addSQLObject(JComponent obj, String field, Object spec) {
        return this.addView(obj, field, spec);
    }

    public void addRequiredSQLObject(JComponent obj, String field, Object spec) {
        this.addSQLObject(obj, field, REQ + ";" + spec);
    }

    public void addRequiredSQLObject(JComponent obj, String field) {
        this.addSQLObject(obj, field, REQ);
    }

    public Component addView(JComponent comp, String fields) {
        return this.addView(comp, fields, null);
    }

    public Component addView(JComponent comp, String fields, Object specObj) {
        final MutableRowItemView rowItemView;
        if (comp instanceof MutableRowItemView) {
            rowItemView = (MutableRowItemView) comp;
        } else {
            final SQLField field = this.getField(SQLRow.toList(fields).get(0));
            rowItemView = createRowItemView(comp, field);
        }
        return this.addView(rowItemView, fields, specObj);
    }

    static public SimpleRowItemView<?> createRowItemView(JComponent comp, final SQLField field) {
        if (comp == null)
            throw new NullPointerException("comp for " + field + " is null");
        return createRowItemView(comp, field.getType().getJavaType());
    }

    // just to make javac happy (type parameter for SimpleRowItemView)
    static private <T> SimpleRowItemView<T> createRowItemView(JComponent comp, final Class<T> javaType) {
        return new SimpleRowItemView<T>(ValueWrapperFactory.create(comp, javaType));
    }

    public Component addView(MutableRowItemView rowItemView, String fields, Object specObj) {
        final List<String> fieldListS = SQLRow.toList(fields);
        final Set<SQLField> fieldList = new HashSet<SQLField>(fieldListS.size());
        for (final String fieldName : fieldListS) {
            fieldList.add(this.getField(fieldName));
        }

        // sqlName
        final String sqlName = fields;
        rowItemView.init(sqlName, fieldList);
        rowItemView.setDescription(this.getLabelFor(sqlName));

        return this.addInitedView(rowItemView, specObj);
    }

    public Component addInitedView(SQLRowItemView v, Object specObj) {
        // if (obj == null)
        // throw new IllegalArgumentException("obj is null");

        final Spec spec;
        if (specObj == null || specObj instanceof String) {
            spec = new SpecParser((String) specObj);
        } else
            spec = (SpecParser) specObj;

        // ParentForeignField is always required
        final String fieldName = v.getField().getName();
        if (spec.isRequired() || fieldName.equals(getElement().getParentForeignField()) || this.getRequiredNames() == null || this.getRequiredNames().contains(v.getSQLName())) {
            this.required.add(v);
            if (v instanceof ElementSQLObject)
                ((ElementSQLObject) v).setRequired(true);
        }
        this.getRequest().add(v);

        if (!this.hide.contains(v.getField())) {
            this.addToUI(v, spec.getWhere());
        }
        if (dontEdit(v))
            v.setEditable(false);

        final JTextComponent textComp = TextComponentUtils.getTextComp(v.getComp());
        if (textComp != null)
            TextBehaviour.manage(textComp);

        return v.getComp();
    }

    private boolean dontEdit(SQLRowItemView v) {
        final String fieldName = v.getField().getName();
        return this.getElement().getReadOnlyFields().contains(fieldName) || (this.getMode() != Mode.INSERTION && this.getElement().getInsertOnlyFields().contains(fieldName));
    }

    protected final void inited() {
        // assure that added views are consistent with our editable status
        this.setChildrenEditable(this.isEditable());
        for (final SQLRowItemView v : this.getRequest().getViews()) {
            v.addEmptyListener(new EmptyListener() {
                public void emptyChange(EmptyObj src, boolean newValue) {
                    emptyOrValidChanged((SQLRowItemView) src);
                }
            });
            v.addValidListener(new ValidListener() {
                public void validChange(ValidObject src, boolean newValue) {
                    emptyOrValidChanged((SQLRowItemView) src);
                }
            });
            // initial status
            updateAnimate(v);
        }
        this.addHierarchyListener(new DisplayabilityListener() {
            @Override
            protected void displayabilityChanged(Component c) {
                getRequest().activate(c.isDisplayable());
            }
        });
        getRequest().activate(this.isDisplayable());
        this.fireValidChange();
        this.initDone();
    }

    protected void initDone() {

    }

    private void updateAnimate(final SQLRowItemView v) {
        if (v.getComp() != null)
            Animator.getInstance().animate(v.getComp(), !isItemViewValid(v));
    }

    protected void emptyOrValidChanged(SQLRowItemView v) {
        this.fireValidChange();
        updateAnimate(v);
    }

    protected void addToUI(SQLRowItemView v, String where) {
        // implement it to do nothing since subclass may choose not to use it
    }

    public final SQLRowItemView getView(String name) {
        return this.getRequest().getView(name);
    }

    protected final SQLRowItemView getView(Component comp) {
        return this.getRequest().getView(comp);
    }

    protected final SQLForeignRowItemView getForeignView(SQLRowItemView v) {
        if (v instanceof SQLForeignRowItemView)
            return (SQLForeignRowItemView) v;
        else if (v.getComp() instanceof SQLForeignRowItemView)
            return (SQLForeignRowItemView) v.getComp();
        else
            throw new IllegalArgumentException("no SQLForeignRowItemView found for " + v);
    }

    public void addValidListener(ValidListener l) {
        this.listeners.add(l);
    }

    protected synchronized final void fireValidChange() {
        // ATTN called very often during a select() (for each SQLObject empty & value change)
        final boolean validated = this.isValidated();
        for (final ValidListener l : this.listeners) {
            l.validChange(this, validated);
        }
    }

    private boolean isItemViewValid(final SQLRowItemView v) {
        return v.isValidated() && !(this.getRequired().contains(v) && v.isEmpty());
    }

    public synchronized boolean isValidated() {
        boolean res = true;
        final List<String> pbs = new ArrayList<String>();
        // tous nos objets sont valides ?
        for (final SQLRowItemView obj : this.getRequest().getViews()) {
            if (!obj.isValidated()) {
                String explanation = "'" + getDesc(obj) + "' n'est pas valide";
                final String txt = obj.getValidationText();
                if (txt != null)
                    explanation += " (" + txt + ")";
                pbs.add(explanation);
                res = false;
                // ne regarder si vide que pour les valides (souvent les non-valides sont vides car
                // il ne peuvent renvoyer de valeur)
            } else if (this.getRequired().contains(obj) && obj.isEmpty()) {
                pbs.add("'" + getDesc(obj) + "' est vide");
                res = false;
            }
        }
        this.invalidityCause = CollectionUtils.join(pbs, "\n");
        return res;
    }

    protected static final String getDesc(final SQLRowItemView obj) {
        final String desc = obj.getDescription();
        return desc == null ? obj.getSQLName() : desc;
    }

    public String getValidationText() {
        return this.invalidityCause;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.sql.SQLComponent#setEditable(boolean)
     */
    public void setEditable(boolean b) {
        if (b != this.editable) {
            this.editable = b;
            this.setChildrenEditable(b);
        }
    }

    private final void setChildrenEditable(boolean b) {
        for (final SQLRowItemView o : this.getRequest().getViews()) {
            // already taken care in addInitedView()
            if (!this.dontEdit(o))
                // a view can only be editable if its parent is too
                o.setEditable(this.isEditable() && b);
        }
    }

    private final boolean isEditable() {
        return this.editable;
    }

    // final : overload select() if need be
    public final void resetValue() {
        this.select(null);
    }

    public final int insert() {
        return this.insert(null);
    }

    public int insert(SQLRow order) {
        try {
            if (!UserRightsManager.getCurrentUserRights().canAdd(getTable()))
                throw new SQLException("forbidden");
            if (order == null)
                return this.getRequest().insert().getID();
            else
                return this.getRequest().insert(order).getID();
        } catch (SQLException e) {
            ExceptionHandler.handle(this, "Impossible d'insérer", e);
            return -1;
        }
    }

    public final void select(int id) {
        this.select(this.getTable().getRow(id));
    }

    public void select(SQLRowAccessor r) {
        try {
            // allow null to pass, ease some code (eg new ListOfSomething().getTable() even if we
            // can't see the table)
            if (r != null && !UserRightsManager.getCurrentUserRights().canView(getTable()))
                throw new IllegalStateException("forbidden");
            // MAYBE prevent repaints of each SQLObject with
            // this.setIgnoreRepaint(true) or RepaintManager
            this.getRequest().select(r);
            // if nonExistantEditable : always editable,
            // otherwise id must exist
            this.setChildrenEditable(this.isNonExistantEditable() || (r != null && r.getID() != SQLRow.NONEXISTANT_ID));

            // don't put defaults if non-editable (eg bottom part of ListeModifyPanel)
            if (r == null && this.isNonExistantEditable()) {
                // selectDefaults() after select() so that we have the final word
                // eg default DESIGNATION of OBS is "ok", but default DESIGNATION of TRANSFO.ID_OBS
                // is "good".
                this.selectDefaults();
            }
        } catch (RowNotFound e) {
            // l'id demandé n'existe pas : prévenir tout le monde
            this.getTable().fireRowDeleted(e.getRow().getID());
            ExceptionHandler.handle(this, "La ligne n'est plus dans la base : " + e.getRow(), e);
        } catch (IllegalStateException e) {
            ExceptionHandler.handle(this, "Impossible de sélectionner " + r, e);
        }
    }

    // private since it only changes views having default values : use #resetValue()
    private final void selectDefaults() {
        final SQLRowValues defaults = this.createDefaults();
        if (defaults != null && defaults.getFields().size() > 0)
            this.getRequest().select(defaults);
    }

    public final void addFillingListener(final PropertyChangeListener l) {
        this.getRequest().addListener(l, "filling");
    }

    public final void rmFillingListener(final PropertyChangeListener l) {
        this.getRequest().rmListener(l, "filling");
    }

    public final void addSelectionListener(final PropertyChangeListener l) {
        this.getRequest().addListener(l, "selectedID");
    }

    public final void rmSelectionListener(final PropertyChangeListener l) {
        this.getRequest().rmListener(l, "selectedID");
    }

    @Override
    public void detach() {
        this.getRequest().detach();
    }

    /**
     * Are we filling our objects.
     * 
     * @return <code>true</code> if we're filling in values from the DB.
     */
    public final boolean isFilling() {
        return this.getRequest().isFilling();
    }

    public int getSelectedID() {
        return this.getRequest().getSelectedID();
    }

    public void update() {
        try {
            if (!UserRightsManager.getCurrentUserRights().canModify(getTable()))
                throw new SQLException("forbidden");
            this.getRequest().update();
        } catch (SQLException e) {
            ExceptionHandler.handle(this, "Impossible de mettre à jour", e);
        }
    }

    public void archive() {
        try {
            if (!UserRightsManager.getCurrentUserRights().canDelete(getTable()))
                throw new SQLException("forbidden");
            // MAYBE for performance (avoid searching for references to cut):
            // if (this.isPrivate()) {
            // ((BaseSQLComponent) this.getSQLParent().getSQLParent()).getSelectedID();
            // this.getElement().archivePrivate(this.getSelectedID(), parentID);
            // } else
            this.getElement().archive(this.getSelectedID());
        } catch (SQLException e) {
            ExceptionHandler.handle(this, "Impossible d'archiver " + this + ": ", e);
        }
    }

    // ** required

    protected final Set<SQLRowItemView> getRequired() {
        return this.required;
    }

    protected final Set<String> getRequiredNames() {
        return this.requiredNames;
    }

    /**
     * The sql names that are required. This implementation includes "DESIGNATION".
     * 
     * @return the required sql names, <code>null</code> meaning all of them.
     */
    protected Set<String> createRequiredNames() {
        return new HashSet<String>(Collections.singleton("DESIGNATION"));
    }

    public String toString() {
        return this.getClass() + " on " + this.getTable() + " " + this.getSelectedID();
    }

    public final boolean isNonExistantEditable() {
        return this.alwaysEditable;
    }

    public final void setNonExistantEditable(boolean alwaysEditable) {
        this.alwaysEditable = alwaysEditable;
    }

    public final String getLabelFor(String field) {
        return Configuration.getTranslator(this.getTable()).getDescFor(this.getTable(), field).getLabel();
    }

    public void doNotShow(SQLField f) {
        this.hide.add(f);
    }

    private static interface Spec {

        boolean isRequired();

        String getWhere();
    }

    private static final class SpecParser implements Spec {

        private boolean isRequired;
        private String where;
        private boolean showSeparator = true;
        private boolean isDecorated = true;

        public SpecParser(String spec) {
            // empty string treated as null
            if (spec == null || spec.length() == 0) {
                this.isRequired = false;
                this.where = null;
            } else {
                final String[] specs = spec.split(";");
                if (specs.length > 4)
                    throw new IllegalArgumentException(spec);
                for (int i = 0; i < specs.length; i++) {
                    final String sp = specs[i];
                    if (sp.equals(REQ)) {
                        this.isRequired = true;
                    } else if (sp.equals(DEC)) {
                        this.isDecorated = false;
                    } else if (sp.equals(SEP)) {
                        this.showSeparator = false;
                    } else {
                        this.where = sp;
                    }
                }
            }
        }

        public boolean showSeparator() {
            return this.showSeparator;
        }

        public boolean isDecorated() {
            return this.isDecorated;
        }

        @Override
        public final boolean isRequired() {
            return this.isRequired;
        }

        @Override
        public final String getWhere() {
            return this.where;
        }
    }

    // *** scrollable

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return this.getPreferredSize();
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        if (getParent() instanceof JViewport) {
            // since getPreferredScrollableViewportSize() returns getPreferredSize() this component
            // will be shown by default at its preferred size, but if the viewport's height is
            // reduced this component will first shrink to its minimum size, and then below that
            // scrollbars will be displayed.
            return (((JViewport) getParent()).getHeight() >= getMinimumSize().height);
        }
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        // no horizontal scroll, too much pb with JTextArea when setLineWrap(true) : its minimum
        // width grows but never shrinks
        return true;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        // difficult to tell because of the textAreas and the private components
        return 15;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        final double length = orientation == SwingConstants.VERTICAL ? visibleRect.getHeight() : visibleRect.getWidth();
        // keep some common area
        return (int) (length - 30);
    }

}
