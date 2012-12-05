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

import org.openconcerto.erp.core.finance.payment.element.TypeReglementSQLElement;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.pos.Caisse;
import org.openconcerto.erp.core.sales.pos.model.Article;
import org.openconcerto.erp.core.sales.pos.model.Categorie;
import org.openconcerto.erp.core.sales.pos.model.Paiement;
import org.openconcerto.erp.core.sales.pos.model.Ticket;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;

public class TextAreaTicketPanel extends JPanel {

    public TextAreaTicketPanel(SQLRow row) {
        super(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0;
        c.weighty = 0;

        final Ticket ticket = createTicket(row);

        JButton button = new JButton("Imprimer");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ticket.print(Caisse.getTicketPrinter());

            }
        });

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        this.add(button, c);

        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        JSeparator sep = new JSeparator();
        c.gridy++;
        c.weightx = 1;
        this.add(sep, c);
        final TextAreaTicketPrinter comp = new TextAreaTicketPrinter();
        c.gridy++;
        c.weighty = 1;
        this.add(comp, c);

        ticket.print(comp);
    }

    private Ticket createTicket(SQLRow row) {
        Ticket t = new Ticket(row.getInt("ID_CAISSE"));
        // t.setNumber(Integer.valueOf(row.getString("NUMERO")));
        t.setDate(row.getDate("DATE").getTime());

        SQLElement eltEncaisser = Configuration.getInstance().getDirectory().getElement("ENCAISSER_MONTANT");
        List<SQLRow> l = row.getReferentRows(eltEncaisser.getTable());
        for (SQLRow row2 : l) {
            long montant = row2.getLong("MONTANT");
            SQLRow rowMode = row2.getForeign("ID_MODE_REGLEMENT");
            int type = Paiement.CB;
            if (rowMode.getInt("ID_TYPE_REGLEMENT") == TypeReglementSQLElement.CB) {
                type = Paiement.CB;
            } else {
                if (rowMode.getInt("ID_TYPE_REGLEMENT") == TypeReglementSQLElement.CHEQUE) {
                    type = Paiement.CHEQUE;
                } else {
                    if (rowMode.getInt("ID_TYPE_REGLEMENT") == TypeReglementSQLElement.ESPECE) {
                        type = Paiement.ESPECES;
                    }
                }
            }
            Paiement p = new Paiement(type);
            p.setMontantInCents((int) montant);
            t.addPaiement(p);
        }

        SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");
        List<SQLRow> l2 = row.getReferentRows(eltArticle.getTable());
        Categorie c = new Categorie("");
        for (SQLRow row2 : l2) {
            Article a = new Article(c, row2.getString("NOM"), row2.getInt("ID_ARTICLE"));
            BigDecimal ht = (BigDecimal) row2.getObject("PV_HT");
            a.setPriceHTInCents(ht);
            int idTaxe = row2.getInt("ID_TAXE");
            float tva = TaxeCache.getCache().getTauxFromId(idTaxe);
            a.setPriceInCents(ht.multiply(new BigDecimal(1.0 + (tva / 100.0D)), MathContext.DECIMAL128));
            a.setIdTaxe(idTaxe);
            t.addArticle(a);
            t.setArticleCount(a, row2.getInt("QTE"));
        }

        return t;
    }
}
