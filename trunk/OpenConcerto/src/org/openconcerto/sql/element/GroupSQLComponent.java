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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class GroupSQLComponent extends BaseSQLComponent {

    private final Group group;
    private final int columns = 2;
    private final boolean forceViewOnly = true;
    private final Map<String, JComponent> labels = new HashMap<String, JComponent>();
    private final Map<String, JComponent> editors = new HashMap<String, JComponent>();

    public GroupSQLComponent(final SQLElement element, final Group group) {
        super(element);
        this.group = group;

    }

    @Override
    protected void addViews() {
        this.group.dumpOneColumn();
        this.setLayout(new GridBagLayout());
        this.group.sort();
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        layout(this.group, 0, 0, 0, c);
    }

    public void layout(final Item currentItem, final Integer order, int x, final int level, final GridBagConstraints c) {
        final String id = currentItem.getId();
        final LayoutHints size = currentItem.getLocalHint();
        if (size.isSeparated()) {
            x = 0;
            c.gridx = 0;
            c.gridy++;
        }
        if (currentItem instanceof Group) {
            Group currentGroup = (Group) currentItem;
            final int stop = currentGroup.getSize();
            for (int i = 0; i < stop; i++) {
                final Item subGroup = currentGroup.getItem(i);
                final Integer subGroupOrder = currentGroup.getOrder(i);

                layout(subGroup, subGroupOrder, x, level + 1, c);

            }

        } else {
            System.out.print(" (" + x + ")");

            System.out.print(order + " " + id + "[" + size + "]");
            c.gridwidth = 1;
            if (size.showLabel()) {
                c.weightx = 0;
                c.weighty = 0;
                // Label
                if (size.isSeparated()) {
                    c.gridwidth = 4;
                    c.fill = GridBagConstraints.NONE;
                } else {
                    c.fill = GridBagConstraints.HORIZONTAL;
                }
                this.add(getLabel(id), c);
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
                c.gridwidth = 1;
            }
            if (c.gridx % 2 == 1) {
                c.weightx = 1;
            }
            this.add(editor, c);
            try {
                this.addView(editor, id);
            } catch (final Exception e) {
                System.err.println(e.getMessage());
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
    }

    public JComponent createEditor(final String id) {

        if (id.startsWith("(") && id.endsWith(")*")) {

            try {
                final String table = id.substring(1, id.length() - 2).trim();
                final String idEditor = GlobalMapper.getInstance().getIds(table).get(0) + ".editor";
                System.out.println("Editor: " + idEditor);
                final Class<?> cl = (Class<?>) GlobalMapper.getInstance().get(idEditor);
                return (JComponent) cl.newInstance();
            } catch (final Exception e) {
                e.printStackTrace();
            }

        }

        final SQLField field = this.getTable().getFieldRaw(id);
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
        // if (/* this.getMode().equals(Mode.VIEW) || */forceViewOnly) {
        // final JLabel jLabel = new JLabel();
        // jLabel.setForeground(Color.gray);
        // return jLabel;
        // }
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
            comp = new ElementComboBox();
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
            updateUI(id, getRIVDesc(id));
        }
        return label;
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
