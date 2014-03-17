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
 
 package org.openconcerto.sql.sqlobject.itemview;

import org.openconcerto.sql.request.SQLRowItemView;

/**
 * If the component of a BaseRowItemView implement that, it will be passed its intialized RIV. Eg if
 * your ui needs to display a list of completion from the db.
 * 
 * @author Sylvain CUAZ
 * @see BaseRowItemView#getComp()
 */
public interface RowItemViewComponent {

    void init(SQLRowItemView v);

}
