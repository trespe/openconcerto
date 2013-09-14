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
 * Créé le 21 mai 2005
 */
package org.openconcerto.sql.navigator;

import org.openconcerto.laf.LAFUtils;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.KeyLabel;
import org.openconcerto.ui.PopupMouseListener;
import org.openconcerto.ui.list.selection.ListSelectionState;
import org.openconcerto.utils.JImage;
import org.openconcerto.utils.cc.IPredicate;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public abstract class SQLBrowserColumn<T, L extends SQLListModel<T>> extends JPanel {

    private static final long serialVersionUID = 3340938099896864844L;

    private static final Color DARK_BLUE = new Color(100, 100, 120);
    private static final Icon iconUp = new ImageIcon(LAFUtils.class.getResource("up.png"));
    private static final Icon iconDown = new ImageIcon(LAFUtils.class.getResource("down.png"));
    private static final Font fontText = new Font("Tahoma", Font.PLAIN, 11);

    private final L model;
    private boolean minimized;

    // Hierachie
    protected SQLBrowser parentBrowser;

    // UI
    private JButton min;
    private JLabel title;
    private final JTextField search;
    // UI Layout
    private JPanel normalPanel;
    private JPanel minimizedPanel;
    protected JList list;
    final KeyLabel keyLabel = new KeyLabel("F1");

    private final PropertyChangeListener focusListener;

    public SQLBrowserColumn(final L model, boolean searchable) {
        this.model = model;
        this.search = searchable ? new JTextField() : null;
        this.focusListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                final boolean hasFocus = focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, SQLBrowserColumn.this);
                focusChanged(hasFocus);
            }
        };
    }

    abstract protected String getHeaderName();

    protected void render(final JLabel comp, T value) {
        if (this.getModel().isALLValue(value)) {
            // -1 pour ne pas me compter
            comp.setText("Tous (" + (getModel().getSize() - 1) + ")");
        } else
            comp.setText(this.getModel().toString(value));
    }

    private final void navigate(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            if (this.next() != null) {
                this.next().selectFirstRow();
            } else if (this.list.getSelectedValue() == null) {
                this.list.setSelectedIndex(this.list.getSelectionModel().getLeadSelectionIndex());
            }
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            this.deselect();
            if (this.previous() != null)
                this.previous().setActive();
        }
    }

    public final void select(boolean nextOrPrevious) {
        final int selIndex = this.list.getSelectedIndex();
        if (nextOrPrevious && selIndex < this.list.getModel().getSize() - 1) {
            this.list.setSelectedIndex(selIndex + 1);
        } else if (!nextOrPrevious && selIndex > 0)
            this.list.setSelectedIndex(selIndex - 1);

        // set the focus of our window to us ; not the focus of the application, ie requestFocus(),
        // that way we can change our selection without bringing our frame to the front, but when
        // our frame do come to the front the focus will be ours
        this.list.requestFocusInWindow();
    }

    protected final void uiInit() {
        UIManager.put("List.background", Color.WHITE);
        // UIManager.put("List.selectionBackground", new Color(100, 100, 120));
        UIManager.put("List.selectionForeground", Color.WHITE);

        this.normalPanel = new JPanel();
        this.normalPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;

        // ** Header
        c.weighty = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0;

        // Minimisation
        JPanel headerPanel = createHeaderPanel();
        this.normalPanel.add(headerPanel, c);
        // ** List
        c.gridx = 0;
        c.gridy++;
        c.weighty = 1;
        c.weightx = 1;
        c.gridwidth = 1;

        this.list = new JList(this.getModel());
        this.list.setSelectionModel(new ReSelectionModel());
        this.list.setCellRenderer(new DefaultListCellRenderer() {
            @SuppressWarnings("unchecked")
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final JLabel comp = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                render(comp, (T) value);
                return comp;
            }
        });
        this.list.setFont(fontText);
        this.list.setSelectionMode(this.getSelectionMode());
        JScrollPane scrollPane = new JScrollPane(this.list);
        scrollPane.setBorder(null);
        scrollPane.setMinimumSize(new Dimension(60, 100));
        this.normalPanel.add(scrollPane, c);
        // On ajoute la recherche
        if (this.isSearchable()) {

            JPanel searchPane = createSearchPanel();
            c.gridwidth = 1;
            c.gridx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weighty = 0;
            c.gridy++;
            c.weightx = 0;

            this.normalPanel.add(searchPane, c);
            this.search.getDocument().addDocumentListener(new SimpleDocumentListener() {
                public void update(DocumentEvent e) {
                    SQLBrowserColumn.this.getModel().setSearchString(SQLBrowserColumn.this.search.getText());
                }
            });
        }
        // listeners
        this.list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            final public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    updateNextCol();
                }
            }
        });
        this.list.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                navigate(e);
            }
        });

        this.getModel().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                // si notre contenu change, il faut mettre à jour les colonnes d'après
                // on ne peut compter sur le ListSelectionListener, car même si on ne change pas de
                // sélection il faut maj (eg 'Tous' est sélectionné, la sélection ne change pas
                // d'index, mais son sens oui)
                if (getParentBrowser() != null && isVirtualSelected())
                    // si l'on n'a pas de père, on ne peux avoir de colonnes suivantes
                    updateNextCol();
            }
        }, "items");

        // version iconifiée
        this.minimizedPanel = new VerticalTextColumn(this.getHeaderName());
        this.minimizedPanel.setVisible(false);
        this.minimizedPanel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // On desiconifie la colonne
                getParentBrowser().maximizeFrom(SQLBrowserColumn.this);
            }
        });
        this.setLayout(new GridBagLayout());
        GridBagConstraints cc = new GridBagConstraints();
        cc.fill = GridBagConstraints.BOTH;
        cc.weightx = 1;
        cc.weighty = 1;
        this.add(this.normalPanel, cc);
        cc.gridx++;
        this.add(this.minimizedPanel, cc);

        // our panel should not be focusable, only our children
        this.setFocusable(false);
        // we're in uiInit() called from the ctor, thus not yet displayable
        // so focusListener will be added automatically
        this.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0)
                    if (e.getChanged().isDisplayable())
                        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(SQLBrowserColumn.this.focusListener);
                    else
                        KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener(SQLBrowserColumn.this.focusListener);
            }
        });
        // never leave no selection : that prevents keyboard navigation
        this.list.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                final ListSelectionModel m = SQLBrowserColumn.this.list.getSelectionModel();
                if (!m.isSelectionEmpty())
                    return;

                final int listCount = SQLBrowserColumn.this.list.getModel().getSize();
                if (m.getLeadSelectionIndex() >= listCount) {
                    if (m instanceof DefaultListSelectionModel) {
                        ((DefaultListSelectionModel) m).moveLeadSelectionIndex(listCount - 1);
                    } else {
                        m.setLeadSelectionIndex(listCount - 1);
                        m.clearSelection();
                    }
                }
            }

            public void focusLost(FocusEvent e) {
                // don't care
            }
        });
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new GridBagLayout());
        GridBagConstraints c2 = new GridBagConstraints();
        c2.gridx = 0;
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.insets = new Insets(0, 2, 1, 4);
        c2.weightx = 0;
        this.min = new JButton();
        this.min.setBorder(null);
        this.min.setBorderPainted(false);
        this.min.setOpaque(false);
        this.min.setMargin(new Insets(0, 0, 0, 0));
        this.min.setContentAreaFilled(false);
        this.min.addActionListener(new ActionListener() {
            public final void actionPerformed(ActionEvent e) {
                getParentBrowser().minimizeUntil(SQLBrowserColumn.this);
            }
        });
        this.min.setIcon(new ImageIcon(this.getClass().getResource("minimize.png")));
        headerPanel.add(this.min, c2);

        c2.gridx++;

        // Titre

        this.title = new JLabel(this.getHeaderName());
        setTitleIcon();
        this.setOpaque(false);
        this.title.setFocusable(false);
        this.title.setFont(fontText);
        this.title.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // On trie
                if (e.getButton() == MouseEvent.BUTTON1) {
                    getModel().sort();
                    setTitleIcon();
                }
            }
        });
        final JPopupMenu menu = new JPopupMenu();
        menu.add(new AbstractAction("Recharger") {
            @Override
            public void actionPerformed(ActionEvent e) {
                getModel().reload(true);
            }
        });
        this.title.addMouseListener(new PopupMouseListener(menu));
        // this.normalPanel.setBackground(new Color(239, 235, 231));

        headerPanel.add(this.title, c2);
        c2.gridx++;

        c2.weightx = 1;
        this.keyLabel.setFont(this.title.getFont());
        headerPanel.add(this.keyLabel, c2);

        return headerPanel;
    }

    private void setTitleIcon() {
        final Icon icon;
        switch (this.getModel().getSortDirection()) {
        case ASCENDING:
            icon = iconUp;
            break;
        case DESCENDING:
            icon = iconDown;
            break;
        default:
            icon = null;
        }
        this.title.setIcon(icon);
    }

    // the panel at the bottom
    private JPanel createSearchPanel() {
        final JPanel searchPane = new JPanel();

        searchPane.setLayout(new GridBagLayout());
        GridBagConstraints c2 = new GridBagConstraints();
        c2.gridx = 0;
        c2.weightx = 0;
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.insets = new Insets(2, 2, 1, 1);

        final JImage image = new JImage(ElementComboBox.class.getResource("loupe.png"));
        searchPane.add(image, c2);

        c2.gridx++;
        c2.weightx = 1;
        searchPane.add(this.search, c2);
        c2.weightx = 0;

        c2.gridx++;
        final JButton del = new JButton(new ImageIcon(BaseSQLComponent.class.getResource("delete.png")));
        del.setBorder(null);
        del.setOpaque(false);
        del.setContentAreaFilled(false);
        del.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SQLBrowserColumn.this.search.setText("");
            }
        });
        searchPane.add(del, c2);

        return searchPane;
    }

    protected abstract int getSelectionMode();

    // *** selection

    public void addSelectionListener(PropertyChangeListener listener) {
        this.addPropertyChangeListener("selection", listener);
    }

    public void removeSelectionListener(PropertyChangeListener listener) {
        this.removePropertyChangeListener("selection", listener);
    }

    // the meaning of our selection has changed (either our selection has changed, either our
    // selection has *not* changed but its meaning did, eg "All")
    final void updateNextCol() {
        final ListSelectionModel m = this.list.getSelectionModel();
        final List groups;
        if (m.isSelectionEmpty()) {
            groups = Collections.EMPTY_LIST;
            this.selectionCleared();
            this.getParentBrowser().rmColumnAfter(this);
        } else {
            // ATTN ne marche que pour une selection continue (SINGLE ou SINGLE_INTERVAL)
            groups = new ArrayList(m.getMaxSelectionIndex() - m.getMinSelectionIndex() + 1);

            final SQLBrowserColumn next = this.selectionChanged(m);
            if (next == null)
                this.getParentBrowser().rmColumnAfter(this);
            else if (next != this.next())
                this.getParentBrowser().addColumn(next, this);

            // display the newly selected index
            final int lead = m.getLeadSelectionIndex();
            // for some unknown reason with some large lists (>1000) this only works in later()
            // (getCellBounds() is not at fault its result is the same now and later())
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    SQLBrowserColumn.this.list.ensureIndexIsVisible(lead);
                }
            });
        }
        // On notifie les listeners
        this.parentBrowser.fireSQLBrowserColumnSelected(this);
        this.firePropertyChange("selection", null, groups);
    }

    protected void selectionCleared() {
        // do nothing by default
    }

    abstract protected SQLBrowserColumn selectionChanged(ListSelectionModel m);

    public abstract void setSelectedRow(SQLRow o);

    void setParentIDs(Collection<? extends Number> ids) {
        this.getModel().setParentIDs(ids);
        // getSelectedRows() might need parentBrowser
        if (this.getParentBrowser() != null && this.getSelectedRows().isEmpty()) {
            // si la selection est vide, oublier le userID sinon qd on reclique sur le pére
            // du userID il est automatiquement reselectionné, hors on s'attend à ce que la
            // selection soit la ligne cliquée (et non 1 de ses fils).
            final ListSelectionState state = this.getSelectionState();
            if (state != null)
                state.selectIDs(Collections.<Integer> emptySet());
        }
    }

    // overload if a selectionState handle this column
    protected ListSelectionState getSelectionState() {
        return null;
    }

    protected final void setSelectedValue(Object o) {
        this.list.setSelectedValue(o, true);
    }

    // ATTN all = all that is searched
    @SuppressWarnings("unchecked")
    public final boolean isAllSelected() {
        // use isEmpty() to distinguish between selection of null and no selection
        return !this.list.isSelectionEmpty() && this.getModel().isALLValue((T) this.list.getSelectedValue());
    }

    /**
     * Whether the current selection's children depend on other lines of this column. Eg
     * BATIMENT[12] is not virtual but "Begining with A" is.
     * 
     * @return <code>false</code> if the selection define by itself its children, <code>true</code>
     *         otherwise.
     */
    boolean isVirtualSelected() {
        return this.isAllSelected();
    }

    protected final void reload() {
        this.getModel().reload();
    }

    public String toString() {
        return this.getClass().getName() + ": " + this.getHeaderName() + " modelSize:" + this.getModel().getSize();
    }

    public final void setTransferHandler(TransferHandler th) {
        this.list.setDragEnabled(th != null);
        this.list.setTransferHandler(th);
    }

    public final void setMinimizedState(boolean b) {
        this.normalPanel.setVisible(!b);
        this.minimizedPanel.setVisible(b);
        this.minimized = b;
    }

    public final boolean isMinimized() {
        return this.minimized;
    }

    public final SQLBrowser getParentBrowser() {
        return this.parentBrowser;
    }

    public final void deselect() {
        this.list.clearSelection();
    }

    final void setParentBrowser(SQLBrowser browser) {
        if (this.parentBrowser != null && browser != null)
            throw new IllegalStateException("browser already set to : " + this.parentBrowser);
        if (browser == null)
            this.die();
        this.parentBrowser = browser;
        if (this.parentBrowser != null)
            this.live();
    }

    abstract protected void live();

    abstract protected void die();

    public final SQLBrowserColumn<?, ?> previous() {
        final int myIndex = this.parentBrowser.getColumns().indexOf(this);
        if (myIndex == 0)
            return null;
        else
            return this.parentBrowser.getColumns().get(myIndex - 1);
    }

    public SQLBrowserColumn<?, ?> next() {
        if (this.parentBrowser.getLastColumn() == this)
            return null;
        else {
            final int myIndex = this.parentBrowser.getColumns().indexOf(this);
            return this.parentBrowser.getColumns().get(myIndex + 1);
        }
    }

    public final RowsSQLBrowserColumn previousRowsColumn() {
        return previousRowsColumn(false);
    }

    public final RowsSQLBrowserColumn previousRowsColumn(final boolean includingThis) {
        SQLBrowserColumn currentCol = includingThis ? this : this.previous();
        while (currentCol != null && !(currentCol instanceof RowsSQLBrowserColumn))
            currentCol = currentCol.previous();
        return (RowsSQLBrowserColumn) currentCol;
    }

    private void selectFirstRow() {
        if (!this.getModel().getRealItems().isEmpty())
            this.list.setSelectedValue(this.getModel().getRealItems().get(0), true);
        else if (this.getModel().getSize() > 0)
            this.list.setSelectedIndex(0);
        else
            throw new IllegalStateException("completely empty column " + this);
        this.setActive();
    }

    /*
     * Tag la colonne comme active (titre en rouge + focus)
     */
    public final void setActive() {
        this.getParentBrowser().activate(this);
    }

    void activate() {
        // only request the focus, if none of our descendants has it
        // (avoid the removal of focus of the search field)
        if (!this.isActive())
            this.list.requestFocusInWindow();
    }

    protected final void focusChanged(boolean gained) {
        if (gained) {
            this.normalPanel.setBackground(DARK_BLUE);
            this.title.setForeground(Color.WHITE);
            this.list.setSelectionBackground(DARK_BLUE);
        } else {
            this.normalPanel.setBackground(null);
            this.title.setForeground(null);
            this.list.setSelectionBackground(Color.LIGHT_GRAY);
        }
        if (this.getParentBrowser() != null)
            this.getParentBrowser().columnFocusChanged(this, gained);
    }

    public final boolean isActive() {
        return this.getParentBrowser().getActiveColumn() == this;
    }

    public final void setShortcut(String string) {
        this.keyLabel.setText(string);
    }

    static class ReSelectionModel extends DefaultListSelectionModel {
        public void setSelectionInterval(int index0, int index1) {
            final boolean alreadySelected = this.isSelectedIndex(index0);
            super.setSelectionInterval(index0, index1);
            // we want to know when we click on the selected index
            if (index0 == index1 && alreadySelected) {
                // single event, so isAdjusting = false
                this.fireValueChanged(index0, index0, false);
            }
        }
    }

    public abstract SQLTable getTable();

    public abstract List<Integer> getSelectedIDs();

    public abstract List<SQLRow> getSelectedRows();

    /**
     * The rows specifically chosen by the user.
     * 
     * @return the list of rows selected.
     */
    public final List<SQLRow> getUserSelectedRows() {
        final RowsSQLBrowserColumn prev = this.previousRowsColumn(true);
        final ListSelectionState state = prev.getSelectionState();
        if (state == null)
            return prev.getSelectedRows();
        else {
            final Set<Integer> userSelectedIDs = state.getUserSelectedIDs();
            final RowsSQLListModel prevModel = prev.getModel();
            final ListStateModel stateModel = prev.getStateModel();
            if ((prevModel.hasALLValue() && userSelectedIDs.contains(ListStateModel.ALL_ID)))
                return prev.getSelectedRows();
            else
                return prevModel.selectItems(false, new IPredicate<SQLRow>() {
                    @Override
                    public boolean evaluateChecked(SQLRow r) {
                        return userSelectedIDs.contains(stateModel.stateIDFromItem(r));
                    }
                });
        }
    }

    protected final boolean isSearchable() {
        return this.search != null;
    }

    protected final boolean isSearched() {
        return this.getModel().getSearch() != null && !this.getModel().getSearch().isEmpty();
    }

    protected final L getModel() {
        return this.model;
    }

    /**
     * Set the selection mode of the lists.
     * 
     * @param selectionMode ListSelectionModel.SINGLE_SELECTION SINGLE_INTERVAL_SELECTION
     *        ListSelectionModel.SINGLE_INTERVAL_SELECTION MULTIPLE_INTERVAL_SELECTION
     *        ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
     */
    public void setSelectionMode(int selectionMode) {
        this.list.setSelectionMode(selectionMode);
    }

}
