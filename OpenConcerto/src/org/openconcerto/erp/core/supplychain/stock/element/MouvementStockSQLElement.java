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
import org.openconcerto.erp.core.supplychain.supplier.component.MouvementStockSQLComponent;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.TreesOfSQLRows;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.preferences.DefaultProps;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.ListMap;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JOptionPane;
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
        l.add("REEL");
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
        return new MouvementStockSQLComponent(this);
    }

    @Override
    protected void archive(SQLRow row, boolean cutLinks) throws SQLException {
        super.archive(row, cutLinks);
    }

    @Override
    protected void archive(TreesOfSQLRows trees, boolean cutLinks) throws SQLException {
        super.archive(trees, cutLinks);
        updateStock(trees.getRows(), true);

    }

    // public CollectionMap<SQLRow, List<SQLRowValues>> updateStock(List<Integer> ids) {
    // return updateStock(ids, false);
    // }

    private final SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");

    /**
     * Ajout des mouvements de Stock
     * 
     * @param rowOrigin SQLRow de la piece d'origine (ex : BL)
     * @param eltTable SQLTable des éléments de la pièce (ex : element du BL)
     * @param label label pour les mouvements de stocks
     * @param entry true si c'est une entrée de stock
     * @throws SQLException
     * @Deprecated
     */
    public void createMouvement(final SQLRow rowOrigin, SQLTable eltTable, StockLabel label, boolean entry, boolean real) throws SQLException {

        // TODO: if (SwingUtilities.isEventDispatchThread()) {
        // throw new IllegalStateException("This method must be called outside of EDT");
        // }
        // On récupére les articles qui composent la piéce
        SQLSelect selEltfact = new SQLSelect();
        selEltfact.addSelectStar(eltTable);
        selEltfact.setWhere(new Where((SQLField) eltTable.getForeignKeys(rowOrigin.getTable()).toArray()[0], "=", rowOrigin.getID()));

        List<SQLRow> lElt = SQLRowListRSH.execute(selEltfact);

        final boolean modeAvance = DefaultNXProps.getInstance().getBooleanValue("ArticleModeVenteAvance", false);
        SQLPreferences prefs = new SQLPreferences(eltTable.getDBRoot());
        final boolean createArticle = prefs.getBoolean(GestionArticleGlobalPreferencePanel.CREATE_ARTICLE_AUTO, true);

        if (lElt != null) {
            List<SQLRow> l = new ArrayList<SQLRow>();
            for (SQLRow rowElt : lElt) {
                SQLRow rowArticleAssocie = (rowElt.getTable().contains("ID_ARTICLE") ? rowElt.getForeign("ID_ARTICLE") : null);

                // Si on a bien sélectionné un article ou qu'il y a un code de saisi
                if ((rowArticleAssocie != null && !rowArticleAssocie.isUndefined()) || rowElt.getString("CODE").trim().length() > 0) {

                    // Si l'article est à créer ou le lien est à refaire (ancienne version saisie
                    // sans ID_ARTICLE en BD)
                    if (rowArticleAssocie == null || rowArticleAssocie.isUndefined()) {
                        // on récupére l'article qui lui correspond
                        SQLRowValues rowArticle = new SQLRowValues(sqlTableArticle);
                        for (SQLField field : sqlTableArticle.getFields()) {
                            if (rowElt.getTable().getFieldsName().contains(field.getName())) {
                                rowArticle.put(field.getName(), rowElt.getObject(field.getName()));
                            }
                        }
                        // rowArticle.loadAllSafe(rowEltFact);
                        int idArticle;
                        if (modeAvance)
                            idArticle = ReferenceArticleSQLElement.getIdForCNM(rowArticle, createArticle);
                        else {
                            idArticle = ReferenceArticleSQLElement.getIdForCN(rowArticle, createArticle);
                        }
                        // Fix row without article id
                        if (idArticle > 0 && idArticle != sqlTableArticle.getUndefinedID()) {
                            SQLRowValues rowVals = rowElt.asRowValues();
                            rowVals.put("ID_ARTICLE", idArticle);
                            rowVals.update();
                        }
                        rowArticleAssocie = rowElt.getTable().getTable("ARTICLE").getRow(idArticle);
                    }

                    if (rowArticleAssocie.getBoolean("GESTION_STOCK")) {

                        // on crée un mouvement de stock pour chacun des articles
                        SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
                        SQLRowValues rowVals = new SQLRowValues(eltMvtStock.getTable());

                        final int qte = rowElt.getInt("QTE");
                        final BigDecimal qteUV = rowElt.getBigDecimal("QTE_UNITAIRE");
                        double qteFinal = qteUV.multiply(new BigDecimal(qte), MathContext.DECIMAL128).doubleValue();

                        if (entry) {
                            rowVals.put("QTE", qteFinal);
                        } else {
                            rowVals.put("QTE", -qteFinal);
                        }

                        rowVals.put("NOM", label.getLabel(rowOrigin, rowElt));
                        rowVals.put("REEL", real);
                        rowVals.put("IDSOURCE", rowOrigin.getID());
                        rowVals.put("SOURCE", rowOrigin.getTable().getName());
                        rowVals.put("ID_ARTICLE", rowArticleAssocie.getID());
                        rowVals.put("DATE", rowOrigin.getObject("DATE"));
                        SQLRow row = rowVals.insert();

                        l.add(row);

                    }
                }
            }
            final ListMap<SQLRow, SQLRowValues> map = updateStock(l, false);
            if (map.size() > 0 && !rowOrigin.getTable().getName().equalsIgnoreCase("TICKET_CAISSE")) {
                if (!rowOrigin.getTable().contains("ID_TARIF")) {
                    System.err.println("Attention la table " + rowOrigin.getTable().getName()
                            + " ne contient pas le champ ID_TARIF. La création automatique d'une commande fournisseur est donc impossible!");
                    Thread.dumpStack();
                } else {
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            if (JOptionPane.showConfirmDialog(null, "Certains articles sont en dessous du stock minimum.\n Voulez vous créer une commande?") == JOptionPane.YES_OPTION) {
                                ComptaPropsConfiguration.getInstanceCompta().getNonInteractiveSQLExecutor().execute(new Runnable() {

                                    @Override
                                    public void run() {
                                        MouvementStockSQLElement.createCommandeF(map, rowOrigin.getForeignRow("ID_TARIF").getForeignRow("ID_DEVISE"));
                                    }
                                });

                            }

                        }
                    });

                }
            }
        }
    }

    /**
     * Mise à jour des stocks ajoute la quantité si archive est à false
     * 
     * @param id mouvement stock
     * @param archive
     */
    public ListMap<SQLRow, SQLRowValues> updateStock(Collection<SQLRow> rowsMvt, boolean archive) {
        // FIXME: if (SwingUtilities.isEventDispatchThread()) {
        // throw new IllegalStateException("This method must be called outside of EDT");
        // }
        // Stock Reel : inc/dec QTE_REEL, inc/dec QTE_LIV_ATTENTE/inc/dec
        // QTE_RECEPT_ATTENTE
        // Stock Th : inc/dec QTE_TH, inc/dec QTE_LIV_ATTENTE/inc/dec
        // QTE_RECEPT_ATTENTE

        final ListMap<SQLRow, SQLRowValues> map = new ListMap<SQLRow, SQLRowValues>();
        SQLTable tableCmdElt = Configuration.getInstance().getBase().getTable("COMMANDE_ELEMENT");
        for (SQLRow rowMvtStock : rowsMvt) {

            // Mise à jour des stocks
            SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
            SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement(sqlTableArticle);
            SQLElement eltStock = Configuration.getInstance().getDirectory().getElement("STOCK");
            final SQLRow rowArticle = rowMvtStock.getForeignRow("ID_ARTICLE");

            SQLRow rowStock = rowArticle.getForeignRow(("ID_STOCK"));
            if (rowMvtStock.getBoolean("REEL")) {
                float qte = rowStock.getFloat("QTE_REEL");
                float qteMvt = rowMvtStock.getFloat("QTE");

                SQLRowValues rowVals = new SQLRowValues(eltStock.getTable());

                float qteNvlle;
                float qteNvlleEnAttenteRecept = rowStock.getFloat("QTE_RECEPT_ATTENTE");
                float qteNvlleEnAttenteExp = rowStock.getFloat("QTE_LIV_ATTENTE");
                if (archive) {
                    qteNvlle = qte - qteMvt;
                    // Réception
                    if (qteMvt > 0) {
                        qteNvlleEnAttenteRecept += qteMvt;
                    } else {
                        // Livraison
                        qteNvlleEnAttenteExp -= qteMvt;
                    }
                } else {
                    qteNvlle = qte + qteMvt;
                    // Réception
                    if (qteMvt > 0) {
                        qteNvlleEnAttenteRecept -= qteMvt;
                    } else {
                        // Livraison
                        qteNvlleEnAttenteExp += qteMvt;
                    }
                }
                rowVals.put("QTE_REEL", qteNvlle);
                rowVals.put("QTE_RECEPT_ATTENTE", qteNvlleEnAttenteRecept);
                rowVals.put("QTE_LIV_ATTENTE", qteNvlleEnAttenteExp);

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
                }

                DefaultProps props = DefaultNXProps.getInstance();
                String stockMin = props.getStringProperty("ArticleStockMin");
                Boolean bStockMin = !stockMin.equalsIgnoreCase("false");
                boolean gestionStockMin = (bStockMin == null || bStockMin.booleanValue());
                if (!archive && rowArticle.getTable().getFieldsName().contains("QTE_MIN") && gestionStockMin && rowArticle.getObject("QTE_MIN") != null && qteNvlle < rowArticle.getInt("QTE_MIN")) {
                    // final float qteShow = qteNvlle;
                    SQLInjector inj = SQLInjector.getInjector(rowArticle.getTable(), tableCmdElt);
                    SQLRowValues rowValsElt = new SQLRowValues(inj.createRowValuesFrom(rowArticle));
                    rowValsElt.put("ID_STYLE", 2);
                    final SQLRow unite = rowArticle.getForeign("ID_UNITE_VENTE");
                    final float qteElt = rowArticle.getInt("QTE_MIN") - qteNvlle;
                    if (unite.isUndefined() || unite.getBoolean("A_LA_PIECE")) {
                        rowValsElt.put("QTE", Math.round(qteElt));
                        rowValsElt.put("QTE_UNITAIRE", BigDecimal.ONE);
                    } else {
                        rowValsElt.put("QTE", 1);
                        rowValsElt.put("QTE_UNITAIRE", new BigDecimal(qteElt));
                    }
                    rowValsElt.put("ID_TAXE", rowValsElt.getObject("ID_TAXE"));
                    rowValsElt.put("T_POIDS", rowValsElt.getLong("POIDS") * qteElt);
                    rowValsElt.put("T_PA_HT", rowValsElt.getLong("PA_HT") * qteElt);
                    rowValsElt.put("T_PA_TTC", rowValsElt.getLong("T_PA_HT") * (rowValsElt.getForeign("ID_TAXE").getFloat("TAUX") / 100.0 + 1.0));

                    map.add(rowArticle.getForeignRow("ID_FOURNISSEUR"), rowValsElt);

                }
            } else {
                float qte = rowStock.getFloat("QTE_TH");
                float qteMvt = rowMvtStock.getFloat("QTE");

                SQLRowValues rowVals = new SQLRowValues(eltStock.getTable());

                float qteNvlle;
                float qteNvlleEnAttenteRecept = rowStock.getFloat("QTE_RECEPT_ATTENTE");
                float qteNvlleEnAttenteExp = rowStock.getFloat("QTE_LIV_ATTENTE");

                if (archive) {
                    qteNvlle = qte - qteMvt;
                    // CommandeF
                    if (qteMvt > 0) {
                        qteNvlleEnAttenteRecept -= qteMvt;
                    } else {
                        // CommanceC
                        qteNvlleEnAttenteExp += qteMvt;
                    }
                } else {
                    qteNvlle = qte + qteMvt;
                    // CommandeF
                    if (qteMvt > 0) {
                        qteNvlleEnAttenteRecept += qteMvt;
                    } else {
                        // CommanceC
                        qteNvlleEnAttenteExp -= qteMvt;
                    }
                }
                rowVals.put("QTE_TH", qteNvlle);
                rowVals.put("QTE_RECEPT_ATTENTE", qteNvlleEnAttenteRecept);
                rowVals.put("QTE_LIV_ATTENTE", qteNvlleEnAttenteExp);

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
                }
            }

        }
        return map;
    }

    public static void createCommandeF(final ListMap<SQLRow, SQLRowValues> col, final SQLRow rowDevise) {
        createCommandeF(col, rowDevise, "");
    }

    public static void createCommandeF(final ListMap<SQLRow, SQLRowValues> col, final SQLRow rowDevise, final String ref) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("This method must be called outside of EDT");
        }
        if (col.size() > 0) {

            final SQLElement commande = Configuration.getInstance().getDirectory().getElement("COMMANDE");
            for (final Entry<SQLRow, List<SQLRowValues>> e : col.entrySet()) {
                final SQLRow fournisseur = e.getKey();
                // On regarde si il existe une commande en cours existante
                final SQLSelect sel = new SQLSelect();
                sel.addSelectStar(commande.getTable());
                Where w = new Where(commande.getTable().getField("EN_COURS"), "=", Boolean.TRUE);
                w = w.and(new Where(commande.getTable().getField("ID_FOURNISSEUR"), "=", fournisseur.getID()));
                sel.setWhere(w);

                final List<SQLRow> rowsCmd = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel));

                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
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
                            final SQLRowValues rowVals = new SQLRowValues(commande.getTable());
                            final SQLElement eltComm = Configuration.getInstance().getDirectory().getElement("COMMERCIAL");
                            int idUser = UserManager.getInstance().getCurrentUser().getId();
                            SQLRow rowsComm = SQLBackgroundTableCache.getInstance().getCacheForTable(eltComm.getTable()).getFirstRowContains(idUser, eltComm.getTable().getField("ID_USER_COMMON"));

                            if (rowsComm != null) {
                                rowVals.put("ID_COMMERCIAL", rowsComm.getID());
                            }
                            rowVals.put("ID_FOURNISSEUR", fournisseur.getID());
                            if (rowDevise != null) {
                                rowVals.put("ID_DEVISE", rowDevise.getID());
                            }
                            if (commande.getTable().contains("ID_ADRESSE")) {
                                rowVals.put("ID_ADRESSE", null);
                            }
                            rowVals.put("NOM", ref);
                            cmp.select(rowVals);
                            cmp.getRowValuesTable().getRowValuesTableModel().clearRows();
                        }

                        final RowValuesTableModel model = cmp.getRowValuesTable().getRowValuesTableModel();
                        for (SQLRowValues rowValsElt : e.getValue()) {
                            SQLRowValues rowValsMatch = null;
                            int index = 0;

                            for (int i = 0; i < model.getRowCount(); i++) {
                                final SQLRowValues rowValsCmdElt = model.getRowValuesAt(i);
                                if (ReferenceArticleSQLElement.isReferenceEquals(rowValsCmdElt, rowValsElt)) {
                                    rowValsMatch = rowValsCmdElt;
                                    index = i;
                                    break;
                                }
                            }
                            if (rowValsMatch != null) {
                                final int qte = rowValsMatch.getInt("QTE");
                                model.putValue(qte + rowValsElt.getInt("QTE"), index, "QTE");
                            } else {
                                model.addRow(rowValsElt);
                            }
                        }

                        frame.pack();
                        FrameUtil.show(frame);

                    }
                });

            }

        }

    }

    public static final void showSource(final int id) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("This method must be called from EDT");
        }
        if (id != 1) {
            final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
            final SQLTable tableMvt = base.getTable("MOUVEMENT_STOCK");
            final String stringTableSource = tableMvt.getRow(id).getString("SOURCE");
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {

                    final EditFrame f;
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
                    FrameUtil.show(f);

                }
            });
        } else {
            System.err.println("Aucun mouvement associé, impossible de modifier ou d'accéder à la source de cette ecriture!");
        }
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".transaction";
    }
}
