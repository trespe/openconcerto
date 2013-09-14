package org.openconcerto.modules.extensionbuilder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.modules.extensionbuilder.component.ComponentCreateMainPanel;
import org.openconcerto.modules.extensionbuilder.component.ComponentDescritor;
import org.openconcerto.modules.extensionbuilder.list.ListCreateMainPanel;
import org.openconcerto.modules.extensionbuilder.list.ListDescriptor;
import org.openconcerto.modules.extensionbuilder.menu.MenuMainPanel;
import org.openconcerto.modules.extensionbuilder.table.TableDescritor;
import org.openconcerto.modules.extensionbuilder.table.TableMainPanel;
import org.openconcerto.modules.extensionbuilder.translation.TranslationMainPanel;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.ReloadPanel;
import org.openconcerto.ui.warning.JLabelWarning;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.StreamUtils;

public class ExtensionInfoPanel extends JPanel implements ChangeListener {
    private Extension extension;

    private JFrame frameTableEditor;

    private JFrame frameListEditor;
    private JFrame frameMenuEditor;
    private JFrame frameTranslationEditor;

    ExtensionInfoPanel(Extension extension, ExtensionListPanel moduleListPanel) {
        this.extension = extension;
        this.extension.addChangeListener(this);
        reloadUI();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                reloadUI();

            }
        });

    }

    private void reloadUI() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Not in AWT");
        }
        System.out.println("ExtensionInfoPanel.reloadUI()");
        this.setBackground(Color.WHITE);
        this.invalidate();
        this.removeAll();

        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);

        this.add(createToolbar(), c);
        c.gridy++;

        this.add(new JSeparator(JSeparator.HORIZONTAL), c);
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;

        this.add(createInfoPanel(), c);
        this.revalidate();
    }

    private JPanel createToolbar() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEADING));
        panel.add(new JLabelBold(this.extension.getName()));
        final ReloadPanel reload = new ReloadPanel();
        panel.add(reload);
        final JButton startButton = new JButton("Démarrer");

        panel.add(startButton);
        startButton.setEnabled(!extension.isStarted());
        final JButton stopButton = new JButton("Arrêter");
        panel.add(stopButton);
        stopButton.setEnabled(extension.isStarted());
        final JButton saveButton = new JButton("Enregister");
        saveButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                extension.save();

            }
        });
        saveButton.setEnabled(extension.isNotSaved());
        panel.add(saveButton);
        // TODO: a mettre en clic droit sur la liste gauche
        final JButton exportButton = new JButton("Exporter");
        panel.add(exportButton);
        final JButton importButton = new JButton("Importer");
        panel.add(importButton);

        exportButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String xml = extension.toXML();
                System.out.println(xml);
                FileDialog fDialog = new FileDialog(new JFrame(), "Export", FileDialog.SAVE);
                fDialog.setFile(extension.getName() + ".ocext");
                fDialog.setVisible(true);
                String fileName = fDialog.getFile();
                if (fileName != null) {
                    File file = new File(fDialog.getDirectory(), fDialog.getFile());
                    byte[] bytes = xml.getBytes(Charset.forName("UTF-8"));
                    byte[] open = "openconcerto".getBytes();
                    final int length = bytes.length;
                    for (int i = 0; i < length; i++) {
                        bytes[i] = (byte) (bytes[i] ^ open[i % 12]);
                    }
                    BufferedOutputStream out = null;
                    try {
                        out = new BufferedOutputStream(new FileOutputStream(file));
                        GZIPOutputStream gZ = new GZIPOutputStream(out);
                        gZ.write(bytes);
                        gZ.close();
                    } catch (Exception e1) {
                        ExceptionHandler.handle("Unable to save extension " + file.getAbsolutePath(), e1);
                    } finally {
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e1) {
                                // Nothing
                            }
                        }
                    }

                }

            }
        });
        importButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final FileDialog fDialog = new FileDialog(new JFrame(), "Import", FileDialog.LOAD);
                fDialog.setVisible(true);
                final String fileName = fDialog.getFile();
                String xml = "";
                if (fileName != null && fileName.endsWith(".ocext")) {
                    File file = new File(fDialog.getDirectory(), fDialog.getFile());
                    if (file.exists()) {
                        FileInputStream fIn = null;
                        GZIPInputStream gIn = null;
                        try {
                            fIn = new FileInputStream(file);
                            BufferedInputStream bIn = new BufferedInputStream(fIn);
                            gIn = new GZIPInputStream(bIn);
                            final ByteArrayOutputStream out = new ByteArrayOutputStream();
                            StreamUtils.copy(gIn, out);
                            out.close();
                            // decode
                            byte[] bytes = out.toByteArray();
                            byte[] open = "openconcerto".getBytes();
                            final int length = bytes.length;
                            for (int i = 0; i < length; i++) {
                                bytes[i] = (byte) (bytes[i] ^ open[i % 12]);
                            }
                            xml = new String(bytes, Charset.forName("UTF-8"));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(new JFrame(), "Invalid extension");
                        } finally {
                            if (gIn != null) {
                                try {
                                    gIn.close();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }
                            if (fIn != null) {
                                try {
                                    fIn.close();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }

                    }

                }
                // Create extension from XML
                extension.stop();
                if (!xml.isEmpty()) {
                    if (!extension.isEmpty()) {
                        int result = JOptionPane.showConfirmDialog(ExtensionInfoPanel.this, "Attention l'extension actuelle sera écrasée.\nContinuer l'importation?");
                        if (result != JOptionPane.YES_OPTION) {
                            return;
                        }
                    }
                    extension.clearAll();
                    extension.importFromXML(xml);
                    extension.setChanged();
                }

            }
        });
        startButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                reload.setMode(ReloadPanel.MODE_ROTATE);
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        final DBRoot root = ComptaPropsConfiguration.getInstanceCompta().getRootSociete();
                        try {
                            extension.start(root);
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        stopButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                reload.setMode(ReloadPanel.MODE_ROTATE);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        extension.stop();
                    }
                }).start();
            }
        });
        return panel;
    }

    private JPanel createInfoPanel() {
        final JPanel panel = new JPanel();
        panel.setOpaque(false);
        final GridBagConstraints c = new DefaultGridBagConstraints();
        panel.setLayout(new GridBagLayout());
        c.fill = GridBagConstraints.NONE;
        c.gridy++;
        // --------- TABLES
        panel.add(new JLabelBold("Tables et champs de la base de données"), c);
        c.gridy++;
        final List<TableDescritor> createTableList = this.extension.getCreateTableList();
        if (createTableList.size() > 0) {

            panel.add(new JLabel("Cette extension crée " + createTableList.size() + " tables :"), c);
            c.gridy++;
            c.gridwidth = 1;
            for (final TableDescritor sqlCreateTable : createTableList) {
                c.gridx = 0;
                c.weightx = 0;
                JPanel line = new JPanel();
                line.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
                line.setOpaque(false);
                line.add(new JLabel("- " + sqlCreateTable.getName()));
                if (sqlCreateTable.getFields().size() > 0) {
                    line.add(new JLabel(" " + sqlCreateTable.getFields().size() + " champs"));
                } else {
                    line.add(new JLabelWarning(" " + "aucun champ"));
                }
                panel.add(line, c);
                c.gridy++;
            }

        }
        final List<TableDescritor> modifyTableList = this.extension.getModifyTableList();
        if (modifyTableList.size() > 0) {
            String tables = "";
            for (int i = 0; i < modifyTableList.size(); i++) {
                String tableName = modifyTableList.get(i).getName();
                if (i < modifyTableList.size() - 1) {
                    tables += tableName + ", ";
                } else {
                    tables += tableName;
                }
            }
            if (modifyTableList.size() > 1) {
                panel.add(new JLabel("Tables modifiées : " + tables), c);
            } else {
                panel.add(new JLabel("Table modifiée : " + tables), c);
            }
            c.gridy++;
        }
        final JButton buttonCreateTable = new JButton("Afficher les tables");
        panel.add(buttonCreateTable, c);
        c.gridy++;
        panel.add(new JLabel(" "), c);
        c.gridy++;
        // --------- UI
        panel.add(new JLabelBold("Interfaces de saisie"), c);
        c.gridy++;
        List<ComponentDescritor> createComponentList = extension.getCreateComponentList();
        if (createComponentList.size() > 0) {
            String components = "";
            for (int i = 0; i < createComponentList.size(); i++) {
                String tableName = createComponentList.get(i).getId();
                if (i < createComponentList.size() - 1) {
                    components += tableName + ", ";
                } else {
                    components += tableName;
                }
            }

            if (createComponentList.size() > 1) {
                panel.add(new JLabel("Interfaces créées : " + components), c);
            } else {
                panel.add(new JLabel("Interface créée : " + components), c);
            }
            c.gridy++;
        }

        final JButton buttonCreateComponent = new JButton("Afficher les interfaces");
        panel.add(buttonCreateComponent, c);
        c.gridy++;
        panel.add(new JLabel(" "), c);
        c.gridy++;

        // --------- LIST
        panel.add(new JLabelBold("Listes"), c);
        c.gridy++;
        List<ListDescriptor> createListList = extension.getCreateListList();
        if (createListList.size() > 0) {
            String components = "";
            for (int i = 0; i < createListList.size(); i++) {
                String tableName = createListList.get(i).getId();
                if (i < createListList.size() - 1) {
                    components += tableName + ", ";
                } else {
                    components += tableName;
                }
            }

            if (createListList.size() > 1) {
                panel.add(new JLabel("Listes créées : " + components), c);
            } else {
                panel.add(new JLabel("Liste créée : " + components), c);
            }
            c.gridy++;
        }

        final JButton buttonCreateList = new JButton("Afficher les listes");
        panel.add(buttonCreateList, c);
        c.gridy++;
        panel.add(new JLabel(" "), c);
        c.gridy++;
        // --------- MENUS
        panel.add(new JLabelBold("Menus et actions"), c);
        c.gridy++;
        final int actionCount = extension.getActionDescriptors().size();
        final int menuCount = extension.getCreateMenuList().size();
        final int menuCount2 = extension.getRemoveMenuList().size();
        if (actionCount > 0) {
            if (actionCount > 1)
                panel.add(new JLabel(actionCount + " actions"), c);
            else
                panel.add(new JLabel(actionCount + " action"), c);

            c.gridy++;
        }
        if (menuCount > 0) {
            if (menuCount > 1)
                panel.add(new JLabel(menuCount + " ajouts de menu"), c);
            else
                panel.add(new JLabel(menuCount + " ajout de menu"), c);
            c.gridy++;
        }
        if (menuCount2 > 0) {
            if (menuCount2 > 1)
                panel.add(new JLabel(menuCount2 + " suppressions de menu"), c);
            else
                panel.add(new JLabel(menuCount2 + " suppression de menu"), c);
            c.gridy++;
        }
        final JButton buttonCreateMenu = new JButton("Afficher les menus");
        panel.add(buttonCreateMenu, c);
        c.gridy++;
        panel.add(new JLabel(" "), c);

        c.gridy++;

        // --------- TRANSLATIONS
        panel.add(new JLabelBold("Traductions et renommage de labels"), c);
        c.gridy++;

        int actionTranslationCount = extension.getActionTranslations().size();
        int menuTranslationCount = extension.getMenuTranslations().size();
        int fieldTranslationCount = extension.getFieldTranslations().size();
        if (fieldTranslationCount > 0) {
            if (fieldTranslationCount > 1)
                panel.add(new JLabel(fieldTranslationCount + " traductions de champs"), c);
            else
                panel.add(new JLabel(fieldTranslationCount + " traduction de champs"), c);

            c.gridy++;
        }
        if (menuTranslationCount > 0) {
            if (menuTranslationCount > 1)
                panel.add(new JLabel(menuTranslationCount + " traductions de menu"), c);
            else
                panel.add(new JLabel(menuTranslationCount + " traduction de menu"), c);
            c.gridy++;
        }

        if (actionTranslationCount > 0) {
            if (actionTranslationCount > 1)
                panel.add(new JLabel(actionTranslationCount + " traductions d'action"), c);
            else
                panel.add(new JLabel(actionTranslationCount + " traduction d'action"), c);
            c.gridy++;
        }

        final JButton buttonCreateTranslation = new JButton("Afficher les traductions");
        panel.add(buttonCreateTranslation, c);
        c.gridy++;
        panel.add(new JLabel(" "), c);
        c.gridy++;
        c.weighty = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        panel.add(new JLabel(" "), c);
        c.gridy++;
        //
        buttonCreateTable.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openTableEditor();
            }
        });
        buttonCreateList.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openListEditor();
            }
        });
        buttonCreateComponent.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openComponentEditor();
            }
        });
        buttonCreateMenu.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openMenuEditor();
            }
        });
        buttonCreateTranslation.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openTranslationEditor();
            }
        });
        return panel;
    }

    private void openTableEditor() {
        final TableMainPanel contentPane = new TableMainPanel(extension);
        openEditor(frameTableEditor, contentPane, "Tables dans la base de données");

    }

    private void openListEditor() {
        final ListCreateMainPanel contentPane = new ListCreateMainPanel(extension);
        openEditor(frameListEditor, contentPane, "Listes personnalisées");

    }

    protected void openComponentEditor() {
        final ComponentCreateMainPanel contentPane = new ComponentCreateMainPanel(extension);
        openEditor(frameListEditor, contentPane, "Interfaces personnalisées");

    }

    protected void openMenuEditor() {
        final MenuMainPanel contentPane = new MenuMainPanel(extension);
        openEditor(frameMenuEditor, contentPane, "Menus et actions");

    }

    protected void openTranslationEditor() {
        final TranslationMainPanel contentPane = new TranslationMainPanel(extension);
        openEditor(frameTranslationEditor, contentPane, "Traductions");

    }

    private void openEditor(JFrame frame, JPanel mainPanel, String title) {
        if (frame == null) {
            frame = new JFrame();
            frame.setTitle(extension.getName() + " - " + title);
            frame.setContentPane(mainPanel);
            frame.setMinimumSize(new Dimension(796, 560));
            frame.pack();
            frame.setLocationRelativeTo(ExtensionInfoPanel.this);
        }
        FrameUtil.show(frame);
    }
}
