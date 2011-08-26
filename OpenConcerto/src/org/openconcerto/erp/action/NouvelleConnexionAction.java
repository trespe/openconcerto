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

import static org.openconcerto.task.config.ComptaBasePropsConfiguration.getStreamStatic;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.MainFrame;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.common.ui.StatusPanel;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.humanresources.payroll.element.CaisseCotisationSQLElement;
import org.openconcerto.erp.element.objet.ClasseCompte;
import org.openconcerto.erp.panel.ComptaTipsFrame;
import org.openconcerto.erp.utils.NXDatabaseAccessor;
import org.openconcerto.map.model.Ville;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.UndefinedRowValuesCache;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.UserProps;
import org.openconcerto.sql.ui.ConnexionPanel;
import org.openconcerto.sql.users.User;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.users.rights.TableAllRights;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.sql.users.rights.UserRightsManager.RightTuple;
import org.openconcerto.task.config.ComptaBasePropsConfiguration;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.state.WindowStateManager;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.JImage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

public class NouvelleConnexionAction extends CreateFrameAbstractAction {
    public NouvelleConnexionAction() {
        super();
        this.putValue(Action.NAME, "Changer d'utilisateur");
    }

    private ConnexionPanel connexionPanel;

    public JFrame createFrame() {
        // needed as done() must come after us
        assert SwingUtilities.isEventDispatchThread();
        final ComptaPropsConfiguration comptaPropsConfiguration = ((ComptaPropsConfiguration) Configuration.getInstance());

        // Vérification de la licence

        Runnable r = new Runnable() {

            public void run() {
                try {
                    final Boolean booleanValue = UserProps.getInstance().getBooleanValue("HideTips");
                    if (!booleanValue) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                ComptaTipsFrame.getFrame(true).setVisible(true);
                            }
                        });
                    }
                    final int selectedSociete = NouvelleConnexionAction.this.connexionPanel == null ? UserProps.getInstance().getLastSocieteID() : NouvelleConnexionAction.this.connexionPanel
                            .getSelectedSociete();
                    comptaPropsConfiguration.setUpSocieteDataBaseConnexion(selectedSociete);


                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            // even for quick login, check the license before displaying the main
                            // frame

                            StatusPanel.getInstance().fireStatusChanged();
                            final JFrame f = new MainFrame();
                            final File f2 = new File(Configuration.getInstance().getConfDir(), "Configuration" + File.separator + "Frame" + File.separator + "mainFrame.xml");
                            WindowStateManager manager = new WindowStateManager(f, f2);
                            String version = comptaPropsConfiguration.getVersion();
                            f.setTitle(comptaPropsConfiguration.getAppName() + " " + version + ", " + " [Société " + comptaPropsConfiguration.getRowSociete().getString("NOM") + "]");
                            f.setMinimumSize(new Dimension(800, 600));
                            // f.setResizable(false);
                            f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                            manager.loadState();

                            FrameUtil.show(f);
                        }
                    });
                    initCache();
                } catch (Throwable e) {
                    ExceptionHandler.handle("Erreur de connexion", e);
                }
            }

            private void fixEcriture() {
                // FIXME Bug archive ecriture (ecriture non archivé ayant un id_mouvement=1)
                SQLElement elt = Configuration.getInstance().getDirectory().getElement("ECRITURE");
                SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());
                sel.addSelect(elt.getTable().getKey());

                Where w = new Where(elt.getTable().getField("ID_MOUVEMENT"), "=", 1);
                sel.setWhere(w);
                System.err.println(sel.asString());
                List<SQLRow> lerrors = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), new SQLRowListRSH(elt.getTable()));
                for (SQLRow row : lerrors) {
                    System.err.println("FIX ERROR ID_MOUVEMENT ON ECRITURE ROW " + row.getID());
                    SQLRowValues rowVals = row.createEmptyUpdateRow();
                    rowVals.put("ARCHIVE", 1);
                    try {
                        rowVals.update();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }

                if (lerrors.size() > 0) {
                    System.err.println(lerrors.size() + " erreurs");
                    Thread.dumpStack();
                    // JOptionPane.showMessageDialog(null, lerrors.size() +
                    // " erreurs ont été trouvé et corrigé dans la base.");
                }
            }
        };

        final JImage image = new JImage(ComptaBasePropsConfiguration.class.getResource("logo.png"));
        image.setBackground(Color.WHITE);
        JPanel p = new JPanel();

        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.BOTH;

        this.connexionPanel = ConnexionPanel.create(r, image, true);
        if (this.connexionPanel == null)
            return null;

        p.add(this.connexionPanel, c);
        final PanelFrame panelFrame = new PanelFrame(p, "Connexion");
        panelFrame.setLocationRelativeTo(null);
        panelFrame.pack();
        panelFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        return panelFrame;
    }

    private void initCache() {
        Thread t = new Thread() {
            @Override
            public void run() {
                // laisse le temps au logiciel de demarrer
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ClasseCompte.loadClasseCompte();
                CaisseCotisationSQLElement.getCaisseCotisation();

                Ville.init(new NXDatabaseAccessor());
                final SQLBase baseSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
                SQLBackgroundTableCache.getInstance().add(baseSociete.getTable("TAXE"), 600);
                SQLBackgroundTableCache.getInstance().add(baseSociete.getTable("PREFS_COMPTE"), 600);
                SQLBackgroundTableCache.getInstance().add(baseSociete.getTable("JOURNAL"), 600);

                SQLBackgroundTableCache.getInstance().add(baseSociete.getTable("COMMERCIAL"), 600);

                SQLBackgroundTableCache.getInstance().add(baseSociete.getTable("TYPE_REGLEMENT"), 1000);
                TaxeCache.getCache();

                final UndefinedRowValuesCache UndefCache = UndefinedRowValuesCache.getInstance();

                UndefCache.getDefaultRowValues(baseSociete.getTable("ADRESSE"));
                UndefCache.getDefaultRowValues(baseSociete.getTable("DEVIS_ELEMENT"));
                UndefCache.getDefaultRowValues(baseSociete.getTable("CONTACT"));
                UndefCache.getDefaultRowValues(baseSociete.getTable("SAISIE_VENTE_FACTURE_ELEMENT"));
                UndefCache.getDefaultRowValues(baseSociete.getTable("SAISIE_KM_ELEMENT"));
                UndefCache.getDefaultRowValues(baseSociete.getTable("BON_DE_LIVRAISON_ELEMENT"));
                UndefCache.getDefaultRowValues(baseSociete.getTable("COMMANDE_CLIENT_ELEMENT"));
                UndefCache.getDefaultRowValues(baseSociete.getTable("AVOIR_CLIENT_ELEMENT"));
                UndefCache.getDefaultRowValues(baseSociete.getTable("BON_RECEPTION_ELEMENT"));

            }

        };
        t.setName("Cache preload");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private String checkLicense(final SwingWorker<String, Object> w) {
        try {
            return w.get();
        } catch (Exception e) {
            throw ExceptionHandler.die("Impossible de charger la licence.", e);
        }
    }

}
