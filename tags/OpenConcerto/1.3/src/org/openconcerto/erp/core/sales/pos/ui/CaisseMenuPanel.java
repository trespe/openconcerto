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
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class CaisseMenuPanel extends JPanel implements ListSelectionListener {

    private JList l;
    private CaisseFrame frame;
    private Image bg;

    CaisseMenuPanel(CaisseFrame caisseFrame) {
        this.frame = caisseFrame;
        this.setBackground(Color.WHITE);
        this.setOpaque(true);
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        final Font font = new Font("Arial", Font.PLAIN, 46);
        l = new JList(new String[] { "Retour", "", "Liste des tickets", "Clôturer", "", "Fermer le logiciel", "" });
        l.setCellRenderer(new ListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = new JLabel(value.toString()) {
                    @Override
                    public void paint(Graphics g) {

                        super.paint(g);

                        g.setColor(Color.LIGHT_GRAY);
                        g.drawLine(0, 0, this.getWidth(), 0);
                    }
                };
                l.setFont(font);
                return l;
            }

        });
        l.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        l.getSelectionModel().addListSelectionListener(this);

        l.setFixedCellHeight(80);
        this.add(l, c);
        bg = new ImageIcon(TicketPanel.class.getResource("toolbar.png")).getImage();
        setFont(new Font("Arial", Font.BOLD, 24));
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        try {
            final int selectedIndex = l.getSelectedIndex();
            switch (selectedIndex) {
            case 0:
                // Retour
                frame.showCaisse();
                break;
            case 2:
                // Liste des tickets
                frame.showTickets(null);
                break;
            case 3:
                // Clôture
                Caisse.commitAll(Caisse.allTickets());
                break;
            case 5:
                // Fermeture
                frame.dispose();
                Frame[] l = Frame.getFrames();
                for (int i = 0; i < l.length; i++) {
                    Frame f = l[i];
                    System.err.println(f.getName() + " " + f + " Displayable: " + f.isDisplayable() + " Valid: " + f.isValid() + " Active: " + f.isActive());
                }
                Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
                for (Thread thread : threadSet) {
                    if (!thread.isDaemon()) {
                        System.err.println(thread.getName() + " " + thread.getId() + " not daemon");
                    }
                }
                break;
            default:
                break;
            }
        } catch (Exception ex) {
            ExceptionHandler.handle("Erreur", ex);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final int w = this.getWidth();
        int imWidth = bg.getWidth(null);
        for (int x = 0; x <= w; x += imWidth) {
            g.drawImage(bg, x, 0, null);
        }

        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(250, 250, 250));
        String str = "Menu Principal";

        Rectangle2D r = g.getFontMetrics().getStringBounds(str, g);
        int x = (int) (this.getWidth() - r.getWidth()) / 2;
        g.drawString(str, x, 30);
    }
}
