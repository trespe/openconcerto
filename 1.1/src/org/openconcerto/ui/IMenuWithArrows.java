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
 * Créé le 3 avr. 2005
 *
 */
package org.openconcerto.ui;

import java.awt.*;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

public class IMenuWithArrows extends JMenu
    implements MenuListener
{
    private IWindowWithArrow arrowWindow;
    public IMenuWithArrows(String s)
    {
        super(s);
        addMenuListener(this);
        arrowWindow = new IWindowWithArrow(this);
    }

    public JMenuItem add(JMenuItem jmenuitem)
    {
        arrowWindow.add(jmenuitem);
        return jmenuitem;
    }

    public void menuSelected(MenuEvent menuevent)
    {
        Point point = getLocationOnScreen();
        arrowWindow.setLocation(point.x, point.y + getSize().height);
        arrowWindow.setVisible(true);
        arrowWindow.requestFocus();
        arrowWindow.repaint();
    }

    public void menuCanceled(MenuEvent menuevent)
    {
        arrowWindow.setVisible(false);
    }

    public void menuDeselected(MenuEvent menuevent)
    {
        arrowWindow.setVisible(false);
    }

    protected void fireMenuCanceled()
    {
        super.fireMenuCanceled();
    }

   
}
