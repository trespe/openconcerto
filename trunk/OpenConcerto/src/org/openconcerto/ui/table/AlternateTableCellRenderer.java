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

import org.openconcerto.ui.component.ComponentWrapper;

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
public class AlternateTableCellRenderer extends TableCellRendererDecorator {

    private static final Color COLOR_LIGHT_GRAY = new Color(233, 237, 243);
    private static final Color DEFAULT_BG_COLOR = Color.WHITE;
    private static Map<Color, Color> DEFAULT_MAP;

    public static void setDefaultMap(Map<Color, Color> m) {
        DEFAULT_MAP = m == null ? Collections.singletonMap(DEFAULT_BG_COLOR, COLOR_LIGHT_GRAY) : Collections.unmodifiableMap(new HashMap<Color, Color>(m));
    }

    static {
        setDefaultMap(null);
    }

    private static final String ODD_BG_COLOR_MAP_PROP = AlternateTableCellRenderer.class.getSimpleName() + " odd background colour map";

    public static void setBGColorMap(final JComponent comp, final Color evenColor, final Color oddColor) {
        setBGColorMap(comp, Collections.singletonMap(evenColor, oddColor));
    }

    /**
     * Indicate which alternate background colour to use for odd rows.
     * 
     * @param comp the renderer component.
     * @param m a map from even colours to odd colours.
     */
    public static void setBGColorMap(final JComponent comp, final Map<Color, Color> m) {
        comp.putClientProperty(ODD_BG_COLOR_MAP_PROP, m);
    }

    @SuppressWarnings("unchecked")
    private static Map<Color, Color> getBGColorMap(final JComponent comp) {
        return (Map<Color, Color>) comp.getClientProperty(ODD_BG_COLOR_MAP_PROP);
    }

    private static Color computeAlternateColor(Color c) {
        return new Color((int) (c.getRed() * .9), (int) (c.getGreen() * .9), (int) (c.getBlue() * .9));
    }

    public static Color getAlternateColor(Color c) {
        return getAlternateColor(c, DEFAULT_MAP);
    }

    private static Color getAlternateColor(Color c, Map<Color, Color> oddColorMap) {
        final Color res = oddColorMap.get(c);
        if (res != null)
            return res;
        else if (oddColorMap != DEFAULT_MAP)
            return getAlternateColor(c, DEFAULT_MAP);
        else
            return computeAlternateColor(c);
    }

    private static final ProxyComp altComp = new ProxyComp();

    private static final class ProxyComp extends JComponent implements ComponentWrapper.I {

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

        @Override
        public Component getComponent() {
            return this.comp;
        }

        private Color getAlternateColor(final Color c) {
            return AlternateTableCellRenderer.getAlternateColor(c, this.oddBGColorMap);
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

    static public final TableCellRendererDecoratorUtils<AlternateTableCellRenderer> UTILS = createUtils(AlternateTableCellRenderer.class);

    private static final PropertyChangeListener RENDERER_L = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if ("cellRenderer".equals(evt.getPropertyName())) {
                UTILS.setRenderer((TableColumn) evt.getSource());
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
        UTILS.setRenderer(col);
    }

    static public AlternateTableCellRenderer createDefault() {
        return new AlternateTableCellRenderer(new DefaultTableCellRenderer());
    }

    public AlternateTableCellRenderer() {
        this(null);
    }

    public AlternateTableCellRenderer(TableCellRenderer renderer) {
        super(renderer);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        TableCellRenderer tableRenderer = this.getRenderer(table, column);
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
}
