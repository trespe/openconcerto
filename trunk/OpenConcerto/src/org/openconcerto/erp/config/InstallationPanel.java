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

import org.openconcerto.erp.modules.ModuleManager;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.changer.convert.AddFK;
import org.openconcerto.sql.changer.correct.CorrectOrder;
import org.openconcerto.sql.changer.correct.FixSerial;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLField.Properties;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.DatabaseGraph;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.model.graph.SQLKey;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.ChangeTable;
import org.openconcerto.sql.utils.DropTable;
import org.openconcerto.sql.utils.ReOrder;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class InstallationPanel extends JPanel {
    private static final boolean DEBUG_FK = false;

    static private void insertUndef(final SQLCreateTable ct) {
        final String insert = "INSERT into " + getTableName(ct).quote() + "(" + SQLBase.quoteIdentifier(SQLSyntax.ORDER_NAME) + ") VALUES(" + ReOrder.MIN_ORDER + ")";
        ct.getRoot().getDBSystemRoot().getDataSource().execute(insert);
    }

    static private SQLName getTableName(final SQLCreateTable ct) {
        return new SQLName(ct.getRoot().getName(), ct.getName());
    }

    JProgressBar bar = new JProgressBar();
    boolean error;

    public InstallationPanel(final ServerFinderPanel finderPanel) {
        super(new GridBagLayout());
        setOpaque(false);
        GridBagConstraints c = new DefaultGridBagConstraints();
        JButton user = new JButton("Créer l'utilisateur");
        user.setOpaque(false);
        // JButton bd = new JButton("Créer la base de données");
        final JButton up = new JButton("Mise à niveau de la base");
        up.setOpaque(false);
        up.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                finderPanel.saveConfigFile();
                bar.setIndeterminate(true);
                up.setEnabled(false);
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        final ComptaPropsConfiguration conf = ComptaPropsConfiguration.create(true);

                        try {
                            final SQLDataSource ds = conf.getSystemRoot().getDataSource();
                            System.err.println("SystemRoot:" + conf.getSystemRoot());
                            System.err.println("Root:" + conf.getRoot());

                            // FixUnbounded varchar
                            fixUnboundedVarchar(conf.getRoot());

                            // FIXME DROP CONSTRAINT UNIQUE ORDRE ON CONTACT_FOURNISSEUR

                            // Mise à jour des taux
                            final SQLTable table = conf.getRoot().getTable("VARIABLE_PAYE");
                            System.out.println("InstallationPanel.InstallationPanel() UPDATE PAYE");
                            updateVariablePaye(table, "SMIC", 9);
                            updateVariablePaye(table, "TRANCHE_A", 2946);
                            updateVariablePaye(table, "PART_SAL_GarantieMP", 23.83);
                            updateVariablePaye(table, "PART_PAT_GarantieMP", 38.98);

                            updateSocieteTable(conf.getRoot());

                            if (!table.getDBRoot().contains("DEVISE")) {
                                System.out.println("InstallationPanel.InstallationPanel() ADD DEVISE");
                                try {
                                    SQLUtils.executeAtomic(ds, new SQLUtils.SQLFactory<Object>() {
                                        @Override
                                        public Object create() throws SQLException {
                                            final SQLCreateTable createDevise = new SQLCreateTable(table.getDBRoot(), "DEVISE");
                                            createDevise.addVarCharColumn("CODE", 128);
                                            createDevise.addVarCharColumn("NOM", 128);
                                            createDevise.addVarCharColumn("LIBELLE", 128);
                                            createDevise.addVarCharColumn("LIBELLE_CENT", 128);
                                            createDevise.addColumn("TAUX", "numeric(16,8) default 1");
                                            ds.execute(createDevise.asString());

                                            insertUndef(createDevise);

                                            conf.getRoot().getSchema().updateVersion();

                                            return null;
                                        }
                                    });
                                } catch (Exception ex) {
                                    throw new IllegalStateException("Erreur lors de la création de la table DEVISE", ex);
                                }
                            }

                            if (!table.getDBRoot().contains("TYPE_MODELE")) {
                                System.out.println("InstallationPanel.InstallationPanel() ADD TYPE_MODELE");
                                try {
                                    SQLUtils.executeAtomic(ds, new SQLUtils.SQLFactory<Object>() {
                                        @Override
                                        public Object create() throws SQLException {
                                            final SQLCreateTable createTypeModele = new SQLCreateTable(table.getDBRoot(), "TYPE_MODELE");
                                            createTypeModele.addVarCharColumn("NOM", 128);
                                            createTypeModele.addVarCharColumn("TABLE", 128);
                                            createTypeModele.addVarCharColumn("DEFAULT_MODELE", 128);
                                            ds.execute(createTypeModele.asString());

                                            insertUndef(createTypeModele);

                                            conf.getRoot().getSchema().updateVersion();

                                            conf.getRoot().refetch();

                                            return null;
                                        }
                                    });
                                    final String[] type = new String[] { "Avoir client", "AVOIR_CLIENT", "Avoir", "Bon de livraison", "BON_DE_LIVRAISON", "BonLivraison", "Commande Client",
                                            "COMMANDE_CLIENT", "CommandeClient", "Devis", "DEVIS", "Devis", "Facture", "SAISIE_VENTE_FACTURE", "VenteFacture" };
                                    // ('FR', 'Français', 1.000), ('EN', 'Anglais', 2.000)
                                    final List<String> values = new ArrayList<String>();
                                    final SQLBase base = table.getDBRoot().getBase();

                                    for (int i = 0; i < type.length; i += 3) {
                                        final int order = values.size() + 1;
                                        values.add("(" + base.quoteString(type[i]) + ", " + base.quoteString(type[i + 1]) + ", " + base.quoteString(type[i + 2]) + ", " + order + ")");
                                    }
                                    final String valuesStr = CollectionUtils.join(values, ", ");
                                    final String insertVals = "INSERT INTO " + conf.getRoot().getTable("TYPE_MODELE").getSQLName().quote() + "(" + SQLBase.quoteIdentifier("NOM") + ", "
                                            + SQLBase.quoteIdentifier("TABLE") + ", " + SQLBase.quoteIdentifier("DEFAULT_MODELE") + ", " + SQLBase.quoteIdentifier(SQLSyntax.ORDER_NAME) + ") VALUES"
                                            + valuesStr;

                                    ds.execute(insertVals);
                                } catch (Exception ex) {
                                    throw new IllegalStateException("Erreur lors de la création de la table TYPE_MODELE", ex);
                                }
                            }

                            // ///////////// updateSocieteTable(conf.getRoot());

                            // we need to upgrade all roots
                            // ///////////////////////////
                            conf.getSystemRoot().getRootsToMap().clear();
                            conf.getSystemRoot().refetch();

                            final Set<String> childrenNames = conf.getSystemRoot().getChildrenNames();

                            SwingUtilities.invokeLater(new Runnable() {

                                @Override
                                public void run() {
                                    bar.setIndeterminate(false);
                                    bar.setMaximum(childrenNames.size() + 1);
                                }
                            });
                            int i = 1;
                            for (final String childName : childrenNames) {
                                System.out.println("InstallationPanel.InstallationPanel() UPDATE SCHEMA " + childName);
                                final int barValue = i;
                                SwingUtilities.invokeLater(new Runnable() {

                                    @Override
                                    public void run() {
                                        bar.setValue(barValue);
                                    }
                                });
                                i++;
                                final DBRoot root = conf.getSystemRoot().getRoot(childName);
                                try {
                                    conf.getSystemRoot().getDataSource().execute("CREATE LANGUAGE plpgsql;");
                                } catch (Exception e) {
                                    System.err.println("Warning: cannot add language plpgsql" + e.getMessage());
                                }
                                final SQLTable tableUndef = root.getTable(SQLTable.undefTable);
                                if (tableUndef != null && tableUndef.getField("UNDEFINED_ID").isNullable() == Boolean.FALSE) {
                                    final AlterTable alterUndef = new AlterTable(tableUndef);
                                    alterUndef.alterColumn("TABLENAME", EnumSet.allOf(Properties.class), "varchar(250)", "''", false);
                                    alterUndef.alterColumn("UNDEFINED_ID", EnumSet.allOf(Properties.class), "int", null, true);
                                    try {
                                        ds.execute(alterUndef.asString());
                                        tableUndef.getSchema().updateVersion();
                                    } catch (SQLException ex) {
                                        throw new IllegalStateException("Erreur lors de la modification de UNDEFINED_ID", ex);
                                    }
                                }

                                if (DEBUG_FK) {
                                    findBadForeignKey(root);
                                }
                                if (childName.equalsIgnoreCase("Common")) {
                                    updateCommon(root);
                                } else if (childName.startsWith(conf.getAppName()) || childName.equalsIgnoreCase("Default")) {
                                    SQLUtils.executeAtomic(ds, new SQLUtils.SQLFactory<Object>() {
                                        @Override
                                        public Object create() throws SQLException {
                                            fixUnboundedVarchar(root);
                                            fixUnboundedNumeric(root);
                                            try {
                                                updateSocieteSchema(root);
                                            } catch (Exception e) {
                                                throw new SQLException(e);
                                            }
                                            updateToV1Dot2(root);
                                            return null;
                                        }
                                    });
                                }

                            }
                            error = false;
                        } catch (Throwable e1) {
                            ExceptionHandler.handle("Echec de mise à jour", e1);
                            error = true;
                        }

                        conf.destroy();
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                up.setEnabled(true);
                                bar.setValue(bar.getMaximum());
                                if (!error) {
                                    JOptionPane.showMessageDialog(InstallationPanel.this, "Mise à niveau réussie");
                                }
                            }
                        });

                    }
                }, "Database structure updater").start();

            }

        });
        if (finderPanel.getToken() == null) {
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
            // JOptionPane.showMessageDialog(InstallationPanel.this,
            // "Impossible de trouver le fichier "
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
            this.add(new JLabelBold("Paramètrages de la base de données"), c);
            c.gridy++;
            c.insets = DefaultGridBagConstraints.getDefaultInsets();
            this.add(new JLabel("Création des fonctions SQL nécessaires (plpgsql)."), c);
            c.gridy++;
            c.weightx = 0;
            c.anchor = GridBagConstraints.EAST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.NONE;

            JButton buttonPL = new JButton("Lancer");
            buttonPL.setOpaque(false);
            buttonPL.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!finderPanel.getServerConfig().getType().equals(ServerFinderConfig.POSTGRESQL)) {

                    } else {
                        final ComptaPropsConfiguration conf = ComptaPropsConfiguration.create(true);
                        try {
                            final SQLDataSource ds = conf.getSystemRoot().getDataSource();
                            // ds.execute("CREATE FUNCTION plpgsql_call_handler() RETURNS language_handler AS '$libdir/plpgsql' LANGUAGE C;"
                            // + "\n"
                            // +
                            // "CREATE FUNCTION plpgsql_validator(oid) RETURNS void AS '$libdir/plpgsql' LANGUAGE C;"
                            // + "\n"
                            // +
                            // "CREATE TRUSTED PROCEDURAL LANGUAGE plpgsql HANDLER plpgsql_call_handler VALIDATOR plpgsql_validator;");
                            ds.execute("CREATE LANGUAGE plpgsql;");

                        } catch (Exception ex) {
                            System.err.println("Impossible d'ajouter le langage PLPGSQL. Peut etre est il déjà installé.");
                        }
                    }
                    JOptionPane.showMessageDialog(null, "Paramètrage terminé.");
                }
            });
            this.add(buttonPL, c);
        }
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(10, 3, 2, 2);
        this.add(new JLabelBold("Mise à niveau de la base OpenConcerto"), c);
        c.gridy++;
        c.insets = DefaultGridBagConstraints.getDefaultInsets();
        this.add(new JLabel("Cette opération est nécessaire à chaque mise à jour du logiciel."), c);
        c.gridy++;
        this.add(new JLabel("La mise à niveau peut prendre plusieurs minutes."), c);
        c.gridy++;
        this.add(this.bar, c);
        c.gridy++;
        c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;

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

    private void fixUnboundedNumeric(DBRoot root) throws SQLException {

        final List<AlterTable> alters = new ArrayList<AlterTable>();
        {
            SQLTable tableAvoir = root.getTable("AVOIR_CLIENT_ELEMENT");
            final AlterTable alter = new AlterTable(tableAvoir);
            SQLField fieldAcompteAvoir = tableAvoir.getField("POURCENT_ACOMPTE");
            if (fieldAcompteAvoir.getType().getSize() > 500) {
                final String fName = fieldAcompteAvoir.getName();
                alter.alterColumn(fName, EnumSet.allOf(Properties.class), "numeric(6,2)", "100", false);
            }

            SQLField fieldRemiseAvoir = tableAvoir.getField("POURCENT_REMISE");
            if (fieldRemiseAvoir.getType().getSize() > 500) {
                final String fName = fieldRemiseAvoir.getName();
                alter.alterColumn(fName, EnumSet.allOf(Properties.class), "numeric(6,2)", "0", false);
            }

            if (!alter.isEmpty())
                alters.add(alter);
        }

        {
            SQLTable tableFacture = root.getTable("SAISIE_VENTE_FACTURE_ELEMENT");
            final AlterTable alter = new AlterTable(tableFacture);
            SQLField fieldAcompteFacture = tableFacture.getField("POURCENT_ACOMPTE");
            if (fieldAcompteFacture.getType().getSize() > 500) {
                final String fName = fieldAcompteFacture.getName();
                alter.alterColumn(fName, EnumSet.allOf(Properties.class), "numeric(6,2)", "100", false);
            }

            SQLField fieldRemiseFacture = tableFacture.getField("POURCENT_REMISE");
            if (fieldRemiseFacture.getType().getSize() > 500) {
                final String fName = fieldRemiseFacture.getName();
                alter.alterColumn(fName, EnumSet.allOf(Properties.class), "numeric(6,2)", "0", false);
            }

            if (tableFacture.getFieldsName().contains("REPARTITION_POURCENT")) {
                SQLField fieldRepFacture = tableFacture.getField("REPARTITION_POURCENT");
                if (fieldRepFacture.getType().getSize() > 500) {
                    final String fName = fieldRepFacture.getName();
                    alter.alterColumn(fName, EnumSet.allOf(Properties.class), "numeric(6,2)", "0", false);
                }
            }

            if (!alter.isEmpty())
                alters.add(alter);

        }
        if (alters.size() > 0) {
            final SQLDataSource ds = root.getDBSystemRoot().getDataSource();
            for (final String sql : ChangeTable.cat(alters, root.getName())) {
                ds.execute(sql);
            }
            root.refetch();
        }
    }

    private void fixUnboundedVarchar(DBRoot root) throws SQLException {
        final Set<String> namesSet = CollectionUtils.createSet("NOM", "PRENOM", "SURNOM", "LOGIN", "PASSWORD");
        final List<AlterTable> alters = new ArrayList<AlterTable>();
        for (final SQLTable t : root.getTables()) {
            final AlterTable alter = new AlterTable(t);
            for (final SQLField f : t.getFields()) {
                if (f.getType().getType() == Types.VARCHAR && f.getType().getSize() == Integer.MAX_VALUE) {
                    final String fName = f.getName();
                    final int size;
                    if (namesSet.contains(fName))
                        size = 128;
                    else if (fName.equals("TEL") || fName.startsWith("TEL_"))
                        size = 32;
                    else if (fName.contains("INFO"))
                        size = 2048;
                    else if (fName.contains("FORMULE"))
                        size = 1024;
                    else if (fName.equals("CONTENU"))
                        size = 2048;
                    else
                        size = 256;
                    alter.alterColumn(fName, EnumSet.allOf(Properties.class), "varchar(" + size + ")", "''", false);
                }
            }
            if (!alter.isEmpty())
                alters.add(alter);
        }
        if (alters.size() > 0) {
            final SQLDataSource ds = root.getDBSystemRoot().getDataSource();
            for (final String sql : ChangeTable.cat(alters, root.getName())) {
                ds.execute(sql);
            }
            root.refetch();
        }
    }

    private void updateToV1Dot2(final DBRoot root) throws SQLException {
        // bigint -> int ID_METRIQUE BON_DE_LIVRAISON_ELEMENT
        final SQLTable tableLivraisonElement = root.getTable("BON_DE_LIVRAISON_ELEMENT");
        AlterTable alter = new AlterTable(tableLivraisonElement);
        alter.alterColumn("ID_METRIQUE_2", EnumSet.of(Properties.TYPE), "integer", null, null);
        String req3 = alter.asString();
        root.getDBSystemRoot().getDataSource().execute(req3);

        final SQLTable tableDevis = root.getTable("DEVIS");
        final SQLDataSource ds = root.getDBSystemRoot().getDataSource();
        if (!tableDevis.getFieldsName().contains("DATE_VALIDITE")) {
            AlterTable t = new AlterTable(tableDevis);
            t.addColumn("DATE_VALIDITE", "date");
            try {
                ds.execute(t.asString());
                tableDevis.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout du champ DATE_VALIDITE à la table DEVIS", ex);
            }
        } else {
            AlterTable t = new AlterTable(tableDevis);
            t.alterColumn("DATE_VALIDITE", EnumSet.allOf(Properties.class), "date", null, true);
            try {
                ds.execute(t.asString());
                tableDevis.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout du champ DATE_VALIDITE à la table DEVIS", ex);
            }
        }

        final SQLTable tableEtatDevis = root.getTable("ETAT_DEVIS");
        if (tableEtatDevis.getRow(5) == null && tableEtatDevis.getRowCount() <= 4) {
            SQLRowValues rowVals = new SQLRowValues(tableEtatDevis);
            rowVals.put("NOM", "En cours de rédaction");
            rowVals.commit();
        }

        // Ajout de la TVA à 0
        SQLSelect selTVA = new SQLSelect(root.getBase());
        SQLTable tableTaxe = root.getTable("TAXE");
        selTVA.addSelect(tableTaxe.getKey(), "COUNT");
        selTVA.setWhere(new Where(tableTaxe.getField("TAUX"), "=", 0));
        Object result = root.getBase().getDataSource().executeScalar(selTVA.asString());
        if (result == null || ((Number) result).longValue() == 0) {
            SQLRowValues rowVals = new SQLRowValues(tableTaxe);
            rowVals.put("NOM", "Non applicable");
            rowVals.put("TAUX", Float.valueOf(0));
            rowVals.commit();
        }

        // Bon de livraison
        {
            SQLTable tableBL = root.getTable("BON_DE_LIVRAISON");
            boolean alterBL = false;
            AlterTable t = new AlterTable(tableBL);
            if (!tableBL.getFieldsName().contains("SOURCE")) {
                t.addVarCharColumn("SOURCE", 512);
                alterBL = true;
            }
            if (!tableBL.getFieldsName().contains("IDSOURCE")) {
                t.addColumn("IDSOURCE", "integer DEFAULT 1");
                alterBL = true;
            }

            if (!tableBL.getFieldsName().contains("DATE_LIVRAISON")) {
                t.addColumn("DATE_LIVRAISON", "date");
                alterBL = true;
            }
            if (alterBL) {
                try {
                    ds.execute(t.asString());
                    tableBL.getSchema().updateVersion();
                    tableBL.fetchFields();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table BON_DE_LIVRAISON", ex);
                }
            }
        }

        // Fournisseur
        {
            SQLTable tableBL = root.getTable("FOURNISSEUR");
            boolean alterBL = false;
            AlterTable t = new AlterTable(tableBL);
            if (!tableBL.getFieldsName().contains("ID_COMPTE_PCE_CHARGE")) {
                t.addForeignColumn("ID_COMPTE_PCE_CHARGE", root.getTable("COMPTE_PCE"));
                alterBL = true;
            }
            if (alterBL) {
                try {
                    ds.execute(t.asString());
                    tableBL.getSchema().updateVersion();
                    tableBL.fetchFields();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table FOURNISSEUR", ex);
                }
            }
        }

        // Numérotation
        {
            SQLTable tableNum = root.getTable("NUMEROTATION_AUTO");
            boolean alterNum = false;
            AlterTable t = new AlterTable(tableNum);
            if (!tableNum.getFieldsName().contains("AVOIR_F_START")) {
                t.addColumn("AVOIR_F_START", "integer DEFAULT 0");
                alterNum = true;
            }
            if (!tableNum.getFieldsName().contains("AVOIR_F_FORMAT")) {
                t.addVarCharColumn("AVOIR_F_FORMAT", 48);
                alterNum = true;
            }

            if (alterNum) {
                try {
                    ds.execute(t.asString());
                    tableNum.getSchema().updateVersion();
                    tableNum.fetchFields();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table NUMEROTATION_AUTO", ex);
                }
            }
        }

        SQLTable tableArticle = root.getTable("ARTICLE");

        AlterTable t = new AlterTable(tableArticle);
        boolean alterArticle = false;
        if (!tableArticle.getFieldsName().contains("QTE_ACHAT")) {
            t.addColumn("QTE_ACHAT", "integer DEFAULT 1");
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("DESCRIPTIF")) {
            t.addVarCharColumn("DESCRIPTIF", 2048);
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("CODE_BARRE")) {
            t.addVarCharColumn("CODE_BARRE", 256);
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("GESTION_STOCK")) {
            t.addColumn("GESTION_STOCK", "boolean DEFAULT true");
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("CODE_DOUANIER")) {
            t.addVarCharColumn("CODE_DOUANIER", 256);
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("QTE_MIN")) {
            t.addColumn("QTE_MIN", "integer DEFAULT 1");
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("ID_DEVISE")) {
            t.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("ID_FOURNISSEUR")) {
            t.addForeignColumn("ID_FOURNISSEUR", root.findTable("FOURNISSEUR", true));
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("PV_U_DEVISE")) {
            t.addColumn("PV_U_DEVISE", "bigint default 0");
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("ID_DEVISE_HA")) {
            t.addForeignColumn("ID_DEVISE_HA", root.findTable("DEVISE", true));
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("PA_DEVISE")) {
            t.addColumn("PA_DEVISE", "bigint default 0");
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("ID_PAYS")) {
            t.addForeignColumn("ID_PAYS", root.findTable("PAYS", true));
            alterArticle = true;
        }
        if (alterArticle) {
            try {
                ds.execute(t.asString());
                tableArticle.getSchema().updateVersion();
                tableArticle.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table ARTICLE", ex);
            }
        }

        // Création de la table Langue
        boolean refetchRoot = false;
        if (!root.contains("OBJECTIF_COMMERCIAL")) {

            SQLCreateTable createObjectif = new SQLCreateTable(root, "OBJECTIF_COMMERCIAL");
            createObjectif.addVarCharColumn("MOIS", 32);
            createObjectif.addColumn("ANNEE", "integer");
            createObjectif.addColumn("MARGE_HT", "bigint DEFAULT 0");
            createObjectif.addColumn("POURCENT_MARGE", "numeric (16,8)");
            createObjectif.addColumn("CHIFFRE_AFFAIRE", "bigint DeFAULT 0");
            createObjectif.addForeignColumn("COMMERCIAL");
            try {
                ds.execute(createObjectif.asString());
                insertUndef(createObjectif);
                tableDevis.getSchema().updateVersion();
                refetchRoot = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table OBJECTIF_COMMERCIAL", ex);
            }

        }

        if (!root.contains("LANGUE")) {

            SQLCreateTable createLangue = new SQLCreateTable(root, "LANGUE");
            createLangue.addVarCharColumn("CODE", 256);
            createLangue.addVarCharColumn("NOM", 256);
            createLangue.addVarCharColumn("CHEMIN", 256);
            try {
                ds.execute(createLangue.asString());
                insertUndef(createLangue);
                tableDevis.getSchema().updateVersion();
                root.refetch(createLangue.getName());
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table LANGUE", ex);
            }

            final String[] langs = new String[] { "FR", "Français", "EN", "Anglais", "SP", "Espagnol", "DE", "Allemand", "NL", "Néerlandais", "IT", "Italien" };
            // ('FR', 'Français', 1.000), ('EN', 'Anglais', 2.000)
            final List<String> values = new ArrayList<String>();
            final SQLBase base = root.getBase();
            for (int i = 0; i < langs.length; i += 2) {
                final int order = values.size() + 1;
                values.add("(" + base.quoteString(langs[i]) + ", " + base.quoteString(langs[i + 1]) + ", " + order + ")");
            }
            final String valuesStr = CollectionUtils.join(values, ", ");
            final String insertVals = "INSERT INTO " + getTableName(createLangue).quote() + "(" + SQLBase.quoteIdentifier("CODE") + ", " + SQLBase.quoteIdentifier("NOM") + ", "
                    + SQLBase.quoteIdentifier(SQLSyntax.ORDER_NAME) + ") VALUES" + valuesStr;
            ds.execute(insertVals);
        }

        // Création de la table Modéle
        if (!root.contains("MODELE")) {

            SQLCreateTable createModele = new SQLCreateTable(root, "MODELE");
            createModele.addVarCharColumn("NOM", 256);
            createModele.addForeignColumn("ID_TYPE_MODELE", root.findTable("TYPE_MODELE", true));
            try {
                ds.execute(createModele.asString());
                insertUndef(createModele);
                tableDevis.getSchema().updateVersion();
                refetchRoot = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table MODELE", ex);
            }
        }

        // Création de la table Modéle
        if (!root.contains("CONTACT_FOURNISSEUR")) {

            SQLCreateTable createModele = new SQLCreateTable(root, "CONTACT_FOURNISSEUR");
            createModele.addVarCharColumn("NOM", 256);
            createModele.addVarCharColumn("PRENOM", 256);
            createModele.addVarCharColumn("TEL_DIRECT", 256);
            createModele.addVarCharColumn("TEL_MOBILE", 256);
            createModele.addVarCharColumn("EMAIL", 256);
            createModele.addVarCharColumn("FAX", 256);
            createModele.addVarCharColumn("FONCTION", 256);
            createModele.addVarCharColumn("TEL_PERSONEL", 256);
            createModele.addVarCharColumn("TEL_STANDARD", 256);
            createModele.addForeignColumn("ID_TITRE_PERSONNEL", root.findTable("TITRE_PERSONNEL"));
            createModele.addForeignColumn("ID_FOURNISSEUR", root.findTable("FOURNISSEUR"));

            try {
                ds.execute(createModele.asString());
                insertUndef(createModele);
                tableDevis.getSchema().updateVersion();
                refetchRoot = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table MODELE", ex);
            }
        }

        // Création de la table Tarif
        if (!root.contains("TARIF")) {

            SQLCreateTable createTarif = new SQLCreateTable(root, "TARIF");
            createTarif.addVarCharColumn("NOM", 256);
            createTarif.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
            createTarif.addForeignColumn("ID_TAXE", root.findTable("TAXE", true));
            createTarif.asString();
            try {
                ds.execute(createTarif.asString());
                insertUndef(createTarif);
                tableDevis.getSchema().updateVersion();
                root.refetch(createTarif.getName());
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table TARIF", ex);
            }
        }

        // Création de la table article Tarif
        if (!root.contains("ARTICLE_TARIF")) {

            SQLCreateTable createTarif = new SQLCreateTable(root, "ARTICLE_TARIF");
            createTarif.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
            createTarif.addForeignColumn("ID_TAXE", root.findTable("TAXE", true));
            createTarif.addForeignColumn("ID_TARIF", root.findTable("TARIF", true));
            createTarif.addForeignColumn("ID_ARTICLE", root.findTable("ARTICLE", true));
            createTarif.addColumn("PV_HT", "bigint DEFAULT 0");
            createTarif.addColumn("PV_TTC", "bigint DEFAULT 0");
            createTarif.addColumn("PRIX_METRIQUE_VT_1", "bigint DEFAULT 0");
            createTarif.addColumn("PRIX_METRIQUE_VT_2", "bigint DEFAULT 0");
            createTarif.addColumn("PRIX_METRIQUE_VT_3", "bigint DEFAULT 0");
            createTarif.asString();
            try {
                ds.execute(createTarif.asString());
                insertUndef(createTarif);
                tableDevis.getSchema().updateVersion();
                refetchRoot = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table ARTICLE_TARIF", ex);
            }
        }

        // Création de la table article Désignation
        if (!root.contains("ARTICLE_DESIGNATION")) {

            SQLCreateTable createTarif = new SQLCreateTable(root, "ARTICLE_DESIGNATION");
            createTarif.addForeignColumn("ID_ARTICLE", root.findTable("ARTICLE", true));
            createTarif.addForeignColumn("ID_LANGUE", root.findTable("LANGUE", true));
            createTarif.addVarCharColumn("NOM", 1024);
            createTarif.asString();
            try {
                ds.execute(createTarif.asString());
                insertUndef(createTarif);
                tableDevis.getSchema().updateVersion();
                refetchRoot = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table ARTICLE_DESIGNATION", ex);
            }
        }

        if (!root.contains("UNITE_VENTE")) {

            SQLCreateTable createUnite = new SQLCreateTable(root, "UNITE_VENTE");
            createUnite.addVarCharColumn("CODE", 32);
            createUnite.addVarCharColumn("NOM", 256);
            createUnite.addColumn("A_LA_PIECE", "boolean DEFAULT false");
            createUnite.addVarCharColumn("INFOS", 256);
            try {
                ds.execute(createUnite.asString());
                insertUndef(createUnite);
                final String insert = "INSERT into "
                        + getTableName(createUnite).quote()
                        + "(\"CODE\",\"NOM\",\"A_LA_PIECE\",\"ORDRE\") VALUES('pièce','à la pièce',true,1),('m','mètres',false,2),('m²','mètres carré',false,3),('m3','mètres cube',false,4),('l','litres',false,5),('kg','kilos',false,6),('h','heures',false,7),('j','jours',false,8),('mois','mois',false,9)";
                root.getDBSystemRoot().getDataSource().execute(insert);
                tableDevis.getSchema().updateVersion();
                refetchRoot = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table UNITE_VENTE", ex);
            }

        }

        // Chargement des tables fraichement créées
        if (refetchRoot)
            root.refetch();

        if (!tableArticle.getFieldsName().contains("ID_UNITE_VENTE")) {
            AlterTable alterTableArticle = new AlterTable(tableArticle);
            alterTableArticle.addForeignColumn("ID_UNITE_VENTE", root.findTable("UNITE_VENTE", true).getSQLName(), "ID", "2");
            try {
                ds.execute(alterTableArticle.asString());
                tableArticle.getSchema().updateVersion();
                tableArticle.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout du champ UNITE_VENTE sur la table ARTICLE", ex);
            }
        }

        SQLTable tableVFElt = root.getTable("SAISIE_VENTE_FACTURE_ELEMENT");
        addVenteEltField(tableVFElt, root);

        SQLTable tableDevisElt = root.getTable("DEVIS_ELEMENT");
        addVenteEltField(tableDevisElt, root);

        SQLTable tableCmdElt = root.getTable("COMMANDE_CLIENT_ELEMENT");
        addVenteEltField(tableCmdElt, root);

        SQLTable tableBonElt = root.getTable("BON_DE_LIVRAISON_ELEMENT");
        addVenteEltField(tableBonElt, root);

        SQLTable tableAvoirElt = root.getTable("AVOIR_CLIENT_ELEMENT");
        addVenteEltField(tableAvoirElt, root);

        SQLTable tableCmdFournElt = root.getTable("COMMANDE_ELEMENT");
        addHAElementField(tableCmdFournElt, root);

        SQLTable tableBonRecptElt = root.getTable("BON_RECEPTION_ELEMENT");
        addHAElementField(tableBonRecptElt, root);

        SQLTable tableBonRecpt = root.getTable("BON_RECEPTION");
        addDeviseHAField(tableBonRecpt, root);

        SQLTable tableCommande = root.getTable("COMMANDE");
        addDeviseHAField(tableCommande, root);

        if (!tableCommande.getFieldsName().contains("ID_ADRESSE")) {
            AlterTable alterCmd = new AlterTable(tableCommande);
            alterCmd.addForeignColumn("ID_ADRESSE", root.findTable("ADRESSE", true));
            try {
                ds.execute(alterCmd.asString());
                tableCommande.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table COMMANDE", ex);
            }

        }
        if (!tableCommande.getFieldsName().contains("ID_CLIENT")) {
            AlterTable alterCmd = new AlterTable(tableCommande);
            alterCmd.addForeignColumn("ID_CLIENT", root.findTable("CLIENT"));
            try {
                ds.execute(alterCmd.asString());
                tableCommande.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table COMMANDE", ex);
            }

        }

        {
            addTotalDeviseField(tableDevis, root);
            addModeleField(tableDevis, root);

            SQLTable tableVF = root.getTable("SAISIE_VENTE_FACTURE");
            addTotalDeviseField(tableVF, root);
            addModeleField(tableVF, root);

            addTotalDeviseField(tableDevis, root);
            addModeleField(tableDevis, root);

            SQLTable tableCmd = root.getTable("COMMANDE_CLIENT");
            addTotalDeviseField(tableCmd, root);
            addModeleField(tableCmd, root);

            SQLTable tableBon = root.getTable("BON_DE_LIVRAISON");
            addTotalDeviseField(tableBon, root);
            addModeleField(tableBon, root);

            SQLTable tableAvoir = root.getTable("AVOIR_CLIENT");
            addTotalDeviseField(tableAvoir, root);
            addModeleField(tableAvoir, root);
        }
        // Change client
        {
            SQLTable tableClient = root.getTable("CLIENT");

            AlterTable tClient = new AlterTable(tableClient);
            boolean alterClient = false;

            if (!tableClient.getFieldsName().contains("ID_TARIF")) {
                tClient.addForeignColumn("ID_TARIF", root.findTable("TARIF", true));
                alterClient = true;
            }
            if (!tableClient.getFieldsName().contains("ID_PAYS")) {
                tClient.addForeignColumn("ID_PAYS", root.findTable("PAYS", true));
                alterClient = true;
            }
            if (!tableClient.getFieldsName().contains("ID_LANGUE")) {
                tClient.addForeignColumn("ID_LANGUE", root.findTable("LANGUE", true));
                alterClient = true;
            }

            if (!tableClient.getFieldsName().contains("ID_DEVISE")) {
                tClient.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
                alterClient = true;
            }
            if (alterClient) {
                try {
                    ds.execute(tClient.asString());
                    tableClient.getSchema().updateVersion();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table CLIENT", ex);
                }
            }
        }

        // Change Pays
        {
            SQLTable tablePays = root.getTable("PAYS");

            AlterTable tPays = new AlterTable(tablePays);
            boolean alterPays = false;

            if (!tablePays.getFieldsName().contains("ID_TARIF")) {
                tPays.addForeignColumn("ID_TARIF", root.findTable("TARIF", true));
                alterPays = true;
            }
            if (!tablePays.getFieldsName().contains("ID_LANGUE")) {
                tPays.addForeignColumn("ID_LANGUE", root.findTable("LANGUE", true));
                alterPays = true;
            }
            if (alterPays) {
                try {
                    ds.execute(tPays.asString());
                    tablePays.getSchema().updateVersion();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table PAYS", ex);
                }
            }
        }
        // Change Commande
        {
            SQLTable tableCmd = root.getTable("COMMANDE");

            AlterTable tCmd = new AlterTable(tableCmd);
            boolean alterCmd = false;

            if (!tableCmd.getFieldsName().contains("EN_COURS")) {
                tCmd.addColumn("EN_COURS", "boolean default true");
                alterCmd = true;
            }
            if (alterCmd) {
                try {
                    ds.execute(tCmd.asString());
                    tableCmd.getSchema().updateVersion();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table COMMANDE", ex);
                }
            }
        }
        // Change Fournisseur
        {
            SQLTable tableFournisseur = root.getTable("FOURNISSEUR");

            AlterTable tFourn = new AlterTable(tableFournisseur);
            boolean alterFourn = false;

            if (!tableFournisseur.getFieldsName().contains("ID_LANGUE")) {
                tFourn.addForeignColumn("ID_LANGUE", root.findTable("LANGUE", true));
                alterFourn = true;
            }
            if (!tableFournisseur.getFieldsName().contains("ID_DEVISE")) {
                tFourn.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
                alterFourn = true;
            }
            if (!tableFournisseur.getFieldsName().contains("RESPONSABLE")) {
                tFourn.addVarCharColumn("RESPONSABLE", 256);
                alterFourn = true;
            }
            if (!tableFournisseur.getFieldsName().contains("TEL_P")) {
                tFourn.addVarCharColumn("TEL_P", 256);
                alterFourn = true;
            }
            if (!tableFournisseur.getFieldsName().contains("MAIL")) {
                tFourn.addVarCharColumn("MAIL", 256);
                alterFourn = true;
            }
            if (!tableFournisseur.getFieldsName().contains("INFOS")) {
                tFourn.addVarCharColumn("INFOS", 2048);
                alterFourn = true;
            }

            if (alterFourn) {
                try {
                    ds.execute(tFourn.asString());
                    tableFournisseur.getSchema().updateVersion();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table FOURNISSEUR", ex);
                }
            }
        }

        updateN4DS(root);

        root.refetch();
    }

    /**
     * Mise à jour du schéma pour N4DS
     * 
     * @param root
     * @throws SQLException
     */
    private void updateN4DS(DBRoot root) throws SQLException {

        {
            SQLTable table = root.findTable("INFOS_SALARIE_PAYE");
            boolean alter = false;
            AlterTable t = new AlterTable(table);
            if (!table.getFieldsName().contains("CODE_AT")) {
                t.addVarCharColumn("CODE_AT", 18);
                alter = true;
            }
            if (!table.getFieldsName().contains("CODE_SECTION_AT")) {
                t.addVarCharColumn("CODE_SECTION_AT", 18);
                alter = true;
            }

            if (alter) {
                try {
                    table.getBase().getDataSource().execute(t.asString());
                    table.getSchema().updateVersion();
                    table.fetchFields();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
                }
            }

        }

        if (!root.contains("CODE_STATUT_CAT_CONV")) {

            SQLCreateTable createTarif = new SQLCreateTable(root, "CODE_STATUT_CAT_CONV");

            createTarif.addVarCharColumn("CODE", 6);
            createTarif.addVarCharColumn("NOM", 256);
            createTarif.asString();
            try {
                root.getBase().getDataSource().execute(createTarif.asString());
                insertUndef(createTarif);

                String insert = "INSERT into " + getTableName(createTarif).quote() + "(\"CODE\",\"NOM\") VALUES ";
                insert += " ('01','agriculteur salarié de son exploitation')";
                insert += ", ('02','artisan ou commerçant salarié de son entreprise')";
                insert += ", ('03','cadre dirigeant (votant au collège employeur des élections prud''''hommales)')";
                insert += ", ('04','autres cadres au sens de la convention collective (ou du statut pour les régimes spéciaux)')";
                insert += ", ('05','profession intermédiaire (technicien, contremaître, agent de maîtrise, clergé)')";
                insert += ", ('06','employé administratif d''''entreprise, de commerce, agent de service')";
                insert += ", ('07','ouvriers qualifiés et non qualifiés y compris ouvriers agricoles');";
                createTarif.getRoot().getDBSystemRoot().getDataSource().execute(insert);

                root.getSchema().updateVersion();
                root.refetch();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table CODE_STATUT_CAT_CONV", ex);
            }
        }

        // Création de la table Modéle
        if (!root.contains("CONTACT_ADMINISTRATIF")) {

            SQLCreateTable createModele = new SQLCreateTable(root, "CONTACT_ADMINISTRATIF");
            createModele.addVarCharColumn("NOM", 256);
            createModele.addVarCharColumn("PRENOM", 256);
            createModele.addVarCharColumn("TEL_DIRECT", 256);
            createModele.addVarCharColumn("TEL_MOBILE", 256);
            createModele.addVarCharColumn("EMAIL", 256);
            createModele.addVarCharColumn("FAX", 256);
            createModele.addVarCharColumn("FONCTION", 256);
            createModele.addVarCharColumn("TEL_PERSONEL", 256);
            createModele.addVarCharColumn("TEL_STANDARD", 256);
            createModele.addForeignColumn("ID_TITRE_PERSONNEL", root.findTable("TITRE_PERSONNEL"));
            createModele.addColumn("N4DS", "boolean DEFAULT false");

            try {
                root.getBase().getDataSource().execute(createModele.asString());
                insertUndef(createModele);
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table MODELE", ex);
            }
        }

        {
            SQLTable tableContrat = root.findTable("CONTRAT_SALARIE", true);
            boolean alter2 = false;
            AlterTable t2 = new AlterTable(tableContrat);
            // UGRR
            if (!tableContrat.getFieldsName().contains("CODE_IRC_UGRR")) {
                t2.addVarCharColumn("CODE_IRC_UGRR", 18);
                alter2 = true;
            }
            if (!tableContrat.getFieldsName().contains("NUMERO_RATTACHEMENT_UGRR")) {
                t2.addVarCharColumn("NUMERO_RATTACHEMENT_UGRR", 64);
                alter2 = true;
            }
            // UGRC
            if (!tableContrat.getFieldsName().contains("CODE_IRC_UGRC")) {
                t2.addVarCharColumn("CODE_IRC_UGRC", 18);
                alter2 = true;
            }
            if (!tableContrat.getFieldsName().contains("NUMERO_RATTACHEMENT_UGRC")) {
                t2.addVarCharColumn("NUMERO_RATTACHEMENT_UGRC", 64);
                alter2 = true;
            }

            // Retraite Compl
            if (!tableContrat.getFieldsName().contains("CODE_IRC_RETRAITE")) {
                t2.addVarCharColumn("CODE_IRC_RETRAITE", 18);
                alter2 = true;
            }
            if (!tableContrat.getFieldsName().contains("NUMERO_RATTACHEMENT_RETRAITE")) {
                t2.addVarCharColumn("NUMERO_RATTACHEMENT_RETRAITE", 64);
                alter2 = true;
            }

            if (!tableContrat.getFieldsName().contains("ID_CODE_STATUT_CAT_CONV")) {
                t2.addForeignColumn("ID_CODE_STATUT_CAT_CONV", root.findTable("CODE_STATUT_CAT_CONV", true));
                alter2 = true;
            }

            if (alter2) {
                try {
                    tableContrat.getBase().getDataSource().execute(t2.asString());
                    tableContrat.getSchema().updateVersion();
                    tableContrat.fetchFields();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + tableContrat.getName(), ex);
                }
            }

        }
    }

    private void addDeviseHAField(SQLTable table, DBRoot root) throws SQLException {
        boolean alter = false;
        AlterTable t = new AlterTable(table);
        if (!table.getFieldsName().contains("ID_DEVISE")) {
            t.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
            alter = true;
        }

        if (!table.getFieldsName().contains("T_DEVISE")) {
            t.addColumn("T_DEVISE", "bigint default 0");
            alter = true;
        }

        if (alter) {
            try {
                table.getBase().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
            }
        }

    }

    private void addHAElementField(SQLTable table, DBRoot root) throws SQLException {
        boolean alter = false;
        AlterTable t = new AlterTable(table);
        if (!table.getFieldsName().contains("QTE_ACHAT")) {
            t.addColumn("QTE_ACHAT", "integer DEFAULT 1");
            alter = true;
        }
        if (!table.getFieldsName().contains("QTE_UNITAIRE")) {
            t.addColumn("QTE_UNITAIRE", "numeric(16,6) DEFAULT 1");
            alter = true;
        }
        if (!table.getFieldsName().contains("ID_UNITE_VENTE")) {
            t.addForeignColumn("ID_UNITE_VENTE", root.findTable("UNITE_VENTE", true).getSQLName(), "ID", "2");
            alter = true;
        }
        if (!table.getFieldsName().contains("ID_ARTICLE")) {
            t.addForeignColumn("ID_ARTICLE", root.findTable("ARTICLE", true));
            alter = true;
        }
        if (!table.getFieldsName().contains("PA_DEVISE")) {
            t.addColumn("PA_DEVISE", "bigint default 0");
            alter = true;
        }
        if (!table.getFieldsName().contains("ID_DEVISE")) {
            t.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
            alter = true;
        }

        if (!table.getFieldsName().contains("PA_DEVISE_T")) {
            t.addColumn("PA_DEVISE_T", "bigint default 0");
            alter = true;
        }

        if (alter) {
            try {
                table.getBase().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
            }
        }

    }

    private void addModeleField(SQLTable table, DBRoot root) throws SQLException {
        boolean alter = false;
        AlterTable t = new AlterTable(table);
        if (!table.getFieldsName().contains("ID_MODELE")) {
            t.addForeignColumn("ID_MODELE", root.findTable("MODELE"));
            alter = true;
        }

        if (alter) {
            try {
                table.getBase().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
            }
        }
    }

    private void addTotalDeviseField(SQLTable table, DBRoot root) throws SQLException {
        boolean alter = false;
        AlterTable t = new AlterTable(table);
        if (!table.getFieldsName().contains("T_DEVISE")) {
            t.addColumn("T_DEVISE", "bigint default 0");
            alter = true;
        } else {
            table.getBase().getDataSource().execute("UPDATE " + table.getSQLName().quote() + " SET \"T_DEVISE\"=0 WHERE \"T_DEVISE\" IS NULL");
            t.alterColumn("T_DEVISE", EnumSet.allOf(Properties.class), "bigint", "0", false);
        }
        if (!table.getFieldsName().contains("T_POIDS")) {
            t.addColumn("T_POIDS", "real default 0");
            alter = true;
        }
        if (!table.getFieldsName().contains("ID_TARIF")) {
            t.addForeignColumn("ID_TARIF", root.findTable("TARIF", true));
            alter = true;
        }

        if (alter) {
            try {
                table.getBase().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
            }
        }
    }

    private void addVenteEltField(SQLTable table, DBRoot root) throws SQLException {

        boolean alter = false;
        AlterTable t = new AlterTable(table);
        if (!table.getFieldsName().contains("QTE_ACHAT")) {
            t.addColumn("QTE_ACHAT", "integer DEFAULT 1");
            alter = true;
        }
        if (!table.getFieldsName().contains("QTE_UNITAIRE")) {
            t.addColumn("QTE_UNITAIRE", "numeric(16,6) DEFAULT 1");
            alter = true;
        }
        if (!table.getFieldsName().contains("ID_UNITE_VENTE")) {
            t.addForeignColumn("ID_UNITE_VENTE", root.findTable("UNITE_VENTE", true).getSQLName(), "ID", "2");
            alter = true;
        }
        if (!table.getFieldsName().contains("ID_ARTICLE")) {
            t.addForeignColumn("ID_ARTICLE", root.findTable("ARTICLE", true));
            alter = true;
        }
        if (!table.getFieldsName().contains("CODE_DOUANIER")) {
            t.addVarCharColumn("CODE_DOUANIER", 256);
            alter = true;
        }
        if (!table.getFieldsName().contains("DESCRIPTIF")) {
            t.addVarCharColumn("DESCRIPTIF", 2048);
            alter = true;
        }
        if (!table.getFieldsName().contains("ID_PAYS")) {
            t.addForeignColumn("ID_PAYS", root.findTable("PAYS", true));
            alter = true;
        }
        if (!table.getFieldsName().contains("MARGE_HT")) {
            t.addColumn("MARGE_HT", "bigint default 0");
            alter = true;
        }

        if (!table.getFieldsName().contains("ID_DEVISE")) {
            t.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
            alter = true;
        }
        if (!table.getFieldsName().contains("PV_U_DEVISE")) {
            t.addColumn("PV_U_DEVISE", "bigint default 0");
            alter = true;
        }
        if (!table.getFieldsName().contains("POURCENT_REMISE")) {
            t.addColumn("POURCENT_REMISE", "numeric(6,2) default 0");
            alter = true;
        }
        if (!table.getFieldsName().contains("PV_T_DEVISE")) {
            t.addColumn("PV_T_DEVISE", "bigint default 0");
            alter = true;
        }
        if (!table.getFieldsName().contains("TAUX_DEVISE")) {
            t.addColumn("TAUX_DEVISE", "numeric (16,8) DEFAULT 1");
            alter = true;
        }
        if (alter) {
            try {
                root.getDBSystemRoot().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
            }
        }
    }

    private void updateSocieteSchema(final DBRoot root) throws IOException, Exception {
        final DBSystemRoot sysRoot = root.getDBSystemRoot();
        final SQLDataSource ds = sysRoot.getDataSource();
        System.out.println("InstallationPanel.InstallationPanel() UPDATE COMMERCIAL " + root);
        // Fix commercial Ordre

        SQLTable tableCommercial = root.getTable("COMMERCIAL");
        CorrectOrder orderCorrect = new CorrectOrder(sysRoot);
        orderCorrect.change(tableCommercial);

        new AddFK(sysRoot).change(root);
        root.getSchema().updateVersion();
        root.refetch();
        // load graph now so that it's coherent with the structure
        // that way we can add foreign columns after without refreshing
        // 1. root.refetch() clears the graph
        // 2. we add some foreign field (the graph is still null)
        // 3. we use a method that needs the graph
        // 4. the graph is created and throws an exception when it wants to use the new field not in
        // the structure
        sysRoot.getGraph();

        try {
            // Add article
            final SQLTable tableArticle = root.getTable("ARTICLE");
            if (!tableArticle.getFieldsName().contains("INFOS")) {
                AlterTable t = new AlterTable(tableArticle);
                t.addVarCharColumn("INFOS", 2048);
                try {
                    ds.execute(t.asString());
                } catch (Exception ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout du champ INFO à la table ARTICLE", ex);
                }
            }

            if (sysRoot.getServer().getSQLSystem().equals(SQLSystem.POSTGRESQL)) {
                // Fix Caisse serial
                SQLTable tableCaisse = root.getTable("CAISSE");

                FixSerial f = new FixSerial(sysRoot);
                try {
                    f.change(tableCaisse);
                } catch (SQLException e2) {
                    throw new IllegalStateException("Erreur lors la mise à jours des sequences de la table CAISSE", e2);
                }
            }
            System.out.println("InstallationPanel.InstallationPanel() UPDATE TICKET_CAISSE " + root);
            // add Mvt on Ticket
            SQLTable tableTicket = root.getTable("TICKET_CAISSE");
            if (!tableTicket.getFieldsName().contains("ID_MOUVEMENT")) {
                AlterTable t = new AlterTable(tableTicket);
                t.addForeignColumn("ID_MOUVEMENT", root.getTable("MOUVEMENT"));
                try {
                    ds.execute(t.asString());
                } catch (Exception ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout du champ ID_MOUVEMENT à la table TICKET_CAISSE", ex);
                }
            }

            // Check type de reglement

            System.out.println("InstallationPanel.InstallationPanel() UPDATE TYPE_REGLEMENT " + root);
            SQLTable tableReglmt = root.getTable("TYPE_REGLEMENT");
            SQLSelect sel = new SQLSelect(tableReglmt.getBase());
            sel.addSelect(tableReglmt.getKey());
            sel.setWhere(new Where(tableReglmt.getField("NOM"), "=", "Virement"));
            List<Number> l = (List<Number>) ds.executeCol(sel.asString());
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
            List<Number> l2 = (List<Number>) ds.executeCol(sel2.asString());
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
            System.out.println("InstallationPanel.InstallationPanel() UPDATE FAMILLE_ARTICLE " + root);
            //
            final SQLTable tableFam = root.getTable("FAMILLE_ARTICLE");
            final int nomSize = 256;
            if (tableFam.getField("NOM").getType().getSize() < nomSize) {
                final AlterTable t = new AlterTable(tableFam);
                t.alterColumn("NOM", EnumSet.allOf(Properties.class), "varchar(" + nomSize + ")", "''", false);
                try {
                    ds.execute(t.asString());
                } catch (Exception ex) {
                    throw new IllegalStateException("Erreur lors de la modification du champs NOM sur la table FAMILLE_ARTICLE", ex);
                }
            }

            // Suppression des champs 1.0
            System.out.println("InstallationPanel.InstallationPanel() UPDATE FROM 1.0 " + root);
            final List<ChangeTable<?>> changes = new ArrayList<ChangeTable<?>>();
            List<String> tablesToRemove = new ArrayList<String>();
            tablesToRemove.add("AFFAIRE");
            tablesToRemove.add("AFFAIRE_ELEMENT");
            tablesToRemove.add("RAPPORT");
            tablesToRemove.add("CODE_MISSION");
            tablesToRemove.add("FICHE_RENDEZ_VOUS");
            tablesToRemove.add("NATURE_MISSION");
            tablesToRemove.add("AVIS_INTERVENTION");
            tablesToRemove.add("POURCENT_CCIP");
            tablesToRemove.add("SECRETAIRE");
            tablesToRemove.add("FICHE_RENDEZ_VOUS_ELEMENT");
            tablesToRemove.add("POURCENT_SERVICE");
            tablesToRemove.add("PROPOSITION");
            tablesToRemove.add("PROPOSITION_ELEMENT");
            tablesToRemove.add("POLE_PRODUIT");
            tablesToRemove.add("BANQUE_POLE_PRODUIT");
            tablesToRemove.add("AFFACTURAGE");
            tablesToRemove.add("SECTEUR_ACTIVITE");

            //
            final ModuleManager instance = new ModuleManager();
            instance.setRoot(root);
            final Set<String> modulesIds = instance.getDBInstalledModules().keySet();
            final Set<String> allUsedTable = new HashSet<String>();
            for (String id : modulesIds) {
                Set<String> tableNames = instance.getCreatedItems(id).get0();
                allUsedTable.addAll(tableNames);
            }
            System.out.println("Tables used by modules:" + allUsedTable);
            final DatabaseGraph graph = sysRoot.getGraph();
            for (String tableName : tablesToRemove) {
                if (!allUsedTable.contains(tableName) && root.contains(tableName)) {

                    final SQLTable table = root.getTable(tableName);
                    for (final Link link : graph.getReferentLinks(table)) {
                        if (!(link.getSource().getDBRoot() == root && tablesToRemove.contains(link.getSource().getName()))) {
                            final AlterTable alter = new AlterTable(link.getSource());
                            alter.dropForeignColumns(link);
                            changes.add(alter);
                        }
                    }
                    changes.add(new DropTable(table));
                }
            }

            final List<String> alterRequests = ChangeTable.cat(changes, root.getName());
            try {
                for (final String req : alterRequests) {
                    ds.execute(req);
                }
            } catch (Exception e1) {
                throw new IllegalStateException("Erreur lors de la mise à jour des tables v1.0", e1);
            }
            System.out.println("InstallationPanel.InstallationPanel() UPDATE CAISSE " + root);
            // Undefined
            try {
                SQLTable.setUndefID(tableTicket.getSchema(), tableTicket.getName(), 1);
                SQLTable.setUndefID(tableTicket.getSchema(), "CAISSE", 1);
            } catch (SQLException e1) {
                throw new IllegalStateException("Erreur lors de la mise à jour des indéfinis de la table CAISSE", e1);
            }
        } finally {
            // Mise à jour du schéma
            root.getSchema().updateVersion();
            root.refetch();
        }
    }

    private void findBadForeignKey(DBRoot root) {
        Set<SQLTable> tables = root.getTables();
        for (SQLTable table : tables) {
            findBadForeignKey(root, table);
        }

    }

    private void findBadForeignKey(DBRoot root, SQLTable table) {
        System.out.println("====================================== " + table.getName());
        Set<SQLField> ffields = table.getForeignKeys();
        Set<SQLField> allFields = table.getFields();

        Set<String> keysString = SQLKey.foreignKeys(table);
        for (String string : keysString) {
            ffields.add(table.getField(string));
        }

        if (ffields.size() == 0) {
            System.out.println("No foreign fields");
        }
        System.out.println("Foreign field for table " + table.getName() + ":" + ffields);
        // Map Champs-> Table sur lequel il pointe
        Map<SQLField, SQLTable> map = new HashMap<SQLField, SQLTable>();
        Set<SQLTable> extTables = new HashSet<SQLTable>();
        for (SQLField sqlField : ffields) {
            SQLTable t = null;
            try {
                t = SQLKey.keyToTable(sqlField);
            } catch (Exception e) {
                System.out.println("Ignoring field:" + sqlField.getName());
            }
            if (t == null) {
                System.out.println("Unable to find table for ff " + sqlField.getName());
            } else {
                extTables.add(t);
                map.put(sqlField, t);
            }
        }
        // Verification des datas
        System.out.println("Foreign table for table " + table.getName() + ":" + extTables);
        // Recupere les ids de toutes les tables
        Map<SQLTable, Set<Number>> ids = getIdsForTables(extTables);

        //
        SQLSelect s = new SQLSelect(root.getBase(), true);
        if (table.getPrimaryKeys().size() != 1) {
            return;
        }
        s.addSelect(table.getKey());
        for (SQLField sqlField : map.keySet()) {
            s.addSelect(sqlField);
        }
        List<Map> result = root.getDBSystemRoot().getDataSource().execute(s.asString());
        for (Map resultRow : result) {

            // Pour toutes les lignes
            Set<String> fields = resultRow.keySet();
            for (String field : fields) {
                // Pour tous les champs
                SQLField fField = table.getField(field);
                if (table.getPrimaryKeys().contains(fField)) {
                    continue;
                }
                SQLTable fTable = map.get(fField);
                if (fTable == null) {
                    System.out.println("Error: null table for field" + field);
                    continue;
                }
                Set<Number> values = ids.get(fTable);

                final Object id = resultRow.get(field);
                if (id == null) {
                    continue;
                } else if (!values.contains((Number) id)) {
                    System.out.println("Checking row " + resultRow);
                    System.out.println("Error: No id found in table " + fTable.getName() + " for row " + field + "in table " + table.getName() + " " + resultRow + " knowns id:" + values);
                }
            }
        }
        System.out.println("======================================\n");
    }

    private Map<SQLTable, Set<Number>> getIdsForTables(Set<SQLTable> extTables) {
        Map<SQLTable, Set<Number>> result = new HashMap<SQLTable, Set<Number>>();
        for (SQLTable sqlTable : extTables) {
            result.put(sqlTable, getIdsForTable(sqlTable));
        }
        return result;
    }

    private Set<Number> getIdsForTable(SQLTable table) {
        final DBRoot dbRoot = table.getDBRoot();
        SQLSelect s = new SQLSelect(dbRoot.getBase(), true);
        s.addSelect(table.getKey());
        List<Number> result = dbRoot.getDBSystemRoot().getDataSource().executeCol(s.asString());
        return new HashSet<Number>(result);
    }

    private void updateCommon(DBRoot root) throws SQLException {

        // rm ID 43 - 47 de SOCIETE_COMMON
        final SQLTable tableSociete = root.getTable("SOCIETE_COMMON");
        String req3 = "DELETE FROM " + tableSociete.getSQLName().quote() + " WHERE ";
        req3 += new Where(tableSociete.getKey(), 43, 47).getClause();
        root.getDBSystemRoot().getDataSource().execute(req3);

        // rm ID 3 à 49 de EXERCICE_COMMON
        final SQLTable tableExercice = root.getTable("EXERCICE_COMMON");
        String req1a = "DELETE FROM " + tableExercice.getSQLName().quote() + " WHERE ";
        req1a += new Where(tableExercice.getKey(), 3, 49).getClause();
        root.getDBSystemRoot().getDataSource().execute(req1a);
        // et 53-57
        root.getDBSystemRoot().getDataSource().execute(req1a);
        String req1b = "DELETE FROM " + tableExercice.getSQLName().quote() + " WHERE ";
        req1b += new Where(tableExercice.getKey(), 53, 57).getClause();
        root.getDBSystemRoot().getDataSource().execute(req1b);
        //

        // TACHE_COMMON, ID_USER_COMMON_*=0 -> 1
        for (final String f : Arrays.asList("ID_USER_COMMON_TO", "ID_USER_COMMON_CREATE", "ID_USER_COMMON_ASSIGN_BY")) {
            final SQLTable tableTache = root.getTable("TACHE_COMMON");
            final UpdateBuilder updateBuilder = new UpdateBuilder(tableTache);
            updateBuilder.set(f, "1").setWhere(new Where(tableTache.getField(f), "=", 0));
            String req2 = updateBuilder.asString();
            root.getDBSystemRoot().getDataSource().execute(req2);
        }

        // FK
        new AddFK(root.getDBSystemRoot()).change(root);
    }

    private void updateSocieteTable(DBRoot root) throws SQLException {
        SQLTable table = root.findTable("SOCIETE_COMMON");
        boolean alter = false;
        AlterTable t = new AlterTable(table);
        if (!table.getFieldsName().contains("RCS")) {
            t.addVarCharColumn("RCS", 256);
            alter = true;
        }

        if (!table.getFieldsName().contains("CAPITAL")) {
            t.addColumn("CAPITAL", "bigint DEFAULT 0");
            alter = true;
        }

        if (alter) {
            try {
                table.getBase().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
            }
        }

    }

    private void updateVariablePaye(SQLTable table, String var, double value) throws SQLException {
        if (table == null) {
            throw new IllegalArgumentException("null table");
        }
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
