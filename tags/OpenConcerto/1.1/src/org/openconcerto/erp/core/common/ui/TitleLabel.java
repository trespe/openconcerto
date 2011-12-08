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
 * Created on 26 sept. 2004
 * 
 * To change the template for this generated file go to Window&gt;Preferences&gt;Java&gt;Code
 * Generation&gt;Code and Comments
 */
package org.openconcerto.erp.core.common.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.JLabel;

/**
 * @author bb
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TitleLabel extends JLabel {

    /**
     * 
     */
    public TitleLabel() {
        super();
        init();
    }

    /**
     * @param text
     */
    public TitleLabel(String text) {
        super(text);
        init();
    }

    /**
     * @param text
     * @param horizontalAlignment
     */
    public TitleLabel(String text, int horizontalAlignment) {
        super(text, horizontalAlignment);
        init();
    }

    /**
     * @param image
     */
    public TitleLabel(Icon image) {
        super(image);
        init();
    }

    /**
     * @param image
     * @param horizontalAlignment
     */
    public TitleLabel(Icon image, int horizontalAlignment) {
        super(image, horizontalAlignment);
        init();
    }

    /**
     * @param text
     * @param icon
     * @param horizontalAlignment
     */
    public TitleLabel(String text, Icon icon, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
        init();
    }

    private void init() {
        setFont(font);

        setForeground(new Color(20, 20, 22));
        // setBackground(new Color(255, 255, 255));
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

        super.paintComponent(g);
    }

    private Font font = new Font("Arial Gras", Font.PLAIN, 15);

}
