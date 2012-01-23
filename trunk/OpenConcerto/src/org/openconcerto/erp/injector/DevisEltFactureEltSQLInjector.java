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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLTable;

import java.math.BigDecimal;

public class DevisEltFactureEltSQLInjector extends SQLInjector {
    public DevisEltFactureEltSQLInjector(final DBRoot root) {
        super(root, "DEVIS_ELEMENT", "SAISIE_VENTE_FACTURE_ELEMENT");
        createDefaultMap();

        final SQLTable tableFacture = getDestination();
        mapDefaultValues(tableFacture.getField("POURCENT_ACOMPTE"), new BigDecimal(100.0D));
        mapDefaultValues(tableFacture.getField("MONTANT_INITIAL"), Long.valueOf(0));
        mapDefaultValues(tableFacture.getField("INDICE_0"), Long.valueOf(0));
        mapDefaultValues(tableFacture.getField("INDICE_N"), Long.valueOf(0));
        mapDefaultValues(tableFacture.getField("TARIF_Q18_HT"), Long.valueOf(0));
        mapDefaultValues(tableFacture.getField("Q18"), Boolean.FALSE);
        mapDefaultValues(tableFacture.getField("MONTANT_REVISABLE"), Boolean.FALSE);
    }
}
