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
 
 package org.openconcerto.map.ui;

import org.openconcerto.map.model.Ville;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.component.ComboLockedMode;
import org.openconcerto.ui.component.combo.ISearchableCombo;
import org.openconcerto.ui.component.text.DocumentComponent;
import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.ui.state.WindowStateManager;
import org.openconcerto.ui.valuewrapper.ValueChangeSupport;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidObject;
import org.openconcerto.utils.checks.ValidState;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

public class ITextComboVilleViewer extends JPanel implements ValueWrapper<Ville>, DocumentComponent, TextComponent {
    /**
     * Selecteur de Ville
     */
    private static final long serialVersionUID = 3397210337907076649L;
    private final ISearchableCombo<Ville> text;
    private final JButton button = new JButton("Afficher sur la carte");
    private final JButton buttonAdd;
    private Ville currentVille = null;

    private final ValueChangeSupport<Ville> supp;
    private final ITextComboCacheVille cache;

    public ITextComboVilleViewer() {
        this.setOpaque(false);
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(0, 0, 0, 2);

        this.buttonAdd = new JButton(new ImageIcon(this.getClass().getResource("add.png")));
        this.supp = new ValueChangeSupport<Ville>(this);
        this.cache = new ITextComboCacheVille();

        this.text = new ISearchableCombo<Ville>(ComboLockedMode.LOCKED_ITEMS_UNLOCKED, 0, 17) {

            @Override
            protected Ville stringToT(String t) {
                Ville v = Ville.getVilleFromVilleEtCode(t);                
                return v;
            }
        };

        this.text.setMaxVisibleRows(20);
        this.text.addValueListener(new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent evt) {
                ITextComboVilleViewer.this.supp.fireValueChange();
            }
        });
        this.text.addValidListener(new ValidListener() {
            @Override
            public void validChange(ValidObject src, ValidState newValue) {
                ITextComboVilleViewer.this.supp.fireValidChange();
            }
        });

        final VilleListModel acache = new VilleListModel();
        this.text.initCache(acache);

        // Listen on data
        final PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                acache.fireModify();
            }
        };

        this.addContainerListener(new ContainerListener() {
            @Override
            public void componentRemoved(ContainerEvent e) {
                Ville.removeListener(listener);
            }

            @Override
            public void componentAdded(ContainerEvent e) {
                Ville.addListener(listener);
            }
        });

        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(this.text, c);
        c.weightx = 0;
        c.gridx++;
        this.button.setOpaque(false);
        this.add(this.button, c);
        c.gridx++;
        this.buttonAdd.setPreferredSize(new Dimension(24, 16));
        this.buttonAdd.setBorder(null);
        this.buttonAdd.setOpaque(false);
        this.buttonAdd.setContentAreaFilled(false);
        this.buttonAdd.setFocusPainted(false);
        this.buttonAdd.setFocusable(false);
        this.add(this.buttonAdd, c);
        this.button.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                final JFrame f = new JFrame();
                final MapViewerPanel mapViewerPanel = new MapViewerPanel(true);

                f.setContentPane(mapViewerPanel);
                f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                f.setSize(600, 500);
                f.setMinimumSize(new Dimension(600, 500));
                final File conf = new File(System.getProperty("user.home"), ".java" + File.separator + "ilm" + File.separator + "map" + File.separator);
                new WindowStateManager(f, new File(conf, "Configuration" + File.separator + "MapFrame.properties"), true).loadState();
                f.setVisible(true);
                if (ITextComboVilleViewer.this.currentVille != null) {
                    final long x = ITextComboVilleViewer.this.currentVille.getXLambert();
                    final long y = ITextComboVilleViewer.this.currentVille.getYLambert();
                    f.setTitle(ITextComboVilleViewer.this.currentVille.getName());
                    mapViewerPanel.getVilleRendererPanel().centerScreenXYLambert(x, y);
                    mapViewerPanel.getVilleRendererPanel().setHighlight(ITextComboVilleViewer.this.currentVille);
                    mapViewerPanel.getVilleRendererPanel().setAlwayVisible(ITextComboVilleViewer.this.currentVille);
                }
            }
        });
        this.addValueListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                ITextComboVilleViewer.this.currentVille = evt.getNewValue() == null ? null : Ville.getVilleFromVilleEtCode(evt.getNewValue().toString());
                ITextComboVilleViewer.this.button.setEnabled(ITextComboVilleViewer.this.currentVille != null && ITextComboVilleViewer.this.isEnabled());
            }
        });
        this.buttonAdd.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final JDialog d = new JDialog((Frame) SwingUtilities.getAncestorOfClass(Frame.class, ITextComboVilleViewer.this), true);

                final String rawTtext = text.getTextComp().getText();
                d.setContentPane(new VilleEditorPanel(rawTtext));
                d.setTitle("Nouvelle ville");
                d.pack();
                d.setResizable(false);
                d.setLocationRelativeTo(ITextComboVilleViewer.this);
                d.setVisible(true);
            }
        });

    }

    public void addValidListener(final ValidListener l) {
        this.supp.addValidListener(l);
    }

    @Override
    public void removeValidListener(ValidListener l) {
        this.supp.removeValidListener(l);
    }

    public void addValueListener(final PropertyChangeListener l) {
        this.supp.addValueListener(l);
    }

    // *** value

    public JComponent getComp() {
        return this;
    }

    public Document getDocument() {
        return this.text.getDocument();
    }

    // *** valid

    public JTextComponent getTextComp() {
        return this.text.getTextComp();
    }

    // *** text

    public Ville getValue() {
        return this.text.getValue();
    }

    @Override
    public ValidState getValidState() {
        return this.text.getValidState();
    }

    public void resetValue() {
        this.text.resetValue();
    }

    public void rmValueListener(final PropertyChangeListener l) {
        this.supp.rmValueListener(l);
    }

    public void setButtonVisible(final boolean b) {
        this.button.setVisible(b);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        this.text.setEnabled(enabled);
        this.button.setEnabled(enabled);
    }

    public void setValue(final Ville val) {
        this.text.setValue(val);
    }

}
