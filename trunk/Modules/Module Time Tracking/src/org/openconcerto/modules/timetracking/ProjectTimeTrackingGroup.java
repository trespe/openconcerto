package org.openconcerto.modules.timetracking;

import org.openconcerto.modules.timetracking.element.ProjectTimeTrackingSQLElement;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.LayoutHints;

public class ProjectTimeTrackingGroup extends Group {

    public ProjectTimeTrackingGroup() {
        super(ProjectTimeTrackingSQLElement.ELEMENT_CODE + ".default");
        addItem("DATE");

        final Group g = new Group(ProjectTimeTrackingSQLElement.ELEMENT_CODE + ".infos", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        g.addItem("DESCRIPTIF", new LayoutHints(true, true, true, true, true, true));
        g.addItem("TEMPS");
        this.add(g);

        addItem("ID_USER_COMMON");
        addItem("ID_COMMANDE_CLIENT_ELEMENT");
        addItem("ID_AFFAIRE", LayoutHints.DEFAULT_VERY_LARGE_FIELD_HINTS);
    }

}
