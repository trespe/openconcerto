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
 
 package org.openconcerto.erp.core.sales.invoice.action;

import org.openconcerto.erp.core.sales.invoice.report.ListeDebiteursXmlSheet;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

public class ListeDebiteursAction extends AbstractAction {
    public ListeDebiteursAction() {
        super();
        this.putValue(Action.NAME, "Liste des débiteurs");
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        ListeDebiteursXmlSheet sheet = new ListeDebiteursXmlSheet();
        sheet.genere(true, false);
    }
}
