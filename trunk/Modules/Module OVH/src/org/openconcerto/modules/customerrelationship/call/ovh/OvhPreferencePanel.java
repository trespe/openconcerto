package org.openconcerto.modules.customerrelationship.call.ovh;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.openconcerto.erp.preferences.DefaultLocalPreferencePanel;

import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.JImage;

public class OvhPreferencePanel extends DefaultLocalPreferencePanel {
    public static final String OVH_PROPERTIES = "ovh.properties";
    final JTextField textAccountLogin = new JTextField();
    final JPasswordField textAccountPassword = new JPasswordField();
    final JTextField textNumber = new JTextField();
    final JTextField textLogin = new JTextField();
    final JPasswordField textPassword = new JPasswordField();

    public OvhPreferencePanel() {
        super("Téléphonie OVH", OVH_PROPERTIES);

        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        // Logo OVH.com
        c.gridwidth = 3;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        final JImage img = new JImage(OvhPreferencePanel.class.getResource("ovh.png"));
        img.setHyperLink("http://www.ovh.com/fr/telephonie/");
        this.add(img, c);

        // Account
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        this.add(new JLabelBold("Compte OVH"), c);
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        this.add(new JLabel("Identifiant OVH", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        this.add(textAccountLogin, c);
        c.gridx++;
        c.weightx = 0;
        this.add(new JLabel("(NIC-handle, Domaine, Email)"), c);

        c.gridx = 0;
        c.gridy++;
        this.add(new JLabel("Mot de passe", SwingConstants.RIGHT), c);
        c.gridx++;
        this.add(textAccountPassword, c);

        // Telephony
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        this.add(new JLabelBold("Téléphonie SIP"), c);
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        this.add(new JLabel("N° de ligne", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        this.add(textNumber, c);
        c.gridx++;
        c.weightx = 0;
        this.add(new JLabel("(ex: 0311442288)"), c);

        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        this.add(new JLabel("Identifiant Click2Call", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        this.add(textLogin, c);
        c.gridx++;
        c.weightx = 0;
        this.add(new JLabel("(identifiant pour appel en 1 clic)"), c);

        c.gridx = 0;
        c.gridy++;
        this.add(new JLabel("Mot de passe", SwingConstants.RIGHT), c);
        c.gridx++;
        this.add(textPassword, c);

        final JButton bTest = new JButton("Appliquer et tester les paramètres");
        bTest.setOpaque(false);
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHEAST;
        this.add(bTest, c);
        bTest.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                storeValues();
                try {
                    OVHApi.testOVHAccount();
                    JOptionPane.showMessageDialog(OvhPreferencePanel.this, "Connexion réussie au service OVH");
                } catch (Exception e1) {
                    JOptionPane.showMessageDialog(OvhPreferencePanel.this, e1.getMessage());
                }
            }
        });
        textAccountLogin.setText(properties.getProperty("account", ""));
        textAccountPassword.setText(properties.getProperty("accountpassword", ""));
        textNumber.setText(properties.getProperty("from", ""));
        textLogin.setText(properties.getProperty("login", ""));
        textPassword.setText(properties.getProperty("password", ""));
    }

    @Override
    public void storeValues() {
        properties.setProperty("account", textAccountLogin.getText());
        properties.setProperty("accountpassword", String.valueOf(textAccountPassword.getPassword()));
        properties.setProperty("from", textNumber.getText());
        properties.setProperty("login", textLogin.getText());
        properties.setProperty("password", String.valueOf(textPassword.getPassword()));
        super.storeValues();
    }

    @Override
    public void restoreToDefaults() {
        textAccountLogin.setText("");
        textAccountPassword.setText("");
        textNumber.setText("");
        textLogin.setText("");
        textPassword.setText("");
    }

    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.setContentPane(new OvhPreferencePanel());
        f.pack();
        f.setVisible(true);
    }
}
