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
 
 package org.openconcerto.erp.core.sales.pos.io;

import org.openconcerto.erp.core.sales.pos.ui.BarcodeListener;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.component.ITextArea;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Lecteur code barres, intercepte les événements clavier pour détecter un scan de code. Le code
 * barre doit terminer par un retour à la ligne.
 * */
public class BarcodeReader implements KeyEventDispatcher {

    public int maxInterKeyDelay = 80;
    private static final int MIN_BARCODE_LENGTH = 2;
    private final List<BarcodeListener> listeners = new ArrayList<BarcodeListener>(1);
    private String value = "";
    private final List<KeyEvent> eve = new ArrayList<KeyEvent>();
    private long firstTime = -1;
    private Timer timer;
    // non final car un TimerTask n'est pas reutilisable
    private TimerTask task;

    public BarcodeReader(int maxInterKeyDelay) {
        this.timer = null;
        this.task = null;
        this.maxInterKeyDelay = maxInterKeyDelay;
    }

    public synchronized void removeBarcodeListener(BarcodeListener l) {
        this.listeners.remove(l);
        if (this.listeners.size() == 0) {
            stop();
        }
    }

    public synchronized void addBarcodeListener(final BarcodeListener l) {
        if (this.timer == null) {
            start();
        }
        this.listeners.add(l);

    }

    private void fire(String code) {
        for (int i = 0; i < this.listeners.size(); i++) {
            this.listeners.get(i).barcodeRead(code);
        }
    }

    /**
     * Commence à ecouter les évenements clavier pour intercepter les codes barres
     * */

    public void start() {
        // init avant que les listeners s'en servent
        this.timer = new Timer(getClass().getName(), true);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    }

    /**
     * Stoppe l'écoute sur les évenements clavier
     * */
    public void stop() {
        if (this.timer != null) {
            this.timer.cancel();
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (this.task != null)
            this.task.cancel();

        final long t = e.getWhen();
        if (this.firstTime < 0) {
            this.firstTime = t;
        }
        int key = e.getKeyCode();

        if (t - this.firstTime > maxInterKeyDelay && key != KeyEvent.VK_SHIFT) {
            // touche normale
            redispatch();
        }

        final char key2 = e.getKeyChar();
        this.eve.add(e);
        if (key != KeyEvent.VK_UNDEFINED && e.getID() == KeyEvent.KEY_RELEASED) {
            if (key == KeyEvent.VK_SHIFT) {
                // rien
            } else if (key2 == '*' || key2 == '$' || key2 == '+' || key2 == '/' || key2 == '%' || key2 == '-' | key2 == ' ') {
                this.value += key2;
            } else if (key >= KeyEvent.VK_0 && key <= KeyEvent.VK_9 || key >= KeyEvent.VK_A && key <= KeyEvent.VK_Z) {
                // from KeyEvent : same as ASCII
                this.value += (char) key;
            } else if (key == KeyEvent.VK_ENTER && this.value.length() >= MIN_BARCODE_LENGTH) {
                // fin de code barre
                this.value = this.value.trim();
                fire(this.value);
                reset();
            } else {
                // Caractere non code barre
                redispatch();
            }
            // lance un timer s'il reste des evenements non dispatchés
            if (this.eve.size() > 0) {
                this.firstTime = t;
                this.task = new TimerTask() {
                    @Override
                    public void run() {
                        redispatchLater();
                    }
                };
                this.timer.schedule(this.task, maxInterKeyDelay);
            }
            // si pas d'evenement, pas de temps associé
            assert this.eve.size() > 0 || this.firstTime == -1;
        }
        return true;
    }

    private void redispatchLater() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                redispatch();
            }
        });
    }

    private void redispatch() {
        for (int i = 0; i < this.eve.size(); i++) {
            final KeyEvent ee = this.eve.get(i);
            KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(ee.getComponent(), ee);
            for (int j = 0; j < this.listeners.size(); j++) {
                this.listeners.get(j).keyReceived(ee);
            }
        }
        reset();
    }

    private void reset() {
        this.value = "";
        this.eve.clear();
        this.firstTime = -1;
    }

    public static void main(String[] args) {
        String delay = "80";
        if (args.length > 0) {
            delay = args[0];
        }
        final int d = Integer.parseInt(delay);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("BarCode reader");
                System.out.println("Using inter key delay: " + d);
                JFrame f = new JFrame();
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                JPanel panel = new JPanel();
                f.setTitle("Barcode reader test");
                f.setContentPane(panel);
                panel.setLayout(new GridBagLayout());
                GridBagConstraints c = new DefaultGridBagConstraints();
                final JLabel l = new JLabel("BarCode reader output :");
                panel.add(l, c);
                c.gridy++;
                c.weighty = 1;
                c.weightx = 1;
                c.fill = GridBagConstraints.BOTH;
                final ITextArea t1 = new ITextArea();
                panel.add(new JScrollPane(t1), c);

                BarcodeReader reader = new BarcodeReader(d);
                reader.addBarcodeListener(new BarcodeListener() {

                    @Override
                    public void keyReceived(KeyEvent ee) {
                        System.err.println("BarcodeReader keyReceived() : " + ee);
                    }

                    @Override
                    public void barcodeRead(String code) {
                        t1.append("Barcode OK : '" + code + "'\n");
                    }
                });

                f.setSize(new Dimension(640, 480));
                f.setLocationRelativeTo(null);
                f.setVisible(true);

            }
        });

    }
}
