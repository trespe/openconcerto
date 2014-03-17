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
 
 package org.openconcerto.erp.model;

import org.openconcerto.erp.core.humanresources.payroll.element.FichePayeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.report.FichePayeSheet;
import org.openconcerto.erp.core.humanresources.payroll.ui.VisualisationPayeFrame;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableListener;
import org.openconcerto.utils.ExceptionHandler;

import java.sql.Date;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class EditionFichePayeModel extends AbstractTableModel {

    private Vector<Map<String, Object>> vData = new Vector<Map<String, Object>>();
    private String[] columnsName = { "A créer", "Salarié", "Brut", "Net", "Visualiser", "Imprimer" };
    private Map<Integer, String> mapColumn = new HashMap<Integer, String>();
    private JProgressBar bar;
    JLabel labelEtat;

    public EditionFichePayeModel(JProgressBar bar, JLabel labelEtat) {

        this.bar = bar;
        this.labelEtat = labelEtat;
        this.mapColumn.put(Integer.valueOf(0), "A_CREER");
        this.mapColumn.put(Integer.valueOf(1), "NOM");
        this.mapColumn.put(Integer.valueOf(2), "BRUT");
        this.mapColumn.put(Integer.valueOf(3), "NET");
        this.mapColumn.put(Integer.valueOf(4), "VISU");
        this.mapColumn.put(Integer.valueOf(5), "IMPRESSION");

        fill();
        updateAll();

        // FIXME Update
        SQLElement eltSal = Configuration.getInstance().getDirectory().getElement("SALARIE");
        eltSal.getTable().addTableListener(new SQLTableListener() {

            public void rowAdded(SQLTable table, int id) {
                updateAll();
            }

            public void rowDeleted(SQLTable table, int id) {
                updateAll();
            }

            public void rowModified(SQLTable table, int id) {
                updateAll();
            }
        });
    }

    public int getColumnCount() {
        return this.columnsName.length;
    }

    public int getRowCount() {
        return this.vData.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {

        String s = this.mapColumn.get(Integer.valueOf(columnIndex)).toString();
        Map<String, Object> m = this.vData.get(rowIndex);
        return m.get(s);
    }

    @Override
    public String getColumnName(int column) {

        return this.columnsName[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {

        String s = this.mapColumn.get(columnIndex).toString();
        if (s.equalsIgnoreCase("A_CREER") || s.equalsIgnoreCase("IMPRESSION") || s.equalsIgnoreCase("VISU")) {
            return Boolean.class;
        } else {
            if (s.equalsIgnoreCase("NOM")) {
                return String.class;
            } else {
                return Float.class;
            }

        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {

        String s = this.mapColumn.get(columnIndex).toString();
        if (s.equalsIgnoreCase("A_CREER") || s.equalsIgnoreCase("IMPRESSION") || s.equalsIgnoreCase("VISU")) {
            return true;
        }
        return false;

    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Map<String, Object> m = this.vData.get(rowIndex);
        String s = this.mapColumn.get(columnIndex).toString();
        Object o = m.get(s);
        if (o instanceof Boolean) {
            m.put(s, aValue);
        }
    }

    /**
     * Charge les données du model
     * 
     */
    private void fill() {

        // on recupere la lsite des salaries
        SQLElement eltSal = Configuration.getInstance().getDirectory().getElement("SALARIE");
        SQLSelect sel = new SQLSelect(eltSal.getTable().getBase());
        sel.addSelect(eltSal.getTable().getField("ID"));

        this.vData.removeAllElements();
        List l = (List) eltSal.getTable().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());
        if (l != null) {
            for (int i = 0; i < l.size(); i++) {
                int idSal = ((Number) ((Object[]) l.get(i))[0]).intValue();

                Map<String, Object> m = new HashMap<String, Object>();
                m.put("A_CREER", Boolean.TRUE);
                m.put("NOM", new Integer(i));
                m.put("BRUT", new Integer(i));
                m.put("NET", new Integer(i));
                m.put("VISU", Boolean.FALSE);
                m.put("IMPRESSION", Boolean.TRUE);
                m.put("ID_SALARIE", new Integer(idSal));

                this.vData.add(m);
            }
        }
    }

    /**
     * Mise à jour du brut et du net pour chacun des salariés
     * 
     */
    private void updateAll() {

        SQLElement eltSal = Configuration.getInstance().getDirectory().getElement("SALARIE");
        SQLElement eltFichePaye = Configuration.getInstance().getDirectory().getElement("FICHE_PAYE");

        for (int i = 0; i < this.vData.size(); i++) {
            Map<String, Object> m = this.vData.get(i);
            Integer idSal = Integer.parseInt(m.get("ID_SALARIE").toString());

            SQLRow rowSal = eltSal.getTable().getRow(idSal.intValue());
            int idFiche = rowSal.getInt("ID_FICHE_PAYE");
            SQLRow rowFiche = eltFichePaye.getTable().getRow(idFiche);
            m.put("BRUT", Float.valueOf(rowFiche.getFloat("SAL_BRUT")));
            m.put("NET", Float.valueOf(rowFiche.getFloat("NET_A_PAYER")));

            String nom = rowSal.getString("CODE") + " " + rowSal.getString("NOM") + " " + rowSal.getString("PRENOM");
            m.put("NOM", nom);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireTableDataChanged();
                System.err.println("Update all");
            }
        });

    }

    public int getIdSalAtRow(int row) {
        Map<String, Object> m = this.vData.get(row);
        return ((Number) m.get("ID_SALARIE")).intValue();
    }

    /**
     * Validation des fiches selectionnées
     * 
     * @param annee
     * @param idMois
     * @param du
     * @param au
     */
    public void validationFiche(final String annee, final int idMois, final Date du, final Date au) {

        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    EditionFichePayeModel.this.bar.setMaximum(EditionFichePayeModel.this.vData.size() * 4 - 1);
                    EditionFichePayeModel.this.bar.setString(null);
                    EditionFichePayeModel.this.bar.setStringPainted(false);
                    int tmp = 0;
                    EditionFichePayeModel.this.bar.setValue(tmp);

                    final SQLElement eltSal = Configuration.getInstance().getDirectory().getElement("SALARIE");
                    final SQLElement eltFichePaye = Configuration.getInstance().getDirectory().getElement("FICHE_PAYE");

                    // On crée la fiche de paye pour chacun des salariés sélectionnés
                    for (int i = 0; i < EditionFichePayeModel.this.vData.size(); i++) {
                        Map<String, Object> m = EditionFichePayeModel.this.vData.get(i);
                        Boolean bCreate = (Boolean) m.get("A_CREER");

                        if (bCreate.booleanValue()) {
                            final int idSal = ((Number) m.get("ID_SALARIE")).intValue();
                            SQLRow rowSalarie = eltSal.getTable().getRow(idSal);
                            final String salName = rowSalarie.getString("CODE") + " " + rowSalarie.getString("NOM");
                            final SQLRow row = eltSal.getTable().getRow(idSal);
                            final int idFiche = row.getInt("ID_FICHE_PAYE");

                            // Update de la periode
                            SQLRowValues rowVals = new SQLRowValues(eltFichePaye.getTable());
                            rowVals.put("ANNEE", Integer.valueOf(annee));
                            rowVals.put("ID_MOIS", idMois);
                            rowVals.put("DU", du);
                            rowVals.put("AU", au);
                            try {
                                rowVals.update(idFiche);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    EditionFichePayeModel.this.labelEtat.setText(salName + " - Mise à jour de la période");
                                }
                            });
                            EditionFichePayeModel.this.bar.setValue(tmp++);

                            // Visualisation
                            Boolean bVisu = (Boolean) m.get("VISU");
                            boolean resume = true;
                            if (bVisu.booleanValue()) {

                                final Semaphore semaphore = new Semaphore(1);
                                try {
                                    semaphore.acquire();
                                    // on demande le sémaphore

                                    final VisualisationPayeFrame frame = new VisualisationPayeFrame(semaphore);
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {

                                            frame.pack();
                                            frame.setSelectedFichePaye(idFiche);
                                            frame.setVisible(true);
                                        }
                                    });

                                    // synchronized (this) {
                                    // try {
                                    // System.err.println("Wait ");
                                    // this.wait();
                                    // System.err.println("WakeUp");
                                    //
                                    // } catch (InterruptedException e) {
                                    // e.printStackTrace();
                                    // }
                                    // }
                                    semaphore.acquire();
                                    System.err.println("Etat --> " + frame.getAnswer());
                                    resume = frame.getAnswer();
                                } catch (InterruptedException e1) {
                                    // TODO Auto-generated catch block
                                    e1.printStackTrace();
                                }
                            }

                            EditionFichePayeModel.this.bar.setValue(tmp++);

                            // test si l'utilisateur n'a pas annulé l'action
                            if (resume) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        EditionFichePayeModel.this.labelEtat.setText(salName + " - Validation de la fiche");
                                    }
                                });
                                // Validation de la fiche
                                FichePayeSQLElement.validationFiche(idFiche);

                                // Update des rubriques
                                SQLRow rowSalNew = eltSal.getTable().getRow(idSal);
                                final int idFicheNew = rowSalNew.getInt("ID_FICHE_PAYE");
                                FichePayeModel ficheModel = new FichePayeModel(idFicheNew);
                                ficheModel.loadAllElements();

                                EditionFichePayeModel.this.bar.setValue(tmp++);

                                // Impression
                                Boolean bPrint = (Boolean) m.get("IMPRESSION");
                                if (bPrint.booleanValue()) {
                                    SQLRow rowFiche = eltFichePaye.getTable().getRow(idFiche);
                                    FichePayeSheet.generation(rowFiche, false);
                                    FichePayeSheet.impression(rowFiche);
                                }

                                EditionFichePayeModel.this.bar.setValue(tmp++);

                            } else {

                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        EditionFichePayeModel.this.labelEtat.setText(salName + " - Création annulée");
                                    }
                                });
                                tmp += 2;
                                EditionFichePayeModel.this.bar.setValue(tmp);

                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        String msg = "Création annulée pour " + row.getString("CODE") + " " + row.getString("NOM") + " " + row.getString("PRENOM");
                                        JOptionPane.showMessageDialog(null, msg, "Création des payes", JOptionPane.INFORMATION_MESSAGE);
                                    }
                                });
                            }
                        } else {

                            tmp += 4;
                            EditionFichePayeModel.this.bar.setValue(tmp);

                        }
                    }
                } catch (Exception e) {
                    ExceptionHandler.handle("Erreur pendant la création des fiches de paye", e);
                }
                // Fin de l'edition
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updateAll();
                        JOptionPane.showMessageDialog(null, "Création des payes terminée", "Création paye", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
                EditionFichePayeModel.this.labelEtat.setText("Traitement terminé");
                EditionFichePayeModel.this.bar.setString("Terminé");
                EditionFichePayeModel.this.bar.setStringPainted(true);
            }
        };
        t.start();
    }
}
