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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLType;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.Tuple3;

public class GroupSQLComponent extends BaseSQLComponent {

    private Group group;
    private int columns = 2;
    private boolean forceViewOnly = true;

    public GroupSQLComponent(SQLElement element, Group group) {
        super(element);
        this.group = group;

    }

    @Override
    protected void addViews() {
        group.dumpOneColumn();
        this.setLayout(new GridBagLayout());
        group.sort();
        GridBagConstraints c = new DefaultGridBagConstraints();
        layout(group, 0, LayoutHints.DEFAULT_GROUP_HINTS, 0, 0, c);
    }

    public int layout(Group currentGroup, Integer order, LayoutHints size, int x, int level, GridBagConstraints c) {
        if (currentGroup.isEmpty()) {
            System.out.print(" (" + x + ")");
            String id = currentGroup.getId();
            System.out.print(order + " " + id + "[" + size + "]");
            c.gridwidth = 1;
            if (size.showLabel()) {
                c.weightx = 0;
                // Label
                if (size.separatedLabel()) {
                    c.gridx = 0;
                    c.gridwidth = 4;
                    c.fill = GridBagConstraints.NONE;
                } else {
                    c.fill = GridBagConstraints.HORIZONTAL;
                }
                this.add(getLabel(id), c);
                if (size.separatedLabel()) {
                    c.gridy++;
                } else {
                    c.gridx++;
                }
            }
            // Editor
            c.weightx = 1;
            if (size.maximizeWidth() && size.maximizeHeight()) {
                c.fill = GridBagConstraints.BOTH;
            } else if (size.maximizeWidth()) {
                c.fill = GridBagConstraints.HORIZONTAL;
            } else if (size.maximizeHeight()) {
                c.fill = GridBagConstraints.VERTICAL;
            } else {
                c.fill = GridBagConstraints.NONE;
            }
            if (size.fill()) {
                c.weighty = 1;
                c.gridwidth = columns * 2;
            }

            this.add(getEditor(id), c);
            c.weighty = 0;
            c.gridx++;
            if ((x % columns) != 0) {
                c.gridx = 0;
                c.gridy++;
            }
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

    JComponent getEditor(String id) {
        if (id.startsWith("(") && id.endsWith(")*")) {

            try {
                String table = id.substring(1, id.length() - 2).trim();
                String idEditor = ElementMapper.getInstance().getIds(table).get(0) + ".editor";
                System.out.println("Editor: " + idEditor);
                Class cl = (Class) ElementMapper.getInstance().get(idEditor);
                return (JComponent) cl.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        SQLField field = this.getTable().getFieldRaw(id);
        if (field == null) {
            final JLabel jLabel = new JLabelBold("No field " + id);
            jLabel.setForeground(Color.RED.darker());
            String t = "<html>";

            final Set<SQLField> fields = this.getTable().getFields();

            for (SQLField sqlField : fields) {
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

        Tuple2<JComponent, SQLType> r = getComp(id);
        final JComponent editorComp = r.get0();
        if (editorComp != null) {
            this.addView(editorComp, id);
            return editorComp;
        }

        return new JButton(id);
    }

    JLabel getLabel(String id) {
        final String fieldLabel = super.getLabelFor(id);
        JLabel jLabel;
        if (fieldLabel == null) {
            jLabel = new JLabel(id);
            jLabel.setForeground(Color.RED.darker());

        } else {
            jLabel = new JLabel(fieldLabel);
        }
        jLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        return jLabel;
    }
}
