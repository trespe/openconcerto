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

public class ProjectHistoryDevisBottomPanel extends JPanel {

    private long montantDevis = 0;
    private final JLabel labelDevis = new JLabel("", SwingConstants.RIGHT);
    private double timeDevis = 0;
    private final JLabel labelTimeDevis = new JLabel("", SwingConstants.RIGHT);

    public ProjectHistoryDevisBottomPanel() {
        setLayout(new GridLayout(2, 3));
        setOpaque(false);
        this.labelTimeDevis.setFont(this.labelTimeDevis.getFont().deriveFont(Font.BOLD));
        this.labelDevis.setFont(this.labelDevis.getFont().deriveFont(Font.BOLD));
        add(this.labelTimeDevis);
        add(this.labelDevis);
        updateLabels();
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
        double timeDevis = 0;
        final SQLTable tableEltDevis = Configuration.getInstance().getDirectory().getElement("DEVIS_ELEMENT").getTable();
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

    private void updateLabels() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ProjectHistoryDevisBottomPanel.this.labelDevis.setText("Total devis : " + GestionDevise.currencyToString(ProjectHistoryDevisBottomPanel.this.montantDevis) + "€");
                ProjectHistoryDevisBottomPanel.this.labelTimeDevis.setText("Total temps  : " + ProjectHistoryDevisBottomPanel.this.timeDevis + " h");
            }
        });

    }
}
