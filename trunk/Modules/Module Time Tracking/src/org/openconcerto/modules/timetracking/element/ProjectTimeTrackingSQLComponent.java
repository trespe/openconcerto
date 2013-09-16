package org.openconcerto.modules.timetracking.element;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.group.Group;

public class ProjectTimeTrackingSQLComponent extends GroupSQLComponent {

    final ElementComboBox boxAffaire;
    final ElementComboBox boxCmdElt;

    public ProjectTimeTrackingSQLComponent(SQLElement element, Group group) {
        super(element, group);

        this.boxCmdElt = new ElementComboBox();
        final SQLElement cmdElement = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT_ELEMENT");
        final ComboSQLRequest comboRequest = cmdElement.getComboRequest(true);
        this.boxCmdElt.init(cmdElement, comboRequest);

        this.boxAffaire = new ElementComboBox();
        this.boxAffaire.setVisible(false);
        this.boxAffaire.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                JComponent comp = getEditor("ID_COMMANDE_CLIENT_ELEMENT");
                if (comp != null) {
                    ElementComboBox boxCmd = (ElementComboBox) comp;
                    SQLRow selectedRow = ProjectTimeTrackingSQLComponent.this.boxAffaire.getSelectedRow();
                    if (selectedRow == null || selectedRow.isUndefined()) {
                        boxCmd.getRequest().setWhere(null);
                    } else {
                        SQLTable cmdEltTable = boxCmd.getRequest().getPrimaryTable();
                        SQLTable cmdTable = cmdEltTable.getTable("COMMANDE_CLIENT");
                        List<SQLRow> l = selectedRow.getReferentRows(cmdTable);
                        if (l.size() > 0) {
                            // Get all the order of the selected project
                            List<Integer> orderIds = new ArrayList<Integer>();
                            for (SQLRow sqlRow : l) {
                                orderIds.add(sqlRow.getID());
                            }
                            boxCmd.getRequest().setWhere(new Where(cmdEltTable.getField("ID_COMMANDE_CLIENT"), orderIds));
                        } else {
                            boxCmd.getRequest().setWhere(null);
                        }
                    }
                }

            }
        });

    }

    @Override
    protected Set<String> createRequiredNames() {
        final Set<String> s = new HashSet<String>(1);
        s.add("ID_USER_COMMON");
        s.add("DATE");
        s.add("TEMPS");
        s.add("DESCRIPTIF");
        s.add("ID_COMMANDE_CLIENT_ELEMENT");
        return s;
    }

    @Override
    public JComponent getEditor(String id) {
        if (id.equals("ID_AFFAIRE")) {
            return this.boxAffaire;
        } else if (id.equals("ID_COMMANDE_CLIENT_ELEMENT")) {
            return this.boxCmdElt;
        } else if (id.equals("DESCRIPTIF")) {
            ITextArea area = new ITextArea(10, 60);
            return area;
        } else if (id.equals("DATE")) {
            return new JDate(true);
        }
        return super.getEditor(id);
    }

    @Override
    public JComponent getLabel(String id) {
        if (id.equals("ID_AFFAIRE")) {
            final JLabel label = new JLabel();
            label.setVisible(false);
            return label;
        }
        return super.getLabel(id);
    }
}
