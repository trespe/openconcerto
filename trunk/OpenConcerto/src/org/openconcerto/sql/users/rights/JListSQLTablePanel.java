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
 
 package org.openconcerto.sql.users.rights;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.sqlobject.IComboSelectionItem;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionListener;

/**
 * JList affichant l'ensemble des valeurs de la table
 * 
 * @author Ludo
 * 
 */
public class JListSQLTablePanel extends JPanel {

    private JListSQLTableModel listModel;
    private JList list;
    private SQLTable table;
    private final ListDataListener dataListener = new ListDataListener() {

        @Override
        public void intervalRemoved(ListDataEvent arg0) {
        }

        @Override
        public void intervalAdded(ListDataEvent arg0) {

        }

        @Override
        public void contentsChanged(ListDataEvent arg0) {
            if (JListSQLTablePanel.this.idToSelect > 0) {

                int id = JListSQLTablePanel.this.idToSelect;
                JListSQLTablePanel.this.idToSelect = -1;
                selectID(id);
            }
        }
    };

    public static ComboSQLRequest createComboRequest(final SQLTable table, boolean withUndef) {

        ComboSQLRequest request = Configuration.getInstance().getDirectory().getElement(table).getComboRequest(true);
        request.setFieldSeparator(" ");
        if (withUndef) {
            // add undefined
            final ITransformer<SQLSelect, SQLSelect> trans = request.getSelectTransf();
            request.setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {
                @Override
                public SQLSelect transformChecked(SQLSelect input) {
                    if (trans != null)
                        input = trans.transformChecked(input);
                    input.setExcludeUndefined(false, table);
                    return input;
                }
            });
        }
        return request;
    }

    public JListSQLTablePanel(final ComboSQLRequest req, final String undefined) {
        super(new GridBagLayout());
        this.table = req.getPrimaryTable();
        this.listModel = new JListSQLTableModel(req);
        this.list = new JList(this.listModel);
        this.list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        if (undefined != null) {
            final ListCellRenderer orig = this.list.getCellRenderer();
            this.list.setCellRenderer(new ListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    final IComboSelectionItem item = (IComboSelectionItem) value;
                    final boolean isUndef = item.getId() == table.getUndefinedID();
                    final String val = isUndef ? undefined : item.getLabel();
                    final Component res = orig.getListCellRendererComponent(list, val, index, isSelected, cellHasFocus);
                    if (isUndef)
                        res.setFont(res.getFont().deriveFont(Font.ITALIC));
                    if (item.getFlag() == IComboSelectionItem.IMPORTANT_FLAG)
                        res.setFont(res.getFont().deriveFont(Font.BOLD));
                    return res;
                }
            });
        }
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(new JScrollPane(this.list), c);

        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Rechercher"), c);

        // Filtre
        c.gridx++;
        c.weightx = 1;
        final JTextField filter = new JTextField(20);
        this.add(filter, c);
        final SimpleDocumentListener listener = new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                JListSQLTablePanel.this.list.clearSelection();
                if (filter.getText().length() > 0) {
                    JListSQLTablePanel.this.listModel.fillTree(filter.getText());
                } else {
                    JListSQLTablePanel.this.listModel.fillTree();
                }

            }
        };
        filter.getDocument().addDocumentListener(listener);

        JListSQLTablePanel.this.listModel.addListDataListener(dataListener);
    }

    public void removeAllTableListener() {
        JListSQLTablePanel.this.listModel.removeListDataListener(dataListener);
        JListSQLTablePanel.this.listModel.removeTableModifiedListener();
    }

    public JListSQLTableModel getModel() {
        return this.listModel;
    }

    public JList getJList() {
        return this.list;
    }

    public int getSelectedIndex() {
        return this.list.getSelectedIndex();
    }

    public void setSelectedIndex(int index) {
        this.list.setSelectedIndex(index);
    }

    public void addListSelectionListener(ListSelectionListener l) {
        this.list.addListSelectionListener(l);
    }

    public void removeListSelectionListener(ListSelectionListener l) {
        this.list.removeListSelectionListener(l);
    }

    int idToSelect = -1;

    public void selectID(final int id) {
        if (getModel().isUpdating()) {
            this.idToSelect = id;
        } else {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    // TODO Raccord de méthode auto-généré
                    int index = getModel().getIndexForId(id);
                    if (index >= 0) {
                        getJList().setSelectedIndex(index);
                        getJList().ensureIndexIsVisible(index);
                    }
                }
            });
        }
    }
}
