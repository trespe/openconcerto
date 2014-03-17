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
 
 package org.openconcerto.erp.importer;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.utils.GestionDevise;

public class CentsValueConverter extends ValueConverter {
    public CentsValueConverter(SQLField f) {
        super(f);
    }

    public Object convertFrom(Object obj) {
        long result = 0;
        if (obj != null) {
            try {
                return GestionDevise.parseLongCurrency(obj.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
