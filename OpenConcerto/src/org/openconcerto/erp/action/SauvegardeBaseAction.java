/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.erp.action;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.customerrelationship.customer.report.FicheClientXmlSheet;
import org.openconcerto.erp.core.finance.accounting.report.BalanceSheet;
import org.openconcerto.erp.core.finance.accounting.report.GrandLivreSheet;
import org.openconcerto.erp.core.finance.accounting.report.JournauxSheet;
import org.openconcerto.erp.core.humanresources.payroll.report.EtatChargesPayeSheet;
import org.openconcerto.erp.core.humanresources.payroll.report.FichePayeSheet;
import org.openconcerto.erp.core.humanresources.payroll.report.LivrePayeSheet;
import org.openconcerto.erp.core.sales.invoice.report.ListeFactureXmlSheet;
import org.openconcerto.erp.core.sales.invoice.report.ListeVenteXmlSheet;
import org.openconcerto.erp.core.sales.invoice.report.VenteFactureXmlSheet;
import org.openconcerto.erp.core.sales.order.report.CommandeClientXmlSheet;
import org.openconcerto.erp.core.sales.quote.report.DevisXmlSheet;
import org.openconcerto.erp.core.sales.shipment.report.BonLivraisonXmlSheet;
import org.openconcerto.erp.generationDoc.SheetXml;
import org.openconcerto.erp.generationDoc.gestcomm.AvoirClientXmlSheet;
import org.openconcerto.erp.generationDoc.gestcomm.AvoirFournisseurXmlSheet;
import org.openconcerto.erp.generationDoc.gestcomm.CommandeXmlSheet;
import org.openconcerto.erp.generationDoc.gestcomm.EtatVentesXmlSheet;
import org.openconcerto.erp.generationDoc.gestcomm.FicheRelanceSheet;
import org.openconcerto.erp.generationDoc.gestcomm.ReleveChequeEmisSheet;
import org.openconcerto.erp.generationDoc.gestcomm.ReleveChequeSheet;
import org.openconcerto.erp.preferences.BackupNXProps;
import org.openconcerto.erp.preferences.TemplateNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.utils.BackupPanel;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.swing.Action;
import javax.swing.JFrame;

public class SauvegardeBaseAction extends CreateFrameAbstractAction {

    public SauvegardeBaseAction() {
        super();
        this.putValue(Action.NAME, "Sauvegarde de la base");
    }

    @Override
    public JFrame createFrame() {
        final JFrame frame = new JFrame();
        final List<File> dirs = new ArrayList<File>();
        TemplateNXProps nxprops = (TemplateNXProps) TemplateNXProps.getInstance();
        final String defaultLocation = nxprops.getDefaultStringValue();

        final HashSet<String> locations = new HashSet<String>();
        final File defaultLocationFile = new File(defaultLocation);
        if (defaultLocationFile.exists()) {
            dirs.add(defaultLocationFile);
        }
        final String serverIp = ((ComptaPropsConfiguration) Configuration.getInstance()).getServerIp();
        if (serverIp != null && serverIp.startsWith("file:")) {
            locations.add(serverIp.substring(5));
        }

        locations.add(defaultLocation);
        locations.add(SheetXml.getLocationForTuple(DevisXmlSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(DevisXmlSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(VenteFactureXmlSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(VenteFactureXmlSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(CommandeClientXmlSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(CommandeClientXmlSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(BonLivraisonXmlSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(BonLivraisonXmlSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(AvoirClientXmlSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(AvoirClientXmlSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(AvoirFournisseurXmlSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(AvoirFournisseurXmlSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(CommandeXmlSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(CommandeXmlSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(EtatVentesXmlSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(EtatVentesXmlSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(FicheClientXmlSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(FicheClientXmlSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(FicheRelanceSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(FicheRelanceSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(ReleveChequeSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(ReleveChequeSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(ReleveChequeEmisSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(ReleveChequeEmisSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(ListeFactureXmlSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(ListeFactureXmlSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(ListeVenteXmlSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(ListeVenteXmlSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(EtatChargesPayeSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(EtatChargesPayeSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(FichePayeSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(FichePayeSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(LivrePayeSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(LivrePayeSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(BalanceSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(BalanceSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(GrandLivreSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(GrandLivreSheet.getTuple2Location(), false));
        locations.add(SheetXml.getLocationForTuple(JournauxSheet.getTuple2Location(), true));
        locations.add(SheetXml.getLocationForTuple(JournauxSheet.getTuple2Location(), false));

        for (String string : locations) {

            if (!string.startsWith(defaultLocation)) {
                final File f = new File(string);
                if (f.exists()) {
                    System.out.println("Directory to backup:" + string);
                    dirs.add(f);
                } else {
                    System.out.println(string + " not found");
                }
            } else {
                System.out.println(string + " already in backup path");
            }
        }

        frame.setContentPane(new BackupPanel(Arrays.asList("Common", ((ComptaPropsConfiguration) Configuration.getInstance()).getSocieteBaseName()), dirs, false, BackupNXProps.getInstance()));
        frame.setTitle("Sauvegarde des données");
        // so that the application can exit
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        frame.setLocationRelativeTo(null);
        frame.pack();
        frame.setMinimumSize(frame.getSize());

        frame.setIconImages(Gestion.getFrameIcon());
        frame.setAlwaysOnTop(true);
        frame.setVisible(true);
        return frame;
    }
}
