package org.openconcerto.modules.project;

import java.math.BigDecimal;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.view.column.ColumnFooterRenderer;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.VFlowLayout;
import org.openconcerto.utils.GestionDevise;

public class QuoteColumnFooterRenderer implements ColumnFooterRenderer {

    @Override
    public JComponent getRenderer(List<? extends SQLRowAccessor> rows, int maxWidth) {
        final JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new VFlowLayout());
        long totalHT = 0;
        long h = 0;
        for (SQLRowAccessor row : rows) {
            totalHT += row.getBigDecimal("T_PV_HT").setScale(2, BigDecimal.ROUND_HALF_UP).movePointRight(2).longValue();
            h += QuoteColumnRowRenderer.getHours(row);
        }
        if (h > 0) {
            p.add(new JLabelBold(h + " heures"));
        }
        p.add(new JLabelBold("Total : " + GestionDevise.currencyToString(totalHT, true) + " € HT"));
        return p;
    }
}
