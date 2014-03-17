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
 
 package org.openconcerto.sql.ui.light;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.model.FieldMapper;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;
import org.openconcerto.ui.group.LayoutHints;
import org.openconcerto.ui.light.CustomEditorProvider;
import org.openconcerto.ui.light.LightUIDescriptor;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUILine;
import org.openconcerto.utils.i18n.TranslationManager;

import java.awt.Color;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GroupToLightUIConvertor {
    private final int maxColumnCount;
    private PropsConfiguration configuration;
    private Map<String, CustomEditorProvider> customEditorProviders = new HashMap<String, CustomEditorProvider>();

    public GroupToLightUIConvertor(PropsConfiguration conf) {
        this(conf, 4);
    }

    public GroupToLightUIConvertor(PropsConfiguration conf, int columns) {
        this.maxColumnCount = columns;
        this.configuration = conf;
    }

    public LightUIDescriptor convert(Group group) {
        final LightUIDescriptor desc = new LightUIDescriptor(group.getId());
        append(desc, group);
        return desc;
    }

    private void append(LightUIDescriptor desc, Item item) {
        if (item instanceof Group) {
            Group gr = (Group) item;
            int size = gr.getSize();
            for (int i = 0; i < size; i++) {
                Item it = gr.getItem(i);
                append(desc, it);
            }
        } else {
            LayoutHints localHint = item.getLocalHint();
            LightUILine currentLine = desc.getLastLine();
            if (localHint.isSeparated()) {
                if (currentLine.getWidth() > 0) {
                    currentLine = new LightUILine();
                    desc.addLine(currentLine);
                }
            }
            if (localHint.fillHeight()) {
                currentLine.setFillHeight(true);
            }

            if (localHint.largeHeight()) {
                currentLine.setWeightY(1);
            }

            if (currentLine.getWidth() >= maxColumnCount) {
                currentLine = new LightUILine();
                desc.addLine(currentLine);
            }
            LightUIElement elementLabel = null;
            if (localHint.showLabel()) {
                elementLabel = new LightUIElement();
                elementLabel.setId(item.getId());
                elementLabel.setType(LightUIElement.TYPE_LABEL);
                String label = TranslationManager.getInstance().getTranslationForItem(item.getId());
                if (label == null) {
                    label = item.getId();
                    elementLabel.setColor(Color.ORANGE);
                    elementLabel.setToolTip("No translation for " + item.getId());
                    Log.get().warning("No translation for " + item.getId());
                }

                elementLabel.setLabel(label);
                if (localHint.isSplit()) {
                    elementLabel.setGridWidth(4);
                } else {
                    elementLabel.setGridWidth(1);
                }

                currentLine.add(elementLabel);
            }
            LightUIElement elementEditor = this.getCustomEditor(item.getId());
            if (elementEditor == null) {
                elementEditor = new LightUIElement();

                elementEditor.setId(item.getId());

                FieldMapper fieldMapper = configuration.getFieldMapper();
                if (fieldMapper == null) {
                    throw new IllegalStateException("null field mapper");
                }

                SQLField field = fieldMapper.getSQLFieldForItem(item.getId());
                if (field != null) {
                    Class<?> javaType = field.getType().getJavaType();
                    if (field.isKey()) {
                        elementEditor.setType(LightUIElement.TYPE_COMBOBOX_ELEMENT);
                        elementEditor.setMinInputSize(20);
                    } else if (javaType.equals(String.class)) {
                        elementEditor.setType(LightUIElement.TYPE_TEXT_FIELD);
                        elementEditor.setValue("");
                        elementEditor.setMinInputSize(10);
                    } else if (javaType.equals(Date.class)) {
                        elementEditor.setType(LightUIElement.TYPE_DATE);
                    } else {
                        elementEditor.setType(LightUIElement.TYPE_TEXT_FIELD);
                        Log.get().warning("unsupported type " + javaType.getName());
                        elementEditor.setValue("unsupported type " + javaType.getName());
                    }
                } else {
                    elementEditor.setType(LightUIElement.TYPE_TEXT_FIELD);
                    elementEditor.setMinInputSize(10);
                    elementEditor.setToolTip("No field attached to " + item.getId());
                    Log.get().warning("No field attached to " + item.getId());
                    if (elementLabel != null) {
                        elementLabel.setColor(Color.ORANGE);
                        elementLabel.setToolTip("No field attached to " + item.getId());
                    }
                }
            }
            if (localHint.isSplit()) {
                if (currentLine.getWidth() > 0) {
                    currentLine = new LightUILine();
                    desc.addLine(currentLine);
                }
            }

            if (localHint.isSplit()) {
                elementEditor.setGridWidth(4);
            } else if (localHint.largeWidth()) {

                if (localHint.showLabel()) {
                    elementEditor.setGridWidth(3);
                } else {
                    elementEditor.setGridWidth(4);
                }
            } else {
                elementEditor.setGridWidth(1);
            }
            elementEditor.setFillWidth(localHint.fillWidth());
            currentLine.add(elementEditor);

        }

    }

    private LightUIElement getCustomEditor(String id) {
        final CustomEditorProvider customEditorProvider = this.customEditorProviders.get(id);
        if (customEditorProvider != null) {
            LightUIElement element = customEditorProvider.createUIElement(id);
            if (element.getId() == null) {
                throw new IllegalStateException("Null id for custom editor for id: " + id);
            }
            return element;
        }
        return null;
    }

    public void setCustomEditorProvider(String id, CustomEditorProvider provider) {
        this.customEditorProviders.put(id, provider);
    }
}
