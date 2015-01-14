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
 
 package org.openconcerto.sql.element;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.model.FieldMapper;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLType;
import org.openconcerto.sql.request.RowItemDesc;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;
import org.openconcerto.ui.group.LayoutHints;
import org.openconcerto.utils.i18n.TranslationManager;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class GroupSQLComponent extends BaseSQLComponent {

    private final Group group;
    private final int columns = 2;
    private final Map<String, JComponent> labels = new HashMap<String, JComponent>();
    private final Map<String, JComponent> editors = new HashMap<String, JComponent>();
    private String startTabAfter = null;
    private boolean tabGroup;
    private int tabDepth;
    private JTabbedPane pane;
    private final List<String> tabsGroupIDs = new ArrayList<String>();

    public GroupSQLComponent(final SQLElement element, final Group group) {
        super(element);
        this.group = group;
    }

    protected final Group getGroup() {
        return this.group;
    }

    public void startTabGroupAfter(String id) {
        startTabAfter = id;
    }

    @Override
    protected void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        this.tabGroup = false;
        this.tabDepth = 0;
        layout(this.group, 0, 0, 0, c, this);
    }

    public void layout(final Item currentItem, final Integer order, int x, final int level, GridBagConstraints c, JPanel panel) {
        final String id = currentItem.getId();

        final LayoutHints size = currentItem.getLocalHint();
        if (!size.isVisible()) {
            return;
        }

        if (size.isSeparated()) {
            x = 0;
            c.gridx = 0;
            c.gridy++;
        }
        if (currentItem instanceof Group) {
            final Group currentGroup = (Group) currentItem;
            final int stop = currentGroup.getSize();
            c.weighty = 0;
            if (this.tabGroup && level == this.tabDepth) {
                panel = new JPanel();
                panel.setLayout(new GridBagLayout());
                panel.setOpaque(false);
                c = new DefaultGridBagConstraints();
                x = 0;
                c.fill = GridBagConstraints.NONE;
                String label = TranslationManager.getInstance().getTranslationForItem(id);// getRIVDescForId(id).getLabel();
                if (label == null) {
                    label = id;
                }
                this.pane.addTab(label, panel);
                this.tabsGroupIDs.add(currentGroup.getId());
            } else {
                if (size.showLabel() && getLabel(id) != null) {
                    x = 0;
                    c.gridy++;
                    c.fill = GridBagConstraints.HORIZONTAL;
                    c.gridx = 0;
                    c.weightx = 1;
                    c.gridwidth = 4;
                    panel.add(getLabel(id), c);
                    c.gridy++;
                }
            }
            for (int i = 0; i < stop; i++) {
                final Item subGroup = currentGroup.getItem(i);
                final Integer subGroupOrder = currentGroup.getOrder(i);
                layout(subGroup, subGroupOrder, x, level + 1, c, panel);
            }
            if (this.tabGroup && level == this.tabDepth) {
                JPanel spacer = new JPanel();
                spacer.setOpaque(false);
                c.gridy++;
                c.weighty = 0.0001;
                panel.add(spacer, c);
            }

        } else {
            c.gridwidth = 1;
            if (size.showLabel()) {
                c.weightx = 0;
                c.weighty = 0;
                // Label
                if (size.isSeparated()) {
                    c.gridwidth = 4;
                    c.weightx = 1;
                    c.fill = GridBagConstraints.NONE;
                } else {
                    c.fill = GridBagConstraints.HORIZONTAL;
                }
                panel.add(getLabel(id), c);
                if (size.isSeparated()) {
                    c.gridy++;
                    c.gridx = 0;
                } else {
                    c.gridx++;
                }
            }
            // Editor
            final JComponent editor = getEditor(id);

            if (size.fillWidth() && size.fillHeight()) {
                c.fill = GridBagConstraints.BOTH;
            } else if (size.fillWidth()) {
                c.fill = GridBagConstraints.HORIZONTAL;
            } else if (size.fillHeight()) {
                c.fill = GridBagConstraints.VERTICAL;
            } else {
                c.fill = GridBagConstraints.NONE;
                DefaultGridBagConstraints.lockMinimumSize(editor);
            }
            if (size.fillHeight()) {
                c.weighty = 1;
            } else {
                c.weighty = 0;
            }
            if (size.largeWidth()) {
                if (size.isSeparated()) {
                    c.gridwidth = this.columns * 2;
                } else {
                    c.gridwidth = this.columns * 2 - 1;
                }
            } else {
                if (size.showLabel() && !size.isSeparated()) {
                    c.gridwidth = 1;
                } else {
                    c.gridwidth = 2;
                }
            }
            if (c.gridx % 2 == 1) {
                c.weightx = 1;
            }

            panel.add(editor, c);

            try {
                JComponent comp = editor;
                if (editor instanceof JScrollPane) {
                    JScrollPane pane = (JScrollPane) editor;
                    comp = (JComponent) pane.getViewport().getView();
                }
                this.addView(comp, id);
            } catch (final Exception e) {
                Log.get().warning(e.getMessage());
            }

            if (size.largeWidth()) {
                if (size.isSeparated()) {
                    c.gridx += 4;
                } else {
                    c.gridx += 3;
                }
            } else {
                c.gridx++;
            }

            if (c.gridx >= this.columns * 2) {
                c.gridx = 0;
                c.gridy++;
                x = 0;
            }

        }
        if (id.equals(startTabAfter)) {
            tabGroup = true;
            tabDepth = level;
            pane = new JTabbedPane();
            c.gridx = 0;
            c.gridy++;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.gridwidth = 4;
            panel.add(pane, c);
        }

    }

    public final void setTabEnabledAt(final String groupID, final boolean enabled) {
        this.pane.setEnabledAt(this.tabsGroupIDs.indexOf(groupID), enabled);
    }

    public final boolean isTabEnabledAt(final String groupID) {
        return this.pane.isEnabledAt(this.tabsGroupIDs.indexOf(groupID));
    }

    public final void selectTabEnabled() {
        final int index = this.pane.getSelectedIndex();
        if (!this.pane.isEnabledAt(index)) {
            final int count = this.pane.getTabCount();
            // 1 since index is disabled
            for (int i = 1; i < count; i++) {
                final int mod = (index + i) % count;
                if (this.pane.isEnabledAt(mod)) {
                    this.pane.setSelectedIndex(mod);
                    return;
                }
            }
        }
    }

    @Override
    public Component addView(JComponent comp, String id) {
        final FieldMapper fieldMapper = PropsConfiguration.getInstance().getFieldMapper();
        SQLField field = null;
        if (fieldMapper != null) {
            field = fieldMapper.getSQLFieldForItem(id);
        }
        // Maybe the id is a field name (deprecated)
        if (field == null) {
            field = this.getTable().getFieldRaw(id);

        }
        return super.addView(comp, field.getName());
    }

    public JComponent createEditor(final String id) {
        if (id.startsWith("(") && id.endsWith(")*")) {
            try {
                final String table = id.substring(1, id.length() - 2).trim();
                final String idEditor = GlobalMapper.getInstance().getIds(table).get(0) + ".editor";
                final Class<?> cl = (Class<?>) GlobalMapper.getInstance().get(idEditor);
                return (JComponent) cl.newInstance();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        final FieldMapper fieldMapper = PropsConfiguration.getInstance().getFieldMapper();
        SQLField field = null;
        if (fieldMapper != null) {
            field = fieldMapper.getSQLFieldForItem(id);
        }
        // Maybe the id is a field name (deprecated)
        if (field == null) {
            field = this.getTable().getFieldRaw(id);

        }
        if (field == null) {
            final JLabel jLabel = new JLabelBold("No field " + id);
            jLabel.setForeground(Color.RED.darker());
            String t = "<html>";

            final Set<SQLField> fields = this.getTable().getFields();

            for (final SQLField sqlField : fields) {
                t += sqlField.getFullName() + "<br>";
            }
            t += "</html>";
            jLabel.setToolTipText(t);
            return jLabel;
        }

        final SQLType type = field.getType();

        final JComponent comp;

        if (getElement().getPrivateElement(field.getName()) != null) {
            // private
            final SQLComponent sqlcomp = this.getElement().getPrivateElement(field.getName()).createDefaultComponent();
            final DefaultElementSQLObject dobj = new DefaultElementSQLObject(this, sqlcomp);
            dobj.setDecorated(false);
            dobj.showSeparator(false);
            DefaultGridBagConstraints.lockMinimumSize(sqlcomp);
            comp = dobj;
        } else if (field.isKey()) {
            // foreign

            final SQLElement foreignElement = getElement().getForeignElement(field.getName());
            if (foreignElement == null) {
                comp = new JLabelBold("no element for foreignd " + id);
                comp.setForeground(Color.RED.darker());
                Log.get().severe("no element for foreign " + field.getName());
            } else {
                comp = new ElementComboBox();
                ((ElementComboBox) comp).init(foreignElement);
            }
            comp.setOpaque(false);
        } else {
            if (Boolean.class.isAssignableFrom(type.getJavaType())) {
                // TODO hack to view the focus (should try to paint around the button)
                comp = new JCheckBox(" ");
                comp.setOpaque(false);
            } else if (Date.class.isAssignableFrom(type.getJavaType())) {
                comp = new JDate();
                comp.setOpaque(false);
            } else {
                comp = new JTextField(Math.min(30, type.getSize()));
            }
        }

        return comp;
    }

    protected JComponent createLabel(final String id) {
        final JLabel jLabel = new JLabel();
        jLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        registerPopupMenu(jLabel, id);
        return jLabel;
    }

    private void registerPopupMenu(final JComponent label, final String id) {
        label.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(final MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3 && e.getModifiersEx() == 128) {

                    final JPopupMenu popMenu = new JPopupMenu();
                    final JMenu menuItemInfo = new JMenu("Information");
                    menuItemInfo.add(new JMenuItem("id: " + id));
                    menuItemInfo.add(new JMenuItem("label: " + getLabel(id).getClass().getName()));
                    menuItemInfo.add(new JMenuItem("editor: " + getEditor(id).getClass().getName()));
                    popMenu.add(menuItemInfo);
                    final JMenuItem menuItemDoc = new JMenuItem("Modifier la documentation");
                    menuItemDoc.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(final ActionEvent e) {
                            new DocumentationEditorFrame(GroupSQLComponent.this, id).setVisible(true);

                        }
                    });

                    popMenu.add(menuItemDoc);
                    popMenu.show(label, e.getX(), e.getY());
                }

            }

        });

    }

    @Override
    protected void updateUI(String id, RowItemDesc desc) {
        super.updateUI(id, desc);
        updateUI(id, getLabel(id), desc, Color.RED.darker());
    }

    public JComponent getLabel(final String id) {
        JComponent label = this.labels.get(id);
        if (label == null) {
            label = createLabel(id);
            this.labels.put(id, label);

            final RowItemDesc rivDesc = getRIVDescForId(id);
            updateUI(id, rivDesc);
        }
        return label;
    }

    private RowItemDesc getRIVDescForId(final String id) {
        final FieldMapper fieldMapper = PropsConfiguration.getInstance().getFieldMapper();
        String t = TranslationManager.getInstance().getTranslationForItem(id);
        if (t != null) {
            return new RowItemDesc(t, t);
        }
        String fieldName = null;
        if (fieldMapper != null) {
            final SQLField sqlFieldForItem = fieldMapper.getSQLFieldForItem(id);
            if (sqlFieldForItem != null) {
                fieldName = sqlFieldForItem.getName();
            }
        }
        if (fieldName == null) {
            fieldName = id;
        }
        final RowItemDesc rivDesc = getRIVDesc(fieldName);
        return rivDesc;
    }

    public JComponent getEditor(final String id) {
        JComponent editor = this.editors.get(id);
        if (editor == null) {
            editor = createEditor(id);
            this.editors.put(id, editor);
        }
        return editor;
    }
}
