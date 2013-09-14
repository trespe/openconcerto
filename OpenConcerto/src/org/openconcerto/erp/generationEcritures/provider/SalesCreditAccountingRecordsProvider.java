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
 
 package org.openconcerto.erp.generationEcritures.provider;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.generationEcritures.GenerationMvtAvoirClient;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;

import java.util.Map;

public class SalesCreditAccountingRecordsProvider implements AccountingRecordsProvider {

    @Override
    public void putPieceLabel(SQLRowAccessor rowSource, SQLRowValues rowValsPiece) {
            rowValsPiece.put("NOM", rowSource.getString("NUMERO"));
    }

    @Override
    public void putLabel(SQLRowAccessor rowSource, Map<String, Object> values) {

            String nom = "Avoir client " + rowSource.getObject("NUMERO").toString();
            values.put("NOM", nom);
    }

    public static void register() {
        AccountingRecordsProviderManager.put(GenerationMvtAvoirClient.ID, new SalesCreditAccountingRecordsProvider());
    }

}
