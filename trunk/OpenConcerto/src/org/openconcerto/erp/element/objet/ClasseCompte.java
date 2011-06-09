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
 
 package org.openconcerto.erp.element.objet;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClasseCompte {
    private int id;
    private String nom;
    private String typeNumeroCompte;

    private static List<ClasseCompte> liste;

    public static void loadClasseCompte() {
        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();

        SQLTable classeCompteTable = base.getTable("CLASSE_COMPTE");

        SQLSelect selClasse = new SQLSelect(base);

        selClasse.addSelect(classeCompteTable.getField("ID"));
        selClasse.addSelect(classeCompteTable.getField("NOM"));
        selClasse.addSelect(classeCompteTable.getField("TYPE_NUMERO_COMPTE"));

        selClasse.addRawOrder("\"CLASSE_COMPTE\".\"TYPE_NUMERO_COMPTE\"");

        String reqClasse = selClasse.asString();
        System.err.println(reqClasse);
        List<Map<String, Object>> obClasse = base.getDataSource().execute(reqClasse);
        liste = new ArrayList<ClasseCompte>();
        for (Map<String, Object> map : obClasse) {

            liste.add(new ClasseCompte(Integer.parseInt(map.get("ID").toString()), map.get("NOM").toString(), map.get("TYPE_NUMERO_COMPTE").toString()));

        }
    }

    public static List<ClasseCompte> getClasseCompte() {
        return liste;
    }

    public ClasseCompte(final int id, final String nom, final String typeNumeroCompte) {
        this.id = id;
        this.nom = nom;
        this.typeNumeroCompte = typeNumeroCompte;
    }

    public int getId() {
        return this.id;
    }

    public String getNom() {
        return this.nom;
    }

    public String getTypeNumeroCompte() {
        return this.typeNumeroCompte;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public void setNom(final String nom) {
        this.nom = nom;
    }

    public void setTypeNumeroCompte(final String type) {
        this.typeNumeroCompte = type;
    }

    @Override
    public String toString() {
        return "ID : " + this.id + " nom : " + this.nom + " type numero de compte : " + this.typeNumeroCompte;
    }
}
