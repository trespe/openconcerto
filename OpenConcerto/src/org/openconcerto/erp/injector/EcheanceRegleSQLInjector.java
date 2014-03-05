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
 
 package org.openconcerto.erp.injector;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLTable;

public class EcheanceRegleSQLInjector extends SQLInjector {
    public EcheanceRegleSQLInjector(final DBRoot root) {
        super(root, "ECHEANCE_FOURNISSEUR", "REGLER_MONTANT_ELEMENT", false);
        final SQLTable tableEch = getSource();
        final SQLTable tableEnc = getDestination();
        map(tableEch.getField("ID"), tableEnc.getField("ID_ECHEANCE_FOURNISSEUR"));
        map(tableEch.getField("ID_MOUVEMENT"), tableEnc.getField("ID_MOUVEMENT_ECHEANCE"));
        map(tableEch.getField("DATE"), tableEnc.getField("DATE"));
        map(tableEch.getField("MONTANT"), tableEnc.getField("MONTANT_A_REGLER"));
        mapDefaultValues(tableEnc.getField("MONTANT_REGLE"), 0);
    }
}
