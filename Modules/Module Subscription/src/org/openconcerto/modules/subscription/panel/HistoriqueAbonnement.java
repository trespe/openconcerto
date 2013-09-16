/*
 * Créé le 3 juin 2012
 */
package org.openconcerto.modules.subscription.panel;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.reports.history.ui.ListeHistoriquePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.ui.PanelFrame;
import org.openconcerto.ui.state.WindowStateManager;

public class HistoriqueAbonnement {
    private PanelFrame panelFrame;
    private ListeHistoriquePanel listPanel;

    public ListeHistoriquePanel getHistoriquePanel() {
        return this.listPanel;
    }

    public HistoriqueAbonnement() {

        final ComptaPropsConfiguration comptaPropsConfiguration = ((ComptaPropsConfiguration) Configuration.getInstance());
        final SQLBase b = comptaPropsConfiguration.getSQLBaseSociete();

        Map<String, List<String>> mapList = new LinkedHashMap<String, List<String>>();
        mapList.put("Devis", Arrays.asList("DEVIS"));
        mapList.put("Bons de commande", Arrays.asList("COMMANDE_CLIENT"));
        mapList.put("Factures", Arrays.asList("SAISIE_VENTE_FACTURE"));

        ComboSQLRequest request = new org.openconcerto.sql.request.ComboSQLRequest(b.getTable("ABONNEMENT"), Arrays.asList("NUMERO", "ID_CLIENT"));
        request.setFieldSeparator(" ");

        this.listPanel = new ListeHistoriquePanel("Abonnements", request, mapList, null, null, null, true, null);

        this.panelFrame = new PanelFrame(this.listPanel, "Historique abonnements");
        this.panelFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                listPanel.removeAllTableListener();
            };
        });

        this.panelFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    }

    public PanelFrame getFrame() {

        this.panelFrame.setIconImages(Gestion.getFrameIcon());

        WindowStateManager stateManager = new WindowStateManager(this.panelFrame, new File(Configuration.getInstance().getConfDir(), "Configuration" + File.separator + "Frame" + File.separator
                + "HistoAbonnements.xml"), true);

        this.panelFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.panelFrame.pack();

        this.panelFrame.setLocationRelativeTo(null);

        stateManager.loadState();

        return this.panelFrame;
    }
}
