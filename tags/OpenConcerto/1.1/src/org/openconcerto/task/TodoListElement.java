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

import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TodoListElement {

    SQLRowValues rowVals;

    public TodoListElement(SQLRowValues rowVals) {
        this.rowVals = rowVals;
    }

    Executor executor = Executors.newSingleThreadExecutor();

    public void reloadValues(final SQLRowAccessor row) {
        executor.execute(new Runnable() {
            public void run() {
                rowVals.load(row, null);
            }
        });
    }

    /**
     * @return Returns the date.
     */
    public Date getDate() {

        final Calendar date = this.rowVals.getDate("DATE_ENTREE");
        if (date == null) {
            return null;
        } else {
            return date.getTime();
        }
    }

    /**
     * @param date The date to set.
     */
    public void setDate(Date date) {
        this.rowVals.put("DATE_ENTREE", date);
    }

    /**
     * @return Returns the done.
     */
    public Boolean isDone() {
        return this.rowVals.getBoolean("FAIT");
    }

    /**
     * @param done The done to set.
     */
    public void setDone(Boolean done) {
        this.rowVals.put("FAIT", done);
        setDoneDate(new Date());
    }

    /**
     * @return Returns the doneDate.
     */
    public Date getDoneDate() {
        final Calendar date = this.rowVals.getDate("DATE_FAIT");
        if (date == null) {
            return null;
        } else {
            return date.getTime();
        }
    }

    /**
     * @param doneDate The doneDate to set.
     */
    public void setDoneDate(Date doneDate) {
        this.rowVals.put("DATE_FAIT", doneDate);
    }

    /**
     * @return Returns the expectedDate.
     */
    public Date getExpectedDate() {
        final Calendar date = this.rowVals.getDate("DATE_EXP");
        if (date == null) {
            return null;
        } else {
            return date.getTime();
        }
    }

    /**
     * @param expectedDate The expectedDate to set.
     */
    public void setExpectedDate(Date expectedDate) {
        this.rowVals.put("DATE_EXP", expectedDate);
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return this.rowVals.getString("NOM");
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.rowVals.put("NOM", name);
    }

    /**
     * @return Returns the priority.
     */
    public Integer getPriority() {
        return this.rowVals.getInt("PRIORITE");
    }

    /**
     * @param priority The priority to set.
     */
    public void setPriority(Integer priority) {
        this.rowVals.put("PRIORITE", priority);
    }

    /**
     * Returns the user who must execute the task.
     * 
     * @return the user id.
     */
    public Integer getUserId() {
        return this.rowVals.getInt("ID_USER_COMMON_TO");
    }

    /**
     * Returns the user who created the task.
     * 
     * @return the user id.
     */
    public Integer getCreatorId() {
        return this.rowVals.getInt("ID_USER_COMMON_ASSIGN_BY");
    }

    /**
     * Set the user who must execute the task.
     * 
     * @param id the user id.
     */
    public void setUserId(Integer id) {
        this.rowVals.put("ID_USER_COMMON_TO", id);
    }

    public void commitChanges() {
        executor.execute(new Runnable() {

            public void run() {
                commitChangesAndWait();
            }
        });

    }

    public void commitChangesAndWait() {

        try {
            SQLRow row = this.rowVals.commit();
            this.rowVals.put(this.rowVals.getTable().getKey().getName(), row.getID());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void archive() {
        this.rowVals.put("ARCHIVE", Integer.valueOf(1));
        try {
            this.rowVals.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String toString() {
        return "Tâche:" + this.rowVals.getString("NOM") + "(id:" + this.rowVals.getID() + ") to " + this.getUserId();
    }

    public SQLRowValues getRowValues() {
        return this.rowVals;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TodoListElement) {
            TodoListElement element = (TodoListElement) obj;
            return this.rowVals.equals(element.getRowValues());
        }
        return super.equals(obj);
    }

    public String getComment() {
        return this.rowVals.getString("COMMENT");
    }

    public void setComment(String text) {
        this.rowVals.put("COMMENT", text);

    }
}
