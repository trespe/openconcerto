package org.openconcerto.modules.extensionbuilder.list;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public abstract class EditableListPanel extends JPanel {

    protected JList list;
    protected ListModel dataModel;
    private JButton removeButton = null;
    private JButton renameButton = null;

    public EditableListPanel(final ListModel dataModel, String title, String addLabel) {
        this(dataModel, title, addLabel, true, true);
    }

    public EditableListPanel(final ListModel dataModel, String title, String addLabel, boolean editable, boolean canRename) {
        this.dataModel = dataModel;
        this.setOpaque(false);

        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.insets = new Insets(2, 2, 2, 0);
        if (title != null) {
            this.add(new JLabel(title), c);
        }
        list = new JList(dataModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final JScrollPane comp2 = new JScrollPane(list);
        comp2.setMinimumSize(new Dimension(150, 150));
        comp2.setPreferredSize(new Dimension(150, 150));
        c.weighty = 1;
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        this.add(comp2, c);

        if (editable) {

            c.gridy++;
            c.weighty = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.SOUTHWEST;
            c.insets = new Insets(2, 2, 2, 0);
            final JButton addButton = new JButton(addLabel);
            this.add(addButton, c);
            c.gridy++;
            if (canRename) {
                renameButton = new JButton("Renommer");
                this.add(renameButton, c);
                renameButton.setEnabled(false);
                renameButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (list.getSelectedValue() != null) {
                            renameItem(list.getSelectedValue());
                        }
                    }

                });
                c.gridy++;
            }

            removeButton = new JButton("Supprimer");
            removeButton.setEnabled(false);
            this.add(removeButton, c);

            // init

            addButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    addNewItem();
                    if (dataModel.getSize() > 0) {
                        selectItem(dataModel.getElementAt(dataModel.getSize() - 1));
                    }
                }

            });

            removeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (list.getSelectedValue() != null) {
                        removeItem(list.getSelectedValue());
                    }
                }
            });
        }
        list.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                final Object selectedValue = list.getSelectedValue();
                if (removeButton != null) {
                    removeButton.setEnabled(selectedValue != null);
                }
                if (renameButton != null) {
                    renameButton.setEnabled(selectedValue != null);
                }
                if (!e.getValueIsAdjusting() && selectedValue != null) {

                    itemSelected(selectedValue);

                }
                if (selectedValue == null) {
                    itemSelected(null);
                }

            }
        });

    }

    /**
     * Select an item in the list
     * */
    public void selectItem(Object item) {
        list.setSelectedValue(item, true);
    }

    /**
     * Called when the user click "add"
     * */
    public abstract void addNewItem();

    /**
     * Called when the user click "rename"
     * */
    public abstract void renameItem(Object item);

    /**
     * Called when the user click "remove"
     * */
    public abstract void removeItem(Object item);

    /**
     * Called when the user select an item in the list
     * 
     * @param item the item, null if all the items are deselected
     * */
    public abstract void itemSelected(Object item);

    public void reload() {
        list.invalidate();
        list.repaint();
    }
}
