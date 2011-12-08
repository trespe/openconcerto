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
 
 package org.openconcerto.erp.core.supplychain.receipt.component;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.supplychain.receipt.element.BonReceptionSQLElement;
import org.openconcerto.erp.core.supplychain.receipt.ui.BonReceptionItemTable;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.preferences.DefaultProps;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.GestionDevise;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class BonReceptionSQLComponent extends TransfertBaseSQLComponent {
    private BonReceptionItemTable tableBonItem;
    private ElementComboBox selectCommande;
    private ElementComboBox fournisseur;
    private JUniqueTextField textNumeroUnique;
    private final SQLTable tableNum = getTable().getBase().getTable("NUMEROTATION_AUTO");
    private final DeviseField textTotalHT = new DeviseField();
    private final DeviseField textTotalTVA = new DeviseField();
    private final DeviseField textTotalTTC = new DeviseField();
    private final JTextField textPoidsTotal = new JTextField(6);
    private final JTextField textReference = new JTextField(25);

    public BonReceptionSQLComponent() {
        super(Configuration.getInstance().getDirectory().getElement("BON_RECEPTION"));
    }

    @Override
    protected SQLRowValues createDefaults() {
        this.tableBonItem.getModel().clearRows();
        return super.createDefaults();
    }

    public void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        this.textTotalHT.setOpaque(false);
        this.textTotalTVA.setOpaque(false);
        this.textTotalTTC.setOpaque(false);

        this.selectCommande = new ElementComboBox();
        // Numero
        JLabel labelNum = new JLabel(getLabelFor("NUMERO"));
        labelNum.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(labelNum, c);

        this.textNumeroUnique = new JUniqueTextField(16);
        c.gridx++;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        DefaultGridBagConstraints.lockMinimumSize(this.textNumeroUnique);
        this.add(this.textNumeroUnique, c);

        // Date
        JLabel labelDate = new JLabel(getLabelFor("DATE"));
        labelDate.setHorizontalAlignment(SwingConstants.RIGHT);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx++;
        c.weightx = 0;
        this.add(labelDate, c);

        JDate date = new JDate(true);
        c.gridx++;
        c.weightx = 0;
        c.weighty = 0;
        this.add(date, c);
        // Reference
        c.gridy++;
        c.gridx = 0;
        final JLabel labelNom = new JLabel(getLabelFor("NOM"));
        labelNom.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(labelNom, c);
        c.gridx++;
        c.fill = GridBagConstraints.HORIZONTAL;
        DefaultGridBagConstraints.lockMinimumSize(this.textReference);
        this.add(this.textReference, c);
        // Fournisseur
        JLabel labelFournisseur = new JLabel(getLabelFor("ID_FOURNISSEUR"));
        labelFournisseur.setHorizontalAlignment(SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.weighty = 0;
        this.add(labelFournisseur, c);

        this.fournisseur = new ElementComboBox();
        c.gridx++;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(this.fournisseur, c);

        // Devise
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("ID_DEVISE"), SwingConstants.RIGHT), c);

        final ElementComboBox boxDevise = new ElementComboBox();
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(boxDevise, c);
        this.addView(boxDevise, "ID_DEVISE");

        // Element du bon
        this.tableBonItem = new BonReceptionItemTable();
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
            JPanel panelPoids = new JPanel(new GridBagLayout());
            GridBagConstraints c2 = new DefaultGridBagConstraints();
            c2.fill = GridBagConstraints.NONE;

            panelPoids.add(new JLabel(getLabelFor("TOTAL_POIDS")), c2);
            // Necessaire pour ne pas avoir de saut de layout
            DefaultGridBagConstraints.lockMinimumSize(this.textPoidsTotal);
            this.textPoidsTotal.setEnabled(false);
            this.textPoidsTotal.setHorizontalAlignment(JTextField.RIGHT);
            this.textPoidsTotal.setDisabledTextColor(Color.BLACK);
            c2.gridx++;
            c2.weightx = 1;
            c2.fill = GridBagConstraints.HORIZONTAL;
            panelPoids.add(this.textPoidsTotal, c2);
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
        cTotalPan.weightx = 0;
        cTotalPan.fill = GridBagConstraints.HORIZONTAL;
        cTotalPan.anchor = GridBagConstraints.WEST;
        final JLabelBold labelTotalHT = new JLabelBold(getLabelFor("TOTAL_HT"));
        panelTotalHT.add(labelTotalHT, cTotalPan);
        cTotalPan.anchor = GridBagConstraints.EAST;
        cTotalPan.gridx++;
        cTotalPan.weightx = 1;
        this.textTotalHT.setFont(labelTotalHT.getFont());
        DefaultGridBagConstraints.lockMinimumSize(this.textTotalHT);
        panelTotalHT.add(this.textTotalHT, cTotalPan);
        this.add(panelTotalHT, c);

        JPanel panelTotalTVA = new JPanel();
        panelTotalTVA.setLayout(new GridBagLayout());
        cTotalPan.gridx = 0;
        cTotalPan.weightx = 0;
        cTotalPan.anchor = GridBagConstraints.WEST;
        cTotalPan.fill = GridBagConstraints.HORIZONTAL;
        panelTotalTVA.add(new JLabelBold(getLabelFor("TOTAL_TVA")), cTotalPan);
        cTotalPan.anchor = GridBagConstraints.EAST;
        cTotalPan.gridx++;
        cTotalPan.weightx = 1;
        DefaultGridBagConstraints.lockMinimumSize(this.textTotalTVA);
        panelTotalTVA.add(this.textTotalTVA, cTotalPan);
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(panelTotalTVA, c);

        JPanel panelTotalTTC = new JPanel();
        panelTotalTTC.setLayout(new GridBagLayout());
        cTotalPan.gridx = 0;
        cTotalPan.anchor = GridBagConstraints.WEST;
        cTotalPan.gridwidth = GridBagConstraints.REMAINDER;
        cTotalPan.fill = GridBagConstraints.BOTH;
        cTotalPan.weightx = 1;
        panelTotalTTC.add(new JSeparator(), cTotalPan);
        cTotalPan.gridwidth = 1;
        cTotalPan.fill = GridBagConstraints.NONE;
        cTotalPan.weightx = 0;
        cTotalPan.gridy++;
        panelTotalTTC.add(new JLabelBold(getLabelFor("TOTAL_TTC")), cTotalPan);
        cTotalPan.anchor = GridBagConstraints.EAST;
        cTotalPan.gridx++;
        this.textTotalTTC.setFont(labelTotalHT.getFont());
        DefaultGridBagConstraints.lockMinimumSize(this.textTotalTTC);
        panelTotalTTC.add(this.textTotalTTC, cTotalPan);
        c.gridy++;
        // probleme de tremblement vertical
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

        c.gridx = 0;
        c.gridy++;
        c.gridheight = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        final JTextArea textInfos = new JTextArea(4, 4);
        JScrollPane scrollPane = new JScrollPane(textInfos);
        DefaultGridBagConstraints.lockMinimumSize(scrollPane);

        this.add(textInfos, c);

        this.addRequiredSQLObject(date, "DATE");
        this.addSQLObject(textInfos, "INFOS");
        this.addSQLObject(this.textReference, "NOM");
        this.addSQLObject(this.selectCommande, "ID_COMMANDE");
        this.addRequiredSQLObject(this.textNumeroUnique, "NUMERO");
        this.addSQLObject(this.textPoidsTotal, "TOTAL_POIDS");
        this.addRequiredSQLObject(this.textTotalHT, "TOTAL_HT");
        this.addRequiredSQLObject(this.textTotalTVA, "TOTAL_TVA");
        this.addRequiredSQLObject(this.textTotalTTC, "TOTAL_TTC");
        this.addRequiredSQLObject(this.fournisseur, "ID_FOURNISSEUR");

        this.textNumeroUnique.setText(NumerotationAutoSQLElement.getNextNumero(BonReceptionSQLElement.class));

        // Listeners
        this.tableBonItem.getModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {

                int columnIndexHT = BonReceptionSQLComponent.this.tableBonItem.getModel().getColumnIndexForElement(BonReceptionSQLComponent.this.tableBonItem.getPrixTotalHTElement());
                int columnIndexTTC = BonReceptionSQLComponent.this.tableBonItem.getModel().getColumnIndexForElement(BonReceptionSQLComponent.this.tableBonItem.getPrixTotalTTCElement());
                int columnIndexPoids = BonReceptionSQLComponent.this.tableBonItem.getModel().getColumnIndexForElement(BonReceptionSQLComponent.this.tableBonItem.getPoidsTotalElement());

                if (e.getColumn() == TableModelEvent.ALL_COLUMNS || e.getColumn() == columnIndexHT || e.getColumn() == columnIndexTTC) {
                    updateTotal();
                }
                if (e.getColumn() == TableModelEvent.ALL_COLUMNS || e.getColumn() == columnIndexPoids) {
                    BonReceptionSQLComponent.this.textPoidsTotal.setText(String.valueOf(Math.round(BonReceptionSQLComponent.this.tableBonItem.getPoidsTotal() * 1000) / 1000.0));
                }
            }
        });

        // Lock UI
        DefaultGridBagConstraints.lockMinimumSize(this.fournisseur);

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
            this.tableBonItem.updateField("ID_BON_RECEPTION", idBon);

            // incrémentation du numéro auto
            if (NumerotationAutoSQLElement.getNextNumero(BonReceptionSQLElement.class).equalsIgnoreCase(this.textNumeroUnique.getText().trim())) {
                SQLRowValues rowVals = new SQLRowValues(this.tableNum);
                int val = this.tableNum.getRow(2).getInt("BON_R_START");
                val++;
                rowVals.put("BON_R_START", new Integer(val));

                try {
                    rowVals.update(2);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            calculPHaPondere(idBon);
            updateStock(idBon);
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
        super.select(r);
        if (r != null) {
            this.tableBonItem.insertFrom("ID_BON_RECEPTION", r.getID());
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
        } else {

            // On efface les anciens mouvements de stocks
            SQLRow row = getTable().getRow(getSelectedID());
            SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
            SQLSelect sel = new SQLSelect(eltMvtStock.getTable().getBase());
            sel.addSelect(eltMvtStock.getTable().getField("ID"));
            Where w = new Where(eltMvtStock.getTable().getField("IDSOURCE"), "=", row.getID());
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

            // Mise à jour de l'élément
            super.update();
            this.tableBonItem.updateField("ID_BON_RECEPTION", getSelectedID());

            // Mise à jour du stock
            updateStock(getSelectedID());
        }
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
     * Création d'un bon de réception à partir d'une commande
     * 
     * @param idCommande
     * 
     */
    public void loadCommande(int idCommande) {

        SQLElement commande = Configuration.getInstance().getDirectory().getElement("COMMANDE");
        SQLElement commandeElt = Configuration.getInstance().getDirectory().getElement("COMMANDE_ELEMENT");

        if (idCommande > 1) {
            SQLInjector injector = SQLInjector.getInjector(commande.getTable(), this.getTable());
            this.select(injector.createRowValuesFrom(idCommande));
        }

        loadItem(this.tableBonItem, commande, idCommande, commandeElt);
    }

    /**
     * Calcul du prix d'achat pondéré pour chacun des articles du bon de reception
     * 
     * @param id id du bon de reception
     */
    private void calculPHaPondere(int id) {
        SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
        SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement(sqlTableArticle);
        SQLElement eltStock = Configuration.getInstance().getDirectory().getElement("STOCK");
        SQLRow row = getTable().getRow(id);

        // On récupére les articles qui composent la facture
        SQLTable sqlTableBonElt = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("BON_RECEPTION_ELEMENT");
        List<SQLRow> elts = row.getReferentRows(sqlTableBonElt);

        for (SQLRow rowEltBon : elts) {

            SQLRowValues rowVals = rowEltBon.createUpdateRow();

            // recupere l'ancien prix d'achat
            int idArticle = ReferenceArticleSQLElement.getIdForCNM(rowVals, false);
            if (idArticle > 1) {
                // Prix d'achat de l'article à l'origine
                SQLRow rowArticle = eltArticle.getTable().getRow(idArticle);
                Long prixHA = (Long) rowArticle.getObject("PRIX_METRIQUE_HA_1");

                // Quantité en stock
                int idStock = rowArticle.getInt("ID_STOCK");
                SQLRow rowStock = eltStock.getTable().getRow(idStock);
                int qteStock = rowStock.getInt("QTE_REEL");
                if (prixHA != null && qteStock > 0) {
                    int qteRecue = rowEltBon.getInt("QTE");
                    Long prixHACmd = (Long) rowEltBon.getObject("PRIX_METRIQUE_HA_1");
                    if (qteRecue > 0 && prixHACmd != null) {
                        long prixHaPond = (qteRecue * prixHACmd + qteStock * prixHA) / (qteRecue + qteStock);
                        SQLRowValues rowValsArticle = rowArticle.createEmptyUpdateRow();
                        rowValsArticle.put("PRIX_METRIQUE_HA_1", Long.valueOf(prixHaPond));
                        try {
                            rowValsArticle.commit();
                        } catch (SQLException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }

    /**
     * Mise à jour des stocks pour chaque article composant du bon
     */
    private void updateStock(int id) {

        SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
        SQLTable sqlTableBonElt = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("BON_RECEPTION_ELEMENT");
        SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement(sqlTableArticle);
        SQLRow row = getTable().getRow(id);

        // On récupére les articles qui composent la facture
        List<SQLRow> elts = row.getReferentRows(sqlTableBonElt);

        for (SQLRow rowElt : elts) {

            // on récupére l'article qui lui correspond
            SQLRowValues rowArticle = new SQLRowValues(eltArticle.getTable());
            for (SQLField field : eltArticle.getTable().getFields()) {
                if (rowElt.getTable().getFieldsName().contains(field.getName())) {
                    rowArticle.put(field.getName(), rowElt.getObject(field.getName()));
                }
            }
            // rowArticle.loadAllSafe(rowEltFact);
            int idArticle = ReferenceArticleSQLElement.getIdForCNM(rowArticle, true);

            // on crée un mouvement de stock pour chacun des articles
            SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
            SQLRowValues rowVals = new SQLRowValues(eltMvtStock.getTable());
            rowVals.put("QTE", rowElt.getInt("QTE"));
            rowVals.put("NOM", "Bon de réception N°" + row.getString("NUMERO"));
            rowVals.put("IDSOURCE", id);
            rowVals.put("SOURCE", getTable().getName());
            rowVals.put("ID_ARTICLE", idArticle);
            rowVals.put("DATE", row.getObject("DATE"));
            try {
                SQLRow rowInsert = rowVals.insert();
                MouvementStockSQLElement.updateStock(Arrays.asList(rowInsert.getID()));
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

    }
}
