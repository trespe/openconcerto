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
 
 package org.openconcerto.erp.core.common.ui;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.io.File;

import javax.swing.JFrame;

import org.jopendocument.model.OpenDocument;
import org.jopendocument.panel.ODSViewerPanel;
import org.jopendocument.print.DefaultXMLDocumentPrinter;

public class PreviewFrame extends JFrame {

    private PreviewFrame(File file) {
        super();
        final OpenDocument doc = new OpenDocument(file);
        this.setContentPane(new ODSViewerPanel(doc, new DefaultXMLDocumentPrinter()));
        this.setTitle(file.getName());
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        this.setMaximizedBounds(ge.getMaximumWindowBounds());
        Dimension maxD = ge.getMaximumWindowBounds().getSize();
        this.setMaximumSize(maxD);
        this.pack();
        Dimension d = this.getSize();
        if (d.width > maxD.width) {
            d.setSize(maxD.width, d.height);
        }
        if (d.height > maxD.height) {
            d.setSize(d.width, maxD.height);
        }
        this.setSize(d);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        this.setLocationRelativeTo(null);
    }

    public static void show(File f) {
        new PreviewFrame(f).setVisible(true);
    }

}
