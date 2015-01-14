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

import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.pos.model.Article;
import org.openconcerto.ui.touch.ScrollableList;
import org.openconcerto.utils.Pair;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

public class TicketCellRenderer implements ListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Pair<Article, Integer> item = (Pair<Article, Integer>) value;
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);
        final JLabel l1 = new JLabel(item.getSecond().toString(), SwingConstants.RIGHT);

        p.add(l1, c);
        c.gridx++;
        c.weightx = 1;
        Article article = item.getFirst();
        final JLabel l2 = new JLabel(article.getName().toUpperCase(), SwingConstants.LEFT);
        p.add(l2, c);
        c.gridx++;
        c.weightx = 0;

        Float tauxFromId = TaxeCache.getCache().getTauxFromId(article.getIdTaxe());
        BigDecimal tauxTVA = new BigDecimal(tauxFromId).movePointLeft(2).add(BigDecimal.ONE);

        BigDecimal multiply = article.getPriceHTInCents().multiply(new BigDecimal(item.getSecond()), MathContext.DECIMAL128).multiply(tauxTVA, MathContext.DECIMAL128);
        final JLabel l3 = new JLabel(centsToString(multiply.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue()), SwingConstants.RIGHT);
        p.add(l3, c);

        //
        l1.setOpaque(false);
        l2.setOpaque(false);
        l3.setOpaque(false);

        if (isSelected) {
            p.setOpaque(true);
            p.setBackground(new Color(232, 242, 254));
        } else {
            p.setOpaque(false);
        }
        // l2.setFont(f);
        l1.setFont(new Font("Arial", Font.PLAIN, 18));
        l2.setFont(new Font("Arial", Font.PLAIN, 18));
        l3.setFont(new Font("Arial", Font.PLAIN, 18));

        return p;
    }

    public void paint(Graphics g, ScrollableList list, Object value, int index, boolean isSelected) {
        @SuppressWarnings("unchecked")
        final Pair<Article, Integer> item = (Pair<Article, Integer>) value;

        if (isSelected) {
            g.setColor(new Color(232, 242, 254));
            g.fillRect(0, 0, list.getWidth(), list.getCellHeight());
        }
        g.setColor(Color.BLACK);

        final int inset = 5;
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        final int height = g.getFontMetrics().getMaxAscent() + g.getFontMetrics().getMaxDescent() + inset;

        final String s1 = item.getSecond().toString();
        g.drawString(s1, inset, height);
        final int width1 = (int) g.getFontMetrics().getStringBounds("999 ", g).getWidth() + inset * 2;

        Article article = item.getFirst();
        String s2 = article.getName().toUpperCase();
        final int maxLength = 13;
        if (s2.length() > maxLength)
            s2 = s2.substring(0, maxLength + 1) + '…';
        g.drawString(s2, width1 + inset, height);

        Float tauxFromId = TaxeCache.getCache().getTauxFromId(article.getIdTaxe());
        BigDecimal tauxTVA = new BigDecimal(tauxFromId).movePointLeft(2).add(BigDecimal.ONE);

        BigDecimal multiply = article.getPriceHTInCents().multiply(new BigDecimal(item.getSecond()), MathContext.DECIMAL128).multiply(tauxTVA, MathContext.DECIMAL128);
        final String s3 = centsToString(multiply.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue());
        final int width3 = (int) g.getFontMetrics().getStringBounds(s3, g).getWidth() + inset * 2;
        g.drawString(s3, list.getWidth() - width3, height);
    }

    public static String centsToString(int cents) {
        final int c = cents % 100;
        String sc = String.valueOf(c);
        if (c < 10) {
            sc = "0" + sc;
        }

        return cents / 100 + "." + sc;
    }

}
