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
 
 package org.openconcerto.erp.model;

import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.generationDoc.SpreadSheetGeneratorCompta;
import org.openconcerto.erp.generationDoc.gestcomm.ReleveChequeEmisSheet;
import org.openconcerto.erp.generationDoc.gestcomm.ReleveChequeSheet;
import org.openconcerto.erp.generationEcritures.GenerationMvtReglementAvoirChequeClient;
import org.openconcerto.erp.generationEcritures.GenerationMvtReglementChequeClient;
import org.openconcerto.erp.generationEcritures.GenerationMvtReglementChequeFourn;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.AliasedTable;
import org.openconcerto.sql.model.FieldRef;
import org.openconcerto.sql.model.IResultSetHandler;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLData;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLFieldsSet;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableListener;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.ExceptionHandler;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingWorker;
import javax.swing.event.EventListenerList;
import javax.swing.table.AbstractTableModel;

public class GestionChequesModel extends AbstractTableModel {

    private List<Map<String, Object>> cheque = new ArrayList<Map<String, Object>>();
    private SQLTableListener tableListener;
    private EventListenerList loadingListener = new EventListenerList();

    // Table qui contient les cheques
    private SQLTable chqTable;

    // colonnes du modeles
    private List<FieldRef> fields;

    // field à cocher (decaissé ou encaissé)
    private SQLField fieldSelect;

    // field date minimum de depot ou d'encaissement
    private SQLField fieldDateMin;

    /**
     * Crée un model permettant de gérer l'encaissement ou le décaissement des cheques
     * 
     * @param table SQLTable contenant les cheques
     * @param fields liste des colonnes à afficher
     * @param select SQLField boolean DECAISSE ou ENCAISSE
     * @param fieldDate date minimum de depot
     */
    public GestionChequesModel(SQLTable table, List<FieldRef> fields, SQLField select, SQLField fieldDate) {
        this.chqTable = table;

        this.fields = fields;
        this.fieldSelect = select;
        this.fieldDateMin = fieldDate;

        if (!fields.contains(select)) {
            throw new IllegalArgumentException("Le field select n'est dans aucune colonne.");
        }

        // loadCheque();
        this.tableListener = new SQLTableListener() {
            public void rowModified(SQLTable table, int id) {

                loadCheque();
                fireTableDataChanged();
            }

            public void rowAdded(SQLTable table, int id) {
                loadCheque();
                fireTableDataChanged();
            }

            public void rowDeleted(SQLTable table, int id) {
                loadCheque();
                fireTableDataChanged();
            }
        };

        this.chqTable.addTableListener(this.tableListener);

    }

    public Class<?> getColumnClass(int c) {

        final FieldRef field = this.fields.get(c);

        if (field.getField().getName().equalsIgnoreCase("ID_MOUVEMENT")) {
            return String.class;
        } else {
            return field.getField().getType().getJavaType();
        }
    }

    public int getRowCount() {
        return this.cheque.size();
    }

    public int getColumnCount() {

        return this.fields.size();
    }

    public String getColumnName(int col) {
        final FieldRef field = this.fields.get(col);
        if (field.getField().getName().equalsIgnoreCase("ID_MOUVEMENT")) {
            return "Source";
        } else {
            return Configuration.getInstance().getTranslator().getLabelFor(field.getField());
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {

        final FieldRef field = this.fields.get(columnIndex);
        if (field.getField().getName().equalsIgnoreCase("ID_MOUVEMENT")) {
            // Integer i = (Integer) this.cheque.get(rowIndex).get(field.getField().getName());
            // if (i != null && i.intValue() > 1) {
            // int idScr = MouvementSQLElement.getSourceId(i);
            // SQLTable tableMvt =
            // Configuration.getInstance().getDirectory().getElement("MOUVEMENT").getTable();
            // if (idScr > 1) {
            // SQLRow rowMvt = tableMvt.getRow(idScr);
            // String source = rowMvt.getString("SOURCE");
            // int idSource = rowMvt.getInt("IDSOURCE");
            // SQLElement eltSource = Configuration.getInstance().getDirectory().getElement(source);
            // if (eltSource != null) {
            // final SQLTable table = eltSource.getTable();
            // SQLRow rowSource = table.getRow(idSource);
            // final Set<String> fields2 = table.getFieldsName();
            //
            // if (rowSource != null) {
            // if (fields2.contains("NUMERO")) {
            // return rowSource.getString("NUMERO");
            // } else {
            // if (fields2.contains("NOM")) {
            // return rowSource.getString("NOM");
            // }
            // }
            // }
            // }
            // }
            //
            // }
            // return "";
            return this.cheque.get(rowIndex).get("SOURCE_MOUVEMENT");
        } else {
            return this.cheque.get(rowIndex).get(field.getField().getName());
        }
    }

    public void fireIsLoading(boolean b) {
        for (LoadingTableListener l : this.loadingListener.getListeners(LoadingTableListener.class)) {
            l.isLoading(b);
        }
    }

    public void addLoadingListener(LoadingTableListener l) {
        this.loadingListener.add(LoadingTableListener.class, l);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

        this.cheque.get(rowIndex).put(this.fields.get(columnIndex).getField().getName(), aValue);
        fireTableDataChanged();
    }

    public boolean isCellEditable(int row, int col) {
        return this.fields.get(col).equals(this.fieldSelect);
    }

    SwingWorker<String, Object> worker;

    /**
     * Charge le model avec les cheques non decaisse, ou non encaisse
     * 
     */
    public void loadCheque() {
        // FIXME cancel worker and restart load
        if (worker != null && !worker.isDone()) {
            return;
        }
        fireIsLoading(true);
        worker = new SwingWorker<String, Object>() {

            @SuppressWarnings("unchecked")
            @Override
            protected String doInBackground() throws Exception {

                SQLBase base = GestionChequesModel.this.chqTable.getBase();
                // System.err.println("Load Cheque");
                final SQLSelect selCheque = new SQLSelect(base);
                selCheque.setWaitPreviousWriteTX(true);
                Where w = new Where(GestionChequesModel.this.fieldSelect, "=", Boolean.FALSE);
                w = w.and(new Where(GestionChequesModel.this.chqTable.getField("REG_COMPTA"), "=", Boolean.FALSE));
                for (int i = 0; i < GestionChequesModel.this.fields.size(); i++) {
                    FieldRef f = GestionChequesModel.this.fields.get(i);

                    selCheque.addSelect(f);

                    if (!f.getField().getTable().equals(GestionChequesModel.this.chqTable)) {

                        Set<SQLField> s = GestionChequesModel.this.chqTable.getForeignKeys(f.getField().getTable());

                        if (s != null && s.size() > 0) {
                            for (SQLField field : s) {
                                w = w.and(new Where(field, "=", f.getField().getTable().getKey()));
                            }
                        } else {
                            AliasedTable aliasedTable = new AliasedTable(f.getField().getTable(), "CLIENT_COMMUN");
                            final SQLTable clientTable = chqTable.getForeignTable("ID_CLIENT");
                            w = w.and(new Where(chqTable.getField("ID_CLIENT"), "=", clientTable.getKey()));
                            w = w.and(new Where(clientTable.getField("ID_CLIENT"), "=", aliasedTable.getField("ID")));
                        }
                    }
                }
                selCheque.addSelect(GestionChequesModel.this.chqTable.getKey());
                selCheque.addSelect(GestionChequesModel.this.chqTable.getField("ID_MOUVEMENT"));

                selCheque.setWhere(w);

                String reqCheque = selCheque.asString();

                GestionChequesModel.this.cheque = (List<Map<String, Object>>) base.getDataSource().execute(reqCheque, new IResultSetHandler(SQLDataSource.MAP_LIST_HANDLER) {
                    @Override
                    public Set<? extends SQLData> getCacheModifiers() {
                        return new SQLFieldsSet(selCheque.getSelectFields()).getTables();
                    }
                });

                for (Map<String, Object> m : GestionChequesModel.this.cheque) {

                    Number i = (Number) m.get("ID_MOUVEMENT");
                    m.put("SOURCE_MOUVEMENT", "");
                    if (i != null && i.intValue() > 1) {
                        int idScr = MouvementSQLElement.getSourceId(i.intValue());
                        SQLTable tableMvt = Configuration.getInstance().getDirectory().getElement("MOUVEMENT").getTable();
                        if (idScr > 1) {
                            SQLRow rowMvt = tableMvt.getRow(idScr);
                            String source = rowMvt.getString("SOURCE");
                            int idSource = rowMvt.getInt("IDSOURCE");
                            SQLElement eltSource = Configuration.getInstance().getDirectory().getElement(source);
                            if (eltSource != null) {
                                final SQLTable table = eltSource.getTable();
                                SQLRow rowSource = table.getRow(idSource);
                                final Set<String> fields2 = table.getFieldsName();

                                if (rowSource != null) {
                                    if (fields2.contains("NUMERO")) {
                                        m.put("SOURCE_MOUVEMENT", rowSource.getString("NUMERO"));
                                    } else {
                                        if (fields2.contains("NOM")) {
                                            m.put("SOURCE_MOUVEMENT", rowSource.getString("NOM"));
                                        }
                                    }
                                }
                            }
                        }

                    }
                }

                return null;
            }

            @Override
            protected void done() {
                fireTableDataChanged();
                fireIsLoading(false);
            }
        };
        worker.execute();
    }

    /**
     * 
     * @return le nombre de cheque sélectionné
     */
    public int getNbChequeSelected() {
        int nbCheq = 0;
        for (int i = 0; i < this.cheque.size(); i++) {
            Map<String, Object> chqTmp = this.cheque.get(i);
            Boolean b = (Boolean) chqTmp.get(this.fieldSelect.getName());
            if ((b != null) && (b.booleanValue())) {
                nbCheq++;
            }
        }
        return nbCheq;
    }

    /**
     * 
     * @return le montant total des cheques selectionne en cents
     */
    public long getMontantTotalSelected() {
        long montant = 0;
        for (int i = 0; i < this.cheque.size(); i++) {
            Map<String, Object> chqTmp = this.cheque.get(i);
            Boolean b = (Boolean) chqTmp.get(this.fieldSelect.getName());
            if ((b != null) && (b.booleanValue())) {
                Long l = (Long) chqTmp.get("MONTANT");

                montant += ((l == null) ? 0 : l.longValue());
            }
        }
        return montant;
    }

    /**
     * Selectionne tous les cheques ayant depasse la date minimum de depot
     * 
     */
    public void selectionDecaisseAll() {
        if (this.cheque == null) {
            return;
        }
        for (int i = 0; i < this.cheque.size(); i++) {

            Map<String, Object> chqTmp = this.cheque.get(i);

            Date d = (Date) chqTmp.get(this.fieldDateMin.getName());
            if (d != null && !d.after(new Date())) {
                chqTmp.put(this.fieldSelect.getName(), Boolean.TRUE);
            } else {
                chqTmp.put(this.fieldSelect.getName(), Boolean.FALSE);
            }
        }
        this.fireTableDataChanged();
    }

    public void deselectionAll() {

        for (int i = 0; i < this.cheque.size(); i++) {

            Map<String, Object> chqTmp = this.cheque.get(i);
            chqTmp.put(this.fieldSelect.getName(), Boolean.FALSE);
        }
        this.fireTableDataChanged();
    }

    public Date getDateMinimum(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < this.getRowCount()) {
            return (Date) this.cheque.get(rowIndex).get(this.fieldDateMin.getName());
        } else {
            throw new IllegalArgumentException();
        }
    }

    public int getIdAtRow(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < this.getRowCount()) {
            return ((Number) this.cheque.get(rowIndex).get(this.chqTable.getKey().getName())).intValue();
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static final int MODE_ACHAT = 1;
    public static final int MODE_VENTE = 2;
    public static final int MODE_AVOIR = 3;

    public void valideDepot(Date d, int mode, boolean print) {
        valideDepot(d, mode, print, null);
    }

    /**
     * Valider le depot des cheques
     * 
     * @param d Date de dépot
     * @param mode
     * @param print
     * @param s libellé pour l'ecriture en banque
     */
    public void valideDepot(Date d, int mode, boolean print, String s) {

        Map<String, Object> m = new HashMap<String, Object>();

        if (mode == MODE_VENTE) {
            m.put("ENCAISSE", Boolean.TRUE);
            m.put("DATE_DEPOT", d);
        } else {
            m.put("DECAISSE", Boolean.TRUE);
            m.put("DATE_DECAISSE", d);
        }

        // chqTable.removeTableListener(tableListener);
        List<Map<String, Object>> l = new ArrayList<Map<String, Object>>(this.cheque);
        List<Integer> listeCheque = new ArrayList<Integer>();
        for (int i = 0; i < l.size(); i++) {

            Map<String, Object> chqTmp = l.get(i);

            Boolean b = (Boolean) chqTmp.get(this.fieldSelect.getName());

            if ((b != null) && (b.booleanValue())) {

                SQLRowValues valChq = new SQLRowValues(this.chqTable, m);
                Number id = (Number) chqTmp.get(this.chqTable.getKey().getName());
                SQLRow rowCheque = this.chqTable.getRow(id.intValue());

                if (!rowCheque.getBoolean(this.fieldSelect.getName())) {
                    listeCheque.add(id.intValue());
                    try {
                        // if (valChq.getInvalid() == null) {

                        // ajout de l'ecriture
                        valChq.update(id.intValue());

                        Number idMvt = (Number) chqTmp.get("ID_MOUVEMENT");

                        if (mode == MODE_AVOIR) {
                            GenerationMvtReglementAvoirChequeClient gen = new GenerationMvtReglementAvoirChequeClient(idMvt.intValue(), Long.valueOf(chqTmp.get("MONTANT").toString()), d,
                                    id.intValue());
                            gen.genere();
                        } else {
                            if (mode == MODE_ACHAT) {
                                new GenerationMvtReglementChequeFourn(idMvt.intValue(), Long.valueOf(chqTmp.get("MONTANT").toString()).longValue(), id.intValue(), d);
                            } else {
                                if (mode == MODE_VENTE) {
                                    GenerationMvtReglementChequeClient gen = new GenerationMvtReglementChequeClient(idMvt.intValue(), Long.valueOf(chqTmp.get("MONTANT").toString()), d, id.intValue(),
                                            s);
                                    gen.genere();
                                } else {
                                    System.err.println("Aucune écriture générer pour le cheque " + id + " mode inconnu");
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur pendant la mise à jour dans la table " + valChq.getTable().getName());
                        ExceptionHandler.handle("Erreur lors de la génération des écritures du chéques + " + this.chqTable.getName() + " ID : " + id);
                        e.printStackTrace();
                    }
                }
            }
        }

        if (print) {
            if (mode == MODE_ACHAT) {
                ReleveChequeEmisSheet sheet = new ReleveChequeEmisSheet(listeCheque);
                new SpreadSheetGeneratorCompta(sheet, "ReleveChequeEmis", false, true);
            } else {
                if (mode == MODE_VENTE) {
                    ReleveChequeSheet sheet = new ReleveChequeSheet(listeCheque, d);
                    sheet.createDocumentAsynchronous();
                    sheet.showPrintAndExportAsynchronous(true, false, true);
                }
            }
        }

    }

    /**
     * Apercu du relevè
     * 
     * @param mode
     */
    public void printPreview(int mode) {

        // chqTable.removeTableListener(tableListener);
        List<Map<String, Object>> l = new ArrayList<Map<String, Object>>(this.cheque);
        List<Integer> listeCheque = new ArrayList<Integer>();
        for (int i = 0; i < l.size(); i++) {

            Map<String, Object> chqTmp = l.get(i);

            Boolean b = (Boolean) chqTmp.get(this.fieldSelect.getName());

            if ((b != null) && (b.booleanValue())) {

                Number id = (Number) chqTmp.get(this.chqTable.getKey().getName());
                SQLRow rowCheque = this.chqTable.getRow(id.intValue());

                if (!rowCheque.getBoolean(this.fieldSelect.getName())) {
                    listeCheque.add(id.intValue());
                }
            }
        }

        if (mode == MODE_ACHAT) {
            ReleveChequeEmisSheet sheet = new ReleveChequeEmisSheet(listeCheque);
            new SpreadSheetGeneratorCompta(sheet, "ReleveChequeEmis", false, true);
        } else {
            if (mode == MODE_VENTE) {
                ReleveChequeSheet sheet = new ReleveChequeSheet(listeCheque, new Date(), true);
                sheet.createDocumentAsynchronous();
                sheet.showPrintAndExportAsynchronous(true, false, true);
            }
        }

    }
}
