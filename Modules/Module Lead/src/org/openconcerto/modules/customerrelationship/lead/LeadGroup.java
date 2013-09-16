package org.openconcerto.modules.customerrelationship.lead;

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.LayoutHints;

public class LeadGroup extends Group {

    public LeadGroup() {
        super("customerrelationship.lead.default");
        final Group g = new Group("customerrelationship.lead.identifier");
        g.addItem("NUMBER");
        g.addItem("DATE");
        g.addItem("COMPANY", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        this.add(g);

        final Group gContact = new Group("customerrelationship.lead.person", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        gContact.addItem("NAME");
        gContact.addItem("FIRSTNAME");
        this.add(gContact);

        final Group gCustomer = new Group("customerrelationship.lead.contact", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        gCustomer.addItem("PHONE");
        gCustomer.addItem("MOBILE");
        gCustomer.addItem("FAX");
        gCustomer.addItem("EMAIL", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        gCustomer.addItem("WEBSITE", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        this.add(gCustomer);

        final Group gAddress = new Group("customerrelationship.lead.address", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        gAddress.addItem("ID_ADRESSE");
        this.add(gAddress);

        final Group gInfos = new Group("customerrelationship.lead.info");
        gInfos.addItem("INFORMATION", new LayoutHints(true, true, true, true, true, true));
        gInfos.addItem("INDUSTRY");
        gInfos.addItem("REVENUE");
        gInfos.addItem("EMPLOYEES");
        this.add(gInfos);

        final Group gState = new Group("customerrelationship.lead.state");
        gState.addItem("RATING");
        gState.addItem("SOURCE");
        gState.addItem("STATUS");
        gState.addItem("ID_COMMERCIAL");
        this.add(gState);

    }

    public static void main(String[] args) {
        final LeadGroup leadGroup = new LeadGroup();
        System.out.println(leadGroup.printTwoColumns());
    }
}
