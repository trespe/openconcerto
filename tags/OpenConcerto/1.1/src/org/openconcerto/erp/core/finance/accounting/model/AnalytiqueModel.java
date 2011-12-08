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
import org.openconcerto.erp.core.finance.accounting.ui.ValiderSuppressionRepartitionFrame;
import org.openconcerto.erp.element.objet.Poste;
import org.openconcerto.erp.element.objet.Repartition;
import org.openconcerto.erp.element.objet.RepartitionElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class AnalytiqueModel extends AbstractTableModel {

    // Association titre poste
    private Vector mesTitres = new Vector();

    // Association ligne vecteur repartition
    private Vector ligneRepartitions = new Vector();

    private List<Poste> postes = new ArrayList<Poste>();
    private List<RepartitionElement> repartitionElements = new ArrayList<RepartitionElement>();
    private List<Repartition> repartitions = new ArrayList<Repartition>();

    private int idAxe;

    // Id temporaire des éléments ajoutés
    private int idNewRep;
    private int idNewRepElem;
    private int idNewPoste;

    private SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();

    public AnalytiqueModel(int IDAxe) {

        int i;
        List myList;

        this.idAxe = IDAxe;

        /*******************************************************************************************
         * POSTES DE L'AXE
         ******************************************************************************************/
        // SELECT NOM FROM POSTE WHERE POSTE.ID_AXE = id
        SQLTable posteTable = base.getTable("POSTE_ANALYTIQUE");
        SQLSelect sel = new SQLSelect(base);
        sel.addSelect(posteTable.getField("ID"));
        sel.addSelect(posteTable.getField("NOM"));
        sel.setWhere("POSTE_ANALYTIQUE.ID_AXE_ANALYTIQUE", "=", IDAxe);

        String req = sel.asString();

        Object ob = base.getDataSource().execute(req, new ArrayListHandler());

        // on stocke les postes
        myList = (List) ob;

        this.idNewPoste = 1;

        if (myList.size() != 0) {

            for (i = 0; i < myList.size(); i++) {

                Object[] objTmp = (Object[]) myList.get(i);
                // ID, nom
                this.postes.add(new Poste(Integer.parseInt(objTmp[0].toString()), objTmp[1].toString(), this.idAxe));

                if (Integer.parseInt(objTmp[0].toString()) > this.idNewPoste) {
                    this.idNewPoste = Integer.parseInt(objTmp[0].toString());
                }
            }

            // on associe le vecteur poste au titre des colonnes
            this.mesTitres.add("Répartition");

            for (i = 1; i < this.postes.size() + 1; i++) {
                this.mesTitres.add(String.valueOf(i - 1));
            }

            /***************************************************************************************
             * REPARTITIONS
             **************************************************************************************/

            // SELECT Rep.NOM, ID_POSTE, taux FROM REP, REP_ELEME
            // WHERE REP_ELEME.ID_POSTE = id, REP_ELEM.IDREP = REp.ID)
            SQLSelect sel2 = new SQLSelect(base);
            sel2.addSelect("REPARTITION_ANALYTIQUE.NOM");
            sel2.addSelect("REPARTITION_ANALYTIQUE_ELEMENT.TAUX");
            sel2.addSelect("REPARTITION_ANALYTIQUE_ELEMENT.ID_POSTE_ANALYTIQUE");
            sel2.addSelect("REPARTITION_ANALYTIQUE_ELEMENT.ID_REPARTITION_ANALYTIQUE");
            sel2.addSelect("REPARTITION_ANALYTIQUE_ELEMENT.ID");

            Where w = new Where(base.getTable("REPARTITION_ANALYTIQUE_ELEMENT").getField("ID_REPARTITION_ANALYTIQUE"), "=", base.getTable("REPARTITION_ANALYTIQUE").getField("ID"));
            Where w2 = new Where(posteTable.getField("ID_AXE_ANALYTIQUE"), "=", this.idAxe);
            Where w3 = new Where(posteTable.getField("ID"), "=", base.getTable("REPARTITION_ANALYTIQUE_ELEMENT").getField("ID_POSTE_ANALYTIQUE"));

            sel2.setWhere(w.and(w2).and(w3));
            sel2.addRawOrder("REPARTITION_ANALYTIQUE_ELEMENT.ID_REPARTITION_ANALYTIQUE");

            String req2 = sel2.asString();
            // System.out.println(req2);

            Object ob2 = base.getDataSource().execute(req2, new ArrayListHandler());
            myList = (List) ob2;

            if (myList.size() != 0) {

                // on remplit les vecteurs à partir de la liste récupérée
                getListRepartition(myList);
            }

        } else {
            this.idNewPoste = 2;
            this.postes.add(new Poste(1, "Nouveau poste", this.idAxe, true));
            this.mesTitres.add("Répartition");
            this.mesTitres.add(String.valueOf(0));
        }
    }

    /***********************************************************************************************
     * remplit les vecteurs et initialise les variables pour les repartitions et les elements
     * 
     * List --> NOM, TAUX, ID_POSTE, ID_REP, ID_REP_ELEM
     **********************************************************************************************/
    private void getListRepartition(List list) {
        int i;
        int id_rep = 1;
        Object[] obj;

        this.idNewRep = 1;
        this.idNewRepElem = 1;

        for (i = 0; i < list.size(); i++) {
            obj = (Object[]) list.get(i);

            // si on change d'id_, on passe à une autre répartition
            if (id_rep != Integer.parseInt(obj[3].toString())) {

                id_rep = Integer.parseInt(obj[3].toString());

                this.repartitions.add(new Repartition(Integer.parseInt(obj[3].toString()), obj[0].toString(), this.idAxe));
                // System.out.println("Axe : " + this.idAxe + "Repartition " + obj[0].toString());
                this.ligneRepartitions.add(String.valueOf(this.repartitions.size() - 1));

                if (id_rep > this.idNewRep) {
                    this.idNewRep = id_rep;
                }

            }

            this.repartitionElements.add(new RepartitionElement(Integer.parseInt(obj[4].toString()), Integer.parseInt(obj[3].toString()), Integer.parseInt(obj[2].toString()), Float.parseFloat(obj[1]
                    .toString())));

            if (Integer.parseInt(obj[3].toString()) > this.idNewRepElem) {
                this.idNewRepElem = Integer.parseInt(obj[3].toString());
            }
        }
    }

    public int getColumnCount() {

        return this.mesTitres.size();
    }

    public Object getValueAt(int parm1, int parm2) {

        int id_poste, i;

        // id de la repartition associé à la ligne parm1
        int idRep = ((Repartition) this.repartitions.get(Integer.parseInt(this.ligneRepartitions.get(parm1).toString()))).getId();

        RepartitionElement tmp;

        if (parm2 > 0) {

            // id du poste associé à la colonne parm2
            id_poste = ((Poste) this.postes.get(Integer.parseInt(this.mesTitres.get(parm2).toString()))).getId();

            for (i = 0; i < this.repartitionElements.size(); i++) {

                tmp = (RepartitionElement) this.repartitionElements.get(i);

                if ((tmp.getIdPoste() == id_poste) && (tmp.getIdRep() == idRep)) {

                    return new Float(tmp.getTaux());
                }
            }
            return Float.valueOf("-1.00");

        }
        return ((Repartition) this.repartitions.get(Integer.parseInt(this.ligneRepartitions.get(parm1).toString()))).getNom();
    }

    public int getRowCount() {

        return this.ligneRepartitions.size();
    }

    public String getColumnName(int col) {

        if (col > 0) {

            int tmp = Integer.parseInt(this.mesTitres.get(col).toString());
            return (this.postes.get(tmp)).getNom();
        } else {

            return this.mesTitres.get(col).toString();
        }
    }

    public Class getColumnClass(int c) {

        if (c == 0) {
            return String.class;
        }
        return Float.class;

    }

    public boolean isCellEditable(int row, int col) {

        return true;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

        // id de la répartition associée à la ligne rowIndex
        int idRep = ((Repartition) this.repartitions.get(Integer.parseInt(this.ligneRepartitions.get(rowIndex).toString()))).getId();
        RepartitionElement tmp;
        int id_poste, i;

        if (aValue == null) {
            return;
        }

        if (columnIndex > 0) {

            if (Float.parseFloat(aValue.toString()) < 0.0) {

                aValue = Float.valueOf("0.0");
            }

            if (Float.parseFloat(aValue.toString()) > 100.0) {

                aValue = Float.valueOf("100.0");
            }

            // id du poste associé à la colonne columnIndex
            id_poste = ((Poste) this.postes.get(Integer.parseInt(this.mesTitres.get(columnIndex).toString()))).getId();

            for (i = 0; i < this.repartitionElements.size(); i++) {

                tmp = (RepartitionElement) this.repartitionElements.get(i);

                if ((tmp.getIdPoste() == id_poste) && (tmp.getIdRep() == idRep)) {

                    tmp.setTaux(Float.parseFloat(aValue.toString()));
                    // System.out.println("Set Value --->" + aValue + " i : " + i + " taux : " +
                    // tmp.getTaux());
                    // this.ligneRepartition.set(i, tmp);
                }
            }

            // System.out.println("Set Value --->" + aValue + " row : " + rowIndex + " column : " +
            // columnIndex + " ID_REP : " + idRep + " ID_POSTE : " + id_poste);

        } else {
            ((Repartition) this.repartitions.get(Integer.parseInt(this.ligneRepartitions.get(rowIndex).toString()))).setNom(aValue.toString());
        }
    }

    /***********************************************************************************************
     * Créer une nouvelle répartition
     **********************************************************************************************/
    public void addElement() {
        int idRepTmp = this.repartitions.size();
        int idElemTmp = this.repartitionElements.size();
        int nbElemAdd;

        this.idNewRep++;
        this.repartitions.add(new Repartition(this.idNewRep, "Nouvelle répartition", this.idAxe, true));

        this.ligneRepartitions.add(String.valueOf(idRepTmp));

        // On crée les éléments de répartitions pour les postes existants
        nbElemAdd = 0;

        for (int i = 1; i < this.mesTitres.size(); i++) {
            this.idNewRepElem++;
            nbElemAdd++;
            this.repartitionElements.add(new RepartitionElement(this.idNewRepElem, this.idNewRep, ((Poste) this.postes.get(Integer.parseInt(this.mesTitres.get(i).toString()))).getId(), 0, true));
        }

        // System.out.println("Ajout Repartition --> " + idRepTmp + " nbElemAdd : " + nbElemAdd);
        this.fireTableDataChanged();
    }

    /***********************************************************************************************
     * Supprimer les répartitons sélectionnées
     **********************************************************************************************/
    public void removeElement(int[] listeRows) {
        int i;

        if ((this.ligneRepartitions.size() > 0) && (listeRows != null)) {

            for (i = 0; i < listeRows.length; i++) {

                removeElement(listeRows[i] - i);
            }
        }
    }

    /***********************************************************************************************
     * Supprimer une répartition
     **********************************************************************************************/
    public void removeElement(int row) {

        // la répartition et ses éléments doivent etre supprimé
        Repartition rep = (Repartition) this.repartitions.get(Integer.parseInt(this.ligneRepartitions.get(row).toString()));

        // Test si la repartition n'est pas deja associe à un compte
        // SELECT ID, ID_COMPTE FROM ASSOCIATION WHERE ID.REP = id

        SQLTable assocTable = base.getTable("ASSOCIATION_COMPTE_ANALYTOQUE");
        SQLSelect selAssoc = new SQLSelect(base);
        selAssoc.addSelect(assocTable.getField("ID"));
        selAssoc.addSelect(assocTable.getField("ID_COMPTE_PCE"));
        selAssoc.setWhere("ASSOCIATION_COMPTE_ANALYTIQUE.ID_REPARTITION_ANALYTIQUE", "=", rep.getId());

        String reqAssoc = selAssoc.asString();
        Object obAssoc = base.getDataSource().execute(reqAssoc, new ArrayListHandler());

        List myListAssoc = (List) obAssoc;

        if (myListAssoc.size() != 0) {
            System.out.println("La répartition est affectée à un compte.");
            ValiderSuppressionRepartitionFrame validFrame = new ValiderSuppressionRepartitionFrame(this, row, myListAssoc);
            validFrame.pack();
            validFrame.setVisible(true);
        } else {
            deleteElement(row);
        }

    }

    public void deleteElement(int row) {

        Repartition rep = (Repartition) this.repartitions.get(Integer.parseInt(this.ligneRepartitions.get(row).toString()));

        rep.setSuppression(true);

        int nbElementSuppr = 0;

        for (int j = 0; j < this.repartitionElements.size(); j++) {
            RepartitionElement repElem = (RepartitionElement) this.repartitionElements.get(j);
            if (repElem.getIdRep() == rep.getId()) {
                repElem.setSuppression(true);
                nbElementSuppr++;
            }
        }

        // on met à jour le vecteur des lignes
        for (int j = row; j < this.ligneRepartitions.size() - 1; j++) {

            this.ligneRepartitions.set(j, new String(this.ligneRepartitions.get(j + 1).toString()));
        }

        this.ligneRepartitions.remove(this.ligneRepartitions.size() - 1);
        this.fireTableDataChanged();
    }

    /***********************************************************************************************
     * Créer un nouveau poste
     **********************************************************************************************/
    public void addPoste(String nom) {

        int nbElemAdd;

        this.idNewPoste++;

        if (nom.trim().length() != 0) {
            this.postes.add(new Poste(this.idNewPoste, nom, this.idAxe, true));
        } else {
            this.postes.add(new Poste(this.idNewPoste, "Nouveau poste", this.idAxe, true));
        }

        this.mesTitres.add(String.valueOf(this.postes.size() - 1));

        nbElemAdd = 0;

        // on ajoute un element pour chaque répartitions existante
        for (int i = 0; i < this.ligneRepartitions.size(); i++) {
            // int id, int idRep, int idPoste, float taux, boolean creation
            this.idNewRepElem++;
            nbElemAdd++;
            this.repartitionElements.add(new RepartitionElement(this.idNewRepElem, ((Repartition) this.repartitions.get(Integer.parseInt(this.ligneRepartitions.get(i).toString()))).getId(),
                    this.idNewPoste, 0, true));
        }

        // System.out.println("Nouveau Poste " + nom + " nbElemAdd : " + nbElemAdd);
        this.fireTableStructureChanged();
    }

    /***********************************************************************************************
     * Supprimer un poste
     **********************************************************************************************/
    public void removePoste(int column) {

        int i, nbElemSuppr;

        if ((this.mesTitres.size() > 2) && (column > 0)) {

            Poste p = (Poste) this.postes.get(Integer.parseInt(this.mesTitres.get(column).toString()));
            p.setSuppression(true);

            // System.out.println("ID poste : " + p.getId());

            nbElemSuppr = 0;
            for (i = 0; i < this.repartitionElements.size(); i++) {

                RepartitionElement repE = (RepartitionElement) this.repartitionElements.get(i);
                if (repE.getIdPoste() == p.getId()) {
                    repE.setSuppression(true);
                    nbElemSuppr++;
                }
            }

            for (i = column; i < this.mesTitres.size() - 1; i++) {

                this.mesTitres.set(i, new String(this.mesTitres.get(i + 1).toString()));
            }
            this.mesTitres.remove(this.mesTitres.size() - 1);

            this.fireTableStructureChanged();
            // System.out.println("Colonne supprimé " + column + " nb Element : " + nbElemSuppr);
        }
    }

    public void modifierNomPoste(int column, String nom) {
        Poste p = (Poste) this.postes.get(Integer.parseInt(this.mesTitres.get(column).toString()));
        p.setNom(nom);
        this.fireTableStructureChanged();
    }

    public List<Repartition> getRepartition() {
        return this.repartitions;
    }

    public List<RepartitionElement> getRepartitionElem() {
        return this.repartitionElements;
    }

    public List<Poste> getPostes() {
        return this.postes;
    }
}
