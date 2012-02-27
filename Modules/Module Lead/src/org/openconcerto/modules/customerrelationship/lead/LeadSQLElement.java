package org.openconcerto.modules.customerrelationship.lead;

import java.util.ArrayList;
import java.util.List;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.ui.group.Group;
import org.openconcerto.utils.CollectionMap;

public class LeadSQLElement extends ComptaSQLConfElement {
    public static final String ELEMENT_CODE = "customerrelationship.lead";

    public LeadSQLElement() {
        super(Module.TABLE_LEAD, "un prospect", "prospects");
    }

    @Override
    protected String createCode() {
        return ELEMENT_CODE;
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("COMPANY");
        l.add("FIRSTNAME");
        l.add("NAME");
        return l;
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("COMPANY");
        l.add("FIRSTNAME");
        l.add("NAME");
        return l;
    }

    @Override
    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_ADRESSE");
        return l;
    }

    @Override
    public CollectionMap<String, String> getShowAs() {
        return CollectionMap.singleton(null, getComboFields());
    }

    @Override
    public SQLComponent createComponent() {
        final String groupId = this.getCode() + ".default";
        final Group group = GlobalMapper.getInstance().getGroup(groupId);
        if (group == null) {
            throw new IllegalStateException("No group found for id " + groupId);
        }
        return new LeadSQLComponent(this, group);
    }

}
