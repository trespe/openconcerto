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
 
 package org.openconcerto.erp.core.sales.shipment.component;

import static org.openconcerto.utils.CollectionUtils.createSet;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureItemSQLElement;
import org.openconcerto.erp.core.sales.shipment.element.BonDeLivraisonItemSQLElement;
import org.openconcerto.erp.core.sales.shipment.element.BonDeLivraisonSQLElement;
import org.openconcerto.erp.core.sales.shipment.report.BonLivraisonXmlSheet;
import org.openconcerto.erp.core.sales.shipment.ui.BonDeLivraisonItemTable;
import org.openconcerto.erp.core.supplychain.stock.element.StockItemsUpdater;
import org.openconcerto.erp.core.supplychain.stock.element.StockItemsUpdater.Type;
import org.openconcerto.erp.core.supplychain.stock.element.StockLabel;
import org.openconcerto.erp.panel.PanelOOSQLComponent;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class BonDeLivraisonSQLComponent extends TransfertBaseSQLComponent {
    private BonDeLivraisonItemTable tableBonItem;
    private ElementComboBox selectCommande, comboClient;
    private PanelOOSQLComponent panelOO;
    private JUniqueTextField textNumeroUnique;
    private final SQLTable tableNum = getTable().getBase().getTable("NUMEROTATION_AUTO");
    private final DeviseField textTotalHT = new DeviseField(6);
    private final DeviseField textTotalTVA = new DeviseField(6);
    private final DeviseField textTotalTTC = new DeviseField(6);
    private final JTextField textPoidsTotal = new JTextField(6);
    private final JTextField textNom = new JTextField(25);

    public BonDeLivraisonSQLComponent() {
        super(Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON"));
    }

    @Override
    protected RowValuesTable getRowValuesTable() {
        return this.tableBonItem.getRowValuesTable();
    }

    @Override
    protected SQLRowValues createDefaults() {
        this.textNumeroUnique.setText(NumerotationAutoSQLElement.getNextNumero(BonDeLivraisonSQLElement.class));
        this.tableBonItem.getModel().clearRows();
        return super.createDefaults();
    }

    public void addViews() {
        this.textTotalHT.setOpaque(false);
        this.textTotalTVA.setOpaque(false);
        this.textTotalTTC.setOpaque(false);

        this.selectCommande = new ElementComboBox();

        this.setLayout(new GridBagLayout());

        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Champ Module
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel addP = ComptaSQLConfElement.createAdditionalPanel();
        this.setAdditionalFieldsPanel(new FormLayouter(addP, 2));
        this.add(addP, c);

        c.gridy++;
        c.gridwidth = 1;

        // Numero
        JLabel labelNum = new JLabel(getLabelFor("NUMERO"));
        labelNum.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(labelNum, c);

        this.textNumeroUnique = new JUniqueTextField(16);
        c.gridx++;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        DefaultGridBagConstraints.lockMinimumSize(textNumeroUnique);
        this.add(this.textNumeroUnique, c);

        // Date
        c.gridx++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("DATE"), SwingConstants.RIGHT), c);

        JDate date = new JDate(true);
        c.gridx++;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(date, c);

        // Reference
        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("NOM"), SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.textNom, c);
        if (getTable().contains("DATE_LIVRAISON")) {
            // Date livraison
            c.gridx++;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 0;
            this.add(new JLabel(getLabelFor("DATE_LIVRAISON"), SwingConstants.RIGHT), c);

            JDate dateLivraison = new JDate(true);
            c.gridx++;
            c.weightx = 0;
            c.weighty = 0;
            c.fill = GridBagConstraints.NONE;
            this.add(dateLivraison, c);
            this.addView(dateLivraison, "DATE_LIVRAISON");
        }
        // Client
        JLabel labelClient = new JLabel(getLabelFor("ID_CLIENT"), SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        this.add(labelClient, c);

        c.gridx++;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        this.comboClient = new ElementComboBox();
        this.add(this.comboClient, c);
        if (getTable().contains("SPEC_LIVRAISON")) {
            // Date livraison
            c.gridx++;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 0;
            this.add(new JLabel(getLabelFor("SPEC_LIVRAISON"), SwingConstants.RIGHT), c);

            JTextField specLivraison = new JTextField();
            c.gridx++;
            c.weightx = 0;
            c.weighty = 0;
            this.add(specLivraison, c);
            this.addView(specLivraison, "SPEC_LIVRAISON");
        }

        final ElementComboBox boxTarif = new ElementComboBox();
        this.comboClient.addValueListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (comboClient.getElement().getTable().contains("ID_TARIF")) {
                    if (BonDeLivraisonSQLComponent.this.isFilling())
                        return;
                    final SQLRow row = ((SQLRequestComboBox) evt.getSource()).getSelectedRow();
                    if (row != null) {
                        // SQLRowAccessor foreignRow = row.getForeignRow("ID_TARIF");
                        // if (foreignRow.isUndefined() &&
                        // !row.getForeignRow("ID_DEVISE").isUndefined()) {
                        // SQLRowValues rowValsD = new SQLRowValues(foreignRow.getTable());
                        // rowValsD.put("ID_DEVISE", row.getObject("ID_DEVISE"));
                        // foreignRow = rowValsD;
                        //
                        // }
                        // tableBonItem.setTarif(foreignRow, true);
                        SQLRowAccessor foreignRow = row.getForeignRow("ID_TARIF");
                        if (!foreignRow.isUndefined() && (boxTarif.getSelectedRow() == null || boxTarif.getSelectedId() != foreignRow.getID())
                                && JOptionPane.showConfirmDialog(null, "Appliquer les tarifs associés au client?") == JOptionPane.YES_OPTION) {
                            boxTarif.setValue(foreignRow.getID());
                            // SaisieVenteFactureSQLComponent.this.tableFacture.setTarif(foreignRow,
                            // true);
                        } else {
                            boxTarif.setValue(foreignRow.getID());
                        }
                    }
                }
            }
        });

        // Bouton tout livrer
        JButton boutonAll = new JButton("Tout livrer");

        boutonAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RowValuesTableModel m = BonDeLivraisonSQLComponent.this.tableBonItem.getModel();

                // on livre tout les éléments
                for (int i = 0; i < m.getRowCount(); i++) {
                    SQLRowValues rowVals = m.getRowValuesAt(i);
                    Object o = rowVals.getObject("QTE");
                    int qte = o == null ? 0 : ((Number) o).intValue();
                    m.putValue(qte, i, "QTE_LIVREE");
                }
            }
        });

        // Tarif
        if (this.getTable().getFieldsName().contains("ID_TARIF")) {
            // TARIF
            c.gridy++;
            c.gridx = 0;
            c.weightx = 0;
            c.weighty = 0;
            c.gridwidth = 1;
            this.add(new JLabel("Tarif à appliquer"), c);
            c.gridx++;
            c.gridwidth = 1;

            c.weightx = 1;
            this.add(boxTarif, c);
            this.addView(boxTarif, "ID_TARIF");
            boxTarif.addModelListener("wantedID", new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    SQLRow selectedRow = boxTarif.getRequest().getPrimaryTable().getRow(boxTarif.getWantedID());
                    tableBonItem.setTarif(selectedRow, !isFilling());
                }
            });
        }

        if (getTable().contains("A_ATTENTION")) {
            // Date livraison
            c.gridx++;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 0;
            this.add(new JLabel(getLabelFor("A_ATTENTION"), SwingConstants.RIGHT), c);

            JTextField specLivraison = new JTextField();
            c.gridx++;
            c.weightx = 0;
            c.weighty = 0;
            this.add(specLivraison, c);
            this.addView(specLivraison, "A_ATTENTION");
        }

        // Element du bon
        List<JButton> l = new ArrayList<JButton>();
        l.add(boutonAll);
        this.tableBonItem = new BonDeLivraisonItemTable(l);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        this.add(this.tableBonItem, c);
        c.anchor = GridBagConstraints.EAST;
        // Totaux
        reconfigure(this.textTotalHT);
        reconfigure(this.textTotalTVA);
        reconfigure(this.textTotalTTC);

        // Poids Total
        c.gridy++;
        c.gridx = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        this.addSQLObject(this.textPoidsTotal, "TOTAL_POIDS");
        this.addRequiredSQLObject(this.textTotalHT, "TOTAL_HT");
        this.addRequiredSQLObject(this.textTotalTVA, "TOTAL_TVA");
        this.addRequiredSQLObject(this.textTotalTTC, "TOTAL_TTC");
        TotalPanel panelTotal = new TotalPanel(tableBonItem, textTotalHT, textTotalTVA, textTotalTTC, new DeviseField(), new DeviseField(), new DeviseField(), new DeviseField(), new DeviseField(),
                textPoidsTotal, null);
        c.gridx = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(panelTotal, c);

        c.anchor = GridBagConstraints.WEST;

        /*******************************************************************************************
         * * INFORMATIONS COMPLEMENTAIRES
         ******************************************************************************************/
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        TitledSeparator sep = new TitledSeparator("Informations complémentaires");
        c.insets = new Insets(10, 2, 1, 2);
        this.add(sep, c);
        c.insets = new Insets(2, 2, 1, 2);

        ITextArea textInfos = new ITextArea(4, 4);

        c.gridx = 0;
        c.gridy++;
        c.gridheight = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;

        final JScrollPane scrollPane = new JScrollPane(textInfos);
        this.add(scrollPane, c);
        textInfos.setBorder(null);
        DefaultGridBagConstraints.lockMinimumSize(scrollPane);

        c.gridx = 0;
        c.gridy++;
        c.gridheight = 1;
        c.gridwidth = 4;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;

        this.panelOO = new PanelOOSQLComponent(this);
        this.add(this.panelOO, c);

        this.addRequiredSQLObject(date, "DATE");
        this.addSQLObject(textInfos, "INFOS");
        this.addSQLObject(this.textNom, "NOM");
        this.addSQLObject(this.selectCommande, "ID_COMMANDE_CLIENT");
        this.addRequiredSQLObject(this.textNumeroUnique, "NUMERO");

        this.addRequiredSQLObject(this.comboClient, "ID_CLIENT");

        // Doit etre locké a la fin
        DefaultGridBagConstraints.lockMinimumSize(comboClient);

    }

    public BonDeLivraisonItemTable getTableBonItem() {
        return this.tableBonItem;
    }

    private void reconfigure(JTextField field) {
        field.setEnabled(false);
        field.setHorizontalAlignment(JTextField.RIGHT);
        field.setDisabledTextColor(Color.BLACK);
        field.setBorder(null);
    }

    public int insert(SQLRow order) {

        int idBon = getSelectedID();
        // on verifie qu'un bon du meme numero n'a pas été inséré entre temps
        if (this.textNumeroUnique.checkValidation()) {
            idBon = super.insert(order);
            this.tableBonItem.updateField("ID_BON_DE_LIVRAISON", idBon);
            this.tableBonItem.createArticle(idBon, this.getElement());

            // generation du document
            BonLivraisonXmlSheet bSheet = new BonLivraisonXmlSheet(getTable().getRow(idBon));
            bSheet.createDocumentAsynchronous();
            bSheet.showPrintAndExportAsynchronous(this.panelOO.isVisualisationSelected(), this.panelOO.isImpressionSelected(), true);

            // incrémentation du numéro auto
            if (NumerotationAutoSQLElement.getNextNumero(BonDeLivraisonSQLElement.class).equalsIgnoreCase(this.textNumeroUnique.getText().trim())) {
                SQLRowValues rowVals = new SQLRowValues(this.tableNum);
                int val = this.tableNum.getRow(2).getInt("BON_L_START");
                val++;
                rowVals.put("BON_L_START", new Integer(val));

                try {
                    rowVals.update(2);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            SQLPreferences prefs = new SQLPreferences(getTable().getDBRoot());

            if (!prefs.getBoolean(GestionArticleGlobalPreferencePanel.STOCK_FACT, true)) {

                try {
                    updateStock(idBon);
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            }
            // updateQte(idBon);
        } else {
            ExceptionHandler.handle("Impossible d'ajouter, numéro de bon de livraison existant.");
            Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
        }

        return idBon;
    }

    @Override
    public void select(SQLRowAccessor r) {
        if (r != null) {
            this.textNumeroUnique.setIdSelected(r.getID());
        }
        if (r == null || r.getIDNumber() == null)
            super.select(r);
        else {
            System.err.println(r);
            final SQLRowValues rVals = r.asRowValues();
            final SQLRowValues vals = new SQLRowValues(r.getTable());
            vals.load(rVals, createSet("ID_CLIENT"));
            vals.setID(rVals.getID());
            System.err.println("Select CLIENT");
            super.select(vals);
            rVals.remove("ID_CLIENT");
            super.select(rVals);
        }
    }

    @Override
    public void update() {
        if (!this.textNumeroUnique.checkValidation()) {
            ExceptionHandler.handle("Impossible d'ajouter, numéro de bon de livraison existant.");
            Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
            return;
        }
        super.update();
        this.tableBonItem.updateField("ID_BON_DE_LIVRAISON", getSelectedID());
        this.tableBonItem.createArticle(getSelectedID(), this.getElement());

        // generation du document
        BonLivraisonXmlSheet bSheet = new BonLivraisonXmlSheet(getTable().getRow(getSelectedID()));
        bSheet.createDocumentAsynchronous();
        bSheet.showPrintAndExportAsynchronous(this.panelOO.isVisualisationSelected(), this.panelOO.isImpressionSelected(), true);

        SQLPreferences prefs = new SQLPreferences(getTable().getDBRoot());
        SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
        if (!prefs.getBoolean(GestionArticleGlobalPreferencePanel.STOCK_FACT, true)) {
            // On efface les anciens mouvements de stocks
            SQLSelect sel = new SQLSelect(eltMvtStock.getTable().getBase());
            sel.addSelect(eltMvtStock.getTable().getField("ID"));
            Where w = new Where(eltMvtStock.getTable().getField("IDSOURCE"), "=", getSelectedID());
            Where w2 = new Where(eltMvtStock.getTable().getField("SOURCE"), "=", getTable().getName());
            sel.setWhere(w.and(w2));

            List l = (List) eltMvtStock.getTable().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());
            if (l != null) {
                for (int i = 0; i < l.size(); i++) {
                    Object[] tmp = (Object[]) l.get(i);
                    try {
                        eltMvtStock.archive(((Number) tmp[0]).intValue());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                updateStock(getSelectedID());
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }

    }

    /***********************************************************************************************
     * Mise à jour des quantités livrées dans les élements de facture
     * 
     * @param idBon id du bon de livraison
     * @throws SQLException
     */
    public void updateQte(int idBon) throws SQLException {

        SQLTable tableFactureElem = new SaisieVenteFactureItemSQLElement().getTable();
        SQLSelect selBonItem = new SQLSelect(getTable().getBase());
        BonDeLivraisonItemSQLElement bonElt = new BonDeLivraisonItemSQLElement();
        selBonItem.addSelect(bonElt.getTable().getField("ID_SAISIE_VENTE_FACTURE_ELEMENT"));
        selBonItem.addSelect(bonElt.getTable().getField("QTE_LIVREE"));
        selBonItem.setWhere(bonElt.getTable().getField("ID_BON_DE_LIVRAISON"), "=", idBon);

        String reqBonItem = selBonItem.asString();
        Object obBonItem = getTable().getBase().getDataSource().execute(reqBonItem, new ArrayListHandler());

        final List<Object[]> myListBonItem = (List<Object[]>) obBonItem;
        final int size = myListBonItem.size();

        for (int i = 0; i < size; i++) {
            final Object[] objTmp = myListBonItem.get(i);
            final SQLRow rowFactElem = tableFactureElem.getRow(((Number) objTmp[0]).intValue());
            final SQLRowValues rowVals = new SQLRowValues(tableFactureElem);
            rowVals.put("QTE_LIVREE", Integer.valueOf(rowFactElem.getInt("QTE_LIVREE") + ((Number) objTmp[1]).intValue()));
            rowVals.update(rowFactElem.getID());
        }

    }

    /***********************************************************************************************
     * Mise à jour des quantités livrées dans les élements de facture
     * 
     * @param idBon id du bon de livraison
     * @throws SQLException
     */
    public void cancelUpdateQte(int idBon) throws SQLException {

        SQLTable tableFactureElem = new SaisieVenteFactureItemSQLElement().getTable();
        SQLSelect selBonItem = new SQLSelect(getTable().getBase());
        BonDeLivraisonItemSQLElement bonElt = new BonDeLivraisonItemSQLElement();
        selBonItem.addSelect(bonElt.getTable().getField("ID_SAISIE_VENTE_FACTURE_ELEMENT"));
        selBonItem.addSelect(bonElt.getTable().getField("QTE_LIVREE"));
        selBonItem.setWhere(bonElt.getTable().getField("ID_BON_DE_LIVRAISON"), "=", idBon);

        String reqBonItem = selBonItem.asString();
        Object obBonItem = getTable().getBase().getDataSource().execute(reqBonItem, new ArrayListHandler());

        final List<Object[]> myListBonItem = (List<Object[]>) obBonItem;
        final int size = myListBonItem.size();

        for (int i = 0; i < size; i++) {
            final Object[] objTmp = myListBonItem.get(i);
            final SQLRow rowFactElem = tableFactureElem.getRow(((Number) objTmp[0]).intValue());
            final SQLRowValues rowVals = new SQLRowValues(tableFactureElem);
            rowVals.put("QTE_LIVREE", Integer.valueOf(((Number) objTmp[1]).intValue() - rowFactElem.getInt("QTE_LIVREE")));
            rowVals.update(rowFactElem.getID());
        }

    }

    protected String getLibelleStock(SQLRowAccessor row, SQLRowAccessor rowElt) {
        return "BL N°" + row.getString("NUMERO");
    }

    /**
     * Mise à jour des stocks pour chaque article composant la facture
     * 
     * @throws SQLException
     */
    private void updateStock(int id) throws SQLException {

        SQLPreferences prefs = new SQLPreferences(getTable().getDBRoot());
        if (!prefs.getBoolean(GestionArticleGlobalPreferencePanel.STOCK_FACT, true)) {

            SQLRow row = getTable().getRow(id);
            StockItemsUpdater stockUpdater = new StockItemsUpdater(new StockLabel() {
                @Override
                public String getLabel(SQLRowAccessor rowOrigin, SQLRowAccessor rowElt) {
                    return getLibelleStock(rowOrigin, rowElt);
                }
            }, row, row.getReferentRows(getTable().getTable("BON_DE_LIVRAISON_ELEMENT")), Type.REAL_DELIVER);

            stockUpdater.update();
        }
    }

}
