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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.openoffice.XMLVersion;
import org.openconcerto.openoffice.spreadsheet.SpreadSheet;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.Log;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.IListener;
import org.openconcerto.sql.view.search.ColumnSearchSpec;
import org.openconcerto.sql.view.search.SearchList;
import org.openconcerto.ui.FontUtils;
import org.openconcerto.ui.FormatEditor;
import org.openconcerto.ui.list.selection.ListSelection;
import org.openconcerto.ui.list.selection.ListSelectionState;
import org.openconcerto.ui.state.JTableStateManager;
import org.openconcerto.ui.table.AlternateTableCellRenderer;
import org.openconcerto.ui.table.ColumnSizeAdjustor;
import org.openconcerto.ui.table.TableColumnModelAdapter;
import org.openconcerto.ui.table.TablePopupMouseListener;
import org.openconcerto.ui.table.ViewTableModel;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.FormatGroup;
import org.openconcerto.utils.TableModelSelectionAdapter;
import org.openconcerto.utils.TableSorter;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.convertor.StringClobConvertor;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Clob;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * Une liste de lignes correspondant à une ListSQLRequest. Diagramme pour la sélection : <img
 * src="doc-files/listSelection.png"/><br/>
 * 
 * @author ILM Informatique
 */
public final class IListe extends JPanel implements AncestorListener {

    private static boolean FORCE_ALT_CELL_RENDERER = false;
    private static final DateFormat MODIF_DATE_FORMAT = new SimpleDateFormat("'le' dd MMMM yyyy 'à' HH:mm:ss");
    static final String SEP = " ► ";
    private static final Map<Class<?>, FormatGroup> FORMATS;
    static {
        FORMATS = new HashMap<Class<?>, FormatGroup>();
        FORMATS.put(Date.class, new FormatGroup(DateFormat.getDateInstance(DateFormat.SHORT), DateFormat.getDateInstance(DateFormat.MEDIUM), DateFormat.getDateInstance(DateFormat.LONG)));
    }

    public static final void remove(InputMap m, KeyStroke key) {
        InputMap current = m;
        while (current != null) {
            current.remove(key);
            current = current.getParent();
        }
    }

    /**
     * Whether to force table cell renderers to always be alternate. I.e. even after the list
     * creation, if the renderer of a cell is changed, a listener will wrap it in an
     * {@link AlternateTableCellRenderer} if necessary.
     * 
     * @param force <code>true</code> to listen to renderer change, and wrap it in an
     *        {@link AlternateTableCellRenderer}.
     */
    public static void setForceAlternateCellRenderer(boolean force) {
        FORCE_ALT_CELL_RENDERER = force;
    }

    // *** instance

    private final JTable jTable;
    private final JTextField filter;
    private boolean debugFilter;
    private FilterWorker filterWorker;
    // optional popup on the table
    private final JPopupMenu popup;
    private final TableSorter sorter;
    // record the source when non-displayable (ie getModel() == null)
    private SQLTableModelSource src;
    private boolean adjustVisible;
    private ColumnSizeAdjustor tcsa;

    // * selection
    private final List<IListener> listeners;
    private final List<IListener> naListeners;

    // * listeners
    private final PropertyChangeSupport supp;
    // for not adjusting listeners
    private final ListSelectionListener selectionListener;
    // filter
    private final PropertyChangeListener filterListener;
    // listen on model's properties
    private final List<PropertyChangeListener> modelPCListeners;

    private final ListSelectionState state;
    private final JTableStateManager tableStateManager;

    public IListe(final ListSQLRequest req) {
        this(req, null);
    }

    public IListe(final ListSQLRequest req, File configFile) {
        this((Object) req, configFile);
    }

    public IListe(final SQLTableModelSource req) {
        this(req, null);
    }

    public IListe(final SQLTableModelSource req, File configFile) {
        this((Object) req, configFile);
    }

    private IListe(final Object req, File configFile) {
        if (req == null)
            throw new NullPointerException("Création d'une IListe avec une requete null");

        this.supp = new PropertyChangeSupport(this);
        this.listeners = new ArrayList<IListener>();
        this.naListeners = new ArrayList<IListener>();
        this.modelPCListeners = new ArrayList<PropertyChangeListener>();

        this.sorter = new TableSorter();
        this.jTable = new JTable(this.sorter) {
            @Override
            public String getToolTipText(MouseEvent event) {
                final String original = super.getToolTipText(event);

                // Locate the row under the event location
                final int rowIndex = rowAtPoint(event.getPoint());
                // has already happened on M3 (not sure how)
                if (rowIndex < 0)
                    return original;

                final List<String> infoL = new ArrayList<String>();
                if (original != null) {
                    final String html = "<html>";
                    if (original.startsWith(html))
                        // -1 since the closing tag is </html>
                        infoL.add(original.substring(html.length(), original.length() - html.length() - 1));
                    else
                        infoL.add(original);
                }

                final SQLRowValues row = IListe.this.getModel().getRow(rowIndex).getRow();

                final String create = getLine("Créée", row, getSource().getPrimaryTable().getCreationUserField(), getSource().getPrimaryTable().getCreationDateField());
                if (create != null)
                    infoL.add(create);
                final String modif = getLine("Modifiée", row, getSource().getPrimaryTable().getModifUserField(), getSource().getPrimaryTable().getModifDateField());
                if (modif != null)
                    infoL.add(modif);

                final String info;
                if (infoL.size() == 0)
                    info = null;
                else
                    info = "<html>" + CollectionUtils.join(infoL, "<br/>") + "</html>";
                // ATTN doesn't follow the mouse if info remains the same, MAYBE add an identifier
                return info;
            }

            public String getLine(final String verb, final SQLRowValues row, final SQLField userF, final SQLField dateF) {
                final Calendar date = dateF == null ? null : row.getDate(dateF.getName());
                final SQLRowAccessor user = userF == null || row.isForeignEmpty(userF.getName()) ? null : row.getForeign(userF.getName());
                if (user == null && date == null)
                    return null;

                String res = verb;
                if (user != null)
                    res += " par " + user.getString("PRENOM") + " " + user.getString("NOM");

                if (date != null)
                    res += " " + MODIF_DATE_FORMAT.format(date.getTime());

                return res;
            }

            @Override
            public void createDefaultColumnsFromModel() {
                super.createDefaultColumnsFromModel();
                // only load when all columns are created
                loadTableState();
            };
        };
        this.adjustVisible = true;
        this.tcsa = null;
        this.filter = new JTextField();
        this.filter.setEditable(false);
        this.debugFilter = false;
        this.filterWorker = null;

        // do not handle F2, let our application use it :
        // remove F2 keybinding, use space
        final InputMap tm = this.jTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        remove(tm, KeyStroke.getKeyStroke("F2"));
        tm.put(KeyStroke.getKeyStroke(' '), "startEditing");
        // don't auto start, otherwise F2 will trigger the edition
        this.jTable.putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);

        this.popup = new JPopupMenu();
        TablePopupMouseListener.add(this.jTable, new ITransformer<MouseEvent, JPopupMenu>() {
            @Override
            public JPopupMenu transformChecked(MouseEvent input) {
                // afficher un menu que si selection
                return hasSelection() ? IListe.this.popup : null;
            }
        });

        this.selectionListener = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    fireNASelectionId();
                }
            }
        };
        this.filterListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateFilter();
            }
        };
        this.jTable.getColumnModel().addColumnModelListener(new TableColumnModelAdapter() {
            // invoked by toggleAutoAdjust(), ITableModel.setDebug() or updateColNames()
            @Override
            public void columnAdded(TableColumnModelEvent e) {
                updateCols(e.getToIndex());
            }
        });
        this.tableStateManager = new JTableStateManager(this.jTable, configFile);

        // MAYBE only set this.src and let the model be null so that the mere creation of an IListe
        // does not spawn several threads and access the db. But a lot of code assumes there's
        // immediately a model.
        if (req instanceof SQLTableModelSource)
            this.setSource((SQLTableModelSource) req);
        else
            this.setRequest((ListSQLRequest) req);
        this.state = ListSelectionState.manage(this.jTable.getSelectionModel(), new TableListStateModel(this.sorter));
        this.state.addPropertyChangeListener("selectedIndex", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                final Number newValue = (Number) evt.getNewValue();
                // if there's no selection (eg some lines were removed)
                // don't try to scroll (it will go to the top)
                if (newValue.intValue() >= 0)
                    IListe.this.jTable.scrollRectToVisible(IListe.this.jTable.getCellRect(newValue.intValue(), 0, true));
            }
        });
        this.state.addPropertyChangeListener("selectedID", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                fireSelectionId(((Number) evt.getNewValue()).intValue(), IListe.this.jTable.getSelectedColumn());
            }
        });
        // this.jTable.setEnabled(!updating) ne sert à rien
        // car les updates du ITableModel se font de manière synchrone dans la EDT
        // donc on ne peut faire aucune action pendant les maj

        uiInit();
    }

    /**
     * Formats used for editing cells.
     * 
     * @return a mapping between cell value's class and its format.
     */
    public final Map<Class<?>, FormatGroup> getFormats() {
        return FORMATS;
    }

    public final void addRowAction(Action action) {
        this.popup.add(action);
    }

    private void uiInit() {
        // * filter
        this.filter.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isAltDown()) {
                    invertDebug();
                }
            }
        });
        FontUtils.setFontFor(this.filter, SEP);
        this.updateFilter();

        // * JTable

        // active/désactive le mode DEBUG du tableModel en ALT-clickant sur les entêtes des colonnes
        this.jTable.getTableHeader().addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.isAltDown()) {
                    final boolean debug = IListe.this.getModel().isDebug();
                    IListe.this.getModel().setDebug(!debug);
                    setDebug(!debug);
                }
            }

            private final JPopupMenu popupMenu;
            {
                this.popupMenu = new JPopupMenu();
                this.popupMenu.add(new JCheckBoxMenuItem(new AbstractAction("Ajuster la largeur des colonnes") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        toggleAutoAdjust();
                    }
                }));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (IListe.this.adjustVisible && e.isPopupTrigger()) {
                    ((JCheckBoxMenuItem) this.popupMenu.getComponent(0)).setSelected(isAutoAdjusting());
                    this.popupMenu.show((Component) e.getSource(), e.getX(), e.getY());
                }
            }

        });
        // use SQLTableModelColumn.getToolTip()
        this.jTable.getTableHeader().setDefaultRenderer(new TableCellRenderer() {
            private final TableCellRenderer orig = IListe.this.jTable.getTableHeader().getDefaultRenderer();

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component res = this.orig.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (res instanceof JComponent) {
                    // column is the view index
                    final SQLTableModelColumn col = getSource().getColumn(table.convertColumnIndexToModel(column));
                    ((JComponent) res).setToolTipText(col.getToolTip());
                }
                return res;
            }
        });
        this.jTable.setDefaultRenderer(Clob.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return super.getTableCellRendererComponent(table, StringClobConvertor.INSTANCE.unconvert((Clob) value), isSelected, hasFocus, row, column);
            }
        });
        for (final Map.Entry<Class<?>, FormatGroup> e : this.getFormats().entrySet())
            this.jTable.setDefaultEditor(e.getKey(), new FormatEditor(e.getValue()));
        this.sorter.setTableHeader(this.jTable.getTableHeader());
        this.addAncestorListener(this);
        // we used to rm this listener, possibly to avoid events once dead, but this doesn't seem
        // necessary anymore
        this.jTable.getSelectionModel().addListSelectionListener(this.selectionListener);

        // TODO speed up like IListPanel buttons
        // works because "JTable.autoStartsEdit" is false
        // otherwise mets un + a la fin de la cellule courante
        this.jTable.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (isSorted())
                    return;

                if (e.getKeyChar() == '+') {
                    deplacerDe(1);
                } else if (e.getKeyChar() == '-') {
                    deplacerDe(-1);
                }
            }
        });

        final JScrollPane scrollPane = new JScrollPane(this.jTable);
        scrollPane.setFocusable(false);

        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
        this.add(this.filter, c);
        c.weighty = 1;
        c.gridy++;
        this.add(scrollPane, c);

        // destroy if non displayable
        this.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0)
                    dispChanged();
            }
        });
    }

    protected synchronized final void invertDebug() {
        this.setDebug(!this.debugFilter);
    }

    protected synchronized final void setDebug(boolean b) {
        this.debugFilter = b;
        updateFilter();
    }

    // thread-safe
    private synchronized void updateFilter() {
        if (this.filterWorker != null) {
            this.filterWorker.cancel(true);
        }
        final FilterWorker worker;
        if (!this.hasRequest()) {
            worker = new RowFilterWorker(null);
        } else if (this.debugFilter) {
            worker = new WhereFilterWorker(this.getRequest().getInstanceWhere());
        } else {
            worker = new RowFilterWorker(this.getRequest().getFilterRows());
        }
        this.filterWorker = worker;
        this.filterWorker.execute();
    }

    /**
     * Sets the filter label.
     * 
     * @param text the text to display, <code>null</code> to hide the label.
     */
    private void setFilter(String text) {
        this.filter.setText(text == null ? "" : text);
        this.filter.setVisible(text != null);
        this.revalidate();
    }

    public void selectID(final int id) {
        this.selectIDs(Collections.singleton(id));
    }

    public void selectIDs(final Collection<Integer> ids) {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("not in EDT");
        // no need to put a runnable in the model queue to wait for an inserted ID to actually
        // show up in the list, the ListSelectionState will record the userID and select it after
        // the update
        if (!isDead())
            this.state.selectIDs(ids);
    }

    // retourne l'ID de la ligne rowIndex à l'écran.
    public int idFromIndex(int rowIndex) {
        return this.state.idFromIndex(rowIndex);
    }

    /**
     * Cherche une chaîne de caractères dans la liste et reclasse les éléments trouvés au début
     * 
     * @param s la chaîne de caractères recherchées
     * @param column la colonne dans laquelle chercher, <code>null</code> pour toutes.
     */
    public void search(String s, String column) {
        this.search(s, column, null);
    }

    public void search(String s, String column, Runnable r) {
        // Determine sur quelle colonne on cherche
        this.getModel().search(SearchList.singleton(ColumnSearchSpec.create(s, this.getModel().getColumnNames().indexOf(column))), r);
    }

    // Export en tableau OpenOffice
    public void exporter(File file) throws IOException {
        exporter(file, false, XMLVersion.getDefault());
    }

    public File exporter(File file, final boolean onlySelection, final XMLVersion version) throws IOException {
        return SpreadSheet.export(getExportModel(onlySelection), file, version);
    }

    protected TableModel getExportModel(final boolean onlySelection) {
        final ViewTableModel res;
        final String appName = Configuration.getInstance() == null ? null : Configuration.getInstance().getAppName();
        final boolean isGestioNX = appName != null && appName.startsWith("OpenConcerto");
        if (isGestioNX) {
            res = new ViewTableModel(this.jTable) {
                @Override
                public Object getValueAt(int rowIndex, int columnIndex) {
                    final Object value = super.getValueAt(rowIndex, columnIndex);
                    if (value instanceof Long || value instanceof BigInteger) {
                        return new Double(((Number) value).longValue() / 100.0);
                    } else {
                        return value;
                    }
                }
            };
        } else
            res = new ViewTableModel(this.jTable);
        return onlySelection ? new TableModelSelectionAdapter(res, this.jTable.getSelectedRows()) : res;
    }

    public void update() {
        this.getModel().updateAll();
    }

    /**
     * Retourne le nombre de ligne de cette liste.
     * 
     * @return le nombre de ligne de cette liste.
     */
    public int getRowCount() {
        return this.getTableModel().getRowCount();
    }

    public int getTotalRowCount() {
        // happens when we're dead
        if (isDead()) {
            return this.getRowCount();
        }
        return this.getModel().getTotalRowCount();
    }

    public final boolean isDead() {
        return this.getTableModel() == null;
    }

    /**
     * Retourne le nombre d'éléments contenu dans cette liste. C'est à dire la somme du champs
     * 'quantité' ou 'nombre d'essai DDR'.
     * 
     * @return la somme ou -1 s'il n'y a pas de champs quantité.
     */
    public int getItemCount() {
        int count = -1;
        if (!this.isDead()) {
            int fieldIndex = -1;
            // ATTN ne marche que si qte est dans les listFields, donc dans le tableModel
            // sinon on pourrait faire un SUM(QUANTITE)
            final SQLField qte;
            final SQLTable t = this.getModel().getTable();
            if (t.contains("QUANTITE"))
                qte = t.getField("QUANTITE");
            else
                qte = t.getFieldRaw("NB_ESSAI_DDR");

            if (qte != null) {
                final SQLTableModelSource src = this.getModel().getReq();
                int i = 0;
                for (final SQLTableModelColumn col : src.getColumns()) {
                    if (CollectionUtils.getSole(col.getFields()) == qte)
                        fieldIndex = i;
                    i++;
                }
            }
            if (fieldIndex > 0) {
                count = 0;
                for (int j = 0; j < this.getTableModel().getRowCount(); j++) {
                    count += ((Number) this.getTableModel().getValueAt(j, fieldIndex)).intValue();
                }
            }
        }
        return count;
    }

    public void deplacerDe(final int inc) {
        this.getModel().moveBy(this.getSelectedId(), inc);
    }

    /**
     * The currently selected id.
     * 
     * @return the currently selected id or -1 if no selection.
     */
    public int getSelectedId() {
        return this.state.getSelectedID();
    }

    public final boolean hasSelection() {
        return this.jTable.getSelectedRow() >= 0;
    }

    public final ListSelection getSelection() {
        return this.state;
    }

    /**
     * Return the line at the passed index.
     * 
     * @param viewIndex the index in the JTable.
     * @return the line at the passed index.
     * @see ITableModel#getLine(TableModel, int)
     */
    public final ListSQLLine getLine(int viewIndex) {
        return ITableModel.getLine(this.getJTable().getModel(), viewIndex);
    }

    private SQLRow getRow(int id) {
        if (id < SQLRow.MIN_VALID_ID) {
            return null;
        } else
            return this.getSource().getPrimaryTable().getRow(id);
    }

    public SQLRow getSelectedRow() {
        return this.getRow(this.getSelectedId());
    }

    public final SQLRow getDesiredRow() {
        return this.getRow(this.getSelection().getUserSelectedID());
    }

    public final void setAdjustVisible(boolean b) {
        this.adjustVisible = b;
    }

    protected final void toggleAutoAdjust() {
        if (this.tcsa == null) {
            this.tcsa = new ColumnSizeAdjustor(this.jTable);
        } else {
            this.tcsa.setInstalled(!this.tcsa.isInstalled());
        }
    }

    public final boolean isAutoAdjusting() {
        if (this.tcsa == null) {
            return false;
        } else
            return this.tcsa.isInstalled();
    }

    // *** Listeners ***//

    public void addIListener(IListener l) {
        this.listeners.add(l);
    }

    public void addNonAdjustingIListener(IListener l) {
        this.naListeners.add(l);
    }

    public void addListener(TableModelListener l) {
        this.jTable.getModel().addTableModelListener(l);
    }

    /**
     * To be notified when the table is being sorted. Each time a sort is requested you'll be
     * notified twice to indicate the beginning and end of the sort. Don't confuse it with the
     * sortED status.
     * 
     * @param l the listener.
     * @see #isSorted()
     */
    public void addSortListener(PropertyChangeListener l) {
        this.sorter.addPropertyChangeListener(new PropertyChangeListenerProxy("sorting", l));
    }

    /**
     * Whether this list is sorted by a column.
     * 
     * @return true if this list is sorted.
     */
    public boolean isSorted() {
        return this.sorter.isSorting();
    }

    private void fireSelectionId(int id, int selectedColumn) {
        for (IListener l : this.listeners) {
            l.selectionId(id, selectedColumn);
        }
    }

    protected final void fireNASelectionId() {
        final int id = this.getSelectedId();
        for (IListener l : this.naListeners) {
            l.selectionId(id, -1);
        }
    }

    public final void addModelListener(final PropertyChangeListener l) {
        this.supp.addPropertyChangeListener("model", l);
    }

    public final void rmModelListener(final PropertyChangeListener l) {
        this.supp.removePropertyChangeListener("model", l);
    }

    /**
     * Ensure that the passed listener will always listen on our current {@link #getModel() model}
     * even if it changes. Warning: to signal model change
     * {@link PropertyChangeListener#propertyChange(PropertyChangeEvent)} will be called with a
     * <code>null</code> name.
     * 
     * @param l the listener.
     */
    public final void addListenerOnModel(final PropertyChangeListener l) {
        this.modelPCListeners.add(l);
        if (getModel() != null)
            getModel().addPropertyChangeListener(l);
    }

    public final void rmListenerOnModel(final PropertyChangeListener l) {
        this.modelPCListeners.remove(l);
        if (getModel() != null)
            getModel().rmPropertyChangeListener(l);
    }

    // *** Ancestors ***//

    public void ancestorAdded(AncestorEvent event) {
        if (event.getAncestor().isVisible())
            this.getModel().setSleeping(false);
    }

    public void ancestorRemoved(AncestorEvent event) {
        // test isDead() since in JComponent.removeNotify() first setDisplayable(false) (in super)
        // then firePropertyChange("ancestor", null).
        if (!this.isDead() && !event.getAncestor().isVisible())
            this.getModel().setSleeping(true);
    }

    public void ancestorMoved(AncestorEvent event) {
        // nothing to do
    }

    public void setSQLEditable(boolean b) {
        this.getModel().setEditable(b);
    }

    /**
     * The {@link ITableModel} of this list.
     * 
     * @return the model, <code>null</code> if destroyed.
     */
    public ITableModel getModel() {
        return (ITableModel) this.getTableModel();
    }

    public TableModel getTableModel() {
        return this.sorter.getTableModel();
    }

    private final void setTableModel(ITableModel t) {
        final ITableModel old = this.getModel();
        if (t == old)
            return;

        if (old != null) {
            for (final PropertyChangeListener l : this.modelPCListeners)
                old.rmPropertyChangeListener(l);
            if (this.hasRequest())
                this.getRequest().rmWhereListener(this.filterListener);
        }
        this.sorter.setTableModel(t);
        if (t != null) {
            // no need to listen to source columns since our ITableModel does, then it
            // fireTableStructureChanged() and our JTable createDefaultColumnsFromModel() so
            // columnAdded() and thus updateCols() are called. Note: we might want to listen to
            // SQLTableModelColumn themselves (and not their list), e.g. if their renderers change.
            for (final PropertyChangeListener l : this.modelPCListeners) {
                t.addPropertyChangeListener(l);
                // signal to the listeners that the model has changed (ie all of its properties)
                l.propertyChange(new PropertyChangeEvent(t, null, null, null));
            }
            if (this.hasRequest()) {
                this.getRequest().addWhereListener(this.filterListener);
                // the where might have changed since we last listened
                this.filterListener.propertyChange(null);
            }
        }
        this.supp.firePropertyChange("model", old, t);
    }

    // must be called when columnModel or getSource() changes
    private void updateCols(final int index) {
        final TableColumnModel columnModel = this.jTable.getColumnModel();
        final int start = index < 0 ? 0 : index;
        final int stop = index < 0 ? columnModel.getColumnCount() : index + 1;
        for (int i = start; i < stop; i++) {
            final TableColumn col = columnModel.getColumn(i);
            final SQLTableModelColumn srcCol = this.getSource().getColumn(i);
            srcCol.install(col);
            if (FORCE_ALT_CELL_RENDERER)
                AlternateTableCellRenderer.setRendererAndListen(col);
            else
                AlternateTableCellRenderer.setRenderer(col);
        }
    }

    public final boolean hasRequest() {
        return this.getSource() instanceof SQLTableModelSourceOnline;
    }

    public final ListSQLRequest getRequest() {
        // TODO a superclass of ListSQLRequest for use in SQLTableModelSource
        // our clients always use either setWhere() or setSelTransf()
        // also add the ability to Offline to respect the filter
        return ((SQLTableModelSourceOnline) this.getSource()).getReq();
    }

    public final void setRequest(ListSQLRequest listReq) {
        // a ListSQLRequest can be changed with setWhere()/setFilterEnable(), so copy it
        this.setSource(new SQLTableModelSourceOnline(listReq));
    }

    public final void setSource(SQLTableModelSource src) {
        if (src == null)
            throw new NullPointerException();
        // necessary to limit table model changes, since it recreates columns (and thus forget about
        // customizations, eg renderers)
        if (this.src == src)
            return;

        this.src = src;
        this.setTableModel(new ITableModel(src));
    }

    public final SQLTableModelSource getSource() {
        final ITableModel m = this.getModel();
        return m == null ? null : m.getReq();
    }

    public final File getConfigFile() {
        // can be null if this is called before the end of the constructor
        return this.tableStateManager == null ? null : this.tableStateManager.getConfigFile();
    }

    public final void setConfigFile(final File configFile) {
        final File oldFile = this.getConfigFile();
        if (!CompareUtils.equals(oldFile, configFile)) {
            if (configFile == null)
                this.tableStateManager.endAutoSave();
            this.tableStateManager.setConfigFile(configFile);
            if (oldFile == null)
                this.tableStateManager.beginAutoSave();
            loadTableState();
        }
    }

    private void loadTableState() {
        if (this.getConfigFile() != null)
            this.tableStateManager.loadState();
    }

    /**
     * Allow this list to be garbage collected. This method is necessary since this instance is
     * listener of SQLTable which will never be gc'd.
     */
    private final void dispChanged() {
        if (!this.isDisplayable()) {
            this.setTableModel(null);
        } else {
            this.setSource(this.src);
        }
    }

    public JTable getJTable() {
        return this.jTable;
    }

    public void grabFocus() {
        this.jTable.grabFocus();
    }

    // *** workers

    private abstract class FilterWorker extends SwingWorker<String, Object> {

        @Override
        protected final void done() {
            if (!this.isCancelled()) {
                // if doInBackground() wasn't cancelled, display our result
                try {
                    setFilter(this.get());
                } catch (Exception e) {
                    if (e instanceof ExecutionException && ((ExecutionException) e).getCause() instanceof InterruptedException) {
                        final String msg = this.getClass() + " interruped";
                        Log.get().fine(msg);
                        setFilter(msg);
                    } else {
                        e.printStackTrace();
                        setFilter(e.getLocalizedMessage());
                    }
                }
                synchronized (IListe.this) {
                    // only doInBackground() can be cancelled, so this might have received cancel()
                    // after doInBackground() had completed but before done() had been called
                    // thus filterWorker is not always this instance
                    if (IListe.this.filterWorker == this) {
                        IListe.this.filterWorker = null;
                    }
                }
            }
        }

    }

    private final class WhereFilterWorker extends FilterWorker {
        private final Where w;

        private WhereFilterWorker(Where r) {
            this.w = r;
        }

        @Override
        protected String doInBackground() throws InterruptedException {
            return this.w == null ? "No where" : this.w.getClause();
        }

    }

    private final class RowFilterWorker extends FilterWorker {
        private final Collection<SQLRow> rows;

        private RowFilterWorker(Collection<SQLRow> r) {
            this.rows = r;
        }

        @Override
        protected String doInBackground() throws InterruptedException {
            if (this.getRows() == null)
                return null;

            // attend 1 peu avant de faire des requetes, comme ca si le filtre change
            // tout le temps, on ne commence meme pas (sleep jette InterruptedExn)
            Thread.sleep(60);

            final List<String> ancestors = new ArrayList<String>();
            final SQLElementDirectory dir = Configuration.getInstance().getDirectory();
            // always put the description of getRows(), but only put their ancestor if they all have
            // the same parent
            Tuple2<SQLRow, String> parentAndDesc = getParent(this.getRows(), dir);
            ancestors.add(parentAndDesc.get1());
            SQLRow current = parentAndDesc.get0();
            while (current != null) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                final SQLElement elem = dir.getElement(current.getTable());
                ancestors.add(0, elem.getDescription(current));
                current = elem.getParent(current);
            }

            return CollectionUtils.join(ancestors, SEP);
        }

        private Tuple2<SQLRow, String> getParent(Collection<SQLRow> rows, final SQLElementDirectory dir) throws InterruptedException {
            SQLRow parent = null;
            boolean sameParent = true;
            final List<String> desc = new ArrayList<String>(rows.size());

            for (final SQLRow current : rows) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                final SQLElement elem = dir.getElement(current.getTable());
                if (parent == null || sameParent) {
                    final SQLRow currentParent = elem.getParent(current);
                    if (parent == null)
                        parent = currentParent;
                    else if (!parent.equals(currentParent))
                        sameParent = false;
                }
                desc.add(elem.getDescription(current));
            }

            return Tuple2.create(sameParent ? parent : null, CollectionUtils.join(desc, " ●"));
        }

        private final Collection<SQLRow> getRows() {
            return this.rows;
        }

        @Override
        public String toString() {
            return super.toString() + " on " + this.getRows();
        }
    }
}
