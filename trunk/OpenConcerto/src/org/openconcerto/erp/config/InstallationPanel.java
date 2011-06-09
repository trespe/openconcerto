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

import org.openconcerto.sql.changer.correct.FixSerial;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class InstallationPanel extends JPanel {

    JProgressBar bar = new JProgressBar();

    public InstallationPanel(final ServerFinderPanel finderPanel) {
        super(new GridBagLayout());
        setOpaque(false);
        GridBagConstraints c = new DefaultGridBagConstraints();
        JButton user = new JButton("Créer l'utilisateur");

        // JButton bd = new JButton("Créer la base de données");
        final JButton up = new JButton("Mise à niveau de la base");
        up.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                bar.setIndeterminate(true);
                up.setEnabled(false);
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        ComptaPropsConfiguration conf = ComptaPropsConfiguration.create(true);

                        // Mise à jour des taux
                        final SQLTable table = conf.getBase().getTable("VARIABLE_PAYE");
                        try {
                            updateVariablePaye(table, "SMIC", 9);
                            updateVariablePaye(table, "TRANCHE_A", 2946);
                            updateVariablePaye(table, "PART_SAL_GarantieMP", 23.83);
                            updateVariablePaye(table, "PART_PAT_GarantieMP", 38.98);

                            System.err.println(conf.getSystemRoot().getName());
                            conf.getSystemRoot().getRootsToMap().clear();

                            conf.getSystemRoot().refetch();
                            final Set<String> childrenNames = conf.getSystemRoot().getChildrenNames();
                            System.err.println(childrenNames);
                            SwingUtilities.invokeLater(new Runnable() {

                                @Override
                                public void run() {
                                    bar.setIndeterminate(false);
                                    bar.setMaximum(childrenNames.size());
                                }
                            });
                            int i = 1;
                            for (String child : childrenNames) {
                                if (child.startsWith(conf.getAppName()) || child.equalsIgnoreCase("Default")) {

                                    updateSocieteSchema(conf, child);
                                }
                                final int barValue = i;
                                SwingUtilities.invokeLater(new Runnable() {

                                    @Override
                                    public void run() {
                                        bar.setValue(barValue);
                                    }
                                });
                                i++;
                            }

                        } catch (Exception e1) {
                            ExceptionHandler.handle("Echec de mise à jour", e1);
                        }

                        conf.destroy();
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                up.setEnabled(true);
                                JOptionPane.showMessageDialog(InstallationPanel.this, "Mise à niveau réussie");
                            }
                        });

                    }
                }, "Database structure updater").start();

            }

        });

        c.weightx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(new JLabelBold("Création de l'utilisateur openconcerto dans la base"), c);
        c.gridy++;
        c.weightx = 1;
        this.add(new JLabel("Identifiant de connexion de votre base "), c);
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        this.add(new JLabel("Login"), c);
        c.gridx++;

        final JTextField login = new JTextField();
        c.weightx = 1;
        this.add(login, c);

        c.gridx++;
        c.weightx = 0;
        this.add(new JLabel("Mot de passe"), c);
        c.gridx++;
        final JTextField mdp = new JTextField();
        c.weightx = 1;
        this.add(mdp, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        this.add(user, c);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        user.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                try {
                    if (finderPanel.getServerConfig().createUserIfNeeded(login.getText(), mdp.getText())) {
                        JOptionPane.showMessageDialog(InstallationPanel.this, "L'utilisateur openconcerto a été correctement ajouté.");
                    } else {
                        JOptionPane.showMessageDialog(InstallationPanel.this, "L'utilisateur openconcerto existe déjà dans la base.");
                    }
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(InstallationPanel.this, "Une erreur est survenue pendant la connexion au serveur, vérifiez vos paramètres de connexion.");
                }
            }
        });

        // Injection SQL
        // c.gridy++;
        // c.weightx = 1;
        // c.gridwidth = GridBagConstraints.REMAINDER;
        // c.insets = new Insets(10, 3, 2, 2);
        // this.add(new TitledSeparator("Injecter la base", true), c);
        //
        // c.gridy++;
        // c.weightx = 0;
        // c.gridwidth = 1;
        // c.insets = DefaultGridBagConstraints.getDefaultInsets();
        // this.add(new JLabel("Fichier"), c);
        //
        // final JTextField chemin = new JTextField();
        // c.gridx++;
        // c.weightx = 1;
        // this.add(chemin, c);
        //
        // c.gridx++;
        // c.weightx = 0;
        // JButton browse = new JButton("...");
        // browse.addActionListener(new ActionListener() {
        //
        // @Override
        // public void actionPerformed(ActionEvent e) {
        // JFileChooser choose = new JFileChooser();
        // if (choose.showOpenDialog(InstallationPanel.this) == JFileChooser.APPROVE_OPTION) {
        // chemin.setText(choose.getSelectedFile().getAbsolutePath());
        // }
        // }
        // });
        // this.add(browse, c);
        //
        // c.gridy++;
        // c.gridx = 0;
        // JButton inject = new JButton("Injecter");
        // this.add(inject, c);
        // inject.addActionListener(new ActionListener() {
        //
        // @Override
        // public void actionPerformed(ActionEvent e) {
        // File f = new File(chemin.getText());
        // if (!f.exists()) {
        // JOptionPane.showMessageDialog(InstallationPanel.this, "Impossible de trouver le fichier "
        // + chemin.getText());
        // return;
        // }
        // BufferedReader input = null;
        // try {
        //
        // input = new BufferedReader(new FileReader(f));
        // StringBuffer sql = new StringBuffer();
        // String s;
        // while ((s = input.readLine()) != null) {
        // sql.append(s + "\n");
        // }
        // input.close();
        //
        // try {
        // final SQLServer sqlServer = finderPanel.getServerConfig().createSQLServer();
        // Number n = (Number)
        // sqlServer.getBase("postgres").getDataSource().executeScalar("select COUNT(*) from pg_database WHERE datname='OpenConcerto'");
        // if (n.intValue() > 0) {
        // JOptionPane.showMessageDialog(InstallationPanel.this,
        // "La base OpenConcerto est déjà présente sur le serveur!");
        // return;
        // }
        // // System.err.println(sqlServer.getBase("OpenConcerto"));
        // sqlServer.getBase("postgres").getDataSource()
        // .execute("CREATE DATABASE \"OpenConcerto\" WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'fr_FR.UTF-8' LC_CTYPE = 'fr_FR.UTF-8';");
        //
        // sqlServer.getBase("postgres").getDataSource().execute("ALTER DATABASE \"OpenConcerto\" OWNER TO openconcerto;");
        //
        // SQLUtils.executeScript(sql.toString(), sqlServer.getSystemRoot("OpenConcerto"));
        // sqlServer.destroy();
        // JOptionPane.showMessageDialog(InstallationPanel.this,
        // "Création de la base OpenConerto terminée.");
        // System.err.println("Création de la base OpenConerto terminée.");
        //
        // } catch (SQLException e1) {
        // // TODO Auto-generated catch block
        //
        // e1.printStackTrace();
        // JOptionPane.showMessageDialog(InstallationPanel.this,
        // "Une erreur s'est produite pendant l'injection du script, vérifier la connexion au serveur et le script.");
        // }
        //
        // } catch (FileNotFoundException ex) {
        // // TODO Auto-generated catch block
        // ex.printStackTrace();
        // } catch (IOException ex) {
        // // TODO Auto-generated catch block
        // ex.printStackTrace();
        // } finally {
        // if (input != null) {
        // try {
        // input.close();
        // } catch (IOException ex) {
        // // TODO Auto-generated catch block
        // ex.printStackTrace();
        // }
        // }
        // }
        //
        // }
        // });

        // c.gridy++;
        // this.add(bd, c);
        c.gridy++;
        c.weightx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(10, 3, 2, 2);
        this.add(new JLabelBold("Mise à niveau de la base OpenConcerto"), c);
        c.gridy++;
        this.add(this.bar, c);
        c.gridy++;
        c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.insets = DefaultGridBagConstraints.getDefaultInsets();
        this.add(up, c);

        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.weightx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 1;
        c.gridy++;
        final JPanel comp = new JPanel();
        comp.setOpaque(false);
        this.add(comp, c);
    }

    private void updateSocieteSchema(ComptaPropsConfiguration conf, String child) {

        // Add article
        SQLTable tableArticle = conf.getSystemRoot().getRoot(child).getTable("ARTICLE");
        if (!tableArticle.getFieldsName().contains("INFOS")) {
            AlterTable t = new AlterTable(tableArticle);
            t.addVarCharColumn("INFOS", 2048);
            tableArticle.getBase().getDataSource().execute(t.asString());
            try {
                tableArticle.fetchFields();
                tableArticle.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout du champ INFO à la table ARTICLE", ex);
            }
        }

        // Fix Caisse serial
        SQLTable tableCaisse = conf.getSystemRoot().getRoot(child).getTable("CAISSE");

        FixSerial f = new FixSerial(conf.getSystemRoot());
        try {
            f.change(tableCaisse);
        } catch (SQLException e2) {
            throw new IllegalStateException("Erreur lors la mise à jours des sequences de la table CAISSE", e2);
        }

        // add Mvt on Ticket
        SQLTable tableTicket = conf.getSystemRoot().getRoot(child).getTable("TICKET_CAISSE");
        if (!tableTicket.getFieldsName().contains("ID_MOUVEMENT")) {
            AlterTable t = new AlterTable(tableTicket);
            t.addForeignColumn("ID_MOUVEMENT", conf.getSystemRoot().getRoot(child).getTable("MOUVEMENT"));
            tableTicket.getBase().getDataSource().execute(t.asString());
            try {
                tableTicket.fetchFields();
                tableTicket.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout du champ ID_MOUVEMENT à la table TICKET_CAISSE", ex);
            }
        }

        // Check type de reglement
        SQLTable tableReglmt = conf.getSystemRoot().getRoot(child).getTable("TYPE_REGLEMENT");
        SQLSelect sel = new SQLSelect(tableReglmt.getBase());
        sel.addSelect(tableReglmt.getKey());
        sel.setWhere(new Where(tableReglmt.getField("NOM"), "=", "Virement"));
        List<Number> l = (List<Number>) tableReglmt.getBase().getDataSource().executeCol(sel.asString());
        if (l.size() == 0) {
            SQLRowValues rowVals = new SQLRowValues(tableReglmt);
            rowVals.put("NOM", "Virement");
            rowVals.put("COMPTANT", Boolean.FALSE);
            rowVals.put("ECHEANCE", Boolean.FALSE);
            try {
                rowVals.commit();
            } catch (SQLException e) {
                throw new IllegalStateException("Erreur lors de l'ajout du type de paiement par virement", e);
            }
        }

        SQLSelect sel2 = new SQLSelect(tableReglmt.getBase());
        sel2.addSelect(tableReglmt.getKey());
        sel2.setWhere(new Where(tableReglmt.getField("NOM"), "=", "CESU"));
        List<Number> l2 = (List<Number>) tableReglmt.getBase().getDataSource().executeCol(sel2.asString());
        if (l2.size() == 0) {
            SQLRowValues rowVals = new SQLRowValues(tableReglmt);
            rowVals.put("NOM", "CESU");
            rowVals.put("COMPTANT", Boolean.FALSE);
            rowVals.put("ECHEANCE", Boolean.FALSE);
            try {
                rowVals.commit();
            } catch (SQLException e) {
                throw new IllegalStateException("Erreur lors de l'ajout du type CESU", e);
            }
        }

        // Undefined
        try {

            SQLTable.setUndefID(tableTicket.getSchema(), tableTicket.getName(), 1);
            SQLTable.setUndefID(tableTicket.getSchema(), "CAISSE", 1);
        } catch (SQLException e1) {
            throw new IllegalStateException("Erreur lors de la mise à jour des indéfinis de la table CAISSE", e1);
        }

        // Mise à jour du schéma
        try {
            tableTicket.getSchema().updateVersion();
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur lors de la mise à jour de la version du schema", e);
        }

    }

    private void updateVariablePaye(SQLTable table, String var, double value) throws SQLException {

        SQLSelect sel = new SQLSelect(table.getBase());
        sel.addSelectStar(table);
        sel.setWhere(new Where(table.getField("NOM"), "=", var));
        List<SQLRow> l = (List<SQLRow>) table.getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel));

        for (SQLRow sqlRow : l) {
            SQLRowValues rowVals = sqlRow.asRowValues();
            rowVals.put("VALEUR", value);
            rowVals.update();
        }
    }

}
