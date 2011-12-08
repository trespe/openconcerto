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
 
 package org.openconcerto.sql.ui;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.preferences.UserProps;
import org.openconcerto.sql.sqlobject.IComboModel;
import org.openconcerto.sql.sqlobject.IComboSelectionItem;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.ReloadPanel;
import org.openconcerto.ui.valuewrapper.EmptyValueWrapper;
import org.openconcerto.ui.valuewrapper.ValueWrapperFactory;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.JImage;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.EmptyObj;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;

/**
 * Panel de connexion de l'utilisateur au démarrage avec choix de la société
 */
public class ConnexionPanel extends JPanel implements ActionListener {

    public static final String QUICK_LOGIN = "org.openconcerto.quickLogin";

    static private boolean quickLogin(final Runnable r, final boolean societeSelector) {
        final String lastLogin = UserProps.getInstance().getLastLoginName();
        final String pass = UserProps.getInstance().getStoredPassword();
        if (Boolean.getBoolean(QUICK_LOGIN) && lastLogin.length() > 0 && pass != null && (!societeSelector || UserProps.getInstance().getLastSocieteID() >= SQLRow.MIN_VALID_ID)) {
            final Tuple2<String, String> res = new Login(Configuration.getInstance().getRoot()).connectEnc(lastLogin, pass);
            if (res.get0() == null) {
                r.run();
                // no need to display a panel
                return true;
            }
        }
        return false;
    }

    /**
     * Create a panel to log with a user/pass if quick login fails. For quick login to succeed the
     * system property {@value #QUICK_LOGIN} must be <code>true</code> and the user must have stored
     * his credential.
     * 
     * @param r what to do once authenticated, *not* run in the EDT.
     * @param imageLogo the picture put above the text fields.
     * @return a panel to log in, <code>null</code> if quick logged.
     */
    static public ConnexionPanel create(final Runnable r, final JImage imageLogo) {
        return create(r, imageLogo, false);

    }

    static public ConnexionPanel create(final Runnable r, final JImage imageLogo, final boolean societeSelector) {
        if (quickLogin(r, societeSelector)) {
            return null;
        } else {
            return new ConnexionPanel(r, imageLogo, societeSelector);
        }
    }

    private final Login login;
    private final JButton buttonConnect = new JButton("Connexion");
    private final JPasswordField textPassWord;
    private final EmptyValueWrapper<String> textLogin;
    private SQLRequestComboBox comboSociete;
    private String encryptedPassword;
    protected String clearPassword;
    private final JCheckBox saveCheckBox = new JCheckBox("Mémoriser le mot de passe");
    private final Runnable r;
    private final boolean societeSelector;
    private final ReloadPanel reloadPanel;
    private boolean isConnecting = false;
    private String connectionAllowed;
    private static final String LOGIN_ADMIN = "Administrateur";

    /**
     * Create a panel to log with a user/pass.
     * 
     * @param r what to do once authenticated, *not* run in the EDT.
     * @param imageLogo the picture put above the text fields.
     */
    public ConnexionPanel(final Runnable r, final JImage imageLogo) {
        this(r, imageLogo, false);
    }

    public ConnexionPanel(final Runnable r, final JImage imageLogo, final boolean societeSelector) {
        this(r, imageLogo, societeSelector, true);
    }

    public ConnexionPanel(final Runnable r, final JImage imageLogo, final boolean societeSelector, final boolean allowStoredPass) {
        super();
        this.login = new Login(Configuration.getInstance().getRoot());

        this.societeSelector = societeSelector;
        this.r = r;
        String lastLoginName = UserProps.getInstance().getLastLoginName();
        if (lastLoginName == null || lastLoginName.trim().length() == 0) {
            lastLoginName = ConnexionPanel.LOGIN_ADMIN;
        }
        final String storedPassword = allowStoredPass ? UserProps.getInstance().getStoredPassword() : null;
        this.encryptedPassword = storedPassword;
        this.connectionAllowed = null;

        this.setLayout(new GridBagLayout());

        final GridBagConstraints c = new GridBagConstraints();
        c.gridheight = 1;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Logo
        imageLogo.check();
        this.add(imageLogo, c);
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(new JSeparator(SwingConstants.HORIZONTAL), c);

        // Login
        c.insets = new Insets(2, 2, 1, 2);
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        final JLabel login = new JLabel("Identifiant");
        login.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(login, c);

        this.textLogin = new EmptyValueWrapper<String>(ValueWrapperFactory.create(new JTextField(), String.class));
        this.textLogin.setValue(lastLoginName);
        c.gridx++;
        c.weightx = 1;
        this.add(this.textLogin.getComp(), c);
        ((JTextField) this.textLogin.getComp()).addActionListener(this);
        this.textLogin.addEmptyListener(new EmptyListener() {
            public void emptyChange(final EmptyObj src, final boolean newValue) {
                checkValidity();
            }
        });

        // Password
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        final JLabel passWord = new JLabel("Mot de passe");
        passWord.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(passWord, c);

        this.textPassWord = new JPasswordField();
        // to show the user its password has been retrieved
        if (storedPassword != null) {
            final char[] s = new char[8];
            Arrays.fill(s, ' ');
            this.textPassWord.setText(new String(s));
            this.clearPassword = null;
        } else
            this.clearPassword = "";
        c.gridx++;
        c.weightx = 1;
        this.add(this.textPassWord, c);
        this.textPassWord.getDocument().addDocumentListener(new SimpleDocumentListener() {
            public void update(final DocumentEvent e) {
                ConnexionPanel.this.clearPassword = String.valueOf(ConnexionPanel.this.textPassWord.getPassword());
                checkValidity();
            }
        });
        this.textPassWord.addActionListener(this);

        if (societeSelector) {
            // Societe

            c.gridy++;
            c.gridx = 0;
            c.weightx = 0;
            final JLabel societe = new JLabel("Société");
            societe.setHorizontalAlignment(SwingConstants.RIGHT);
            this.add(societe, c);

            final SQLTable tableSociete = this.login.getUserTable().getDBRoot().findTable("SOCIETE_COMMON");

            if (tableSociete == null) {
                throw ExceptionUtils.createExn(IllegalStateException.class, "Table manquante: SOCIETE_COMMON", null);
            }

            this.comboSociete = new SQLRequestComboBox(false, 25);
            final IComboModel model = new IComboModel(Configuration.getInstance().getDirectory().getElement(tableSociete).getComboRequest());
            final int lastSociete = UserProps.getInstance().getLastSocieteID();
            if (lastSociete >= SQLRow.MIN_VALID_ID) {
                model.setValue(lastSociete);
            } else {
                model.setFirstFillSelection(new ITransformer<List<IComboSelectionItem>, IComboSelectionItem>() {
                    @Override
                    public IComboSelectionItem transformChecked(List<IComboSelectionItem> input) {
                        // Guillaume 25/06/2010
                        return CollectionUtils.getFirst(input);
                    }
                });
            }
            this.comboSociete.uiInit(model);

            this.comboSociete.addEmptyListener(new EmptyListener() {
                public void emptyChange(final EmptyObj src, final boolean newValue) {
                    checkValidity();
                }
            });
            this.comboSociete.addValueListener(new PropertyChangeListener() {
                public void propertyChange(final PropertyChangeEvent evt) {
                    checkValidity();
                }
            });
            c.gridx++;
            this.add(this.comboSociete, c);
        }

        // Button
        final JPanel panelButton = new JPanel();
        panelButton.setOpaque(false);
        panelButton.setLayout(new GridBagLayout());
        final GridBagConstraints c2 = new DefaultGridBagConstraints();
        c2.weightx = 1;
        if (allowStoredPass) {
            this.saveCheckBox.setOpaque(false);
            panelButton.add(this.saveCheckBox, c2);
            c2.weightx = 0;
            if (storedPassword != null && storedPassword.length() > 0) {
                this.saveCheckBox.setSelected(true);
            }
        }
        c2.gridx++;
        this.reloadPanel = new ReloadPanel();
        this.reloadPanel.setOpaque(false);
        /*
         * reloadPanel.setPreferredSize(new Dimension(20, 20)); reloadPanel.setMinimumSize(new
         * Dimension(20, 20)); reloadPanel.setMaximumSize(new Dimension(20, 20));
         */
        this.reloadPanel.setMode(ReloadPanel.MODE_EMPTY);
        panelButton.add(this.reloadPanel, c2);
        c2.gridx++;
        c2.weightx = 0;
        buttonConnect.setOpaque(false);
        panelButton.add(this.buttonConnect, c2);

        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 1;
        this.add(panelButton, c);

        this.buttonConnect.addActionListener(this);

        checkValidity();
    }

    private void checkValidity() {
        this.buttonConnect.setEnabled(this.connectionAllowed == null && this.areFieldsValidated());
        this.buttonConnect.setToolTipText(this.connectionAllowed);
    }

    private boolean areFieldsValidated() {
        if (this.societeSelector) {
            final SQLRow selectedRow = this.comboSociete.getSelectedRow();
            if (selectedRow == null || !selectedRow.isData()) {
                return false;
            }
        }

        if (this.textLogin == null || this.textLogin.isEmpty() || this.textPassWord == null) {
            return false;
        } else {
            return this.textLogin.getValidState().isValid();
        }
    }

    public void actionPerformed(final ActionEvent e) {
        if (isConnecting())
            return;

        if (this.textLogin.getValue().length() < 1) {
            return;
        }

        setConnecting(true);

        final Thread t = new Thread(new Runnable() {
            public void run() {
                connect();
            }
        });
        t.setName("ConnexionPanel Login");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    public void setConnectionAllowed(String connectionAllowed) {
        this.connectionAllowed = connectionAllowed;
        this.checkValidity();
    }

    private synchronized boolean isConnecting() {
        return this.isConnecting;
    }

    private synchronized void setConnecting(final boolean b) {

        if (b) {
            this.reloadPanel.setMode(ReloadPanel.MODE_ROTATE);
            this.buttonConnect.setText("Connexion...");
            this.buttonConnect.setEnabled(false);
            this.saveCheckBox.setEnabled(false);
            if (this.comboSociete != null) {
                this.comboSociete.setEnabled(false);
            }
            ((JTextField) this.textLogin.getComp()).setEditable(false);
            this.textPassWord.setEditable(false);
        } else {
            this.buttonConnect.setText("Connexion");
            this.buttonConnect.setEnabled(true);
            this.saveCheckBox.setEnabled(true);
            if (this.comboSociete != null) {
                this.comboSociete.setEnabled(true);
            }
            ((JTextField) this.textLogin.getComp()).setEditable(true);
            this.textPassWord.setEditable(true);
            this.reloadPanel.setMode(ReloadPanel.MODE_EMPTY);

        }
        this.isConnecting = b;
    }

    private void connect() {
        final Tuple2<String, String> loginRes;
        // if the user has not typed anything and there was a stored pass
        if (this.clearPassword == null)
            loginRes = this.login.connectEnc(this.textLogin.getValue(), this.encryptedPassword);
        else
            // handle legacy passwords
            loginRes = this.login.connectClear(this.textLogin.getValue(), this.clearPassword, "\"" + this.clearPassword + "\"");

        if (loginRes.get0() == null) {
            // --->Connexion
            UserProps.getInstance().setLastLoginName(this.textLogin.getValue());
            if (this.societeSelector) {
                UserProps.getInstance().setLastSocieteID(this.comboSociete.getSelectedId());
            }
            if (this.saveCheckBox.isSelected()) {
                UserProps.getInstance().setEncryptedStoredPassword(loginRes.get1());
            } else
                UserProps.getInstance().setEncryptedStoredPassword(null);
            UserProps.getInstance().store();

            // Fermeture des frames et execution du Runnable
            this.r.run();
            // only dispose the panel after r has run so that there's always something on screen for
            // the user to see
            SwingUtilities.getWindowAncestor(this).dispose();
        } else {
            unlockUIOnError(loginRes.get0());
        }
    }

    private void unlockUIOnError(final String error) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ConnexionPanel.this.reloadPanel.setMode(ReloadPanel.MODE_BLINK);
                JOptionPane.showMessageDialog(ConnexionPanel.this, error);
                // Guillaume wants this for the Nego
                if (Login.UNKNOWN_USER.equals(error))
                    ConnexionPanel.this.textLogin.setValue(ConnexionPanel.LOGIN_ADMIN);
                setConnecting(false);
            }
        });
    }

    public int getSelectedSociete() {
        return this.comboSociete.getSelectedId();
    }
}
