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
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureItemSQLElement;
import org.openconcerto.erp.core.sales.shipment.element.BonDeLivraisonItemSQLElement;
import org.openconcerto.erp.core.sales.shipment.element.BonDeLivraisonSQLElement;
import org.openconcerto.erp.core.sales.shipment.report.BonLivraisonXmlSheet;
import org.openconcerto.erp.core.sales.shipment.ui.BonDeLivraisonItemTable;
import org.openconcerto.erp.panel.PanelOOSQLComponent;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.preferences.DefaultProps;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.GestionDevise;

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
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

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
        final JPanel addP = new JPanel();
        this.setAdditionalFieldsPanel(new FormLayouter(addP, 1));
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

        // Client
        JLabel labelClient = new JLabel(getLabelFor("ID_CLIENT"));
        labelClient.setHorizontalAlignment(SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.weighty = 0;
        this.add(labelClient, c);

        c.gridx++;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        this.comboClient = new ElementComboBox();

        this.add(this.comboClient, c);
        final ElementComboBox boxTarif = new ElementComboBox();
        if (this.comboClient.getElement().getTable().contains("ID_TARIF")) {
            this.comboClient.addValueListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
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
            });
        }

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
            c.gridwidth = GridBagConstraints.REMAINDER;

            c.weightx = 1;
            this.add(boxTarif, c);
            this.addView(boxTarif, "ID_TARIF");
            boxTarif.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    tableBonItem.setTarif(boxTarif.getSelectedRow(), false);
                }
            });
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

        DefaultProps props = DefaultNXProps.getInstance();
        Boolean b = props.getBooleanValue("ArticleShowPoids");
        if (b) {
            JPanel panelPoids = new JPanel();
            final JLabel labelPoids = new JLabel(getLabelFor("TOTAL_POIDS"));
            panelPoids.add(labelPoids);
            this.textPoidsTotal.setEnabled(false);
            this.textPoidsTotal.setHorizontalAlignment(JTextField.RIGHT);
            this.textPoidsTotal.setDisabledTextColor(Color.BLACK);
            panelPoids.add(this.textPoidsTotal);

            this.textPoidsTotal.setVisible(b);
            labelPoids.setVisible(b);
            DefaultGridBagConstraints.lockMinimumSize(panelPoids);
            this.add(panelPoids, c);
        }
        c.gridx = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.HORIZONTAL;

        final GridBagConstraints cTotalPan = new DefaultGridBagConstraints();

        JPanel panelTotalHT = new JPanel();
        panelTotalHT.setLayout(new GridBagLayout());
        cTotalPan.gridx = 0;
        cTotalPan.anchor = GridBagConstraints.WEST;
        cTotalPan.weightx = 0;
        final JLabelBold labelTotalHT = new JLabelBold(getLabelFor("TOTAL_HT"));
        panelTotalHT.add(labelTotalHT, cTotalPan);
        cTotalPan.anchor = GridBagConstraints.EAST;
        cTotalPan.gridx++;
        cTotalPan.weightx = 1;
        textTotalHT.setFont(labelTotalHT.getFont());
        panelTotalHT.add(this.textTotalHT, cTotalPan);
        this.add(panelTotalHT, c);

        JPanel panelTotalTVA = new JPanel();
        panelTotalTVA.setLayout(new GridBagLayout());
        cTotalPan.gridx = 0;
        cTotalPan.anchor = GridBagConstraints.WEST;
        cTotalPan.weightx = 0;
        panelTotalTVA.add(new JLabelBold(getLabelFor("TOTAL_TVA")), cTotalPan);
        cTotalPan.anchor = GridBagConstraints.EAST;
        cTotalPan.gridx++;
        cTotalPan.weightx = 1;
        panelTotalTVA.add(this.textTotalTVA, cTotalPan);
        c.gridy++;
        this.add(panelTotalTVA, c);

        JPanel panelTotalTTC = new JPanel();
        panelTotalTTC.setLayout(new GridBagLayout());
        cTotalPan.gridx = 0;
        cTotalPan.anchor = GridBagConstraints.WEST;
        cTotalPan.gridwidth = GridBagConstraints.REMAINDER;
        cTotalPan.fill = GridBagConstraints.BOTH;

        panelTotalTTC.add(new JSeparator(), cTotalPan);
        cTotalPan.gridwidth = 1;
        cTotalPan.fill = GridBagConstraints.HORIZONTAL;
        cTotalPan.weightx = 0;
        cTotalPan.gridy++;
        panelTotalTTC.add(new JLabelBold(getLabelFor("TOTAL_TTC")), cTotalPan);
        cTotalPan.anchor = GridBagConstraints.EAST;
        cTotalPan.gridx++;
        cTotalPan.weightx = 1;
        textTotalTTC.setFont(labelTotalHT.getFont());
        panelTotalTTC.add(this.textTotalTTC, cTotalPan);
        c.gridy++;
        this.add(panelTotalTTC, c);
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
        this.addSQLObject(this.textPoidsTotal, "TOTAL_POIDS");
        this.addRequiredSQLObject(this.textTotalHT, "TOTAL_HT");
        this.addRequiredSQLObject(this.textTotalTVA, "TOTAL_TVA");
        this.addRequiredSQLObject(this.textTotalTTC, "TOTAL_TTC");
        this.addRequiredSQLObject(this.comboClient, "ID_CLIENT");

        this.tableBonItem.getModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {

                int columnIndexHT = BonDeLivraisonSQLComponent.this.tableBonItem.getModel().getColumnIndexForElement(BonDeLivraisonSQLComponent.this.tableBonItem.getPrixTotalHTElement());
                int columnIndexTTC = BonDeLivraisonSQLComponent.this.tableBonItem.getModel().getColumnIndexForElement(BonDeLivraisonSQLComponent.this.tableBonItem.getPrixTotalTTCElement());
                int columnIndexPoids = BonDeLivraisonSQLComponent.this.tableBonItem.getModel().getColumnIndexForElement(BonDeLivraisonSQLComponent.this.tableBonItem.getPoidsTotalElement());

                if (e.getColumn() == TableModelEvent.ALL_COLUMNS || e.getColumn() == columnIndexHT || e.getColumn() == columnIndexTTC) {
                    updateTotal();
                }
                if (e.getColumn() == TableModelEvent.ALL_COLUMNS || e.getColumn() == columnIndexPoids) {
                    BonDeLivraisonSQLComponent.this.textPoidsTotal.setText(String.valueOf(Math.round(BonDeLivraisonSQLComponent.this.tableBonItem.getPoidsTotal() * 1000) / 1000.0));
                }
            }
        });
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

        if (r != null) {
            this.tableBonItem.insertFrom("ID_BON_DE_LIVRAISON", r.getID());
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

    }

    private void updateTotal() {
        RowValuesTableModel model = this.tableBonItem.getModel();

        long totalHT = 0;
        long totalTTC = 0;
        int columnIndexHT = model.getColumnIndexForElement(this.tableBonItem.getPrixTotalHTElement());
        int columnIndexTTC = model.getColumnIndexForElement(this.tableBonItem.getPrixTotalTTCElement());

        // columnIndexHT = model.getColumnIndexForElement(getTable().get);
        for (int i = 0; i < model.getRowCount(); i++) {
            Number nHT = (Number) model.getValueAt(i, columnIndexHT);
            totalHT += nHT.longValue();

            Number nTTC = (Number) model.getValueAt(i, columnIndexTTC);
            totalTTC += nTTC.longValue();
        }

        this.textTotalHT.setText(GestionDevise.currencyToString(totalHT));
        this.textTotalTVA.setText(GestionDevise.currencyToString(totalTTC - totalHT));
        this.textTotalTTC.setText(GestionDevise.currencyToString(totalTTC));
    }

    /**
     * Chargement des éléments d'une commande dans la table
     * 
     * @param idCommande
     * 
     */
    public void loadCommande(int idCommande) {

        SQLElement commande = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
        SQLElement commandeElt = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT_ELEMENT");

        if (idCommande > 1) {
            SQLInjector injector = SQLInjector.getInjector(commande.getTable(), this.getTable());
            SQLRow rowCmd = commande.getTable().getRow(idCommande);
            SQLRowValues createRowValuesFrom = injector.createRowValuesFrom(idCommande);
            String string = rowCmd.getString("NOM");
            createRowValuesFrom.put("NOM", string + (string.trim().length() == 0 ? "" : ",") + rowCmd.getString("NUMERO"));
            this.select(createRowValuesFrom);
        }

        loadItem(this.tableBonItem, commande, idCommande, commandeElt);
    }

    /**
     * Chargement des éléments d'une facture dans la table
     * 
     * @param idFacture
     * 
     */
    public void loadFacture(int idFacture) {

        SQLElement facture = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        SQLElement factureElt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");

        if (idFacture > 1) {
            SQLInjector injector = SQLInjector.getInjector(facture.getTable(), this.getTable());
            SQLRow rowFact = facture.getTable().getRow(idFacture);
            SQLRowValues createRowValuesFrom = injector.createRowValuesFrom(idFacture);
            String string = rowFact.getString("NOM");
            createRowValuesFrom.put("NOM", string + (string.trim().length() == 0 ? "" : ",") + rowFact.getString("NUMERO"));
            this.select(createRowValuesFrom);
        }

        loadItem(this.tableBonItem, facture, idFacture, factureElt);

    }

    /**
     * Chargement des éléments d'une facture dans la table
     * 
     * @param idSaisieVenteFacture
     * 
     */
    public void loadFactureItem(int idSaisieVenteFacture) {
        SQLElement facture = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        SQLElement factureElt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");
        loadItem(this.tableBonItem, facture, idSaisieVenteFacture, factureElt);
        for (int i = 0; i < this.tableBonItem.getRowValuesTable().getRowCount(); i++) {
            SQLRowValues rowVals = this.tableBonItem.getRowValuesTable().getRowValuesTableModel().getRowValuesAt(i);
            this.tableBonItem.getRowValuesTable().getRowValuesTableModel().putValue(rowVals.getObject("QTE"), i, "QTE_LIVREE");
        }
    }

    /***********************************************************************************************
     * Mise à jour des quantités livrées dans les élements de facture
     * 
     * @param idBon id du bon de livraison
     */
    public void updateQte(int idBon) {

        SQLTable tableFactureElem = new SaisieVenteFactureItemSQLElement().getTable();
        SQLSelect selBonItem = new SQLSelect(getTable().getBase());
        BonDeLivraisonItemSQLElement bonElt = new BonDeLivraisonItemSQLElement();
        selBonItem.addSelect(bonElt.getTable().getField("ID_SAISIE_VENTE_FACTURE_ELEMENT"));
        selBonItem.addSelect(bonElt.getTable().getField("QTE_LIVREE"));
        selBonItem.setWhere("BON_DE_LIVRAISON_ELEMENT.ID_BON_DE_LIVRAISON", "=", idBon);

        String reqBonItem = selBonItem.asString();
        Object obBonItem = getTable().getBase().getDataSource().execute(reqBonItem, new ArrayListHandler());

        final List<Object[]> myListBonItem = (List<Object[]>) obBonItem;
        final int size = myListBonItem.size();
        try {
            for (int i = 0; i < size; i++) {
                final Object[] objTmp = myListBonItem.get(i);
                final SQLRow rowFactElem = tableFactureElem.getRow(((Number) objTmp[0]).intValue());
                final SQLRowValues rowVals = new SQLRowValues(tableFactureElem);
                rowVals.put("QTE_LIVREE", Integer.valueOf(rowFactElem.getInt("QTE_LIVREE") + ((Number) objTmp[1]).intValue()));
                rowVals.update(rowFactElem.getID());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /***********************************************************************************************
     * Mise à jour des quantités livrées dans les élements de facture
     * 
     * @param idBon id du bon de livraison
     */
    public void cancelUpdateQte(int idBon) {

        SQLTable tableFactureElem = new SaisieVenteFactureItemSQLElement().getTable();
        SQLSelect selBonItem = new SQLSelect(getTable().getBase());
        BonDeLivraisonItemSQLElement bonElt = new BonDeLivraisonItemSQLElement();
        selBonItem.addSelect(bonElt.getTable().getField("ID_SAISIE_VENTE_FACTURE_ELEMENT"));
        selBonItem.addSelect(bonElt.getTable().getField("QTE_LIVREE"));
        selBonItem.setWhere("BON_DE_LIVRAISON_ELEMENT.ID_BON_DE_LIVRAISON", "=", idBon);

        String reqBonItem = selBonItem.asString();
        Object obBonItem = getTable().getBase().getDataSource().execute(reqBonItem, new ArrayListHandler());

        final List<Object[]> myListBonItem = (List<Object[]>) obBonItem;
        final int size = myListBonItem.size();
        try {
            for (int i = 0; i < size; i++) {
                final Object[] objTmp = myListBonItem.get(i);
                final SQLRow rowFactElem = tableFactureElem.getRow(((Number) objTmp[0]).intValue());
                final SQLRowValues rowVals = new SQLRowValues(tableFactureElem);
                rowVals.put("QTE_LIVREE", Integer.valueOf(((Number) objTmp[1]).intValue() - rowFactElem.getInt("QTE_LIVREE")));
                rowVals.update(rowFactElem.getID());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

}
