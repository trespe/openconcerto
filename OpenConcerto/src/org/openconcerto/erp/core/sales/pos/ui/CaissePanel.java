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

import org.openconcerto.erp.core.sales.pos.model.Article;
import org.openconcerto.erp.core.sales.pos.model.Categorie;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

public class CaissePanel extends JPanel implements CaisseListener {
    private CaisseControler controler;

    private StatusBar st;

    public CaissePanel(final CaisseFrame caisseFrame) {
        this.setLayout(new GridBagLayout());
        this.setBackground(Color.WHITE);
        this.setOpaque(isOpaque());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;

        this.controler = new CaisseControler(caisseFrame);

        c.fill = GridBagConstraints.HORIZONTAL;
        this.st = new StatusBar("toolbar.png", "toolbar_menu.png");
        this.st.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getX() < 110) {
                    // Valider
                    try {
                        CaissePanel.this.controler.printTicket();
                        CaissePanel.this.controler.printTicket();
                    } catch (Throwable ex) {
                        ExceptionHandler.handle("Erreur d'impression du ticket", ex);
                    }
                    try {
                        CaissePanel.this.controler.saveAndClearTicket();
                    } catch (Throwable ex) {
                        ExceptionHandler.handle("Erreur de sauvegardes des informations du ticket", ex);
                    }

                } else if (e.getX() > 165 && e.getX() < 275) {
                    // Menu
                    try {
                        caisseFrame.showMenu();
                    } catch (Throwable ex) {
                        ExceptionHandler.handle("Erreur d'affichage du menu", ex);
                    }
                }

            }
        });
        this.st.setPrevious(true);
        this.add(this.st, c);

        TicketPanel t = new TicketPanel(this.controler);
        // fillExampleArticle();
        loadArticles();
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.weighty = 1;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.SOUTHWEST;
        c.fill = GridBagConstraints.NONE;
        this.add(t, c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx++;
        c.weightx = 1;
        c.gridy--;
        c.gridheight = 2;
        this.add(new ArticleSelectorPanel(this.controler), c);

        c.gridx++;
        c.weightx = 0;
        this.add(new PaiementPanel(this.controler), c);
        this.controler.addCaisseListener(this);
    }

    private void loadArticles() {

        Map<Integer, Categorie> m = new HashMap<Integer, Categorie>();

        SQLElement eltFam = Configuration.getInstance().getDirectory().getElement("FAMILLE_ARTICLE");
        SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement("ARTICLE");

        SQLSelect selFamille = new SQLSelect(Configuration.getInstance().getBase());

        selFamille.addSelectStar(eltFam.getTable());
        selFamille.addRawOrder(eltFam.getTable().getField("CODE").getFieldRef());
        List<SQLRow> l = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(selFamille.asString(), SQLRowListRSH.createFromSelect(selFamille, eltFam.getTable()));

        for (SQLRow row : l) {

            final Categorie cP = m.get(row.getInt("ID_FAMILLE_ARTICLE_PERE"));
            Categorie c;
            if (cP != null) {
                c = new Categorie(row.getString("NOM"));
                cP.add(c);
            } else {
                c = new Categorie(row.getString("NOM"), true);
            }

            m.put(row.getID(), c);
        }

        SQLSelect selArticle = new SQLSelect(Configuration.getInstance().getBase());
        selArticle.addSelectStar(eltArticle.getTable());
        List<SQLRow> l2 = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(selArticle.asString(), SQLRowListRSH.createFromSelect(selArticle, eltArticle.getTable()));

        for (SQLRow row : l2) {

            final Categorie s1 = m.get(row.getInt("ID_FAMILLE_ARTICLE"));
            if (s1 != null) {
                Article a = new Article(s1, row.getString("NOM"), row.getID());
                a.setBarCode(row.getString("CODE_BARRE"));
                a.setCode(row.getString("CODE"));
                a.setIdTaxe(row.getInt("ID_TAXE"));
                a.setPriceHTInCents((BigDecimal) row.getObject("PV_HT"));
                a.setPriceInCents((BigDecimal) row.getObject("PV_TTC"));

            }
        }

    }

    private void fillExampleArticle() {
        //
        Categorie c1 = new Categorie("Carte postales", true);
        Categorie s1 = new Categorie("Baie de Somme");
        c1.add(s1);
        Categorie s2 = new Categorie("Villes");
        c1.add(s2);

        Categorie c2 = new Categorie("Livres", true);

        Categorie s2_1 = new Categorie("Livre Baie");
        c2.add(s2_1);
        Categorie s2_2 = new Categorie("Livre Abbeville");
        c2.add(s2_2);

        for (int i = 0; i < 100; i++) {
            final Article al = new Article(s2_2, "ILM" + i, 1);
            al.setPriceInCents(new BigDecimal(5000));
            al.setBarCode("INFORMATIQUE " + i);
        }
        final Article a = new Article(s1, "012345678901234567890123456789", 1);
        a.setPriceInCents(new BigDecimal(106));
        final Article b = new Article(s1, "St Valery", 1);
        b.setBarCode("ST VALERY");
        b.setPriceInCents(new BigDecimal(300));
        final Article cc = new Article(s1, "Cayeux", 1);
        cc.setPriceInCents(new BigDecimal(300));
        final Article v1 = new Article(s2, "Abbeville", 1);
        v1.setPriceInCents(new BigDecimal(106));
        final Article v2 = new Article(s2, "Amiens", 1);
        v2.setPriceInCents(new BigDecimal(300));
        final Article v3 = new Article(s2, "Rue", 1);
        v3.setPriceInCents(new BigDecimal(300));
        this.controler.addArticle(a);
        this.controler.addArticle(a);
        this.controler.addArticle(b);
        this.controler.addArticle(cc);
    }

    @Override
    public void paint(Graphics g) {
        System.err.println("CaissePanel.paint()" + this.getWidth() + " x " + this.getHeight());
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Prix
        int x = 300;
        int y = 110;
        String euros;
        String cents;
        Rectangle2D r;
        g.setFont(g.getFont().deriveFont(66f));
        final int total = this.controler.getTotal();
        euros = CaisseControler.getEuros(total) + ".";
        cents = CaisseControler.getCents(total);
        r = g.getFontMetrics().getStringBounds(euros, g);
        x = x - (int) r.getWidth();
        g.drawString(euros, x, y);
        g.setFont(g.getFont().deriveFont(40f));
        g.drawString(cents, x + (int) r.getWidth(), y);
        // Paiement
        y += 40;
        x = 300;
        final int paye = this.controler.getPaidTotal();
        euros = CaisseControler.getEuros(paye) + ".";
        cents = CaisseControler.getCents(paye);

        g.setFont(g.getFont().deriveFont(18f));
        Rectangle2D r2 = g.getFontMetrics().getStringBounds("Payé", g);
        if (paye >= total) {
            g.setColor(Color.DARK_GRAY);
        } else {
            g.setColor(Color.ORANGE);
        }
        g.setFont(g.getFont().deriveFont(32f));
        r = g.getFontMetrics().getStringBounds(euros, g);
        g.drawString(euros, x - (int) r.getWidth(), y);
        g.setFont(g.getFont().deriveFont(24f));
        g.drawString(cents, x, y);
        g.setFont(g.getFont().deriveFont(18f));
        g.setColor(Color.GRAY);
        g.drawString("Payé", x - (int) r2.getWidth() - (int) r.getWidth() - 10, y);
        // A rendre
        final boolean minimalHeight = this.getHeight() < 750;
        if (!minimalHeight) {
            y += 40;
            x = 300;
        } else {
            x = 140;
        }
        int aRendre = paye - total;
        if (aRendre != 0) {
            String label;
            if (aRendre > 0) {
                label = "Rendu";
            } else {
                if (!minimalHeight) {
                    label = "Reste à payer";
                } else {
                    label = "Doit";
                }
                aRendre = -aRendre;
            }

            euros = CaisseControler.getEuros(aRendre) + ".";
            cents = CaisseControler.getCents(aRendre);

            g.setFont(g.getFont().deriveFont(18f));
            Rectangle2D r3 = g.getFontMetrics().getStringBounds(label, g);

            g.setColor(Color.DARK_GRAY);
            g.setFont(g.getFont().deriveFont(32f));
            r = g.getFontMetrics().getStringBounds(euros, g);
            g.drawString(euros, x - (int) r.getWidth(), y);
            g.setFont(g.getFont().deriveFont(24f));
            g.drawString(cents, x, y);
            g.setFont(g.getFont().deriveFont(18f));
            g.setColor(Color.GRAY);
            g.drawString(label, x - (int) r3.getWidth() - (int) r.getWidth() - 10, y);

        }

    }

    @Override
    public void caisseStateChanged() {
        repaint();
    }
}
