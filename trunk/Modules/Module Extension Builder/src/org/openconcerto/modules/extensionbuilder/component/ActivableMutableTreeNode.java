package org.openconcerto.modules.extensionbuilder.component;

import javax.swing.tree.DefaultMutableTreeNode;

public class ActivableMutableTreeNode extends DefaultMutableTreeNode {
    private boolean active = true;

    public ActivableMutableTreeNode(Object obj) {
        super(obj);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

}
