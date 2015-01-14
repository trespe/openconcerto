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
 
 package org.openconcerto.erp.action;

import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.ui.light.LightUIDescriptorProvider;
import org.openconcerto.sql.view.list.IListeAction;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.light.ActivationOnSelectionControler;
import org.openconcerto.ui.light.ColumnSpec;
import org.openconcerto.ui.light.ColumnsSpec;
import org.openconcerto.ui.light.CustomEditorProvider;
import org.openconcerto.ui.light.LightUIDescriptor;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUILine;
import org.openconcerto.ui.light.ListToolbarLine;
import org.openconcerto.ui.light.TableSpec;
import org.openconcerto.utils.i18n.TranslationManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class CreateListFrameAbstractAction extends CreateFrameAbstractAction implements LightUIDescriptorProvider {
    abstract public String getTableName();

    @Override
    public LightUIDescriptor getUIDescriptor(PropsConfiguration configuration) {
        SQLElement element = configuration.getDirectory().getElement(getTableName());
        String descriptorId = element.getCode() + ".element";
        final LightUIDescriptor desc = new LightUIDescriptor(descriptorId);

        String listId = element.getCode() + ".list";
        LightUIElement list = getListCustomEditorProvider(element).createUIElement(listId);
        list.setFillWidth(true);
        // UI
        Collection<IListeAction> actions = element.getRowActions();
        LightUILine l0 = new LightUILine();
        l0.setGridAlignment(LightUILine.ALIGN_LEFT);
        for (Iterator iterator = actions.iterator(); iterator.hasNext();) {
            RowAction iListeAction = (RowAction) iterator.next();
            if (iListeAction.inHeader()) {
                LightUIElement element2 = new LightUIElement();
                element2.setType(LightUIElement.TYPE_BUTTON_WITH_CONTEXT);
                element2.setValue(iListeAction.getID());
                element2.setId(iListeAction.getID());

                String label = TranslationManager.getInstance().getTranslationForAction(iListeAction.getID());

                element2.setLabel(label);

                l0.add(element2);

                desc.addControler(new ActivationOnSelectionControler(listId, element2.getId()));

            }
        }
        desc.addLine(l0);

        LightUILine l1 = new LightUILine();
        l1.setFillHeight(true);
        l1.setWeightY(1);
        l1.add(list);
        desc.addLine(l1);

        desc.addLine(new ListToolbarLine());
        desc.dump(System.out);
        return desc;
    }

    public static CustomEditorProvider getListCustomEditorProvider(final SQLElement element) {
        // generic list of elements
        return new CustomEditorProvider() {

            @Override
            public LightUIElement createUIElement(String id) {
                LightUIElement e = new LightUIElement();
                e.setId(id);
                e.setType(LightUIElement.TYPE_LIST);
                List<String> visibleIds = new ArrayList<String>();
                List<String> sortedIds = new ArrayList<String>();

                SQLTableModelSourceOnline source = element.getTableSource();
                List<SQLTableModelColumn> columns = source.getColumns();

                List<ColumnSpec> cols = new ArrayList<ColumnSpec>(columns.size());
                for (SQLTableModelColumn column : columns) {

                    // FIXME : recuperer l'info sauvegardée sur le serveur par user (à coder)
                    int width = column.getName().length() * 20 + 20;

                    ColumnSpec col = new ColumnSpec(column.getIdentifier(), column.getValueClass(), column.getName(), null, width, false, null);
                    cols.add(col);
                    visibleIds.add(column.getIdentifier());
                }

                // FIXME : recuperer l'info sauvegardée sur le serveur par user (à coder)
                sortedIds.add(visibleIds.get(0));

                ColumnsSpec columnsSpec = new ColumnsSpec(element.getCode(), cols, visibleIds, sortedIds);
                TableSpec tSpec = new TableSpec();
                tSpec.setColumns(columnsSpec);
                e.setRawContent(tSpec);

                return e;
            }
        };
    }
}
