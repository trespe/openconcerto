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
 
 /*
 * Created on 17 janv. 2005
 * 
 * TODO To change the template for this generated file go to Window - Preferences - Java - Code
 * Style - Code Templates
 */
package org.openconcerto.sql.view;

import java.awt.Dimension;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

/**
 * @author ilm
 * 
 *         TODO To change the template for this generated type comment go to Window - Preferences -
 *         Java - Code Style - Code Templates
 */
public class IListButton extends JButton {
    private static ImageIcon icon = null;

    /**
     *  
     */
    public IListButton() {
        super();
        init();
    }

    /**
     * @param text
     */
    public IListButton(String text) {
        super(text);
        init();
    }

    /**
     * @param text
     * @param icon
     */
    public IListButton(String text, Icon icon) {
        super(text, icon);
        init();
    }

    /**
     * @param a
     */
    public IListButton(Action a) {
        super(a);
        init();
    }

    /**
     * @param icon
     */
    public IListButton(Icon icon) {
        super(icon);
        init();
    }

    private final void init() {
        if (icon == null) {
            icon = new ImageIcon(getClass().getResource("liste.png"));
        }
        setIcon(icon);
        setPreferredSize(new Dimension(24, 16));
        initButton(this);
    }

    public static final void initButton(JButton b) {
        b.setBorder(BorderFactory.createEmptyBorder());
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
    }
}
