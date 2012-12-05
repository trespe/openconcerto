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
 
 package org.openconcerto.erp.core.customerrelationship.customer.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.customerrelationship.customer.report.FicheClientXmlSheet;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.map.model.Ville;
import org.openconcerto.ql.LabelCreator;
import org.openconcerto.ql.QLPrinter;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.ui.EmailComposer;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

public class ClientNormalSQLElement extends ComptaSQLConfElement {

    public ClientNormalSQLElement() {
        super("CLIENT", "un client", "clients");
        final String property = PrinterNXProps.getInstance().getProperty("QLPrinter");
            if (property != null && property.trim().length() > 0) {
                PredicateRowAction actionPrintLabel = new PredicateRowAction(new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final SQLRowAccessor row = IListe.get(e).getSelectedRow();
                        final LabelCreator c = new LabelCreator(720);
                        c.setLeftMargin(10);
                        c.setTopMargin(10);
                        c.setDefaultFont(new Font("Verdana", Font.PLAIN, 50));

                        c.addLineBold(row.getString("NOM"));
                        final SQLRowAccessor foreignRow = row.getForeign("ID_ADRESSE");
                        final String string = foreignRow.getString("RUE");
                        String[] s = string.split("\n");
                        for (String string2 : s) {
                            System.err.println(string2);
                            c.addLineNormal(string2);
                        }

                        Ville v = Ville.getVilleFromVilleEtCode(foreignRow.getString("VILLE"));
                        c.addLineNormal(v.getCodepostal() + " " + v.getName());

                        System.err.println("\"" + property + "\"");
                        final QLPrinter prt = new QLPrinter(property);
                        try {
                            prt.print(c.getImage());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }, false, "customerrelationship.customer.label.print");
                actionPrintLabel.setPredicate(IListeEvent.getSingleSelectionPredicate());
                getRowActions().add(actionPrintLabel);
            }



        PredicateRowAction action = new PredicateRowAction(new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                sendMail(IListe.get(e).getSelectedRows());

            }
        }, true, "customerrelationship.customer.email.send");
        action.setPredicate(IListeEvent.getNonEmptySelectionPredicate());
        getRowActions().add(action);
    }

    protected void sendMail(List<SQLRowAccessor> l) {

        String mail = "";
            for (SQLRowAccessor rowCli : l) {
                String string = rowCli.getString("MAIL");
                if (string != null && string.trim().length() > 0) {
                    mail += string + ";";
                }
            }
        try {
            EmailComposer.getInstance().compose(mail, "", "");
        } catch (Exception exn) {
            ExceptionHandler.handle(null, "Impossible de créer le courriel", exn);
        }

    }

    protected boolean showMdr = true;

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
            l.add("CODE");
            // l.add("FORME_JURIDIQUE");
        l.add("NOM");
        if (getTable().getFieldsName().contains("LOCALISATION")) {
            l.add("LOCALISATION");
        }

        l.add("RESPONSABLE");

        l.add("ID_ADRESSE");
        l.add("TEL");
        l.add("FAX");
        l.add("MAIL");
            l.add("NUMERO_TVA");
            l.add("SIRET");
            l.add("ID_COMPTE_PCE");
            l.add("ID_MODE_REGLEMENT");
        l.add("INFOS");
        if (getTable().contains("GRAND_COMPTE"))
            l.add("GRAND_COMPTE");

        return l;
    }

    @Override
    public synchronized ListSQLRequest createListRequest() {
        return new ListSQLRequest(getTable(), getListFields()) {
            @Override
            protected void customizeToFetch(SQLRowValues graphToFetch) {
                super.customizeToFetch(graphToFetch);
                graphToFetch.grow("ID_MODE_REGLEMENT").put("AJOURS", null).put("LENJOUR", null);
            }
        };

    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        // l.add("FORME_JURIDIQUE");
        l.add("NOM");
        if (getTable().getFieldsName().contains("LOCALISATION")) {
            l.add("LOCALISATION");
        } else {
            l.add("CODE");
        }
        return l;
    }

    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_ADRESSE");
        l.add("ID_ADRESSE_L");
        l.add("ID_ADRESSE_F");
        l.add("ID_MODE_REGLEMENT");

        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new ClientNormalSQLComponent(this);
    }

}
