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

import org.openconcerto.erp.core.finance.payment.element.ChequeType;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.sql.view.list.ListSQLLine;
import org.openconcerto.utils.ExceptionHandler;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the selection and the actions on that selection.
 * 
 * @author Sylvain
 * @see #valideDepot(Date, boolean, String)
 * @see #printPreview()
 */
public class GestionChequesModel {

    // Table qui contient les cheques
    private SQLTable chqTable;

    private final IListe list;
    private final ChequeType chequeType;

    /**
     * Crée un modèle permettant de gérer l'encaissement ou le décaissement des chèques.
     * 
     * @param list list of cheques.
     * @param fields fields.
     */
    public GestionChequesModel(final IListe list, ChequeType fields) {
        this.list = list;
        this.chqTable = list.getSource().getPrimaryTable();
        this.chequeType = fields;
    }

    public final ChequeType getChequeType() {
        return this.chequeType;
    }

    private final IListe getList() {
        return this.list;
    }

    /**
     * 
     * @return le nombre de cheque sélectionné
     */
    public int getNbChequeSelected() {
        return this.getList().getSelection().getSelectedIDs().size();
    }

    public final void addSelectionListener(final PropertyChangeListener l) {
        this.getList().getSelection().addPropertyChangeListener("selectedIDs", l);
    }

    /**
     * 
     * @return le montant total des cheques selectionne en cents
     */
    public long getMontantTotalSelected() {
        long montant = 0;
        for (final SQLRowAccessor chqTmp : this.getList().getSelectedRows()) {
            final Number amount = (Number) chqTmp.getObject("MONTANT");
            montant += amount == null ? 0 : amount.longValue();
        }
        return montant;
    }

    /**
     * Selectionne tous les cheques ayant depasse la date minimum de depot
     * 
     */
    // TODO rename to selectAfterMinimumDate()
    public void selectionDecaisseAll() {
        final Date now = new Date();
        final ITableModel model = getList().getModel();
        final int rowCount = model.getRowCount();
        final List<Integer> ids = new ArrayList<Integer>(rowCount / 2);
        for (int i = 0; i < rowCount; i++) {
            final ListSQLLine row = model.getRow(i);
            final Calendar dateMin = row.getRow().getDate("DATE_MIN_DEPOT");
            if (dateMin != null && now.after(dateMin.getTime()))
                ids.add(row.getID());
        }
        this.getList().selectIDs(ids);
    }

    // TODO rename to selectNone()
    public void deselectionAll() {
        this.getList().selectIDs(Collections.<Integer> emptyList());
    }

    public void valideDepot(Date d, boolean print) {
        valideDepot(d, print, null);
    }

    /**
     * Valider le depot des cheques
     * 
     * @param d Date de dépot
     * @param mode
     * @param print
     * @param s libellé pour l'ecriture en banque
     */
    public void valideDepot(Date d, boolean print, String s) {
        final String fieldSelect = this.chequeType.getDoneFieldName();
        Map<String, Object> m = new HashMap<String, Object>();

        m.put(fieldSelect, Boolean.TRUE);
        m.put(this.chequeType.getDateFieldName(), d);

        final List<SQLRowAccessor> selection = this.getList().getSelectedRows();
        List<Integer> listeCheque = new ArrayList<Integer>();
        for (final SQLRowAccessor rowCheque : selection) {
            SQLRowValues valChq = new SQLRowValues(this.chqTable, m);
            Number id = rowCheque.getIDNumber();

            if (!rowCheque.getBoolean(fieldSelect)) {
                listeCheque.add(id.intValue());
                try {
                    // if (valChq.getInvalid() == null) {

                    // ajout de l'ecriture
                    valChq.update(id.intValue());

                    this.chequeType.handle(rowCheque, d, s);
                } catch (Exception e) {
                    System.err.println("Erreur pendant la mise à jour dans la table " + valChq.getTable().getName());
                    ExceptionHandler.handle("Erreur lors de la génération des écritures du chéques + " + this.chqTable.getName() + " ID : " + id);
                    e.printStackTrace();
                }
            }
        }

        if (print) {
            this.print(false, d);
        }
    }

    public void printPreview() {
        this.print(true, new Date());
    }

    private void print(final boolean preview, final Date d) {
        final String fieldSelect = this.chequeType.getDoneFieldName();

        final List<SQLRowAccessor> selection = this.getList().getSelectedRows();
        List<Integer> listeCheque = new ArrayList<Integer>();
        for (final SQLRowAccessor rowCheque : selection) {
            Number id = rowCheque.getIDNumber();

            if (!rowCheque.getBoolean(fieldSelect)) {
                listeCheque.add(id.intValue());
            }
        }

        this.chequeType.print(listeCheque, preview, d);
    }
}
