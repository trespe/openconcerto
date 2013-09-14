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

import org.openconcerto.erp.core.common.ui.TotalCalculator;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.Callable;

public class GenerationMvtTicketCaisse extends GenerationEcritures {

    private static final String source = "TICKET_CAISSE";

    private final SQLRow rowTicket;
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
        this(ticket, 1);
    }

    public Callable<Integer> genereMouvement() {
        final Callable<Integer> c = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                SQLRow clientRow = GenerationMvtTicketCaisse.this.rowTicket.getForeignRow("ID_CLIENT");

                int idCompteClient = clientRow.getInt("ID_COMPTE_PCE");

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

                TotalCalculator calc = getValuesFromElement(rowTicket, rowTicket.getTable().getTable("SAISIE_VENTE_FACTURE_ELEMENT"), BigDecimal.ZERO, null, null);
                long ttcLongValue = calc.getTotalTTC().movePointRight(2).longValue();

                // compte Vente Produits

                for (SQLRowAccessor row : calc.getMapHt().keySet()) {
                    long b = calc.getMapHt().get(row).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValue();
                    if (b != 0) {
                        GenerationMvtTicketCaisse.this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(row.getID()));
                        GenerationMvtTicketCaisse.this.mEcritures.put("DEBIT", Long.valueOf(0));
                        GenerationMvtTicketCaisse.this.mEcritures.put("CREDIT", Long.valueOf(b));
                        ajoutEcriture();
                    }
                }

                // compte TVA
                Map<SQLRowAccessor, BigDecimal> tvaMap = calc.getMapHtTVA();
                for (SQLRowAccessor rowAc : tvaMap.keySet()) {
                    long longValue = tvaMap.get(rowAc).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValue();
                    if (longValue != 0) {
                        GenerationMvtTicketCaisse.this.mEcritures.put("ID_COMPTE_PCE", rowAc.getID());
                        GenerationMvtTicketCaisse.this.mEcritures.put("DEBIT", Long.valueOf(0));
                        GenerationMvtTicketCaisse.this.mEcritures.put("CREDIT", longValue);
                        ajoutEcriture();
                    }
                }

                // compte Clients

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
                GenerationMvtTicketCaisse.this.mEcritures.put("DEBIT", ttcLongValue);
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
