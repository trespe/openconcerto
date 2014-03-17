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
 
 package org.openconcerto.sql.utils;

import static org.openconcerto.sql.TM.getTM;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.ReloadPanel;
import org.openconcerto.ui.VFlowLayout;
import org.openconcerto.ui.preferences.BackupProps;
import org.openconcerto.utils.Backup;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

public class BackupPanel extends JPanel implements ActionListener {

    private final DateFormat format = new SimpleDateFormat("EEEE", getTM().getTranslationsLocale());

    private JProgressBar barDB = new JProgressBar();
    private JTextField textDest = new JTextField();
    private JButton buttonBackup = new JButton(getTM().translate("backup"));
    private JButton buttonBrowse = new JButton("...");

    private JLabel labelErrors = new JLabel(" ");
    private JTextComponent labelLastBackup = new JTextArea();
    private JLabel labelState = new JLabelBold(" ");
    JButton buttonClose;
    private List<String> listDb;
    private List<File> dirs2save;
    private boolean closed = false;
    private boolean autoClose = false;
    private ReloadPanel reloadPanel = new ReloadPanel();

    BackupProps props;

    public BackupPanel(List<String> listDB, List<File> dirs2Save, boolean startNow, BackupProps props) {
        super(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        this.props = props;
        Insets defaultInsets = new Insets(2, 2, 1, 2);

        Insets separatorInsets = new Insets(12, 2, 1, 2);

        // TODO ADD JLABEL TO SHOW THE COPIED FILES
        // TODO SHOW ERRORS

        this.listDb = listDB;
        this.dirs2save = dirs2Save;

        JPanel topPanel = new JPanel(new VFlowLayout(VFlowLayout.MIDDLE, 4, 2, true));

        topPanel.setBackground(Color.white);

        topPanel.add(new JLabelBold(getTM().translate("backup")));
        topPanel.add(this.labelLastBackup);

        this.labelLastBackup.setEditable(false);
        this.labelLastBackup.setFont(this.labelErrors.getFont());
        updateLastBackup();

        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 0);
        this.add(topPanel, c);
        c.gridy++;
        this.add(new JSeparator(), c);

        JLabelBold sep1 = new JLabelBold(getTM().translate("location"));
        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.insets = DefaultGridBagConstraints.getDefaultInsets();
        this.add(sep1, c);

        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        JPanel panelEmplacement = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.textDest.setEditable(false);
        this.textDest.setBackground(Color.white);
        // this.textDest.setEnabled(false);
        c.gridwidth = startNow ? GridBagConstraints.REMAINDER : 1;
        this.add(this.textDest, c);
        this.textDest.setText(props.getDestination());

        if (!startNow) {
            c.gridx++;
            c.weightx = 0;
            this.add(this.buttonBrowse, c);
        }

        this.add(panelEmplacement, c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(new JLabel(getTM().translate("backupPanel.differentDisks")), c);

        // Progression
        final JLabelBold sep = new JLabelBold(getTM().translate("backupPanel.progress"));
        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.insets = separatorInsets;
        this.add(sep, c);

        c.weightx = 1;
        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = defaultInsets;
        c.gridwidth = 1;

        this.add(this.barDB, c);
        this.barDB.setMinimum(0);
        this.barDB.setMaximum(2 + this.dirs2save.size());

        c.gridx++;
        c.weightx = 0;
        this.reloadPanel.setMode(ReloadPanel.MODE_EMPTY);
        this.add(this.reloadPanel, c);

        // Erreurs
        c.gridy++;
        c.gridx = 0;
        this.add(this.labelErrors, c);
        int errors = props.getErrors();
        if (errors > 0) {
            this.labelErrors.setText(getTM().translate("backupPanel.errorsOnLastBackup"));
        }

        // Etat de la sauvegarde
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy++;
        this.add(this.labelState, c);

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.SOUTHEAST;
        c.gridy++;
        c.weighty = 1;

        JPanel panelButton = new JPanel();
        this.buttonClose = new JButton(getTM().translate("close"));
        if (!startNow) {
            panelButton.add(this.buttonBackup);
        }
        panelButton.add(this.buttonClose);
        this.add(panelButton, c);

        this.buttonClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (BackupPanel.this.autoClose) {
                    BackupPanel.this.closed = true;
                } else {
                    BackupPanel.this.closed = true;
                    ((Window) SwingUtilities.getRoot(BackupPanel.this)).dispose();
                    doOnClose();
                }
            }
        });

        this.buttonBackup.addActionListener(this);
        this.buttonBrowse.addActionListener(this);

        if (startNow && this.textDest.getText().trim().length() > 0) {
            sauvegarde();
        }
    }

    protected void updateLastBackup() {
        final Date lastBackup = this.props.getLastBackup();
        if (lastBackup != null) {
            this.labelLastBackup.setText(getTM().trM("backupPanel.lastBackup", "date", lastBackup, "destination", this.props.getProperty("LastBackupDestination")));
        } else {
            this.labelLastBackup.setText("");
        }
    }

    public void doOnClose() {
        // Default do nothing
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.buttonBrowse) {
            JFileChooser choose = new JFileChooser();
            choose.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (choose.showDialog(this, getTM().translate("choose")) == JFileChooser.APPROVE_OPTION) {
                final File selectedFile = choose.getSelectedFile();
                final String absolutePath = selectedFile.getAbsolutePath();
                this.textDest.setText(absolutePath);
                props.setDestination(absolutePath);
                props.store();
            }
        } else {
            if (e.getSource() == this.buttonBackup) {
                sauvegarde();
            }
        }
    }

    public final void sauvegarde() {
        if (!this.buttonBackup.isEnabled())
            return;

        this.barDB.setStringPainted(false);
        this.buttonBackup.setEnabled(false);
        this.labelState.setText(getTM().translate("backupPanel.inProgress"));
        this.reloadPanel.setMode(ReloadPanel.MODE_ROTATE);
        this.barDB.setValue(1);

        final String dest = this.textDest.getText();
        final File fTmp = new File(dest, Configuration.getInstance().getAppName());
        final File fDest = new File(fTmp, this.format.format(new Date()));

        final Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    // Test d'accés au répertoire de destination
                    if (!fDest.exists() && !fDest.mkdirs()) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                JOptionPane.showMessageDialog(BackupPanel.this, getTM().translate("backupPanel.createFolderError"));
                            }
                        });
                    } else {

                        File testFileCreate = new File(fDest, "testrw");
                        try {
                            if (!testFileCreate.createNewFile()) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        JOptionPane.showMessageDialog(BackupPanel.this, getTM().translate("backupPanel.folderRightsError"));

                                        BackupPanel.this.barDB.setValue(0);
                                    }
                                });
                            } else {
                                testFileCreate.delete();
                            }
                        } catch (IOException e1) {
                            e1.printStackTrace();
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    JOptionPane.showMessageDialog(BackupPanel.this, getTM().translate("backupPanel.folderRightsError"));
                                    BackupPanel.this.barDB.setValue(0);
                                }
                            });
                        }

                        int errors = 0;
                        props.setProperty("LastBackupDestination", BackupPanel.this.textDest.getText());

                        // Sauvegarde de la base
                        if (BackupPanel.this.listDb != null) {

                            final DBRoot root = Configuration.getInstance().getRoot();
                            final DBSystemRoot sysRoot = root.getDBSystemRoot();
                            final SQLSystem system = sysRoot.getServer().getSQLSystem();

                            // Sauvegarde pour H2
                            if (system == SQLSystem.H2) {
                                sysRoot.getDataSource().execute("backup to " + root.getBase().quoteString(new File(fDest, "Base.zip").getAbsolutePath()));
                                // TODO don't backup H2 files below ('backup to' is the only safe
                                // way);
                            } else {
                                // Sauvegarde autres bases
                                File fBase = new File(fDest, "Base");
                                Copy copy;
                                try {
                                    copy = new Copy(true, fBase, sysRoot, false, false);
                                    for (String db : BackupPanel.this.listDb) {
                                        copy.applyTo(db, null);
                                    }

                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    errors++;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    errors++;
                                }
                            }
                        }
                        // Fin de sauvegarde de la base
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                BackupPanel.this.barDB.setValue(2);
                            }
                        });

                        // Sauvegarde des documents
                        if (BackupPanel.this.dirs2save != null) {

                            Backup backup = new Backup(fDest);
                            int i = 1;
                            for (File f : BackupPanel.this.dirs2save) {
                                errors += backup.applyTo(f);
                                final int tmp = i + 2;
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        BackupPanel.this.barDB.setValue(tmp);
                                    }
                                });
                                i++;
                            }
                            backup.close();

                        }
                        // Fin de sauvegarde
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                // BackupPanel.this.barDocs.setIndeterminate(false);
                                // BackupPanel.this.barDocs.setString("Terminée");
                                // BackupPanel.this.barDocs.setStringPainted(true);
                            }
                        });

                        props.setLastBackup(new Date());
                        props.setErrors(errors);

                        props.store();

                        final int backupErrors = errors;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                if (backupErrors > 0) {
                                    BackupPanel.this.labelErrors.setText(getTM().translate("backupPanel.errorsOnLastBackup"));
                                    BackupPanel.this.labelState.setText(getTM().translate("backupPanel.endFail"));
                                    BackupPanel.this.reloadPanel.setMode(ReloadPanel.MODE_BLINK);
                                } else {
                                    BackupPanel.this.labelErrors.setText("");
                                    BackupPanel.this.labelState.setText(getTM().translate("backupPanel.endSuccess"));
                                    BackupPanel.this.reloadPanel.setMode(ReloadPanel.MODE_EMPTY);
                                    closeAfter5Secondes();
                                }

                                updateLastBackup();
                                BackupPanel.this.buttonBackup.setEnabled(true);
                            }

                        });

                    }
                } catch (Exception e) {
                    ExceptionHandler.handle(BackupPanel.this, getTM().translate("backupPanel.failed"), e);
                }
            }
        });
        thread.start();
    }

    Thread t;
    private int seconde = 7;

    public synchronized void closeAfter5Secondes() {
        if (this.t == null) {
            this.t = new Thread() {
                @Override
                public void run() {
                    BackupPanel.this.autoClose = true;
                    BackupPanel.this.seconde = 7;
                    while (BackupPanel.this.seconde > 0 && !BackupPanel.this.closed) {
                        final int rest = BackupPanel.this.seconde;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                BackupPanel.this.buttonClose.setText(getTM().translate("backupPanel.closingIn", rest));
                            }
                        });
                        BackupPanel.this.seconde--;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    BackupPanel.this.autoClose = false;
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            BackupPanel.this.buttonClose.setText(getTM().translate("close"));
                        }
                    });
                    if (!BackupPanel.this.closed) {
                        ((Window) SwingUtilities.getRoot(BackupPanel.this)).dispose();
                        doOnClose();
                    }
                    BackupPanel.this.closed = false;
                }
            };
            this.t.start();
        } else {
            BackupPanel.this.seconde = 7;
        }
    }
}
