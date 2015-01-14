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

import org.openconcerto.erp.core.sales.pos.ui.ConfigCaissePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLSchema;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.VFlowLayout;
import org.openconcerto.utils.DesktopEnvironment;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.PropertiesUtils;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ServerFinderPanel extends JPanel {

    private ServerConfigListModel dataModel;
    private File confFile;
    private JComboBox comboMode;
    private JTextField textMainProperties;
    private JTextField textIP;
    private JTextField textPort;
    private JTextField textFile;
    private JTextField textBase;
    private Properties props = new Properties();
    private JButton buttonDir;
    private JTabbedPane tabbedPane;

    public static void main(String[] args) {
        System.setProperty(org.openconcerto.sql.PropsConfiguration.REDIRECT_TO_FILE, "true");
        System.setProperty(SQLBase.ALLOW_OBJECT_REMOVAL, "true");
        // this class is used to edit a configuration, not to modify the DB
        System.setProperty(SQLSchema.NOAUTO_CREATE_METADATA, "true");

        ExceptionHandler.setForceUI(true);
        ExceptionHandler.setForumURL("http://www.openconcerto.org/forum");
        PropsConfiguration conf = new PropsConfiguration(new Properties()) {
            @Override
            protected File createWD() {
                return new File(DesktopEnvironment.getDE().getDocumentsFolder().getAbsolutePath() + File.separator + ComptaPropsConfiguration.APP_NAME);
            }
        };
        conf.setupLogging();

        final File confFile = ComptaPropsConfiguration.getConfFile();
        System.out.println("Reading configuration from: " + confFile.getAbsolutePath());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                JFrame f = new JFrame("Configuration OpenConcerto");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                final ServerFinderPanel panel = new ServerFinderPanel();
                panel.uiInit();

                boolean loaded = panel.setConfigFile(confFile);
                if (!loaded) {
                    panel.fillWithDefaultH2Config();
                    panel.updateUIFromProps();
                }
                // Cloud UI need a loaded config
                panel.tabbedPane.addTab("Cloud", new CloudPanel(panel));
                f.setContentPane(panel);
                f.pack();
                f.setMinimumSize(new Dimension(f.getWidth(), f.getHeight()));
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            }
        });
    }

    protected void fillWithDefaultH2Config() {
        this.props.setProperty("server.driver", "h2");
        this.props.setProperty("customer", "Gestion_Default");
        this.props.setProperty("systemRoot", "OpenConcerto");
        this.props.setProperty("base.root", "Common");
        this.props.setProperty("server.ip", "file:${data.dir}/");
    }

    public static final String getDefaultSystemRootName() {
        return ComptaPropsConfiguration.APP_NAME;
    }

    public static final boolean containsValidH2DB(File dir) {
        return testH2DBDir(dir) == null;
    }

    public static final String testH2DBDir(File dir) {
        final String err;
        if (dir == null) {
            err = "Dossier de base de données non défini";
        } else if (!dir.exists()) {
            err = "Dossier de base de données inexistant";
        } else {
            final String fileName = getDefaultSystemRootName() + ".h2.db";
            final File h2File = new File(dir, fileName);
            if (!h2File.exists()) {
                err = "Le dossier de base de données ne contient pas " + fileName;
            } else if (h2File.length() < 50000) {
                err = "Le dossier de base de données contient un fichier " + fileName + " vide";
            } else {
                err = null;
            }
        }
        return err;
    }

    private boolean loadConfigFile() {
        this.textMainProperties.setText(this.confFile.getAbsolutePath());
        // load defaults since store() only outputs local keys
        final Properties defaults = new Properties();
        defaults.put("server.driver", "postgresql");
        defaults.put("server.ip", "127.0.0.1");
        defaults.put("systemRoot", getDefaultSystemRootName());
        defaults.put("customer", "Gestion_Default");
        this.props.clear();
        PropertiesUtils.load(this.props, defaults);

        if (!this.confFile.exists()) {
            System.out.println("Unable to find: " + this.confFile.getAbsolutePath());

            final File dir1 = new File(Configuration.getDefaultConfDir(), ComptaPropsConfiguration.APP_NAME + "-GESTION_DEFAULT/DBData");
            final File dir2 = new File(Configuration.getDefaultConfDir(), ComptaPropsConfiguration.APP_NAME + "/DBData");
            final File validDir;
            if (containsValidH2DB(dir1)) {
                validDir = dir1;
            } else if (containsValidH2DB(dir2)) {
                validDir = dir2;
            } else {
                validDir = null;
            }
            if (validDir != null) {
                System.out.println("Found DB in " + validDir);
                updateUIForMode(SQLSystem.H2);
                this.textFile.setText(validDir.getAbsolutePath());
                this.textBase.setText(getDefaultSystemRootName());
            }
            return validDir != null;
        }
        System.out.println("Loading: " + this.confFile.getAbsolutePath());
        if (!this.confFile.isFile()) {
            JOptionPane.showMessageDialog(null, this.confFile + " n'est pas un fichier valide");
        } else if (!this.confFile.canRead()) {
            JOptionPane.showMessageDialog(null, "Vous n'avez pas les droits de lire le fichier " + this.confFile);
        } else {
            if (!this.confFile.canWrite()) {
                JOptionPane.showMessageDialog(null, "Vous n'avez pas les droits de modifier le fichier " + this.confFile);
            }
            try {
                this.props.load(new FileInputStream(this.confFile));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Impossible de lire le fichier " + this.confFile + " \n" + e.getLocalizedMessage());
            }
            updateUIFromProps();

        }
        return true;
    }

    private void updateUIFromProps() {
        try {
            updateUIForMode(SQLSystem.get(this.props.getProperty("server.driver")));
        } catch (Exception e) {
            // Fallback on POSTGRESQL
            updateUIForMode(SQLSystem.POSTGRESQL);
        }
        final String serverIp = this.props.getProperty("server.ip");
        if (serverIp.startsWith("file:")) {
            // create conf to replace dataDir, clone since ComptaPropsConfiguration modify its
            // parameter
            this.textFile.setText(new ComptaPropsConfiguration(new Properties(this.props), false, false).getServerIp().substring(5));
        } else {
            final int i = serverIp.lastIndexOf(':');
            if (i > 0) {
                this.textIP.setText(serverIp.substring(0, i));
                this.textPort.setText(serverIp.substring(i + 1));
            } else {
                this.textIP.setText(serverIp);
            }
        }
        this.textBase.setText(this.props.getProperty("systemRoot"));
        if (getToken() != null) {
            this.textIP.setText("Cloud OpenConcerto");
            this.textPort.setText("");
            this.textFile.setText("");
        }
    }

    protected void saveConfigFile() {
        System.out.println("Saving:" + this.confFile.getAbsolutePath());
        if (this.confFile.getParentFile() != null)
            try {
                FileUtils.mkdir_p(this.confFile.getParentFile());
            } catch (IOException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(null, "Impossible de créer le dossier");
                return;
            }
        if (this.confFile.exists()) {
            if (!this.confFile.isFile()) {
                JOptionPane.showMessageDialog(null, this.confFile + " n'est pas un fichier valide");
                return;
            } else if (!this.confFile.canRead()) {
                JOptionPane.showMessageDialog(null, "Vous n'avez pas les droits de lire le fichier " + this.confFile);
                return;
            } else if (!this.confFile.canWrite()) {
                JOptionPane.showMessageDialog(null, "Vous n'avez pas les droits de modifier le fichier " + this.confFile);
                return;
            }
        }
        FileOutputStream out = null;
        try {
            final ServerFinderConfig config = createServerFinderConfig();
            final String serverDriver = config.getSystem().getJDBCName();
            final String serverIp;
            if (config.getSystem() == SQLSystem.H2) {
                File file = config.getFile();
                if (file == null)
                    file = new File("");
                final String filePath;
                if (file.getPath().length() == 0) {
                    filePath = ComptaPropsConfiguration.DATA_DIR_VAR;
                    if (getToken() != null) {
                        JOptionPane.showMessageDialog(null, "Attention. Le dossier de données n'est pas rempli");
                    }
                } else {
                    // replace getDataDir() by ComptaPropsConfiguration.DATA_DIR_VAR
                    final File canonFile = file.getCanonicalFile();
                    final File dataDir = config.createConf().getDataDir().getCanonicalFile();
                    if (canonFile.getPath().startsWith(dataDir.getPath())) {
                        filePath = ComptaPropsConfiguration.DATA_DIR_VAR + canonFile.getPath().substring(dataDir.getPath().length());
                    } else {
                        filePath = file.getPath();
                    }
                }
                serverIp = "file:" + filePath + (filePath.endsWith("/") ? "" : "/");
            } else {
                String ip = config.getIp();
                if (ip == null || ip.trim().length() == 0) {
                    if (getToken() != null) {
                        ip = "127.0.0.1";
                        JOptionPane.showMessageDialog(null, "Attention. L'adresse du serveur n'est pas remplie");
                    }
                }
                final String port = config.getPort().trim();
                serverIp = ip + (port.length() > 0 ? (":" + port) : "");
            }
            this.props.put("server.driver", serverDriver);
            this.props.put("server.ip", serverIp);
            this.props.put("systemRoot", config.getSystemRoot());
            out = new FileOutputStream(this.confFile);
            this.props.store(out, ComptaPropsConfiguration.APP_NAME);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Impossible d'écrire le fichier " + this.confFile + " \n" + e.getLocalizedMessage());
        } finally {
            if (out != null)
                try {
                    out.close();
                } catch (IOException e) {
                    // tant pis
                    e.printStackTrace();
                }
        }
        JOptionPane.showMessageDialog(this, "Configuration sauvegardée.\n" + this.confFile.getAbsolutePath());
    }

    protected void deleteConfigFile() {
        if (this.confFile.exists()) {
            final int ans = JOptionPane.showConfirmDialog(this, "Supprimer le fichier de configuration ?", "Confirmation", JOptionPane.YES_NO_OPTION);
            if (ans == JOptionPane.YES_OPTION)
                if (!this.confFile.delete())
                    JOptionPane.showMessageDialog(this, "Impossible d'effacer le fichier " + this.confFile, "Erreur", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Rien à effacer", null, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    protected boolean setConfigFile(File f) {
        this.confFile = f;
        return this.loadConfigFile();
    }

    private void uiInit() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        this.tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        this.add(this.tabbedPane, c);
        final JPanel configurationPanel = createPanelConfig();
        final JPanel findServerPanel = createPanelFinder();

        this.tabbedPane.addTab("Configuration", configurationPanel);
        this.tabbedPane.addTab("Recherche", findServerPanel);

        final ConfigCaissePanel createPanelCaisse = createPanelCaisse();
        this.tabbedPane.addTab("Caisse", createPanelCaisse);
        this.tabbedPane.addTab("Installation", new InstallationPanel(this));

        final JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout(FlowLayout.RIGHT, 2, 1));
        buttons.setOpaque(false);

        buttons.add(new JButton(new AbstractAction("Supprimer la configuration") {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                deleteConfigFile();
            }
        }));
        final JButton bSave = new JButton("Enregistrer la configuration");
        bSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveConfigFile();
                createPanelCaisse.saveConfiguration();
            }
        });
        buttons.add(bSave);
        c.weighty = 0;
        c.gridy++;
        this.add(buttons, c);
        updateUIForMode(ServerFinderConfig.H2);
        this.comboMode.addActionListener((new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateUIForMode((SQLSystem) ServerFinderPanel.this.comboMode.getSelectedItem());

            }
        }

        ));
    }

    private ConfigCaissePanel createPanelCaisse() {
        final ConfigCaissePanel configCaissePanel = new ConfigCaissePanel(this);
        configCaissePanel.loadConfiguration();
        return configCaissePanel;
    }

    public void updateUIForMode(SQLSystem mode) {
        if (getToken() != null) {
            // Cloud
            this.comboMode.setEnabled(false);
            this.comboMode.setSelectedItem(ServerFinderConfig.POSTGRESQL);
            this.textIP.setEnabled(false);
            this.textPort.setEnabled(false);
            this.textFile.setEnabled(false);
            this.buttonDir.setEnabled(false);
        } else {
            this.comboMode.setEnabled(true);
            if (mode.equals(ServerFinderConfig.H2)) {
                this.comboMode.setSelectedItem(ServerFinderConfig.H2);
                this.textIP.setEnabled(false);
                this.textPort.setEnabled(false);
                this.textFile.setEnabled(true);
                this.buttonDir.setEnabled(true);
            } else if (mode.equals(ServerFinderConfig.MYSQL)) {
                this.comboMode.setSelectedItem(ServerFinderConfig.MYSQL);
                this.textIP.setEnabled(true);
                this.textPort.setEnabled(true);
                this.textFile.setEnabled(false);
                this.buttonDir.setEnabled(false);
            } else if (mode.equals(ServerFinderConfig.POSTGRESQL)) {
                this.comboMode.setSelectedItem(ServerFinderConfig.POSTGRESQL);
                this.textIP.setEnabled(true);
                this.textPort.setEnabled(true);
                this.textFile.setEnabled(false);
                this.buttonDir.setEnabled(false);
            } else {
                throw new IllegalArgumentException(mode + " is not a valid access mode");
            }
        }
    }

    private JPanel createPanelConfig() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new DefaultGridBagConstraints();
        // L0: Fichier
        c.weightx = 0;
        p.add(new JLabel("Fichier de configuration", SwingConstants.RIGHT), c);
        c.gridx++;
        c.gridwidth = 3;
        c.weightx = 1;
        this.textMainProperties = new JTextField("");
        this.textMainProperties.setEditable(false);
        p.add(this.textMainProperties, c);

        // L1: Type
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        p.add(new JLabel("Type", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.comboMode = new JComboBox(new SQLSystem[] { ServerFinderConfig.H2, ServerFinderConfig.POSTGRESQL, ServerFinderConfig.MYSQL });
        this.comboMode.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, ((SQLSystem) value).getLabel(), index, isSelected, cellHasFocus);
            }
        });
        p.add(this.comboMode, c);
        // L2: IP, port
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(new JLabel("Adresse du serveur", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weighty = 0;
        this.textIP = new JTextField(20);
        p.add(this.textIP, c);
        c.gridx++;
        c.weightx = 0;
        p.add(new JLabel("port", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weighty = 0;
        this.textPort = new JTextField(5);
        p.add(this.textPort, c);

        // L3: file
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        p.add(new JLabel("Base de données", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weighty = 0;
        c.gridwidth = 1;
        this.textBase = new JTextField();
        this.textBase.setEditable(false);
        p.add(this.textBase, c);

        // L4: file
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = 1;
        p.add(new JLabel("Dossier de base de données", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weighty = 0;
        c.gridwidth = 2;
        this.textFile = new JTextField();
        this.textFile.setEditable(false);
        p.add(this.textFile, c);
        c.gridx += 2;
        c.weightx = 0;
        c.gridwidth = 1;
        this.buttonDir = new JButton("Sélectionner");
        this.buttonDir.setOpaque(false);
        this.buttonDir.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                File oldDir = new File(ServerFinderPanel.this.textFile.getText());
                if (oldDir.exists() && oldDir.isDirectory()) {
                    fc.setCurrentDirectory(oldDir);
                }
                int returnVal = fc.showOpenDialog(ServerFinderPanel.this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    try {
                        ServerFinderPanel.this.textFile.setText(file.getCanonicalPath());
                    } catch (IOException e1) {
                        ServerFinderPanel.this.textFile.setText(file.getAbsolutePath());
                        e1.printStackTrace();
                    }
                }
            }
        });
        p.add(this.buttonDir, c);
        // L4: Test
        c.gridx = 1;
        c.gridy++;
        final JButton buttonTest = new JButton("Tester la connexion");
        buttonTest.setOpaque(false);
        buttonTest.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                buttonTest.setEnabled(false);
                SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
                    @Override
                    public String doInBackground() {
                        ServerFinderConfig conf = createServerFinderConfig();
                        System.out.println("Testing:" + conf);
                        String test = conf.test();
                        test = test.replace("(", "\n(");
                        System.out.println(test);
                        return test;
                    }

                    @Override
                    public void done() {
                        buttonTest.setEnabled(true);
                        try {
                            String r = get();
                            JOptionPane.showMessageDialog(ServerFinderPanel.this, r);
                        } catch (InterruptedException ignore) {
                        } catch (java.util.concurrent.ExecutionException e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(ServerFinderPanel.this, e.getMessage());
                        }
                    }
                };
                worker.execute();
            }
        });
        c.fill = GridBagConstraints.NONE;
        p.add(buttonTest, c);

        // Spacer
        c.gridx = 0;
        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        final JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(10, 200));
        p.add(spacer, c);

        return p;
    }

    private JPanel createPanelFinder() {
        JPanel p = new JPanel();
        GridBagConstraints c = new DefaultGridBagConstraints();
        p.setLayout(new GridBagLayout());

        // Search

        final JProgressBar bar = new JProgressBar(0, 100);
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(bar, c);

        final JButton searchButton = new JButton("Rechercher");
        searchButton.setOpaque(false);
        searchButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                searchButton.setEnabled(false);
                searchButton.setText("Recherche en cours...");
                bar.setValue(0);
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    public Void doInBackground() {
                        try {
                            ServerFinderPanel.this.dataModel.startScan(new PropertyChangeListener() {

                                @Override
                                public void propertyChange(PropertyChangeEvent evt) {
                                    final int i = (Integer) evt.getNewValue();
                                    SwingUtilities.invokeLater(new Runnable() {

                                        @Override
                                        public void run() {
                                            bar.setValue(i);

                                        }
                                    });
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    public void done() {
                        bar.setValue(100);
                        searchButton.setEnabled(true);
                        searchButton.setText("Rechercher");
                    }
                };
                worker.execute();

            }
        });
        c.weightx = 0;
        c.gridx++;
        p.add(searchButton, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weightx = 1;
        p.add(new JLabel("Bases de données disponibles:"), c);

        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        this.dataModel = new ServerConfigListModel();
        final JList l = new JList(this.dataModel);
        l.setCellRenderer(new ListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JPanel p = new JPanel();
                if (!isSelected) {
                    p.setBackground(list.getBackground());
                    p.setForeground(list.getForeground());
                } else {
                    p.setBackground(list.getSelectionBackground());
                    p.setForeground(list.getSelectionForeground());
                }
                p.setLayout(new VFlowLayout());
                ServerFinderConfig c = (ServerFinderConfig) value;
                final String label;
                if (!c.getType().equals(ServerFinderConfig.H2)) {
                    label = c.getIp() + ":" + c.getPort() + " " + c.getType();
                } else {
                    label = c.getFile().getAbsolutePath() + " " + c.getType();
                }
                JLabel l1 = new JLabelBold(label);
                l1.setOpaque(false);
                p.add(l1);
                JLabel l2 = new JLabel(c.getProduct() == null ? "Connexion impossible" : c.getProduct());
                l2.setOpaque(false);
                p.add(l2);
                JLabel l3 = new JLabel(c.getError() == null ? "Serveur opérationnel" : c.getError());
                l3.setOpaque(false);
                p.add(l3);
                l1.setForeground(p.getForeground());
                l2.setForeground(p.getForeground());
                l3.setForeground(p.getForeground());
                return p;
            }
        });
        l.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    useSelectedConfig(l);
                }
            }
        });
        l.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        l.setBorder(BorderFactory.createEtchedBorder());
        final JButton buttonSelect = new JButton("Utiliser ce serveur");
        buttonSelect.setOpaque(false);
        buttonSelect.setEnabled(false);
        buttonSelect.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                useSelectedConfig(l);

            }
        });
        l.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    Object sel = l.getSelectedValue();
                    System.err.println(sel + " " + ((ServerFinderConfig) sel).getError());
                    buttonSelect.setEnabled(sel != null);

                }

            }
        });
        p.add(l, c);

        // Use
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.weighty = 0;
        p.add(buttonSelect, c);
        p.setOpaque(false);
        return p;
    }

    public ServerFinderConfig getServerConfig() {
        return this.createServerFinderConfig();
    }

    private void useSelectedConfig(final JList l) {
        Object sel = l.getSelectedValue();

        if (sel != null) {
            ServerFinderConfig config = (ServerFinderConfig) sel;
            this.textIP.setText(config.getIp());
            this.textPort.setText(String.valueOf(config.getPort()));
            if (config.getType().equals(ServerFinderConfig.H2)) {
                this.textIP.setText("");
                this.textPort.setText("");
                this.textFile.setText(config.getFile().getAbsolutePath());
            } else {
                this.textIP.setText(config.getIp());
                this.textPort.setText(String.valueOf(config.getPort()));
            }
            updateUIForMode(config.getType());
            this.tabbedPane.setSelectedIndex(0);

            if (((ServerFinderConfig) sel).getError() != null) {
                JOptionPane.showMessageDialog(ServerFinderPanel.this, "Attention. Ce serveur n'est pas configuré correctement.");
            }
        }
    }

    public ServerFinderConfig createServerFinderConfig() {
        ServerFinderConfig conf = new ServerFinderConfig(this.props);
        conf.setType((SQLSystem) this.comboMode.getSelectedItem());
        conf.setSystemRoot(this.textBase.getText());
        if (!conf.getType().equals(ServerFinderConfig.H2)) {
            conf.resetFile();
            conf.setIp(this.textIP.getText());
            conf.setPort(this.textPort.getText());
        } else {
            final File file = new File(this.textFile.getText());
            conf.setFile(file);
            conf.setIp("");
            conf.setPort("");
        }
        return conf;
    }

    public String getToken() {
        final String property = this.props.getProperty("token");
        if (property != null && property.length() < 20)
            return null;
        return property;
    }

    public void setToken(String token) {
        if (token == null) {
            this.props.remove("token");
        } else {
            this.props.setProperty("token", token);
        }
    }
}
