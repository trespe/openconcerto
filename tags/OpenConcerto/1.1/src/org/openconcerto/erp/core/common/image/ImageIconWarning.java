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
 
 package org.openconcerto.erp.core.common.image;

import org.openconcerto.sql.sqlobject.ElementComboBox;

import javax.swing.ImageIcon;

/**
 * ImageIcon avec un symbole de warning sous forme de triangle jaune
 * 
 */
public final class ImageIconWarning extends ImageIcon {

    private static final ImageIconWarning instance = new ImageIconWarning();

    private ImageIconWarning() {
        super(ElementComboBox.class.getResource("warning.png"));
    }

    public synchronized final static ImageIconWarning getInstance() {
        return instance;
    }

}
