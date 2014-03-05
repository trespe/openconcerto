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
 
 package org.openconcerto.ui.table;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class CloseTableHeaderRenderer extends JPanel implements TableCellRenderer, MouseListener {
    private ImageIcon icon1 = new ImageIcon(CloseTableHeaderRenderer.class.getResource("remove.png"));
    private ImageIcon icon2 = new ImageIcon(CloseTableHeaderRenderer.class.getResource("remove2.png"));
    private final JButton b = new JButton(icon1);
    private JTableHeader header;
    private JTable editedTable;
    private int editedColumn;
    private JLabel label = new JLabel();
    private List popupActions;   

    public CloseTableHeaderRenderer(final Action buttonAction, List popupActions) {

        this.popupActions=popupActions;
        this.setBorder(BorderFactory.createEtchedBorder());
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 2, 0, 1);
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        this.add(label, c);

        c.weightx = 0;
        c.gridx++;
        b.setBorder(null);
        b.setFocusable(false);
        b.setOpaque(false);
        b.setMargin(new Insets(1, 1, 1, 1));

        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                buttonAction.actionPerformed(e);
               
            }
        });
        this.add(b, c);

    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        
        
        this.header = table.getTableHeader();
        header.setReorderingAllowed(false); 
        this.editedTable = table;
        this.editedColumn = column;
        this.label.setText(value.toString());
        this.header.removeMouseListener(this);
        this.header.addMouseListener(this);
        return this;
    }
    public void destroy(){
        
        this.header.removeMouseListener(this);        
        this.editedTable=null;
        this.header=null;        
    }
    
    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mousePressed(MouseEvent e) {
        
        if (header == null)
            return;
        
        int col = header.columnAtPoint(e.getPoint());
        
        if(col<0) 
            return;
       // System.out.println(col);
       // System.out.println(this.getBounds());
        TableColumn tcol = this.editedTable.getColumnModel().getColumn(col);
        System.out.println(tcol.getHeaderValue());
        int x = 0;
        for (int i = 0; i < editedColumn; i++) {
            TableColumn acol = this.editedTable.getColumnModel().getColumn(i);
            x += acol.getWidth();
        }
       // System.out.println("x:" + x);
      //  System.out.println("b:" + b.getBounds());
        System.out.println("p:" + (e.getX() - x) + "," + e.getY());
        System.out.println("p:"+ getBounds());
        boolean test = b.getBounds().contains(e.getX() - x, e.getY());
       System.out.println(test);

        if (test) {
            b.doClick();
        } else {
            if (e.getButton() == MouseEvent.BUTTON3 && new Rectangle(-getBounds().x, -getBounds().y).contains(e.getX() - x, e.getY())) {
              
                JPopupMenu pop=new JPopupMenu();
               
                for (int i = 0; i < popupActions.size(); i++) {
                    
                    pop.add((Action) popupActions.get(i));
                }
                pop.show(this.header,e.getX(),e.getY());
            }                        
        }

    }

    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub

    }

}
