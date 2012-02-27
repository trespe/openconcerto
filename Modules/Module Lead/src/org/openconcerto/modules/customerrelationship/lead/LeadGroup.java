package org.openconcerto.modules.customerrelationship.lead;

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.LayoutHints;

public class LeadGroup extends Group {

    public LeadGroup() {
        super("customerrelationship.lead.default");
        final Group g = new Group("customerrelationship.lead.identifier");
        g.add("NUMBER");
        g.add("DATE");
        g.add("COMPANY", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        this.add(g);

        final Group gContact = new Group("customerrelationship.lead.person");
        gContact.add("NAME");
        gContact.add("FIRSTNAME");
        this.add(gContact, LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);

        final Group gCustomer = new Group("customerrelationship.lead.contact");
        gCustomer.add("PHONE");
        gCustomer.add("MOBILE");
        gCustomer.add("FAX");
        gCustomer.add("EMAIL", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        gCustomer.add("WEBSITE", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        this.add(gCustomer, LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);

        final Group gAddress = new Group("customerrelationship.lead.address");
        gAddress.add("ID_ADRESSE");
        this.add(gAddress, LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);

        final Group gInfos = new Group("customerrelationship.lead.info");
        gInfos.add("INFORMATION", new LayoutHints(true, true, true, true, true));
        gInfos.add("INDUSTRY");
        gInfos.add("REVENUE");
        gInfos.add("EMPLOYEES");
        this.add(gInfos);

        final Group gState = new Group("customerrelationship.lead.state");
        gState.add("RATING");
        gState.add("SOURCE");
        gState.add("STATUS");
        gState.add("ID_COMMERCIAL");
        this.add(gState);

    }

    public static void main(String[] args) {
        final LeadGroup leadGroup = new LeadGroup();
        leadGroup.dumpTwoColumn();
        leadGroup.dumpOneColumn();
    }
}
