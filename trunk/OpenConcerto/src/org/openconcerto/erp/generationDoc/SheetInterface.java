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
 
 package org.openconcerto.erp.generationDoc;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;

import java.util.HashMap;
import java.util.Map;


public abstract class SheetInterface {

    // SQLRow de l'élément de la table qui est utilisé pour la génération
    protected SQLRow row;

    // Map contenant les cellules ou il faut subsistuer _
    protected Map mapReplace;

    // Map contenant les cellules avec le style associée
    protected Map<Integer, String> mapStyleRow;

    // Map contenant les cellules avec les valeurs à insérer
    protected Map<String, Object> mCell;

    // nombre de page du document
    protected int nbPage;

    // nom de l'imprimante à utiliser
    protected String printer;

    // modele du document
    protected String modele;

    // emplacement du fichier OO généré
    protected String locationOO;

    // emplacement du fichier PDF généré
    protected String locationPDF;

    // nom du futur fichier
    protected String fileName;

    // nombre de ligne par pages
    protected int nbRowsPerPage;

    public static final int typeOO = 1;
    public static final int typePDF = 2;
    public static final int typeNoExtension = 3;

    protected static final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();

    public String getLocationOO() {
        return this.locationOO;
    }

    public String getLocationPDF() {
        return this.locationPDF;
    }

    public int getNbPage() {
        return this.nbPage;
    }

    public String getPrinter() {
        return this.printer;
    }

    public String getModele() {
        return this.modele;
    }

    public SheetInterface() {
        this.mapReplace = new HashMap();
        this.mapStyleRow = new HashMap();
        this.mCell = new HashMap<String, Object>();
    }

    public SheetInterface(int id, SQLTable table) {
        this();
        this.row = table.getRow(id);
        createMap();
    }

    public SheetInterface(SQLRow row) {
        this();
        this.row = row;
        createMap();
    }

    public Map getMapStyleRow() {
        return this.mapReplace;
    }

    public Map getMapReplace() {
        return this.mapReplace;
    }

    public Map getMap() {
        return this.mCell;
    }

    protected abstract void createMap();

}
