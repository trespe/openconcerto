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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLType;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.group.Group;
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
    private final Map<String, String> docs = new HashMap<String, String>();

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
        layout(this.group, 0, LayoutHints.DEFAULT_GROUP_HINTS, 0, 0, c);
    }

    public int layout(final Group currentGroup, final Integer order, final LayoutHints size, int x, final int level, final GridBagConstraints c) {
        if (currentGroup.isEmpty()) {
            System.out.print(" (" + x + ")");
            final String id = currentGroup.getId();
            System.out.print(order + " " + id + "[" + size + "]");
            c.gridwidth = 1;
            if (size.showLabel()) {
                c.weightx = 0;
                // Label
                if (size.isSeparated()) {
                    c.gridx = 0;
                    c.gridwidth = 4;
                    c.fill = GridBagConstraints.NONE;
                } else {
                    c.fill = GridBagConstraints.HORIZONTAL;
                }
                this.add(getLabel(id), c);
                if (size.isSeparated()) {
                    c.gridy++;
                } else {
                    c.gridx++;
                }
            }
            // Editor
            final JComponent editor = getEditor(id);

            if (size.maximizeWidth() && size.maximizeHeight()) {
                c.fill = GridBagConstraints.BOTH;
            } else if (size.maximizeWidth()) {
                c.fill = GridBagConstraints.HORIZONTAL;
            } else if (size.maximizeHeight()) {
                c.fill = GridBagConstraints.VERTICAL;
            } else {
                c.fill = GridBagConstraints.NONE;
                DefaultGridBagConstraints.lockMinimumSize(editor);
            }
            if (size.fill()) {
                c.weighty = 1;
                c.gridwidth = this.columns * 2;
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
            c.weighty = 0;
            c.gridx++;
            if ((x % this.columns) != 0) {
                c.gridx = 0;
                c.gridy++;
            }
        }
        if (size.isSeparated()) {
            x = 0;
            c.gridx = 0;
            c.gridy++;
        }
        final int stop = currentGroup.getSize();
        for (int i = 0; i < stop; i++) {
            final Group subGroup = currentGroup.getGroup(i);
            final Integer subGroupOrder = currentGroup.getOrder(i);
            final LayoutHints subGroupSize = currentGroup.getLayoutHints(i);
            x = layout(subGroup, subGroupOrder, subGroupSize, x, level + 1, c);

        }

        if (currentGroup.isEmpty()) {
            x++;
        } else {
            if (size.maximizeWidth()) {
                c.gridx = 0;
                c.gridy++;
            }
        }

        return x;
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

    public JComponent createLabel(final String id) {
        final String fieldLabel = super.getLabelFor(id);
        JLabel jLabel;
        if (fieldLabel == null) {
            jLabel = new JLabel(id);
            jLabel.setForeground(Color.RED.darker());

        } else {
            jLabel = new JLabel(fieldLabel);
        }
        jLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        registerPopupMenu(jLabel, id);
        final String doc = ((PropsConfiguration) Configuration.getInstance()).getMetadata().getDocumentation(this.getCode(), this.group.getId(), id);
        setDocumentation(jLabel, id, doc);
        jLabel.setToolTipText(doc);
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
                            new DocumentationEditorFrame(GroupSQLComponent.this, id, getDocumentation(id)).setVisible(true);

                        }
                    });

                    popMenu.add(menuItemDoc);
                    popMenu.show(label, e.getX(), e.getY());
                }

            }

        });

    }

    public void setDocumentation(final JComponent jLabel, final String id, final String text) {
        this.docs.put(id, text);
        if (text != null) {
            jLabel.setToolTipText(text);
        }
    }

    public void setDocumentation(final String id, final String text) {
        setDocumentation(getLabel(id), id, text);
    }

    public void saveDocumentation(final String id, final String text) {
        setDocumentation(id, text);
        ((PropsConfiguration) Configuration.getInstance()).getMetadata().setDocumentation(this.getElement().getCode(), this.getCode(), this.group.getId(), id, text);
    }

    public String getDocumentation(final String id) {
        return this.docs.get(id);
    }

    public JComponent getLabel(final String id) {
        JComponent label = this.labels.get(id);
        if (label == null) {
            label = createLabel(id);
            this.labels.put(id, label);
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
