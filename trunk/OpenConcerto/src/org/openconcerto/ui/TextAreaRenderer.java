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
 
 package org.openconcerto.ui;

import org.openconcerto.ui.table.TableCellRendererUtils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

public class TextAreaRenderer extends DefaultTableCellRenderer {

    public final static Color couleurFacture = new Color(225, 254, 207);
    public final static Color couleurFactureMore = new Color(215, 244, 197);
    public final static Color couleurFactureDark = couleurFacture.darker();

    public final static Color couleurBon = new Color(253, 243, 204);
    public final static Color couleurBonMore = new Color(243, 233, 194);
    public final static Color couleurBonDark = couleurBon.darker();

    final JTextArea area;
    private JTable currentTable;
    private int currentRow;

    public TextAreaRenderer() {
        if (UIManager.getLookAndFeel().getName().equalsIgnoreCase("nimbus")) {
            this.area = new JTextArea() {
                // remplace le bord blanc des JTextArea de Nimbus
                protected void paintComponent(java.awt.Graphics g) {
                    final Color c = g.getColor();
                    final Color background = getBackground();
                    g.setColor(background);
                    g.fillRect(0, 0, TextAreaRenderer.this.area.getWidth(), TextAreaRenderer.this.area.getHeight());
                    g.setColor(c);
                    super.paintComponent(g);
                };

            };
        } else {
            this.area = new JTextArea();
        }
        this.area.setLineWrap(true);
        this.area.setWrapStyleWord(true);
    }

    public Dimension getPreferredSize() {
        Dimension e = this.area.getPreferredSize();
        EnhancedTable eTable = (EnhancedTable) this.currentTable;
        Dimension r = new Dimension(e.width, Math.max(e.height, eTable.getMaxRowHeight(this.currentRow)));

        return r;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        TableCellRendererUtils.setColors(this, table, isSelected);
        this.currentTable = table;
        this.currentRow = row;

        this.setHorizontalAlignment(SwingConstants.LEFT);

        // Important: we need to clone the colors due to a bug of GTK L&F
        this.area.setForeground(new Color(this.getForeground().getRGB()));
        this.area.setBackground(new Color(this.getBackground().getRGB()));
        //
        this.area.setFont(this.getFont());
        this.area.setBorder(null);
        this.area.setMargin(null);
        this.area.setText(value == null ? "" : (String) value);
        EnhancedTable eTable = (EnhancedTable) table;
        //
        TableColumnModel columnModel = table.getColumnModel();
        // // This line was very important to get it working with JDK1.4
        this.area.setSize(columnModel.getColumn(column).getWidth(), 165863);
        //
        int height_wanted = (int) this.area.getPreferredSize().getHeight();
        //
        eTable.setPreferredRowHeight(row, column, height_wanted);
        //
        eTable.setRowHeight(row, eTable.getMaxRowHeight(row));

        return this.area;
    }

}
