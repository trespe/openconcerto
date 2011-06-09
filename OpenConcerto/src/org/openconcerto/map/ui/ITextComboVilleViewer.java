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
import org.openconcerto.ui.component.ComboLockedMode;
import org.openconcerto.ui.component.ITextSelector;
import org.openconcerto.ui.component.text.DocumentComponent;
import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.ui.state.WindowStateManager;
import org.openconcerto.ui.valuewrapper.ValueChangeSupport;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.EmptyObject;
import org.openconcerto.utils.checks.EmptyObjectHelper;
import org.openconcerto.utils.checks.ValidListener;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;


import org.apache.commons.collections.Predicate;

public class ITextComboVilleViewer extends JPanel implements ValueWrapper<String>, DocumentComponent, TextComponent, EmptyObject {
    /**
     * Selecteur de Ville
     */
    private static final long serialVersionUID = 3397210337907076649L;
    private final ITextSelector text = new ITextSelector(ComboLockedMode.ITEMS_LOCKED);
    private final JButton button = new JButton("Afficher sur la carte");
    private Ville currentVille = null;
    private final EmptyObjectHelper emptyHelper;

    private final ValueChangeSupport<String> supp;
    private final ITextComboCacheVille cache;

    public ITextComboVilleViewer() {
        this.setOpaque(false);
        this.setLayout(new BorderLayout());

        this.supp = new ValueChangeSupport<String>(this);
        this.emptyHelper = new EmptyObjectHelper(this, new Predicate() {
            public boolean evaluate(final Object object) {
                // object: le getUncheckedValue()
                return ITextComboVilleViewer.this.getValue() == null || ITextComboVilleViewer.this.getValue().trim().length() == 0;
            }
        });
        this.text.addValueListener(new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent evt) {
                ITextComboVilleViewer.this.supp.fireValueChange();
            }
        });

        this.cache = new ITextComboCacheVille();
        this.text.initCache(this.cache);
        this.add(this.text, BorderLayout.CENTER);

        this.add(this.button, BorderLayout.EAST);
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
        this.text.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(final DocumentEvent e) {
                ITextComboVilleViewer.this.currentVille = Ville.getVilleFromVilleEtCode(ITextComboVilleViewer.this.text.getValue());
                ITextComboVilleViewer.this.button.setEnabled(ITextComboVilleViewer.this.currentVille != null && ITextComboVilleViewer.this.isEnabled());

            }

            public void insertUpdate(final DocumentEvent e) {
                this.changedUpdate(e);
            }

            public void removeUpdate(final DocumentEvent e) {
                this.changedUpdate(e);
            }
        });
        final JPopupMenu popupMenu = new JPopupMenu();
        // FIXME ajouter la possibilité de supprimer une ville précédemment enregistrée
        final JMenuItem menuItem = new JMenuItem("Enregistrer cette ville");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                final String t = ITextComboVilleViewer.this.text.getTextComp().getText();
                ITextComboVilleViewer.this.cache.addToCache(t);
                final Ville createVilleFrom = ITextComboVilleViewer.this.cache.createVilleFrom(t);
                if (createVilleFrom != null) {
                    final String villeEtCode = createVilleFrom.getVilleEtCode();
                    ITextComboVilleViewer.this.setValue(villeEtCode);
                    ITextComboVilleViewer.this.firePropertyChange("value", null, villeEtCode);
                }
            }
        });
        popupMenu.add(menuItem);

        this.text.getTextComp().addMouseListener(new MouseAdapter() {
            private void maybeShowPopup(final MouseEvent e) {

                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mousePressed(final MouseEvent e) {
                this.maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                this.maybeShowPopup(e);
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

    public String getValidationText() {
        return this.getValue() + " n'existe pas";
    }

    // *** text

    public String getValue() {
        return this.text.getValue();
    }

    @Override
    public boolean isEmpty() {
        return this.emptyHelper.isEmpty();
    }

    public boolean isValidated() {
        final Ville villeFromVilleEtCode = Ville.getVilleFromVilleEtCode(this.getValue());
        final boolean b = villeFromVilleEtCode != null;
        if (b) {
            this.cache.setLastGood(villeFromVilleEtCode);
        }
        return b;
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
        this.text.setEditable(enabled);
        this.button.setEnabled(enabled);
    }

    public void setValue(final String val) {
        this.text.setValue(val);
    }
}
