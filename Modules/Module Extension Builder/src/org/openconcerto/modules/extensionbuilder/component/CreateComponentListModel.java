package org.openconcerto.modules.extensionbuilder.component;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.ui.DefaultListModel;

public class CreateComponentListModel extends DefaultListModel implements ChangeListener {
    private final Extension extension;

    CreateComponentListModel(Extension extension) {
        this.extension = extension;
        addContent(extension);
        extension.addChangeListener(this);
    }

    private void addContent(Extension extension) {
        this.addAll(extension.getCreateComponentList());
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        this.clear();
        addContent(extension);
    }

    public void addComponentList() {
        final ComponentDescritor l = new ComponentDescritor("Interface de saisie " + (this.getSize() + 1));
        this.addElement(l);
        extension.addCreateComponent(l);
    }

}
