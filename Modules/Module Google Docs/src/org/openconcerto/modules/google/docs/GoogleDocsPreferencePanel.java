package org.openconcerto.modules.google.docs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.openconcerto.erp.preferences.DefaultLocalPreferencePanel;
import org.openconcerto.ui.DefaultGridBagConstraints;

import com.google.gdata.util.AuthenticationException;

public class GoogleDocsPreferencePanel extends DefaultLocalPreferencePanel {
    public static final String SHOW_MENU = "showMenu";
    public static final String AUTO = "auto";
    public static final String ACCOUNT_PASSWORD = "accountPassword";
    public static final String ACCOUNT_LOGIN = "accountLogin";
    public static final String GOOGLE_DOCS_PROPERTIES = "googledocs.properties";
    final JTextField textAccountLogin = new JTextField();
    final JPasswordField textAccountPassword = new JPasswordField();
    final JCheckBox checkAuto = new JCheckBox("Sauvegarder automatiquement les documents générés");
    final JCheckBox checkShowMenu = new JCheckBox("Activer le menu contextuel (clic droit sur les listes)");

    public GoogleDocsPreferencePanel() {
        super("Google Docs", GOOGLE_DOCS_PROPERTIES);

        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();

        // Account
        c.gridx = 0;
        c.gridy++;

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.weightx = 0;
        this.add(new JLabel("Identifiant Google", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        this.add(textAccountLogin, c);

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
                        try {
                            final GoogleDocsUtils ll = new GoogleDocsUtils("OpenConcerto");
                            ll.login(textAccountLogin.getText(), new String(textAccountPassword.getPassword()));
                            final File f = File.createTempFile("test_ovh", "openconcerto.txt");
                            final FileOutputStream fOut = new FileOutputStream(f);
                            fOut.write("OpenConcerto".getBytes());
                            fOut.flush();
                            fOut.close();
                            ll.uploadFile(f, "OpenConcerto/Devis/2010", "Test Google Docs", true);
                            JOptionPane.showMessageDialog(GoogleDocsPreferencePanel.this, "Connexion réussie");
                        } catch (AuthenticationException e) {
                            JOptionPane.showMessageDialog(GoogleDocsPreferencePanel.this, "Identifiant ou mot de passe invalide");
                        } catch (Throwable e1) {
                            JOptionPane.showMessageDialog(GoogleDocsPreferencePanel.this, e1.getMessage());
                        }
                    }
                });
                t.setDaemon(true);
                t.setName("Test Google Docs");
                t.start();
            }
        });
        textAccountLogin.setText(properties.getProperty(ACCOUNT_LOGIN, ""));
        textAccountPassword.setText(properties.getProperty(ACCOUNT_PASSWORD, ""));
        checkAuto.setSelected(properties.getProperty(AUTO, "true").equals("true"));
        checkShowMenu.setSelected(properties.getProperty(SHOW_MENU, "false").equals("true"));

    }

    @Override
    public void storeValues() {
        properties.setProperty(ACCOUNT_LOGIN, textAccountLogin.getText());
        properties.setProperty(ACCOUNT_PASSWORD, String.valueOf(textAccountPassword.getPassword()));
        properties.setProperty(AUTO, String.valueOf(this.checkAuto.isSelected()));
        properties.setProperty(SHOW_MENU, String.valueOf(this.checkShowMenu.isSelected()));
        super.storeValues();
    }

    @Override
    public void restoreToDefaults() {
        textAccountLogin.setText("");
        textAccountPassword.setText("");
        checkAuto.setSelected(true);
        checkShowMenu.setSelected(false);
    }

    public static Properties getProperties() throws IOException {
        return DefaultLocalPreferencePanel.getPropertiesFromFile(GOOGLE_DOCS_PROPERTIES);
    }

}
