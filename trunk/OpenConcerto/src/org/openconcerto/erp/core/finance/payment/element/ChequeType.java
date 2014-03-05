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
 
 package org.openconcerto.erp.core.finance.payment.element;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;

import java.util.Date;
import java.util.List;

public interface ChequeType {
    public String getDoneFieldName();

    public String getDateFieldName();

    public String getMinDateFieldName();

    public SQLTableModelSourceOnline createDepositTableSource();

    public void print(final List<Integer> rows, final boolean preview, final Date d);

    public void handle(final SQLRowAccessor rowCheque, final Date d, String label) throws Exception;
}
