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
 
 package org.openconcerto.erp.config;

import org.openconcerto.erp.action.NouvelleConnexionAction;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.sales.quote.element.DevisSQLElement;
import org.openconcerto.erp.core.sales.quote.ui.DevisItemTable;
import org.openconcerto.erp.modules.ModuleManager;
import org.openconcerto.erp.panel.PostgreSQLFrame;
import org.openconcerto.erp.panel.UserExitPanel;
import org.openconcerto.ftp.updater.UpdateManager;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.State;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRequestLog;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.view.ListeModifyPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.component.WaitIndeterminatePanel;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.protocol.Helper;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.SplashScreen;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

public class Gestion {

    public static final File MODULES_DIR = new File("Modules");

    /**
     * When this system property is set to <code>true</code>, Gestion will hide most of its normal
     * UI. E.g. no SOCIETE selection in the login panel, minimalist menu bar, etc.
     */
    public static final String MINIMAL_PROP = "org.openconcerto.erp.minimal";

    private static List<Image> frameIcon;
    // Check that we are on Mac OS X. This is crucial to loading and using the OSXAdapter class.
    static final boolean MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");

    // only available from sun's release 6u10
    private static String getNimbusClassName() {
        // http://java.sun.com/javase/6/docs/technotes/guides/jweb/otherFeatures/nimbus_laf.html
        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                return info.getClassName();
            }
        }
        return null;
    }

    static boolean inWebStart() {
        // cannot rely on system properties since they vary from one implementation to another
        try {
            // ATTN on OpenJDK jnlp classes are in rt.jar, so this doesn't throw
            // ClassNotFoundException, we have to check the result
            final String[] names = (String[]) Class.forName("javax.jnlp.ServiceManager").getMethod("getServiceNames").invoke(null);
            return names != null && names.length > 0;
        } catch (Throwable e) {
            return false;
        }
    }

    public static void openPDF(File f) {
        try {
            FileUtils.openFile(f);
        } catch (IOException e) {
            ExceptionHandler
                    .handle("Impossible d'ouvrir le fichier " + f + ".\nVérifiez qu'un logiciel pour lire les fichiers PDF est installé sur votre ordinateur (http://get.adobe.com/fr/reader).");
        }
    }

    public static final boolean isMinimalMode() {
        return Boolean.getBoolean(MINIMAL_PROP);
    }

    /**
     * @param args
     */
    // -Dorg.openconcerto.sql.sqlCombo.selectSoleItem=true
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                ExceptionHandler.handle("UncaughtException on thread " + t, e);

            }
        });
        System.out.println(System.getProperty("java.vendor", "??") + " - " + System.getProperty("java.version", "??"));
        System.out.println(System.getProperty("java.runtime.version", "??") + " - " + System.getProperty("os.name", "??"));
        ExceptionHandler.setForceUI(true);
        ExceptionHandler.setForumURL("http://www.openconcerto.org/forum");
        final boolean logRequests = Boolean.getBoolean("org.openconcerto.sql.logRequests");
        if (logRequests) {
            SQLRequestLog.setEnabled(true);
        }
        System.setProperty(PropsConfiguration.REDIRECT_TO_FILE, "true");
        // Mac
        // only works with Aqua laf
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        ToolTipManager.sharedInstance().setInitialDelay(0);
        // SpeedUp Linux
        System.setProperty("sun.java2d.pmoffscreen", "false");

        System.setProperty("org.openconcerto.editpanel.noborder", "true");
        System.setProperty("org.openconcerto.editpanel.noborder", "true");
        System.setProperty("org.openconcerto.sql.editpanel.endAdd", "true");
        System.setProperty("org.openconcerto.sql.listPanel.deafEditPanel", "true");
        System.setProperty("org.openconcerto.ui.addComboButton", "true");
        System.setProperty("org.openconcerto.sql.structure.useXML", "true");
        // don't put any suffix, rely on Animator
        System.setProperty(UISQLComponent.REQUIRED_SUFFIX_PROP, "");
        System.setProperty(ElementComboBox.CAN_MODIFY, "true");
        System.setProperty("org.openconcerto.ui.removeSwapSearchCheckBox", "true");

        if (System.getProperty("org.openconcerto.oo.useODSViewer") == null) {
            System.setProperty("org.openconcerto.oo.useODSViewer", "true");
        }
        if (System.getProperty(State.DEAF) == null) {
            System.setProperty(State.DEAF, "true");
        }
        IListe.setForceAlternateCellRenderer(true);
        ITableModel.setDefaultEditable(false);

        // Initialisation du splashScreen
        // ne pas oublier en param -splash:image.png
        SplashScreen.getSplashScreen();
        try {

            final String nimbusClassName = getNimbusClassName();
            if (nimbusClassName == null || !System.getProperty("os.name", "??").toLowerCase().contains("linux")) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } else {
                UIManager.setLookAndFeel(nimbusClassName);
                UIManager.put("control", new Color(240, 240, 240));
                UIManager.put("Table.showGrid", Boolean.TRUE);
                UIManager.put("FormattedTextField.background", new Color(240, 240, 240));
                UIManager.put("Table.alternateRowColor", Color.WHITE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        UpdateManager.start();

        Toolkit.getDefaultToolkit().setDynamicLayout(true);

        ComboSQLRequest.setDefaultFieldSeparator(" ");

        // Init des caches
        long t1 = System.currentTimeMillis();
        final ComptaPropsConfiguration conf = ComptaPropsConfiguration.create(true);
        if (conf == null) {
            ServerFinderPanel.main(new String[0]);
            return;
        }
        if (inWebStart()) {
            // needed since our classes aren't loaded by the same loader as the library classes
            Helper.setURLStreamHandlerFactory();
            // even if we set <all-permissions/> in the jnlp, this only applies to the main jar, not
            // to dynamically loaded jars. So to allow OOConnexion to work we need to remove the
            // security manager.
            System.setSecurityManager(null);
        }
        Configuration.setInstance(conf);
        long t4 = System.currentTimeMillis();
        System.out.println("Ip:" + conf.getServerIp());
        if (conf.getServerIp().startsWith("127.0.0.1:6543")) {
            File f = new File("PostgreSQL/data/postmaster.pid");
            if (!f.exists()) {
                startDB(conf);
            }
        }
        try {
            conf.getBase();
        } catch (Exception e) {
            System.out.println("Init phase 1 error:" + (System.currentTimeMillis() - t4) + "ms");
            ExceptionHandler.die("Erreur de connexion à la base de données", e);
        }
        System.out.println("Init phase 1:" + (System.currentTimeMillis() - t1) + "ms");
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                long t1 = System.currentTimeMillis();

                AWTEventListener awtListener = new AWTEventListener() {
                    @Override
                    public final void eventDispatched(final AWTEvent event) {
                        assert event != null;
                        if (event instanceof HierarchyEvent && event.getID() == HierarchyEvent.HIERARCHY_CHANGED) {
                            // This event represents a change in the containment hierarcy.
                            // Now let's figure out what kind.
                            final HierarchyEvent hevent = (HierarchyEvent) event;
                            final Component changed = hevent.getChanged();
                            if (changed instanceof JFrame && ((hevent.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) && changed.isDisplayable()) {
                                JFrame frame = (JFrame) changed;
                                frame.setIconImages(getFrameIcon());
                            }
                        }
                    }
                };

                final Toolkit toolkit = Toolkit.getDefaultToolkit();
                assert toolkit != null;
                toolkit.addAWTEventListener(awtListener, AWTEvent.HIERARCHY_EVENT_MASK);
                if (logRequests) {
                    SQLRequestLog.showFrame();
                }

                JFrame f = null;
                try {
                    f = new NouvelleConnexionAction().createFrame();
                    // happens with quick login
                    if (f != null) {
                        f.pack();
                        f.setResizable(false);
                        f.addWindowListener(new WindowAdapter() {
                            public void windowClosing(WindowEvent e) {
                                askForExit();
                            };
                        });
                        // f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    }
                } catch (Exception e) {
                    Thread.dumpStack();
                    e.printStackTrace();
                    ExceptionHandler.handle("Erreur lors de la tentative de connexion à la base.", e);

                } finally {
                    if (f != null) {
                        FrameUtil.show(f);
                    }
                    System.out.println("Init phase 2:" + (System.currentTimeMillis() - t1) + "ms");
                }
            }
        });

        // needed so that we can uninstall modules
        System.setProperty(SQLBase.ALLOW_OBJECT_REMOVAL, "true");
        try {
            ModuleManager.getInstance().addFactories(MODULES_DIR);
        } catch (Throwable e) {
            ExceptionHandler.handle("Erreur d'accès aux modules", e);
        }
    }

    /**
     * Si la base est 127.0.0.1 ou localhost alors on essaye de lancer postgres.
     * 
     * @param conf the configuration to connect to.
     */
    private static void startDB(PropsConfiguration conf) {

        List<String> l = new ArrayList<String>();
        l.add("Lancement de la base de données");
        PostgreSQLFrame pgFrame = null;
        try {
            pgFrame = new PostgreSQLFrame("Démarrage en cours");
            pgFrame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.err.println("start DB");
        final WaitIndeterminatePanel panel = new WaitIndeterminatePanel(l);
        final PanelFrame f = new PanelFrame(panel, "Gestion NX");
        f.setResizable(false);
        f.setLocationRelativeTo(null);
        f.setVisible(true);

        // on recupere les properties
        final String serverAdr = conf.getProperty("server.ip");
        // si la base est en local on relance postgres
        if (serverAdr.startsWith("127.0.0.1") || serverAdr.startsWith("localhost")) {

            Runtime runtime = Runtime.getRuntime();
            try {
                File file = new File(".\\PostgreSQL\\bin\\");
                if (!file.canWrite()) {
                    ExceptionHandler.die("Vous n'avez pas le droit en écriture sur la base de données.\nImpossible de lancer le logiciel!");
                }
                final Process p = runtime.exec(new String[] { "cmd.exe", "/C", "launchPostGres.bat" }, null, file);
                // Consommation de la sortie standard de l'application externe dans un Thread
                // separe
                new Thread() {
                    public void run() {
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                            String line = "";
                            try {
                                while ((line = reader.readLine()) != null) {
                                    System.out.println(line);
                                }
                            } finally {
                                reader.close();
                            }
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                }.start();

                // Consommation de la sortie d'erreur de l'application externe dans un Thread
                // separe
                new Thread() {
                    public void run() {
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                            String line = "";
                            try {
                                while ((line = reader.readLine()) != null) {
                                    System.err.println(line);
                                }
                            } finally {
                                reader.close();
                            }
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                }.start();
                try {
                    p.waitFor();
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else {
            System.err.println("L'adresse du serveur n'est pas en local (" + serverAdr + ").");
        }

        panel.taskEnded(0);
        if (pgFrame != null) {
            pgFrame.dispose();
        }
        f.dispose();
        String realIp = "127.0.0.1";
        realIp = getIp();
        try {
            pgFrameStart = new PostgreSQLFrame(realIp + " port " + "6543");
            pgFrameStart.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static String getIp() {
        String realIp = "127.0.0.1";
        try {
            InetAddress Ip = InetAddress.getLocalHost();
            realIp = Ip.getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) e.nextElement();
                Enumeration e2 = ni.getInetAddresses();
                while (e2.hasMoreElements()) {
                    InetAddress ip = (InetAddress) e2.nextElement();
                    final String iip = ip.toString().replace('/', ' ').trim();
                    if (iip.startsWith("192")) {
                        return iip;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return realIp;
    }

    public static PostgreSQLFrame pgFrameStart = null;

    private static void testModifyRowValuesTable() {

        JFrame f = new JFrame();
        f.getContentPane().setLayout(new GridLayout(1, 1));

        f.getContentPane().add(new ListeModifyPanel(new DevisSQLElement()));
        f.pack();
        f.setSize(800, 600);

        // f.setResizable(false);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    }

    private static void testRowValuesTable() {

        JFrame f = new JFrame();
        f.getContentPane().setLayout(new GridLayout(1, 1));

        f.getContentPane().add(new DevisItemTable());
        f.pack();
        f.setSize(800, 600);

        // f.setResizable(false);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    }

    private static JDialog frameExit = null;

    static void askForExit() {
        JDialog exitDialog = new JDialog();
        exitDialog.setModal(true);

        if (frameExit == null) {
            frameExit = new JDialog();
            frameExit.setContentPane(new UserExitPanel());
            frameExit.setTitle("Quitter");
            frameExit.setModal(true);
            frameExit.setIconImages(Gestion.getFrameIcon());
            frameExit.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        }

        frameExit.pack();
        frameExit.setResizable(false);
        frameExit.setLocationRelativeTo(null);
        frameExit.setAlwaysOnTop(true);
        frameExit.setVisible(true);
    }

    public static List<Image> getFrameIcon() {
        if (frameIcon == null) {
            frameIcon = new ArrayList<Image>();
            int[] sizes = { 16, 32, 48, 96 };
            for (int i = 0; i < sizes.length; i++) {
                int v = sizes[i];
                try {
                    frameIcon.add(new ImageIcon(Gestion.class.getResource(v + ".png")).getImage());
                } catch (Exception e) {
                    ExceptionHandler.die("Impossible de charger l'icone de fenetre " + v + ".png");
                }
            }
        }
        return frameIcon;
    }
}
