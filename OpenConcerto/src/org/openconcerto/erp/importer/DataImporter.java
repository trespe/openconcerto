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
 
 package org.openconcerto.erp.importer;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.openoffice.spreadsheet.Sheet;
import org.openconcerto.openoffice.spreadsheet.SpreadSheet;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.utils.text.CSVReader;
import org.openconcerto.utils.text.CharsetHelper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;

public class DataImporter {
    private boolean skipFirstLine = true;
    private final SQLTable table;
    private final Map<Integer, ValueConverter> map = new HashMap<Integer, ValueConverter>();
    private final Map<SQLField, List<Integer>> fieldMap = new HashMap<SQLField, List<Integer>>();
    private final Map<Integer, Constraint> constraints = new HashMap<Integer, Constraint>();
    private List<SQLField> uniqueField = new ArrayList<SQLField>();
    private List<SQLRowValues> valuesToUpdate = new ArrayList<SQLRowValues>();
    private List<SQLRowValues> valuesToInsert = new ArrayList<SQLRowValues>();
    private Map<ValueConverter, SQLField> foreignMap = new HashMap<ValueConverter, SQLField>();

    public DataImporter(SQLTable table) {
        this.table = table;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        System.setProperty("org.openconcerto.sql.structure.useXML", "true");
        final ComptaPropsConfiguration conf = ComptaPropsConfiguration.create();
        conf.setupLogging("Logs");
        Configuration.setInstance(conf);
        try {
            conf.getBase();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final ComptaPropsConfiguration comptaPropsConfiguration = ((ComptaPropsConfiguration) Configuration.getInstance());
        comptaPropsConfiguration.setUpSocieteDataBaseConnexion(39);
        UserManager.getInstance().setCurrentUser(2);

        SQLTable table = Configuration.getInstance().getRoot().findTable("ARTICLE");
        DataImporter importer = new DataImporter(table);
        importer.skipFirstLine = false;
        importer.map(0, table.getField("CODE"));
        importer.map(8, table.getField("ID_FOURNISSEUR"));
        importer.map(4, table.getField("NOM"));
        importer.addContraint(0, new NotEmptyConstraint());
        importer.addUniqueField(table.getField("CODE"));
        // ArrayTableModel m = importer.createModelFromODS(new File("c:/products-en.ods"));
        // ArrayTableModel m = importer.createModelFromCSV(new File("c:/products-en.csv"));
        // ArrayTableModel m = importer.createModelFromCSV(new File("c:/products-en.scsv.csv"));
        ArrayTableModel m = importer.createModelFromXLS(new File("c:/products-en.xls"));
        m.dump(0, 4);
        m = importer.createConvertedModel(m);
        System.out.println("Dump");
        m.dump(0, 4);
        importer.importFromModel(m);
        System.out.println(importer.getValuesToInsert().size() + " rows to insert");
        System.out.println(importer.getValuesToUpdate().size() + " rows to update");
        // importer.commit();

    }

    public void commit() throws SQLException {
        for (SQLRowValues row : this.valuesToInsert) {
            row.insert();
        }
        for (SQLRowValues row : this.valuesToUpdate) {
            row.update();
        }
    }

    public List<SQLRowValues> getValuesToInsert() {
        return valuesToInsert;
    }

    public List<SQLRowValues> getValuesToUpdate() {
        return valuesToUpdate;
    }

    public void addUniqueField(SQLField field) {
        if (this.uniqueField.contains(field)) {
            throw new IllegalStateException("Field " + field + " already specified");
        }
        this.uniqueField.add(field);

    }

    public void addContraint(int columnIndex, Constraint c) {
        constraints.put(Integer.valueOf(columnIndex), c);
    }

    public void map(int columnIndex, SQLField field) {
        map(columnIndex, field, new ValueConverter(field));
    }

    public void map(int columnIndex, ValueConverter converter) {
        map(columnIndex, converter.getField(), converter);
    }

    public void map(int columnIndex, SQLField field, SQLField foreignField) {
        final ValueConverter converter = new ValueConverter(foreignField);
        map(columnIndex, foreignField, converter);
        foreignMap.put(converter, field);

    }

    public void map(int columnIndex, SQLField field, ValueConverter converter) {
        final Integer value = Integer.valueOf(columnIndex);
        map.put(value, converter);
        List<Integer> l = fieldMap.get(field);
        if (l == null) {
            l = new ArrayList<Integer>();
            fieldMap.put(field, l);
        } else if (!field.getType().getJavaType().equals(String.class)) {
            throw new IllegalArgumentException("Mapping multiple column is only supoprted for String values");
        }
        if (l.contains(value)) {
            throw new IllegalArgumentException("Column " + columnIndex + " already mapped for field " + field.getFullName());
        }
        l.add(value);
    }

    public ArrayTableModel createModelFromODS(File odsFile) throws IOException {
        final SpreadSheet spreadSheet = SpreadSheet.createFromFile(odsFile);
        if (spreadSheet.getSheetCount() < 1) {
            return null;
        }
        final Sheet sheet = spreadSheet.getSheet(0);
        final int rowCount = sheet.getRowCount();
        int columnCount = 0;
        if (rowCount > 0) {
            final int maxColumnCount = sheet.getColumnCount();
            for (int j = 0; j < maxColumnCount; j++) {
                final Object valueAt = sheet.getValueAt(j, 0);
                if (valueAt == null || valueAt.toString().trim().isEmpty()) {
                    break;
                }
                columnCount++;
            }
        }
        int start = 0;
        if (skipFirstLine) {
            start = 1;
        }
        final List<List<Object>> rows = new ArrayList<List<Object>>(rowCount - start);
        for (int i = start; i < rowCount; i++) {
            List<Object> row = new ArrayList<Object>();
            for (int j = 0; j < columnCount; j++) {
                row.add(sheet.getValueAt(j, i));
            }
            rows.add(row);
        }

        return new ArrayTableModel(rows);
    }

    public ArrayTableModel createModelFromXLS(File xlsFile) throws IOException {
        final InputStream inputStream = new FileInputStream(xlsFile);
        final POIFSFileSystem fileSystem = new POIFSFileSystem(new BufferedInputStream(inputStream));
        final HSSFWorkbook workBook = new HSSFWorkbook(fileSystem);
        final HSSFSheet sheet = workBook.getSheetAt(0);
        Iterator<Row> rowsIterator = sheet.rowIterator();
        int columnCount = 0;
        int rowCount = 0;
        while (rowsIterator.hasNext()) {
            Row row = rowsIterator.next();
            int i = row.getPhysicalNumberOfCells();
            if (i > columnCount) {
                columnCount = i;
            }
            rowCount++;
        }
        // Extract data
        rowsIterator = sheet.rowIterator();
        int start = 0;
        if (skipFirstLine) {
            start = 1;
            rowsIterator.next();
        }
        final List<List<Object>> rows = new ArrayList<List<Object>>(rowCount - start);
        FormulaEvaluator evaluator = workBook.getCreationHelper().createFormulaEvaluator();

        while (rowsIterator.hasNext()) {
            final Row row = rowsIterator.next();
            final List<Object> rowData = new ArrayList<Object>();
            for (int i = 0; i < columnCount; i++) {
                final Cell cell = row.getCell(i);

                if (cell == null) {
                    rowData.add("");
                } else {
                    CellValue cellValue = evaluator.evaluate(cell);
                    if (cellValue == null) {
                        rowData.add("");
                    } else {
                        switch (cellValue.getCellType()) {
                        case Cell.CELL_TYPE_BOOLEAN:
                            rowData.add(Boolean.valueOf(cellValue.getBooleanValue()));
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            rowData.add(Double.valueOf(cellValue.getNumberValue()));
                            break;
                        case Cell.CELL_TYPE_STRING:
                            rowData.add(cellValue.getStringValue());
                            break;
                        case Cell.CELL_TYPE_FORMULA:
                            rowData.add(cell.getCellFormula());
                            break;
                        case Cell.CELL_TYPE_BLANK:
                            rowData.add("");
                            break;
                        default:
                            rowData.add(cellValue.getStringValue());
                            break;

                        }
                    }
                }
            }

            rows.add(rowData);

        }
        inputStream.close();
        return new ArrayTableModel(rows);

    }

    public ArrayTableModel createModelFromCSV(File csvFile) throws IOException {
        Charset cs = CharsetHelper.guessEncoding(csvFile, 4096, Charset.forName("Cp1252"));

        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), cs));
        String l = r.readLine();
        if (l == null) {
            return null;
        }
        char separator = ',';
        int cCount = 0;
        int scCount = 0;
        for (int i = 0; i < l.length(); i++) {
            char c = l.charAt(i);
            if (c == ',') {
                cCount++;
            } else if (c == ';') {
                scCount++;
            }
        }
        r.close();
        if (scCount > cCount) {
            separator = ';';
        }

        CSVReader csvReader = new CSVReader(new InputStreamReader(new FileInputStream(csvFile), cs), separator);
        List<String[]> lines = csvReader.readAll();
        final int rowCount = lines.size();
        final int columnCount = lines.get(0).length;

        int start = 0;
        if (skipFirstLine) {
            start = 1;
        }
        final List<List<Object>> rows = new ArrayList<List<Object>>(rowCount - start);
        for (int i = start; i < rowCount; i++) {
            List<Object> row = new ArrayList<Object>();
            String[] values = lines.get(i);
            for (int j = 0; j < columnCount; j++) {
                row.add(values[j]);
            }
            rows.add(row);
        }
        csvReader.close();
        return new ArrayTableModel(rows);

    }

    public ArrayTableModel createConvertedModel(ArrayTableModel model) {
        final int rowCount = model.getRowCount();
        final ArrayList<Integer> colsUsed = new ArrayList<Integer>(map.keySet());
        colsUsed.addAll(constraints.keySet());

        final int columnCount = 1 + Collections.max(colsUsed);

        final List<List<Object>> rows = new ArrayList<List<Object>>(rowCount);

        for (int i = 0; i < rowCount; i++) {
            boolean validRow = true;
            final List<Object> row = new ArrayList<Object>();
            for (int j = 0; j < columnCount; j++) {
                Object value = model.getValueAt(i, j);
                ValueConverter converter = map.get(j);
                if (converter != null) {
                    value = converter.convertFrom(value);
                }
                final Constraint constraint = constraints.get(j);
                // Verification de la validité de la valeur à importer
                if (constraint != null && !constraint.isValid(value)) {
                    validRow = false;
                    break;
                }
                row.add(value);
            }
            if (validRow) {
                rows.add(row);
            }
        }

        return new ArrayTableModel(rows);
    }

    public void importFromModel(ArrayTableModel model) throws IOException {
        final int rowCount = model.getRowCount();
        // Load existing data for duplication check
        final SQLRowValues vals = new SQLRowValues(table);

        for (SQLField field : this.fieldMap.keySet()) {
            if (field.getTable().equals(table)) {
                vals.put(field.getName(), null);
            } else {
                final Set<SQLField> foreignKeys = table.getForeignKeys(field.getTable());
                for (SQLField sqlField : foreignKeys) {
                    vals.put(sqlField.getName(), null);
                }
            }
        }

        System.out.println("Fetching values");
        SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(vals);
        List<SQLRowValues> existingRows = fetcher.fetch();
        System.out.println("Computing cache");
        final int existingRowsCount = existingRows.size();
        final ValueConverter[] converters = map.values().toArray(new ValueConverter[map.size()]);

        // Une map <Object(valeur),SQLRowValues> pour chaque champs unique
        Map<SQLField, Map<Object, SQLRowValues>> cache = new HashMap<SQLField, Map<Object, SQLRowValues>>();
        for (SQLField field : this.uniqueField) {
            Map<Object, SQLRowValues> m = new HashMap<Object, SQLRowValues>();
            cache.put(field, m);
            final String fieldName = field.getName();
            for (int j = 0; j < existingRowsCount; j++) {
                SQLRowValues row = existingRows.get(j);
                m.put(row.getObject(fieldName), row);
            }
        }

        // Parcours des lignes des données à importer
        for (int i = 0; i < rowCount; i++) {

            // Recherche d'existant
            SQLRowValues existingRow = null;
            for (SQLField field : this.uniqueField) {
                List<Integer> cols = fieldMap.get(field);
                Object objectToInsert = null;
                for (Integer col : cols) {
                    Object v = model.getValueAt(i, col);
                    if (objectToInsert == null) {
                        objectToInsert = v;
                    } else if (v instanceof String) {
                        objectToInsert = objectToInsert.toString() + "\n" + (String) v;
                    }

                }

                existingRow = cache.get(field).get(objectToInsert);
                if (existingRow != null) {
                    break;
                }
            }

            updateOrInsert(model, converters, i, existingRow);
        }

    }

    private void updateOrInsert(ArrayTableModel model, final ValueConverter[] converters, int i, SQLRowValues existingRow) {

        final Map<String, Object> newValues = new HashMap<String, Object>();
        if (existingRow != null) {
            // Préremplissage de la map avec la row existante
            newValues.putAll(existingRow.getAbsolutelyAll());
        }
        for (int j = 0; j < converters.length; j++) {
            ValueConverter valueConverter = converters[j];

            List<Integer> cols = fieldMap.get(valueConverter.getField());
            Object objectToInsert = null;
            for (Integer col : cols) {
                Object v = model.getValueAt(i, col);
                if (objectToInsert == null) {
                    objectToInsert = v;
                } else if (v instanceof String) {
                    objectToInsert = objectToInsert.toString() + "\n" + (String) v;
                }

            }

            final String fieldName = valueConverter.getFieldName();
            if (objectToInsert != null || !valueConverter.isIgnoringEmptyValue()) {
                if (valueConverter.getField().getTable().equals(table)) {
                    newValues.put(fieldName, objectToInsert);
                } else {

                    final SQLField sqlField = foreignMap.get(valueConverter);

                    final Object value = newValues.get(sqlField.getName());
                    if (value == null || value instanceof SQLRowValues) {
                        SQLRowValues fRowValues = (SQLRowValues) value;
                        if (fRowValues == null) {
                            fRowValues = new SQLRowValues(valueConverter.getField().getTable());
                            newValues.put(sqlField.getName(), fRowValues);
                        }
                        fRowValues.put(valueConverter.getField().getName(), objectToInsert);
                    }

                }
            }
        }
        if (existingRow == null) {
            this.valuesToInsert.add(new SQLRowValues(table, newValues));
        } else if (!newValues.equals(existingRow.getAbsolutelyAll())) {
            this.valuesToUpdate.add(new SQLRowValues(table, newValues));
        }
    }

    public void setSkipFirstLine(boolean skipFirstLine) {
        this.skipFirstLine = skipFirstLine;
    }

    public ArrayTableModel createModelFrom(File file) throws IOException {
        if (!file.exists()) {
            throw new IllegalArgumentException(file.getAbsolutePath() + " does not exist");
        }
        String name = file.getName().toLowerCase();
        if (name.endsWith(".ods")) {
            return createModelFromODS(file);
        } else if (name.endsWith(".csv")) {
            return createModelFromCSV(file);
        } else if (name.endsWith(".xls")) {
            return createModelFromXLS(file);
        }
        throw new IllegalArgumentException("File format not supported");

    }
}
