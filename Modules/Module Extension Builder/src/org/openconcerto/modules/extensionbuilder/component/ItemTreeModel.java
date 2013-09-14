package org.openconcerto.modules.extensionbuilder.component;

import java.util.HashSet;
import java.util.Set;

import org.openconcerto.modules.extensionbuilder.table.FieldDescriptor;

public class ItemTreeModel extends GroupTreeModel {

    private Set<FieldDescriptor> allFields = new HashSet<FieldDescriptor>();

    public ItemTreeModel() {

    }

    public boolean containsFieldDescritor(FieldDescriptor d) {
        return this.allFields.contains(d);
    }

}
