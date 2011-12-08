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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableEvent.Mode;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.sql.sqlobject.IComboSelectionItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.DefaultTableCellRenderer;

public class KeyTableCellRenderer extends DefaultTableCellRenderer {

    private String lastStringValue;
    private Object toSelect;
    private boolean isLoading = false;
    private final SQLElement el;
    static private final Map<SQLElement, Map<Integer, IComboSelectionItem>> cacheMap = new HashMap<SQLElement, Map<Integer, IComboSelectionItem>>();

    public KeyTableCellRenderer(final SQLElement el) {
        super();
        this.el = el;

        if (cacheMap.get(this.el) == null) {
            loadCacheAsynchronous();
        }
    }

    public void setValue(Object value) {

        if (this.isLoading) {
            this.toSelect = value;
            setText("Chargement ...");
            return;
        }

        String newValue = "id non trouvé pour:" + value;
        if (value == null)
            return;
        try {

            if (value instanceof SQLRowValues) {
                newValue = ((SQLRowValues) value).getString("CODE");
            } else {

                int id = Integer.parseInt(value.toString());

                if (id > 1) {
                    IComboSelectionItem item = cacheMap.get(this.el).get(id);
                    if (item != null) {
                        newValue = item.getLabel();
                    }
                } else {
                    newValue = SQLTableElement.UNDEFINED_STRING;
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();

        }

        if (!newValue.equals(this.lastStringValue)) {

            this.lastStringValue = newValue;
            setText(newValue);

        }
    }

    private void loadCacheAsynchronous() {
        this.isLoading = true;
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                List<IComboSelectionItem> items = KeyTableCellRenderer.this.el.getComboRequest().getComboItems();
                final Map<Integer, IComboSelectionItem> m = new HashMap<Integer, IComboSelectionItem>();
                for (IComboSelectionItem comboSelectionItem : items) {
                    m.put(comboSelectionItem.getId(), comboSelectionItem);
                }
                cacheMap.put(KeyTableCellRenderer.this.el, m);
                KeyTableCellRenderer.this.el.getTable().addPremierTableModifiedListener(new SQLTableModifiedListener() {
                    @Override
                    public void tableModified(SQLTableEvent evt) {
                        final int id = evt.getId();
                        if (evt.getMode() == Mode.ROW_DELETED)
                            m.remove(id);
                        else
                            m.put(id, KeyTableCellRenderer.this.el.getComboRequest().getComboItem(id));
                    }
                });

                KeyTableCellRenderer.this.isLoading = false;
                if (KeyTableCellRenderer.this.toSelect != null) {
                    setValue(KeyTableCellRenderer.this.toSelect);
                }
            }
        });
        thread.start();
    }
}
