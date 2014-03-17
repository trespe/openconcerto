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
 
 package org.openconcerto.erp.core.sales.pos.ui;


import java.awt.GridLayout;

import javax.swing.JPanel;

public class ArticleSelectorPanel extends JPanel {
    ArticleSelectorPanel(CaisseControler controller) {
        final ArticleSelector comp = new ArticleSelector(controller);
        this.setLayout(new GridLayout(0, 2));
        this.add(new CategorieSelector(controller, comp.getModel()));

        this.add(comp);
       
    }

}
