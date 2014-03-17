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
 
 package org.openconcerto.ui.list;

import org.openconcerto.ui.table.IconTableCellRenderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class ItemListCellRenderer extends JPanel implements ListCellRenderer {
    private final HighLightableJLabel label1 = new HighLightableJLabel();
    private final HighLightableJLabel label2 = new HighLightableJLabel();
    private Icon icon;
    private int vGap;
    private int hGap;

    public ItemListCellRenderer() {
        this(2, 4);
    }

    public ItemListCellRenderer(int hGap, int vGap) {
        this.hGap = hGap;
        this.vGap = vGap;
        this.setLayout(null);
        this.add(label1);
        this.add(label2);
    }

    public void setHighLightedText(String str) {
        label1.setHightlight(str);
        label2.setHightlight(str);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (icon != null) {
            final int x = this.getWidth() - icon.getIconWidth() - hGap;
            final int y = (this.getHeight() - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g, x, y);
        }
    }

    @Override
    public void doLayout() {
        this.label1.setBounds(hGap, vGap, this.getWidth() - this.getHeight(), this.getHeight() / 2);
        this.label2.setBounds(2 * hGap, this.getHeight() / 2, this.getWidth() - this.getHeight(), this.getHeight() / 2);
    }

    @Override
    public void setFont(Font font) {
        if (label1 != null) {
            label1.setFont(font);
            label2.setFont(font);
        }
        super.setFont(font);
    }

    @Override
    public Dimension getPreferredSize() {
        int h = 4 * vGap + label1.getPreferredSize().height + label2.getPreferredSize().height;
        int w = 2 * hGap + Math.max(label1.getPreferredSize().width, label2.getPreferredSize().width) + h;
        return new Dimension(w, h);
    }

    @Override
    public void setForeground(Color fg) {
        if (label1 != null) {
            label1.setForeground(fg);
            if (fg.equals(Color.BLACK)) {
                label2.setForeground(Color.GRAY);
            } else {
                label2.setForeground(fg.brighter().brighter());
            }
        }
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        final ListItem item = (ListItem) value;
        Color bg = null;
        Color fg = null;
        if (isSelected) {
            setBackground(bg == null ? list.getSelectionBackground() : bg);
            setForeground(fg == null ? list.getSelectionForeground() : fg);
            final Color brighterColor = getBackground().brighter();
            label1.setHighLightColor(brighterColor);
            label2.setHighLightColor(brighterColor);
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            label1.setHighLightColor(HighLightableJLabel.DEFAULT_COLOR);
            label2.setHighLightColor(HighLightableJLabel.DEFAULT_COLOR);
        }

        setEnabled(list.isEnabled());
        setFont(list.getFont());
        this.label1.setText(item.getTitle());
        this.label2.setText(item.getComment());
        this.icon = item.getIcon();

        return this;
    }

    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JFrame f = new JFrame();
        ListItem[] data = new ListItem[2];
        data[0] = new ListItem("Hello", "World comment", null);
        data[1] = new ListItem("Hello World", "How are you today?", new ImageIcon(IconTableCellRenderer.class.getResource("okay.png")));
        final JList contentPane = new JList(data);
        final ItemListCellRenderer cellRenderer = new ItemListCellRenderer();
        cellRenderer.setHighLightedText("orl");
        contentPane.setCellRenderer(cellRenderer);

        contentPane.setFont(contentPane.getFont().deriveFont(24f));
        f.setContentPane(contentPane);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

}
