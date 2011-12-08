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
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionHandler;

import java.sql.SQLException;
import java.util.Date;


public class GenerationMvtSaisieVenteComptoir extends GenerationEcritures implements Runnable {

    private static final String source = "SAISIE_VENTE_COMPTOIR";
    private static final Integer journal = new Integer(JournalSQLElement.VENTES);
    private static final SQLTable saisieVCTable = base.getTable("SAISIE_VENTE_COMPTOIR");
    private static final SQLTable mvtTable = base.getTable("MOUVEMENT");
    private int idSaisieVenteComptoir;
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

    public GenerationMvtSaisieVenteComptoir(int idSaisieVenteComptoir) {

        this.idSaisieVenteComptoir = idSaisieVenteComptoir;
        this.idMvt = 1;
        (new Thread(GenerationMvtSaisieVenteComptoir.this)).start();
    }

    public GenerationMvtSaisieVenteComptoir(int idSaisieVenteComptoir, int idMvt) {

        this.idSaisieVenteComptoir = idSaisieVenteComptoir;
        this.idMvt = idMvt;
        (new Thread(GenerationMvtSaisieVenteComptoir.this)).start();
    }

    private void genereMouvement() throws IllegalArgumentException {

        SQLRow saisieRow = GenerationMvtSaisieVenteComptoir.saisieVCTable.getRow(this.idSaisieVenteComptoir);
        SQLRow clientRow = base.getTable("CLIENT").getRow(saisieRow.getInt("ID_CLIENT"));
        SQLRow taxeRow = base.getTable("TAXE").getRow(saisieRow.getInt("ID_TAXE"));

        // Calcul des montants
        PrixTTC prixTTC = new PrixTTC(((Long) saisieRow.getObject("MONTANT_TTC")).longValue());
        PrixHT prixHT = new PrixHT(prixTTC.calculLongHT(taxeRow.getFloat("TAUX") / 100));
        long service = ((Long) saisieRow.getObject("MONTANT_SERVICE")).longValue();

        // iniatilisation des valeurs de la map
        this.date = (Date) saisieRow.getObject("DATE");
        String string = "Vente comptoir ";
        final String rowLib = saisieRow.getObject("NOM").toString();
        if (rowLib != null && rowLib.trim().length() > 0) {
            string += rowLib.trim();
        } else {
            string += saisieRow.getForeignRow("ID_ARTICLE").getString("NOM");
        }
        this.nom = string;
        this.mEcritures.put("DATE", this.date);
        this.mEcritures.put("NOM", this.nom);
        this.mEcritures.put("ID_JOURNAL", GenerationMvtSaisieVenteComptoir.journal);
        this.mEcritures.put("ID_MOUVEMENT", new Integer(1));

        // on calcule le nouveau numero de mouvement
        if (this.idMvt == 1) {
            this.idMvt = getNewMouvement(GenerationMvtSaisieVenteComptoir.source, this.idSaisieVenteComptoir, 1, string);
            this.mEcritures.put("ID_MOUVEMENT", new Integer(this.idMvt));
        }

        // generation des ecritures + maj des totaux du compte associe

        // compte Vente
        if (service != 0) {

            int idCompteVenteService = rowPrefsCompte.getInt("ID_COMPTE_PCE_VENTE_SERVICE");
            if (idCompteVenteService <= 1) {
                try {
                    idCompteVenteService = ComptePCESQLElement.getIdComptePceDefault("VentesServices");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteVenteService));
            this.mEcritures.put("DEBIT", new Long(0));
            this.mEcritures.put("CREDIT", new Long(service));
            ajoutEcriture();

            // System.out.println("___________---> Value " + (prixHT.getValue() - service));
            if ((prixHT.getLongValue() - service) > 0) {
                int idCompteVenteProduit = rowPrefsCompte.getInt("ID_COMPTE_PCE_VENTE_PRODUIT");
                if (idCompteVenteProduit <= 1) {
                    try {
                        idCompteVenteProduit = ComptePCESQLElement.getIdComptePceDefault("VentesProduits");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteVenteProduit));
                this.mEcritures.put("DEBIT", new Long(0));
                this.mEcritures.put("CREDIT", new Long(prixHT.getLongValue() - service));
                ajoutEcriture();
            }

        } else {

            int idCompteVenteProduit = rowPrefsCompte.getInt("ID_COMPTE_PCE_VENTE_PRODUIT");
            if (idCompteVenteProduit <= 1) {
                try {
                    idCompteVenteProduit = ComptePCESQLElement.getIdComptePceDefault("VentesProduits");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteVenteProduit));
            this.mEcritures.put("DEBIT", new Long(0));
            this.mEcritures.put("CREDIT", new Long(prixHT.getLongValue()));
            ajoutEcriture();
        }

        // compte TVA
        long tva = prixTTC.calculLongTVA(taxeRow.getFloat("TAUX") / 100.0);
        if (tva > 0) {
            int idCompteTVA = rowPrefsCompte.getInt("ID_COMPTE_PCE_TVA_VENTE");
            if (idCompteTVA <= 1) {
                try {
                    idCompteTVA = ComptePCESQLElement.getIdComptePceDefault("TVACollectee");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteTVA));
            this.mEcritures.put("DEBIT", new Long(0));
            this.mEcritures.put("CREDIT", new Long(tva));
            ajoutEcriture();
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
        this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteClient));
        this.mEcritures.put("DEBIT", new Long(prixTTC.getLongValue()));
        this.mEcritures.put("CREDIT", new Long(0));
        ajoutEcriture();

        // Règlement
        String s = "Vente comptoir ";
        final String name = saisieRow.getObject("NOM").toString();
        if (name != null && rowLib.trim().length() > 0) {
            s += name.trim();
        } else {
            s += saisieRow.getForeignRow("ID_ARTICLE").getString("NOM");
        }
        SQLRow modeRegRow = saisieRow.getForeignRow("ID_MODE_REGLEMENT");

        new GenerationReglementVenteNG(s, clientRow, prixTTC, this.date, modeRegRow, saisieRow, mvtTable.getRow(this.idMvt));

        // On place le nuemro de mouvement associe à la saisie
        SQLRowValues valSaisieVC = new SQLRowValues(GenerationMvtSaisieVenteComptoir.saisieVCTable);
        valSaisieVC.put("ID_MOUVEMENT", new Integer(this.idMvt));

        try {
            if (valSaisieVC.getInvalid() == null) {
                // ajout de l'ecriture
                valSaisieVC.update(this.idSaisieVenteComptoir);
            }
        } catch (SQLException e) {
            System.err.println("Erreur à l'insertion dans la table " + valSaisieVC.getTable().getName() + " : " + e);
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            genereMouvement();
        } catch (IllegalArgumentException e) {
            ExceptionHandler.handle("Erreur pendant la générations des écritures comptables", e);
            e.printStackTrace();
        }
    }
}
