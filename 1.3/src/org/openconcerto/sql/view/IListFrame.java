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

import static org.openconcerto.utils.FileUtils.FILENAME_ESCAPER;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.State;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.state.WindowStateManager;

import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * Une frame affichant une liste et des boutons pour la manipuler, ainsi que des statistiques dans
 * la barre de titre.
 * 
 * @author ILM Informatique 11 juin 2004
 */
public class IListFrame extends JFrame {

    public static final String SHORT_TITLE = "org.openconcerto.listframe.shortTitle";

    // state-EditFrame/cpi-window.xml
    static public final File getConfigFile(final SQLElement elem, final Class<? extends JFrame> c) {
        return getConfigFile(elem, null, c);
    }

    // state-EditFrame/cpi-FullComp-window.xml
    static public final File getConfigFile(final SQLComponent comp, final Class<? extends JFrame> c) {
        return getConfigFile(comp.getElement(), comp, c);
    }

    static private final File getConfigFile(final SQLElement elem, final SQLComponent comp, final Class<? extends JFrame> c) {
        final Configuration conf = Configuration.getInstance();
        if (conf == null)
            return null;
        // no getSimpleName() since for inner classes this is empty
        final String compName = comp == null ? "" : "-" + comp.getClass().getName();
        final String filename = FILENAME_ESCAPER.escape(elem.getPluralName() + compName) + "-window.xml";
        return new File(conf.getConfDir(), "state-" + c.getSimpleName() + File.separator + filename);
    }

    private final IListPanel panel;
    private String title;

    public IListFrame(final IListPanel panel) {
        this.panel = panel;
        this.title = null;

        // rafraichir le titre à chaque changement de la liste
        this.panel.getListe().addListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                setTitle();
            }
        });
        this.getPanel().getListe().addListenerOnModel(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName() == null || evt.getPropertyName().equals("loading") || evt.getPropertyName().equals("searching"))
                    setTitle();
            }
        });

        this.uiInit();
        if (State.DEBUG) {
            State.INSTANCE.frameCreated();
            this.addComponentListener(new ComponentAdapter() {
                public void componentHidden(ComponentEvent e) {
                    State.INSTANCE.frameHidden();
                }

                public void componentShown(ComponentEvent e) {
                    State.INSTANCE.frameShown();
                }
            });
        }
    }

    protected String getPlural(String s, int nb) {
        return nb + " " + s + (nb > 1 ? "s" : "");
    }

    protected void setTitle(boolean displayRowCount, boolean displayItemCount) {
        String title;
        if (this.title == null) {
            final String prefix = Boolean.getBoolean(SHORT_TITLE) ? "" : "Liste des ";
            title = prefix + this.panel.getElement().getPluralName();
        } else
            title = this.title;
        if (this.panel.getListe().isDead())
            title += ", détruite";
        else {
            if (displayRowCount) {
                final int rowCount = this.panel.getListe().getRowCount();
                title += ", " + getPlural("ligne", rowCount);
                final int total = this.panel.getListe().getTotalRowCount();
                if (total != rowCount)
                    title += " / " + total;
            }
            if (displayItemCount) {
                int count = this.panel.getListe().getItemCount();
                if (count >= 0)
                    title += ", " + this.getPlural("élément", count);
            }
            final ITableModel model = this.getPanel().getListe().getModel();
            if (model.isLoading())
                title += ", chargement en cours";
            if (model.isSearching())
                title += ", recherche en cours";
        }
        this.setTitle(title);
    }

    /**
     * Change the title of this frame from the default "Liste des " + getElement().getPluralName()
     * to <code>s</code>.
     * 
     * @param s the title of this frame, can be <code>null</code> to restore the default behaviour.
     */
    public void setTextTitle(String s) {
        this.title = s;
    }

    public void setTitle() {
        this.setTitle(true, true);
    }

    final private void uiInit() {
        this.setTitle();
        this.getContentPane().setLayout(new GridLayout());
        this.getContentPane().add(this.panel);
        FrameUtil.setBounds(this);
        final File file;
        if (this.getPanel().getSQLComponent() != null)
            file = getConfigFile(this.getPanel().getSQLComponent(), this.getClass());
        else
            file = getConfigFile(this.getPanel().getElement(), this.getClass());
        if (file != null)
            new WindowStateManager(this, file).loadState();
    }

    public final IListPanel getPanel() {
        return this.panel;
    }

}
