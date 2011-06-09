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
 
 package org.openconcerto.erp.generationDoc;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.print.PrintService;

import org.jopendocument.model.OpenDocument;
import org.jopendocument.print.ODTPrinterXML;
import org.jopendocument.renderer.ODTRenderer;

public class ODTPrinterNX extends ODTPrinterXML {
    protected ODTRenderer renderer;

    public ODTPrinterNX(final OpenDocument doc) {
        super(doc);
        this.renderer = new ODTRenderer(doc);
        this.renderer.setPaintMaxResolution(true);
    }

    public void print(final String printerName, final int copies) {
        final PrinterJob printJob = PrinterJob.getPrinterJob();
        printJob.setPrintable(this);

        // Set the printer
        PrintService myService = null;
        if (printerName != null && printerName.trim().length() != 0) {
            final PrintService[] services = PrinterJob.lookupPrintServices();

            for (int i = 0; i < services.length; i++) {

                if (services[i].getName().equals(printerName)) {

                    myService = services[i];
                    break;

                }

            }
            if (myService != null) {
                try {
                    printJob.setPrintService(myService);
                } catch (PrinterException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        Thread t;
        if (myService == null) {
            t = new Thread(new Runnable() {
                public void run() {
                    if (printJob.printDialog()) {
                        try {
                            printJob.print();
                        } catch (PrinterException e) {
                            e.printStackTrace();
                        }
                    }

                }
            });
        } else {
            t = new Thread(new Runnable() {
                public void run() {
                    try {
                        printJob.print();
                    } catch (PrinterException e) {
                        e.printStackTrace();
                    }
                }
            });

        }
        t.setName("ODTDPrinter Thread");
        t.setDaemon(true);
        t.start();

    }
}
