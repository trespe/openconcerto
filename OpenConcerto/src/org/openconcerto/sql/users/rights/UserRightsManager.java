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
 
 package org.openconcerto.sql.users.rights;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.CompareUtils.Equalizer;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.Tuple3;
import org.openconcerto.utils.cc.IFactory;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.GuardedBy;

public class UserRightsManager {

    public static final String USER_RIGHT_TABLE = UserRightSQLElement.TABLE_NAME;
    public static final String SUPERUSER_FIELD = "SUPERUSER";
    private static final int ADMIN_ID = SQLRow.NONEXISTANT_ID;
    /**
     * Only administrators can see user rights.
     */
    public static final String ADMIN_FIELD = "ADMIN";
    private static UserRightsManager instance;
    private static final CollectionMap<String, Tuple2<String, Boolean>> SUPERUSER_RIGHTS = CollectionMap.singleton(null, Tuple2.create((String) null, true));
    private static final CollectionMap<String, Tuple2<String, Boolean>> NO_RIGHTS = CollectionMap.singleton(null, Tuple2.create((String) null, false));
    public static final List<MacroRight> DEFAULT_MACRO_RIGHTS = Collections.synchronizedList(new ArrayList<MacroRight>());
    static {
        // "addRight() ambiguous"
        assert ADMIN_ID < SQLRow.MIN_VALID_ID;
        DEFAULT_MACRO_RIGHTS.add(new LockAdminUserRight());
        DEFAULT_MACRO_RIGHTS.add(new TableAllRights(true));
        DEFAULT_MACRO_RIGHTS.add(new TableAllRights(false));
    }

    /**
     * Call setInstance() if none exists.
     * 
     * @param conf where to find the table.
     * @return the instance created, <code>null</code> if an instance already existed or if no table
     *         could be found.
     * @see #clearInstanceIfSame(UserRightsManager)
     */
    public synchronized static UserRightsManager setInstanceIfNone(final DBRoot root) {
        if (instance != null) {
            return null;
        } else {
            return setInstanceFromRoot(root);
        }
    }

    /**
     * Call setInstance(null) if the instance is the same as the parameter.
     * 
     * @param urMngr a manager.
     * @return <code>true</code> if the instance was cleared.
     */
    public synchronized static boolean clearInstanceIfSame(final UserRightsManager urMngr) {
        if (instance == urMngr) {
            setInstance(null);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Set the instance using the table in the passed root.
     * 
     * @param root the root where the rights should be.
     * @return the new instance, <code>null</code> if <code>root</code> does not contain the
     *         {@value #USER_RIGHT_TABLE} table.
     */
    public static UserRightsManager setInstanceFromRoot(final DBRoot root) {
        // do not require rights, one can check the result to see if it's not null
        return setInstance(root.findTable(USER_RIGHT_TABLE, false));
    }

    /**
     * Set the instance.
     * 
     * @param t the table, <code>null</code> to remove.
     * @return the new instance, <code>null</code> if t was.
     */
    public synchronized static UserRightsManager setInstance(final SQLTable t) {
        final SQLTable currentTable = instance == null ? null : instance.getTable();
        if (currentTable != t) {
            if (instance != null) {
                instance.destroy();
            }
            instance = t == null ? null : new UserRightsManager(t);
        }
        return getInstance();
    }

    public synchronized static UserRightsManager getInstance() {
        return instance;
    }

    public static final UserRights getCurrentUserRights() {
        final UserManager mngr = UserManager.getInstance();
        // if right table doesn't exist, give access to everything
        if (getInstance() == null)
            return UserRights.ALLOW_ALL;
        // else if there are rights (and thus users) but no user is defined, use the default rights
        else if (mngr.getCurrentUser() == null)
            return new UserRights(mngr.getTable().getUndefinedID());
        else
            return mngr.getCurrentUser().getRights();
    }

    // Gérer un droit avec une classe
    private final Map<String, MacroRight> macroRights;
    // {user -> {code -> [<object, bool>]}}
    private final Map<Integer, CollectionMap<String, Tuple2<String, Boolean>>> rights;
    private final SQLTable table;
    @GuardedBy("this")
    private SQLTableModifiedListener tableL;
    @GuardedBy("rights")
    private final ListMap<Integer, RightTuple> javaRights;

    private UserRightsManager(final SQLTable t) {
        if (t == null)
            throw new NullPointerException("Missing table");
        this.macroRights = new HashMap<String, MacroRight>();
        this.rights = new HashMap<Integer, CollectionMap<String, Tuple2<String, Boolean>>>();
        this.javaRights = new ListMap<Integer, RightTuple>();
        this.table = t;
        this.tableL = new SQLTableModifiedListener() {
            @Override
            public void tableModified(final SQLTableEvent evt) {
                rightsInvalid();
            }
        };
        this.table.addTableModifiedListener(this.tableL);
        defaultRegister();
    }

    /**
     * enregistre les instances gérants les droits
     */
    private void defaultRegister() {
        synchronized (DEFAULT_MACRO_RIGHTS) {
            for (final MacroRight macroRight : DEFAULT_MACRO_RIGHTS) {
                register(macroRight);
            }
        }
    }

    /**
     * Ajoute une instance pour la gestion d'un droit
     * 
     * @param userRight the instance which will now be used for <code>userRight.getCode()</code>.
     */
    public void register(final MacroRight userRight) {
        this.macroRights.put(userRight.getCode(), userRight);
    }

    /**
     * Add an unconditional right for the passed user. I.e. it is loaded before the SQL ones and
     * even default unconditional rights are before user SQL rights.
     * 
     * @param userID the user id, <code>null</code> meaning for everyone.
     * @param right the right the user should always have.
     */
    public void addRight(Integer userID, RightTuple right) {
        if (right == null)
            throw new NullPointerException("Null right entry");
        synchronized (this.rights) {
            this.javaRights.add(getKey(userID), right);
            this.rightsInvalid();
        }
    }

    /**
     * Add an unconditional right for administrators. This will be after user rights and before
     * default rights.
     * 
     * @param right the right to add.
     */
    public void addRightForAdmins(RightTuple right) {
        if (right == null)
            throw new NullPointerException("Null right entry");
        synchronized (this.rights) {
            this.javaRights.add(ADMIN_ID, right);
            this.rightsInvalid();
        }
    }

    private final int getKey(final Integer userID) {
        if (userID != null && userID < SQLRow.MIN_VALID_ID)
            throw new IllegalArgumentException("invalid ID : " + userID);
        return userID == null ? getDefaultUserId() : userID;
    }

    /**
     * Remove a right.
     * 
     * @param userID the user id, <code>null</code> meaning default user.
     * @param right the right to remove, <code>null</code> meaning remove all.
     * @see #addRight(Integer, RightTuple)
     */
    public void removeRight(final Integer userID, final RightTuple right) {
        synchronized (this.rights) {
            if (right == null)
                this.javaRights.remove(getKey(userID));
            else
                this.javaRights.remove(getKey(userID), right);
            this.rightsInvalid();
        }
    }

    public void removeRightForAdmins(final RightTuple right) {
        this.removeRight(ADMIN_ID, right);
    }

    public synchronized final boolean isValid() {
        return this.tableL != null;
    }

    public synchronized final void destroy() {
        if (this.isValid()) {
            this.getTable().removeTableModifiedListener(this.tableL);
            this.tableL = null;
        }
        assert !this.isValid();
    }

    public final SQLTable getTable() {
        return this.table;
    }

    public final DBRoot getRoot() {
        return this.getTable().getDBRoot();
    }

    public final boolean haveRight(final int userID, final String code) {
        return this.haveRight(userID, code, null);
    }

    public final boolean haveRight(final int userID, final String code, final String object) {
        return this.haveRight(userID, code, object, CompareUtils.OBJECT_EQ);
    }

    /**
     * Whether <code>userID</code> should be allowed the <code>code</code> (e.g. DELETE) right on
     * <code>object</code> (e.g. TENSION).<br>
     * The rights are ordered and the first one that matches is returned. Furthermore after
     * searching for the passed <code>userID</code> the default user is searched. <br>
     * To match, the code of the right must be equal to <code>code</code> and either the object of
     * the right is <code>null</code> or <code>objectMatcher</code> returns <code>true</code> when
     * passed both objects. There's also a special case if <code>object</code> is <code>null</code>
     * : in that case all found objects must be allowed until a right with a <code>null</code>
     * object for the right to be granted. With these rules setting the object of the right to
     * <code>null</code> means giving the right to any object. And searching for the object
     * <code>null</code> means asking if the right is allowed for all the objects. <br>
     * For example if you have these rights (* meaning <code>null</code>) :
     * <ol>
     * <li>del T yes</li>
     * <li>ins T no</li>
     * <li>del T no</li>
     * <li>ins * yes</li>
     * <li>del * yes</li>
     * </ol>
     * then you can delete from T but not insert ; you can however do both on any other object. If
     * you pass <code>null</code> for <code>object</code>, it will return <code>true</code> for del,
     * but <code>false</code> for ins.
     * 
     * @param userID the user.
     * @param code the requested right.
     * @param requestedObject the requested object, can be <code>null</code>.
     * @param objectMatcher how to match objects, first parameter passed is the right object, the
     *        second is <code>requestedObject</code>.
     * @return <code>true</code> if the right is allowed.
     */
    public final boolean haveRight(final int userID, final String code, final String requestedObject, final Equalizer<? super String> objectMatcher) {
        final Set<String> unicity = new HashSet<String>();
        final Boolean userRight = haveRightP(userID, code, requestedObject, objectMatcher, unicity);
        if (userRight != null)
            return userRight;
        final int defaultUser = getDefaultUserId();
        if (defaultUser != userID) {
            final Boolean defaultRight = haveRightP(defaultUser, code, requestedObject, objectMatcher, unicity);
            if (defaultRight != null)
                return defaultRight;
        }

        return false;
    }

    private final Boolean haveRightP(final int userID, final String code, final String object, final Equalizer<? super String> objectMatcher, Set<String> unicity) {
        final CollectionMap<String, Tuple2<String, Boolean>> rightsForUser = getRightsForUser(userID);
        // super-user
        if (rightsForUser == SUPERUSER_RIGHTS)
            return true;
        if (rightsForUser == NO_RIGHTS)
            return false;

        if (rightsForUser.containsKey(code)) {
            for (final Tuple2<String, Boolean> t : rightsForUser.getNonNull(code)) {
                // as explained in expand() we need unicity for null object, we have it for each
                // user, but we also need it between userID and undefinedID
                if (unicity.add(t.get0())) {
                    // if the object of the right matches the requested object :
                    // null for the right matches any requested object
                    // null for the requested object means searching for all objects so we can't let
                    // objectMatcher match and thus ignore subsequent objects
                    if (t.get0() == null || (object != null && safeEquals(objectMatcher, t, object)))
                        return t.get1();
                    // but null for the requested object means that all right objects must be true
                    else if (object == null && !t.get1())
                        return false;
                }
            }
        }
        return null;
    }

    private boolean safeEquals(final Equalizer<? super String> objectMatcher, final Tuple2<String, Boolean> t, final String requestedObject) {
        final String rightObject = t.get0();
        try {
            return objectMatcher.equals(rightObject, requestedObject);
        } catch (Exception e) {
            // if the right could be allowed we don't match (so the row is ignored)
            // if the right could be disallowed we match
            final boolean res = !t.get1();
            final String desc = !res ? "Row ignored." : "Right denied.";
            Log.get().warning("Couldn't compare " + rightObject + " and " + requestedObject + ". " + desc);
            e.printStackTrace();
            return res;
        }
    }

    // if the db change, clear our cache, that way the next method call will query the db again
    private final void rightsInvalid() {
        synchronized (this.rights) {
            // MAYBE find out diff, and fire some events
            // so that eg IListe add/rm appropriate buttons
            this.rights.clear();
        }
    }

    private CollectionMap<String, Tuple2<String, Boolean>> getRightsForUser(final int userID) {
        synchronized (this.rights) {
            if (this.rights.containsKey(userID))
                return this.rights.get(userID);
            else {
                final CollectionMap<String, Tuple2<String, Boolean>> rightsForUser = loadRightsForUser(userID);
                this.rights.put(userID, rightsForUser);
                return rightsForUser;
            }
        }
    }

    /**
     * Charge les droits définit dans la table USER_RIGHT.
     * 
     * @param userID which user.
     * @return the user's rights by CODE.
     */
    private final CollectionMap<String, Tuple2<String, Boolean>> loadRightsForUser(final int userID) {
        try {
            final SQLRow userRow = this.getTable().getForeignTable("ID_USER_COMMON").getRow(userID);
            if (userRow != null && userRow.getBoolean(SUPERUSER_FIELD))
                return SUPERUSER_RIGHTS;

            final CollectionMap<String, Tuple2<String, Boolean>> res = new CollectionMap<String, Tuple2<String, Boolean>>(ArrayList.class);
            final Set<Tuple2<String, String>> unicity = new HashSet<Tuple2<String, String>>();
            // only superuser can modify RIGHTs
            expand(res, unicity, TableAllRights.createRight(TableAllRights.CODE_MODIF, this.getTable().getForeignTable("ID_RIGHT"), false));
            // only admin can modify or see USER_RIGHTs
            final boolean isAdmin = userRow != null && userRow.getBoolean(ADMIN_FIELD);
            expand(res, unicity, TableAllRights.createRight(TableAllRights.CODE, this.getTable(), isAdmin));

            // java rights have priority over SQL rights
            for (final RightTuple t : this.javaRights.getNonNull(userID)) {
                expand(res, unicity, t);
            }
            // perhaps allow SQL to also specify admin rights
            if (isAdmin) {
                for (final RightTuple t : this.javaRights.getNonNull(ADMIN_ID))
                    expand(res, unicity, t);
            }
            final int defaultUser = getDefaultUserId();
            if (defaultUser != userID) {
                // even default java rights are before user SQL rights
                for (final RightTuple t : this.javaRights.getNonNull(defaultUser)) {
                    expand(res, unicity, t);
                }
            }

            final SQLRowValues vals = new SQLRowValues(getTable()).setAllToNull();
            vals.putRowValues("ID_RIGHT").setAllToNull();

            final SQLRowValuesListFetcher sel = new SQLRowValuesListFetcher(vals);
            sel.setOrdered(true);
            sel.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
                @Override
                public SQLSelect transformChecked(final SQLSelect sel) {
                    sel.setWhere(new Where(getTable().getField("ID_USER_COMMON"), "=", userID));
                    return sel;
                }
            });

            final List<SQLRowValues> list = sel.fetch();
            for (final SQLRowValues row : list) {
                final SQLRowAccessor right = row.getForeign("ID_RIGHT");
                if (row.isUndefined()) {
                    Log.get().warning(row.asRow() + " has undef right");
                } else {
                    final String rightCode = right.getString("CODE");
                    // do *not* load null code has it means SUPERUSER
                    if (rightCode == null)
                        Log.get().warning(right + " has null CODE");
                    else {
                        final String object = row.getString("OBJECT");
                        final Boolean haveRight = row.getBoolean("HAVE_RIGHT");
                        expand(res, unicity, rightCode, object, haveRight);
                    }
                }
            }

            return res;
        } catch (Exception e) {
            ExceptionHandler.handle("Erreur lors du chargement des droits utilisateurs pour l'utilisateur (Id:" + userID + ")", e);
            return NO_RIGHTS;
        }
    }

    private final void expand(final CollectionMap<String, Tuple2<String, Boolean>> res, final Set<Tuple2<String, String>> unicity, final RightTuple t) {
        this.expand(res, unicity, t.get0(), t.get1(), t.get2());
    }

    private final void expand(final CollectionMap<String, Tuple2<String, Boolean>> res, final Set<Tuple2<String, String>> unicity, final String rightCode, final String object, final Boolean haveRight) {
        if (haveRight == null)
            throw new IllegalStateException("HAVE_RIGHT cannot be null");

        if (this.macroRights.containsKey(rightCode)) {
            for (final RightTuple t : this.macroRights.get(rightCode).expand(this, rightCode, object, haveRight)) {
                expand(res, unicity, t);
            }
        } else if (unicity.add(Tuple2.create(rightCode, object))) {
            // we need to have unique rights, otherwise simple queries will still work since they
            // will stop at the first match. But for queries with null object we need to traverse
            // all rights.
            res.put(rightCode, Tuple2.create(object, haveRight));
        }
    }

    /**
     * Return the list of objects the passed user is allowed for the passed code.
     * 
     * @param userID the user.
     * @param code the requested right.
     * @param allObjects depending on the rights it might be necessary to know the full list of
     *        possible values.
     * @return the allowed objects or <code>null</code> if they're all allowed.
     */
    public final Set<String> getObjects(final int userID, final String code, final IFactory<Set<String>> allObjects) {
        // test for everything to avoid calling allObjects
        // (also takes care of superuser)
        if (this.haveRight(userID, code))
            return null;

        // the above line handles "* true", MAYBE we should search for e.g. "A false, * false"
        // and then return {}.

        // try to add all objects which we are allowed to
        // but stop at the first null since it means we have to do a subtraction.
        // (e.g. A f, * t)
        final Set<String> unicity = new HashSet<String>();
        final Set<String> userRight = getObjectsP(userID, code, unicity);
        if (userRight != null) {
            final Set<String> defaultRight = getObjectsP(getDefaultUserId(), code, unicity);
            if (defaultRight != null) {
                userRight.addAll(defaultRight);
                return userRight;
            }
        }
        // there was at least one null
        final Set<String> res = new HashSet<String>();
        for (final String object : allObjects.createChecked()) {
            if (this.haveRight(userID, code, object))
                res.add(object);
        }
        return res;
    }

    private int getDefaultUserId() {
        return this.getTable().getForeignTable("ID_USER_COMMON").getUndefinedID();
    }

    public void preloadRightsForUserId(int userID) {
        getRightsForUser(getDefaultUserId());
        getRightsForUser(userID);
    }

    private final Set<String> getObjectsP(final int userID, final String code, Set<String> unicity) {
        final CollectionMap<String, Tuple2<String, Boolean>> rightsForUser = getRightsForUser(userID);
        // don't let it proceed, otherwise it will then load objects for undef
        if (rightsForUser == NO_RIGHTS)
            return null;
        final Set<String> res = new HashSet<String>();
        if (rightsForUser.containsKey(code)) {
            for (final Tuple2<String, Boolean> t : rightsForUser.getNonNull(code)) {
                // as usual don't let following rights overwrite preceding ones (ie if userID has
                // "A false" and undef has "A true", then the second one should be ignored)
                if (unicity.add(t.get0())) {
                    if (t.get0() == null)
                        return null;
                    else if (t.get1())
                        res.add(t.get0());
                }
            }
        }
        return res;
    }

    public final Set<String> getObjects(final int userID, final String code, final Set<String> objectsToTest, final Equalizer<? super String> objectMatcher) {
        // test for everything to avoid looping through potentially numerous allObjects
        // (also takes care of superuser)
        if (this.haveRight(userID, code, null, objectMatcher))
            return objectsToTest;

        // if userID hasn't the right to any object we can't try to list objects since in this case
        // (with an Equalizer) the objects in the DB are more like patterns.
        final Set<String> res = new HashSet<String>();
        for (final String object : objectsToTest) {
            if (this.haveRight(userID, code, object, objectMatcher))
                res.add(object);
        }
        return res;
    }

    static public final class RightTuple extends Tuple3<String, String, Boolean> {
        public RightTuple(final String code, final boolean haveRight) {
            this(code, null, haveRight);
        }

        public RightTuple(final String code, final String object, final boolean haveRight) {
            super(code, object, haveRight);
        }
    }
}
