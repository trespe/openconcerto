package org.openconcerto.modules.extensionbuilder.component;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;
import org.openconcerto.ui.group.LayoutHints;

public class GroupDescritor {

    private String id;

    private Group group;
    private List<ChangeListener> groupChangeListener = new ArrayList<ChangeListener>();

    public GroupDescritor(String id) {
        this.id = id;
        this.group = new Group(id);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Group getGroup() {
        return group;
    }

    @Override
    public String toString() {
        return this.getId();
    }

    public boolean containsGroupId(String gId) {
        return containsGroup(group, gId);
    }

    private boolean containsGroup(Item item, String gId) {
        if (item.getId().equals(gId)) {
            return true;
        }
        if (item instanceof Group) {
            Group group = (Group) item;
            final int size = group.getSize();
            for (int i = 0; i < size; i++) {
                boolean b = containsGroup(group.getItem(i), gId);
                if (b) {
                    return true;
                }
            }
        }
        return false;
    }

    public Item getItemFromId(String id) {
        return getItemFromId(group, id);
    }

    private Item getItemFromId(Item item, String gId) {
        if (item.getId().equals(gId)) {
            return item;
        }
        if (item instanceof Group) {
            Group group = (Group) item;
            final int size = group.getSize();
            for (int i = 0; i < size; i++) {
                Item b = getItemFromId(group.getItem(i), gId);
                if (b != null) {
                    return b;
                }
            }
        }
        return null;
    }

    public void removeGroup(Item gr) {
        remove(this.group, gr.getId());

    }

    private void remove(Item item, String gId) {
        if (item instanceof Group) {
            Group group2 = (Group) item;
            group2.remove(gId);
        }
    }

    public void updateGroupFrom(GroupTreeModel model) {
        this.group = new Group(this.getId());
        walk(model, this.group, model.getRoot());
        this.group = (Group) this.group.getItem(0);

    }

    protected void walk(GroupTreeModel model, Group gr, Object o) {
        int cc = model.getChildCount(o);
        for (int i = 0; i < cc; i++) {
            ActivableMutableTreeNode child = (ActivableMutableTreeNode) model.getChild(o, i);
            if (child.isActive()) {
                final Item userObject = (Item) child.getUserObject();
                if (userObject instanceof Group) {
                    final Group item = new Group(userObject.getId());
                    item.setLocalHint(new LayoutHints(userObject.getLocalHint()));
                    gr.add(item);
                    walk(model, item, child);
                } else {
                    final Item item = new Item(userObject.getId());
                    item.setLocalHint(new LayoutHints(userObject.getLocalHint()));
                    gr.add(item);
                }

            }
        }
    }

    public void addGroupChangeListener(ChangeListener changeListener) {
        this.groupChangeListener.add(changeListener);

    }

    public void fireGroupChanged() {
        for (ChangeListener l : this.groupChangeListener) {
            l.stateChanged(new ChangeEvent(this));
        }
    }

}
