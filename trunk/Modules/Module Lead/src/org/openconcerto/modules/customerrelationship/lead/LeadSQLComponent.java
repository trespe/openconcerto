package org.openconcerto.modules.customerrelationship.lead;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.sqlobject.SQLSearchableTextCombo;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.component.ComboLockedMode;
import org.openconcerto.ui.group.Group;

public class LeadSQLComponent extends GroupSQLComponent {
    public LeadSQLComponent(SQLElement element, Group group) {
        super(element, group);
    }

    @Override
    protected Set<String> createRequiredNames() {
        final Set<String> s = new HashSet<String>(1);
        s.add("ID_ADRESSE");
        return s;
    }

    @Override
    public JComponent createEditor(String id) {
        if (id.equals("INFORMATION")) {
            final JTextArea jTextArea = new JTextArea();
            jTextArea.setFont(new JLabel().getFont());
            return new JScrollPane(jTextArea);
        } else if (id.equals("INDUSTRY") || id.equals("STATUS") || id.equals("RATING") || id.equals("SOURCE")) {
            return new SQLSearchableTextCombo(ComboLockedMode.UNLOCKED, 1, 20, false);
        } else if (id.equals("DATE")) {
            return new JDate(true);
        }
        return super.createEditor(id);
    }
}
