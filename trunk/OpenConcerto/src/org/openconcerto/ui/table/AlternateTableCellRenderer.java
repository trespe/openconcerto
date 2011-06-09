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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * A renderer that color odd lines (à la iTunes).
 * 
 * @author Sylvain
 */
public class AlternateTableCellRenderer implements TableCellRenderer {

    public static final Color COLOR_LIGHT_GRAY = new Color(243, 243, 243);
    public static final Color DEFAULT_BG_COLOR = Color.WHITE;
    /** Default map from white to gray */
    public static final Map<Color, Color> DEFAULT_MAP = Collections.singletonMap(DEFAULT_BG_COLOR, COLOR_LIGHT_GRAY);

    private static final String ODD_BG_COLOR_MAP_PROP = AlternateTableCellRenderer.class.getSimpleName() + " odd background colour map";

    public static void setBGColorMap(final JComponent comp, final Color evenColor, final Color oddColor) {
        setBGColorMap(comp, Collections.singletonMap(evenColor, oddColor));
    }

    /**
     * Indicate which alternate background colour to use for odd rows. The {@link #DEFAULT_MAP} is
     * always set first.
     * 
     * @param comp the renderer component.
     * @param m a map from even colours to odd colours.
     */
    public static void setBGColorMap(final JComponent comp, final Map<Color, Color> m) {
        final Map<Color, Color> res = new HashMap<Color, Color>(DEFAULT_MAP);
        res.putAll(m);
        comp.putClientProperty(ODD_BG_COLOR_MAP_PROP, res);
    }

    @SuppressWarnings("unchecked")
    private static Map<Color, Color> getBGColorMap(final JComponent comp) {
        return (Map<Color, Color>) comp.getClientProperty(ODD_BG_COLOR_MAP_PROP);
    }

    private static Color getAlternateColor(Color c) {
        return new Color((int) (c.getRed() * .9), (int) (c.getGreen() * .9), (int) (c.getBlue() * .9));
    }

    private static final ProxyComp altComp = new ProxyComp();

    private static final class ProxyComp extends JComponent {

        private Component comp;
        private JComponent jComp;
        private Map<Color, Color> oddBGColorMap;

        public ProxyComp setComp(Component comp) {
            if (this.comp != comp) {
                this.comp = comp;
                this.jComp = comp instanceof JComponent ? (JComponent) comp : null;
            }
            final Map<Color, Color> jCompMap = this.jComp == null ? null : getBGColorMap(this.jComp);
            this.oddBGColorMap = jCompMap == null ? DEFAULT_MAP : jCompMap;
            return this;
        }

        private Color getAlternateColor(final Color c) {
            final Color res = this.oddBGColorMap.get(c);
            return res != null ? res : AlternateTableCellRenderer.getAlternateColor(c);
        }

        @Override
        public void paint(Graphics g) {
            final Color origBG = this.comp.getBackground();
            final Color altBG = this.getAlternateColor(origBG);

            boolean wasDoubleBuffered = false;
            if (this.jComp != null && this.jComp.isDoubleBuffered()) {
                wasDoubleBuffered = true;
                this.jComp.setDoubleBuffered(false);
            }

            // can't rely on *validate() since this.comp isn't displayed
            if (this.getWidth() != this.comp.getWidth() || this.getHeight() != this.comp.getHeight()) {
                this.comp.setSize(this.getWidth(), this.getHeight());
                this.comp.doLayout();
            }

            this.comp.setBackground(altBG);
            this.comp.paint(g);
            this.comp.setBackground(origBG);

            if (wasDoubleBuffered) {
                this.jComp.setDoubleBuffered(true);
            }
        }
    };

    /**
     * Set the cell renderer of all the columns of <code>jTable</code> to an
     * AlternateTableCellRenderer.
     * 
     * @param jTable the wannabe iTunes jTable.
     */
    static public void setAllColumns(final JTable jTable) {
        for (int i = 0; i < jTable.getColumnModel().getColumnCount(); i++) {
            setRenderer(jTable.getColumnModel().getColumn(i));
        }
    }

    private static final PropertyChangeListener RENDERER_L = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if ("cellRenderer".equals(evt.getPropertyName())) {
                setRenderer((TableColumn) evt.getSource());
            }
        }
    };

    static public final void setRendererAndListen(final TableColumn col) {
        // handle calling more than once
        col.removePropertyChangeListener(RENDERER_L);
        setRenderer(col);
        col.addPropertyChangeListener(RENDERER_L);
    }

    static public final void setRenderer(final TableColumn col) {
        final TableCellRenderer currentRenderer = col.getCellRenderer();
        if (!(currentRenderer instanceof AlternateTableCellRenderer))
            col.setCellRenderer(new AlternateTableCellRenderer(currentRenderer));
    }

    static public void clearColumns(JTable jTable) {
        for (int i = 0; i < jTable.getColumnModel().getColumnCount(); i++) {
            final TableColumn tc = jTable.getColumnModel().getColumn(i);
            tc.setCellRenderer(null);
        }
    }

    static public AlternateTableCellRenderer createDefault() {
        return new AlternateTableCellRenderer(new DefaultTableCellRenderer());
    }

    private final TableCellRenderer renderer;

    public AlternateTableCellRenderer() {
        this(null);
    }

    public AlternateTableCellRenderer(TableCellRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        TableCellRenderer tableRenderer = this.getRenderer(table, row, column);
        if (tableRenderer instanceof IAlternateTableCellRenderer) {
            return ((IAlternateTableCellRenderer) tableRenderer).getAlternateTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        } else {
            final Component comp = tableRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isSelected || row % 2 == 0) {
                return comp;
            } else {
                return altComp.setComp(comp);
            }
        }
    }

    private TableCellRenderer getRenderer(JTable table, int row, int column) {
        return this.renderer == null ? table.getDefaultRenderer(table.getColumnClass(column)) : this.renderer;
    }
}
