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
 
 package org.openconcerto.ui.coreanimation;

import org.openconcerto.ui.DisplayabilityListener;
import org.openconcerto.ui.component.ComboComponent;
import org.openconcerto.ui.component.text.TextComponent;

import java.awt.Component;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.WeakHashMap;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

public class Animator {
    private Pulse[] tmpAnims = new Pulse[] {};
    private final Vector<Pulse> animators;
    private Thread thread;
    private boolean isAnimating = true;

    private final Map<Component, Pulse> activeComps;
    private final Map<Component, Object> managedComps;
    private final DisplayabilityListener displayabilityListener;

    static final Animator instance = new Animator();

    private Animator() {
        this.animators = new Vector<Pulse>();
        this.activeComps = new HashMap<Component, Pulse>();
        this.managedComps = new WeakHashMap<Component, Object>();
        this.displayabilityListener = new DisplayabilityListener() {
            @Override
            protected void displayabilityChanged(Component c) {
                updateComp(c);
            }
        };
    }

    public synchronized static final Animator getInstance() {
        return instance;
    }

    private void pulse() {
        // no need to sync tmpAnims, only used by our thread
        final int size;
        synchronized (this.animators) {
            size = this.animators.size();
            // otherwise tmpAnims will never shrink
            if (this.tmpAnims.length > size + 128)
                this.tmpAnims = new Pulse[size + 8];
            this.tmpAnims = this.animators.toArray(this.tmpAnims);
        }
        for (int i = 0; i < size; i++) {
            this.tmpAnims[i].pulse();
            // don't hold onto references
            this.tmpAnims[i] = null;
        }
    }

    synchronized void setAnimating(boolean isAnimating) {
        this.isAnimating = isAnimating;
    }

    synchronized public boolean isAnimating() {
        return this.isAnimating;
    }

    public void stopAnimation() {
        this.setAnimating(false);
    }

    public void startAnimation() {
        this.setAnimating(true);

    }

    /**
     * Remove the passed pulse.
     * 
     * @param p the pulse to remove, can be <code>null</code>.
     * @return <code>true</code> if <code>p</code> was actually removed.
     */
    public synchronized boolean remove(Pulse p) {
        if (this.animators.remove(p)) {
            p.resetState();
            return true;
        }
        return false;
    }

    public void remove(Component c) {
        this.remove(c, true);
    }

    private void remove(Component c, boolean completely) {
        remove(this.activeComps.remove(c));
        if (completely) {
            c.removeHierarchyListener(this.displayabilityListener);
            this.managedComps.remove(c);
        }
    }

    private void put(Component f, Pulse pulse) {
        this.activeComps.put(f, pulse);
        this.add(pulse);
    }

    public synchronized void add(Pulse p) {
        if (p == null)
            throw new NullPointerException("null Pulse");
        // System.err.println(this.animators);
        if (this.animators.contains(p)) {
            return;
        }
        this.animators.add(p);
        if (thread == null) {
            thread = new Thread(new Runnable() {

                public void run() {
                    while (isAnimating()) {
                        try {
                            pulse();
                            Thread.sleep(100, 0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

            }, this.getClass().getSimpleName() + " pulse");
            thread.setName("Background Animator");
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        JFrame f = new JFrame();
        f.getContentPane().setLayout(new FlowLayout());
        f.getContentPane().add(new JLabel("Test"));
        final JTextField textField = new JTextField(20);
        Animator anim = Animator.getInstance();
        anim.animate(textField);

        f.getContentPane().add(textField);

        final JCheckBox textField2 = new JCheckBox("Checkme");

        anim.animate(textField2);
        f.getContentPane().add(textField2);
        //
        final JRadioButton radio = new JRadioButton("Checkme");
        anim.animate(radio);
        f.getContentPane().add(radio);
        //
        final JComboBox combo = new JComboBox(new String[] { "Hello", "You" });
        anim.animate(combo);
        f.getContentPane().add(combo);
        //
        final JComboBox combo2 = new JComboBox(new String[] { "Hey", "You" });
        combo2.setEditable(true);
        anim.animate(combo2);
        f.getContentPane().add(combo2);
        //
        final JTextArea tx = new JTextArea("eeeeeeeeee dds  ");
        anim.animate(tx);
        f.getContentPane().add(tx);
        //
        f.pack();
        f.setLocation(10, 10);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        textField.setText("bad");
        f.setVisible(true);
    }

    public void animate(Component f, boolean animate) {
        if (animate)
            this.animate(f);
        else
            this.remove(f);
    }

    /**
     * Try to animate f.
     * 
     * @param f the component to animate.
     * @return <code>true</code> if <code>f</code> is animated.
     */
    public boolean animate(Component f) {
        if (this.managedComps.containsKey(f))
            return this.activeComps.containsKey(f);

        this.managedComps.put(f, null);
        f.addHierarchyListener(this.displayabilityListener);
        return updateComp(f);
    }

    private final boolean updateComp(Component f) {
        boolean res = false;
        if (f.isDisplayable()) {
            final Component pc = f instanceof Pulseable ? ((Pulseable) f).getPulseComponent() : f;
            final Pulse p = this.createPulse(pc);
            if (p != null) {
                this.put(f, p);
                res = true;
            }
        } else {
            // continue to listen to it
            this.remove(f, false);
        }
        return res;
    }

    private Pulse createPulse(Component f) {
        Pulse res = null;
        if (f instanceof JToggleButton) {
            res = new JComponentForegroundAnimator(((JToggleButton) f));
            // combos before Text as editable combo can expose their editor
        } else if (f instanceof JComboBox) {
            res = createComboPulse((JComboBox) f);
        } else if (f instanceof ComboComponent && ((ComboComponent) f).getComboComp() != null) {
            res = createComboPulse(((ComboComponent) f).getComboComp());
        } else if (f instanceof JTextComponent) {
            res = new JComponentBackGroundAnimator(((JTextComponent) f));
        } else if (f instanceof TextComponent && bkgdUseable((TextComponent) f)) {
            res = new JComponentBackGroundAnimator(((TextComponent) f).getTextComp());
        } else if (f instanceof JPanel) {
            res = new JComponentBackGroundAnimator((JPanel) f);
        }
        return res;
    }

    // don't try to animate an invisible background
    private final boolean bkgdUseable(TextComponent t) {
        // from http://java.sun.com/products/jfc/tsc/articles/painting/ :
        // opaque == false : The component makes no guarantees about painting all the bits within
        // its rectangular bounds ; i.e. it might still paint the majority
        // Avec Nimbus, un TextComponent peut etre Opaque==false pour masquer ses bords,
        // cependant son background est bien dessiné
        return t.getTextComp() != null && (t.getTextComp().isOpaque() || UIManager.getLookAndFeel().getName().equals("Mac OS X") || UIManager.getLookAndFeel().getName().equals("Nimbus"));
    }

    private final Pulse createComboPulse(JComboBox comp) {
        if (comp.isEditable()) {
            final JComponent editor = (JComponent) comp.getEditor().getEditorComponent();
            // otherwise no background
            editor.setOpaque(true);
            return new JComponentBackGroundAnimator(editor);
        } else {
            return new JComponentBackGroundAnimator(comp);
        }
    }

    public void resetState() {
        synchronized (animators) {
            for (Pulse t : animators) {
                t.resetState();
            }
        }
    }
}
