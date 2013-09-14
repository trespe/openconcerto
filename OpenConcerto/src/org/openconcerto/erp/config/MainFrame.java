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
 
 package org.openconcerto.erp.config;

import org.openconcerto.erp.action.AboutAction;
import org.openconcerto.erp.action.PreferencesAction;
import org.openconcerto.erp.core.common.ui.StatusPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.task.TodoListPanel;
import org.openconcerto.task.config.ComptaBasePropsConfiguration;
import org.openconcerto.ui.AutoHideTabbedPane;
import org.openconcerto.ui.MenuUtils;
import org.openconcerto.ui.SwingThreadUtils;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;
import org.openconcerto.ui.state.WindowStateManager;
import org.openconcerto.utils.JImage;
import org.openconcerto.utils.OSXAdapter;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;

public class MainFrame extends JFrame {

    // menus
    public static final String STRUCTURE_MENU = "menu.organization";
    public static final String PAYROLL_MENU = "menu.payroll";
    public static final String PAYMENT_MENU = "menu.payment";
    public static final String STATS_MENU = "menu.stats";
    public static final String DECLARATION_MENU = "menu.report";
    public static final String STATE_MENU = "menu.accounting";
    public static final String LIST_MENU = "menu.list";
    public static final String CREATE_MENU = "menu.create";
    public static final String FILE_MENU = "menu.file";
    public static final String HELP_MENU = "menu.help";

    static private final List<Runnable> runnables = new ArrayList<Runnable>();
    static private MainFrame instance = null;

    public static MainFrame getInstance() {
        return instance;
    }

    private static void setInstance(MainFrame f) {
        if (f != null && instance != null)
            throw new IllegalStateException("More than one main frame");
        instance = f;
        if (f != null) {
            for (final Runnable r : runnables)
                r.run();
            runnables.clear();
        }
    }

    /**
     * Execute the runnable in the EDT after the main frame has been created. Thus if the main frame
     * has already been created and we're in the EDT, execute <code>r</code> immediately.
     * 
     * @param r the runnable to run.
     * @see #getInstance()
     */
    public static void invoke(final Runnable r) {
        SwingThreadUtils.invoke(new Runnable() {
            @Override
            public void run() {
                if (instance == null) {
                    runnables.add(r);
                } else {
                    r.run();
                }
            }
        });
    }

    private final AutoHideTabbedPane tabContainer;
    private TodoListPanel todoPanel;
    private JImage image;

    public TodoListPanel getTodoPanel() {
        return this.todoPanel;
    }

    public MainFrame() {
        super();

        this.setIconImage(new ImageIcon(this.getClass().getResource("frameicon.png")).getImage());

        Container co = this.getContentPane();
        co.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        // // co.add(new RangeSlider(2005), c);
        c.weightx = 1;
        c.weighty = 0;
        this.image = new JImage(ComptaBasePropsConfiguration.class.getResource("logo.png"));
        this.image.setBackground(Color.WHITE);
        this.image.check();
        co.add(this.image, c);
        c.weighty = 0;
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        co.add(new JSeparator(JSeparator.HORIZONTAL), c);
        c.gridy++;
        c.weighty = 1;
        this.tabContainer = new AutoHideTabbedPane();
        co.add(this.tabContainer, c);
        Dimension minSize;
        final String confSuffix;
        if (!Gestion.isMinimalMode()) {
            this.todoPanel = new TodoListPanel();
            this.getTabbedPane().addTab("Tâches", this.todoPanel);
            minSize = new Dimension(800, 600);
            confSuffix = "";
        } else {
            minSize = null;
            confSuffix = "-minimal";
        }
        c.weighty = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        co.add(StatusPanel.getInstance(), c);

        if (minSize == null) {
            this.pack();
            minSize = new Dimension(this.getSize());
        }
        this.setMinimumSize(minSize);

        final File confFile = new File(Configuration.getInstance().getConfDir(), "Configuration" + File.separator + "Frame" + File.separator + "mainFrame" + confSuffix + ".xml");
        new WindowStateManager(this, confFile).loadState();

        registerForMacOSXEvents();

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                quit();
            }
        });

        setInstance(this);
        // Overrive logo
        Image im = ComptaPropsConfiguration.getInstanceCompta().getCustomLogo();
        if (im != null) {
            image.setImage(im);
        }
        new NewsUpdater(this.image);
    }

    // create the UI menu from the menu manager
    public final void initMenuBar() {
        final MenuManager mm = MenuManager.getInstance();
        final PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                // TODO change only if needed (mind the actions)
                setJMenuBar(createMenu(mm));
                getLayeredPane().revalidate();
            }
        };
        mm.addPropertyChangeListener(listener);
        listener.propertyChange(null);
    }

    private final JMenuBar createMenu(final MenuManager mm) {
        final JMenuBar result = new JMenuBar();
        final Group g = mm.getGroup();
        for (int i = 0; i < g.getSize(); i++) {
            final Item item = g.getItem(i);
            if (item.getLocalHint().isVisible())
                result.add(createJMenuFrom(item, mm));
        }
        return result;
    }

    private final JMenu createJMenuFrom(final Item item, final MenuManager mm) {
        assert item.getLocalHint().isVisible();
        final String id = item.getId();
        final String name = mm.getLabelForId(id);
        final String menuLabel;
        final Color c;
        if (name == null || name.trim().isEmpty()) {
            menuLabel = id;
            c = new Color(200, 65, 20);
        } else {
            menuLabel = name;
            c = null;
        }
        assert menuLabel != null;
        final JMenu m = new JMenu(menuLabel);
        if (c != null)
            m.setForeground(c);

        if (item instanceof Group) {
            createMenuItemsRec(mm, m, Collections.<String> emptyList(), (Group) item);
        } else {
            m.add(createJMenuItemForId(id, mm));
        }
        return m;
    }

    private final void createMenuItemsRec(final MenuManager mm, final JMenu topLevelMenu, final List<String> path, final Group g) {
        assert g.getLocalHint().isVisible();
        for (int i = 0; i < g.getSize(); i++) {
            final Item child = g.getItem(i);
            if (!child.getLocalHint().isVisible())
                continue;

            if (child instanceof Group) {
                final List<String> newPath = new ArrayList<String>(path);
                newPath.add(child.getId());
                createMenuItemsRec(mm, topLevelMenu, newPath, (Group) child);
            } else {
                final List<String> newPath;
                if (path.size() % 2 == 0) {
                    newPath = new ArrayList<String>(path);
                    newPath.add(null);
                } else {
                    newPath = path;
                }
                MenuUtils.addMenuItem(createJMenuItemForId(child.getId(), mm), topLevelMenu, newPath);
            }
        }
    }

    static public String getFirstNonEmpty(final List<String> l) {
        for (final String s : l) {
            if (s != null && !s.trim().isEmpty()) {
                return s;
            }
        }
        return null;
    }

    private final JMenuItem createJMenuItemForId(final String id, final MenuManager mm) {
        final String mngrLabel = mm.getLabelForId(id);
        final Action mngrAction = mm.getActionForId(id);
        final String mngrActionName = mngrAction == null || mngrAction.getValue(Action.NAME) == null ? null : mngrAction.getValue(Action.NAME).toString();

        final String label = getFirstNonEmpty(Arrays.asList(mngrLabel, mngrActionName, id));
        assert label != null;
        final Action action;
        final Color fg;
        if (mngrAction == null) {
            action = new AbstractAction(label) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(MainFrame.this, "No action for " + id);
                }
            };
            fg = new Color(200, 65, 95);
        } else {
            action = mngrAction;
            fg = label.equals(id) ? new Color(20, 65, 200) : null;
        }

        final JMenuItem res = new JMenuItem(action);
        // don't use action name but the provided label
        res.setHideActionText(true);
        res.setText(label);
        if (fg != null)
            res.setForeground(fg);
        return res;
    }

    public JMenuItem addMenuItem(final Action action, final String... path) {
        return this.addMenuItem(action, Arrays.asList(path));
    }

    /**
     * Adds a menu item to this menu. The path should be an alternation of menu and group within
     * that menu. All items within the same group will be grouped together inside separators. Menus
     * will be created as needed.
     * 
     * @param action the action to perform.
     * @param path where to add the menu item.
     * @return the newly created item.
     * @throws IllegalArgumentException if path is not even.
     */
    public JMenuItem addMenuItem(final Action action, final List<String> path) throws IllegalArgumentException {
        if (path.size() == 0 || path.size() % 2 != 0)
            throw new IllegalArgumentException("Path should be of the form menu/group/menu/group/... : " + path);
        final JMenu topLevelMenu = getMenu(path.get(0));
        return MenuUtils.addMenuItem(action, topLevelMenu, path.subList(1, path.size()));
    }

    // get or create (at the end) a top level menu
    private JMenu getMenu(final String name) {
        final JMenu existing = MenuUtils.findChild(this.getJMenuBar(), name, JMenu.class);
        final JMenu res;
        if (existing == null) {
            res = new JMenu(name);
            // insert before the help menu
            this.getJMenuBar().add(res, this.getJMenuBar().getComponentCount() - 1);
        } else {
            res = existing;
        }
        return res;
    }

    /**
     * Remove the passed item from this menu. This method handles the cleanup of separators and
     * empty menus.
     * 
     * @param item the item to remove.
     * @throws IllegalArgumentException if <code>item</code> is not in this menu.
     */
    public void removeMenuItem(final JMenuItem item) throws IllegalArgumentException {
        if (SwingThreadUtils.getAncestorOrSelf(JMenuBar.class, item) != this.getJMenuBar())
            throw new IllegalArgumentException("Item not in this menu " + item);
        MenuUtils.removeMenuItem(item);
    }

    // Generic registration with the Mac OS X application menu
    // Checks the platform, then attempts to register with the Apple EAWT
    // See OSXAdapter.java to see how this is done without directly referencing any Apple APIs
    public void registerForMacOSXEvents() {
        if (Gestion.MAC_OS_X) {
            try {
                // Generate and register the OSXAdapter, passing it a hash of all the methods we
                // wish to use as delegates for various com.apple.eawt.ApplicationListener methods
                OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("quit", new Class[0]));
                OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("about", new Class[0]));
                OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("preferences", new Class[0]));
                // no OSXAdapter.setFileHandler() for now
            } catch (Exception e) {
                System.err.println("Error while loading the OSXAdapter:");
                e.printStackTrace();
            }
        }
    }

    // used by OSXAdapter
    public final void preferences() {
        new PreferencesAction().actionPerformed(null);
    }

    public final void about() {
        AboutAction.getInstance().actionPerformed(null);
    }

    public boolean quit() {
        if (this.getTodoPanel() != null)
            this.getTodoPanel().stopUpdate();
        Gestion.askForExit();
        return false;
    }

    public final AutoHideTabbedPane getTabbedPane() {
        return this.tabContainer;
    }
}
