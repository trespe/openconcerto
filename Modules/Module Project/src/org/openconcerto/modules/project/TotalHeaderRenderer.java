package org.openconcerto.modules.project;

import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.JLabel;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.view.column.ColumnHeaderRenderer;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.GestionDevise;

public class TotalHeaderRenderer extends ColumnHeaderRenderer {

    private JLabel label = new JLabelBold("Total : en cours de calcul");

    public TotalHeaderRenderer() {
        this.setLayout(new FlowLayout(FlowLayout.LEFT));
        this.add(label);
        this.setOpaque(false);

    }

    @Override
    public void setContent(List<? extends SQLRowAccessor> rows) {
        String s = "Total :";
        // Hours
        long totalHT = 0;
        long h = 0;
        for (SQLRowAccessor row : rows) {
            totalHT += row.getBigDecimal("T_PV_HT").setScale(2, BigDecimal.ROUND_HALF_UP).movePointRight(2).longValue();
            h += OrderColumnRowRenderer.getHours(row);
        }

        if (h > 0) {
            s += " " + h + " heures prévues,";
        }

        if (rows.size() > 0) {
            if (rows.get(0).getTable().getDBRoot().contains("AFFAIRE_TEMPS")) {
                // Time spent
                double t = 0;
                for (SQLRowAccessor row : rows) {
                    t += OrderColumnRowRenderer.getTimeSpent(row);
                }
                if (t > 0) {
                    s += " " + hourFormater(t) + " heures passées,";
                }
            }
        }
        s += " " + GestionDevise.currencyToString(totalHT, true) + " € HT";
        this.label.setText(s);
    }

    public static String hourFormater(double h) {
        DecimalFormat f = new DecimalFormat("#0.00");
        String s = f.format(h);
        if (s.endsWith(".00") || s.endsWith(",00")) {
            s = s.substring(0, s.length() - 3);
        }
        return s;
    }
}
