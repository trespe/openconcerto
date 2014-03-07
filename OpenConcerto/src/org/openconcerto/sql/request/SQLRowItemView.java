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
 
 package org.openconcerto.sql.request;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.utils.checks.EmptyObj;
import org.openconcerto.utils.checks.ValidObject;

import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * An element of a SQLRowView, it can be either a field (DESIGNATION) or not.
 * 
 * @author Sylvain CUAZ
 */
public interface SQLRowItemView extends EmptyObj, ValidObject {

    // eg DESIGNATION, OBSERVATIONS
    public String getSQLName();

    // null if no fields, Exception if more than one
    public SQLField getField();

    public List<SQLField> getFields();

    // TODO rename en reset()
    public void resetValue();

    public void show(SQLRowAccessor r);

    public void insert(SQLRowValues vals);

    public void update(SQLRowValues vals);

    public void setEditable(boolean b);

    public Component getComp();

    // TODO rename en addChangeListener, un RIV n'a pas de valeur à proprement parler
    public void addValueListener(PropertyChangeListener l);

}
