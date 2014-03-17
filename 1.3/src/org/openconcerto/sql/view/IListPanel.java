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

import static javax.swing.JOptionPane.DEFAULT_OPTION;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import org.openconcerto.openoffice.ContentType;
import org.openconcerto.openoffice.OOUtils;
import org.openconcerto.openoffice.XMLFormatVersion;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.TM;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.sql.users.rights.TableAllRights;
import org.openconcerto.sql.users.rights.UserRights;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.sql.view.search.SearchListComponent;
import org.openconcerto.ui.ContinuousButtonModel;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.SwingThreadUtils;
import org.openconcerto.ui.component.JRadioButtons.JStringRadioButtons;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.change.ListChangeIndex;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Un panel affichant une liste et des boutons pour la manipuler.
 * 
 * @author ILM Informatique 11 juin 2004
 */
abstract public class IListPanel extends JPanel implements ActionListener {

    /**
     * System property to control the clone button. The value can be either <code>true</code>, in
     * which case the button will always appear, or it can be a list of table names.
     */
    public static final String CAN_CLONE = "org.openconcerto.sql.canCloneInList";

    static private final String EXPORT_DIR_KEY = "exportDir";

    static public final File getConfigFile(final SQLElement elem, final Class<? extends Container> c) {
        return getConfigFile(elem, c, null);
    }

    static public final File getConfigFile(final SQLElement elem, final Class<? extends Container> c, final String variant) {
        final Configuration conf = Configuration.getInstance();
        if (conf == null)
            return null;
        final String suffix = variant == null || variant.length() == 0 ? "" : "-" + variant;
        return new File(conf.getConfDir(), "state-" + c.getSimpleName() + "-list" + File.separator + elem.getPluralName() + suffix + ".xml");
    }

    private final IListe liste;

    protected final SQLElement element;
    protected final BtnTooltipMnger btnMngr;

    protected JButton buttonActualiser;

    boolean selectRowOnAdd = true;

    // partagé par ListModifyFrame
    protected JButton buttonModifier;
    protected JButton buttonEffacer;
    protected JButton buttonAjouter;
    protected JButton buttonClone;
    protected JButton saveBtn;
    private JButton buttonPlus;
    private JButton buttonMoins;
    protected final JPanel searchPanel = new JPanel(new GridBagLayout());
    private static final Icon UP_ARROW = new ImageIcon(IListPanel.class.getResource("fleche_haut.png"));
    private static final Icon DOWN_ARROW = new ImageIcon(IListPanel.class.getResource("fleche_bas.png"));

    private static final JButton createBtn(Icon i) {
        final JButton res = new JButton(i);
        res.setMargin(new Insets(1, 1, 1, 1));
        res.setModel(new ContinuousButtonModel(300));
        res.setBorder(BorderFactory.createEmptyBorder());
        res.setOpaque(false);
        res.setFocusPainted(true);
        res.setContentAreaFilled(false);
        return res;
    }

    protected EditFrame createFrame;

    protected SearchListComponent searchComponent;

    public IListPanel(SQLElement elem) {
        this(elem, null);
    }

    public IListPanel(SQLElement elem, IListe list) {
        this(elem, list, null);
    }

    /**
     * Create a new instance. Often several panels for the same element are needed, in this case
     * <code>variant</code> should be used to identify them, this allows each panel to have its own
     * {@link IListe#getConfigFile() state}.
     * 
     * @param elem the element that will be displayed.
     * @param list the list to use, if <code>null</code> <code>elem</code> will configure a new one.
     * @param variant this parameter should identify each panel on the same element, can be
     *        <code>null</code>.
     */
    public IListPanel(SQLElement elem, IListe list, String variant) {
        this.element = elem;
        this.btnMngr = new BtnTooltipMnger();
        // if the same conf is needed for subclasses, use IListPanel.class
        final File config = getConfigFile(this.getElement(), this.getClass(), variant);
        if (list == null) {
            list = new IListe(this.getElement().getTableSource(), config);
        } else {
            if (list.getSource().getPrimaryTable() != elem.getTable())
                throw new IllegalArgumentException("Different tables : " + elem.getTable() + " != " + list.getSource().getPrimaryTable());
            if (list.getConfigFile() == null)
                list.setConfigFile(config);
        }
        this.liste = list;
        final IClosure<ListChangeIndex<IListeAction>> l = new IClosure<ListChangeIndex<IListeAction>>() {
            @Override
            public void executeChecked(ListChangeIndex<IListeAction> input) {
                getListe().removeIListeActions(input.getItemsRemoved());
                getListe().addIListeActions(input.getItemsAdded());
            }
        };
        // remove listener if non displayable since getElement() never dies
        this.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0)
                    if (isDisplayable()) {
                        getListe().addIListeActions(getElement().getRowActions());
                        getElement().addRowActionsListener(l);
                    } else {
                        getElement().removeRowActionsListener(l);
                        getListe().removeIListeActions(getElement().getRowActions());
                    }
            }
        });

        this.init();
    }

    protected final void init() {
        this.uiInit();

        // on écoute pour mettre à jour les boutons effacer et modifier
        this.liste.addIListener(new IListener() {
            @Override
            public void selectionId(int id, int field) {
                IListPanel.this.listSelectionChanged(id);
            }
        });
        this.liste.addSelectionDataListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                listSelectionDataChanged();
            }
        });

        this.liste.addModelListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                IListPanel.this.searchComponent.reset((ITableModel) evt.getNewValue());
            }
        });
        // selectID() alone won't init us if NONEXISTANT_ID is already the selected id
        this.btnMngr.updateBtns();
        this.getListe().selectID(SQLRow.NONEXISTANT_ID);
    }

    final private void uiInit() {
        this.createUI();

        Container container = this;
        container.setLayout(new GridBagLayout());
        final GridBagConstraints c = createConstraints();
        c.weighty = 1;
        container.add(this.liste, c);
        c.gridy++;

        c.weighty = 0;
        c.insets = new Insets(4, 1, 2, 1);
        container.add(this.getMiddlePanel(), c);

        this.addComponents(container, c);
        this.setOpaque(false);
    }

    /**
     * The constraints used to add the list and its panel. The {@link #getListe() list} will be
     * added at the coordinates returned, and the {@link #getMiddleCompsLayout() panel} will be
     * added underneath (i.e. gridy++).
     * 
     * @return the constraints.
     */
    protected GridBagConstraints createConstraints() {
        final GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        return c;
    }

    protected void createUI() {
        this.setOpaque(false);
        this.buttonActualiser = new JButton(new ImageIcon(IListPanel.class.getResource("reload.png")));
        this.buttonActualiser.setBorderPainted(false);
        this.buttonActualiser.setFocusPainted(false);
        this.buttonActualiser.setOpaque(false);
        this.buttonActualiser.setContentAreaFilled(false);
        this.buttonActualiser.setMinimumSize(new Dimension(20, 20));
        this.buttonActualiser.setPreferredSize(new Dimension(20, 20));
        this.buttonActualiser.setMaximumSize(new Dimension(20, 20));
        this.searchComponent = new SearchListComponent(this.liste.getModel());
        this.searchComponent.setFormats(this.liste.getSearchFormats());

        this.buttonModifier = new JButton(TM.tr("modify"));
        this.buttonModifier.setOpaque(false);
        this.btnMngr.addBtn(this.buttonModifier, "noRightToModify", TableAllRights.MODIFY_ROW_TABLE, true, modifyIsImmediate());
        this.buttonEffacer = new JButton(TM.tr("remove"));
        this.buttonEffacer.setOpaque(false);
        this.btnMngr.addBtn(this.buttonEffacer, "noRightToDel", TableAllRights.DELETE_ROW_TABLE, true);
        this.buttonAjouter = new JButton(TM.tr("add"));
        this.buttonAjouter.setOpaque(false);
        this.btnMngr.addBtn(this.buttonAjouter, "noRightToAdd", TableAllRights.ADD_ROW_TABLE, false, false);
        this.buttonClone = new JButton(TM.tr("duplicate"));
        this.buttonClone.setOpaque(false);
        this.btnMngr.addBtn(this.buttonClone, "noRightToClone", TableAllRights.ADD_ROW_TABLE, true, false);
        this.btnMngr.setOKToolTip(this.buttonClone, TM.tr("listPanel.cloneToolTip"));

        this.saveBtn = new JButton(new ImageIcon(IListPanel.class.getResource("save.png")));
        this.saveBtn.setFocusPainted(false);
        this.saveBtn.setOpaque(false);
        this.saveBtn.setContentAreaFilled(false);
        this.saveBtn.setBorderPainted(false);
        this.saveBtn.setMinimumSize(new Dimension(20, 20));
        this.saveBtn.setPreferredSize(new Dimension(20, 20));
        this.saveBtn.setMaximumSize(new Dimension(20, 20));
        this.buttonMoins = createBtn(UP_ARROW);
        this.buttonPlus = createBtn(DOWN_ARROW);

        // needSelection = false since we handle it with the transformer
        this.btnMngr.addBtn(this.buttonMoins, "noRightToReorder", TableAllRights.MODIFY_ROW_TABLE, false);
        this.btnMngr.addBtn(this.buttonPlus, "noRightToReorder", TableAllRights.MODIFY_ROW_TABLE, false);
        final ITransformer<JButton, String> transf = new ITransformer<JButton, String>() {
            @Override
            public String transformChecked(JButton input) {
                final boolean ok = getListe().hasSelection() && !getListe().isSorted();
                // keep them enabled when armed otherwise they will be disabled when used
                // since they refresh the list which in turn does a clearSelection()
                return input.getModel().isArmed() || ok ? null : TM.tr("listPanel.noSelectionOrSort");
            }
        };
        this.btnMngr.setAdditional(this.buttonMoins, transf);
        this.btnMngr.setAdditional(this.buttonPlus, transf);

        this.searchPanel.setOpaque(false);
        // ne pas permettre de changer l'ordre quand on trie
        this.getListe().addSortListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateOrderButtons();
            }
        });
        if (Boolean.getBoolean("org.openconcerto.listpanel.simpleui")) {
            this.setAdjustVisible(false);
            this.setSearchFullMode(false);
        } else {
            this.searchPanel.setBorder(BorderFactory.createEtchedBorder());
        }
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 2, 0, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;

        final JLabel label = new JLabel(TM.tr("search"));
        label.setOpaque(false);
        this.searchPanel.add(label, c);
        c.gridx++;

        c.weightx = 1;
        this.searchComponent.setOpaque(false);
        this.searchPanel.setOpaque(false);
        this.searchPanel.add(this.searchComponent, c);

    }

    protected abstract boolean modifyIsImmediate();

    /**
     * Permet aux sous classes d'ajouter d'autres composants.
     * 
     * @param container le conteneur dans lequel ajouter.
     * @param c les contraintes actuelles.
     */
    protected void addComponents(Container container, GridBagConstraints c) {
        // par défaut ne fait rien
    }

    protected Object[] getMiddleCompsLayout() {
        final JComponent[] comps = { this.buttonPlus, this.buttonMoins, this.buttonActualiser, canSave() ? this.saveBtn : null, this.searchPanel, this.buttonAjouter,
                canClone() ? this.buttonClone : null, this.buttonModifier, this.buttonEffacer };
        // le champ de recherche prend toute la largeur disponible
        return new Object[] { comps, this.searchPanel };
    }

    private boolean canClone() {
        final String prop = System.getProperty(CAN_CLONE, "");
        return Boolean.parseBoolean(prop) || SQLRow.toList(prop).contains(getElement().getTable().getName());
    }

    private boolean canSave() {
        // TODO use default right from UserRightsManager (see issue #79)
        final String prop = System.getProperty("org.openconcerto.sql.canSaveInList", "true");
        return Boolean.parseBoolean(prop) || TableAllRights.currentUserHasRight(TableAllRights.SAVE_ROW_TABLE, getElement().getTable());
    }

    final private JPanel getMiddlePanel() {
        JPanel container = new JPanel();
        container.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 2, 0, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        Object[] middleCompsL = this.getMiddleCompsLayout();
        JComponent[] comps = (JComponent[]) middleCompsL[0];
        JComponent largest = (JComponent) middleCompsL[1];
        for (int i = 0; i < comps.length; i++) {
            final JComponent component = comps[i];
            if (component != null) {
                c.weightx = (component == largest) ? 1 : 0;
                container.add(component, c);
                if (component instanceof JButton)
                    ((JButton) component).addActionListener(this);
                c.gridx++;
            }
        }
        container.setOpaque(false);
        return container;
    }

    /**
     * Recherche s dans ce panneau.
     * 
     * @param s la chaine à rechercher.
     */
    public void search(String s) {
        this.searchComponent.setSearchString(s);
    }

    /**
     * Recherche <code>s</code> dans ce panneau et s'assure que <code>r</code> soit exécuté dans la
     * thread Swing une fois la recherche effectuée.
     * 
     * @param s la chaine à rechercher.
     * @param r le runnable à exécuter.
     */
    public synchronized void search(final String s, final Runnable r) {
        this.search(s);
        // attendre les modifs en cours (update de la base + recherche que l'on vient d'effectuer)
        this.getListe().getModel().invokeLater(r);
    }

    public final void actionPerformed(ActionEvent e) {
        this.handleAction((JButton) e.getSource(), e);
    }

    private final int askSerious(Object msg, String title) {
        return JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    }

    private final JPanel createClonePanel(final int selectedLines, final boolean rec, final SQLRequestComboBox combo) {
        final String msg = TM.getInstance().trM("listPanel.cloneRows", "rowCount", selectedLines, "rec", rec);

        final JPanel p = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(2, 2, 2, 2);

        p.add(new JLabel(msg), c);

        if (combo != null) {
            c.gridx = 0;
            c.gridy++;
            p.add(new JLabel(TM.tr("clone.newPlace")), c);

            c.gridy++;
            combo.uiInit(getElement().getParentElement().getComboRequest());
            combo.setPreferredSize(new Dimension(600, combo.getPreferredSize().height));
            p.add(combo, c);
        }

        return p;
    }

    protected void handleAction(JButton source, ActionEvent evt) {
        if (source == this.buttonMoins) {
            this.getListe().deplacerDe(-getInc(evt));
        } else if (source == this.buttonPlus) {
            this.getListe().deplacerDe(getInc(evt));
        } else if (source == this.buttonActualiser) {
            this.liste.update();
        } else if (source == this.buttonAjouter) {
            final boolean deaf = isDeaf();
            // toujours remplir la createFrame avec la ligne sélectionnée
            // car la frame écoute la sélection mais pas les modif, et se reset qd on la ferme
            // donc si on clic ajouter, on ferme, on modif la ligne, on clic ajouter
            // on doit reremplir l'EditFrame
            final int selectedId = this.getListe().getSelectedId();
            if (!deaf && this.selectRowOnAdd && selectedId >= 0) {
                this.getCreateFrame().selectionId(selectedId);
            }
            FrameUtil.show(this.getCreateFrame());
        } else if (source == this.buttonClone) {
            final List<Integer> selectedIDs = this.getListe().getSelection().getSelectedIDs();
            final boolean rec = (evt.getModifiers() & ActionEvent.CTRL_MASK) != 0;
            // on Ubuntu ALT-Click is used to move windows
            final boolean showParent = (evt.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
            final SQLRequestComboBox combo = showParent && getElement().getParentElement() != null ? new SQLRequestComboBox() : null;
            if (askSerious(createClonePanel(selectedIDs.size(), rec, combo), TM.tr("duplication")) == JOptionPane.YES_OPTION) {
                final SQLRow parent = combo == null ? null : combo.getSelectedRow();
                for (final int id : selectedIDs) {
                    final SQLRow row = this.getElement().getTable().getRow(id);
                    try {
                        if (rec)
                            this.getElement().copyRecursive(row, parent);
                        else
                            this.getElement().copy(row, parent);
                    } catch (SQLException e) {
                        ExceptionHandler.handle(this, TM.tr("listPanel.duplicationError", row), e);
                    }
                }
            }
        } else if (source == this.buttonEffacer) {
            this.getElement().askArchive(this, this.getListe().getSelection().getSelectedIDs());
        } else if (source == this.saveBtn) {
            try {
                final String allRows = TM.tr("listPanel.wholeList");
                final String selectedRows = TM.tr("listPanel.selection");
                final JStringRadioButtons radios = new JStringRadioButtons(false, Arrays.asList(allRows, selectedRows));
                // we rarely mean to save one row or less
                radios.setValue(this.getListe().getSelection().getSelectedIDs().size() <= 1 ? allRows : selectedRows);
                final JPanel p = new JPanel(new BorderLayout());
                p.add(new JLabel(TM.tr("export")), BorderLayout.PAGE_START);
                p.add(radios, BorderLayout.LINE_START);
                final Object[] options = { TM.tr("open"), TM.tr("save") + "...", TM.tr("cancel") };
                final int answer = JOptionPane.showOptionDialog(this, p, TM.tr("listPanel.export"), DEFAULT_OPTION, QUESTION_MESSAGE, null, options, options[0]);
                if (answer == 0 || answer == 1) {
                    final boolean tmp = answer == 0;
                    final XMLFormatVersion version = XMLFormatVersion.getDefault();
                    final String prefix = this.element.getPluralName().replace('/', '-');
                    final String suffix = "." + ContentType.SPREADSHEET.getVersioned(version.getXMLVersion()).getExtension();
                    final File file;
                    if (tmp) {
                        file = File.createTempFile(prefix, suffix);
                    } else {
                        final FileDialog fd = new FileDialog(SwingThreadUtils.getAncestorOrSelf(Frame.class, this), TM.tr("listPanel.save"), FileDialog.SAVE);
                        final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
                        fd.setDirectory(prefs.get(EXPORT_DIR_KEY, Configuration.getInstance().getWD().getAbsolutePath()));
                        fd.setFile(prefix + suffix);
                        fd.setVisible(true);
                        if (fd.getFile() != null) {
                            file = new File(fd.getDirectory(), fd.getFile());
                            prefs.put(EXPORT_DIR_KEY, fd.getDirectory());
                        } else {
                            file = null;
                        }
                    }
                    if (file != null) {
                        final File exportedFile = this.liste.exporter(file, radios.getValue().equals(selectedRows), version);
                        if (tmp) {
                            exportedFile.setWritable(false, false);
                            exportedFile.deleteOnExit();
                        }
                        OOUtils.open(exportedFile);
                    }
                }
            } catch (Exception e) {
                ExceptionHandler.handle(this, TM.tr("saveError"), e);
            }
        } else {
            throw new IllegalStateException("button '" + source.getText() + "' not implemented");
        }
    }

    /**
     * Whether the create panel listen to the selection of its list.
     * 
     * @return <code>true</code> if the create panel should be empty.
     */
    protected boolean isDeaf() {
        return Boolean.getBoolean("org.openconcerto.sql.listPanel.deafEditPanel");
    }

    protected final EditFrame getCreateFrame() {
        if (this.createFrame == null) {
            this.createFrame = new EditFrame(this.element, EditPanel.CREATION);
            this.createFrame.getPanel().setIList(this.getListe());
            if (!isDeaf()) {
                // la frame d'ajout se remplit suivant la sélection de cette frame
                this.liste.addIListener(this.createFrame);
            }
        }
        return this.createFrame;
    }

    // notre liste a changé de sélection
    protected void listSelectionChanged(int id) {
    }

    // selection or selection content changed
    protected void listSelectionDataChanged() {
        // even if the same row is selected, its content can change (e.g. get locked)
        this.btnMngr.updateBtns();
    }

    // handle enabled and tooltip properties of our buttons
    protected final class BtnTooltipMnger {

        private final Map<JButton, Tuple2<String, String>> code;
        private final Set<JButton> needSelection, needRWSelection;
        // btn -> tooltip
        private final Map<JButton, ITransformer<JButton, String>> additional;
        private final Map<JButton, String> okTooltip;

        public BtnTooltipMnger() {
            super();
            this.needSelection = new HashSet<JButton>();
            this.needRWSelection = new HashSet<JButton>();
            this.code = new HashMap<JButton, Tuple2<String, String>>();
            this.additional = new HashMap<JButton, ITransformer<JButton, String>>();
            this.okTooltip = new HashMap<JButton, String>();
        }

        public void addBtn(final JButton btn, String desc, String rightCode, final boolean needSelection) {
            this.addBtn(btn, desc, rightCode, needSelection, true);
        }

        public void addBtn(final JButton btn, String desc, String rightCode, final boolean needSelection, final boolean needRWSelection) {
            // otherwise have to remove from other attributes
            if (this.code.containsKey(btn))
                throw new IllegalStateException("already in");
            this.code.put(btn, Tuple2.create(desc, rightCode));
            if (needSelection)
                this.needSelection.add(btn);
            if (needRWSelection && getElement().getTable().contains(SQLComponent.READ_ONLY_FIELD))
                this.needRWSelection.add(btn);
        }

        public void setAdditional(final JButton btn, ITransformer<JButton, String> additional) {
            if (additional != null)
                this.additional.put(btn, additional);
            else
                this.additional.remove(btn);
        }

        public void setOKToolTip(final JButton btn, String tooltip) {
            if (tooltip != null)
                this.okTooltip.put(btn, tooltip);
            else
                this.okTooltip.remove(btn);
        }

        void updateBtn(final JButton btn) {
            if (!this.code.containsKey(btn))
                throw new IllegalArgumentException();
            this.updateBtns(Collections.singleton(btn));
        }

        void updateBtns() {
            this.updateBtns(this.code.keySet());
        }

        private void updateBtns(final Set<JButton> btns) {
            final boolean hasSelection = getListe().getSelectedId() >= SQLRow.MIN_VALID_ID;
            final UserRights rights = UserRightsManager.getCurrentUserRights();
            for (final JButton btn : btns) {
                final Tuple2<String, String> t = this.code.get(btn);

                final boolean ok;
                final String tooltip;
                if (!TableAllRights.hasRight(rights, t.get1(), getElement().getTable())) {
                    ok = false;
                    tooltip = TM.tr(t.get0());
                } else if (this.needRWSelection.contains(btn) && isRO()) {
                    ok = false;
                    tooltip = TM.tr("editPanel.readOnlySelection");
                } else if (this.needSelection.contains(btn) && !hasSelection) {
                    ok = false;
                    tooltip = TM.tr("noSelection");
                } else if (this.additional.containsKey(btn)) {
                    tooltip = this.additional.get(btn).transformChecked(btn);
                    ok = tooltip == null;
                } else {
                    ok = true;
                    tooltip = this.okTooltip.get(btn);
                }
                btn.setToolTipText(tooltip);
                btn.setEnabled(ok);
            }
        }

        private boolean isRO() {
            final SQLRowAccessor r = getListe().getSelectedRow();
            return r != null && SQLComponent.isReadOnly(r);
        }
    }

    protected final void updateOrderButtons() {
        this.btnMngr.updateBtn(this.buttonMoins);
        this.btnMngr.updateBtn(this.buttonPlus);
    }

    public final IListe getListe() {
        return this.liste;
    }

    public final SQLElement getElement() {
        return this.element;
    }

    /**
     * The SQLComponent inside this panel, if any.
     * 
     * @return our child component, or <code>null</code>.
     */
    protected SQLComponent getSQLComponent() {
        return null;
    }

    public abstract SQLComponent getModifComp();

    public final SQLComponent getAddComp() {
        return this.getCreateFrame().getSQLComponent();
    }

    public void grabFocus() {
        this.liste.grabFocus();
    }

    // compute the increment from the event
    private int getInc(ActionEvent evt) {
        // move only 1 by 1 for the first 3
        final int times = (int) evt.getWhen() - 3;
        return times < 2 ? 1 : (int) Math.pow(times, 2);
    }

    public void setUpAndDownVisible(boolean b) {
        this.buttonPlus.setVisible(b);
        this.buttonMoins.setVisible(b);
        // also disable move by drag and drop
        this.getListe().getJTable().setDragEnabled(b);
    }

    public void setAddVisible(boolean b) {
        this.buttonAjouter.setVisible(b);
    }

    public void setAdjustVisible(boolean b) {
        this.liste.setAdjustVisible(b);
    }

    public void setReloadVisible(boolean b) {
        this.buttonActualiser.setVisible(b);
    }

    public void setSaveVisible(boolean b) {
        this.saveBtn.setVisible(b);
    }

    public void setSearchVisible(boolean b) {
        this.searchPanel.setVisible(b);
    }

    public void setDeleteVisible(boolean b) {
        this.buttonEffacer.setVisible(b);
    }

    public void setModifyVisible(boolean b) {
        this.buttonModifier.setVisible(b);
    }

    public void setCloneVisible(boolean b) {
        this.buttonClone.setVisible(b);
    }

    public void setReadWriteButtonsVisible(final boolean b) {
        this.setUpAndDownVisible(b);
        this.setAddVisible(b);
        this.setDeleteVisible(b);
        this.setModifyVisible(b);
        this.setCloneVisible(b);
    }

    public void setSearchFullMode(boolean b) {
        if (b)
            this.searchPanel.setBorder(BorderFactory.createEtchedBorder());
        else
            this.searchPanel.setBorder(BorderFactory.createEmptyBorder());
        this.searchComponent.setSearchFullMode(b);
    }

    public void setRequest(ListSQLRequest req) {
        if (!req.getPrimaryTable().equals(this.getElement().getTable()))
            throw new IllegalArgumentException("table diff: " + req + " / " + this.getElement());
        this.getListe().setRequest(req);
    }

    /**
     * Récupérer les valeurs de la row sélectionnée lors de l'ajout
     * 
     * @param b
     */
    public void setSelectRowOnAdd(boolean b) {
        this.selectRowOnAdd = b;
    }

}
