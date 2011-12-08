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
 
 package org.openconcerto.erp.core.sales.pos.io;

import jpos.CashDrawer;
import jpos.CashDrawerControl19;
import jpos.POSPrinter;
import jpos.POSPrinterConst;
import jpos.POSPrinterControl19;

public class JPOSTicketPrinter extends DefaultTicketPrinter {
    private String name;

    public JPOSTicketPrinter(String name) {
        this.name = name;
    }

    @Override
    public void printBuffer() throws Exception {

        final POSPrinterControl19 ptr = (POSPrinterControl19) new POSPrinter();

        // Init
        ptr.open(name);
        ptr.claim(1000);
        ptr.setDeviceEnabled(true);
        // Verification de l'imprimante
        ptr.checkHealth(1); // UPOSConst.UPOS_CH_INTERNAL

        final int stop = this.strings.size();
        for (int i = 0; i < stop; i++) {
            String string = this.strings.get(i);
            int mode = modes.get(i);
            if (mode == BARCODE) {
                // Code 128 pour ne pas depasser la largeur en 58mm
                ptr.printBarCode(POSPrinterConst.PTR_S_RECEIPT, string, POSPrinterConst.PTR_BCS_Code128, 60, ptr.getRecLineWidth(), POSPrinterConst.PTR_BC_CENTER, POSPrinterConst.PTR_BC_TEXT_BELOW);
            } else {
                if (mode == NORMAL) {
                    ptr.printNormal(POSPrinterConst.PTR_S_RECEIPT, string + "\n");
                } else if (mode == BOLD) {
                    ptr.printNormal(POSPrinterConst.PTR_S_RECEIPT, "\u001b|bC" + string + "\n");
                } else if (mode == BOLD_LARGE) {
                    ptr.printNormal(POSPrinterConst.PTR_S_RECEIPT, "\u001b|2C" + string + "\n");
                }
            }
        }
        // Ejection
        ptr.printNormal(POSPrinterConst.PTR_S_RECEIPT, "\n\n\n\n \n\n\n\n");
        // Coupe 100% de la largeur
        ptr.cutPaper(100);

        // Bye
        ptr.setDeviceEnabled(false);
        ptr.release();
        ptr.close();
    }

    public static void main(String[] args) {
        JPOSTicketPrinter p = new JPOSTicketPrinter("POSPrinter");
        p.addToBuffer("Hello JPOS");
        p.addToBuffer("Texte gras", JPOSTicketPrinter.BOLD);
        p.addToBuffer("Text gras & large", JPOSTicketPrinter.BOLD_LARGE);
        p.addToBuffer("123456789", JPOSTicketPrinter.BARCODE);
        try {
            p.printBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void openDrawer() throws Exception {
        final CashDrawerControl19 draw = (CashDrawerControl19) new CashDrawer();
        draw.open("CashDrawer");
        draw.claim(1000);
        draw.setDeviceEnabled(true);
        draw.openDrawer();
        draw.setDeviceEnabled(false);
        draw.release();
        draw.close();
    }
}
