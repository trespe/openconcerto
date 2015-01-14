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
 
 package org.openconcerto.erp.core.sales.account;

import org.openconcerto.erp.core.common.component.TransfertGroupSQLComponent;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.AbstractArticleItemTable;
import org.openconcerto.erp.core.common.ui.Acompte;
import org.openconcerto.erp.core.common.ui.AcompteField;
import org.openconcerto.erp.core.common.ui.AcompteRowItemView;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.core.common.ui.AbstractVenteArticleItemTable.TypeCalcul;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureSQLElement;
import org.openconcerto.erp.core.sales.invoice.report.VenteFactureXmlSheet;
import org.openconcerto.erp.core.sales.invoice.ui.FactureSituationItemTable;
import org.openconcerto.erp.generationEcritures.GenerationMvtSaisieVenteFacture;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.group.Group;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;

public class VenteFactureSituationSQLComponent extends TransfertGroupSQLComponent {
    public static final String ID = "sales.invoice.partial";

    public VenteFactureSituationSQLComponent(SQLElement element) {
        super(element, (Group) GlobalMapper.getInstance().get(ID));
    }

    public VenteFactureSituationSQLComponent(SQLElement element, Group p) {
        super(element, p);
    }

    @Override
    protected Set<String> createRequiredNames() {
        final Set<String> s = new HashSet<String>(1);
        s.add("ID_CLIENT");
        s.add("NUMERO");
        s.add("DATE");
        s.add("MONTANT_FACTURABLE");
        s.add("ID_MODE_REGLEMENT");
        return s;
    }

    @Override
    protected RowValuesTable getRowValuesTable() {

        return ((AbstractArticleItemTable) getEditor("sales.invoice.partial.items.list")).getRowValuesTable();
    }

    JCheckBox box = new JCheckBox("partial");

    @Override
    protected void addViews() {

        super.addViews();

        box.setSelected(true);
        this.addView(box, "PARTIAL", REQ);

        final DeviseField totalHT = (DeviseField) getEditor("T_HT");
        final DeviseField totalService = (DeviseField) getEditor("T_SERVICE");
        final DeviseField totalSupply = (DeviseField) getEditor("T_HA");
        final DeviseField totalDevise = (DeviseField) getEditor("T_DEVISE");
        final JTextField totalWeight = (JTextField) getEditor("T_POIDS");
        final DeviseField totalTTC = (DeviseField) getEditor("T_TTC");
        final DeviseField totalTVA = (DeviseField) getEditor("T_TVA");
        final DeviseField totalRemise = (DeviseField) getEditor("REMISE_HT");
        final DeviseField totalPORT = (DeviseField) getEditor("PORT_HT");
        this.addView(totalPORT, "PORT_HT");
        this.addView(totalRemise, "REMISE_HT");
        this.addView(totalTVA, "T_TVA");
        this.addView(totalTTC, "T_TTC");
        this.addView(totalWeight, "T_POIDS");
        this.addView(totalDevise, "T_DEVISE");
        this.addView(totalSupply, "T_HA");
        this.addView(totalService, "T_SERVICE");
        this.addView(totalHT, "T_HT");

        final SQLRequestComboBox sqlRequestComboBox = (SQLRequestComboBox) getEditor("sales.invoice.customer");
        sqlRequestComboBox.addModelListener("wantedID", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {

                SQLElement sqleltModeRegl = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
                final SQLRow client = sqlRequestComboBox.getSelectedRow();
                if (client != null && !client.isUndefined()) {
                    int idModeRegl = client.getInt("ID_MODE_REGLEMENT");
                    if (idModeRegl > 1) {
                        SQLRow rowModeRegl = sqleltModeRegl.getTable().getRow(idModeRegl);
                        SQLRowValues rowValsModeRegl = rowModeRegl.createUpdateRow();
                        rowValsModeRegl.clearPrimaryKeys();
                        ((ElementSQLObject) getEditor("ID_MODE_REGLEMENT")).setValue(rowValsModeRegl);
                    }
                }

            }
        });
        sqlRequestComboBox.setEnabled(false);

        final AcompteField acompteField = ((AcompteField) getEditor("sales.invoice.partial.amount"));
        final FactureSituationItemTable table = ((FactureSituationItemTable) getEditor("sales.invoice.partial.items.list"));
        acompteField.getDocument().addDocumentListener(new SimpleDocumentListener() {

            @Override
            public void update(DocumentEvent e) {
                Acompte a = acompteField.getValue();
                table.calculPourcentage(a,TypeCalcul.CALCUL_FACTURABLE);
            }
        });
        final TotalPanel total = ((TotalPanel) getEditor("sales.invoice.partial.total.amount"));
        total.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                acompteField.setTotal(new BigDecimal(total.getTotalHT()).movePointLeft(2));

            }
        });

    }

    int countPole = 0;

    // @Override
    // public Component addView(MutableRowItemView rowItemView, String fields, Object specObj) {
    //
    // if (fields.contains("ID_POLE_PRODUIT") && countPole == 0) {
    // countPole++;
    // return null;
    // } else {
    // return super.addView(rowItemView, fields, specObj);
    // }
    // }

    @Override
    public JComponent getLabel(String id) {
        if (id.equals("sales.invoice.partial.amount")) {
            final JLabel jLabel = new JLabel("Montant à facturer");
            jLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            return jLabel;
        } else if (id.equals("sales.invoice.partial.total.amount")) {
            return new JLabel();
        } else {
            return super.getLabel(id);
        }
    }

    JUniqueTextField numberField;

    @Override
    public JComponent createEditor(String id) {

        if (id.equals("sales.invoice.number")) {
            this.numberField = new JUniqueTextField(20);
            return this.numberField;
        } else if (id.equals("INFOS")) {
            final ITextArea jTextArea = new ITextArea();
            jTextArea.setFont(new JLabel().getFont());
            return jTextArea;
        } else if (id.equals("sales.invoice.partial.items.list")) {
            return new FactureSituationItemTable();
        } else if (id.equals("DATE")) {
            return new JDate(true);
        } else if (id.equals("T_POIDS")) {
            return new JTextField();
        } else if (id.equals("sales.invoice.partial.total.amount")) {
            final AbstractArticleItemTable items = (AbstractArticleItemTable) getEditor("sales.invoice.partial.items.list");
            // Set only VAT Editable
            for (int i = 0; i < items.getRowValuesTable().getColumnModel().getColumnCount(false); i++) {
                final SQLTableElement sqlTableElementAt = items.getRowValuesTable().getRowValuesTableModel().getSQLTableElementAt(i);
                if (sqlTableElementAt.getField() == null || !sqlTableElementAt.getField().getName().equalsIgnoreCase("ID_TAXE")) {
                    sqlTableElementAt.setEditable(false);
                } else {
                    sqlTableElementAt.setEditable(true);
                }
            }

            final DeviseField totalHT = (DeviseField) getEditor("T_HT");
            final DeviseField totalService = (DeviseField) getEditor("T_SERVICE");
            final DeviseField totalSupply = (DeviseField) getEditor("T_HA");
            final DeviseField totalDevise = (DeviseField) getEditor("T_DEVISE");
            final JTextField totalWeight = (JTextField) getEditor("T_POIDS");
            final DeviseField totalTTC = (DeviseField) getEditor("T_TTC");
            final DeviseField totalTVA = (DeviseField) getEditor("T_TVA");
            final DeviseField totalRemise = (DeviseField) getEditor("REMISE_HT");
            final DeviseField totalPORT = (DeviseField) getEditor("PORT_HT");

            return new TotalPanel(items, totalHT, totalTVA, totalTTC, totalRemise, totalPORT, totalService, totalSupply, totalDevise, totalWeight, null);
        } else if (id.startsWith("T_")) {
            return new DeviseField();
        } else if (id.equals("REMISE_HT") || id.equals("PORT_HT")) {
            return new DeviseField();
        } else if (id.equals("sales.invoice.partial.amount")) {
            return new AcompteField();
        }
        return super.createEditor(id);
    }

    private final SQLTable tableNum = getElement().getTable().getTable("NUMEROTATION_AUTO");

    @Override
    public int insert(SQLRow order) {

        int idSaisieVF = SQLRow.NONEXISTANT_ID;

        if (this.numberField.checkValidation()) {

            idSaisieVF = super.insert(order);
            SQLRow rowFacture = getTable().getRow(idSaisieVF);
            // incrémentation du numéro auto
            if (NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, rowFacture.getDate("DATE").getTime()).equalsIgnoreCase(this.numberField.getText().trim())) {
                SQLRowValues rowVals = new SQLRowValues(this.tableNum);

                String labelNumberFor = NumerotationAutoSQLElement.getLabelNumberFor(SaisieVenteFactureSQLElement.class);
                int val = this.tableNum.getRow(2).getInt(labelNumberFor);
                val++;
                rowVals.put(labelNumberFor, Integer.valueOf(val));
                try {
                    rowVals.update(2);
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }

                ((AbstractArticleItemTable) getEditor("sales.invoice.partial.items.list")).updateField("ID_SAISIE_VENTE_FACTURE", idSaisieVF);

                new GenerationMvtSaisieVenteFacture(idSaisieVF);

                try {
                    VenteFactureXmlSheet sheet = new VenteFactureXmlSheet(rowFacture);
                    sheet.createDocument();
                    sheet.showPrintAndExport(true, false, false);
                } catch (Exception e) {
                    ExceptionHandler.handle("Une erreur est survenue lors de la création du document.", e);
                }

            }
        } else {
            ExceptionHandler.handle("Impossible d'ajouter, numéro de facture existant.");
            Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
        }
        return idSaisieVF;
    }

    @Override
    public void update() {

        super.update();
        int id = getSelectedID();
        ((AbstractArticleItemTable) getEditor("sales.invoice.partial.items.list")).updateField("ID_SAISIE_VENTE_FACTURE", id);

    }

    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues rowVals = new SQLRowValues(getTable());
        rowVals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(SaisieVenteFactureSQLElement.class, new Date()));
        rowVals.put("PARTIAL", Boolean.TRUE);
        return rowVals;
    }

    @Override
    public Component addView(JComponent comp, String id) {
        if (id.equals("sales.invoice.partial.amount")) {
            return super.addView(new AcompteRowItemView((AcompteField) comp), "MONTANT_FACTURABLE,POURCENT_FACTURABLE", REQ);
        } else {
            return super.addView(comp, id);
        }
    }
}
