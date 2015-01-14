package org.jopenchart;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataModel2D extends DataModel {

    private String[][] data;
    private int rowCount;
    private int colCount;
    private List<String> rowLabels = new ArrayList<String>();
    private List<String> colLabels = new ArrayList<String>();

    public DataModel2D(int row, int col) {
        this.rowCount = row;
        this.colCount = col;
        data = new String[row][col];
        Random r = new Random(row * col);
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                data[i][j] = String.valueOf(r.nextInt(1000));
            }
        }

        for (int i = 0; i < row; i++) {
            this.rowLabels.add(String.valueOf((char) ('A' + i)));
        }
        for (int i = 0; i < col; i++) {
            this.colLabels.add(String.valueOf(((1 + i))));
        }

    }

    public String getValue(int row, int col) {
        return data[row][col];
    }

    public void setValue(String value, int row, int col) {
        data[row][col] = value;
    }

    public String getColumnLabel(int col) {
        return this.colLabels.get(col);
    }

    public String getRowLabel(int row) {
        return this.rowLabels.get(row);
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getColumnCount() {
        return colCount;
    }

}
