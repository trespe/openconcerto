package org.openconcerto.modules.extensionbuilder;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openconcerto.modules.extensionbuilder.list.EditableListPanel;
import org.openconcerto.ui.DefaultListModel;

public class ExtensionListModel extends DefaultListModel implements ChangeListener {

    private ExtensionListPanel moduleListPanel;

    ExtensionListModel(ExtensionListPanel moduleListPanel) {
        this.moduleListPanel = moduleListPanel;
    }

    public void fill(final EditableListPanel list) {
        addAll(moduleListPanel.getExtensionBuilderModule().getExtensions());
        final int size = this.getSize();
        if (size > 0) {
            final Object firstElement = firstElement();
            list.selectItem(firstElement);
        }
        for (int i = 0; i < size; i++) {
            Extension e = (Extension) this.get(i);
            e.addChangeListener(this);
        }

    }

    public void addNewModule() {
        final Extension e = new Extension("extension " + (this.size() + 1));
        this.addElement(e);
        e.addChangeListener(this);
    }

    @Override
    public void addElement(Object obj) {
        final Extension e = (Extension) obj;
        moduleListPanel.getExtensionBuilderModule().add(e);
        e.addChangeListener(this);
        super.addElement(obj);
    }

    @Override
    public boolean removeElement(Object obj) {
        final Extension extenstion = (Extension) obj;
        final int answer = JOptionPane.showConfirmDialog(new JFrame(), "Voulez vous vraiment supprimer l'extension " + extenstion.getName() + " ?", "Suppression", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.OK_OPTION) {
            moduleListPanel.getExtensionBuilderModule().remove(extenstion);
            extenstion.removeChangeListener(this);
            return super.removeElement(obj);
        }
        return false;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        moduleListPanel.modelChanged();
    }

}
