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
import org.openconcerto.ui.component.ITextComboCache;
import org.openconcerto.ui.component.combo.ISearchableTextCombo;
import org.openconcerto.utils.model.DefaultIListModel;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class StatusPanel extends JPanel implements VilleRendererListener, ZoomListener {

    /**
     * 
     */
    private static final long serialVersionUID = 4859531406653415107L;
    private final JLabel label = new JLabel("Status");
    private VilleRendererPanel v;
    private Ville currentVille = null;
    private JToggleButton radio1 = null;
    private JToggleButton radio2 = null;
    private final JSlider slider;

    public StatusPanel(final VilleRendererPanel villeRendererPanel, boolean viewOnly) {
        this.v = villeRendererPanel;

        this.setLayout(new GridBagLayout());
        // this.add(label);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 0, 2, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.insets = new Insets(2, 2, 2, 2);
        //
        if (!viewOnly) {
            c.gridx++;
            this.radio1 = new JToggleButton(new ImageIcon(StatusPanel.class.getResource("move.png")));
            this.radio1.setMargin(new Insets(2, 4, 2, 4));
            this.radio1.setSelected(true);
            this.add(this.radio1, c);
            this.radio2 = new JToggleButton(new ImageIcon(StatusPanel.class.getResource("draw.png")));
            c.gridx++;
            this.radio2.setMargin(new Insets(2, 4, 2, 4));
            this.add(this.radio2, c);
            ButtonGroup g = new ButtonGroup();
            g.add(this.radio2);
            g.add(this.radio1);

        }
        /* final JButton button = new JButton("Centrer"); */
        ISearchableTextCombo txt = new ISearchableTextCombo(ComboLockedMode.ITEMS_LOCKED, 1, 40);
        txt.addValueListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                final String trim = evt.getNewValue().toString().trim();
                StatusPanel.this.currentVille = Ville.getVilleFromVilleEtCode(trim);
                if (StatusPanel.this.currentVille != null) {
                    System.out.println("CurrentVille:" + StatusPanel.this.currentVille + " from:" + trim);
                    // button.setEnabled(currentVille != null);
                    StatusPanel.this.v.setAlwayVisible(StatusPanel.this.currentVille);
                    StatusPanel.this.v.setHighlight(StatusPanel.this.currentVille);
                    StatusPanel.this.v.centerScreenXYLambert(StatusPanel.this.currentVille.getXLambert(), StatusPanel.this.currentVille.getYLambert());
                }
            }

        });
        txt.setMinimumSearch(0);
        txt.setMaximumResult(200);

        txt.initCache(new DefaultIListModel<String>(new ITextComboCacheVille().getCache()));
        c.weightx = 1;
        c.gridx++;
        this.add(txt, c);

        c.weightx = 0;
        c.gridx++;
        this.slider = new JSlider(0, villeRendererPanel.getMaxZoomIndex(), 1);

        this.slider.setMajorTickSpacing(1);
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put(new Integer(0), new JLabel("Zoom Minimum"));
        labelTable.put(new Integer(villeRendererPanel.getMaxZoomIndex()), new JLabel("Maximum"));
        this.slider.setLabelTable(labelTable);
        this.slider.setPaintLabels(true);
        // set min size since by default it is about 5*5
        // at the end so that labels are used
        final Dimension preferredSize = this.slider.getPreferredSize();
        this.slider.setMinimumSize(new Dimension((int) (preferredSize.width * 0.8), preferredSize.height));

        this.slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                StatusPanel.this.v.setZoomIndex(StatusPanel.this.slider.getValue());
            }
        });
        this.v.addZoomListener(this);

        this.add(this.slider, c);
        if (!viewOnly) { // Radio Listeners
            this.radio1.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    StatusPanel.this.v.setMode(VilleRendererPanel.MODE_MOVE);

                }
            });
            this.radio2.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    StatusPanel.this.v.setMode(VilleRendererPanel.MODE_DRAW);
                }
            });

            villeRendererPanel.addModeListener(new ModeListener() {

                public void modeChanged() {
                    if (StatusPanel.this.v.getMode() == VilleRendererPanel.MODE_MOVE)
                        StatusPanel.this.radio1.setSelected(true);
                    else
                        StatusPanel.this.radio2.setSelected(true);
                }
            });

        }

    }

    public void selectionChanged(VilleRendererPanel src) {
        this.label.setText(src.getStatus());

    }

    @Override
    public void zoomChanged(int newZoom) {
        if (newZoom != this.slider.getValue()) {
            this.slider.setValue(newZoom);
        }

    }
}
