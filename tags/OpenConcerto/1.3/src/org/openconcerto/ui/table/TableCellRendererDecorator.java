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

import org.openconcerto.utils.cc.IPredicate;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.Component;
import java.lang.reflect.Constructor;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * A renderer that decorates another one.
 * 
 * @author Sylvain
 */
public abstract class TableCellRendererDecorator implements TableCellRenderer {

    static private final <T extends TableCellRendererDecorator> ITransformer<TableCellRenderer, T> getTransf(final Class<T> clazz) {
        final Constructor<T> ctor;
        try {
            ctor = clazz.getConstructor(TableCellRenderer.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Missing TableCellRenderer constructor in " + clazz);
        }
        return new ITransformer<TableCellRenderer, T>() {
            @Override
            public T transformChecked(TableCellRenderer input) {
                try {
                    return ctor.newInstance(input);
                } catch (Exception e) {
                    throw new IllegalStateException("Couldn't create a " + clazz + " with " + input);
                }
            }
        };
    }

    // syntactic sugar
    static protected final <T extends TableCellRendererDecorator> TableCellRendererDecoratorUtils<T> createUtils(final Class<T> clazz) {
        return new TableCellRendererDecoratorUtils<T>(clazz);
    }

    static public class TableCellRendererDecoratorUtils<T extends TableCellRendererDecorator> {

        private final Class<T> clazz;
        private final ITransformer<TableCellRenderer, T> transf;
        private final T nullRenderer;

        protected TableCellRendererDecoratorUtils(final Class<T> clazz) {
            this(clazz, getTransf(clazz));
        }

        protected TableCellRendererDecoratorUtils(final Class<T> clazz, final ITransformer<TableCellRenderer, T> transf) {
            super();
            this.clazz = clazz;
            this.transf = transf;
            this.nullRenderer = transf.transformChecked(null);
        }

        protected final T getDecoratedRenderer(final TableCellRenderer currentRenderer) {
            final T res = currentRenderer == null ? this.nullRenderer : this.transf.transformChecked(currentRenderer);
            assert res != null;
            return res;
        }

        /**
         * Set the cell renderer of all the columns of <code>jTable</code> to an instance of
         * <code>T</code>.
         * 
         * @param jTable the jTable which will have its columns changed.
         * @see #setRenderer(TableColumn)
         */
        public final void setAllColumns(final JTable jTable) {
            for (int i = 0; i < jTable.getColumnModel().getColumnCount(); i++) {
                setRenderer(jTable.getColumnModel().getColumn(i));
            }
        }

        /**
         * Change the renderer of <code>col</code> if it doesn't already use a
         * TableCellRendererDecorator of this class.
         * 
         * @param col the column to change.
         */
        public final void setRenderer(final TableColumn col) {
            final TableCellRenderer currentRenderer = col.getCellRenderer();
            final TableCellRenderer newR = getRenderer(currentRenderer);
            if (currentRenderer != newR)
                col.setCellRenderer(newR);
        }

        public final TableCellRenderer getRenderer(final TableCellRenderer currentRenderer) {
            return getRenderer(currentRenderer, new IPredicate<TableCellRenderer>() {
                @Override
                public boolean evaluateChecked(TableCellRenderer input) {
                    return replaces(input);
                }
            });
        }

        public final boolean alreadyDecorates(final TableCellRenderer currentRenderer) {
            TableCellRenderer r = currentRenderer;
            boolean decorates = false;
            while (!decorates && r != null) {
                if (doesTheSame(r))
                    decorates = true;
                r = r instanceof TableCellRendererDecorator ? ((TableCellRendererDecorator) r).renderer : null;
            }
            return decorates;
        }

        // walk through the decorators of currentRenderer adding this renderer if necessary
        private final TableCellRenderer getRenderer(final TableCellRenderer currentRenderer, final IPredicate<? super TableCellRenderer> toReplace) {
            TableCellRenderer res = currentRenderer;
            TableCellRenderer r = currentRenderer;
            TableCellRendererDecorator previousR = null;
            boolean needToCreate = true;
            // create only one renderer
            while (needToCreate && r != null) {
                final TableCellRendererDecorator decorator = r instanceof TableCellRendererDecorator ? (TableCellRendererDecorator) r : null;
                final TableCellRenderer next = decorator != null ? decorator.renderer : null;
                if (doesTheSame(r)) {
                    needToCreate = false;
                } else if (toReplace.evaluateChecked(r)) {
                    final T created = getDecoratedRenderer(next);
                    if (previousR == null)
                        res = created;
                    else
                        previousR.setRenderer(created);
                    needToCreate = false;
                }
                previousR = decorator;
                r = next;
            }
            return needToCreate ? getDecoratedRenderer(res) : res;
        }

        // if true a new TableCellRendererDecorator won't be created
        protected boolean doesTheSame(final TableCellRenderer r) {
            return r.getClass() == this.clazz;
        }

        // if true r will be replaced by a new instance of this decorator
        protected boolean replaces(final TableCellRenderer r) {
            return false;
        }
    }

    static public void clearColumns(JTable jTable) {
        for (int i = 0; i < jTable.getColumnModel().getColumnCount(); i++) {
            final TableColumn tc = jTable.getColumnModel().getColumn(i);
            tc.setCellRenderer(null);
        }
    }

    // Allow to use <code>renderer</code> for more than one column : ignore unselectedForeground &&
    // unselectedBackground.
    static public TableCellRendererDecorator tableColorsRenderer(DefaultTableCellRenderer renderer) {
        if (renderer == null)
            throw new NullPointerException();
        return new TableCellRendererDecorator(renderer) {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component comp = getRenderer(table, column).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (table.getDropLocation() == null && !isSelected)
                    TableCellRendererUtils.setColors(comp, table, isSelected);
                return comp;
            }
        };
    }

    private TableCellRenderer renderer;

    public TableCellRendererDecorator() {
        this(null);
    }

    public TableCellRendererDecorator(TableCellRenderer renderer) {
        this.renderer = renderer;
    }

    // ATTN do not use the same DefaultTableCellRenderer (like the defaults in JTable) decorated and
    // bare. DefaultTableCellRenderer overload set*Color() to store unselected colors, so if a
    // decorator modify them, the bare one will use the same colors. See tableColorsRenderer().
    protected final TableCellRenderer getRenderer(JTable table, int column) {
        return this.renderer == null ? table.getDefaultRenderer(table.getColumnClass(column)) : this.renderer;
    }

    protected final void setRenderer(TableCellRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " on <" + this.renderer + ">";
    }
}
