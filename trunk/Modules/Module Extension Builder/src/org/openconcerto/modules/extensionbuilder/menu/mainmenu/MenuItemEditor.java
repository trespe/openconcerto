package org.openconcerto.modules.extensionbuilder.menu.mainmenu;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.MenuManager;
import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.component.ComponentDescritor;
import org.openconcerto.modules.extensionbuilder.list.ListDescriptor;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;

public class MenuItemEditor extends JPanel {
    private JTextField textId;
    private JComboBox comboActionType;
    private JComboBox comboActionChoice;
    private JCheckBox shownInMenu;
    private boolean isEditingGroup;
    final Extension extension;
    private Vector<String> componentIds;
    private Vector<String> listIds;
    protected String previousId;
    private MenuItemTreeModel treeModel;

    public MenuItemEditor(MenuItemTreeModel model, final Item item, final Extension extension) {
        this.extension = extension;
        this.treeModel = model;
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        if (item instanceof Group) {
            this.isEditingGroup = true;
        }

        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Identifiant", SwingConstants.RIGHT), c);
        c.gridx++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridwidth = 2;
        textId = new JTextField();

        this.add(textId, c);
        c.gridy++;

        c.gridwidth = 1;
        if (!this.isEditingGroup) {
            c.weightx = 0;
            c.gridx = 0;
            this.add(new JLabel("Action", SwingConstants.RIGHT), c);
            c.gridx++;

            c.fill = GridBagConstraints.HORIZONTAL;
            comboActionType = new JComboBox();

            this.add(comboActionType, c);
            c.gridx++;
            c.weightx = 1;
            comboActionChoice = new JComboBox();
            this.add(comboActionChoice, c);

            c.gridy++;
        }

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        shownInMenu = new JCheckBox("Afficher dans le menu");

        this.add(shownInMenu, c);

        JPanel spacer = new JPanel();
        c.gridx = 1;
        c.gridy++;
        c.weighty = 1;

        this.add(spacer, c);
        initUIFrom(item.getId());

        // Listeners
        if (!isEditingGroup) {
            // comboActionType
            comboActionType.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    JComboBox cb = (JComboBox) e.getSource();
                    int type = cb.getSelectedIndex();
                    if (type == 0) {
                        comboActionChoice.setModel(new DefaultComboBoxModel(componentIds));

                        MenuDescriptor desc = extension.getCreateMenuItemFromId(item.getId());
                        desc.setType(MenuDescriptor.CREATE);
                        desc.setListId(null);
                        if (componentIds.size() > 0) {
                            comboActionChoice.setSelectedIndex(0);
                            desc.setComponentId(comboActionChoice.getSelectedItem().toString());
                        } else {
                            desc.setComponentId(null);
                        }
                        extension.setChanged();
                    } else {
                        comboActionChoice.setModel(new DefaultComboBoxModel(listIds));
                        MenuDescriptor desc = extension.getCreateMenuItemFromId(item.getId());
                        desc.setType(MenuDescriptor.LIST);
                        if (listIds.size() > 0) {
                            comboActionChoice.setSelectedIndex(0);
                            desc.setListId(comboActionChoice.getSelectedItem().toString());
                        } else {
                            desc.setListId(null);
                        }
                        desc.setComponentId(null);
                        extension.setChanged();
                    }

                }
            });
            comboActionChoice.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    MenuDescriptor desc = extension.getCreateMenuItemFromId(item.getId());
                    if (desc.getType() == MenuDescriptor.CREATE) {
                        desc.setComponentId(comboActionChoice.getSelectedItem().toString());
                    } else if (desc.getType() == MenuDescriptor.LIST) {
                        desc.setListId(comboActionChoice.getSelectedItem().toString());
                    } else {
                        desc.setComponentId(null);
                        desc.setListId(null);
                    }
                }
            });

        }
        shownInMenu.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean selected = shownInMenu.isSelected();

                treeModel.setActive(selected, item.getId());
            }
        });

    }

    private void initUIFrom(String itemId) {
        boolean hasCreated = extension.getCreateMenuItemFromId(itemId) != null;
        textId.setEnabled(hasCreated);
        previousId = itemId;
        if (hasCreated) {
            textId.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void removeUpdate(DocumentEvent e) {
                    changedUpdate(e);
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    changedUpdate(e);
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    String t = textId.getText();
                    System.err.println("MenuItemEditor.initUIFrom(...).new DocumentListener() {...}.changedUpdate()" + t);

                    if (!previousId.equals(t)) {
                        treeModel.renameMenuItem(previousId, t);
                        previousId = t;

                    }
                }
            });
        }

        shownInMenu.setSelected(extension.getRemoveMenuItemFromId(itemId) == null);
        if (textId != null) {
            textId.setText(itemId);
        }
        if (!isEditingGroup) {
            comboActionType.setEnabled(true);
            comboActionChoice.setEnabled(true);
            final Action actionForId = MenuManager.getInstance().getActionForId(itemId);
            if (hasCreated) {
                MenuDescriptor desc = extension.getCreateMenuItemFromId(itemId);
                comboActionType.setModel(new DefaultComboBoxModel(new String[] { "Saisie", "Liste" }));
                //
                final List<ComponentDescritor> compDescList = extension.getCreateComponentList();
                componentIds = new Vector<String>(compDescList.size());
                for (ComponentDescritor componentDescritor : compDescList) {
                    componentIds.add(componentDescritor.getId());
                }

                Collections.sort(componentIds);

                final List<ListDescriptor> listDescList = extension.getCreateListList();
                listIds = new Vector<String>(listDescList.size());
                for (ListDescriptor listDescritor : listDescList) {
                    listIds.add(listDescritor.getId());
                }

                Collections.sort(listIds);
                //

                String type = desc.getType();
                if (type.equals(MenuDescriptor.CREATE)) {
                    final String componentId = desc.getComponentId();
                    if (!componentIds.contains(componentId)) {
                        componentIds.add(componentId);
                    }
                    comboActionType.setSelectedIndex(0);
                    comboActionChoice.setModel(new DefaultComboBoxModel(componentIds));
                    comboActionChoice.setSelectedItem(componentId);

                } else if (type.equals(MenuDescriptor.LIST)) {
                    final String listId = desc.getListId();
                    if (!listIds.contains(listId)) {
                        listIds.add(listId);
                    }
                    comboActionType.setSelectedIndex(1);
                    comboActionChoice.setModel(new DefaultComboBoxModel(listIds));
                    comboActionChoice.setSelectedItem(listId);

                } else {
                    throw new IllegalArgumentException("Unknown type " + type);
                }
            } else {
                if (actionForId != null) {
                    if (actionForId instanceof CreateFrameAbstractAction) {
                        CreateFrameAbstractAction a = (CreateFrameAbstractAction) actionForId;
                        JFrame frame = a.createFrame();
                        if (frame != null) {
                            if (frame instanceof EditFrame) {
                                comboActionType.setModel(new DefaultComboBoxModel(new String[] { "Saisie" }));
                            } else if (frame instanceof IListFrame) {
                                comboActionType.setModel(new DefaultComboBoxModel(new String[] { "Liste" }));
                            } else {
                                comboActionType.setModel(new DefaultComboBoxModel(new String[] { "Autre" }));
                            }

                            comboActionChoice.setModel(new DefaultComboBoxModel(new String[] { frame.getTitle() }));
                        } else {
                            comboActionType.setModel(new DefaultComboBoxModel(new String[] { "Autre" }));
                            comboActionChoice.setModel(new DefaultComboBoxModel(new String[] { actionForId.getClass().getName() }));
                        }

                    } else {
                        comboActionType.setModel(new DefaultComboBoxModel(new String[] { "Autre" }));
                        comboActionChoice.setModel(new DefaultComboBoxModel(new String[] { actionForId.getClass().getName() }));
                    }

                }

            }
        }

    }
}
