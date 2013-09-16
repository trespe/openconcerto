/*
 * Créé le 3 juin 2012
 */
package org.openconcerto.modules.project;

import java.awt.GridBagConstraints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.reports.history.ui.ListeHistoriquePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.view.IListPanel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.PanelFrame;
import org.openconcerto.ui.state.WindowStateManager;

public class ProjectHistory {
    private PanelFrame panelFrame;
    private ListeHistoriquePanel listPanel;

    public ListeHistoriquePanel getHistoriquePanel() {
        return this.listPanel;
    }

    public ProjectHistory() {

        final ComptaPropsConfiguration comptaPropsConfiguration = ((ComptaPropsConfiguration) Configuration.getInstance());
        final SQLBase b = comptaPropsConfiguration.getSQLBaseSociete();

        final Map<String, List<String>> mapList = new LinkedHashMap<String, List<String>>();
        mapList.put("Devis", Arrays.asList("DEVIS"));
        mapList.put("Bons de commande", Arrays.asList("COMMANDE_CLIENT"));
        mapList.put("Factures", Arrays.asList("SAISIE_VENTE_FACTURE"));
        mapList.put("Avoirs", Arrays.asList("AVOIR_CLIENT"));

        if (Configuration.getInstance().getRoot().findTable("AFFAIRE_TEMPS") != null) {
            mapList.put("Temps", Arrays.asList("AFFAIRE_TEMPS"));
        }

        final ComboSQLRequest request = new org.openconcerto.sql.request.ComboSQLRequest(b.getTable("AFFAIRE"), Arrays.asList("NUMERO", "ID_CLIENT"));
        request.setUndefLabel("Toutes les affaires");
        request.setFieldSeparator(" ");

        this.listPanel = new ListeHistoriquePanel("Affaires", request, mapList, null, null, "Toutes les affaires", true, null);

        final IListPanel listeDevis = listPanel.getIListePanelFromTableName("DEVIS");
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridy = 4;
        c.fill = GridBagConstraints.BOTH;
        final ProjectHistoryDevisBottomPanel devisPanel = new ProjectHistoryDevisBottomPanel();
        listeDevis.add(devisPanel, c);
        listeDevis.getListe().getTableModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                devisPanel.updateDevis(listeDevis.getListe());
                devisPanel.updateTimeDevis(listeDevis.getListe());
            }
        });

        final IListPanel listeCmd = listPanel.getIListePanelFromTableName("COMMANDE_CLIENT");
        final ProjectHistoryCmdBottomPanel cmdPanel = new ProjectHistoryCmdBottomPanel();

        listeCmd.add(cmdPanel, c);
        listeCmd.getListe().getTableModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                cmdPanel.updateCmd(listeCmd.getListe());
                cmdPanel.updateTimeCmd(listeCmd.getListe());
            }
        });

        final IListPanel listeTemps = listPanel.getIListePanelFromTableName("AFFAIRE_TEMPS");
        final ProjectHistoryTimeBottomPanel timePanel = new ProjectHistoryTimeBottomPanel();
        listeTemps.add(timePanel, c);
        listeTemps.getListe().getTableModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                timePanel.updateTime(listeTemps.getListe());
            }
        });

        this.panelFrame = new PanelFrame(this.listPanel, "Historique affaires");
        this.panelFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                listPanel.removeAllTableListener();
            };
        });

        this.panelFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    }

    public PanelFrame getFrame() {
        this.panelFrame.setIconImages(Gestion.getFrameIcon());
        final WindowStateManager stateManager = new WindowStateManager(this.panelFrame, new File(Configuration.getInstance().getConfDir(), "Configuration" + File.separator + "Frame" + File.separator
                + "HistoAffaires.xml"), true);
        this.panelFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.panelFrame.pack();
        this.panelFrame.setLocationRelativeTo(null);
        stateManager.loadState();
        return this.panelFrame;
    }
}
