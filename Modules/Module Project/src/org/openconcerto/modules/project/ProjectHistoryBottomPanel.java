package org.openconcerto.modules.project;

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

public class ProjectHistoryBottomPanel extends JPanel {

    private long montantCmd = 0;
    private final JLabel labelCmd = new JLabel("", SwingConstants.LEFT);

    private long montantDevis = 0;
    private final JLabel labelDevis = new JLabel("", SwingConstants.LEFT);

    private double timeCmd = 0;
    private final JLabel labelTimeCmd = new JLabel("", SwingConstants.LEFT);

    private double timeDevis = 0;
    private final JLabel labelTimeDevis = new JLabel("", SwingConstants.LEFT);

    private double time = 0;
    private final JLabel labelTime = new JLabel("", SwingConstants.LEFT);

    public ProjectHistoryBottomPanel() {
        setLayout(new GridLayout(2, 3));
        add(this.labelTimeDevis);
        add(this.labelTimeCmd);
        add(this.labelTime);
        add(this.labelDevis);
        add(this.labelCmd);
    }

    public synchronized void updateDevis(final IListe liste) {
        if (liste.isDead())
            return;
        long montantDevis = 0;
        final int rowCount = liste.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            final SQLRowAccessor sqlRowAccessor = liste.getModel().getRow(i).getRow();
            montantDevis += sqlRowAccessor.getLong("T_HT");
        }
        this.montantDevis = montantDevis;
        updateLabels();
    }

    public synchronized void updateTimeDevis(final IListe liste) {
        if (liste.isDead())
            return;
        final SQLTable tableEltDevis = Configuration.getInstance().getDirectory().getElement("DEVIS_ELEMENT").getTable();
        double timeDevis = 0;
        final int rowCount = liste.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            final SQLRowAccessor sqlRowAccessor = liste.getModel().getRow(i).getRow();
            final Collection<? extends SQLRowAccessor> rows = sqlRowAccessor.getReferentRows(tableEltDevis);
            for (SQLRowAccessor sqlRowAccessor2 : rows) {
                timeDevis += OrderColumnRowRenderer.getHours(sqlRowAccessor2);
            }
        }
        this.timeDevis = timeDevis;
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

    public synchronized void updateTime(final IListe liste) {
        if (liste.isDead())
            return;
        double time = 0;
        final int rowCount = liste.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            final SQLRowAccessor sqlRowAccessor = liste.getModel().getRow(i).getRow();
            time += sqlRowAccessor.getFloat("TEMPS");
        }
        this.time = time;
        updateLabels();
    }

    private void updateLabels() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ProjectHistoryBottomPanel.this.labelDevis.setText("Total devis : " + GestionDevise.currencyToString(ProjectHistoryBottomPanel.this.montantDevis) + "€");
                ProjectHistoryBottomPanel.this.labelTimeDevis.setText("Total temps  : " + ProjectHistoryBottomPanel.this.timeDevis + " h");
                ProjectHistoryBottomPanel.this.labelTimeCmd.setText("Total temps prévu : " + ProjectHistoryBottomPanel.this.timeCmd + " h");
                ProjectHistoryBottomPanel.this.labelCmd.setText("Total commande : " + GestionDevise.currencyToString(ProjectHistoryBottomPanel.this.montantCmd) + "€");
                ProjectHistoryBottomPanel.this.labelTime.setText("Total temps réalisés : " + ProjectHistoryBottomPanel.this.time + " h");
            }
        });

    }
}
