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
 
 package org.openconcerto.erp.core.sales.order.ui;

import org.openconcerto.erp.core.common.ui.AbstractVenteArticleItemTable;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;

public class CommandeClientItemTable extends AbstractVenteArticleItemTable {

    @Override
    protected String getConfigurationFileName() {
        return "Table_Commande_client.xml";

    }

    @Override
    public SQLElement getSQLElement() {
        return Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT_ELEMENT");

    }
}
