package org.openconcerto.modules.project;

import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.view.list.IListe;

public class ProjectHistoryTimeBottomPanel extends JPanel {

    private double time = 0;
    private final JLabel labelTime = new JLabel("", SwingConstants.RIGHT);

    public ProjectHistoryTimeBottomPanel() {
        setOpaque(false);
        setLayout(new GridLayout(2, 1));
        this.labelTime.setFont(this.labelTime.getFont().deriveFont(Font.BOLD));
        add(this.labelTime);
        updateLabels();
    }

    public synchronized void updateTime(final IListe liste) {
        if (liste.isDead())
            return;
        double time = 0;
        final int rowCount = liste.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            SQLRowAccessor sqlRowAccessor = liste.getModel().getRow(i).getRow();
            time += sqlRowAccessor.getFloat("TEMPS");
        }
        this.time = time;
        updateLabels();
    }

    private void updateLabels() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ProjectHistoryTimeBottomPanel.this.labelTime.setText("Total temps réalisés : " + ProjectHistoryTimeBottomPanel.this.time + " h");
            }
        });

    }
}
