package org.openconcerto.modules.extensionbuilder;

import java.awt.Component;
import java.awt.Window;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openconcerto.modules.extensionbuilder.list.EditableListPanel;

public class ExtensionMainListPanel extends EditableListPanel {

    private ExtensionListPanel moduleListPanel;

    ExtensionMainListPanel(final ExtensionListPanel moduleListPanel) {
        super(new ExtensionListModel(moduleListPanel), "Vos extensions", "Créer une extension");
        this.moduleListPanel = moduleListPanel;
        this.list.setFixedCellHeight(new JLabel("A").getPreferredSize().height + 8);
        this.list.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final JLabel listCellRendererComponent = (JLabel) super.getListCellRendererComponent(list, ((Extension) value).getName(), index, isSelected, cellHasFocus);
                Extension e = (Extension) value;
                if (e.isStarted()) {
                    listCellRendererComponent.setIcon(new ImageIcon(ExtensionMainListPanel.this.getClass().getResource("started.png")));
                }
                return listCellRendererComponent;
            }

        });
    }

    public void fill() {
        ((ExtensionListModel) dataModel).fill(this);

    }

    @Override
    public void addNewItem() {
        ((ExtensionListModel) dataModel).addNewModule();
    }

    @Override
    public void renameItem(Object item) {
        final Extension e = (Extension) item;
        final Window w = SwingUtilities.windowForComponent(this);
        final String s = (String) JOptionPane.showInputDialog(w, "Nouveau nom", "Renommer l'extension", JOptionPane.PLAIN_MESSAGE, null, null, e.getName());
        if ((s != null) && (s.length() > 0)) {
            e.setName(s);
        }
    }

    @Override
    public void removeItem(Object item) {
        ((ExtensionListModel) dataModel).removeElement(item);
    }

    @Override
    public void itemSelected(Object item) {
        if (item != null) {
            final ExtensionInfoPanel p = new ExtensionInfoPanel((Extension) item, moduleListPanel);
            moduleListPanel.setRightPanel(p);
        } else {
            moduleListPanel.setRightPanel(new JPanel());
        }

    }

    public void modelChanged() {
        list.invalidate();
        list.repaint();
    }
}
