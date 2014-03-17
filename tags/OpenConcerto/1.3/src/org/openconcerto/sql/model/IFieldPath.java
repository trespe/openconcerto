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
 
 package org.openconcerto.sql.model;

import org.openconcerto.sql.model.graph.Path;

/**
 * A path plus a name designating a field of the last table of the path, eg
 * [SITE,SITE.ID_CONTACT_CHEF]+"EMAIL".
 * 
 * @author Sylvain
 */
public interface IFieldPath {

    public Path getPath();

    /**
     * The last table of the path, ie the table of the field.
     * 
     * @return the last table.
     */
    public SQLTable getTable();

    public String getFieldName();

    /**
     * The field.
     * 
     * @return the field.
     */
    public SQLField getField();

    /**
     * Return an instance of {@link FieldPath} for more methods.
     * 
     * @return the corresponding FieldPath.
     */
    public FieldPath getFieldPath();

}
