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
 
 package org.openconcerto.task;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.IResultSetHandler;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.User;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.task.config.ComptaBasePropsConfiguration;
import org.openconcerto.utils.cc.IFactory;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

public class TodoListModel extends DefaultTableModel {

    public static final int EXTENDED_MODE = 1;
    public static final int SIMPLE_MODE = 2;
    public int mode = SIMPLE_MODE;
    private static final int MIN_DELAY = 30;// * 1000; // en secondes
    private static final int MAX_DELAY = 6;// 120 * 1000;
    private long currentDelay = MIN_DELAY;
    private boolean stop = false;
    private final List<Integer> listIdListener = new Vector<Integer>(); // Contient des Integer, id
    // que l'on ecoute
    private JTable table = null;
    private List<ModelStateListener> stateListenerList = new Vector<ModelStateListener>(1);
    private transient User currentUser;
    protected List<UserTaskRight> rights;
    private boolean historyVisible = false;

    TodoListModel(User currentUser) {
        this.currentUser = currentUser;
        launchUpdaterThread();
        this.mode = SIMPLE_MODE;
    }

    private void launchUpdaterThread() {
        final Thread thread = new Thread(new Runnable() {

            public void run() {
                // Remplissage périodique
                while (!TodoListModel.this.stop) {
                    try {
                        Thread.sleep(TodoListModel.this.currentDelay * 1000);
                        if (!TodoListModel.this.stop) {
                            synchronousFill();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                // System.err.println("launchUpdaterThread end");

            }
        });
        // this only read data to be displayed, it can be safely interrupted at any moment
        thread.setDaemon(true);
        thread.setName("TodoListModel UpdaterThread");
        thread.start();
    }

    public void asynchronousFill() {

        final Thread thread = new Thread(new Runnable() {

            public void run() {
                // System.err.println("asynchronousFill.run()");
                synchronousFill();
                // System.err.println("asynchronousFill.run() done");
            }

        });
        thread.setName("TodoListModel asynchronousFill");
        thread.start();

    }

    /**
     * 
     */
    private synchronized void synchronousFill() {
        if (this.table == null)
            return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireModelStateChanged(ModelStateListener.STATE_RELOADING);
            }
        });
        // System.out.println("TodoListModel.synchronousFill()" + new
        // Timestamp(System.currentTimeMillis()));
        final Map<Integer, TodoListElement> newDataVector = new LinkedHashMap<Integer, TodoListElement>();
        try {
            fillFromDatabase(newDataVector);

        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    fireModelStateChanged(ModelStateListener.STATE_DEAD);
                }
            });
            return;
        }

        final Vector<Integer> rowsModified = new Vector<Integer>();
        final Vector<TodoListElement> rowsDeleted = new Vector<TodoListElement>();
        // size before removing
        final int newSize = newDataVector.size();
        final int oldSize;
        synchronized (this.dataVector) {
            oldSize = this.dataVector.size();
            for (int i = 0; i < this.dataVector.size(); i++) {
                TodoListElement elt = (TodoListElement) this.dataVector.get(i);
                TodoListElement eltN = newDataVector.remove(elt.getRowValues().getID());
                if (eltN == null) {
                    rowsDeleted.add(elt);
                } else {
                    if (!eltN.equals(elt)) {
                        rowsModified.add(i);
                        elt.reloadValues(eltN.getRowValues());
                    }
                }
            }

            for (TodoListElement elt : rowsDeleted) {
                int index = this.dataVector.indexOf(elt);
                if (index >= 0) {
                    removeRow(index);
                }
            }

            for (Integer i : newDataVector.keySet()) {
                this.dataVector.add(newDataVector.get(i));
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if ((rowsModified.size() == newSize) && (rowsModified.size() > 0)) {
                    fireTableDataChanged();
                    fireModelStateChanged(ModelStateListener.CONTENT_MODIFIED);
                } else if (newSize != oldSize) {
                    fireTableDataChanged();
                    fireModelStateChanged(ModelStateListener.CONTENT_MODIFIED);
                } else {
                    for (int i = 0; i < rowsModified.size(); i++) {
                        Integer indexModified = rowsModified.get(i);
                        fireTableRowsUpdated(indexModified, indexModified);
                    }
                    if (rowsModified.size() > 0) {
                        fireModelStateChanged(ModelStateListener.CONTENT_MODIFIED);
                    }
                }
                fireModelStateChanged(ModelStateListener.STATE_OK);
            }
        });

    }

    private final Where getAuthorizedTaskTypes(final int userID, final SQLTable tableTache) {
        final SQLField typeF = tableTache.getFieldRaw("TYPE");
        if (typeF == null)
            return null;

        final Set<String> types = UserRightsManager.getInstance().getObjects(userID, "TASK", new IFactory<Set<String>>() {
            @SuppressWarnings("unchecked")
            @Override
            public Set<String> createChecked() {
                final SQLSelect sel = new SQLSelect(tableTache.getBase());
                sel.addSelect(typeF);
                sel.addGroupBy(typeF);
                return new HashSet<String>(tableTache.getDBSystemRoot().getDataSource().executeCol(sel.asString()));
            }
        });
        return types == null ? Where.TRUE : new Where(typeF, types);
    }

    private static final int societeID = ((ComptaBasePropsConfiguration) Configuration.getInstance()).getSocieteID();
    private static final DBSystemRoot base = Configuration.getInstance().getSystemRoot();
    private static final SQLTable tableTache = base.getRoot("Common").getTable("TACHE_COMMON");
    private final static Where where2 = new Where(tableTache.getField("ID_SOCIETE_COMMON"), "=", tableTache.getUndefinedID()).or(new Where(tableTache.getField("ID_SOCIETE_COMMON"), "=", societeID));

    private synchronized void fillFromDatabase(final Map<Integer, TodoListElement> m) {
        long time1 = System.currentTimeMillis();

        final SQLSelect select = new SQLSelect(tableTache.getBase());
        select.addSelectStar(tableTache);
        Where where = new Where(tableTache.getField("ID_USER_COMMON_TO"), this.listIdListener);
        final int userID = UserManager.getInstance().getCurrentUser().getId();
        where = where.or(new Where(tableTache.getField("ID_USER_COMMON_ASSIGN_BY"), "=", userID));

        where = where.or(getAuthorizedTaskTypes(userID, tableTache));

        if (!isHistoryVisible()) {
            Where w3 = new Where(tableTache.getField("FAIT"), "=", Boolean.FALSE);
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            Where w4 = new Where(tableTache.getField("DATE_FAIT"), "<>", (Object) null);
            w4 = w4.and(new Where(tableTache.getField("DATE_FAIT"), ">", cal.getTime()));
            w3 = w3.or(w4);
            where = where.and(w3);
        }
        select.setWhere(where.and(where2));

        select.addFieldOrder(tableTache.getField("ID_USER_COMMON_TO"));
        select.addFieldOrder(tableTache.getField("DATE_EXP"));

        // System.out.println(select.asString());
        this.rights = UserTaskRight.getUserTaskRight(getCurrentUser());
        // don't use the cache since by definition this table is shared by everyone, so we can't
        // rely on our modifications
        final IResultSetHandler rsh = new IResultSetHandler(SQLRowListRSH.createFromSelect(select), false);
        @SuppressWarnings("unchecked")
        final List<SQLRow> l = (List<SQLRow>) base.getDataSource().execute(select.asString(), rsh);
        for (SQLRow row : l) {
            TodoListElement t = new TodoListElement(row.asRowValues());

            // Calendar t2 = row.getDate("DATE_FAIT");
            //

            // add tasks that we created, we must do, or that we can read
            // plus for preventec, tasks with a type must visible to everybody
            boolean add = false;
            String type = row.getString("TYPE");
            if (type != null && type.trim().length() > 0) {
                // if (isHistoryVisible()) {
                // m.put(row.getID(), t);
                // } else if (!row.getBoolean("FAIT")) {
                // m.put(row.getID(), t);
                // } else if (t2 != null && t2.after(cal.getTime())) {
                add = true;
                // }
            } else if (row.getInt("ID_USER_COMMON_CREATE") == userID || row.getInt("ID_USER_COMMON_TO") == userID) {
                add = true;
            } else {
                for (int i = 0; i < TodoListModel.this.rights.size(); i++) {
                    UserTaskRight element = TodoListModel.this.rights.get(i);

                    if (element.getIdToUser() == row.getInt("ID_USER_COMMON_TO") && element.canRead()) {
                        // if (isHistoryVisible()) {
                        // m.put(row.getID(), t);
                        // } else if (!row.getBoolean("FAIT")) {
                        // m.put(row.getID(), t);
                        // } else if (t2 != null && t2.after(cal.getTime())) {
                        add = true;
                        // }
                        break;
                    }
                }
            }
            if (add)
                m.put(row.getID(), t);
        }

        long time2 = System.currentTimeMillis();
        final long t = time2 - time1;
        // System.err.println("Time to fill from DB : " + t);
        long delay = 2 + t / 1000;
        if (delay > MAX_DELAY)
            delay = MAX_DELAY;
        if (delay < MIN_DELAY)
            delay = MIN_DELAY;
        this.currentDelay = delay;
    }

    public int getColumnCount() {
        if (this.mode == EXTENDED_MODE)
            return 7;
        return 5;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        synchronized (this.dataVector) {
            // Modifier la priorité de la tache
            if (columnIndex == 1) {
                return true;
            }
            if (this.mode == EXTENDED_MODE && columnIndex == 3) {
                // Impossible de modifier la date de creation
                return false;
            }

            if (this.dataVector.size() <= rowIndex) {
                System.err.println("pb! taille:" + this.dataVector.size() + " i:" + rowIndex);
                rowIndex = 0;
            }
            TodoListElement task = (TodoListElement) this.dataVector.get(rowIndex);
            if (task == null)
                return false;
            if (columnIndex == 0)
                // Validation
                for (int i = 0; i < this.rights.size(); i++) {
                    UserTaskRight right = this.rights.get(i);
                    if (right.getIdToUser() == task.getUserId() && right.canValidate()) {
                        return true;
                    }
                }
            else if (columnIndex == this.getColumnCount() - 1) {
                // Colonne d'assignement
                return (task.getCreatorId().equals(UserManager.getInstance().getCurrentUser().getId()));
            } else {
                // Modification
                for (int i = 0; i < this.rights.size(); i++) {
                    UserTaskRight right = this.rights.get(i);
                    // i.e. we can still modify tasks we created and assigned to another user, but
                    // we cannot change tasks assigned to us
                    if (right.getIdToUser() == task.getCreatorId() && right.canModify()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Class<?> getColumnClass(final int columnIndex) {
        switch (columnIndex) {
        case 0:
            return Boolean.class;
        case 1:
            return Integer.class;
        case 2:
            return String.class;
        case 3:
            return Timestamp.class;
        case 4:
            if (this.mode != EXTENDED_MODE) {
                return Integer.class;
            }
        case 5:
            return Timestamp.class;
        case 6:
            return Integer.class;
        default:
            return String.class;

        }

    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        synchronized (this.dataVector) {
            if (this.dataVector.size() <= rowIndex) {
                System.err.println("pb! taille:" + this.dataVector.size() + " i:" + rowIndex);
                rowIndex = 0;
            }
            TodoListElement task = (TodoListElement) this.dataVector.get(rowIndex);

            switch (columnIndex) {
            case 0:
                return task.isDone();
            case 1:
                return task.getPriority();
            case 2:
                return task.getName();
            case 3:
                if (this.mode == EXTENDED_MODE) {
                    return task.getDate();
                }
                return task.getExpectedDate();
            case 4:
                if (this.mode == EXTENDED_MODE) {
                    return task.getDoneDate();
                }
                return task.getUserId();
            case 5:
                return task.getExpectedDate();
            case 6:
                return task.getUserId();
            default:
                return "????????";

            }
        }
    }

    public TodoListElement getTaskAtRow(int rowIndex) {
        return (TodoListElement) this.dataVector.get(rowIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        synchronized (this.dataVector) {
            // System.out.println("TodoListModel.setValueAt():" + aValue + "(" + rowIndex + "," +
            // columnIndex + ")");
            if (rowIndex >= getRowCount()) {
                // Cas de la perte de l'edition de la derniere ligne supprimee
                return;
            }
            TodoListElement task = (TodoListElement) this.dataVector.get(rowIndex);

            switch (columnIndex) {
            case 0:
                task.setDone((Boolean) aValue);
                break;
            case 1:
                task.setPriority((Integer) aValue);
                break;
            case 2:
                task.setName((String) aValue);
                break;
            case 3:
                if (this.mode == EXTENDED_MODE) {
                    task.setDate((Timestamp) aValue);
                }

                task.setExpectedDate((Timestamp) aValue);
                break;
            case 4:
                if (this.mode == EXTENDED_MODE) {
                    task.setDoneDate((Timestamp) aValue);

                    break;
                }
                task.setUserId((Integer) aValue);
                break;
            case 5:
                task.setExpectedDate((Timestamp) aValue);
                break;
            case 6:
                task.setUserId((Integer) aValue);
                break;
            default:
                break;

            }

            task.commitChanges();
        }
        fireTableRowsUpdated(rowIndex, rowIndex);
    }

    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
        case 0:
            return "";
        case 1:
            return "";
        case 2:
            return "A faire...";
        case 3:
            if (this.mode == EXTENDED_MODE) {
                return "créé le";
            }
            return "à faire pour le";
        case 4:
            if (this.mode == EXTENDED_MODE) {
                return "fait le";
            }
            return "assignée à";
        case 5:
            return "à faire pour le";
        case 6:
            return "assignée à";
        default:
            return "Oups!!!!!!!!!";

        }
    }

    /**
     * Ajoute une nouvelle Tâche de manière asynchrone
     */
    public void addNewTask() {
        final SwingWorker<?, ?> worker = new SwingWorker<Object, Object>() {

            @Override
            public Object doInBackground() {
                SQLRowValues rowV = new SQLRowValues(Configuration.getInstance().getBase().getTable("TACHE_COMMON"));
                Calendar cal = Calendar.getInstance();

                rowV.put("DATE_ENTREE", new java.sql.Timestamp(cal.getTimeInMillis()));
                cal.add(Calendar.HOUR_OF_DAY, 1);
                rowV.put("DATE_EXP", new java.sql.Timestamp(cal.getTimeInMillis()));
                cal.set(Calendar.YEAR, 2000);

                cal.set(Calendar.DAY_OF_YEAR, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.MILLISECOND, 0);
                rowV.put("DATE_FAIT", new java.sql.Timestamp(cal.getTimeInMillis()));
                final int currentUserId = UserManager.getInstance().getCurrentUser().getId();
                rowV.put("ID_USER_COMMON_ASSIGN_BY", currentUserId);
                rowV.put("ID_USER_COMMON_TO", currentUserId);
                try {
                    rowV.insert();
                } catch (SQLException e) {
                    fireModelStateChanged(ModelStateListener.STATE_DEAD);
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public void done() {
                // synchrone pour que le fire fonctionne bien
                synchronousFill();
                fireTableRowsInserted(getRowCount(), getRowCount());
            }

        };
        worker.execute();

    }

    synchronized void setMode(int mode) {
        this.mode = mode;
        fireTableStructureChanged();
    }

    public synchronized int getMode() {
        return this.mode;
    }

    public boolean deleteTaskAtIndex(int index) {
        synchronized (this.dataVector) {

            // System.out.println("TodoListModel.deleteTaskAtIndex(" + index + ")");
            TodoListElement t = (TodoListElement) this.dataVector.get(index);
            // System.out.println("Effacement de " + t);
            final int currentUserId = UserManager.getInstance().getCurrentUser().getId();
            if (t.getCreatorId() != currentUserId) {
                JOptionPane.showMessageDialog(this.table, "Vous n'êtes pas autorisé à effacer\n des tâches dont vous n'êtes pas l'auteur!");
                return false;
            }
            t.archive();
        }
        removeRow(index);
        return true;
    }

    public void addIdListener(Integer id) {
        this.listIdListener.add(id);
        asynchronousFill();
    }

    public void addIdListenerSilently(Integer id) {
        this.listIdListener.add(id);
    }

    public void removeIdListener(Integer id) {
        this.listIdListener.remove(id);
        asynchronousFill();
    }

    public boolean listenToId(Integer id) {
        return this.listIdListener.contains(id);
    }

    public void setTable(JTable t) {
        this.table = t;
    }

    public void stopUpdate() {
        this.stop = true;
    }

    public void addModelStateListener(ModelStateListener l) {
        if (!this.stateListenerList.contains(l)) {
            this.stateListenerList.add(l);
        }
    }

    public void removeModelStateListener(ModelStateListener l) {
        if (this.stateListenerList.contains(l)) {
            this.stateListenerList.remove(l);
        }
    }

    private void fireModelStateChanged(int state) {
        for (int i = 0; i < this.stateListenerList.size(); i++) {
            this.stateListenerList.get(i).stateChanged(state);
        }
    }

    public User getCurrentUser() {
        return this.currentUser;
    }

    public synchronized boolean isHistoryVisible() {
        return this.historyVisible;
    }

    public synchronized void setHistoryVisible(boolean historyVisible) {
        this.historyVisible = historyVisible;
    }
}
