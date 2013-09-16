package org.openconcerto.modules.project;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.column.ColumnRowRenderer;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.VFlowLayout;
import org.openconcerto.utils.GestionDevise;

public class OrderColumnRowRenderer implements ColumnRowRenderer {
    @Override
    public JComponent getRenderer(final SQLRowAccessor row, int maxWidth) {
        final JPanel p = new JPanel();
        p.setLayout(new VFlowLayout(VFlowLayout.TOP, 2, 2, true));
        p.setBorder(BorderFactory.createLineBorder(new Color(206, 226, 255)));
        final String number = row.getForeign("ID_COMMANDE_CLIENT").getString("NUMERO");
        final String customer = row.getForeign("ID_COMMANDE_CLIENT").getForeign("ID_CLIENT").getString("NOM");

        // Amount
        final long amount = row.getBigDecimal("T_PV_HT").setScale(2, BigDecimal.ROUND_HALF_UP).movePointRight(2).longValue();
        String total = GestionDevise.currencyToString(amount, true) + " € HT";
        final long totalAmount = row.getForeign("ID_COMMANDE_CLIENT").getLong("T_HT");
        if (totalAmount != amount) {
            total += ", commande de " + GestionDevise.currencyToString(totalAmount, true) + " € HT";
        }
        // Hours
        int h = getHours(row);

        p.add(new JLabelBold(number + " " + customer));
        p.add(new JLabel(total));
        if (h > 0) {
            p.add(new JLabel(h + " heures prévues"));
        }
        if (row.getTable().getDBRoot().contains("AFFAIRE_TEMPS")) {
            // Time spend
            double time = getTimeSpent(row);

            final JLabel lTime = new JLabel(time + " heures passées");
            if (time > h) {
                lTime.setFont(lTime.getFont().deriveFont(Font.BOLD));
                lTime.setForeground(new Color(255, 135, 30));
            }
            if (time > 0) {
                p.add(lTime);
            }
        }

        p.setBackground(new Color(239, 243, 248));
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    final SQLRowAccessor foreign = row.getForeign("ID_COMMANDE_CLIENT");
                    EditFrame frame = new EditFrame(ComptaPropsConfiguration.getInstanceCompta().getDirectory().getElement(foreign.getTable()), EditPanel.MODIFICATION);
                    frame.selectionId(foreign.getID());
                    FrameUtil.showPacked(frame);
                }
            }
        });

        return p;
    }

    public static double getTimeSpent(SQLRowAccessor row) {
        double time = 0;
        final Collection<? extends SQLRowAccessor> rTimes = row.getReferentRows(row.getTable().getTable("AFFAIRE_TEMPS"));
        for (SQLRowAccessor r : rTimes) {
            time += r.getFloat("TEMPS");

        }
        return time;
    }

    public static int getHours(SQLRowAccessor row) {
        int h = 0;
        final String code = row.getForeign("ID_UNITE_VENTE").getString("CODE");
        if (code.equals("h")) {
            h += row.getLong("QTE_UNITAIRE") * row.getLong("QTE");
        } else if (code.equals("j")) {
            h += row.getLong("QTE_UNITAIRE") * row.getLong("QTE") * 8;
        }
        return h;
    }
}
