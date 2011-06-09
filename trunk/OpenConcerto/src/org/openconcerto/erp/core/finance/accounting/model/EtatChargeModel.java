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
 
 package org.openconcerto.erp.core.finance.accounting.model;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

/***************************************************************************************************
 * JTableModel affichant l'etat des charges de paye pour une periode et une caisse de cotisation
 * donnée
 **************************************************************************************************/
public class EtatChargeModel extends AbstractTableModel {

    // Données du model --> ((IDSOURCE),RowValues de FichePayeElement)
    private Map mapCot = new HashMap();
    private List listRubCaisse;

    // Total des cotisations pour une caisse
    private float cotSal, cotPat;
    private int moisDe, moisAu;
    private int annee, idCaisse;

    private final static SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
    private static final SQLTable tableFichePaye = base.getTable("FICHE_PAYE");
    private static final SQLTable tableFichePayeElt = base.getTable("FICHE_PAYE_ELEMENT");
    private static final SQLTable tableRubCot = Configuration.getInstance().getBase().getTable("RUBRIQUE_COTISATION");

    private String[] titres;

    public EtatChargeModel(int moisDe, int moisAu, int annee, int idCaisse) {

        this.annee = annee;
        this.moisDe = moisDe;
        this.moisAu = moisAu;
        this.idCaisse = idCaisse;

        fill();

        this.titres = new String[7];
        this.titres[0] = "Nom";
        this.titres[1] = "Base";
        this.titres[2] = "Taux Sal.";
        this.titres[3] = "Montant Sal. Ded.";
        this.titres[4] = "Taux Pat.";
        this.titres[5] = "Montant Pat.";
        this.titres[6] = "Total";
    }

    private void fill() {
        new SwingWorker<String, Object>() {

            @Override
            protected String doInBackground() throws Exception {

                if (listRubCaisse == null) {
                    fillMapCaisse();
                }

                // this.vectCot = new Vector();
                EtatChargeModel.this.mapCot = new HashMap();
                EtatChargeModel.this.cotSal = 0.0F;
                EtatChargeModel.this.cotPat = 0.0F;

                // Requete
                SQLSelect selElt = new SQLSelect(base);
                selElt.addSelect(tableFichePayeElt.getField("SOURCE"));
                selElt.addSelect(tableFichePayeElt.getField("IDSOURCE"));
                selElt.addSelect(tableFichePayeElt.getField("NOM"));
                selElt.addSelect(tableFichePayeElt.getField("NB_BASE"));
                selElt.addSelect(tableFichePayeElt.getField("TAUX_SAL"));
                selElt.addSelect(tableFichePayeElt.getField("MONTANT_SAL_AJ"));
                selElt.addSelect(tableFichePayeElt.getField("MONTANT_SAL_DED"));
                selElt.addSelect(tableFichePayeElt.getField("TAUX_PAT"));
                selElt.addSelect(tableFichePayeElt.getField("MONTANT_PAT"));
                Where w = new Where(tableFichePaye.getField("VALIDE"), "=", Boolean.TRUE);
                Where w2 = new Where(tableFichePaye.getField("ANNEE"), "=", EtatChargeModel.this.annee);
                Where w3 = new Where(tableFichePaye.getField("ID_MOIS"), new Integer(EtatChargeModel.this.moisDe), new Integer(EtatChargeModel.this.moisAu));
                Where w4 = new Where(tableFichePayeElt.getField("ID_FICHE_PAYE"), "=", tableFichePaye.getField("ID"));
                Where w5 = new Where(tableFichePayeElt.getField("SOURCE"), "=", tableRubCot.getName());
                // FIXME prefixer tous les champs par le nom du schema --> voir avec Sylvain
                // Where w6 = new Where(tableFichePayeElt.getField("IDSOURCE"), "=",
                // "`GestionCommon`." +
                // tableRubCot.getField("ID").getFullName());
                // Where w7 = new Where(tableRubCot.getField("ID_CAISSE_COTISATION"), "=",
                // this.idCaisse);

                selElt.setWhere(w.and(w2).and(w3).and(w4).and(w5));// .and(w6).and(w7));

                String req = selElt.asString();

                // StringBuffer req = new StringBuffer("SELECT \"" + base.getName() + "\"." +
                // tableFichePayeElt.getField("SOURCE").getFullName());
                // req.append(", \"" + base.getName() + "\"." +
                // tableFichePayeElt.getField("IDSOURCE").getFullName());
                // req.append(", \"" + base.getName() + "\"." +
                // tableFichePayeElt.getField("NOM").getFullName());
                // req.append(", \"" + base.getName() + "\"." +
                // tableFichePayeElt.getField("NB_BASE").getFullName());
                // req.append(", \"" + base.getName() + "\"." +
                // tableFichePayeElt.getField("TAUX_SAL").getFullName());
                // req.append(", \"" + base.getName() + "\"." +
                // tableFichePayeElt.getField("MONTANT_SAL_AJ").getFullName());
                // req.append(", \"" + base.getName() + "\"." +
                // tableFichePayeElt.getField("MONTANT_SAL_DED").getFullName());
                // req.append(", \"" + base.getName() + "\"." +
                // tableFichePayeElt.getField("TAUX_PAT").getFullName());
                // req.append(", \"" + base.getName() + "\"." +
                // tableFichePayeElt.getField("MONTANT_PAT").getFullName());
                //
                // req.append(" FROM \"" + base.getName() + "\"." + tableFichePaye.getName());
                // req.append(", \"" + base.getName() + "\"." + tableFichePayeElt.getName());
                // req.append(", \"" + tableRubCot.getBase().getName() + "\"." +
                // tableRubCot.getName());
                //
                // req.append(" WHERE (\"" + base.getName() + "\"." + "FICHE_PAYE.ID!=1)");
                // req.append(" AND (\"" + base.getName() + "\"." + "FICHE_PAYE.ARCHIVE=0)");
                //
                // req.append(" AND (\"" + base.getName() + "\"." + "FICHE_PAYE_ELEMENT.ID!=1)");
                // req.append(" AND (\"" + base.getName() + "\"." +
                // "FICHE_PAYE_ELEMENT.ARCHIVE=0)");
                //
                // req.append(" AND (\"" + base.getName() + "\"." + "FICHE_PAYE.VALIDE=1)");
                // req.append(" AND (\"" + base.getName() + "\"." + "FICHE_PAYE.ANNEE=" + this.annee
                // + ")");
                // req.append(" AND (\"" + base.getName() + "\"." + "FICHE_PAYE.ID_MOIS BETWEEN " +
                // this.moisDe + " AND " + this.moisAu + ")");
                //
                // req.append(" AND (\"" + base.getName() + "\"." +
                // "FICHE_PAYE_ELEMENT.ID_FICHE_PAYE = \""
                // + base.getName() + "\"." + "FICHE_PAYE.ID)");
                // req.append(" AND (\"" + base.getName() + "\"." + "FICHE_PAYE_ELEMENT.SOURCE='" +
                // tableRubCot.getName() + "')");
                // req.append(" AND (\"" + base.getName() + "\"." + "FICHE_PAYE_ELEMENT.IDSOURCE=\""
                // +
                // tableRubCot.getBase().getName() + "\"." +
                // tableRubCot.getField("ID").getFullName() +
                // ")");
                //
                // req.append(" AND (\"" + tableRubCot.getBase().getName() + "\"." +
                // tableRubCot.getField("ID_CAISSE_COTISATION").getFullName() + "=" + this.idCaisse
                // + ")");

                System.err.println(req);
                List listFicheElt = ((List) base.getDataSource().execute(req.toString(), new ArrayListHandler()));

                // Fill map
                for (Iterator i = listFicheElt.iterator(); i.hasNext();) {

                    Object[] tmp = (Object[]) i.next();

                    SQLRowValues rowVals = null;
                    if (EtatChargeModel.this.mapCot.get(new Integer(tmp[1].toString())) == null && EtatChargeModel.this.listRubCaisse.contains(tmp[1])) {
                        rowVals = new SQLRowValues(tableFichePayeElt);
                        rowVals.put("NOM", (tmp[2] == null) ? "" : tmp[2].toString());
                        rowVals.put("IDSOURCE", (tmp[1] == null) ? 0 : Integer.parseInt(tmp[1].toString()));
                        rowVals.put("TAUX_SAL", (tmp[4] == null) ? new Float(0) : new Float(tmp[4].toString()));
                        rowVals.put("TAUX_PAT", (tmp[7] == null) ? new Float(0) : new Float(tmp[7].toString()));
                        EtatChargeModel.this.mapCot.put(new Integer(tmp[1].toString()), rowVals);
                    } else {
                        rowVals = (SQLRowValues) EtatChargeModel.this.mapCot.get(new Integer(tmp[1].toString()));
                    }

                    if (rowVals != null) {
                        // Cumul des valeurs
                        float base = (rowVals.getObject("NB_BASE") == null) ? 0.0F : ((Float) rowVals.getObject("NB_BASE")).floatValue();
                        base += (tmp[3] == null) ? 0.0F : ((Float) tmp[3]).floatValue();
                        rowVals.put("NB_BASE", new Float(base));

                        float montantSal = (rowVals.getObject("MONTANT_SAL_AJ") == null) ? 0.0F : ((Float) rowVals.getObject("MONTANT_SAL_AJ")).floatValue();
                        montantSal += (tmp[5] == null) ? 0.0F : ((Float) tmp[5]).floatValue();
                        rowVals.put("MONTANT_SAL_AJ", new Float(montantSal));

                        float montantSalDed = (rowVals.getObject("MONTANT_SAL_DED") == null) ? 0.0F : ((Float) rowVals.getObject("MONTANT_SAL_DED")).floatValue();
                        montantSalDed += (tmp[6] == null) ? 0.0F : ((Float) tmp[6]).floatValue();
                        rowVals.put("MONTANT_SAL_DED", new Float(montantSalDed));

                        float montantPat = (rowVals.getObject("MONTANT_PAT") == null) ? 0.0F : ((Float) rowVals.getObject("MONTANT_PAT")).floatValue();
                        montantPat += (tmp[8] == null) ? 0.0F : ((Float) tmp[8]).floatValue();
                        rowVals.put("MONTANT_PAT", new Float(montantPat));

                        // Cumuls total des cotisations
                        EtatChargeModel.this.cotPat += (tmp[8] == null) ? 0.0F : new Float(tmp[8].toString()).intValue();
                        EtatChargeModel.this.cotSal += (tmp[6] == null) ? 0.0F : new Float(tmp[6].toString()).intValue();
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                EtatChargeModel.this.fireTableDataChanged();
            }
        }.execute();

    }

    private void fillMapCaisse() {

        SQLSelect selElt = new SQLSelect(Configuration.getInstance().getBase());
        selElt.addSelect(tableRubCot.getField("ID"));
        selElt.setWhere(new Where(tableRubCot.getField("ID_CAISSE_COTISATION"), "=", this.idCaisse));

        List l = Configuration.getInstance().getBase().getDataSource().executeA(selElt.asString());

        if (l != null) {
            this.listRubCaisse = new ArrayList(l.size());
            for (int i = 0; i < l.size(); i++) {
                Object o = ((Object[]) l.get(i))[0];
                this.listRubCaisse.add(((Number) o).intValue());
            }
        } else {
            this.listRubCaisse = new ArrayList();
        }
    }

    public int getColumnCount() {

        return this.titres.length;
    }

    public int getRowCount() {

        return this.mapCot.keySet().size();
    }

    public String getColumnName(int column) {

        return this.titres[column];
    }

    public Class getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return String.class;
        } else {
            return Float.class;
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {

        SQLRowValues rowVals = (SQLRowValues) this.mapCot.get(this.mapCot.keySet().toArray()[rowIndex]);

        if (rowVals != null) {
            if (columnIndex == 0) {
                return rowVals.getObject("NOM");
            }
            if (columnIndex == 1) {
                return rowVals.getObject("NB_BASE");
            }
            if (columnIndex == 2) {
                return rowVals.getObject("TAUX_SAL");
            }
            if (columnIndex == 3) {
                return rowVals.getObject("MONTANT_SAL_DED");
            }
            if (columnIndex == 4) {
                return rowVals.getObject("TAUX_PAT");
            }
            if (columnIndex == 5) {
                return rowVals.getObject("MONTANT_PAT");
            }
            if (columnIndex == 6) {
                return new Float(((Float) rowVals.getObject("MONTANT_PAT")).floatValue() + ((Float) rowVals.getObject("MONTANT_SAL_DED")).floatValue());
            }
        }
        return null;
    }

    public void reload(int moisDe, int moisAu, int annee) {
        this.moisDe = moisDe;
        this.moisAu = moisAu;
        this.annee = annee;

        fill();
        fireTableDataChanged();
    }

    public float getTotalCotisationSal() {

        return this.cotSal;
    }

    public float getTotalCotisationPat() {
        return this.cotPat;

    }
}
