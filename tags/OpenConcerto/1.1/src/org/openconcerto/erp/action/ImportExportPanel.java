package org.openconcerto.erp.action;

import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openconcerto.erp.model.ImportModel;
import org.openconcerto.sql.model.SQLTable;

public class ImportExportPanel extends JPanel {

    private JTable table;
    private SQLTable table2Import;
    private ImportModel model;
    private JTextField textPathFic, textFormatDate;
    private JRadioButton radioSep = new JRadioButton("Séparateur");
    private JRadioButton radioLongueurFixe = new JRadioButton("Longueur fixe");
    private JTextField textSep = new JTextField(1);

    public ImportExportPanel(SQLTable table2Import, List fields) {
        this(table2Import, fields, false);
    }

    // FIXME l'import peut amener un desequilibre dans la compta
    public ImportExportPanel(SQLTable table2Import, List fields, boolean export) {
        super();

        this.table2Import = table2Import;
        final Date dateDay = new Date();
        this.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 1, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 2;
        c.gridheight = 1;

        JLabel labelImport;
        if (export) {
            labelImport = new JLabel("Fichier de sauvegarde");
        } else {
            labelImport = new JLabel("Fichier d'importation");
        }
        this.add(labelImport, c);

        this.textPathFic = new JTextField();
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 1;
        c.weightx = 1;
        this.add(this.textPathFic, c);

        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        JButton boutonChooseFile = new JButton("...");
        this.add(boutonChooseFile, c);
        boutonChooseFile.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                FileDialog fileDiag = new FileDialog((JFrame) SwingUtilities.getRoot(ImportExportPanel.this), "Sélection du fichier à importer", FileDialog.LOAD);
                fileDiag.pack();
                fileDiag.setVisible(true);
                String nom = fileDiag.getFile();
                if (nom != null) {
                    textPathFic.setText(fileDiag.getDirectory() + fileDiag.getFile());
                }
            }
        });

        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel labelDate = new JLabel("Format des dates");
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(labelDate, c);

        // Format des dates
        this.textFormatDate = new JTextField();
        c.gridwidth = 1;
        c.gridx = GridBagConstraints.RELATIVE;
        c.weightx = 1;
        this.add(this.textFormatDate, c);

        final JLabel labelDateTips = new JLabel();
        c.gridx = GridBagConstraints.RELATIVE;
        c.weightx = 0;
        this.add(labelDateTips, c);

        // longueur fixe ou séparateur
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        this.add(this.radioSep, c);
        c.gridx++;

        this.add(this.textSep, c);
        c.gridy++;
        c.gridx = 0;
        this.add(this.radioLongueurFixe, c);

        ButtonGroup group = new ButtonGroup();
        group.add(this.radioLongueurFixe);
        group.add(this.radioSep);
        this.radioSep.setSelected(true);

        JButton up = new JButton("UP");
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        this.add(up, c);
        up.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                int selectRow = table.getSelectedRow();

                selectRow = model.upField(selectRow);
                table.setRowSelectionInterval(selectRow, selectRow);
            }
        });

        JButton down = new JButton("DOWN");
        c.gridx = GridBagConstraints.RELATIVE;
        this.add(down, c);
        down.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                int selectRow = table.getSelectedRow();
                selectRow = model.downField(selectRow);
                table.setRowSelectionInterval(selectRow, selectRow);
            }
        });

        this.model = new ImportModel(fields);
        this.table = new JTable(this.model);
        c.gridy++;
        c.gridx = 0;
        c.weighty = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;

        JScrollPane scroll = new JScrollPane(this.table);
        Dimension d;
        if (this.table.getPreferredSize().height > 200) {
            d = new Dimension(scroll.getPreferredSize().width, 200);
        } else {
            d = new Dimension(scroll.getPreferredSize().width, this.table.getPreferredSize().height + 30);
        }
        scroll.setPreferredSize(d);

        this.add(scroll, c);

        this.textFormatDate.getDocument().addDocumentListener(new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {

                setLabelDate(labelDateTips, dateDay);
            }

            public void removeUpdate(DocumentEvent e) {
                setLabelDate(labelDateTips, dateDay);
            }

            public void changedUpdate(DocumentEvent e) {

                setLabelDate(labelDateTips, dateDay);
            }
        });
        this.textFormatDate.setText("yyyy-MM-dd");
    }

    private void setLabelDate(JLabel label, Date d) {
        try {
            DateFormat dateFormat = new SimpleDateFormat(textFormatDate.getText());
            label.setText("Ex : " + dateFormat.format(d));
        } catch (IllegalArgumentException e) {
            label.setText("format invalide");
            e.printStackTrace();
        }
    }

    public boolean isLongueurFixeSelected() {
        return this.radioLongueurFixe.isSelected();
    }

    public String getSeparator() {
        return this.textSep.getText();
    }

    public String getPathFile() {
        return this.textPathFic.getText();
    }

    public ImportModel getModel() {
        return this.model;
    }

    public String getFormatDate() {
        return this.textFormatDate.getText();
    }

    public SQLTable getTable2Import() {
        return this.table2Import;
    }
}
