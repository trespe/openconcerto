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
 
 package org.openconcerto.erp.core.common.component;

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.common.ui.AbstractArticleItemTable;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValues.ForeignCopyMode;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.group.Group;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.cc.ITransformer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;

public abstract class TransfertGroupSQLComponent extends GroupSQLComponent {
    protected SQLRowAccessor selectedRow;
    private List<SQLRowValues> sourceRows;

    public TransfertGroupSQLComponent(SQLElement element, Group group) {
        super(element, group);
    }

    /**
     * Chargement d'élément à partir d'une autre table ex : les éléments d'un BL dans une facture
     * 
     * @param table ItemTable du component de destination (ex : tableFacture)
     * @param elt element source (ex : BL)
     * @param id id de la row source
     * @param itemsElt elements des items de la source (ex : BL_ELEMENT)
     */
    public void loadItem(AbstractArticleItemTable table, SQLElement elt, int id, SQLElement itemsElt) {
        loadItem(table, elt, id, itemsElt, true);
    }

    public void loadItem(AbstractArticleItemTable table, SQLElement elt, int id, SQLElement itemsElt, boolean clear) {
        List<SQLRow> myListItem = elt.getTable().getRow(id).getReferentRows(itemsElt.getTable());

        if (myListItem.size() != 0) {
            SQLInjector injector = SQLInjector.getInjector(itemsElt.getTable(), table.getSQLElement().getTable());
            if (clear) {
                table.getModel().clearRows();
            }
            for (SQLRow rowElt : myListItem) {

                SQLRowValues createRowValuesFrom = injector.createRowValuesFrom(rowElt);
                if (createRowValuesFrom.getTable().getFieldsName().contains("POURCENT_ACOMPTE")) {
                    if (createRowValuesFrom.getObject("POURCENT_ACOMPTE") == null) {
                        createRowValuesFrom.put("POURCENT_ACOMPTE", new BigDecimal(100.0));
                    }
                }
                table.getModel().addRow(createRowValuesFrom);
                int rowIndex = table.getModel().getRowCount() - 1;
                table.getModel().fireTableModelModified(rowIndex);
            }
        } else {
            if (clear) {
                table.getModel().clearRows();
                table.getModel().addNewRowAt(0);
            }
        }
        table.getModel().fireTableDataChanged();
        table.repaint();
    }

    public void importFrom(List<SQLRowValues> rows) {
        this.sourceRows = rows;
        if (rows.size() > 0) {
            final SQLInjector injector = SQLInjector.getInjector(rows.get(0).getTable(), this.getTable());
            final SQLRowValues rValues = injector.createRowValuesFrom(rows);
            select(rValues);
        } else {
            select(null);
        }
    }

    @Override
    public int insert(SQLRow order) {
        // TODO: Pour l'instant appelé dans Swing, mais cela va changer...
        final int insertedId = super.insert(order);
        if (insertedId != SQLRow.NONEXISTANT_ID && sourceRows != null && !sourceRows.isEmpty()) {
            final SQLInjector injector = SQLInjector.getInjector(sourceRows.get(0).getTable(), this.getTable());
            try {
                injector.commitTransfert(sourceRows, insertedId);
            } catch (Exception e) {
                ExceptionHandler.handle("Unable to insert transfert", e);
            }
        }
        return insertedId;
    }

    @Override
    public void select(SQLRowAccessor r) {
        if (r == null) {
            super.select(null);
            return;
        }
        // remove foreign and replace rowvalues by id
        final SQLRowValues singleRowValues = new SQLRowValues(r.asRowValues(), ForeignCopyMode.COPY_ID_OR_RM);
        super.select(singleRowValues);
        final RowValuesTable table = this.getRowValuesTable();
        if (table != null) {
            table.clear();
            table.insertFrom(r);
        }
    }

    protected RowValuesTable getRowValuesTable() {
        return null;
    }

    public static void openTransfertFrame(List<SQLRowValues> sourceRows, String destTableName, String groupID) {
        final SQLElement elt = Configuration.getInstance().getDirectory().getElement(destTableName);
        final EditFrame editFrame = new EditFrame(elt.createComponent(groupID), EditMode.CREATION);
        editFrame.setIconImage(new ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());
        final SQLComponent sqlComponent = editFrame.getSQLComponent();
        if (sqlComponent instanceof TransfertGroupSQLComponent) {
            final TransfertGroupSQLComponent comp = (TransfertGroupSQLComponent) sqlComponent;

            if (!sourceRows.isEmpty()) {
                // fetch all fields of all table to avoid 1 request by referent row
                final List<Number> ids = new ArrayList<Number>(sourceRows.size());
                for (SQLRowValues sqlRowValues : sourceRows) {
                    ids.add(sqlRowValues.getIDNumber());
                }
                final SQLRowValues row = sourceRows.get(0).deepCopy();
                // FIXME don't work in the general case
                for (final SQLField rk : row.getTable().getDBSystemRoot().getGraph().getReferentKeys(row.getTable())) {
                    final Set<SQLRowValues> referentRows = row.getReferentRows(rk);
                    if (referentRows.size() > 1) {
                        final Iterator<SQLRowValues> iter = new ArrayList<SQLRowValues>(referentRows).iterator();
                        // keep the first
                        iter.next();
                        while (iter.hasNext()) {
                            final SQLRowValues ref = iter.next();
                            ref.remove(rk.getName());
                        }
                    }
                }
                for (SQLRowValues r : row.getGraph().getItems()) {
                    final Set<String> fields = new HashSet<String>(r.getTable().getFieldsName());
                    fields.removeAll(r.getFields());
                    r.putNulls(fields, false);
                }
                final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(row);
                fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

                    @Override
                    public SQLSelect transformChecked(SQLSelect input) {
                        input.setWhere(new Where(row.getTable().getKey(), ids));
                        return input;
                    }
                });

                final List<SQLRowValues> result = fetcher.fetch();
                comp.importFrom(result);
                FrameUtil.show(editFrame);
            }
        } else {
            throw new IllegalArgumentException("Table " + destTableName + " SQLComponent is not a TransfertBaseSQLComponent");
        }
    }
}
