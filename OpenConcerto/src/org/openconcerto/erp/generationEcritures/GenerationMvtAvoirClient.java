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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.JournalSQLElement;
import org.openconcerto.erp.model.PrixHT;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionHandler;

import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

public class GenerationMvtAvoirClient extends GenerationEcritures {

    private static final String source = "AVOIR_CLIENT";
    // TODO dans quel journal les ecritures des avoirs? OD?
    private static final Integer journal = Integer.valueOf(JournalSQLElement.VENTES);
    private int idAvoirClient;
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

    public GenerationMvtAvoirClient(int idAvoirClient) {
        this.idMvt = 1;
        this.idAvoirClient = idAvoirClient;
    }

    public GenerationMvtAvoirClient(int idAvoirClient, int idMvt) {
        this.idMvt = idMvt;
        this.idAvoirClient = idAvoirClient;
    }

    public int genereMouvement() {

        SQLTable avoirClientTable = base.getTable("AVOIR_CLIENT");

        SQLRow avoirRow = avoirClientTable.getRow(this.idAvoirClient);

        boolean affacturage = avoirRow.getBoolean("AFFACTURE");

        SQLRow rowClient;
            rowClient = avoirRow.getForeignRow("ID_CLIENT");

        // Calcul des montants
        PrixTTC prixTTC = new PrixTTC(((Long) avoirRow.getObject("MONTANT_TTC")).longValue());
        PrixHT prixHT = new PrixHT(((Long) avoirRow.getObject("MONTANT_HT")).longValue());
        PrixHT prixTVA = new PrixHT(((Long) avoirRow.getObject("MONTANT_TVA")).longValue());
        PrixHT prixService = new PrixHT(((Long) avoirRow.getObject("MONTANT_SERVICE")).longValue());

        // iniatilisation des valeurs de la map
        this.date = (Date) avoirRow.getObject("DATE");
        this.nom = avoirRow.getObject("NOM").toString();
        this.mEcritures.put("DATE", new java.sql.Date(this.date.getTime()));
        this.mEcritures.put("NOM", "Avoir Client : " + avoirRow.getString("NUMERO") + " " + rowClient.getString("NOM"));

        this.mEcritures.put("ID_JOURNAL", GenerationMvtAvoirClient.journal);
        if (affacturage) {

            int idJrnlFactor = rowPrefsCompte.getInt("ID_JOURNAL_FACTOR");
            if (idJrnlFactor > 1) {
                this.mEcritures.put("ID_JOURNAL", idJrnlFactor);
            }
        }

        this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(1));

        // on cree un nouveau mouvement
        if (this.idMvt == 1) {
            getNewMouvement(GenerationMvtAvoirClient.source, this.idAvoirClient, 1, "Avoir Client : " + avoirRow.getString("NUMERO"));
        } else {
            this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));
        }

        // generation des ecritures + maj des totaux du compte associe

        int idCompteVenteService = avoirRow.getInt("ID_COMPTE_PCE_SERVICE");
        if (idCompteVenteService <= 1) {
            idCompteVenteService = rowPrefsCompte.getInt("ID_COMPTE_PCE_VENTE_SERVICE");
            if (idCompteVenteService <= 1) {
                try {
                    idCompteVenteService = ComptePCESQLElement.getIdComptePceDefault("VentesServices");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            // compte Vente Produits
            final long produitHT = prixHT.getLongValue() - prixService.getLongValue();
            if (produitHT >= 0) {

                if (produitHT > 0) {
                    int idCompteVenteProduit = rowPrefsCompte.getInt("ID_COMPTE_PCE_VENTE_PRODUIT");
                    if (idCompteVenteProduit <= 1) {
                        try {
                            idCompteVenteProduit = ComptePCESQLElement.getIdComptePceDefault("VentesProduits");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteVenteProduit));
                    this.mEcritures.put("DEBIT", Long.valueOf(produitHT));
                    this.mEcritures.put("CREDIT", Long.valueOf(0));
                    ajoutEcriture();
                }

                // si on a des frais de service
                if (prixService.getLongValue() > 0) {
                    // compte Vente Services

                    this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteVenteService));
                    this.mEcritures.put("DEBIT", Long.valueOf(prixService.getLongValue()));
                    this.mEcritures.put("CREDIT", Long.valueOf(0));
                    ajoutEcriture();
                }
            } else// la remise déborde sur les frais de service donc aucun frais
                  // pour les produits
            {
                // compte Vente Services
                this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteVenteService));
                this.mEcritures.put("DEBIT", Long.valueOf(prixHT.getLongValue()));
                this.mEcritures.put("CREDIT", Long.valueOf(0));
                ajoutEcriture();

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

                Map<Integer, Long> m = getMultiTVAFromRow(avoirRow, avoirClientTable.getTable("AVOIR_CLIENT_ELEMENT"), true);
                long allTaxe = 0;
                for (Integer i : m.keySet()) {
                    Long l = m.get(i);
                    if (l != null && l > 0) {
                        // FIXME
                        int idCpt = i;
                        if (idCpt <= 1) {
                            idCpt = idCompteTVA;
                        }
                        this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCpt));
                        this.mEcritures.put("DEBIT", Long.valueOf(0));
                        this.mEcritures.put("CREDIT", Long.valueOf(l));
                        ajoutEcriture();
                        allTaxe += l;
                    }
                }
                if (allTaxe < prixTVA.getLongValue()) {
                    this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteTVA));
                    this.mEcritures.put("DEBIT", Long.valueOf(0));
                    this.mEcritures.put("CREDIT", Long.valueOf(prixTVA.getLongValue() - allTaxe));
                    ajoutEcriture();
                }

            }
            //
            // // compte TVA
            // int idCompteTVA = rowPrefsCompte.getInt("ID_COMPTE_PCE_TVA_VENTE");
            // if (idCompteTVA <= 1) {
            // try {
            // idCompteTVA = ComptePCESQLElement.getIdComptePceDefault("TVACollectee");
            // } catch (Exception e) {
            //
            // e.printStackTrace();
            // }
            // }
            // this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteTVA));
            // this.mEcritures.put("DEBIT", Long.valueOf(prixTVA.getLongValue()));
            // this.mEcritures.put("CREDIT", Long.valueOf(0));
            // ajoutEcriture();

            // compte Clients
            int idCompteClient = avoirRow.getForeignRow("ID_CLIENT").getInt("ID_COMPTE_PCE");
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
            this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteClient));
            this.mEcritures.put("DEBIT", Long.valueOf(0));
            this.mEcritures.put("CREDIT", Long.valueOf(prixTTC.getLongValue()));
            ajoutEcriture();

            // Mise à jour de mouvement associé à la facture d'avoir
            SQLRowValues valAvoir = new SQLRowValues(avoirClientTable);
            valAvoir.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));

            try {
                if (valAvoir.getInvalid() == null) {

                    valAvoir.update(this.idAvoirClient);
                }
            } catch (SQLException e) {
                System.err.println("Erreur à l'insertion dans la table " + avoirClientTable.getName() + " : " + e);
                e.printStackTrace();
            }

            if (affacturage) {
                this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteClient));
                this.mEcritures.put("DEBIT", Long.valueOf(prixTTC.getLongValue()));
                this.mEcritures.put("CREDIT", Long.valueOf(0));
                ajoutEcriture();

                // compte Factor
                int idComptefactor = rowPrefsCompte.getInt("ID_COMPTE_PCE_FACTOR");
                if (idComptefactor <= 1) {
                    idComptefactor = rowPrefsCompte.getInt("ID_COMPTE_PCE_FACTOR");
                    if (idComptefactor <= 1) {
                        try {
                            idComptefactor = ComptePCESQLElement.getIdComptePceDefault("Factor");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                this.mEcritures.put("ID_COMPTE_PCE", idComptefactor);
                this.mEcritures.put("DEBIT", Long.valueOf(0));
                this.mEcritures.put("CREDIT", Long.valueOf(prixTTC.getLongValue()));
                ajoutEcriture();
            }

            if (avoirRow.getInt("ID_MODE_REGLEMENT") > 1) {
                new GenerationMvtReglementAvoir(this.idAvoirClient, this.idMvt);
            } else {
                valAvoir.put("SOLDE", Boolean.FALSE);
                try {
                    valAvoir.update(this.idAvoirClient);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

        } catch (IllegalArgumentException e) {
            ExceptionHandler.handle("Erreur pendant la générations des écritures comptables", e);
            e.printStackTrace();
        }

        return this.idMvt;
    }
}
