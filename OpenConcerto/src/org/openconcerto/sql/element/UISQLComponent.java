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
 
 /*
 * Created on 5 nov. 2004
 */
package org.openconcerto.sql.element;

import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.ui.FormLayouter;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.LayoutFocusTraversalPolicy;

public abstract class UISQLComponent extends BaseSQLComponent {

    /** System property used to find the suffix to add to required items */
    public static final String REQUIRED_SUFFIX_PROP = "org.openconcerto.sql.requiredSuffix";
    // static since it should be the same in an application
    private static final String REQ_SUFFIX = System.getProperty(REQUIRED_SUFFIX_PROP, " *");

    private FormLayouter autoLayouter;
    private JPanel currentPanel;
    private JTabbedPane tabbedPane;
    // default values for the layouter
    private final int width, def;

    public UISQLComponent(SQLElement element) {
        this(element, 2);
    }

    public UISQLComponent(SQLElement element, int width) {
        this(element, width, 0);
    }

    public UISQLComponent(SQLElement element, int width, int def) {
        super(element);

        this.width = width;
        this.def = def;

        // par defaut pas de tab
        this.tabbedPane = null;
        this.currentPanel = this;
        // ne pas instancier le layouter tout de suite
        // car on ne sait pas encore si tab ou pas
        this.autoLayouter = null;
    }

    protected void addToUI(SQLRowItemView obj, String where) {
        String desc = getDesc(obj);
        if (this.getRequired().contains(obj))
            desc += REQ_SUFFIX;
        if (where == null)
            where = this.getDefaultWhere(obj);
        if (where == null)
            this.getLayouter().add(desc, obj.getComp());
        else if (where.equals("bordered")) {
            this.getLayouter().addBordered(desc, obj.getComp(), 0);
        } else if (where.equals("left")) {
            this.getLayouter().newLine();
            this.getLayouter().add(desc, obj.getComp(), this.getLayouter().getWidth() / 2);
        } else if (where.equals("right"))
            this.getLayouter().add(desc, obj.getComp(), (this.getLayouter().getWidth() + 1) / 2);
        else {
            final int aWidth = Integer.parseInt(where);
            this.getLayouter().add(desc, obj.getComp(), aWidth);
        }
    }

    private String getDefaultWhere(SQLRowItemView obj) {
        if (getElement().getPrivateForeignFields().contains(obj.getField().getName()))
            return "bordered";
        return null;
    }

    protected void addUITitle(String title) {
        this.getLayouter().add(title, null);
    }

    protected final void addTab() {
        this.addTab("Général");
    }

    protected final void addTab(String tabTitle) {
        this.addTab(tabTitle, this.width, this.def);
    }

    /**
     * Add a new tab with the specified title.
     * 
     * @param tabTitle the title
     * @param w the total width of the tab.
     * @param d the default width of a view.
     */
    protected final void addTab(String tabTitle, final int w, final int d) {
        if (this.tabbedPane == null) {
            if (this.getComponentCount() > 0)
                throw new IllegalStateException("you cannot create a tab after adding views");
            this.tabbedPane = new JTabbedPane();
            // reach all fields of this component
            this.tabbedPane.setFocusCycleRoot(true);
            this.tabbedPane.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {

                private final JTabbedPane tab = UISQLComponent.this.tabbedPane;

                @Override
                public Component getComponentAfter(Container container, Component component) {
                    if (component == getLastComponent(this.tab)) {
                        this.tab.setSelectedIndex((this.tab.getSelectedIndex() + 1) % this.tab.getTabCount());
                        return getFirstComponent((Container) this.tab.getSelectedComponent());
                    } else
                        return super.getComponentAfter(container, component);
                }

                @Override
                public Component getComponentBefore(Container container, Component component) {
                    if (component == getFirstComponent(this.tab)) {
                        // + count to avoid negative number
                        final int index = (this.tab.getTabCount() + this.tab.getSelectedIndex() - 1) % this.tab.getTabCount();
                        this.tab.setSelectedIndex(index);
                        return getLastComponent((Container) this.tab.getSelectedComponent());
                    } else
                        return super.getComponentBefore(container, component);
                }

                @Override
                protected boolean accept(Component component) {
                    // do not accept tab otherwise tab is its own successor and the focus never
                    // change
                    if (component == this.tab)
                        return false;
                    else
                        return super.accept(component);
                }
            });
            this.setLayout(new GridLayout(1, 1));
            this.add(this.tabbedPane);
        }
        this.currentPanel = new JPanel();
        // from Guillaume : tabs shouldn't be opaque in Windows L&F
        this.currentPanel.setOpaque(false);
        this.tabbedPane.addTab(tabTitle, this.currentPanel);
        this.setLayouter(w, d);
    }

    private void setLayouter(int w, int d) {
        this.autoLayouter = new FormLayouter(this.currentPanel, w, d);
        this.setAdditionalFieldsPanel(this.autoLayouter);
    }

    protected final FormLayouter getLayouter() {
        if (this.autoLayouter == null)
            this.setLayouter(this.width, this.def);
        return this.autoLayouter;
    }
}
