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
 
 package org.openconcerto.sql.view.column;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.VFlowLayout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class ColumnView extends JPanel {
    private Column column;
    private GridBagConstraints c = new DefaultGridBagConstraints();
    private final int width;
    private final ColumnRowRenderer cRenderer;
    private final ColumnFooterRenderer fRenderer;
    private final JPanel spacer = new JPanel();

    ColumnView(Column column, int width, ColumnRowRenderer cRenderer, ColumnFooterRenderer fRenderer) {
        this.column = column;
        this.width = width;
        this.cRenderer = cRenderer;
        this.fRenderer = fRenderer;
        this.setLayout(new GridBagLayout());
        final JLabel header = new JLabelBold(column.getName());
        setMinimumSize(new Dimension(width, header.getMinimumSize().height));
        setPreferredSize(new Dimension(width, header.getMinimumSize().height));
        c.insets = new Insets(2, 4, 2, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        this.add(header, c);
        c.fill = GridBagConstraints.BOTH;
        c.gridy++;
        this.c.weighty = 1;
        spacer.setOpaque(false);
        this.add(spacer, c);

    }

    public void updateContent() {
        this.remove(this.spacer);
        JPanel content = new JPanel();
        content.setLayout(new VFlowLayout(VFlowLayout.TOP, 2, 3, true));
        content.setBackground(Color.WHITE);
        final List<? extends SQLRowAccessor> rows = column.getRows();
        for (SQLRowAccessor row : rows) {
            content.add(cRenderer.getRenderer(row, width - 8));
        }
        this.c.fill = GridBagConstraints.BOTH;

        this.c.gridy++;

        this.c.weighty = 1;
        final JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        this.add(scroll, c);
        this.c.gridy++;

        this.c.weighty = 0;
        this.add(fRenderer.getRenderer(rows, width), c);

    }
}
