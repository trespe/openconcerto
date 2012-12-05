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
 
 package org.openconcerto.erp.panel;

import org.openconcerto.erp.utils.ActionDB;
import org.openconcerto.erp.utils.StatusListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSchema;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class ChargementCreationSocietePanel extends JPanel implements StatusListener {

    private JProgressBar progressBar;
    private JLabel label;

    public ChargementCreationSocietePanel(final int idSoc, final int typePCG) {

        super();
        this.progressBar = new JProgressBar();
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;

        JLabel label1 = new JLabelBold("Création de la base de données de la nouvelle société");
        this.add(label1, c);
        c.gridy++;

        JLabel label2 = new JLabel("Cette opération peut prendre plusieurs minutes.");
        this.add(label2, c);
        c.gridy++;
        this.label = new JLabel(" ");
        this.add(this.label, c);
        c.gridy++;
        this.add(this.progressBar, c);
        this.progressBar.setIndeterminate(true);

        new Thread("Creation de societe") {
            public void run() {
                try {
                    creationBase(idSoc);

                    statusChanged("Importation du plan comptable");
                    importationPlanComptable(idSoc, typePCG);
                    statusChanged("Création terminée!");
                    ChargementCreationSocietePanel.this.progressBar.setIndeterminate(false);
                    ChargementCreationSocietePanel.this.progressBar.setString(null);

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            ((JFrame) SwingUtilities.getRoot(ChargementCreationSocietePanel.this)).dispose();
                        }
                    });
                } catch (Throwable e) {
                    ExceptionHandler.handle("Erreur pendant la création de la base!", e);
                }
            }
        }.start();
    }

    private void creationBase(int id) throws SQLException {

        System.err.println("Création de la base");

        ActionDB.dupliqueDB("Default", "OpenConcerto" + id, this);

        statusChanged("Mise à jour des sociétés");
        SQLRowValues rowVals = new SQLRowValues(Configuration.getInstance().getBase().getTable("SOCIETE_COMMON"));
        rowVals.put("DATABASE_NAME", "OpenConcerto" + id);

        rowVals.update(id);

    }

    private void importationPlanComptable(int id, int typePCG) {

        SQLRow rowSociete = Configuration.getInstance().getBase().getTable("SOCIETE_COMMON").getRow(id);

        // FIXME by Sylvain comme on a déjà accede à la base, les nouvelles tables n'ont pas été
        // rechargées
        final SQLSchema baseNewSociete = Configuration.getInstance().getBase().getSchema(rowSociete.getString("DATABASE_NAME"));

        SQLTable tableComptePCG = baseNewSociete.getTable("COMPTE_PCG");
        final SQLSelect sel = new SQLSelect();
        sel.addSelect(tableComptePCG.getField("NUMERO"));
        sel.addSelect(tableComptePCG.getField("NOM"));
        sel.addSelect(tableComptePCG.getField("INFOS"));

        if (typePCG == 0) {
            sel.setWhere(new Where(tableComptePCG.getField("ID_TYPE_COMPTE_PCG_BASE"), "!=", 1));
        } else if (typePCG == 1) {
            sel.setWhere(new Where(tableComptePCG.getField("ID_TYPE_COMPTE_PCG_AB"), "!=", 1));
        }

        try {
            baseNewSociete.getBase().getDataSource().useConnection(new ConnectionHandlerNoSetup<Object, SQLException>() {

                @Override
                public Object handle(SQLDataSource ds) throws SQLException, SQLException {
                    String insert = "INSERT INTO \"" + baseNewSociete.getName() + "\".\"COMPTE_PCE\" (\"NUMERO\", \"NOM\", \"INFOS\") VALUES (?, ?, ?)";
                    PreparedStatement stmt = ds.getConnection().prepareStatement(insert);
                    List tmpCpt = baseNewSociete.getBase().getDataSource().execute(sel.asString());
                    for (int i = 0; i < tmpCpt.size(); i++) {
                        Map tmp = (Map) tmpCpt.get(i);
                        String numero = (tmp.get("NUMERO") == null) ? "" : tmp.get("NUMERO").toString();
                        stmt.setString(1, numero);
                        String nom = (tmp.get("NOM") == null) ? "" : tmp.get("NOM").toString();
                        stmt.setString(2, nom);
                        String infos = (tmp.get("INFOS") == null) ? "" : tmp.get("INFOS").toString();
                        stmt.setString(3, infos);
                        stmt.executeUpdate();

                    }
                    return null;
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void statusChanged(final String message) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                ChargementCreationSocietePanel.this.label.setText(message);

            }
        });

    }
}
