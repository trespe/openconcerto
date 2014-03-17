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
 
 package org.openconcerto.erp.core.sales.order.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.SQLComponent;

import java.util.ArrayList;
import java.util.List;

public class TransferCustomerOrderSQLElement extends ComptaSQLConfElement {

    public TransferCustomerOrderSQLElement() {
        super("TR_COMMANDE_CLIENT", "un transfert de commande", "transferts de commande");
    }

    @Override
    protected void ffInited() {
        setAction("ID_COMMANDE", ReferenceAction.CASCADE);
        setAction("ID_COMMANDE_CLIENT", ReferenceAction.CASCADE);
        setAction("ID_BON_DE_LIVRAISON", ReferenceAction.CASCADE);
        setAction("ID_SAISIE_VENTE_FACTURE", ReferenceAction.CASCADE);
    }

    @Override
    protected List<String> getListFields() {
        return new ArrayList<String>(0);
    }

    @Override
    protected SQLComponent createComponent() {
        return null;
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".transfer";
    }
}
