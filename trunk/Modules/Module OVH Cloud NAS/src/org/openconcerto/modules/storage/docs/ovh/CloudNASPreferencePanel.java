package org.openconcerto.modules.storage.docs.ovh;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openconcerto.erp.preferences.DefaultLocalPreferencePanel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.StringInputStream;

public class CloudNASPreferencePanel extends DefaultLocalPreferencePanel {
    public static final String SHOW_MENU = "showMenu";
    public static final String AUTO = "auto";
    public static final String ACCOUNT_PASSWORD = "accountPassword";
    public static final String ACCOUNT_LOGIN = "accountLogin";
    public static final String ACCOUNT = "account";
    public static final String OVH_CLOUD_NAS_PROPERTIES = "ovhcloudnas.properties";
    final JTextField textAccount = new JTextField();
    final JTextField textAccountLogin = new JTextField();
    final JPasswordField textAccountPassword = new JPasswordField();
    final JCheckBox checkAuto = new JCheckBox("Sauvegarder automatiquement les documents générés");
    final JCheckBox checkShowMenu = new JCheckBox("Activer le menu contextuel (clic droit sur les listes)");
    final JLabel url = new JLabel();

    public CloudNASPreferencePanel() {
        super("OVH Cloud NAS", OVH_CLOUD_NAS_PROPERTIES);

        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();

        // Account
        c.gridx = 0;
        c.gridy = 0;

        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridwidth = 1;
        c.weightx = 0;

        this.add(new JLabel("Compte", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        this.add(textAccount, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Identifiant", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        this.add(textAccountLogin, c);
        c.gridy++;

        this.add(url, c);
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Mot de passe", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        this.add(textAccountPassword, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;

        this.add(checkAuto, c);
        c.gridx = 0;

        c.gridy++;

        this.add(checkShowMenu, c);
        c.gridx = 0;
        c.gridy++;

        final JButton bTest = new JButton("Tester les paramètres");
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

                final Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        CloudNASStorageEngine ll = null;
                        try {
                            ll = new CloudNASStorageEngine(textAccount.getText());
                            ll.login(textAccountLogin.getText(), new String(textAccountPassword.getPassword()));
                        } catch (Throwable e1) {
                            ll = null;
                            JOptionPane.showMessageDialog(CloudNASPreferencePanel.this, "Echec de la connexion (" + e1.getMessage() + ").\nVérifiez les paramètres");
                        }
                        if (ll != null) {
                            try {
                                ll.store(new StringInputStream("OpenConcerto"), "Test", "Test OVH Cloud NAS.txt", true);
                                JOptionPane.showMessageDialog(CloudNASPreferencePanel.this, "Test réussi, compte operationnel.");
                                storeValues();
                            } catch (Throwable e1) {
                                JOptionPane.showMessageDialog(CloudNASPreferencePanel.this, "Erreur de transfert (" + e1.getMessage() + ").\nVérifiez les paramètres");
                            }
                        }
                    }
                });
                t.setDaemon(true);
                t.setName("Test OVH Cloud NAS");
                t.start();
            }
        });
        textAccount.setText(properties.getProperty(ACCOUNT, "votreCompte"));
        textAccountLogin.setText(properties.getProperty(ACCOUNT_LOGIN, "votreIdentifiant"));
        textAccountPassword.setText(properties.getProperty(ACCOUNT_PASSWORD, ""));
        checkAuto.setSelected(properties.getProperty(AUTO, "true").equals("true"));
        checkShowMenu.setSelected(properties.getProperty(SHOW_MENU, "false").equals("true"));
        final DocumentListener listener = new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateURL();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateURL();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateURL();
            }
        };
        this.textAccount.getDocument().addDocumentListener(listener);
        this.textAccountLogin.getDocument().addDocumentListener(listener);
        updateURL();
    }

    private void updateURL() {
        this.url.setText("https://cloud.ovh.fr/" + this.textAccount.getText() + "/" + this.textAccountLogin.getText() + "/");
    }

    @Override
    public void storeValues() {
        properties.setProperty(ACCOUNT, textAccount.getText());
        properties.setProperty(ACCOUNT_LOGIN, textAccountLogin.getText());
        properties.setProperty(ACCOUNT_PASSWORD, String.valueOf(textAccountPassword.getPassword()));
        properties.setProperty(AUTO, String.valueOf(this.checkAuto.isSelected()));
        properties.setProperty(SHOW_MENU, String.valueOf(this.checkShowMenu.isSelected()));
        super.storeValues();
    }

    @Override
    public void restoreToDefaults() {
        textAccount.setText("votreCompte");
        textAccountLogin.setText("votreIdentifiant");
        textAccountPassword.setText("");
        checkAuto.setSelected(true);
        checkShowMenu.setSelected(false);
    }

    public static Properties getProperties() throws IOException {
        return DefaultLocalPreferencePanel.getPropertiesFromFile(OVH_CLOUD_NAS_PROPERTIES);
    }

}
