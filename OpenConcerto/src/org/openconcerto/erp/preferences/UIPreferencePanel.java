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
 
 package org.openconcerto.erp.preferences;

import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.table.AlternateTableCellRenderer;
import org.openconcerto.ui.warning.JLabelWarning;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.LineBorder;

public class UIPreferencePanel extends DefaultLocalPreferencePanel {

    private static final String UI_PROPERTIES = "ui.properties";
    private static final String ALTERNATE_COLOR_BLUE = "ui.list.alternate.color.blue";
    private static final String ALTERNATE_COLOR_GREEN = "ui.list.alternate.color.green";
    private static final String ALTERNATE_COLOR_RED = "ui.list.alternate.color.red";
    private static final String UI_LOOK = "ui.look";
    private JLabel selectedButton;
    private JComboBox comboLook;

    public UIPreferencePanel() {
        super("Interface graphique", UI_PROPERTIES);
    }

    @Override
    public void uiInit() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        this.add(new JLabel("Look"), c);
        comboLook = new JComboBox(new String[] { "natif du système", "Nimbus" });
        String look = this.properties.getProperty(UI_LOOK);
        if (look != null && !look.equals("system")) {
            comboLook.setSelectedIndex(1);
        } else {
            comboLook.setSelectedIndex(0);
        }

        c.gridx++;
        this.add(comboLook, c);
        final JLabel labelAlternate = new JLabel("Couleur de fond dans les liste pour l'alternance");
        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy++;
        this.add(labelAlternate, c);
        List<Color> colors = new ArrayList<Color>();
        // Gris
        int col = 245;
        colors.add(new Color(col, col, col));
        col -= 10;
        colors.add(new Color(col, col, col));
        col -= 10;
        colors.add(new Color(col, col, col));
        col -= 10;
        colors.add(new Color(col, col, col));
        // Bleu petrole
        colors.add(new Color(232, 232, 240));
        colors.add(new Color(206, 206, 222));
        colors.add(new Color(180, 180, 205));
        colors.add(new Color(154, 154, 188));

        // Bleus
        colors.add(new Color(224, 240, 247));
        colors.add(new Color(190, 222, 239));
        colors.add(new Color(155, 205, 230));
        colors.add(new Color(120, 188, 221));

        // Bleus vert
        colors.add(new Color(217, 240, 247));
        colors.add(new Color(173, 222, 239));
        colors.add(new Color(130, 205, 230));
        colors.add(new Color(87, 188, 221));
        // Bleus Ciel
        colors.add(new Color(224, 247, 240));
        colors.add(new Color(190, 239, 222));
        colors.add(new Color(155, 230, 205));
        colors.add(new Color(120, 221, 188));
        // Violet
        colors.add(new Color(240, 217, 232));
        colors.add(new Color(222, 173, 206));
        colors.add(new Color(205, 130, 180));
        colors.add(new Color(188, 87, 154));

        // Marrons
        colors.add(new Color(240, 224, 217));
        colors.add(new Color(222, 190, 173));
        colors.add(new Color(205, 155, 130));
        colors.add(new Color(188, 120, 87));

        final JPanel colorPanel = new JPanel();
        colorPanel.setBorder(new LineBorder(Color.white, 2));
        colorPanel.setOpaque(true);
        colorPanel.setBackground(Color.WHITE);
        colorPanel.setLayout(new GridLayout(colors.size() / 4, 4, 2, 2));
        final JLabel[] buttonsAlternate = new JLabel[colors.size()];

        for (int i = 0; i < colors.size(); i++) {
            final Color color = colors.get(i);
            final JLabel button = new JLabel("    label test   ");
            button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
            buttonsAlternate[i] = button;
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectButton(button);
                }
            });
            button.setOpaque(true);
            button.setBackground(color);
            colorPanel.add(button);
        }
        c.gridy++;

        final String cRed = properties.getProperty(ALTERNATE_COLOR_RED);
        final String cGreen = properties.getProperty(ALTERNATE_COLOR_GREEN);
        final String cBlue = properties.getProperty(ALTERNATE_COLOR_BLUE);
        int r = -1, g = -1, b = -1;
        if (cRed != null) {
            r = Integer.parseInt(cRed);
        }
        if (cGreen != null) {
            g = Integer.parseInt(cGreen);
        }
        if (cBlue != null) {
            b = Integer.parseInt(cBlue);

        }
        selectButton(buttonsAlternate[1]);
        // Select the saved color setting
        if (r >= 0 && g >= 0 && b >= 0) {
            final Color expectedColor = new Color(r, g, b);
            for (int i = 0; i < buttonsAlternate.length; i++) {
                final JLabel jLabel = buttonsAlternate[i];
                if (jLabel.getBackground().equals(expectedColor)) {
                    selectButton(jLabel);
                    break;
                }
            }
        }

        this.add(colorPanel, c);
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        this.add(new JLabelWarning("Les modifications d'interface nécessitent un redémarrage du logiciel."), c);
    }

    private void selectButton(JLabel button) {
        if (button != this.selectedButton) {
            if (selectedButton != null) {
                this.selectedButton.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
            }
            button.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            this.selectedButton = button;
        }
    }

    @Override
    public void storeValues() {
        final Color background = this.selectedButton.getBackground();
        properties.setProperty(ALTERNATE_COLOR_RED, String.valueOf(background.getRed()));
        properties.setProperty(ALTERNATE_COLOR_GREEN, String.valueOf(background.getGreen()));
        properties.setProperty(ALTERNATE_COLOR_BLUE, String.valueOf(background.getBlue()));
        AlternateTableCellRenderer.setDefaultMap(Collections.singletonMap(Color.WHITE, background));

        try {
            if (this.comboLook.getSelectedIndex() == 0) {
                properties.setProperty(UI_LOOK, "system");
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } else {
                properties.setProperty(UI_LOOK, "nimbus");
                useNimbusLF();
            }
        } catch (Exception e) {
            ExceptionHandler.handle("Unable to set L&F", e);
        }
        super.storeValues();
    }

    private static void useNimbusLF() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(getNimbusClassName());
        UIManager.put("control", new Color(240, 240, 240));
        UIManager.put("Table.showGrid", Boolean.TRUE);
        UIManager.put("FormattedTextField.background", new Color(240, 240, 240));
        UIManager.put("Table.alternateRowColor", Color.WHITE);
    }

    // only available from sun's release 6u10
    public static String getNimbusClassName() {
        // http://java.sun.com/javase/6/docs/technotes/guides/jweb/otherFeatures/nimbus_laf.html
        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                return info.getClassName();
            }
        }
        return null;
    }

    public static void initUIFromPreferences() {
        Properties properties;
        try {
            properties = getPropertiesFromFile(UI_PROPERTIES);
            final String cRed = properties.getProperty(ALTERNATE_COLOR_RED);
            final String cGreen = properties.getProperty(ALTERNATE_COLOR_GREEN);
            final String cBlue = properties.getProperty(ALTERNATE_COLOR_BLUE);
            int r = -1, g = -1, b = -1;
            if (cRed != null) {
                r = Integer.parseInt(cRed);
            }
            if (cGreen != null) {
                g = Integer.parseInt(cGreen);
            }
            if (cBlue != null) {
                b = Integer.parseInt(cBlue);
            }
            if (r >= 0 && g >= 0 && b >= 0) {
                AlternateTableCellRenderer.setDefaultMap(Collections.singletonMap(Color.WHITE, new Color(r, g, b)));
            }

            final String look = properties.getProperty(UI_LOOK);
            final String nimbusClassName = getNimbusClassName();

            if (look != null && look.equals("system")) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } else if (look != null && look.equals("nimbus")) {
                useNimbusLF();
            } else if (nimbusClassName == null || !System.getProperty("os.name", "??").toLowerCase().contains("linux")) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } else {
                useNimbusLF();
            }

        } catch (Exception e) {
            ExceptionHandler.handle("Unable to restore UI preferences", e);
        }
    }
}
