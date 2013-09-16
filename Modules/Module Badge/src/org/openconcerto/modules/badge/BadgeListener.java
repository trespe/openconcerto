package org.openconcerto.modules.badge;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.sql.SQLException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.ServerFinderPanel;
import org.openconcerto.erp.modules.ModuleManager;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.preferences.UserProps;
import org.openconcerto.sql.sqlobject.IComboSelectionItem;
import org.openconcerto.utils.ExceptionHandler;

public class BadgeListener implements Runnable {
    private static final int UDP_PORT = 1470;
    private String ip;
    private int relai;

    protected TrayIcon trayIcon;

    public BadgeListener() {
        final ComptaPropsConfiguration conf = ComptaPropsConfiguration.create(true);
        if (conf == null) {
            ServerFinderPanel.main(new String[0]);
            return;
        }

        Configuration.setInstance(conf);

        try {
            conf.getBase();
            // create table if necessary
            SQLPreferences.getPrefTable(conf.getRoot());
        } catch (Exception e) {
            ExceptionHandler.die("Erreur de connexion à la base de données", e);
            // since we're not in the EDT, the previous call doesn't block,
            // so return (it won't quit the VM since a dialog is displaying)
            return;
        }
        try {
            final File moduleDir = new File("Modules");
            moduleDir.mkdir();
            ModuleManager.getInstance().addFactories(moduleDir);
        } catch (Throwable e) {
            ExceptionHandler.handle("Erreur d'accès aux modules", e);
        }

        int selectedSociete = UserProps.getInstance().getLastSocieteID();
        if (selectedSociete < SQLRow.MIN_VALID_ID) {
            final SQLElement elem = conf.getDirectory().getElement(conf.getRoot().getTable("SOCIETE_COMMON"));
            final List<IComboSelectionItem> comboItems = elem.getComboRequest().getComboItems();
            if (comboItems.size() > 0)
                selectedSociete = comboItems.get(0).getId();
            else
                throw new IllegalStateException("No " + elem + " found");
        }
        conf.setUpSocieteDataBaseConnexion(selectedSociete);

    }

    private PopupMenu createTrayMenu() {
        ActionListener exitListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Bye from the tray");
                System.exit(0);
            }
        };

        ActionListener executeListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Relai r = new Relai(ip, relai);
                try {
                    r.pulse(2);
                    trayIcon.displayMessage("Ouverture", "Ouverture de la porte OK", TrayIcon.MessageType.INFO);
                } catch (Throwable ex) {
                    trayIcon.displayMessage("Erreur", ex.getMessage(), TrayIcon.MessageType.ERROR);
                }
            }
        };

        PopupMenu menu = new PopupMenu();
        MenuItem execItem = new MenuItem("Ouvrir la porte");
        execItem.addActionListener(executeListener);
        menu.add(execItem);

        MenuItem exitItem = new MenuItem("Quitter");
        exitItem.addActionListener(exitListener);
        menu.add(exitItem);
        return menu;
    }

    private TrayIcon createTrayIcon() {
        Image image = new ImageIcon(this.getClass().getResource("badge.png")).getImage();
        PopupMenu popup = createTrayMenu();
        TrayIcon ti = new TrayIcon(image, "Service de badge", popup);
        ti.setImageAutoSize(true);
        return ti;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        BadgeListener bl = new BadgeListener();
        bl.readConfiguration();
        bl.initUI();
        bl.startDaemon();

    }

    public void startDaemon() {
        Thread t = new Thread(this);
        t.setName("UDP Listener");
        t.start();
    }

    public void initUI() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (!SystemTray.isSupported()) {
                    JOptionPane.showMessageDialog(new JFrame(), "System tray not supported on this platform");
                    System.exit(1);
                }
                try {
                    final SystemTray sysTray = SystemTray.getSystemTray();
                    trayIcon = createTrayIcon();
                    sysTray.add(trayIcon);
                    trayIcon.displayMessage("Service de badge", "Ecoute sur port " + UDP_PORT, TrayIcon.MessageType.NONE);
                } catch (AWTException e) {
                    System.out.println("Unable to add icon to the system tray");
                    System.exit(1);
                }

            }
        });

    }

    public void readConfiguration() {
        final Properties props = new Properties();
        final File file = new File("badge.properties");
        System.out.println("Reading from: " + file.getAbsolutePath());
        try {
            final FileInputStream inStream = new FileInputStream(file);
            props.load(inStream);
            this.ip = props.getProperty("ip");
            this.relai = Integer.parseInt(props.getProperty("relai", "1"));
            inStream.close();
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(new JFrame(), "Fichier manquant\n" + file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(new JFrame(), e.getMessage());
        }

    }

    @Override
    public void run() {
        while (true) {
            DatagramSocket serverSocket = null;

            try {
                final byte[] receiveData = new byte[1024];
                serverSocket = new DatagramSocket(UDP_PORT);

                while (true) {
                    final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);
                    final String sentence = new String(receivePacket.getData()).trim();
                    System.out.println(sentence);
                    if (isBadgeAllowed(sentence)) {
                        final Relai r = new Relai(ip, relai);
                        try {
                            r.pulse(2);
                        } catch (Throwable ex) {
                            trayIcon.displayMessage("Erreur", ex.getMessage(), TrayIcon.MessageType.ERROR);
                        }
                    } else {
                        trayIcon.displayMessage("Carte refusée", "Carte " + sentence + " non acceptée", TrayIcon.MessageType.INFO);
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
                trayIcon.displayMessage("Erreur", (e == null) ? "" : e.getMessage(), TrayIcon.MessageType.ERROR);
            } finally {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            }
            try {
                System.out.println("Waiting 10s");
                Thread.sleep(10 * 1000);
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public boolean isBadgeAllowed(String cardNumber) {
        SQLBase base = Configuration.getInstance().getBase();
        SQLSelect sel = new SQLSelect(base);
        SQLTable tableAdh = Configuration.getInstance().getRoot().findTable("ADHERENT");
        sel.addSelectStar(tableAdh);
        sel.setWhere(new Where(tableAdh.getField("NUMERO_CARTE"), "=", cardNumber));
        List<SQLRow> list = (List<SQLRow>) base.getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel));

        String motif = "";
        Boolean onlyAdmin = ModuleManager.getInstance().getFactory("org.openconcerto.modules.badge").getSQLPreferences(tableAdh.getDBRoot()).getBoolean(Module.ENTREE_PREF, false);
        boolean allow = false;
        SQLRow adh = null;
        // Aucun adhérent assigné à cette carte
        if (list == null || list.isEmpty()) {
            motif = "Aucun adhérent associé à la carte " + cardNumber;
            System.err.println(motif);

        } else if (list.size() > 1) {
            motif = list.size() + " adhérents sont liés à la même carte " + cardNumber;
            System.err.println(motif);
            Thread.dumpStack();
        } else {

            for (SQLRow sqlRow : list) {

                adh = sqlRow;

                // Admin toujours autorisé
                if (sqlRow.getBoolean("ADMIN")) {
                    allow = true;
                    motif = "Administrateur toujours autorisé";
                    break;
                }

                if (onlyAdmin) {
                    motif = "Seul les membres administrateurs sont autorisés!";
                    break;
                }

                if (!sqlRow.getBoolean("ACTIF")) {
                    motif = "La carte de l'adhérent n'est pas active dans sa fiche";
                    break;
                }

                Calendar cal = Calendar.getInstance();
                final Date d = cal.getTime();
                final Calendar dateValidite = sqlRow.getDate("DATE_VALIDITE_INSCRIPTION");

                if (dateValidite != null && dateValidite.before(cal)) {
                    motif = "La date d'autorisation est expirée";
                    break;
                }

                SQLRow rowPlage = sqlRow.getForeignRow("ID_PLAGE_HORAIRE");

                if (rowPlage != null) {
                    Time time = new Time(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE");
                    String day = dateFormat.format(d).toUpperCase();
                    {
                        Time d1 = (Time) rowPlage.getObject("DEBUT_1_" + day);
                        Time f1 = (Time) rowPlage.getObject("FIN_1_" + day);
                        if (d1 != null && f1 != null) {
                            if (time.after(d1) && time.before(f1)) {
                                allow = true;
                                motif = "Autorisé sur la plage " + rowPlage.getString("NOM") + " 1";
                                break;
                            }
                        }
                    }
                    {
                        Time d1 = (Time) rowPlage.getObject("DEBUT_2_" + day);
                        Time f1 = (Time) rowPlage.getObject("FIN_2_" + day);

                        if (d1 != null && f1 != null) {
                            if (time.after(d1) && time.before(f1)) {
                                allow = true;
                                motif = "Autorisé sur la plage " + rowPlage.getString("NOM") + " 2";
                                break;
                            }
                        }
                    }

                    {
                        Time d1 = (Time) rowPlage.getObject("DEBUT_3_" + day);
                        Time f1 = (Time) rowPlage.getObject("FIN_3_" + day);

                        if (d1 != null && f1 != null) {
                            if (time.after(d1) && time.before(f1)) {
                                allow = true;
                                motif = "Autorisé sur la plage " + rowPlage.getString("NOM") + " 3";
                                break;
                            }
                        }
                    }
                    motif = "Non autorisé sur la plage horaire " + rowPlage.getString("NOM");
                } else {
                    motif = "Aucune plage horaire associée";
                }
            }
        }

        // Création de l'entrée dans la table
        SQLTable tableEntree = Configuration.getInstance().getRoot().findTable("ENTREE");
        SQLRowValues rowVals = new SQLRowValues(tableEntree);
        rowVals.put("DATE", new Date());
        rowVals.put("NUMERO_CARTE", cardNumber);
        rowVals.put("ACCEPTE", allow);
        rowVals.put("MOTIF", motif);

        if (adh != null) {
            rowVals.put("ADHERENT", adh.getString("NOM") + " " + adh.getString("PRENOM"));
        }
        try {
            rowVals.commit();
        } catch (SQLException exn) {
            exn.printStackTrace();
        }
        return allow;
    }

}
