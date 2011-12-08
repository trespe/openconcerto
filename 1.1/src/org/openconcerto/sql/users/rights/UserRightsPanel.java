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
 
 package org.openconcerto.sql.users.rights;

import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.sqlobject.IComboSelectionItem;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.users.rights.UserRightSQLElement.UserRightComp;
import org.openconcerto.sql.view.ListeModifyPanel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.cc.IClosure;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class UserRightsPanel extends JPanel {

    // Liste des utilisateurs
    private final JListSQLTablePanel list;
    private final ListeModifyPanel modifPanel;

    public UserRightsPanel() {
        super(new GridBagLayout());

        // init the list before adding it, otherwise we see the first refresh from all lines to just
        // these of undef
        // instantiate our own element to be safe
        this.modifPanel = new ListeModifyPanel(new UserRightSQLElement());
        this.modifPanel.setSearchFullMode(false);
        final SQLTable table = this.getTable().getForeignTable("ID_USER_COMMON");
        this.list = new JListSQLTablePanel(table, Arrays.asList("NOM", "PRENOM"), "Droits par défaut");
        // only superusers can see superusers (that's how we prevent the setting of superuser
        // rights)
        if (!UserRightsManager.getCurrentUserRights().isSuperUser())
            this.list.getModel().setWhere(new Where(table.getField("SUPERUSER"), "=", false));
        this.list.getModel().setItemCustomizer(new IClosure<IComboSelectionItem>() {
            @Override
            public void executeChecked(IComboSelectionItem input) {
                if (input.getId() == UserManager.getUserID())
                    input.setFlag(IComboSelectionItem.IMPORTANT_FLAG);
            }
        });
        this.list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateListFromSelection();
            }
        });
        this.updateListFromSelection();

        // Liste des utilisateurs
        JPanel listePanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        listePanel.add(new JLabel("Liste des utilisateurs"), c);

        c.weightx = 1;
        c.weighty = 1;
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        listePanel.add(this.list, c);

        // Droits
        JPanel panelDroits = new JPanel(new GridBagLayout());
        GridBagConstraints c2 = new DefaultGridBagConstraints();
        c2.gridwidth = GridBagConstraints.REMAINDER;
        panelDroits.add(new JLabel("Droits"), c2);
        c2.gridy++;
        c2.weightx = 1;
        c2.weighty = 0.7;
        c2.fill = GridBagConstraints.BOTH;

        panelDroits.add(new JScrollPane(this.modifPanel), c2);

        // SplitPane
        JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listePanel, panelDroits);
        GridBagConstraints c3 = new GridBagConstraints();
        c3.weightx = 1;
        c3.weighty = 1;
        c3.fill = GridBagConstraints.BOTH;
        this.add(pane, c3);
    }

    private void updateListFromSelection() {
        final int selectedIndex = this.list.getSelectedIndex();

        final boolean b = selectedIndex >= 0;

        final ListSQLRequest req = this.modifPanel.getListe().getRequest();
        final int userID;
        if (b) {
            userID = this.list.getModel().getRowAt(selectedIndex).getID();
        } else {
            // since we don't display user in the list (to avoid undef)
            // we need to always display at most one user
            userID = this.list.getModel().getTable().getUndefinedID();
        }
        req.setWhere(new Where(req.getPrimaryTable().getField("ID_USER_COMMON"), "=", userID));

        // enforce the limitation
        ((UserRightComp) this.modifPanel.getModifComp()).setUserID(userID);
        ((UserRightComp) this.modifPanel.getAddComp()).setUserID(userID);
    }

    public final SQLTable getTable() {
        return this.modifPanel.getElement().getTable();
    }
}
