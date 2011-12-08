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
 
 package org.openconcerto.ui;

import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IClosure;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * A component like {@link JTabbedPane} but hiding the tab bar when there's only one tab.
 * 
 * @author Sylvain CUAZ
 */
public class AutoHideTabbedPane extends JPanel {

    private static final String TITLE_PROP = AutoHideTabbedPane.class.getName() + " title prop";
    private static final String ICON_PROP = AutoHideTabbedPane.class.getName() + " icon prop";
    private static final String TOOLTIP_PROP = AutoHideTabbedPane.class.getName() + " tooltip prop";

    private final JTabbedPane tabbedPane;
    // can't use putClientProperty() since we can't retrieve it from JTabbedPane
    private final Map<JComponent, IClosure<Tuple2<JTabbedPane, Integer>>> customizers;

    public AutoHideTabbedPane() {
        this(new JTabbedPane());
    }

    public AutoHideTabbedPane(final JTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;
        this.customizers = new IdentityHashMap<JComponent, IClosure<Tuple2<JTabbedPane, Integer>>>();
        this.setLayout(new GridLayout(1, 1));
    }

    public final void addTab(final String title, final JComponent comp) {
        insertTab(title, null, comp, null, -1);
    }

    /**
     * Insert a tab at the specified index.
     * 
     * @param title the title to be displayed in this tab.
     * @param icon the icon to be displayed in this tab, can be <code>null</code>.
     * @param component the component to be displayed when this tab is clicked.
     * @param tip the tooltip to be displayed for this tab, can be <code>null</code>.
     * @param index the position to insert this new tab, -1 meaning at the end.
     */
    public final void insertTab(String title, Icon icon, JComponent comp, String tip, final int index) {
        this.insertTab(title, icon, comp, tip, index, null);
    }

    public final void insertTab(String title, Icon icon, JComponent comp, String tip, final int index, final IClosure<Tuple2<JTabbedPane, Integer>> customize) {
        this.customizers.put(comp, customize);
        if (getComponentCount() == 0) {
            putClientProps(title, icon, comp, tip);
            this.add(comp);
        } else {
            JTabbedPane tabbedPane = this.getDisplayedTabbedPane();
            // if there was no tabbedPane move the current component to it
            if (tabbedPane == null) {
                final JComponent onlyComp = (JComponent) this.getComponent(0);
                this.remove(0);
                tabbedPane = this.tabbedPane;
                tabbedPane.addTab((String) onlyComp.getClientProperty(TITLE_PROP), (Icon) onlyComp.getClientProperty(ICON_PROP), onlyComp, (String) onlyComp.getClientProperty(TOOLTIP_PROP));
                final IClosure<Tuple2<JTabbedPane, Integer>> closure = this.customizers.get(onlyComp);
                if (closure != null)
                    closure.executeChecked(Tuple2.create(tabbedPane, 0));
                this.add(tabbedPane);
                assert tabbedPane == this.getDisplayedTabbedPane();
            }
            final int realIndex = index < 0 ? tabbedPane.getTabCount() : index;
            tabbedPane.insertTab(title, icon, comp, tip, realIndex);
            if (customize != null)
                customize.executeChecked(Tuple2.create(tabbedPane, realIndex));
        }
        assert this.customizers.size() == getTabContainer().getComponentCount();
        this.revalidate();
        this.repaint();
    }

    private final void putClientProps(String title, Icon icon, JComponent comp, String tip) {
        comp.putClientProperty(TITLE_PROP, title);
        comp.putClientProperty(ICON_PROP, icon);
        comp.putClientProperty(TOOLTIP_PROP, tip);
    }

    public final int removeTab(final JComponent comp) {
        if (comp == null)
            throw new NullPointerException("Null component");
        final int index = Arrays.asList(getTabContainer().getComponents()).indexOf(comp);
        if (index >= 0)
            this.removeTabAt(index);
        return index;
    }

    public final JComponent removeTabAt(final int index) {
        final JComponent res;
        final JTabbedPane tabbedPane = this.getDisplayedTabbedPane();
        if (tabbedPane == null) {
            res = (JComponent) this.getComponent(index);
            this.remove(index);
            this.customizers.clear();
        } else {
            res = (JComponent) tabbedPane.getComponentAt(index);
            this.customizers.remove(res);
            tabbedPane.removeTabAt(index);
            // remove the tabbedPane if there's only one tab
            if (tabbedPane.getTabCount() == 1) {
                final JComponent onlyComp = (JComponent) tabbedPane.getComponentAt(0);
                // store tab properties, so that it can be recreated later
                putClientProps(tabbedPane.getTitleAt(0), tabbedPane.getIconAt(0), onlyComp, tabbedPane.getToolTipTextAt(0));
                tabbedPane.removeTabAt(0);
                this.remove(0);
                this.add(onlyComp);
            }
        }
        assert this.customizers.size() == getTabContainer().getComponentCount();
        this.revalidate();
        this.repaint();
        return res;
    }

    // Allow to access the {@link JTabbedPane}. Note: if you use the result to add a tab, some of
    // its properties might get lost, and if you remove a tab this instance will leak memory
    protected final JTabbedPane getDisplayedTabbedPane() {
        if (this.getComponentCount() == 0)
            return null;
        final Component res = this.getComponent(0);
        return res instanceof JTabbedPane ? (JTabbedPane) res : null;
    }

    private final Container getTabContainer() {
        final JTabbedPane tabbedPane = this.getDisplayedTabbedPane();
        return tabbedPane == null ? this : tabbedPane;
    }
}
