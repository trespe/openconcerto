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
 
 package org.openconcerto.sql.element;

import static org.openconcerto.sql.TM.getTM;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.Log;
import org.openconcerto.sql.TM;
import org.openconcerto.sql.model.DBStructureItemNotFound;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLFieldsSet;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowMode;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelect.ArchiveMode;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.DatabaseGraph;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.request.SQLCache;
import org.openconcerto.sql.request.SQLFieldTranslator;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;
import org.openconcerto.sql.view.list.IListeAction;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelColumnPath;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cache.CacheResult;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.change.ListChangeIndex;
import org.openconcerto.utils.change.ListChangeRecorder;
import org.openconcerto.utils.i18n.Grammar;
import org.openconcerto.utils.i18n.Grammar_fr;
import org.openconcerto.utils.i18n.NounClass;
import org.openconcerto.utils.i18n.Phrase;

import java.awt.Component;
import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;

import net.jcip.annotations.GuardedBy;

import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.iterators.EntrySetMapIterator;

/**
 * Décrit comment manipuler un élément de la BD (pas forcément une seule table, voir
 * privateForeignField).
 * 
 * @author ilm
 */
public abstract class SQLElement {

    static final private Set<String> computingFF = Collections.unmodifiableSet(new HashSet<String>());
    static final private Set<SQLField> computingRF = Collections.unmodifiableSet(new HashSet<SQLField>());

    private static Phrase createPhrase(String singular, String plural) {
        final NounClass nounClass;
        final String base;
        if (singular.startsWith("une ")) {
            nounClass = NounClass.FEMININE;
            base = singular.substring(4);
        } else if (singular.startsWith("un ")) {
            nounClass = NounClass.MASCULINE;
            base = singular.substring(3);
        } else {
            nounClass = null;
            base = singular;
        }
        final Phrase res = new Phrase(Grammar_fr.getInstance(), base, nounClass);
        if (nounClass != null)
            res.putVariant(Grammar.INDEFINITE_ARTICLE_SINGULAR, singular);
        res.putVariant(Grammar.PLURAL, plural);
        return res;
    }

    // from the most loss of information to the least.
    public static enum ReferenceAction {
        /** If a referenced row is archived, empty the foreign field */
        SET_EMPTY,
        /** If a referenced row is archived, archive this row too */
        CASCADE,
        /** If a referenced row is to be archived, abort the operation */
        RESTRICT
    }

    static final public String DEFAULT_COMP_ID = "default component code";
    /**
     * If this value is passed to the constructor, {@link #createCode()} will only be called the
     * first time {@link #getCode()} is. This allow the method to use objects passed to the
     * constructor of a subclass.
     */
    static final public String DEFERRED_CODE = new String("deferred code");

    @GuardedBy("this")
    private SQLElementDirectory directory;
    private String l18nPkgName;
    private Class<?> l18nClass;
    private Phrase name;
    private final SQLTable primaryTable;
    // used as a key in SQLElementDirectory so it should be immutable
    private String code;
    private ComboSQLRequest combo;
    private ListSQLRequest list;
    private SQLTableModelSourceOnline tableSrc;
    private final ListChangeRecorder<IListeAction> rowActions;
    private final CollectionMap<String, ITransformer<Tuple2<SQLElement, String>, SQLComponent>> components;
    // foreign fields
    private Set<String> normalFF;
    private String parentFF;
    private Set<String> sharedFF;
    private Map<String, SQLElement> privateFF;
    private final Map<String, ReferenceAction> actions;
    // referent fields
    private Set<SQLField> childRF;
    private Set<SQLField> privateParentRF;
    private Set<SQLField> otherRF;
    // lazy creation
    private SQLCache<SQLRowAccessor, Object> modelCache;

    private final Map<String, JComponent> additionalFields;
    private final List<SQLTableModelColumn> additionalListCols;
    @GuardedBy("this")
    private List<String> mdPath;

    @Deprecated
    public SQLElement(String singular, String plural, SQLTable primaryTable) {
        this(primaryTable, createPhrase(singular, plural));
    }

    public SQLElement(SQLTable primaryTable) {
        this(primaryTable, null);
    }

    public SQLElement(final SQLTable primaryTable, final Phrase name) {
        this(primaryTable, name, null);
    }

    public SQLElement(final SQLTable primaryTable, final Phrase name, final String code) {
        super();
        if (primaryTable == null) {
            throw new DBStructureItemNotFound("table is null for " + this.getClass());
        }
        this.primaryTable = primaryTable;
        this.setL18nPackageName(null);
        this.setDefaultName(name);
        this.code = code == null ? createCode() : code;
        this.combo = null;
        this.list = null;
        this.rowActions = new ListChangeRecorder<IListeAction>(new ArrayList<IListeAction>());
        this.actions = new HashMap<String, ReferenceAction>();
        this.resetRelationships();

        this.components = new CollectionMap<String, ITransformer<Tuple2<SQLElement, String>, SQLComponent>>(new LinkedList<ITransformer<Tuple2<SQLElement, String>, SQLComponent>>());

        this.modelCache = null;

        // the components should always be in the same order
        this.additionalFields = new LinkedHashMap<String, JComponent>();
        this.additionalListCols = new ArrayList<SQLTableModelColumn>();
        this.mdPath = Collections.emptyList();
    }

    /**
     * Should return the code for this element. This method is only called if the <code>code</code>
     * parameter of the constructor is <code>null</code>.
     * 
     * @return the default code for this element.
     */
    protected String createCode() {
        return getClass().getName() + "-" + getTable().getName();
    }

    /**
     * Must be called if foreign/referent keys are added or removed.
     */
    public synchronized void resetRelationships() {
        this.privateFF = null;
        this.parentFF = null;
        this.normalFF = null;
        this.sharedFF = null;
        this.actions.clear();

        this.childRF = null;
        this.privateParentRF = null;
        this.otherRF = null;
    }

    protected synchronized final boolean areRelationshipsInited() {
        return this.sharedFF != null;
    }

    private void checkSelfCall(boolean check, final String methodName) {
        assert check : this + " " + methodName + "() is calling itself, and thus the caller will only see a partial state";
    }

    private synchronized void initFF() {
        checkSelfCall(this.sharedFF != computingFF, "initFF");
        if (areRelationshipsInited())
            return;
        this.sharedFF = computingFF;

        final Set<String> privates = new HashSet<String>(this.getPrivateFields());
        this.privateFF = new HashMap<String, SQLElement>(privates.size());
        final Set<String> parents = new HashSet<String>();
        this.normalFF = new HashSet<String>();
        final Set<String> tmpSharedFF = new HashSet<String>();
        for (final SQLField ff : this.getTable().getForeignKeys()) {
            final String fieldName = ff.getName();
            final SQLElement foreignElement = this.getForeignElement(fieldName);
            if (privates.contains(fieldName)) {
                privates.remove(fieldName);
                this.privateFF.put(fieldName, foreignElement);
            } else if (foreignElement.isShared()) {
                tmpSharedFF.add(fieldName);
            } else if (foreignElement.getChildrenReferentFields().contains(ff)) {
                parents.add(fieldName);
            } else {
                this.normalFF.add(fieldName);
            }
        }
        if (parents.size() > 1)
            throw new IllegalStateException("for " + this + " more than one parent :" + parents);
        this.parentFF = parents.size() == 0 ? null : (String) parents.iterator().next();
        if (privates.size() > 0)
            throw new IllegalStateException("for " + this + " these private foreign fields are not valid :" + privates);
        this.sharedFF = tmpSharedFF;

        // MAYBE move required fields to SQLElement and use RESTRICT
        this.actions.put(this.parentFF, ReferenceAction.CASCADE);
        for (final String s : this.privateFF.keySet()) {
            this.actions.put(s, ReferenceAction.SET_EMPTY);
        }
        for (final String s : this.normalFF) {
            this.actions.put(s, ReferenceAction.SET_EMPTY);
        }
        for (final String s : this.sharedFF) {
            this.actions.put(s, ReferenceAction.RESTRICT);
        }
        this.ffInited();
    }

    protected void ffInited() {
        // MAYBE use DELETE_RULE of Link
    }

    // convert the list of String of getChildren() to a Set of SQLField pointing to this table
    private synchronized Set<SQLField> computeChildrenRF() {
        final Set<SQLField> res = new HashSet<SQLField>();
        // eg "BATIMENT" or "BATIMENT.ID_SITE"
        for (final String child : this.getChildren()) {
            // a field from our child to us, eg |BATIMENT.ID_SITE|
            final SQLField childField;

            final int comma = child.indexOf(',');
            final String tableName = comma < 0 ? child : child.substring(0, comma);
            final SQLTable childTable = this.getTable().getTable(tableName);

            if (comma < 0) {
                final Set<SQLField> keys = childTable.getForeignKeys(this.getTable());
                if (keys.size() != 1)
                    throw new IllegalArgumentException("cannot find a foreign from " + child + " to " + this.getTable());
                childField = keys.iterator().next();
            } else {
                childField = childTable.getField(child.substring(comma + 1));
                final SQLTable foreignTable = childField.getDBSystemRoot().getGraph().getForeignTable(childField);
                if (!foreignTable.equals(this.getTable())) {
                    throw new IllegalArgumentException(childField + " doesn't point to " + this.getTable());
                }
            }
            res.add(childField);
        }
        return res;
    }

    private synchronized void initRF() {
        checkSelfCall(this.otherRF != computingRF, "initRF");
        if (this.otherRF != null)
            return;
        this.otherRF = computingRF;

        this.privateParentRF = new HashSet<SQLField>();
        final Set<SQLField> tmpOtherRF = new HashSet<SQLField>();
        for (final SQLField refField : this.getTable().getBase().getGraph().getReferentKeys(this.getTable())) {
            // don't force every table to have an SQLElement (eg ELEMENT_MISSION)
            final SQLElement refElem = this.getElementLenient(refField.getTable());
            if (refElem != null && refElem.getPrivateForeignFields().contains(refField.getName())) {
                this.privateParentRF.add(refField);
            } else if (!this.getChildrenReferentFields().contains(refField)) {
                tmpOtherRF.add(refField);
            }
        }
        this.otherRF = tmpOtherRF;
    }

    // childRF is done outside initRF() to avoid :
    // MISSION.initRF() -> ELEMENT_MISSION.getPrivateForeignFields() ->
    // ELEMENT_MISSION.initFF() -> MISSION.getChildrenReferentFields() -> MISSION.initRF()
    private synchronized void initChildRF() {
        checkSelfCall(this.childRF != computingRF, "initFF");
        if (this.childRF != null)
            return;
        this.childRF = computingRF;

        final Set<SQLField> children = this.computeChildrenRF();

        final Set<SQLField> tmpChildRF = new HashSet<SQLField>();
        for (final SQLField refField : this.getTable().getBase().getGraph().getReferentKeys(this.getTable())) {
            // don't force every table to have an SQLElement (eg ELEMENT_MISSION)
            final SQLElement refElem = this.getElementLenient(refField.getTable());
            // if no element found, treat as elements with no parent
            final SQLField refParentFF = refElem == null ? null : refElem.getParentFF();
            // check coherence, either overload getParentFFName() or use getChildren(), but not both
            if (refParentFF != null && children.contains(refField))
                throw new IllegalStateException(refElem + " specifies this as its parent: " + refParentFF + " and is also mentioned as our (" + this + ") child: " + refField);
            if (children.contains(refField) || refParentFF == refField) {
                tmpChildRF.add(refField);
            }
        }
        // pas besoin de faire comme dans initFF pour vérifier children :
        // computeChildrenRF le fait déjà
        this.childRF = tmpChildRF;
    }

    final void setDirectory(final SQLElementDirectory directory) {
        // since this method should only be called at the end of SQLElementDirectory.addSQLElement()
        assert directory == null || directory.getElement(this.getTable()) == this;
        synchronized (this) {
            if (this.directory != directory) {
                if (this.areRelationshipsInited())
                    this.resetRelationships();
                this.directory = directory;
            }
        }
    }

    public synchronized final SQLElementDirectory getDirectory() {
        return this.directory;
    }

    final SQLElement getElement(SQLTable table) {
        final SQLElement res = getElementLenient(table);
        if (res == null)
            throw new IllegalStateException("no element for " + table.getSQLName());
        return res;
    }

    final SQLElement getElementLenient(SQLTable table) {
        synchronized (this) {
            return this.getDirectory().getElement(table);
        }
    }

    public final SQLElement getForeignElement(String foreignField) {
        try {
            return this.getElement(this.getForeignTable(foreignField));
        } catch (RuntimeException e) {
            throw new IllegalStateException("no element for " + foreignField + " in " + this, e);
        }
    }

    private final SQLTable getForeignTable(String foreignField) {
        return this.getTable().getBase().getGraph().getForeignTable(this.getTable().getField(foreignField));
    }

    public final synchronized String getL18nPackageName() {
        return this.l18nPkgName;
    }

    public final synchronized Class<?> getL18nClass() {
        return this.l18nClass;
    }

    public final void setL18nLocation(Class<?> clazz) {
        this.setL18nLocation(clazz.getPackage().getName(), clazz);
    }

    public final void setL18nPackageName(String name) {
        this.setL18nLocation(name, null);
    }

    /**
     * Set the location for the localized name.
     * 
     * @param name a package name, can be <code>null</code> :
     *        {@link SQLElementDirectory#getL18nPackageName()} will be used.
     * @param ctxt the class loader to load the resource, <code>null</code> meaning this class.
     * @see SQLElementDirectory#getName(SQLElement)
     */
    public final synchronized void setL18nLocation(final String name, final Class<?> ctxt) {
        this.l18nPkgName = name;
        this.l18nClass = ctxt == null ? this.getClass() : ctxt;
    }

    /**
     * Set the default name, used if no translations could be found.
     * 
     * @param name the default name, if <code>null</code> the {@link #getTable() table} name will be
     *        used.
     */
    public final synchronized void setDefaultName(Phrase name) {
        this.name = name != null ? name : Phrase.getInvariant(getTable().getName());
    }

    /**
     * The default name.
     * 
     * @return the default name, never <code>null</code>.
     */
    public final synchronized Phrase getDefaultName() {
        return this.name;
    }

    /**
     * The name of this element in the current locale.
     * 
     * @return the name of this, {@link #getDefaultName()} if there's no {@link #getDirectory()
     *         directory} or if it hasn't a name for this.
     * @see SQLElementDirectory#getName(SQLElement)
     */
    public final Phrase getName() {
        final SQLElementDirectory dir = this.getDirectory();
        final Phrase res = dir == null ? null : dir.getName(this);
        return res == null ? this.getDefaultName() : res;
    }

    public String getPluralName() {
        return this.getName().getVariant(Grammar.PLURAL);
    }

    public String getSingularName() {
        return this.getName().getVariant(Grammar.INDEFINITE_ARTICLE_SINGULAR);
    }

    public CollectionMap<String, String> getShowAs() {
        // nothing by default
        return null;
    }

    /**
     * Fields that can neither be inserted nor updated.
     * 
     * @return fields that cannot be modified.
     */
    public Set<String> getReadOnlyFields() {
        return Collections.emptySet();
    }

    /**
     * Fields that can only be set on insertion.
     * 
     * @return fields that cannot be modified.
     */
    public Set<String> getInsertOnlyFields() {
        return Collections.emptySet();
    }

    private final SQLCache<SQLRowAccessor, Object> getModelCache() {
        if (this.modelCache == null)
            this.modelCache = new SQLCache<SQLRowAccessor, Object>(60, -1, "modelObjects of " + this.getCode());
        return this.modelCache;
    }

    public void unarchiveNonRec(int id) throws SQLException {
        this.unarchiveNonRec(this.getTable().getRow(id));
    }

    private void unarchiveNonRec(SQLRow row) throws SQLException {
        checkUndefined(row);
        if (!row.isArchived())
            return;

        final Set<SQLRow> connectedRows = this.getArchivedConnectedRows(Collections.singleton(row));
        for (final SQLRow desc : connectedRows) {
            getElement(desc.getTable()).unarchiveSingle(desc);
        }
        for (final SQLRow desc : connectedRows) {
            DeletionMode.UnArchiveMode.fireChange(desc);
        }
    }

    // *** getConnected*

    private Set<SQLRow> getArchivedConnectedRows(Collection<SQLRow> rows) throws SQLException {
        final Set<SQLRow> res = new HashSet<SQLRow>();
        for (final SQLRow row : rows) {
            this.getElement(row.getTable()).getArchivedConnectedRows(row, res);
        }
        return res;
    }

    private void getArchivedConnectedRows(SQLRow row, Set<SQLRow> rows) throws SQLException {
        check(row);
        // si on était déjà dedans, ne pas continuer
        if (!rows.add(row))
            return;

        // we want ARCHIVED existant and defined rows (since we never touch undefined ones)
        final SQLRowMode mode = new SQLRowMode(ArchiveMode.ARCHIVED, true, true);
        final Set<SQLRow> foreigns = new HashSet<SQLRow>(this.getNormalForeigns(row, mode).values());
        final SQLRow parent = this.getForeignParent(row, mode);
        if (parent != null) {
            foreigns.add(parent);
        }
        // private ff are handled by DeletionMode
        // shared ff are never touched by DeletionMode

        // recurse
        for (final SQLRow foreign : foreigns) {
            this.getElement(foreign.getTable()).getArchivedConnectedRows(foreign, rows);
        }
    }

    // *** update

    /**
     * Compute the necessary steps to transform <code>from</code> into <code>to</code>.
     * 
     * @param from the row currently in the db.
     * @param to the new values.
     * @return the script transforming <code>from</code> into <code>to</code>.
     */
    public final UpdateScript update(SQLRowValues from, SQLRowValues to) {
        check(from);
        check(to);

        if (!from.hasID())
            throw new IllegalArgumentException("missing id in " + from);
        if (from.getID() != to.getID())
            throw new IllegalArgumentException("not the same row: " + from + " != " + to);

        final Set<SQLField> fks = this.getTable().getForeignKeys();
        final UpdateScript res = new UpdateScript(this.getTable());
        for (final String field : to.getFields()) {
            if (!fks.contains(this.getTable().getField(field))) {
                res.getUpdateRow().put(field, to.getObject(field));
            } else {
                final Object fromPrivate = from.getObject(field);
                final Object toPrivate = to.getObject(field);
                if (!fromPrivate.getClass().equals(toPrivate.getClass()))
                    throw new IllegalArgumentException("asymmetric tree " + fromPrivate + " != " + toPrivate);
                final boolean isPrivate = this.getPrivateForeignFields().contains(field);
                if (fromPrivate instanceof SQLRowValues) {
                    final SQLRowValues fromPR = (SQLRowValues) fromPrivate;
                    final SQLRowValues toPR = (SQLRowValues) toPrivate;
                    if (isPrivate) {
                        if (from.isForeignEmpty(field) && to.isForeignEmpty(field)) {
                            // nothing to do, don't add to v
                        } else if (from.isForeignEmpty(field)) {
                            // insert, eg CPI.ID_OBS=1 -> CPI.ID_OBS={DES="rouillé"}
                            // clear referents otherwise we will merge the updateRow with the to
                            // graph (toPR being a private is pointed to by its owner, which itself
                            // points to others, but we just want the private)
                            res.getUpdateRow().put(field, toPR.deepCopy().clearReferents());
                        } else if (to.isForeignEmpty(field)) {
                            // archive
                            res.addToArchive(this.getForeignElement(field), fromPR);
                        } else {
                            // neither is empty
                            if (fromPR.getID() != toPR.getID())
                                throw new IllegalArgumentException("private have changed: " + fromPR + " != " + toPR);
                            res.put(field, this.getForeignElement(field).update(fromPR, toPR));
                        }
                    } else {
                        res.getUpdateRow().put(field, toPR.getID());
                    }
                } else {
                    final Number fromP_ID = (Number) fromPrivate;
                    final Number toP_ID = (Number) toPrivate;
                    if (isPrivate) {
                        // avoid Integer(3) != Long(3)
                        if (fromP_ID.longValue() != toP_ID.longValue())
                            throw new IllegalArgumentException("cannot change private ID");
                        // if they're the same, nothing to do
                    } else {
                        res.getUpdateRow().put(field, toP_ID);
                    }
                }
            }
        }

        return res;
    }

    public final void unarchive(int id) throws SQLException {
        this.unarchive(this.getTable().getRow(id));
    }

    public void unarchive(final SQLRow row) throws SQLException {
        checkUndefined(row);
        // don't test row.isArchived() (it is done by getTree())
        // to allow an unarchived parent to unarchive all its descendants.

        // nos descendants
        final List<SQLRow> descsAndMe = this.getTree(row, true);
        final Set<SQLRow> connectedRows = this.getArchivedConnectedRows(descsAndMe);
        SQLUtils.executeAtomic(this.getTable().getBase().getDataSource(), new SQLFactory<Object>() {
            @Override
            public Object create() throws SQLException {
                for (final SQLRow desc : connectedRows) {
                    getElement(desc.getTable()).unarchiveSingle(desc);
                }
                for (final SQLRow desc : connectedRows) {
                    DeletionMode.UnArchiveMode.fireChange(desc);
                }

                // reference
                // nothing to do : nobody points to an archived row

                return null;
            }
        });
    }

    public final void archive(int id) throws SQLException {
        this.archive(this.getTable().getRow(id));
    }

    public final void archive(SQLRow row) throws SQLException {
        this.archive(row, true);
    }

    /**
     * Archive la ligne demandée et tous ses descendants mais ne cherche pas à couper les références
     * pointant sur ceux-ci. ATTN peut donc laisser la base dans un état inconsistent, à n'utiliser
     * que si aucun lien ne pointe sur ceux ci. En revanche, accélère grandement (par exemple pour
     * OBSERVATION) car pas besoin de chercher toutes les références.
     * 
     * @param id la ligne voulue.
     * @throws SQLException if pb while archiving.
     */
    public final void archiveNoCut(int id) throws SQLException {
        this.archive(this.getTable().getRow(id), false);
    }

    protected void archive(final SQLRow row, final boolean cutLinks) throws SQLException {
        this.archive(new TreesOfSQLRows(this, row), cutLinks);
    }

    protected void archive(final TreesOfSQLRows trees, final boolean cutLinks) throws SQLException {
        if (trees.getElem() != this)
            throw new IllegalArgumentException(this + " != " + trees.getElem());
        for (final SQLRow row : trees.getRows())
            checkUndefined(row);

        SQLUtils.executeAtomic(this.getTable().getBase().getDataSource(), new SQLFactory<Object>() {
            @Override
            public Object create() throws SQLException {
                // reference
                // d'abord couper les liens qui pointent sur les futurs archivés
                if (cutLinks) {
                    // TODO prend bcp de temps
                    // FIXME update tableau pour chaque observation, ecrase les changements
                    // faire : 'La base à changée voulez vous recharger ou garder vos modifs ?'
                    final MultiMap externReferences = trees.getExternReferences();
                    // avoid toString() which might make requests to display rows (eg archived)
                    if (Log.get().isLoggable(Level.FINEST))
                        Log.get().finest("will cut : " + externReferences);
                    final MapIterator refIter = new EntrySetMapIterator(externReferences);
                    while (refIter.hasNext()) {
                        final SQLField refKey = (SQLField) refIter.next();
                        final Collection<?> refList = (Collection<?>) refIter.getValue();
                        final Iterator<?> listIter = refList.iterator();
                        while (listIter.hasNext()) {
                            final SQLRow ref = (SQLRow) listIter.next();
                            ref.createEmptyUpdateRow().putEmptyLink(refKey.getName()).update();
                        }
                    }
                    Log.get().finest("done cutting links");
                }

                // on archive tous nos descendants
                for (final SQLRowAccessor desc : trees.getFlatDescendants()) {
                    getElement(desc.getTable()).archiveSingle(desc);
                    // ne pas faire les fire après sinon qd on efface plusieurs éléments de la même
                    // table :
                    // on fire pour le 1er => updateSearchList => IListe.select(userID)
                    // hors si userID a aussi été archivé (mais il n'y a pas eu son fire
                    // correspondant), le component va lancer un RowNotFound
                    DeletionMode.ArchiveMode.fireChange(desc);
                }
                // foreign field
                // nothing to do

                return null;
            }
        });
    }

    private final void archiveSingle(SQLRowAccessor r) throws SQLException {
        this.changeSingle(r, DeletionMode.ArchiveMode);
    }

    private final void unarchiveSingle(SQLRowAccessor r) throws SQLException {
        this.changeSingle(r, DeletionMode.UnArchiveMode);
    }

    private final void changeSingle(SQLRowAccessor r, DeletionMode m) throws SQLException {
        m.execute(this, r);
    }

    public void delete(SQLRowAccessor r) throws SQLException {
        this.check(r);
        if (true)
            throw new UnsupportedOperationException("not yet implemented.");

        this.changeSingle(r, DeletionMode.DeleteMode);
    }

    public final SQLTable getTable() {
        return this.primaryTable;
    }

    /**
     * A code identifying a specific meaning for the table and fields. I.e. it is used by
     * {@link #getName() names} and {@link SQLFieldTranslator item metadata}. E.g. if two
     * applications use the same table for different purposes (at different times, of course), their
     * elements should not share a code. On the contrary, if one application merely adds a field to
     * an existing table, the new element should keep the same code so that existing name and
     * documentation remain.
     * 
     * @return a code for the table and its meaning.
     */
    public synchronized final String getCode() {
        if (this.code == DEFERRED_CODE) {
            final String createCode = this.createCode();
            if (createCode == DEFERRED_CODE)
                throw new IllegalStateException("createCode() returned DEFERRED_CODE");
            this.code = createCode;
        }
        return this.code;
    }

    /**
     * Is the rows of this element shared, ie rows are unique and must not be copied.
     * 
     * @return <code>true</code> if this element is shared.
     */
    public boolean isShared() {
        return false;
    }

    /**
     * Must the rows of this element be copied when traversing a hierarchy.
     * 
     * @return <code>true</code> if the element must not be copied.
     */
    public boolean dontDeepCopy() {
        return false;
    }

    // *** rf

    public final synchronized Set<SQLField> getOtherReferentFields() {
        this.initRF();
        return this.otherRF;
    }

    public final synchronized Set<SQLField> getChildrenReferentFields() {
        this.initChildRF();
        return this.childRF;
    }

    /**
     * The private foreign fields pointing to this table. Eg if this is OBSERVATION,
     * {SOURCE.ID_OBS1, SOURCE.ID_OBS2, CPI.ID_OBS, LOCAL.ID_OBS} ; if this is LOCAL, {}.
     * 
     * @return the private foreign fields pointing to this table.
     */
    public final synchronized Set<SQLField> getPrivateParentReferentFields() {
        this.initRF();
        return this.privateParentRF;
    }

    /**
     * Specify the tables whose rows are contained in rows of this element. They can be specified
     * with table names, in which case there must be exactly one foreign field from the specified
     * table to this element (eg "BATIMENT" for element SITE). Otherwise it must the fullname of
     * foreign field which points to the table of this element (eg "RECEPTEUR.ID_LOCAL").
     * 
     * @return a Set of String.
     * @see #getParentFFName()
     */
    protected Set<String> getChildren() {
        return Collections.emptySet();
    }

    // *** ff

    public final synchronized Set<String> getNormalForeignFields() {
        this.initFF();
        return this.normalFF;
    }

    public final synchronized Set<String> getSharedForeignFields() {
        this.initFF();
        return this.sharedFF;
    }

    public final synchronized String getParentForeignField() {
        this.initFF();
        return this.parentFF;
    }

    private final SQLField getParentFF() {
        final String name = getParentFFName();
        return name == null ? null : this.getTable().getField(name);
    }

    /**
     * Should be overloaded to specify our parent. NOTE the relationship must be specified only once
     * either with this method or with {@link #getChildren()}. This method is preferred since it
     * avoids writing IFs to account for customer differences and there's no ambiguity (you return a
     * field of this table instead of a table name that must be searched in roots and then a foreign
     * key must be found).
     * 
     * @return <code>null</code> for this implementation.
     * @see #getChildren()
     */
    protected String getParentFFName() {
        return null;
    }

    public final SQLElement getParentElement() {
        if (this.getParentForeignField() == null)
            return null;
        else
            return this.getForeignElement(this.getParentForeignField());
    }

    private final synchronized Map<String, SQLElement> getPrivateFF() {
        this.initFF();
        return this.privateFF;
    }

    /**
     * The fields that private to this table, ie rows pointed by these fields are referenced only by
     * one row of this table.
     * 
     * @return private fields of this element.
     */
    public final Set<String> getPrivateForeignFields() {
        return Collections.unmodifiableSet(this.getPrivateFF().keySet());
    }

    public final SQLElement getPrivateElement(String foreignField) {
        return this.getPrivateFF().get(foreignField);
    }

    /**
     * The graph of this table and its private fields.
     * 
     * @return a rowValues of this element's table with rowValues for each private foreign field.
     */
    public final SQLRowValues getPrivateGraph() {
        final SQLRowValues res = new SQLRowValues(this.getTable());
        res.setAllToNull();
        for (final Entry<String, SQLElement> e : this.getPrivateFF().entrySet()) {
            res.put(e.getKey(), e.getValue().getPrivateGraph());
        }
        return res;
    }

    /**
     * Renvoie les champs qui sont 'privé' càd que les ligne pointées par ce champ ne sont
     * référencées que par une et une seule ligne de cette table. Cette implementation renvoie une
     * liste vide. This method is intented for subclasses, call {@link #getPrivateForeignFields()}
     * which does some checks.
     * 
     * @return la List des noms des champs privés, eg ["ID_OBSERVATION_2"].
     */
    protected List<String> getPrivateFields() {
        return Collections.emptyList();
    }

    public final void clearPrivateFields(SQLRowValues rowVals) {
        for (String s : getPrivateFF().keySet()) {
            rowVals.remove(s);
        }
    }

    final Map<String, ReferenceAction> getActions() {
        this.initFF();
        return this.actions;
    }

    /**
     * Specify an action for a normal foreign field.
     * 
     * @param ff the foreign field name.
     * @param action what to do if a referenced row must be archived.
     * @throws IllegalArgumentException if <code>ff</code> is not a normal foreign field.
     */
    public final void setAction(final String ff, ReferenceAction action) throws IllegalArgumentException {
        // shared must be RESTRICT, parent at least CASCADE (to avoid child without a parent),
        // normal is free
        if (action.compareTo(ReferenceAction.RESTRICT) < 0 && !this.getNormalForeignFields().contains(ff))
            // getField() checks if the field exists
            throw new IllegalArgumentException(getTable().getField(ff).getSQLName() + " is not a normal foreign field : " + this.getNormalForeignFields());
        this.getActions().put(ff, action);
    }

    // *** rf and ff

    /**
     * The links towards the parents (either {@link #getParentForeignField()} or
     * {@link #getPrivateParentReferentFields()}) of this element.
     * 
     * @return the graph links towards the parents of this element.
     */
    public final Set<Link> getParentsLinks() {
        final Set<SQLField> refFields = this.getPrivateParentReferentFields();
        final Set<Link> res = new HashSet<Link>(refFields.size());
        final DatabaseGraph graph = this.getTable().getDBSystemRoot().getGraph();
        for (final SQLField refField : refFields)
            res.add(graph.getForeignLink(refField));
        if (this.getParentForeignField() != null)
            res.add(graph.getForeignLink(this.getTable().getField(getParentForeignField())));
        return res;
    }

    /**
     * The elements beneath this, ie both children and privates.
     * 
     * @return our children elements.
     */
    public final Set<SQLElement> getChildrenElements() {
        final Set<SQLElement> res = new HashSet<SQLElement>();
        res.addAll(this.getPrivateFF().values());
        for (final SQLTable child : new SQLFieldsSet(this.getChildrenReferentFields()).getTables())
            res.add(getElement(child));
        return res;
    }

    public final SQLElement getChildElement(final String tableName) {
        final SQLField field = CollectionUtils.getSole(new SQLFieldsSet(this.getChildrenReferentFields()).getFields(tableName));
        if (field == null)
            throw new IllegalStateException("no child table named " + tableName);
        else
            return this.getElement(field.getTable());
    }

    /**
     * The tables beneath this.
     * 
     * @return our descendants, including this.
     * @see #getChildrenElements()
     */
    public final Set<SQLTable> getDescendantTables() {
        final Set<SQLTable> res = new HashSet<SQLTable>();
        this.getDescendantTables(res);
        return res;
    }

    private final void getDescendantTables(Set<SQLTable> res) {
        res.add(this.getTable());
        for (final SQLElement elem : this.getChildrenElements()) {
            res.addAll(elem.getDescendantTables());
        }
    }

    // *** request

    public final ComboSQLRequest getComboRequest() {
        return getComboRequest(false);
    }

    /**
     * Return a combo request for this element.
     * 
     * @param create <code>true</code> if a new instance should be returned, <code>false</code> to
     *        return a shared instance.
     * @return a combo request for this.
     */
    public final ComboSQLRequest getComboRequest(final boolean create) {
        if (!create) {
            if (this.combo == null) {
                this.combo = this.createComboRequest();
            }
            return this.combo;
        } else {
            return this.createComboRequest();
        }
    }

    protected ComboSQLRequest createComboRequest() {
        return new ComboSQLRequest(this.getTable(), this.getComboFields());
    }

    // not all elements need to be displayed in combos so don't make this method abstract
    protected List<String> getComboFields() {
        return this.getListFields();
    }

    public final synchronized ListSQLRequest getListRequest() {
        if (this.list == null) {
            this.list = createListRequest();
        }
        return this.list;
    }

    protected ListSQLRequest createListRequest() {
        return new ListSQLRequest(this.getTable(), this.getListFields());
    }

    public final SQLTableModelSourceOnline getTableSource() {
        return this.getTableSource(!cacheTableSource());
    }

    /**
     * Return a table source for this element.
     * 
     * @param create <code>true</code> if a new instance should be returned, <code>false</code> to
     *        return a shared instance.
     * @return a table source for this.
     */
    public final synchronized SQLTableModelSourceOnline getTableSource(final boolean create) {
        if (!create) {
            if (this.tableSrc == null) {
                this.tableSrc = createAndInitTableSource();
            }
            return this.tableSrc;
        } else
            return this.createAndInitTableSource();
    }

    public final SQLTableModelSourceOnline createTableSource(final List<String> fields) {
        return initTableSource(new SQLTableModelSourceOnline(new ListSQLRequest(this.getTable(), fields)));
    }

    public final SQLTableModelSourceOnline createTableSource(final Where w) {
        final SQLTableModelSourceOnline res = this.getTableSource(true);
        res.getReq().setWhere(w);
        return res;
    }

    private final SQLTableModelSourceOnline createAndInitTableSource() {
        final SQLTableModelSourceOnline res = this.createTableSource();
        res.getColumns().addAll(this.additionalListCols);
        return initTableSource(res);
    }

    protected synchronized void _initTableSource(final SQLTableModelSourceOnline res) {
    }

    public final synchronized SQLTableModelSourceOnline initTableSource(final SQLTableModelSourceOnline res) {
        // do init first since it can modify the columns
        this._initTableSource(res);
        // setEditable(false) on read only fields
        // MAYBE setReadOnlyFields() on SQLTableModelSource, so that SQLTableModelLinesSource can
        // check in commit()
        final Set<String> dontModif = CollectionUtils.union(this.getReadOnlyFields(), this.getInsertOnlyFields());
        for (final String f : dontModif)
            for (final SQLTableModelColumn col : res.getColumns(getTable().getField(f)))
                if (col instanceof SQLTableModelColumnPath)
                    ((SQLTableModelColumnPath) col).setEditable(false);
        return res;
    }

    protected SQLTableModelSourceOnline createTableSource() {
        // also create a new ListSQLRequest, otherwise it's a backdoor to change the behaviour of
        // the new TableModelSource
        return new SQLTableModelSourceOnline(this.createListRequest());
    }

    /**
     * Whether to cache our tableSource.
     * 
     * @return <code>true</code> to call {@link #createTableSource()} only once, or
     *         <code>false</code> to call it each time {@link #getTableSource()} is.
     */
    protected boolean cacheTableSource() {
        return true;
    }

    abstract protected List<String> getListFields();

    public final void addListFields(final List<String> fields) {
        for (final String f : fields)
            this.addListColumn(new SQLTableModelColumnPath(getTable().getField(f)));
    }

    public final void addListColumn(SQLTableModelColumn col) {
        this.additionalListCols.add(col);
    }

    public final Collection<IListeAction> getRowActions() {
        return this.rowActions;
    }

    public final void addRowActionsListener(final IClosure<ListChangeIndex<IListeAction>> listener) {
        this.rowActions.getRecipe().addListener(listener);
    }

    public final void removeRowActionsListener(final IClosure<ListChangeIndex<IListeAction>> listener) {
        this.rowActions.getRecipe().rmListener(listener);
    }

    public String getDescription(SQLRow fromRow) {
        return fromRow.toString();
    }

    // *** iterators

    static interface ChildProcessor<R extends SQLRowAccessor> {
        public void process(R parent, SQLField joint, R child) throws SQLException;
    }

    /**
     * Execute <code>c</code> for each children of <code>row</code>. NOTE: <code>c</code> will be
     * called with <code>row</code> as its first parameter, and with its child of the same type
     * (SQLRow or SQLRowValues) for the third parameter.
     * 
     * @param <R> type of SQLRowAccessor to use.
     * @param row the parent row.
     * @param c what to do for each children.
     * @param deep <code>true</code> to ignore {@link #dontDeepCopy()}.
     * @param archived <code>true</code> to iterate over archived children.
     * @throws SQLException if <code>c</code> raises an exn.
     */
    private <R extends SQLRowAccessor> void forChildrenDo(R row, ChildProcessor<? super R> c, boolean deep, boolean archived) throws SQLException {
        for (final SQLField childField : this.getChildrenReferentFields()) {
            if (deep || !this.getElement(childField.getTable()).dontDeepCopy()) {
                final List<SQLRow> children = row.asRow().getReferentRows(childField, archived ? SQLSelect.ARCHIVED : SQLSelect.UNARCHIVED);
                // eg BATIMENT[516]
                for (final SQLRow child : children) {
                    c.process(row, childField, convert(child, row));
                }
            }
        }
    }

    // convert toConv to same type as row
    @SuppressWarnings("unchecked")
    private <R extends SQLRowAccessor> R convert(final SQLRow toConv, R row) {
        final R ch;
        if (row instanceof SQLRow)
            ch = (R) toConv;
        else if (row instanceof SQLRowValues)
            ch = (R) toConv.createUpdateRow();
        else
            throw new IllegalStateException("SQLRowAccessor is neither SQLRow nor SQLRowValues: " + toConv);
        return ch;
    }

    // first the leaves
    private void forDescendantsDo(final SQLRow row, final ChildProcessor<SQLRow> c, final boolean deep) throws SQLException {
        this.forDescendantsDo(row, c, deep, true, false);
    }

    <R extends SQLRowAccessor> void forDescendantsDo(final R row, final ChildProcessor<R> c, final boolean deep, final boolean leavesFirst, final boolean archived) throws SQLException {
        this.check(row);
        this.forChildrenDo(row, new ChildProcessor<R>() {
            public void process(R parent, SQLField joint, R child) throws SQLException {
                if (!leavesFirst)
                    c.process(parent, joint, child);
                getElement(child.getTable()).forDescendantsDo(child, c, deep, leavesFirst, archived);
                if (leavesFirst)
                    c.process(parent, joint, child);
            }
        }, deep, archived);
    }

    void check(SQLRowAccessor row) {
        if (!row.getTable().equals(this.getTable()))
            throw new IllegalArgumentException("row must of table " + this.getTable() + " : " + row);
    }

    private void checkUndefined(SQLRow row) {
        this.check(row);
        if (row.isUndefined())
            throw new IllegalArgumentException("row is undefined: " + row);
    }

    // *** copy

    public final SQLRow copyRecursive(int id) throws SQLException {
        return this.copyRecursive(this.getTable().getRow(id));
    }

    public final SQLRow copyRecursive(SQLRow row) throws SQLException {
        return this.copyRecursive(row, null);
    }

    public SQLRow copyRecursive(final SQLRow row, final SQLRow parent) throws SQLException {
        return this.copyRecursive(row, parent, null);
    }

    /**
     * Copy <code>row</code> and its children into <code>parent</code>.
     * 
     * @param row which row to clone.
     * @param parent which parent the clone will have, <code>null</code> meaning the same than
     *        <code>row</code>.
     * @param c allow one to modify the copied rows before they are inserted, can be
     *        <code>null</code>.
     * @return the new copy.
     * @throws SQLException if an error occurs.
     */
    public SQLRow copyRecursive(final SQLRow row, final SQLRow parent, final IClosure<SQLRowValues> c) throws SQLException {
        check(row);
        if (row.isUndefined())
            return row;

        // current => new copy
        final Map<SQLRow, SQLRowValues> copies = new HashMap<SQLRow, SQLRowValues>();

        return SQLUtils.executeAtomic(this.getTable().getBase().getDataSource(), new SQLFactory<SQLRow>() {
            @Override
            public SQLRow create() throws SQLException {

                // eg SITE[128]
                final SQLRowValues copy = createTransformedCopy(row, parent, c);
                copies.put(row, copy);

                forDescendantsDo(row, new ChildProcessor<SQLRow>() {
                    public void process(SQLRow parent, SQLField joint, SQLRow desc) throws SQLException {
                        final SQLRowValues parentCopy = copies.get(parent);
                        if (parentCopy == null)
                            throw new IllegalStateException("null copy of " + parent);
                        final SQLRowValues descCopy = createTransformedCopy(desc, null, c);
                        descCopy.put(joint.getName(), parentCopy);
                        copies.put(desc, descCopy);
                    }
                }, false, false, false);
                // ne pas descendre en deep

                // reference
                forDescendantsDo(row, new ChildProcessor<SQLRow>() {
                    public void process(SQLRow parent, SQLField joint, SQLRow desc) throws SQLException {
                        final CollectionMap<SQLField, SQLRow> normalReferents = getElement(desc.getTable()).getNonChildrenReferents(desc);
                        for (final Entry<SQLField, Collection<SQLRow>> e : normalReferents.entrySet()) {
                            // eg SOURCE.ID_CPI
                            final SQLField refField = e.getKey();
                            for (final SQLRow ref : e.getValue()) {
                                // eg copy of SOURCE[12] is SOURCE[354]
                                final SQLRowValues refCopy = copies.get(ref);
                                if (refCopy != null) {
                                    // CPI[1203]
                                    final SQLRowValues referencedCopy = copies.get(desc);
                                    refCopy.put(refField.getName(), referencedCopy);
                                }
                            }
                        }
                    }
                }, false);

                // we used to remove foreign links pointing outside the copy, but this was almost
                // never right, e.g. : copy a batiment, its locals loose ID_FAMILLE ; copy a local,
                // if a source in it points to an item in another local, its copy won't.

                return copy.insert();
            }
        });
    }

    private final SQLRowValues createTransformedCopy(SQLRow desc, SQLRow parent, final IClosure<SQLRowValues> c) throws SQLException {
        final SQLRowValues copiedVals = getElement(desc.getTable()).createCopy(desc, parent);
        assert copiedVals != null : "failed to copy " + desc;
        if (c != null)
            c.executeChecked(copiedVals);
        return copiedVals;
    }

    public final SQLRow copy(int id) throws SQLException {
        return this.copy(this.getTable().getRow(id));
    }

    public final SQLRow copy(SQLRow row) throws SQLException {
        return this.copy(row, null);
    }

    public final SQLRow copy(SQLRow row, SQLRow parent) throws SQLException {
        final SQLRowValues copy = this.createCopy(row, parent);
        return copy == null ? row : copy.insert();
    }

    public final SQLRowValues createCopy(int id) {
        final SQLRow row = this.getTable().getRow(id);
        return this.createCopy(row, null);
    }

    /**
     * Copies the passed row into an SQLRowValues. NOTE: this method does not access the DB, ie the
     * copy won't be a copy of the current values in DB, but of the current values of the passed
     * instance.
     * 
     * @param row the row to copy, can be <code>null</code>.
     * @param parent the parent the copy will be in, <code>null</code> meaning the same as
     *        <code>row</code>.
     * @return a copy ready to be inserted, or <code>null</code> if <code>row</code> cannot be
     *         copied.
     */
    public SQLRowValues createCopy(SQLRow row, SQLRow parent) {
        // do NOT copy the undefined
        if (row == null || row.isUndefined())
            return null;
        this.check(row);

        final SQLRowValues copy = new SQLRowValues(this.getTable());
        this.loadAllSafe(copy, row);

        for (final String privateName : this.getPrivateForeignFields()) {
            final SQLElement privateElement = this.getPrivateElement(privateName);
            if (!privateElement.dontDeepCopy() && !row.isForeignEmpty(privateName)) {
                final SQLRowValues child = privateElement.createCopy(row.getInt(privateName));
                copy.put(privateName, child);
            } else {
                copy.putEmptyLink(privateName);
            }
        }
        // si on a spécifié un parent, eg BATIMENT[23]
        if (parent != null) {
            final SQLTable foreignTable = this.getTable().getBase().getGraph().getForeignTable(this.getTable().getField(this.getParentForeignField()));
            if (!parent.getTable().equals(foreignTable))
                throw new IllegalArgumentException(parent + " is not a parent of " + row);
            copy.put(this.getParentForeignField(), parent.getID());
        }

        return copy;
    }

    /**
     * Load all values that can be safely copied (shared by multiple rows). This means all values
     * except private, primary, order and archive.
     * 
     * @param vals the row to modify.
     * @param row the row to be loaded.
     */
    public final void loadAllSafe(final SQLRowValues vals, final SQLRow row) {
        check(vals);
        check(row);
        vals.setAll(row.getAllValues());
        vals.load(row, this.getNormalForeignFields());
        if (this.getParentForeignField() != null)
            vals.put(this.getParentForeignField(), row.getObject(this.getParentForeignField()));
        vals.load(row, this.getSharedForeignFields());
    }

    // *** getRows

    /**
     * Returns the descendant rows : the children of this element, recursively. ATTN does not carry
     * the hierarchy.
     * 
     * @param row a SQLRow.
     * @return the descendant rows by SQLTable.
     */
    public final CollectionMap<SQLTable, SQLRow> getDescendants(SQLRow row) {
        check(row);
        final CollectionMap<SQLTable, SQLRow> mm = new CollectionMap<SQLTable, SQLRow>();
        try {
            this.forDescendantsDo(row, new ChildProcessor<SQLRow>() {
                public void process(SQLRow parent, SQLField joint, SQLRow child) throws SQLException {
                    mm.put(joint.getTable(), child);
                }
            }, true);
        } catch (SQLException e) {
            // never happen
            e.printStackTrace();
        }
        return mm;
    }

    /**
     * Returns the tree beneath the passed row. The list is ordered "leaves-first", ie the last item
     * is the root.
     * 
     * @param row the root of the desired tree.
     * @param archived <code>true</code> if the returned rows should be archived.
     * @return a List of SQLRow.
     */
    private List<SQLRow> getTree(SQLRow row, boolean archived) {
        check(row);
        // nos descendants
        final List<SQLRow> descsAndMe = new ArrayList<SQLRow>();
        try {
            this.forDescendantsDo(row, new ChildProcessor<SQLRow>() {
                public void process(SQLRow parent, SQLField joint, SQLRow desc) throws SQLException {
                    descsAndMe.add(desc);
                }
            }, true, true, archived);
        } catch (SQLException e) {
            // never happen cause process don't throw it
            e.printStackTrace();
        }
        if (row.isArchived() == archived)
            descsAndMe.add(row);
        return descsAndMe;
    }

    /**
     * Returns the children of the passed row.
     * 
     * @param row a SQLRow.
     * @return the children rows by SQLTable.
     */
    public CollectionMap<SQLTable, SQLRow> getChildrenRows(SQLRow row) {
        check(row);
        // ArrayList
        final CollectionMap<SQLTable, SQLRow> mm = new CollectionMap<SQLTable, SQLRow>();
        try {
            this.forChildrenDo(row, new ChildProcessor<SQLRow>() {
                public void process(SQLRow parent, SQLField joint, SQLRow child) throws SQLException {
                    mm.put(child.getTable(), child);
                }
            }, true, false);
        } catch (SQLException e) {
            // never happen
            e.printStackTrace();
        }
        return mm;
    }

    public SQLRowAccessor getParent(SQLRowAccessor row) {
        check(row);
        final List<SQLRowAccessor> parents = new ArrayList<SQLRowAccessor>();
        for (final Link l : this.getParentsLinks()) {
            parents.addAll(row.followLink(l));
        }
        if (parents.size() > 1)
            throw new IllegalStateException("More than one parent for " + row + " : " + parents);
        return parents.size() == 0 ? null : parents.get(0);
    }

    public SQLRow getForeignParent(SQLRow row) {
        return this.getForeignParent(row, SQLRowMode.VALID);
    }

    // ATTN cannot replace with getParent(SQLRowAccessor) since some callers assume the result to be
    // a foreign row (which isn't the case for private)
    private SQLRow getForeignParent(SQLRow row, final SQLRowMode mode) {
        check(row);
        return this.getParentForeignField() == null ? null : row.getForeignRow(this.getParentForeignField(), mode);
    }

    // {SQLField => List<SQLRow>}
    CollectionMap<SQLField, SQLRow> getNonChildrenReferents(SQLRow row) {
        check(row);
        final CollectionMap<SQLField, SQLRow> mm = new CollectionMap<SQLField, SQLRow>();
        final Set<SQLField> nonChildren = new HashSet<SQLField>(row.getTable().getDBSystemRoot().getGraph().getReferentKeys(row.getTable()));
        nonChildren.removeAll(this.getChildrenReferentFields());
        for (final SQLField refField : nonChildren) {
            // eg CONTACT.ID_SITE => [CONTACT[12], CONTACT[13]]
            mm.putAll(refField, row.getReferentRows(refField));
        }
        return mm;
    }

    public Map<String, SQLRow> getNormalForeigns(SQLRow row) {
        return this.getNormalForeigns(row, SQLRowMode.DEFINED);
    }

    private Map<String, SQLRow> getNormalForeigns(SQLRow row, final SQLRowMode mode) {
        check(row);
        final Map<String, SQLRow> mm = new HashMap<String, SQLRow>();
        final Iterator<String> iter = this.getNormalForeignFields().iterator();
        while (iter.hasNext()) {
            // eg SOURCE.ID_CPI
            final String ff = iter.next();
            // eg CPI[12]
            final SQLRow foreignRow = row.getForeignRow(ff, mode);
            if (foreignRow != null)
                mm.put(ff, foreignRow);
        }
        return mm;
    }

    /**
     * Returns a java object modeling the passed row.
     * 
     * @param row the row to model.
     * @return an instance modeling the passed row or <code>null</code> if there's no class to model
     *         this table.
     * @see SQLRowAccessor#getModelObject()
     */
    public Object getModelObject(SQLRowAccessor row) {
        check(row);
        if (this.getModelClass() == null)
            return null;

        final Object res;
        // seuls les SQLRow peuvent être cachées
        if (row instanceof SQLRow) {
            // MAYBE make the modelObject change
            final CacheResult<Object> cached = this.getModelCache().check(row);
            if (cached.getState() == CacheResult.State.NOT_IN_CACHE) {
                res = this.createModelObject(row);
                this.getModelCache().put(row, res, Collections.singleton(row));
            } else
                res = cached.getRes();
        } else
            res = this.createModelObject(row);

        return res;
    }

    private final Object createModelObject(SQLRowAccessor row) {
        if (!RowBacked.class.isAssignableFrom(this.getModelClass()))
            throw new IllegalStateException("modelClass must inherit from RowBacked: " + this.getModelClass());
        final Constructor<? extends RowBacked> ctor;
        try {
            ctor = this.getModelClass().getConstructor(new Class[] { SQLRowAccessor.class });
        } catch (Exception e) {
            throw ExceptionUtils.createExn(IllegalStateException.class, "no SQLRowAccessor constructor", e);
        }
        try {
            return ctor.newInstance(new Object[] { row });
        } catch (Exception e) {
            throw ExceptionUtils.createExn(RuntimeException.class, "pb creating instance", e);
        }
    }

    protected Class<? extends RowBacked> getModelClass() {
        return null;
    }

    // *** equals

    public boolean equals(SQLRow row, SQLRow row2) throws SQLException {
        check(row);
        if (!row2.getTable().equals(this.getTable()))
            return false;
        if (row.equals(row2))
            return true;
        // the same table but not the same id

        if (!row.getAllValues().equals(row2.getAllValues()))
            return false;

        // shared doivent être partagées (!)
        for (final String shared : this.getSharedForeignFields()) {
            if (row.getInt(shared) != row2.getInt(shared))
                return false;
        }

        // les private equals
        for (final String prvt : this.getPrivateForeignFields()) {
            final SQLElement foreignElement = this.getForeignElement(prvt);
            // ne pas tester
            if (!foreignElement.dontDeepCopy() && !foreignElement.equals(row.getForeignRow(prvt), row2.getForeignRow(prvt)))
                return false;
        }

        return true;
    }

    public boolean equalsRecursive(SQLRow row, SQLRow row2) throws SQLException {
        // if (!equals(row, row2))
        // return false;
        return new SQLElementRowR(this, row).equals(new SQLElementRowR(this, row2));
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof SQLElement) {
            final SQLElement o = (SQLElement) obj;
            // don't need to compare SQLField computed by initFF() & initRF() since they're function
            // of this.table (and by extension its graph) & this.directory
            final boolean parentEq = CompareUtils.equals(this.getParentFFName(), o.getParentFFName());
            return this.getTable().equals(o.getTable()) && CompareUtils.equals(this.getDirectory(), o.getDirectory()) && parentEq && this.getPrivateFields().equals(o.getPrivateFields())
                    && this.getChildren().equals(o.getChildren());
        } else {
            return false;
        }
    }

    @Override
    public synchronized int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.primaryTable.hashCode();
        result = prime * result + ((this.directory == null) ? 0 : this.directory.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " " + this.getTable().getSQLName();
    }

    // *** gui

    public final void addComponentFactory(final String id, final ITransformer<Tuple2<SQLElement, String>, SQLComponent> t) {
        if (t == null)
            throw new NullPointerException();
        this.components.put(id, t);
    }

    public final void removeComponentFactory(final String id, final ITransformer<Tuple2<SQLElement, String>, SQLComponent> t) {
        if (t == null)
            throw new NullPointerException();
        this.components.remove(id, t);
    }

    private final SQLComponent createComponent(final String id, final boolean defaultItem) {
        final String actualID = defaultItem ? DEFAULT_COMP_ID : id;
        final Tuple2<SQLElement, String> t = Tuple2.create(this, id);
        // start from the most recently added factory
        final Iterator<ITransformer<Tuple2<SQLElement, String>, SQLComponent>> iter = ((LinkedList<ITransformer<Tuple2<SQLElement, String>, SQLComponent>>) this.components.getNonNull(actualID))
                .descendingIterator();
        while (iter.hasNext()) {
            final SQLComponent res = iter.next().transformChecked(t);
            if (res != null)
                return res;
        }
        return null;
    }

    public final SQLComponent createDefaultComponent() {
        return this.createComponent(DEFAULT_COMP_ID);
    }

    /**
     * Create the component for the passed ID. First factories for the passed ID are executed, after
     * that if ID is the {@link #DEFAULT_COMP_ID default} then {@link #createComponent()} is called
     * else factories for {@link #DEFAULT_COMP_ID} are executed.
     * 
     * @param id the requested ID.
     * @return the component or <code>null</code> if all factories return <code>null</code>.
     */
    public final SQLComponent createComponent(final String id) {
        SQLComponent res = this.createComponent(id, false);
        if (res == null) {
            if (CompareUtils.equals(id, DEFAULT_COMP_ID)) {
                // since we don't pass id to this method, only call it for DEFAULT_ID
                res = this.createComponent();
            } else {
                res = this.createComponent(id, true);
            }
        }
        res.setCode(id);
        return res;
    }

    /**
     * Retourne l'interface graphique de saisie.
     * 
     * @return l'interface graphique de saisie.
     */
    protected abstract SQLComponent createComponent();

    public final void addToMDPath(final String mdVariant) {
        if (mdVariant == null)
            throw new NullPointerException();
        synchronized (this) {
            final LinkedList<String> newL = new LinkedList<String>(this.mdPath);
            newL.addFirst(mdVariant);
            this.mdPath = Collections.unmodifiableList(newL);
        }
    }

    public synchronized final void removeFromMDPath(final String mdVariant) {
        final LinkedList<String> newL = new LinkedList<String>(this.mdPath);
        if (newL.remove(mdVariant))
            this.mdPath = Collections.unmodifiableList(newL);
    }

    /**
     * The variants searched to find item metadata by
     * {@link SQLFieldTranslator#getDescFor(SQLTable, String, String)}. This allow to configure this
     * element to choose between the simultaneously loaded metadata.
     * 
     * @return the variants path.
     */
    public synchronized final List<String> getMDPath() {
        return this.mdPath;
    }

    /**
     * Allows a module to add a view for a field to this element.
     * 
     * @param field the field of the component.
     * @return <code>true</code> if no view existed.
     */
    public final boolean putAdditionalField(final String field) {
        return this.putAdditionalField(field, (JComponent) null);
    }

    public final boolean putAdditionalField(final String field, final JTextComponent comp) {
        return this.putAdditionalField(field, (JComponent) comp);
    }

    public final boolean putAdditionalField(final String field, final SQLTextCombo comp) {
        return this.putAdditionalField(field, (JComponent) comp);
    }

    // private as only a few JComponent are OK
    private final boolean putAdditionalField(final String field, final JComponent comp) {
        if (this.additionalFields.containsKey(field)) {
            return false;
        } else {
            this.additionalFields.put(field, comp);
            return true;
        }
    }

    public final Map<String, JComponent> getAdditionalFields() {
        return Collections.unmodifiableMap(this.additionalFields);
    }

    public final void removeAdditionalField(final String field) {
        this.additionalFields.remove(field);
    }

    public final boolean askArchive(final Component comp, final Number ids) {
        return this.askArchive(comp, Collections.singleton(ids));
    }

    /**
     * Ask to the user before archiving.
     * 
     * @param comp the parent component.
     * @param ids which rows to archive.
     * @return <code>true</code> if the rows were successfully archived, <code>false</code>
     *         otherwise.
     */
    public boolean askArchive(final Component comp, final Collection<? extends Number> ids) {
        boolean shouldArchive = false;
        final int rowCount = ids.size();
        if (rowCount == 0)
            return true;
        try {
            if (!UserRightsManager.getCurrentUserRights().canDelete(getTable()))
                throw new SQLException("forbidden");
            final TreesOfSQLRows trees = TreesOfSQLRows.createFromIDs(this, ids);
            final CollectionMap<SQLTable, SQLRowAccessor> descs = trees.getDescendantsByTable();
            final SortedMap<SQLField, Integer> externRefs = trees.getExternReferencesCount();
            final String confirmDelete = getTM().trA("sqlElement.confirmDelete");
            final Map<String, Object> map = new HashMap<String, Object>();
            map.put("rowCount", rowCount);
            final int descsSize = descs.size();
            final int externsSize = externRefs.size();
            if (descsSize + externsSize > 0) {
                final String descsS = descsSize > 0 ? toString(descs) : null;
                final String externsS = externsSize > 0 ? toStringExtern(externRefs) : null;
                map.put("descsSize", descsSize);
                map.put("descs", descsS);
                map.put("externsSize", externsSize);
                map.put("externs", externsS);
                map.put("times", "once");
                int i = askSerious(comp, getTM().trM("sqlElement.deleteRef.details", map) + getTM().trM("sqlElement.deleteRef", map), confirmDelete);
                if (i == JOptionPane.YES_OPTION) {
                    map.put("times", "twice");
                    final String msg = externsSize > 0 ? getTM().trM("sqlElement.deleteRef.details2", map) : "";
                    i = askSerious(comp, msg + getTM().trM("sqlElement.deleteRef", map), confirmDelete);
                    if (i == JOptionPane.YES_OPTION) {
                        shouldArchive = true;
                    } else {
                        JOptionPane.showMessageDialog(comp, getTM().trA("sqlElement.noLinesDeleted"), getTM().trA("sqlElement.noLinesDeletedTitle"), JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            } else {
                int i = askSerious(comp, getTM().trM("sqlElement.deleteNoRef", map), confirmDelete);
                if (i == JOptionPane.YES_OPTION) {
                    shouldArchive = true;
                }
            }
            if (shouldArchive) {
                this.archive(trees, true);
                return true;
            } else
                return false;
        } catch (SQLException e) {
            ExceptionHandler.handle(comp, TM.tr("sqlElement.archiveError", this, ids), e);
            return false;
        }
    }

    @SuppressWarnings("rawtypes")
    private final String toString(MultiMap descs) {
        final List<String> l = new ArrayList<String>();
        final Iterator iter = descs.keySet().iterator();
        while (iter.hasNext()) {
            final SQLTable t = (SQLTable) iter.next();
            final Collection rows = (Collection) descs.get(t);
            final SQLElement elem = getElement(t);
            l.add(elemToString(rows.size(), elem));
        }
        return CollectionUtils.join(l, "\n");
    }

    private static final String elemToString(int count, SQLElement elem) {
        return "- " + elem.getName().getNumeralVariant(count, Grammar.INDEFINITE_NUMERAL);
    }

    // traduire TRANSFO.ID_ELEMENT_TABLEAU_PRI -> {TRANSFO[5], TRANSFO[12]}
    // en 2 transformateurs vont perdre leurs champs 'Circuit primaire'
    private final String toStringExtern(SortedMap<SQLField, Integer> externRef) {
        final List<String> l = new ArrayList<String>();
        final Map<String, Object> map = new HashMap<String, Object>(4);
        for (final Map.Entry<SQLField, Integer> entry : externRef.entrySet()) {
            final SQLField foreignKey = entry.getKey();
            final int count = entry.getValue();
            final String label = Configuration.getTranslator(foreignKey.getTable()).getLabelFor(foreignKey);
            final SQLElement elem = getElement(foreignKey.getTable());
            map.put("elementName", elem.getName());
            map.put("count", count);
            map.put("linkName", label);
            l.add(getTM().trM("sqlElement.linksWillBeCut", map));
        }
        return CollectionUtils.join(l, "\n");
    }

    private final int askSerious(Component comp, String msg, String title) {
        return JOptionPane.showConfirmDialog(comp, msg, title + " (" + this.getPluralName() + ")", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    }

}
