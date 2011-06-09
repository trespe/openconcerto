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
 
 package org.openconcerto.erp.utils;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.changer.correct.FixSerial;
import org.openconcerto.sql.model.DBStructureItemDB;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSchema;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionHandler;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class ActionDB {

    static private final Properties props;
    static {
        props = new Properties();
        props.put(PropsConfiguration.JDBC_CONNECTION + "allowMultiQueries", "true");
    }

    /**
     * Création d'une société à partir de la base GestionDefault
     * 
     * @param baseDefault nom de la base par défaut
     * @param newBase nom de la nouvelle base
     */
    public static void dupliqueMySQLDB(String baseDefault, String newBase, StatusListener l) {

        // ComptaPropsConfiguration instance = ComptaPropsConfiguration.create();
        // Configuration.setInstance(instance);

        // instance.setUpSocieteDataBaseConnexion(baseDefault);
        DatabaseMetaData dbMetaDataSocieteDefault;
        try {

            // Creation de la nouvelle base
            String reqDatabase = "CREATE DATABASE \"" + newBase + "\" CHARACTER SET 'UTF8'";

            Configuration.getInstance().getBase().getDataSource().getConnection().prepareStatement(reqDatabase).executeUpdate();
            SQLBase baseSQLNew = Configuration.getInstance().getBase().getServer().getBase(newBase, "openconcerto", "openconcerto");
            SQLBase baseSQLDefault = Configuration.getInstance().getBase().getServer().getBase(baseDefault, "openconcerto", "openconcerto");
            // Table de la base par defaut
            dbMetaDataSocieteDefault = baseSQLDefault.getDataSource().getConnection().getMetaData();

            ResultSet rs = dbMetaDataSocieteDefault.getTables("", "", null, null);
            while (rs.next()) {
                StringBuffer result = new StringBuffer();
                String tableName = rs.getString("TABLE_NAME");
                String tableType = rs.getString("TABLE_TYPE");

                if ("TABLE".equalsIgnoreCase(tableType)) {

                    // Creation d'une table
                    // result.append("\n\n-- " + tableName);
                    result.append("\nCREATE TABLE `" + newBase + "`.`" + tableName + "` (\n");
                    ResultSet tableMetaData = dbMetaDataSocieteDefault.getColumns(null, null, tableName, "%");
                    boolean firstLine = true;

                    // On recupere la cle primaire
                    String primaryKey = "";
                    try {
                        ResultSet primaryKeys = dbMetaDataSocieteDefault.getPrimaryKeys(null, null, tableName);
                        while (primaryKeys.next()) {
                            primaryKey = primaryKeys.getString("COLUMN_NAME");
                        }
                    } catch (SQLException e) {
                        ExceptionHandler.handle("Erreur pendant la création de la base!", e);
                        System.err.println("Unable to get primary keys for table " + tableName + " because " + e);
                    }

                    System.err.println(primaryKey);

                    // Creation des columns
                    while (tableMetaData.next()) {

                        if (firstLine) {
                            firstLine = false;
                        } else {
                            result.append(",\n");
                        }

                        // COLUMN NAME, TYPE AND SIZE
                        String columnName = tableMetaData.getString("COLUMN_NAME");
                        String columnType = tableMetaData.getString("TYPE_NAME");
                        String decimalDigits = tableMetaData.getString("DECIMAL_DIGITS");

                        int columnSize = tableMetaData.getInt("COLUMN_SIZE");

                        // NULL OR NOT NULL
                        String nullable = tableMetaData.getString("IS_NULLABLE");
                        String nullString = "NULL";
                        if ("NO".equalsIgnoreCase(nullable)) {
                            nullString = "NOT NULL";
                        }

                        // DEFAULT
                        String defaultValue = tableMetaData.getString("COLUMN_DEF");
                        String defaultValueString = "";
                        if (defaultValue != null) {
                            defaultValueString = " default '" + defaultValue + "'";
                        }

                        if (columnType.trim().equalsIgnoreCase("int unsigned")) {
                            result.append("    " + " `" + columnName + "` " + "int (" + columnSize + ")" + " unsigned " + nullString);
                        } else {
                            if (columnType.trim().equalsIgnoreCase("bigint unsigned")) {
                                result.append("    " + " `" + columnName + "` " + "bigint (" + columnSize + ")" + " unsigned " + nullString);
                            } else {
                                if (columnType.trim().equalsIgnoreCase("date")) {
                                    result.append("    " + " `" + columnName + "` date " + nullString);
                                } else {
                                    if (columnType.trim().equalsIgnoreCase("numeric")) {
                                        result.append("    " + " `" + columnName + "` " + columnType + " (" + columnSize + "," + decimalDigits + ")" + " " + nullString);
                                    } else {
                                        result.append("    " + " `" + columnName + "` " + columnType + " (" + columnSize + ")" + " " + nullString);
                                    }
                                }
                            }
                        }

                        if (primaryKey.equalsIgnoreCase(columnName) && (columnType.trim().equalsIgnoreCase("int unsigned") || columnType.trim().equalsIgnoreCase("int"))) {
                            result.append(" auto_increment");
                        } else {
                            result.append(defaultValueString);
                        }

                        // for (int i = 1; i < 20; i++) {
                        // System.err.println(i + " " + tableMetaData.getObject(i));
                        // }

                    }
                    tableMetaData.close();

                    // PRIMARY
                    if (primaryKey.trim().length() != 0) {
                        result.append(",\n     PRIMARY KEY (`" + primaryKey + "`)\n);\n");
                    } else {
                        result.append(");\n");
                    }
                    // Creation de la table dans la nouvelle base
                    baseSQLNew.getDataSource().getConnection().prepareStatement("DROP TABLE IF EXISTS `" + newBase + "`.`" + tableName + "`").executeUpdate();
                    System.err.println("Execute Query " + result);
                    baseSQLNew.getDataSource().getConnection().prepareStatement(result.toString()).executeUpdate();

                    // Dump Table
                    // dumpTable(baseSQLDefault, baseSQLNew, tableName);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            ExceptionHandler.handle("Erreur pendant la création de la base!", e);
        }
    }

    public static void dupliquePGSqlDB(String baseDefault, String newBase, StatusListener l) {
        final SQLBase base = Configuration.getInstance().getBase();

        // FIXME Replace by SQLCopy

        // FIXME ADD TRIGGER TO UPDATE SOLDE COMPTE_PCE
        // ComptaPropsConfiguration instance = ComptaPropsConfiguration.create();
        // Configuration.setInstance(instance);

        // instance.setUpSocieteDataBaseConnexion(baseDefault);
        // DatabaseMetaData dbMetaDataSocieteDefault;
        List<SQLTable> listTableDefault;
        try {
            log(l, "Création du schéma");

            // Creation de la nouvelle base
            final String reqDatabase = SQLSelect.quote("CREATE SCHEMA %i", newBase);

            base.getDataSource().execute(reqDatabase);
            final Set<String> rootsToMap = base.getDBSystemRoot().getRootsToMap();
            rootsToMap.add(baseDefault);
            rootsToMap.add(newBase);
            // TODO: Sylvain voir pour y faire rapidement
            base.fetchTables();
            final SQLSchema baseSQLDefault = base.getSchema(baseDefault);
            final SQLSchema baseSQLNew = base.getSchema(newBase);
            // Table de la base par defaut
            listTableDefault = new ArrayList<SQLTable>(baseSQLDefault.getTables());
            log(l, "Traitement des " + listTableDefault.size() + " tables");
            // SQLSchema baseSQLNew = Configuration.getInstance().getBase().getSchema(newBase);
            // ResultSet rs = dbMetaDataSocieteDefault.getTables("", "", null, null);
            for (int i = 0; i < listTableDefault.size(); i++) {
                SQLTable table = listTableDefault.get(i);
                StringBuffer result = new StringBuffer();
                String tableName = table.getName();

                // Creation d'une table
                // result.append("\n\n-- " + tableName);
                result.append("\nCREATE TABLE \"" + newBase + "\".\"" + tableName + "\" (\n");
                Set<SQLField> tableFields = table.getFields();
                boolean firstLine = true;

                // On recupere la cle primaire
                String primaryKey = (table.getKey() == null) ? "" : table.getKey().getName();

                System.err.println(primaryKey);

                // Creation des columns
                for (SQLField field : tableFields) {

                    if (firstLine) {
                        firstLine = false;
                    } else {
                        result.append(",\n");
                    }

                    // COLUMN NAME, TYPE AND SIZE
                    String columnName = field.getName();
                    String columnType = field.getType().getTypeName();

                    // NULL OR NOT NULL
                    // field.getType().getSize();
                    // String nullable = tableMetaData.getString("IS_NULLABLE");
                    String nullString = "NULL";
                    DBStructureItemDB db = field.getDB();
                    // if ("NO".equalsIgnoreCase(nullable)) {
                    // nullString = "NOT NULL";
                    // }

                    // DEFAULT
                    Object defaultValueO = field.getDefaultValue();
                    String defaultValue = (defaultValueO == null) ? null : defaultValueO.toString();
                    String defaultValueString = "";
                    if (defaultValue != null) {
                        defaultValueString = " default " + defaultValue;
                    }

                    int columnSize = field.getType().getSize();
                    Integer decimalDigit = (Integer) field.getMetadata("DECIMAL_DIGITS");
                    String stringColumnSize = "";
                    if (Integer.valueOf(columnSize).intValue() > 0 && Integer.valueOf(columnSize).intValue() < 10000) {
                        stringColumnSize = " (" + columnSize;

                        if (decimalDigit != null && Integer.valueOf(decimalDigit).intValue() > 0) {
                            stringColumnSize += ", " + decimalDigit;
                        }

                        stringColumnSize += ")";
                    }

                    if (primaryKey.equalsIgnoreCase(columnName) && (columnType.trim().equalsIgnoreCase("serial"))) {
                        result.append("    " + " \"" + columnName + "\" " + columnType);
                    } else {
                        if ((columnType.trim().equalsIgnoreCase("character varying") || columnType.trim().equalsIgnoreCase("varchar") || columnType.trim().equalsIgnoreCase("numeric"))
                                && stringColumnSize.length() > 0) {
                            result.append("    " + " \"" + columnName + "\" " + columnType + stringColumnSize + " ");
                            result.append(defaultValueString);
                        } else {
                            result.append("    " + " \"" + columnName + "\" " + columnType + " ");
                            result.append(defaultValueString);
                        }
                    }
                }

                // FOREIGN
                Set<SQLField> setForeign = table.getForeignKeys();
                for (SQLField field2 : setForeign) {

                    // Only if not in default
                    final SQLTable foreignTable = base.getGraph().getForeignTable(field2);
                    SQLName pointsToTable = foreignTable.getSQLName();
                    String pointsToBase = "\"" + pointsToTable.getFirst() + "\"";
                    String pointsToSchema = foreignTable.getSchema().getName();
                    String pointsToField = foreignTable.getKey().getName();

                    if (!pointsToSchema.equalsIgnoreCase(baseSQLDefault.getName())) {
                        result.append(",\n     FOREIGN KEY (\"" + field2.getName() + "\")" + " REFERENCES " + pointsToBase + ".\"" + pointsToSchema + "\".\"" + pointsToTable.getName() + "\" (\""
                                + pointsToField + "\") MATCH SIMPLE" + " ON UPDATE NO ACTION ON DELETE NO ACTION");
                    }
                }

                // PRIMARY
                if (primaryKey.trim().length() != 0) {
                    result.append(",\n     PRIMARY KEY (\"" + primaryKey + "\")\n);\n");
                } else {
                    result.append(");\n");
                }
                // Creation de la table dans la nouvelle base
                log(l, "Création de la table " + tableName + " " + (i + 1) + "/" + listTableDefault.size());
                base.getDataSource().getConnection().prepareStatement("DROP TABLE IF EXISTS \"" + newBase + "\".\"" + tableName + "\"").executeUpdate();
                System.err.println("Execute Query " + result);
                base.getDataSource().getConnection().prepareStatement(result.toString()).executeUpdate();

                log(l, "Copie de la table " + tableName + " " + (i + 1) + "/" + listTableDefault.size());

                // Dump Table
                dumpTable(baseSQLDefault, baseSQLNew, tableName);
                log(l, "Maj des séquences table " + tableName + " " + (i + 1) + "/" + listTableDefault.size());
                log(l, "Table " + tableName + " " + (i + 1) + "/" + listTableDefault.size() + " OK");
            }
            // TODO: Sylvain voir pour le faire rapidement: fetchSchema(...)

            base.fetchTables();
            Changer.change(baseSQLNew, FixSerial.class);

            log(l, "Duplication terminée");

        } catch (SQLException e) {
            e.printStackTrace();
            ExceptionHandler.handle("Erreur pendant la création de la base!", e);
            log(l, "Erreur pendant la duplication");
        }

    }

    private static String getControleStatement(String customer, String base, String year) {

        StringBuffer s = new StringBuffer();
        s.append("ALTER TABLE \"" + base + "\".\"AFFAIRE_ELEMENT\" ADD COLUMN \"ID_DOMAINE\" integer default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"AFFAIRE_ELEMENT\" ADD CONSTRAINT \"AFFAIRE_ELEMENT_ID_DOMAINE_fkey\" FOREIGN KEY (\"ID_DOMAINE\") REFERENCES \"" + customer
                + "_Common\".\"DISCIPLINE\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");
        s.append("ALTER TABLE \"" + base + "\".\"CODE_MISSION\" ADD COLUMN \"ID_DOMAINE\" integer default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"CODE_MISSION\" ADD CONSTRAINT \"CODE_MISSION_ID_DOMAINE_fkey\" FOREIGN KEY (\"ID_DOMAINE\") REFERENCES \"" + customer
                + "_Common\".\"DISCIPLINE\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"FICHE_RENDEZ_VOUS_ELEMENT\" ADD COLUMN \"ID_DOMAINE\" integer default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"FICHE_RENDEZ_VOUS_ELEMENT\" ADD CONSTRAINT \"FICHE_RENDEZ_VOUS_ELEMENT_ID_DOMAINE_fkey\" FOREIGN KEY (\"ID_DOMAINE\") REFERENCES \"" + customer
                + "_Common\".\"DISCIPLINE\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"PROPOSITION_ELEMENT\" ADD COLUMN \"ID_DOMAINE\" integer default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"PROPOSITION_ELEMENT\" ADD CONSTRAINT \"PROPOSITION_ELEMENT_ID_DOMAINE_fkey\" FOREIGN KEY (\"ID_DOMAINE\") REFERENCES \"" + customer
                + "_Common\".\"DISCIPLINE\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"SAISIE_VENTE_FACTURE_ELEMENT\" ADD COLUMN \"ID_DOMAINE\" integer default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"SAISIE_VENTE_FACTURE_ELEMENT\" ADD CONSTRAINT \"SAISIE_VENTE_FACTURE_ELEMENT_ID_DOMAINE_fkey\" FOREIGN KEY (\"ID_DOMAINE\") REFERENCES \"" + customer
                + "_Common\".\"DISCIPLINE\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"SAISIE_VENTE_FACTURE\" ADD COLUMN \"ID_VERIFICATEUR\"  int4  default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"SAISIE_VENTE_FACTURE\" ADD CONSTRAINT \"SAISIE_VENTE_FACTURE_ID_VERIFICATEUR_fkey\" FOREIGN KEY (\"ID_VERIFICATEUR\") REFERENCES \"" + customer + "_"
                + year + "\".\"VERIFICATEUR\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"POURCENT_SERVICE\" ADD COLUMN \"ID_VERIFICATEUR\"  int4  default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"POURCENT_SERVICE\" ADD CONSTRAINT \"POURCENT_SERVICE_ID_VERIFICATEUR_fkey\" FOREIGN KEY (\"ID_VERIFICATEUR\") REFERENCES \"" + customer + "_" + year
                + "\".\"VERIFICATEUR\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"FICHE_RENDEZ_VOUS\" ADD COLUMN \"ID_VERIFICATEUR\"  int4  default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"FICHE_RENDEZ_VOUS\" ADD CONSTRAINT \"FICHE_RENDEZ_VOUS_ID_VERIFICATEUR_fkey\" FOREIGN KEY (\"ID_VERIFICATEUR\") REFERENCES \"" + customer + "_" + year
                + "\".\"VERIFICATEUR\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"FICHE_RENDEZ_VOUS_ELEMENT\" ADD COLUMN \"ID_VERIFICATEUR\"  int4  default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"FICHE_RENDEZ_VOUS_ELEMENT\" ADD CONSTRAINT \"FICHE_RENDEZ_VOUS_ELEMENT_ID_VERIFICATEUR_fkey\" FOREIGN KEY (\"ID_VERIFICATEUR\") REFERENCES \""
                + customer + "_" + year + "\".\"VERIFICATEUR\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"RAPPORT\" ADD COLUMN \"ID_VERIFICATEUR\"  int4  default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"RAPPORT\" ADD CONSTRAINT \"RAPPORT_ID_VERIFICATEUR_fkey\" FOREIGN KEY (\"ID_VERIFICATEUR\") REFERENCES \"" + customer + "_" + year
                + "\".\"VERIFICATEUR\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"ORDRE_MISSION\" ADD COLUMN \"ID_VERIFICATEUR\"  int4  default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"ORDRE_MISSION\" ADD CONSTRAINT \"ORDRE_MISSION_ID_VERIFICATEUR_fkey\" FOREIGN KEY (\"ID_VERIFICATEUR\") REFERENCES \"" + customer + "_" + year
                + "\".\"VERIFICATEUR\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"SAISIE_VENTE_FACTURE_ELEMENT\" ADD COLUMN \"ID_MISSION\"  int4  default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"SAISIE_VENTE_FACTURE_ELEMENT\" ADD CONSTRAINT \"SAISIE_VENTE_FACTURE_ELEMENT_ID_MISSION_fkey\" FOREIGN KEY (\"ID_MISSION\") REFERENCES \"" + customer
                + "_" + year + "\".\"MISSION\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"FICHE_RENDEZ_VOUS_ELEMENT\" ADD COLUMN \"ID_MISSION\"  int4  default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"FICHE_RENDEZ_VOUS_ELEMENT\" ADD CONSTRAINT \"FICHE_RENDEZ_VOUS_ELEMENT_ID_MISSION_fkey\" FOREIGN KEY (\"ID_MISSION\") REFERENCES \"" + customer + "_"
                + year + "\".\"MISSION\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"POURCENT_SERVICE\" ADD COLUMN \"ID_SERVICE\"  int4  default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"POURCENT_SERVICE\" ADD CONSTRAINT \"POURCENT_SERVICE_ID_SERVICE_fkey\" FOREIGN KEY (\"ID_SERVICE\") REFERENCES \"" + customer
                + "_Common\".\"SERVICE\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"CODE_MISSION\" ADD COLUMN \"ID_SERVICE\"  int4  default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"CODE_MISSION\" ADD CONSTRAINT \"CODE_MISSION_ID_SERVICE_fkey\" FOREIGN KEY (\"ID_SERVICE\") REFERENCES \"" + customer
                + "_Common\".\"SERVICE\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"AFFAIRE_ELEMENT\" ADD COLUMN \"ID_PERIODICITE\"  int4  default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"AFFAIRE_ELEMENT\" ADD CONSTRAINT \"AFFAIRE_ELEMENT_ID_PERIODICITE_fkey\" FOREIGN KEY (\"ID_PERIODICITE\") REFERENCES \"" + customer
                + "_Common\".\"PERIODICITE\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"FICHE_RENDEZ_VOUS_ELEMENT\" ADD COLUMN \"ID_PERIODICITE\"  int4  default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"FICHE_RENDEZ_VOUS_ELEMENT\" ADD CONSTRAINT \"FICHE_RENDEZ_VOUS_ELEMENT_ID_PERIODICITE_fkey\" FOREIGN KEY (\"ID_PERIODICITE\") REFERENCES \""
                + customer + "_Common\".\"PERIODICITE\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"PROPOSITION_ELEMENT\" ADD COLUMN \"ID_PERIODICITE\"  int4  default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"PROPOSITION_ELEMENT\" ADD CONSTRAINT \"PROPOSITION_ELEMENT_ID_PERIODICITE_fkey\" FOREIGN KEY (\"ID_PERIODICITE\") REFERENCES \"" + customer
                + "_Common\".\"PERIODICITE\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"RAPPORT\" ADD COLUMN \"ID_PERIODICITE\"  int4  default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"RAPPORT\" ADD CONSTRAINT \"RAPPORT_ID_PERIODICITE_fkey\" FOREIGN KEY (\"ID_PERIODICITE\") REFERENCES \"" + customer
                + "_Common\".\"PERIODICITE\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"SAISIE_VENTE_FACTURE_ELEMENT\" ADD COLUMN \"ID_PERIODICITE\"  int4  default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"SAISIE_VENTE_FACTURE_ELEMENT\" ADD CONSTRAINT \"SAISIE_VENTE_FACTURE_ELEMENT_ID_PERIODICITE_fkey\" FOREIGN KEY (\"ID_PERIODICITE\") REFERENCES \""
                + customer + "_Common\".\"PERIODICITE\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");

        s.append("ALTER TABLE \"" + base + "\".\"AVOIR_CLIENT_ELEMENT\" ADD COLUMN \"ID_PERIODICITE\"  int4  default 1;");
        s.append("ALTER TABLE \"" + base + "\".\"AVOIR_CLIENT_ELEMENT\" ADD CONSTRAINT \"AVOIR_CLIENT_ELEMENT_ID_PERIODICITE_fkey\" FOREIGN KEY (\"ID_PERIODICITE\") REFERENCES \"" + customer
                + "_Common\".\"PERIODICITE\" (\"ID\") MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;");
        return s.toString();
    }

    private static void log(StatusListener l, String message) {
        if (l != null) {
            l.statusChanged(message);
        }
    }

    /**
     * copie l'integralite de la table "tableName" de la base "base" dans la nouvelle base "baseNew"
     * 
     * @param base
     * @param baseNew
     * @param tableName
     */
    private static void dumpTable(SQLSchema base, SQLSchema baseNew, String tableName) {

        try {
            // First we output the create table stuff
            // PreparedStatement stmt = dbConn.prepareStatement("SELECT * FROM " + tableName);

            List<Map> rs = base.getBase().getDataSource().execute("SELECT * FROM \"" + base.getName() + "\".\"" + tableName + "\"");

            // ResultSetMetaData metaData = rs.getMetaData();
            if (rs == null || rs.size() == 0) {
                return;
            }
            int columnCount = rs.get(0).keySet().size();

            // Now we can output the actual data
            PreparedStatement statement;
            StringBuffer query = new StringBuffer();
            query = new StringBuffer("INSERT INTO \"" + baseNew.getName() + "\".\"" + tableName + "\"(");

            Map m = rs.get(0);
            for (Iterator i = m.keySet().iterator(); i.hasNext();) {
                String key = i.next().toString();
                if (i.hasNext()) {
                    query.append("\"" + key.toString() + "\", ");
                } else {
                    query.append("\"" + key.toString() + "\") VALUES (");
                }
            }

            // StringBuffer query = new StringBuffer("INSERT INTO `" + baseNew.getName() + "`.`" +
            // tableName + "` VALUES (");
            for (int i = 0; i < columnCount - 1; i++) {
                query.append("?,");
            }
            query.append("?)");
            statement = baseNew.getBase().getDataSource().getConnection().prepareStatement(query.toString());

            for (Map map : rs) {
                int i = 0;
                for (Object key : map.keySet()) {

                    statement.setObject(i + 1, map.get(key));
                    i++;
                }
                // TODO: g
                statement.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println("Unable to dump table " + tableName + " because: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Affiche si il y a des différences entre les tables de base et baseDefault
     * 
     * @param base
     * @param baseDefault
     */
    public static void compareDB(int base, int baseDefault) {

        try {
            if (Configuration.getInstance() == null) {
                Configuration.setInstance(ComptaPropsConfiguration.create());
            }
            Configuration instance = Configuration.getInstance();
            SQLTable tableSociete = Configuration.getInstance().getBase().getTable("SOCIETE_COMMON");

            String baseName = tableSociete.getRow(base).getString("DATABASE_NAME");
            String baseDefaultName = tableSociete.getRow(baseDefault).getString("DATABASE_NAME");

            instance.getBase().getDBSystemRoot().getRootsToMap().clear();
            try {
                Set<String> s = new HashSet<String>();
                s.add(baseName);
                s.add(baseDefaultName);
                instance.getBase().fetchTables(s);
            } catch (SQLException e) {
                throw new IllegalStateException("could not access societe base", e);
            }
            // instance.getBase().getDBRoot(baseName);
            // instance.getBase().getDBRoot(baseDefaultName);
            System.err.println("baseName" + baseName);
            System.err.println("baseDefault" + baseDefaultName);
            instance.getSystemRoot().prependToRootPath("Common");
            instance.getSystemRoot().prependToRootPath(baseName);
            instance.getSystemRoot().prependToRootPath(baseDefaultName);

            SQLSchema baseSQL = instance.getBase().getSchema(baseName);
            SQLSchema baseSQLDefault = instance.getBase().getSchema(baseDefaultName);

            DatabaseMetaData dbMetaDataSociete = baseSQL.getBase().getDataSource().getConnection().getMetaData();
            DatabaseMetaData dbMetaDataSocieteDefault = baseSQLDefault.getBase().getDataSource().getConnection().getMetaData();

            Map<String, Map<String, SQLField>> mapTableSociete = new HashMap<String, Map<String, SQLField>>();
            Map<String, Map<String, SQLField>> mapTableSocieteDefault = new HashMap<String, Map<String, SQLField>>();

            ResultSet rs = dbMetaDataSociete.getTables("", baseSQL.getName(), "%", null);

            System.err.println("Start");

            while (rs.next()) {
                // System.err.println(rs.getString("TABLE_NAME") + ", TYPE ::" +
                // rs.getString("TABLE_TYPE"));

                if (rs.getString("TABLE_TYPE") != null && rs.getString("TABLE_TYPE").equalsIgnoreCase("TABLE")) {
                    Map<String, SQLField> m = new HashMap<String, SQLField>();
                    baseSQL.getTableNames();
                    Set<SQLField> s = baseSQL.getTable(rs.getString("TABLE_NAME")).getFields();
                    for (SQLField field : s) {
                        m.put(field.getName(), field);
                    }
                    mapTableSociete.put(rs.getString("TABLE_NAME"), m);
                }
            }
            rs.close();

            rs = dbMetaDataSocieteDefault.getTables("", baseSQLDefault.getName(), "%", null);

            while (rs.next()) {
                // System.err.println(rs.getString("TABLE_NAME") + ", TYPE ::" +
                // rs.getString("TABLE_TYPE"));
                if (rs.getString("TABLE_TYPE") != null && rs.getString("TABLE_TYPE").equalsIgnoreCase("TABLE")) {
                    Map<String, SQLField> m = new HashMap<String, SQLField>();
                    Set<SQLField> s = baseSQLDefault.getTable(rs.getString("TABLE_NAME")).getFields();
                    for (SQLField field : s) {
                        m.put(field.getName(), field);
                    }
                    mapTableSocieteDefault.put(rs.getString("TABLE_NAME"), m);
                }
            }
            rs.close();

            System.err.println("Test 1 " + mapTableSociete.keySet().size());
            // On verifie que toutes les tables de la societe sont contenues dans la base default
            for (String tableName : mapTableSociete.keySet()) {

                if (!mapTableSocieteDefault.containsKey(tableName)) {
                    System.err.println("!! **** La table " + tableName + " n'est pas dans la base " + baseDefault);

                } else {
                    Map<String, SQLField> mSoc = mapTableSociete.get(tableName);
                    Map<String, SQLField> mDef = mapTableSocieteDefault.get(tableName);
                    if (mSoc.keySet().containsAll(mDef.keySet())) {
                        if (mSoc.keySet().size() == mDef.keySet().size()) {
                            System.err.println("Table " + tableName + " --- OK");
                            compareTypeField(mSoc, mDef);
                        } else {
                            if (mSoc.keySet().size() > mDef.keySet().size()) {
                                for (String fieldName : mDef.keySet()) {
                                    mSoc.remove(fieldName);
                                }
                                System.err.println("!! **** Difference Table " + tableName);
                                System.err.println(tableSociete.getRow(baseDefault).getString("DATABASE_NAME") + " Set Column " + mSoc);
                                System.err.println(getAlterTable(mSoc, tableSociete.getRow(baseDefault).getString("DATABASE_NAME"), tableName));
                            } else {

                            }
                        }
                    } else {
                        // System.err.println("!! **** Difference Table " + tableName);
                        // System.err.println(tableSociete.getRow(base).getString("DATABASE_NAME") +
                        // " Set Column " + mapTableSociete.get(tableName));
                        // System.err.println(tableSociete.getRow(baseDefault).getString("DATABASE_NAME")
                        // + " Set Column " + mapTableSocieteDefault.get(tableName));
                        for (String fieldName : mSoc.keySet()) {
                            mDef.remove(fieldName);
                        }
                        System.err.println("!! **** Difference Table " + tableName);
                        System.err.println(tableSociete.getRow(base).getString("DATABASE_NAME") + " Set Column " + mDef);
                        System.err.println(getAlterTable(mDef, tableSociete.getRow(base).getString("DATABASE_NAME"), tableName));
                    }
                }
            }

            System.err.println("Test 2 " + mapTableSocieteDefault.keySet().size());
            // On verifie que toutes les tables de la base default sont contenues dans la base
            // societe
            for (Iterator i = mapTableSocieteDefault.keySet().iterator(); i.hasNext();) {
                Object tableName = i.next();
                if (!mapTableSociete.containsKey(tableName)) {
                    System.err.println("!! **** La table " + tableName + " n'est pas dans la base " + baseDefault);
                }
            }

            SQLSchema schem = instance.getBase().getSchema("Common");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String getAlterTable(Map<String, SQLField> m, String baseName, String tableName) {
        StringBuffer buf = new StringBuffer();
        for (String s : m.keySet()) {
            SQLField field = m.get(s);
            buf.append("ALTER TABLE \"" + baseName + "\".\"" + tableName + "\" ADD COLUMN ");
            buf.append("\"" + field.getName() + "\" ");
            buf.append(getType(field));
            buf.append(";\n");
        }

        return buf.toString();
    }

    private static String getType(SQLField field) {
        String columnName = field.getName();
        String columnType = field.getType().getTypeName();
        StringBuffer result = new StringBuffer();
        // NULL OR NOT NULL
        // field.getType().getSize();
        // String nullable = tableMetaData.getString("IS_NULLABLE");
        String nullString = "NULL";
        DBStructureItemDB db = field.getDB();
        // if ("NO".equalsIgnoreCase(nullable)) {
        // nullString = "NOT NULL";
        // }

        // DEFAULT
        Object defaultValueO = field.getDefaultValue();
        String defaultValue = (defaultValueO == null) ? null : defaultValueO.toString();
        String defaultValueString = "";
        if (defaultValue != null) {
            defaultValueString = " default " + defaultValue;
        }

        int columnSize = field.getType().getSize();
        Integer decimalDigit = (Integer) field.getMetadata("DECIMAL_DIGITS");
        String stringColumnSize = "";
        if (Integer.valueOf(columnSize).intValue() > 0) {
            stringColumnSize = " (" + columnSize;

            if (decimalDigit != null && Integer.valueOf(decimalDigit).intValue() > 0) {
                stringColumnSize += ", " + decimalDigit;
            }

            stringColumnSize += ")";
        }

        if ((columnType.trim().equalsIgnoreCase("character varying") || columnType.trim().equalsIgnoreCase("varchar") || columnType.trim().equalsIgnoreCase("numeric"))
                && stringColumnSize.length() > 0) {
            result.append(" " + columnType + stringColumnSize + " ");
            result.append(defaultValueString);
        } else {
            result.append(" " + columnType + " ");
            result.append(defaultValueString);
        }
        return result.toString();
    }

    private static void compareTypeField(Map fieldsDefault, Map fields) {
        for (Iterator i = fieldsDefault.keySet().iterator(); i.hasNext();) {

            Object o = i.next();
            SQLField field = (SQLField) fieldsDefault.get(o);
            SQLField fieldDefault = (SQLField) fields.get(o);

            if (field != null && fieldDefault != null && field.getType() != fieldDefault.getType()) {
                System.err.println("---------> Type different Table " + field.getTable() + " -- Field " + field.getName());
            }
        }
    }

    private static void updateMultiBase() {
        // Calcul automatique du ht des saisies de vente avec facture

        System.err.println("Start");
        // on recupere les differentes bases
        ComptaPropsConfiguration instance = ComptaPropsConfiguration.create();
        Configuration.setInstance(instance);
        SQLBase base = Configuration.getInstance().getBase();
        SQLTable tableBase = base.getTable("SOCIETE_COMMON");

        SQLSelect sel = new SQLSelect(base, false);
        sel.addSelect(tableBase.getField("DATABASE_NAME"));

        List listBasesNX = (List) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());

        // for (int i = 0; i < listBasesNX.size(); i++) {
        // Object[] tmp = (Object[]) listBasesNX.get(i);
        //
        // setOrdreComptePCE(tmp[0].toString());
        // }

        reOrderCompteID("Default");
        System.err.println("End");
    }

    private static void updateSaisieVC(String databaseName) {

        System.err.println("Update " + databaseName);
        String select = "SELECT MONTANT_TTC, TAUX, ID FROM \"" + databaseName + "\".SAISIE_VENTE_COMPTOIR, \"" + databaseName + "\".TAXE";
        System.err.println("Request " + select);
        List listBaseNX = (List) Configuration.getInstance().getBase().getDataSource().execute(select, new ArrayListHandler());

        for (int i = 0; i < listBaseNX.size(); i++) {
            Object[] tmp = (Object[]) listBaseNX.get(i);
            PrixTTC p = new PrixTTC(Long.parseLong(tmp[0].toString()));
            long ht = p.calculLongHT(((Float) tmp[1]).doubleValue() / 100.0);

            // Update Value
            String updateVC = "UPDATE \"" + databaseName + "\".SAISIE_VENTE_COMPTOIR SET MONTANT_HT = " + ht + " WHERE ID=" + tmp[2];
            Configuration.getInstance().getBase().execute(updateVC);
        }
    }

    public static void setOrdreComptePCE(String databaseName) {
        String select = "SELECT ID, ORDRE FROM \"" + databaseName + "\".COMPTE_PCE ORDER BY NUMERO";
        List listBaseNX = (List) Configuration.getInstance().getBase().getDataSource().execute(select, new ArrayListHandler());
        for (int i = 0; i < listBaseNX.size(); i++) {
            Object[] tmp = (Object[]) listBaseNX.get(i);

            String update = "UPDATE \"" + databaseName + "\".COMPTE_PCE SET ORDRE=" + (i + 1) + " WHERE ID=" + tmp[0];
            Configuration.getInstance().getBase().execute(update);
        }
    }

    public static void reOrderCompteID(String databaseName) {
        String select = "SELECT ID, ORDRE FROM \"" + databaseName + "\".COMPTE_PCE WHERE ID > 1 ORDER BY NUMERO";

        List listBaseNX = (List) Configuration.getInstance().getBase().getDataSource().execute(select, new ArrayListHandler());
        for (int i = 0; i < listBaseNX.size(); i++) {
            Object[] tmp = (Object[]) listBaseNX.get(i);
            int id = Integer.valueOf(tmp[0].toString()).intValue();
            String update = "UPDATE \"" + databaseName + "\".COMPTE_PCE SET ID=" + (id + 1000) + " WHERE ID=" + id;
            Configuration.getInstance().getBase().execute(update);
        }

        select = "SELECT ID, ORDRE FROM \"" + databaseName + "\".COMPTE_PCE WHERE ID > 1 ORDER BY NUMERO";

        listBaseNX = (List) Configuration.getInstance().getBase().getDataSource().execute(select, new ArrayListHandler());
        for (int i = 0; i < listBaseNX.size(); i++) {
            Object[] tmp = (Object[]) listBaseNX.get(i);
            int id = Integer.valueOf(tmp[0].toString()).intValue();
            String update = "UPDATE \"" + databaseName + "\".COMPTE_PCE SET ID=" + (i + 2) + " WHERE ID=" + id;
            Configuration.getInstance().getBase().execute(update);
        }

    }

    public static void setOrder(SQLBase base) {
        Set<String> tableNames = base.getTableNames();
        for (String tableName : tableNames) {
            SQLTable table = base.getTable(tableName);
            // SQLField fieldPrimaryKey = table.getKey();
            SQLField field = table.getOrderField();

            if (field != null) {
                base.execute("ALTER TABLE \"" + tableName + "\" ALTER COLUMN \"" + field.getName() + "\" SET DEFAULT 0;");
                base.execute("ALTER TABLE \"" + tableName + "\" ALTER COLUMN \"" + field.getName() + "\" SET NOT NULL;");
            }
        }
    }

    /**
     * Check si la table posséde au moins une ligne avec un ordre different null le cas cas échéant
     * le crée
     * 
     * @param base
     */
    public static void correct(SQLBase base) {
        Set<String> tableNames = base.getTableNames();
        for (String tableName : tableNames) {

            if (base.getTable(tableName).contains("ORDRE")) {
                SQLSelect select = new SQLSelect(base);
                select.addSelect("ORDRE");
                List l = base.getDataSource().execute(select.asString());
                if (l == null || l.size() == 0) {
                    SQLRowValues rowVals = new SQLRowValues(base.getTable(tableName));
                    rowVals.put("ORDRE", 0);
                    try {
                        rowVals.commit();
                    } catch (SQLException e) {

                        e.printStackTrace();
                    }
                }
            }
        }
        // TODO Checker que toutes les tables sont dans FWK_UNDEFINED_ID
    }

    public static void addUndefined(SQLBase base) {
        Set<String> tableNames = base.getTableNames();
        for (String tableName : tableNames) {
            SQLTable table = base.getTable(tableName);
            SQLField fieldPrimaryKey = table.getKey();

            if (fieldPrimaryKey != null && fieldPrimaryKey.getType().getJavaType().getSuperclass() != null && fieldPrimaryKey.getType().getJavaType().getSuperclass() == Number.class) {

                String patch = "INSERT INTO \"" + tableName + "\"(\"" + fieldPrimaryKey.getName() + "\") VALUES (1)";
                base.execute(patch);
            }
        }

    }

    public static void fixUserCommon(int base) {

        if (Configuration.getInstance() == null) {
            Configuration.setInstance(ComptaPropsConfiguration.create());
        }
        Configuration instance = Configuration.getInstance();
        SQLTable tableSociete = Configuration.getInstance().getBase().getTable("SOCIETE_COMMON");

        String baseName = tableSociete.getRow(base).getString("DATABASE_NAME");

        instance.getBase().getDBSystemRoot().getRootsToMap().clear();
        try {
            Set<String> s = new HashSet<String>();
            s.add(baseName);
            instance.getBase().fetchTables(s);
        } catch (SQLException e) {
            throw new IllegalStateException("could not access societe base", e);
        }

        System.err.println("baseName" + baseName);
        instance.getSystemRoot().prependToRootPath("Common");
        instance.getSystemRoot().prependToRootPath(baseName);

        SQLSchema baseSQL = instance.getBase().getSchema(baseName);

        DatabaseMetaData dbMetaDataSociete;
        try {
            dbMetaDataSociete = baseSQL.getBase().getDataSource().getConnection().getMetaData();

            String[] type = new String[1];
            type[0] = "TABLE";
            ResultSet rs = dbMetaDataSociete.getTables("", baseSQL.getName(), "%", null);

            System.err.println("Start " + rs.getFetchSize());
            int i = 0;
            while (rs.next()) {

                if (rs.getString("TABLE_TYPE") != null && rs.getString("TABLE_TYPE").equalsIgnoreCase("TABLE")) {
                    // System.err.println("FIND TABLE");
                    // baseSQL.getTableNames();
                    final SQLTable table = baseSQL.getTable(rs.getString("TABLE_NAME"));
                    Set<SQLField> s = table.getFields();
                    for (SQLField field : s) {
                        if (field.getName().equalsIgnoreCase("ID_USER_COMMON_CREATE") || field.getName().equalsIgnoreCase("ID_USER_COMMON_MODIFY")) {
                            Object o = field.getDefaultValue();
                            if (o == null || (o instanceof Integer && ((Integer) o) == 0)

                            ) {
                                System.err.println("Bad default on " + field);
                                baseSQL.getBase()
                                        .execute(
                                                "ALTER TABLE \"" + field.getTable().getSchema().getName() + "\".\"" + field.getTable().getName() + "\" ALTER COLUMN \"" + field.getName()
                                                        + "\" SET DEFAULT 1;");

                                baseSQL.getBase().execute(
                                        "UPDATE \"" + field.getTable().getSchema().getName() + "\".\"" + field.getTable().getName() + "\" SET \"" + field.getName() + "\"=1 WHERE \"" + field.getName()
                                                + "\"=0 OR \"" + field.getName() + "\" IS NULL;");
                            }

                        }
                    }
                }
                // System.err.println(i++ + " " + rs.getString("TABLE_TYPE"));
            }
            rs.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // updateMultiBase();
        // compareDB(41, 1);

        fixUserCommon(41);

        // try {
        // patchSequences(new
        // PropsConfiguration(ActionDB.class.getResourceAsStream("changeBase.properties"),
        // props).getBase());
        // setOrder(new
        // PropsConfiguration(ActionDB.class.getResourceAsStream("changeBase.properties"),
        // props).getBase());
        // } catch (IOException e) {
        // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
    }
}
