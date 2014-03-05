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

import org.openconcerto.laf.LAFUtils;
import org.openconcerto.ui.DisplayabilityListener;
import org.openconcerto.ui.component.ComboComponent;
import org.openconcerto.ui.component.ComboLockedMode;
import org.openconcerto.ui.component.JRadioButtons.JStringRadioButtons;
import org.openconcerto.ui.component.combo.ISearchableTextCombo;
import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.utils.model.DefaultIListModel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.WeakHashMap;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

import net.jcip.annotations.GuardedBy;

public class Animator {

    // avoid allocating a new array every pulse(), in EDT
    private Pulse[] tmpAnims = new Pulse[] {};
    @GuardedBy("animators")
    private final Vector<Pulse> animators;
    @GuardedBy("this")
    private Thread thread = null;
    @GuardedBy("this")
    private boolean isAnimating = true;

    // all in EDT
    private final Map<Component, Pulse> activeComps;
    private final Map<Component, Object> managedComps;
    private final DisplayabilityListener displayabilityListener;

    static private final Animator instance = new Animator();

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
        assert SwingUtilities.isEventDispatchThread();
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
    public boolean remove(Pulse p) {
        assert SwingUtilities.isEventDispatchThread();
        final boolean res;
        synchronized (this.animators) {
            res = this.animators.remove(p);
        }
        if (res) {
            p.resetState();
        }
        return res;
    }

    public void remove(Component c) {
        this.remove(c, true);
    }

    private void remove(Component c, boolean completely) {
        assert SwingUtilities.isEventDispatchThread();
        remove(this.activeComps.remove(c));
        if (completely) {
            c.removeHierarchyListener(this.displayabilityListener);
            this.managedComps.remove(c);
        }
    }

    private void put(Component f, Pulse pulse) {
        assert SwingUtilities.isEventDispatchThread();
        this.activeComps.put(f, pulse);
        this.add(pulse);
    }

    public synchronized void add(Pulse p) {
        if (p == null)
            throw new NullPointerException("null Pulse");
        synchronized (this.animators) {
            if (this.animators.contains(p)) {
                return;
            }
            this.animators.add(p);
            this.animators.notify();
        }
        if (this.thread == null) {
            this.thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    final Runnable pulseRunnable = new Runnable() {
                        @Override
                        public void run() {
                            pulse();
                        }
                    };
                    while (isAnimating()) {
                        try {
                            // only call invokeLater() when necessary otherwise this prevents the
                            // JVM from exiting (plus it slows down Swing for nothing)
                            synchronized (Animator.this.animators) {
                                while (Animator.this.animators.size() == 0)
                                    Animator.this.animators.wait();
                            }
                            SwingUtilities.invokeLater(pulseRunnable);
                            Thread.sleep(100, 0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

            }, this.getClass().getSimpleName() + " pulse");
            this.thread.setName("Background Animator");
            this.thread.setDaemon(true);
            this.thread.start();
        }
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
        assert SwingUtilities.isEventDispatchThread();
        if (this.managedComps.containsKey(f))
            return this.activeComps.containsKey(f);

        this.managedComps.put(f, null);
        f.addHierarchyListener(this.displayabilityListener);
        return updateComp(f);
    }

    private final boolean updateComp(Component f) {
        assert SwingUtilities.isEventDispatchThread();
        boolean res = false;
        if (f.isDisplayable()) {
            // FIXME f can be displayable but not yet initialized (e.g. radio buttons waiting an SQL
            // request)
            final Collection<? extends Component> pc = f instanceof Pulseable ? ((Pulseable) f).getPulseComponents() : Arrays.asList(f);
            final Pulse p = this.createPulseFromList(pc);
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

    private Pulse createPulseFromList(final Collection<? extends Component> comps) {
        final List<Pulse> l = new LinkedList<Pulse>();
        for (final Component c : comps) {
            final Pulse p = this.createPulse(c);
            if (p != null)
                l.add(p);
        }
        return l.size() == 0 ? null : new MultiPulse(l);
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
            // FIXME the editor is changed on updateUI(), so the combo stops glowing
            final JComponent editor = (JComponent) comp.getEditor().getEditorComponent();
            // opaque otherwise no background (at least with the Windows l&f)
            return new JComponentBackGroundAnimator(editor, true);
        } else {
            return new JComponentForegroundAnimator(comp);
        }
    }

    public void resetState() {
        assert SwingUtilities.isEventDispatchThread();
        synchronized (this.animators) {
            for (Pulse t : this.animators) {
                t.resetState();
            }
        }
    }

    static private class MultiPulse implements Pulse {

        private final List<Pulse> l;

        // private since l isn't copied
        private MultiPulse(List<Pulse> l) {
            super();
            this.l = l;
        }

        @Override
        public void pulse() {
            for (final Pulse p : this.l)
                p.pulse();
        }

        @Override
        public void resetState() {
            for (final Pulse p : this.l)
                p.resetState();
        }
    }

    public static void main(String[] args) throws Exception {
        LAFUtils.setLookAndFeel();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                displayFrame();
            }
        });
    }

    static private void displayFrame() {
        final Container comps = new JPanel();

        comps.add(new JLabel("Test"));
        comps.add(new JTextField("bad", 20));
        comps.add(new JCheckBox("Checkme"));
        comps.add(new JStringRadioButtons(Arrays.asList("Checkme", "Or else")));
        comps.add(new JComboBox(new String[] { "Hello", "You" }));

        final JComboBox comboEditable = new JComboBox(new String[] { "Hey", "You" });
        comboEditable.setEditable(true);
        comps.add(comboEditable);

        final ISearchableTextCombo lockedSC = new ISearchableTextCombo(ComboLockedMode.LOCKED, 1, 12);
        lockedSC.initCache(new DefaultIListModel<String>(Arrays.asList("Hey", "You")));
        comps.add(lockedSC);

        comps.add(new JTextArea("eeeeeeeeee dds  "));

        final JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel("Version : " + System.getProperty("java.version") + " ; L&F : " + UIManager.getLookAndFeel().getName()), BorderLayout.PAGE_START);
        p.add(comps, BorderLayout.CENTER);
        final Animator anim = Animator.getInstance();
        p.add(new JButton(new AbstractAction("Start/Stop") {

            private boolean animated = false;

            @Override
            public void actionPerformed(ActionEvent e) {
                this.animated = !this.animated;
                for (final Component c : comps.getComponents())
                    anim.animate(c, this.animated);
            }
        }), BorderLayout.PAGE_END);

        final JFrame f = new JFrame();
        f.setContentPane(p);
        f.pack();
        f.setLocation(10, 10);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }
}
