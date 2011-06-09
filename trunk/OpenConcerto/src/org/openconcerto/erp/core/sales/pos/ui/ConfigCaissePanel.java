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
 
 package org.openconcerto.erp.core.sales.pos.ui;

import org.openconcerto.erp.config.ServerFinderConfig;
import org.openconcerto.erp.config.ServerFinderPanel;
import org.openconcerto.erp.core.sales.pos.Caisse;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLServer;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class ConfigCaissePanel extends JPanel {
    private final JComboBox comboType;
    private final JTextField textPrintWidth;
    private final JTextField textPort;
    private final JTextField textLibJPOS;
    private final JCheckBox checkboxDouble;
    private int userId;
    private int societeId;
    private int caisseId;
    private final ServerFinderPanel serverFinderPanel;
    private final JComboBox comboSociete;
    private final JComboBox comboCaisse;
    private final JComboBox comboUtilisateur;
    private final TicketLineTable headerTable;
    private final TicketLineTable footerTable;

    // Selecteur de societe
    // Selecteur d'utilisateur
    // Selecteur de numero de caisse
    // Headers
    // Footers
    // JPOS
    // ESCP
    public ConfigCaissePanel(final ServerFinderPanel serverFinderPanel) {
        this.serverFinderPanel = serverFinderPanel;
        setOpaque(false);

        setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weighty = 0;
        // Connexion
        final JLabelBold titleConnexion = new JLabelBold("Connexion");
        c.gridwidth = 2;
        this.add(titleConnexion, c);
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        this.add(new JLabel("Société", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        this.comboSociete = new JComboBox();
        this.comboSociete.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                final SQLRow r = (SQLRow) value;
                String label = "";
                if (r != null) {
                    label = r.getString("NOM") + " (" + r.getString("ID") + ")";
                }
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            }
        });
        this.add(this.comboSociete, c);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Caisse", SwingConstants.RIGHT), c);
        c.gridx++;
        this.comboCaisse = new JComboBox();
        this.comboCaisse.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                final SQLRow r = (SQLRow) value;
                String label = "";
                if (r != null) {
                    label = r.getString("NOM") + " (" + r.getString("ID") + ")";
                }
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            }
        });
        this.add(this.comboCaisse, c);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Utilisateur", SwingConstants.RIGHT), c);
        c.gridx++;
        this.comboUtilisateur = new JComboBox();
        this.comboUtilisateur.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                final SQLRow r = (SQLRow) value;
                String label = "";
                if (r != null) {
                    label = r.getString("NOM") + " " + r.getString("PRENOM") + " (" + r.getString("ID") + ")";
                }
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            }
        });
        this.add(this.comboUtilisateur, c);

        // Ticket
        final JLabelBold titleTicket = new JLabelBold("Ticket");
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(titleTicket, c);
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        this.add(new JLabel("Entête", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;

        c.fill = GridBagConstraints.BOTH;
        this.headerTable = new TicketLineTable();
        this.add(this.headerTable, c);
        c.gridx = 0;
        c.gridy++;

        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Pied de page", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;

        c.fill = GridBagConstraints.BOTH;
        this.footerTable = new TicketLineTable();
        this.add(this.footerTable, c);
        c.fill = GridBagConstraints.HORIZONTAL;

        this.checkboxDouble = new JCheckBox("imprimer le ticket en double exemplaire");
        this.checkboxDouble.setOpaque(false);
        c.gridx = 1;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        this.add(this.checkboxDouble, c);

        // Imprimante
        final JLabelBold titleImprimante = new JLabelBold("Imprimante ticket");
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(titleImprimante, c);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(new JLabel("Largeur (en caractères)", SwingConstants.RIGHT), c);
        c.gridx++;
        this.textPrintWidth = new JTextField(8);
        this.add(this.textPrintWidth, c);
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Type", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.comboType = new JComboBox(new String[] { "jpos", "escp" });
        this.add(this.comboType, c);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Port/nom", SwingConstants.RIGHT), c);
        c.gridx++;
        this.textPort = new JTextField();
        this.add(this.textPort, c);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Dossier lib JPOS", SwingConstants.RIGHT), c);
        c.gridx++;
        this.textLibJPOS = new JTextField();
        this.add(this.textLibJPOS, c);

        // Spacer
        c.gridx = 0;
        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        final JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(10, 10));

        add(spacer, c);

        // Listeners
        this.comboSociete.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    ConfigCaissePanel.this.societeId = ((SQLRow) e.getItem()).getID();
                    reloadCaisses();
                }
            }
        });
        this.comboCaisse.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    ConfigCaissePanel.this.caisseId = ((SQLRow) e.getItem()).getID();
                }
            }
        });
        this.comboUtilisateur.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    ConfigCaissePanel.this.userId = ((SQLRow) e.getItem()).getID();
                }
            }
        });
    }

    protected void reloadCaisses() {
        this.comboCaisse.setEnabled(false);
        final int id = this.societeId;
        final ServerFinderConfig config = ConfigCaissePanel.this.serverFinderPanel.createServerFinderConfig();
        if (!config.isOnline()) {
            JOptionPane.showMessageDialog(ConfigCaissePanel.this, "Impossible de se connecter au serveur");
            return;
        }

        try {

            SQLServer server = config.createServer("Common");
            DBSystemRoot r = server.getSystemRoot("OpenConcerto");
            DBRoot root = r.getRoot("Common");
            // Sociétés
            SQLSelect sel = new SQLSelect(root.getBase());
            sel.addSelectStar(root.getTable("SOCIETE_COMMON"));
            sel.setWhere("SOCIETE_COMMON.ID", "=", id);

            final List<SQLRow> societes = SQLRowListRSH.execute(sel);
            server.destroy();
            if (societes.size() > 0) {
                final String name = societes.get(0).getString("DATABASE_NAME");
                server = config.createServer(name);
                r = server.getSystemRoot("OpenConcerto");
                root = r.getRoot(name);
                // Caisses
                sel = new SQLSelect(root.getBase());
                sel.addSelectStar(root.getTable("CAISSE"));
                final List<SQLRow> caisses = SQLRowListRSH.execute(sel);

                server.destroy();

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (caisses.isEmpty()) {
                            JOptionPane.showMessageDialog(ConfigCaissePanel.this, "Pas de caisses définies dans la société " + name);
                        }
                        ConfigCaissePanel.this.comboCaisse.setModel(new DefaultComboBoxModel(new Vector<SQLRow>(caisses)));
                        ConfigCaissePanel.this.comboUtilisateur.setEnabled(true);
                        ConfigCaissePanel.this.comboCaisse.setEnabled(true);
                        final ComboBoxModel model = ConfigCaissePanel.this.comboCaisse.getModel();
                        final int stop = model.getSize();
                        if (stop > 0) {
                            // Force la reselection si la valeur n'existe plus,
                            // nécessité de recuperer l'id
                            ConfigCaissePanel.this.caisseId = ((SQLRow) model.getElementAt(0)).getID();
                        }
                        for (int i = 0; i < stop; i++) {
                            final SQLRow r = (SQLRow) model.getElementAt(i);
                            if (r.getID() == ConfigCaissePanel.this.caisseId) {
                                ConfigCaissePanel.this.comboCaisse.setSelectedItem(r);
                                break;
                            }
                        }
                    }

                });
            } else {
                JOptionPane.showMessageDialog(this, "Impossible de trouver la société d'ID " + id);
            }

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public void loadConfiguration() {
        if (Caisse.isUsingJPos()) {
            this.comboType.setSelectedIndex(0);
            this.textPort.setText(Caisse.getJPosPrinter());
        } else {
            this.comboType.setSelectedIndex(1);
            this.textPort.setText(Caisse.getESCPPort());
        }
        this.textPrintWidth.setText(String.valueOf(Caisse.getTicketWidth()));
        this.textLibJPOS.setText(Caisse.getJPosDirectory());
        this.checkboxDouble.setSelected(Caisse.isCopyActive());
        this.userId = Caisse.getUserID();
        this.societeId = Caisse.getSocieteID();
        this.caisseId = Caisse.getID();
        this.headerTable.fillFrom(Caisse.getHeaders());
        this.footerTable.fillFrom(Caisse.getFooters());

        addComponentListener(new ComponentListener() {
            @Override
            public void componentHidden(final ComponentEvent event) {
            }

            @Override
            public void componentMoved(final ComponentEvent event) {
            }

            @Override
            public void componentResized(final ComponentEvent event) {
            }

            @Override
            public void componentShown(final ComponentEvent event) {
                final Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        ConfigCaissePanel.this.comboSociete.setEnabled(false);
                        ConfigCaissePanel.this.comboUtilisateur.setEnabled(false);
                        ConfigCaissePanel.this.comboCaisse.setEnabled(false);
                        final ServerFinderConfig config = ConfigCaissePanel.this.serverFinderPanel.createServerFinderConfig();
                        if (!config.isOnline()) {
                            JOptionPane.showMessageDialog(ConfigCaissePanel.this, "Impossible de se connecter au serveur");
                            return;
                        }
                        String result = "Erreur de connexion. \n";
                        try {

                            final SQLServer server = config.createServer("Common");
                            final DBSystemRoot r = server.getSystemRoot("OpenConcerto");
                            final DBRoot root = r.getRoot("Common");
                            // Sociétés
                            SQLSelect sel = new SQLSelect(root.getBase());
                            sel.addSelectStar(root.getTable("SOCIETE_COMMON"));
                            final List<SQLRow> societes = SQLRowListRSH.execute(sel);

                            // Utilisateurs
                            sel = new SQLSelect(root.getBase());
                            sel.addSelectStar(root.getTable("USER_COMMON"));
                            final List<SQLRow> utilisateurs = SQLRowListRSH.execute(sel);

                            server.destroy();

                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    ConfigCaissePanel.this.comboSociete.setModel(new DefaultComboBoxModel(new Vector<SQLRow>(societes)));
                                    ConfigCaissePanel.this.comboUtilisateur.setModel(new DefaultComboBoxModel(new Vector<SQLRow>(utilisateurs)));

                                    ConfigCaissePanel.this.comboSociete.setEnabled(true);
                                    ConfigCaissePanel.this.comboUtilisateur.setEnabled(true);

                                    ComboBoxModel model = ConfigCaissePanel.this.comboSociete.getModel();
                                    int stop = model.getSize();
                                    for (int i = 0; i < stop; i++) {
                                        final SQLRow r = (SQLRow) model.getElementAt(i);
                                        if (r.getID() == ConfigCaissePanel.this.societeId) {
                                            ConfigCaissePanel.this.comboSociete.setSelectedItem(r);
                                            break;
                                        }
                                    }
                                    model = ConfigCaissePanel.this.comboUtilisateur.getModel();
                                    stop = model.getSize();
                                    for (int i = 0; i < stop; i++) {
                                        final SQLRow r = (SQLRow) model.getElementAt(i);
                                        if (r.getID() == ConfigCaissePanel.this.userId) {
                                            ConfigCaissePanel.this.comboUtilisateur.setSelectedItem(r);
                                            break;
                                        }
                                    }
                                }

                            });

                        } catch (final Exception e) {
                            result += e.getMessage();
                            e.printStackTrace();
                        }

                    }
                });
                t.setDaemon(true);
                t.start();
            }
        });
    }

    public void saveConfiguration() {
        Caisse.setPrinterType(this.comboType.getSelectedItem().toString());
        if (this.comboType.getSelectedIndex() == 0) {
            // JPOS
            Caisse.setJPosPrinter(this.textPort.getText());
        } else {
            Caisse.setESCPPort(this.textPort.getText());
        }
        Caisse.setTicketWidth(this.textPrintWidth.getText());
        Caisse.setJPosDirectory(this.textLibJPOS.getText());
        Caisse.setCopyActive(this.checkboxDouble.isSelected());

        Caisse.setUserID(this.userId);
        Caisse.setSocieteID(this.societeId);
        Caisse.setID(this.caisseId);

        Caisse.setHeaders(this.headerTable.getLines());
        Caisse.setFooters(this.footerTable.getLines());
        Caisse.saveConfiguration();
    }
}
