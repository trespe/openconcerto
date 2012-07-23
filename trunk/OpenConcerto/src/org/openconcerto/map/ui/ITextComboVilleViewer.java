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
import org.openconcerto.ui.component.IComboCacheListModel;
import org.openconcerto.ui.component.combo.ISearchableTextCombo;
import org.openconcerto.ui.component.text.DocumentComponent;
import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.ui.state.WindowStateManager;
import org.openconcerto.ui.valuewrapper.ValueChangeSupport;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.EmptyObject;
import org.openconcerto.utils.checks.EmptyObjectHelper;
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

import org.apache.commons.collections.Predicate;

public class ITextComboVilleViewer extends JPanel implements ValueWrapper<String>, DocumentComponent, TextComponent, EmptyObject {
    /**
     * Selecteur de Ville
     */
    private static final long serialVersionUID = 3397210337907076649L;
    private final ISearchableTextCombo text;
    private final JButton button = new JButton("Afficher sur la carte");
    private final JButton buttonAdd;
    private Ville currentVille = null;
    private final EmptyObjectHelper emptyHelper;

    private final ValueChangeSupport<String> supp;
    private final ITextComboCacheVille cache;

    public ITextComboVilleViewer() {
        this.setOpaque(false);
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(0, 0, 0, 2);

        this.buttonAdd = new JButton(new ImageIcon(this.getClass().getResource("add.png")));
        this.supp = new ValueChangeSupport<String>(this);
        this.cache = new ITextComboCacheVille();

        this.text = new ISearchableTextCombo(ComboLockedMode.LOCKED_ITEMS_UNLOCKED, 0, 17) {
            @Override
            protected String stringToT(String t) {
                // MAYBE ISearchableCombo<Ville>
                final Ville v = ITextComboVilleViewer.this.cache.createVilleFrom(t);
                if (v != null) {
                    return t;
                } else {
                    throw new IllegalArgumentException("Format incorrect, la ville doit être du format VILLE (CODEPOSTAL)\n Ex:  Abbeville (80100)");
                }
            };
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
        this.emptyHelper = new EmptyObjectHelper(this, new Predicate() {
            public boolean evaluate(final Object object) {
                // object: le getUncheckedValue()
                return ITextComboVilleViewer.this.getValue() == null || ITextComboVilleViewer.this.getValue().trim().length() == 0;
            }
        });

        final IComboCacheListModel comboCacheListModel = new IComboCacheListModel(this.cache);
        this.text.initCache(comboCacheListModel.load());
        // Listen on data
        final PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                comboCacheListModel.reload();
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

    @Override
    public void addEmptyListener(final EmptyListener l) {
        this.emptyHelper.addListener(l);
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

    @Override
    public Object getUncheckedValue() {
        return this.getValue();
    }

    // *** text

    public String getValue() {
        return this.text.getValue();
    }

    @Override
    public boolean isEmpty() {
        return this.emptyHelper.isEmpty();
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

    public void setValue(final String val) {
        this.text.setValue(val);
    }

}
