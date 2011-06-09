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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ServerConfigListModel extends AbstractListModel {

    // Thread safe...
    private List<ServerFinderConfig> confs = new Vector<ServerFinderConfig>();

    @Override
    public Object getElementAt(int index) {
        return confs.get(index);
    }

    @Override
    public int getSize() {
        return confs.size();
    }

    public void startScan(PropertyChangeListener propertyChangeListener) {
        confs.clear();
        fireContentsChanged(this, 0, 0);
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, "Impossible de charger le driver PostgreSQL");
            e.printStackTrace();
            return;
        }
        ServerFinder f = new ServerFinder();
        List<String> l = f.getIPsToScan();
        int stop = l.size();
        for (int i = 0; i < stop; i++) {
            String ip = l.get(i);
            final String s = ip;
            scan(s);
            propertyChangeListener.propertyChange(new PropertyChangeEvent(this, "bar", null, new Integer(1 + (i * 100) / stop)));
        }
    }

    public void scan(final String ip) {
        ServerFinder f = new ServerFinder();
        System.out.println("Test:'" + ip + "'");
        if (!ip.equals("127.0.0.1")) {
            InetAddress addr;
            try {
                addr = InetAddress.getByName(ip);
                if (!ServerFinder.ping(addr)) {
                    return;
                }
            } catch (UnknownHostException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        boolean maybePostgreSQL = f.isPortOpen(ip, 5432);

        if (false) {
            // TODO: add MySQL
            boolean maybeMySQL = f.isPortOpen(ip, 3306);
            if (maybeMySQL)
                System.out.println("maybe my" + ip);
            if (!maybeMySQL && !maybePostgreSQL) {
                return;
            }
        }
        if (maybePostgreSQL) {

            // load the driver
            Connection db = null;
            final ServerFinderConfig c = new ServerFinderConfig();
            c.setIp(ip);
            c.setPort("5432");
            c.setType(ServerFinderConfig.POSTGRESQL);
            try {
                db = DriverManager.getConnection("jdbc:postgresql://" + ip + ":5432/OpenConcerto", "openconcerto", "openconcerto");
            } catch (SQLException e) {
                e.printStackTrace();
                String message = e.getMessage();

                c.setError(message);

            }
            if (db != null) {
                try {
                    DatabaseMetaData dbmd = db.getMetaData();
                    c.setProduct(dbmd.getDatabaseProductName() + " " + dbmd.getDatabaseProductVersion());
                } catch (SQLException e) {
                    c.setError(e.getMessage());
                    e.printStackTrace();
                }
                try {
                    db.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            confs.add(c);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    fireContentsChanged(this, 0, confs.size());
                }
            });
        }

    }

    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, "Impossible de charger le driver PostgreSQL");
            e.printStackTrace();
            return;
        }
        final ServerConfigListModel serverConfigListModel = new ServerConfigListModel();

        serverConfigListModel.scan("127.0.0.1");
        serverConfigListModel.scan("192.168.1.10");
        serverConfigListModel.scan("192.168.1.16");
        serverConfigListModel.scan("192.168.1.3");
        serverConfigListModel.scan("192.168.1.4");
        serverConfigListModel.scan("192.168.1.5");
        serverConfigListModel.scan("192.168.1.6");
        serverConfigListModel.scan("192.168.1.7");
        serverConfigListModel.scan("192.168.1.8");
        serverConfigListModel.scan("192.168.1.9");
        serverConfigListModel.scan("192.168.1.10");

        // serverConfigListModel.startScan(null);
        try {
            Thread.sleep(100 * 1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
