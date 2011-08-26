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
 
 package org.openconcerto.erp.core.supplychain.stock.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.supplychain.order.component.CommandeSQLComponent;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.preferences.DefaultProps;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class MouvementStockSQLElement extends ComptaSQLConfElement {

    public MouvementStockSQLElement() {
        super("MOUVEMENT_STOCK", "un mouvement de stock", "mouvements de stock");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("NOM");
        l.add("ID_ARTICLE");
        l.add("QTE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("QTE");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {

        return new BaseSQLComponent(this) {

            private SQLTextCombo textLib;
            private JTextField textQte;
            private JDate date;

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Libellé
                JLabel labelLib = new JLabel(getLabelFor("NOM"), SwingConstants.RIGHT);
                this.add(labelLib, c);

                c.gridx++;
                c.weightx = 1;
                this.textLib = new SQLTextCombo();
                this.add(this.textLib, c);

                // Date
                c.gridx++;
                c.weightx = 0;
                JLabel labelDate = new JLabel(getLabelFor("DATE"), SwingConstants.RIGHT);
                this.add(labelDate, c);

                c.gridx++;
                this.date = new JDate(true);
                this.add(this.date, c);

                // Article
                final ElementComboBox articleSelect = new ElementComboBox();

                c.gridx = 0;
                c.gridy++;
                JLabel labelArticle = new JLabel(getLabelFor("ID_ARTICLE"), SwingConstants.RIGHT);
                this.add(labelArticle, c);

                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1;
                this.add(articleSelect, c);

                // QTE
                c.gridwidth = 1;
                c.weightx = 0;
                c.gridy++;
                c.gridx = 0;
                c.anchor = GridBagConstraints.EAST;
                JLabel labelQte = new JLabel(getLabelFor("QTE"), SwingConstants.RIGHT);
                this.add(labelQte, c);

                c.gridx++;
                c.fill = GridBagConstraints.NONE;
                this.textQte = new JTextField(6);
                c.weighty = 1;
                c.anchor = GridBagConstraints.NORTHWEST;
                this.add(this.textQte, c);
                DefaultGridBagConstraints.lockMinimumSize(this.textQte);
                DefaultGridBagConstraints.lockMaximumSize(this.textQte);
                this.addRequiredSQLObject(this.textQte, "QTE");
                this.addSQLObject(this.textLib, "NOM");
                this.addRequiredSQLObject(articleSelect, "ID_ARTICLE");
                this.addRequiredSQLObject(this.date, "DATE");
            }

            @Override
            public int insert(SQLRow order) {

                int id = super.insert(order);

                updateStock(Arrays.asList(id));

                return id;
            }

            @Override
            public void update() {

                int id = getSelectedID();
                updateStock(Arrays.asList(id), true);

                super.update();

                updateStock(Arrays.asList(id));
            }
        };
    }

    @Override
    protected void archive(SQLRow row, boolean cutLinks) throws SQLException {
        super.archive(row, cutLinks);

        updateStock(Arrays.asList(row.getID()), true);
    }

    public static CollectionMap<SQLRow, List<SQLRowValues>> updateStock(List<Integer> ids) {
        return updateStock(ids, false);
    }

    /**
     * Mise à jour des stocks ajoute la quantité si archive est à false
     * 
     * @param id mouvement stock
     * @param archive
     */
    public static CollectionMap<SQLRow, List<SQLRowValues>> updateStock(List<Integer> ids, boolean archive) {
        CollectionMap<SQLRow, List<SQLRowValues>> map = new CollectionMap<SQLRow, List<SQLRowValues>>();
        SQLTable tableCmdElt = Configuration.getInstance().getBase().getTable("COMMANDE_ELEMENT");
        for (Integer id : ids) {

            // Mise à jour des stocks
            SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
            SQLRow rowMvtStock = eltMvtStock.getTable().getRow(id);

            SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
            SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement(sqlTableArticle);
            SQLElement eltStock = Configuration.getInstance().getDirectory().getElement("STOCK");
            final SQLRow rowArticle = rowMvtStock.getForeignRow("ID_ARTICLE");
            SQLRow rowStock = rowArticle.getForeignRow(("ID_STOCK"));

            float qte = rowStock.getFloat("QTE_REEL");
            float qteMvt = rowMvtStock.getFloat("QTE");

            SQLRowValues rowVals = new SQLRowValues(eltStock.getTable());

            float qteNvlle;
            if (archive) {
                qteNvlle = qte - qteMvt;
            } else {
                qteNvlle = qte + qteMvt;
            }
            rowVals.put("QTE_REEL", qteNvlle);

            try {
                if (rowStock.getID() <= 1) {
                    SQLRow row = rowVals.insert();
                    SQLRowValues rowValsArt = new SQLRowValues(eltArticle.getTable());
                    rowValsArt.put("ID_STOCK", row.getID());

                    final int idArticle = rowArticle.getID();
                    if (idArticle > 1) {
                        rowValsArt.update(idArticle);
                    }
                } else {
                    rowVals.update(rowStock.getID());
                }
            } catch (SQLException e) {

                ExceptionHandler.handle("Erreur lors de la mise à jour du stock pour l'article " + rowArticle.getString("CODE"));
                e.printStackTrace();
            }

            DefaultProps props = DefaultNXProps.getInstance();
            String stockMin = props.getStringProperty("ArticleStockMin");
            Boolean bStockMin = !stockMin.equalsIgnoreCase("false");
            boolean gestionStockMin = (bStockMin == null || bStockMin.booleanValue());
            if (!archive && rowArticle.getTable().getFieldsName().contains("QTE_MIN") && gestionStockMin && qteNvlle < rowArticle.getInt("QTE_MIN")) {
                // final float qteShow = qteNvlle;
                SQLInjector inj = SQLInjector.getInjector(rowArticle.getTable(), tableCmdElt);
                SQLRowValues rowValsElt = new SQLRowValues(inj.createRowValuesFrom(rowArticle));

                rowValsElt.put("QTE", Math.round(rowArticle.getInt("QTE_MIN") - qteNvlle));
                rowValsElt.put("ID_TAXE", rowValsElt.getObject("ID_TAXE"));
                rowValsElt.put("T_POIDS", rowValsElt.getLong("POIDS") * rowValsElt.getInt("QTE"));
                rowValsElt.put("T_PA_HT", rowValsElt.getLong("PA_HT") * rowValsElt.getInt("QTE"));
                rowValsElt.put("T_PA_TTC", rowValsElt.getLong("T_PA_HT") * (rowValsElt.getForeign("ID_TAXE").getFloat("TAUX") / 100.0 + 1.0));

                map.put(rowArticle.getForeignRow("ID_FOURNISSEUR"), rowValsElt);
                // SwingUtilities.invokeLater(new Runnable() {
                //
                // @Override
                // public void run() {
                // JOptionPane.showMessageDialog(null, "L'article " + rowArticle.getString("NOM") +
                // " est en dessous du stock minimum (" + rowArticle.getFloat("QTE_MIN")
                // + ").\nIl vous en reste " + qteShow + " en stock.");
                // }
                // });
            }
        }
        return map;
    }

    public static void createCommandeF(final CollectionMap<SQLRow, List<SQLRowValues>> col, final SQLRow rowDevise) {

        if (col.keySet().size() > 0) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {

                    SQLElement commande = Configuration.getInstance().getDirectory().getElement("COMMANDE");
                    for (SQLRow fournisseur : col.keySet()) {

                        // On regarde si il existe une commande en cours existante
                        SQLSelect sel = new SQLSelect(commande.getTable().getBase());
                        sel.addSelectStar(commande.getTable());
                        Where w = new Where(commande.getTable().getField("EN_COURS"), "=", Boolean.TRUE);
                        w = w.and(new Where(commande.getTable().getField("ID_FOURNISSEUR"), "=", fournisseur.getID()));
                        sel.setWhere(w);

                        List<SQLRow> rowsCmd = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel));

                        SQLRow commandeExistante = null;
                        if (rowsCmd != null && rowsCmd.size() > 0) {
                            commandeExistante = rowsCmd.get(0);
                        }

                        EditFrame frame;
                        CommandeSQLComponent cmp;

                        if (commandeExistante != null) {

                            frame = new EditFrame(commande, EditMode.MODIFICATION);
                            cmp = (CommandeSQLComponent) frame.getSQLComponent();
                            cmp.select(commandeExistante);
                        } else {
                            frame = new EditFrame(commande);
                            cmp = (CommandeSQLComponent) frame.getSQLComponent();
                            SQLRowValues rowVals = new SQLRowValues(commande.getTable());
                            SQLElement eltComm = Configuration.getInstance().getDirectory().getElement("COMMERCIAL");
                            int idUser = UserManager.getInstance().getCurrentUser().getId();
                            SQLRow rowsComm = SQLBackgroundTableCache.getInstance().getCacheForTable(eltComm.getTable()).getFirstRowContains(idUser, eltComm.getTable().getField("ID_USER_COMMON"));

                            if (rowsComm != null) {
                                rowVals.put("ID_COMMERCIAL", rowsComm.getID());
                            }
                            rowVals.put("ID_FOURNISSEUR", fournisseur.getID());
                            if (rowDevise != null) {
                                rowVals.put("ID_DEVISE", rowDevise.getID());
                            }
                            cmp.select(rowVals);
                            cmp.getRowValuesTable().getRowValuesTable().getRowValuesTableModel().clearRows();
                        }

                        RowValuesTableModel model = cmp.getRowValuesTable().getRowValuesTable().getRowValuesTableModel();
                        SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement("ARTICLE");
                        for (SQLRowValues rowValsElt : (List<SQLRowValues>) col.get(fournisseur)) {
                            SQLRowValues rowValsMatch = null;
                            int index = 0;

                            for (int i = 0; i < model.getRowCount(); i++) {
                                SQLRowValues rowValsCmdElt = model.getRowValuesAt(i);
                                if (ReferenceArticleSQLElement.isReferenceEquals(rowValsCmdElt, rowValsElt)) {
                                    rowValsMatch = rowValsCmdElt;
                                    index = i;
                                    break;
                                }
                            }
                            if (rowValsMatch != null) {
                                int qte = rowValsMatch.getInt("QTE");
                                model.putValue(qte + rowValsElt.getInt("QTE"), index, "QTE");
                            } else {
                                model.addRow(rowValsElt);
                            }
                        }

                        frame.pack();
                        frame.setVisible(true);
                    }
                }
            });
        }

    }

    public static final void showSource(int id) {
        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLTable tableMvt = base.getTable("MOUVEMENT_STOCK");

        String stringTableSource = tableMvt.getRow(id).getString("SOURCE");
        EditFrame f;

        if (id != 1) {

            // Si une source est associée on l'affiche en readonly
            if (stringTableSource.trim().length() != 0 && tableMvt.getRow(id).getInt("IDSOURCE") != 1) {

                f = new EditFrame(Configuration.getInstance().getDirectory().getElement(stringTableSource), EditPanel.READONLY);
                f.selectionId(tableMvt.getRow(id).getInt("IDSOURCE"));

            } else {
                // Sinon on affiche le mouvement de stock
                f = new EditFrame(Configuration.getInstance().getDirectory().getElement(tableMvt), EditPanel.READONLY);
                f.selectionId(id);
            }
            f.pack();
            f.setVisible(true);
        } else {
            System.err.println("Aucun mouvement associé, impossible de modifier ou d'accéder à la source de cette ecriture!");
        }
    }
}
