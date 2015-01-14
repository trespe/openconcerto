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
 
 package org.openconcerto.erp.core.sales.quote.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.sales.invoice.component.SaisieVenteFactureSQLComponent;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.quote.component.DevisSQLComponent;
import org.openconcerto.erp.core.sales.quote.report.DevisXmlSheet;
import org.openconcerto.erp.core.sales.quote.ui.QuoteEditGroup;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.sqlobject.ElementComboBoxUtils;
import org.openconcerto.sql.ui.StringWithId;
import org.openconcerto.sql.ui.light.GroupToLightUIConvertor;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.ui.light.ActivationOnSelectionControler;
import org.openconcerto.ui.light.ColumnSpec;
import org.openconcerto.ui.light.ColumnsSpec;
import org.openconcerto.ui.light.CustomEditorProvider;
import org.openconcerto.ui.light.LightControler;
import org.openconcerto.ui.light.LightUIButton;
import org.openconcerto.ui.light.LightUIButtonUnmanaged;
import org.openconcerto.ui.light.LightUIButtonWithContext;
import org.openconcerto.ui.light.LightUIComboElement;
import org.openconcerto.ui.light.LightUIDescriptor;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUILine;
import org.openconcerto.ui.light.LightUITextField;
import org.openconcerto.ui.light.Row;
import org.openconcerto.ui.light.RowSpec;
import org.openconcerto.ui.light.TableContent;
import org.openconcerto.ui.light.TableSpec;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.channels.IllegalSelectorException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

public class DevisSQLElement extends ComptaSQLConfElement {

    public static final String TABLENAME = "DEVIS";

    public DevisSQLElement() {
        this("un devis", "devis");
    }

    public DevisSQLElement(String singular, String plural) {
        super(TABLENAME, singular, plural);
        getRowActions().addAll(getDevisRowActions());
        setDefaultGroup(new QuoteEditGroup());
    }

    private List<RowAction> getDevisRowActions() {

        List<RowAction> rowsActions = new ArrayList<RowAction>();

        // List<RowAction> list = new ArrayList<RowAction>();
        // Transfert vers facture
        RowAction factureAction = getDevis2FactureAction();

        rowsActions.add(factureAction);

        // Voir le document
        RowAction actionTransfertCmd = getDevis2CmdFournAction();
        rowsActions.add(actionTransfertCmd);

        // Transfert vers commande
        RowAction commandeAction = getDevis2CmdCliAction();

        rowsActions.add(commandeAction);

        RowAction accepteEtCmdAction = getAcceptAndCmdClientAction();
        rowsActions.add(accepteEtCmdAction);

        // Marqué accepté
        RowAction accepteAction = getAcceptAction();

        rowsActions.add(accepteAction);

        // Marqué accepté
        RowAction refuseAction = getRefuseAction();

        rowsActions.add(refuseAction);

        // // Dupliquer
        RowAction cloneAction = getCloneAction();

        rowsActions.add(cloneAction);

        MouseSheetXmlListeListener mouseSheetXmlListeListener = new MouseSheetXmlListeListener(DevisXmlSheet.class);
        mouseSheetXmlListeListener.setGenerateHeader(true);
        mouseSheetXmlListeListener.setShowHeader(true);

        rowsActions.addAll(mouseSheetXmlListeListener.getRowActions());

        return rowsActions;
    }

    public RowAction getCloneAction() {
        return new RowAction(new AbstractAction() {
            private EditFrame editFrame;

            public void actionPerformed(ActionEvent e) {
                SQLRowAccessor selectedRow = IListe.get(e).getSelectedRow();

                if (this.editFrame == null) {
                    SQLElement eltFact = Configuration.getInstance().getDirectory().getElement("DEVIS");
                    this.editFrame = new EditFrame(eltFact, EditPanel.CREATION);
                }

                ((DevisSQLComponent) this.editFrame.getSQLComponent()).loadDevisExistant(selectedRow.getID());
                this.editFrame.setVisible(true);
            }
        }, true, "sales.quote.clone") {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                return (selection != null && selection.size() == 1);
            };
        };
    }

    public RowAction getRefuseAction() {
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SQLRowValues rowVals = IListe.get(e).getSelectedRow().asRow().createEmptyUpdateRow();
                rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.REFUSE);
                try {
                    rowVals.update();
                } catch (SQLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }, false, "sales.quote.refuse") {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                if (selection != null && selection.size() == 1) {
                    if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.EN_ATTENTE) {
                        return true;
                    }
                }
                return false;
            };
        };
    }

    public RowAction getAcceptAction() {
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SQLRow selectedRow = IListe.get(e).getSelectedRow().asRow();
                SQLRowValues rowVals = selectedRow.createEmptyUpdateRow();
                rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.ACCEPTE);
                try {
                    rowVals.update();
                } catch (SQLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }, false, "sales.quote.accept") {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                if (selection != null && selection.size() == 1) {
                    if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.EN_ATTENTE) {
                        return true;
                    }
                }
                return false;
            };
        };
    }

    public RowAction getDevis2FactureAction() {
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                TransfertBaseSQLComponent.openTransfertFrame(IListe.get(e).copySelectedRows(), "SAISIE_VENTE_FACTURE");
            }
        }, true, "sales.quote.create.invoice") {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                if (selection != null && selection.size() == 1) {
                    if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.ACCEPTE) {
                        return true;
                    }
                }
                return false;
            };
        };
    }

    public RowAction getDevis2CmdFournAction() {
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                final SQLRow selectedRow = IListe.get(e).fetchSelectedRow();
                ComptaPropsConfiguration.getInstanceCompta().getNonInteractiveSQLExecutor().execute(new Runnable() {

                    @Override
                    public void run() {
                        transfertCommande(selectedRow);

                    }
                });

            }
        }, false, "sales.quote.create.supplier.order") {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                if (selection != null && selection.size() == 1) {
                    if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.ACCEPTE) {
                        return true;
                    }
                }
                return false;
            };
        };
    }

    public RowAction getDevis2CmdCliAction() {
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                final List<SQLRowValues> copySelectedRows = IListe.get(e).copySelectedRows();
                transfertCommandeClient(copySelectedRows);
            }

        }, true, "sales.quote.create.customer.order") {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                if (selection != null && selection.size() == 1) {
                    if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.ACCEPTE) {
                        return true;
                    }
                }
                return false;
            };
        };
    }

    public RowAction getAcceptAndCmdClientAction() {
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SQLRow selectedRow = IListe.get(e).fetchSelectedRow();
                SQLRowValues rowVals = selectedRow.createEmptyUpdateRow();
                rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.ACCEPTE);
                try {
                    rowVals.update();
                } catch (SQLException e1) {
                    ExceptionHandler.handle("Erreur la de la mise à jour de l'état du devis!", e1);

                }
                transfertCommandeClient(IListe.get(e).copySelectedRows());
            }
        }, false, "sales.quote.accept.create.customer.order") {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                if (selection != null && selection.size() == 1) {
                    if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.EN_ATTENTE) {
                        return true;
                    }
                }
                return false;
            };
        };
    }

    private void transfertCommandeClient(final List<SQLRowValues> copySelectedRows) {

        SwingWorker<Boolean, Object> worker = new SwingWorker<Boolean, Object>() {
            @Override
            protected Boolean doInBackground() throws Exception {

                final SQLTable tableTransfert = getTable().getTable("TR_DEVIS");
                SQLRowValues rowVals = new SQLRowValues(tableTransfert);
                rowVals.put("ID_DEVIS", new SQLRowValues(getTable()).put("NUMERO", null));
                rowVals.put("ID_COMMANDE", null);
                rowVals.put("ID", null);

                final List<Number> lID = new ArrayList<Number>();
                for (SQLRowValues sqlRowValues : copySelectedRows) {
                    lID.add(sqlRowValues.getID());
                }

                SQLRowValuesListFetcher fetch = SQLRowValuesListFetcher.create(rowVals);
                fetch.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

                    @Override
                    public SQLSelect transformChecked(SQLSelect input) {
                        Where w = new Where(tableTransfert.getField("ID_DEVIS"), lID);
                        w = w.and(new Where(tableTransfert.getField("ID_COMMANDE_CLIENT"), "IS NOT", (Object) null));
                        input.setWhere(w);
                        return input;
                    }
                });

                List<SQLRowValues> rows = fetch.fetch();
                if (rows != null && rows.size() > 0) {
                    String numero = "";

                    for (SQLRowValues sqlRow : rows) {
                        numero += sqlRow.getForeign("ID_DEVIS").getString("NUMERO") + " ,";
                    }

                    numero = numero.substring(0, numero.length() - 2);
                    String label = "Attention ";
                    if (rows.size() > 1) {
                        label += " les devis " + numero + " ont déjà été transféré en commande!";
                    } else {
                        label += " le devis " + numero + " a déjà été transféré en commande!";
                    }
                    label += "\n Voulez vous continuer?";

                    int ans = JOptionPane.showConfirmDialog(null, label, "Transfert devis en commande", JOptionPane.YES_NO_OPTION);
                    if (ans == JOptionPane.NO_OPTION) {
                        return Boolean.FALSE;
                    }

                }
                return Boolean.TRUE;

            }

            @Override
            protected void done() {

                try {
                    Boolean b = get();
                    if (b != null && b) {
                        TransfertBaseSQLComponent.openTransfertFrame(copySelectedRows, "COMMANDE_CLIENT");
                    }
                } catch (Exception e) {
                    ExceptionHandler.handle("Erreur lors du transfert des devis en commande!", e);
                }
                super.done();
            }
        };
        worker.execute();
    }

    protected List<String> getComboFields() {
        List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        return l;
    }

    private void transfertCommande(final SQLRow row) {
        ComptaPropsConfiguration.getInstanceCompta().getNonInteractiveSQLExecutor().execute(new Runnable() {

            @Override
            public void run() {
                DevisItemSQLElement elt = (DevisItemSQLElement) Configuration.getInstance().getDirectory().getElement("DEVIS_ELEMENT");
                SQLTable tableCmdElt = Configuration.getInstance().getDirectory().getElement("COMMANDE_ELEMENT").getTable();
                SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement("ARTICLE");
                List<SQLRow> rows = row.getReferentRows(elt.getTable());
                final ListMap<SQLRow, SQLRowValues> map = new ListMap<SQLRow, SQLRowValues>();
                SQLRow rowDeviseF = null;
                for (SQLRow sqlRow : rows) {
                    // on récupére l'article qui lui correspond
                    SQLRowValues rowArticle = new SQLRowValues(eltArticle.getTable());
                    for (SQLField field : eltArticle.getTable().getFields()) {
                        if (sqlRow.getTable().getFieldsName().contains(field.getName())) {
                            rowArticle.put(field.getName(), sqlRow.getObject(field.getName()));
                        }
                    }

                    // rowArticle.loadAllSafe(rowEltFact);
                    int idArticle = ReferenceArticleSQLElement.getIdForCNM(rowArticle, true);
                    SQLRow rowArticleFind = eltArticle.getTable().getRow(idArticle);

                    SQLInjector inj = SQLInjector.getInjector(rowArticle.getTable(), tableCmdElt);
                    SQLRowValues rowValsElt = new SQLRowValues(inj.createRowValuesFrom(rowArticleFind));
                    rowValsElt.put("ID_STYLE", sqlRow.getObject("ID_STYLE"));
                    rowValsElt.put("QTE", sqlRow.getObject("QTE"));
                    rowValsElt.put("T_POIDS", rowValsElt.getLong("POIDS") * rowValsElt.getInt("QTE"));
                    rowValsElt.put("T_PA_HT", ((BigDecimal) rowValsElt.getObject("PA_HT")).multiply(new BigDecimal(rowValsElt.getInt("QTE"), MathContext.DECIMAL128)));
                    rowValsElt.put("T_PA_TTC",
                            ((BigDecimal) rowValsElt.getObject("T_PA_HT")).multiply(new BigDecimal(rowValsElt.getForeign("ID_TAXE").getFloat("TAUX") / 100.0 + 1.0), MathContext.DECIMAL128));

                    // gestion de la devise
                    rowDeviseF = sqlRow.getForeignRow("ID_DEVISE");
                    SQLRow rowDeviseHA = rowArticleFind.getForeignRow("ID_DEVISE_HA");
                    BigDecimal qte = new BigDecimal(rowValsElt.getInt("QTE"));
                    if (rowDeviseF != null && !rowDeviseF.isUndefined()) {
                        if (rowDeviseF.getID() == rowDeviseHA.getID()) {
                            rowValsElt.put("PA_DEVISE", rowArticleFind.getObject("PA_DEVISE"));
                            rowValsElt.put("PA_DEVISE_T", ((BigDecimal) rowArticleFind.getObject("PA_DEVISE")).multiply(qte, MathContext.DECIMAL128));
                            rowValsElt.put("ID_DEVISE", rowDeviseF.getID());
                        } else {
                            BigDecimal taux = (BigDecimal) rowDeviseF.getObject("TAUX");
                            rowValsElt.put("PA_DEVISE", taux.multiply((BigDecimal) rowValsElt.getObject("PA_HT")));
                            rowValsElt.put("PA_DEVISE_T", ((BigDecimal) rowValsElt.getObject("PA_DEVISE")).multiply(qte, MathContext.DECIMAL128));
                            rowValsElt.put("ID_DEVISE", rowDeviseF.getID());
                        }
                    }

                    map.add(rowArticleFind.getForeignRow("ID_FOURNISSEUR"), rowValsElt);

                }
                MouvementStockSQLElement.createCommandeF(map, rowDeviseF);
            }
        });

    }

    protected List<String> getListFields() {
        List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("DATE");
        l.add("ID_CLIENT");
        l.add("OBJET");
        l.add("ID_COMMERCIAL");
        l.add("T_HT");
        l.add("T_TTC");
        l.add("INFOS");
        return l;
    }

    @Override
    public CollectionMap<String, String> getShowAs() {

        CollectionMap<String, String> map = new CollectionMap<String, String>();
        map.put(null, "NUMERO");
        return map;
    }

    @Override
    public synchronized ListSQLRequest createListRequest() {
        return new ListSQLRequest(getTable(), getListFields()) {
            @Override
            protected void customizeToFetch(SQLRowValues graphToFetch) {
                super.customizeToFetch(graphToFetch);
                graphToFetch.put("ID_ETAT_DEVIS", null);
            }
        };
    }

    @Override
    protected Set<String> getChildren() {
        Set<String> set = new HashSet<String>();
        set.add("DEVIS_ELEMENT");
        return set;
    }

    @Override
    protected List<String> getPrivateFields() {
        List<String> s = new ArrayList<String>(1);
        s.add("ID_ADRESSE");
        return s;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
            return new DevisSQLComponent(this);
    }

    /**
     * Transfert d'un devis en facture
     * 
     * @param devisID
     */
    public void transfertFacture(int devisID) {

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        EditFrame editFactureFrame = new EditFrame(elt);
        editFactureFrame.setIconImage(new ImageIcon(Gestion.class.getResource("frameicon.png")).getImage());

        SaisieVenteFactureSQLComponent comp = (SaisieVenteFactureSQLComponent) editFactureFrame.getSQLComponent();

        comp.setDefaults();
        comp.loadDevis(devisID);

        editFactureFrame.pack();
        editFactureFrame.setState(JFrame.NORMAL);
        editFactureFrame.setVisible(true);
    }

    @Override
    public LightUIDescriptor getUIDescriptorForModification(PropsConfiguration configuration, long quoteId) {
        final GroupToLightUIConvertor convertor = new GroupToLightUIConvertor(configuration);
        convertor.setCustomEditorProvider("sales.quote.items.list", getItemsCustomEditorProvider(configuration, quoteId));
        final LightUIDescriptor desc = convertor.convert(getGroupForModification());

        return desc;
    }

    public LightUIDescriptor getUIDescriptorForCreation(PropsConfiguration configuration) {
        final GroupToLightUIConvertor convertor = new GroupToLightUIConvertor(configuration);
        convertor.setCustomEditorProvider("sales.quote.items.list", getItemsCustomEditorProvider(configuration, -1));
        final LightUIDescriptor desc = convertor.convert(getGroupForCreation());
        return desc;
    }

    CustomEditorProvider getItemsCustomEditorProvider(final PropsConfiguration configuration, final long quoteId) {
        return new CustomEditorProvider() {

            @Override
            public LightUIElement createUIElement(String id) {
                LightUIElement eList = new LightUIElement();
                eList.setId(id);
                eList.setType(LightUIElement.TYPE_LIST);
                eList.setFillWidth(true);

                ColumnSpec c1 = new ColumnSpec("sales.quote.item.style", StringWithId.class, "Style", new StringWithId(2, "Normal"), 50, true, new LightUIComboElement("sales.quote.item.style"));
                ColumnSpec c2 = new ColumnSpec("sales.quote.item.code", String.class, "Code", "", 50, true, new LightUITextField("sales.quote.item.code"));
                ColumnSpec c3 = new ColumnSpec("sales.quote.item.label", String.class, "Nom", "", 50, true, new LightUITextField("sales.quote.item.name"));
                ColumnSpec c4 = new ColumnSpec("sales.quote.item.description", String.class, "Descriptif", "", 50, true, new LightUITextField("sales.quote.item.description"));
                ColumnSpec c5 = new ColumnSpec("sales.quote.item.purchase.unit.price", BigDecimal.class, "P.U. Achat HT", new BigDecimal(0), 50, true, new LightUITextField(
                        "sales.quote.item.purchase.unit.price"));
                ColumnSpec c6 = new ColumnSpec("sales.quote.item.sales.unit.price", BigDecimal.class, "P.U. Vente HT", new BigDecimal(0), 50, true, new LightUITextField(
                        "sales.quote.item.sales.unit.price"));
                ColumnSpec c7 = new ColumnSpec("sales.quote.item.quantity", Integer.class, "Quantité", new BigDecimal(1), 50, true, new LightUITextField("sales.quote.item.quantity"));

                List<ColumnSpec> cols = new ArrayList<ColumnSpec>();
                cols.add(c1);
                cols.add(c2);
                cols.add(c3);
                cols.add(c4);
                cols.add(c5);
                cols.add(c6);
                cols.add(c7);
                List<String> visibleIds = new ArrayList<String>();
                for (ColumnSpec c : cols) {
                    visibleIds.add(c.getId());
                }

                ColumnsSpec columsSpec = new ColumnsSpec("sales.quote.items", cols, visibleIds, null);

                final TableSpec rawContent = new TableSpec();
                rawContent.setColumns(columsSpec);
                if (quoteId > 0) {
                    // send: id,value
                    SQLTable table = configuration.getDirectory().getElement("DEVIS_ELEMENT").getTable();
                    final List<SQLField> fieldsToFetch = new ArrayList<SQLField>();
                    for (ColumnSpec cs : cols) {
                        String colId = cs.getId();
                        SQLField f = configuration.getFieldMapper().getSQLFieldForItem(colId);
                        if (f != null) {
                            fieldsToFetch.add(f);
                        } else {
                            throw new IllegalStateException("No field in " + table + " for column id " + colId);
                        }
                    }

                    final Where where = new Where(table.getKey(), "=", quoteId);
                    List<SQLRowValues> fetchedRows = ElementComboBoxUtils.fetchRows(configuration, table, fieldsToFetch, where);

                    List<Row> rows = new ArrayList<Row>();
                    for (final SQLRowValues vals : fetchedRows) {
                        Row r = new Row(vals.getID(), cols.size());
                        List<Object> values = new ArrayList<Object>();
                        for (ColumnSpec cs : cols) {
                            String colId = cs.getId();
                            SQLField f = configuration.getFieldMapper().getSQLFieldForItem(colId);
                            if (f != null) {
                                Object object = vals.getObject(f.getName());
                                System.out.println("DevisSQLElement.getItemsCustomEditorProvider(...).createUIElement()" + f.getName() + ":" + object + ":" + object.getClass().getCanonicalName());
                                if (object instanceof SQLRowValues) {
                                    SQLRowValues sqlRowValues = (SQLRowValues) object;
                                    long rowId = sqlRowValues.getIDNumber().longValue();
                                    List<SQLField> fieldsToExpand = configuration.getShowAs().getFieldExpand(sqlRowValues.getTable());
                                    String strValue = "";
                                    for (SQLField sqlField : fieldsToExpand) {
                                        strValue += sqlRowValues.getObject(sqlField.getName()).toString() + " ";
                                    }
                                    strValue = strValue.trim();
                                    StringWithId str = new StringWithId(rowId, strValue);
                                    object = str;
                                }
                                values.add(object);
                            } else {
                                throw new IllegalStateException("No field in " + table + " for column id " + colId);
                            }
                        }
                        r.setValues(values);
                        rows.add(r);
                    }

                    TableContent tableContent = new TableContent();
                    tableContent.setRows(rows);
                    // tableContent.setSpec(new RowSpec());
                    rawContent.setContent(tableContent);

                }

                eList.setRawContent(rawContent);

                LightUIDescriptor desc = new LightUIDescriptor("sales.quote.items.list");
                desc.setGridWidth(1);
                desc.setFillWidth(true);

                LightUILine toolbarLine = new LightUILine();

                LightUIElement b1 = new LightUIElement();
                b1.setType(LightUIElement.TYPE_BUTTON_UNMANAGED);
                b1.setId("up");
                b1.setGridWidth(1);
                b1.setIcon("up.png");
                desc.addControler(new ActivationOnSelectionControler(id, b1.getId()));
                desc.addControler(new LightControler(LightControler.TYPE_UP, id, b1.getId()));
                toolbarLine.add(b1);

                LightUIElement b2 = new LightUIElement();
                b2.setType(LightUIElement.TYPE_BUTTON_UNMANAGED);
                b2.setId("down");
                b2.setGridWidth(1);
                b2.setIcon("down.png");
                desc.addControler(new ActivationOnSelectionControler(id, b2.getId()));
                desc.addControler(new LightControler(LightControler.TYPE_DOWN, id, b2.getId()));
                toolbarLine.add(b2);
                // Add
                LightUIElement addButton = createButton("add", "Ajouter une ligne");
                desc.addControler(new LightControler(LightControler.TYPE_ADD_DEFAULT, id, addButton.getId()));
                toolbarLine.add(addButton);
                // Insert
                LightUIElement insertButton = createButton("insert", "Insérer une ligne");
                desc.addControler(new LightControler(LightControler.TYPE_INSERT_DEFAULT, id, insertButton.getId()));
                toolbarLine.add(insertButton);

                // Copy
                LightUIElement copyButton = createButton("copy", "Dupliquer");
                desc.addControler(new ActivationOnSelectionControler(id, copyButton.getId()));
                desc.addControler(new LightControler(LightControler.TYPE_COPY, id, copyButton.getId()));
                toolbarLine.add(copyButton);

                // Remove
                LightUIElement removeButton = createButton("remove", "Supprimer");
                desc.addControler(new ActivationOnSelectionControler(id, removeButton.getId()));
                desc.addControler(new LightControler(LightControler.TYPE_REMOVE, id, removeButton.getId()));
                toolbarLine.add(removeButton);

                desc.addLine(toolbarLine);
                final LightUILine listLine = new LightUILine();
                listLine.setWeightY(1);
                listLine.setFillHeight(true);
                listLine.add(eList);

                //
                desc.addLine(listLine);

                return desc;
            }

            LightUIElement createButton(String id, String label) {
                LightUIElement b1 = new LightUIElement();
                b1.setType(LightUIElement.TYPE_BUTTON_UNMANAGED);
                b1.setId(id);
                b1.setGridWidth(1);
                b1.setLabel(label);
                return b1;
            }
        };
    }
}
