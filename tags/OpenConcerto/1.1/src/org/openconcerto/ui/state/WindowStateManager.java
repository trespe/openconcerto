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
 
 package org.openconcerto.ui.state;

import org.openconcerto.ui.ScreenUtils;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Always save state.
 * 
 * @author Sylvain CUAZ
 */
public class WindowStateManager extends ListenerXMLStateManager<Window, WindowListener> {

    public WindowStateManager(Window w, File f) {
        this(w, f, true);
    }

    public WindowStateManager(Window w, File f, boolean autosave) {
        super(w, f, autosave);
    }

    @Override
    protected void addListener(WindowListener l) {
        this.getSrc().addWindowListener(l);
    }

    @Override
    protected WindowListener createListener() {
        return new WindowAdapter() {
            // don't save twice for the same event
            private WindowEvent evt = null;

            public void windowClosed(WindowEvent e) {
                if (e != this.evt)
                    save();
            }

            public void windowClosing(WindowEvent e) {
                save();
                this.evt = e;
            }
        };
    }

    @Override
    protected void rmListener(WindowListener l) {
        this.getSrc().removeWindowListener(l);
    }

    protected final void save() {
        try {
            saveState();
        } catch (IOException exn) {
            ExceptionHandler.handle(this.getSrc().isDisplayable() ? this.getSrc() : null, "Impossible de sauvegarder la position de la fenêtre.", exn);
        }
    }

    @Override
    protected void writeState(PrintStream out) throws IOException {
        out.println("<window>");
        // Taille
        out.print("<size");
        out.print(" width=\"" + this.getSrc().getSize().width + "\"");
        out.print(" height=\"" + this.getSrc().getSize().height + "\"");
        out.println("/>");
        // Position
        out.print("<location");
        out.print(" x=\"" + this.getSrc().getLocation().x + "\"");
        out.print(" y=\"" + this.getSrc().getLocation().y + "\"");
        out.println("/>");

        out.println("</window>");
    }

    @Override
    protected boolean readState(Document doc) {
        final Rectangle desiredBounds = this.getBounds(doc);
        // make sure the window is visible
        final Rectangle virtualBounds = ScreenUtils.getDisplayBounds();
        if (!virtualBounds.intersects(desiredBounds)) {
            desiredBounds.setLocation(0, 0);
            final Rectangle maxWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            desiredBounds.setBounds(desiredBounds.intersection(maxWindowBounds));
        }

        this.getSrc().setBounds(desiredBounds);
        return true;
    }

    private Rectangle getBounds(Document doc) {
        Node size = doc.getElementsByTagName("size").item(0);
        // largeur
        final String sWidth = (size.getAttributes().getNamedItem("width").getNodeValue());
        final int width = Math.max(Integer.parseInt(sWidth), 50);
        // hauteur
        final String sHeight = (size.getAttributes().getNamedItem("height").getNodeValue());
        final int height = Math.max(Integer.parseInt(sHeight), 50);

        final Node location = doc.getElementsByTagName("location").item(0);
        final String sX = (location.getAttributes().getNamedItem("x").getNodeValue());
        final String sY = (location.getAttributes().getNamedItem("y").getNodeValue());

        return new Rectangle(Integer.parseInt(sX), Integer.parseInt(sY), width, height);
    }
}
