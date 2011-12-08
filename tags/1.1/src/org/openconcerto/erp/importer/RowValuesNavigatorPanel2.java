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
 
 package org.openconcerto.erp.importer;

import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class RowValuesNavigatorPanel2 extends JPanel implements ActionListener {

    private List<SQLRowValues> list;
    private int currentIndex = -1;
    private GridBagConstraints pConstraint;
    JPanel currentPanel;
    JButton bFirst = new JButton("<<");
    JButton bPrevious = new JButton("<");
    JButton bNext = new JButton(">");
    JButton bLast = new JButton(">>");
    private JTextField indexText;

    public RowValuesNavigatorPanel2(List<SQLRowValues> list) {
        this.setOpaque(false);
        this.list = list;
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.NONE;

        final JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new FlowLayout());

        buttons.add(bFirst);

        buttons.add(bPrevious);

        buttons.add(bNext);

        buttons.add(bLast);
        indexText = new JTextField(6);
        indexText.setText("1");
        buttons.add(indexText);
        buttons.add(new JLabel(" / " + list.size()));

        this.add(buttons, c);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0;
        c.gridy++;

        pConstraint = (GridBagConstraints) c.clone();
        currentPanel = new JPanel();
        this.add(currentPanel, c);
        setCurrentIndex(0);

        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        c.gridy++;
        c.weighty = 1;
        this.add(spacer, c);

        bFirst.setOpaque(false);
        bPrevious.setOpaque(false);
        bNext.setOpaque(false);
        bLast.setOpaque(false);

        bFirst.addActionListener(this);
        bPrevious.addActionListener(this);
        bNext.addActionListener(this);
        bLast.addActionListener(this);
    }

    private void setCurrentIndex(int i) {
        if (this.currentIndex == i) {
            return;
        }
        this.indexText.setText(String.valueOf(i + 1));
        this.currentIndex = i;
        this.remove(currentPanel);
        RowValuesPanel p = new RowValuesPanel(this.list.get(i));
        p.setOpaque(false);
        this.invalidate();
        this.currentPanel = p;
        this.add(currentPanel, pConstraint);
        this.revalidate();
        this.repaint();
        this.bFirst.setEnabled(currentIndex > 0);
        this.bPrevious.setEnabled(currentIndex > 0);
        this.bNext.setEnabled(currentIndex < list.size() - 1);
        this.bLast.setEnabled(currentIndex < list.size() - 1);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(bFirst)) {
            setCurrentIndex(0);
        }
        if (e.getSource().equals(bPrevious)) {
            setCurrentIndex(currentIndex - 1);
        }
        if (e.getSource().equals(bNext)) {
            setCurrentIndex(currentIndex + 1);
        }
        if (e.getSource().equals(bLast)) {
            setCurrentIndex(this.list.size() - 1);
        }
    }

}
