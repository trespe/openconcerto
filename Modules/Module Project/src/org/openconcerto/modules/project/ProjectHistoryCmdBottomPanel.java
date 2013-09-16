package org.openconcerto.modules.project;

import java.awt.Font;
import java.awt.GridLayout;
import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.utils.GestionDevise;

public class ProjectHistoryCmdBottomPanel extends JPanel {

    private long montantCmd = 0;
    private final JLabel labelCmd = new JLabel("", SwingConstants.RIGHT);

    private double timeCmd = 0;
    private final JLabel labelTimeCmd = new JLabel("", SwingConstants.RIGHT);

    public ProjectHistoryCmdBottomPanel() {
        setLayout(new GridLayout(2, 1));
        setOpaque(false);
        add(this.labelTimeCmd);
        this.labelTimeCmd.setFont(this.labelTimeCmd.getFont().deriveFont(Font.BOLD));
        this.labelCmd.setFont(this.labelCmd.getFont().deriveFont(Font.BOLD));
        add(this.labelCmd);
        updateLabels();
    }

    public synchronized void updateTimeCmd(final IListe liste) {
        if (liste.isDead())
            return;
        double timeCmd = 0;
        final SQLTable tableEltCmd = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT_ELEMENT").getTable();
        final int rowCount = liste.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            final SQLRowAccessor sqlRowAccessor = liste.getModel().getRow(i).getRow();
            final Collection<? extends SQLRowAccessor> rows = sqlRowAccessor.getReferentRows(tableEltCmd);
            for (SQLRowAccessor sqlRowAccessor2 : rows) {
                timeCmd += OrderColumnRowRenderer.getHours(sqlRowAccessor2);
            }
        }
        this.timeCmd = timeCmd;
        updateLabels();
    }

    public synchronized void updateCmd(final IListe liste) {
        if (liste.isDead())
            return;
        long montantCmd = 0;
        final int rowCount = liste.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            final SQLRowAccessor sqlRowAccessor = liste.getModel().getRow(i).getRow();
            montantCmd += sqlRowAccessor.getLong("T_HT");
        }
        this.montantCmd = montantCmd;
        updateLabels();
    }

    private void updateLabels() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ProjectHistoryCmdBottomPanel.this.labelTimeCmd.setText("Total temps prévu : " + ProjectHistoryCmdBottomPanel.this.timeCmd + " h");
                ProjectHistoryCmdBottomPanel.this.labelCmd.setText("Total commande : " + GestionDevise.currencyToString(ProjectHistoryCmdBottomPanel.this.montantCmd) + "€");
            }
        });

    }
}
