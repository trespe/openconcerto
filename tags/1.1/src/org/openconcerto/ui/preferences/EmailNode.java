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
 
 package org.openconcerto.ui.preferences;

import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.component.ITextArea;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class EmailNode extends DefaultPreferencePanel {

    /**
	 * 
	 */
    private static final long serialVersionUID = -2626095170221913765L;

    private JFileChooser fileChooser;

    private JRadioButton radioDefault;
    private JTextField textThunderbirdPath;
    private JRadioButton radioThunderbird;

    private JRadioButton radioOutlook;

    private JTextField textTitle;

    private JTextArea textHeader;

    private JTextArea textFooter;

    /**
     * Preference des emails
     */
    // Different pour chaque poste!!!
    public EmailNode() {
        super();

        this.setLayout(new GridBagLayout());

        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        JLabel l = new JLabel("Envoyer les emails avec :");
        c.gridwidth = 3;
        this.add(l, c);

        this.radioDefault = new JRadioButton("le logiciel email par défaut");

        c.gridy++;
        this.add(this.radioDefault, c);

        this.radioThunderbird = new JRadioButton("Mozilla Thunderbird");
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        this.add(this.radioThunderbird, c);

        this.textThunderbirdPath = new JTextField();
        c.weightx = 1;
        c.gridx++;
        this.add(this.textThunderbirdPath, c);

        JButton buttonModele = new JButton("Selectionner");
        c.gridx++;
        c.weightx = 0;
        buttonModele.setEnabled(false);
        this.add(buttonModele, c);

        this.radioOutlook = new JRadioButton("Microsoft Outlook");
        c.gridx = 0;
        c.gridy++;
        this.add(this.radioOutlook, c);

        ButtonGroup grp = new ButtonGroup();
        grp.add(this.radioDefault);
        grp.add(this.radioThunderbird);
        grp.add(this.radioOutlook);
        this.radioDefault.setSelected(true);

        JLabel le = new JLabel("Préremplissage des champs :");
        c.gridy++;
        c.gridwidth = 3;
        this.add(le, c);

        JLabel labelTitle = new JLabel("Titre", SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        this.add(labelTitle, c);

        this.textTitle = new JTextField();
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 2;
        this.add(this.textTitle, c);

        JLabel labelHeader = new JLabel("Entête", SwingConstants.RIGHT);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        this.add(labelHeader, c);

        this.textHeader = new ITextArea(3, 3);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 2;
        this.add(this.textHeader, c);

        JLabel labelFooter = new JLabel("Signature", SwingConstants.RIGHT);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        this.add(labelFooter, c);

        this.textFooter = new ITextArea(3, 3);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 2;
        c.weighty = 1;

        this.add(this.textFooter, c);

        buttonModele.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                directoryChoose(EmailNode.this.textThunderbirdPath);
            }
        });

        this.textThunderbirdPath.setEditable(false);

        this.radioDefault.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                EmailNode.this.textThunderbirdPath.setEnabled(false);
            }
        });
        this.radioThunderbird.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                EmailNode.this.textThunderbirdPath.setEnabled(true);
                EmailNode.this.textThunderbirdPath.setEditable(true);
            }
        });
        this.radioOutlook.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                EmailNode.this.textThunderbirdPath.setEnabled(false);
            }
        });
        setValues();
    }

    public void setValues() {

        try {
            this.textTitle.setText(EmailProps.getInstance().getTitle());
            this.textHeader.setText(EmailProps.getInstance().getHeader());
            this.textFooter.setText(EmailProps.getInstance().getFooter());
            this.textThunderbirdPath.setText(EmailProps.getInstance().getThunderbirdPath());
            int mode = EmailProps.getInstance().getMode();
            if (mode == EmailProps.THUNDERBIRD) {
                this.radioThunderbird.setSelected(true);
            } else if (mode == EmailProps.OUTLOOK) {
                this.radioOutlook.setSelected(true);
            } else {
                this.radioDefault.setSelected(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void storeValues() {

        try {
            EmailProps.getInstance().setTitle(this.textTitle.getText());
            EmailProps.getInstance().setHeader(this.textHeader.getText());
            EmailProps.getInstance().setFooter(this.textFooter.getText());
            EmailProps.getInstance().setThunderbirdPath(this.textThunderbirdPath.getText());
            int mode = EmailProps.DEFAULT;
            if (this.radioThunderbird.isSelected()) {
                mode = EmailProps.THUNDERBIRD;
            } else if (this.radioOutlook.isSelected()) {
                mode = EmailProps.OUTLOOK;
            }
            EmailProps.getInstance().setMode(mode);

        } catch (Exception e) {
            e.printStackTrace();
        }

        EmailProps.getInstance().store();
    }

    @Override
    public void restoreToDefaults() {
        this.textThunderbirdPath.setText("C:\\Program Files\\Mozilla Thunderbird\\thunderbird.exe");
        this.radioDefault.setSelected(true);
        this.textHeader.setText("Bonjour,\n");
        this.textFooter.setText("\nCordialement,\nLa direction");
    }

    @Override
    public String getTitleName() {
        return "Emails";
    }

    public void directoryChoose(final JTextField field) {

        if (this.fileChooser == null) {
            this.fileChooser = new JFileChooser();
            this.fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                if (EmailNode.this.fileChooser.showDialog(EmailNode.this, "Sélectionner") == JFileChooser.APPROVE_OPTION) {

                    field.setText(EmailNode.this.fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
    }
}
