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
 
 package org.openconcerto.erp.core.sales.pos.ui;

import org.openconcerto.erp.core.sales.pos.Caisse;
import org.openconcerto.erp.core.sales.pos.io.BarcodeReader;
import org.openconcerto.erp.core.sales.pos.io.TicketPrinter;
import org.openconcerto.erp.core.sales.pos.model.Article;
import org.openconcerto.erp.core.sales.pos.model.Paiement;
import org.openconcerto.erp.core.sales.pos.model.Ticket;
import org.openconcerto.utils.Pair;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class CaisseControler implements BarcodeListener {

    private Article articleSelected;
    private Paiement paiementSelected;
    private Ticket t;
    private List<CaisseListener> listeners = new ArrayList<CaisseListener>();

    private BarcodeReader r;
    private Paiement p1 = new Paiement(Paiement.ESPECES);
    private Paiement p2 = new Paiement(Paiement.CB);
    private Paiement p3 = new Paiement(Paiement.CHEQUE);
    private CaisseFrame caisseFrame;

    public CaisseControler(CaisseFrame caisseFrame) {
        this.caisseFrame = caisseFrame;
        this.t = new Ticket(Caisse.getID());

        this.t.addPaiement(this.p1);
        this.t.addPaiement(this.p2);
        this.t.addPaiement(this.p3);

        this.r = new BarcodeReader();
        this.r.start();
        this.r.addBarcodeListener(this);

    }

    public Article getArticleSelected() {
        return this.articleSelected;
    }

    public Paiement getPaiementSelected() {
        return this.paiementSelected;
    }

    void setArticleSelected(Article a) {
        System.out.println("CaisseControler.setArticleSelected() :  " + a);
        this.articleSelected = a;
        this.paiementSelected = null;
        fire();
    }

    void setPaiementSelected(Paiement p) {
        System.out.println("CaisseControler.setPaiementSelected() :  " + p);
        this.paiementSelected = p;
        this.articleSelected = null;
        fire();
    }

    // Listeners
    private void fire() {
        int stop = this.listeners.size();
        for (int i = 0; i < stop; i++) {
            this.listeners.get(i).caisseStateChanged();
        }
    }

    void addCaisseListener(CaisseListener l) {
        this.listeners.add(l);
    }

    // Articles
    void addArticle(Article a) {
        this.t.addArticle(a);
        fire();
    }

    void incrementArticle(Article a) {
        this.t.incrementArticle(a);
        fire();
    }

    void removeArticle(Article a) {
        this.t.removeArticle(a);
        fire();
    }

    // Paiements
    public List<Paiement> getPaiements() {
        return this.t.getPaiements();
    }

    public void addPaiement(Paiement p) {
        this.t.addPaiement(p);
        fire();
    }

    public void clearPaiement(Paiement paiement) {
        if (this.p1.equals(paiement) || this.p2.equals(paiement) || this.p3.equals(paiement)) {

            paiement.setMontantInCents(0);
        }
        fire();
    }

    public void setPaiementValue(Paiement paiement, int v) {
        paiement.setMontantInCents(v);
        fire();

    }

    // Totaux
    public int getTotal() {
        return this.t.getTotal();

    }

    public int getPaidTotal() {
        return this.t.getPaidTotal();
    }

    //

    public List<Pair<Article, Integer>> getItems() {
        return this.t.getArticles();
    }

    public int getItemCount(Article article) {
        return this.t.getItemCount(article);

    }

    public void clearArticle(Article article) {
        this.t.clearArticle(article);
        this.setArticleSelected(null);
    }

    public void setArticleCount(Article article, int count) {
        this.t.setArticleCount(article, count);
        this.setArticleSelected(null);
    }

    @Override
    public void barcodeRead(String code) {
        if (code.equalsIgnoreCase("especes")) {
            autoFillPaiement(this.p1);

        } else if (code.equalsIgnoreCase("cb")) {
            autoFillPaiement(this.p2);

        } else if (code.equalsIgnoreCase("cheque")) {
            autoFillPaiement(this.p3);

        } else if (code.equalsIgnoreCase("annuler")) {
            if (this.articleSelected != null) {
                this.clearArticle(this.articleSelected);

            } else {
                if (this.paiementSelected != null) {
                    this.paiementSelected.setMontantInCents(0);
                    // setPaiementSelected(null);
                    fire();
                }

            }
        } else if (code.equalsIgnoreCase("valider")) {

        } else if (code.equalsIgnoreCase("facture")) {

        } else if (code.equalsIgnoreCase("ticket")) {

        } else {
            Article a = Article.getArticleFromBarcode(code);
            if (a != null) {
                this.incrementArticle(a);
                this.setArticleSelected(a);
            }
            Ticket t = Ticket.getTicketFromCode(code);
            if (t != null) {
                caisseFrame.showTickets(t);
            }
        }

    }

    void autoFillPaiement(Paiement p) {
        int montant = p.getMontantInCents();

        p.setMontantInCents(getTotal() - getPaidTotal() + montant);
        setPaiementSelected(p);
    }

    void addBarcodeListener(BarcodeListener l) {
        this.r.addBarcodeListener(l);
    }

    public boolean canAddPaiement(int type) {
        final int paiementCount = this.t.getPaiements().size();
        if (paiementCount >= 6)
            return false;
        for (int i = 0; i < paiementCount; i++) {
            Paiement p = this.t.getPaiements().get(i);
            if (p.getType() == type && p.getMontantInCents() <= 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void keyReceived(KeyEvent ee) {
        // TODO Auto-generated method stub

    }

    public static String getCents(int cents) {
        String s = String.valueOf(cents % 100);
        if (s.length() < 2) {
            s = "0" + s;
        }
        return s;
    }

    public static String getEuros(int cents) {
        String s = String.valueOf(cents / 100);

        return s;
    }

    public void saveAndClearTicket() {
        if (this.t.getTotal() > 0) {
            if (this.getPaidTotal() >= this.getTotal()) {
                this.t.save();
                t = new Ticket(Caisse.getID());
                p1 = new Paiement(Paiement.ESPECES);
                p2 = new Paiement(Paiement.CB);
                p3 = new Paiement(Paiement.CHEQUE);
                this.t.addPaiement(this.p1);
                this.t.addPaiement(this.p2);
                this.t.addPaiement(this.p3);
                this.setPaiementSelected(null);
                this.setArticleSelected(null);

            }
        }
    }

    public int getTicketNumber() {
        return this.t.getNumber();
    }

    public void printTicket() {
        if (this.t.getTotal() > 0) {
            if (this.getPaidTotal() >= this.getTotal()) {
                TicketPrinter prt = Caisse.getTicketPrinter();
                t.print(prt);
            } else {
                System.err.println("Ticket not printed because not paid");
            }
        } else {
            System.err.println("Ticket not printed total <= 0");
        }
    }

    public void openDrawer() {
        try {
            TicketPrinter prt = Caisse.getTicketPrinter();
            prt.openDrawer();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
