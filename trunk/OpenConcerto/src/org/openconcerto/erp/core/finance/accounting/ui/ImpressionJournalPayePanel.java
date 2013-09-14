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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import org.openconcerto.erp.generationDoc.gestcomm.JournalPaieXmlSheet;

import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutionException;

public class ImpressionJournalPayePanel extends ImpressionPayePanel {

    public ImpressionJournalPayePanel() {
        super();
    }

    public void actionPerformed(ActionEvent e) {
        final JournalPaieXmlSheet bSheet = new JournalPaieXmlSheet(selMoisDeb.getSelectedId(), selMoisEnd.getSelectedId(), Integer.valueOf(textAnnee.getText()));
        try {
            bSheet.createDocument();
            bSheet.showPrintAndExport(true, false, false);
        } catch (InterruptedException exn) {
            // TODO Bloc catch auto-généré
            exn.printStackTrace();
        } catch (ExecutionException exn) {
            // TODO Bloc catch auto-généré
            exn.printStackTrace();
        }
    }

}
