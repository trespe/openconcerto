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
import org.openconcerto.sql.changer.correct.FixSerial;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBStructureItemDB;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSchema;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.utils.ChangeTable.ConcatStep;
import org.openconcerto.sql.utils.SQLCreateRoot;
import org.openconcerto.utils.ExceptionHandler;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumSet;
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
    public static void dupliqueDB(String baseDefault, String newBase, StatusListener l) {
        final DBSystemRoot sysRoot = Configuration.getInstance().getSystemRoot();

        // FIXME ADD TRIGGER TO UPDATE SOLDE COMPTE_PCE
        // ComptaPropsConfiguration instance = ComptaPropsConfiguration.create();
        // Configuration.setInstance(instance);

        try {
            log(l, "Création du schéma");
            if (!sysRoot.getChildrenNames().contains(baseDefault)) {
                sysRoot.addRootToMap(baseDefault);
                sysRoot.refetch(Collections.singleton(baseDefault));
            }
            final DBRoot baseSQLDefault = sysRoot.getRoot(baseDefault);
            log(l, "Traitement des " + baseSQLDefault.getChildrenNames().size() + " tables");

            final SQLCreateRoot createRoot = baseSQLDefault.getDefinitionSQL(sysRoot.getServer().getSQLSystem());
            final SQLDataSource ds = sysRoot.getDataSource();
            // be safe don't add DROP SCHEMA
            final List<String> sql = createRoot.asStringList(newBase, false, true, EnumSet.of(ConcatStep.ADD_FOREIGN));
            // create root
            ds.execute(sql.get(0));
            // create tables (without constraints)
            ds.execute(sql.get(1));
            sysRoot.addRootToMap(newBase);
            // TODO find a more functional way
            final boolean origVal = Boolean.getBoolean(SQLSchema.NOAUTO_CREATE_METADATA);
            if (!origVal)
                System.setProperty(SQLSchema.NOAUTO_CREATE_METADATA, "true");
            sysRoot.refetch(Collections.singleton(newBase));
            if (!origVal)
                System.setProperty(SQLSchema.NOAUTO_CREATE_METADATA, "false");
            final DBRoot baseSQLNew = sysRoot.getRoot(newBase);

            final Set<SQLTable> newTables = baseSQLNew.getTables();
            int i = 0;
            for (final SQLTable table : newTables) {
                String tableName = table.getName();

                log(l, "Copie de la table " + tableName + " " + (i + 1) + "/" + newTables.size());
                // Dump Table
                dumpTable(baseSQLDefault, table);
                log(l, "Table " + tableName + " " + (i + 1) + "/" + newTables.size() + " OK");
                i++;
            }
            // create constraints
            ds.execute(sql.get(2));
            assert sql.size() == 3;

            if (sysRoot.getServer().getSQLSystem() == SQLSystem.POSTGRESQL) {
                log(l, "Maj des séquences des tables");
                new FixSerial(sysRoot).changeAll(baseSQLNew);
            }


            log(l, "Duplication terminée");

        } catch (Throwable e) {
            e.printStackTrace();
            ExceptionHandler.handle("Erreur pendant la création de la base!", e);
            log(l, "Erreur pendant la duplication");
        }

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
    private static void dumpTable(DBRoot source, SQLTable newTable) {
        try {
            SQLRowValues.insertFromTable(newTable, source.getTable(newTable.getName()));
        } catch (SQLException e) {
            System.err.println("Unable to dump table " + newTable.getName());
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

            instance.getBase().getDBSystemRoot().mapAllRoots();
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
            final SQLTable t = base.getTable(tableName);
            final SQLField orderF = t.getOrderField();
            if (orderF != null) {
                SQLSelect select = new SQLSelect(base);
                select.addSelect(orderF);
                List l = base.getDataSource().execute(select.asString());
                if (l == null || l.size() == 0) {
                    SQLRowValues rowVals = new SQLRowValues(t);
                    rowVals.put(orderF.getName(), 0);
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

        instance.getBase().getDBSystemRoot().mapAllRoots();
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
