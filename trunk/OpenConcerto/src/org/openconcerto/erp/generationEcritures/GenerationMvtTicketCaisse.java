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
 
 package org.openconcerto.erp.generationEcritures;

import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.JournalSQLElement;
import org.openconcerto.erp.model.PrixHT;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;

import java.util.Map;
import java.util.concurrent.Callable;

public class GenerationMvtTicketCaisse extends GenerationEcritures {

    private static final String source = "TICKET_CAISSE";
    public static final Integer journal = Integer.valueOf(JournalSQLElement.VENTES);
    private final SQLRow rowTicket;
    private static final SQLTable ticketTable = base.getTable("TICKET_CAISSE");
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

    /**
     * Generation de la comptabilité associée à un ticket
     * 
     * @param idTicket
     * @param idMvt id du mouvement qui est dejà associé au ticket
     */
    public GenerationMvtTicketCaisse(SQLRow ticket, int idMvt) {

        System.err.println("********* init GeneRation");
        this.idMvt = idMvt;
        this.rowTicket = ticket;

    }

    /**
     * Generation de la comptabilité associée à la création d'un ticket
     * 
     * @param idSaisieVenteFacture
     */
    public GenerationMvtTicketCaisse(SQLRow ticket) {

        System.err.println("********* init GeneRation");
        this.idMvt = 1;
        this.rowTicket = ticket;
    }

    public Callable<Integer> genereMouvement() {
        final Callable<Integer> c = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                SQLRow clientRow = GenerationMvtTicketCaisse.this.rowTicket.getForeignRow("ID_CLIENT");

                // Calcul des montants
                PrixTTC prixTTC = new PrixTTC(((Long) GenerationMvtTicketCaisse.this.rowTicket.getObject("TOTAL_TTC")).longValue());
                PrixHT prixTVA = new PrixHT(((Long) GenerationMvtTicketCaisse.this.rowTicket.getObject("TOTAL_TVA")).longValue());
                PrixHT prixHT = new PrixHT(((Long) GenerationMvtTicketCaisse.this.rowTicket.getObject("TOTAL_HT")).longValue());

                // iniatilisation des valeurs de la map
                GenerationMvtTicketCaisse.this.date = GenerationMvtTicketCaisse.this.rowTicket.getDate("DATE").getTime();
                GenerationMvtTicketCaisse.this.nom = "Ticket " + GenerationMvtTicketCaisse.this.rowTicket.getString("NUMERO");
                GenerationMvtTicketCaisse.this.mEcritures.put("DATE", GenerationMvtTicketCaisse.this.date);
                GenerationMvtTicketCaisse.this.mEcritures.put("NOM", GenerationMvtTicketCaisse.this.nom);
                GenerationMvtTicketCaisse.this.mEcritures.put("ID_JOURNAL", GenerationMvtSaisieVenteFacture.journal);
                GenerationMvtTicketCaisse.this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(1));

                // on calcule le nouveau numero de mouvement
                if (GenerationMvtTicketCaisse.this.idMvt == 1) {
                    getNewMouvement(GenerationMvtTicketCaisse.source, GenerationMvtTicketCaisse.this.rowTicket.getID(), 1, GenerationMvtTicketCaisse.this.nom);
                } else {
                    GenerationMvtTicketCaisse.this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(GenerationMvtTicketCaisse.this.idMvt));
                }

                // compte Vente Produits
                final long produitHT = prixHT.getLongValue();
                if (produitHT >= 0) {

                    if (produitHT > 0) {

                        int idCompteVenteProduit = 1;
                        if (idCompteVenteProduit <= 1) {
                            idCompteVenteProduit = rowPrefsCompte.getInt("ID_COMPTE_PCE_VENTE_PRODUIT");
                            if (idCompteVenteProduit <= 1) {
                                try {
                                    idCompteVenteProduit = ComptePCESQLElement.getIdComptePceDefault("VentesProduits");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        GenerationMvtTicketCaisse.this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteVenteProduit));
                        GenerationMvtTicketCaisse.this.mEcritures.put("DEBIT", Long.valueOf(0));
                        GenerationMvtTicketCaisse.this.mEcritures.put("CREDIT", Long.valueOf(produitHT));
                        ajoutEcriture();
                    }

                }

                // compte TVA
                if (prixTVA.getLongValue() > 0) {
                    int idCompteTVA = rowPrefsCompte.getInt("ID_COMPTE_PCE_TVA_VENTE");
                    if (idCompteTVA <= 1) {
                        try {
                            idCompteTVA = ComptePCESQLElement.getIdComptePceDefault("TVACollectee");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    Map<Integer, Long> m = getMultiTVAFromRow(GenerationMvtTicketCaisse.this.rowTicket, GenerationMvtTicketCaisse.this.rowTicket.getTable().getTable("SAISIE_VENTE_FACTURE_ELEMENT"),
                            true, prixHT, 0);
                    long allTaxe = 0;
                    for (Integer i : m.keySet()) {
                        Long l = m.get(i);
                        if (l != null && l > 0) {
                            // FIXME
                            int idCpt = i;
                            if (idCpt <= 1) {
                                idCpt = idCompteTVA;
                            }
                            GenerationMvtTicketCaisse.this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCpt));
                            GenerationMvtTicketCaisse.this.mEcritures.put("DEBIT", Long.valueOf(0));
                            GenerationMvtTicketCaisse.this.mEcritures.put("CREDIT", Long.valueOf(l));
                            ajoutEcriture();
                            allTaxe += l;
                        }
                    }
                    if (allTaxe < prixTVA.getLongValue()) {
                        GenerationMvtTicketCaisse.this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteTVA));
                        GenerationMvtTicketCaisse.this.mEcritures.put("DEBIT", Long.valueOf(0));
                        GenerationMvtTicketCaisse.this.mEcritures.put("CREDIT", Long.valueOf(prixTVA.getLongValue() - allTaxe));
                        ajoutEcriture();
                    }

                }
                // compte Clients
                int idCompteClient = clientRow.getInt("ID_COMPTE_PCE");
                if (idCompteClient <= 1) {
                    idCompteClient = rowPrefsCompte.getInt("ID_COMPTE_PCE_CLIENT");
                    if (idCompteClient <= 1) {
                        try {
                            idCompteClient = ComptePCESQLElement.getIdComptePceDefault("Clients");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                GenerationMvtTicketCaisse.this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteClient));
                GenerationMvtTicketCaisse.this.mEcritures.put("DEBIT", Long.valueOf(prixTTC.getLongValue()));
                GenerationMvtTicketCaisse.this.mEcritures.put("CREDIT", Long.valueOf(0));
                ajoutEcriture();
                return GenerationMvtTicketCaisse.this.idMvt;
            }
        };
        return c;

        // // Mise à jour de mouvement associé à la facture
        //
        // SQLRowValues valTicket = new SQLRowValues(GenerationMvtTicketCaisse.ticketTable);
        // valTicket.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));
        //
        // try {
        // if (valTicket.getInvalid() == null) {
        //
        // valTicket.update(this.rowTicket.getID());
        // }
        // } catch (SQLException e) {
        // System.err.println("Erreur à l'insertion dans la table " + valTicket.getTable().getName()
        // + " : " + e);
        // e.printStackTrace();
        // }
    }

}
