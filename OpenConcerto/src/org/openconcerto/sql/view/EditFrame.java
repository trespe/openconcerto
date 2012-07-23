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
 
 package org.openconcerto.sql.view;

import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.ui.state.WindowStateManager;
import org.openconcerto.utils.doc.Documented;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * @author ilm Created on 8 oct. 2003
 */
public class EditFrame extends JFrame implements IListener, EditPanelListener, Documented {

    public static final EditMode MODIFICATION = EditPanel.MODIFICATION;
    public static final EditMode CREATION = EditPanel.CREATION;
    /**
     * If this system property is true, then the minimum size of an edit frame will match the one
     * from its content pane.
     */
    public final static String SMALL_MIN_SIZE = "org.openconcerto.sql.editFrame.smallMinSize";

    private boolean frameResize;

    private final EditPanel panel;
    // whether our component was filled since the last setVisible(false)
    private boolean wasFilled;

    /**
     * Crée une fenêtre de création.
     * 
     * @param e the element to display.
     */
    public EditFrame(SQLElement e) {
        this(e, EditPanel.CREATION);
    }

    public EditFrame(SQLElement e, EditMode mode) {
        this(e.createDefaultComponent(), mode);
    }

    /**
     * Creates an instance:
     * <ul>
     * <li>to create if mode is CREATION</li>
     * <li>to modify if MODIFICATION</li>
     * <li>to view if READONLY</li>
     * </ul>
     * 
     * @param comp the component to display.
     * @param mode the edit mode, one of CREATION, MODIFICATION or READONLY.
     */
    public EditFrame(SQLComponent comp, EditMode mode) {
        super();
        this.frameResize = false;

        this.panel = new EditPanel(comp, mode);

        final PropertyChangeListener fillingL = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                EditFrame.this.wasFilled = true;
            }
        };
        this.getSQLComponent().addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
                    final boolean isDesc = SwingUtilities.getAncestorOfClass(EditFrame.this.getClass(), e.getComponent()) != null;
                    if (isDesc)
                        ((BaseSQLComponent) getSQLComponent()).addFillingListener(fillingL);
                    else
                        ((BaseSQLComponent) getSQLComponent()).rmFillingListener(fillingL);
                }
            }
        });

        this.setContentPane(this.panel);
        this.initTitle(comp.getElement(), mode);
        this.setLocation(0, 50);

        // The minimum size of the frame must be the size when packed
        this.pack();
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        final int wantedW, wantedH;
        if (Boolean.getBoolean(SMALL_MIN_SIZE)) {
            final Dimension minimumSize = this.getMinimumSize();
            wantedW = minimumSize.width;
            wantedH = minimumSize.height;
        } else {
            wantedW = getWidth();
            wantedH = getHeight();
        }
        final int w = Math.min(d.width - 100, wantedW);
        final int h = Math.min(d.height - 100, wantedH);
        setMinimumSize(new Dimension(w, h));

        // View resized
        this.viewResized();
        addEditPanelListener(this);
        if (mode == CREATION) {
            // was just reset by uiInit() but the component is not yet visible
            // and the default value can still change
            this.wasFilled = false;
            // raz quand on affiche la fenêtre sauf si déjà rempli
            // use hierarchyChanged() since it works for dispose() and setVisible(false)
            // (ComponentListener.componentHidden() is not called when this is disposed)
            this.addHierarchyListener(new HierarchyListener() {
                @Override
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                        if (!e.getComponent().isShowing()) {
                            if (getPanel().getSQLComponent().getResetMode().isOnHide())
                                getPanel().resetValue();
                            EditFrame.this.wasFilled = false;
                        } else if (!EditFrame.this.wasFilled) {
                            // do it on show() to have current default values
                            if (getPanel().getSQLComponent().getResetMode().isOnShow())
                                getPanel().resetValue();
                        }
                    }
                }
            });
        }
        this.panel.addComponentListenerOnViewPort(new ComponentAdapter() {
            public void componentResized(ComponentEvent ev) {
                viewResized();
            }
        });
        this.panel.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent ev) {
                setFrameResize(true);
            }
        });

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        new WindowStateManager(this, IListFrame.getConfigFile(this.getPanel().getSQLComponent(), this.getClass())).loadState();
    }

    private final void initTitle(SQLElement element, EditMode mode) {
        switch (mode) {
        case CREATION:
            this.setTitle("Créer " + element.getSingularName());
            break;
        case MODIFICATION:
            this.setTitle("Modifier " + element.getSingularName());
            break;
        case READONLY:
            this.setTitle("Détail d'" + element.getSingularName());
            break;
        default:
            throw new IllegalArgumentException("unknown mode : " + mode);
        }
    }

    /**
     * Redimensionne la frame pour qu'elle soit de taille maximum sans déborder de l'écran. <img
     * src="doc-files/resizeFrame.png"/>
     */
    protected void viewResized() {
        if (!this.frameResize) {
            this.setSize(this.getPanel().getViewResizedDimesion(this.getSize()));
        }
        this.setFrameResize(false);
    }

    protected void setFrameResize(boolean b) {
        this.frameResize = b;
    }

    public void addEditPanelListener(EditPanelListener listener) {
        this.getPanel().addEditPanelListener(listener);
    }

    public void cancelled() {
        this.close();
    }

    public void deleted() {
        this.close();
    }

    public void inserted(int id) {
        if (!this.getPanel().alwaysVisible()) {
            this.close();
        }
    }

    public void modified() {
        this.close();
    }

    private void close() {
        // simulate a user closing the window (respect the defaultCloseOperation)
        this.processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    @Deprecated
    public void selectionId(int selectedId, int i) {
        this.getPanel().selectionId(selectedId, i);
    }

    public void selectionId(int selectedId) {
        this.getPanel().selectionId(selectedId, 0);
    }

    public SQLComponent getSQLComponent() {
        return this.getPanel().getSQLComponent();
    }

    public EditPanel getPanel() {
        return this.panel;
    }

    public String getDocId() {
        return "EditFrame" + this.panel.getDocId();
    }

    public String getGenericDoc() {
        return "La fermeture de la fenêtre annule l'action en cours";
    }

    public boolean onScreen() {
        return false;
    }

    public boolean isDocTransversable() {
        return true;
    }
}
