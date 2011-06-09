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

import org.openconcerto.erp.core.finance.accounting.element.JournalSQLElement;
import org.openconcerto.utils.ExceptionHandler;

import java.util.Date;

public class GenerationMvtVirement extends GenerationEcritures {

    private int id_compte_depart, id_compte_arrive, id_journal;
    private long debit, credit;
    private String labelPiece;
    private boolean clearMvt = true;

    /***********************************************************************************************
     * 
     * @param id_compte_depart
     * @param id_compte_arrive
     * @param debit debit a preleve sur le compte de depart
     * @param credit credit a preleve sur le compte de depart
     * @param label
     */
    public GenerationMvtVirement(int id_compte_depart, int id_compte_arrive, long debit, long credit, String label) {

        this(id_compte_depart, id_compte_arrive, debit, credit, label, new Date());
    }

    public GenerationMvtVirement(int id_compte_depart, int id_compte_arrive, long debit, long credit, String label, Date d) {

        this(id_compte_depart, id_compte_arrive, debit, credit, label, d, JournalSQLElement.BANQUES, label);
    }

    public GenerationMvtVirement(int id_compte_depart, int id_compte_arrive, long debit, long credit, String label, Date d, int idJournal, String labelPiece) {

        this.id_compte_depart = id_compte_depart;
        this.id_compte_arrive = id_compte_arrive;
        this.id_journal = idJournal;
        this.debit = debit;
        this.credit = credit;
        this.nom = label;
        this.date = d;
        this.idPiece = -1;
        this.labelPiece = labelPiece;

    }

    /**
     * Permet de générer plusieurs virement pour une meme piece
     * 
     * @param id_compte_depart
     * @param id_compte_arrive
     * @param debit
     * @param credit
     * @param label
     * @param d
     * @param idJournal
     */
    public void setValues(int id_compte_depart, int id_compte_arrive, long debit, long credit, String label, Date d, int idJournal, boolean clearMvt) {
        this.id_compte_depart = id_compte_depart;
        this.id_compte_arrive = id_compte_arrive;
        this.id_journal = idJournal;
        this.debit = debit;
        this.credit = credit;
        this.nom = label;
        this.date = d;
        this.clearMvt = clearMvt;
    }

    public int genereMouvement() {

        // iniatilisation des valeurs de la map
        this.mEcritures.put("DATE", this.date);
        this.mEcritures.put("NOM", this.nom);
        this.mEcritures.put("ID_JOURNAL", this.id_journal);
        this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(1));

        // on calcule le nouveau numero de mouvement
        if (this.idPiece <= 1) {
            getNewMouvement("", 1, 1, this.labelPiece);
        } else {
            if (clearMvt || this.idMvt <= 1) {
                getNewMouvement("", 1, 1, this.idPiece);
            }
        }
        this.mEcritures.put("ID_MOUVEMENT", this.idMvt);

        // generation des ecritures + maj des totaux du compte associe
        try {
            // compte Départ
            this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(this.id_compte_depart));
            this.mEcritures.put("DEBIT", Long.valueOf(this.debit));
            this.mEcritures.put("CREDIT", Long.valueOf(this.credit));
            ajoutEcriture();

            // compte arrivé
            this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(this.id_compte_arrive));
            this.mEcritures.put("DEBIT", Long.valueOf(this.credit));
            this.mEcritures.put("CREDIT", Long.valueOf(this.debit));
            ajoutEcriture();
        } catch (IllegalArgumentException e) {
            ExceptionHandler.handle("Erreur pendant la générations des écritures comptables", e);
            e.printStackTrace();
        }

        return this.idMvt;
    }
}
