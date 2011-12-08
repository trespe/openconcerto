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
 
 package org.openconcerto.utils.html;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class HTMLManipulatorFrame extends JFrame {
    JTextField textDirPath;

    JTextField textDivname;
    JTextArea textContent;
    JButton buttonFind;
    JLabel labelStatus;
    JButton buttonReplace;

    HTMLManipulatorFrame() {
        super("Div replace content");
        JPanel p = new JPanel();
        this.setContentPane(p);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);
        p.add(new JLabel("Directory path:"), c);
        c.gridx++;
        c.weightx = 1;
        textDirPath = new JTextField("T:\\SitesWeb\\Dyseurope\\DysEuropeV7\\en");
        p.add(textDirPath, c);

        // Ligne 2
        c.gridy++;
        c.weightx = 0;
        c.gridx = 0;
        p.add(new JLabel("Div id:"), c);
        c.gridx++;
        c.weightx = 1;
        textDivname = new JTextField("siteInfo");
        p.add(textDivname, c);

        // Ligne 3
        c.gridy++;
        c.weightx = 0;
        c.gridx = 0;
        p.add(new JLabel("Replace content by:"), c);
        c.gridx++;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        this.textContent = new JTextArea("<p>Hello</p>");
        this.textContent.setPreferredSize(new Dimension(300, 200));
        this.textContent.setMinimumSize(new Dimension(300, 200));
        this.textContent.setFont(textDivname.getFont());
        this.textContent.setLineWrap(true);
        p.add(textContent, c);

        // Ligne 4
        c.gridy++;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridwidth = 2;
        this.labelStatus = new JLabel();
        p.add(this.labelStatus, c);

        // Ligne 5
        c.gridy++;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;

        this.buttonFind = new JButton("Find");
        this.buttonFind.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                apply(false);
            }
        });

        p.add(buttonFind, c);

        c.gridx++;
        this.buttonReplace = new JButton("Replace");
        this.buttonReplace.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                apply(true);
            }
        });
        p.add(buttonReplace, c);

    }

    private void apply(boolean b) {
        disableActions();
        updateStatus("Please wait...");
        File f = new File(textDirPath.getText());
        if (!f.exists()) {
            updateStatus("Directory not found");
            enableActions();
            return;
        }
        if (!f.isDirectory()) {
            updateStatus(textDirPath.getText() + " Not a directory");
            enableActions();
            return;
        }
        File[] files = f.listFiles();
        int count = 0;
        int found = 0;
        String divText = textDivname.getText();
        String text = textContent.getText();
        for (int i = 0; i < files.length; i++) {
            File htmlFile = files[i];
            if (htmlFile.getName().toLowerCase().endsWith(".html")) {
                count++;

                HTMLFile hF = new HTMLFile(htmlFile);
                HTMLDiv div = hF.getDivId(divText);
                if (div != null) {
                    found++;
                    if (b) {
                        hF.replaceContent(div, text);
                        hF.saveAs(htmlFile);
                    }
                }
            }
        }
        updateStatus("Found " + count + " html files (" + found + " containing div id: " + divText);

        enableActions();
    }

    protected void enableActions() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                buttonFind.setEnabled(true);
                buttonReplace.setEnabled(true);
            }
        });

    }

    protected void disableActions() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                buttonFind.setEnabled(false);
                buttonReplace.setEnabled(false);
            }
        });

    }

    void updateStatus(final String t) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                labelStatus.setText(t);
            }
        });
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (UnsupportedLookAndFeelException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                HTMLManipulatorFrame f = new HTMLManipulatorFrame();
                f.pack();
                f.setVisible(true);

            }
        });

    }

}
