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
import org.openconcerto.erp.element.objet.Association;
import org.openconcerto.erp.element.objet.Axe;
import org.openconcerto.erp.element.objet.ClasseCompte;
import org.openconcerto.erp.element.objet.Compte;
import org.openconcerto.erp.element.objet.RepartitionAssociation;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class AssociationAnalytiqueModel extends AbstractTableModel {

    private Vector associations = new Vector();
    private Association[][] dataAssociations;
    private Vector repartitionsAxe = new Vector();
    private Vector axes = new Vector();
    private Vector titres = new Vector();
    private Vector comptes = new Vector();

    // Compte ID - Vecteur compte index
    private Map mapCompte = new HashMap();

    // Repartition ID - Vecteur repartition index
    private Map mapRepartition = new HashMap();
    private SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();

    public AssociationAnalytiqueModel(ClasseCompte cc) {

        // on recupere les comptes

        SQLTable compteTable = base.getTable("COMPTE_PCE");

        // SQLBase base = elt.getTable().getBase();
        SQLSelect selCompte = new SQLSelect(base);
        selCompte.addSelect(compteTable.getField("ID"));
        selCompte.addSelect(compteTable.getField("NUMERO"));
        selCompte.addSelect(compteTable.getField("NOM"));

        selCompte.setWhere(new Where(compteTable.getField("NUMERO"), "REGEXP", cc.getTypeNumeroCompte()));

        selCompte.addRawOrder("COMPTE_PCE.NUMERO");
        // selCompte.setDistinct(true);

        String reqCompte = selCompte.asString();

        // System.out.println(reqCompte);

        Object obCompte = base.getDataSource().execute(reqCompte, new ArrayListHandler());

        List myListCompte = (List) obCompte;

        if (myListCompte.size() != 0) {
            for (int i = 0; i < myListCompte.size(); i++) {
                Object[] objTmp = (Object[]) myListCompte.get(i);

                this.mapCompte.put(new Integer(Integer.parseInt(objTmp[0].toString())), new Integer(this.comptes.size()));
                this.comptes.add(new Compte(Integer.parseInt(objTmp[0].toString()), objTmp[1].toString(), objTmp[2].toString()));
            }
        }

        // on recupere les axes existants
        SQLTable axeTable = base.getTable("AXE_ANALYTIQUE");
        SQLSelect selAxe = new SQLSelect(base);
        selAxe.addSelect(axeTable.getField("ID"));
        selAxe.addSelect(axeTable.getField("NOM"));
        selAxe.addRawOrder("AXE_ANALYTIQUE.NOM");
        String reqAxe = selAxe.asString();

        Object obAxe = base.getDataSource().execute(reqAxe, new ArrayListHandler());

        List myListAxe = (List) obAxe;

        this.titres.add("Compte");
        this.titres.add("Libellé");

        if (myListAxe.size() != 0) {

            for (int i = 0; i < myListAxe.size(); i++) {

                Object[] objTmp = (Object[]) myListAxe.get(i);
                this.axes.add(new Axe(Integer.parseInt(objTmp[0].toString()), objTmp[1].toString()));
                this.titres.add(objTmp[1].toString());
            }

            this.dataAssociations = new Association[this.comptes.size()][this.axes.size()];

            for (int j = 0; j < this.axes.size(); j++) {

                // on recupere les repartitions pour chaque axe
                // RepartitionAnalytiqueSQLElement repElt = new RepartitionAnalytiqueSQLElement();
                SQLTable repTable = base.getTable("REPARTITION_ANALYTIQUE");

                // RepartitionAnalytiqueElementSQLElement repElemElt = new
                // RepartitionAnalytiqueElementSQLElement();
                SQLTable repElemTable = base.getTable("REPARTITION_ANALYTIQUE_ELEMENT");

                // PosteAnalytiqueSQLElement posteElt = new PosteAnalytiqueSQLElement();
                SQLTable posteTable = base.getTable("POSTE_ANALYTIQUE");

                SQLSelect selRep = new SQLSelect(base);
                selRep.addSelect(repTable.getField("ID"));
                selRep.addSelect(repTable.getField("NOM"));
                selRep.addSelect(posteTable.getField("NOM"));
                selRep.addSelect(repElemTable.getField("TAUX"));

                Where w = new Where(repElemTable.getField("ID_POSTE_ANALYTIQUE"), "=", posteTable.getField("ID"));
                Where w2 = new Where(repElemTable.getField("ID_REPARTITION_ANALYTIQUE"), "=", repTable.getField("ID"));
                Where w3 = new Where(posteTable.getField("ID_AXE_ANALYTIQUE"), "=", ((Axe) this.axes.get(j)).getId());

                selRep.setWhere(w.and(w2).and(w3));
                // selRep.setDistinct(true);
                selRep.addRawOrder("REPARTITION_ANALYTIQUE.ID");

                String reqRep = selRep.asString();
                Object obRep = base.getDataSource().execute(reqRep, new ArrayListHandler());

                List myListRep = (List) obRep;

                if (myListRep.size() != 0) {
                    Vector repartition = new Vector();

                    repartition.add(new RepartitionAssociation(1, "Indéfini", ((Axe) this.axes.get(j)).getId(), ""));
                    this.mapRepartition.put(new Integer(1), new Integer(0));

                    int valId = 0;
                    RepartitionAssociation repTmp = null;

                    for (int i = 0; i < myListRep.size(); i++) {
                        Object[] objTmp = (Object[]) myListRep.get(i);

                        if (valId != Integer.parseInt(objTmp[0].toString())) {

                            if (repTmp != null) {
                                repartition.add(repTmp);
                                this.mapRepartition.put(new Integer(Integer.parseInt(objTmp[0].toString())), new Integer(repartition.size() - 1));
                            }

                            valId = Integer.parseInt(objTmp[0].toString());

                            if (Float.parseFloat(objTmp[3].toString()) > 0.0) {
                                repTmp = new RepartitionAssociation(Integer.parseInt(objTmp[0].toString()), objTmp[1].toString(), ((Axe) this.axes.get(j)).getId(), objTmp[2].toString() + ":"
                                        + objTmp[3].toString() + "%");
                            } else {
                                repTmp = new RepartitionAssociation(Integer.parseInt(objTmp[0].toString()), objTmp[1].toString(), ((Axe) this.axes.get(j)).getId(), "");
                            }

                        } else {
                            if (repTmp != null) {
                                if (Float.parseFloat(objTmp[3].toString()) > 0.0) {
                                    repTmp.addValPoste(objTmp[2].toString() + ":" + objTmp[3].toString() + "%");
                                }
                            }
                        }
                    }
                    repartition.add(repTmp);
                    this.mapRepartition.put(new Integer(repTmp.getId()), new Integer(repartition.size() - 1));
                    this.repartitionsAxe.add(repartition);
                } else {
                    Vector repartition = new Vector();

                    repartition.add(new RepartitionAssociation(1, "Indéfini", ((Axe) this.axes.get(j)).getId(), ""));
                    this.mapRepartition.put(new Integer(1), new Integer(0));

                    this.repartitionsAxe.add(repartition);
                }

                // on recupere les associations
                // AssociationCompteAnalytiqueSQLElement associationElt = new
                // AssociationCompteAnalytiqueSQLElement();
                SQLSelect selAssoc = new SQLSelect(base);
                SQLTable associationTable = base.getTable("ASSOCIATION_COMPTE_ANALYTIQUE");
                selAssoc.addSelect(associationTable.getField("ID"));
                selAssoc.addSelect(associationTable.getField("ID_COMPTE_PCE"));
                selAssoc.addSelect(associationTable.getField("ID_REPARTITION_ANALYTIQUE"));

                selAssoc.setWhere("ASSOCIATION_COMPTE_ANALYTIQUE.ID_AXE_ANALYTIQUE", "=", ((Axe) this.axes.get(j)).getId());

                String reqAssoc = selAssoc.asString();
                Object obAssoc = base.getDataSource().execute(reqAssoc, new ArrayListHandler());

                List myListAssoc = (List) obAssoc;

                if (myListAssoc.size() != 0) {
                    Vector assoc = new Vector();

                    for (int i = 0; i < myListAssoc.size(); i++) {
                        Object[] objTmp = (Object[]) myListAssoc.get(i);

                        Association assocTmp = new Association(Integer.parseInt(objTmp[0].toString()), Integer.parseInt(objTmp[1].toString()), Integer.parseInt(objTmp[2].toString()));

                        if ((this.mapCompte.get(new Integer(assocTmp.getIdCompte())) != null) && (this.mapRepartition.get(new Integer(assocTmp.getIdRep())) != null)) {

                            this.dataAssociations[Integer.parseInt(this.mapCompte.get(new Integer(assocTmp.getIdCompte())).toString())][j] = assocTmp;
                            assoc.add(assocTmp);
                        }
                    }

                    this.associations.add(assoc);
                } else {
                    this.associations.add(new Vector());
                }
            }
        }
    }

    public Class getColumnClass(int c) {

        if (c > 1) {
            return RepartitionAssociation.class;
        }
        return String.class;

    }

    public int getRowCount() {
        return this.comptes.size();
    }

    public int getColumnCount() {

        return this.titres.size();
    }

    public String getColumnName(int col) {
        return this.titres.get(col).toString();
    }

    public boolean isCellEditable(int row, int col) {

        String numeroCompte = getValueAt(row, 0).toString();
        String numeroCompteSuiv = null;

        if (row < getRowCount() - 1) {
            numeroCompteSuiv = getValueAt(row + 1, 0).toString();
        }

        // Si c'est un compte racine
        if (getValueAt(row, 0).toString().trim().length() == 1) {
            return false;
        }

        /*
         * if ((numeroCompteSuiv != null) && (numeroCompte.trim().length() <
         * numeroCompteSuiv.trim().length()) &&
         * (numeroCompte.trim().equalsIgnoreCase(numeroCompteSuiv.trim().substring(0,
         * numeroCompte.length())))) { return false; }
         */

        return (col > 1);

    }

    public Object getValueAt(int rowIndex, int columnIndex) {

        if (columnIndex == 0) {
            return ((Compte) this.comptes.get(rowIndex)).getNumero();
        }

        if (columnIndex == 1) {
            return ((Compte) this.comptes.get(rowIndex)).getNom();
        }

        if (this.dataAssociations[rowIndex][columnIndex - 2] != null) {
            Association assocTmp = this.dataAssociations[rowIndex][columnIndex - 2];
            return ((Vector) this.repartitionsAxe.get(columnIndex - 2)).get(Integer.parseInt(this.mapRepartition.get(new Integer(assocTmp.getIdRep())).toString()));
        }

        if (isCellEditable(rowIndex, columnIndex)) {
            return ((Vector) this.repartitionsAxe.get(columnIndex - 2)).get(0);
        }
        return null;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

        if (columnIndex > 1) {
            if (this.dataAssociations[rowIndex][columnIndex - 2] != null) {
                if (((RepartitionAssociation) aValue).getId() == 1) {

                    Association assocTmp = this.dataAssociations[rowIndex][columnIndex - 2];
                    assocTmp.setSuppression(true);
                    validAssociation(assocTmp, columnIndex - 2);
                    this.dataAssociations[rowIndex][columnIndex - 2] = null;
                } else {

                    this.dataAssociations[rowIndex][columnIndex - 2].setIdRep(((RepartitionAssociation) aValue).getId());

                    Vector assoc = (Vector) this.associations.get(columnIndex - 2);
                    for (int i = 0; i < assoc.size(); i++) {
                        validAssociation((Association) assoc.get(i), columnIndex - 2);
                        // System.out.println(assoc.get(i));
                    }
                }
            } else {
                if (((RepartitionAssociation) aValue).getId() == 1) {
                    return;
                }

                Association assocTmp = new Association(1, ((Compte) this.comptes.get(rowIndex)).getId(), ((RepartitionAssociation) aValue).getId(), true);

                this.dataAssociations[rowIndex][columnIndex - 2] = assocTmp;
                ((Vector) this.associations.get(columnIndex - 2)).add(assocTmp);
                validAssociation(assocTmp, columnIndex - 2);
                // dataAssociations[rowIndex][columnIndex-2] = new Association(1, 1, 1);

            }

            // Vector reps = (Vector) repartitionsAxe.get(columnIndex - 2);
            // System.out.println("Value --> " + ((Repartition)aValue).getId());
        }
    }

    public Vector getRepartitionsAxe() {
        return this.repartitionsAxe;
    }

    private void validAssociation(Association a, int index_axe) {

        // AssociationCompteAnalytiqueSQLElement associationElt = new
        // AssociationCompteAnalytiqueSQLElement();
        SQLTable associationTable = base.getTable("ASSOCIATION_COMPTE_ANALYTIQUE");

        if (a.getSuppression()) {
            SQLRowValues vals = new SQLRowValues(associationTable);
            vals.put("ARCHIVE", 1);
            try {
                vals.update(a.getId());
            } catch (SQLException e) {
                System.err.println("Erreur suppression association " + a);
                e.printStackTrace();
            }
            return;
        }

        if (a.getCreation()) {

            Map m = new HashMap();
            m.put("ID_REPARTITION_ANALYTIQUE", new Integer(a.getIdRep()));
            m.put("ID_COMPTE_PCE", new Integer(a.getIdCompte()));
            m.put("ID_AXE_ANALYTIQUE", new Integer(((Axe) this.axes.get(index_axe)).getId()));

            SQLRowValues val = new SQLRowValues(associationTable, m);

            try {

                if (val.getInvalid() == null) {
                    SQLRow row = val.insert();
                    a.setId(row.getID());
                } else {
                    System.out.println("Impossible d'ajouter " + a + " clef etrangere invalide");
                }
            } catch (SQLException e) {
                System.out.println("Error insert row in " + val.getTable().getName());
            }

        } else {
            if (a.getModification()) {

                SQLRowValues vals = new SQLRowValues(associationTable);
                vals.put("ID_REPARTITION_ANALYTIQUE", a.getIdRep());

                try {
                    vals.update(a.getId());
                } catch (SQLException e) {
                    System.out.println("Erreur modification association " + a);
                }
            }
        }

        a.setCreation(false);
        a.setModification(false);
    }
}
