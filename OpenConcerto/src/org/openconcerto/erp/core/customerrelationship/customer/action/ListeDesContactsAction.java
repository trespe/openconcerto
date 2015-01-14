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
 
 package org.openconcerto.erp.core.customerrelationship.customer.action;

import org.openconcerto.erp.action.CreateListFrameAbstractAction;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.task.config.ComptaBasePropsConfiguration;

import javax.swing.Action;
import javax.swing.JFrame;

public class ListeDesContactsAction extends CreateListFrameAbstractAction {

    public ListeDesContactsAction() {
        super();
        this.putValue(Action.NAME, "Liste des contacts");
    }

    public JFrame createFrame() {
        final ComptaBasePropsConfiguration conf = ((ComptaBasePropsConfiguration) Configuration.getInstance());
        return new IListFrame(new ListeAddPanel(conf.getDirectory().getElement("CONTACT")));
    }

    @Override
    public String getTableName() {
        return "CONTACT";
    }
}
