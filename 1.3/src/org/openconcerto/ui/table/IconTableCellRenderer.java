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
 
 package org.openconcerto.ui.table;

import org.openconcerto.utils.JImage;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class IconTableCellRenderer extends AbstractCellEditor implements TableCellEditor, TableCellRenderer, ItemListener {

    private int va;
    private final List<JImage> images = new Vector<JImage>();
    IconCellRenderer renderer;

    /**
     * @param icons list de String, ex: c:\niceicon.png
     */
    public IconTableCellRenderer(List<URL> icons) {
        for (int i = 0; i < icons.size(); i++) {
            URL filenameUrl = icons.get(i);
            // System.out.println("Chargement de l'image " + filename);

            JImage img = new JImage(filenameUrl);
            img.setCenterImage(true);
            img.check();
            img.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    super.mousePressed(e);
                    System.out.println(e);
                    IconTableCellRenderer.this.va++;
                    if (IconTableCellRenderer.this.va >= IconTableCellRenderer.this.images.size()) {
                        IconTableCellRenderer.this.va = 0;
                    }

                    stopCellEditing();
                }
            });
            this.images.add(img);
        }
        this.renderer = new IconCellRenderer(this.images);
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.va = ((Integer) value).intValue();
        final JImage image = this.images.get(this.va);
        return image;
    }

    public Object getCellEditorValue() {
        return new Integer(this.va);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return this.renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    public void itemStateChanged(ItemEvent e) {
        System.out.println("IconTableCellRenderer.itemStateChanged()");
    }

}

class IconCellRenderer extends DefaultTableCellRenderer {
    List<JImage> images;

    public IconCellRenderer(List<JImage> images) {
        super();
        this.images = images;
        this.setHorizontalAlignment(CENTER);
        this.setBorder(null);
        this.setIconTextGap(0);
        this.setHorizontalTextPosition(0);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        int val = ((Integer) value).intValue();
        final JImage image = this.images.get(val);
        this.setIcon(image.getImageIcon());
        TableCellRendererUtils.setColors(image, table, isSelected);
        TableCellRendererUtils.setColors(this, table, isSelected);
        return this;
    }

}
