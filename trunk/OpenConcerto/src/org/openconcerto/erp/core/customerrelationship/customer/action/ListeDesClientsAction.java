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
 
 package org.openconcerto.erp.core.customerrelationship.customer.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.customerrelationship.customer.report.FicheClientXmlSheet;
import org.openconcerto.erp.core.sales.invoice.ui.EcheanceRenderer;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.map.model.Ville;
import org.openconcerto.ql.LabelCreator;
import org.openconcerto.ql.QLPrinter;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.SQLTableModelSource;
import org.openconcerto.utils.cc.IClosure;

import java.awt.Font;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JTable;

public class ListeDesClientsAction extends CreateFrameAbstractAction {
    private static SQLTable tableModeReglement = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT").getTable();

    public ListeDesClientsAction() {
        super();
        this.putValue(Action.NAME, "Liste des clients");
    }

    protected SQLTableModelSource getTableSource() {
        SQLTable tableClient = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("CLIENT");
        return Configuration.getInstance().getDirectory().getElement(tableClient).getTableSource(true);
    }

    public JFrame createFrame() {
        SQLTable tableClient = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("CLIENT");
        final ListeAddPanel panel = new ListeAddPanel(Configuration.getInstance().getDirectory().getElement(tableClient), new IListe(getTableSource()));
        IListFrame frame = new IListFrame(panel);

        // Renderer
        final EcheanceRenderer rend = EcheanceRenderer.getInstance();
        JTable jTable = frame.getPanel().getListe().getJTable();
        for (int i = 0; i < jTable.getColumnCount(); i++) {
            int realColIndex = frame.getPanel().getListe().getJTable().getColumnModel().getColumn(i).getModelIndex();
            final Set<SQLField> fields = frame.getPanel().getListe().getSource().getColumn(realColIndex).getFields();
            // System.err.println("Column " + column + " Fields : " + fields);
            if (fields.contains(tableModeReglement.getField("AJOURS"))) {

                // if (jTable.getColumnClass(i) == Long.class || jTable.getColumnClass(i) ==
                // BigInteger.class) {
                jTable.getColumnModel().getColumn(i).setCellRenderer(rend);
            }
        }

        final String property = PrinterNXProps.getInstance().getProperty("QLPrinter");
            if (property != null && property.trim().length() > 0) {
                panel.getListe().addRowAction(RowAction.createAction("Imprimer l'étiquette client", null, new IClosure<List<SQLRowAccessor>>() {
                    @Override
                    public void executeChecked(List<SQLRowAccessor> input) {
                        final SQLRowAccessor row = input.get(0);
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
                }));
            }


        panel.setSearchFullMode(true);
        panel.setSelectRowOnAdd(false);
        return frame;
    }
}
