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
 
 package org.openconcerto.erp.core.reports.history.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.GestionDevise;

import java.awt.GridLayout;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class HistoriqueClientBilanPanel extends JPanel {
    private final JLabel labelVentesComptoir = new JLabel();
    private final JLabel labelVentesFacture = new JLabel();
    private final JLabel labelPropositions = new JLabel();
    private final JLabel labelCheques = new JLabel();
    private final JLabel labelEcheances = new JLabel();
    private final JLabel labelRelances = new JLabel();
    private final JLabel labelTotalVente = new JLabel();
    private long nbVentesCompoir;
    private long totalVentesCompoir;
    private long nbVentesFacture;
    private long totalVentesFacture;
    private long nbTotalCheques;
    private long totalCheques;
    private long nbChequesNonEncaisses;
    private long nbRelances;
    private int delaiPaiementMoyen;
    private long nbPropositions;
    private long totalPropositions;
    private long nbFacturesImpayees;
    private long totalFacturesImpayees;
    private int poucentageVentes;

    public HistoriqueClientBilanPanel() {
        super();

        setLayout(new GridLayout(4, 2));

        // Saisie Vente Comptoir --> HT, TTC
        final String valModeVenteComptoir = DefaultNXProps.getInstance().getStringProperty("ArticleVenteComptoir");
        final Boolean bModeVenteComptoir = Boolean.valueOf(valModeVenteComptoir);
        if (bModeVenteComptoir) {
            add(this.labelVentesComptoir);
        }
        // Saisie VF --> HT, TTC
        add(this.labelVentesFacture);
        // Proposition
        add(this.labelPropositions);
        // Cheque
        add(this.labelCheques);
        // Echeances
        add(this.labelEcheances);
        // Relances
        add(this.labelRelances);
        // Total vente
        add(this.labelTotalVente);
    }

    public synchronized void updateRelance(final List<Integer> listId) {
        final int nb = listId.size();
        if (this.nbRelances != nb) {
            setRelances(nb);
            updateLabels();
        }
    }

    SwingWorker<String, Object> workerTotalVente;

    public synchronized void updateTotalVente(final int idClient) {
        if (workerTotalVente != null && !workerTotalVente.isDone()) {
            workerTotalVente.cancel(true);
        }
        workerTotalVente = new SwingWorker<String, Object>() {
            @Override
            protected String doInBackground() throws Exception {

                final SQLBase base = ((ComptaPropsConfiguration) ComptaPropsConfiguration.getInstance()).getSQLBaseSociete();

                final SQLTable tableVF = base.getTable("SAISIE_VENTE_FACTURE");
                final SQLTable tableVC = base.getTable("SAISIE_VENTE_COMPTOIR");

                // Total VF
                final SQLSelect selVF = new SQLSelect(base);
                selVF.addSelect(tableVF.getField("T_HT"), "SUM");

                if (idClient > 1) {
                    selVF.setWhere("SAISIE_VENTE_FACTURE.ID_CLIENT", "=", idClient);
                }
                final String req = selVF.asString();
                // System.err.println(req);
                final Object o = base.getDataSource().executeScalar(req);
                final long totalVF = o == null ? 0 : ((Number) o).longValue();

                // Total VC
                final SQLSelect selVC = new SQLSelect(base);
                selVC.addSelect(tableVC.getField("MONTANT_HT"), "SUM");

                if (idClient > 1) {
                    selVC.setWhere("SAISIE_VENTE_COMPTOIR.ID_CLIENT", "=", idClient);
                }
                final String reqVC = selVC.asString();
                // System.err.println(reqVC);
                final Object oVC = base.getDataSource().executeScalar(reqVC);
                final long totalVC = oVC == null ? 0 : ((Number) oVC).longValue();

                final SQLSelect selAllVF = new SQLSelect(base);
                selAllVF.addSelect(tableVF.getField("T_HT"), "SUM");
                final Object o2 = base.getDataSource().executeScalar(selAllVF.asString());
                final long totalAllVF = o2 == null ? 0 : ((Number) o2).longValue();

                final SQLSelect selAllVC = new SQLSelect(base);
                selAllVC.addSelect(tableVC.getField("MONTANT_HT"), "SUM");
                final Object oVCA = base.getDataSource().executeScalar(selAllVC.asString());
                final long totalAllVC = oVCA == null ? 0 : ((Number) oVCA).longValue();

                if (totalAllVC + totalAllVF == 0) {
                    setPoucentageVentes(0);
                } else {
                    final double pourCentage = (totalVF + totalVC) / (double) (totalAllVC + totalAllVF) * 100.0;
                    setPoucentageVentes((int) Math.round(pourCentage * 100.0) / 100);
                } // TODO Auto-generated method stub
                return null;
            }

            @Override
            protected void done() {
                updateLabels();
                // TODO Auto-generated method stub
                super.done();
            }
        };
        workerTotalVente.execute();
    }

    // TODO: se passer de requete en utilisant les valeurs de la IListe si c'est possible
    // TODO: eviter les N meme requetes en double quand on selectionne un client
    public synchronized void updateEcheance(final List<Integer> listId) {
        final SQLBase base = ((ComptaPropsConfiguration) ComptaPropsConfiguration.getInstance()).getSQLBaseSociete();
        final SQLTable tableEch = base.getTable("ECHEANCE_CLIENT");
        long valueTotal = 0;
        if (listId != null) {
            for (final Iterator<Integer> i = listId.iterator(); i.hasNext();) {
                final SQLRow row = tableEch.getRow(i.next());
                if (row != null) {
                    final Object montantO = row.getObject("MONTANT");
                    valueTotal += Long.parseLong(montantO.toString());
                }
            }
        }
        setNbFacturesImpayees(listId == null ? 0 : listId.size());
        setTotalFacturesImpayees(valueTotal);
        updateLabels();
    }

    public synchronized void updateVFData(final List<Integer> listId, final int idClient) {
        final SQLBase base = ((ComptaPropsConfiguration) ComptaPropsConfiguration.getInstance()).getSQLBaseSociete();

        final SQLTable tableVF = base.getTable("SAISIE_VENTE_FACTURE");

        long valueTotal = 0;
        if (listId != null) {
            for (final Iterator<Integer> i = listId.iterator(); i.hasNext();) {
                final SQLRow rowTmp = tableVF.getRow(i.next());
                if (rowTmp != null) {
                    final Object montantO = rowTmp.getObject("T_HT");
                    valueTotal += new Long(montantO.toString());
                }
            }
        }
        final Map<Object, Date> mapDateFact = new HashMap<Object, Date>();
        // On recupere les dates de facturations VF
        final SQLSelect selDateFacture = new SQLSelect(base);
        final SQLTable tableFacture = base.getTable("SAISIE_VENTE_FACTURE");
        final SQLTable tableEncaisse = base.getTable("ENCAISSER_MONTANT");
        final SQLTable tableEcheance = base.getTable("ECHEANCE_CLIENT");
        final SQLTable tableMvt = base.getTable("MOUVEMENT");
        selDateFacture.addSelect(tableFacture.getField("DATE"));
        selDateFacture.addSelect(tableMvt.getField("ID_PIECE"));
        Where w = new Where(tableFacture.getField("ID_MOUVEMENT"), "=", tableMvt.getKey());
        if (idClient > 1) {
            w = w.and(new Where(tableFacture.getField("ID_CLIENT"), "=", idClient));
        }
        selDateFacture.setWhere(w);

        addDatesToMap(base, selDateFacture, mapDateFact);

        // On recupere les dates de facturations
        final SQLSelect selDateFactureC = new SQLSelect(base);
        final SQLTable tableComptoir = base.getTable("SAISIE_VENTE_COMPTOIR");
        selDateFactureC.addSelect(tableComptoir.getField("DATE"));
        selDateFactureC.addSelect(tableMvt.getField("ID_PIECE"));
        Where wC = new Where(tableComptoir.getField("ID_MOUVEMENT"), "=", tableMvt.getKey());
        if (idClient > 1) {
            wC = wC.and(new Where(tableComptoir.getField("ID_CLIENT"), "=", idClient));
        }
        selDateFactureC.setWhere(wC);
        addDatesToMap(base, selDateFactureC, mapDateFact);

        // On recupere les dates d'encaissement
        final SQLSelect selDateEncaisse = new SQLSelect(base);
        selDateEncaisse.addSelect(tableEncaisse.getField("DATE"));
        selDateEncaisse.addSelect(tableMvt.getField("ID_PIECE"));
        selDateEncaisse.addSelect(tableEcheance.getField("ID"));
        Where wEncaisse = new Where(tableEcheance.getField("ID"), "=", tableEncaisse.getField("ID_ECHEANCE_CLIENT"));
        wEncaisse = wEncaisse.and(new Where(tableEcheance.getField("ID_MOUVEMENT"), "=", tableMvt.getField("ID")));
        wEncaisse = wEncaisse.and(new Where(tableEcheance.getArchiveField(), "=", 1));

        if (idClient > 1) {
            wEncaisse = wEncaisse.and(new Where(tableEcheance.getField("ID_CLIENT"), "=", idClient));
        }

        selDateEncaisse.setWhere(wEncaisse);
        selDateEncaisse.setArchivedPolicy(SQLSelect.BOTH);

        final List<Object[]> lDateEncaisse = (List<Object[]>) base.getDataSource().execute(selDateEncaisse.asString(), new ArrayListHandler());
        final Map<Object, Date> mapDateEncaisse = new HashMap<Object, Date>();
        for (int i = 0; i < lDateEncaisse.size(); i++) {
            final Object[] tmp = lDateEncaisse.get(i);
            final Date d2 = (Date) tmp[0];
            final Object d = mapDateEncaisse.get(tmp[1]);
            if (d != null) {
                final Date d1 = (Date) d;
                if (d1.before(d2)) {
                    mapDateEncaisse.put(tmp[1], d2);
                }
            } else {
                mapDateEncaisse.put(tmp[1], d2);
            }
        }

        // Calcul moyenne
        int cpt = 0;
        int day = 0;
        final Calendar cal1 = Calendar.getInstance();
        final Calendar cal2 = Calendar.getInstance();
        for (final Iterator i = mapDateFact.keySet().iterator(); i.hasNext();) {
            final Object key = i.next();
            final Date dFact = mapDateFact.get(key);
            final Date dEncaisse = mapDateEncaisse.get(key);

            if (dFact != null && dEncaisse != null) {
                cpt++;
                cal1.setTime(dFact);
                cal2.setTime(dEncaisse);
                cal1.set(Calendar.HOUR, 0);
                cal1.set(Calendar.MINUTE, 0);
                cal1.set(Calendar.SECOND, 0);
                cal1.set(Calendar.MILLISECOND, 0);
                cal2.set(Calendar.HOUR, 0);
                cal2.set(Calendar.MINUTE, 0);
                cal2.set(Calendar.SECOND, 0);
                cal2.set(Calendar.MILLISECOND, 0);
                day += (cal2.getTime().getTime() - cal1.getTime().getTime()) / 86400000;
            }
        }

        setPoucentageVentes(cpt == 0 ? 0 : day / cpt);
        setTotalVentesFacture(valueTotal);
        setNbVentesFacture(listId == null ? 0 : listId.size());
        updateLabels();
    }

    private void addDatesToMap(final SQLBase base, final SQLSelect selDateFacture, final Map mapDateFact) {
        final List<Object[]> lDateFact = (List<Object[]>) base.getDataSource().execute(selDateFacture.asString(), new ArrayListHandler());

        final int size = lDateFact.size();
        for (int i = 0; i < size; i++) {
            final Object[] tmp = lDateFact.get(i);
            mapDateFact.put(tmp[1], tmp[0]);
        }
    }

    public synchronized void updateVCData(final List<Integer> listId) {
        final SQLBase base = ((ComptaPropsConfiguration) ComptaPropsConfiguration.getInstance()).getSQLBaseSociete();
        final SQLTable tableVC = base.getTable("SAISIE_VENTE_COMPTOIR");
        long valueTotal = 0;
        if (listId != null) {
            for (final Iterator<Integer> i = listId.iterator(); i.hasNext();) {
                final SQLRow rowTmp = tableVC.getRow(i.next());
                if (rowTmp != null) {
                    final Object montantO = rowTmp.getObject("MONTANT_HT");
                    valueTotal += new Long(montantO.toString());
                }
            }
        }

        setNbVentesComptoir(listId == null ? 0 : listId.size());
        setTotalVentesComptoir(valueTotal);
        updateLabels();
    }


    public synchronized void updateChequeData(final List<Integer> listId) {
        final SQLBase base = ((ComptaPropsConfiguration) ComptaPropsConfiguration.getInstance()).getSQLBaseSociete();
        final SQLTable tableC = base.getTable("CHEQUE_A_ENCAISSER");
        long valueTotalTmp = 0;
        long valueNonEncaisseTmp = 0;
        if (listId != null) {
            for (final Iterator<Integer> i = listId.iterator(); i.hasNext();) {
                final SQLRow row = tableC.getRow(i.next());
                if (row != null) {
                    final Object montantO = row.getObject("MONTANT");
                    valueTotalTmp += Long.parseLong(montantO.toString());
                    if (!row.getBoolean("ENCAISSE")) {
                        valueNonEncaisseTmp++;
                    }
                }
            }
        }
        setNbTotalCheques(listId == null ? 0 : listId.size());
        setNbChequesNonEncaisses(valueNonEncaisseTmp);
        setTotalCheques(valueTotalTmp);
        updateLabels();
    }

    // Ventes comptoir
    public void setNbVentesComptoir(final long nb) {
        this.nbVentesCompoir = nb;
    }

    public void setTotalVentesComptoir(final long totalInCents) {
        this.totalVentesCompoir = totalInCents;
    }

    // Ventes avec facture
    public void setNbVentesFacture(final long nb) {
        this.nbVentesFacture = nb;
    }

    public void setTotalVentesFacture(final long totalInCents) {
        this.totalVentesFacture = totalInCents;
    }

    // Cheques
    public void setNbTotalCheques(final long nb) {
        this.nbTotalCheques = nb;
    }

    public void setTotalCheques(final long totalInCents) {
        this.totalCheques = totalInCents;
    }

    public void setNbChequesNonEncaisses(final long nb) {
        this.nbChequesNonEncaisses = nb;
    }

    // Relances
    public void setRelances(final long nb) {
        this.nbRelances = nb;
    }

    public void setDelaiPaiementMoyen(final int nb) {
        this.delaiPaiementMoyen = nb;
    }

    // Propositions
    public void setNbPropositions(final long nb) {
        this.nbPropositions = nb;
    }

    public void setTotalPropositions(final long totalInCents) {
        this.totalPropositions = totalInCents;
    }

    // Facture impayées
    public void setNbFacturesImpayees(final long nb) {
        this.nbFacturesImpayees = nb;
    }

    public void setTotalFacturesImpayees(final long totalInCents) {
        this.totalFacturesImpayees = totalInCents;
    }

    // Pourcentage des vente
    public void setPoucentageVentes(final int pourCent) {
        this.poucentageVentes = pourCent;
    }

    private void updateLabels() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                String total;
                long nb;
                // Ventes comptoir
                total = GestionDevise.currencyToString(HistoriqueClientBilanPanel.this.totalVentesCompoir, true);
                nb = HistoriqueClientBilanPanel.this.nbVentesCompoir;
                if (nb == 0) {
                    HistoriqueClientBilanPanel.this.labelVentesComptoir.setText("- pas de vente comptoir");
                } else if (nb == 1) {
                    HistoriqueClientBilanPanel.this.labelVentesComptoir.setText("- une vente comptoir d'un montant de " + total + " € HT");
                } else {
                    HistoriqueClientBilanPanel.this.labelVentesComptoir.setText("- " + nb + " ventes comptoir d'un montant total de " + total + " € HT");
                }
                // Ventes facture
                total = GestionDevise.currencyToString(HistoriqueClientBilanPanel.this.totalVentesFacture, true);
                nb = HistoriqueClientBilanPanel.this.nbVentesFacture;
                if (nb == 0) {
                    HistoriqueClientBilanPanel.this.labelVentesFacture.setText("- pas de vente avec facture");
                } else if (nb == 1) {
                    HistoriqueClientBilanPanel.this.labelVentesFacture.setText("- une vente avec facture d'un montant de " + total + " € HT");
                } else {
                    HistoriqueClientBilanPanel.this.labelVentesFacture.setText("- " + nb + " ventes avec facture d'un montant total de " + total + " € HT");
                }
                // Propositions
                total = GestionDevise.currencyToString(HistoriqueClientBilanPanel.this.totalPropositions, true);
                nb = HistoriqueClientBilanPanel.this.nbPropositions;
                if (nb == 0) {
                    HistoriqueClientBilanPanel.this.labelPropositions.setText("- pas de proposition commerciale");
                } else if (nb == 1) {
                    HistoriqueClientBilanPanel.this.labelPropositions.setText("- une proposition commerciale d'un montant de " + total + " € HT");
                } else {
                    HistoriqueClientBilanPanel.this.labelPropositions.setText("- " + nb + " propositions commerciales d'un montant total de " + total + " € HT");
                }
                // Chèques
                nb = HistoriqueClientBilanPanel.this.nbTotalCheques;
                total = GestionDevise.currencyToString(HistoriqueClientBilanPanel.this.totalCheques, true);
                if (nb == 0) {
                    HistoriqueClientBilanPanel.this.labelCheques.setText("- pas de chèque");
                } else if (nb == 1) {
                    if (HistoriqueClientBilanPanel.this.nbChequesNonEncaisses == 0) {
                        HistoriqueClientBilanPanel.this.labelCheques.setText("- un chèque d'un montant de " + total + " € HT");
                    } else {
                        HistoriqueClientBilanPanel.this.labelCheques.setText("- un chèque non encaissé d'un montant de " + total + " € HT");
                    }
                } else {
                    if (HistoriqueClientBilanPanel.this.nbChequesNonEncaisses == 0) {
                        HistoriqueClientBilanPanel.this.labelCheques.setText("- " + nb + " chèques d'un montant total de " + total + " € HT");
                    } else if (HistoriqueClientBilanPanel.this.nbChequesNonEncaisses == nb) {
                        HistoriqueClientBilanPanel.this.labelCheques.setText("- " + nb + " chèques non encaissés d'un montant total de " + total + " € HT");
                    } else {
                        HistoriqueClientBilanPanel.this.labelCheques.setText("- " + nb + " chèques non d'un montant total de " + total + " € HT dont "
                                + HistoriqueClientBilanPanel.this.nbChequesNonEncaisses + " non encaissés");
                    }
                }
                // Factures impayées
                nb = HistoriqueClientBilanPanel.this.nbFacturesImpayees;
                total = GestionDevise.currencyToString(HistoriqueClientBilanPanel.this.totalFacturesImpayees, true);
                if (nb == 0) {
                    HistoriqueClientBilanPanel.this.labelEcheances.setText("- pas de facture impayée");
                } else if (nb == 1) {
                    HistoriqueClientBilanPanel.this.labelEcheances.setText("- une facture impayée d'un montant de " + total + " € HT");
                } else {
                    HistoriqueClientBilanPanel.this.labelEcheances.setText("- " + nb + " factures impayées d'un montant total de " + total + " € HT");
                }
                // Relances
                nb = HistoriqueClientBilanPanel.this.nbRelances;
                String txt;
                if (nb == 0) {
                    txt = "- pas de relance effectuée";
                } else if (nb == 1) {
                    txt = "- une relance effectuée";
                } else {
                    txt = "- " + nb + " relances effectuées";
                }
                if (nb > 0) {
                    if (HistoriqueClientBilanPanel.this.delaiPaiementMoyen == 1) {
                        txt += ", délai moyen de paiment d'une journée";
                    } else if (HistoriqueClientBilanPanel.this.delaiPaiementMoyen > 1) {
                        txt += ", délai moyen de paiment de " + HistoriqueClientBilanPanel.this.delaiPaiementMoyen + " jours";
                    }
                }
                HistoriqueClientBilanPanel.this.labelRelances.setText(txt);
                // % des ventes
                final long cents = HistoriqueClientBilanPanel.this.totalVentesCompoir + HistoriqueClientBilanPanel.this.totalVentesFacture;
                total = GestionDevise.currencyToString(cents, true);
                if (cents == 0) {
                    HistoriqueClientBilanPanel.this.labelTotalVente.setText("- pas de vente");
                } else if (HistoriqueClientBilanPanel.this.poucentageVentes <= 0) {
                    HistoriqueClientBilanPanel.this.labelTotalVente.setText("- ventes de " + total + " € HT");
                } else {
                    HistoriqueClientBilanPanel.this.labelTotalVente.setText("- ventes de " + total + " € HT, soit " + HistoriqueClientBilanPanel.this.poucentageVentes + "% des ventes totales");
                }
            }
        });

    }
}
