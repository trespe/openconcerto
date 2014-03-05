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
 
 package org.openconcerto.erp.preferences;

import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.preferences.DefaultPreferencePanel;
import org.openconcerto.ui.preferences.DefaultProps;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;

public class GestionPieceCommercialePanel extends DefaultPreferencePanel {

    public static final String SHOW_MOUVEMENT_NUMBER = "ShowMouvementNumber";
    private final JCheckBox checkMvtPieceShow;

    public GestionPieceCommercialePanel() {
        super();
        setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        c.weighty = 1;

        this.checkMvtPieceShow = new JCheckBox("Afficher le numéro de mouvement lors de la création d'une pièce commerciale");

        this.add(this.checkMvtPieceShow, c);

        setValues();

    }

    @Override
    public void storeValues() {
        final DefaultProps props = DefaultNXProps.getInstance();

        props.setProperty(SHOW_MOUVEMENT_NUMBER, String.valueOf(this.checkMvtPieceShow.isSelected()));
        props.store();
    }

    @Override
    public void restoreToDefaults() {
        this.checkMvtPieceShow.setSelected(false);
    }

    @Override
    public String getTitleName() {
        return "Gestion des pièces commerciales";
    }

    private void setValues() {
        final DefaultProps props = DefaultNXProps.getInstance();

        final String showMvt = props.getStringProperty(SHOW_MOUVEMENT_NUMBER);
        final Boolean bShowMvt = Boolean.valueOf(showMvt);
        this.checkMvtPieceShow.setSelected(bShowMvt == null || bShowMvt.booleanValue());
    }

}
