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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.net.URL;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.JWindow;
import javax.swing.Timer;

class IWindowWithArrow extends JWindow
 implements MouseListener, ActionListener
{

 public IWindowWithArrow(IMenuWithArrows arrowmenu)
 {
     fontSize = 12;
     up = null;
     down = null;
     timer = null;
     initialDelay = 300;
     repeatDelay = 50;
     increment = 0;
     menu = arrowmenu;
     items = new Vector();
     viewport = new JViewport();
     list = new JList();
     font = new Font(null, 0, fontSize);
     list.setFont(font);
     list.setBackground(new Color(207, 207, 207));
     list.setForeground(Color.BLACK);
     list.setSelectionBackground(new Color(144, 151, 207));
     list.setSelectionForeground(Color.BLACK);
     //list.setVisibleRowCount(10);
    
     
     list.addMouseListener(this);
     viewport.setView(list);
     resource = getClass().getResource("arrow-up.gif");
     up = new JButton(new ImageIcon(resource));
     up.setBackground(new Color(207, 207, 207));
     up.setBorder(null);
     up.addMouseListener(this);
     resource = getClass().getResource("arrow-down.gif");
     down = new JButton(new ImageIcon(resource));
     down.setBackground(new Color(207, 207, 207));
     down.setBorder(null);
     down.addMouseListener(this);
     Container container = getContentPane();
     JPanel jpanel = new JPanel();
     jpanel.setBorder(BorderFactory.createBevelBorder(0));
     jpanel.setLayout(new BorderLayout());
     jpanel.add(up, "North");
     jpanel.add(viewport, "Center");
     jpanel.add(down, "South");
     container.add(jpanel);
     timer = new Timer(repeatDelay, this);
     timer.setInitialDelay(initialDelay);
 }

 public void add(JMenuItem jmenuitem)
 {
     AItem aitem = new AItem(jmenuitem);
     items.addElement(aitem);
     list.setListData(items);
 }

 public void setVisible(boolean flag)
 {
     int i = 0;
     int j = 0;
     FontRenderContext fontrendercontext = new FontRenderContext(null, false, false);
     for(int k = 0; k < items.size(); k++)
     {
         float f = (float)font.getStringBounds(((AItem)items.elementAt(k)).label, fontrendercontext).getWidth();
         if(f > (float)i)
             i = (int)f;
     }

     Point point = getLocation();
     Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
     j = dimension.height - point.y - 30;
     index = 0;
     viewport.setViewPosition(list.indexToLocation(0));
     list.setSelectedIndex(0);
     setSize(i, j);
     super.setVisible(flag);
 }

 public void mousePressed(MouseEvent mouseevent)
 {
     increment = 0;
     if(mouseevent.getSource() == up)
     {
         if(index > 0)
         {
             increment = -1;
             timer.start();
         }
     } else
     if(mouseevent.getSource() == down && list.getLastVisibleIndex() < items.size() - 1)
     {
         increment = 1;
         timer.start();
     }
     index += increment;
     viewport.setViewPosition(list.indexToLocation(index));
 }

 public void mouseReleased(MouseEvent mouseevent)
 {
     if(timer != null)
         timer.stop();
     if(mouseevent.getSource() == list)
     {
         ((AItem)items.elementAt(list.getSelectedIndex())).menuItem.doClick();
         menu.fireMenuCanceled();
     }
 }

 public void mouseClicked(MouseEvent mouseevent)
 {
 }

 public void mouseEntered(MouseEvent mouseevent)
 {
 }

 public void mouseExited(MouseEvent mouseevent)
 {
 }

 public void actionPerformed(ActionEvent actionevent)
 {
     if(increment == 1)
     {
         if(list.getLastVisibleIndex() < items.size() - 1)
             index++;
     } else
     if(increment == -1 && index > 0)
         index--;
     viewport.setViewPosition(list.indexToLocation(index));
 }
IMenuWithArrows menu;
 JList list;
 JViewport viewport;
 Vector items;
 Font font;
 int fontSize;
 JButton up;
 JButton down;
 int index;
 Timer timer;
 int initialDelay;
 int repeatDelay;
 int increment;
 Image image;
 URL resource;
 
 class AItem
 {

     public AItem(JMenuItem jmenuitem)
     {
         menuItem = jmenuitem;
         label = jmenuitem.getText();
     }

     public String toString()
     {
         return label;
     }

     JMenuItem menuItem;
     String label;
 }
}
