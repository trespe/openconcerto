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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.table.AlternateTableCellRenderer;
import org.openconcerto.ui.table.TableCellRendererUtils;
import org.openconcerto.utils.CollectionUtils;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class CompteRowValuesRenderer extends DefaultTableCellRenderer {

    private static final Color red = new Color(255, 31, 52);
    private static final Color redLightGrey = new Color(240, 65, 85);
    private static final Color redGrey = new Color(224, 115, 137);
    private static final Color orange = new Color(255, 134, 62);
    private static final Color orangeGrey = new Color(255, 160, 110);
    private static final Color orangeLight = new Color(255, 201, 168);
    private boolean createAutoActive = false;
    private Map<String, Boolean> cache = new HashMap<String, Boolean>();

    static {
        final Thread th = new Thread(new Runnable() {

            @Override
            public void run() {
                final SQLTable table = Configuration.getInstance().getDirectory().getElement("COMPTE_PCE").getTable();
                SQLBackgroundTableCache.getInstance().add(table, 3600);
                // Force preload
                SQLBackgroundTableCache.getInstance().getCacheForTable(table);

            }
        });
        th.setDaemon(true);
        th.setPriority(Thread.MIN_PRIORITY);
        th.start();
    }

    public CompteRowValuesRenderer() {
        super();
        AlternateTableCellRenderer.setBGColorMap(this, CollectionUtils.createMap(orange, orangeGrey, red, redLightGrey));
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        TableCellRendererUtils.setColors(comp, table, isSelected);
        if (column == 0 && value != null && value instanceof String) {
            final String account = (String) value;
            final boolean exist;
            if (cache.get(account) == null) {
                exist = ComptePCESQLElement.isExist(account);
                cache.put(account, exist);
            } else {
                exist = cache.get(account);
            }
            if (!exist) {
                if (!isSelected) {
                    if (this.createAutoActive) {
                        comp.setBackground(orange);
                    } else {
                        comp.setBackground(red);
                    }
                } else {
                    if (this.createAutoActive) {
                        comp.setBackground(orangeLight);
                    } else {
                        comp.setBackground(redGrey);
                    }
                }
                comp.setForeground(Color.WHITE);
            }
        }
        return comp;
    }

    public void setCreateActive(boolean b) {
        this.createAutoActive = b;
    }

}
